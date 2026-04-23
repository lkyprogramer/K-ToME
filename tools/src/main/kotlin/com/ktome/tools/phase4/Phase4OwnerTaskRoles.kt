package com.ktome.tools.phase4

internal object Phase4OwnerTaskRoles {
    val NON_AGGREGATED_OWNER_TASK_IDS: Set<String> = linkedSetOf("contractLint", "keywordRegistryLint")

    val AGGREGATION_ONLY_TASK_IDS: Set<String> =
        linkedSetOf(
            "mapgenSmoke",
            "solvabilityHarness",
            "whiteBoxHiddenContent",
        )
}
