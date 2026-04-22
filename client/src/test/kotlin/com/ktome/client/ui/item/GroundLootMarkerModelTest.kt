package com.ktome.client.ui.item

import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GroundLootMarkerModelTest {
    @Test
    fun `single item marker uses the item icon and no count badge`() {
        val marker =
            requireNotNull(
                GroundLootMarkerModel.fromCell(
                    cell = cell(items = listOf(item(id = "short_sword", iconKey = "item.short_sword.icon"))),
                    isActorOccupied = false,
                ),
            )

        assertEquals("item.short_sword.icon", marker.iconKey)
        assertEquals(1, marker.itemCount)
        assertEquals(null, marker.countBadge)
        assertEquals(GroundLootMarkerPlacement.CELL_CENTER, marker.placement)
    }

    @Test
    fun `head item sorting prefers special then rarity then icon key`() {
        val marker =
            requireNotNull(
                GroundLootMarkerModel.fromCell(
                    cell =
                        cell(
                            items =
                                listOf(
                                    item(id = "rare_sword", iconKey = "z.icon", qualityTierId = "RARE"),
                                    item(id = "magic_sword", iconKey = "a.icon", qualityTierId = "MAGIC"),
                                    item(
                                        id = "unique_sword",
                                        iconKey = "m.icon",
                                        qualityTierId = "MAGIC",
                                        specialTemplateId = "unique.test",
                                        specialTierId = "UNIQUE",
                                    ),
                                ),
                        ),
                    isActorOccupied = true,
                ),
            )

        assertEquals("m.icon", marker.iconKey)
        assertEquals("3", marker.countBadge)
        assertEquals(GroundLootMarkerPlacement.ACTOR_CORNER, marker.placement)
        assertEquals(SpecialAccentTokenId.UNIQUE, marker.qualityPresentation.specialAccentTokenId)
    }

    @Test
    fun `count badge caps at nine plus`() {
        val marker =
            requireNotNull(
                GroundLootMarkerModel.fromCell(
                    cell = cell(items = (1..10).map { index -> item(id = "item_$index", iconKey = "item.$index.icon") }),
                    isActorOccupied = false,
                ),
            )

        assertEquals("9+", marker.countBadge)
    }

    @Test
    fun `missing icon or unknown quality fails fast`() {
        assertThrows(IllegalArgumentException::class.java) {
            GroundLootMarkerModel.fromCell(cell(items = listOf(item(id = "missing", iconKey = null))), isActorOccupied = false)
        }
        assertThrows(IllegalStateException::class.java) {
            GroundLootMarkerModel.fromCell(cell(items = listOf(item(id = "bad", iconKey = "bad.icon", qualityTierId = "LEGENDARY"))), isActorOccupied = false)
        }
    }

    private fun cell(items: List<ItemRenderSnapshot>): MapCellSnapshot =
        MapCellSnapshot(
            x = 1,
            y = 2,
            visibility = CellVisibilitySnapshot.VISIBLE,
            terrainTypeId = "floor",
            terrainVisualKey = "tileset.test.ground",
            items = items,
        )

    private fun item(
        id: String,
        iconKey: String?,
        qualityTierId: String = "NORMAL",
        specialTemplateId: String? = null,
        specialTierId: String? = null,
    ): ItemRenderSnapshot =
        ItemRenderSnapshot(
            baseItemId = id,
            specialTemplateId = specialTemplateId,
            specialTierId = specialTierId,
            nameKey = "item.$id.name",
            typeId = "WEAPON",
            iconKey = iconKey,
            qualityTierId = qualityTierId,
        )
}
