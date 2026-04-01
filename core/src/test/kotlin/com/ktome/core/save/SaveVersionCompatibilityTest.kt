package com.ktome.core.save

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SaveVersionCompatibilityTest {
    private val codec = SaveCodec()

    @Test
    fun `phase1 legacy save is rejected with explicit failure`() {
        val exception =
            assertThrows(LegacySaveFormatException::class.java) {
                codec.decode("""{"version":2,"timestampEpochMillis":123}""")
            }

        assertTrue(exception.message!!.contains("Legacy saves"))
    }

    @Test
    fun `unsupported save contract version fails fast`() {
        val encoded =
            codec.encode(SaveFixtures.emptyScene())
                .replace("\"major\": ${SaveContractVersion.CURRENT.major}", "\"major\": 99")

        val exception =
            assertThrows(UnsupportedSaveContractVersionException::class.java) {
                codec.decode(encoded)
            }

        assertTrue(exception.message!!.contains("99.${SaveContractVersion.CURRENT.minor}"))
    }

    @Test
    fun `invalid save contract payload is normalized to invalid save exception`() {
        val encoded =
            codec.encode(SaveFixtures.emptyScene())
                .replace("\"major\": ${SaveContractVersion.CURRENT.major}", "\"major\": 0")

        val exception =
            assertThrows(InvalidSaveException::class.java) {
                codec.decode(encoded)
            }

        assertTrue(exception.message!!.contains("saveContractVersion"))
    }

    @Test
    fun `save contract version rejects non positive major and negative minor`() {
        assertThrows(IllegalArgumentException::class.java) {
            SaveContractVersion(0, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SaveContractVersion(SaveContractVersion.CURRENT.major, -1)
        }
    }

    @Test
    fun `semantically invalid current contract save is normalized to invalid save exception`() {
        val invalidCurrentContractSave =
            codec.encode(SaveFixtures.emptyScene())
                .replace("\"routeIndex\": 0", "\"routeIndex\": 99")

        val exception =
            assertThrows(InvalidSaveException::class.java) {
                codec.decode(invalidCurrentContractSave)
            }

        assertTrue(
            exception.message!!.contains("save schema", ignoreCase = true) ||
                exception.message!!.contains("validation", ignoreCase = true),
        )
    }
}
