package com.ktome.tools.mapgen

internal object WhiteBoxSolvabilityFailLane {
    const val LANE_ID: String = "reveal-fail"
    private val fixtureZoneFloors: Set<SolvabilityHarnessRunner.SolvabilityZoneFloorKey> =
        linkedSetOf(
            SolvabilityHarnessRunner.SolvabilityZoneFloorKey(zoneId = "abyssal_temple", floorIndex = 1),
            SolvabilityHarnessRunner.SolvabilityZoneFloorKey(zoneId = "abyssal_temple", floorIndex = 2),
            SolvabilityHarnessRunner.SolvabilityZoneFloorKey(zoneId = "underground_river", floorIndex = 1),
            SolvabilityHarnessRunner.SolvabilityZoneFloorKey(zoneId = "underground_river", floorIndex = 2),
        )

    fun fixtureZoneFloors(): Set<SolvabilityHarnessRunner.SolvabilityZoneFloorKey> = fixtureZoneFloors

    fun buildSpec(cases: List<SolvabilityCase>, expectedCaseCount: Int): WhiteBoxSolvabilityLaneSpec =
        WhiteBoxSolvabilityLaneSpec(
            laneId = LANE_ID,
            description = "Deterministic fail fixtures for fail-capable zone/floor pairs with discovery tags intentionally withheld.",
            cases = cases,
            zoneFloorAggregateRules = revealFailZoneFloorRules(expectedCaseCount),
            corpusAggregateRules = revealFailCorpusRules,
        )
}
