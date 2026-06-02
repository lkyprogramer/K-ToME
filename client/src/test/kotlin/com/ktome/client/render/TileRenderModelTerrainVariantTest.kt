package com.ktome.client.render

import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RenderMetadataSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TileRenderModelTerrainVariantTest {
    @Test
    fun `terrain base uses manifest floor variants deterministically without changing snapshot keys`() {
        val assets = ClientAssetBundleLoader.load()
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val snapshot = sampleRuinsSnapshot(width = 4, height = 3)

        val first =
            TileRenderModelBuilder
                .build(localizer, assets.visualResolver, snapshot, OverlayState(mode = UiMode.MAP))
                .terrainTiles
        val second =
            TileRenderModelBuilder
                .build(localizer, assets.visualResolver, snapshot, OverlayState(mode = UiMode.MAP))
                .terrainTiles
        val firstKeys = first.map { placement -> placement.asset.resolvedKey }
        val secondKeys = second.map { placement -> placement.asset.resolvedKey }
        val firstFlips = first.map { placement -> placement.flipX to placement.flipY }
        val secondFlips = second.map { placement -> placement.flipX to placement.flipY }

        assertEquals(secondKeys, firstKeys)
        assertEquals(secondFlips, firstFlips)
        assertTrue(firstKeys.any { key -> key != "tileset.ruins.ground_01" }, firstKeys.toString())
        assertTrue(
            firstKeys.all { key -> key in assets.visualResolver.terrainVariantKeys("tileset.ruins.ground_01") },
            firstKeys.toString(),
        )
        assertTrue(
            first.any { placement -> placement.flipX || placement.flipY },
            "floor variant placement should also use deterministic flips so repeated 32px stone motifs do not keep the same orientation",
        )
        assertTrue(snapshot.mapCells.all { cell -> cell.terrainVisualKey == "tileset.ruins.ground_01" })
    }

    @Test
    fun `wall terrain uses manifest wall family pieces from adjacency without changing snapshot keys`() {
        val assets = ClientAssetBundleLoader.load()
        val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)
        val floor =
            (1..3).flatMap { x ->
                (1..3).map { y ->
                    MapCellSnapshot(
                        x = x,
                        y = y,
                        visibility = CellVisibilitySnapshot.VISIBLE,
                        terrainTypeId = "floor",
                        terrainVisualKey = "tileset.ruins.ground_01",
                    )
                }
            } +
                listOf(
                    MapCellSnapshot(1, 0, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "floor", terrainVisualKey = "tileset.ruins.ground_01"),
                    MapCellSnapshot(0, 1, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "floor", terrainVisualKey = "tileset.ruins.ground_01"),
                )
        val walls =
            listOf(
                MapCellSnapshot(2, 4, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "wall", terrainVisualKey = "tileset.ruins.wall_01"),
                MapCellSnapshot(2, 0, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "wall", terrainVisualKey = "tileset.ruins.wall_01"),
                MapCellSnapshot(0, 3, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "wall", terrainVisualKey = "tileset.ruins.wall_01"),
                MapCellSnapshot(0, 2, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "wall", terrainVisualKey = "tileset.ruins.wall_01"),
                MapCellSnapshot(4, 2, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "wall", terrainVisualKey = "tileset.ruins.wall_01"),
                MapCellSnapshot(0, 0, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "wall", terrainVisualKey = "tileset.ruins.wall_01"),
                MapCellSnapshot(4, 4, CellVisibilitySnapshot.VISIBLE, terrainTypeId = "wall", terrainVisualKey = "tileset.ruins.wall_01"),
            )
        val snapshot = sampleRuinsSnapshot(width = 5, height = 5, cells = floor + walls, playerX = 2, playerY = 2)

        val wallPlacements =
            TileRenderModelBuilder
                .build(localizer, assets.visualResolver, snapshot, OverlayState(mode = UiMode.MAP))
                .terrainTiles
                .filter { placement -> placement.asset.entry.category == "tile_wall" }
        val wallKeys =
            wallPlacements
                .map { placement -> placement.asset.resolvedKey }
                .toSet()
        val wallByPoint = wallPlacements.associateBy { placement -> placement.x to placement.y }

        assertTrue("tileset.ruins.wall_01" in wallKeys, wallKeys.toString())
        assertTrue("tileset.ruins.wall_01.crown" in wallKeys, wallKeys.toString())
        assertTrue("tileset.ruins.wall_01.side" in wallKeys, wallKeys.toString())
        assertTrue("tileset.ruins.wall_01.corner" in wallKeys, wallKeys.toString())
        val westSide = requireNotNull(wallByPoint[0 to 3])
        assertEquals("tileset.ruins.wall_01.side", westSide.asset.resolvedKey)
        assertFalse(westSide.flipX)
        assertFalse(westSide.flipY)
        val eastSide = requireNotNull(wallByPoint[4 to 2])
        assertEquals("tileset.ruins.wall_01.side", eastSide.asset.resolvedKey)
        assertTrue(eastSide.flipX)
        assertFalse(eastSide.flipY)
        val southWestCorner = requireNotNull(wallByPoint[0 to 0])
        assertEquals("tileset.ruins.wall_01.corner", southWestCorner.asset.resolvedKey)
        assertFalse(southWestCorner.flipX)
        assertTrue(southWestCorner.flipY)
        assertTrue(snapshot.mapCells.filter { cell -> cell.terrainTypeId == "wall" }.all { cell -> cell.terrainVisualKey == "tileset.ruins.wall_01" })
    }

    private fun sampleRuinsSnapshot(
        width: Int,
        height: Int,
        cells: List<MapCellSnapshot>? = null,
        playerX: Int = 0,
        playerY: Int = 0,
    ): RenderSnapshot =
        RenderSnapshot(
            metadata =
                RenderMetadataSnapshot(
                    revision = 1,
                    zoneId = "shattered_outpost",
                    zoneNameKey = "zone.shattered_outpost.name",
                    zoneDescKey = "zone.shattered_outpost.desc",
                    currentFloor = 1,
                    maxFloor = 2,
                    width = width,
                    height = height,
                    playerX = playerX,
                    playerY = playerY,
                    zoneVisualKey = "zone.shattered_outpost.visual",
                    zoneAudioProfile = "audio.zone.shattered_outpost",
                    tilesetKey = "tileset.ruins",
                    ambientProfile = "ambient.shattered_outpost",
                ),
            mapCells =
                cells ?:
                    (0 until width).flatMap { x ->
                        (0 until height).map { y ->
                            MapCellSnapshot(
                                x = x,
                                y = y,
                                visibility = CellVisibilitySnapshot.VISIBLE,
                                terrainTypeId = "floor",
                                terrainVisualKey = "tileset.ruins.ground_01",
                            )
                        }
                    },
            actors =
                listOf(
                    ActorRenderSnapshot(
                        entityId = 1,
                        x = playerX,
                        y = playerY,
                        visualKey = "actor.vanguard",
                        nameKey = "actor.player.name",
                        isPlayer = true,
                        roleKind = ActorRoleKindSnapshot.PLAYER,
                    ),
                ),
            uiState =
                RenderUiStateSnapshot(
                    playerStatus =
                        PlayerStatusSnapshot(
                            currentHp = 24,
                            maxHp = 24,
                            currentResource = 12,
                            maxResource = 12,
                            resourceLabelKey = "ui.hud.stamina.short",
                            resourceTypeId = "STAMINA",
                            level = 1,
                            currentExperience = 0,
                            nextLevelRequirement = 12,
                            statPoints = 0,
                            talentPoints = 0,
                            attack = 7,
                            defense = 5,
                            accuracy = 6,
                            evasion = 4,
                            speed = 100,
                        ),
                    equipment = emptyList(),
                    talents = emptyList(),
                    inventory = emptyList(),
                    targetablePositions = listOf(GridPointSnapshot(0, 0)),
                ),
        )
}
