package com.ktome.client.render

import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.ui.item.QualityColorTokenId
import com.ktome.client.ui.item.QualityPresentation
import com.ktome.client.ui.talent.DescriptionLine
import com.ktome.client.ui.talent.DescriptionLineKind
import com.ktome.client.ui.talent.DescriptionPresenter
import com.ktome.core.snapshot.EquipmentSlotSnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.ItemStatModifierSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.inventoryStackIdentity
import com.ktome.game.i18n.Localizer

data class InventoryWorkbenchCellCoordinate(
    val column: Int,
    val row: Int,
) {
    fun visualIndex(): Int = row * InventoryWorkbenchGrid.COLUMNS + column

    companion object {
        val ORIGIN: InventoryWorkbenchCellCoordinate = InventoryWorkbenchCellCoordinate(0, 0)
    }
}

internal object InventoryWorkbenchGrid {
    const val COLUMNS: Int = 6
    const val ROWS: Int = 4
    const val PAGE_SIZE: Int = COLUMNS * ROWS
    const val CELL_SIZE_PX: Int = 24

    fun coordinateForVisualIndex(index: Int): InventoryWorkbenchCellCoordinate =
        InventoryWorkbenchCellCoordinate(
            column = Math.floorMod(index, COLUMNS),
            row = (index / COLUMNS).coerceIn(0, ROWS - 1),
        )

    fun coerce(coordinate: InventoryWorkbenchCellCoordinate): InventoryWorkbenchCellCoordinate =
        InventoryWorkbenchCellCoordinate(
            column = coordinate.column.coerceIn(0, COLUMNS - 1),
            row = coordinate.row.coerceIn(0, ROWS - 1),
        )
}

internal data class InventoryWorkbenchStackGroup(
    val stableStackId: String,
    val entries: List<InventoryEntrySnapshot>,
) {
    val representative: InventoryEntrySnapshot = entries.first()
    val entryIds: List<Int> = entries.map(InventoryEntrySnapshot::index)
    val quantity: Int = entries.size
    val actionTarget: InventoryWorkbenchStackActionTarget =
        InventoryWorkbenchStackActionTarget(
            stableStackId = stableStackId,
            representativeEntryId = representative.index,
            entryIds = entryIds,
            quantity = quantity,
        )
}

internal data class InventoryWorkbenchStackActionTarget(
    val stableStackId: String,
    val representativeEntryId: Int,
    val entryIds: List<Int>,
    val quantity: Int,
)

internal object InventoryWorkbenchStacks {
    fun groups(inventory: List<InventoryEntrySnapshot>): List<InventoryWorkbenchStackGroup> {
        val stackBuckets = linkedMapOf<String, MutableList<InventoryEntrySnapshot>>()
        val groups = mutableListOf<InventoryWorkbenchStackGroup>()
        inventory.sortedBy(InventoryEntrySnapshot::index).forEach { entry ->
            val stackIdentity = entry.item.inventoryStackIdentity()
            if (stackIdentity == null) {
                groups += InventoryWorkbenchStackGroup("entry:${entry.index}", listOf(entry))
            } else {
                stackBuckets.getOrPut(stackIdentity) { mutableListOf() } += entry
            }
        }
        stackBuckets.forEach { (identity, entries) ->
            groups += InventoryWorkbenchStackGroup("stack:$identity", entries.toList())
        }
        return groups.sortedBy { group -> group.representative.index }
    }

    fun pageCount(groups: List<InventoryWorkbenchStackGroup>): Int =
        ((groups.size + InventoryWorkbenchGrid.PAGE_SIZE - 1) / InventoryWorkbenchGrid.PAGE_SIZE).coerceAtLeast(1)

    fun pageIndexForEntry(
        groups: List<InventoryWorkbenchStackGroup>,
        entryId: Int,
    ): Int? {
        val position = groups.indexOfFirst { group -> entryId in group.entryIds }
        return position.takeIf { index -> index >= 0 }?.let { index -> index / InventoryWorkbenchGrid.PAGE_SIZE }
    }

    fun coordinateForEntry(
        groups: List<InventoryWorkbenchStackGroup>,
        entryId: Int,
    ): InventoryWorkbenchCellCoordinate? {
        val position = groups.indexOfFirst { group -> entryId in group.entryIds }
        if (position < 0) {
            return null
        }
        return InventoryWorkbenchGrid.coordinateForVisualIndex(position % InventoryWorkbenchGrid.PAGE_SIZE)
    }

    fun groupAt(
        groups: List<InventoryWorkbenchStackGroup>,
        pageIndex: Int,
        coordinate: InventoryWorkbenchCellCoordinate,
    ): InventoryWorkbenchStackGroup? =
        groups.getOrNull(pageIndex.coerceAtLeast(0) * InventoryWorkbenchGrid.PAGE_SIZE + InventoryWorkbenchGrid.coerce(coordinate).visualIndex())

}

internal data class InventoryWorkbenchEquipmentSocketModel(
    val slotId: String?,
    val labelToken: String?,
    val label: String,
    val tooltipToken: String?,
    val disabledReasonToken: String?,
    val enabled: Boolean,
    val visualOnly: Boolean,
    val frame: ResolvedVisualAsset,
    val itemIcon: ResolvedVisualAsset?,
    val qualityTierId: String?,
    val selected: Boolean,
    val targetCue: Boolean,
)

internal data class InventoryWorkbenchGridCellModel(
    val coordinate: InventoryWorkbenchCellCoordinate,
    val stableCellId: String,
    val stableStackId: String?,
    val representativeEntryId: Int?,
    val entryIds: List<Int>,
    val itemName: String?,
    val frame: ResolvedVisualAsset,
    val itemIcon: ResolvedVisualAsset?,
    val quantityText: String?,
    val stackActionTarget: InventoryWorkbenchStackActionTarget?,
    val qualityTierId: String?,
    val selected: Boolean,
    val focused: Boolean,
    val hovered: Boolean,
    val tooltipAnchorId: String?,
) {
    val empty: Boolean = representativeEntryId == null
}

internal data class InventoryWorkbenchGridModel(
    val columns: Int,
    val rows: Int,
    val cellSizePx: Int,
    val pageIndex: Int,
    val pageCount: Int,
    val pageLabel: String,
    val capacityText: String,
    val focusedCell: InventoryWorkbenchCellCoordinate,
    val hoveredCell: InventoryWorkbenchCellCoordinate?,
    val selectedEntryId: Int?,
    val cells: List<InventoryWorkbenchGridCellModel>,
)

internal data class InventoryWorkbenchTextRowModel(
    val sourceFieldId: String,
    val labelToken: String?,
    val label: String,
    val value: String,
    val tone: TileTextTone,
    val statId: String? = null,
    val currentValue: String? = null,
    val candidateValue: String? = null,
    val deltaValue: String? = null,
)

internal data class InventoryWorkbenchActionModel(
    val actionId: String,
    val shortcutToken: String,
    val shortcutText: String,
    val labelToken: String,
    val label: String,
    val enabled: Boolean,
    val disabledReasonToken: String?,
    val disabledReason: String?,
    val stackActionTarget: InventoryWorkbenchStackActionTarget?,
)

internal data class InventoryWorkbenchFooterHintModel(
    val shortcutToken: String,
    val keyText: String,
    val labelToken: String,
    val label: String,
)

internal data class InventoryWorkbenchPresentation(
    val title: String,
    val equipmentTitle: String,
    val backpackTitle: String,
    val detailTitle: String,
    val compareTitle: String,
    val actionsTitle: String,
    val equipmentSockets: List<InventoryWorkbenchEquipmentSocketModel>,
    val grid: InventoryWorkbenchGridModel,
    val selectedItemTitle: String,
    val selectedItemIcon: ResolvedVisualAsset?,
    val selectedItemTone: TileTextTone,
    val detailRows: List<InventoryWorkbenchTextRowModel>,
    val compareRows: List<InventoryWorkbenchTextRowModel>,
    val actions: List<InventoryWorkbenchActionModel>,
    val footerHints: List<InventoryWorkbenchFooterHintModel>,
    val equipmentTargetSlotId: String?,
)

internal data class InventoryWorkbenchPresenterRequest(
    val localizer: Localizer,
    val visualResolver: VisualManifestResolver,
    val equipment: List<EquipmentSlotSnapshot>,
    val inventory: List<InventoryEntrySnapshot>,
    val selectedEntryId: Int?,
    val focusedCell: InventoryWorkbenchCellCoordinate,
    val hoveredCell: InventoryWorkbenchCellCoordinate?,
    val pageIndex: Int,
)

internal object InventoryWorkbenchPresenter {
    private const val VISUAL_ONLY_SOCKET_COUNT = 5
    private val visualOnlyDisabledToken = "ui.inventory.slot.visual_only_unavailable"

    fun present(request: InventoryWorkbenchPresenterRequest): InventoryWorkbenchPresentation {
        val groups = InventoryWorkbenchStacks.groups(request.inventory)
        val pageCount = InventoryWorkbenchStacks.pageCount(groups)
        val pageIndex = request.pageIndex.coerceIn(0, pageCount - 1)
        val focusedCell = InventoryWorkbenchGrid.coerce(request.focusedCell)
        val selectedEntry = request.selectedEntryId?.let { selectedEntryId -> request.inventory.firstOrNull { entry -> entry.index == selectedEntryId } }
        val selectedItem = selectedEntry?.item
        val selectedGroup = selectedEntry?.let { entry -> groups.firstOrNull { group -> entry.index in group.entryIds } }
        val equipmentSockets = equipmentSockets(request, selectedItem?.slotId)
        return InventoryWorkbenchPresentation(
            title = request.localizer.text("ui.inventory.workbench.title"),
            equipmentTitle = request.localizer.text("ui.inventory.workbench.equipment"),
            backpackTitle = request.localizer.text("ui.inventory.workbench.backpack"),
            detailTitle = request.localizer.text("ui.inventory.workbench.detail"),
            compareTitle = request.localizer.text("ui.inventory.workbench.compare"),
            actionsTitle = request.localizer.text("ui.inventory.workbench.actions"),
            equipmentSockets = equipmentSockets,
            grid = grid(request, groups, pageIndex, pageCount, focusedCell, selectedEntry?.index),
            selectedItemTitle = selectedItem?.let { item -> renderItemDisplay(request.localizer, item) } ?: request.localizer.text("ui.inventory.empty_selection"),
            selectedItemIcon = selectedItem?.let { item -> resolveItemIconVisual(request.visualResolver, item) },
            selectedItemTone = selectedItem?.let(::itemTone) ?: TileTextTone.GRAY,
            detailRows = detailRows(request.localizer, selectedItem),
            compareRows = compareRows(request.localizer, request.equipment, selectedItem),
            actions = actionRows(request.localizer, selectedItem, selectedGroup?.actionTarget),
            footerHints = footerHints(request.localizer),
            equipmentTargetSlotId = equipmentSockets.firstOrNull { socket -> socket.targetCue }?.slotId,
        )
    }

    private fun equipmentSockets(
        request: InventoryWorkbenchPresenterRequest,
        selectedItemSlotId: String?,
    ): List<InventoryWorkbenchEquipmentSocketModel> {
        val bySlot = request.equipment.associateBy(EquipmentSlotSnapshot::slotId)
        val typedSlots =
            typedEquipmentSlotOrder.map { slotId ->
                val item = bySlot[slotId]?.item
                val selected = selectedItemSlotId == slotId
                InventoryWorkbenchEquipmentSocketModel(
                    slotId = slotId,
                    labelToken = equipmentSlotLabelKey(slotId),
                    label = equipmentSlotLabel(request.localizer, slotId),
                    tooltipToken = equipmentSlotLabelKey(slotId),
                    disabledReasonToken = null,
                    enabled = true,
                    visualOnly = false,
                    frame = resolveExact(request.visualResolver, frameKey(item = item, selected = selected)),
                    itemIcon = item?.let { resolveItemIconVisual(request.visualResolver, it) },
                    qualityTierId = item?.qualityTierId,
                    selected = selected,
                    targetCue = selected,
                )
            }
        val visualOnlySlots =
            List(VISUAL_ONLY_SOCKET_COUNT) { index ->
                InventoryWorkbenchEquipmentSocketModel(
                    slotId = null,
                    labelToken = null,
                    label = "",
                    tooltipToken = null,
                    disabledReasonToken = visualOnlyDisabledToken,
                    enabled = false,
                    visualOnly = true,
                    frame = resolveExact(request.visualResolver, DarkUiChromeVisualKeys.SLOT_EMPTY),
                    itemIcon = null,
                    qualityTierId = null,
                    selected = false,
                    targetCue = false,
                )
            }
        return typedSlots + visualOnlySlots
    }

    private fun grid(
        request: InventoryWorkbenchPresenterRequest,
        groups: List<InventoryWorkbenchStackGroup>,
        pageIndex: Int,
        pageCount: Int,
        focusedCell: InventoryWorkbenchCellCoordinate,
        selectedEntryId: Int?,
    ): InventoryWorkbenchGridModel {
        val hoveredCell = request.hoveredCell?.let(InventoryWorkbenchGrid::coerce)
        val pageGroups = groups.drop(pageIndex * InventoryWorkbenchGrid.PAGE_SIZE).take(InventoryWorkbenchGrid.PAGE_SIZE)
        val cells =
            (0 until InventoryWorkbenchGrid.PAGE_SIZE).map { visualIndex ->
                val coordinate = InventoryWorkbenchGrid.coordinateForVisualIndex(visualIndex)
                val group = pageGroups.getOrNull(visualIndex)
                if (group == null) {
                    emptyCell(request.visualResolver, pageIndex, coordinate, focusedCell, hoveredCell)
                } else {
                    itemCell(
                        request = request,
                        group = group,
                        pageIndex = pageIndex,
                        coordinate = coordinate,
                        focusedCell = focusedCell,
                        hoveredCell = hoveredCell,
                        selectedEntryId = selectedEntryId,
                    )
                }
            }
        return InventoryWorkbenchGridModel(
            columns = InventoryWorkbenchGrid.COLUMNS,
            rows = InventoryWorkbenchGrid.ROWS,
            cellSizePx = InventoryWorkbenchGrid.CELL_SIZE_PX,
            pageIndex = pageIndex,
            pageCount = pageCount,
            pageLabel = request.localizer.text("ui.inventory.page", "current" to pageIndex + 1, "total" to pageCount),
            capacityText = request.localizer.text("ui.inventory.capacity", "shown" to pageGroups.size, "capacity" to InventoryWorkbenchGrid.PAGE_SIZE),
            focusedCell = focusedCell,
            hoveredCell = hoveredCell,
            selectedEntryId = selectedEntryId,
            cells = cells,
        )
    }

    private fun itemCell(
        request: InventoryWorkbenchPresenterRequest,
        group: InventoryWorkbenchStackGroup,
        pageIndex: Int,
        coordinate: InventoryWorkbenchCellCoordinate,
        focusedCell: InventoryWorkbenchCellCoordinate,
        hoveredCell: InventoryWorkbenchCellCoordinate?,
        selectedEntryId: Int?,
    ): InventoryWorkbenchGridCellModel {
        val item = group.representative.item
        val selected = selectedEntryId != null && selectedEntryId in group.entryIds
        return InventoryWorkbenchGridCellModel(
            coordinate = coordinate,
            stableCellId = "inventory-page-$pageIndex-cell-${coordinate.visualIndex()}",
            stableStackId = group.stableStackId,
            representativeEntryId = group.representative.index,
            entryIds = group.entryIds,
            itemName = renderItemDisplay(request.localizer, item),
            frame = resolveExact(request.visualResolver, frameKey(item = item, selected = selected)),
            itemIcon = resolveItemIconVisual(request.visualResolver, item),
            quantityText = quantityText(group.quantity),
            stackActionTarget = group.actionTarget,
            qualityTierId = item.qualityTierId,
            selected = selected,
            focused = coordinate == focusedCell,
            hovered = coordinate == hoveredCell,
            tooltipAnchorId = "inventory:${group.representative.index}",
        )
    }

    private fun emptyCell(
        visualResolver: VisualManifestResolver,
        pageIndex: Int,
        coordinate: InventoryWorkbenchCellCoordinate,
        focusedCell: InventoryWorkbenchCellCoordinate,
        hoveredCell: InventoryWorkbenchCellCoordinate?,
    ): InventoryWorkbenchGridCellModel =
        InventoryWorkbenchGridCellModel(
            coordinate = coordinate,
            stableCellId = "inventory-page-$pageIndex-cell-${coordinate.visualIndex()}",
            stableStackId = null,
            representativeEntryId = null,
            entryIds = emptyList(),
            itemName = null,
            frame = resolveExact(visualResolver, DarkUiChromeVisualKeys.SLOT_EMPTY),
            itemIcon = null,
            quantityText = null,
            stackActionTarget = null,
            qualityTierId = null,
            selected = false,
            focused = coordinate == focusedCell,
            hovered = coordinate == hoveredCell,
            tooltipAnchorId = null,
        )

    private fun detailRows(
        localizer: Localizer,
        item: ItemRenderSnapshot?,
    ): List<InventoryWorkbenchTextRowModel> {
        if (item == null) {
            return listOf(
                InventoryWorkbenchTextRowModel(
                    sourceFieldId = "empty",
                    labelToken = "ui.inventory.workbench.detail",
                    label = localizer.text("ui.inventory.workbench.detail"),
                    value = localizer.text("ui.inventory.empty_selection"),
                    tone = TileTextTone.GRAY,
                ),
            )
        }
        val rows = mutableListOf<InventoryWorkbenchTextRowModel>()
        item.slotId?.let { slotId ->
            rows += row(localizer, "slot", "ui.inspect.slot", placeholder = "slot", value = equipmentSlotLabel(localizer, slotId))
        }
        item.qualityNameKey?.let { qualityNameKey ->
            rows += row(localizer, "quality", "ui.inspect.quality", placeholder = "quality", value = localizer.text(qualityNameKey))
        }
        item.materialNameKey?.let { materialNameKey ->
            rows += row(localizer, "material", "ui.inspect.material", placeholder = "material", value = localizer.text(materialNameKey))
        }
        item.affixNameKeys.forEachIndexed { index, affixNameKey ->
            rows += row(localizer, "affix:$index", "ui.inspect.affix", placeholder = "affix", value = localizer.text(affixNameKey))
        }
        DescriptionPresenter.presentInventoryItemLines(localizer, item).forEachIndexed { index, line ->
            rows +=
                InventoryWorkbenchTextRowModel(
                    sourceFieldId = "description:$index",
                    labelToken = item.descKey,
                    label = "",
                    value = line.text,
                    tone = descriptionTone(line),
                )
        }
        statModifierRows(localizer, item.stats).forEachIndexed { index, statRow ->
            rows += statRow.copy(sourceFieldId = "stat:$index")
        }
        when (item.effectTypeId) {
            "HEAL" -> rows += row(localizer, "effect:heal", "ui.inspect.restore_hp", placeholder = "amount", value = item.magnitude.toString())
            "TELEPORT" ->
                rows +=
                    InventoryWorkbenchTextRowModel(
                        sourceFieldId = "effect:teleport",
                        labelToken = "ui.inspect.teleport_random",
                        label = "",
                        value = localizer.text("ui.inspect.teleport_random"),
                        tone = TileTextTone.LIGHT_GRAY,
                    )

            null -> Unit
        }
        if (rows.isEmpty()) {
            rows +=
                InventoryWorkbenchTextRowModel(
                    sourceFieldId = "empty-effect",
                    labelToken = "ui.inspect.no_special_effect",
                    label = "",
                    value = localizer.text("ui.inspect.no_special_effect"),
                    tone = TileTextTone.GRAY,
                )
        }
        return rows
    }

    private fun compareRows(
        localizer: Localizer,
        equipment: List<EquipmentSlotSnapshot>,
        selectedItem: ItemRenderSnapshot?,
    ): List<InventoryWorkbenchTextRowModel> {
        val slotId = selectedItem?.slotId ?: return emptyCompareRows(localizer)
        val slotLabel = equipmentSlotLabel(localizer, slotId)
        val equipped = equipment.firstOrNull { slot -> slot.slotId == slotId }?.item
            ?: return listOf(
                InventoryWorkbenchTextRowModel(
                    sourceFieldId = "compare:no-equipped",
                    labelToken = "ui.inventory.detail.compare.no_equipped",
                    label = slotLabel,
                    value = localizer.text("ui.inventory.detail.compare.no_equipped", "slot" to slotLabel),
                    tone = TileTextTone.GRAY,
                ),
            )
        val header =
            InventoryWorkbenchTextRowModel(
                sourceFieldId = "compare:equipped",
                labelToken = "ui.inventory.detail.compare.equipped",
                label = slotLabel,
                value = localizer.text("ui.inventory.detail.compare.equipped", "item" to renderItemDisplay(localizer, equipped)),
                tone = TileTextTone.GOLD,
            )
        val deltas = statDeltaRows(localizer, selectedItem.stats, equipped.stats)
        return listOf(header) +
            if (deltas.isEmpty()) {
                listOf(
                    InventoryWorkbenchTextRowModel(
                        sourceFieldId = "compare:no-delta",
                        labelToken = "ui.inventory.detail.compare.no_delta",
                        label = "",
                        value = localizer.text("ui.inventory.detail.compare.no_delta"),
                        tone = TileTextTone.GRAY,
                    ),
                )
            } else {
                deltas
            }
    }

    private fun emptyCompareRows(localizer: Localizer): List<InventoryWorkbenchTextRowModel> =
        listOf(
            InventoryWorkbenchTextRowModel(
                sourceFieldId = "compare:empty",
                labelToken = "ui.inventory.empty_compare",
                label = "",
                value = localizer.text("ui.inventory.empty_compare"),
                tone = TileTextTone.GRAY,
            ),
        )

    private fun actionRows(
        localizer: Localizer,
        selectedItem: ItemRenderSnapshot?,
        stackActionTarget: InventoryWorkbenchStackActionTarget?,
    ): List<InventoryWorkbenchActionModel> {
        val actions = mutableListOf<InventoryWorkbenchActionModel>()
        val noItemToken = "ui.inventory.action.disabled.no_item"
        actions +=
            InventoryWorkbenchActionModel(
                actionId = "inspect",
                shortcutToken = "ui.key.enter",
                shortcutText = localizer.text("ui.key.enter"),
                labelToken = "ui.inventory.action.inspect",
                label = localizer.text("ui.inventory.action.inspect"),
                enabled = selectedItem != null,
                disabledReasonToken = noItemToken.takeIf { selectedItem == null },
                disabledReason = noItemToken.takeIf { selectedItem == null }?.let(localizer::text),
                stackActionTarget = stackActionTarget,
            )
        selectedItem?.let { item ->
            if (item.slotId != null) {
                actions +=
                    InventoryWorkbenchActionModel(
                        actionId = "equip",
                        shortcutToken = "ui.key.e",
                        shortcutText = localizer.text("ui.key.e"),
                        labelToken = "ui.inventory.action.equip_one",
                        label = localizer.text("ui.inventory.action.equip_one"),
                        enabled = true,
                        disabledReasonToken = null,
                        disabledReason = null,
                        stackActionTarget = stackActionTarget,
                    )
            } else if (item.typeId == "CONSUMABLE" && item.effectTypeId != null) {
                actions +=
                    InventoryWorkbenchActionModel(
                        actionId = "use_one",
                        shortcutToken = "ui.key.e",
                        shortcutText = localizer.text("ui.key.e"),
                        labelToken = "ui.inventory.action.use_one",
                        label = localizer.text("ui.inventory.action.use_one"),
                        enabled = true,
                        disabledReasonToken = null,
                        disabledReason = null,
                        stackActionTarget = stackActionTarget,
                    )
            }
        }
        actions +=
            InventoryWorkbenchActionModel(
                actionId = "drop",
                shortcutToken = "ui.key.d",
                shortcutText = localizer.text("ui.key.d"),
                labelToken = if ((stackActionTarget?.quantity ?: 0) > 1) "ui.inventory.action.drop_one" else "ui.inventory.action.drop",
                label = localizer.text(if ((stackActionTarget?.quantity ?: 0) > 1) "ui.inventory.action.drop_one" else "ui.inventory.action.drop"),
                enabled = selectedItem != null,
                disabledReasonToken = noItemToken.takeIf { selectedItem == null },
                disabledReason = noItemToken.takeIf { selectedItem == null }?.let(localizer::text),
                stackActionTarget = stackActionTarget,
            )
        actions +=
            InventoryWorkbenchActionModel(
                actionId = "return",
                shortcutToken = "ui.key.esc",
                shortcutText = localizer.text("ui.key.esc"),
                labelToken = "ui.inventory.action.return",
                label = localizer.text("ui.inventory.action.return"),
                enabled = true,
                disabledReasonToken = null,
                disabledReason = null,
                stackActionTarget = null,
            )
        return actions
    }

    private fun footerHints(localizer: Localizer): List<InventoryWorkbenchFooterHintModel> =
        listOf(
            footer(localizer, "ui.key.arrows", "ui.inventory.footer.move"),
            footer(localizer, "ui.key.enter", "ui.inventory.footer.select"),
            footer(localizer, "ui.key.e", "ui.inventory.footer.equip_use"),
            footer(localizer, "ui.key.d", "ui.inventory.footer.drop"),
            footer(localizer, "ui.key.page_up_down", "ui.inventory.footer.page"),
            footer(localizer, "ui.key.esc", "ui.inventory.footer.return"),
        )

    private fun footer(
        localizer: Localizer,
        shortcutToken: String,
        labelToken: String,
    ): InventoryWorkbenchFooterHintModel =
        InventoryWorkbenchFooterHintModel(
            shortcutToken = shortcutToken,
            keyText = localizer.text(shortcutToken),
            labelToken = labelToken,
            label = localizer.text(labelToken),
        )

    private fun quantityText(quantity: Int): String? =
        when {
            quantity <= 1 -> null
            quantity <= 99 -> "x$quantity"
            else -> "x99+"
        }

    private fun renderItemDisplay(
        localizer: Localizer,
        item: ItemRenderSnapshot,
    ): String {
        val presentation = QualityPresentation.from(item)
        val baseName = item.displayName?.let { token -> renderTextToken(localizer, token) } ?: localizer.text(item.nameKey)
        return presentation.cornerGlyph?.let { glyph -> "$glyph $baseName" } ?: baseName
    }

    private fun row(
        localizer: Localizer,
        sourceFieldId: String,
        labelToken: String,
        placeholder: String,
        value: String,
    ): InventoryWorkbenchTextRowModel {
        val text = localizer.text(labelToken, placeholder to value)
        return InventoryWorkbenchTextRowModel(
            sourceFieldId = sourceFieldId,
            labelToken = labelToken,
            label = text,
            value = text,
            tone = TileTextTone.LIGHT_GRAY,
        )
    }

    private fun statModifierRows(
        localizer: Localizer,
        modifier: ItemStatModifierSnapshot,
    ): List<InventoryWorkbenchTextRowModel> =
        buildList {
            statDefinitions.forEach { stat ->
                val value = stat.valueFrom(modifier)
                if (value != 0.0) {
                    add(statTextRow(localizer, stat, value, TileTextTone.LIGHT_GRAY))
                }
            }
        }

    private fun statDeltaRows(
        localizer: Localizer,
        candidate: ItemStatModifierSnapshot,
        equipped: ItemStatModifierSnapshot,
    ): List<InventoryWorkbenchTextRowModel> =
        buildList {
            statDefinitions.forEach { stat ->
                val candidateValue = stat.valueFrom(candidate)
                val currentValue = stat.valueFrom(equipped)
                val delta = candidateValue - currentValue
                if (delta != 0.0) {
                    add(statDeltaRow(localizer, stat, currentValue, candidateValue, delta))
                }
            }
        }

    private fun statTextRow(
        localizer: Localizer,
        stat: InventoryWorkbenchStatDefinition,
        value: Double,
        tone: TileTextTone,
    ): InventoryWorkbenchTextRowModel {
        val label = localizer.text(stat.labelToken)
        val displayValue = stat.formatSigned(value)
        return InventoryWorkbenchTextRowModel(
            sourceFieldId = "stat:${stat.id}",
            labelToken = stat.labelToken,
            label = label,
            value = "$label $displayValue",
            tone = tone,
            statId = stat.id,
            currentValue = null,
            candidateValue = stat.formatPlain(value),
            deltaValue = displayValue,
        )
    }

    private fun statDeltaRow(
        localizer: Localizer,
        stat: InventoryWorkbenchStatDefinition,
        currentValue: Double,
        candidateValue: Double,
        delta: Double,
    ): InventoryWorkbenchTextRowModel {
        val label = localizer.text(stat.labelToken)
        val deltaText = stat.formatSigned(delta)
        return InventoryWorkbenchTextRowModel(
            sourceFieldId = "compare:${stat.id}",
            labelToken = stat.labelToken,
            label = label,
            value = "$label $deltaText",
            tone = deltaTone(delta),
            statId = stat.id,
            currentValue = stat.formatPlain(currentValue),
            candidateValue = stat.formatPlain(candidateValue),
            deltaValue = deltaText,
        )
    }

    private fun renderTextToken(
        localizer: Localizer,
        token: RenderTextTokenSnapshot,
    ): String =
        localizer.text(
            token.key,
            *token.arguments.map { argument -> argument.name to resolveArgument(localizer, argument) }.toTypedArray(),
        )

    private fun resolveArgument(
        localizer: Localizer,
        argument: RenderTextArgumentSnapshot,
    ): Any =
        argument.valueKey?.let(localizer::text)
            ?: argument.value
            ?: ""

    private fun itemTone(item: ItemRenderSnapshot): TileTextTone =
        itemTone(QualityPresentation.from(item))

    private fun itemTone(presentation: QualityPresentation): TileTextTone =
        when (presentation.specialAccentTokenId) {
            com.ktome.client.ui.item.SpecialAccentTokenId.UNIQUE -> TileTextTone.MAGENTA
            com.ktome.client.ui.item.SpecialAccentTokenId.ARTIFACT -> TileTextTone.RED
            null ->
                when (presentation.colorTokenId) {
                    QualityColorTokenId.NORMAL -> TileTextTone.WHITE
                    QualityColorTokenId.MAGIC -> TileTextTone.BLUE
                    QualityColorTokenId.RARE -> TileTextTone.GOLD
                }
        }

    private fun descriptionTone(line: DescriptionLine): TileTextTone =
        when (line.kind) {
            DescriptionLineKind.PRIMARY -> TileTextTone.WHITE
            DescriptionLineKind.SECONDARY -> TileTextTone.LIGHT_GRAY
            DescriptionLineKind.KEYWORD -> TileTextTone.CYAN
            DescriptionLineKind.STATE -> TileTextTone.GOLD
        }

    private fun deltaTone(value: Double): TileTextTone =
        when {
            value > 0.0 -> TileTextTone.GREEN
            value < 0.0 -> TileTextTone.RED
            else -> TileTextTone.GRAY
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
            "Inventory workbench presentation requires exact visual key '$key'."
        }
        return resolved
    }

    private data class InventoryWorkbenchStatDefinition(
        val id: String,
        val labelToken: String,
        val format: InventoryWorkbenchStatFormat,
        val valueFrom: (ItemStatModifierSnapshot) -> Double,
    ) {
        fun formatPlain(value: Double): String =
            when (format) {
                InventoryWorkbenchStatFormat.INTEGER -> value.toInt().toString()
                InventoryWorkbenchStatFormat.DECIMAL -> "%.1f".format(value)
                InventoryWorkbenchStatFormat.PERCENT -> "${(value * 100).toInt()}%"
            }

        fun formatSigned(value: Double): String =
            when (format) {
                InventoryWorkbenchStatFormat.INTEGER -> signedInteger(value.toInt())
                InventoryWorkbenchStatFormat.DECIMAL -> signedDouble(value)
                InventoryWorkbenchStatFormat.PERCENT -> "${signedInteger((value * 100).toInt())}%"
            }

        private fun signedInteger(value: Int): String =
            if (value > 0) "+$value" else value.toString()

        private fun signedDouble(value: Double): String =
            if (value > 0.0) "+%.1f".format(value) else "%.1f".format(value)
    }

    private enum class InventoryWorkbenchStatFormat {
        INTEGER,
        DECIMAL,
        PERCENT,
    }

    private val statDefinitions =
        listOf(
            InventoryWorkbenchStatDefinition("str", "ui.stat.str", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.str.toDouble() },
            InventoryWorkbenchStatDefinition("dex", "ui.stat.dex", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.dex.toDouble() },
            InventoryWorkbenchStatDefinition("con", "ui.stat.con", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.con.toDouble() },
            InventoryWorkbenchStatDefinition("wil", "ui.stat.wil", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.wil.toDouble() },
            InventoryWorkbenchStatDefinition("attack", "ui.hud.attack.short", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.attack.toDouble() },
            InventoryWorkbenchStatDefinition("defense", "ui.hud.defense.short", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.defense.toDouble() },
            InventoryWorkbenchStatDefinition("accuracy", "ui.hud.accuracy.short", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.accuracy.toDouble() },
            InventoryWorkbenchStatDefinition("evasion", "ui.hud.evasion.short", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.evasion.toDouble() },
            InventoryWorkbenchStatDefinition("speed", "ui.hud.speed.short", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.speed.toDouble() },
            InventoryWorkbenchStatDefinition("maxHp", "ui.hud.hp.short", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.maxHp.toDouble() },
            InventoryWorkbenchStatDefinition("maxStamina", "ui.hud.stamina.short", InventoryWorkbenchStatFormat.INTEGER) { stats -> stats.maxStamina.toDouble() },
            InventoryWorkbenchStatDefinition("hpRegen", "ui.inspect.mod.hp_regen", InventoryWorkbenchStatFormat.DECIMAL) { stats -> stats.hpRegen },
            InventoryWorkbenchStatDefinition("staminaRegen", "ui.inspect.mod.stamina_regen", InventoryWorkbenchStatFormat.DECIMAL) { stats -> stats.staminaRegen },
            InventoryWorkbenchStatDefinition("critChance", "ui.inspect.mod.crit", InventoryWorkbenchStatFormat.PERCENT) { stats -> stats.critChance },
            InventoryWorkbenchStatDefinition("talentPower", "ui.inspect.mod.talent", InventoryWorkbenchStatFormat.PERCENT) { stats -> stats.talentPower },
        )
}
