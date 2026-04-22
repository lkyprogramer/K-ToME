package com.ktome.tools.lint

import com.ktome.client.assets.VisualManifestResolver
import com.ktome.game.data.schema.ItemBundleSchemaV2

internal data class ItemIconKeyCoverageFinding(
    val ownerId: String,
    val iconKey: String?,
    val reason: String,
)

internal object ItemIconKeyCoverageRule {
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
                }
            }
            (itemBundle.uniqueTemplates + itemBundle.artifactTemplates).forEach { template ->
                if (template.iconKey.isBlank()) {
                    add(ItemIconKeyCoverageFinding(ownerId = template.id, iconKey = template.iconKey, reason = "missing-special-icon-key"))
                } else if (!visualResolver.canResolve(template.iconKey)) {
                    add(ItemIconKeyCoverageFinding(ownerId = template.id, iconKey = template.iconKey, reason = "unresolved-special-icon-key"))
                }
            }
        }
}
