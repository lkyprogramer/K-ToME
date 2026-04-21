package com.ktome.tools.mapgen

internal object WhiteBoxSolvabilitySuccessLane {
    const val LANE_ID: String = "reveal-success"

    fun buildSpec(cases: List<SolvabilityCase>, expectedCaseCount: Int): WhiteBoxSolvabilityLaneSpec =
        WhiteBoxSolvabilityLaneSpec(
            laneId = LANE_ID,
            description = "Deterministic success corpus for reveal/backtrack/hidden-anchor proofs.",
            cases = cases,
            zoneFloorAggregateRules = revealSuccessZoneFloorRules(expectedCaseCount),
            corpusAggregateRules = revealSuccessCorpusRules,
        )
}
