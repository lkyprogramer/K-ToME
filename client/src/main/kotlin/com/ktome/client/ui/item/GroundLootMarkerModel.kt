package com.ktome.client.ui.item

import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.MapCellSnapshot

internal enum class GroundLootMarkerPlacement {
    CELL_CENTER,
    ACTOR_CORNER,
}

internal data class GroundLootMarkerModel(
    val x: Int,
    val y: Int,
    val iconKey: String,
    val itemCount: Int,
    val countBadge: String?,
    val qualityPresentation: QualityPresentation,
    val placement: GroundLootMarkerPlacement,
) {
    companion object {
        fun fromCell(
            cell: MapCellSnapshot,
            isActorOccupied: Boolean,
        ): GroundLootMarkerModel? {
            if (cell.items.isEmpty()) {
                return null
            }
            val head = selectHeadItem(cell.items)
            val iconKey =
                requireNotNull(head.iconKey?.takeIf(String::isNotBlank)) {
                    "Ground loot marker requires item iconKey for ${head.baseItemId}."
                }
            val itemCount = cell.items.size
            return GroundLootMarkerModel(
                x = cell.x,
                y = cell.y,
                iconKey = iconKey,
                itemCount = itemCount,
                countBadge = countBadge(itemCount),
                qualityPresentation = QualityPresentation.from(head),
                placement = if (isActorOccupied) GroundLootMarkerPlacement.ACTOR_CORNER else GroundLootMarkerPlacement.CELL_CENTER,
            )
        }

        private fun selectHeadItem(items: List<ItemRenderSnapshot>): ItemRenderSnapshot {
            var best = items.first()
            for (index in 1 until items.size) {
                val candidate = items[index]
                if (isBetterHead(candidate, best)) {
                    best = candidate
                }
            }
            return best
        }

        private fun isBetterHead(
            candidate: ItemRenderSnapshot,
            current: ItemRenderSnapshot,
        ): Boolean {
            val candidateSpecial = candidate.specialTemplateId != null
            val currentSpecial = current.specialTemplateId != null
            if (candidateSpecial != currentSpecial) {
                return candidateSpecial
            }

            val candidateQuality = QualityPresentation.from(candidate).rarityRank
            val currentQuality = QualityPresentation.from(current).rarityRank
            if (candidateQuality != currentQuality) {
                return candidateQuality > currentQuality
            }

            val candidateIcon = candidate.iconKey.orEmpty()
            val currentIcon = current.iconKey.orEmpty()
            return candidateIcon < currentIcon
        }

        private fun countBadge(itemCount: Int): String? =
            when {
                itemCount <= 1 -> null
                itemCount >= 10 -> "9+"
                else -> itemCount.toString()
            }
    }
}
