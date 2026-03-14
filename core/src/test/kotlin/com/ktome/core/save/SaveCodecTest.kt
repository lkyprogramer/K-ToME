package com.ktome.core.save

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SaveCodecTest {
    private val codec = SaveCodec()

    @Test
    fun `encoded save includes explicit contract fields and omits presentation payload`() {
        val encoded = codec.encode(SaveFixtures.resourceHeavyScene())

        assertTrue(encoded.contains("\"saveContractVersion\""))
        assertTrue(encoded.contains("\"schemaVersion\""))
        assertFalse(encoded.contains("glyph"))
        assertFalse(encoded.contains("colorHex"))
        assertFalse(encoded.contains("\"messageLog\""))
        assertFalse(encoded.contains("\"name\""))
    }
}
