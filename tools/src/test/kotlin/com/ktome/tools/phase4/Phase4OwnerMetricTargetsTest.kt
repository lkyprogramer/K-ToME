package com.ktome.tools.phase4

import com.ktome.tools.verification.VerificationExpectedMetricRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Phase4OwnerMetricTargetsTest {
    @Test
    fun `build identity floor targets render as foundation profession coverage`() {
        assertEquals(
            "4/4 foundation professions",
            Phase4OwnerMetricTargets.targetText(
                metricId = "professionCapstoneAdoptionFloor",
                range = VerificationExpectedMetricRange(metricId = "professionCapstoneAdoptionFloor", minValue = 4.0),
            ),
        )
        assertEquals(
            "4/4 foundation professions",
            Phase4OwnerMetricTargets.targetText(
                metricId = "nonWeaponBuildPayoffFloor",
                range = VerificationExpectedMetricRange(metricId = "nonWeaponBuildPayoffFloor", minValue = 4.0),
            ),
        )
    }

    @Test
    fun `milestone reward adoption delta target keeps adopted greater than not adopted semantics`() {
        assertEquals(
            "adopted > notAdopted (delta >= 1)",
            Phase4OwnerMetricTargets.targetText(
                metricId = "milestoneRewardAdoptionDelta",
                range = VerificationExpectedMetricRange(metricId = "milestoneRewardAdoptionDelta", minValue = 1.0),
            ),
        )
    }
}
