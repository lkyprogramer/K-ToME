package com.ktome.game.data

import com.ktome.core.talent.ActionCost
import com.ktome.core.talent.EffectOp
import com.ktome.core.combat.ApplicationPolicy
import com.ktome.core.combat.DamageType
import com.ktome.core.combat.SaveDimension
import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.loot.SourceTier
import com.ktome.core.random.RandomSource
import com.ktome.core.resource.ResourceType
import com.ktome.core.status.StatusEffectType
import com.ktome.core.ai.AISelectionPolicy
import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentRole
import com.ktome.game.data.schema.LootPoolStrategy
import com.ktome.game.i18n.GameLocale
import com.ktome.game.data.schema.TalentSchemaV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException
import kotlin.random.Random

class SchemaV2LoaderTest {
    @Test
    fun `schema v2 loader reads full content catalog`() {
        val loader = DataLoader(GameLocale.EN_US)
        val catalog = loader.loadSchemaCatalog()
        val runtimeTalentsById = loader.loadTalentDefinitions().associateBy(TalentDef::id)

        assertEquals(
            setOf("vanguard", "arcanist", "rogue", "templar", "berserker", "spellblade", "shadowblade", "warden"),
            catalog.professions.map { it.id }.toSet(),
        )
        assertEquals(29, catalog.talentTrees.size)
        assertTrue(
            catalog.talents.map { it.id }.toSet().containsAll(
                setOf(
                    "power_strike",
                    "charge",
                    "linebreaker",
                    "shield_bash",
                    "war_cry",
                    "fireball",
                    "void_breach",
                    "blink",
                    "backstab",
                    "shadow_bind",
                    "poison_blade",
                    "stealth",
                    "roll",
                    "holy_strike",
                    "consecration",
                    "holy_light",
                    "holy_shield",
                    "devotion",
                    "blood_rush",
                    "riven_edge",
                    "pursuit_drive",
                    "fault_line",
                    "aftershock",
                    "pain_fuel",
                    "slaughter_drive",
                    "spell_parry",
                    "runic_edge",
                    "sunder_sigil",
                    "balance_point",
                    "flux_reversal",
                    "blink_strike",
                    "counter_seal",
                    "human_resolve",
                    "dwarf_forge_heart",
                ),
            ),
        )
        assertEquals(
            setOf(
                "shattered_outpost",
                "greenwood_fringe",
                "bandit_camp",
                "elven_ruins",
                "deep_iron_pit",
                "molten_core",
                "grey_gate_depths",
                "underground_river",
                "crystal_cavern",
                "abyssal_temple",
                "abyssal_heart",
            ),
            catalog.zones.map { it.id }.toSet(),
        )
        assertEquals(
            setOf(
                "armory_gate",
                "merchant_stall",
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
                "bandit_cache",
                "elven_wardstone",
                "molten_pressure_valve",
                "crystal_resonance_node",
                "crystal_cache_chest",
                "river_ferry_anchor",
                "temple_ward_reliquary",
                "heart_ward_focus",
            ),
            catalog.interactables.map { it.id }.toSet(),
        )
        assertEquals(3, catalog.professions.first { it.id == "vanguard" }.combatProfile.baseDefense)
        assertEquals(103, catalog.professions.first { it.id == "arcanist" }.combatProfile.baseSpeed)
        assertEquals(listOf("backstab", "poison_blade", "deathblow", "execution", "crippling_strike", "eviscerate"), catalog.talentTrees.first { it.id == "rogue_assassination" }.nodes)
        assertEquals(listOf("holy_light", "holy_shield", "purify", "sanctuary", "absolution"), catalog.talentTrees.first { it.id == "templar_grace" }.nodes)
        assertEquals(listOf("blood_rush", "savage_hew", "riven_edge", "pursuit_drive"), catalog.talentTrees.first { it.id == "berserker_wrath" }.nodes)
        assertEquals(listOf("reckless_slam", "rupture_wave", "fault_line", "aftershock"), catalog.talentTrees.first { it.id == "berserker_ruin" }.nodes)
        assertEquals(listOf("kill_frenzy", "last_stand", "pain_fuel", "slaughter_drive"), catalog.talentTrees.first { it.id == "berserker_bloodwar" }.nodes)
        assertEquals(listOf("arcane_edge", "spell_rend", "runic_edge", "sunder_sigil"), catalog.talentTrees.first { it.id == "spellblade_enchanted_blade" }.nodes)
        assertEquals(listOf("flux_anchor", "flux_burst", "balance_point", "flux_reversal"), catalog.talentTrees.first { it.id == "spellblade_elemental_flux" }.nodes)
        assertEquals(listOf("mana_lunge", "spell_parry", "blink_strike", "counter_seal"), catalog.talentTrees.first { it.id == "spellblade_battle_spell" }.nodes)
        assertEquals(3, catalog.talents.first { it.id == "charge" }.unlockLevel)
        assertEquals(2, catalog.talents.first { it.id == "shadowstep" }.unlockLevel)
        assertEquals(3, catalog.talents.first { it.id == "shadow_bind" }.unlockLevel)
        assertEquals("charge_lane", catalog.talents.first { it.id == "charge" }.telegraphRef)
        assertEquals(4, catalog.talents.first { it.id == "blink" }.levelEffects.getValue(5).rangeBonus)
        assertEquals(4, catalog.talents.first { it.id == "blink" }.breakpoints.single().atRank)
        assertEquals(4, catalog.talents.first { it.id == "guard_stance" }.breakpoints.single().atRank)
        assertEquals(4, catalog.talents.first { it.id == "taunt" }.breakpoints.single().atRank)
        assertEquals(2, catalog.talents.first { it.id == "shadowstep" }.breakpoints.single().atRank)
        assertEquals(4, catalog.talents.first { it.id == "shadow_bind" }.breakpoints.single().atRank)
        assertEquals(3, catalog.talents.first { it.id == "holy_mark" }.breakpoints.single().atRank)
        assertEquals(4, catalog.talents.first { it.id == "purify" }.breakpoints.single().atRank)
        assertTrue(
            requireNotNull(runtimeTalentsById["taunt"]).breakpoints.single().unlockedEffects.any { effect ->
                effect is EffectOp.ApplyStatus && effect.statusId == StatusEffectType.GUARD.schemaId
            },
        )
        assertTrue(
            requireNotNull(runtimeTalentsById["shadowstep"]).breakpoints.single().unlockedEffects.any { effect ->
                effect is EffectOp.ApplyStatus && effect.statusId == StatusEffectType.MARKED.schemaId
            },
        )
        assertTrue(
            requireNotNull(runtimeTalentsById["purify"]).breakpoints.single().unlockedEffects.any { effect ->
                effect is EffectOp.ApplyStatus && effect.statusId == StatusEffectType.HOLY_SHIELD_BUFF.schemaId
            },
        )
        assertEquals("PHYSICAL", catalog.talents.first { it.id == "power_strike" }.powerDimension)
        assertEquals(
            "HOSTILE_HIT_THEN_SAVE",
            catalog.talents.first { it.id == "power_strike" }.levelEffects.getValue(5).associatedEffects.single().applicationPolicy,
        )
        assertEquals(
            "MENTAL",
            catalog.talents.first { it.id == "intimidation" }.levelEffects.getValue(1).associatedEffects.single().saveDimension,
        )
        assertEquals(
            "INSTANT_ACTION",
            catalog.talents.first { it.id == "purify" }.levelEffects.getValue(1).cleanseEffect?.applicationPolicy,
        )
        assertEquals(0.10, catalog.talents.first { it.id == "mana_surge" }.levelEffects.getValue(1).resourceRestoreFraction)
        assertEquals(0.18, catalog.talents.first { it.id == "holy_light" }.levelEffects.getValue(1).healFraction)
        assertEquals(0.20, catalog.talents.first { it.id == "divine_intervention" }.levelEffects.getValue(1).healFraction)
        assertEquals("audio.talent.shadowstep", catalog.talents.first { it.id == "shadowstep" }.audioProfile)
        assertEquals("icon.skill.templar.holy_shield", catalog.talents.first { it.id == "holy_shield" }.iconKey)
        assertEquals(
            setOf(
                "shattered_outpost_breach",
                "greenwood_signal_hunt",
                "bandit_camp_cache_raid",
                "deep_iron_pit_forge_run",
                "elven_ruins_relic_ward",
                "grey_gate_seal_rite",
                "molten_core_pressure",
                "underground_river_crossing",
                "crystal_cavern_resonance",
                "abyssal_temple_sanctum",
                "abyssal_heart_finale",
            ),
            catalog.objectiveSets.map { it.id }.toSet(),
        )
        assertEquals("shattered_outpost", catalog.worldGraph.startZoneId)
        assertEquals(10, catalog.worldGraph.connections.size)
        assertEquals(10, catalog.routeRewards.size)
        assertEquals(
            listOf("loot.underground_river.reward", "loot.foundation.boss"),
            catalog.routeRewards.single { reward -> reward.routeId == "route.grey_gate_depths.underground_river" }.milestoneRewardProfileIds,
        )
        assertEquals(
            listOf("loot.underground_river.reward", "loot.foundation.elite"),
            catalog.routeRewards.single { reward -> reward.routeId == "route.underground_river.crystal_cavern" }.milestoneRewardProfileIds,
        )
        assertEquals(
            listOf("loot.abyssal_temple.reward", "loot.foundation.boss"),
            catalog.routeRewards.single { reward -> reward.routeId == "route.underground_river.abyssal_temple" }.milestoneRewardProfileIds,
        )
        assertEquals(3, catalog.shopNodes.size)
        assertEquals(4, catalog.objectiveSets.single { it.id == "shattered_outpost_breach" }.placements.size)
        assertEquals(
            setOf("room_center", "boss_entry", "stairs_up", "player_start"),
            catalog.objectiveSets.single { it.id == "shattered_outpost_breach" }.placements.map { it.anchor }.toSet(),
        )
        assertEquals(
            listOf("river_ferry_anchor", "crystal_cache_chest"),
            catalog.objectiveSets.single { it.id == "underground_river_crossing" }.interactables,
        )
        assertEquals(listOf("temple_ward_reliquary"), catalog.objectiveSets.single { it.id == "abyssal_temple_sanctum" }.interactables)
        assertEquals(
            "abyssal_reliquary_post",
            catalog.interactables.single { it.id == "temple_ward_reliquary" }.shopNodeId,
        )
        assertEquals("quest.bandit_camp", catalog.objectiveSets.single { it.id == "bandit_camp_cache_raid" }.linkedQuestId)
        assertEquals("cache_raid", catalog.objectiveSets.single { it.id == "bandit_camp_cache_raid" }.questObjectiveId)
        assertEquals("quest.elven_ruins", catalog.objectiveSets.single { it.id == "elven_ruins_relic_ward" }.linkedQuestId)
        assertEquals("relic_ward", catalog.objectiveSets.single { it.id == "elven_ruins_relic_ward" }.questObjectiveId)
        assertEquals("quest.molten_core", catalog.objectiveSets.single { it.id == "molten_core_pressure" }.linkedQuestId)
        assertEquals("pressure", catalog.objectiveSets.single { it.id == "molten_core_pressure" }.questObjectiveId)
        assertEquals("quest.crystal_cavern", catalog.objectiveSets.single { it.id == "crystal_cavern_resonance" }.linkedQuestId)
        assertEquals("resonance", catalog.objectiveSets.single { it.id == "crystal_cavern_resonance" }.questObjectiveId)
        assertEquals(setOf("normal"), catalog.difficulties.map { it.id }.toSet())
        assertEquals(60, catalog.monsters.size)
        assertEquals(53, catalog.itemBundle.items.size)
        assertEquals(84, catalog.itemBundle.affixes.size)
        assertTrue(catalog.itemBundle.uniqueTemplates.size >= 20)
        assertTrue(catalog.itemBundle.artifactTemplates.size >= 8)
        assertTrue(
            catalog.itemBundle.artifactTemplates.any { template -> SourceTier.SECRET_ZONE in template.allowedSourceTiers },
            "PR-05 artifact templates must include secret-zone coverage.",
        )
        assertTrue(
            catalog.itemBundle.affixes.any { affix -> affix.stats.castSpeedRating > 0 },
            "PR-05 must ship at least one formal castSpeedRating affix.",
        )
        assertTrue(
            setOf("greenwood_fringe", "deep_iron_pit", "underground_river", "abyssal_temple")
                .all { zoneId ->
                    catalog.itemBundle.uniqueTemplates.any { template -> zoneId in template.allowedZones } ||
                        catalog.itemBundle.artifactTemplates.any { template -> zoneId in template.allowedZones }
                },
            "PR-05 special template coverage must include the four target zones.",
        )
        assertTrue(catalog.itemBundle.items.count { item -> "weapon" in item.tags } >= 6)
        assertTrue(catalog.itemBundle.items.count { item -> "armor" in item.tags && "accessory" !in item.tags } >= 6)
        assertTrue(catalog.itemBundle.items.count { item -> "accessory" in item.tags } >= 4)
        assertTrue(catalog.itemBundle.items.count { item -> "consumable" in item.tags } >= 6)
        assertTrue(catalog.itemBundle.items.count { item -> setOf("reward", "boss_reward", "quest").any(item.tags::contains) } >= 2)
        assertTrue(
            catalog.itemBundle.items.count { item -> item.passive != null && "accessory" !in item.tags } >= 4,
            "Phase 2 PR-F4 should expose at least four non-accessory passive items.",
        )
        val runtimeItemBundle = loader.loadItemBundle()
        assertEquals(
            setOf(
                "ConditionalStatBonus",
                "DamageTypeBonus",
                "DamageVsStatus",
                "DamageVsTag",
                "HpRegenPerTurn",
                "OnHitStatusProc",
                "OnKillResourceRestore",
                "ResistanceBonus",
                "TerrainAffinityBonus",
            ),
            runtimeItemBundle.affixes.mapNotNull { affix -> affix.passive?.let { passive -> passive::class.simpleName } }.toSet(),
        )
        assertEquals("DamageVsTag", runtimeItemBundle.baseItems.first { item -> item.id == "bandit_trophy" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("ResistanceBonus", runtimeItemBundle.baseItems.first { item -> item.id == "seal_reliquary" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("DamageVsTag", runtimeItemBundle.baseItems.first { item -> item.id == "hunter_bow" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("DamageVsTag", runtimeItemBundle.baseItems.first { item -> item.id == "long_sword" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("ResistanceBonus", runtimeItemBundle.baseItems.first { item -> item.id == "chain_mail" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("ResistanceBonus", runtimeItemBundle.baseItems.first { item -> item.id == "shadow_cloak" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("DamageVsStatus", runtimeItemBundle.affixes.first { affix -> affix.id == "of_piercing" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("DamageVsStatus", runtimeItemBundle.affixes.first { affix -> affix.id == "of_shadow" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("HpRegenPerTurn", runtimeItemBundle.affixes.first { affix -> affix.id == "of_cleansing" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("OnHitStatusProc", runtimeItemBundle.affixes.first { affix -> affix.id == "briarhook" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("OnKillResourceRestore", runtimeItemBundle.affixes.first { affix -> affix.id == "lucid" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("ConditionalStatBonus", runtimeItemBundle.affixes.first { affix -> affix.id == "lastlight" }.passive?.let { passive -> passive::class.simpleName })
        assertEquals("TerrainAffinityBonus", runtimeItemBundle.affixes.first { affix -> affix.id == "floodtouched" }.passive?.let { passive -> passive::class.simpleName })
        assertTrue(catalog.visualKeys.contains("actor.vanguard"))
        assertTrue(catalog.visualKeys.contains("talent.arcanist.mana_surge.icon"))
        assertTrue(catalog.visualKeys.contains("icon.skill.rogue.backstab"))
        assertTrue(catalog.visualKeys.contains("icon.skill.templar.divine_intervention"))
        assertTrue(catalog.visualKeys.contains("actor.orc.miner"))
        assertTrue(catalog.visualKeys.contains("actor.cultist.shadow_priest"))
        assertTrue(catalog.visualKeys.contains("actor.orc.molten_giant"))
        assertTrue(catalog.visualKeys.contains("actor.abyssal.guardian"))
        assertTrue(catalog.visualKeys.contains("boss.orc.molten_giant.visual"))
        assertTrue(catalog.visualKeys.contains("boss.abyssal.guardian.visual"))
        assertTrue(catalog.visualKeys.contains("talent.vanguard.linebreaker.visual"))
        assertTrue(catalog.visualKeys.contains("icon.skill.templar.ritual_break"))
        assertTrue(catalog.visualKeys.contains("talent.berserker.riven_edge.visual"))
        assertTrue(catalog.visualKeys.contains("talent.spellblade.runic_edge.visual"))
        assertTrue(catalog.visualKeys.contains("icon.skill.berserker.fault_line"))
        assertTrue(catalog.visualKeys.contains("icon.skill.spellblade.counter_seal"))
        assertTrue(catalog.visualKeys.contains("prop.mine_furnace"))
        assertTrue(catalog.visualKeys.contains("prop.ritual_altar"))
        assertTrue(catalog.visualKeys.contains("prop.hidden_entrance.revealed"))
        assertTrue(catalog.visualKeys.contains("prop.hidden_entrance.return_bridge"))
        assertTrue(catalog.visualKeys.contains("zone.secret.greenwood_hidden_cache.visual"))
        assertTrue(catalog.visualKeys.contains("zone.secret.deep_iron_slag_cache.icon"))
        assertTrue(catalog.visualKeys.contains("zone.secret.underground_river_crystal_rift.visual"))
        assertTrue(catalog.visualKeys.contains("zone.secret.abyssal_temple_warded_archive.icon"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.power_strike"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.mana_surge"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.backstab"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.holy_light"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.linebreaker"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.void_breach"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.riven_edge"))
        assertTrue(catalog.audioProfiles.contains("audio.talent.runic_edge"))
        assertTrue(catalog.audioProfiles.contains("audio.monster.bandit_family"))
        assertTrue(catalog.audioProfiles.contains("audio.boss.orc.molten_giant"))
        assertTrue(catalog.audioProfiles.contains("audio.monster.default"))
        assertTrue(catalog.audioProfiles.contains("audio.hidden.reveal.secret_entrance"))
        assertTrue(catalog.audioProfiles.contains("audio.interactable.hidden_return"))
        assertTrue(catalog.audioProfiles.contains("audio.secret_zone.greenwood_hidden_cache"))
        assertTrue(catalog.audioProfiles.contains("audio.secret_zone.abyssal_temple_warded_archive"))
        assertTrue(catalog.audioProfiles.contains("audio.secret_zone.deep_iron_smuggler_stash"))
        assertEquals(12, catalog.hiddenEvents.size)
        assertEquals(
            setOf(
                "greenwood_hidden_cache",
                "deep_iron_slag_cache",
                "deep_iron_smuggler_stash",
                "underground_river_crystal_rift",
                "abyssal_temple_warded_archive",
            ),
            catalog.secretZones.map { secretZone -> secretZone.id.id }.toSet(),
        )
        assertEquals(
            setOf("hidden.branch", "hidden.critical.adjacent", "hidden.goal.adjacent"),
            catalog.secretZones.map { secretZone -> secretZone.entranceBindingId.value }.toSet(),
        )
        assertTrue(
            catalog.hiddenEvents
                .flatMap { hiddenEvent -> hiddenEvent.rewards.map { reward -> reward.key.name } }
                .toSet()
                .containsAll(setOf("REVEAL_SECRET_ZONE", "GRANT_BUFF", "LOOT_PROFILE", "TRIGGER_ENCOUNTER")),
        )
        val rewardKeysByHiddenEventId =
            catalog.hiddenEvents.associate { hiddenEvent ->
                hiddenEvent.id to hiddenEvent.rewards.map { reward -> reward.key.name }
            }
        assertEquals(
            listOf("GRANT_BUFF", "LOOT_PROFILE"),
            rewardKeysByHiddenEventId.getValue("hidden.event.greenwood.hidden_cache.reward"),
        )
        assertEquals(
            listOf("GRANT_BUFF", "LOOT_PROFILE"),
            rewardKeysByHiddenEventId.getValue("hidden.event.abyssal_temple.warded_archive.reward"),
        )
        assertEquals(
            listOf("GRANT_BUFF", "TRIGGER_ENCOUNTER", "LOOT_PROFILE"),
            rewardKeysByHiddenEventId.getValue("hidden.event.deep_iron.smuggler_stash.reward"),
        )
        assertTrue(
            catalog.zoneMapgenProfiles
                .flatMap { profile -> profile.hiddenEntrancePlans }
                .none { entrance -> entrance.targetSecretZoneId.id.endsWith("_stub") },
            "PR-07 must replace all secret-zone stub ids with formal ids.",
        )
        assertEquals(
            listOf("healing_potion", "short_sword", "leather_armor", "bandit_trophy", "stamina_draught", "hunter_bow"),
            catalog.lootProfiles.first { it.id == "loot.foundation.common" }.itemIds,
        )
        assertEquals(3, catalog.lootProfiles.first { it.id == "loot.foundation.common" }.schemaVersion)
        assertEquals(LootPoolStrategy.FIXED_LIST, catalog.lootProfiles.first { it.id == "loot.foundation.common" }.poolStrategy)
        assertEquals(6, catalog.lootProfiles.first { it.id == "loot.foundation.common" }.rewardBudget)
        assertTrue(catalog.lootProfiles.first { it.id == "loot.foundation.common" }.itemIds.contains("hunter_bow"))
        assertEquals(
            listOf("basic_shield", "mana_potion", "chain_mail", "apprentice_robe", "long_sword", "emerald_charm", "furnace_talisman", "energy_tonic"),
            catalog.lootProfiles.first { it.id == "loot.foundation.elite" }.itemIds,
        )
        assertEquals(3, catalog.lootProfiles.first { it.id == "loot.foundation.elite" }.schemaVersion)
        assertEquals(LootPoolStrategy.FIXED_LIST, catalog.lootProfiles.first { it.id == "loot.foundation.elite" }.poolStrategy)
        assertEquals(8, catalog.lootProfiles.first { it.id == "loot.foundation.elite" }.rewardBudget)
        assertTrue(
            catalog.lootProfiles.first { it.id == "loot.foundation.elite" }.itemIds.containsAll(listOf("chain_mail", "long_sword")),
        )
        assertEquals(
            listOf("battle_axe", "long_sword", "hunter_bow", "plate_armor", "arcane_staff", "scroll_teleport", "mana_potion", "forgebreaker_pick", "sanctified_seal", "seal_reliquary", "shadow_cloak", "consecrated_oil"),
            catalog.lootProfiles.first { it.id == "loot.foundation.boss" }.itemIds,
        )
        assertEquals(3, catalog.lootProfiles.first { it.id == "loot.foundation.boss" }.schemaVersion)
        assertEquals(LootPoolStrategy.FIXED_LIST, catalog.lootProfiles.first { it.id == "loot.foundation.boss" }.poolStrategy)
        assertEquals(10, catalog.lootProfiles.first { it.id == "loot.foundation.boss" }.rewardBudget)
        assertTrue(catalog.lootProfiles.first { it.id == "loot.foundation.boss" }.itemIds.contains("shadow_cloak"))
        assertEquals(
            listOf("abyssal_heartstone"),
            catalog.lootProfiles.first { it.id == "loot.abyssal_heart.reward" }.itemIds,
        )
        assertEquals(3, catalog.lootProfiles.first { it.id == "loot.abyssal_heart.reward" }.schemaVersion)
        assertEquals(LootPoolStrategy.FIXED_LIST, catalog.lootProfiles.first { it.id == "loot.abyssal_heart.reward" }.poolStrategy)
        assertEquals(1, catalog.lootProfiles.first { it.id == "loot.abyssal_heart.reward" }.rewardBudget)
        val banditCaptainAi = catalog.aiProfiles.first { it.id == "ai.boss.bandit_captain.phase_full" }
        assertEquals(AISelectionPolicy.WEIGHTED_RANDOM, banditCaptainAi.selectionPolicy)
        assertEquals("strike", banditCaptainAi.actions.first().id)
        assertEquals("earthshaker", catalog.aiProfiles.first { it.id == "ai.boss.molten_giant.phase_enraged" }.actions.first().id)
        assertEquals("ritual_break", catalog.aiProfiles.first { it.id == "ai.boss.dungeon_lord.phase_enraged" }.actions.first().id)
        assertEquals("void_breach", catalog.aiProfiles.first { it.id == "ai.boss.abyssal_guardian.phase_abyssal" }.actions.first().id)
        assertTrue(catalog.telegraphSpecs.any { spec -> spec.id == "molten_giant_phase_warning" })
        assertTrue(catalog.telegraphSpecs.any { spec -> spec.id == "abyssal_guardian_phase_warning" })
        assertTrue(catalog.threatProfiles.any { profile -> profile.id == "threat.frontliner.mid" })
        assertEquals("molten_giant_encounter", catalog.zones.first { it.id == "deep_iron_pit" }.bossEncounterId)
        assertEquals(2, catalog.bossEncounters.first { it.id == "dungeon_lord_encounter" }.phases.size)
        assertTrue(catalog.arenas.any { it.id == "arena.shattered_outpost.boss" })
        assertTrue(catalog.arenas.any { it.id == "arena.deep_iron_pit.boss" })
        assertTrue(catalog.arenas.any { it.id == "arena.abyssal_heart.boss" })
    }

    @Test
    fun `runtime projections follow active locale`() {
        val enLoader = DataLoader(GameLocale.EN_US)
        val zhLoader = DataLoader(GameLocale.ZH_CN)

        assertEquals("Rat", enLoader.loadMonsterCatalog().monsters.first { it.id == "beast.rat" }.name)
        assertEquals("老鼠", zhLoader.loadMonsterCatalog().monsters.first { it.id == "beast.rat" }.name)
        assertEquals("Short Sword", enLoader.loadItemBundle().baseItems.first { it.id == "short_sword" }.name)
        assertEquals("短剑", zhLoader.loadItemBundle().baseItems.first { it.id == "short_sword" }.name)
        assertEquals("talent.vanguard.power_strike.name", enLoader.loadTalentDefinitions().first { it.id == "power_strike" }.nameKey)
        assertEquals("talent.vanguard.power_strike.name", zhLoader.loadTalentDefinitions().first { it.id == "power_strike" }.nameKey)
        assertEquals("talent.arcanist.fireball.name", enLoader.loadTalentDefinitions().first { it.id == "fireball" }.nameKey)
        assertEquals("talent.arcanist.fireball.name", zhLoader.loadTalentDefinitions().first { it.id == "fireball" }.nameKey)
        assertEquals(DamageType.FIRE, enLoader.loadTalentDefinitions().first { it.id == "fireball" }.damageType)
        assertEquals(SaveDimension.PHYSICAL, enLoader.loadTalentDefinitions().first { it.id == "power_strike" }.powerDimension)
        assertEquals(ActionCost.STANDARD, enLoader.loadTalentDefinitions().first { it.id == "power_strike" }.actionCost)
        assertEquals(
            ApplicationPolicy.HOSTILE_HIT_THEN_SAVE,
            enLoader.loadTalentDefinitions().first { it.id == "power_strike" }.levelEffects.getValue(5).associatedEffects.single().applicationPolicy,
        )
        assertEquals(
            StatusEffectType.HOLY_SHIELD_BUFF.schemaId,
            enLoader.loadTalentDefinitions().first { it.id == "holy_shield" }.levelEffects.getValue(1).associatedEffects.single().statusId,
        )
        assertEquals(
            ApplicationPolicy.INSTANT_ACTION,
            enLoader.loadTalentDefinitions().first { it.id == "purify" }.levelEffects.getValue(1).cleanseEffect?.applicationPolicy,
        )
        assertEquals(
            ResourceType.MANA,
            enLoader.loadTalentDefinitions().first { it.id == "mana_surge" }
                .levelEffects
                .getValue(1)
                .effectOps
                .filterIsInstance<EffectOp.ResourceRestore>()
                .single()
                .type,
        )
        assertEquals(
            ResourceType.ENERGY,
            enLoader.loadTalentDefinitions().first { it.id == "deathblow" }
                .levelEffects
                .getValue(1)
                .effectOps
                .filterIsInstance<EffectOp.ResourceRestore>()
                .single()
                .type,
        )
        assertEquals(0.10, enLoader.loadTalentDefinitions().first { it.id == "mana_surge" }.levelEffects.getValue(1).resourceRestoreFraction)
        assertEquals(0.18, enLoader.loadTalentDefinitions().first { it.id == "holy_light" }.levelEffects.getValue(1).healFraction)
        assertTrue(
            enLoader.loadTalentDefinitions().first { it.id == "blink" }
                .breakpoints
                .single()
                .unlockedEffects
                .any { effect -> effect is EffectOp.ResourceRestore },
        )
        assertTrue(
            enLoader.loadTalentDefinitions().first { it.id == "guard_stance" }
                .breakpoints
                .single()
                .unlockedEffects
                .any { effect -> effect is EffectOp.ApplyStatus && effect.statusId == StatusEffectType.GUARD.schemaId },
        )
        assertTrue(
            enLoader.loadTalentDefinitions().first { it.id == "shadow_bind" }
                .breakpoints
                .single()
                .unlockedEffects
                .any { effect -> effect is EffectOp.ApplyStatus && effect.statusId == StatusEffectType.MARKED.schemaId },
        )
        assertTrue(
            enLoader.loadTalentDefinitions().first { it.id == "holy_mark" }
                .breakpoints
                .single()
                .unlockedEffects
                .any { effect -> effect is EffectOp.ApplyStatus && effect.statusId == StatusEffectType.BANE.schemaId },
        )
        assertTrue(enLoader.loadItemBundle().baseItems.first { it.id == "emerald_charm" }.passive is EquipmentPassive.HpRegenPerTurn)
        assertTrue(enLoader.loadItemBundle().baseItems.first { it.id == "hunter_bow" }.passive is EquipmentPassive.DamageVsTag)
        assertTrue(enLoader.loadItemBundle().baseItems.first { it.id == "long_sword" }.passive is EquipmentPassive.DamageVsTag)
        assertTrue(enLoader.loadItemBundle().baseItems.first { it.id == "chain_mail" }.passive is EquipmentPassive.ResistanceBonus)
        assertTrue(enLoader.loadItemBundle().baseItems.first { it.id == "shadow_cloak" }.passive is EquipmentPassive.ResistanceBonus)
        assertTrue(enLoader.loadItemBundle().affixes.first { it.id == "of_smite" }.passive is EquipmentPassive.DamageVsStatus)
        assertTrue(enLoader.loadItemBundle().affixes.first { it.id == "of_cleansing" }.passive is EquipmentPassive.HpRegenPerTurn)
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
        assertEquals("ai.boss.dungeon_lord.phase_full", boss.aiProfileId)
        assertEquals("loot.foundation.boss", boss.lootProfileId)
        assertEquals(mapOf("battlefield_command" to 3, "shadow_bind" to 3, "ritual_break" to 4, "arcane_shield" to 3), boss.talentLevels)
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

        assertEquals("ai.boss.bandit_captain.phase_full", boss.aiProfileId)
        assertEquals("arena.shattered_outpost.boss", encounter.arenaId)
    }

    @Test
    fun `abyssal guardian keeps dedicated finale routing ids`() {
        val boss =
            DataLoader(GameLocale.EN_US)
                .loadMonsterCatalog()
                .monsters
                .first { monster -> monster.id == "abyssal.guardian" }
        val encounter =
            DataLoader(GameLocale.EN_US)
                .loadSchemaCatalog()
                .bossEncounters
                .first { schema -> schema.id == "abyssal_guardian_encounter" }

        assertEquals("ai.boss.abyssal_guardian.phase_full", boss.aiProfileId)
        assertEquals("loot.abyssal_heart.reward", boss.lootProfileId)
        assertEquals("arena.abyssal_heart.boss", encounter.arenaId)
        assertEquals("ai.boss.abyssal_guardian.phase_abyssal", encounter.phases.last().aiProfileId)
    }

    @Test
    fun `schema v2 loader rejects removed legacy effect fields`() {
        val loader = DataLoader(GameLocale.EN_US)
        val parseMethod =
            DataLoader::class.java.getDeclaredMethod("parseTalentLevelEffect", Map::class.java).apply {
                isAccessible = true
            }

        val error =
            assertThrows(InvocationTargetException::class.java) {
                parseMethod.invoke(
                    loader,
                    linkedMapOf(
                        "damageMultiplier" to 1.2,
                        "stunDuration" to 2,
                        "associatedEffects" to
                            listOf(
                                linkedMapOf(
                                    "effectId" to "legacy_stun",
                                    "effectType" to "STUN",
                                    "applicationPolicy" to "HOSTILE_HIT_THEN_SAVE",
                                    "saveDimension" to "PHYSICAL",
                                    "duration" to 2,
                                ),
                            ),
                    ),
                )
            }

        assertTrue(error.cause is IllegalArgumentException)
        assertTrue(error.cause?.message?.contains("removed legacy fields stunDuration") == true)
    }

    @Test
    fun `schema v2 loader rejects save gated effects without save dimension`() {
        val loader = DataLoader(GameLocale.EN_US)
        val parseMethod =
            DataLoader::class.java.getDeclaredMethod("parseTalentLevelEffect", Map::class.java).apply {
                isAccessible = true
            }

        val error =
            assertThrows(InvocationTargetException::class.java) {
                parseMethod.invoke(
                    loader,
                    linkedMapOf(
                        "associatedEffects" to
                            listOf(
                                linkedMapOf(
                                    "effectId" to "invalid_save_gate",
                                    "effectType" to "STUN",
                                    "applicationPolicy" to "HOSTILE_HIT_THEN_SAVE",
                                    "duration" to 2,
                                ),
                            ),
                    ),
                )
            }

        assertTrue(error.cause is IllegalArgumentException)
        assertTrue(error.cause?.message?.contains("requires saveDimension") == true)
    }

    @Test
    fun `schema v2 loader requires explicit preferred terrain tags and rejects invalid terrain tags on elite mutations`() {
        val loader = DataLoader(GameLocale.EN_US)
        val parseMethod =
            DataLoader::class.java.getDeclaredMethod("parseEliteMutationDefs", Map::class.java).apply {
                isAccessible = true
            }

        val missingFieldError =
            assertThrows(InvocationTargetException::class.java) {
                parseMethod.invoke(
                    loader,
                    linkedMapOf(
                        "eliteMutations" to
                            listOf(
                                linkedMapOf(
                                    "id" to "elite.missing_preferred_terrain",
                                    "kind" to "STAT_PACKAGE",
                                    "tier" to "MINOR",
                                    "threatCost" to 1,
                                    "nameKey" to "mutation.test.missing_preferred_terrain.name",
                                    "iconKey" to "icon.mutation.test.missing_preferred_terrain",
                                    "applyToTags" to listOf("elite"),
                                    "minFloor" to 1,
                                ),
                            ),
                    ),
                )
            }

        assertTrue(missingFieldError.cause is IllegalStateException)
        assertTrue(missingFieldError.cause?.message?.contains("preferredTerrainTags") == true)

        val invalidTagError =
            assertThrows(InvocationTargetException::class.java) {
                parseMethod.invoke(
                    loader,
                    linkedMapOf(
                        "eliteMutations" to
                            listOf(
                                linkedMapOf(
                                    "id" to "elite.invalid_preferred_terrain",
                                    "kind" to "STAT_PACKAGE",
                                    "tier" to "MINOR",
                                    "threatCost" to 1,
                                    "nameKey" to "mutation.test.invalid_preferred_terrain.name",
                                    "iconKey" to "icon.mutation.test.invalid_preferred_terrain",
                                    "applyToTags" to listOf("elite"),
                                    "minFloor" to 1,
                                    "preferredTerrainTags" to listOf("LAVA"),
                                ),
                            ),
                    ),
                )
            }

        assertTrue(invalidTagError.cause is IllegalArgumentException)
        assertTrue(invalidTagError.cause?.message?.contains("No enum constant") == true)
    }

    @Test
    fun `schema v2 loader parses and projects talent ai hints`() {
        val loader = DataLoader(GameLocale.EN_US)
        val parseMethod =
            DataLoader::class.java.getDeclaredMethod("parseTalentSchemas", Map::class.java, Set::class.java).apply {
                isAccessible = true
            }
        val toRuntimeMethod =
            DataLoader::class.java.getDeclaredMethod("toRuntimeTalent", TalentSchemaV2::class.java, ResourceType::class.java).apply {
                isAccessible = true
            }

        @Suppress("UNCHECKED_CAST")
        val schema =
            (parseMethod.invoke(
                loader,
                linkedMapOf(
                    "talents" to
                        listOf(
                            linkedMapOf(
                                "id" to "test_ai_hints",
                                "nameKey" to "talent.test.ai_hints.name",
                                "descKey" to "talent.test.ai_hints.desc",
                                "visualKey" to "talent.test.ai_hints.visual",
                                "iconKey" to "icon.skill.test.ai_hints",
                                "audioProfile" to "audio.talent.test.ai_hints",
                                "schemaVersion" to 2,
                                "maxPoints" to 5,
                                "tier" to 2,
                                "category" to "ACTIVE",
                                "damageType" to "SHADOW",
                                "powerDimension" to "MENTAL",
                                "kind" to "ACTIVE",
                                "cooldown" to 8,
                                "castTime" to "STANDARD",
                                "targeting" to "SINGLE_TARGET",
                                "range" to 5,
                                "minRange" to 1,
                                "resourceCosts" to linkedMapOf("MANA" to 12),
                                "levelEffects" to linkedMapOf("1" to linkedMapOf<String, Any?>()),
                                "aiHints" to
                                    linkedMapOf(
                                        "role" to "CONTROL",
                                        "preferredRange" to listOf(3, 5),
                                        "isSustainToggle" to true,
                                    ),
                                "treeId" to "arcanist_arcane",
                            ),
                        ),
                ),
                emptySet<String>(),
            ) as List<TalentSchemaV2>).single()

        assertEquals("CONTROL", schema.aiHints?.role)
        assertEquals(3, schema.aiHints?.preferredRange?.start)
        assertEquals(5, schema.aiHints?.preferredRange?.endInclusive)
        assertTrue(schema.aiHints?.isSustainToggle == true)

        val runtimeTalent =
            toRuntimeMethod.invoke(loader, schema, ResourceType.MANA) as TalentDef

        assertEquals(TalentRole.CONTROL, runtimeTalent.aiHints?.role)
        assertEquals(3..5, runtimeTalent.aiHints?.preferredRange)
        assertTrue(runtimeTalent.aiHints?.isSustainToggle == true)
    }

    @Test
    fun `schema v2 loader rejects profession schemas without primary spend axis`() {
        val loader = DataLoader(GameLocale.EN_US)
        val parseMethod =
            DataLoader::class.java.getDeclaredMethod("parseProfessionSchemas", Map::class.java).apply {
                isAccessible = true
            }

        val error =
            assertThrows(InvocationTargetException::class.java) {
                parseMethod.invoke(
                    loader,
                    linkedMapOf(
                        "professions" to
                            listOf(
                                linkedMapOf(
                                    "id" to "broken_profession",
                                    "nameKey" to "profession.broken.name",
                                    "descKey" to "profession.broken.desc",
                                    "resourceHintKey" to "profession.broken.resource_hint",
                                    "visualKey" to "actor.vanguard",
                                    "iconKey" to "icon.profession.vanguard",
                                    "audioProfile" to "audio.profession.vanguard",
                                    "schemaVersion" to 1,
                                    "resourceProfiles" to
                                        listOf(
                                            linkedMapOf(
                                                "axis" to "STAMINA",
                                                "initialCurrent" to 40,
                                                "max" to 40,
                                            ),
                                        ),
                                    "baseStats" to linkedMapOf("str" to 10, "dex" to 8, "con" to 9, "wil" to 7),
                                    "combatProfile" to
                                        linkedMapOf(
                                            "baseHp" to 22,
                                            "baseAttack" to 6,
                                            "baseDefense" to 4,
                                            "baseAccuracy" to 6,
                                            "baseEvasion" to 4,
                                            "baseSpeed" to 100,
                                        ),
                                    "statGrowth" to linkedMapOf("str" to 2, "dex" to 1, "con" to 2, "wil" to 1),
                                    "talentTrees" to emptyList<String>(),
                                    "startingTalents" to emptyList<String>(),
                                    "startingKit" to emptyList<String>(),
                                    "soloContract" to
                                        linkedMapOf(
                                            "offenseTags" to listOf("single_target"),
                                            "defenseTags" to listOf("guard"),
                                            "mobilityTags" to listOf("dash"),
                                            "aoeAnswerTags" to listOf("cleave"),
                                            "bossAnswerTags" to listOf("burst"),
                                            "panicAnswerTags" to listOf("shield"),
                                        ),
                                ),
                            ),
                    ),
                )
            }

        val causeMessages =
            generateSequence(error.cause) { throwable -> throwable.cause }
                .mapNotNull(Throwable::message)
                .toList()

        assertTrue(causeMessages.isNotEmpty())
        assertTrue(causeMessages.any { message -> "primarySpendAxis" in message })
    }

    @Test
    fun `schema v2 loader rejects profession schemas without resource hint key`() {
        val loader = DataLoader(GameLocale.EN_US)
        val parseMethod =
            DataLoader::class.java.getDeclaredMethod("parseProfessionSchemas", Map::class.java).apply {
                isAccessible = true
            }

        val error =
            assertThrows(InvocationTargetException::class.java) {
                parseMethod.invoke(
                    loader,
                    linkedMapOf(
                        "professions" to
                            listOf(
                                linkedMapOf(
                                    "id" to "broken_profession",
                                    "nameKey" to "profession.broken.name",
                                    "descKey" to "profession.broken.desc",
                                    "visualKey" to "actor.vanguard",
                                    "iconKey" to "icon.profession.vanguard",
                                    "audioProfile" to "audio.profession.vanguard",
                                    "schemaVersion" to 1,
                                    "resourceProfiles" to
                                        listOf(
                                            linkedMapOf(
                                                "axis" to "STAMINA",
                                                "initialCurrent" to 40,
                                                "max" to 40,
                                            ),
                                        ),
                                    "primarySpendAxis" to "STAMINA",
                                    "baseStats" to linkedMapOf("str" to 10, "dex" to 8, "con" to 9, "wil" to 7),
                                    "combatProfile" to
                                        linkedMapOf(
                                            "baseHp" to 22,
                                            "baseAttack" to 6,
                                            "baseDefense" to 4,
                                            "baseAccuracy" to 6,
                                            "baseEvasion" to 4,
                                            "baseSpeed" to 100,
                                        ),
                                    "statGrowth" to linkedMapOf("str" to 2, "dex" to 1, "con" to 2, "wil" to 1),
                                    "talentTrees" to emptyList<String>(),
                                    "startingTalents" to emptyList<String>(),
                                    "startingKit" to emptyList<String>(),
                                    "soloContract" to
                                        linkedMapOf(
                                            "offenseTags" to listOf("single_target"),
                                            "defenseTags" to listOf("guard"),
                                            "mobilityTags" to listOf("dash"),
                                            "aoeAnswerTags" to listOf("cleave"),
                                            "bossAnswerTags" to listOf("burst"),
                                            "panicAnswerTags" to listOf("shield"),
                                        ),
                                ),
                            ),
                    ),
                )
            }

        val causeMessages =
            generateSequence(error.cause) { throwable -> throwable.cause }
                .mapNotNull(Throwable::message)
                .toList()

        assertTrue(causeMessages.isNotEmpty())
        assertTrue(causeMessages.any { message -> "resourceHintKey" in message })
    }

    @Test
    fun `fixed seed loot corpora surface long sword and shadow cloak on formal short run reward paths`() {
        val loader = DataLoader(GameLocale.EN_US)
        val schemaCatalog = loader.loadSchemaCatalog()
        val itemBundle = loader.loadItemBundle()

        val eliteProfile =
            schemaCatalog.lootProfiles.first { profile -> profile.id == "loot.foundation.elite" }
        val bossProfile =
            schemaCatalog.lootProfiles.first { profile -> profile.id == "loot.foundation.boss" }

        val eliteSeenBaseIds =
            generatedLootBaseIds(
                candidateItems = eliteProfile.itemIds.map { itemId -> resolveBaseItem(itemBundle, itemId) },
                floor = 2,
                seeds = 20260318L..20260381L,
            )
        val bossSeenBaseIds =
            generatedLootBaseIds(
                candidateItems = bossProfile.itemIds.map { itemId -> resolveBaseItem(itemBundle, itemId) },
                floor = 4,
                seeds = 20260318L..20260381L,
            )

        assertTrue(
            "long_sword" in eliteSeenBaseIds,
            "Expected fixed elite loot seed corpus to surface long_sword from loot.foundation.elite.",
        )
        assertTrue(
            "shadow_cloak" in bossSeenBaseIds,
            "Expected fixed boss loot seed corpus to surface shadow_cloak from loot.foundation.boss.",
        )
    }

    private fun generatedLootBaseIds(
        candidateItems: List<ItemBaseDef>,
        floor: Int,
        seeds: LongRange,
    ): Set<String> =
        seeds.map { seed ->
            val random = RandomSource.from(Random(seed))
            val floorCandidates = candidateItems.filter { item -> floor in item.dropFloors }.ifEmpty { candidateItems }
            val chosenBase = chooseWeightedLootItem(floorCandidates, random)
            chosenBase.id
        }.toSet()

    private fun resolveBaseItem(
        itemBundle: ItemDataBundle,
        itemId: String,
    ): ItemBaseDef =
        requireNotNull(itemBundle.baseItems.firstOrNull { item -> item.id == itemId }) {
            "Missing item base $itemId in bundle."
        }

    private fun chooseWeightedLootItem(
        candidates: List<ItemBaseDef>,
        random: RandomSource,
    ): ItemBaseDef {
        val totalWeight = candidates.sumOf { item -> item.dropWeight.coerceAtLeast(1) }
        require(totalWeight > 0) { "Loot selection requires a positive total weight." }
        var roll = random.nextInt(0, totalWeight)
        candidates.forEach { item ->
            roll -= item.dropWeight.coerceAtLeast(1)
            if (roll < 0) {
                return item
            }
        }
        return candidates.last()
    }
}
