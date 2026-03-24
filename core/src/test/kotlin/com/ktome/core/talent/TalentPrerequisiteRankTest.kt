package com.ktome.core.talent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TalentPrerequisiteRankTest {
    @Test
    fun `validator reports missing rank based prerequisites`() {
        val prerequisites =
            listOf(
                TalentPrerequisite(talentId = "power_strike", minRank = 2),
                TalentPrerequisite(talentId = "charge", minRank = 1),
            )

        val missing = TalentPrerequisiteValidator.missingPrerequisites(prerequisites, mapOf("power_strike" to 1, "charge" to 1))

        assertEquals(listOf(TalentPrerequisite("power_strike", 2)), missing)
        assertTrue(TalentPrerequisiteValidator.isSatisfied(prerequisites, mapOf("power_strike" to 2, "charge" to 1)).also { satisfied -> satisfied })
    }
}
