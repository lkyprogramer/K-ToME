package com.ktome.game

import com.ktome.core.resource.ResourceType
import com.ktome.core.status.StatusTracker
import com.ktome.core.talent.TalentLoadout
import com.ktome.core.ecs.get
import com.ktome.core.map.Point
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SpellbladeEquilibriumTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `equilibrium shifts once on the next turn after the last successful non neutral action`() {
        val session = ClassFormalizationTestSupport.newSession(tempDir, professionId = "spellblade")
        ClassFormalizationTestSupport.clearMonsters(session)
        val dummyId = ClassFormalizationTestSupport.installCombatDummy(session)
        val dummyPoint = ClassFormalizationTestSupport.entityPoint(session, dummyId)
        val manaPool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.MANA)
        val equilibriumPool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.EQUILIBRIUM)
        val neutralSlot = ClassFormalizationTestSupport.talentSlot(session, "spell_parry")
        val arcaneSlot = ClassFormalizationTestSupport.talentSlot(session, "arcane_edge")

        manaPool.current = manaPool.max
        equilibriumPool.current = 50

        assertTrue(session.perform(PlayerCommand.UseTalent(neutralSlot)))
        assertEquals(50, session.playerResourceView().secondary?.current)

        val hpBeforeArcaneEdge = ClassFormalizationTestSupport.monsterHp(session, dummyId)
        assertTrue(session.perform(PlayerCommand.UseTalent(arcaneSlot, dummyPoint)))
        val afterArcane = requireNotNull(session.playerResourceView().secondary).current
        assertTrue(afterArcane > 50)
        assertTrue(ClassFormalizationTestSupport.monsterHp(session, dummyId) < hpBeforeArcaneEdge)
        assertTrue(manaPool.current < manaPool.max)

        assertTrue(session.perform(PlayerCommand.Wait))
        assertEquals(afterArcane, requireNotNull(session.playerResourceView().secondary).current)
    }

    @Test
    fun `spellblade pr11 control talents add burn pressure and a defensive reset`() {
        val session = ClassFormalizationTestSupport.newSession(tempDir, professionId = "spellblade")
        ClassFormalizationTestSupport.clearMonsters(session)
        val dummyId = ClassFormalizationTestSupport.installCombatDummy(session)
        val dummyPoint = ClassFormalizationTestSupport.entityPoint(session, dummyId)
        val world = ClassFormalizationTestSupport.runtimeWorld(session)
        val manaPool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.MANA)

        val runicEdgeSlot = ensureTalentEquipped(session, "runic_edge", slot = 3)
        val fluxReversalSlot = ensureTalentEquipped(session, "flux_reversal", slot = 4)

        manaPool.current = manaPool.max
        assertTrue(session.perform(PlayerCommand.UseTalent(runicEdgeSlot, dummyPoint)))
        val dummyTracker = requireNotNull(world.get<StatusTracker>(dummyId))
        assertTrue(dummyTracker.activeEffects().any { effect -> effect.schemaId == "OVERCHARGE" })

        val defenseBefore = requireNotNull(world.get<com.ktome.core.ecs.DerivedStats>(session.playerId)).defense
        assertTrue(session.perform(PlayerCommand.UseTalent(fluxReversalSlot)))
        val defenseAfter = requireNotNull(world.get<com.ktome.core.ecs.DerivedStats>(session.playerId)).defense
        assertTrue(defenseAfter >= defenseBefore)
    }

    @Test
    fun `spellblade remaining pr11 talents resolve at runtime`() {
        val sunderSigilSession = ClassFormalizationTestSupport.newSession(tempDir.resolve("sunder-sigil"), professionId = "spellblade")
        ClassFormalizationTestSupport.clearMonsters(sunderSigilSession)
        val sunderSigilDummyId = ClassFormalizationTestSupport.installCombatDummy(sunderSigilSession)
        val sunderSigilDummyPoint = ClassFormalizationTestSupport.entityPoint(sunderSigilSession, sunderSigilDummyId)
        val sunderSigilWorld = ClassFormalizationTestSupport.runtimeWorld(sunderSigilSession)
        val sunderSigilSlot = ensureTalentEquipped(sunderSigilSession, "sunder_sigil", slot = 3)
        val sunderSigilManaPool = ClassFormalizationTestSupport.resourcePool(sunderSigilSession, ResourceType.MANA)
        sunderSigilManaPool.current = sunderSigilManaPool.max
        val sunderSigilHpBefore = ClassFormalizationTestSupport.monsterHp(sunderSigilSession, sunderSigilDummyId)
        assertTrue(sunderSigilSession.perform(PlayerCommand.UseTalent(sunderSigilSlot, sunderSigilDummyPoint)))
        assertTrue(ClassFormalizationTestSupport.playerCooldown(sunderSigilSession, "sunder_sigil") > 0)
        assertTrue(ClassFormalizationTestSupport.monsterHp(sunderSigilSession, sunderSigilDummyId) < sunderSigilHpBefore)
        val sunderSigilTracker = requireNotNull(sunderSigilWorld.get<StatusTracker>(sunderSigilDummyId))
        assertTrue(sunderSigilTracker.activeEffects().any { effect -> effect.schemaId == "ARMOR_BREAK" })

        val balancePointSession = ClassFormalizationTestSupport.newSession(tempDir.resolve("balance-point"), professionId = "spellblade")
        val balancePointWorld = ClassFormalizationTestSupport.runtimeWorld(balancePointSession)
        val balancePointSlot = ensureTalentEquipped(balancePointSession, "balance_point", slot = 3)
        val balancePointManaPool = ClassFormalizationTestSupport.resourcePool(balancePointSession, ResourceType.MANA)
        balancePointManaPool.current = balancePointManaPool.max
        assertTrue(balancePointSession.perform(PlayerCommand.UseTalent(balancePointSlot)))
        assertTrue(ClassFormalizationTestSupport.playerCooldown(balancePointSession, "balance_point") > 0)
        val balancePointTracker = requireNotNull(balancePointWorld.get<StatusTracker>(balancePointSession.playerId))
        assertTrue(balancePointTracker.activeEffects().any { effect -> effect.schemaId == "MANA_SURGE_BUFF" })

        val blinkStrikeSession = ClassFormalizationTestSupport.newSession(tempDir.resolve("blink-strike"), professionId = "spellblade")
        ClassFormalizationTestSupport.clearMonsters(blinkStrikeSession)
        val blinkStrikeTarget = chargeTargetPoint(blinkStrikeSession)
        val blinkStrikeDummyId = ClassFormalizationTestSupport.installCombatDummy(blinkStrikeSession, position = blinkStrikeTarget)
        val blinkStrikeSlot = ensureTalentEquipped(blinkStrikeSession, "blink_strike", slot = 3)
        val blinkStrikeManaPool = ClassFormalizationTestSupport.resourcePool(blinkStrikeSession, ResourceType.MANA)
        blinkStrikeManaPool.current = blinkStrikeManaPool.max
        val blinkStrikeBefore = blinkStrikeSession.playerPosition()
        val blinkStrikeHpBefore = ClassFormalizationTestSupport.monsterHp(blinkStrikeSession, blinkStrikeDummyId)
        assertTrue(blinkStrikeSession.perform(PlayerCommand.UseTalent(blinkStrikeSlot, blinkStrikeTarget)))
        assertTrue(ClassFormalizationTestSupport.playerCooldown(blinkStrikeSession, "blink_strike") > 0)
        assertTrue(blinkStrikeSession.playerPosition() != blinkStrikeBefore)
        assertTrue(ClassFormalizationTestSupport.monsterHp(blinkStrikeSession, blinkStrikeDummyId) < blinkStrikeHpBefore)

        val counterSealSession = ClassFormalizationTestSupport.newSession(tempDir.resolve("counter-seal"), professionId = "spellblade")
        val counterSealWorld = ClassFormalizationTestSupport.runtimeWorld(counterSealSession)
        val counterSealSlot = ensureTalentEquipped(counterSealSession, "counter_seal", slot = 3)
        val counterSealManaPool = ClassFormalizationTestSupport.resourcePool(counterSealSession, ResourceType.MANA)
        counterSealManaPool.current = counterSealManaPool.max
        assertTrue(counterSealSession.perform(PlayerCommand.UseTalent(counterSealSlot)))
        assertTrue(ClassFormalizationTestSupport.playerCooldown(counterSealSession, "counter_seal") > 0)
        val counterSealTracker = requireNotNull(counterSealWorld.get<StatusTracker>(counterSealSession.playerId))
        assertTrue(counterSealTracker.activeEffects().any { effect -> effect.schemaId == "HOLY_SHIELD_BUFF" })
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
