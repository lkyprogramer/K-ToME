package com.ktome.client.render

import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.ui.layout.ModalFrame
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.core.map.Point

internal data class TileViewportFocusProjectionRequest(
    val playerTile: Point,
    val overlayState: OverlayState,
    val validationInspectProjection: ValidationInspectProjection? = null,
)

internal data class ValidationInspectProjection(
    val cursor: Point,
    val reason: ValidationProjectionReason,
)

internal enum class TileViewportFocusSourceKind {
    PLAYER,
    OVERLAY_INSPECT,
    OVERLAY_TARGETING,
    MODAL_INSPECT,
    MODAL_TARGETING,
    MODAL_COMBAT_DECISION,
    VALIDATION_INSPECT,
}

internal enum class ValidationProjectionReason {
    VALIDATION_FIXTURE,
    MANUAL_VALIDATION_PROBE,
    DEBUG_WHITEBOX_PROJECTION,
}

internal data class TileViewportFocusProjectionResult(
    val resolvedMode: TileViewportFocusMode,
    val resolvedFocusTile: Point,
    val sourceKind: TileViewportFocusSourceKind,
    val anchorTile: Point?,
    val tooltipAnchorKind: TileOverlayAnchorKind?,
    val isTooltipAnchorValid: Boolean,
    val validationReason: ValidationProjectionReason? = null,
)

internal data class TileViewportFocusCandidate(
    val mode: TileViewportFocusMode,
    val sourceKind: TileViewportFocusSourceKind,
    val cursor: Point?,
    val anchorKind: TileOverlayAnchorKind?,
    val validationReason: ValidationProjectionReason? = null,
)

internal object TileViewportFocusProjection {
    fun resolve(request: TileViewportFocusProjectionRequest): TileViewportFocusProjectionResult {
        val modalCandidates = modalCandidates(request.overlayState.modalFrames)
        val overlayTargetingCandidate =
            if (request.overlayState.mode == UiMode.TARGETING) {
                TileViewportFocusCandidate(
                    mode = TileViewportFocusMode.TARGETING,
                    sourceKind = TileViewportFocusSourceKind.OVERLAY_TARGETING,
                    cursor = request.overlayState.targetingCursor,
                    anchorKind = TileOverlayAnchorKind.WORLD_TILE,
                )
            } else {
                null
            }
        val overlayInspectCandidate =
            if (request.overlayState.mode == UiMode.INSPECT) {
                TileViewportFocusCandidate(
                    mode = TileViewportFocusMode.INSPECT,
                    sourceKind = TileViewportFocusSourceKind.OVERLAY_INSPECT,
                    cursor = request.overlayState.inspectCursor,
                    anchorKind = TileOverlayAnchorKind.WORLD_TILE,
                )
            } else {
                null
            }
        val validationCandidate =
            request.validationInspectProjection?.let { projection ->
                TileViewportFocusCandidate(
                    mode = TileViewportFocusMode.INSPECT,
                    sourceKind = TileViewportFocusSourceKind.VALIDATION_INSPECT,
                    cursor = projection.cursor,
                    anchorKind = TileOverlayAnchorKind.WORLD_TILE,
                    validationReason = projection.reason,
                )
            }
        val selected =
            when {
                modalCandidates.isNotEmpty() -> selectByModePriority(modalCandidates)
                overlayTargetingCandidate != null -> overlayTargetingCandidate
                overlayInspectCandidate != null -> overlayInspectCandidate
                validationCandidate != null -> validationCandidate
                else ->
                    TileViewportFocusCandidate(
                        mode = TileViewportFocusMode.PLAYER,
                        sourceKind = TileViewportFocusSourceKind.PLAYER,
                        cursor = request.playerTile,
                        anchorKind = null,
                    )
            }

        checkModalOverlayConsistency(
            selected = selected,
            modalCandidates = modalCandidates,
            overlayTargetingCandidate = overlayTargetingCandidate,
            overlayInspectCandidate = overlayInspectCandidate,
        )

        if (selected.sourceKind == TileViewportFocusSourceKind.PLAYER) {
            return TileViewportFocusProjectionResult(
                resolvedMode = TileViewportFocusMode.PLAYER,
                resolvedFocusTile = request.playerTile,
                sourceKind = TileViewportFocusSourceKind.PLAYER,
                anchorTile = null,
                tooltipAnchorKind = null,
                isTooltipAnchorValid = false,
            )
        }
        val cursor = selected.cursor
        return TileViewportFocusProjectionResult(
            resolvedMode = selected.mode,
            resolvedFocusTile = cursor ?: request.playerTile,
            sourceKind = selected.sourceKind,
            anchorTile = cursor,
            tooltipAnchorKind = selected.anchorKind.takeIf { cursor != null },
            isTooltipAnchorValid = cursor != null && selected.anchorKind != null,
            validationReason = selected.validationReason,
        )
    }

    private fun modalCandidates(frames: List<ModalFrame>): List<TileViewportFocusCandidate> =
        frames.asReversed().mapNotNull { frame ->
            when (frame.kind) {
                ModalFrameKind.TARGETING ->
                    TileViewportFocusCandidate(
                        mode = TileViewportFocusMode.TARGETING,
                        sourceKind = TileViewportFocusSourceKind.MODAL_TARGETING,
                        cursor = frame.localState.targetingCursor,
                        anchorKind = TileOverlayAnchorKind.WORLD_TILE,
                    )
                ModalFrameKind.COMBAT_DECISION ->
                    TileViewportFocusCandidate(
                        mode = TileViewportFocusMode.TARGETING,
                        sourceKind = TileViewportFocusSourceKind.MODAL_COMBAT_DECISION,
                        cursor = frame.localState.targetingCursor,
                        anchorKind = TileOverlayAnchorKind.WORLD_TILE,
                    )
                ModalFrameKind.INSPECT ->
                    TileViewportFocusCandidate(
                        mode = TileViewportFocusMode.INSPECT,
                        sourceKind = TileViewportFocusSourceKind.MODAL_INSPECT,
                        cursor = frame.localState.inspectCursor,
                        anchorKind = TileOverlayAnchorKind.WORLD_TILE,
                    )
                else -> null
            }
        }

    private fun selectByModePriority(candidates: List<TileViewportFocusCandidate>): TileViewportFocusCandidate =
        candidates.firstOrNull { candidate -> candidate.mode == TileViewportFocusMode.TARGETING }
            ?: candidates.first()

    private fun checkModalOverlayConsistency(
        selected: TileViewportFocusCandidate,
        modalCandidates: List<TileViewportFocusCandidate>,
        overlayTargetingCandidate: TileViewportFocusCandidate?,
        overlayInspectCandidate: TileViewportFocusCandidate?,
    ) {
        if (modalCandidates.isEmpty()) {
            return
        }
        when (selected.sourceKind) {
            TileViewportFocusSourceKind.MODAL_TARGETING,
            TileViewportFocusSourceKind.MODAL_COMBAT_DECISION,
            -> {
                overlayTargetingCandidate?.let { overlay ->
                    require(overlay.cursor == null || overlay.cursor == selected.cursor) {
                        "${selected.sourceKind} diverges from ${overlay.sourceKind}: modal=${selected.cursor}, overlay=${overlay.cursor}."
                    }
                }
                overlayInspectCandidate?.let { overlay ->
                    error("${selected.sourceKind} suppresses ${overlay.sourceKind}: overlay=${overlay.cursor}.")
                }
            }

            TileViewportFocusSourceKind.MODAL_INSPECT -> {
                overlayInspectCandidate?.let { overlay ->
                    require(overlay.cursor == null || overlay.cursor == selected.cursor) {
                        "${selected.sourceKind} diverges from ${overlay.sourceKind}: modal=${selected.cursor}, overlay=${overlay.cursor}."
                    }
                }
                overlayTargetingCandidate?.let { overlay ->
                    error("${selected.sourceKind} suppresses ${overlay.sourceKind}: overlay=${overlay.cursor}.")
                }
            }

            else -> error("Modal spatial candidate should have selected before ${selected.sourceKind}.")
        }
    }
}
