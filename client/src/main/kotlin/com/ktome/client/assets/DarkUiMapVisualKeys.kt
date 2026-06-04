package com.ktome.client.assets

import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.MapCellSnapshot

internal data class RoomArtPlateFamilyVisualKeys(
    val tilesetKey: String,
    val groundKey: String,
    val wallKey: String,
    val roomArtPlateKey: String,
) {
    fun ownsBaseMaterial(visualKey: String): Boolean = visualKey == groundKey || visualKey == wallKey
}

internal object DarkUiMapVisualKeys {
    const val RUINS_TILESET: String = "tileset.ruins"
    const val RUINS_GROUND: String = "tileset.ruins.ground_01"
    const val RUINS_WALL: String = "tileset.ruins.wall_01"
    const val RUINS_ROOM_MATERIAL_BREAKUP: String = "tileset.ruins.room_breakup_01"
    const val RUINS_ROOM_ART_PLATE_PROTOTYPE: String = "ui.map_stage.ruins.room_plate.pr08_demo"
    const val RUINS_ROOM_TOPOLOGY_SOURCE_PROTOTYPE: String = "ui.map_stage.ruins.room_topology_source.pr08_demo"

    const val FOREST_EDGE_TILESET: String = "tileset.forest_edge"
    const val FOREST_EDGE_GROUND: String = "tileset.forest_edge.ground_01"
    const val FOREST_EDGE_WALL: String = "tileset.forest_edge.wall_01"
    const val FOREST_EDGE_ROOM_ART_PLATE_PROTOTYPE: String = "ui.map_stage.forest_edge.room_plate.pr08_demo"
    const val FOREST_EDGE_ROOM_TOPOLOGY_SOURCE_PROTOTYPE: String = "ui.map_stage.forest_edge.room_topology_source.pr08_demo"

    const val MINE_TILESET: String = "tileset.mine"
    const val MINE_GROUND: String = "tileset.mine.ground_01"
    const val MINE_WALL: String = "tileset.mine.wall_01"
    const val MINE_ROOM_ART_PLATE_PROTOTYPE: String = "ui.map_stage.mine.room_plate.pr08_demo"
    const val MINE_ROOM_TOPOLOGY_SOURCE_PROTOTYPE: String = "ui.map_stage.mine.room_topology_source.pr08_demo"

    const val SHADOW_DEPTHS_TILESET: String = "tileset.shadow_depths"
    const val SHADOW_DEPTHS_GROUND: String = "tileset.shadow_depths.ground_01"
    const val SHADOW_DEPTHS_WALL: String = "tileset.shadow_depths.wall_01"
    const val SHADOW_DEPTHS_ROOM_ART_PLATE_PROTOTYPE: String = "ui.map_stage.shadow_depths.room_plate.pr08_demo"
    const val SHADOW_DEPTHS_ROOM_TOPOLOGY_SOURCE_PROTOTYPE: String = "ui.map_stage.shadow_depths.room_topology_source.pr08_demo"

    private val roomArtPlateFamilies: List<RoomArtPlateFamilyVisualKeys> =
        listOf(
            RoomArtPlateFamilyVisualKeys(
                tilesetKey = RUINS_TILESET,
                groundKey = RUINS_GROUND,
                wallKey = RUINS_WALL,
                roomArtPlateKey = RUINS_ROOM_ART_PLATE_PROTOTYPE,
            ),
            RoomArtPlateFamilyVisualKeys(
                tilesetKey = FOREST_EDGE_TILESET,
                groundKey = FOREST_EDGE_GROUND,
                wallKey = FOREST_EDGE_WALL,
                roomArtPlateKey = FOREST_EDGE_ROOM_ART_PLATE_PROTOTYPE,
            ),
            RoomArtPlateFamilyVisualKeys(
                tilesetKey = MINE_TILESET,
                groundKey = MINE_GROUND,
                wallKey = MINE_WALL,
                roomArtPlateKey = MINE_ROOM_ART_PLATE_PROTOTYPE,
            ),
            RoomArtPlateFamilyVisualKeys(
                tilesetKey = SHADOW_DEPTHS_TILESET,
                groundKey = SHADOW_DEPTHS_GROUND,
                wallKey = SHADOW_DEPTHS_WALL,
                roomArtPlateKey = SHADOW_DEPTHS_ROOM_ART_PLATE_PROTOTYPE,
            ),
        )

    fun roomArtPlateFamilyFor(
        tilesetKey: String,
        cells: Iterable<MapCellSnapshot>,
    ): RoomArtPlateFamilyVisualKeys? {
        val family = roomArtPlateFamilies.firstOrNull { candidate -> candidate.tilesetKey == tilesetKey } ?: return null
        return if (cells.any { cell -> cell.isVisibleFamilyMaterial(family) }) family else null
    }

    fun roomTopologySourceKeyFor(family: RoomArtPlateFamilyVisualKeys): String? =
        when (family.tilesetKey) {
            RUINS_TILESET -> RUINS_ROOM_TOPOLOGY_SOURCE_PROTOTYPE
            FOREST_EDGE_TILESET -> FOREST_EDGE_ROOM_TOPOLOGY_SOURCE_PROTOTYPE
            MINE_TILESET -> MINE_ROOM_TOPOLOGY_SOURCE_PROTOTYPE
            SHADOW_DEPTHS_TILESET -> SHADOW_DEPTHS_ROOM_TOPOLOGY_SOURCE_PROTOTYPE
            else -> null
        }

    fun supportsRuinsRoomPresentation(
        tilesetKey: String,
        cells: Iterable<MapCellSnapshot>,
    ): Boolean =
        roomArtPlateFamilyFor(tilesetKey, cells)?.tilesetKey == RUINS_TILESET

    fun isRuinsBaseMaterial(visualKey: String): Boolean = visualKey == RUINS_GROUND || visualKey == RUINS_WALL

    private fun MapCellSnapshot.isVisibleFamilyMaterial(family: RoomArtPlateFamilyVisualKeys): Boolean =
        visibility == CellVisibilitySnapshot.VISIBLE && family.ownsBaseMaterial(terrainVisualKey)
}
