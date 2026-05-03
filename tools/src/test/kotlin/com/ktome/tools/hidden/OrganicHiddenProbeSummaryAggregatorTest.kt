package com.ktome.tools.hidden

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OrganicHiddenProbeSummaryAggregatorTest {
    @Test
    fun `primer-only hidden events do not count as lead discovery or first hidden discovery`() {
        val summary =
            OrganicHiddenProbeSummaryAggregator.summarize(
                OrganicHiddenProbeSummaryRequest(
                    results =
                        listOf(
                            caseResult(seed = 1L, hiddenEventIds = listOf("primer.only"), firstHiddenDiscoveryTurn = null),
                            caseResult(seed = 2L, searchRevealCount = 1, firstHiddenDiscoveryTurn = 7),
                        ),
                    professionIds = listOf("vanguard"),
                    raceIds = listOf("human"),
                    seedsPerZoneCombo = 2,
                    perZoneSecretEntryMinRate = 0.05,
                ),
            )

        assertEquals(1, summary.discoveryWithoutPrimerCount)
        assertEquals(0.5, summary.leadDiscoveryRate)
        assertEquals(0.5, summary.zoneSearchPromptVisibility)
        assertEquals(7.0, summary.averageFirstHiddenDiscoveryTurn)
        assertEquals(7, summary.firstHiddenDiscoveryTurnP50)
        assertNull(summary.zoneBreakdown.getValue("greenwood_fringe").averageFirstSecretZoneEntryTurn)
    }

    @Test
    fun `failing secret entry zone ids reflect per-zone threshold and secret conversion`() {
        val summary =
            OrganicHiddenProbeSummaryAggregator.summarize(
                OrganicHiddenProbeSummaryRequest(
                    results =
                        listOf(
                            caseResult(zoneId = "greenwood_fringe", seed = 1L, searchRevealCount = 1, secretZoneIds = listOf("secret.greenwood"), firstHiddenDiscoveryTurn = 5, firstSecretZoneEntryTurn = 7),
                            caseResult(zoneId = "greenwood_fringe", seed = 2L, searchRevealCount = 1, firstHiddenDiscoveryTurn = 6),
                            caseResult(zoneId = "abyssal_temple", seed = 3L, searchRevealCount = 1, firstHiddenDiscoveryTurn = 4),
                            caseResult(zoneId = "abyssal_temple", seed = 4L, searchRevealCount = 1, firstHiddenDiscoveryTurn = 8),
                        ),
                    professionIds = listOf("vanguard"),
                    raceIds = listOf("human"),
                    seedsPerZoneCombo = 2,
                    perZoneSecretEntryMinRate = 0.50,
                ),
            )

        assertEquals(listOf("abyssal_temple"), summary.failingSecretEntryZoneIds)
        assertEquals(0.25, summary.secretZoneEntryRate)
        assertEquals(0.5, summary.zoneDiscoveryDistribution.getValue("greenwood_fringe"))
        assertEquals(1.0, summary.secretZoneDiscoveryDistribution.getValue("secret.greenwood"))
        assertEquals(0.5, summary.zoneBreakdown.getValue("greenwood_fringe").secretConversionRate)
        assertEquals(0.0, summary.zoneBreakdown.getValue("abyssal_temple").secretZoneEntryRate)
        assertEquals(1.0, summary.topZoneLeadShare)
        assertEquals("abyssal_temple", summary.topZoneLeadShareZoneId)
        assertEquals(0.5, summary.secretZoneSearchConversionFloorReportOnly.getValue("greenwood_fringe"))
        assertEquals(1.0, summary.perZoneSearchUseFloorReportOnly.getValue("abyssal_temple"))
    }

    @Test
    fun `slag cue density uses measured cue candidates per eligible room`() {
        val summary =
            OrganicHiddenProbeSummaryAggregator.summarize(
                OrganicHiddenProbeSummaryRequest(
                    results =
                        listOf(
                            caseResult(zoneId = "deep_iron_pit", seed = 1L, slagCueEligibleRoomCount = 4, slagCueCandidateCount = 1),
                            caseResult(zoneId = "deep_iron_pit", seed = 2L, slagCueEligibleRoomCount = 2, slagCueCandidateCount = 2),
                            caseResult(zoneId = "greenwood_fringe", seed = 3L, slagCueEligibleRoomCount = 10, slagCueCandidateCount = 10),
                        ),
                    professionIds = listOf("vanguard"),
                    raceIds = listOf("human"),
                    seedsPerZoneCombo = 2,
                    perZoneSecretEntryMinRate = 0.05,
                ),
            )

        assertEquals(0.5, summary.slagCueDensityPerEligibleRoomReportOnly.getValue("deep_iron_pit"))
    }

    private fun caseResult(
        zoneId: String = "greenwood_fringe",
        seed: Long,
        searchRevealCount: Int = 0,
        hiddenEventIds: List<String> = emptyList(),
        secretZoneIds: List<String> = emptyList(),
        firstHiddenDiscoveryTurn: Int? = null,
        firstSecretZoneEntryTurn: Int? = null,
        slagCueEligibleRoomCount: Int = 0,
        slagCueCandidateCount: Int = 0,
    ): OrganicHiddenProbeCaseResult =
        OrganicHiddenProbeCaseResult(
            zoneId = zoneId,
            floorIndex = 1,
            professionId = "vanguard",
            raceId = "human",
            seed = seed,
            turnCount = 12,
            searchAttemptCount = if (searchRevealCount > 0) 1 else 0,
            searchActionUseCount = if (searchRevealCount > 0) 1 else 0,
            searchPromptVisibleCount = if (searchRevealCount > 0) 1 else 0,
            searchRevealCount = searchRevealCount,
            slagCueEligibleRoomCount = slagCueEligibleRoomCount,
            slagCueCandidateCount = slagCueCandidateCount,
            hiddenEventIds = hiddenEventIds,
            secretZoneIds = secretZoneIds,
            firstHiddenDiscoveryTurn = firstHiddenDiscoveryTurn,
            firstSecretZoneEntryTurn = firstSecretZoneEntryTurn,
            lastCommands = listOf("WAIT"),
            finalZoneId = zoneId,
            finalFloor = 1,
        )
}
