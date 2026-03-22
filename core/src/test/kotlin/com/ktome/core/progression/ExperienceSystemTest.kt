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
        assertEquals(0, experience.unspentTalentPoints)
    }

    @Test
    fun multiLevelGainAwardsTalentPointsEveryOtherLevel() {
        val experience = Experience()

        ExperienceSystem.applyReward(experience = experience, reward = 450)

        assertEquals(3, experience.level)
        assertEquals(4, experience.unspentStatPoints)
        assertEquals(1, experience.unspentTalentPoints)
        assertEquals(50, experience.current)
    }
}
