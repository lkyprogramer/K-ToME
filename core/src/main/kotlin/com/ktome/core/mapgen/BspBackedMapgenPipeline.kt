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
        val topology = LinearTopologyProjector.project(map)
        val rooms = CompatibilityRoomProjector.project(map = map, topology = topology)
        val terrainTags =
            TerrainTagPainter.paint(
                map = map,
                profile = profile,
                seed = request.seed,
                rooms = rooms,
                biomeFamilies = emptyMap(),
                seededTags = emptyMap(),
            )
        return GeneratedFloor.compatibility(
            zoneId = request.zoneId,
            floorIndex = request.floorIndex,
            seed = request.seed,
            map = map,
            terrainTags = terrainTags,
            topology = topology,
            biomeFamilyIds = profile.allowedBiomeFamilies.sorted().take(2),
        )
    }
}
