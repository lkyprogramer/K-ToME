package com.ktome.client.render

import com.ktome.client.assets.DarkUiChromeVisualKeys
import com.ktome.client.assets.ManifestLogSink
import com.ktome.client.assets.ManifestPrefixRule
import com.ktome.client.assets.VisualManifest
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.ui.UiCompanionVisualKeys
import com.ktome.core.snapshot.EquipmentSlotSnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.ItemStatModifierSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InventoryWorkbenchPresenterTest {
    @Test
    fun `workbench owns nine equipment sockets and disabled visual-only placeholders`() {
        val model =
            present(
                equipment =
                    listOf(
                        EquipmentSlotSnapshot(
                            slotId = "WEAPON",
                            item = item(baseItemId = "hunter_bow", slotId = "WEAPON", iconKey = "item.hunter_bow.icon"),
                        ),
                    ),
                inventory =
                    listOf(
                        InventoryEntrySnapshot(0, item(baseItemId = "healing_potion", typeId = "CONSUMABLE", iconKey = "item.healing_potion.icon")),
                    ),
            )

        assertEquals(9, model.equipmentSockets.size)
        assertEquals(listOf("WEAPON", "OFF_HAND", "ARMOR", "ACCESSORY"), model.equipmentSockets.take(4).map { socket -> socket.slotId })
        val placeholders = model.equipmentSockets.drop(4)
        assertEquals(5, placeholders.size)
        assertTrue(placeholders.all { socket -> socket.visualOnly })
        assertTrue(placeholders.all { socket -> !socket.enabled })
        assertTrue(placeholders.all { socket -> socket.labelToken == null && socket.tooltipToken == null })
        assertTrue(placeholders.all { socket -> socket.disabledReasonToken == "ui.inventory.slot.visual_only_unavailable" })
    }

    @Test
    fun `grid is six by four and exposes typed stack action target`() {
        val model =
            present(
                inventory =
                    listOf(
                        InventoryEntrySnapshot(
                            index = 0,
                            item = item(baseItemId = "healing_potion", typeId = "CONSUMABLE", iconKey = "item.healing_potion.icon"),
                        ),
                        InventoryEntrySnapshot(
                            index = 1,
                            item = item(baseItemId = "healing_potion", typeId = "CONSUMABLE", iconKey = "item.healing_potion.icon"),
                        ),
                    ),
            )
        val firstCell = model.grid.cells.first()

        assertEquals(6, model.grid.columns)
        assertEquals(4, model.grid.rows)
        assertEquals(24, model.grid.cells.size)
        assertEquals(InventoryWorkbenchGrid.CELL_SIZE_PX, model.grid.cellSizePx)
        assertEquals(listOf(0, 1), firstCell.entryIds)
        assertEquals("x2", firstCell.quantityText)
        assertEquals(0, firstCell.representativeEntryId)
        assertTrue(firstCell.stableStackId?.startsWith("stack:healing_potion|CONSUMABLE|NORMAL|") == true)
        assertEquals(0, firstCell.stackActionTarget?.representativeEntryId)
        assertTrue(model.grid.cells.drop(1).all { cell -> cell.empty })
    }

    @Test
    fun `stack quantity badge thresholds are stable`() {
        assertEquals("x2", quantityBadgeFor(2))
        assertEquals("x10", quantityBadgeFor(10))
        assertEquals("x99", quantityBadgeFor(99))
        assertEquals("x99+", quantityBadgeFor(100))
    }

    @Test
    fun `empty inventory keeps full grid and disables item actions`() {
        val model = present()

        assertEquals(24, model.grid.cells.size)
        assertTrue(model.grid.cells.all { cell -> cell.empty })
        assertTrue(model.selectedItemTitle.isNotBlank())
        assertTrue(model.detailRows.any { row -> row.sourceFieldId == "empty" })
        assertTrue(model.actions.filter { action -> action.actionId != "return" }.all { action -> !action.enabled })
        assertTrue(model.actions.filter { action -> action.actionId != "return" }.all { action -> action.disabledReasonToken == "ui.inventory.action.disabled.no_item" })
    }

    @Test
    fun `equipment and special identities do not merge even when base item ids match`() {
        val inventory =
            listOf(
                InventoryEntrySnapshot(
                    0,
                    item(
                        baseItemId = "long_sword",
                        typeId = "WEAPON",
                        slotId = "WEAPON",
                        iconKey = "item.long_sword.icon",
                    ),
                ),
                InventoryEntrySnapshot(
                    1,
                    item(
                        baseItemId = "long_sword",
                        typeId = "WEAPON",
                        slotId = "WEAPON",
                        iconKey = "item.long_sword.icon",
                    ),
                ),
                InventoryEntrySnapshot(
                    2,
                    item(
                        baseItemId = "healing_potion",
                        typeId = "CONSUMABLE",
                        iconKey = "item.healing_potion.icon",
                        qualityTierId = "MAGIC",
                    ),
                ),
                InventoryEntrySnapshot(
                    3,
                    item(baseItemId = "healing_potion", typeId = "CONSUMABLE", iconKey = "item.healing_potion.icon"),
                ),
            )

        val cells = present(inventory = inventory).grid.cells.take(4)

        assertEquals(listOf(0), cells[0].entryIds)
        assertEquals(listOf(1), cells[1].entryIds)
        assertNull(cells[0].quantityText)
        assertNull(cells[1].quantityText)
        assertEquals(listOf(2), cells[2].entryIds)
        assertEquals(listOf(3), cells[3].entryIds)
    }

    @Test
    fun `material variants do not merge in stack identity`() {
        val groups =
            InventoryWorkbenchStacks.groups(
                listOf(
                    InventoryEntrySnapshot(
                        0,
                        item(
                            baseItemId = "healing_potion",
                            typeId = "CONSUMABLE",
                            materialNameKey = "item.material.glass",
                        ),
                    ),
                    InventoryEntrySnapshot(
                        1,
                        item(
                            baseItemId = "healing_potion",
                            typeId = "CONSUMABLE",
                            materialNameKey = "item.material.iron",
                        ),
                    ),
                    InventoryEntrySnapshot(
                        2,
                        item(
                            baseItemId = "healing_potion",
                            typeId = "CONSUMABLE",
                            materialNameKey = "item.material.glass",
                        ),
                    ),
                ),
            )

        assertEquals(listOf(listOf(0, 2), listOf(1)), groups.map { group -> group.entryIds })
        assertEquals(2, groups.first().actionTarget.quantity)
    }

    @Test
    fun `detail compare and action rows are typed from selected entry rather than hover preview`() {
        val equipped =
            item(
                baseItemId = "long_sword",
                typeId = "WEAPON",
                slotId = "WEAPON",
                iconKey = "item.long_sword.icon",
                stats = ItemStatModifierSnapshot(attack = 1, defense = 2),
            )
        val candidate =
            item(
                baseItemId = "hunter_bow",
                typeId = "WEAPON",
                slotId = "WEAPON",
                iconKey = "item.hunter_bow.icon",
                stats = ItemStatModifierSnapshot(attack = 4, defense = 1),
            )
        val hoverOnly =
            item(baseItemId = "healing_potion", typeId = "CONSUMABLE", iconKey = "item.healing_potion.icon")

        val model =
            present(
                equipment = listOf(EquipmentSlotSnapshot("WEAPON", equipped)),
                inventory =
                    listOf(
                        InventoryEntrySnapshot(3, candidate),
                        InventoryEntrySnapshot(7, hoverOnly),
                ),
                selectedEntryId = 3,
                hoveredCell = InventoryWorkbenchCellCoordinate(column = 1, row = 0),
            )

        assertTrue(model.selectedItemTitle.contains("Hunter Bow"))
        assertTrue(model.grid.cells.first { cell -> cell.representativeEntryId == 3 }.selected)
        assertEquals(InventoryWorkbenchCellCoordinate(column = 1, row = 0), model.grid.hoveredCell)
        assertTrue(model.grid.cells.first { cell -> cell.representativeEntryId == 7 }.hovered)
        assertFalse(model.grid.cells.first { cell -> cell.representativeEntryId == 7 }.selected)
        val attackDelta = model.compareRows.single { row -> row.statId == "attack" }
        assertEquals("1", attackDelta.currentValue)
        assertEquals("4", attackDelta.candidateValue)
        assertEquals("+3", attackDelta.deltaValue)
        assertEquals(TileTextTone.GREEN, attackDelta.tone)
        val defenseDelta = model.compareRows.single { row -> row.statId == "defense" }
        assertEquals("2", defenseDelta.currentValue)
        assertEquals("1", defenseDelta.candidateValue)
        assertEquals("-1", defenseDelta.deltaValue)
        assertEquals(TileTextTone.RED, defenseDelta.tone)
        assertEquals(listOf("inspect", "equip", "drop", "return"), model.actions.map { action -> action.actionId })
        assertEquals("WEAPON", model.equipmentTargetSlotId)
        assertTrue(model.actions.all { action -> action.shortcutToken.startsWith("ui.key.") })
        assertTrue(model.footerHints.all { hint -> hint.shortcutToken.startsWith("ui.key.") })
    }

    @Test
    fun `empty cell hover is represented by hovered cell without changing selection`() {
        val model =
            present(
                inventory =
                    listOf(
                        InventoryEntrySnapshot(
                            index = 0,
                            item = item(baseItemId = "healing_potion", typeId = "CONSUMABLE", iconKey = "item.healing_potion.icon"),
                        ),
                    ),
                selectedEntryId = 0,
                hoveredCell = InventoryWorkbenchCellCoordinate(column = 2, row = 0),
            )
        val emptyHoveredCell = model.grid.cells.first { cell -> cell.coordinate == InventoryWorkbenchCellCoordinate(column = 2, row = 0) }

        assertEquals(InventoryWorkbenchCellCoordinate(column = 2, row = 0), model.grid.hoveredCell)
        assertTrue(emptyHoveredCell.empty)
        assertTrue(emptyHoveredCell.hovered)
        assertEquals(0, model.grid.selectedEntryId)
        assertTrue(model.grid.cells.first { cell -> cell.representativeEntryId == 0 }.selected)
    }

    @Test
    fun `non equippable material-like item does not expose equip action`() {
        val model =
            present(
                inventory =
                    listOf(
                        InventoryEntrySnapshot(
                            index = 0,
                            item = item(baseItemId = "iron_shard", typeId = "MATERIAL", iconKey = "item.healing_potion.icon"),
                        ),
                    ),
            )

        assertEquals(listOf("inspect", "drop", "return"), model.actions.map { action -> action.actionId })
        assertFalse(model.actions.any { action -> action.labelToken == "ui.inventory.action.equip_or_use" })
    }

    @Test
    fun `empty selected entry renders empty state without selecting a previous item`() {
        val model =
            present(
                inventory =
                    listOf(
                        InventoryEntrySnapshot(
                            index = 0,
                            item = item(baseItemId = "healing_potion", typeId = "CONSUMABLE", iconKey = "item.healing_potion.icon"),
                        ),
                    ),
                selectedEntryId = null,
            )

        assertNull(model.grid.selectedEntryId)
        assertFalse(model.grid.cells.first().selected)
        assertTrue(model.detailRows.any { row -> row.sourceFieldId == "empty" })
        assertTrue(model.actions.filter { action -> action.actionId != "return" }.all { action -> !action.enabled })
    }

    @Test
    fun `layout profiles keep documented column and cell minimums`() {
        val desktop =
            com.ktome.client.render.layout.InventoryWorkbenchLayoutSolver.resolve(
                com.ktome.client.render.layout.InventoryWorkbenchLayoutRequest(viewportWidth = 1672, viewportHeight = 941),
            )
        assertTrue(desktop.equipmentColumn.width >= 300f)
        assertTrue(desktop.backpackColumn.width >= 520f)
        assertTrue(desktop.detailColumn.width >= 400f)
        assertEquals(64f, desktop.footer.height)
        assertEquals(7, desktop.detailMaxLines)
        assertTrue(desktop.backpackCellBounds.all { bounds -> bounds.width in 64f..72f })
        assertTrue(desktop.root.right <= 1672f)
        assertTrue(desktop.root.top <= 941f)
        assertTrue(desktop.content.right <= desktop.root.right)
        assertTrue(desktop.content.top <= desktop.root.top)
        assertTrue(desktop.detailColumn.right <= desktop.content.right)

        val standard =
            com.ktome.client.render.layout.InventoryWorkbenchLayoutSolver.resolve(
                com.ktome.client.render.layout.InventoryWorkbenchLayoutRequest(viewportWidth = 1280, viewportHeight = 800),
            )
        assertTrue(standard.equipmentColumn.width >= 240f)
        assertTrue(standard.backpackColumn.width >= 440f)
        assertTrue(standard.detailColumn.width >= 320f)
        assertEquals(60f, standard.footer.height)
        assertEquals(6, standard.detailMaxLines)
        assertTrue(standard.backpackCellBounds.all { bounds -> bounds.width in 58f..64f })
        assertTrue(standard.root.right <= 1280f)
        assertTrue(standard.root.top <= 800f)
        assertTrue(standard.content.right <= standard.root.right)
        assertTrue(standard.content.top <= standard.root.top)
        assertTrue(standard.detailColumn.right <= standard.content.right)

        val compact =
            com.ktome.client.render.layout.InventoryWorkbenchLayoutSolver.resolve(
                com.ktome.client.render.layout.InventoryWorkbenchLayoutRequest(viewportWidth = 1024, viewportHeight = 768),
            )
        assertTrue(compact.equipmentColumn.width >= 190f)
        assertTrue(compact.backpackColumn.width >= 384f)
        assertTrue(compact.detailColumn.width >= 280f)
        assertEquals(56f, compact.footer.height)
        assertEquals(5, compact.detailMaxLines)
        assertTrue(compact.backpackCellBounds.all { bounds -> bounds.width in 52f..56f })
        assertTrue(compact.root.right <= 1024f)
        assertTrue(compact.root.top <= 768f)
        assertTrue(compact.content.right <= compact.root.right)
        assertTrue(compact.content.top <= compact.root.top)
        assertTrue(compact.detailColumn.right <= compact.content.right)

        val constrained =
            com.ktome.client.render.layout.InventoryWorkbenchLayoutSolver.resolve(
                com.ktome.client.render.layout.InventoryWorkbenchLayoutRequest(viewportWidth = 800, viewportHeight = 600),
            )
        assertTrue(constrained.root.right <= 800f)
        assertTrue(constrained.root.top <= 600f)
        assertTrue(constrained.content.right <= constrained.root.right)
        assertTrue(constrained.content.top <= constrained.root.top)
        assertTrue(constrained.detailColumn.right <= constrained.content.right)
        assertTrue(constrained.equipmentColumn.width >= 0f)
        assertTrue(constrained.backpackColumn.width >= 0f)
        assertTrue(constrained.detailColumn.width >= 0f)
        assertTrue(constrained.equipmentSlotBounds.all { bounds -> bounds.x >= constrained.equipmentColumn.x && bounds.right <= constrained.equipmentColumn.right })
        assertTrue(constrained.backpackCellBounds.all { bounds -> bounds.x >= constrained.backpackColumn.x && bounds.right <= constrained.backpackColumn.right })
    }

    private fun present(
        equipment: List<EquipmentSlotSnapshot> = emptyList(),
        inventory: List<InventoryEntrySnapshot> = emptyList(),
        selectedEntryId: Int? = 0,
        hoveredCell: InventoryWorkbenchCellCoordinate? = null,
        pageIndex: Int = 0,
    ): InventoryWorkbenchPresentation =
        InventoryWorkbenchPresenter.present(
            InventoryWorkbenchPresenterRequest(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = sampleResolver(),
                equipment = equipment,
                inventory = inventory,
                selectedEntryId = selectedEntryId,
                focusedCell = InventoryWorkbenchCellCoordinate.ORIGIN,
                hoveredCell = hoveredCell,
                pageIndex = pageIndex,
            ),
        )

    private fun item(
        baseItemId: String,
        typeId: String = "CONSUMABLE",
        slotId: String? = null,
        iconKey: String? = null,
        qualityTierId: String = "NORMAL",
        materialNameKey: String? = null,
        stats: ItemStatModifierSnapshot = ItemStatModifierSnapshot(),
    ): ItemRenderSnapshot =
        ItemRenderSnapshot(
            baseItemId = baseItemId,
            nameKey = "item.$baseItemId.name",
            typeId = typeId,
            iconKey = iconKey,
            slotId = slotId,
            qualityTierId = qualityTierId,
            materialNameKey = materialNameKey,
            stats = stats,
        )

    private fun sampleResolver(logSink: ManifestLogSink = ManifestLogSink { error("Unexpected manifest fallback: $it") }): VisualManifestResolver =
        VisualManifestResolver(
            manifest =
                VisualManifest(
                    manifestVersion = 1,
                    styleTag = "test-style",
                    fallbackKey = "missing_visual",
                    entries =
                        listOf(
                            VisualManifestEntry("missing_visual", "debug", "debug/missing_visual.png", "ui"),
                            VisualManifestEntry(UiCompanionVisualKeys.EMPTY_INVENTORY, "icon", "dark-v1/ui/ui_empty_inventory_icon.png", "ui"),
                            VisualManifestEntry(DarkUiChromeVisualKeys.SLOT_EMPTY, "ui_frame", "dark-v1/ui/ui_frame_slot_empty.png", "ui"),
                            VisualManifestEntry(DarkUiChromeVisualKeys.SLOT_EQUIPPED, "ui_frame", "dark-v1/ui/ui_frame_slot_equipped.png", "ui"),
                            VisualManifestEntry(DarkUiChromeVisualKeys.SLOT_SELECTED, "ui_frame", "dark-v1/ui/ui_frame_slot_selected.png", "ui"),
                            VisualManifestEntry("item.hunter_bow.icon", "icon_item", "dark-v1/items/item_hunter_bow_icon.png", "ui"),
                            VisualManifestEntry("item.long_sword.icon", "icon_item", "dark-v1/items/item_long_sword_icon.png", "ui"),
                            VisualManifestEntry("item.healing_potion.icon", "icon_item", "dark-v1/items/item_healing_potion_icon.png", "ui"),
                        ),
                    prefixRules = listOf(ManifestPrefixRule(prefix = "icon.", targetKey = "missing_visual")),
                ),
            logSink = logSink,
        )

    private fun quantityBadgeFor(quantity: Int): String? {
        val model =
            present(
                inventory =
                    List(quantity) { index ->
                        InventoryEntrySnapshot(
                            index = index,
                            item = item(baseItemId = "healing_potion", typeId = "CONSUMABLE", iconKey = "item.healing_potion.icon"),
                        )
                    },
            )
        val cell = model.grid.cells.first()
        assertNotNull(cell.stackActionTarget)
        assertEquals(quantity, cell.stackActionTarget?.quantity)
        return cell.quantityText
    }
}
