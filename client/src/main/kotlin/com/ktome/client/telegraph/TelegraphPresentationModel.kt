package com.ktome.client.telegraph

import com.ktome.client.ui.status.StatusPresentationBuilder
import com.ktome.client.ui.status.StatusPresentationModel
import com.ktome.client.ui.status.TelegraphStatusPresentationRequest
import com.ktome.core.snapshot.OverlayRenderSnapshot

internal data class TelegraphPresentationModel(
    val typeId: String,
    val nameKey: String?,
    val iconKey: String?,
    val dangerLevel: Int,
    val previewTurnsRemaining: Int?,
    val badgeText: String,
    val shapeId: String,
    val affectedCellCount: Int,
) {
    fun toStatusPresentation(): StatusPresentationModel =
        StatusPresentationBuilder.buildTelegraph(
            TelegraphStatusPresentationRequest(
                typeId = typeId,
                nameKey = nameKey,
                iconKey = iconKey,
                dangerLevel = dangerLevel,
                previewTurnsRemaining = previewTurnsRemaining,
            ),
        )

    companion object {
        fun fromOverlay(overlay: OverlayRenderSnapshot): TelegraphPresentationModel =
            TelegraphPresentationModel(
                typeId = overlay.sourceAbilityId,
                nameKey = overlay.warningMessage?.key,
                iconKey = overlay.visualKey,
                dangerLevel = overlay.dangerLevel,
                previewTurnsRemaining = overlay.previewTurns,
                badgeText = StatusPresentationBuilder.telegraphBadge(overlay.previewTurns),
                shapeId = overlay.shape.name,
                affectedCellCount = overlay.cells.size,
            )

        fun sorted(overlays: List<OverlayRenderSnapshot>): List<Pair<OverlayRenderSnapshot, TelegraphPresentationModel>> =
            overlays
                .map { overlay -> overlay to fromOverlay(overlay) }
                .sortedWith(
                    compareByDescending<Pair<OverlayRenderSnapshot, TelegraphPresentationModel>> { (_, model) ->
                        model.toStatusPresentation().priority
                    }.thenBy { (_, model) -> model.typeId },
                )
    }
}
