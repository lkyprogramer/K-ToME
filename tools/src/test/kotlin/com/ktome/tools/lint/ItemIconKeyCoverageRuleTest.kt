package com.ktome.tools.lint

import com.ktome.client.assets.ClientAssetBundleLoader
import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("contractLint")
class ItemIconKeyCoverageRuleTest {
    @Test
    fun `official items and special templates resolve exact icon keys`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val assets = ClientAssetBundleLoader.load()

        val findings = ItemIconKeyCoverageRule.validate(catalog.itemBundle, assets.visualResolver)

        assertTrue(findings.isEmpty(), "Unexpected item icon coverage findings: $findings")
    }

    @Test
    fun `missing and unresolved icon keys are reported`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val itemBundle =
            catalog.itemBundle.copy(
                items =
                    catalog.itemBundle.items.take(1).map { item ->
                        item.copy(iconKey = "")
                    } +
                        catalog.itemBundle.items.drop(1).take(1).map { item ->
                            item.copy(iconKey = "item.test.missing.icon")
                        },
                uniqueTemplates = emptyList(),
                artifactTemplates = emptyList(),
            )
        val assets = ClientAssetBundleLoader.load()

        val findings = ItemIconKeyCoverageRule.validate(itemBundle, assets.visualResolver)

        assertEquals(listOf("missing-item-icon-key", "unresolved-item-icon-key"), findings.map(ItemIconKeyCoverageFinding::reason))
    }
}
