package com.ktome.game.elites

import com.ktome.core.mapgen.TerrainTag
import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EliteMutationRegistryTest {
    private val schemaCatalog = DataLoader().loadSchemaCatalog()
    private val phase4Registry =
        EliteMutationRegistry(
            config = schemaCatalog.eliteMutationConfig,
            statModifiersById = schemaCatalog.mutationStatModifiers.associateBy(MutationStatModifierDef::id),
            definitionsById = schemaCatalog.eliteMutations.associateBy(EliteMutationDef::id),
        )

    @Test
    fun `signature tier stays unavailable before late floors even when content exists`() {
        val registry = testRegistry()
        val counts =
            sampleTierCounts(
                registry = registry,
                context =
                    MutationSelectionContext(
                        zoneId = "abyssal_temple",
                        floorIndex = 3,
                        applyToTags = setOf("elite"),
                    ),
            )

        assertEquals(0, counts.getValue(MutationTier.SIGNATURE))
    }

    @Test
    fun `high pressure late zones increase major and signature selection share`() {
        val registry = testRegistry()
        val baseline =
            sampleTierCounts(
                registry = registry,
                context =
                    MutationSelectionContext(
                        zoneId = "deep_iron_pit",
                        floorIndex = 4,
                        applyToTags = setOf("elite"),
                    ),
            )
        val highPressure =
            sampleTierCounts(
                registry = registry,
                context =
                    MutationSelectionContext(
                        zoneId = "abyssal_temple",
                        floorIndex = 4,
                        applyToTags = setOf("elite"),
                    ),
            )

        assertTrue(highPressure.getValue(MutationTier.SIGNATURE) > baseline.getValue(MutationTier.SIGNATURE))
        assertTrue(highPressure.getValue(MutationTier.MAJOR) > baseline.getValue(MutationTier.MAJOR))
        assertTrue(highPressure.getValue(MutationTier.MINOR) < baseline.getValue(MutationTier.MINOR))
    }

    @Test
    fun `phase four registry exposes twelve mutations and locked incompatibility contracts`() {
        val mutationsById = phase4Registry.all().associateBy(EliteMutationDef::id)
        val expectedContracts =
            listOf(
                ExpectedMutationContract(
                    id = "elite.ironhide",
                    kind = MutationKind.STAT_PACKAGE,
                    tier = MutationTier.MINOR,
                    threatCost = 2,
                    minFloor = 1,
                    allowedZones = emptySet(),
                    preferredTerrainTags = emptyList(),
                    incompatibleWith = emptySet(),
                ),
                ExpectedMutationContract(
                    id = "elite.phase_runner",
                    kind = MutationKind.AI_SHIFT,
                    tier = MutationTier.MINOR,
                    threatCost = 2,
                    minFloor = 2,
                    allowedZones = emptySet(),
                    preferredTerrainTags = emptyList(),
                    grantedTalents = listOf("elite_phase_step"),
                    aiProfileOverlay = "ai.elite.phase_runner",
                    incompatibleWith = emptySet(),
                ),
                ExpectedMutationContract(
                    id = "elite.war_caller",
                    kind = MutationKind.ABILITY_GRANT,
                    tier = MutationTier.MAJOR,
                    threatCost = 3,
                    minFloor = 2,
                    allowedZones = emptySet(),
                    preferredTerrainTags = emptyList(),
                    grantedTalents = listOf("elite_war_call"),
                    aiProfileOverlay = "ai.elite.war_caller",
                    incompatibleWith = setOf("elite.battle_drill"),
                ),
                ExpectedMutationContract(
                    id = "elite.corrosion_cloud",
                    kind = MutationKind.AURA,
                    tier = MutationTier.MAJOR,
                    threatCost = 4,
                    minFloor = 2,
                    allowedZones = setOf("deep_iron_pit", "molten_core"),
                    preferredTerrainTags = listOf(TerrainTag.OIL),
                    incompatibleWith = setOf("elite.dread_aura", "elite.void_mirror"),
                    auraStatusId = "ARMOR_BREAK",
                ),
                ExpectedMutationContract(
                    id = "elite.frostbound",
                    kind = MutationKind.ELEMENT_PACKAGE,
                    tier = MutationTier.MAJOR,
                    threatCost = 4,
                    minFloor = 1,
                    allowedZones = setOf("greenwood_fringe", "underground_river", "crystal_cavern"),
                    preferredTerrainTags = listOf(TerrainTag.WATER, TerrainTag.ICE),
                    grantedTalents = listOf("elite_frost_nova"),
                    aiProfileOverlay = "ai.elite.frostbound",
                    incompatibleWith = setOf("elite.emberblood", "elite.tidebound"),
                ),
                ExpectedMutationContract(
                    id = "elite.tidebound",
                    kind = MutationKind.ELEMENT_PACKAGE,
                    tier = MutationTier.MAJOR,
                    threatCost = 4,
                    minFloor = 1,
                    allowedZones = setOf("greenwood_fringe", "underground_river", "crystal_cavern"),
                    preferredTerrainTags = listOf(TerrainTag.WATER),
                    grantedTalents = listOf("glacial_seal"),
                    aiProfileOverlay = "ai.elite.tidebound",
                    incompatibleWith = setOf("elite.emberblood", "elite.frostbound"),
                ),
                ExpectedMutationContract(
                    id = "elite.void_mirror",
                    kind = MutationKind.AURA,
                    tier = MutationTier.SIGNATURE,
                    threatCost = 5,
                    minFloor = 4,
                    allowedZones = setOf("grey_gate_depths", "abyssal_temple", "abyssal_heart"),
                    preferredTerrainTags = emptyList(),
                    incompatibleWith = setOf("elite.corrosion_cloud", "elite.dread_aura"),
                    auraStatusId = "ARCANE_SHIELD_BUFF",
                ),
            )
        val expectedIncompatibilityMap =
            mapOf(
                "elite.battle_drill" to setOf("elite.war_caller"),
                "elite.dread_aura" to setOf("elite.battle_drill", "elite.corrosion_cloud", "elite.void_mirror"),
                "elite.emberblood" to setOf("elite.frostbound", "elite.tidebound"),
                "elite.war_caller" to setOf("elite.battle_drill"),
                "elite.corrosion_cloud" to setOf("elite.dread_aura", "elite.void_mirror"),
                "elite.frostbound" to setOf("elite.emberblood", "elite.tidebound"),
                "elite.tidebound" to setOf("elite.emberblood", "elite.frostbound"),
                "elite.void_mirror" to setOf("elite.corrosion_cloud", "elite.dread_aura"),
            )

        assertEquals(12, mutationsById.size)
        assertEquals(2, phase4Registry.config.maxMutationsPerElite)
        expectedContracts.forEach { expected ->
            val mutation = requireNotNull(mutationsById[expected.id])
            assertEquals(expected.kind, mutation.kind, "${expected.id} kind")
            assertEquals(expected.tier, mutation.tier, "${expected.id} tier")
            assertEquals(expected.threatCost, mutation.threatCost, "${expected.id} threatCost")
            assertEquals(expected.minFloor, mutation.minFloor, "${expected.id} minFloor")
            assertEquals(expected.allowedZones, mutation.allowedZones, "${expected.id} allowedZones")
            assertEquals(expected.preferredTerrainTags, mutation.preferredTerrainTags, "${expected.id} preferredTerrainTags")
            assertEquals(expected.grantedTalents, mutation.grantedTalents.map(TalentGrantRef::talentId), "${expected.id} grantedTalents")
            assertEquals(expected.aiProfileOverlay, mutation.aiProfileOverlay, "${expected.id} aiProfileOverlay")
            assertEquals(expected.incompatibleWith, mutation.incompatibleWith, "${expected.id} incompatibleWith")
            assertEquals(expected.auraStatusId, mutation.auraStatusId, "${expected.id} auraStatusId")
        }
        expectedIncompatibilityMap.forEach { (mutationId, incompatibleWith) ->
            assertEquals(incompatibleWith, requireNotNull(mutationsById[mutationId]).incompatibleWith, "$mutationId incompatibility contract")
        }
    }

    @Test
    fun `double mutation selection respects floor zone incompatibility and dual signature lock`() {
        var cursor = 0
        repeat(256) {
            val selected =
                phase4Registry.select(
                    context =
                        MutationSelectionContext(
                            zoneId = "abyssal_heart",
                            floorIndex = 5,
                            applyToTags = setOf("elite", "abyssal"),
                            allowDoubleMutation = true,
                        ),
                ) { bound ->
                    val roll = cursor % bound
                    cursor += 1
                    roll
                }

            assertTrue(selected.size <= 2)
            assertTrue(selected.count { mutation -> mutation.tier == MutationTier.SIGNATURE } <= 1)
            assertTrue(
                selected.all { mutation ->
                    mutation.minFloor <= 5 &&
                        (mutation.allowedZones.isEmpty() || "abyssal_heart" in mutation.allowedZones)
                },
            )
            if (selected.size == 2) {
                val left = selected[0]
                val right = selected[1]
                assertTrue(right.id !in left.incompatibleWith && left.id !in right.incompatibleWith)
            }
        }
    }

    @Test
    fun `elite mutation def defaults preferred terrain to empty list and rejects duplicates`() {
        val mutation =
            EliteMutationDef(
                id = "elite.test.default_terrain",
                kind = MutationKind.STAT_PACKAGE,
                tier = MutationTier.MINOR,
                threatCost = 1,
                nameKey = "mutation.test.default_terrain.name",
                iconKey = "icon.mutation.test.default_terrain",
                applyToTags = setOf("elite"),
                minFloor = 1,
                maxFloor = null,
                allowedZones = emptySet(),
                statModifiers = emptyList(),
                grantedTalents = emptyList(),
                aiProfileOverlay = null,
                incompatibleWith = emptySet(),
            )

        assertEquals(emptyList<TerrainTag>(), mutation.preferredTerrainTags)
        assertThrows(IllegalArgumentException::class.java) {
            mutation.copy(preferredTerrainTags = listOf(TerrainTag.WATER, TerrainTag.WATER))
        }
    }

    @Test
    fun `matching terrain affinity increases mutation selection share inside its formal zones`() {
        val registry =
            EliteMutationRegistry(
                config = EliteMutationConfig(),
                statModifiersById = emptyMap(),
                definitionsById =
                    listOf(
                        EliteMutationDef(
                            id = "elite.generic_major",
                            kind = MutationKind.AURA,
                            tier = MutationTier.MAJOR,
                            threatCost = 3,
                            nameKey = "mutation.test.generic_major.name",
                            iconKey = "icon.mutation.test.generic_major",
                            applyToTags = setOf("elite"),
                            minFloor = 1,
                            maxFloor = null,
                            allowedZones = setOf("greenwood_fringe"),
                            statModifiers = emptyList(),
                            grantedTalents = emptyList(),
                            aiProfileOverlay = null,
                            incompatibleWith = emptySet(),
                        ),
                        EliteMutationDef(
                            id = "elite.terrain_major",
                            kind = MutationKind.ELEMENT_PACKAGE,
                            tier = MutationTier.MAJOR,
                            threatCost = 3,
                            nameKey = "mutation.test.terrain_major.name",
                            iconKey = "icon.mutation.test.terrain_major",
                            applyToTags = setOf("elite"),
                            minFloor = 1,
                            maxFloor = null,
                            allowedZones = setOf("greenwood_fringe"),
                            preferredTerrainTags = listOf(TerrainTag.WATER),
                            statModifiers = emptyList(),
                            grantedTalents = emptyList(),
                            aiProfileOverlay = null,
                            incompatibleWith = emptySet(),
                        ),
                    ).associateBy(EliteMutationDef::id),
            )

        val counts =
            mutableMapOf(
                "elite.generic_major" to 0,
                "elite.terrain_major" to 0,
            )
        var cursor = 0
        repeat(240) {
            val selected =
                registry.select(
                    MutationSelectionContext(
                        zoneId = "greenwood_fringe",
                        floorIndex = 1,
                        applyToTags = setOf("elite"),
                    ),
                ) { bound ->
                    val roll = cursor % bound
                    cursor += 1
                    roll
                }
            counts.compute(selected.single().id) { _, value -> (value ?: 0) + 1 }
        }

        assertTrue(counts.getValue("elite.terrain_major") > counts.getValue("elite.generic_major"))
    }

    @Test
    fun `greenwood elite bandit context unlocks terrain affinity mutations`() {
        val withoutEliteTag = sampleMutationIds(zoneId = "greenwood_fringe", floorIndex = 2, applyToTags = setOf("monster", "bandit"))
        val withEliteTag = sampleMutationIds(zoneId = "greenwood_fringe", floorIndex = 2, applyToTags = setOf("monster", "bandit", "elite"))

        assertTrue("elite.frostbound" !in withoutEliteTag)
        assertTrue("elite.tidebound" !in withoutEliteTag)
        assertTrue("elite.frostbound" in withEliteTag)
        assertTrue("elite.tidebound" in withEliteTag)
    }

    private fun sampleTierCounts(
        registry: EliteMutationRegistry,
        context: MutationSelectionContext,
        iterations: Int = 240,
    ): Map<MutationTier, Int> {
        var cursor = 0
        val counts = mutableMapOf<MutationTier, Int>().withDefault { 0 }
        repeat(iterations) {
            val selected =
                registry.select(context) { bound ->
                    val roll = cursor % bound
                    cursor += 1
                    roll
                }
            val tier = selected.single().tier
            counts[tier] = counts.getValue(tier) + 1
        }
        return counts
    }

    private fun sampleMutationIds(
        zoneId: String,
        floorIndex: Int,
        applyToTags: Set<String>,
        iterations: Int = 240,
    ): Set<String> {
        var cursor = 0
        val ids = linkedSetOf<String>()
        repeat(iterations) {
            val selected =
                phase4Registry.select(
                    MutationSelectionContext(
                        zoneId = zoneId,
                        floorIndex = floorIndex,
                        applyToTags = applyToTags,
                    ),
                ) { bound ->
                    val roll = cursor % bound
                    cursor += 1
                    roll
                }
            ids += selected.map(EliteMutationDef::id)
        }
        return ids
    }

    private fun testRegistry(): EliteMutationRegistry =
        EliteMutationRegistry(
            config = EliteMutationConfig(),
            statModifiersById = emptyMap(),
            definitionsById =
                listOf(
                    EliteMutationDef(
                        id = "elite.minor.test",
                        kind = MutationKind.STAT_PACKAGE,
                        tier = MutationTier.MINOR,
                        threatCost = 1,
                        nameKey = "mutation.test.minor",
                        iconKey = "icon.mutation.test.minor",
                        applyToTags = setOf("elite"),
                        minFloor = 1,
                        maxFloor = null,
                        allowedZones = emptySet(),
                        preferredTerrainTags = emptyList(),
                        statModifiers = emptyList(),
                        grantedTalents = emptyList(),
                        aiProfileOverlay = null,
                        incompatibleWith = emptySet(),
                    ),
                    EliteMutationDef(
                        id = "elite.major.test",
                        kind = MutationKind.ABILITY_GRANT,
                        tier = MutationTier.MAJOR,
                        threatCost = 2,
                        nameKey = "mutation.test.major",
                        iconKey = "icon.mutation.test.major",
                        applyToTags = setOf("elite"),
                        minFloor = 1,
                        maxFloor = null,
                        allowedZones = emptySet(),
                        preferredTerrainTags = emptyList(),
                        statModifiers = emptyList(),
                        grantedTalents = emptyList(),
                        aiProfileOverlay = null,
                        incompatibleWith = emptySet(),
                    ),
                    EliteMutationDef(
                        id = "elite.signature.test",
                        kind = MutationKind.ELEMENT_PACKAGE,
                        tier = MutationTier.SIGNATURE,
                        threatCost = 3,
                        nameKey = "mutation.test.signature",
                        iconKey = "icon.mutation.test.signature",
                        applyToTags = setOf("elite"),
                        minFloor = 1,
                        maxFloor = null,
                        allowedZones = emptySet(),
                        preferredTerrainTags = emptyList(),
                        statModifiers = emptyList(),
                        grantedTalents = emptyList(),
                        aiProfileOverlay = null,
                        incompatibleWith = emptySet(),
                    ),
                ).associateBy(EliteMutationDef::id),
        )
}

private data class ExpectedMutationContract(
    val id: String,
    val kind: MutationKind,
    val tier: MutationTier,
    val threatCost: Int,
    val minFloor: Int,
    val allowedZones: Set<String>,
    val preferredTerrainTags: List<TerrainTag> = emptyList(),
    val grantedTalents: List<String> = emptyList(),
    val aiProfileOverlay: String? = null,
    val incompatibleWith: Set<String>,
    val auraStatusId: String? = null,
)
