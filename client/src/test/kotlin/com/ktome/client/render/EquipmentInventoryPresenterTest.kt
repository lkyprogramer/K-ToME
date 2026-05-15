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
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EquipmentInventoryPresenterTest {
    @Test
    fun `equipment slots expose typed identity without fake extra sockets`() {
        val weapon =
            item(
                baseItemId = "hunter_bow",
                iconKey = "item.hunter_bow.icon",
                slotId = "WEAPON",
                qualityTierId = "MAGIC",
            )
        val accessory =
            item(
                baseItemId = "emerald_charm",
                iconKey = "item.emerald_charm.icon",
                slotId = "ACCESSORY",
                qualityTierId = "RARE",
            )

        val model =
            present(
                equipment =
                    listOf(
                        EquipmentSlotSnapshot("WEAPON", weapon),
                        EquipmentSlotSnapshot("ACCESSORY", accessory),
                    ),
                selectedEquipmentSlotId = "ACCESSORY",
            )

        val typed = model.equipmentSlots.take(4)
        assertEquals(listOf("WEAPON", "OFF_HAND", "ARMOR", "ACCESSORY"), typed.map(EquipmentSlotCellModel::slotId))
        assertEquals(listOf("ui.sidebar.weapon", "ui.sidebar.off_hand", "ui.sidebar.armor", "ui.reward.slot.accessory"), typed.map(EquipmentSlotCellModel::labelKey))
        assertEquals("item.hunter_bow.icon", typed[0].itemIcon?.resolvedKey)
        assertEquals(DarkUiChromeVisualKeys.SLOT_SELECTED, typed[3].frame.resolvedKey)
        assertEquals("RARE", typed[3].qualityTierId)
        assertEquals("ACCESSORY", typed[3].tooltipAnchorId)

        val displayOnly = model.equipmentSlots.drop(4)
        assertEquals(5, displayOnly.size)
        assertTrue(displayOnly.all(EquipmentSlotCellModel::visualOnly))
        assertTrue(displayOnly.all { slot -> slot.slotId == null && slot.itemIcon == null })
    }

    @Test
    fun `inventory grid keeps entry index identity and dark fallback icon`() {
        val manifestMessages = mutableListOf<String>()
        val repeatedA = item(baseItemId = "healing_potion", iconKey = "item.healing_potion.icon", qualityTierId = "NORMAL")
        val repeatedB = item(baseItemId = "healing_potion", iconKey = "item.healing_potion.icon", qualityTierId = "MAGIC")
        val missingIcon = item(baseItemId = "debug_missing", iconKey = "item.missing.debug.icon", visualKey = "item.missing.debug.visual", qualityTierId = "RARE")

        val model =
            present(
                inventory =
                    listOf(
                        InventoryEntrySnapshot(index = 7, item = repeatedA),
                        InventoryEntrySnapshot(index = 3, item = repeatedB),
                        InventoryEntrySnapshot(index = 9, item = missingIcon),
                    ),
                inventorySelection = 9,
                visualResolver = sampleResolver(ManifestLogSink { message -> manifestMessages += message }),
            )

        val cells = model.inventoryGrid.cells.take(3)
        assertEquals(listOf(3, 7, 9), cells.map(InventoryGridCellModel::identityIndex))
        assertEquals(9, model.inventoryGrid.selectedInventoryIndex)
        assertEquals("inventory:9", cells[2].tooltipAnchorId)
        assertEquals(DarkUiChromeVisualKeys.SLOT_SELECTED, cells[2].frame.resolvedKey)
        assertEquals(UiCompanionVisualKeys.EMPTY_INVENTORY, cells[2].itemIcon?.resolvedKey)
        assertEquals("RARE", cells[2].qualityTierId)
        assertTrue(cells.all { cell -> cell.quantityText == null })
        assertTrue(manifestMessages.any { message -> "item.missing.debug.icon" in message }, manifestMessages.toString())
        assertTrue(manifestMessages.any { message -> "item.missing.debug.visual" in message }, manifestMessages.toString())
    }

    @Test
    fun `inventory overflow is paged while empty cells stay empty state only`() {
        val entries =
            (0..9).map { index ->
                InventoryEntrySnapshot(index = index, item = item(baseItemId = "healing_potion", iconKey = "item.healing_potion.icon"))
            }

        val model = present(inventory = entries, inventorySelection = 9)

        assertEquals(EquipmentInventoryOverflowPolicy.PAGED, model.inventoryGrid.overflowPolicy)
        assertEquals("2/2  PgUp/PgDn", model.inventoryGrid.pageLabel)
        assertEquals(listOf(8, 9), model.inventoryGrid.cells.take(2).map(InventoryGridCellModel::identityIndex))
        assertFalse(model.inventoryGrid.cells.drop(2).any { cell -> cell.itemIcon != null || cell.identityIndex != null })
        assertTrue(model.inventoryGrid.cells.drop(2).all { cell -> cell.frame.resolvedKey == DarkUiChromeVisualKeys.SLOT_EMPTY })
    }

    private fun present(
        equipment: List<EquipmentSlotSnapshot> = emptyList(),
        inventory: List<InventoryEntrySnapshot> = emptyList(),
        inventorySelection: Int = 0,
        selectedEquipmentSlotId: String? = null,
        visualResolver: VisualManifestResolver = sampleResolver(),
    ): EquipmentInventoryPresentation =
        EquipmentInventoryPresenter.present(
            EquipmentInventoryPresenterRequest(
                localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                visualResolver = visualResolver,
                equipment = equipment,
                inventory = inventory,
                inventorySelection = inventorySelection,
                selectedEquipmentSlotId = selectedEquipmentSlotId,
            ),
        )

    private fun item(
        baseItemId: String,
        iconKey: String?,
        visualKey: String? = null,
        slotId: String? = null,
        qualityTierId: String = "NORMAL",
    ): ItemRenderSnapshot =
        ItemRenderSnapshot(
            baseItemId = baseItemId,
            nameKey = "item.$baseItemId.name",
            typeId = slotId ?: "CONSUMABLE",
            visualKey = visualKey,
            iconKey = iconKey,
            slotId = slotId,
            qualityTierId = qualityTierId,
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
                            VisualManifestEntry("item.emerald_charm.icon", "icon_item", "dark-v1/items/item_emerald_charm_icon.png", "ui"),
                            VisualManifestEntry("item.healing_potion.icon", "icon_item", "dark-v1/items/item_healing_potion_icon.png", "ui"),
                        ),
                    prefixRules = listOf(ManifestPrefixRule(prefix = "icon.", targetKey = "missing_visual")),
                ),
            logSink = logSink,
        )
}
