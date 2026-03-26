package com.ktome.core.profile

import com.ktome.core.item.ItemQuality
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.item.EquipSlot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ProfileDataTest {
    @Test
    fun `profile codec round trips release unlocks and run history`() {
        val profile =
            ProfileData(
                releaseUnlockedClasses = setOf("berserker"),
                runHistory =
                    listOf(
                        RunSummary(
                            seed = 1L,
                            finishedAtEpochMillis = 2L,
                            classId = "vanguard",
                            raceId = "human",
                            finalZoneId = "abyssal_heart",
                            turnCount = 345,
                            headlessTurnEquivalent = 345,
                            zoneRouteHash = "route",
                            buildHash = "build",
                            milestoneRewards =
                                listOf(
                                    MilestoneRewardSummary(
                                        rewardSource = MilestoneRewardSource.ROUTE,
                                        sourceId = "route.greenwood_fringe.deep_iron_pit",
                                        zoneId = "greenwood_fringe",
                                        baseItemId = "forgebreaker_pick",
                                        equipSlot = EquipSlot.WEAPON,
                                        qualityTier = ItemQuality.MAGIC,
                                        buildHashAtGrant = "grant-build",
                                        affixIds = listOf("flaming"),
                                        equippedBaseItemIdBeforeReward = "arcane_staff",
                                        equippedBaseItemIdAtRunEnd = "forgebreaker_pick",
                                        adoptedInFinalBuild = true,
                                    ),
                                ),
                            rulesetVersion = "phase3",
                            victory = true,
                        ),
                    ),
            )

        val restored = ProfileCodec().decode(ProfileCodec().encode(profile))

        assertEquals(profile, restored)
    }

    @Test
    fun `default profile starts with no release unlocks`() {
        val profile = ProfileData()

        assertTrue(profile.releaseUnlockedClasses.isEmpty())
        assertTrue(profile.runHistory.isEmpty())
    }

    @Test
    fun `profile codec rejects missing top level required fields`() {
        val profile = ProfileData()
        val codec = ProfileCodec()
        val json = Json { prettyPrint = true }
        val root = json.parseToJsonElement(codec.encode(profile)).jsonObject
        val corrupted = JsonObject(root.filterKeys { key -> key != "runHistory" })

        assertThrows(IllegalArgumentException::class.java) {
            codec.decode(json.encodeToString(JsonObject.serializer(), corrupted))
        }
    }

    @Test
    fun `profile codec rejects run history entries missing pr06 fields`() {
        val profile =
            ProfileData(
                runHistory =
                    listOf(
                        RunSummary(
                            seed = 1L,
                            finishedAtEpochMillis = 2L,
                            classId = "vanguard",
                            raceId = "human",
                            finalZoneId = "abyssal_heart",
                            turnCount = 345,
                            headlessTurnEquivalent = 345,
                            zoneRouteHash = "route",
                            zonePath = listOf("shattered_outpost", "abyssal_heart"),
                            claimedRouteRewardIds = listOf("route.shattered_outpost.greenwood_fringe"),
                            buildHash = "build",
                            rulesetVersion = "phase3",
                            victory = true,
                        ),
                    ),
            )
        val codec = ProfileCodec()
        val json = Json { prettyPrint = true }
        val root = json.parseToJsonElement(codec.encode(profile)).jsonObject
        val runHistory = root.getValue("runHistory").jsonArray
        val firstSummary = runHistory.first().jsonObject
        val corruptedSummary = JsonObject(firstSummary.filterKeys { key -> key != "claimedRouteRewardIds" })
        val corrupted =
            JsonObject(
                root.toMutableMap().apply {
                    put("runHistory", JsonArray(listOf(corruptedSummary)))
                },
            )

        assertThrows(IllegalArgumentException::class.java) {
            codec.decode(json.encodeToString(JsonObject.serializer(), corrupted))
        }
    }

    @Test
    fun `profile codec rejects outdated profile versions before validating run history fields`() {
        val codec = ProfileCodec()
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                codec.decode(
                    """
                    {
                      "profileVersion": 1,
                      "releaseUnlockedClasses": [],
                      "runHistory": [
                        {
                          "seed": 1,
                          "finishedAtEpochMillis": 2,
                          "classId": "vanguard",
                          "raceId": "human",
                          "finalZoneId": "abyssal_heart",
                          "turnCount": 345,
                          "headlessTurnEquivalent": 345,
                          "zoneRouteHash": "route",
                          "buildHash": "build",
                          "rulesetVersion": "phase3",
                          "victory": true
                        }
                      ]
                    }
                    """.trimIndent(),
                )
            }

        assertTrue(requireNotNull(exception.message).contains("Unsupported profile version 1"))
    }
}
