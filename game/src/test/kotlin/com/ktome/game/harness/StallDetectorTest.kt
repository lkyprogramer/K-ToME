package com.ktome.game.harness

import com.ktome.game.FoundationGameSession
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FOUNDATION_PROFESSION_ID
import com.ktome.game.GameModule
import org.junit.jupiter.api.Assertions.assertEquals
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

    private fun sampleObservation(): RunObservation {
        val session =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260312L, playerProfessionId = FOUNDATION_PROFESSION_ID),
            )
        return RunObservationCapture.capture(session, 0)
    }
}
