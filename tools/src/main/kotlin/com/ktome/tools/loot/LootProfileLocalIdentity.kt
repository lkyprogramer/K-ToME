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

internal fun canonicalZoneIdFromProfileId(profileId: String): String? =
    when {
        "greenwood_fringe" in profileId -> "greenwood_fringe"
        "deep_iron_pit" in profileId || "deep_iron_" in profileId -> "deep_iron_pit"
        "underground_river" in profileId -> "underground_river"
        "crystal_cavern" in profileId -> "crystal_cavern"
        "abyssal_temple" in profileId -> "abyssal_temple"
        "grey_gate_depths" in profileId || "grey_gate" in profileId -> "grey_gate_depths"
        "molten_core" in profileId -> "molten_core"
        "bandit_camp" in profileId -> "bandit_camp"
        "elven_ruins" in profileId -> "elven_ruins"
        "shattered_outpost" in profileId -> "shattered_outpost"
        else -> null
    }

internal fun normalizeLootTag(tag: String): String = tag.trim().lowercase()
