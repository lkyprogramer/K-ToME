package com.ktome.game.loot

import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemType
import com.ktome.core.item.SpecialItemTemplate
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTier
import com.ktome.game.data.schema.LootPoolStrategy
import com.ktome.game.data.schema.LootProfileSchemaV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LootProfileCandidatePoolResolverTest {
    private val resolver = LootProfileCandidatePoolResolver(itemBundle = testItemBundle())

    @Test
    fun `tag preferred special templates respect exclude ids`() {
        val profile =
            LootProfileSchemaV3(
                id = "loot.secret.test",
                schemaVersion = 3,
                tags = listOf("loot", "secret"),
                itemIds = emptyList(),
                rewardBudget = 5,
                poolStrategy = LootPoolStrategy.TAG_WEIGHTED,
                itemTagFilter = listOf("zone_tag"),
                excludeIds = listOf("excluded_artifact_base"),
                specialTemplateTagPreference = listOf("zone_tag"),
            )

        val resolved = resolver.resolve(profile)

        assertEquals(setOf("allowed_artifact_base", "zone_potion"), resolved.allCandidateBaseIds)
        assertEquals(setOf("template.allowed"), resolved.preferredSpecialTemplateIds)
    }

    @Test
    fun `explicit preferred special templates respect exclude ids`() {
        val profile =
            LootProfileSchemaV3(
                id = "loot.secret.explicit",
                schemaVersion = 3,
                tags = listOf("loot", "secret"),
                itemIds = listOf("allowed_artifact_base", "excluded_artifact_base", "zone_potion"),
                rewardBudget = 5,
                poolStrategy = LootPoolStrategy.FIXED_LIST,
                excludeIds = listOf("excluded_artifact_base"),
                specialTemplateTagPreference = listOf("zone_tag"),
            )

        val resolved = resolver.resolve(profile)

        assertEquals(setOf("template.allowed"), resolved.preferredSpecialTemplateIds)
    }

    private fun testItemBundle(): ItemDataBundle =
        ItemDataBundle(
            baseItems =
                listOf(
                    baseItem(id = "allowed_artifact_base", tags = setOf("zone_tag")),
                    baseItem(id = "excluded_artifact_base", tags = setOf("zone_tag")),
                    baseItem(id = "zone_potion", type = ItemType.CONSUMABLE, tags = setOf("zone_tag")),
                ),
            materials = emptyList(),
            affixes = emptyList(),
            specialTemplates =
                listOf(
                    specialTemplate(
                        id = "template.allowed",
                        itemId = "allowed_artifact_base",
                        tags = setOf("zone_tag"),
                    ),
                    specialTemplate(
                        id = "template.excluded",
                        itemId = "excluded_artifact_base",
                        tags = setOf("zone_tag"),
                    ),
                ),
        )

    private fun baseItem(
        id: String,
        type: ItemType = ItemType.WEAPON,
        tags: Set<String>,
    ): ItemBaseDef =
        ItemBaseDef(
            id = id,
            name = id,
            type = type,
            tags = tags,
            glyph = if (type == ItemType.CONSUMABLE) '!' else ')',
            colorHex = "#FFFFFF",
            effect = if (type == ItemType.CONSUMABLE) ConsumableEffect.HEAL else null,
            magnitude = if (type == ItemType.CONSUMABLE) 20 else 0,
        )

    private fun specialTemplate(
        id: String,
        itemId: String,
        tags: Set<String>,
    ): SpecialItemTemplate =
        SpecialItemTemplate(
            id = id,
            itemId = itemId,
            specialTier = SpecialTier.ARTIFACT,
            nameKey = "$id.name",
            descKey = "$id.desc",
            visualKey = "$id.visual",
            iconKey = "$id.icon",
            audioProfile = "$id.audio",
            schemaVersion = 2,
            tags = tags,
            allowedSourceTiers = setOf(SourceTier.SECRET_ZONE),
            allowedZones = setOf("test_zone"),
        )
}
