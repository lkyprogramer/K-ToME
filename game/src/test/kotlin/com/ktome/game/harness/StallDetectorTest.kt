package com.ktome.game.harness

import com.ktome.game.FoundationGameSession
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FOUNDATION_PROFESSION_ID
import com.ktome.game.GameModule
import com.ktome.game.TalentReserveView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StallDetectorTest {
    @Test
    fun `stall detector triggers after repeated identical observations`() {
        val detector = StallDetector(maxRepeats = 3)
        val observation = sampleObservation()

        assertNull(detector.observe(observation))
        assertNull(detector.observe(observation))
        val reason = detector.observe(observation)
        assertEquals("Repeated state ${observation.signature()} for 3 observations.", reason)
    }

    @Test
    fun `signature changes when loadout remap changes active or reserve talents`() {
        val baseline = sampleObservation()
        val remapped =
            baseline.copy(
                talentSlots =
                    baseline.talentSlots.map { slot ->
                        if (slot.slot == 1) {
                            slot.copy(talentId = "charge")
                        } else {
                            slot
                        }
                    },
                reserveTalents =
                    baseline.reserveTalents +
                        TalentReserveView(
                            talentId = "war_cry",
                            name = "War Cry",
                            level = 1,
                            maxLevel = 5,
                            resourceCost = 12,
                            range = 0,
                            minRange = 0,
                            currentCooldown = 0,
                            maxCooldown = 6,
                            requiresTarget = false,
                        ),
            )

        assertNotEquals(baseline.signature(), remapped.signature())
    }

    private fun sampleObservation(): RunObservation {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260312L, playerProfessionId = FOUNDATION_PROFESSION_ID),
            )
        return RunObservationCapture.capture(session, 0)
    }
}
