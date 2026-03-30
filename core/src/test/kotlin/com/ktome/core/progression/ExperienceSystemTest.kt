package com.ktome.core.progression

import com.ktome.core.ecs.Experience
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExperienceSystemTest {
    @Test
    fun grantWithoutLevelUpKeepsProgress() {
        val experience = Experience()

        val result = ExperienceSystem.applyReward(experience = experience, reward = 20)

        assertFalse(result.shouldRestoreHealthToMax)
        assertFalse(result.shouldRestorePrimaryResourceToMax)
        assertEquals(20, experience.current)
        assertEquals(1, experience.level)
    }

    @Test
    fun levelUpAwardsPointsAndRestoresResources() {
        val experience = Experience(current = 140)
        val result = ExperienceSystem.applyReward(experience = experience, reward = 20)

        assertTrue(result.shouldRestoreHealthToMax)
        assertTrue(result.shouldRestorePrimaryResourceToMax)
        assertEquals(2, experience.level)
        assertEquals(2, experience.unspentStatPoints)
        assertEquals(1, experience.unspentTalentPoints)
        assertEquals(2, result.gainedStatPoints)
        assertEquals(1, result.gainedTalentPoints)
    }

    @Test
    fun multiLevelGainAwardsTalentPointsOnEveryLevel() {
        val experience = Experience()

        ExperienceSystem.applyReward(experience = experience, reward = 450)

        assertEquals(3, experience.level)
        assertEquals(4, experience.unspentStatPoints)
        assertEquals(2, experience.unspentTalentPoints)
        assertEquals(50, experience.current)
    }

    @Test
    fun `level 1 to 20 grants nineteen talent points and thirty eight stat points`() {
        val experience = Experience()
        var totalReward = 0
        for (level in 1 until 20) {
            totalReward += ExperienceSystem.nextLevelExp(level)
        }

        val result = ExperienceSystem.applyReward(experience = experience, reward = totalReward)

        assertEquals(20, experience.level)
        assertEquals(0, experience.current)
        assertEquals(19, experience.unspentTalentPoints)
        assertEquals(38, experience.unspentStatPoints)
        assertEquals(19, result.gainedTalentPoints)
        assertEquals(38, result.gainedStatPoints)
        assertEquals(19, result.levelsGained)
    }
}
