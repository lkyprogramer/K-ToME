package com.ktome.client.render

internal object TileLayerComposer {
    fun compose(model: TileRenderModel): List<TileVisualPlacement> =
        buildList {
            addAll(model.terrainTiles)
            addAll(model.propTiles)
            addAll(model.overlayTiles)
            addAll(model.actorTiles)
        }
}
