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

    @Test
    fun `profession equipment cannot reuse unrelated item icons`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val itemBundle =
            catalog.itemBundle.copy(
                items =
                    catalog.itemBundle.items.map { item ->
                        if (item.id == "hunter_bow") {
                            item.copy(iconKey = "item.long_sword.icon")
                        } else {
                            item
                        }
                    },
                uniqueTemplates = emptyList(),
                artifactTemplates = emptyList(),
            )
        val assets = ClientAssetBundleLoader.load()

        val findings = ItemIconKeyCoverageRule.validate(itemBundle, assets.visualResolver)

        assertEquals(listOf("profession-equipment-uses-unrelated-icon"), findings.map(ItemIconKeyCoverageFinding::reason))
    }

    @Test
    fun `off-hand accessories must use off-hand base or dedicated icons`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val itemBundle =
            catalog.itemBundle.copy(
                items =
                    catalog.itemBundle.items.map { item ->
                        if (item.id == "bandit_trophy") {
                            item.copy(iconKey = "item.base.rogue.weapon.icon")
                        } else {
                            item
                        }
                    },
                uniqueTemplates = emptyList(),
                artifactTemplates = emptyList(),
            )
        val assets = ClientAssetBundleLoader.load()

        val findings = ItemIconKeyCoverageRule.validate(itemBundle, assets.visualResolver)

        assertEquals(listOf("base-icon-category-mismatch"), findings.map(ItemIconKeyCoverageFinding::reason))
    }

    @Test
    fun `special items and templates cannot use base icons`() {
        val catalog = DataLoader().loadSchemaCatalog()
        val itemBundle =
            catalog.itemBundle.copy(
                items =
                    catalog.itemBundle.items.map { item ->
                        if (item.id == "unique_thornpath_crook") {
                            item.copy(iconKey = "item.base.rogue.weapon.icon")
                        } else {
                            item
                        }
                    },
                uniqueTemplates =
                    catalog.itemBundle.uniqueTemplates.take(1).map { template ->
                        template.copy(iconKey = "item.base.rogue.weapon.icon")
                    },
                artifactTemplates = emptyList(),
            )
        val assets = ClientAssetBundleLoader.load()

        val findings = ItemIconKeyCoverageRule.validate(itemBundle, assets.visualResolver)

        assertEquals(
            listOf("special-item-uses-base-icon", "special-template-uses-base-icon"),
            findings.map(ItemIconKeyCoverageFinding::reason),
        )
    }
}
