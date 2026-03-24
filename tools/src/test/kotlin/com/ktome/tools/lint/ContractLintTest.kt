package com.ktome.tools.lint

import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.core.talent.KeywordRegistry
import com.ktome.game.data.DataLoader
import com.ktome.game.i18n.GameLocale
import com.ktome.game.telegraph.FoundationTelegraphRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("contractLint")
class ContractLintTest {
    @Test
    fun `schema v2 contracts resolve all mandatory cross references`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val assets = ClientAssetBundleLoader.load()
        val professionIds = catalog.professions.map { it.id }.toSet()
        val talentIds = catalog.talents.map { it.id }.toSet()
        val treeIds = catalog.talentTrees.map { it.id }.toSet()
        val monsterIds = catalog.monsters.map { it.id }.toSet()
        val bossIds = catalog.bossEncounters.map { it.id }.toSet()
        val interactableIds = catalog.interactables.map { it.id }.toSet()
        val objectiveIds = catalog.objectiveSets.map { it.id }.toSet()
        val itemIds = catalog.itemBundle.items.map { it.id }.toSet()
        val lootIds = catalog.lootProfiles.map { it.id }.toSet()
        val tilesetIds = catalog.tilesets.map { it.id }.toSet()
        val aiIds = catalog.aiProfiles.map { it.id }.toSet()
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
            assertEquals(2, lootProfile.schemaVersion)
            lootProfile.itemIds.forEach { itemId ->
                assertTrue(itemId in itemIds, "Unknown loot profile item $itemId")
            }
        }

        catalog.professions.forEach { profession ->
            assertEquals(2, profession.schemaVersion)
            assertEquals("profession.${profession.id}.name", profession.nameKey)
            assertEquals("profession.${profession.id}.desc", profession.descKey)
            assertExactVisualKey(profession.visualKey)
            assertExactVisualKey(profession.iconKey)
            assertExactAudioKey(profession.audioProfile)
            profession.talentTrees.forEach { treeId -> assertTrue(treeIds.contains(treeId), "Unknown profession tree $treeId") }
            profession.startingTalents.forEach { talentId -> assertTrue(talentIds.contains(talentId), "Unknown starter talent $talentId") }
            profession.startingKit.forEach { itemId -> assertTrue(itemIds.contains(itemId), "Unknown starter item $itemId") }
        }

        catalog.talentTrees.forEach { tree ->
            assertEquals(2, tree.schemaVersion)
            if (tree.raceId == null) {
                assertTrue(professionIds.contains(tree.professionId), "Unknown talent tree profession ${tree.professionId}")
            } else {
                assertTrue(tree.professionId.isBlank(), "Race tree ${tree.id} must not also reference profession ${tree.professionId}")
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
            assertEquals(2, talent.schemaVersion)
            assertTrue(treeIds.contains(talent.treeId), "Unknown talent tree ${talent.treeId}")
            val tree = requireNotNull(treeById[talent.treeId]) { "Unknown tree ${talent.treeId}" }
            val namespace = ownerNamespace(tree)
            assertEquals("talent.$namespace.${talent.id}.name", talent.nameKey)
            assertEquals("talent.$namespace.${talent.id}.desc", talent.descKey)
            assertExactVisualKey(talent.visualKey)
            assertExactVisualKey(talent.iconKey)
            assertExactAudioKey(talent.audioProfile)
            assertTrue(talent.castTime in setOf("INSTANT", "QUICK", "STANDARD", "HEAVY"), "Unsupported castTime ${talent.castTime}")
            KeywordRegistry.CORE.resolveAll(talent.keywords)
            talent.telegraphRef?.let(FoundationTelegraphRegistry.CORE::require)
            talent.requirements.talentPrereqs.forEach { prereq ->
                assertTrue(talentIds.contains(prereq.talentId), "Unknown talent prerequisite ${prereq.talentId}")
            }
        }

        catalog.aiProfiles.forEach { aiProfile ->
            assertEquals(2, aiProfile.schemaVersion)
            aiProfile.talentPriority.forEach { talentId ->
                assertTrue(talentIds.contains(talentId), "Unknown AI profile talent $talentId")
            }
            aiProfile.skipRules.forEach { rule ->
                assertTrue(rule.talentId in aiProfile.talentPriority, "AI profile ${aiProfile.id} skip rule must reference configured talent ${rule.talentId}")
            }
        }

        catalog.monsters.forEach { monster ->
            assertEquals(2, monster.schemaVersion)
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
            assertEquals("boss.${boss.bossTemplateId}.name", boss.nameKey)
            assertEquals("boss.${boss.bossTemplateId}.desc", boss.descKey)
            assertExactVisualKey(boss.visualKey)
            assertExactVisualKey(boss.iconKey)
            assertExactAudioKey(boss.audioProfile)
            assertTrue(monsterIds.contains(boss.bossTemplateId), "Unknown boss template ${boss.bossTemplateId}")
            assertTrue(arenaIds.contains(boss.arenaId), "Unknown arena ${boss.arenaId}")
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
            assertTrue(objective.interactables.isNotEmpty(), "Objective ${objective.id} must reference at least one interactable.")
            assertTrue(objective.placements.isNotEmpty(), "Objective ${objective.id} must define interactable placements.")
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
            assertEquals("item.${item.id}.name", item.nameKey)
            assertEquals("item.${item.id}.desc", item.descKey)
            assertExactVisualKey(item.visualKey)
            assertExactVisualKey(item.iconKey)
            assertExactAudioKey(item.audioProfile)
            item.materials.forEach { materialId -> assertTrue(catalog.itemBundle.materials.any { it.id == materialId }, "Unknown material $materialId") }
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

        assertEquals(setOf("vanguard", "arcanist", "rogue", "templar"), catalog.professions.map { it.id }.toSet())
        assertEquals(setOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths"), catalog.zones.map { it.id }.toSet())
        assertEquals(
            mapOf(
                "shattered_outpost" to 2,
                "greenwood_fringe" to 2,
                "deep_iron_pit" to 2,
                "grey_gate_depths" to 2,
            ),
            catalog.zones.associate { zone -> zone.id to zone.floorCount },
        )
        assertEquals(
            mapOf(
                "shattered_outpost" to "bandit_captain_encounter",
                "greenwood_fringe" to null,
                "deep_iron_pit" to null,
                "grey_gate_depths" to "dungeon_lord_encounter",
            ),
            catalog.zones.associate { zone -> zone.id to zone.bossEncounterId },
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
