package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.ktome.client.input.OverlayState
import com.ktome.client.render.layout.GameShellBounds
import com.ktome.client.render.layout.ModalSafeBounds
import com.ktome.client.render.layout.RectInt
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.client.ui.talent.TalentAssignPanelLayoutSolver
import com.ktome.client.ui.talent.TalentAssignPanelModalBoundsRequest
import com.ktome.client.ui.token.UiDesignTokens
import com.ktome.core.map.Point
import kotlin.math.roundToInt

internal enum class TileOverlayAnchorKind {
    WORLD_TILE,
    PANEL_SLOT,
    PANEL_ROW_OR_CARD,
    QUEST_ROW,
    MODAL_ROW,
}

internal sealed interface TileOverlayAnchor {
    val kind: TileOverlayAnchorKind

    data class WorldTile(
        val tile: Point,
    ) : TileOverlayAnchor {
        override val kind: TileOverlayAnchorKind = TileOverlayAnchorKind.WORLD_TILE
    }

    data class PanelSlot(
        val bounds: RectInt,
        val slotId: String,
    ) : TileOverlayAnchor {
        override val kind: TileOverlayAnchorKind = TileOverlayAnchorKind.PANEL_SLOT
    }

    data class PanelRowOrCard(
        val bounds: RectInt,
        val sourceId: String,
    ) : TileOverlayAnchor {
        override val kind: TileOverlayAnchorKind = TileOverlayAnchorKind.PANEL_ROW_OR_CARD
    }

    data class QuestRow(
        val bounds: RectInt,
        val questId: String,
    ) : TileOverlayAnchor {
        override val kind: TileOverlayAnchorKind = TileOverlayAnchorKind.QUEST_ROW
    }

    data class ModalRow(
        val bounds: RectInt,
        val frameKind: ModalFrameKind,
        val rowId: String,
    ) : TileOverlayAnchor {
        override val kind: TileOverlayAnchorKind = TileOverlayAnchorKind.MODAL_ROW
    }
}

internal data class ResolvedTileOverlayAnchor(
    val source: TileOverlayAnchor,
    val bounds: RectInt,
    val coordinateAuthority: TileOverlayCoordinateAuthority,
)

internal sealed interface TileOverlayAnchorResolution {
    val source: TileOverlayAnchor

    data class Resolved(
        val anchor: ResolvedTileOverlayAnchor,
    ) : TileOverlayAnchorResolution {
        override val source: TileOverlayAnchor = anchor.source
    }

    data class Failed(
        override val source: TileOverlayAnchor,
        val reason: TileTooltipSuppressionReason,
    ) : TileOverlayAnchorResolution
}

internal enum class TileOverlayCoordinateAuthority {
    TILE_MAP_VIEWPORT,
    SHELL_LAYOUT,
    PRESENTER_LAYOUT,
}

internal interface TileOverlayAnchorResolver {
    fun resolve(anchor: TileOverlayAnchor): TileOverlayAnchorResolution
}

internal class FrameTileOverlayAnchorResolver(
    private val viewport: TileMapViewport,
) : TileOverlayAnchorResolver {
    override fun resolve(anchor: TileOverlayAnchor): TileOverlayAnchorResolution =
        when (anchor) {
            is TileOverlayAnchor.WorldTile ->
                if (viewport.containsTile(anchor.tile)) {
                    TileOverlayAnchorResolution.Resolved(
                        ResolvedTileOverlayAnchor(
                            source = anchor,
                            bounds = viewport.tileRect(anchor.tile),
                            coordinateAuthority = TileOverlayCoordinateAuthority.TILE_MAP_VIEWPORT,
                        ),
                    )
                } else {
                    TileOverlayAnchorResolution.Failed(anchor, TileTooltipSuppressionReason.ANCHOR_OUTSIDE_VISIBLE_RANGE)
                }

            is TileOverlayAnchor.PanelSlot ->
                resolvedPresenterAnchor(anchor, anchor.bounds, TileOverlayCoordinateAuthority.SHELL_LAYOUT)

            is TileOverlayAnchor.PanelRowOrCard ->
                resolvedPresenterAnchor(anchor, anchor.bounds, TileOverlayCoordinateAuthority.PRESENTER_LAYOUT)

            is TileOverlayAnchor.QuestRow ->
                resolvedPresenterAnchor(anchor, anchor.bounds, TileOverlayCoordinateAuthority.PRESENTER_LAYOUT)

            is TileOverlayAnchor.ModalRow ->
                resolvedPresenterAnchor(anchor, anchor.bounds, TileOverlayCoordinateAuthority.PRESENTER_LAYOUT)
        }

    private fun resolvedPresenterAnchor(
        anchor: TileOverlayAnchor,
        bounds: RectInt,
        authority: TileOverlayCoordinateAuthority,
    ): TileOverlayAnchorResolution =
        if (bounds.width > 0 && bounds.height > 0) {
            TileOverlayAnchorResolution.Resolved(
                ResolvedTileOverlayAnchor(
                    source = anchor,
                    bounds = bounds,
                    coordinateAuthority = authority,
                ),
            )
        } else {
            TileOverlayAnchorResolution.Failed(anchor, TileTooltipSuppressionReason.ANCHOR_RESOLUTION_FAILED)
        }
}

internal data class ColorSpec(
    val hex: String,
    val alpha: Float = 1f,
) {
    fun color(): Color = Color.valueOf(hex.removePrefix("#")).also { color -> color.a = alpha }
}

internal enum class TileTooltipSource {
    MODAL_EXPLICIT,
    INSPECT_CURSOR,
    TARGETING_CURSOR,
    FOCUSED_ENTITY,
}

internal enum class TileTooltipSuppressionReason {
    ACTIVE_MODAL_SUPPRESSED_PASSIVE,
    ANCHOR_OUTSIDE_VISIBLE_RANGE,
    ANCHOR_RESOLUTION_FAILED,
    LOWER_PRIORITY_TOOLTIP,
}

internal data class TileTooltipSuppression(
    val source: TileTooltipSource,
    val reason: TileTooltipSuppressionReason,
)

internal data class TileTooltipModel(
    val anchor: ResolvedTileOverlayAnchor,
    val titleLine: TileTextLine,
    val bodyLines: List<TileTextLine>,
    val placedRect: RectInt,
)

internal data class TileModalBackdropModel(
    val bounds: GameShellBounds,
    val color: ColorSpec,
    val alpha: Float,
)

internal data class TileModalModel(
    val frameKind: ModalFrameKind,
    val bounds: RectInt,
    val visualBounds: RectInt? = null,
    val titleLine: TileTextLine,
    val bodyLines: List<TileTextLine>,
    val footerHintLines: List<TileTextLine>,
    val talentAssignPanel: TileTalentAssignPanelRenderModel? = null,
)

internal data class TileToastModel(
    val line: TileTextLine,
)

internal data class TileDebugHintModel(
    val line: TileTextLine,
)

internal data class TileOverlayModel(
    val selectedTooltip: TileTooltipModel?,
    val selectedTooltipSource: TileTooltipSource?,
    val activeModal: TileModalModel?,
    val modalBackdrop: TileModalBackdropModel?,
    val toast: TileToastModel?,
    val debugHints: List<TileDebugHintModel>,
    val suppressedTooltipSources: List<TileTooltipSuppression>,
)

internal data class TileOverlayModelRequest(
    val renderModel: TileRenderModel,
    val overlayState: OverlayState,
    val projection: TileViewportFocusProjectionResult,
    val anchorResolver: TileOverlayAnchorResolver,
    val shellContentBounds: GameShellBounds,
    val viewportBounds: GameShellBounds = shellContentBounds,
    val modalSafeBounds: ModalSafeBounds,
    val bottomLogReservedBounds: GameShellBounds,
    val explicitModalTooltip: TileTooltipModel? = null,
)

internal object TileOverlayModelBuilder {
    fun build(request: TileOverlayModelRequest): TileOverlayModel {
        val topModal = request.overlayState.modalFrames.lastOrNull()
        val activeModal =
            topModal?.let { frame ->
                val bounds = modalBounds(frame.kind, request)
                TileModalModel(
                    frameKind = frame.kind,
                    bounds = bounds,
                    visualBounds =
                        if (frame.kind == ModalFrameKind.TALENT_ASSIGN || frame.kind == ModalFrameKind.ACTIVE_TALENT_SLOT_CHOICE) {
                            talentAssignVisualBounds(request)
                        } else {
                            null
                        },
                    titleLine = TileTextLine(modalTitle(frame.kind), TileTextTone.GOLD),
                    bodyLines =
                        request.renderModel.sidebar.rows
                            .take(8)
                            .map { row -> TileTextLine(row.text, row.tone) },
                    footerHintLines =
                        request.renderModel.shell.footerHints
                            .take(2)
                            .map { row -> TileTextLine(row.text, row.tone) },
                    talentAssignPanel =
                        request.renderModel.talentAssignPanel
                            ?.takeIf { frame.kind == ModalFrameKind.TALENT_ASSIGN || frame.kind == ModalFrameKind.ACTIVE_TALENT_SLOT_CHOICE },
                )
            }
        val passiveTooltip = passiveTooltip(request)
        val explicitTooltip = request.explicitModalTooltip?.let { tooltip -> tooltip.withPlacement(request) }
        val suppressed = mutableListOf<TileTooltipSuppression>()
        val selectedTooltip =
            when {
                explicitTooltip != null -> {
                    passiveTooltip?.let {
                        suppressed += TileTooltipSuppression(TileTooltipSource.FOCUSED_ENTITY, TileTooltipSuppressionReason.LOWER_PRIORITY_TOOLTIP)
                    }
                    explicitTooltip
                }
                activeModal != null && passiveTooltip != null -> {
                    suppressed +=
                        TileTooltipSuppression(
                            source = tooltipSourceForProjection(request.projection),
                            reason = TileTooltipSuppressionReason.ACTIVE_MODAL_SUPPRESSED_PASSIVE,
                        )
                    null
                }
                else -> passiveTooltip
            }
        return TileOverlayModel(
            selectedTooltip = selectedTooltip,
            selectedTooltipSource =
                when {
                    selectedTooltip == null -> null
                    explicitTooltip != null && selectedTooltip === explicitTooltip -> TileTooltipSource.MODAL_EXPLICIT
                    else -> tooltipSourceForProjection(request.projection)
                },
            activeModal = activeModal,
            modalBackdrop =
                activeModal?.let {
                    TileModalBackdropModel(
                        bounds = request.shellContentBounds,
                        color = ColorSpec(UiDesignTokens.color.surface.base.hexString()),
                        alpha = UiDesignTokens.fixed.modalBackdropAlpha,
                    )
                },
            toast =
                request.overlayState.debugMessageKey?.takeIf { activeModal == null }?.let { key ->
                    TileToastModel(TileTextLine(key, TileTextTone.CYAN))
                },
            debugHints = emptyList(),
            suppressedTooltipSources = suppressed + passiveTooltipSuppressions(request),
        )
    }

    private fun passiveTooltip(request: TileOverlayModelRequest): TileTooltipModel? {
        val anchorTile = request.projection.anchorTile ?: return null
        val anchor = TileOverlayAnchor.WorldTile(anchorTile)
        return when (val resolution = request.anchorResolver.resolve(anchor)) {
            is TileOverlayAnchorResolution.Failed -> null
            is TileOverlayAnchorResolution.Resolved ->
                TileTooltipModel(
                    anchor = resolution.anchor,
                    titleLine =
                        TileTextLine(
                            request.renderModel.targetCard.title
                                ?: request.renderModel.hud.focusName
                                ?: "Tile ${anchorTile.x},${anchorTile.y}",
                            TileTextTone.GOLD,
                        ),
                    bodyLines =
                        (request.renderModel.targetCard.lines.ifEmpty { request.renderModel.hud.focusLines })
                            .take(TILE_TOOLTIP_BODY_LINE_LIMIT)
                            .map { line -> TileTextLine(line, TileTextTone.LIGHT_GRAY) },
                    placedRect =
                        TileTooltipPlacementSolver.resolve(
                            anchor = resolution.anchor,
                            bodyLineCount = (request.renderModel.targetCard.lines.ifEmpty { request.renderModel.hud.focusLines }).size,
                            shellContentBounds = request.shellContentBounds,
                            bottomLogReservedBounds = request.bottomLogReservedBounds,
                        ),
                )
        }
    }

    private fun TileTooltipModel.withPlacement(request: TileOverlayModelRequest): TileTooltipModel =
        copy(
            placedRect =
                TileTooltipPlacementSolver.resolve(
                    anchor = anchor,
                    bodyLineCount = bodyLines.size,
                    shellContentBounds = request.shellContentBounds,
                    bottomLogReservedBounds = request.bottomLogReservedBounds,
                ),
        )

    private fun passiveTooltipSuppressions(request: TileOverlayModelRequest): List<TileTooltipSuppression> {
        val anchorTile = request.projection.anchorTile ?: return emptyList()
        val anchor = TileOverlayAnchor.WorldTile(anchorTile)
        return when (val resolution = request.anchorResolver.resolve(anchor)) {
            is TileOverlayAnchorResolution.Failed ->
                listOf(TileTooltipSuppression(tooltipSourceForProjection(request.projection), resolution.reason))
            is TileOverlayAnchorResolution.Resolved -> emptyList()
        }
    }

    private fun tooltipSourceForProjection(projection: TileViewportFocusProjectionResult): TileTooltipSource =
        when (projection.resolvedMode) {
            TileViewportFocusMode.TARGETING -> TileTooltipSource.TARGETING_CURSOR
            TileViewportFocusMode.INSPECT -> TileTooltipSource.INSPECT_CURSOR
            TileViewportFocusMode.PLAYER -> TileTooltipSource.FOCUSED_ENTITY
        }

    private fun modalBounds(
        kind: ModalFrameKind,
        request: TileOverlayModelRequest,
    ): RectInt =
        when (kind) {
            ModalFrameKind.TALENT_ASSIGN,
            ModalFrameKind.ACTIVE_TALENT_SLOT_CHOICE,
            -> talentAssignModalBounds(request)

            else -> defaultModalBounds(request.modalSafeBounds)
        }

    private fun defaultModalBounds(modalSafeBounds: ModalSafeBounds): RectInt {
        val padding = UiDesignTokens.fixed.modalPadding.roundToInt()
        val width = minOf(UiDesignTokens.fixed.modalMaxWidthCap.roundToInt(), modalSafeBounds.width - padding * 2).coerceAtLeast(160)
        val height = (modalSafeBounds.height - padding * 2).coerceIn(120, 420)
        return RectInt(
            x = modalSafeBounds.left + (modalSafeBounds.width - width) / 2,
            y = modalSafeBounds.bottom + (modalSafeBounds.height - height) / 2,
            width = width,
            height = height,
        )
    }

    private fun talentAssignModalBounds(request: TileOverlayModelRequest): RectInt {
        val bounds =
            TalentAssignPanelLayoutSolver.modalBounds(
                TalentAssignPanelModalBoundsRequest(
                    viewportWidth = request.shellContentBounds.right,
                    viewportHeight = request.shellContentBounds.top,
                ),
            )
        return RectInt(
            x = bounds.x.roundToInt(),
            y = bounds.y.roundToInt(),
            width = bounds.width.roundToInt(),
            height = bounds.height.roundToInt(),
        )
    }

    private fun talentAssignVisualBounds(request: TileOverlayModelRequest): RectInt {
        val viewportRight = request.viewportBounds.right.roundToInt()
        val viewportTop = request.viewportBounds.top.roundToInt()
        val horizontalPadding = 0
        val verticalPadding = 0
        return RectInt(
            x = horizontalPadding,
            y = verticalPadding,
            width = (viewportRight - horizontalPadding * 2).coerceAtLeast(720),
            height = (viewportTop - verticalPadding * 2).coerceAtLeast(520),
        )
    }

    private fun modalTitle(kind: ModalFrameKind): String =
        kind.name.lowercase().split('_').joinToString(" ") { word -> word.replaceFirstChar(Char::titlecase) }
}
