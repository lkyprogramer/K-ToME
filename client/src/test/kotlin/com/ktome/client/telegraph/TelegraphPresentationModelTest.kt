package com.ktome.client.telegraph

import com.ktome.client.ui.status.StatusPresentationGroup
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TelegraphPresentationModelTest {
    @Test
    fun `overlay projection keeps telegraph as client presentation group`() {
        val overlay =
            OverlayRenderSnapshot(
                id = "boss-warning",
                visualKey = "telegraph.boss_slam",
                previewTurns = 1,
                dangerLevel = 4,
                shape = OverlayShapeSnapshot.SINGLE_TILE,
                sourceAbilityId = "boss_slam",
                cells = listOf(GridPointSnapshot(1, 2)),
            )

        val model = TelegraphPresentationModel.fromOverlay(overlay)
        val status = model.toStatusPresentation()

        assertEquals("1t", model.badgeText)
        assertEquals(StatusPresentationGroup.TELEGRAPH, status.group)
        assertEquals("boss_slam", status.typeId)
        assertTrue(status.priority >= 984)
    }

    @Test
    fun `missing preview turns does not get inverse-turn bonus`() {
        val missingPreview =
            TelegraphPresentationModel(
                typeId = "missing",
                nameKey = null,
                iconKey = null,
                dangerLevel = 3,
                previewTurnsRemaining = null,
                badgeText = com.ktome.client.ui.status.StatusPresentationBuilder.telegraphBadge(null),
            ).toStatusPresentation()
        val fiveTurns =
            TelegraphPresentationModel(
                typeId = "five",
                nameKey = null,
                iconKey = null,
                dangerLevel = 3,
                previewTurnsRemaining = 5,
                badgeText = com.ktome.client.ui.status.StatusPresentationBuilder.telegraphBadge(5),
            ).toStatusPresentation()

        assertEquals(fiveTurns.priority, missingPreview.priority)
    }
}
