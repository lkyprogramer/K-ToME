package com.ktome.core.progression

import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.Health
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExperienceSystemTest {
    @Test
    fun grantWithoutLevelUpKeepsProgress() {
        val experience = Experience()
        val health = Health(current = 10, max = 10)

        val result = ExperienceSystem.applyReward(experience = experience, health = health, reward = 20)

        assertFalse(result.healthRestored)
        assertEquals(20, experience.current)
        assertEquals(1, experience.level)
    }

    @Test
    fun levelUpAwardsPointsAndRestoresResources() {
        val experience = Experience(current = 140)
        val health = Health(current = 5, max = 10)

        val result = ExperienceSystem.applyReward(experience = experience, health = health, reward = 20)

        assertTrue(result.healthRestored)
        assertEquals(2, experience.level)
        assertEquals(10, health.current)
        assertEquals(2, experience.unspentStatPoints)
        assertEquals(0, experience.unspentTalentPoints)
    }

    @Test
    fun multiLevelGainAwardsTalentPointsEveryOtherLevel() {
        val experience = Experience()
        val health = Health(current = 8, max = 8)

        ExperienceSystem.applyReward(experience = experience, health = health, reward = 450)

        assertEquals(3, experience.level)
        assertEquals(4, experience.unspentStatPoints)
        assertEquals(1, experience.unspentTalentPoints)
        assertEquals(50, experience.current)
    }
}
