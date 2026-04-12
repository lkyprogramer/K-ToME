package com.ktome.tools.verification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificationDemoProbeTest {
    @Test
    fun `first probe still passes`() {
        assertTrue(true)
    }

    @Test
    fun `second probe still passes`() {
        assertEquals(4, 2 + 2)
    }
}
