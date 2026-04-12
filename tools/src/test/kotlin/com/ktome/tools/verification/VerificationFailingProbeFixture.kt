package com.ktome.tools.verification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("verificationFixtureFailure")
class VerificationFailingProbeFixture {
    @Test
    fun `passing fixture probe still passes`() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `failing fixture probe is captured`() {
        assertEquals(5, 2 + 2)
    }
}
