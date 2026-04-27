package com.ktome.game

import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TalentProgressionTest {
    @Test
    fun `starter talent materialization honors unlock level gates`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val berserker = catalog.professions.first { profession -> profession.id == "berserker" }
        val spellblade = catalog.professions.first { profession -> profession.id == "spellblade" }

        val berserkerLevelOne =
            TalentProgression.startingTalentIds(
                TalentProgressionRequest(
                    schemaCatalog = catalog,
                    profession = berserker,
                    level = 1,
                    learnedRanks = emptyMap(),
                ),
            )
        val spellbladeLevelOne =
            TalentProgression.startingTalentIds(
                TalentProgressionRequest(
                    schemaCatalog = catalog,
                    profession = spellblade,
                    level = 1,
                    learnedRanks = emptyMap(),
                ),
            )

        assertEquals(listOf("blood_rush", "savage_hew"), berserkerLevelOne)
        assertFalse(berserkerLevelOne.contains("kill_frenzy"))
        assertEquals(listOf("arcane_edge", "mana_lunge"), spellbladeLevelOne)
        assertFalse(spellbladeLevelOne.contains("spell_parry"))

        val berserkerLevelTwo =
            TalentProgression.startingTalentIds(
                TalentProgressionRequest(
                    schemaCatalog = catalog,
                    profession = berserker,
                    level = 2,
                    learnedRanks = emptyMap(),
                ),
            )
        val spellbladeLevelTwo =
            TalentProgression.startingTalentIds(
                TalentProgressionRequest(
                    schemaCatalog = catalog,
                    profession = spellblade,
                    level = 2,
                    learnedRanks = emptyMap(),
                ),
            )

        assertEquals(listOf("blood_rush", "savage_hew", "kill_frenzy"), berserkerLevelTwo)
        assertEquals(listOf("arcane_edge", "mana_lunge", "spell_parry"), spellbladeLevelTwo)
    }

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
