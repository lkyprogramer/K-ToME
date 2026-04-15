package com.ktome.tools.loot

import com.ktome.game.data.schema.LootProfileLocalIdentityCategory
import com.ktome.game.data.schema.LootProfileSchemaV3
import com.ktome.game.data.schema.localIdentityCategory

internal data class LootProfileLocalIdentityMetadata(
    val canonicalZoneId: String?,
    val category: LootProfileLocalIdentityCategory,
)

internal fun LootProfileSchemaV3.localIdentityMetadata(): LootProfileLocalIdentityMetadata {
    val category = localIdentityCategory()
    require(!category.requiresCanonicalZoneId || canonicalZoneId != null) {
        "Loot profile '$id' tagged as ${category.token} must declare canonicalZoneId for same-zone local identity metrics."
    }
    return LootProfileLocalIdentityMetadata(
        canonicalZoneId = canonicalZoneId,
        category = category,
    )
}

internal fun normalizeLootTag(tag: String): String = tag.trim().lowercase()
