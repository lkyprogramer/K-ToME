package com.ktome.game

import com.ktome.core.ai.AIPerceptionState
import com.ktome.core.ai.BossEncounter
import com.ktome.core.ai.BossEncounterState
import com.ktome.core.ai.BossPhaseTransitionTiming
import com.ktome.core.ai.PendingTelegraphState
import com.ktome.core.combat.CombatRuleset
import com.ktome.core.combat.CombatResolver
import com.ktome.core.combat.DamageType
import com.ktome.core.dungeon.StairDirection
import com.ktome.core.dungeon.DungeonManager
import com.ktome.core.dungeon.FloorState
import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Interactable
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.ResistanceProfile
import com.ktome.core.ecs.Stair
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.ecs.remove
import com.ktome.core.effect.AreaEffectEmitter
import com.ktome.core.effect.WorldEffect
import com.ktome.core.inscription.InscriptionCooldownState
import com.ktome.core.inscription.InscriptionLoadout
import com.ktome.core.inscription.InscriptionSlot
import com.ktome.core.item.AffixDef
import com.ktome.core.item.AffixType
import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemQuality
import com.ktome.core.item.ItemType
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar
import com.ktome.core.random.RandomSource
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import com.ktome.core.resource.StaminaPools
import com.ktome.core.save.SaveManager
import com.ktome.core.save.SaveRestoreException
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.ActiveEffect
import com.ktome.core.talent.EffectTracker
import com.ktome.core.status.StatusEffectType
import com.ktome.core.talent.TalentAllocationDraft
import com.ktome.core.talent.TalentCategory
import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentLevelEffect
import com.ktome.core.talent.TalentResolver
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.core.status.StatusLifecycle
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import com.ktome.game.model.MonsterTemplate
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FoundationGameSessionTest {
    @TempDir
    lateinit var tempDir: Path

    private val dataLoader = DataLoader()
    private val talents = dataLoader.loadTalentDefinitions()
    private val talentRegistry = TalentRegistry().apply { registerAll(talents) }
    private val itemBasesById = dataLoader.loadItemBundle().baseItems.associateBy { item -> item.id }

    @Test
    fun `player kill grants experience and level up`() {
        val map = GameMap.fromAscii(
            rows = listOf(
                ".....",
                ".....",
                ".....",
            ),
            playerStart = Point(1, 1),
        )
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        val monsterId = factory.createMonster(
            world = world,
            template = MonsterTemplate(
                id = "training_dummy",
                name = "Training Dummy",
                glyph = 'd',
                colorHex = "#AAAAAA",
                stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 0, wil = 0),
                baseHp = 1,
                baseAttack = 1,
                baseDefense = 0,
                speed = 90,
                ai = AIType.CHASE,
                expReward = 180,
                spawnFloors = listOf(1),
                spawnWeight = 1,
            ),
            position = Point(2, 1),
        )

        val session = FoundationGameSession(
            config = FoundationGameConfig(width = 5, height = 3),
            map = map,
            world = world,
            playerId = playerId,
            combatResolver = combatResolver(doubleValue = 0.0, intValue = 2),
            talentRegistry = talentRegistry,
            talentResolver = TalentResolver(talentRegistry, combatResolver(doubleValue = 0.0, intValue = 2)),
            sessionRandom = fixedRandom(0.0, 2),
        )

        val consumed = session.perform(PlayerCommand.Move(Point(1, 0)))
        val runtimeWorld = runtimeWorld(session)

        assertTrue(consumed)
        assertFalse(runtimeWorld.isAlive(monsterId))
        assertEquals(2, requireNotNull(runtimeWorld.get<Experience>(playerId)).level)
        assertEquals(2, requireNotNull(runtimeWorld.get<Experience>(playerId)).unspentStatPoints)
        assertEquals(requireNotNull(runtimeWorld.get<Health>(playerId)).max, requireNotNull(runtimeWorld.get<Health>(playerId)).current)
        assertTrue(session.messageLog().any { it.contains("gain 180 experience") })
        assertTrue(session.messageLog().any { it.contains("advance to level 2") })
    }

    @Test
    fun `level up applies profession stat growth and unlock cadence`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "vanguard", messageLogSize = 24),
                SaveManager(tempDir.resolve("vanguard-growth-save")),
            )
        clearMonsters(session)
        val baseline = session.playerStatus()
        val dummyId = installExperienceDummy(session, id = "vanguard_growth_dummy", expReward = 1500)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()

        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))

        val runtimeWorld = runtimeWorld(session)
        assertEquals(5, requireNotNull(runtimeWorld.get<Experience>(session.playerId)).level)
        assertTrue(session.playerStatus().attack > baseline.attack)
        assertTrue(session.playerStatus().maxHp > baseline.maxHp)
        assertEquals(
            listOf("猛击", "盾击", "格挡姿态", "战吼"),
            session.talentSlots().map { slot -> slot.name },
        )
        val reserveTalentNames = session.reserveTalentSlots().map { slot -> slot.name }
        assertTrue(reserveTalentNames.containsAll(listOf("冲锋", "横扫", "碎甲", "嘲讽", "不屈")))
        assertTrue(reserveTalentNames.any { name -> name == "集结战旗" || name == "威压" })
        assertTrue(session.messageLog().any { message -> message.contains("冲锋") || message.contains("Charge") })
        val logKeys = session.renderSnapshot().logEvents.map { event -> event.message.key }
        assertTrue(logKeys.contains("log.level_up.stats"))
        assertTrue(logKeys.contains("log.level_up.hp_max"))
        assertTrue(logKeys.contains("log.level_up.resource_max"))
        val levelUpStats = requireNotNull(logEventByKey(session, "log.level_up.stats")).message.arguments.associateBy { argument -> argument.name }
        assertEquals("ui.stat.str", levelUpStats.getValue("strLabel").valueKey)
        assertEquals("+12", levelUpStats.getValue("str").value)
        assertEquals("、", levelUpStats.getValue("sep1").value)
        assertEquals("ui.stat.dex", levelUpStats.getValue("dexLabel").valueKey)
        assertEquals("+8", levelUpStats.getValue("dex").value)
        assertEquals("ui.stat.con", levelUpStats.getValue("conLabel").valueKey)
        assertEquals("+12", levelUpStats.getValue("con").value)
        assertEquals("ui.stat.wil", levelUpStats.getValue("wilLabel").valueKey)
        assertEquals("+4", levelUpStats.getValue("wil").value)
        assertEquals(
            "96",
            requireNotNull(logEventByKey(session, "log.level_up.hp_max"))
                .message
                .arguments
                .first { argument -> argument.name == "amount" }
                .value,
        )
        assertEquals(
            "20",
            requireNotNull(logEventByKey(session, "log.level_up.resource_max"))
                .message
                .arguments
                .first { argument -> argument.name == "amount" }
                .value,
        )
        assertEquals(
            "ui.hud.stamina.short",
            requireNotNull(logEventByKey(session, "log.level_up.resource_max"))
                .message
                .arguments
                .first { argument -> argument.name == "resource" }
                .valueKey,
        )
        assertTrue(logKeys.indexOf("log.level_up") < logKeys.indexOf("log.talent.unlock"))
    }

    @Test
    fun `arcanist level growth unlocks mana surge and increases blink range`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "arcanist", messageLogSize = 24),
                SaveManager(tempDir.resolve("arcanist-growth-save")),
            )
        clearMonsters(session)
        val baselineBlink = session.talentSlots().first { slot -> slot.talentId == "blink" }
        val dummyId = installExperienceDummy(session, id = "arcanist_growth_dummy", expReward = 1500)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()

        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))

        assertTrue(session.perform(PlayerCommand.AssignTalent("blink")))
        val blink = session.talentSlots().first { slot -> slot.talentId == "blink" }
        assertEquals(5, requireNotNull(runtimeWorld(session).get<Experience>(session.playerId)).level)
        assertTrue(blink.range > baselineBlink.range)
        assertTrue(session.reserveTalentSlots().any { slot -> slot.talentId == "mana_surge" })
        assertTrue(session.reserveTalentSlots().any { slot -> slot.talentId == "ice_prison" })
        assertEquals(
            "96",
            requireNotNull(logEventByKey(session, "log.level_up.resource_max"))
                .message
                .arguments
                .first { argument -> argument.name == "amount" }
                .value,
        )
        assertEquals(
            "ui.hud.mana.short",
            requireNotNull(logEventByKey(session, "log.level_up.resource_max"))
                .message
                .arguments
                .first { argument -> argument.name == "resource" }
                .valueKey,
        )
    }

    @Test
    fun `assign talent stays in draft until confirm commits live rank`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("talent-draft-confirm-save")),
            )
        clearMonsters(session)
        val dummyId = installExperienceDummy(session, id = "talent_draft_confirm_dummy", expReward = 1500)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))

        val liveLoadout = requireNotNull(runtimeWorld(session).get<com.ktome.core.talent.TalentLoadout>(session.playerId))
        val committedRank = liveLoadout.levelOf("blink")

        assertTrue(session.perform(PlayerCommand.AssignTalent("blink")))
        assertEquals(committedRank + 1, session.talentSlots().first { slot -> slot.talentId == "blink" }.level)
        assertEquals(committedRank, session.talentSlots().first { slot -> slot.talentId == "blink" }.committedLevel)
        assertEquals(committedRank, liveLoadout.levelOf("blink"))

        assertTrue(session.perform(PlayerCommand.ConfirmTalentDraft))
        assertEquals(committedRank + 1, liveLoadout.levelOf("blink"))
        assertEquals(committedRank + 1, session.talentSlots().first { slot -> slot.talentId == "blink" }.committedLevel)
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.talent.draft_confirmed" })
    }

    @Test
    fun `confirm talent draft is blocked during combat and keeps pending preview intact`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("talent-draft-confirm-combat-save")),
            )
        clearMonsters(session)
        val growthDummyId = installExperienceDummy(session, id = "talent_draft_combat_growth_dummy", expReward = 1500)
        val growthDummyPosition = requireNotNull(runtimeWorld(session).get<Position>(growthDummyId)).toPoint()
        assertTrue(session.perform(PlayerCommand.Move(growthDummyPosition - session.playerPosition())))

        val liveLoadout = requireNotNull(runtimeWorld(session).get<com.ktome.core.talent.TalentLoadout>(session.playerId))
        val committedRank = liveLoadout.levelOf("blink")
        assertTrue(session.perform(PlayerCommand.AssignTalent("blink")))
        forcePlayerInCombat(session)

        assertFalse(session.perform(PlayerCommand.ConfirmTalentDraft))
        assertEquals(committedRank, liveLoadout.levelOf("blink"))
        val blink = session.talentSlots().first { slot -> slot.talentId == "blink" }
        assertEquals(committedRank + 1, blink.level)
        assertEquals(committedRank, blink.committedLevel)
        assertTrue(blink.hasPendingAllocation)
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.talent.draft_confirm_blocked" })
    }

    @Test
    fun `rollback clears pending talent preview without mutating live rank`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("talent-draft-rollback-save")),
            )
        clearMonsters(session)
        val dummyId = installExperienceDummy(session, id = "talent_draft_rollback_dummy", expReward = 1500)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))

        val liveLoadout = requireNotNull(runtimeWorld(session).get<com.ktome.core.talent.TalentLoadout>(session.playerId))
        val committedRank = liveLoadout.levelOf("blink")

        assertTrue(session.perform(PlayerCommand.AssignTalent("blink")))
        assertTrue(session.perform(PlayerCommand.RollbackTalentDraft))
        assertEquals(committedRank, liveLoadout.levelOf("blink"))
        assertEquals(committedRank, session.talentSlots().first { slot -> slot.talentId == "blink" }.level)
        assertFalse(session.talentSlots().any { slot -> slot.hasPendingAllocation })
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.talent.draft_rollback" })
    }

    @Test
    fun `reserve talent can be assigned directly without equipping it first`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("reserve-talent-direct-assign-save")),
            )
        clearMonsters(session)
        val dummyId = installExperienceDummy(session, id = "reserve_talent_direct_assign_dummy", expReward = 1500)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))

        val liveLoadout = requireNotNull(runtimeWorld(session).get<com.ktome.core.talent.TalentLoadout>(session.playerId))
        val committedRank = liveLoadout.levelOf("mana_surge")

        assertTrue(session.perform(PlayerCommand.AssignTalent("mana_surge")))
        val preview = session.reserveTalentSlots().first { slot -> slot.talentId == "mana_surge" }
        assertEquals(committedRank + 1, preview.level)
        assertEquals(committedRank, preview.committedLevel)
        assertTrue(preview.hasPendingAllocation)

        assertTrue(session.perform(PlayerCommand.ConfirmTalentDraft))
        assertEquals(committedRank + 1, liveLoadout.levelOf("mana_surge"))
    }

    @Test
    fun `rollback only removes the most recent pending talent step even after save load`() {
        val saveManager = SaveManager(tempDir.resolve("talent-draft-rollback-history-save"))
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                saveManager,
            )
        clearMonsters(session)
        val dummyId = installExperienceDummy(session, id = "talent_draft_rollback_history_dummy", expReward = 1500)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))

        val liveLoadout = requireNotNull(runtimeWorld(session).get<com.ktome.core.talent.TalentLoadout>(session.playerId))
        val committedBlink = liveLoadout.levelOf("blink")
        val committedManaSurge = liveLoadout.levelOf("mana_surge")

        assertTrue(session.perform(PlayerCommand.AssignTalent("blink")))
        assertTrue(session.perform(PlayerCommand.AssignTalent("mana_surge")))
        assertTrue(session.saveOnExit())

        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        assertTrue(loaded.perform(PlayerCommand.RollbackTalentDraft))

        val blink = loaded.talentSlots().first { slot -> slot.talentId == "blink" }
        val manaSurge = loaded.reserveTalentSlots().first { slot -> slot.talentId == "mana_surge" }
        assertEquals(committedBlink + 1, blink.level)
        assertEquals(committedBlink, blink.committedLevel)
        assertTrue(blink.hasPendingAllocation)
        assertEquals(committedManaSurge, manaSurge.level)
        assertEquals(committedManaSurge, manaSurge.committedLevel)
        assertFalse(manaSurge.hasPendingAllocation)
        assertFalse(loaded.perform(PlayerCommand.RollbackTalentDraft))
    }

    @Test
    fun `pending talent draft survives save and load without touching live loadout`() {
        val saveManager = SaveManager(tempDir.resolve("talent-draft-persist-save"))
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                saveManager,
            )
        clearMonsters(session)
        val dummyId = installExperienceDummy(session, id = "talent_draft_persist_dummy", expReward = 1500)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))

        val liveLoadout = requireNotNull(runtimeWorld(session).get<com.ktome.core.talent.TalentLoadout>(session.playerId))
        val committedRank = liveLoadout.levelOf("blink")

        assertTrue(session.perform(PlayerCommand.AssignTalent("blink")))
        assertTrue(session.saveOnExit())

        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val loadedBlink = loaded.talentSlots().first { slot -> slot.talentId == "blink" }
        val loadedLoadout = requireNotNull(runtimeWorld(loaded).get<com.ktome.core.talent.TalentLoadout>(loaded.playerId))

        assertEquals(committedRank + 1, loadedBlink.level)
        assertEquals(committedRank, loadedBlink.committedLevel)
        assertTrue(loadedBlink.hasPendingAllocation)
        assertEquals(committedRank, loadedLoadout.levelOf("blink"))
    }

    @Test
    fun `templar level growth omits fixed resource cap change log`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "templar", messageLogSize = 24),
                SaveManager(tempDir.resolve("templar-growth-save")),
            )
        clearMonsters(session)
        val dummyId = installExperienceDummy(session, id = "templar_growth_dummy", expReward = 1500)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()

        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))

        val logKeys = session.renderSnapshot().logEvents.map { event -> event.message.key }
        assertTrue(logKeys.contains("log.level_up.stats"))
        assertTrue(logKeys.contains("log.level_up.hp_max"))
        assertFalse(logKeys.contains("log.level_up.resource_max"))
    }

    @Test
    fun `rogue level growth logs stat gains and unlocks without resource cap change`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "rogue", messageLogSize = 24),
                SaveManager(tempDir.resolve("rogue-growth-save")),
            )
        clearMonsters(session)
        val baseline = session.playerStatus()
        val dummyId = installExperienceDummy(session, id = "rogue_growth_dummy", expReward = 1500)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()

        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))

        val runtimeWorld = runtimeWorld(session)
        assertEquals(5, requireNotNull(runtimeWorld.get<Experience>(session.playerId)).level)
        assertTrue(session.playerStatus().attack > baseline.attack)
        assertTrue(session.playerStatus().maxHp > baseline.maxHp)
        assertTrue(session.talentSlots().any { slot -> slot.talentId == "roll" })
        assertTrue(session.reserveTalentSlots().any { slot -> slot.talentId == "shadowstep" })
        assertTrue(session.reserveTalentSlots().any { slot -> slot.talentId == "deathblow" })
        assertTrue(session.messageLog().any { message -> message.contains("影袭") || message.contains("Shadowstep") })

        val logKeys = session.renderSnapshot().logEvents.map { event -> event.message.key }
        assertTrue(logKeys.contains("log.level_up.stats"))
        assertTrue(logKeys.contains("log.level_up.hp_max"))
        assertFalse(logKeys.contains("log.level_up.resource_max"))
        assertTrue(logKeys.indexOf("log.level_up") < logKeys.indexOf("log.talent.unlock"))
    }

    @Test
    fun `vanguard reserve charge can replace active slot and remap preserves cooldown across save load`() {
        val saveManager = SaveManager(tempDir.resolve("vanguard-loadout-save"))
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager,
            )
        clearMonsters(session)
        val dummyId = installExperienceDummy(session, id = "vanguard_loadout_dummy", expReward = 1500)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()

        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))
        assertTrue(session.reserveTalentSlots().any { slot -> slot.talentId == "charge" })

        assertTrue(session.perform(PlayerCommand.EquipTalentToSlot(slot = 4, talentId = "charge")))
        assertEquals("charge", session.talentSlots().first { slot -> slot.slot == 4 }.talentId)
        assertTrue(session.reserveTalentSlots().any { slot -> slot.talentId == "war_cry" })

        requireNotNull(runtimeWorld(session).get<com.ktome.core.talent.CooldownState>(session.playerId)).remainingByTalentId["charge"] = 2
        val cooldownAfterUse = playerCooldown(session, "charge")
        assertTrue(cooldownAfterUse > 0)

        assertTrue(session.perform(PlayerCommand.EquipTalentToSlot(slot = 1, talentId = "charge")))
        assertEquals("charge", session.talentSlots().first { slot -> slot.slot == 1 }.talentId)
        assertEquals(cooldownAfterUse, playerCooldown(session, "charge"))
        assertTrue(session.saveOnExit())

        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))

        assertEquals("charge", loaded.talentSlots().first { slot -> slot.slot == 1 }.talentId)
        assertEquals("power_strike", loaded.talentSlots().first { slot -> slot.slot == 4 }.talentId)
        assertEquals(cooldownAfterUse, playerCooldown(loaded, "charge"))
        assertTrue(loaded.reserveTalentSlots().any { slot -> slot.talentId == "war_cry" })
    }

    @Test
    fun `templar reserve judgment hammer can be equipped and cast from active slot`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260320L, zoneId = "shattered_outpost", playerProfessionId = "templar"),
                SaveManager(tempDir.resolve("templar-loadout-save")),
            )
        clearMonsters(session)
        val dummyId = installExperienceDummy(session, id = "templar_loadout_dummy", expReward = 600)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()

        assertTrue(session.perform(PlayerCommand.Move(dummyPosition - session.playerPosition())))
        assertTrue(session.reserveTalentSlots().any { slot -> slot.talentId == "judgment_hammer" })
        requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.POSITIVE_ENERGY)).current = 30

        assertTrue(session.perform(PlayerCommand.EquipTalentToSlot(slot = 2, talentId = "judgment_hammer")))
        val hammerSlot = session.talentSlots().first { slot -> slot.talentId == "judgment_hammer" }.slot
        val targetPoint = findOpenPointAtDistance(session, minDistance = 1, maxDistance = 3)
        installCombatDummy(session, id = "hammer_target", position = targetPoint)
        session.automationMovePlayerTo(session.playerPosition())

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = hammerSlot, target = targetPoint)))
        assertTrue(playerCooldown(session, "judgment_hammer") > 0)
    }

    @Test
    fun `invalid long delta cannot trigger melee attack`() {
        val map = GameMap.fromAscii(
            rows = listOf(
                ".....",
                ".....",
                ".....",
            ),
            playerStart = Point(1, 1),
        )
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        val monsterId = factory.createMonster(
            world = world,
            template = MonsterTemplate(
                id = "remote_dummy",
                name = "Remote Dummy",
                glyph = 'd',
                colorHex = "#AAAAAA",
                stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 0, wil = 0),
                baseHp = 10,
                baseAttack = 1,
                baseDefense = 0,
                speed = 90,
                ai = AIType.CHASE,
                expReward = 10,
                spawnFloors = listOf(1),
                spawnWeight = 1,
            ),
            position = Point(3, 1),
        )

        val session = FoundationGameSession(
            config = FoundationGameConfig(width = 5, height = 3),
            map = map,
            world = world,
            playerId = playerId,
            combatResolver = CombatResolver(fixedRandom(0.0, 2)),
            talentRegistry = talentRegistry,
            talentResolver = TalentResolver(talentRegistry, CombatResolver(fixedRandom(0.0, 2))),
            sessionRandom = fixedRandom(0.0, 2),
        )

        val consumed = session.perform(PlayerCommand.Move(Point(2, 0)))
        val runtimeWorld = runtimeWorld(session)

        assertFalse(consumed)
        assertEquals(Point(1, 1), session.playerPosition())
        assertTrue(runtimeWorld.isAlive(monsterId))
        assertEquals(10, requireNotNull(runtimeWorld.get<Health>(monsterId)).current)
    }

    @Test
    fun `single input only consumes one queued player action`() {
        val map = GameMap.fromAscii(
            rows = listOf(
                ".......",
                ".......",
                ".......",
            ),
            playerStart = Point(1, 1),
        )
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        val boostedStats = requireNotNull(world.get<DerivedStats>(playerId)).copy(speed = 200)
        world.add(playerId, boostedStats)

        val session = FoundationGameSession(
            config = FoundationGameConfig(width = 7, height = 3),
            map = map,
            world = world,
            playerId = playerId,
            combatResolver = CombatResolver(fixedRandom(0.0, 2)),
            talentRegistry = talentRegistry,
            talentResolver = TalentResolver(talentRegistry, CombatResolver(fixedRandom(0.0, 2))),
            sessionRandom = fixedRandom(0.0, 2),
        )

        val firstConsumed = session.perform(PlayerCommand.Move(Point(1, 0)))

        assertTrue(firstConsumed)
        assertEquals(Point(2, 1), session.playerPosition())

        val secondConsumed = session.perform(PlayerCommand.Move(Point(1, 0)))

        assertTrue(secondConsumed)
        assertEquals(Point(3, 1), session.playerPosition())
    }

    @Test
    fun `pick up and equip updates inventory state`() {
        val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val world = World()
        val factory = EntityFactory()
        val itemFactory = ItemFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        itemFactory.createGroundItem(
            world = world,
            item =
                ItemInstance(
                    baseId = "short_sword",
                    name = "Steel Short Sword",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#C0C0C0",
                    stats = StatModifier(attack = 5),
                ),
            position = Point(1, 1),
        )

        val session = session(world, map, playerId)

        assertTrue(session.perform(PlayerCommand.PickUp))
        assertEquals(1, session.inventoryItems().size)
        assertTrue(session.perform(PlayerCommand.ActivateInventoryItem(0)))
        assertEquals(EquipSlot.WEAPON, session.inventoryItems().single().equippedSlot)
        assertTrue(session.playerStatus().attack > 25)
    }

    @Test
    fun `pick up log keeps quality material and affix composition`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("pick-up-log-save")),
            )
        clearMonsters(session)
        ItemFactory().createGroundItem(
            world = runtimeWorld(session),
            item =
                ItemInstance(
                    baseId = "short_sword",
                    name = "Short Sword",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#C0C0C0",
                    quality = ItemQuality.RARE,
                    materialId = "MITHRIL",
                    materialName = "Mithril",
                    affixes = listOf(AffixDef(id = "of_speed", name = "of Speed", type = AffixType.SUFFIX, statModifiers = StatModifier(speed = 15))),
                    stats = StatModifier(attack = 9, speed = 15),
                ),
            position = session.playerPosition(),
        )

        assertTrue(session.perform(PlayerCommand.PickUp))
        val pickupLog = requireNotNull(logEventByKey(session, "log.inventory.pick_up"))
        val itemArgument = pickupLog.message.arguments.single { argument -> argument.name == "item" }
        val displayToken = requireNotNull(itemArgument.valueToken)

        assertNull(itemArgument.value)
        assertNull(itemArgument.valueKey)
        assertEquals("item.display.composed", displayToken.key)
        assertEquals("item.short_sword.name", displayToken.arguments.first { argument -> argument.name == "base" }.valueKey)
        assertEquals(
            "item.display.part.quality",
            requireNotNull(displayToken.arguments.first { argument -> argument.name == "quality" }.valueToken).key,
        )
        assertEquals(
            "item.display.part.material",
            requireNotNull(displayToken.arguments.first { argument -> argument.name == "material" }.valueToken).key,
        )
        assertEquals(
            "item.display.part.suffix",
            requireNotNull(displayToken.arguments.first { argument -> argument.name == "suffix1" }.valueToken).key,
        )
    }

    @Test
    fun `emerald charm restores hp at turn start and emits passive log`() {
        val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        val inventory = requireNotNull(world.get<com.ktome.core.item.Inventory>(playerId))
        val equipment = requireNotNull(world.get<com.ktome.core.item.Equipment>(playerId))
        val itemId =
            ItemFactory().createCarriedItem(
                world = world,
                item =
                    ItemInstance(
                        baseId = "emerald_charm",
                        name = "Emerald Charm",
                        type = ItemType.ARMOR,
                        slot = EquipSlot.OFF_HAND,
                        glyph = ']',
                        colorHex = "#4F8F6B",
                        quality = ItemQuality.COMMON,
                        stats = StatModifier(wil = 1),
                        passive = EquipmentPassive.HpRegenPerTurn(amount = 2),
                    ),
            )
        inventory.itemIds += itemId
        equipment.slots[EquipSlot.OFF_HAND] = itemId
        requireNotNull(world.get<Health>(playerId)).current = 10

        val session = session(world, map, playerId)

        assertTrue(session.perform(PlayerCommand.Wait))
        assertEquals(14, requireNotNull(runtimeWorld(session).get<Health>(playerId)).current)
        assertTrue(session.messageLog().any { message -> message.contains("Emerald Charm restores 2 HP") })
    }

    @Test
    fun `bandit trophy logs bonus damage against bandit tagged target`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260322L, zoneId = "shattered_outpost", playerProfessionId = "rogue"),
                SaveManager(tempDir.resolve("bandit-trophy-save")),
            )
        clearMonsters(session)
        val world = runtimeWorld(session)
        requireNotNull(world.get<Stats>(session.playerId)).dex = 40
        world.add(
            session.playerId,
            requireNotNull(world.get<CombatProfile>(session.playerId)).copy(baseAccuracy = 120),
        )
        StatsCalculator.recalculateAndStore(world, session.playerId)
        val banditId =
            EntityFactory().createMonster(
                world = world,
                template = dataLoader.loadMonsterCatalog().monsters.first { monster -> monster.id == "bandit.sentry" },
                position = findOpenAdjacentPoint(session, session.playerPosition()),
            )
        world.remove<AIBehavior>(banditId)
        val trophyIndex = addInventoryItem(session, baseItem("bandit_trophy"))
        assertTrue(session.perform(PlayerCommand.ActivateInventoryItem(trophyIndex)))
        val banditPoint = requireNotNull(world.get<Position>(banditId)).toPoint()
        val attackOrigin = findOpenAdjacentPoint(session, banditPoint)
        movePlayerTo(session, attackOrigin)

        assertTrue(session.perform(PlayerCommand.Move(banditPoint - attackOrigin)))
        assertNotNull(logEventByKey(session, "log.passive.damage_bonus_vs_tag"))
    }

    @Test
    fun `furnace talisman logs fire damage bonus on fire talents`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260322L, zoneId = "deep_iron_pit", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("furnace-talisman-save")),
            )
        clearMonsters(session)
        val world = runtimeWorld(session)
        requireNotNull(world.get<Stats>(session.playerId)).dex = 40
        world.add(
            session.playerId,
            requireNotNull(world.get<CombatProfile>(session.playerId)).copy(baseAccuracy = 120),
        )
        StatsCalculator.recalculateAndStore(world, session.playerId)
        val fireResistant =
            EntityFactory().createMonster(
                world = world,
                template =
                    dataLoader.loadMonsterCatalog().monsters.first { monster ->
                        (monster.resistances[DamageType.FIRE] ?: 0) > 0
                    },
                position = findOpenAdjacentPoint(session, session.playerPosition()),
            )
        world.remove<AIBehavior>(fireResistant)
        requireNotNull(world.get<Stats>(fireResistant)).apply {
            str = 4
            dex = 0
            con = 4
            wil = 1
        }
        world.add(
            fireResistant,
            CombatProfile(
                baseAttack = 4,
                baseDefense = 0,
                baseAccuracy = 0,
                baseEvasion = 0,
                baseHp = 200,
            ),
        )
        StatsCalculator.recalculateAndStore(world, fireResistant)
        requireNotNull(world.get<Health>(fireResistant)).apply {
            current = 200
            max = 200
        }
        val talismanIndex = addInventoryItem(session, baseItem("furnace_talisman"))
        assertTrue(session.perform(PlayerCommand.ActivateInventoryItem(talismanIndex)))
        val fireballSlot = session.talentSlots().first { slot -> slot.talentId == "fireball" }.slot
        val targetPoint = requireNotNull(world.get<Position>(fireResistant)).toPoint()

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = fireballSlot, target = targetPoint)))
        assertNotNull(logEventByKey(session, "log.passive.damage_bonus_type"))
    }

    @Test
    fun `seal reliquary syncs shadow resistance onto player`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260322L, zoneId = "grey_gate_depths", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("seal-reliquary-save")),
            )
        val reliquaryIndex = addInventoryItem(session, baseItem("seal_reliquary"))

        assertTrue(session.perform(PlayerCommand.ActivateInventoryItem(reliquaryIndex)))

        val world = runtimeWorld(session)
        val resistanceProfile = requireNotNull(world.get<ResistanceProfile>(session.playerId))
        assertEquals(10, resistanceProfile.valueFor(DamageType.SHADOW))
    }

    @Test
    fun `interact opens supply crate drops reward and removes interactable`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("interact-crate-save")),
            )
        val cratePoint =
            session.renderSnapshot().props
                .first { prop -> prop.propTypeId == "supply_crate" }
                .let { prop -> Point(prop.x, prop.y) }

        session.automationMovePlayerTo(cratePoint)

        assertTrue(session.perform(PlayerCommand.Interact))

        val world = session.automationWorld()
        val groundItems =
            world.entitiesWith(Position::class, ItemInstance::class)
                .filter { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() == cratePoint }
                .mapNotNull { entityId -> world.get<ItemInstance>(entityId)?.baseId }
        val remainingInteractables =
            world.entitiesWith(Position::class, Interactable::class)
                .filter { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() == cratePoint }

        assertTrue("scroll_teleport" in groundItems || "healing_potion" in groundItems)
        assertTrue(remainingInteractables.isEmpty())
        assertTrue(session.messageLog().any { message -> message.contains("补给箱") || message.contains("Supply Crate") })
    }

    @Test
    fun `interact alarm bonfire escalates floor pressure and records objective progress`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("interact-bonfire-save")),
            )
        val bonfirePoint = interactablePoint(session, "alarm_bonfire")
        val world = session.automationWorld()
        val baselineMonsterCount = world.entitiesWith(MonsterTemplateId::class).size
        val baselineBehaviors =
            world.entitiesWith(AIBehavior::class)
                .associateWith { entityId -> requireNotNull(world.get<AIBehavior>(entityId)).copy() }

        session.automationMovePlayerTo(bonfirePoint)

        assertTrue(session.perform(PlayerCommand.Interact))

        val nextMonsterCount = world.entitiesWith(MonsterTemplateId::class).size
        val alertedBehaviors =
            baselineBehaviors.entries.count { (entityId, behavior) ->
                world.get<AIBehavior>(entityId)?.let { updated ->
                    updated.sightRadius > behavior.sightRadius || (behavior.type == AIType.PATROL && updated.type == AIType.CHASE)
                } == true
            }
        val remainingInteractables =
            world.entitiesWith(Position::class, Interactable::class)
                .filter { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() == bonfirePoint }

        assertEquals(baselineMonsterCount + 1, nextMonsterCount)
        assertTrue(alertedBehaviors > 0)
        assertTrue(remainingInteractables.isEmpty())
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.objective.progress" })
    }

    @Test
    fun `interact armory gate on floor two grants support reward and resupply`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("interact-gate-save")),
            )

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        val gatePoint = interactablePoint(session, "armory_gate")
        val baselineInventoryCount = session.inventoryItems().size
        val world = session.automationWorld()
        requireNotNull(requireNotNull(world.get<ResourcePools>(session.playerId)).pool(ResourceType.MANA)).current = 5

        session.automationMovePlayerTo(gatePoint)

        assertTrue(session.perform(PlayerCommand.Interact))
        assertTrue(session.inventoryItems().size >= baselineInventoryCount + 1)
        assertTrue(session.playerResourceView().current > 5)
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.objective.progress" })
        val rewardSummary =
            requireNotNull(
                session.milestoneRewardSummaries().firstOrNull { reward ->
                    reward.rewardSource == MilestoneRewardSource.CACHE &&
                        reward.sourceId == "armory_gate" &&
                        reward.qualityTier.ordinal >= ItemQuality.MAGIC.ordinal &&
                        reward.affixIds.isNotEmpty()
                },
            )
        assertEquals(EquipSlot.OFF_HAND, requireNotNull(itemBasesById[rewardSummary.baseItemId]).slot)
        assertEquals(EquipSlot.OFF_HAND, rewardSummary.equipSlot)
        assertNull(rewardSummary.equippedBaseItemIdBeforeReward)
        assertFalse(rewardSummary.buildHashAtGrant.isBlank())
        assertNull(rewardSummary.equippedBaseItemIdAtRunEnd)
        assertFalse(rewardSummary.adoptedInFinalBuild)
        val rewardInventoryIndex = inventoryIndexOfBaseId(session, rewardSummary.baseItemId)
        assertTrue(session.perform(PlayerCommand.ActivateInventoryItem(rewardInventoryIndex)))
        val adoptedSummary =
            requireNotNull(
                session.milestoneRewardSummaries().firstOrNull { reward ->
                    reward.rewardSource == MilestoneRewardSource.CACHE &&
                        reward.sourceId == "armory_gate"
                },
            )
        assertEquals(rewardSummary.baseItemId, adoptedSummary.equippedBaseItemIdAtRunEnd)
        assertTrue(adoptedSummary.adoptedInFinalBuild)
    }

    @Test
    fun `interact armory gate on floor two avoids duplicate vanguard starter gear rewards`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("interact-gate-vanguard-save")),
            )

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        val gatePoint = interactablePoint(session, "armory_gate")

        session.automationMovePlayerTo(gatePoint)

        assertTrue(session.perform(PlayerCommand.Interact))
        assertEquals(1, inventoryBaseIds(session).count { baseId -> baseId == "basic_shield" })
        assertEquals(1, inventoryBaseIds(session).count { baseId -> baseId == "chain_mail" })
        assertTrue(inventoryBaseIds(session).count { baseId -> baseId == "healing_potion" } >= 1)
        assertTrue(
            session.milestoneRewardSummaries().any { reward ->
                reward.rewardSource == MilestoneRewardSource.CACHE &&
                    reward.sourceId == "armory_gate" &&
                    reward.affixIds.isNotEmpty()
            },
        )
    }

    @Test
    fun `interact trail cache drops a greenwood reward and records objective progress`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "greenwood_fringe", playerProfessionId = "rogue"),
                SaveManager(tempDir.resolve("greenwood-trail-cache-save")),
            )
        val cachePoint = interactablePoint(session, "trail_cache")

        session.automationMovePlayerTo(cachePoint)

        assertTrue(session.perform(PlayerCommand.Interact))

        val world = session.automationWorld()
        val groundItems =
            world.entitiesWith(Position::class, ItemInstance::class)
                .filter { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() == cachePoint }
                .mapNotNull { entityId -> world.get<ItemInstance>(entityId)?.baseId }
                .toSet()

        assertTrue(groundItems.any { itemId -> itemId in setOf("bandit_trophy", "emerald_charm", "hunter_bow", "stamina_draught") })
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.objective.progress" })
        assertTrue(
            session.milestoneRewardSummaries().any { reward ->
                reward.rewardSource == MilestoneRewardSource.CACHE &&
                    reward.sourceId == "trail_cache" &&
                    reward.qualityTier.ordinal >= ItemQuality.MAGIC.ordinal &&
                    reward.affixIds.isNotEmpty()
            },
        )
    }

    @Test
    fun `interact mine furnace grants route support reward and restores stamina`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "deep_iron_pit", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("deep-iron-furnace-save")),
            )

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        val furnacePoint = interactablePoint(session, "mine_furnace")
        val baselineInventoryCount = session.inventoryItems().size
        StaminaPools.syncTo(session.automationWorld(), session.playerId, nextCurrent = 5, nextMax = StaminaPools.max(session.automationWorld(), session.playerId))

        session.automationMovePlayerTo(furnacePoint)

        assertTrue(session.perform(PlayerCommand.Interact))
        assertTrue(session.inventoryItems().size >= baselineInventoryCount + 1)
        assertTrue(session.playerResourceView().current > 5)
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.objective.progress" })
    }

    @Test
    fun `mana potion restores arcanist mana from starter kit`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("mana-potion-save")),
            )
        val world = session.automationWorld()
        requireNotNull(requireNotNull(world.get<ResourcePools>(session.playerId)).pool(ResourceType.MANA)).current = 10
        val potionIndex = session.inventoryItems().indexOfFirst { item -> item.name == "法力药水" }

        assertTrue(potionIndex >= 0)
        assertTrue(session.perform(PlayerCommand.ActivateInventoryItem(potionIndex)))
        assertEquals(42, session.playerResourceView().current)
    }

    @Test
    fun `using arcanist talent consumes mana from resource pools`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("arcanist-talent-save")),
            )
        clearMonsters(session)
        val dummyId = installCombatDummy(session)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        val fireballSlot = session.talentSlots().first { slot -> slot.talentId == "fireball" }.slot
        requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.MANA)).current = 20

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = fireballSlot, target = dummyPosition)))
        assertEquals(14, session.playerResourceView().current)
        assertEquals(14, session.renderSnapshot().uiState.playerStatus.currentResource)
    }

    @Test
    fun `elemental talent logs resisted feedback for elemental resistance hits`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("arcanist-resistant-log-save")),
            )
        clearMonsters(session)
        val dummyId =
            installCombatDummy(
                session = session,
                id = "resistant_dummy",
                resistances = mapOf(DamageType.FIRE to 20),
            )
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        val fireballSlot = session.talentSlots().first { slot -> slot.talentId == "fireball" }.slot
        requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.MANA)).current = 20

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = fireballSlot, target = dummyPosition)))

        val feedback = requireNotNull(logEventByKey(session, "log.talent.damage_resisted"))
        assertEquals("20", feedback.message.arguments.first { argument -> argument.name == "amount" }.value)
        assertEquals(
            "damage_type.fire.name",
            feedback.message.arguments.first { argument -> argument.name == "damageType" }.valueKey,
        )
    }

    @Test
    fun `elemental talent logs vulnerability feedback for elemental weakness hits`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("arcanist-vulnerable-log-save")),
            )
        clearMonsters(session)
        val dummyId =
            installCombatDummy(
                session = session,
                id = "vulnerable_dummy",
                resistances = mapOf(DamageType.FIRE to -15),
            )
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        val fireballSlot = session.talentSlots().first { slot -> slot.talentId == "fireball" }.slot
        requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.MANA)).current = 20

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = fireballSlot, target = dummyPosition)))

        val feedback = requireNotNull(logEventByKey(session, "log.talent.damage_vulnerable"))
        assertEquals("15", feedback.message.arguments.first { argument -> argument.name == "amount" }.value)
        assertEquals(
            "damage_type.fire.name",
            feedback.message.arguments.first { argument -> argument.name == "damageType" }.valueKey,
        )
    }

    @Test
    fun `using rogue talent consumes energy from resource pools`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "rogue"),
                SaveManager(tempDir.resolve("rogue-talent-save")),
            )
        clearMonsters(session)
        val rollSlot = session.talentSlots().first { slot -> slot.talentId == "roll" }.slot
        val initialStamina =
            requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.STAMINA)).current
        requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.ENERGY)).current = 30
        val target = Point(session.playerPosition().x + 2, session.playerPosition().y)

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = rollSlot, target = target)))
        assertFalse(session.playerResourceView().current == 30)
        assertEquals(
            initialStamina,
            requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.STAMINA)).current,
        )
    }

    @Test
    fun `rogue successful hit logs hit driven energy restore`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "rogue"),
                SaveManager(tempDir.resolve("rogue-hit-restore-save")),
            )
        clearMonsters(session)
        val dummyId = installCombatDummy(session)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        val backstabSlot = session.talentSlots().first { slot -> slot.talentId == "backstab" }.slot
        requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.ENERGY)).current = 20

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = backstabSlot, target = dummyPosition)))
        val feedback = requireNotNull(logEventByKey(session, "log.talent.resource_restore"))
        val arguments = feedback.message.arguments.associateBy { argument -> argument.name }
        assertEquals("ui.hud.energy.short", arguments.getValue("resource").valueKey)
        assertEquals(null, arguments.getValue("resource").value)
    }

    @Test
    fun `holy light heals templar and emits heal log`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "templar"),
                SaveManager(tempDir.resolve("templar-heal-save")),
            )
        clearMonsters(session)
        val holyLightSlot = session.talentSlots().first { slot -> slot.talentId == "holy_light" }.slot
        requireNotNull(runtimeWorld(session).get<Health>(session.playerId)).current = 20
        requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.POSITIVE_ENERGY)).current = 30

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = holyLightSlot)))
        assertTrue(session.playerStatus().currentHp > 20)
        assertTrue(session.renderSnapshot().logEvents.any { event -> event.message.key == "log.talent.heal" })
    }

    @Test
    fun `mana gated talent fails without spending cooldown or health on target`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                SaveManager(tempDir.resolve("arcanist-no-mana-save")),
            )
        clearMonsters(session)
        val dummyId = installCombatDummy(session)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        val fireballSlot = session.talentSlots().first { slot -> slot.talentId == "fireball" }.slot
        requireNotNull(requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.MANA)).current = 5
        val baselineHp = monsterHp(session, dummyId)

        assertFalse(session.perform(PlayerCommand.UseTalent(slot = fireballSlot, target = dummyPosition)))
        assertEquals(7, session.playerResourceView().current)
        assertEquals(baselineHp, monsterHp(session, dummyId))
        assertEquals(0, playerCooldown(session, "fireball"))
    }

    @Test
    fun `using a talent consumes stamina and starts cooldown`() {
        val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        factory.createMonster(
            world = world,
            template =
                MonsterTemplate(
                    id = "dummy",
                    name = "Dummy",
                    glyph = 'd',
                    colorHex = "#AAAAAA",
                    stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 0, wil = 0),
                    baseHp = 40,
                    baseAttack = 1,
                    baseDefense = 0,
                    speed = 90,
                    ai = AIType.CHASE,
                    expReward = 10,
                    spawnFloors = listOf(1),
                    spawnWeight = 1,
                ),
            position = Point(2, 1),
        )
        val session = session(world, map, playerId)

        val consumed = session.perform(PlayerCommand.UseTalent(slot = 1, target = Point(2, 1)))

        assertTrue(consumed)
        assertTrue(session.playerResourceView().current < session.playerResourceView().max)
        assertEquals(2, session.talentSlots().first { it.slot == 1 }.currentCooldown)
    }

    @Test
    fun `stunned status uses the exact formal icon key in render snapshots`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("status-icon-save")),
            )
        clearMonsters(session)
        val monsterId = installCombatDummy(session)
        val shieldBashSlot = session.talentSlots().first { it.talentId == "shield_bash" }.slot

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = shieldBashSlot, target = requireNotNull(runtimeWorld(session).get<Position>(monsterId)).toPoint())))

        val stunnedEffect =
            session.renderSnapshot().actors
                .first { it.entityId == monsterId.value }
                .statusEffects
                .first { it.typeId == StatusEffectType.STUN.schemaId }
        assertEquals("icon.status.stunned", stunnedEffect.iconKey)
    }

    @Test
    fun `stacked armor break is exposed through render snapshot status metadata`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("armor-break-status-save")),
            )
        clearMonsters(session)
        val monsterId = installCombatDummy(session, id = "armor_dummy")
        val tracker = requireNotNull(runtimeWorld(session).get<EffectTracker>(monsterId))
        repeat(2) { index ->
            StatusLifecycle.applyEffect(
                tracker,
                StatusLifecycle.createInstance(
                    type = StatusEffectType.ARMOR_BREAK,
                    effectId = "armor_break_$index",
                    duration = 3,
                ),
            )
        }
        StatsCalculator.recalculateAndStore(runtimeWorld(session), monsterId)

        val armorBreak =
            session.renderSnapshot().actors
                .first { actor -> actor.entityId == monsterId.value }
                .statusEffects
                .first { effect -> effect.typeId == StatusEffectType.ARMOR_BREAK.schemaId }

        assertEquals(2, armorBreak.stackCount)
        assertEquals(3, armorBreak.stackCap)
    }

    @Test
    fun `bleed ticks before stunned monster skips its turn`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("bleed-stun-save")),
            )
        clearMonsters(session)
        val monsterId = installCombatDummy(session, id = "bleed_dummy")
        val tracker = requireNotNull(runtimeWorld(session).get<EffectTracker>(monsterId))
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.BLEED,
                effectId = "bleed",
                duration = 2,
                sourceEntityId = session.playerId,
                tickDamageOverride = 4,
            ),
        )
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.STUN,
                effectId = "stun",
                duration = 1,
                sourceEntityId = session.playerId,
            ),
        )
        StatsCalculator.recalculateAndStore(runtimeWorld(session), monsterId)
        val before = requireNotNull(runtimeWorld(session).get<Health>(monsterId)).current

        var after = before
        repeat(3) {
            if (after < before) {
                return@repeat
            }
            assertTrue(session.perform(PlayerCommand.Wait))
            after = requireNotNull(runtimeWorld(session).get<Health>(monsterId)).current
        }
        assertTrue(after < before)
    }

    @Test
    fun `all actor layer dots resolve before death and preserve original killer`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("dot-layer-death-save")),
            )
        clearMonsters(session)
        val monsterId = installCombatDummy(session, id = "dot_layer_dummy")
        val world = runtimeWorld(session)
        val tracker = requireNotNull(world.get<EffectTracker>(monsterId))
        requireNotNull(world.get<Health>(monsterId)).current = 2
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.BLEED,
                effectId = "bleed",
                duration = 1,
                sourceEntityId = session.playerId,
                tickDamageOverride = 2,
            ),
        )
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.POISON,
                effectId = "poison",
                duration = 1,
                sourceEntityId = session.playerId,
                tickDamageOverride = 2,
            ),
        )
        StatsCalculator.recalculateAndStore(world, monsterId)

        repeat(5) {
            if (!world.isAlive(monsterId)) {
                return@repeat
            }
            assertTrue(session.perform(PlayerCommand.Wait))
        }

        assertFalse(world.isAlive(monsterId))
        val recentEvents = recentEventSummaries(session)
        assertEquals(2, recentEvents.count { event -> event.startsWith("status_tick:${monsterId.value}:") })
        assertTrue(recentEvents.contains("death:${monsterId.value}:${session.playerId.value}"))
    }

    @Test
    fun `area and world carrier effects decay after the affected actor turn`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("carrier-decay-save")),
            )
        clearMonsters(session)
        val monsterId = installCombatDummy(session, id = "carrier_decay_dummy")
        val world = runtimeWorld(session)
        val areaEntity = world.createEntity()
        val worldEntity = world.createEntity()
        world.add(
            areaEntity,
            AreaEffectEmitter(
                emitterId = "poison_cloud",
                sourceEntityId = session.playerId,
                affectedActorIds = setOf(monsterId),
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.POISON,
                            effectId = "poison_area",
                            duration = 1,
                            sourceEntityId = session.playerId,
                            tickDamageOverride = 1,
                        ),
                    ),
            ),
        )
        world.add(
            worldEntity,
            WorldEffect(
                effectId = "arena_aura",
                affectedActorIds = setOf(monsterId),
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.BURN,
                            effectId = "burn_world",
                            duration = 1,
                            tickDamageOverride = 1,
                        ),
                    ),
            ),
        )

        repeat(5) {
            if (requireNotNull(world.get<AreaEffectEmitter>(areaEntity)).effects.isEmpty() &&
                requireNotNull(world.get<WorldEffect>(worldEntity)).effects.isEmpty()
            ) {
                return@repeat
            }
            assertTrue(session.perform(PlayerCommand.Wait))
        }

        assertTrue(requireNotNull(world.get<AreaEffectEmitter>(areaEntity)).effects.isEmpty())
        assertTrue(requireNotNull(world.get<WorldEffect>(worldEntity)).effects.isEmpty())
    }

    @Test
    fun `lethal world carrier tick without source still handles death`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("world-dot-no-source-death-save")),
            )
        clearMonsters(session)
        val monsterId = installCombatDummy(session, id = "world_dot_no_source_dummy")
        val world = runtimeWorld(session)
        val worldEntity = world.createEntity()
        requireNotNull(world.get<Health>(monsterId)).current = 1
        world.add(
            worldEntity,
            WorldEffect(
                effectId = "hazard_no_source",
                affectedActorIds = setOf(monsterId),
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.BURN,
                            effectId = "burn_world_no_source",
                            duration = 1,
                            tickDamageOverride = 2,
                        ),
                    ),
            ),
        )

        repeat(5) {
            if (!world.isAlive(monsterId)) {
                return@repeat
            }
            assertTrue(session.perform(PlayerCommand.Wait))
        }

        assertFalse(world.isAlive(monsterId))
        val recentEvents = recentEventSummaries(session)
        assertTrue(recentEvents.any { event -> event == "death:${monsterId.value}:${monsterId.value}" })
    }

    @Test
    fun `killing common monster drops ground loot from its loot profile`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("common-loot-save")),
            )

        val world = runtimeWorld(session)
        val monsterId =
            requireNotNull(
                entityByTemplateId(session, "beast.rat")
                    ?: entityByTemplateId(session, "beast.rat_scavenger")
                    ?: entityByTemplateId(session, "bandit.sentry"),
            )
        requireNotNull(world.get<Health>(monsterId)).current = 1
        val deathPoint = requireNotNull(world.get<Position>(monsterId)).toPoint()
        val attackOrigin = findOpenAdjacentPoint(session, deathPoint)
        movePlayerTo(session, attackOrigin)

        assertTrue(session.perform(PlayerCommand.Move(deathPoint - attackOrigin)))
        assertTrue(groundItemBaseIdsAt(session, deathPoint).any { baseId -> baseId in setOf("healing_potion", "short_sword", "leather_armor") })
        assertTrue(session.messageLog().any { message -> message.contains("drops") || message.contains("掉落") })
    }

    @Test
    fun `killing elite monster drops ground loot from elite profile`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("elite-loot-save")),
            )

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        clearMonsters(session)

        val world = runtimeWorld(session)
        val elitePosition = findOpenAdjacentPoint(session, session.playerPosition())
        val monsterId =
            EntityFactory().createMonster(
                world = world,
                template = dataLoader.loadMonsterCatalog().monsters.first { monster -> monster.id == "undead.bone_guard" },
                position = elitePosition,
            )
        world.remove<AIBehavior>(monsterId)
        requireNotNull(world.get<Health>(monsterId)).current = 1
        val deathPoint = requireNotNull(world.get<Position>(monsterId)).toPoint()
        val attackOrigin = findOpenAdjacentPoint(session, deathPoint)
        movePlayerTo(session, attackOrigin)

        assertTrue(session.perform(PlayerCommand.Move(deathPoint - attackOrigin)))
        assertTrue(
            groundItemBaseIdsAt(session, deathPoint).any { baseId ->
                baseId in setOf("basic_shield", "mana_potion", "chain_mail", "apprentice_robe", "long_sword")
            },
        )
        val droppedItems = groundItemsAt(session, deathPoint)
        assertTrue(droppedItems.any { item -> item.quality != ItemQuality.COMMON })
        val dropLog = requireNotNull(logEventByKey(session, "log.loot.monster_drop_quality"))
        assertTrue(
            dropLog.message.arguments.any { argument ->
                argument.name == "quality" && argument.valueKey in setOf("item.quality.magic", "item.quality.rare")
            },
        )
    }

    @Test
    fun `self applied war cry keeps full duration after player turn ends`() {
        val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        val session = session(world, map, playerId)

        val consumed = session.perform(PlayerCommand.UseTalent(slot = 4))
        val runtimeWorld = runtimeWorld(session)

        assertTrue(consumed)
        val effect =
            requireNotNull(runtimeWorld.get<com.ktome.core.talent.EffectTracker>(playerId))
                .effects
                .single { it.schemaId == "war_cry_empower" }
        assertEquals(5, effect.remainingTurns)
        assertTrue(recentEventSummaries(session).any { summary -> summary == "status_apply:${playerId.value}:war_cry_empower:5" })
    }

    @Test
    fun `respec draft only rewinds the selected owner tree when profession and race talents coexist`() {
        val session = mixedOwnerRespecSession(tempDir.resolve("mixed-owner-respec-save"))

        assertTrue(
            session.perform(
                PlayerCommand.RespecTalentTree(
                    ownerType = TalentTreeOwnerType.PROFESSION,
                    treeOwnerId = "vanguard",
                ),
            ),
        )

        val draft = requireNotNull(runtimeWorld(session).get<TalentAllocationDraft>(session.playerId))
        assertEquals(TalentTreeOwnerType.PROFESSION, draft.ownerType)
        assertEquals("vanguard", draft.treeOwnerId)
        assertEquals(mapOf("power_strike" to 1), draft.pendingRanks)
        assertFalse("moon_blessing" in draft.pendingRanks)

        assertTrue(session.perform(PlayerCommand.ConfirmTalentDraft))

        val loadout = requireNotNull(runtimeWorld(session).get<com.ktome.core.talent.TalentLoadout>(session.playerId))
        assertEquals(1, loadout.levelOf("power_strike"))
        assertEquals(4, loadout.levelOf("moon_blessing"))
    }

    @Test
    fun `descending stairs advances floor and auto saves`() {
        val saveManager = SaveManager(tempDir.resolve("floor-save"))
        val session = GameModule.newFoundationSession(saveManager = saveManager)

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))

        assertTrue(session.perform(PlayerCommand.Descend))
        assertEquals(2, session.currentFloor())
        assertTrue(saveManager.hasSave())
    }

    @Test
    fun `manual save and continue restore floor and inventory`() {
        val saveManager = SaveManager(tempDir.resolve("load-save"))
        val session = GameModule.newFoundationSession(saveManager = saveManager)
        val world = runtimeWorld(session)
        ItemFactory().createGroundItem(
            world = world,
            item =
                ItemInstance(
                    baseId = "short_sword",
                    name = "短剑",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#C0C0C0",
                    stats = StatModifier(attack = 3),
                ),
            position = session.playerPosition(),
        )

        assertTrue(session.perform(PlayerCommand.PickUp))
        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        assertTrue(session.perform(PlayerCommand.SaveGame))

        val loaded = GameModule.loadFoundationSession(saveManager, com.ktome.game.i18n.GameLocale.ZH_CN)

        assertNotNull(loaded)
        assertEquals(2, loaded?.currentFloor())
        assertTrue(loaded?.inventoryItems()?.any { it.name == "短剑" } == true)
    }

    @Test
    fun `manual save preserves milestone reward summaries across reload`() {
        val saveManager = SaveManager(tempDir.resolve("milestone-reward-reload-save"))
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "arcanist"),
                saveManager,
            )

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        session.automationMovePlayerTo(interactablePoint(session, "armory_gate"))
        assertTrue(session.perform(PlayerCommand.Interact))

        val expectedMilestoneRewards = session.milestoneRewardSummaries()
        assertFalse(expectedMilestoneRewards.isEmpty())
        assertTrue(session.perform(PlayerCommand.SaveGame))

        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        assertEquals(expectedMilestoneRewards, loaded.milestoneRewardSummaries())

        loaded.automationForceDefeatPlayer()
        val summary = requireNotNull(loaded.profileRunSummary(finishedAtEpochMillis = 1234L))
        assertEquals(expectedMilestoneRewards, summary.milestoneRewards)
    }

    @Test
    fun `route transitions emit zone entry messages for each newly entered zone`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260322L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "rogue",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 1,
                    ),
                saveManager = SaveManager(tempDir.resolve("route-zone-enter-save")),
            )

        val initialZoneEnter = requireNotNull(logEventByKey(session, "log.zone.enter"))
        assertEquals("zone.greenwood_fringe.name", initialZoneEnter.message.arguments.first { argument -> argument.name == "zone" }.valueKey)
        assertEquals("zone.greenwood_fringe.desc", initialZoneEnter.message.arguments.first { argument -> argument.name == "desc" }.valueKey)

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val snapshot = session.renderSnapshot()
        val zoneEnterEvents = snapshot.logEvents.filter { event -> event.message.key == "log.zone.enter" }
        val latestZoneEnter = zoneEnterEvents.last()

        assertEquals("deep_iron_pit", snapshot.metadata.zoneId)
        assertEquals("zone.deep_iron_pit.desc", snapshot.metadata.zoneDescKey)
        assertTrue(snapshot.logEvents.any { event -> event.message.key == "log.route.advance" })
        assertEquals(1, zoneEnterEvents.count { event -> event.message.arguments.first { argument -> argument.name == "zone" }.valueKey == "zone.deep_iron_pit.name" })
        assertEquals("zone.deep_iron_pit.name", latestZoneEnter.message.arguments.first { argument -> argument.name == "zone" }.valueKey)
        assertEquals("zone.deep_iron_pit.desc", latestZoneEnter.message.arguments.first { argument -> argument.name == "desc" }.valueKey)
    }

    @Test
    fun `loading a save keeps zone metadata without replaying zone entry log`() {
        val saveManager = SaveManager(tempDir.resolve("zone-desc-load-save"))
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "greenwood_fringe", playerProfessionId = "rogue"),
                saveManager = saveManager,
            )

        assertTrue(session.perform(PlayerCommand.SaveGame))

        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val snapshot = loaded.renderSnapshot()

        assertEquals("greenwood_fringe", snapshot.metadata.zoneId)
        assertEquals("zone.greenwood_fringe.desc", snapshot.metadata.zoneDescKey)
        assertEquals("log.session.loaded", snapshot.logEvents.firstOrNull()?.message?.key)
        assertFalse(snapshot.logEvents.any { event -> event.message.key == "log.zone.enter" })
    }

    @Test
    fun `repeated zone ids are allowed when the route follows the world graph`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260322L,
                        zoneId = "shattered_outpost",
                        playerProfessionId = "vanguard",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe", "shattered_outpost"),
                        routeIndex = 0,
                    ),
                saveManager = SaveManager(tempDir.resolve("duplicate-route-save")),
            )

        assertEquals(listOf("shattered_outpost", "greenwood_fringe", "shattered_outpost"), session.config.zoneRoute)
    }

    @Test
    fun `zone entry logs do not pollute run summary last events`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("zone-summary-save")),
            )

        session.automationForceDefeatPlayer()
        val summary = requireNotNull(session.outcomeSummary())

        assertFalse(summary.lastEvents.any { event -> event.key == "log.zone.enter" })
        assertTrue(summary.lastEvents.any { event -> event.key == "log.player.death" })
    }

    @Test
    fun `profile run summary uses frozen core contract fields`() {
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260322L,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "rogue",
                        playerRaceId = "elf",
                        zoneRoute = listOf("shattered_outpost", "greenwood_fringe"),
                        routeIndex = 1,
                    ),
                saveManager = SaveManager(tempDir.resolve("profile-run-summary-save")),
            )

        session.automationForceDefeatPlayer()
        val summary = requireNotNull(session.profileRunSummary(finishedAtEpochMillis = 1234L))

        assertEquals(20260322L, summary.seed)
        assertEquals(1234L, summary.finishedAtEpochMillis)
        assertEquals("rogue", summary.classId)
        assertEquals("elf", summary.raceId)
        assertEquals("greenwood_fringe", summary.finalZoneId)
        assertEquals(zoneRouteHash(listOf("shattered_outpost", "greenwood_fringe")), summary.zoneRouteHash)
        assertEquals(listOf("shattered_outpost", "greenwood_fringe"), summary.zonePath)
        assertFalse(summary.buildHash.isBlank())
        assertEquals(CombatRuleset.RULESET_VERSION, summary.rulesetVersion)
        assertFalse(summary.victory)
        assertEquals(session.runOutcome().toString(), summary.defeatReason)
    }

    @Test
    fun `manual save preserves prepared player turn state across load`() {
        val saveManager = SaveManager(tempDir.resolve("prepared-turn-save"))
        val baseline = GameModule.newFoundationSession(saveManager = SaveManager(tempDir.resolve("prepared-turn-baseline")))
        val persisted = GameModule.newFoundationSession(saveManager = saveManager)
        clearMonsters(baseline)
        clearMonsters(persisted)
        primePlayerTurnState(baseline)
        primePlayerTurnState(persisted)

        assertTrue(persisted.perform(PlayerCommand.SaveGame))
        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))

        assertTrue(baseline.perform(PlayerCommand.Wait))
        assertTrue(loaded.perform(PlayerCommand.Wait))

        assertEquals(baseline.playerResourceView().current, loaded.playerResourceView().current)
        assertEquals(playerCooldown(baseline, "power_strike"), playerCooldown(loaded, "power_strike"))
    }

    @Test
    fun `manual save preserves stamina component pool and hud in lockstep`() {
        val saveManager = SaveManager(tempDir.resolve("stamina-lockstep-save"))
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager,
            )
        clearMonsters(session)
        val dummyId = installCombatDummy(session)
        val dummyPosition = requireNotNull(runtimeWorld(session).get<Position>(dummyId)).toPoint()
        val powerStrikeSlot = session.talentSlots().first { slot -> slot.talentId == "power_strike" }.slot

        assertTrue(session.perform(PlayerCommand.UseTalent(slot = powerStrikeSlot, target = dummyPosition)))
        val liveStamina = session.playerResourceView().current
        val livePool =
            requireNotNull(
                requireNotNull(runtimeWorld(session).get<ResourcePools>(session.playerId)).pool(ResourceType.STAMINA),
            ).current
        val liveHud = session.renderSnapshot().uiState.playerStatus.currentResource

        assertEquals(liveStamina, livePool)
        assertEquals(liveStamina, liveHud)
        assertEquals(liveStamina, session.playerResourceView().current)

        assertTrue(session.perform(PlayerCommand.SaveGame))
        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val loadedStamina = loaded.playerResourceView().current
        val loadedPool =
            requireNotNull(
                requireNotNull(runtimeWorld(loaded).get<ResourcePools>(loaded.playerId)).pool(ResourceType.STAMINA),
            ).current
        val loadedHud = loaded.renderSnapshot().uiState.playerStatus.currentResource

        assertEquals(liveStamina, loadedStamina)
        assertEquals(loadedStamina, loadedPool)
        assertEquals(loadedStamina, loadedHud)
        assertEquals(loadedStamina, loaded.playerResourceView().current)
    }

    @Test
    fun `load normalizes invalid enum tokens to save restore exception`() {
        val saveManager = SaveManager(tempDir.resolve("invalid-enum-save"))
        val session = GameModule.newFoundationSession(saveManager = saveManager)

        assertTrue(session.perform(PlayerCommand.SaveGame))
        Files.writeString(
            saveManager.savePath(),
            Files.readString(saveManager.savePath()).replace("\"PLAYER\"", "\"NOT_A_FACTION\""),
        )

        val exception =
            assertThrows(SaveRestoreException::class.java) {
                GameModule.loadFoundationSession(saveManager)
            }

        assertTrue(exception.message!!.contains("faction"))
    }

    @Test
    fun `descending auto save captures post turn state instead of pre commit snapshot`() {
        val saveManager = SaveManager(tempDir.resolve("checkpoint-save"))
        val session = GameModule.newFoundationSession(saveManager = saveManager)
        requireNotNull(runtimeWorld(session).get<EffectTracker>(session.playerId)).effects +=
            StatusLifecycle.createInstance(
                type = StatusEffectType.GUARD_STANCE_BUFF,
                effectId = "checkpoint_buff",
                duration = 3,
                statModifierOverride = StatModifier(attack = 1),
            )

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))

        assertTrue(session.perform(PlayerCommand.Descend))
        val liveEffect =
            requireNotNull(runtimeWorld(session).get<EffectTracker>(session.playerId))
                .effects
                .single { effect -> effect.id == "checkpoint_buff" }
        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val restoredEffect =
            requireNotNull(runtimeWorld(loaded).get<EffectTracker>(loaded.playerId))
                .effects
                .single { effect -> effect.id == "checkpoint_buff" }

        assertEquals(2, liveEffect.remainingTurns)
        assertEquals(liveEffect.remainingTurns, restoredEffect.remainingTurns)
        assertEquals(session.currentFloor(), loaded.currentFloor())
        assertEquals(session.playerPosition(), loaded.playerPosition())
    }

    @Test
    fun `boss telegraph and phase runtime survive save load`() {
        val saveManager = SaveManager(tempDir.resolve("boss-trigger-save"))
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "grey_gate_depths", playerProfessionId = "vanguard"),
                saveManager = saveManager,
            )
        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val bossId = requireNotNull(entityByTemplateId(session, "cultist.dungeon_lord"))
        val world = runtimeWorld(session)
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        movePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
        val bossHealth = requireNotNull(world.get<Health>(bossId))
        bossHealth.current = bossHealth.max * 2 / 5

        advanceTurnsUntil(session, maxTurns = 4) {
            world.get<PendingTelegraphState>(bossId)?.telegraphSpecId == "dungeon_lord_phase_warning" &&
                world.get<BossEncounterState>(bossId)?.currentPhaseId == "phase_desperate"
        }
        val pendingTelegraph = requireNotNull(world.get<PendingTelegraphState>(bossId))
        val phaseState = requireNotNull(world.get<BossEncounterState>(bossId))
        val phaseTrace = session.recentBossTraces().last { trace -> trace.actorId == bossId.value }

        assertEquals("dungeon_lord_phase_warning", pendingTelegraph.telegraphSpecId)
        assertEquals("dungeon_lord_phase_warning", pendingTelegraph.sourceAbilityId)
        assertEquals("phase_desperate", phaseState.currentPhaseId)
        assertNotNull(logEventByKey(session, "log.boss.desperate"))
        assertEquals("phase_desperate", phaseTrace.toPhase)
        assertTrue(phaseTrace.sideEffects.contains("TELEGRAPH:dungeon_lord_phase_warning"))

        assertTrue(session.perform(PlayerCommand.SaveGame))
        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val loadedBossId = requireNotNull(entityByTemplateId(loaded, "cultist.dungeon_lord"))
        val loadedWorld = runtimeWorld(loaded)
        val loadedPendingTelegraph = requireNotNull(loadedWorld.get<PendingTelegraphState>(loadedBossId))
        val loadedPhaseState = requireNotNull(loadedWorld.get<BossEncounterState>(loadedBossId))

        assertEquals(pendingTelegraph.telegraphSpecId, loadedPendingTelegraph.telegraphSpecId)
        assertEquals(pendingTelegraph.sourceAbilityId, loadedPendingTelegraph.sourceAbilityId)
        assertEquals(pendingTelegraph.remainingTurns, loadedPendingTelegraph.remainingTurns)
        assertEquals(phaseState.currentPhaseId, loadedPhaseState.currentPhaseId)
        assertEquals(phaseState.encounterTurnCount, loadedPhaseState.encounterTurnCount)
    }

    @Test
    fun `boss phase entry trace is not replayed after losing sight and re engaging`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "grey_gate_depths", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-trigger-los-save")),
            )
        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val bossId = requireNotNull(entityByTemplateId(session, "cultist.dungeon_lord"))
        val world = runtimeWorld(session)
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        movePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
        requireNotNull(world.get<Health>(bossId)).current = requireNotNull(world.get<Health>(bossId)).max * 2 / 5

        advanceTurnsUntil(session, maxTurns = 4) {
            world.get<BossEncounterState>(bossId)?.currentPhaseId == "phase_desperate"
        }
        val transitionCount = session.recentBossTraces().count { trace -> trace.actorId == bossId.value && trace.toPhase == "phase_desperate" }
        assertEquals("phase_desperate", requireNotNull(world.get<BossEncounterState>(bossId)).currentPhaseId)

        movePlayerTo(session, findOpenPointAtDistance(session, center = bossPoint, distance = 13))

        assertTrue(session.perform(PlayerCommand.Wait))
        assertEquals("phase_desperate", requireNotNull(world.get<BossEncounterState>(bossId)).currentPhaseId)
        assertEquals(
            transitionCount,
            session.recentBossTraces().count { trace -> trace.actorId == bossId.value && trace.toPhase == "phase_desperate" },
        )

        movePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))

        assertTrue(session.perform(PlayerCommand.Wait))
        assertEquals(
            transitionCount,
            session.recentBossTraces().count { trace -> trace.actorId == bossId.value && trace.toPhase == "phase_desperate" },
        )
        assertEquals("phase_desperate", requireNotNull(world.get<BossEncounterState>(bossId)).currentPhaseId)
    }

    @Test
    fun `boss stealth does not create extra phase transitions before hp gate is met`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "grey_gate_depths", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-stealth-phase-gate")),
            )
        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val world = runtimeWorld(session)
        val bossId = requireNotNull(entityByTemplateId(session, "cultist.dungeon_lord"))
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        movePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
        requireNotNull(world.get<com.ktome.core.talent.CooldownState>(bossId)).remainingByTalentId.apply {
            this["war_cry"] = 99
            this["power_strike"] = 99
            this["charge"] = 99
        }

        repeat(3) {
            if (world.get<BossEncounterState>(bossId)?.currentPhaseId == "phase_full") {
                return@repeat
            }
            assertTrue(session.perform(PlayerCommand.Wait))
        }
        assertEquals("phase_full", requireNotNull(world.get<BossEncounterState>(bossId)).currentPhaseId)
        val desperateTraceCountBeforeStealth =
            session.recentBossTraces().count { trace -> trace.actorId == bossId.value && trace.toPhase == "phase_desperate" }

        StatusLifecycle.applyEffect(
            requireNotNull(world.get<EffectTracker>(session.playerId)),
            StatusLifecycle.createInstance(
                type = StatusEffectType.STEALTH,
                effectId = "boss_stealth_gate_test",
                duration = 10,
                sourceEntityId = session.playerId,
            ),
        )

        repeat(3) {
            assertTrue(session.perform(PlayerCommand.Wait))
        }

        assertEquals("phase_full", requireNotNull(world.get<BossEncounterState>(bossId)).currentPhaseId)
        assertEquals(
            desperateTraceCountBeforeStealth,
            session.recentBossTraces().count { trace -> trace.actorId == bossId.value && trace.toPhase == "phase_desperate" },
        )

        val bossHealth = requireNotNull(world.get<Health>(bossId))
        bossHealth.current = bossHealth.max * 2 / 5
        assertTrue(session.perform(PlayerCommand.Wait))

        assertEquals("phase_desperate", requireNotNull(world.get<BossEncounterState>(bossId)).currentPhaseId)
        assertEquals(
            desperateTraceCountBeforeStealth + 1,
            session.recentBossTraces().count { trace -> trace.actorId == bossId.value && trace.toPhase == "phase_desperate" },
        )
    }

    @Test
    fun `boss hp threshold transition posts message and records trace`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "grey_gate_depths", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-hp-trigger-save")),
            )
        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val world = runtimeWorld(session)
        val bossId = requireNotNull(entityByTemplateId(session, "cultist.dungeon_lord"))
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        movePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
        val bossHealth = requireNotNull(world.get<Health>(bossId))
        bossHealth.current = bossHealth.max * 2 / 5

        advanceTurnsUntil(session, maxTurns = 4) {
            world.get<PendingTelegraphState>(bossId)?.telegraphSpecId == "dungeon_lord_phase_warning" &&
                world.get<BossEncounterState>(bossId)?.currentPhaseId == "phase_desperate" &&
                session.recentBossTraces().any { recent -> recent.actorId == bossId.value && recent.toPhase == "phase_desperate" }
        }
        val trace = requireNotNull(session.recentBossTraces().lastOrNull { recent -> recent.actorId == bossId.value })
        assertNotNull(logEventByKey(session, "log.boss.desperate"))
        assertEquals("phase_desperate", requireNotNull(world.get<BossEncounterState>(bossId)).currentPhaseId)
        assertEquals("phase_desperate", trace.toPhase)
        assertEquals("hp_threshold", trace.trigger)
        assertTrue(trace.sideEffects.contains("TELEGRAPH:dungeon_lord_phase_warning"))
        assertEquals("dungeon_lord_phase_warning", requireNotNull(world.get<PendingTelegraphState>(bossId)).telegraphSpecId)
    }

    @Test
    fun `allow fatal transition phase prevents boss death and enters configured phase`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "deep_iron_pit", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-fatal-phase-transition")),
            )
        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val world = runtimeWorld(session)
        val bossId = requireNotNull(entityByTemplateId(session, "orc.molten_giant"))
        replaceContent(
            session,
            sessionContent(session).let { content ->
                val definition = requireNotNull(content.bossDefinitions["molten_giant_encounter"])
                val updatedEncounter =
                    BossEncounter(
                        id = definition.encounter.id,
                        templateId = definition.encounter.templateId,
                        phases =
                            definition.encounter.phases.map { phase ->
                                if (phase.id == "phase_enraged") {
                                    phase.copy(transitionTiming = BossPhaseTransitionTiming.ALLOW_FATAL_TRANSITION)
                                } else {
                                    phase
                                }
                            },
                    )
                content.copy(
                    bossDefinitions =
                        content.bossDefinitions +
                            ("molten_giant_encounter" to definition.copy(encounter = updatedEncounter)),
                )
            },
        )

        val bossState = requireNotNull(world.get<BossEncounterState>(bossId))
        bossState.currentPhaseId = "phase_full"
        val bossHealth = requireNotNull(world.get<Health>(bossId))
        bossHealth.current = 0

        invokeHandleDeath(session, bossId, session.playerId)

        assertTrue(world.isAlive(bossId))
        assertEquals("phase_enraged", bossState.currentPhaseId)
        assertEquals(1, bossHealth.current)
        assertEquals(
            "molten_giant_phase_warning",
            requireNotNull(world.get<PendingTelegraphState>(bossId)).telegraphSpecId,
        )
        assertTrue(
            session.recentBossTraces().any { trace ->
                trace.actorId == bossId.value &&
                    trace.toPhase == "phase_enraged" &&
                    "TELEGRAPH:molten_giant_phase_warning" in trace.sideEffects
            },
        )
    }

    @Test
    fun `start of turn phase change clears stale queued telegraph before it resolves`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "grey_gate_depths", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-phase-clears-stale-telegraph")),
            )
        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val world = runtimeWorld(session)
        val bossId = requireNotNull(entityByTemplateId(session, "cultist.dungeon_lord"))
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        movePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
        requireNotNull(world.get<com.ktome.core.talent.CooldownState>(bossId)).remainingByTalentId.apply {
            this["war_cry"] = 99
            this["power_strike"] = 0
            this["charge"] = 99
        }

        var queuedTelegraph: PendingTelegraphState? = null
        for (attempt in 0 until 6) {
            assertTrue(session.perform(PlayerCommand.Wait))
            val candidate = world.get<PendingTelegraphState>(bossId)
            if (candidate?.sourceAbilityId == "power_strike") {
                queuedTelegraph = candidate
                break
            }
        }
        queuedTelegraph = requireNotNull(queuedTelegraph)
        assertEquals("power_strike", queuedTelegraph.sourceAbilityId)

        val bossHealth = requireNotNull(world.get<Health>(bossId))
        bossHealth.current = bossHealth.max * 2 / 5

        assertTrue(session.perform(PlayerCommand.Wait))

        val replacementTelegraph = requireNotNull(world.get<PendingTelegraphState>(bossId))
        val trace = requireNotNull(session.recentBossTraces().lastOrNull { recent -> recent.actorId == bossId.value })
        assertEquals("phase_desperate", requireNotNull(world.get<BossEncounterState>(bossId)).currentPhaseId)
        assertEquals("dungeon_lord_phase_warning", replacementTelegraph.telegraphSpecId)
        assertEquals("dungeon_lord_phase_warning", replacementTelegraph.sourceAbilityId)
        assertNotEquals("power_strike", replacementTelegraph.sourceAbilityId)
        assertEquals("hp_threshold", trace.trigger)
        assertTrue(trace.sideEffects.contains("CLEAR_PENDING_TELEGRAPH"))
        assertTrue(trace.sideEffects.contains("TELEGRAPH:dungeon_lord_phase_warning"))
    }

    @Test
    fun `telegraph lifecycle clears after resolution for a live boss telegraph`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "grey_gate_depths", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("boss-opening-window-save")),
            )
        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val world = runtimeWorld(session)
        val bossId = requireNotNull(entityByTemplateId(session, "cultist.dungeon_lord"))
        val bossPoint = requireNotNull(world.get<Position>(bossId)).toPoint()
        movePlayerTo(session, findOpenAdjacentPoint(session, bossPoint))
        world.add(
            bossId,
            PendingTelegraphState(
                telegraphSpecId = "dungeon_lord_phase_warning",
                sourceAbilityId = "dungeon_lord_phase_warning",
                remainingTurns = 1,
                targetPoint = bossPoint,
                queuedAbilityId = null,
                resolvedDangerLevel = com.ktome.core.ai.DangerLevel.HIGH,
            ),
        )

        advanceTurnsUntil(session, maxTurns = 2) {
            world.get<PendingTelegraphState>(bossId) == null
        }
        assertNull(world.get<PendingTelegraphState>(bossId))
    }

    @Test
    fun `kite ai falls back to attack when retreat has no valid step`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("blocked-retreat-fallback")),
            )
        clearMonsters(session)
        val world = runtimeWorld(session)
        val playerPoint = session.playerPosition()
        val monsterPoint = findOpenAdjacentPoint(session, playerPoint)
        val monsterId =
            installAiMonster(
                session = session,
                id = "blocked_kiter",
                position = monsterPoint,
                aiProfileId = "ai.kite.basic",
                aiType = AIType.KITE,
            )

        Point.ALL_DIRECTIONS
            .map { delta -> monsterPoint + delta }
            .filter { point ->
                point != playerPoint &&
                    session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement
            }.forEach { point ->
                createTargetDummy(session, point)
            }

        val before = requireNotNull(world.get<Position>(monsterId)).toPoint()
        val playerHealthBefore = requireNotNull(world.get<Health>(session.playerId)).current
        var after = before
        repeat(3) {
            assertTrue(session.perform(PlayerCommand.Wait))
            after = requireNotNull(world.get<Position>(monsterId)).toPoint()
            if (
                logEventByKey(session, "log.attack.hit") != null ||
                logEventByKey(session, "log.attack.miss") != null ||
                logEventByKey(session, "log.attack.crit") != null ||
                requireNotNull(world.get<Health>(session.playerId)).current < playerHealthBefore
            ) {
                return@repeat
            }
        }

        assertEquals(before, after)
        assertTrue(
            logEventByKey(session, "log.attack.hit") != null ||
                logEventByKey(session, "log.attack.miss") != null ||
                logEventByKey(session, "log.attack.crit") != null ||
                requireNotNull(world.get<Health>(session.playerId)).current < playerHealthBefore,
        )
    }

    @Test
    fun `kite ai does not deal melee damage from preferred range`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("kite-range-guard")),
            )
        clearMonsters(session)
        val world = runtimeWorld(session)
        val monsterPoint = findOpenPointAtDistance(session, minDistance = 3, maxDistance = 3)
        installAiMonster(
            session = session,
            id = "range_locked_kiter",
            position = monsterPoint,
            aiProfileId = "ai.kite.basic",
            aiType = AIType.KITE,
        )

        val playerHealthBefore = requireNotNull(world.get<Health>(session.playerId)).current
        repeat(3) {
            assertTrue(session.perform(PlayerCommand.Wait))
        }

        assertEquals(playerHealthBefore, requireNotNull(world.get<Health>(session.playerId)).current)
        assertNull(logEventByKey(session, "log.attack.hit"))
        assertNull(logEventByKey(session, "log.attack.crit"))
        assertNull(logEventByKey(session, "log.attack.miss"))
    }

    @Test
    fun `grey gate first floor downstairs remains pathable from the player start`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260317L, zoneId = "grey_gate_depths", playerProfessionId = "templar"),
                saveManager = SaveManager(tempDir.resolve("grey-gate-floor-one-pathing")),
            )
        val stairsDown = requireNotNull(session.automationStairPoint(StairDirection.DOWN))

        val path =
            AStar.findPath(
                map = session.map,
                start = session.playerPosition(),
                goal = stairsDown,
            )

        assertTrue(path.isNotEmpty(), "Expected a static path from player start to grey gate downstairs.")
    }

    @Test
    fun `stealth hides player and ai walks to last known position`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("stealth-last-known-position")),
            )
        val world = runtimeWorld(session)
        clearMonsters(session)
        val playerStart = session.playerPosition()
        val monsterId =
            EntityFactory().createMonster(
                world = world,
                template = dataLoader.loadMonsterCatalog().monsters.first { monster -> monster.id == "beast.rat" },
                position = findOpenPointAtDistance(session, minDistance = 4, maxDistance = 4),
            )

        val perception = requireNotNull(world.get<AIPerceptionState>(monsterId))
        repeat(3) {
            if (perception.lastKnownTargetPosition == playerStart) {
                return@repeat
            }
            assertTrue(session.perform(PlayerCommand.Wait))
        }
        val beforeStealth = requireNotNull(world.get<Position>(monsterId)).toPoint()
        assertEquals(playerStart, perception.lastKnownTargetPosition)

        StatusLifecycle.applyEffect(
            requireNotNull(world.get<EffectTracker>(session.playerId)),
            StatusLifecycle.createInstance(
                type = StatusEffectType.STEALTH,
                effectId = "player_stealth_test",
                duration = 5,
                sourceEntityId = session.playerId,
            ),
        )
        val evadePoint = findOpenAdjacentPoint(session, playerStart)

        assertTrue(session.perform(PlayerCommand.Move(evadePoint - session.playerPosition())))
        var afterStealth = requireNotNull(world.get<Position>(monsterId)).toPoint()
        repeat(3) {
            if (afterStealth.chebyshevDistanceTo(playerStart) < beforeStealth.chebyshevDistanceTo(playerStart)) {
                return@repeat
            }
            assertTrue(session.perform(PlayerCommand.Wait))
            afterStealth = requireNotNull(world.get<Position>(monsterId)).toPoint()
        }

        assertTrue(afterStealth.chebyshevDistanceTo(playerStart) < beforeStealth.chebyshevDistanceTo(playerStart))
        assertEquals(playerStart, perception.lastKnownTargetPosition)

        repeat(5) {
            if (perception.lastKnownTargetPosition != null) {
                assertTrue(session.perform(PlayerCommand.Wait))
            }
        }

        assertNull(perception.lastKnownTargetPosition)
    }

    @Test
    fun `ai clears last known target and falls back after reaching it`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("stealth-last-known-fallback")),
            )
        val world = runtimeWorld(session)
        clearMonsters(session)
        val playerStart = session.playerPosition()
        val monsterId =
            EntityFactory().createMonster(
                world = world,
                template = dataLoader.loadMonsterCatalog().monsters.first { monster -> monster.id == "beast.rat" },
                position = findOpenPointAtDistance(session, minDistance = 4, maxDistance = 4),
            )

        val perception = requireNotNull(world.get<AIPerceptionState>(monsterId))
        repeat(3) {
            if (perception.lastKnownTargetPosition == playerStart) {
                return@repeat
            }
            assertTrue(session.perform(PlayerCommand.Wait))
        }
        assertEquals(playerStart, perception.lastKnownTargetPosition)

        StatusLifecycle.applyEffect(
            requireNotNull(world.get<EffectTracker>(session.playerId)),
            StatusLifecycle.createInstance(
                type = StatusEffectType.STEALTH,
                effectId = "player_stealth_fallback_test",
                duration = 10,
                sourceEntityId = session.playerId,
            ),
        )
        val hiddenDestination = findOpenPointAtDistance(session, center = playerStart, distance = 6)
        movePlayerTo(session, hiddenDestination)

        repeat(8) {
            if (perception.lastKnownTargetPosition == null) {
                return@repeat
            }
            assertTrue(session.perform(PlayerCommand.Wait))
        }

        val settledPosition = requireNotNull(world.get<Position>(monsterId)).toPoint()
        assertEquals(playerStart, settledPosition)
        assertNull(perception.lastKnownTargetPosition)

        assertTrue(session.perform(PlayerCommand.Wait))
        val afterFallback = requireNotNull(world.get<Position>(monsterId)).toPoint()
        assertEquals(settledPosition, afterFallback)
        assertTrue(afterFallback.chebyshevDistanceTo(hiddenDestination) >= settledPosition.chebyshevDistanceTo(hiddenDestination))
    }

    @Test
    fun `taunt forces ai toward taunt source and restores player targeting after it ends`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("taunt-target-selection")),
            )
        val world = runtimeWorld(session)
        clearMonsters(session)
        val playerStart = session.playerPosition()
        val monsterPoint = findOpenPointAtDistance(session, minDistance = 2, maxDistance = 2)
        val monsterId =
            installAiMonster(
                session = session,
                id = "taunt_tracker",
                position = monsterPoint,
            )
        val tauntSourcePoint = findOpenPointAwayFrom(session, center = monsterPoint, awayFrom = playerStart)
        val tauntSourceId = createTargetDummy(session, tauntSourcePoint)

        StatusLifecycle.applyEffect(
            requireNotNull(world.get<EffectTracker>(monsterId)),
            StatusLifecycle.createInstance(
                type = StatusEffectType.TAUNT,
                effectId = "forced_taunt_test",
                duration = 3,
                sourceEntityId = tauntSourceId,
            ),
        )

        val playerHealthBefore = requireNotNull(world.get<Health>(session.playerId)).current
        val tauntSourceHealth = requireNotNull(world.get<Health>(tauntSourceId))
        var tauntedPosition = requireNotNull(world.get<Position>(monsterId)).toPoint()
        repeat(3) {
            assertTrue(session.perform(PlayerCommand.Wait))
            tauntedPosition = requireNotNull(world.get<Position>(monsterId)).toPoint()
            if (tauntSourceHealth.current < tauntSourceHealth.max || tauntedPosition.chebyshevDistanceTo(tauntSourcePoint) < monsterPoint.chebyshevDistanceTo(tauntSourcePoint)) {
                return@repeat
            }
        }
        assertTrue(tauntSourceHealth.current < tauntSourceHealth.max || tauntedPosition.chebyshevDistanceTo(tauntSourcePoint) < monsterPoint.chebyshevDistanceTo(tauntSourcePoint))
        assertEquals(playerHealthBefore, requireNotNull(world.get<Health>(session.playerId)).current)

        requireNotNull(world.get<EffectTracker>(monsterId)).effects.removeIf { effect -> effect.type == StatusEffectType.TAUNT }

        var restoredPosition = tauntedPosition
        repeat(3) {
            assertTrue(session.perform(PlayerCommand.Wait))
            restoredPosition = requireNotNull(world.get<Position>(monsterId)).toPoint()
            if (restoredPosition.chebyshevDistanceTo(session.playerPosition()) < tauntedPosition.chebyshevDistanceTo(session.playerPosition())) {
                return@repeat
            }
        }
        assertTrue(restoredPosition.chebyshevDistanceTo(session.playerPosition()) < tauntedPosition.chebyshevDistanceTo(session.playerPosition()))
    }

    @Test
    fun `controlled phase inscription teleports to the chosen open tile within range`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260323L),
                saveManager = SaveManager(tempDir.resolve("controlled-phase-inscription")),
            )
        equipInscription(session, hotkey = 5, inscriptionId = "controlled_phase")
        val destination = findOpenPointAtDistance(session, minDistance = 3, maxDistance = 3)

        assertTrue(session.perform(PlayerCommand.UseInscription(hotkey = 5, target = destination)))
        assertEquals(destination, session.playerPosition())
    }

    @Test
    fun `controlled phase inscription rejects blocked or out of range targets`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260324L),
                saveManager = SaveManager(tempDir.resolve("controlled-phase-invalid-target")),
            )
        equipInscription(session, hotkey = 5, inscriptionId = "controlled_phase")
        val originalPosition = session.playerPosition()
        val invalidTarget = findOpenPointAtDistance(session, center = originalPosition, distance = 9)

        assertFalse(session.perform(PlayerCommand.UseInscription(hotkey = 5, target = invalidTarget)))
        assertEquals(originalPosition, session.playerPosition())
        assertEquals("log.inscription.no_teleport_destination", requireNotNull(logEventByKey(session, "log.inscription.no_teleport_destination")).message.key)
    }

    @Test
    fun `save and load preserve future teleport randomness`() {
        val config = FoundationGameConfig(seed = 20260313L)
        val baseline = GameModule.newFoundationSession(config = config, saveManager = SaveManager(tempDir.resolve("teleport-baseline")))
        val persistedSaveManager = SaveManager(tempDir.resolve("teleport-persisted"))
        val persisted = GameModule.newFoundationSession(config = config, saveManager = persistedSaveManager)

        addTeleportScrolls(baseline, count = 2)
        addTeleportScrolls(persisted, count = 2)
        repeat(2) {
            assertTrue(baseline.perform(PlayerCommand.PickUp))
            assertTrue(persisted.perform(PlayerCommand.PickUp))
        }
        assertTrue(baseline.perform(PlayerCommand.ActivateInventoryItem(0)))
        assertTrue(persisted.perform(PlayerCommand.ActivateInventoryItem(0)))
        assertEquals(baseline.playerPosition(), persisted.playerPosition())

        assertTrue(persisted.perform(PlayerCommand.SaveGame))
        val loaded = requireNotNull(GameModule.loadFoundationSession(persistedSaveManager))

        assertTrue(baseline.perform(PlayerCommand.ActivateInventoryItem(0)))
        assertTrue(loaded.perform(PlayerCommand.ActivateInventoryItem(0)))
        assertEquals(baseline.playerPosition(), loaded.playerPosition())
    }

    @Test
    fun `save and load preserve future combat randomness`() {
        val config = FoundationGameConfig(seed = 20260314L)
        val baseline = GameModule.newFoundationSession(config = config, saveManager = SaveManager(tempDir.resolve("combat-baseline")))
        val persistedSaveManager = SaveManager(tempDir.resolve("combat-persisted"))
        val persisted = GameModule.newFoundationSession(config = config, saveManager = persistedSaveManager)

        val baselineDummy = installCombatDummy(baseline)
        val persistedDummy = installCombatDummy(persisted)
        val attackDelta = requireNotNull(runtimeWorld(baseline).get<Position>(baselineDummy)).toPoint() - baseline.playerPosition()

        assertTrue(baseline.perform(PlayerCommand.Move(attackDelta)))
        assertTrue(persisted.perform(PlayerCommand.Move(attackDelta)))
        assertEquals(monsterHp(baseline, baselineDummy), monsterHp(persisted, persistedDummy))

        assertTrue(persisted.perform(PlayerCommand.SaveGame))
        val loaded = requireNotNull(GameModule.loadFoundationSession(persistedSaveManager))
        val loadedDummy = requireNotNull(entityByTemplateId(loaded, "orc.raider"))
        val loadedAttackDelta = requireNotNull(runtimeWorld(loaded).get<Position>(loadedDummy)).toPoint() - loaded.playerPosition()

        assertTrue(baseline.perform(PlayerCommand.Move(attackDelta)))
        assertTrue(loaded.perform(PlayerCommand.Move(loadedAttackDelta)))
        assertEquals(monsterHp(baseline, baselineDummy), monsterHp(loaded, loadedDummy))
    }

    @Test
    fun `killing final floor boss in grey gate depths advances the route and persists the checkpoint save`() {
        val saveManager = SaveManager(tempDir.resolve("boss-save"))
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        zoneId = "grey_gate_depths",
                        playerProfessionId = "arcanist",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 3,
                    ),
                saveManager = saveManager,
            )

        repeat(1) {
            movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
            assertTrue(session.perform(PlayerCommand.Descend))
        }
        val world = runtimeWorld(session)
        val bossId =
            world.entitiesWith(MonsterTemplateId::class)
                .single { entityId -> requireNotNull(world.get<MonsterTemplateId>(entityId)).value == FOUNDATION_BOSS_TEMPLATE_ID }
        val baselineInventoryCount = session.inventoryItems().size
        requireNotNull(world.get<Health>(bossId)).current = 1
        val bossPosition = requireNotNull(world.get<Position>(bossId)).toPoint()
        val attackOrigin = if (bossPosition.x > 0) Point(bossPosition.x - 1, bossPosition.y) else Point(bossPosition.x + 1, bossPosition.y)
        movePlayerTo(session, attackOrigin)

        assertTrue(session.perform(PlayerCommand.Move(bossPosition - attackOrigin)))
        assertFalse(session.isVictory())
        assertEquals("underground_river", session.config.zoneId)
        assertEquals(4, session.config.routeIndex)
        assertEquals(1, session.currentFloor())
        assertTrue(saveManager.hasSave())
        assertTrue("route.grey_gate_depths.underground_river" in session.worldProgress().claimedRouteRewards)
        assertTrue(session.inventoryItems().size >= baselineInventoryCount + 1)
        assertTrue(inventoryBaseIds(session).count { baseId -> baseId == "seal_reliquary" } >= 1)
        val rewardSummary =
            requireNotNull(
                session.milestoneRewardSummaries().firstOrNull { reward ->
                    reward.rewardSource == MilestoneRewardSource.BOSS &&
                        reward.sourceId == "dungeon_lord_encounter" &&
                        reward.qualityTier == ItemQuality.RARE &&
                        reward.affixIds.size >= 2
                },
            )
        assertEquals(EquipSlot.OFF_HAND, requireNotNull(itemBasesById[rewardSummary.baseItemId]).slot)
        val rewardLog = logEventByKey(session, "log.boss.reward.claimed") ?: logEventByKey(session, "log.boss.reward.dropped")
        rewardLog?.let { snapshot ->
            assertEquals(
                "item.seal_reliquary.name",
                snapshot.message.arguments.first { argument -> argument.name == "item" }.valueKey,
            )
        }
    }

    @Test
    fun `killing shattered outpost boss advances to greenwood without duplicating starter shield`() {
        val saveManager = SaveManager(tempDir.resolve("bandit-boss-save"))
        val session =
            GameModule.newFoundationSession(
                config =
                    FoundationGameConfig(
                        seed = 20260318L,
                        zoneId = "shattered_outpost",
                        playerProfessionId = "vanguard",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 0,
                    ),
                saveManager = saveManager,
            )

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))

        val world = runtimeWorld(session)
        val bossId = requireNotNull(entityByTemplateId(session, "bandit.captain"))
        requireNotNull(world.get<Health>(bossId)).current = 1
        val bossPosition = requireNotNull(world.get<Position>(bossId)).toPoint()
        val attackOrigin = if (bossPosition.x > 0) Point(bossPosition.x - 1, bossPosition.y) else Point(bossPosition.x + 1, bossPosition.y)
        movePlayerTo(session, attackOrigin)

        assertTrue(session.perform(PlayerCommand.Move(bossPosition - attackOrigin)))
        assertFalse(session.isVictory())
        assertEquals("greenwood_fringe", session.config.zoneId)
        assertEquals(1, session.config.routeIndex)
        assertEquals(1, inventoryBaseIds(session).count { baseId -> baseId == "basic_shield" })
        assertTrue(inventoryBaseIds(session).count { baseId -> baseId == "healing_potion" } >= 2)
        val rewardSummary =
            requireNotNull(
                session.milestoneRewardSummaries().firstOrNull { reward ->
                    reward.rewardSource == MilestoneRewardSource.ROUTE &&
                        reward.sourceId == "route.shattered_outpost.greenwood_fringe" &&
                        reward.qualityTier.ordinal >= ItemQuality.MAGIC.ordinal &&
                        reward.affixIds.isNotEmpty()
                },
            )
        assertEquals(EquipSlot.OFF_HAND, rewardSummary.equipSlot)
        assertEquals(EquipSlot.OFF_HAND, requireNotNull(itemBasesById[rewardSummary.baseItemId]).slot)
        assertEquals("basic_shield", rewardSummary.equippedBaseItemIdBeforeReward)
        assertTrue("route.shattered_outpost.greenwood_fringe" in session.worldProgress().claimedRouteRewards)
        assertTrue(saveManager.hasSave())
    }

    @Test
    fun `killing abyssal guardian grants finale specific reward profile`() {
        val saveManager = SaveManager(tempDir.resolve("abyssal-guardian-reward-save"))
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260328L, zoneId = "abyssal_heart", playerProfessionId = "rogue"),
                saveManager = saveManager,
            )

        val world = runtimeWorld(session)
        val bossId = requireNotNull(entityByTemplateId(session, "abyssal.guardian"))
        val baselineInventoryCount = session.inventoryItems().size
        invokeHandleDeath(session, bossId, session.playerId)
        assertTrue(session.isVictory())
        assertEquals(baselineInventoryCount + 1, session.inventoryItems().size)
        assertEquals(1, inventoryBaseIds(session).count { baseId -> baseId == "abyssal_heartstone" })
        assertTrue(
            session.milestoneRewardSummaries().any { reward ->
                reward.rewardSource == MilestoneRewardSource.BOSS &&
                    reward.sourceId == "abyssal_guardian_encounter" &&
                    reward.qualityTier == ItemQuality.RARE &&
                    reward.affixIds.size >= 3
            },
        )
        val rewardLog = logEventByKey(session, "log.boss.reward.claimed") ?: logEventByKey(session, "log.boss.reward.dropped")
        rewardLog?.let { snapshot ->
            assertEquals(
                "item.abyssal_heartstone.name",
                snapshot.message.arguments.first { argument -> argument.name == "item" }.valueKey,
            )
        }
    }

    @Test
    fun `player death deletes existing save`() {
        val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        factory.createMonster(
            world = world,
            template =
                MonsterTemplate(
                    id = "killer",
                    name = "Killer",
                    glyph = 'k',
                    colorHex = "#FF0000",
                    stats = com.ktome.core.ecs.Stats(str = 10, dex = 1, con = 1, wil = 1),
                    baseHp = 10,
                    baseAttack = 10,
                    baseDefense = 0,
                    speed = 90,
                    ai = AIType.CHASE,
                    expReward = 0,
                    spawnFloors = listOf(1),
                    spawnWeight = 1,
                ),
            position = Point(2, 1),
        )
        requireNotNull(world.get<Health>(playerId)).current = 1

        val session =
            FoundationGameSession(
                config = FoundationGameConfig(width = 5, height = 3),
                map = map,
                world = world,
                playerId = playerId,
                combatResolver = combatResolver(doubleValue = 0.0, intValue = 2),
                talentRegistry = talentRegistry,
                talentResolver = TalentResolver(talentRegistry, combatResolver(doubleValue = 0.0, intValue = 2)),
                sessionRandom = fixedRandom(0.0, 2),
            )
        val sessionSaveManager = sessionSaveManager(session)

        assertTrue(session.perform(PlayerCommand.SaveGame))
        assertTrue(sessionSaveManager.hasSave())
        repeat(3) {
            if (!session.isGameOver()) {
                assertTrue(session.perform(PlayerCommand.Wait))
            }
        }
        assertTrue(session.isGameOver())
        assertFalse(sessionSaveManager.hasSave())
    }

    @Test
    fun `run summary defeat captures killer resource and final events`() {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260322L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("summary-defeat-save")),
            )
        clearMonsters(session)
        val world = runtimeWorld(session)
        val killer =
            EntityFactory().createMonster(
                world = world,
                template = dataLoader.loadMonsterCatalog().monsters.first { monster -> monster.id == "bandit.raider" },
                position = findOpenAdjacentPoint(session, session.playerPosition()),
            )
        requireNotNull(world.get<Health>(session.playerId)).current = 1

        repeat(3) {
            if (!session.isGameOver()) {
                assertTrue(session.perform(PlayerCommand.Wait))
            }
        }
        assertTrue(session.isGameOver())

        val summary = requireNotNull(session.outcomeSummary())
        assertEquals("zone.shattered_outpost.name", summary.zoneNameKey)
        assertEquals("ui.summary.reason.player_died", summary.outcomeReasonKey)
        assertEquals("monster.bandit.raider.name", summary.killerNameKey)
        assertEquals("bandit.raider", summary.killerTemplateId)
        assertEquals(0, summary.finalHpCurrent)
        assertEquals("STAMINA", summary.finalResourceTypeId)
        assertEquals("ui.hud.stamina.short", summary.finalResourceLabelKey)
        assertTrue(summary.lastEvents.isNotEmpty())
        assertEquals("log.player.death", summary.lastEvents.last().key)
        assertTrue(summary.lastEvents.any { event -> event.key == "log.attack.hit" || event.key == "log.attack.crit" })
        assertTrue(world.isAlive(killer))
    }

    @Test
    fun `inspect view exposes visible actor stats status and item details`() {
        val map = GameMap.fromAscii(rows = listOf(".........", ".........", "........."), playerStart = Point(1, 1))
        val world = World()
        val factory = EntityFactory()
        val itemFactory = ItemFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        val monsterId =
            factory.createMonster(
                world = world,
                template =
                    MonsterTemplate(
                        id = "inspect_dummy",
                        name = "Inspect Dummy",
                        glyph = 'd',
                        colorHex = "#AAAAAA",
                        stats = com.ktome.core.ecs.Stats(str = 4, dex = 3, con = 2, wil = 1),
                        baseHp = 12,
                        baseAttack = 4,
                        baseDefense = 2,
                        speed = 90,
                        ai = AIType.CHASE,
                        expReward = 10,
                        spawnFloors = listOf(1),
                        spawnWeight = 1,
                    ),
                position = Point(2, 1),
            )
        requireNotNull(world.get<EffectTracker>(monsterId)).effects +=
            StatusLifecycle.createInstance(
                type = StatusEffectType.STUN,
                effectId = "inspect_stun",
                duration = 2,
            )
        itemFactory.createGroundItem(
            world = world,
            item =
                ItemInstance(
                    baseId = "inspect_blade",
                    name = "Inspect Blade",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#E0E0E0",
                    quality = ItemQuality.MAGIC,
                    materialId = "MITHRIL",
                    materialName = "Mithril",
                    affixes = listOf(AffixDef(id = "of_speed", name = "of Speed", type = AffixType.SUFFIX, statModifiers = StatModifier(speed = 1))),
                    stats = StatModifier(attack = 5, speed = 1),
                    passive = EquipmentPassive.DamageVsTag(tag = "bandit", bonusPercent = 0.15),
                ),
            position = Point(1, 1),
        )
        val session = session(world, map, playerId)

        val monsterInspect = session.inspectAt(Point(2, 1))
        val playerTileInspect = session.inspectAt(Point(1, 1))

        assertEquals(TileVisibility.VISIBLE, monsterInspect.visibility)
        assertEquals("Floor", monsterInspect.terrainName)
        assertEquals("Inspect Dummy", monsterInspect.actor?.name)
        assertEquals("Monster Chase", monsterInspect.actor?.role)
        assertEquals(requireNotNull(world.get<Health>(monsterId)).max, monsterInspect.actor?.maxHp)
        assertTrue(monsterInspect.actor?.statusEffects?.contains("Stunned 2t") == true)

        val itemInspect = playerTileInspect.items.single()
        assertEquals("Mithril Inspect Blade of Speed", itemInspect.name)
        assertEquals("Weapon", itemInspect.typeLabel)
        assertTrue(itemInspect.details.contains("Slot Weapon"))
        assertTrue(itemInspect.details.contains("Quality Magic"))
        assertTrue(itemInspect.details.contains("Material Mithril"))
        assertTrue(itemInspect.details.contains("Affix of Speed"))
        assertTrue(itemInspect.details.contains("+15% damage vs Bandits"))
        assertTrue(itemInspect.details.contains("ATK +5"))
        assertTrue(itemInspect.details.contains("SPD +1"))
    }

    @Test
    fun `status interactions are surfaced through visible render log messages`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260324L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("status-feedback-log")),
            )
        clearMonsters(session)
        val monsterId = installCombatDummy(session, id = "status_feedback_dummy")
        val method = FoundationGameSession::class.java.getDeclaredMethod("logTalentResult", com.ktome.core.talent.TalentResult::class.java)
        method.isAccessible = true

        method.invoke(
            session,
            com.ktome.core.talent.TalentResult(
                talentId = "status_feedback_test",
                talentName = "Status Feedback",
                user = session.playerId,
                targets = listOf(monsterId),
                effects =
                    listOf(
                        com.ktome.core.talent.TalentEffectResult.StatusApplied(
                            target = monsterId,
                            type = StatusEffectType.BURN,
                            duration = 3,
                            interactionId = "FREEZE_OVERWRITTEN_BY_BURN",
                        ),
                        com.ktome.core.talent.TalentEffectResult.StatusApplied(
                            target = monsterId,
                            type = StatusEffectType.TAUNT,
                            duration = 2,
                            interactionId = "TAUNT_OVERRIDE",
                            previousSource = monsterId,
                        ),
                    ),
            ),
        )

        assertNotNull(logEventByKey(session, "log.status.freeze_overwritten_by_burn"))
        assertNotNull(logEventByKey(session, "log.status.taunt_override"))
        assertTrue(recentEventSummaries(session).any { summary -> summary.contains("status_interaction:${monsterId.value}:BURN:FREEZE_OVERWRITTEN_BY_BURN") })
        assertTrue(recentEventSummaries(session).any { summary -> summary.contains("taunt_override:${monsterId.value}:TAUNT:${session.playerId.value}") })
    }

    @Test
    fun `melee attacks that break stealth add a visible log message`() {
        val session =
            GameModule.newFoundationSession(
                FoundationGameConfig(seed = 20260324L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                SaveManager(tempDir.resolve("stealth-break-log")),
            )
        clearMonsters(session)
        val monsterId = installCombatDummy(session, id = "stealth_dummy")
        StatusLifecycle.applyEffect(
            requireNotNull(runtimeWorld(session).get<EffectTracker>(monsterId)),
            StatusLifecycle.createInstance(
                type = StatusEffectType.STEALTH,
                effectId = "monster_stealth",
                duration = 3,
            ),
        )
        val targetPoint = requireNotNull(runtimeWorld(session).get<Position>(monsterId)).toPoint()

        assertTrue(session.perform(PlayerCommand.Move(targetPoint - session.playerPosition())))

        assertNotNull(logEventByKey(session, "log.status.stealth_broken"))
        assertFalse(requireNotNull(runtimeWorld(session).get<EffectTracker>(monsterId)).has(StatusEffectType.STEALTH))
        assertTrue(recentEventSummaries(session).any { summary -> summary.contains("stealth_break:${monsterId.value}:") })
    }

    @Test
    fun `inspect view does not leak actors on hidden tiles`() {
        val map =
            GameMap.fromAscii(
                rows = listOf(".........................", ".........................", "........................."),
                playerStart = Point(1, 1),
            )
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        factory.createMonster(
            world = world,
            template =
                MonsterTemplate(
                    id = "hidden_dummy",
                    name = "Hidden Dummy",
                    glyph = 'h',
                    colorHex = "#777777",
                    stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 1, wil = 1),
                    baseHp = 8,
                    baseAttack = 2,
                    baseDefense = 1,
                    speed = 90,
                    ai = AIType.CHASE,
                    expReward = 0,
                    spawnFloors = listOf(1),
                    spawnWeight = 1,
                ),
            position = Point(20, 1),
        )
        val session = session(world, map, playerId)

        val inspect = session.inspectAt(Point(20, 1))

        assertEquals(TileVisibility.HIDDEN, inspect.visibility)
        assertEquals("Unknown", inspect.terrainName)
        assertNull(inspect.actor)
        assertTrue(inspect.items.isEmpty())
    }

    private fun fixedRandom(
        doubleValue: Double,
        intValue: Int,
    ): RandomSource =
        object : RandomSource {
            override fun nextDouble(): Double = doubleValue

            override fun nextInt(
                fromInclusive: Int,
                untilExclusive: Int,
            ): Int = intValue
        }

    private fun combatResolver(
        doubleValue: Double,
        intValue: Int,
    ): CombatResolver = CombatResolver(fixedRandom(doubleValue, intValue))

    private fun session(
        world: World,
        map: GameMap,
        playerId: com.ktome.core.ecs.EntityId,
    ): FoundationGameSession {
        val combatResolver = combatResolver(0.0, 2)
        return FoundationGameSession(
            config = FoundationGameConfig(width = map.width, height = map.height, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
            map = map,
            world = world,
            playerId = playerId,
            combatResolver = combatResolver,
            talentRegistry = talentRegistry,
            talentResolver = TalentResolver(talentRegistry, combatResolver, dataLoader.loadStatusCatalog()),
            sessionRandom = fixedRandom(0.0, 2),
        )
    }

    private fun mixedOwnerRespecSession(savePath: Path): FoundationGameSession {
        val professionTalent = requireNotNull(talents.firstOrNull { talent -> talent.id == "power_strike" })
        val raceTalent =
            TalentDef(
                id = "moon_blessing",
                nameKey = "talent.shalore.moon_blessing.name",
                descriptionTemplateKey = "talent.shalore.moon_blessing.desc",
                maxRank = 5,
                category = TalentCategory.PASSIVE,
                cooldown = 0,
                levelEffects = (1..5).associateWith { TalentLevelEffect() },
                treeId = "race_tree",
            )

        val schemaCatalog = dataLoader.loadSchemaCatalog()
        val professionSchema =
            requireNotNull(schemaCatalog.professions.firstOrNull { profession -> profession.id == "vanguard" }).copy(
                talentTrees = listOf("profession_tree"),
                startingTalents = listOf(professionTalent.id),
                startingKit = emptyList(),
            )
        val professionTalentSchema = requireNotNull(schemaCatalog.talents.firstOrNull { talent -> talent.id == professionTalent.id })
        val professionTree =
            requireNotNull(schemaCatalog.talentTrees.firstOrNull { tree -> tree.id == professionTalentSchema.treeId }).copy(
                id = "profession_tree",
                nodes = listOf(professionTalent.id),
            )
        val raceTree =
            com.ktome.game.data.schema.TalentTreeSchemaV2(
                id = "race_tree",
                professionId = "",
                raceId = "shalore",
                nameKey = "talent_tree.race_tree.name",
                descKey = "talent_tree.race_tree.desc",
                visualKey = "tree.race",
                iconKey = "tree.race.icon",
                audioProfile = "audio.tree.race",
                schemaVersion = 2,
                tags = emptyList(),
                layout = "grid",
                nodes = listOf("moon_blessing"),
            )
        val raceTalentSchema =
            com.ktome.game.data.schema.TalentSchemaV2(
                id = "moon_blessing",
                nameKey = "talent.shalore.moon_blessing.name",
                descKey = "talent.shalore.moon_blessing.desc",
                visualKey = "talent.shalore.moon_blessing.visual",
                iconKey = "talent.shalore.moon_blessing.icon",
                audioProfile = "audio.talent.shalore",
                schemaVersion = 2,
                tags = emptyList(),
                maxPoints = 5,
                tier = 1,
                category = "PASSIVE",
                damageType = null,
                powerDimension = null,
                kind = "UTILITY",
                cooldown = 0,
                castTime = "INSTANT",
                targeting = com.ktome.game.data.schema.TalentTargetingSchemaV2(type = "SELF", range = 0, minRange = 0, areaRadius = 0),
                resourceCosts = emptyList(),
                unlockLevel = 1,
                requirements = com.ktome.game.data.schema.TalentRequirementsSchemaV2(),
                levelEffects = linkedMapOf(1 to com.ktome.game.data.schema.TalentLevelEffectSchemaV2()),
                breakpoints = emptyList(),
                keywords = emptyList(),
                callbacks = emptyList(),
                telegraphRef = null,
                aiHints = null,
                treeId = "race_tree",
            )
        val content =
            GameContent(
                talents = listOf(professionTalent, raceTalent),
                statuses = schemaCatalog.statuses,
                statusCatalog = dataLoader.loadStatusCatalog(),
                talentRegistry = TalentRegistry().apply { registerAll(listOf(professionTalent, raceTalent)) },
                monsterCatalog = emptyList(),
                itemBundle = dataLoader.loadItemBundle(),
                bossDefinitions = emptyMap(),
                schemaCatalog =
                    schemaCatalog.copy(
                        professions = listOf(professionSchema),
                        talents = listOf(professionTalentSchema.copy(treeId = "profession_tree"), raceTalentSchema),
                        talentTrees = listOf(professionTree, raceTree),
                    ),
                localizer = dataLoader.localizer,
            )

        val world = World()
        val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val playerId = EntityFactory().createPlayer(world, Point(1, 1), listOf(professionTalent, raceTalent))
        val loadout = requireNotNull(world.get<com.ktome.core.talent.TalentLoadout>(playerId))
        loadout.talentLevels["power_strike"] = 3
        loadout.talentLevels["moon_blessing"] = 4

        val floorState =
            FloorState(
                floor = 1,
                payload =
                    SessionSnapshotMapper.captureFloor(
                        map = map,
                        stairsUp = null,
                        stairsDown = null,
                        exploredTiles = emptySet(),
                        world = world,
                        excludedEntities = setOf(playerId),
                    ),
            )
        val dungeonManager = DungeonManager<FloorRuntimeState>(maxFloor = 1, floorLoader = { floorState }).apply { putState(floorState) }

        return FoundationGameSession(
            config = FoundationGameConfig(width = map.width, height = map.height, zoneId = "shattered_outpost", playerProfessionId = "vanguard", maxFloor = 1),
            content = content,
            saveManager = SaveManager(savePath),
            dungeonManager = dungeonManager,
            playerSnapshot = SessionSnapshotMapper.capturePlayer(world, playerId),
        )
    }

    private fun runtimeWorld(session: FoundationGameSession): World {
        val field = FoundationGameSession::class.java.getDeclaredField("world")
        field.isAccessible = true
        return field.get(session) as World
    }

    private fun sessionContent(session: FoundationGameSession): GameContent {
        val field = FoundationGameSession::class.java.getDeclaredField("content")
        field.isAccessible = true
        return field.get(session) as GameContent
    }

    private fun replaceContent(
        session: FoundationGameSession,
        content: GameContent,
    ) {
        val field = FoundationGameSession::class.java.getDeclaredField("content")
        field.isAccessible = true
        field.set(session, content)
    }

    private fun invokeHandleDeath(
        session: FoundationGameSession,
        target: EntityId,
        killer: EntityId?,
    ) {
        val method =
            FoundationGameSession::class.java.declaredMethods
                .first { declared -> declared.name.startsWith("handleDeath-") && declared.parameterCount == 2 }
        method.isAccessible = true
        method.invoke(session, target.value, killer)
    }

    private fun forcePlayerInCombat(session: FoundationGameSession) {
        val turnCountField = FoundationGameSession::class.java.getDeclaredField("turnCount").apply { isAccessible = true }
        val combatTurnField = FoundationGameSession::class.java.getDeclaredField("lastPlayerCombatTurn").apply { isAccessible = true }
        combatTurnField.setInt(session, turnCountField.getInt(session))
    }

    private fun recentEventSummaries(session: FoundationGameSession): List<String> {
        val field = FoundationGameSession::class.java.getDeclaredField("recentEvents")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return (field.get(session) as ArrayDeque<String>).toList()
    }

    private fun movePlayerTo(
        session: FoundationGameSession,
        point: Point,
    ) {
        requireNotNull(runtimeWorld(session).get<Position>(session.playerId)).moveTo(point)
    }

    private fun stairPoint(
        session: FoundationGameSession,
        direction: com.ktome.core.dungeon.StairDirection,
    ): Point {
        val world = runtimeWorld(session)
        return world.entitiesWith(Position::class, Stair::class)
            .first { entityId -> requireNotNull(world.get<Stair>(entityId)).direction == direction }
            .let { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
    }

    private fun addTeleportScrolls(
        session: FoundationGameSession,
        count: Int,
    ) {
        val world = runtimeWorld(session)
        val itemFactory = ItemFactory()
        repeat(count) { index ->
            itemFactory.createGroundItem(
                world = world,
                item =
                    ItemInstance(
                        baseId = "scroll_teleport",
                        name = "传送卷轴",
                        type = ItemType.CONSUMABLE,
                        glyph = '?',
                        colorHex = "#00FFFF",
                        effect = ConsumableEffect.TELEPORT,
                    ),
                position = session.playerPosition(),
            )
        }
    }

    private fun clearMonsters(session: FoundationGameSession) {
        val world = runtimeWorld(session)
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)
    }

    private fun primePlayerTurnState(session: FoundationGameSession) {
        val world = runtimeWorld(session)
        StaminaPools.syncTo(world, session.playerId, nextCurrent = 0, nextMax = StaminaPools.max(world, session.playerId))
        requireNotNull(world.get<com.ktome.core.talent.CooldownState>(session.playerId)).remainingByTalentId["power_strike"] = 3
    }

    private fun playerCooldown(
        session: FoundationGameSession,
        talentId: String,
    ): Int = requireNotNull(runtimeWorld(session).get<com.ktome.core.talent.CooldownState>(session.playerId)).remainingByTalentId[talentId] ?: 0

    private fun installCombatDummy(
        session: FoundationGameSession,
        id: String = "orc",
        resistances: Map<DamageType, Int> = emptyMap(),
        position: Point? = null,
    ): com.ktome.core.ecs.EntityId {
        val world = runtimeWorld(session)
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)
        val dummyPosition = position ?: findOpenAdjacentPoint(session, session.playerPosition())
        val dummyId =
            EntityFactory().createMonster(
                world = world,
                template =
                    MonsterTemplate(
                        id = id,
                        name = "Orc",
                        glyph = 'o',
                        colorHex = "#3AAE4B",
                        stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 1, wil = 1),
                        baseHp = 200,
                        baseAttack = 1,
                        baseDefense = 0,
                        speed = 90,
                        ai = AIType.CHASE,
                        expReward = 0,
                        spawnFloors = listOf(session.currentFloor()),
                        spawnWeight = 1,
                        resistances = resistances,
                    ),
                position = dummyPosition,
            )
        world.remove<AIBehavior>(dummyId)
        return dummyId
    }

    private fun installAiMonster(
        session: FoundationGameSession,
        id: String,
        position: Point,
        aiProfileId: String = "ai.chase.basic",
        aiType: AIType = AIType.CHASE,
    ): com.ktome.core.ecs.EntityId =
        EntityFactory().createMonster(
            world = runtimeWorld(session),
            template =
                MonsterTemplate(
                    id = id,
                    name = "AI Tracker",
                    glyph = 't',
                    colorHex = "#CC5555",
                    stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 1, wil = 1),
                    baseHp = 40,
                    baseAttack = 2,
                    baseDefense = 0,
                    speed = 90,
                    ai = aiType,
                    expReward = 0,
                    spawnFloors = listOf(session.currentFloor()),
                    spawnWeight = 1,
                    aiProfileId = aiProfileId,
                ),
            position = position,
        )

    private fun createTargetDummy(
        session: FoundationGameSession,
        position: Point,
    ): com.ktome.core.ecs.EntityId {
        val world = runtimeWorld(session)
        val entityId = world.createEntity()
        val stats = Stats(str = 1, dex = 1, con = 1, wil = 1)
        val combatProfile = CombatProfile(baseAttack = 1, baseDefense = 0, baseHp = 20)
        val derived = StatsCalculator.calculate(stats, combatProfile)
        world.add(entityId, Position(position.x, position.y))
        world.add(entityId, Name("Taunt Dummy"))
        world.add(entityId, Health(current = 20, max = 20))
        world.add(entityId, stats)
        world.add(entityId, combatProfile)
        world.add(entityId, derived)
        world.add(entityId, ResistanceProfile())
        world.add(entityId, EffectTracker(ownerId = entityId))
        world.add(entityId, BlocksMovement())
        return entityId
    }

    private fun findOpenPointAtDistance(
        session: FoundationGameSession,
        minDistance: Int,
        maxDistance: Int,
    ): Point {
        val origin = session.playerPosition()
        val world = runtimeWorld(session)
        val occupied = world.entitiesWith(Position::class).mapTo(linkedSetOf()) { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
        return session.map.floorPoints()
            .filter { point ->
                point != origin &&
                    point in session.visibleTiles() &&
                    point.chebyshevDistanceTo(origin) in minDistance..maxDistance &&
                    point !in occupied
            }
            .sortedWith(compareBy<Point> { point -> point.chebyshevDistanceTo(origin) }.thenBy(Point::y).thenBy(Point::x))
            .first()
    }

    private fun installExperienceDummy(
        session: FoundationGameSession,
        id: String,
        expReward: Int,
    ): com.ktome.core.ecs.EntityId {
        val world = runtimeWorld(session)
        val dummyPosition = findOpenAdjacentPoint(session, session.playerPosition())
        val dummyId =
            EntityFactory().createMonster(
                world = world,
                template =
                    MonsterTemplate(
                        id = id,
                        name = "Growth Dummy",
                        glyph = 'd',
                        colorHex = "#AAAAAA",
                        stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 1, wil = 1),
                        baseHp = 1,
                        baseAttack = 1,
                        baseDefense = 0,
                        speed = 90,
                        ai = AIType.CHASE,
                        expReward = expReward,
                        spawnFloors = listOf(session.currentFloor()),
                        spawnWeight = 1,
                    ),
                position = dummyPosition,
            )
        world.remove<AIBehavior>(dummyId)
        return dummyId
    }

    private fun monsterHp(
        session: FoundationGameSession,
        entityId: com.ktome.core.ecs.EntityId,
    ): Int = requireNotNull(runtimeWorld(session).get<Health>(entityId)).current

    private fun entityByTemplateId(
        session: FoundationGameSession,
        templateId: String,
    ): com.ktome.core.ecs.EntityId? {
        val world = runtimeWorld(session)
        return world.entitiesWith(MonsterTemplateId::class)
            .firstOrNull { entityId -> requireNotNull(world.get<MonsterTemplateId>(entityId)).value == templateId }
    }

    private fun logEventByKey(
        session: FoundationGameSession,
        key: String,
    ): com.ktome.core.snapshot.RenderLogEventSnapshot? =
        session.renderSnapshot().logEvents.firstOrNull { event -> event.message.key == key }

    private fun findOpenAdjacentPoint(
        session: FoundationGameSession,
        center: Point,
    ): Point {
        val world = runtimeWorld(session)
        val occupied =
            world.entitiesWith(Position::class)
                .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
                .toSet()

        return Point.ALL_DIRECTIONS
            .map { delta -> center + delta }
            .first { point ->
                session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement &&
                    point !in occupied
            }
    }

    private fun equipInscription(
        session: FoundationGameSession,
        hotkey: Int,
        inscriptionId: String,
    ) {
        val world = runtimeWorld(session)
        val loadout = world.get<InscriptionLoadout>(session.playerId) ?: InscriptionLoadout().also { world.add(session.playerId, it) }
        loadout.slots.clear()
        loadout.slots += InscriptionSlot(hotkey = hotkey, inscriptionId = inscriptionId)
        val cooldowns = world.get<InscriptionCooldownState>(session.playerId) ?: InscriptionCooldownState().also { world.add(session.playerId, it) }
        cooldowns.remainingByInscriptionId.clear()
    }

    private fun findOpenPointAtDistance(
        session: FoundationGameSession,
        center: Point,
        distance: Int,
    ): Point {
        val world = runtimeWorld(session)
        val occupied =
            world.entitiesWith(Position::class)
                .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
                .toSet()

        return (0 until session.map.height).asSequence()
            .flatMap { y -> (0 until session.map.width).asSequence().map { x -> Point(x, y) } }
            .filter { point ->
                point.chebyshevDistanceTo(center) == distance &&
                    session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement &&
                    point !in occupied
            }
            .sortedWith(compareBy<Point>(Point::y).thenBy(Point::x))
            .first()
    }

    private fun findOpenPointAwayFrom(
        session: FoundationGameSession,
        center: Point,
        awayFrom: Point,
    ): Point {
        val world = runtimeWorld(session)
        val occupied =
            world.entitiesWith(Position::class)
                .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
                .toSet()

        return Point.ALL_DIRECTIONS.asSequence()
            .map { delta -> center + delta }
            .filter { point ->
                session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement &&
                    point !in occupied
            }.sortedWith(compareByDescending<Point> { point -> point.chebyshevDistanceTo(awayFrom) }.thenBy(Point::y).thenBy(Point::x))
            .first()
    }

    private fun interactablePoint(
        session: FoundationGameSession,
        interactableId: String,
    ): Point {
        val world = runtimeWorld(session)
        val entityId =
            world.entitiesWith(Position::class, Interactable::class)
                .first { candidate -> requireNotNull(world.get<Interactable>(candidate)).id == interactableId }
        return requireNotNull(world.get<Position>(entityId)).toPoint()
    }

    private fun inventoryBaseIds(session: FoundationGameSession): List<String> {
        val world = runtimeWorld(session)
        val inventory = requireNotNull(world.get<com.ktome.core.item.Inventory>(session.playerId))
        return inventory.itemIds.map { itemId -> requireNotNull(world.get<ItemInstance>(itemId)).baseId }
    }

    private fun groundItemBaseIdsAt(
        session: FoundationGameSession,
        point: Point,
    ): List<String> {
        val world = runtimeWorld(session)
        return world.entitiesWith(Position::class, ItemInstance::class)
            .filter { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() == point }
            .map { entityId -> requireNotNull(world.get<ItemInstance>(entityId)).baseId }
    }

    private fun groundItemsAt(
        session: FoundationGameSession,
        point: Point,
    ): List<ItemInstance> {
        val world = runtimeWorld(session)
        return world.entitiesWith(Position::class, ItemInstance::class)
            .filter { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() == point }
            .map { entityId -> requireNotNull(world.get<ItemInstance>(entityId)) }
    }

    private fun addInventoryItem(
        session: FoundationGameSession,
        item: ItemInstance,
    ): Int {
        val world = runtimeWorld(session)
        val inventory = requireNotNull(world.get<com.ktome.core.item.Inventory>(session.playerId))
        val index = inventory.itemIds.size
        inventory.itemIds += ItemFactory().createCarriedItem(world, item)
        return index
    }

    private fun inventoryIndexOfBaseId(
        session: FoundationGameSession,
        baseItemId: String,
    ): Int =
        requireNotNull(
            session.inventoryItems().firstOrNull { itemView ->
                inventoryBaseIdAt(session, itemView.index) == baseItemId
            }?.index,
        ) {
            "Expected inventory item '$baseItemId'."
        }

    private fun inventoryBaseIdAt(
        session: FoundationGameSession,
        inventoryIndex: Int,
    ): String {
        val world = runtimeWorld(session)
        val inventory = requireNotNull(world.get<com.ktome.core.item.Inventory>(session.playerId))
        val itemId = requireNotNull(inventory.itemIds.getOrNull(inventoryIndex)) { "Missing inventory slot $inventoryIndex." }
        return requireNotNull(world.get<ItemInstance>(itemId)).baseId
    }

    private fun baseItem(baseId: String): ItemInstance {
        val base = requireNotNull(dataLoader.loadItemBundle().baseItems.firstOrNull { item -> item.id == baseId })
        return ItemInstance(
            baseId = base.id,
            name = base.name,
            type = base.type,
            slot = base.slot,
            glyph = base.glyph,
            colorHex = base.colorHex,
            quality = ItemQuality.COMMON,
            stats = base.baseStats.copy(),
            effect = base.effect,
            resourceTypeId = base.resourceTypeId,
            magnitude = base.magnitude,
            passive = base.passive,
        )
    }

    private fun sessionSaveManager(session: FoundationGameSession): SaveManager {
        val field = FoundationGameSession::class.java.getDeclaredField("saveManager")
        field.isAccessible = true
        return field.get(session) as SaveManager
    }

    private fun advanceTurnsUntil(
        session: FoundationGameSession,
        maxTurns: Int,
        predicate: () -> Boolean,
    ) {
        repeat(maxTurns) {
            if (predicate()) {
                return
            }
            assertTrue(session.perform(PlayerCommand.Wait))
        }
        assertTrue(predicate(), "Condition was not reached within $maxTurns turns.")
    }
}
