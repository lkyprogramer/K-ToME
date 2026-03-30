package com.ktome.game

import com.ktome.core.resource.ResourceType
import com.ktome.core.status.StatusTracker
import com.ktome.core.talent.TalentLoadout
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BerserkerPlayableTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `berserker starter loop can build hate buff and spend it on an attack`() {
        val session = ClassFormalizationTestSupport.newSession(tempDir, professionId = "berserker")
        ClassFormalizationTestSupport.clearMonsters(session)
        val dummyId = ClassFormalizationTestSupport.installCombatDummy(session)
        val dummyPoint = ClassFormalizationTestSupport.entityPoint(session, dummyId)
        val hatePool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.HATE)
        val bloodRushSlot = ClassFormalizationTestSupport.talentSlot(session, "blood_rush")
        val savageHewSlot = ClassFormalizationTestSupport.talentSlot(session, "savage_hew")

        hatePool.current = 0
        val hpBeforeBloodRush = ClassFormalizationTestSupport.monsterHp(session, dummyId)
        assertTrue(session.perform(PlayerCommand.UseTalent(bloodRushSlot, dummyPoint)))
        assertTrue(ClassFormalizationTestSupport.playerCooldown(session, "blood_rush") > 0)
        assertTrue(ClassFormalizationTestSupport.monsterHp(session, dummyId) < hpBeforeBloodRush)

        hatePool.current = 20
        val hpBeforeSavageHew = ClassFormalizationTestSupport.monsterHp(session, dummyId)
        assertTrue(session.perform(PlayerCommand.UseTalent(savageHewSlot, dummyPoint)))
        assertTrue(ClassFormalizationTestSupport.playerCooldown(session, "savage_hew") > 0)
        assertTrue(ClassFormalizationTestSupport.monsterHp(session, dummyId) < hpBeforeSavageHew)
        assertTrue(hatePool.current < 20)
    }

    @Test
    fun `berserker pr11 sustain loop can spend and rebuild hate`() {
        val session = ClassFormalizationTestSupport.newSession(tempDir, professionId = "berserker")
        ClassFormalizationTestSupport.clearMonsters(session)
        val dummyId = ClassFormalizationTestSupport.installCombatDummy(session)
        val dummyPoint = ClassFormalizationTestSupport.entityPoint(session, dummyId)
        val hatePool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.HATE)
        val world = ClassFormalizationTestSupport.runtimeWorld(session)

        val painFuelSlot = ensureTalentEquipped(session, "pain_fuel", slot = 3)
        val pursuitDriveSlot = ensureTalentEquipped(session, "pursuit_drive", slot = 2)
        val slaughterDriveSlot = ensureTalentEquipped(session, "slaughter_drive", slot = 4)

        val health = requireNotNull(world.get<com.ktome.core.ecs.Health>(session.playerId))
        health.current = (health.max * 0.55).toInt()
        hatePool.current = 26
        val hpBeforePainFuel = health.current

        assertTrue(session.perform(PlayerCommand.UseTalent(painFuelSlot)))
        assertTrue(health.current > hpBeforePainFuel)
        val tracker = requireNotNull(world.get<StatusTracker>(session.playerId))
        assertTrue(tracker.activeEffects().any { effect -> effect.schemaId == "war_cry_empower" })

        assertTrue(session.perform(PlayerCommand.UseTalent(pursuitDriveSlot, dummyPoint)))
        assertTrue(hatePool.current < 26)

        val hateAfterStrike = hatePool.current
        assertTrue(session.perform(PlayerCommand.UseTalent(slaughterDriveSlot)))
        assertTrue(hatePool.current >= hateAfterStrike)
        assertTrue(tracker.activeEffects().any { effect -> effect.schemaId == "war_cry_empower" })
    }

    @Test
    fun `berserker remaining pr11 attack talents resolve at runtime`() {
        val rivenEdgeSession = ClassFormalizationTestSupport.newSession(tempDir.resolve("riven-edge"), professionId = "berserker")
        ClassFormalizationTestSupport.clearMonsters(rivenEdgeSession)
        val rivenEdgeDummyId = ClassFormalizationTestSupport.installCombatDummy(rivenEdgeSession)
        val rivenEdgeDummyPoint = ClassFormalizationTestSupport.entityPoint(rivenEdgeSession, rivenEdgeDummyId)
        val rivenEdgeWorld = ClassFormalizationTestSupport.runtimeWorld(rivenEdgeSession)
        val rivenEdgeSlot = ensureTalentEquipped(rivenEdgeSession, "riven_edge", slot = 3)
        val rivenEdgeHatePool = ClassFormalizationTestSupport.resourcePool(rivenEdgeSession, ResourceType.HATE)
        rivenEdgeHatePool.current = rivenEdgeHatePool.max
        val rivenEdgeHpBefore = ClassFormalizationTestSupport.monsterHp(rivenEdgeSession, rivenEdgeDummyId)
        assertTrue(rivenEdgeSession.perform(PlayerCommand.UseTalent(rivenEdgeSlot, rivenEdgeDummyPoint)))
        assertTrue(ClassFormalizationTestSupport.playerCooldown(rivenEdgeSession, "riven_edge") > 0)
        assertTrue(ClassFormalizationTestSupport.monsterHp(rivenEdgeSession, rivenEdgeDummyId) < rivenEdgeHpBefore)
        val rivenEdgeTracker = requireNotNull(rivenEdgeWorld.get<StatusTracker>(rivenEdgeDummyId))
        assertTrue(rivenEdgeTracker.activeEffects().any { effect -> effect.schemaId == "ARMOR_BREAK" })

        val faultLineSession = ClassFormalizationTestSupport.newSession(tempDir.resolve("fault-line"), professionId = "berserker")
        ClassFormalizationTestSupport.clearMonsters(faultLineSession)
        val faultLineTarget = chargeTargetPoint(faultLineSession)
        val faultLineDummyId = ClassFormalizationTestSupport.installCombatDummy(faultLineSession, position = faultLineTarget)
        val faultLineWorld = ClassFormalizationTestSupport.runtimeWorld(faultLineSession)
        val faultLineSlot = ensureTalentEquipped(faultLineSession, "fault_line", slot = 3)
        val faultLineHatePool = ClassFormalizationTestSupport.resourcePool(faultLineSession, ResourceType.HATE)
        faultLineHatePool.current = faultLineHatePool.max
        val faultLineHpBefore = ClassFormalizationTestSupport.monsterHp(faultLineSession, faultLineDummyId)
        assertTrue(faultLineSession.perform(PlayerCommand.UseTalent(faultLineSlot, faultLineTarget)))
        assertTrue(ClassFormalizationTestSupport.playerCooldown(faultLineSession, "fault_line") > 0)
        assertTrue(ClassFormalizationTestSupport.monsterHp(faultLineSession, faultLineDummyId) < faultLineHpBefore)
        val faultLineTracker = requireNotNull(faultLineWorld.get<StatusTracker>(faultLineDummyId))
        assertTrue(faultLineTracker.activeEffects().any { effect -> effect.schemaId == "WEAKEN" })

        val aftershockSession = ClassFormalizationTestSupport.newSession(tempDir.resolve("aftershock"), professionId = "berserker")
        ClassFormalizationTestSupport.clearMonsters(aftershockSession)
        val aftershockDummyId = ClassFormalizationTestSupport.installCombatDummy(aftershockSession)
        val aftershockSlot = ensureTalentEquipped(aftershockSession, "aftershock", slot = 3)
        val aftershockHatePool = ClassFormalizationTestSupport.resourcePool(aftershockSession, ResourceType.HATE)
        aftershockHatePool.current = aftershockHatePool.max
        val aftershockHpBefore = ClassFormalizationTestSupport.monsterHp(aftershockSession, aftershockDummyId)
        assertTrue(aftershockSession.perform(PlayerCommand.UseTalent(aftershockSlot)))
        assertTrue(ClassFormalizationTestSupport.playerCooldown(aftershockSession, "aftershock") > 0)
        assertTrue(ClassFormalizationTestSupport.monsterHp(aftershockSession, aftershockDummyId) < aftershockHpBefore)
    }

    private fun ensureTalentEquipped(
        session: FoundationGameSession,
        talentId: String,
        slot: Int = 4,
    ): Int {
        session.talentSlots().firstOrNull { talent -> talent.talentId == talentId }?.let { talent ->
            return talent.slot
        }
        val loadout = requireNotNull(ClassFormalizationTestSupport.runtimeWorld(session).get<TalentLoadout>(session.playerId))
        loadout.talentLevels.putIfAbsent(talentId, 1)
        check(session.perform(PlayerCommand.EquipTalentToSlot(slot = slot, talentId = talentId))) {
            "Failed to equip talent '$talentId' into slot $slot."
        }
        return slot
    }

    private fun chargeTargetPoint(session: FoundationGameSession): Point {
        val origin = session.playerPosition()
        return listOf(
            Point(origin.x + 2, origin.y),
            Point(origin.x - 2, origin.y),
            Point(origin.x, origin.y + 2),
            Point(origin.x, origin.y - 2),
        ).first { point ->
            session.map.isInBounds(point.x, point.y) &&
                !session.map[point].blocksMovement
        }
    }
}
