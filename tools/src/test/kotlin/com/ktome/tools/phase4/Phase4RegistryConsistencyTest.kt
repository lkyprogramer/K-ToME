package com.ktome.tools.phase4

import com.ktome.tools.verification.VerificationTaskRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Phase4RegistryConsistencyTest {
    @Test
    fun `phase4 owner baseline registry stays aligned with routed owner tasks`() {
        val routedOwnerTaskIds = VerificationTaskRegistry.phaseOwnerTaskIds("phase4")

        assertTrue(
            Phase4OwnerBaselineRegistry.registeredTaskIds().all(routedOwnerTaskIds::contains),
            "Every Phase 4 owner baseline task must be backed by a routed unified verification owner task.",
        )
    }

    @Test
    fun `phase4 metric catalog owners stay aligned with routed owner tasks`() {
        val routedOwnerTaskIds = VerificationTaskRegistry.phaseOwnerTaskIds("phase4")

        assertTrue(
            Phase4MetricCatalog.ownerTaskIds().all(routedOwnerTaskIds::contains),
            "Every Phase 4 owner metric task must be backed by a routed unified verification owner task.",
        )
    }

    @Test
    fun `phase4 aggregation inventory equals routed owners plus explicit aggregation only tasks`() {
        val routedOwnerTaskIds = VerificationTaskRegistry.phaseOwnerTaskIds("phase4")
        val manifest = Phase4AggregationManifestRuntime.manifest()
        val aggregationOnlyTaskIds = manifest.aggregationOnlyTasks.mapTo(linkedSetOf(), Phase4AggregationManifestTask::taskId)
        val manifestOwnerTaskIds = manifest.ownerTasks.mapTo(linkedSetOf(), Phase4AggregationManifestTask::taskId)
        val aggregatedTaskIds = manifest.tasks.mapTo(linkedSetOf(), Phase4AggregationManifestTask::taskId)

        assertEquals(
            Phase4OwnerTaskRoles.AGGREGATION_ONLY_TASK_IDS,
            aggregationOnlyTaskIds,
        )
        assertEquals(
            routedOwnerTaskIds - Phase4OwnerTaskRoles.NON_AGGREGATED_OWNER_TASK_IDS,
            manifestOwnerTaskIds,
        )
        assertEquals(aggregatedTaskIds, Phase4DomainArtifactRegistry.registeredTaskIds())
        assertEquals(aggregatedTaskIds, Phase4DomainArtifactRegistry.readerTaskIds())
        assertTrue(aggregationOnlyTaskIds.none(routedOwnerTaskIds::contains))
    }
}
