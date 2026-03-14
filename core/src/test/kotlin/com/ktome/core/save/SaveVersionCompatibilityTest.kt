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
                .replace("\"major\": 2", "\"major\": 99")

        val exception =
            assertThrows(UnsupportedSaveContractVersionException::class.java) {
                codec.decode(encoded)
            }

        assertTrue(exception.message!!.contains("99.0"))
    }

    @Test
    fun `invalid save contract payload is normalized to invalid save exception`() {
        val encoded =
            codec.encode(SaveFixtures.emptyScene())
                .replace("\"major\": 2", "\"major\": 0")

        val exception =
            assertThrows(InvalidSaveException::class.java) {
                codec.decode(encoded)
            }

        assertTrue(exception.message!!.contains("saveContractVersion"))
    }

    @Test
    fun `semantically invalid current contract save is normalized to invalid save exception`() {
        val invalidCurrentContractSave =
            """
            {
              "schemaVersion": 1,
              "saveContractVersion": { "major": 2, "minor": 0 },
              "buildMetadata": "phase2-dev",
              "timestampEpochMillis": 1,
              "worldSeed": 42,
              "currentZoneId": "foundation_dungeon",
              "floorIndex": 1,
              "mapWidth": 80,
              "mapHeight": 50,
              "fovRadius": 8,
              "messageLogSize": 8,
              "playerProfessionId": "foundation_hero",
              "maxFloor": 1,
              "turnCount": 0,
              "player": { "entity": { "id": 1, "isPlayerControlled": true } },
              "floors": [
                {
                  "floorIndex": 1,
                  "map": {
                    "rows": [],
                    "playerStart": { "x": 0, "y": 0 }
                  }
                }
              ]
            }
            """.trimIndent()

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
