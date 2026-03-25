package com.ktome.core.race

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RaceSystemTest {
    @Test
    fun `race talent points are granted every four levels`() {
        assertEquals(0, RaceTalentPointProgression.totalGrantedByLevel(3))
        assertEquals(1, RaceTalentPointProgression.totalGrantedByLevel(4))
        assertEquals(2, RaceTalentPointProgression.totalGrantedByLevel(8))
    }

    @Test
    fun `delta only counts newly crossed race thresholds`() {
        assertEquals(1, RaceTalentPointProgression.deltaForLevelRange(previousLevel = 3, nextLevel = 4))
        assertEquals(1, RaceTalentPointProgression.deltaForLevelRange(previousLevel = 7, nextLevel = 8))
        assertEquals(0, RaceTalentPointProgression.deltaForLevelRange(previousLevel = 8, nextLevel = 9))
    }
}
