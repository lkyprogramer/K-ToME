package com.ktome.core.save

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
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

    @Test
    fun `decode rejects floor payloads whose dimensions drift from top level map contract`() {
        val raw =
            """
            {
              "schemaVersion": ${SaveSnapshot.CURRENT_SCHEMA_VERSION},
              "saveContractVersion": { "major": ${SaveContractVersion.CURRENT.major}, "minor": ${SaveContractVersion.CURRENT.minor} },
              "buildMetadata": "phase2-dev",
              "timestampEpochMillis": 1,
              "worldSeed": 20260318,
              "currentZoneId": "greenwood_fringe",
              "floorIndex": 1,
              "mapWidth": 70,
              "mapHeight": 45,
              "fovRadius": 8,
              "messageLogSize": 8,
              "playerProfessionId": "vanguard",
              "maxFloor": 2,
              "turnCount": 0,
              "player": {
                "entity": {
                  "id": 1,
                  "position": { "x": 1, "y": 1 },
                  "isPlayerControlled": true
                },
                "carriedEntities": []
              },
              "floors": [
                {
                  "floorIndex": 1,
                  "map": {
                    "rows": [".....", ".....", ".....", ".....", "....."],
                    "playerStart": { "x": 1, "y": 1 }
                  },
                  "exploredTiles": [],
                  "entities": []
                }
              ],
              "pendingActionIds": []
            }
            """.trimIndent()

        assertThrows(InvalidSaveException::class.java) {
            codec.decode(raw)
        }
    }
}
