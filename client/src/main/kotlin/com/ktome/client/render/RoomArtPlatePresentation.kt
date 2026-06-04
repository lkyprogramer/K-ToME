package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.ktome.client.assets.DarkUiMapVisualKeys
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.RoomArtPlateFamilyVisualKeys
import com.ktome.client.assets.TerrainWallPieceRole
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.core.map.Point
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import kotlin.math.abs

internal data class RoomPresentationPlan(
    val artPlate: RoomArtPlateModel? = null,
) {
    val compositorStrategy: RoomCompositorStrategy =
        when {
            artPlate == null -> RoomCompositorStrategy.LEGACY_TILE_DECORATION
            artPlate.topology.decision.drawsFullPlate -> RoomCompositorStrategy.ART_PLATE_PRESENTATION
            else -> RoomCompositorStrategy.TOPOLOGY_RISK_HYBRID_PRESENTATION
        }
}

internal enum class RoomCompositorStrategy {
    LEGACY_TILE_DECORATION,
    ART_PLATE_PRESENTATION,
    TOPOLOGY_RISK_HYBRID_PRESENTATION,
    ;

    val usesRoomArtPlateGroundMaterial: Boolean
        get() = this != LEGACY_TILE_DECORATION

    val usesRoomArtPlateInteractionGrammar: Boolean
        get() = this != LEGACY_TILE_DECORATION
}

internal data class RoomArtPlateModel(
    val source: RoomArtPlateSource,
    val topology: RoomArtPlateTopology,
)

internal data class RoomArtPlateSource(
    val asset: ResolvedVisualAsset,
    val topologySourceAsset: ResolvedVisualAsset,
    val topologySourceBandAlpha: Float,
    val tilesetKey: String,
    val family: RoomArtPlateFamilyVisualKeys,
    val componentAssets: RoomArtPlateComponentAssets,
)

internal class RoomArtPlateComponentAssets private constructor(
    private val byRole: Map<TerrainWallPieceRole, ResolvedVisualAsset>,
) {
    fun assetFor(role: TerrainWallPieceRole): ResolvedVisualAsset? = byRole[role]

    companion object {
        fun resolve(
            visualResolver: VisualManifestResolver,
            wallKey: String,
        ): RoomArtPlateComponentAssets =
            RoomArtPlateComponentAssets(
                TerrainWallPieceRole.entries.associateWith { role ->
                    visualResolver.resolveTerrainWallPiece(wallKey, role)
                },
            )
    }
}

internal data class RoomArtPlateTopology(
    val shape: RoomArtPlateTopologyShape,
    val metrics: RoomArtPlateTopologyMetrics,
    val connectedComponents: Int,
    val decision: RoomArtPlateTopologyDecision,
)

internal data class RoomArtPlateTopologyShape(
    val bounds: RoomArtPlateTopologyBounds,
    val visiblePoints: Set<Point>,
)

internal data class RoomArtPlateTopologyBounds(
    val minX: Int,
    val minY: Int,
    val width: Int,
    val height: Int,
) {
    val area: Int = width * height
}

internal data class RoomArtPlateTopologyMetrics(
    val visibleCellCount: Int,
    val fillPermille: Int,
    val aspectPermille: Int,
)

internal enum class RoomArtPlateTopologyDecision {
    FULL_PLATE_SAFE,
    TOPOLOGY_RISK_DISCONNECTED,
    TOPOLOGY_RISK_LOW_FILL,
    TOPOLOGY_RISK_EXTREME_ASPECT,
    ;

    val drawsFullPlate: Boolean
        get() = this == FULL_PLATE_SAFE
}

internal object RoomArtPlateCatalog {
    private const val FALLBACK_TOPOLOGY_SOURCE_BAND_ALPHA = 0.36f
    private const val RUINS_DEDICATED_TOPOLOGY_SOURCE_BAND_ALPHA = 0.50f
    private const val NON_RUINS_DEDICATED_TOPOLOGY_SOURCE_BAND_ALPHA = 0.62f

    fun resolve(
        visualResolver: VisualManifestResolver,
        tilesetKey: String,
        cells: List<MapCellSnapshot>,
    ): RoomArtPlateModel? {
        val family = DarkUiMapVisualKeys.roomArtPlateFamilyFor(tilesetKey, cells) ?: return null
        if (!visualResolver.canResolve(family.roomArtPlateKey)) {
            return null
        }
        val artPlateAsset = visualResolver.resolve(family.roomArtPlateKey)
        val topology = RoomArtPlateTopologyContract.evaluate(family, cells) ?: return null
        val topologySourceKey =
            if (topology.decision.drawsFullPlate) {
                null
            } else {
                DarkUiMapVisualKeys.roomTopologySourceKeyFor(family)
            }
        val topologySourceAsset =
            if (topologySourceKey == null) {
                artPlateAsset
            } else {
                if (!visualResolver.canResolve(topologySourceKey)) {
                    return null
                }
                visualResolver.resolve(topologySourceKey)
            }
        return RoomArtPlateModel(
            source =
                RoomArtPlateSource(
                    asset = artPlateAsset,
                    topologySourceAsset = topologySourceAsset,
                    topologySourceBandAlpha =
                        if (topologySourceKey == null) {
                            FALLBACK_TOPOLOGY_SOURCE_BAND_ALPHA
                        } else {
                            dedicatedTopologySourceBandAlpha(tilesetKey)
                        },
                    tilesetKey = tilesetKey,
                    family = family,
                    componentAssets = RoomArtPlateComponentAssets.resolve(visualResolver, family.wallKey),
                ),
            topology = topology,
        )
    }

    private fun dedicatedTopologySourceBandAlpha(tilesetKey: String): Float =
        when (tilesetKey) {
            DarkUiMapVisualKeys.RUINS_TILESET -> RUINS_DEDICATED_TOPOLOGY_SOURCE_BAND_ALPHA
            else -> NON_RUINS_DEDICATED_TOPOLOGY_SOURCE_BAND_ALPHA
        }
}

internal object RoomArtPlateTopologyContract {
    private const val PLATE_SAFE_MIN_FILL_PERMILLE = 750
    private const val PLATE_SAFE_MAX_ASPECT_PERMILLE = 1600

    fun evaluate(
        family: RoomArtPlateFamilyVisualKeys,
        cells: List<MapCellSnapshot>,
    ): RoomArtPlateTopology? {
        val visiblePoints =
            cells
                .filter { cell ->
                    cell.visibility == CellVisibilitySnapshot.VISIBLE &&
                        family.ownsBaseMaterial(cell.terrainVisualKey)
                }.map { cell -> Point(cell.x, cell.y) }
        val visiblePointSet = visiblePoints.toSet()
        if (visiblePointSet.isEmpty()) {
            return null
        }

        val bounds = visiblePoints.toRoomArtPlateTopologyBounds()
        val metrics =
            RoomArtPlateTopologyMetrics(
                visibleCellCount = visiblePoints.size,
                fillPermille = visiblePoints.size * 1000 / bounds.area,
                aspectPermille = maxOf(bounds.width, bounds.height) * 1000 / minOf(bounds.width, bounds.height),
            )
        val connectedComponents = visiblePointSet.countConnectedComponents()
        val decision =
            when {
                connectedComponents != 1 -> RoomArtPlateTopologyDecision.TOPOLOGY_RISK_DISCONNECTED
                metrics.fillPermille < PLATE_SAFE_MIN_FILL_PERMILLE -> RoomArtPlateTopologyDecision.TOPOLOGY_RISK_LOW_FILL
                metrics.aspectPermille > PLATE_SAFE_MAX_ASPECT_PERMILLE -> RoomArtPlateTopologyDecision.TOPOLOGY_RISK_EXTREME_ASPECT
                else -> RoomArtPlateTopologyDecision.FULL_PLATE_SAFE
            }
        return RoomArtPlateTopology(
            shape =
                RoomArtPlateTopologyShape(
                    bounds = bounds,
                    visiblePoints = visiblePointSet,
                ),
            metrics = metrics,
            connectedComponents = connectedComponents,
            decision = decision,
        )
    }

    private fun List<Point>.toRoomArtPlateTopologyBounds(): RoomArtPlateTopologyBounds {
        val minX = minOf { point -> point.x }
        val maxX = maxOf { point -> point.x }
        val minY = minOf { point -> point.y }
        val maxY = maxOf { point -> point.y }
        return RoomArtPlateTopologyBounds(
            minX = minX,
            minY = minY,
            width = maxX - minX + 1,
            height = maxY - minY + 1,
        )
    }

    private fun Set<Point>.countConnectedComponents(): Int {
        val remaining = toMutableSet()
        var componentCount = 0
        while (remaining.isNotEmpty()) {
            componentCount += 1
            val queue = ArrayDeque<Point>()
            val start = remaining.first()
            remaining.remove(start)
            queue.add(start)
            while (queue.isNotEmpty()) {
                val point = queue.removeFirst()
                point.cardinalNeighbors().forEach { neighbor ->
                    if (remaining.remove(neighbor)) {
                        queue.add(neighbor)
                    }
                }
            }
        }
        return componentCount
    }

    private fun Point.cardinalNeighbors(): List<Point> =
        listOf(
            Point(x + 1, y),
            Point(x - 1, y),
            Point(x, y + 1),
            Point(x, y - 1),
        )
}

internal object RoomArtPlateRenderer {
    fun render(
        canvas: TileCanvas,
        frame: MapRenderFrame,
    ) {
        val artPlate = frame.model.roomPresentationPlan.artPlate ?: return
        when (artPlate.topology.decision) {
            RoomArtPlateTopologyDecision.FULL_PLATE_SAFE -> {
                val bounds = roomBounds(frame) ?: return
                canvas.drawAsset(artPlate.source.asset, bounds)
            }

            RoomArtPlateTopologyDecision.TOPOLOGY_RISK_DISCONNECTED,
            RoomArtPlateTopologyDecision.TOPOLOGY_RISK_LOW_FILL,
            RoomArtPlateTopologyDecision.TOPOLOGY_RISK_EXTREME_ASPECT,
            -> renderTopologyRiskHybrid(canvas, frame, artPlate)
        }
    }

    private fun renderTopologyRiskHybrid(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        artPlate: RoomArtPlateModel,
    ) {
        val visiblePoints = artPlate.topology.shape.visiblePoints.filter(frame.viewport::containsTile).toSet()
        if (visiblePoints.isEmpty()) {
            return
        }
        drawTopologyRiskBandMantleFields(canvas, frame, artPlate, visiblePoints)
        drawTopologyRiskMaterialRuns(canvas, frame, visiblePoints)
        drawTopologyRiskInteriorSeamDissolveFields(canvas, frame, visiblePoints)
        drawTopologyRiskAmbientDepthFields(canvas, frame, visiblePoints)
        drawTopologyRiskSourceCroppedBands(canvas, frame, artPlate, visiblePoints)
        drawTopologyRiskAperturePressure(canvas, frame, visiblePoints)
        drawTopologyRiskBoundaryWallMassSlabs(canvas, frame, visiblePoints)
        drawTopologyRiskWallRunVeils(canvas, frame, visiblePoints)
        drawTopologyRiskLocalLightPools(canvas, frame, artPlate.topology, visiblePoints)
        drawTopologyRiskBoundaryMarks(canvas, frame, visiblePoints)
        drawTopologyRiskWallComponents(canvas, frame, artPlate, visiblePoints)
    }

    private fun drawTopologyRiskBandMantleFields(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        artPlate: RoomArtPlateModel,
        visiblePoints: Set<Point>,
    ) {
        val palette = topologyRiskMantlePalette(artPlate.source.tilesetKey)
        visiblePoints
            .toTopologyBands()
            .filter { band -> band.width >= 3 && band.height >= 2 }
            .forEach { band ->
                val startRect = frame.viewport.tileRect(Point(band.startX, band.startY))
                val cellSize = frame.viewport.cellSize.toFloat()
                val x = startRect.x.toFloat()
                val y = startRect.y.toFloat()
                val width = cellSize * band.width
                val height = cellSize * band.height
                canvas.drawRect(
                    tileBounds(x + 1f, y + 2f, width - 2f, height - 4f),
                    plateColor(palette.fieldHex, palette.fieldAlpha),
                )
                canvas.drawRect(
                    tileBounds(x + 5f, y + height - 12f, width - 10f, 9f),
                    plateColor(palette.upperShadowHex, palette.upperShadowAlpha),
                )
                canvas.drawRect(
                    tileBounds(x + 8f, y + 4f, (width - 16f).coerceAtLeast(4f), 7f),
                    plateColor(palette.lowerLipHex, palette.lowerLipAlpha),
                )
            }
    }

    private fun drawTopologyRiskSourceCroppedBands(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        artPlate: RoomArtPlateModel,
        visiblePoints: Set<Point>,
    ) {
        visiblePoints
            .toTopologyBands()
            .filter { band -> band.width >= 3 && band.height >= 2 }
            .forEach { band ->
                val startRect = frame.viewport.tileRect(Point(band.startX, band.startY))
                val cellSize = frame.viewport.cellSize.toFloat()
                val x = startRect.x.toFloat()
                val y = startRect.y.toFloat()
                val width = cellSize * band.width
                val height = cellSize * band.height
                canvas.drawAsset(
                    TileAssetDraw(
                        asset = artPlate.source.topologySourceAsset,
                        bounds = tileBounds(x + 4f, y + 4f, width - 8f, height - 8f),
                        alpha = artPlate.source.topologySourceBandAlpha,
                        sourceRegion = band.toSourceRegion(artPlate.topology.shape.bounds),
                    ),
                )
            }
    }

    private fun drawTopologyRiskAperturePressure(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        visiblePoints: Set<Point>,
    ) {
        visiblePoints
            .toTopologyBands()
            .filter { band -> band.width >= 3 && band.height >= 2 }
            .forEachIndexed { index, band ->
                val startRect = frame.viewport.tileRect(Point(band.startX, band.startY))
                val cellSize = frame.viewport.cellSize.toFloat()
                val x = startRect.x.toFloat()
                val y = startRect.y.toFloat()
                val width = cellSize * band.width
                val height = cellSize * band.height
                val stagger = if (index % 2 == 0) 0.12f else 0.22f
                canvas.drawRect(
                    tileBounds(x + cellSize * 0.30f, y + cellSize * 0.42f, width - cellSize * 0.60f, height - cellSize * 0.84f),
                    plateColor("566447", 0.046f),
                )
                val upperWidth = width * 0.68f
                val upperX = (x + width * stagger).coerceAtMost(x + width - upperWidth - cellSize * 0.12f)
                canvas.drawRect(
                    tileBounds(upperX, y + height - cellSize * 0.58f, upperWidth, cellSize * 0.34f),
                    plateColor("070A08", 0.162f),
                )
                canvas.drawRect(
                    tileBounds(upperX + upperWidth * 0.16f, y + height - cellSize * 0.25f, upperWidth * 0.52f, 3f),
                    plateColor("D2AD68", 0.182f),
                )

                val lowerWidth = width * 0.54f
                val lowerX = x + width * if (index % 2 == 0) 0.34f else 0.18f
                canvas.drawRect(
                    tileBounds(lowerX.coerceAtMost(x + width - lowerWidth), y + cellSize * 0.12f, lowerWidth, cellSize * 0.28f),
                    plateColor("060806", 0.138f),
                )
                canvas.drawRect(
                    tileBounds(lowerX + lowerWidth * 0.20f, y + cellSize * 0.36f, lowerWidth * 0.44f, 2.5f),
                    plateColor("A8905E", 0.132f),
                )

                if (band.height >= 3) {
                    val sideWidth = cellSize * 0.42f
                    canvas.drawRect(
                        tileBounds(x + width - sideWidth - cellSize * 0.10f, y + height * 0.22f, sideWidth, height * 0.52f),
                        plateColor("070A08", 0.154f),
                    )
                    canvas.drawRect(
                        tileBounds(x + width - sideWidth - cellSize * 0.04f, y + height * 0.34f, 2.5f, height * 0.30f),
                        plateColor("D2AD68", 0.126f),
                    )
                }
            }
    }

    private fun drawTopologyRiskBoundaryWallMassSlabs(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        visiblePoints: Set<Point>,
    ) {
        visiblePoints
            .groupBy(Point::y)
            .forEach { (rowY, rowPoints) ->
                val rowXs = rowPoints.map(Point::x).toSet()
                rowXs
                    .filter { x -> Point(x, rowY + 1) !in visiblePoints }
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 3 }
                    .forEach { run -> drawHorizontalTopologyRiskBoundaryWallMassSlab(canvas, frame, rowY, run, north = true) }
                rowXs
                    .filter { x -> Point(x, rowY - 1) !in visiblePoints }
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 3 }
                    .forEach { run -> drawHorizontalTopologyRiskBoundaryWallMassSlab(canvas, frame, rowY, run, north = false) }
            }

        visiblePoints
            .groupBy(Point::x)
            .forEach { (columnX, columnPoints) ->
                val columnYs = columnPoints.map(Point::y).toSet()
                columnYs
                    .filter { y -> Point(columnX - 1, y) !in visiblePoints }
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 3 }
                    .forEach { run -> drawVerticalTopologyRiskBoundaryWallMassSlab(canvas, frame, columnX, run, west = true) }
                columnYs
                    .filter { y -> Point(columnX + 1, y) !in visiblePoints }
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 3 }
                    .forEach { run -> drawVerticalTopologyRiskBoundaryWallMassSlab(canvas, frame, columnX, run, west = false) }
            }
    }

    private fun drawHorizontalTopologyRiskBoundaryWallMassSlab(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        rowY: Int,
        run: TopologyRun,
        north: Boolean,
    ) {
        val startRect = frame.viewport.tileRect(Point(run.start, rowY))
        val cellSize = frame.viewport.cellSize.toFloat()
        val x = startRect.x.toFloat()
        val y =
            if (north) {
                startRect.y + cellSize - 31f
            } else {
                startRect.y + 1f
            }
        val width = cellSize * run.length
        canvas.drawRect(
            tileBounds(x + 4f, y, width - 8f, 29f),
            plateColor("050604", 0.232f),
        )
        canvas.drawRect(
            tileBounds(x + cellSize * 0.92f, y + if (north) 19f else 7f, width - cellSize * 1.84f, 4f),
            plateColor("8A7654", 0.092f),
        )
        if (run.length >= 5) {
            val capX = x + cellSize * if (north) 2.20f else 1.35f
            canvas.drawRect(
                tileBounds(capX, y + if (north) 6f else 14f, cellSize * 1.70f, 10f),
                plateColor("171A14", 0.166f),
            )
            canvas.drawRect(
                tileBounds(capX + cellSize * 1.18f, y + if (north) 4f else 12f, 3f, 16f),
                plateColor("050604", 0.154f),
            )
        }
    }

    private fun drawVerticalTopologyRiskBoundaryWallMassSlab(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        columnX: Int,
        run: TopologyRun,
        west: Boolean,
    ) {
        val startRect = frame.viewport.tileRect(Point(columnX, run.start))
        val cellSize = frame.viewport.cellSize.toFloat()
        val x =
            if (west) {
                startRect.x + 1f
            } else {
                startRect.x + cellSize - 31f
            }
        val y = startRect.y.toFloat()
        val height = cellSize * run.length
        canvas.drawRect(
            tileBounds(x, y + 4f, 29f, height - 8f),
            plateColor("050604", 0.224f),
        )
        canvas.drawRect(
            tileBounds(if (west) x + 7f else x + 19f, y + cellSize * 0.92f, 4f, height - cellSize * 1.84f),
            plateColor("8A7654", 0.084f),
        )
        if (run.length >= 5) {
            val capY = y + cellSize * if (west) 2.35f else 1.42f
            canvas.drawRect(
                tileBounds(x + if (west) 13f else 6f, capY, 10f, cellSize * 1.56f),
                plateColor("171A14", 0.154f),
            )
            canvas.drawRect(
                tileBounds(x + if (west) 11f else 4f, capY + cellSize * 1.04f, 16f, 3f),
                plateColor("050604", 0.148f),
            )
        }
    }

    private fun drawTopologyRiskWallRunVeils(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        visiblePoints: Set<Point>,
    ) {
        visiblePoints
            .groupBy(Point::y)
            .forEach { (rowY, rowPoints) ->
                val rowXs = rowPoints.map(Point::x).toSet()
                rowXs
                    .filter { x -> Point(x, rowY + 1) !in visiblePoints }
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 3 }
                    .forEach { run -> drawHorizontalTopologyRiskWallRunVeil(canvas, frame, rowY, run, north = true) }
                rowXs
                    .filter { x -> Point(x, rowY - 1) !in visiblePoints }
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 3 }
                    .forEach { run -> drawHorizontalTopologyRiskWallRunVeil(canvas, frame, rowY, run, north = false) }
            }

        visiblePoints
            .groupBy(Point::x)
            .forEach { (columnX, columnPoints) ->
                val columnYs = columnPoints.map(Point::y).toSet()
                columnYs
                    .filter { y -> Point(columnX - 1, y) !in visiblePoints }
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 3 }
                    .forEach { run -> drawVerticalTopologyRiskWallRunVeil(canvas, frame, columnX, run, west = true) }
                columnYs
                    .filter { y -> Point(columnX + 1, y) !in visiblePoints }
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 3 }
                    .forEach { run -> drawVerticalTopologyRiskWallRunVeil(canvas, frame, columnX, run, west = false) }
            }
    }

    private fun drawHorizontalTopologyRiskWallRunVeil(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        rowY: Int,
        run: TopologyRun,
        north: Boolean,
    ) {
        val startRect = frame.viewport.tileRect(Point(run.start, rowY))
        val cellSize = frame.viewport.cellSize.toFloat()
        val x = startRect.x.toFloat()
        val y =
            if (north) {
                startRect.y + cellSize - 23f
            } else {
                startRect.y + 3f
            }
        canvas.drawRect(
            tileBounds(x + 5f, y, cellSize * run.length - 10f, 21f),
            plateColor("0B0D0A", 0.148f),
        )
        canvas.drawRect(
            tileBounds(x + 13f, if (north) y + 16f else y + 3f, cellSize * run.length - 26f, 2.5f),
            plateColor("9A8256", 0.074f),
        )
    }

    private fun drawVerticalTopologyRiskWallRunVeil(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        columnX: Int,
        run: TopologyRun,
        west: Boolean,
    ) {
        val startRect = frame.viewport.tileRect(Point(columnX, run.start))
        val cellSize = frame.viewport.cellSize.toFloat()
        val x =
            if (west) {
                startRect.x + 3f
            } else {
                startRect.x + cellSize - 23f
            }
        val y = startRect.y.toFloat()
        canvas.drawRect(
            tileBounds(x, y + 5f, 21f, cellSize * run.length - 10f),
            plateColor("0B0D0A", 0.148f),
        )
        canvas.drawRect(
            tileBounds(if (west) x + 3f else x + 16f, y + 13f, 2.5f, cellSize * run.length - 26f),
            plateColor("9A8256", 0.070f),
        )
    }

    private fun drawTopologyRiskMaterialRuns(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        visiblePoints: Set<Point>,
    ) {
        visiblePoints
            .groupBy(Point::y)
            .forEach { (_, rowPoints) ->
                rowPoints
                    .map(Point::x)
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 3 }
                    .forEach { run ->
                        val startRect = frame.viewport.tileRect(Point(run.start, rowPoints.first().y))
                        val cellSize = frame.viewport.cellSize.toFloat()
                        val x = startRect.x.toFloat()
                        val y = startRect.y.toFloat()
                        val width = cellSize * run.length
                        canvas.drawRect(
                            tileBounds(x + 3f, y + 4f, width - 6f, cellSize - 8f),
                            plateColor("18251F", 0.092f),
                        )
                        canvas.drawRect(
                            tileBounds(x + 8f, y + cellSize - 7f, (width - 16f).coerceAtLeast(4f), 2.5f),
                            plateColor("8A7654", 0.054f),
                        )
                    }
            }
    }

    private fun drawTopologyRiskInteriorSeamDissolveFields(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        visiblePoints: Set<Point>,
    ) {
        val seamDissolve = plateColor("111610", 0.112f)
        visiblePoints
            .groupBy(Point::y)
            .forEach { (rowY, rowPoints) ->
                rowPoints
                    .map(Point::x)
                    .filter { x -> Point(x, rowY + 1) in visiblePoints }
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 4 }
                    .forEach { run ->
                        val startRect = frame.viewport.tileRect(Point(run.start, rowY))
                        val cellSize = frame.viewport.cellSize.toFloat()
                        val x = startRect.x.toFloat()
                        val y = startRect.y.toFloat()
                        val width = cellSize * run.length
                        canvas.drawRect(
                            tileBounds(x + 7f, y + cellSize - 5.5f, width - 14f, 11f),
                            seamDissolve,
                        )
                    }
            }
        visiblePoints
            .groupBy(Point::x)
            .forEach { (columnX, columnPoints) ->
                columnPoints
                    .map(Point::y)
                    .filter { y -> Point(columnX + 1, y) in visiblePoints }
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 4 }
                    .forEach { run ->
                        val startRect = frame.viewport.tileRect(Point(columnX, run.start + run.length - 1))
                        val cellSize = frame.viewport.cellSize.toFloat()
                        val x = startRect.x.toFloat()
                        val y = startRect.y.toFloat()
                        val height = cellSize * run.length
                        canvas.drawRect(
                            tileBounds(x + cellSize - 5.5f, y + 7f, 11f, height - 14f),
                            seamDissolve,
                        )
                    }
            }
    }

    private fun drawTopologyRiskAmbientDepthFields(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        visiblePoints: Set<Point>,
    ) {
        val upperShadow = plateColor("050604", 0.118f)
        val lowerShadow = plateColor("0B0906", 0.084f)
        val sidePressure = plateColor("060806", 0.104f)
        visiblePoints
            .groupBy(Point::y)
            .forEach { (rowY, rowPoints) ->
                rowPoints
                    .map(Point::x)
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 3 }
                    .forEach { run ->
                        val startRect = frame.viewport.tileRect(Point(run.start, rowY))
                        val cellSize = frame.viewport.cellSize.toFloat()
                        val x = startRect.x.toFloat()
                        val y = startRect.y.toFloat()
                        val width = cellSize * run.length
                        val northOpenCount = run.countOpenNeighbor(rowY, visiblePoints, verticalOffset = 1)
                        val southOpenCount = run.countOpenNeighbor(rowY, visiblePoints, verticalOffset = -1)
                        val openThreshold = maxOf(2, run.length / 2)
                        if (northOpenCount >= openThreshold) {
                            canvas.drawRect(tileBounds(x + 5f, y + cellSize - 15f, width - 10f, 14f), upperShadow)
                        }
                        if (southOpenCount >= openThreshold) {
                            canvas.drawRect(tileBounds(x + 8f, y + 2f, width - 16f, 10f), lowerShadow)
                        }
                        if (Point(run.start - 1, rowY) !in visiblePoints) {
                            canvas.drawRect(tileBounds(x + 1f, y + 4f, 14f, cellSize - 8f), sidePressure)
                        }
                        if (Point(run.start + run.length, rowY) !in visiblePoints) {
                            canvas.drawRect(tileBounds(x + width - 15f, y + 4f, 14f, cellSize - 8f), sidePressure)
                        }
                    }
            }
    }

    private fun drawTopologyRiskLocalLightPools(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        topology: RoomArtPlateTopology,
        visiblePoints: Set<Point>,
    ) {
        topologyRiskLightAnchors(topology.shape, visiblePoints).forEach { anchor ->
            val rect = frame.viewport.tileRect(anchor.point)
            val cellSize = frame.viewport.cellSize.toFloat()
            val centerX = rect.x + rect.width * 0.5f
            val centerY = rect.y + rect.height * 0.5f
            canvas.drawRect(
                tileBounds(centerX - cellSize * 1.85f, centerY - cellSize * 0.95f, cellSize * 3.70f, cellSize * 1.90f),
                plateColor("8A5A2E", 0.062f),
            )
            canvas.drawRect(
                tileBounds(centerX - cellSize * 1.10f, centerY - cellSize * 0.55f, cellSize * 2.20f, cellSize * 1.05f),
                plateColor("C3873D", 0.054f),
            )
            canvas.drawRect(
                tileBounds(centerX - cellSize * 0.80f, centerY + cellSize * 0.36f, cellSize * 1.60f, 2.5f),
                plateColor("E0B46A", 0.046f),
            )
        }
    }

    private fun topologyRiskLightAnchors(
        shape: RoomArtPlateTopologyShape,
        visiblePoints: Set<Point>,
    ): List<TopologyRiskLightAnchor> {
        val centerX2 = shape.bounds.minX * 2 + shape.bounds.width - 1
        val centerY2 = shape.bounds.minY * 2 + shape.bounds.height - 1
        return visiblePoints
            .groupBy(Point::y)
            .flatMap { (rowY, rowPoints) ->
                rowPoints
                    .map(Point::x)
                    .sorted()
                    .toTopologyRuns()
                    .filter { run -> run.length >= 4 }
                    .map { run ->
                        TopologyRiskLightAnchor(
                            point = Point(run.start + run.length / 2, rowY),
                            runLength = run.length,
                        )
                    }
            }.sortedWith(
                compareBy<TopologyRiskLightAnchor> { anchor ->
                    abs(anchor.point.x * 2 - centerX2) + abs(anchor.point.y * 2 - centerY2)
                }.thenByDescending { anchor -> anchor.runLength }
                    .thenBy { anchor -> anchor.point.y }
                    .thenBy { anchor -> anchor.point.x },
            ).take(2)
    }

    private fun drawTopologyRiskBoundaryMarks(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        visiblePoints: Set<Point>,
    ) {
        val darkEdge = plateColor("050604", 0.255f)
        val warmLip = plateColor("A8905E", 0.090f)
        visiblePoints.forEach { point ->
            val rect = frame.viewport.tileRect(point)
            val x = rect.x.toFloat()
            val y = rect.y.toFloat()
            val size = rect.width.toFloat()
            val northOpen = Point(point.x, point.y + 1) !in visiblePoints
            val southOpen = Point(point.x, point.y - 1) !in visiblePoints
            val westOpen = Point(point.x - 1, point.y) !in visiblePoints
            val eastOpen = Point(point.x + 1, point.y) !in visiblePoints
            if (northOpen) {
                canvas.drawRect(tileBounds(x + 2f, y + size - 8f, size - 4f, 8f), darkEdge)
                canvas.drawRect(tileBounds(x + 7f, y + size - 3f, size - 14f, 2f), warmLip)
            }
            if (southOpen) {
                canvas.drawRect(tileBounds(x + 2f, y, size - 4f, 7f), darkEdge)
                canvas.drawRect(tileBounds(x + 7f, y + 5f, size - 14f, 2f), warmLip)
            }
            if (westOpen) {
                canvas.drawRect(tileBounds(x, y + 2f, 7f, size - 4f), darkEdge)
                canvas.drawRect(tileBounds(x + 5f, y + 7f, 2f, size - 14f), warmLip)
            }
            if (eastOpen) {
                canvas.drawRect(tileBounds(x + size - 8f, y + 2f, 8f, size - 4f), darkEdge)
                canvas.drawRect(tileBounds(x + size - 7f, y + 7f, 2f, size - 14f), warmLip)
            }
            if ((northOpen || southOpen) && (westOpen || eastOpen)) {
                drawTopologyRiskCornerMass(canvas, x, y, size, northOpen, southOpen, westOpen, eastOpen)
            }
        }
    }

    private fun drawTopologyRiskWallComponents(
        canvas: TileCanvas,
        frame: MapRenderFrame,
        artPlate: RoomArtPlateModel,
        visiblePoints: Set<Point>,
    ) {
        visiblePoints.forEach { point ->
            topologyRiskWallComponentPlacements(point, visiblePoints)
                .filter { placement -> placement.shouldDrawAnchor(point) }
                .forEach { placement ->
                    val asset = artPlate.source.componentAssets.assetFor(placement.role) ?: return@forEach
                    canvas.drawAsset(
                        asset,
                        topologyRiskComponentBounds(frame, point),
                        placement.role.componentAlpha,
                        flipX = placement.flipX,
                        flipY = placement.flipY,
                    )
                }
        }
    }

    private fun TopologyRiskWallComponentPlacement.shouldDrawAnchor(point: Point): Boolean =
        when (role) {
            TerrainWallPieceRole.CORNER,
            TerrainWallPieceRole.DOOR_CONTACT,
            -> true

            TerrainWallPieceRole.CROWN,
            TerrainWallPieceRole.SIDE,
            TerrainWallPieceRole.BASE,
            -> (point.x * 31 + point.y * 17).mod(6) == 0
        }

    private fun topologyRiskWallComponentPlacements(
        point: Point,
        visiblePoints: Set<Point>,
    ): List<TopologyRiskWallComponentPlacement> {
        val northOpen = Point(point.x, point.y + 1) !in visiblePoints
        val southOpen = Point(point.x, point.y - 1) !in visiblePoints
        val westOpen = Point(point.x - 1, point.y) !in visiblePoints
        val eastOpen = Point(point.x + 1, point.y) !in visiblePoints
        topologyRiskCornerPlacement(northOpen, southOpen, westOpen, eastOpen)?.let { placement ->
            return listOf(placement)
        }

        val placements = mutableListOf<TopologyRiskWallComponentPlacement>()
        if (northOpen) {
            placements += topologyRiskHorizontalPlacement(point, visiblePoints, north = true)
        }
        if (southOpen) {
            placements += topologyRiskHorizontalPlacement(point, visiblePoints, north = false)
        }
        if (westOpen) {
            placements += topologyRiskVerticalPlacement(point, visiblePoints, west = true)
        }
        if (eastOpen) {
            placements += topologyRiskVerticalPlacement(point, visiblePoints, west = false)
        }
        return placements
    }

    private fun topologyRiskCornerPlacement(
        northOpen: Boolean,
        southOpen: Boolean,
        westOpen: Boolean,
        eastOpen: Boolean,
    ): TopologyRiskWallComponentPlacement? =
        if ((northOpen || southOpen) && (westOpen || eastOpen)) {
            TopologyRiskWallComponentPlacement(
                role = TerrainWallPieceRole.CORNER,
                flipX = eastOpen,
                flipY = northOpen,
            )
        } else {
            null
        }

    private fun topologyRiskHorizontalPlacement(
        point: Point,
        visiblePoints: Set<Point>,
        north: Boolean,
    ): TopologyRiskWallComponentPlacement {
        val openPoint =
            if (north) {
                Point(point.x, point.y + 1)
            } else {
                Point(point.x, point.y - 1)
            }
        val hasLeftContact = Point(openPoint.x - 1, openPoint.y) in visiblePoints
        val hasRightContact = Point(openPoint.x + 1, openPoint.y) in visiblePoints
        return if (hasLeftContact || hasRightContact) {
            TopologyRiskWallComponentPlacement(
                role = TerrainWallPieceRole.DOOR_CONTACT,
                flipX = hasRightContact,
                flipY = !north,
            )
        } else {
            TopologyRiskWallComponentPlacement(
                role = TerrainWallPieceRole.CROWN,
                flipY = !north,
            )
        }
    }

    private fun topologyRiskVerticalPlacement(
        point: Point,
        visiblePoints: Set<Point>,
        west: Boolean,
    ): TopologyRiskWallComponentPlacement {
        val openPoint =
            if (west) {
                Point(point.x - 1, point.y)
            } else {
                Point(point.x + 1, point.y)
            }
        val hasNorthContact = Point(openPoint.x, openPoint.y + 1) in visiblePoints
        val hasSouthContact = Point(openPoint.x, openPoint.y - 1) in visiblePoints
        return if (hasNorthContact || hasSouthContact) {
            TopologyRiskWallComponentPlacement(
                role = TerrainWallPieceRole.DOOR_CONTACT,
                flipX = !west,
                flipY = hasNorthContact,
            )
        } else {
            TopologyRiskWallComponentPlacement(
                role = TerrainWallPieceRole.SIDE,
                flipX = !west,
            )
        }
    }

    private fun topologyRiskComponentBounds(
        frame: MapRenderFrame,
        point: Point,
    ): TileFloatBounds {
        val rect = frame.viewport.tileRect(point)
        val inset = 1.5f
        return tileBounds(
            rect.x + inset,
            rect.y + inset,
            rect.width - inset * 2f,
            rect.height - inset * 2f,
        )
    }

    private data class TopologyRiskWallComponentPlacement(
        val role: TerrainWallPieceRole,
        val flipX: Boolean = false,
        val flipY: Boolean = false,
    )

    private data class TopologyRiskLightAnchor(
        val point: Point,
        val runLength: Int,
    )

    private data class TopologyRiskMantlePalette(
        val fieldHex: String,
        val fieldAlpha: Float,
        val upperShadowHex: String,
        val upperShadowAlpha: Float,
        val lowerLipHex: String,
        val lowerLipAlpha: Float,
    )

    private fun topologyRiskMantlePalette(tilesetKey: String): TopologyRiskMantlePalette =
        when (tilesetKey) {
            DarkUiMapVisualKeys.FOREST_EDGE_TILESET ->
                TopologyRiskMantlePalette(
                    fieldHex = "111B14",
                    fieldAlpha = 0.156f,
                    upperShadowHex = "050604",
                    upperShadowAlpha = 0.108f,
                    lowerLipHex = "8E7540",
                    lowerLipAlpha = 0.046f,
                )

            DarkUiMapVisualKeys.MINE_TILESET ->
                TopologyRiskMantlePalette(
                    fieldHex = "18130E",
                    fieldAlpha = 0.148f,
                    upperShadowHex = "050504",
                    upperShadowAlpha = 0.112f,
                    lowerLipHex = "A06F3D",
                    lowerLipAlpha = 0.048f,
                )

            DarkUiMapVisualKeys.SHADOW_DEPTHS_TILESET ->
                TopologyRiskMantlePalette(
                    fieldHex = "0D0F1A",
                    fieldAlpha = 0.164f,
                    upperShadowHex = "050508",
                    upperShadowAlpha = 0.118f,
                    lowerLipHex = "6D64A6",
                    lowerLipAlpha = 0.052f,
                )

            else ->
                TopologyRiskMantlePalette(
                    fieldHex = "11130E",
                    fieldAlpha = 0.150f,
                    upperShadowHex = "050604",
                    upperShadowAlpha = 0.110f,
                    lowerLipHex = "91764A",
                    lowerLipAlpha = 0.046f,
                )
        }

    private val TerrainWallPieceRole.componentAlpha: Float
        get() =
            when (this) {
                TerrainWallPieceRole.BASE -> 0.26f
                TerrainWallPieceRole.CROWN -> 0.36f
                TerrainWallPieceRole.SIDE -> 0.34f
                TerrainWallPieceRole.CORNER -> 0.42f
                TerrainWallPieceRole.DOOR_CONTACT -> 0.46f
            }

    private fun drawTopologyRiskCornerMass(
        canvas: TileCanvas,
        x: Float,
        y: Float,
        size: Float,
        northOpen: Boolean,
        southOpen: Boolean,
        westOpen: Boolean,
        eastOpen: Boolean,
    ) {
        val corner = plateColor("050604", 0.305f)
        if (northOpen && westOpen) {
            canvas.drawRect(tileBounds(x + 1f, y + size - 15f, 15f, 14f), corner)
        }
        if (northOpen && eastOpen) {
            canvas.drawRect(tileBounds(x + size - 16f, y + size - 15f, 15f, 14f), corner)
        }
        if (southOpen && westOpen) {
            canvas.drawRect(tileBounds(x + 1f, y + 1f, 15f, 14f), corner)
        }
        if (southOpen && eastOpen) {
            canvas.drawRect(tileBounds(x + size - 16f, y + 1f, 15f, 14f), corner)
        }
    }

    private data class TopologyRun(
        val start: Int,
        val length: Int,
    )

    private data class TopologyBand(
        val startX: Int,
        val startY: Int,
        val width: Int,
        val height: Int,
    )

    private fun TopologyBand.toSourceRegion(bounds: RoomArtPlateTopologyBounds): TileAssetSourceRegion =
        TileAssetSourceRegion(
            leftRatio = (startX - bounds.minX).toFloat() / bounds.width,
            bottomRatio = (startY - bounds.minY).toFloat() / bounds.height,
            widthRatio = width.toFloat() / bounds.width,
            heightRatio = height.toFloat() / bounds.height,
        )

    private fun TopologyRun.countOpenNeighbor(
        rowY: Int,
        visiblePoints: Set<Point>,
        verticalOffset: Int,
    ): Int =
        (start until start + length).count { x ->
            Point(x, rowY + verticalOffset) !in visiblePoints
        }

    private fun List<Int>.toTopologyRuns(): List<TopologyRun> {
        if (isEmpty()) {
            return emptyList()
        }
        val runs = mutableListOf<TopologyRun>()
        var start = first()
        var previous = first()
        drop(1).forEach { value ->
            if (value == previous + 1) {
                previous = value
            } else {
                runs += TopologyRun(start, previous - start + 1)
                start = value
                previous = value
            }
        }
        runs += TopologyRun(start, previous - start + 1)
        return runs
    }

    private fun Set<Point>.toTopologyBands(): List<TopologyBand> {
        val rowRuns =
            groupBy(Point::y)
                .mapValues { (_, rowPoints) ->
                    rowPoints
                        .map(Point::x)
                        .sorted()
                        .toTopologyRuns()
                }
                .toSortedMap()
        val bands = mutableListOf<TopologyBand>()
        val openBands = linkedMapOf<TopologyRun, TopologyBand>()
        var previousRowY: Int? = null
        rowRuns.forEach { (rowY, runs) ->
            if (previousRowY != null && rowY > previousRowY + 1) {
                bands += openBands.values
                openBands.clear()
            }
            val rowRunSet = runs.toSet()
            openBands
                .keys
                .filterNot(rowRunSet::contains)
                .toList()
                .forEach { closedRun ->
                    bands += requireNotNull(openBands.remove(closedRun))
                }
            runs.forEach { run ->
                val existing = openBands[run]
                openBands[run] =
                    if (existing == null) {
                        TopologyBand(startX = run.start, startY = rowY, width = run.length, height = 1)
                    } else {
                        existing.copy(height = rowY - existing.startY + 1)
                    }
            }
            previousRowY = rowY
        }
        bands += openBands.values
        return bands
    }

    private fun roomBounds(frame: MapRenderFrame): TileFloatBounds? {
        val viewport = frame.viewport
        val rects =
            frame.model.mapCellMaterials
                .asSequence()
                .filter { material ->
                    material.visibility == CellVisibilitySnapshot.VISIBLE &&
                        viewport.containsTile(Point(material.x, material.y))
                }.map { material -> viewport.tileRect(Point(material.x, material.y)) }
                .toList()
        if (rects.isEmpty()) {
            return null
        }
        val left = rects.minOf { rect -> rect.x }.toFloat()
        val right = rects.maxOf { rect -> rect.x + rect.width }.toFloat()
        val bottom = rects.minOf { rect -> rect.y }.toFloat()
        val top = rects.maxOf { rect -> rect.y + rect.height }.toFloat()
        return tileBounds(left, bottom, right - left, top - bottom)
    }

    private fun plateColor(
        hex: String,
        alpha: Float,
    ): Color = Color.valueOf(hex).also { color -> color.a = alpha }
}
