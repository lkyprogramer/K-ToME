package com.ktome.game.mapgen

import com.ktome.core.mapgen.BiomeFamilyDef
import com.ktome.core.mapgen.MapgenContentCatalog
import com.ktome.core.mapgen.TerrainTag
import com.ktome.game.data.schema.SchemaCatalog

object SchemaMapgenContentCatalogFactory {
    fun from(schemaCatalog: SchemaCatalog): MapgenContentCatalog {
        val fallbackBiomeFamilies =
            schemaCatalog.zones
                .asSequence()
                .filter { zone -> zone.mapgenProfileId == null }
                .map { zone -> fallbackBiomeFamily(zone.biome) }
                .distinctBy(BiomeFamilyDef::id)
                .toList()
        return MapgenContentCatalog(
            roomDefs = schemaCatalog.roomDefs,
            patternRooms = schemaCatalog.patternRooms,
            patternTemplates = schemaCatalog.patternTemplates.associateBy { template -> template.id },
            vaultDefs = schemaCatalog.vaults,
            vaultTemplates = schemaCatalog.vaultTemplates.associateBy { template -> template.id },
            biomeFamilies = (schemaCatalog.biomeFamilies + fallbackBiomeFamilies).distinctBy(BiomeFamilyDef::id),
        )
    }

    private fun fallbackBiomeFamily(biome: String): BiomeFamilyDef {
        val normalizedId =
            when (biome) {
                "ruins" -> "ruin"
                else -> biome
            }
        val terrainWeights =
            when (normalizedId) {
                "mine" -> mapOf(TerrainTag.OIL to 0.10f)
                "cavern" -> mapOf(TerrainTag.WATER to 0.10f)
                else -> emptyMap()
            }
        val primaryTileSet =
            when (normalizedId) {
                "forest" -> "tileset.forest_edge"
                "mine" -> "tileset.mine"
                "ruin" -> "tileset.ruins"
                else -> "tileset.shadow_depths"
            }
        val secondaryTileSet =
            when (normalizedId) {
                "forest", "mine", "ruin" -> null
                else -> "tileset.ruins"
            }
        val allowedRoomTags =
            when (normalizedId) {
                "forest" -> setOf("sightline", "ambush", "hidden_cache")
                "ruin" -> setOf("shrine", "ritual", "hidden_cache")
                "mine" -> setOf("forge", "sightline", "hidden_cache")
                "depths" -> setOf("ritual", "ambush", "shrine")
                "cavern" -> setOf("bridge", "ambush", "hidden_cache")
                "temple" -> setOf("ritual", "shrine", "hidden_cache")
                else -> setOf("ambush", "hidden_cache")
        }
        return BiomeFamilyDef(
            id = "fallback.$normalizedId",
            primaryTileSet = primaryTileSet,
            secondaryTileSet = secondaryTileSet,
            terrainTagWeights = terrainWeights,
            allowedRoomTags = allowedRoomTags,
        )
    }
}
