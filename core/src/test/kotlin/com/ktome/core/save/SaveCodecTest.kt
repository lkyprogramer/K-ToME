package com.ktome.core.save

import com.ktome.core.loot.PityTracker
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertTrue(encoded.contains("\"phase4RunState\""))
        assertFalse(encoded.contains("glyph"))
        assertFalse(encoded.contains("colorHex"))
        assertFalse(encoded.contains("\"messageLog\""))
        assertFalse(encoded.contains("\"name\""))
    }

    @Test
    fun `save codec round trip preserves phase4 run state`() {
        val snapshot =
            SaveFixtures.resourceHeavyScene().copy(
                phase4RunState =
                    Phase4RunStateSnapshot(
                        pityTracker = PityTracker(rollsSinceLastRare = 6, eligibleSpecialRollsSinceLastUnique = 14),
                    ),
            )

        val restored = codec.decode(codec.encode(snapshot))

        assertEquals(snapshot.phase4RunState, restored.phase4RunState)
    }

    @Test
    fun `decode rejects save payloads missing phase4 run state`() {
        val json = Json { prettyPrint = true }
        val root = json.parseToJsonElement(codec.encode(SaveFixtures.emptyScene())).jsonObject
        val corrupted = JsonObject(root.filterKeys { key -> key != "phase4RunState" })

        assertThrows(InvalidSaveException::class.java) {
            codec.decode(json.encodeToString(JsonObject.serializer(), corrupted))
        }
    }

    @Test
    fun `decode rejects missing malformed and stale talent schema version`() {
        val json = Json { prettyPrint = true }
        val root = json.parseToJsonElement(codec.encode(SaveFixtures.emptyScene())).jsonObject
        val expectedMessage = "INCOMPATIBLE_PHASE4_V4_TALENT_SCHEMA: Start a new run."

        val missing = JsonObject(root.filterKeys { key -> key != "talentSchemaVersion" })
        val missingException =
            assertThrows(InvalidSaveException::class.java) {
                codec.decode(json.encodeToString(JsonObject.serializer(), missing))
            }
        assertEquals(expectedMessage, missingException.message)

        val malformed = JsonObject(root + ("talentSchemaVersion" to JsonPrimitive("legacy-unlocked")))
        val malformedException =
            assertThrows(InvalidSaveException::class.java) {
                codec.decode(json.encodeToString(JsonObject.serializer(), malformed))
            }
        assertEquals(expectedMessage, malformedException.message)

        val stale = JsonObject(root + ("talentSchemaVersion" to JsonPrimitive(SaveSnapshot.CURRENT_TALENT_SCHEMA_VERSION - 1)))
        val staleException =
            assertThrows(InvalidSaveException::class.java) {
                codec.decode(json.encodeToString(JsonObject.serializer(), stale))
            }
        assertEquals(expectedMessage, staleException.message)
    }

    @Test
    fun `snapshot validation rejects stale talent schema version`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                SaveFixtures.emptyScene().copy(talentSchemaVersion = SaveSnapshot.CURRENT_TALENT_SCHEMA_VERSION - 1)
            }

        assertEquals("INCOMPATIBLE_PHASE4_V4_TALENT_SCHEMA: Start a new run.", exception.message)
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
