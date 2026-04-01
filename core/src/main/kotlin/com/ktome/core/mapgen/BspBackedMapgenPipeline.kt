package com.ktome.core.mapgen

import com.ktome.core.map.BspConfig
import com.ktome.core.map.BspGenerator

class BspBackedMapgenPipeline(
    private val profileResolver: ZoneMapgenProfileResolver,
) : MapgenPipeline {
    override fun run(request: MapgenRequest): GeneratedFloor {
        val profile = profileResolver.resolve(request.zoneId)
        val map =
            BspGenerator(
                seed = request.seed,
                config = BspConfig(width = request.targetWidth, height = request.targetHeight),
            ).generate()
        return GeneratedFloor(
            zoneId = request.zoneId,
            floorIndex = request.floorIndex,
            seed = request.seed,
            topology = LinearTopologyProjector.project(map),
            terrainTags = TerrainTagPainter.paint(map = map, profile = profile, seed = request.seed),
            entrances = emptyList(),
            map = map,
        )
    }
}
