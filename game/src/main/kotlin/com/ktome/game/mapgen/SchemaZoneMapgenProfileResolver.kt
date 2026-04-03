package com.ktome.game.mapgen

import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.mapgen.ZoneMapgenProfile
import com.ktome.core.mapgen.ZoneMapgenProfileResolver
import com.ktome.game.data.schema.ZoneSchemaV2

class SchemaZoneMapgenProfileResolver(
    zones: Collection<ZoneSchemaV2>,
) : ZoneMapgenProfileResolver {
    private val zoneById: Map<String, ZoneSchemaV2> = zones.associateBy(ZoneSchemaV2::id)

    override fun resolve(zoneId: String): ZoneMapgenProfile {
        val zone = requireNotNull(zoneById[zoneId]) { "Unknown zone '$zoneId' for ZoneMapgenProfileResolver." }
        return ZoneMapgenProfile(
            zoneId = zone.id,
            allowedBiomeFamilies = setOf("family.${zone.biome}"),
            loopCountRange = if (zone.worldRole == "optional") 0..1 else 0..0,
            vaultPool = emptySet(),
            terrainTagWeights = deriveTerrainTagWeights(zone),
            roomTagFilter = buildSet {
                add(zone.biome)
                add(zone.worldRole)
                addAll(zone.specialMechanics)
            },
        )
    }

    private fun deriveTerrainTagWeights(zone: ZoneSchemaV2): Map<TerrainTag, Float> =
        linkedMapOf<TerrainTag, Float>().apply {
            val mechanics = zone.specialMechanics.joinToString(separator = "|")
            if (zone.biome == "cavern" || mechanics.contains("river") || mechanics.contains("current") || mechanics.contains("ferry")) {
                put(TerrainTag.WATER, 1.0f)
            }
            if (zone.biome == "mine" || mechanics.contains("forge") || mechanics.contains("slag") || mechanics.contains("oil")) {
                put(TerrainTag.OIL, 0.8f)
            }
            if (mechanics.contains("crystal") || mechanics.contains("frozen") || mechanics.contains("ice") || mechanics.contains("resonance")) {
                put(TerrainTag.ICE, 0.6f)
            }
        }
}
