package com.ktome.client.render

import com.ktome.core.map.Point

internal data class TileCursorLayerModel(
    val tile: Point,
    val mode: TileViewportFocusMode,
    val state: TileTargetCursorState?,
)

internal data class TilePlayerIndicatorModel(
    val tile: Point,
)

internal data class TileMapLayerPlan(
    val terrainBase: List<TileVisualPlacement>,
    val propsAndDecals: List<TileVisualPlacement>,
    val spriteOverlaysAndTelegraphs: List<TileVisualPlacement>,
    val actors: List<TileVisualPlacement>,
    val fogVeils: List<TileFogPlacement>,
    val groundLootMarkers: List<TileGroundLootMarkerModel>,
    val playerIndicators: List<TilePlayerIndicatorModel>,
    val activeCursor: TileCursorLayerModel?,
    val combatFeedback: List<TileCombatFeedbackModel>,
)

internal object TileLayerComposer {
    fun compose(
        model: TileRenderModel,
        projection: TileViewportFocusProjectionResult,
    ): TileMapLayerPlan =
        TileMapLayerPlan(
            terrainBase = model.terrainTiles,
            propsAndDecals = model.propTiles,
            spriteOverlaysAndTelegraphs = model.overlayTiles,
            actors = model.actorTiles,
            fogVeils = model.fogTiles,
            groundLootMarkers = model.groundLootMarkers,
            playerIndicators = listOf(TilePlayerIndicatorModel(model.playerTile)),
            activeCursor = activeCursor(model, projection),
            combatFeedback = model.combatFeedback,
        )

    private fun activeCursor(
        model: TileRenderModel,
        projection: TileViewportFocusProjectionResult,
    ): TileCursorLayerModel? {
        if (!projection.isTooltipAnchorValid || projection.resolvedMode == TileViewportFocusMode.PLAYER) {
            return null
        }
        return TileCursorLayerModel(
            tile = projection.resolvedFocusTile,
            mode = projection.resolvedMode,
            state =
                if (projection.resolvedMode == TileViewportFocusMode.TARGETING) {
                    model.targetCursorState
                } else {
                    null
                },
        )
    }
}
