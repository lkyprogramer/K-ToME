package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.render.layout.GameShellBounds
import com.ktome.client.render.layout.ModalSafeBounds
import com.ktome.client.render.layout.RectInt
import com.ktome.client.ui.layout.ModalFrame
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TileOverlayLayerTest {
    @Test
    fun sharesItemPresenterBetweenTooltipAndModal() {
        val model = overlayModel(
            overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = Point(1, 1)),
            projection = projection(TileViewportFocusMode.INSPECT, Point(1, 1)),
        )

        assertEquals("Short Sword", model.selectedTooltip?.titleLine?.text)
    }

    @Test
    fun suppressesPassiveTooltipWhenModalActive() {
        val model =
            overlayModel(
                overlayState =
                    OverlayState(
                        mode = UiMode.INSPECT,
                        inspectCursor = Point(1, 1),
                        modalFrames = listOf(ModalFrame(ModalFrameKind.ITEM_DETAIL)),
                    ),
                projection = projection(TileViewportFocusMode.INSPECT, Point(1, 1)),
            )

        assertNull(model.selectedTooltip)
        assertTrue(model.suppressedTooltipSources.any { it.reason == TileTooltipSuppressionReason.ACTIVE_MODAL_SUPPRESSED_PASSIVE })
    }

    @Test
    fun usesTopModalBackdropOnly() {
        val model =
            overlayModel(
                overlayState =
                    OverlayState(
                        mode = UiMode.INVENTORY,
                        modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY), ModalFrame(ModalFrameKind.ITEM_DETAIL)),
                    ),
                projection = projection(TileViewportFocusMode.PLAYER, Point.ZERO, valid = false),
            )

        assertNotNull(model.modalBackdrop)
        assertEquals(ModalFrameKind.ITEM_DETAIL, model.activeModal?.frameKind)
    }

    @Test
    fun hidesTooltipWhenAnchorLeavesVisibleRange() {
        val model =
            overlayModel(
                overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = Point(50, 50)),
                projection = projection(TileViewportFocusMode.INSPECT, Point(50, 50)),
            )

        assertNull(model.selectedTooltip)
        assertTrue(model.suppressedTooltipSources.any { it.reason == TileTooltipSuppressionReason.ANCHOR_OUTSIDE_VISIBLE_RANGE })
    }

    @Test
    fun rendersAtMostOneTooltipPerFrame() {
        val explicit = explicitTooltip()
        val model =
            overlayModel(
                overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = Point(1, 1), modalFrames = listOf(ModalFrame(ModalFrameKind.ITEM_DETAIL))),
                projection = projection(TileViewportFocusMode.INSPECT, Point(1, 1)),
                explicitModalTooltip = explicit,
            )

        assertEquals(explicit, model.selectedTooltip)
        assertEquals(TileTooltipSource.MODAL_EXPLICIT, model.selectedTooltipSource)
    }

    @Test
    fun activeModalSuppressesPassiveAnchoredTooltip() {
        suppressesPassiveTooltipWhenModalActive()
    }

    @Test
    fun selectsModalExplicitTooltipBeforePassiveTooltip() {
        rendersAtMostOneTooltipPerFrame()
    }

    @Test
    fun recordsSuppressedPassiveTooltipWhenModalActive() {
        suppressesPassiveTooltipWhenModalActive()
    }

    @Test
    fun rendererUsesSelectedTooltipFromModelOnly() {
        val canvas = RecordingOverlayCanvas()
        val frame = overlayFrame(overlayModel = TileOverlayModel(explicitTooltip(), TileTooltipSource.MODAL_EXPLICIT, null, null, null, emptyList(), emptyList()))

        TileRenderer.renderOverlayFrame(canvas, frame)

        assertEquals(2, canvas.tooltipRects.size)
        assertTrue(canvas.flushes.contains(TileLayerFlushReason.OVERLAY_MODAL_EXPLICIT_TOOLTIP))
    }

    @Test
    fun rendererDoesNotBranchOnModalFrameKind() {
        val inventory = overlayModel(overlayState = OverlayState(mode = UiMode.INVENTORY, modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY))))
        val detail = overlayModel(overlayState = OverlayState(mode = UiMode.INVENTORY, modalFrames = listOf(ModalFrame(ModalFrameKind.ITEM_DETAIL))))

        assertEquals(inventory.modalBackdrop?.alpha, detail.modalBackdrop?.alpha)
    }

    @Test
    fun cornerAnchorsFlipInsideShellBounds() {
        val canvas = RecordingOverlayCanvas()
        val anchor = ResolvedTileOverlayAnchor(TileOverlayAnchor.WorldTile(Point(9, 9)), RectInt(924, 724, 32, 32), TileOverlayCoordinateAuthority.TILE_MAP_VIEWPORT)
        val frame = overlayFrame(TileOverlayModel(tooltip(anchor, "Corner"), TileTooltipSource.INSPECT_CURSOR, null, null, null, emptyList(), emptyList()))

        TileRenderer.renderOverlayFrame(canvas, frame)

        assertTrue(canvas.tooltipRects.first().right <= frame.shellContentBounds.right)
        assertTrue(canvas.tooltipRects.first().top <= frame.shellContentBounds.top)
    }

    @Test
    fun productionTooltipPlacementUsesRightDownLeftUpCandidateOrder() {
        assertEquals(RectInt(136, 500, 360, 52), tooltipBackgroundRect(RectInt(100, 500, 32, 32)))
        assertEquals(RectInt(900, 444, 360, 52), tooltipBackgroundRect(RectInt(900, 500, 32, 32)))
        assertEquals(RectInt(536, 230, 360, 52), tooltipBackgroundRect(RectInt(900, 230, 32, 32)))
        assertEquals(RectInt(900, 256, 360, 52), tooltipBackgroundRect(RectInt(900, 220, 32, 32)))
    }

    @Test
    fun placesTooltipRectDuringOverlayModelBuild() {
        val model =
            overlayModel(
                overlayState = OverlayState(mode = UiMode.INSPECT, inspectCursor = Point(1, 1)),
                projection = projection(TileViewportFocusMode.INSPECT, Point(1, 1)),
            )

        assertEquals(RectInt(68, 480, 360, 62), model.selectedTooltip?.placedRect)
    }

    @Test
    fun cornerAnchorsUseTileMapViewportResolvedWorldTileRect() {
        val resolver = FrameTileOverlayAnchorResolver(viewport())
        val resolved = resolver.resolve(TileOverlayAnchor.WorldTile(Point(1, 1))) as TileOverlayAnchorResolution.Resolved

        assertEquals(TileOverlayCoordinateAuthority.TILE_MAP_VIEWPORT, resolved.anchor.coordinateAuthority)
        assertEquals(viewport().tileRect(Point(1, 1)), resolved.anchor.bounds)
    }

    @Test
    fun recordsAnchorOutsideVisibleRangeSuppressionReason() {
        val resolution = FrameTileOverlayAnchorResolver(viewport()).resolve(TileOverlayAnchor.WorldTile(Point(50, 50)))

        assertEquals(TileTooltipSuppressionReason.ANCHOR_OUTSIDE_VISIBLE_RANGE, (resolution as TileOverlayAnchorResolution.Failed).reason)
    }

    @Test
    fun recordsAnchorResolutionFailedWhenPresenterRectMissing() {
        val resolution = FrameTileOverlayAnchorResolver(viewport()).resolve(TileOverlayAnchor.PanelSlot(RectInt(0, 0, 0, 0), "slot"))

        assertEquals(TileTooltipSuppressionReason.ANCHOR_RESOLUTION_FAILED, (resolution as TileOverlayAnchorResolution.Failed).reason)
    }

    @Test
    fun usesLayoutModalSafeBoundsWithoutWindowRecalculation() {
        val model = overlayModel(overlayState = OverlayState(mode = UiMode.INVENTORY, modalFrames = listOf(ModalFrame(ModalFrameKind.INVENTORY))))

        assertNotEquals(1280 / 2 - model.activeModal!!.bounds.width / 2, model.activeModal.bounds.x)
        assertEquals(ModalSafeBounds(12, 1000, 780, 236).left + (ModalSafeBounds(12, 1000, 780, 236).width - model.activeModal.bounds.width) / 2, model.activeModal.bounds.x)
    }

    private fun overlayModel(
        overlayState: OverlayState = OverlayState(mode = UiMode.MAP),
        projection: TileViewportFocusProjectionResult = projection(TileViewportFocusMode.PLAYER, Point.ZERO, valid = false),
        explicitModalTooltip: TileTooltipModel? = null,
    ): TileOverlayModel =
        TileOverlayModelBuilder.build(
            TileOverlayModelRequest(
                renderModel = OverlayTestModels.renderModel(),
                overlayState = overlayState,
                projection = projection,
                anchorResolver = FrameTileOverlayAnchorResolver(viewport()),
                shellContentBounds = GameShellBounds(0f, 224f, 1280f, 576f),
                modalSafeBounds = ModalSafeBounds(12, 1000, 780, 236),
                bottomLogReservedBounds = bottomLogReservedBounds(),
                explicitModalTooltip = explicitModalTooltip,
            ),
        )

    private fun projection(
        mode: TileViewportFocusMode,
        focus: Point,
        valid: Boolean = true,
    ): TileViewportFocusProjectionResult =
        TileViewportFocusProjectionResult(
            resolvedMode = mode,
            resolvedFocusTile = focus,
            sourceKind = if (mode == TileViewportFocusMode.TARGETING) TileViewportFocusSourceKind.OVERLAY_TARGETING else TileViewportFocusSourceKind.OVERLAY_INSPECT,
            anchorTile = focus.takeIf { valid },
            tooltipAnchorKind = TileOverlayAnchorKind.WORLD_TILE.takeIf { valid },
            isTooltipAnchorValid = valid,
        )

    private fun viewport(): TileMapViewport =
        TileMapViewport.resolve(
            identity =
                TileMapViewportIdentity(
                    zoneId = "test",
                    currentFloor = 1,
                    mapDimensions = TileMapDimensions(20, 20),
                    cellSize = 32,
                    cellAlignedMapBounds = RectInt(0, 224, 320, 320),
                    focusMode = TileViewportFocusMode.PLAYER,
                ),
            playerTile = Point(4, 4),
            focusTile = Point(4, 4),
            previousState = null,
        )

    private fun explicitTooltip(): TileTooltipModel =
        tooltip(
            anchor =
                ResolvedTileOverlayAnchor(
                    source = TileOverlayAnchor.ModalRow(RectInt(400, 400, 120, 24), ModalFrameKind.ITEM_DETAIL, "row"),
                    bounds = RectInt(400, 400, 120, 24),
                    coordinateAuthority = TileOverlayCoordinateAuthority.PRESENTER_LAYOUT,
                ),
            title = "Explicit",
        )

    private fun tooltipBackgroundRect(anchorBounds: RectInt): RectInt {
        val anchor = ResolvedTileOverlayAnchor(TileOverlayAnchor.WorldTile(Point.ZERO), anchorBounds, TileOverlayCoordinateAuthority.TILE_MAP_VIEWPORT)
        val tooltip = tooltip(anchor)
        val frame =
            overlayFrame(
                TileOverlayModel(
                    selectedTooltip = tooltip,
                    selectedTooltipSource = TileTooltipSource.INSPECT_CURSOR,
                    modalBackdrop = null,
                    activeModal = null,
                    toast = null,
                    suppressedTooltipSources = emptyList(),
                    debugHints = emptyList(),
                ),
            )
        val canvas = RecordingOverlayCanvas()

        TileRenderer.renderOverlayFrame(canvas, frame)

        assertEquals(tooltip.placedRect, canvas.tooltipRects.first())
        return canvas.tooltipRects.first()
    }

    private fun overlayFrame(overlayModel: TileOverlayModel): OverlayRenderFrame =
        OverlayRenderFrame(
            overlayModel = overlayModel,
            shellContentBounds = shellContentBounds(),
            modalSafeBounds = ModalSafeBounds(12, 1000, 780, 236),
            bottomLogReservedBounds = bottomLogReservedBounds(),
            textMetrics = TileTextMetrics,
        )

    private fun tooltip(
        anchor: ResolvedTileOverlayAnchor,
        title: String = "Tooltip",
        bodyLines: List<TileTextLine> = emptyList(),
    ): TileTooltipModel =
        TileTooltipModel(
            anchor = anchor,
            titleLine = TileTextLine(title, TileTextTone.GOLD),
            bodyLines = bodyLines,
            placedRect =
                TileTooltipPlacementSolver.resolve(
                    anchor = anchor,
                    bodyLineCount = bodyLines.size,
                    shellContentBounds = shellContentBounds(),
                    bottomLogReservedBounds = bottomLogReservedBounds(),
                ),
        )

    private fun shellContentBounds(): GameShellBounds = GameShellBounds(0f, 224f, 1280f, 576f)

    private fun bottomLogReservedBounds(): GameShellBounds = GameShellBounds(0f, 18f, 1280f, 92f)
}

private class RecordingOverlayCanvas : TileCanvas {
    val tooltipRects = mutableListOf<RectInt>()
    val flushes = mutableListOf<TileLayerFlushReason>()

    override fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
    ) {
        tooltipRects += RectInt(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    }

    override fun drawAsset(
        asset: ResolvedVisualAsset,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        alpha: Float,
        tintColorHex: String?,
    ) = Unit

    override fun drawText(
        style: TileTextStyle,
        text: String,
        x: Float,
        y: Float,
        color: Color,
    ) = Unit

    override fun flushLayer(reason: TileLayerFlushReason) {
        flushes += reason
    }
}

private object OverlayTestModels {
    fun renderModel(): TileRenderModel =
        TileRenderModel(
            terrainTiles = emptyList(),
            propTiles = emptyList(),
            overlayTiles = emptyList(),
            groundLootMarkers = emptyList(),
            actorTiles = emptyList(),
            fogTiles = emptyList(),
            targetCursorState = null,
            hud =
                TileHudModel(
                    playerName = "Hero",
                    zoneName = "Zone",
                    floorText = "1",
                    hpGauge = gauge("HEALTH"),
                    resourceGauge = gauge("STAMINA"),
                    experienceGauge = gauge("EXPERIENCE"),
                    statusIcons = emptyList(),
                    focusIcon = null,
                    focusName = null,
                    focusLines = listOf("Focus"),
                    hotbar = emptyList(),
                    summaryText = "",
                ),
            messageLines = emptyList(),
            logPresentation = com.ktome.client.ui.panel.LogPresentationModel(emptyList(), "empty", "fallback"),
            playerCard = com.ktome.client.ui.panel.PlayerCardModel("Hero", "HP 1/1", "STA 1/1", null, 0, "empty"),
            targetCard = com.ktome.client.ui.panel.TargetCardModel("Short Sword", listOf("A reliable blade."), "empty"),
            actionPanel = com.ktome.client.ui.panel.ActionPanelModel(emptyList(), "empty"),
            combatFeedback = emptyList(),
            sidebar = TileSidebarModel("Item Detail", listOf(TileTextRow("Short Sword", TileTextTone.GOLD))),
            shell = TileShellModel(TilePanelModel("Left", emptyList()), TilePanelModel("Right", emptyList()), emptyList()),
            playerTile = Point.ZERO,
            mapDimensions = TileMapDimensions(20, 20),
        )

    private fun gauge(type: String): TileGaugeModel =
        TileGaugeModel(label = type, current = 1, max = 1, tone = TileTextTone.WHITE, resourceTypeId = type)
}
