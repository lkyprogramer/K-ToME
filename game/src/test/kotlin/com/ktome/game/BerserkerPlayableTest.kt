package com.ktome.game

import com.ktome.core.resource.ResourceType
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
}
