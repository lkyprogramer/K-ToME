package com.ktome.game

import com.ktome.core.resource.ResourceType
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SpellbladeEquilibriumAffinityTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `arcane and physical talents push equilibrium in opposite directions`() {
        val session = ClassFormalizationTestSupport.newSession(tempDir, professionId = "spellblade")
        ClassFormalizationTestSupport.clearMonsters(session)
        val dummyId = ClassFormalizationTestSupport.installCombatDummy(session)
        val dummyPoint = ClassFormalizationTestSupport.entityPoint(session, dummyId)
        val manaPool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.MANA)
        val equilibriumPool = ClassFormalizationTestSupport.resourcePool(session, ResourceType.EQUILIBRIUM)
        val arcaneSlot = ClassFormalizationTestSupport.talentSlot(session, "arcane_edge")
        val physicalSlot = ClassFormalizationTestSupport.talentSlot(session, "mana_lunge")

        manaPool.current = manaPool.max
        equilibriumPool.current = 50

        assertTrue(session.perform(PlayerCommand.UseTalent(arcaneSlot, dummyPoint)))
        val afterArcane = requireNotNull(session.playerResourceView().secondary).current
        assertTrue(afterArcane > 50)

        manaPool.current = manaPool.max
        assertTrue(session.perform(PlayerCommand.UseTalent(physicalSlot, dummyPoint)))
        val afterPhysical = requireNotNull(session.playerResourceView().secondary).current
        assertTrue(afterPhysical < afterArcane)
    }
}
