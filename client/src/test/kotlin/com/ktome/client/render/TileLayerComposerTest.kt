package com.ktome.client.render

import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TileLayerComposerTest {
    @Test
    fun ordersMapSublayersForFogLootCursorAndCombatFeedback() {
        val model = minimalModel()
        val plan =
            TileLayerComposer.compose(
                model,
                TileViewportFocusProjectionResult(
                    resolvedMode = TileViewportFocusMode.INSPECT,
                    resolvedFocusTile = Point(1, 1),
                    sourceKind = TileViewportFocusSourceKind.OVERLAY_INSPECT,
                    anchorTile = Point(1, 1),
                    tooltipAnchorKind = TileOverlayAnchorKind.WORLD_TILE,
                    isTooltipAnchorValid = true,
                ),
            )

        assertEquals(model.terrainTiles, plan.terrainBase)
        assertEquals(model.propTiles, plan.propsAndDecals)
        assertEquals(model.overlayTiles, plan.spriteOverlaysAndTelegraphs)
        assertEquals(model.actorTiles, plan.actors)
        assertEquals(model.fogTiles, plan.fogVeils)
        assertEquals(model.groundLootMarkers, plan.groundLootMarkers)
        assertEquals(model.combatFeedback, plan.combatFeedback)
    }

    @Test
    fun composesTerrainPropsVfxTelegraphBeforeActors() {
        val terrain = placement("tileset.forest_edge.ground_01")
        val prop = placement("prop.stairs.up")
        val terrainDecal = placement("vfx.terrain.interaction.ice")
        val telegraph = placement("vfx.telegraph.warning.sigil_01")
        val actor = placement("actor.arcanist")
        val model =
            minimalModel().copy(
                terrainTiles = listOf(terrain),
                propTiles = listOf(prop, terrainDecal),
                overlayTiles = listOf(telegraph),
                actorTiles = listOf(actor),
            )

        val plan = TileLayerComposer.compose(model, projection(TileViewportFocusMode.INSPECT, Point(0, 0), valid = true))

        assertEquals(listOf(terrain), plan.terrainBase)
        assertEquals(listOf(prop, terrainDecal), plan.propsAndDecals)
        assertEquals(listOf(telegraph), plan.spriteOverlaysAndTelegraphs)
        assertEquals(listOf(actor), plan.actors)
    }

    @Test
    fun keepsBossTelegraphAboveOrdinaryVfx() {
        val ordinaryVfx = placement("vfx.zone.effect.void_pressure_01", drawPriority = 1)
        val bossTelegraph = placement("vfx.boss.warning.sigil_01", drawPriority = 3)
        val model = minimalModel().copy(overlayTiles = listOf(bossTelegraph, ordinaryVfx))

        val plan = TileLayerComposer.compose(model, projection(TileViewportFocusMode.INSPECT, Point(0, 0), valid = true))

        assertEquals(listOf(ordinaryVfx, bossTelegraph), plan.spriteOverlaysAndTelegraphs)
        assertEquals("vfx.boss.warning.sigil_01", plan.spriteOverlaysAndTelegraphs.last().asset.resolvedKey)
    }

    @Test
    fun drawsOnlyActiveProjectionCursor() {
        val model = minimalModel()
        val targeting =
            TileLayerComposer.compose(
                model,
                projection(TileViewportFocusMode.TARGETING, Point(2, 2), valid = true),
            )
        val inspect =
            TileLayerComposer.compose(
                model,
                projection(TileViewportFocusMode.INSPECT, Point(1, 1), valid = true),
            )
        val fallback =
            TileLayerComposer.compose(
                model,
                projection(TileViewportFocusMode.TARGETING, model.playerTile, valid = false),
            )

        assertEquals(TileViewportFocusMode.TARGETING, targeting.activeCursor?.mode)
        assertEquals(Point(2, 2), targeting.activeCursor?.tile)
        assertEquals(TileViewportFocusMode.INSPECT, inspect.activeCursor?.mode)
        assertEquals(Point(1, 1), inspect.activeCursor?.tile)
        assertNull(fallback.activeCursor)
    }

    private fun projection(
        mode: TileViewportFocusMode,
        focus: Point,
        valid: Boolean,
    ): TileViewportFocusProjectionResult =
        TileViewportFocusProjectionResult(
            resolvedMode = mode,
            resolvedFocusTile = focus,
            sourceKind =
                if (mode == TileViewportFocusMode.TARGETING) {
                    TileViewportFocusSourceKind.OVERLAY_TARGETING
                } else {
                    TileViewportFocusSourceKind.OVERLAY_INSPECT
                },
            anchorTile = focus.takeIf { valid },
            tooltipAnchorKind = TileOverlayAnchorKind.WORLD_TILE.takeIf { valid },
            isTooltipAnchorValid = valid,
        )

    private fun minimalModel(): TileRenderModel =
        TileRenderModel(
            terrainTiles = emptyList(),
            propTiles = emptyList(),
            overlayTiles = emptyList(),
            groundLootMarkers = emptyList(),
            actorTiles = emptyList(),
            fogTiles = emptyList(),
            targetCursorState = TileTargetCursorState.LEGAL,
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
                    focusLines = emptyList(),
                    hotbar = emptyList(),
                    summaryText = "",
                ),
            messageLines = emptyList(),
            logPresentation = com.ktome.client.ui.panel.LogPresentationModel(emptyList(), "empty", "fallback"),
            playerCard = com.ktome.client.ui.panel.PlayerCardModel("Hero", "HP 1/1", "STA 1/1", null, 0, "empty"),
            targetCard = com.ktome.client.ui.panel.TargetCardModel(null, emptyList(), "empty"),
            actionPanel = com.ktome.client.ui.panel.ActionPanelModel(emptyList(), "empty"),
            combatFeedback = emptyList(),
            sidebar = TileSidebarModel("Sidebar", emptyList()),
            shell = TileShellModel(TilePanelModel("Left", emptyList()), TilePanelModel("Right", emptyList()), emptyList()),
            playerTile = Point.ZERO,
            mapDimensions = TileMapDimensions(10, 10),
        )

    private fun gauge(type: String): TileGaugeModel =
        TileGaugeModel(label = type, current = 1, max = 1, tone = TileTextTone.WHITE, resourceTypeId = type)

    private fun placement(
        key: String,
        drawPriority: Int = 0,
    ): TileVisualPlacement =
        TileVisualPlacement(
            x = 0,
            y = 0,
            drawPriority = drawPriority,
            asset =
                ResolvedVisualAsset(
                    requestedKey = key,
                    resolvedKey = key,
                    matchedByPrefix = false,
                    fallbackUsed = false,
                    entry =
                        VisualManifestEntry(
                            key = key,
                            category = "test",
                            rawOutputPath = "dark-v1/test/${key.replace('.', '_')}.png",
                            footprint = "1x1",
                        ),
                ),
        )
}
