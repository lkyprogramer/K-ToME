package com.ktome.game.elites

import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.AiProfileOverride
import com.ktome.core.ecs.EliteMutationLoadout
import com.ktome.core.ecs.PreferredTerrainAffinity
import com.ktome.core.ecs.ResistanceProfile
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.item.StatModifier
import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.TalentRegistry
import com.ktome.game.GameContent
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.SchemaCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EncounterDecorationServiceTest {
    private val loader = DataLoader()
    private val baseSchemaCatalog = loader.loadSchemaCatalog()
    private val talents = loader.loadTalentDefinitions()

    @Test
    fun `preferred boss variant must match requested base encounter`() {
        val content = newContent(baseSchemaCatalog)
        val service = EncounterDecorationService(content)
        val preferredVariant = content.bossVariantRegistry.resolve("boss.variant.grey_crown")
        val request =
            SpawnDecorationRequest(
                zoneId = "deep_iron_pit",
                floorIndex = 4,
                template = content.bossDefinitions.getValue("molten_giant_encounter").template,
                bossEncounterId = "molten_giant_encounter",
                preferredBossVariantId = requireNotNull(preferredVariant).id,
                bossVariantSelectionMode = BossVariantSelectionMode.FORCE_AVAILABLE,
            )

        val ex =
            assertThrows(IllegalArgumentException::class.java) {
                service.selectDecoration(request) { 0 }
            }

        assertTrue(ex.message.orEmpty().contains("belongs to 'dungeon_lord_encounter'"))
    }

    @Test
    fun `multiple mutation stat modifiers are merged into one permanent effect`() {
        val compositeMutation = "elite.test.composite"
        val content =
            newContent(
                baseSchemaCatalog.copy(
                    mutationStatModifiers =
                        baseSchemaCatalog.mutationStatModifiers +
                            listOf(
                                MutationStatModifierDef(
                                    id = "mod.test.composite.alpha",
                                    statModifier = StatModifier(attack = 3, defense = 2),
                                    resistances = mapOf(DamageType.FIRE to 10),
                                ),
                                MutationStatModifierDef(
                                    id = "mod.test.composite.beta",
                                    statModifier = StatModifier(speed = 5, maxHp = 12),
                                    resistances = mapOf(DamageType.COLD to 7),
                                ),
                            ),
                    eliteMutations =
                        baseSchemaCatalog.eliteMutations +
                            EliteMutationDef(
                                id = compositeMutation,
                                kind = MutationKind.STAT_PACKAGE,
                                tier = MutationTier.MAJOR,
                                threatCost = 3,
                                nameKey = "mutation.stonehide.name",
                                iconKey = "icon.mutation.stonehide",
                                applyToTags = setOf("elite"),
                                minFloor = 1,
                                maxFloor = null,
                                allowedZones = emptySet(),
                                statModifiers =
                                    listOf(
                                        StatModifierRef("mod.test.composite.alpha"),
                                        StatModifierRef("mod.test.composite.beta"),
                                    ),
                                grantedTalents = emptyList(),
                                aiProfileOverlay = null,
                                incompatibleWith = emptySet(),
                            ),
                ),
            )
        val service = EncounterDecorationService(content)
        val world = World()
        val entityId = world.createEntity()

        service.applyDecoration(
            world = world,
            entityId = entityId,
            decoration =
                EncounterDecoration(
                    mutations = listOf(requireNotNull(content.eliteMutationRegistry.resolve(compositeMutation))),
                ),
        )

        val tracker = requireNotNull(world.get<EffectTracker>(entityId))
        val activeEffects = tracker.activeEffects()
        val appliedEffect = activeEffects.single()
        val resistances = requireNotNull(world.get<ResistanceProfile>(entityId))

        assertEquals("mutation:$compositeMutation", appliedEffect.schemaId)
        assertEquals(3, appliedEffect.statModifiers.attack)
        assertEquals(2, appliedEffect.statModifiers.defense)
        assertEquals(5, appliedEffect.statModifiers.speed)
        assertEquals(12, appliedEffect.statModifiers.maxHp)
        assertEquals(10, resistances.valueFor(DamageType.FIRE))
        assertEquals(7, resistances.valueFor(DamageType.COLD))
    }

    @Test
    fun `multiple overlay mutations are preserved in mutation loadout instead of a singleton override`() {
        val content = newContent(baseSchemaCatalog)
        val service = EncounterDecorationService(content)
        val world = World()
        val entityId = world.createEntity()

        service.applyDecoration(
            world = world,
            entityId = entityId,
            decoration =
                EncounterDecoration(
                    mutations =
                        listOf(
                            requireNotNull(content.eliteMutationRegistry.resolve("elite.phase_runner")),
                            requireNotNull(content.eliteMutationRegistry.resolve("elite.war_caller")),
                        ),
                ),
        )

        assertEquals(
            listOf("elite.phase_runner", "elite.war_caller"),
            requireNotNull(world.get<EliteMutationLoadout>(entityId)).mutationIds,
        )
        assertEquals(null, world.get<AiProfileOverride>(entityId))
    }

    @Test
    fun `boss variant decoration aggregates preferred terrain tags from granted mutations`() {
        val content = newContent(baseSchemaCatalog)
        val service = EncounterDecorationService(content)

        val decoration =
            service.selectDecoration(
                SpawnDecorationRequest(
                    zoneId = "deep_iron_pit",
                    floorIndex = 2,
                    template = content.bossDefinitions.getValue("molten_giant_encounter").template,
                    bossEncounterId = "molten_giant_encounter",
                    preferredBossVariantId = "boss.variant.molten_glass",
                    bossVariantSelectionMode = BossVariantSelectionMode.FORCE_AVAILABLE,
                ),
            ) { 0 }

        assertEquals(setOf(TerrainTag.OIL), decoration.preferredTerrainTags)
    }

    @Test
    fun `apply decoration caches preferred terrain affinity on the entity`() {
        val content = newContent(baseSchemaCatalog)
        val service = EncounterDecorationService(content)
        val world = World()
        val entityId = world.createEntity()

        service.applyDecoration(
            world = world,
            entityId = entityId,
            decoration =
                EncounterDecoration(
                    mutations = listOf(requireNotNull(content.eliteMutationRegistry.resolve("elite.tidebound"))),
                    preferredTerrainTags = setOf(TerrainTag.WATER),
                ),
        )

        assertEquals(setOf(TerrainTag.WATER), world.get<PreferredTerrainAffinity>(entityId)?.terrainTags)
    }

    @Test
    fun `elite pool entries can force elite mutation eligibility without template elite tags`() {
        val content = newContent(baseSchemaCatalog)
        val service = EncounterDecorationService(content)
        val template = requireNotNull(content.monsterCatalog.firstOrNull { monster -> monster.id == "bandit.sentry" })

        val undecorated =
            service.selectDecoration(
                SpawnDecorationRequest(
                    zoneId = "greenwood_fringe",
                    floorIndex = 2,
                    template = template,
                ),
            ) { 0 }
        val decorated =
            service.selectDecoration(
                SpawnDecorationRequest(
                    zoneId = "greenwood_fringe",
                    floorIndex = 2,
                    template = template,
                    forceEliteMutationEligibility = true,
                ),
            ) { 0 }

        assertTrue(undecorated.mutations.isEmpty())
        assertTrue(decorated.mutations.isNotEmpty())
    }

    @Test
    fun `force elite mutation eligibility injects elite selection tag for preferred terrain mutations`() {
        val terrainAffinityMutation =
            EliteMutationDef(
                id = "elite.a_test_force_terrain_affinity",
                kind = MutationKind.ELEMENT_PACKAGE,
                tier = MutationTier.MAJOR,
                threatCost = 3,
                nameKey = "mutation.test.force_terrain_affinity.name",
                iconKey = "icon.mutation.test.force_terrain_affinity",
                applyToTags = setOf("elite"),
                minFloor = 1,
                maxFloor = null,
                allowedZones = setOf("greenwood_fringe"),
                preferredTerrainTags = listOf(TerrainTag.WATER),
                statModifiers = emptyList(),
                grantedTalents = emptyList(),
                aiProfileOverlay = null,
                incompatibleWith = emptySet(),
            )
        val content =
            newContent(
                baseSchemaCatalog.copy(
                    eliteMutations = baseSchemaCatalog.eliteMutations + terrainAffinityMutation,
                ),
            )
        val service = EncounterDecorationService(content)
        val template = requireNotNull(content.monsterCatalog.firstOrNull { monster -> monster.id == "bandit.sentry" })

        val decoration =
            service.selectDecoration(
                SpawnDecorationRequest(
                    zoneId = "greenwood_fringe",
                    floorIndex = 2,
                    template = template,
                    forceEliteMutationEligibility = true,
                ),
            ) { 0 }

        assertEquals(listOf(terrainAffinityMutation.id), decoration.mutations.map(EliteMutationDef::id))
        assertEquals(setOf(TerrainTag.WATER), decoration.preferredTerrainTags)
    }

    private fun newContent(schemaCatalog: SchemaCatalog): GameContent =
        GameContent(
            talents = talents,
            statuses = schemaCatalog.statuses,
            statusCatalog = loader.loadStatusCatalog(),
            talentRegistry = TalentRegistry().apply { registerAll(talents) },
            monsterCatalog = loader.loadMonsterCatalog().monsters,
            itemBundle = loader.loadItemBundle(),
            bossDefinitions = loader.loadBossDefinitions(),
            schemaCatalog = schemaCatalog,
            localizer = loader.localizer,
        )
}
