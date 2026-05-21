package com.ktome.client.render

import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.core.snapshot.EquipmentSlotSnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.game.i18n.Localizer

internal enum class EquipmentInventoryOverflowPolicy {
    NONE,
    PAGED,
}

internal const val EQUIPMENT_INVENTORY_COMPANION_COLUMNS: Int = 4
internal const val EQUIPMENT_INVENTORY_COMPANION_VISIBLE_ROWS: Int = 2
internal const val EQUIPMENT_INVENTORY_COMPANION_PAGE_SIZE: Int =
    EQUIPMENT_INVENTORY_COMPANION_COLUMNS * EQUIPMENT_INVENTORY_COMPANION_VISIBLE_ROWS

internal data class EquipmentSlotCellModel(
    val slotId: String?,
    val labelKey: String?,
    val label: String,
    val frame: ResolvedVisualAsset,
    val itemIcon: ResolvedVisualAsset?,
    val qualityTierId: String?,
    val selected: Boolean,
    val tooltipAnchorId: String?,
    val visualOnly: Boolean = false,
)

internal data class InventoryGridCellModel(
    val identityIndex: Int?,
    val frame: ResolvedVisualAsset,
    val itemIcon: ResolvedVisualAsset?,
    val quantityText: String?,
    val qualityTierId: String?,
    val selected: Boolean,
    val tooltipAnchorId: String?,
)

internal data class InventoryGridModel(
    val columns: Int,
    val visibleRows: Int,
    val cellSizePx: Int,
    val gapPx: Int,
    val overflowPolicy: EquipmentInventoryOverflowPolicy,
    val selectedInventoryIndex: Int?,
    val cells: List<InventoryGridCellModel>,
    val pageLabel: String,
)

internal data class EquipmentInventoryPresentation(
    val equipmentSlots: List<EquipmentSlotCellModel>,
    val inventoryGrid: InventoryGridModel,
) {
    companion object {
        fun empty(): EquipmentInventoryPresentation =
            EquipmentInventoryPresentation(
                equipmentSlots = emptyList(),
                inventoryGrid =
                    InventoryGridModel(
                        columns = EQUIPMENT_INVENTORY_COMPANION_COLUMNS,
                        visibleRows = EQUIPMENT_INVENTORY_COMPANION_VISIBLE_ROWS,
                        cellSizePx = 128,
                        gapPx = 6,
                        overflowPolicy = EquipmentInventoryOverflowPolicy.NONE,
                        selectedInventoryIndex = null,
                        cells = emptyList(),
                        pageLabel = "",
                    ),
            )
    }
}

internal data class EquipmentInventoryPresenterRequest(
    val localizer: Localizer,
    val visualResolver: VisualManifestResolver,
    val equipment: List<EquipmentSlotSnapshot>,
    val inventory: List<InventoryEntrySnapshot>,
    val inventorySelection: Int,
    val selectedEquipmentSlotId: String? = null,
    val inventoryColumns: Int = EQUIPMENT_INVENTORY_COMPANION_COLUMNS,
    val inventoryVisibleRows: Int = EQUIPMENT_INVENTORY_COMPANION_VISIBLE_ROWS,
    val cellSizePx: Int = 128,
    val gapPx: Int = 6,
)

internal val typedEquipmentSlotOrder: List<String> = listOf("WEAPON", "OFF_HAND", "ARMOR", "ACCESSORY")

internal object EquipmentInventoryPresenter {
    private const val VISUAL_ONLY_SOCKET_COUNT = 5

    fun present(request: EquipmentInventoryPresenterRequest): EquipmentInventoryPresentation =
        EquipmentInventoryPresentation(
            equipmentSlots = equipmentSlots(request),
            inventoryGrid = inventoryGrid(request),
        )

    private fun equipmentSlots(request: EquipmentInventoryPresenterRequest): List<EquipmentSlotCellModel> {
        val bySlot = request.equipment.associateBy(EquipmentSlotSnapshot::slotId)
        val typedSlots =
            typedEquipmentSlotOrder.map { slotId ->
                val item = bySlot[slotId]?.item
                val selected = request.selectedEquipmentSlotId == slotId
                val frameKey = frameKey(item = item, selected = selected)
                val itemIcon = item?.let { resolveItemIconVisual(request.visualResolver, it) }
                EquipmentSlotCellModel(
                    slotId = slotId,
                    labelKey = equipmentSlotLabelKey(slotId),
                    label = equipmentSlotLabel(request.localizer, slotId),
                    frame = resolveExact(request.visualResolver, frameKey),
                    itemIcon = itemIcon,
                    qualityTierId = item?.qualityTierId,
                    selected = selected,
                    tooltipAnchorId = slotId,
                )
            }
        // PR-02-1 demo shell keeps five visual-only sockets around the four typed equipment slots.
        val visualOnlySlots =
            List(VISUAL_ONLY_SOCKET_COUNT) { index ->
                EquipmentSlotCellModel(
                    slotId = null,
                    labelKey = null,
                    label = "",
                    frame = resolveExact(request.visualResolver, DarkUiChromeVisualKeys.SLOT_EMPTY),
                    itemIcon = null,
                    qualityTierId = null,
                    selected = false,
                    tooltipAnchorId = "visual-socket-$index",
                    visualOnly = true,
                )
            }
        return typedSlots + visualOnlySlots
    }

    private fun inventoryGrid(request: EquipmentInventoryPresenterRequest): InventoryGridModel {
        val sortedInventory = request.inventory.sortedBy(InventoryEntrySnapshot::index)
        val selectedEntry =
            sortedInventory.firstOrNull { entry -> entry.index == request.inventorySelection }
                ?: sortedInventory.getOrNull(request.inventorySelection)
        val selectedInventoryIndex = selectedEntry?.index
        val pageSize = request.inventoryColumns * request.inventoryVisibleRows
        val selectedPosition = selectedEntry?.let(sortedInventory::indexOf)?.takeIf { index -> index >= 0 } ?: request.inventorySelection
        val pageCount = ((sortedInventory.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val pageIndex = (selectedPosition / pageSize).coerceIn(0, pageCount - 1)
        val pageItems = sortedInventory.drop(pageIndex * pageSize).take(pageSize)
        val cells =
            pageItems.map { entry ->
                val selected = entry.index == selectedInventoryIndex
                val frameKey = frameKey(item = entry.item, selected = selected)
                val itemIcon = resolveItemIconVisual(request.visualResolver, entry.item)
                InventoryGridCellModel(
                    identityIndex = entry.index,
                    frame = resolveExact(request.visualResolver, frameKey),
                    itemIcon = itemIcon,
                    quantityText = null,
                    qualityTierId = entry.item.qualityTierId,
                    selected = selected,
                    tooltipAnchorId = "inventory:${entry.index}",
                )
            } +
                List((pageSize - pageItems.size).coerceAtLeast(0)) {
                    InventoryGridCellModel(
                        identityIndex = null,
                        frame = resolveExact(request.visualResolver, DarkUiChromeVisualKeys.SLOT_EMPTY),
                        itemIcon = null,
                        quantityText = null,
                        qualityTierId = null,
                        selected = false,
                        tooltipAnchorId = null,
                    )
                }
        return InventoryGridModel(
            columns = request.inventoryColumns,
            visibleRows = request.inventoryVisibleRows,
            cellSizePx = request.cellSizePx,
            gapPx = request.gapPx,
            overflowPolicy = if (pageCount > 1) EquipmentInventoryOverflowPolicy.PAGED else EquipmentInventoryOverflowPolicy.NONE,
            selectedInventoryIndex = selectedInventoryIndex,
            cells = cells,
            pageLabel =
                if (pageCount > 1) {
                    request.localizer.text(
                        "ui.inventory.companion.page_hint",
                        "current" to pageIndex + 1,
                        "total" to pageCount,
                        "shortcut" to request.localizer.text("ui.key.page_up_down"),
                    )
                } else {
                    ""
                },
        )
    }

    private fun frameKey(
        item: ItemRenderSnapshot?,
        selected: Boolean,
    ): String =
        when {
            selected -> DarkUiChromeVisualKeys.SLOT_SELECTED
            item != null -> DarkUiChromeVisualKeys.SLOT_EQUIPPED
            else -> DarkUiChromeVisualKeys.SLOT_EMPTY
        }

    private fun resolveExact(
        visualResolver: VisualManifestResolver,
        key: String,
    ): ResolvedVisualAsset {
        val resolved = visualResolver.resolve(key)
        require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
            "Equipment inventory presentation requires exact visual key '$key'."
        }
        return resolved
    }

}
