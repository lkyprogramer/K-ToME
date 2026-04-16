package com.ktome.game.mapgen

import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.mapgen.ZoneMapgenProfile
import com.ktome.core.mapgen.ZoneMapgenProfileResolver
import com.ktome.game.data.schema.ZoneSchemaV2

class SchemaZoneMapgenProfileResolver(
    zones: Collection<ZoneSchemaV2>,
    profiles: Collection<ZoneMapgenProfile>,
) : ZoneMapgenProfileResolver {
    private val zoneById: Map<String, ZoneSchemaV2> = zones.associateBy(ZoneSchemaV2::id)
    private val profileById: Map<String, ZoneMapgenProfile> = profiles.associateBy(ZoneMapgenProfile::id)

    override fun resolve(
        zoneId: String,
        floorIndex: Int,
    ): ZoneMapgenProfile {
        val zone = requireNotNull(zoneById[zoneId]) { "Unknown zone '$zoneId' for ZoneMapgenProfileResolver." }
        val explicitProfileId = zone.resolvedMapgenProfileId(floorIndex)
        if (explicitProfileId != null) {
            val profile = requireNotNull(profileById[explicitProfileId]) {
                "Zone '${zone.id}' references unknown mapgen profile '$explicitProfileId'."
            }
            require(profile.zoneId == zone.id) {
                "Mapgen profile '${profile.id}' belongs to '${profile.zoneId}', but zone '${zone.id}' referenced it."
            }
            return profile
        }
        return ZoneMapgenProfile(
            id = "${zone.id}.fallback",
            zoneId = zone.id,
            allowedBiomeFamilies = setOf("fallback.${normalizeFamilyId(zone.biome)}"),
            loopCountRange = if (zone.worldRole == "optional") 0..1 else 0..0,
            vaultPool = emptySet(),
            terrainTagWeights = deriveTerrainTagWeights(zone),
            roomTagFilter = emptySet(),
        )
    }

    private fun normalizeFamilyId(biome: String): String =
        when (biome) {
            "ruins" -> "ruin"
            else -> biome
        }

    private fun deriveTerrainTagWeights(zone: ZoneSchemaV2): Map<TerrainTag, Float> =
        linkedMapOf<TerrainTag, Float>().apply {
            val mechanics = zone.specialMechanics.joinToString(separator = "|")
            if (zone.biome == "cavern" || mechanics.contains("river") || mechanics.contains("current") || mechanics.contains("ferry")) {
                put(TerrainTag.WATER, 0.10f)
            }
            if (zone.biome == "mine" || mechanics.contains("forge") || mechanics.contains("slag") || mechanics.contains("oil")) {
                put(TerrainTag.OIL, 0.12f)
            }
            if (mechanics.contains("crystal") || mechanics.contains("frozen") || mechanics.contains("ice") || mechanics.contains("resonance")) {
                put(TerrainTag.ICE, 0.08f)
            }
        }
}
