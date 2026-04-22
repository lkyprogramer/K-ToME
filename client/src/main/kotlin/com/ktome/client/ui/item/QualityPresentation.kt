package com.ktome.client.ui.item

import com.ktome.core.loot.RarityTier
import com.ktome.core.loot.SpecialTier
import com.ktome.core.snapshot.ItemRenderSnapshot

internal enum class QualityColorTokenId {
    NORMAL,
    MAGIC,
    RARE,
}

internal enum class SpecialAccentTokenId {
    UNIQUE,
    ARTIFACT,
}

internal data class QualityPresentation(
    val rarityTierId: String,
    val rarityRank: Int,
    val colorTokenId: QualityColorTokenId,
    val cornerGlyph: String?,
    val specialAccentTokenId: SpecialAccentTokenId?,
) {
    companion object {
        fun from(item: ItemRenderSnapshot): QualityPresentation {
            val rarity = RarityPresentation.from(item.qualityTierId)
            val specialAccent = specialAccentFor(item)
            return QualityPresentation(
                rarityTierId = rarity.id,
                rarityRank = rarity.rank,
                colorTokenId = rarity.colorTokenId,
                cornerGlyph = rarity.cornerGlyph,
                specialAccentTokenId = specialAccent,
            )
        }

        private fun specialAccentFor(item: ItemRenderSnapshot): SpecialAccentTokenId? {
            val templateId = item.specialTemplateId
            val specialTierId = item.specialTierId
            require((templateId == null) == (specialTierId == null)) {
                "Special item presentation requires specialTemplateId and specialTierId to be present together for ${item.baseItemId}."
            }
            return when (specialTierId) {
                null -> null
                else ->
                    when (val specialTier = parseSpecialTier(specialTierId, item.baseItemId)) {
                        SpecialTier.UNIQUE -> SpecialAccentTokenId.UNIQUE
                        SpecialTier.ARTIFACT -> SpecialAccentTokenId.ARTIFACT
                    }
            }
        }

        private fun parseSpecialTier(
            specialTierId: String,
            baseItemId: String,
        ): SpecialTier =
            runCatching { SpecialTier.valueOf(specialTierId) }
                .getOrElse { error("Unsupported specialTierId '$specialTierId' for $baseItemId.") }
    }
}

private data class RarityPresentation(
    val id: String,
    val rank: Int,
    val colorTokenId: QualityColorTokenId,
    val cornerGlyph: String?,
) {
    companion object {
        fun from(rawTierId: String): RarityPresentation {
            val tier =
                runCatching { RarityTier.valueOf(rawTierId.uppercase()) }
                    .getOrElse { error("Unsupported qualityTierId '$rawTierId'.") }
            return when (tier) {
                RarityTier.NORMAL ->
                    RarityPresentation(
                        id = tier.name,
                        rank = 0,
                        colorTokenId = QualityColorTokenId.NORMAL,
                        cornerGlyph = null,
                    )

                RarityTier.MAGIC ->
                    RarityPresentation(
                        id = tier.name,
                        rank = 1,
                        colorTokenId = QualityColorTokenId.MAGIC,
                        cornerGlyph = "\u25C6",
                    )

                RarityTier.RARE ->
                    RarityPresentation(
                        id = tier.name,
                        rank = 2,
                        colorTokenId = QualityColorTokenId.RARE,
                        cornerGlyph = "\u25C6\u25C6",
                    )
            }
        }
    }
}
