package com.ktome.game

import com.ktome.core.resource.ResourceType
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
        val neutralSlot = ClassFormalizationTestSupport.talentSlot(session, "flux_anchor")
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
}
