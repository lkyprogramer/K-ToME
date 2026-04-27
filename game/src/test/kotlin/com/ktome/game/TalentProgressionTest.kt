package com.ktome.game

import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TalentProgressionTest {
    @Test
    fun `tier investment gates use committed learned ranks only`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val profession = catalog.professions.first { profession -> profession.id == "vanguard" }
        val request =
            TalentProgressionRequest(
                schemaCatalog = catalog,
                profession = profession,
                level = 3,
                learnedRanks =
                    mapOf(
                        "power_strike" to 1,
                        "shield_bash" to 1,
                    ),
            )

        val lockReasons = TalentProgression.talentLockReasons(request, "sweeping_strike")

        assertTrue(
            lockReasons.any { reason ->
                reason.type == TalentLockReasonType.TREE_INVESTMENT &&
                    reason.requiredPoints == 2 &&
                    reason.currentPoints == 1
            },
        )
        assertFalse(TalentProgression.learnableTalentIds(request).contains("sweeping_strike"))
    }
}
