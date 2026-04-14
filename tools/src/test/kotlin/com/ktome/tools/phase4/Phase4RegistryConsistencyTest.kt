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
        val aggregationOnlyTaskIds = Phase4DomainArtifactRegistry.aggregationOnlyTaskIds()
        val aggregatedTaskIds = Phase4DomainArtifactRegistry.registeredTaskIds()
        val reportOwnerTaskIds = aggregatedTaskIds - aggregationOnlyTaskIds

        assertEquals(
            setOf("mapgenSmoke", "solvabilityHarness", "whiteBoxHiddenContent", "whiteBoxContentPack"),
            aggregationOnlyTaskIds,
        )
        assertEquals(setOf("contractLint"), routedOwnerTaskIds - reportOwnerTaskIds)
        assertEquals(reportOwnerTaskIds + aggregationOnlyTaskIds, aggregatedTaskIds)
        assertTrue(reportOwnerTaskIds.all(routedOwnerTaskIds::contains))
        assertTrue(aggregationOnlyTaskIds.none(routedOwnerTaskIds::contains))
    }
}
