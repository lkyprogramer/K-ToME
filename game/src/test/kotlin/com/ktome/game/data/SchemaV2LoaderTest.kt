package com.ktome.game.data

import com.ktome.core.combat.DamageType
import com.ktome.core.item.EquipmentPassive
import com.ktome.game.i18n.GameLocale
import com.ktome.game.data.schema.AITriggerConditionKindSchemaV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SchemaV2LoaderTest {
    @Test
    fun `schema v2 loader reads full content catalog`() {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()

        assertEquals(setOf("vanguard", "arcanist", "rogue", "templar"), catalog.professions.map { it.id }.toSet())
        assertEquals(12, catalog.talentTrees.size)
        assertTrue(
            catalog.talents.map { it.id }.toSet().containsAll(
                setOf(
                    "power_strike",
                    "charge",
                    "shield_bash",
                    "war_cry",
                    "fireball",
                    "blink",
                    "backstab",
                    "poison_blade",
                    "stealth",
                    "roll",
                    "holy_strike",
                    "holy_light",
                    "holy_shield",
                    "devotion",
                ),
            ),
        )
        assertEquals(setOf("shattered_outpost", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths"), catalog.zones.map { it.id }.toSet())
        assertEquals(
            setOf(
                "armory_gate",
                "supply_crate",
                "alarm_bonfire",
                "trail_cache",
                "warden_beacon",
                "hunter_snare",
                "ore_stash",
                "mine_furnace",
                "slag_valve",
                "seal_cache",
                "ritual_altar",
                "shadow_brazier",
            ),
            catalog.interactables.map { it.id }.toSet(),
        )
        assertEquals(3, catalog.professions.first { it.id == "vanguard" }.combatProfile.baseDefense)
        assertEquals(102, catalog.professions.first { it.id == "arcanist" }.combatProfile.baseSpeed)
        assertEquals(listOf("backstab", "poison_blade", "deathblow"), catalog.talentTrees.first { it.id == "rogue_assassination" }.nodes)
        assertEquals(listOf("holy_light", "holy_shield", "purify"), catalog.talentTrees.first { it.id == "templar_grace" }.nodes)
        assertEquals(3, catalog.talents.first { it.id == "charge" }.unlockLevel)
        assertEquals(4, catalog.talents.first { it.id == "blink" }.levelEffects.getValue(5).rangeBonus)
        assertEquals("audio.talent.shadowstep", catalog.talents.first { it.id == "shadowstep" }.audioProfile)
        assertEquals("icon.skill.templar.holy_shield", catalog.talents.first { it.id == "holy_shield" }.iconKey)
        assertEquals(
            setOf("shattered_outpost_breach", "greenwood_signal_hunt", "deep_iron_pit_forge_run", "grey_gate_seal_rite"),
            catalog.objectiveSets.map { it.id }.toSet(),
        )
        assertEquals(4, catalog.objectiveSets.single { it.id == "shattered_outpost_breach" }.placements.size)
        assertEquals(
            setOf("room_center", "boss_entry", "stairs_up", "player_start"),
            catalog.objectiveSets.single { it.id == "shattered_outpost_breach" }.placements.map { it.anchor }.toSet(),
        )
        assertEquals(setOf("normal"), catalog.difficulties.map { it.id }.toSet())
        assertEquals(24, catalog.monsters.size)
        assertEquals(24, catalog.itemBundle.items.size)
        assertTrue(catalog.itemBundle.items.count { item -> "weapon" in item.tags } >= 6)
        assertTrue(catalog.itemBundle.items.count { item -> "armor" in item.tags && "accessory" !in item.tags } >= 6)
        assertTrue(catalog.itemBundle.items.count { item -> "accessory" in item.tags } >= 4)
        assertTrue(catalog.itemBundle.items.count { item -> "consumable" in item.tags } >= 6)
        assertTrue(catalog.itemBundle.items.count { item -> setOf("reward", "boss_reward", "quest").any(item.tags::contains) } >= 2)
        assertEquals("DamageVsTag", catalog.itemBundle.items.first { item -> item.id == "bandit_trophy" }.passive?.kind)
        assertEquals("ResistanceBonus", catalog.itemBundle.items.first { item -> item.id == "seal_reliquary" }.passive?.kind)
        assertTrue(catalog.visualKeys.contains("actor.vanguard"))
        assertTrue(catalog.visualKeys.contains("talent.arcanist.mana_surge.icon"))
        assertTrue(catalog.visualKeys.contains("icon.skill.rogue.backstab"))
        assertTrue(catalog.visualKeys.contains("icon.skill.templar.divine_intervention"))
        assertTrue(catalog.visualKeys.contains("actor.orc.miner"))
        assertTrue(catalog.visualKeys.contains("actor.cultist.shadow_priest"))
        assertTrue(catalog.visualKeys.contains("prop.mine_furnace"))
        assertTrue(catalog.visualKeys.contains("prop.ritual_altar"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.power_strike"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.mana_surge"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.backstab"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.holy_light"))
        assertTrue(catalog.audioProfiles.contains("audio.monster.default"))
        assertEquals(
            listOf("healing_potion", "short_sword", "leather_armor", "bandit_trophy", "stamina_draught", "hunter_bow"),
            catalog.lootProfiles.first { it.id == "loot.foundation.common" }.itemIds,
        )
        assertEquals(
            listOf("basic_shield", "mana_potion", "chain_mail", "apprentice_robe", "long_sword", "emerald_charm", "furnace_talisman", "energy_tonic"),
            catalog.lootProfiles.first { it.id == "loot.foundation.elite" }.itemIds,
        )
        assertEquals(
            listOf("battle_axe", "plate_armor", "arcane_staff", "scroll_teleport", "mana_potion", "forgebreaker_pick", "sanctified_seal", "seal_reliquary", "shadow_cloak", "consecrated_oil"),
            catalog.lootProfiles.first { it.id == "loot.foundation.boss" }.itemIds,
        )
        val banditCaptainAi = catalog.aiProfiles.first { it.id == "ai.boss.bandit_captain" }
        assertEquals(listOf("power_strike", "shield_bash"), banditCaptainAi.talentPriority)
        assertTrue(banditCaptainAi.skipRules.isEmpty())
        assertEquals(2, banditCaptainAi.triggers.size)
        assertEquals("bandit_captain_opening_shield_bash", banditCaptainAi.triggers.first().triggerId)
        assertEquals(AITriggerConditionKindSchemaV2.ON_COMBAT_START, banditCaptainAi.triggers.first().condition)
        assertTrue(banditCaptainAi.triggers.first().once)
        assertEquals("bandit_captain_enrage_40", banditCaptainAi.triggers.last().triggerId)
        assertEquals(AITriggerConditionKindSchemaV2.HP_BELOW_RATIO, banditCaptainAi.triggers.last().condition)
        assertTrue(banditCaptainAi.triggers.last().once)
        val dungeonLordAi = catalog.aiProfiles.first { it.id == "ai.boss.dungeon_lord" }
        assertEquals("log.boss.desperate", dungeonLordAi.triggers.last().postMessageKey)
        assertEquals("monster.cultist.dungeon_lord.name", dungeonLordAi.triggers.last().postMessageArgs.getValue("source"))
        assertTrue(catalog.arenas.any { it.id == "arena.shattered_outpost.boss" })
    }

    @Test
    fun `runtime projections follow active locale`() {
        val enLoader = DataLoader(GameLocale.EN_US)
        val zhLoader = DataLoader(GameLocale.ZH_CN)

        assertEquals("Rat", enLoader.loadMonsterCatalog().monsters.first { it.id == "beast.rat" }.name)
        assertEquals("老鼠", zhLoader.loadMonsterCatalog().monsters.first { it.id == "beast.rat" }.name)
        assertEquals("Short Sword", enLoader.loadItemBundle().baseItems.first { it.id == "short_sword" }.name)
        assertEquals("短剑", zhLoader.loadItemBundle().baseItems.first { it.id == "short_sword" }.name)
        assertEquals("Power Strike", enLoader.loadTalentDefinitions().first { it.id == "power_strike" }.name)
        assertEquals("猛击", zhLoader.loadTalentDefinitions().first { it.id == "power_strike" }.name)
        assertEquals("Fireball", enLoader.loadTalentDefinitions().first { it.id == "fireball" }.name)
        assertEquals("火球", zhLoader.loadTalentDefinitions().first { it.id == "fireball" }.name)
        assertEquals(DamageType.FIRE, enLoader.loadTalentDefinitions().first { it.id == "fireball" }.damageType)
        assertTrue(enLoader.loadItemBundle().baseItems.first { it.id == "emerald_charm" }.passive is EquipmentPassive.HpRegenPerTurn)
        assertEquals(4, enLoader.loadTalentDefinitions().first { it.id == "blink" }.levelEffects.getValue(5).rangeBonus)
    }

    @Test
    fun `runtime monster projection keeps schema v2 contract fields available`() {
        val boss =
            DataLoader(GameLocale.EN_US)
                .loadMonsterCatalog()
                .monsters
                .first { monster -> monster.id == "cultist.dungeon_lord" }

        assertEquals("boss", boss.archetype)
        assertEquals("actor.cultist.dungeon_lord", boss.visualKey)
        assertEquals("icon.monster.cultist.dungeon_lord", boss.iconKey)
        assertEquals("audio.monster.cultist.dungeon_lord", boss.audioProfile)
        assertEquals("ai.boss.dungeon_lord", boss.aiProfileId)
        assertEquals("loot.foundation.boss", boss.lootProfileId)
        assertEquals(mapOf("war_cry" to 3, "power_strike" to 4, "charge" to 2), boss.talentLevels)
        val huntmaster =
            DataLoader(GameLocale.EN_US)
                .loadMonsterCatalog()
                .monsters
                .first { monster -> monster.id == "bandit.wild_huntmaster" }
        assertEquals(mapOf("power_strike" to 2), huntmaster.talentLevels)
    }

    @Test
    fun `bandit captain slice keeps dedicated boss routing ids`() {
        val boss =
            DataLoader(GameLocale.EN_US)
                .loadMonsterCatalog()
                .monsters
                .first { monster -> monster.id == "bandit.captain" }
        val encounter =
            DataLoader(GameLocale.EN_US)
                .loadSchemaCatalog()
                .bossEncounters
                .first { schema -> schema.id == "bandit_captain_encounter" }

        assertEquals("ai.boss.bandit_captain", boss.aiProfileId)
        assertEquals("arena.shattered_outpost.boss", encounter.arenaId)
    }
}
