package com.ktome.tools.lint

import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.core.ai.AIActionType
import com.ktome.core.talent.KeywordRegistry
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.data.DataLoader
import com.ktome.game.i18n.GameLocale
import com.ktome.game.loot.LootProfileCandidatePoolResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("contractLint")
class ContractLintTest {
    private val phase2RouteZoneIds: List<String> =
        listOf(
            "shattered_outpost",
            "greenwood_fringe",
            "deep_iron_pit",
            "grey_gate_depths",
        )

    @Test
    fun `schema v2 contracts resolve all mandatory cross references`() {
        val loader = DataLoader()
        val catalog = loader.loadSchemaCatalog()
        val assets = ClientAssetBundleLoader.load()
        val professionIds = catalog.professions.map { it.id }.toSet()
        val advancedProfessionIds = setOf("berserker", "spellblade", "shadowblade", "warden")
        val talentIds = catalog.talents.map { it.id }.toSet()
        val treeIds = catalog.talentTrees.map { it.id }.toSet()
        val raceIds = catalog.races.map { it.id }.toSet()
        val monsterIds = catalog.monsters.map { it.id }.toSet()
        val bossIds = catalog.bossEncounters.map { it.id }.toSet()
        val interactableIds = catalog.interactables.map { it.id }.toSet()
        val objectiveIds = catalog.objectiveSets.map { it.id }.toSet()
        val itemIds = catalog.itemBundle.items.map { it.id }.toSet()
        val runtimeItemBundle = loader.loadItemBundle()
        val lootProfileResolver = LootProfileCandidatePoolResolver(runtimeItemBundle)
        val inscriptionIds = catalog.inscriptions.map { it.id }
        val uniqueInscriptionIds = inscriptionIds.toSet()
        val lootIds = catalog.lootProfiles.map { it.id }.toSet()
        val statusIds = catalog.statuses.map { it.id }.toSet()
        val mutationModifierIds = catalog.mutationStatModifiers.map { it.id }.toSet()
        val mutationIds = catalog.eliteMutations.map { it.id }.toSet()
        val actionWeightProfileIds = catalog.actionWeightProfiles.map { it.id }.toSet()
        val tilesetIds = catalog.tilesets.map { it.id }.toSet()
        val aiIds = catalog.aiProfiles.map { it.id }.toSet()
        val telegraphIds = catalog.telegraphSpecs.map { it.id }.toSet()
        val arenaIds = catalog.arenas.map { it.id }.toSet()
        val ambientIds = catalog.ambientProfiles.map { it.id }.toSet()
        fun ownerNamespace(tree: com.ktome.game.data.schema.TalentTreeSchemaV2): String = tree.raceId ?: tree.professionId
        fun assertExactVisualKey(key: String) {
            val resolved = assets.visualResolver.resolve(key)
            assertFalse(resolved.fallbackUsed, "Unknown visual key $key")
            assertFalse(resolved.matchedByPrefix, "Visual key $key resolved via prefix fallback")
        }
        fun assertExactAudioKey(key: String) {
            val resolved = assets.audioResolver.resolve(key)
            assertFalse(resolved.fallbackUsed, "Unknown audio profile $key")
            assertFalse(resolved.matchedByPrefix, "Audio profile $key resolved via prefix fallback")
        }

        catalog.lootProfiles.forEach { lootProfile ->
            assertEquals(3, lootProfile.schemaVersion)
            lootProfile.itemIds.forEach { itemId ->
                assertTrue(itemId in itemIds, "Unknown loot profile item $itemId")
            }
            val candidatePool = lootProfileResolver.resolve(lootProfile)
            assertTrue(candidatePool.allCandidateBaseIds.isNotEmpty(), "Loot profile ${lootProfile.id} resolved empty allCandidateBaseIds")
            assertTrue(candidatePool.standardCandidateBaseIds.isNotEmpty(), "Loot profile ${lootProfile.id} resolved empty standardCandidateBaseIds")
            assertTrue(candidatePool.allCandidateBaseIds.containsAll(candidatePool.standardCandidateBaseIds))
        }

        catalog.professions.forEach { profession ->
            assertEquals(3, profession.schemaVersion)
            assertEquals("profession.${profession.id}.name", profession.nameKey)
            assertEquals("profession.${profession.id}.desc", profession.descKey)
            assertEquals("profession.${profession.id}.resource_hint", profession.resourceHintKey)
            assertExactVisualKey(profession.visualKey)
            assertExactVisualKey(profession.iconKey)
            assertExactAudioKey(profession.audioProfile)
            profession.talentTrees.forEach { treeId -> assertTrue(treeIds.contains(treeId), "Unknown profession tree $treeId") }
            profession.startingTalents.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown starter talent $talentId") }
            profession.startingKit.forEach { itemId -> assertTrue(itemIds.contains(itemId), "Unknown starter item $itemId") }
        }

        catalog.talentTrees.forEach { tree ->
            assertTrue(tree.schemaVersion in setOf(2, 3), "Unsupported talent tree schemaVersion ${tree.schemaVersion} for ${tree.id}")
            if (tree.raceId == null) {
                assertTrue(professionIds.contains(tree.professionId), "Unknown talent tree profession ${tree.professionId}")
                if (tree.professionId in advancedProfessionIds) {
                    assertEquals(3, tree.schemaVersion, "Advanced profession tree ${tree.id} must use schemaVersion 3.")
                }
            } else {
                assertTrue(tree.professionId.isBlank(), "Race tree ${tree.id} must not also reference profession ${tree.professionId}")
                assertTrue(raceIds.contains(tree.raceId), "Unknown race talent tree owner ${tree.raceId}")
                assertEquals(3, tree.schemaVersion, "Race tree ${tree.id} must use schemaVersion 3.")
            }
            assertEquals("talent_tree.${tree.id}.name", tree.nameKey)
            assertEquals("talent_tree.${tree.id}.desc", tree.descKey)
            assertExactVisualKey(tree.visualKey)
            assertExactVisualKey(tree.iconKey)
            assertExactAudioKey(tree.audioProfile)
            tree.nodes.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown tree node $talentId") }
        }

        val treeById = catalog.talentTrees.associateBy { it.id }
        catalog.talents.forEach { talent ->
            assertTrue(talent.schemaVersion in setOf(2, 3), "Unsupported talent schemaVersion ${talent.schemaVersion} for ${talent.id}")
            assertTrue(treeIds.contains(talent.treeId), "Unknown talent tree ${talent.treeId}")
            val tree = requireNotNull(treeById[talent.treeId]) { "Unknown tree ${talent.treeId}" }
            val namespace = ownerNamespace(tree)
            val expectedKeyBase =
                tree.raceId?.let { raceId ->
                    val leafId = talent.id.removePrefix("${raceId}_")
                    "talent.race.$raceId.$leafId"
                } ?: "talent.$namespace.${talent.id}"
            assertEquals("$expectedKeyBase.name", talent.nameKey)
            assertEquals("$expectedKeyBase.desc", talent.descKey)
            assertExactVisualKey(talent.visualKey)
            assertExactVisualKey(talent.iconKey)
            assertExactAudioKey(talent.audioProfile)
            assertTrue(talent.castTime in setOf("INSTANT", "QUICK", "STANDARD", "HEAVY"), "Unsupported castTime ${talent.castTime}")
            KeywordRegistry.CORE.resolveAll(talent.keywords)
            talent.telegraphRef?.let { telegraphRef -> assertTrue(telegraphRef in telegraphIds, "Unknown telegraph $telegraphRef") }
            talent.requirements.talentPrereqs.forEach { prereq ->
                assertTrue(talentIds.contains(prereq.talentId), "Unknown talent prerequisite ${prereq.talentId}")
            }
        }

        catalog.races.forEach { race ->
            assertEquals(1, race.schemaVersion)
            assertEquals("race.${race.id}.name", race.nameKey)
            assertEquals("race.${race.id}.desc", race.descKey)
            assertExactVisualKey(race.visualKey)
            assertExactVisualKey(race.iconKey)
            assertExactAudioKey(race.audioProfile)
            race.talentTrees.forEach { treeId -> assertTrue(treeId in treeIds, "Unknown race tree $treeId") }
            race.startingTalents.forEach { talentId -> assertTrue(talentId in talentIds, "Unknown race talent $talentId") }
        }

        catalog.inscriptions.forEach { inscription ->
            assertEquals("inscription.${inscription.id}.name", inscription.nameKey)
            assertEquals("inscription.${inscription.id}.desc", inscription.descKey)
            assertExactVisualKey(inscription.iconKey)
            assertTrue(inscription.cooldown >= 0, "Inscription ${inscription.id} must not have negative cooldown.")
        }
        assertEquals(
            inscriptionIds.size,
            uniqueInscriptionIds.size,
            "Inscription ids must stay unique.",
        )

        catalog.aiProfiles.forEach { aiProfile ->
            assertTrue(aiProfile.perceptionRange > 0, "AI profile ${aiProfile.id} must declare positive perceptionRange")
            assertTrue(aiProfile.actions.isNotEmpty(), "AI profile ${aiProfile.id} must declare at least one action")
            aiProfile.actions.forEach { action ->
                if (action.type == AIActionType.USE_ABILITY) {
                    assertTrue(action.abilityId in talentIds, "Unknown AI ability ${action.abilityId} in profile ${aiProfile.id}")
                }
            }
        }

        assertEquals(2, catalog.eliteMutationConfig.maxMutationsPerElite, "Elite mutation cap must stay frozen at 2.")
        catalog.eliteMutations.forEach { mutation ->
            assertTrue(mutation.id.startsWith("elite."), "Elite mutation id must use elite.* namespace: ${mutation.id}")
            val mutationLeafId = mutation.id.removePrefix("elite.")
            assertEquals("mutation.$mutationLeafId.name", mutation.nameKey)
            assertExactVisualKey(mutation.iconKey)
            assertExactAudioKey("audio.mutation.$mutationLeafId")
            assertTrue(mutation.applyToTags.isNotEmpty(), "Elite mutation ${mutation.id} must target at least one tag.")
            mutation.statModifiers.forEach { ref ->
                assertTrue(ref.modifierId in mutationModifierIds, "Unknown mutation stat modifier ${ref.modifierId}")
            }
            mutation.grantedTalents.forEach { ref ->
                assertTrue(ref.talentId in talentIds, "Unknown mutation granted talent ${ref.talentId}")
            }
            mutation.aiProfileOverlay?.let { overlayId ->
                assertTrue(overlayId in aiIds, "Unknown mutation AI overlay $overlayId")
            }
            mutation.incompatibleWith.forEach { incompatibleId ->
                assertTrue(incompatibleId in mutationIds, "Unknown incompatible mutation $incompatibleId")
            }
            mutation.auraStatusId?.let { auraStatusId ->
                assertTrue(auraStatusId in statusIds, "Unknown mutation aura status $auraStatusId")
            }
        }
        catalog.bossVariants.forEach { variant ->
            assertTrue(variant.id.startsWith("boss.variant."), "Boss variant id must use boss.variant.* namespace: ${variant.id}")
            assertTrue(variant.baseEncounterId in bossIds, "Unknown boss variant base encounter ${variant.baseEncounterId}")
            val variantLeafId = variant.id.removePrefix("boss.variant.")
            assertExactAudioKey("audio.boss.variant.$variantLeafId")
            variant.grantedMutations.forEach { ref ->
                assertTrue(ref.mutationId in mutationIds, "Unknown boss variant mutation ${ref.mutationId}")
            }
            variant.lootProfileOverride?.let { lootProfileId ->
                assertTrue(lootProfileId in lootIds, "Unknown boss variant loot override $lootProfileId")
            }
            variant.visualTintKey?.let(::assertExactVisualKey)
            variant.actionWeightProfileId?.let { profileId ->
                assertTrue(profileId in actionWeightProfileIds, "Unknown boss variant action weight profile $profileId")
            }
        }

        ThreatProfileLint.validate(catalog)

        catalog.monsters.forEach { monster ->
            assertTrue(monster.schemaVersion in setOf(2, 3), "Unsupported monster schemaVersion ${monster.schemaVersion} for ${monster.id}")
            assertTrue(monster.id.contains('.'), "Monster id must use family namespace: ${monster.id}")
            assertTrue(monster.nameKey.startsWith("monster.${monster.id}."), "Monster key must follow monster.<family>.<id> namespace: ${monster.nameKey}")
            assertExactVisualKey(monster.visualKey)
            assertExactVisualKey(monster.iconKey)
            assertExactAudioKey(monster.audioProfile)
            assertTrue(aiIds.contains(monster.aiProfileId), "Unknown AI profile ${monster.aiProfileId}")
            assertTrue(lootIds.contains(monster.lootProfileId), "Unknown loot profile ${monster.lootProfileId}")
            monster.talents.keys.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown monster talent $talentId") }
        }

        catalog.bossEncounters.forEach { boss ->
            assertEquals(2, boss.schemaVersion)
            assertEquals("boss.${boss.templateId}.name", boss.nameKey)
            assertEquals("boss.${boss.templateId}.desc", boss.descKey)
            assertExactVisualKey(boss.visualKey)
            assertExactVisualKey(boss.iconKey)
            assertExactAudioKey(boss.audioProfile)
            assertTrue(monsterIds.contains(boss.templateId), "Unknown boss template ${boss.templateId}")
            assertTrue(arenaIds.contains(boss.arenaId), "Unknown arena ${boss.arenaId}")
            assertTrue(boss.phases.isNotEmpty(), "Boss ${boss.id} must declare at least one phase")
            boss.phases.forEach { phase ->
                assertTrue(aiIds.contains(phase.aiProfileId), "Unknown boss phase AI profile ${phase.aiProfileId}")
                phase.onEnter.forEach { event ->
                    event.telegraphSpecId?.let { telegraphId ->
                        assertTrue(telegraphId in telegraphIds, "Unknown boss telegraph $telegraphId")
                    }
                }
            }
            boss.rewards.forEach { rewardId -> assertTrue(lootIds.contains(rewardId), "Unknown boss reward $rewardId") }
        }

        catalog.zones.forEach { zone ->
            assertEquals(2, zone.schemaVersion)
            assertEquals("zone.${zone.id}.name", zone.nameKey)
            assertEquals("zone.${zone.id}.desc", zone.descKey)
            assertTrue(zone.floorCount > 0, "Zone floorCount must stay positive for ${zone.id}")
            assertTrue(zone.mapSize.width > 0 && zone.mapSize.height > 0, "Zone mapSize must stay positive for ${zone.id}")
            assertExactVisualKey(zone.visualKey)
            assertExactVisualKey(zone.iconKey)
            assertExactAudioKey(zone.audioProfile)
            assertExactAudioKey(zone.ambientProfile)
            assertTrue(tilesetIds.contains(zone.tilesetKey), "Unknown tileset ${zone.tilesetKey}")
            assertTrue(ambientIds.contains(zone.ambientProfile), "Unknown ambient profile ${zone.ambientProfile}")
            zone.monsterPools.forEach { monsterId -> assertTrue(monsterIds.contains(monsterId), "Unknown zone monster $monsterId") }
            zone.elitePools.forEach { monsterId -> assertTrue(monsterIds.contains(monsterId), "Unknown zone elite $monsterId") }
            zone.bossEncounterId?.let { bossId -> assertTrue(bossIds.contains(bossId), "Unknown boss encounter $bossId") }
            zone.objectiveSetId?.let { objectiveId -> assertTrue(objectiveIds.contains(objectiveId), "Unknown objective set $objectiveId") }
        }

        catalog.interactables.forEach { interactable ->
            assertEquals(2, interactable.schemaVersion)
            assertEquals("interactable.${interactable.id}.name", interactable.nameKey)
            assertEquals("interactable.${interactable.id}.desc", interactable.descKey)
            assertExactVisualKey(interactable.visualKey)
            assertExactAudioKey(interactable.audioProfile)
            assertTrue(interactable.interactionTags.isNotEmpty(), "Interactable ${interactable.id} must expose interaction tags.")
        }

        catalog.objectiveSets.forEach { objective ->
            assertEquals(2, objective.schemaVersion)
            assertEquals("objective.${objective.id}.name", objective.nameKey)
            assertEquals("objective.${objective.id}.desc", objective.descKey)
            assertEquals(
                objective.interactables.isEmpty(),
                objective.placements.isEmpty(),
                "Objective ${objective.id} must not declare empty interactables with non-empty placements, or vice versa.",
            )
            objective.interactables.forEach { interactableId ->
                assertTrue(interactableIds.contains(interactableId), "Unknown objective interactable $interactableId")
            }
            objective.placements.forEach { placement ->
                assertTrue(interactableIds.contains(placement.interactableId), "Unknown objective placement interactable ${placement.interactableId}")
                assertTrue(placement.interactableId in objective.interactables, "Placement ${placement.interactableId} must be declared in objective ${objective.id}.")
                assertTrue(placement.floor > 0, "Objective placement floor must stay positive for ${objective.id}.")
                assertTrue(
                    placement.anchor in setOf("player_start", "stairs_up", "stairs_down", "room_center", "boss_entry"),
                    "Unsupported objective placement anchor ${placement.anchor}",
                )
            }
        }

        catalog.zones.forEach { zone ->
            zone.objectiveSetId?.let { objectiveId ->
                val objective = requireNotNull(catalog.objectiveSets.firstOrNull { it.id == objectiveId }) { "Unknown objective $objectiveId" }
                objective.placements.forEach { placement ->
                    assertTrue(placement.floor in 1..zone.floorCount, "Objective placement floor ${placement.floor} exceeds zone ${zone.id} floorCount ${zone.floorCount}.")
                }
            }
        }

        catalog.itemBundle.materials.forEach { material ->
            assertEquals(2, material.schemaVersion)
            assertEquals("material.${material.id.lowercase()}.name", material.nameKey)
            assertEquals("material.${material.id.lowercase()}.desc", material.descKey)
            assertExactVisualKey(material.visualKey)
            assertExactVisualKey(material.iconKey)
            assertExactAudioKey(material.audioProfile)
        }
        catalog.itemBundle.affixes.forEach { affix ->
            assertEquals(2, affix.schemaVersion)
            assertEquals("affix.${affix.id}.name", affix.nameKey)
            assertEquals("affix.${affix.id}.desc", affix.descKey)
            assertExactVisualKey(affix.visualKey)
            assertExactVisualKey(affix.iconKey)
            assertExactAudioKey(affix.audioProfile)
        }
        catalog.itemBundle.items.forEach { item ->
            assertEquals(2, item.schemaVersion)
            val expectedItemKeyBase =
                when {
                    item.id.startsWith("unique_") -> "item.unique.${item.id.removePrefix("unique_")}"
                    item.id.startsWith("artifact_") -> "item.artifact.${item.id.removePrefix("artifact_")}"
                    else -> "item.${item.id}"
                }
            assertEquals("$expectedItemKeyBase.name", item.nameKey)
            assertEquals("$expectedItemKeyBase.desc", item.descKey)
            assertExactVisualKey(item.visualKey)
            assertExactVisualKey(item.iconKey)
            assertExactAudioKey(item.audioProfile)
            item.materials.forEach { materialId -> assertTrue(catalog.itemBundle.materials.any { it.id == materialId }, "Unknown material $materialId") }
        }
        (catalog.itemBundle.uniqueTemplates + catalog.itemBundle.artifactTemplates).forEach { template ->
            val item = catalog.itemBundle.items.firstOrNull { candidate -> candidate.id == template.itemId }
            assertTrue(item != null, "Special template ${template.id} must resolve to a real item ${template.itemId}")
            assertExactVisualKey(template.visualKey)
            assertExactVisualKey(template.iconKey)
            assertExactAudioKey(template.audioProfile)
            assertEquals(2, template.schemaVersion)
            item?.let { resolvedItem ->
                assertEquals(resolvedItem.nameKey, template.nameKey)
                assertEquals(resolvedItem.descKey, template.descKey)
                assertEquals(resolvedItem.visualKey, template.visualKey)
                assertEquals(resolvedItem.iconKey, template.iconKey)
                assertEquals(resolvedItem.audioProfile, template.audioProfile)
            }
        }

        catalog.difficulties.forEach { difficulty ->
            assertEquals(2, difficulty.schemaVersion)
            assertEquals("difficulty.${difficulty.id}.name", difficulty.nameKey)
            assertEquals("difficulty.${difficulty.id}.desc", difficulty.descKey)
            assertExactVisualKey(difficulty.visualKey)
            assertExactVisualKey(difficulty.iconKey)
            assertExactAudioKey(difficulty.audioProfile)
        }
    }

    @Test
    fun `phase2 skeleton ids and family namespaces stay frozen`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val zoneById = catalog.zones.associateBy { it.id }
        val frozenRouteZones = phase2RouteZoneIds.associateWith { zoneId -> requireNotNull(zoneById[zoneId]) { "Missing frozen route zone $zoneId." } }

        assertEquals(
            setOf("vanguard", "arcanist", "rogue", "templar", "berserker", "spellblade", "shadowblade", "warden"),
            catalog.professions.map { it.id }.toSet(),
        )
        assertTrue(catalog.zones.map { it.id }.containsAll(FOUNDATION_ZONE_ROUTE), "Foundation route zones must all remain present.")
        assertEquals(
            phase2RouteZoneIds,
            catalog.zones.map { it.id }.filter(phase2RouteZoneIds::contains),
            "Phase 2 route zones must remain in frozen order within the expanded Phase 3 catalog.",
        )
        assertEquals(
            mapOf(
                "shattered_outpost" to 2,
                "greenwood_fringe" to 2,
                "deep_iron_pit" to 2,
                "grey_gate_depths" to 2,
            ),
            frozenRouteZones.mapValues { (_, zone) -> zone.floorCount },
        )
        assertEquals(
            mapOf(
                "shattered_outpost" to "bandit_captain_encounter",
                "greenwood_fringe" to null,
                "deep_iron_pit" to "molten_giant_encounter",
                "grey_gate_depths" to "dungeon_lord_encounter",
            ),
            frozenRouteZones.mapValues { (_, zone) -> zone.bossEncounterId },
        )
        assertEquals(setOf("normal"), catalog.difficulties.map { it.id }.toSet())
        assertTrue(
            catalog.monsters.map { it.id }.toSet().containsAll(
                setOf(
                    "beast.rat",
                    "beast.rat_scavenger",
                    "goblin.scout",
                    "bandit.raider",
                    "bandit.archer",
                    "bandit.captain",
                    "undead.restless_skeleton",
                    "undead.bone_guard",
                    "undead.bone_archer",
                    "bandit.sentry",
                    "orc.raider",
                    "cultist.dungeon_lord",
                ),
            ),
        )

        val uniqueNameKeys =
            buildList {
                addAll(catalog.professions.map { it.nameKey })
                addAll(catalog.races.map { it.nameKey })
                addAll(catalog.inscriptions.map { it.nameKey })
                addAll(catalog.talentTrees.map { it.nameKey })
                addAll(catalog.talents.map { it.nameKey })
                addAll(catalog.monsters.map { it.nameKey })
                addAll(catalog.bossEncounters.map { it.nameKey })
                addAll(catalog.zones.map { it.nameKey })
                addAll(catalog.interactables.map { it.nameKey })
                addAll(catalog.objectiveSets.map { it.nameKey })
                addAll(catalog.difficulties.map { it.nameKey })
                addAll(catalog.itemBundle.materials.map { it.nameKey })
                addAll(catalog.itemBundle.affixes.map { it.nameKey })
                addAll(catalog.itemBundle.items.map { it.nameKey })
            }
        assertEquals(uniqueNameKeys.size, uniqueNameKeys.toSet().size, "nameKey values must stay unique.")

        val uniqueIds =
            buildList {
                addAll(catalog.professions.map { "profession:${it.id}" })
                addAll(catalog.races.map { "race:${it.id}" })
                addAll(catalog.inscriptions.map { "inscription:${it.id}" })
                addAll(catalog.talentTrees.map { "talent_tree:${it.id}" })
                addAll(catalog.talents.map { "talent:${it.id}" })
                addAll(catalog.monsters.map { "monster:${it.id}" })
                addAll(catalog.bossEncounters.map { "boss:${it.id}" })
                addAll(catalog.zones.map { "zone:${it.id}" })
                addAll(catalog.interactables.map { "interactable:${it.id}" })
                addAll(catalog.objectiveSets.map { "objective:${it.id}" })
                addAll(catalog.difficulties.map { "difficulty:${it.id}" })
                addAll(catalog.itemBundle.materials.map { "material:${it.id}" })
                addAll(catalog.itemBundle.affixes.map { "affix:${it.id}" })
                addAll(catalog.itemBundle.items.map { "item:${it.id}" })
            }
        assertEquals(uniqueIds.size, uniqueIds.toSet().size, "Object ids must stay unique inside their namespaces.")
    }

    @Test
    fun `locale keyword markup stays aligned with keyword registry`() {
        GameLocale.entries.forEach { locale ->
            LintFixtures.localeKeywordMarkupIds(locale).forEach { keywordId ->
                assertTrue(
                    KeywordRegistry.CORE.resolve(keywordId) != null,
                    "Locale ${locale.id} references unknown keyword markup [[$keywordId]].",
                )
            }
        }
    }

    @Test
    fun `stage e formalized namespaces do not resolve through prefix fallback`() {
        val assets = ClientAssetBundleLoader.load()

        listOf(
            "affix.__stage_e_probe__.icon",
            "affix.__stage_e_probe__.visual",
            "material.__stage_e_probe__.icon",
            "material.__stage_e_probe__.visual",
            "difficulty.__stage_e_probe__.icon",
            "difficulty.__stage_e_probe__.visual",
        ).forEach { key ->
            val resolved = assets.visualResolver.resolve(key)
            assertTrue(resolved.fallbackUsed, "Unknown visual key $key must fall back directly to the sentinel entry.")
            assertFalse(resolved.matchedByPrefix, "Unknown visual key $key must not resolve through prefix fallback.")
        }

        listOf(
            "audio.affix.__stage_e_probe__",
            "audio.material.__stage_e_probe__",
            "audio.difficulty.__stage_e_probe__",
        ).forEach { key ->
            val resolved = assets.audioResolver.resolve(key)
            assertTrue(resolved.fallbackUsed, "Unknown audio key $key must fall back directly to the sentinel entry.")
            assertFalse(resolved.matchedByPrefix, "Unknown audio key $key must not resolve through prefix fallback.")
        }
    }
}
