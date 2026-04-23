package com.ktome.tools.lint

import com.ktome.client.assets.VisualManifestResolver
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemType
import com.ktome.game.data.schema.ItemBundleSchemaV2
import com.ktome.game.data.schema.ItemSchemaV2
import com.ktome.game.data.schema.SpecialItemTemplateSchemaV2

internal data class ItemIconKeyCoverageFinding(
    val ownerId: String,
    val iconKey: String?,
    val reason: String,
)

internal object ItemIconKeyCoverageRule {
    private val professionTags = setOf("vanguard", "arcanist", "rogue", "templar")
    private val baseIconPattern = Regex("""^item\.base\.([a-z_]+)\.(weapon|armor|off_hand)\.icon$""")

    fun validate(
        itemBundle: ItemBundleSchemaV2,
        visualResolver: VisualManifestResolver,
    ): List<ItemIconKeyCoverageFinding> =
        buildList {
            itemBundle.items.forEach { item ->
                if (item.iconKey.isBlank()) {
                    add(ItemIconKeyCoverageFinding(ownerId = item.id, iconKey = item.iconKey, reason = "missing-item-icon-key"))
                } else if (!visualResolver.canResolve(item.iconKey)) {
                    add(ItemIconKeyCoverageFinding(ownerId = item.id, iconKey = item.iconKey, reason = "unresolved-item-icon-key"))
                } else {
                    addAll(validateEquipmentIconSemantics(item))
                }
            }
            (itemBundle.uniqueTemplates + itemBundle.artifactTemplates).forEach { template ->
                if (template.iconKey.isBlank()) {
                    add(ItemIconKeyCoverageFinding(ownerId = template.id, iconKey = template.iconKey, reason = "missing-special-icon-key"))
                } else if (!visualResolver.canResolve(template.iconKey)) {
                    add(ItemIconKeyCoverageFinding(ownerId = template.id, iconKey = template.iconKey, reason = "unresolved-special-icon-key"))
                } else {
                    addAll(validateSpecialTemplateIconSemantics(template))
                }
            }
        }

    private fun validateEquipmentIconSemantics(item: ItemSchemaV2): List<ItemIconKeyCoverageFinding> {
        if (item.type == ItemType.CONSUMABLE) {
            return emptyList()
        }

        val category = equipmentCategory(item) ?: return emptyList()
        val baseIcon = parseBaseIcon(item.iconKey)
        val itemProfessionTags = item.tags.map(String::lowercase).filterTo(linkedSetOf()) { tag -> tag in professionTags }
        val isSpecialItem = item.tags.any { tag -> tag.equals("unique", ignoreCase = true) || tag.equals("artifact", ignoreCase = true) }

        return buildList {
            if (isSpecialItem && baseIcon != null) {
                add(ItemIconKeyCoverageFinding(ownerId = item.id, iconKey = item.iconKey, reason = "special-item-uses-base-icon"))
            }

            if (baseIcon != null) {
                if (baseIcon.category != category) {
                    add(ItemIconKeyCoverageFinding(ownerId = item.id, iconKey = item.iconKey, reason = "base-icon-category-mismatch"))
                }
                if (itemProfessionTags.isNotEmpty() && baseIcon.profession !in itemProfessionTags) {
                    add(ItemIconKeyCoverageFinding(ownerId = item.id, iconKey = item.iconKey, reason = "base-icon-profession-mismatch"))
                }
                return@buildList
            }

            if (item.iconKey == "item.${item.id}.icon") {
                return@buildList
            }
            if (isSpecialItem && (item.iconKey.startsWith("item.unique.") || item.iconKey.startsWith("item.artifact."))) {
                return@buildList
            }

            if (itemProfessionTags.isNotEmpty()) {
                add(ItemIconKeyCoverageFinding(ownerId = item.id, iconKey = item.iconKey, reason = "profession-equipment-uses-unrelated-icon"))
            }
            if (category == "off_hand" && item.iconKey !in allowedOffHandDedicatedIcons(item.id)) {
                add(ItemIconKeyCoverageFinding(ownerId = item.id, iconKey = item.iconKey, reason = "offhand-equipment-uses-unrelated-icon"))
            }
        }
    }

    private fun validateSpecialTemplateIconSemantics(template: SpecialItemTemplateSchemaV2): List<ItemIconKeyCoverageFinding> =
        if (parseBaseIcon(template.iconKey) != null) {
            listOf(ItemIconKeyCoverageFinding(ownerId = template.id, iconKey = template.iconKey, reason = "special-template-uses-base-icon"))
        } else {
            emptyList()
        }

    private fun equipmentCategory(item: ItemSchemaV2): String? =
        when {
            item.tags.any { it.equals("accessory", ignoreCase = true) } || item.slot == EquipSlot.OFF_HAND -> "off_hand"
            item.type == ItemType.WEAPON || item.tags.any { it.equals("weapon", ignoreCase = true) } -> "weapon"
            item.type == ItemType.ARMOR || item.tags.any { it.equals("armor", ignoreCase = true) } -> "armor"
            else -> null
        }

    private fun parseBaseIcon(iconKey: String): EquipmentBaseIcon? {
        val match = baseIconPattern.matchEntire(iconKey) ?: return null
        return EquipmentBaseIcon(profession = match.groupValues[1], category = match.groupValues[2])
    }

    private fun allowedOffHandDedicatedIcons(itemId: String): Set<String> =
        setOf(
            "item.$itemId.icon",
            "item.basic_shield.icon",
            "item.abyssal_heartstone.icon",
        )

    private data class EquipmentBaseIcon(
        val profession: String,
        val category: String,
    )
}
