package com.ktome.client.render

import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.ui.layout.ModalFrame
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.client.ui.layout.ModalFrameLocalState
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TileViewportFocusProjectionTest {
    private val player = Point(4, 4)

    @Test
    fun playerModeUsesPlayerTile() {
        val result = resolve(OverlayState(mode = UiMode.MAP))

        assertEquals(TileViewportFocusMode.PLAYER, result.resolvedMode)
        assertEquals(player, result.resolvedFocusTile)
        assertEquals(TileViewportFocusSourceKind.PLAYER, result.sourceKind)
        assertFalse(result.isTooltipAnchorValid)
    }

    @Test
    fun inspectModeResolvesCursorFromOverlayOrModalState() {
        val cursor = Point(1, 2)
        val result = resolve(OverlayState(mode = UiMode.INSPECT, inspectCursor = cursor))

        assertEquals(TileViewportFocusMode.INSPECT, result.resolvedMode)
        assertEquals(cursor, result.resolvedFocusTile)
        assertEquals(TileViewportFocusSourceKind.OVERLAY_INSPECT, result.sourceKind)
        assertEquals(cursor, result.anchorTile)
    }

    @Test
    fun targetingModeResolvesCursorFromOverlayOrModalState() {
        val cursor = Point(6, 2)
        val result = resolve(OverlayState(mode = UiMode.TARGETING, targetingCursor = cursor))

        assertEquals(TileViewportFocusMode.TARGETING, result.resolvedMode)
        assertEquals(cursor, result.resolvedFocusTile)
        assertEquals(TileViewportFocusSourceKind.OVERLAY_TARGETING, result.sourceKind)
    }

    @Test
    fun modalTargetingCursorWinsOverStaleOverlayCursor() {
        val cursor = Point(7, 3)
        val result =
            resolve(
                OverlayState(
                    mode = UiMode.TARGETING,
                    targetingCursor = cursor,
                    modalFrames =
                        listOf(
                            ModalFrame(
                                ModalFrameKind.TARGETING,
                                ModalFrameLocalState(targetingCursor = cursor),
                            ),
                        ),
                ),
            )

        assertEquals(TileViewportFocusSourceKind.MODAL_TARGETING, result.sourceKind)
        assertEquals(cursor, result.anchorTile)
    }

    @Test
    fun modalInspectCursorWinsOverStaleOverlayCursor() {
        val cursor = Point(2, 3)
        val result =
            resolve(
                OverlayState(
                    mode = UiMode.INSPECT,
                    inspectCursor = cursor,
                    modalFrames =
                        listOf(
                            ModalFrame(
                                ModalFrameKind.INSPECT,
                                ModalFrameLocalState(inspectCursor = cursor),
                            ),
                        ),
                ),
            )

        assertEquals(TileViewportFocusSourceKind.MODAL_INSPECT, result.sourceKind)
        assertEquals(cursor, result.anchorTile)
    }

    @Test
    fun modalSpatialCandidateSuppressesCrossModeOverlayCandidate() {
        assertThrows(IllegalStateException::class.java) {
            resolve(
                OverlayState(
                    mode = UiMode.TARGETING,
                    targetingCursor = Point(9, 9),
                    modalFrames =
                        listOf(
                            ModalFrame(
                                ModalFrameKind.INSPECT,
                                ModalFrameLocalState(inspectCursor = Point(2, 2)),
                            ),
                        ),
                ),
            )
        }
    }

    @Test
    fun rejectsOverlayTargetingWhenModalInspectCandidateExists() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                resolve(
                    OverlayState(
                        mode = UiMode.TARGETING,
                        targetingCursor = Point(9, 9),
                        modalFrames = listOf(ModalFrame(ModalFrameKind.INSPECT, ModalFrameLocalState(inspectCursor = Point(2, 2)))),
                    ),
                )
            }

        assertTrue(exception.message!!.contains("MODAL_INSPECT"))
        assertTrue(exception.message!!.contains("OVERLAY_TARGETING"))
    }

    @Test
    fun rejectsOverlayInspectWhenModalTargetingCandidateExists() {
        val exception =
            assertThrows(IllegalStateException::class.java) {
                resolve(
                    OverlayState(
                        mode = UiMode.INSPECT,
                        inspectCursor = Point(1, 1),
                        modalFrames = listOf(ModalFrame(ModalFrameKind.TARGETING, ModalFrameLocalState(targetingCursor = Point(2, 2)))),
                    ),
                )
            }

        assertTrue(exception.message!!.contains("MODAL_TARGETING"))
        assertTrue(exception.message!!.contains("OVERLAY_INSPECT"))
    }

    @Test
    fun sourceKindAndAnchorComeFromSameCandidate() {
        val result =
            resolve(
                OverlayState(
                    mode = UiMode.TARGETING,
                    targetingCursor = Point(5, 5),
                    modalFrames = listOf(ModalFrame(ModalFrameKind.TARGETING, ModalFrameLocalState(targetingCursor = Point(5, 5)))),
                ),
            )

        assertEquals(TileViewportFocusSourceKind.MODAL_TARGETING, result.sourceKind)
        assertEquals(Point(5, 5), result.anchorTile)
    }

    @Test
    fun rejectsDivergentOverlayAndModalCursorForSameSurface() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                resolve(
                    OverlayState(
                        mode = UiMode.INSPECT,
                        inspectCursor = Point(1, 1),
                        modalFrames = listOf(ModalFrame(ModalFrameKind.INSPECT, ModalFrameLocalState(inspectCursor = Point(2, 2)))),
                    ),
                )
            }

        assertTrue(exception.message!!.contains("MODAL_INSPECT"))
        assertTrue(exception.message!!.contains("OVERLAY_INSPECT"))
    }

    @Test
    fun nonSpatialModalPreservesCurrentSpatialProjection() {
        val result =
            resolve(
                OverlayState(
                    mode = UiMode.MAP,
                    modalFrames =
                        listOf(
                            ModalFrame(ModalFrameKind.INSPECT, ModalFrameLocalState(inspectCursor = Point(3, 3))),
                            ModalFrame(ModalFrameKind.ITEM_DETAIL),
                        ),
                ),
            )

        assertEquals(TileViewportFocusSourceKind.MODAL_INSPECT, result.sourceKind)
        assertEquals(Point(3, 3), result.anchorTile)
    }

    @Test
    fun targetingProjectionWinsOverInspectProjection() {
        val result =
            resolve(
                OverlayState(
                    mode = UiMode.MAP,
                    modalFrames =
                        listOf(
                            ModalFrame(ModalFrameKind.INSPECT, ModalFrameLocalState(inspectCursor = Point(3, 3))),
                            ModalFrame(ModalFrameKind.TARGETING, ModalFrameLocalState(targetingCursor = Point(7, 7))),
                        ),
                ),
            )

        assertEquals(TileViewportFocusMode.TARGETING, result.resolvedMode)
        assertEquals(Point(7, 7), result.anchorTile)
    }

    @Test
    fun validationCursorRequiresExplicitInspectProjection() {
        val withoutExplicit = resolve(OverlayState(mode = UiMode.VALIDATION))
        val withExplicit =
            resolve(
                OverlayState(mode = UiMode.VALIDATION),
                ValidationInspectProjection(Point(8, 8), ValidationProjectionReason.VALIDATION_FIXTURE),
            )

        assertEquals(TileViewportFocusMode.PLAYER, withoutExplicit.resolvedMode)
        assertEquals(TileViewportFocusMode.INSPECT, withExplicit.resolvedMode)
        assertEquals(TileViewportFocusSourceKind.VALIDATION_INSPECT, withExplicit.sourceKind)
        assertEquals(ValidationProjectionReason.VALIDATION_FIXTURE, withExplicit.validationReason)
    }

    @Test
    fun inspectFallbackUsesPlayerForViewportButSuppressesTooltipAnchor() {
        val result = resolve(OverlayState(mode = UiMode.INSPECT, inspectCursor = null))

        assertEquals(TileViewportFocusMode.INSPECT, result.resolvedMode)
        assertEquals(player, result.resolvedFocusTile)
        assertEquals(null, result.anchorTile)
        assertFalse(result.isTooltipAnchorValid)
    }

    @Test
    fun targetingFallbackUsesPlayerForViewportButDoesNotConfirmTarget() {
        val result = resolve(OverlayState(mode = UiMode.TARGETING, targetingCursor = null))

        assertEquals(TileViewportFocusMode.TARGETING, result.resolvedMode)
        assertEquals(player, result.resolvedFocusTile)
        assertEquals(null, result.anchorTile)
        assertFalse(result.isTooltipAnchorValid)
    }

    @Test
    fun keepsSpatialModeCandidateWhenCursorIsNull() {
        val result = resolve(OverlayState(mode = UiMode.TARGETING, targetingCursor = null))

        assertEquals(TileViewportFocusSourceKind.OVERLAY_TARGETING, result.sourceKind)
        assertEquals(TileViewportFocusMode.TARGETING, result.resolvedMode)
    }

    @Test
    fun usesOverlayStateModalFramesAsSingleAuthority() {
        val result =
            resolve(
                OverlayState(
                    mode = UiMode.MAP,
                    modalFrames = listOf(ModalFrame(ModalFrameKind.COMBAT_DECISION, ModalFrameLocalState(targetingCursor = Point(6, 6)))),
                ),
            )

        assertEquals(TileViewportFocusSourceKind.MODAL_COMBAT_DECISION, result.sourceKind)
        assertEquals(Point(6, 6), result.anchorTile)
    }

    @Test
    fun scansModalFramesTopToBottomFromBottomFirstList() {
        val result =
            resolve(
                OverlayState(
                    mode = UiMode.MAP,
                    modalFrames =
                        listOf(
                            ModalFrame(ModalFrameKind.INSPECT, ModalFrameLocalState(inspectCursor = Point(1, 1))),
                            ModalFrame(ModalFrameKind.INSPECT, ModalFrameLocalState(inspectCursor = Point(2, 2))),
                        ),
                ),
            )

        assertEquals(Point(2, 2), result.anchorTile)
    }

    private fun resolve(
        overlayState: OverlayState,
        validation: ValidationInspectProjection? = null,
    ): TileViewportFocusProjectionResult =
        TileViewportFocusProjection.resolve(
            TileViewportFocusProjectionRequest(
                playerTile = player,
                overlayState = overlayState,
                validationInspectProjection = validation,
            ),
        )
}
