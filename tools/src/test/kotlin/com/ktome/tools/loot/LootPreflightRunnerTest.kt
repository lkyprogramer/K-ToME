package com.ktome.tools.loot

import com.ktome.core.item.MilestoneRewardSource
import com.ktome.game.loot.MilestoneRewardRejectionReason
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LootPreflightRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("verifyLootPreflight")
    fun `loot preflight writes culprit diff friendly structure`() {
        System.setProperty("ktome.phase4.loot.preflight.reportDir", tempDir.toString())
        val run = LootPreflightRunner.run()
        val json = Json.parseToJsonElement(run.detailsPath.readText()).jsonArray

        assertTrue(run.profileCount > 0)
        assertTrue(run.pairCount > 0)
        assertTrue(json.isNotEmpty())
        val firstPair = json.first().jsonObject
        assertTrue(firstPair.containsKey("sharedBaseIds"))
        assertTrue(firstPair.containsKey("leftOnlyBaseIds"))
        assertTrue(firstPair.containsKey("rightOnlyBaseIds"))
        val sourceBreakdown = firstPair.getValue("explicitVsTagMatched").jsonObject
        assertTrue(sourceBreakdown.containsKey("leftExplicitOnlyBaseIds"))
        assertTrue(sourceBreakdown.containsKey("rightTagMatchedOnlyBaseIds"))
        // Ensure culprit reasons stay serialized as stable strings for quick diff review.
        firstPair.getValue("culpritReasons").jsonArray.forEach { culpritReason ->
            culpritReason.jsonPrimitive.content
        }
    }

    @Test
    @Tag("verifyLootPreflight")
    fun `loot preflight keeps duplicate and preferred source coverage regressions green for current data`() {
        System.setProperty("ktome.phase4.loot.preflight.reportDir", tempDir.toString())
        val run = LootPreflightRunner.run()
        val summary = requireNotNull(LootPreflightRunner.readSummary(tempDir))

        assertEquals(run.profileCount, summary.profileCount)
        assertEquals(run.pairCount, summary.pairCount)
        assertEquals(run.culpritPairCount, summary.culpritPairCount)

        val duplicateSummary = summary.specialTierPassiveFamilyDuplicateSummary
        assertEquals(0, duplicateSummary.duplicateFamilyCount)
        assertEquals(0, duplicateSummary.duplicatedZoneCount)
        assertTrue(duplicateSummary.duplicateFamilies.isEmpty())

        val sourceCoverageSummary = summary.rewardRoutingCoverageSummary
        assertEquals(1.0, sourceCoverageSummary.professionCapstoneSourceCoverageRate)
        assertTrue(sourceCoverageSummary.professionSourceCoverage.isNotEmpty())
        assertTrue(sourceCoverageSummary.professionSourceCoverage.all(ProfessionCapstoneSourceCoverage::covered))
        assertTrue(sourceCoverageSummary.professionSourceCoverage.all { coverage -> coverage.coveredSourceIds.isNotEmpty() })
        assertTrue(
            sourceCoverageSummary.topRejectedCapstoneCandidates.all { candidate ->
                candidate.sourceId.isNotBlank() &&
                    candidate.zoneId.isNotBlank() &&
                    candidate.baseItemId.isNotBlank() &&
                    candidate.rejectionReason.name.isNotBlank()
            },
        )

        assertNoPreflightRegressions(summary)
    }

    @Test
    @Tag("verifyLootPreflight")
    fun `loot preflight regression fixture requires culprit payloads for duplicate and source coverage failures`() {
        val failingSummary =
            LootPreflightSummary(
                profileCount = 2,
                pairCount = 1,
                culpritPairCount = 0,
                culpritPairs = emptyList(),
                specialTierPassiveFamilyDuplicateSummary =
                    SpecialTierPassiveFamilyDuplicateSummary(
                        duplicateFamilies =
                            listOf(
                                SpecialTierPassiveFamilyDuplicate(
                                    canonicalZoneId = "abyssal_heart",
                                    passiveFamily = "OnKillResourceRestore:ENERGY",
                                    templateIds = listOf("artifact.briar_heart", "unique.thornpath_crook"),
                                    itemIds = listOf("artifact_briar_heart", "unique_thornpath_crook"),
                                    professionTags = listOf("rogue"),
                                ),
                            ),
                    ),
                rewardRoutingCoverageSummary =
                    RewardRoutingCoverageSummary(
                        criticalSources =
                            listOf(
                                CriticalRewardRoutingSourceSummary(
                                    sourceId = "abyssal_heart/heart_ward_focus/SUPPORT_GRANT",
                                    zoneId = "abyssal_heart",
                                    interactableId = "heart_ward_focus",
                                    rewardSource = MilestoneRewardSource.SUPPORT,
                                    profileIds = listOf("loot.abyssal_heart.reward"),
                                    fallbackBaseId = "abyssal_heartstone",
                                    hasNonFoundationProfile = true,
                                ),
                            ),
                        professionSourceCoverage =
                            listOf(
                                ProfessionCapstoneSourceCoverage(
                                    professionId = "rogue",
                                    rewardSource = MilestoneRewardSource.BOSS,
                                    covered = false,
                                    coveredSourceIds = emptyList(),
                                    culpritSourceIds = listOf("abyssal_guardian_encounter"),
                                ),
                            ),
                        topRejectedCapstoneCandidates =
                            listOf(
                                RejectedCapstoneCandidateSummary(
                                    professionId = "rogue",
                                    rewardSource = MilestoneRewardSource.BOSS,
                                    sourceId = "abyssal_guardian_encounter",
                                    zoneId = "abyssal_heart",
                                    baseItemId = "artifact_briar_heart",
                                    rejectionReason = MilestoneRewardRejectionReason.SPECIAL_TEMPLATE_ZONE_MISMATCH,
                                ),
                            ),
                    ),
            )

        val error = assertThrows(AssertionError::class.java) { assertNoPreflightRegressions(failingSummary) }
        val message = requireNotNull(error.message)
        assertTrue(message.contains("specialTierPassiveFamilyDuplicateSummary"))
        assertTrue(message.contains("abyssal_heart"))
        assertTrue(message.contains("OnKillResourceRestore:ENERGY"))
        assertTrue(message.contains("professionCapstoneSourceCoverage"))
        assertTrue(message.contains("rogue:BOSS"))
        assertTrue(message.contains("abyssal_guardian_encounter"))
        assertTrue(message.contains("SPECIAL_TEMPLATE_ZONE_MISMATCH"))
    }

    private fun assertNoPreflightRegressions(summary: LootPreflightSummary) {
        val duplicateSummary = summary.specialTierPassiveFamilyDuplicateSummary
        val failures = mutableListOf<String>()
        if (duplicateSummary.duplicateFamilies.isNotEmpty()) {
            failures +=
                "specialTierPassiveFamilyDuplicateSummary regressed: " +
                    duplicateSummary.duplicateFamilies.joinToString(separator = "; ") { duplicate ->
                        "${duplicate.canonicalZoneId}/${duplicate.passiveFamily}" +
                            "[templates=${duplicate.templateIds.joinToString()}, items=${duplicate.itemIds.joinToString()}]"
                    }
        }
        val uncoveredPairs = summary.rewardRoutingCoverageSummary.professionSourceCoverage.filterNot(ProfessionCapstoneSourceCoverage::covered)
        val rejectedCandidates = summary.rewardRoutingCoverageSummary.topRejectedCapstoneCandidates
        if (uncoveredPairs.isNotEmpty()) {
            failures +=
                "professionCapstoneSourceCoverage regressed: " +
                    uncoveredPairs.joinToString(separator = "; ") { coverage ->
                        val rejectionNote =
                            rejectedCandidates
                                .filter { candidate ->
                                    candidate.professionId == coverage.professionId &&
                                        candidate.rewardSource == coverage.rewardSource &&
                                        candidate.sourceId in coverage.culpritSourceIds
                                }.joinToString(separator = ",") { candidate ->
                                    "${candidate.sourceId}:${candidate.baseItemId}:${candidate.rejectionReason.name}"
                                }.ifBlank { "noRejectedCandidates" }
                        "${coverage.professionId}:${coverage.rewardSource.name}" +
                            "[culprits=${coverage.culpritSourceIds.joinToString().ifBlank { "none" }}, rejected=$rejectionNote]"
                    }
        }
        if (uncoveredPairs.isNotEmpty() && rejectedCandidates.isEmpty()) {
            failures += "professionCapstoneSourceCoverage regressions must keep topRejectedCapstoneCandidates for culprit inspection."
        }
        assertFalse(failures.isNotEmpty(), failures.joinToString(separator = " | "))
    }
}
