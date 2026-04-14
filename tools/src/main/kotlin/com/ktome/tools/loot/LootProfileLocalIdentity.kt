package com.ktome.tools.loot

import com.ktome.game.data.schema.LootProfileSchemaV3

internal data class LootProfileLocalIdentityMetadata(
    val zoneId: String?,
    val category: String,
)

internal fun LootProfileSchemaV3.localIdentityMetadata(): LootProfileLocalIdentityMetadata {
    val category =
        when {
            "secret" in tags -> "secret"
            "cadence" in tags -> "cadence"
            "reward" in tags -> "reward"
            else -> "other"
        }
    val zoneId =
        itemTagFilter.firstOrNull()
            ?.takeIf(String::isNotBlank)
            ?: canonicalZoneIdFromProfileId(id)
    require(category != "secret" || zoneId != null) {
        "Secret loot profile '$id' must resolve a canonical zone id for same-zone local identity metrics."
    }
    return LootProfileLocalIdentityMetadata(
        zoneId = zoneId,
        category = category,
    )
}

private val CANONICAL_ZONE_ID_PREFIXES: Map<String, List<String>> =
    linkedMapOf(
        "greenwood_fringe" to listOf("loot.greenwood_fringe.", "loot.greenwood_hidden_cache."),
        "deep_iron_pit" to listOf("loot.deep_iron_pit.", "loot.deep_iron_slag_cache.", "loot.deep_iron_smuggler_stash."),
        "underground_river" to listOf("loot.underground_river.", "loot.underground_river_crystal_rift."),
        "crystal_cavern" to listOf("loot.crystal_cavern."),
        "abyssal_temple" to listOf("loot.abyssal_temple.", "loot.abyssal_temple_warded_archive."),
        "grey_gate_depths" to listOf("loot.grey_gate_depths.", "loot.grey_gate."),
        "molten_core" to listOf("loot.molten_core."),
        "bandit_camp" to listOf("loot.bandit_camp."),
        "elven_ruins" to listOf("loot.elven_ruins."),
        "shattered_outpost" to listOf("loot.shattered_outpost."),
    )

internal fun canonicalZoneIdFromProfileId(profileId: String): String? =
    CANONICAL_ZONE_ID_PREFIXES.entries
        .firstOrNull { (_, prefixes) -> prefixes.any(profileId::startsWith) }
        ?.key

internal fun normalizeLootTag(tag: String): String = tag.trim().lowercase()
