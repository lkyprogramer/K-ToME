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
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `weighting prefers strong build matches and soft caps dominant risk weapons`() {
        val profile =
            LootProfileSchemaV3(
                id = "loot.identity.test",
                schemaVersion = 3,
                tags = listOf("loot", "boss"),
                itemIds = listOf("battle_axe", "arcane_staff"),
                rewardBudget = 8,
                poolStrategy = LootPoolStrategy.FIXED_LIST,
                typeWeights = mapOf(ItemType.WEAPON to 4),
            )

        val resolved = resolver.resolve(profile)
        val arcanistContext =
            LootBaseSelectionContext(
                buildTags = setOf("mana", "spell", "ranged"),
            )
        val vanguardContext =
            LootBaseSelectionContext(
                buildTags = setOf("melee", "stamina", "guard"),
            )
        val battleAxe = testItemBundle().baseItems.first { item -> item.id == "battle_axe" }
        val arcaneStaff = testItemBundle().baseItems.first { item -> item.id == "arcane_staff" }

        val arcanistBattleAxeWeight = resolved.weightFor(battleAxe, arcanistContext)
        val arcanistArcaneStaffWeight = resolved.weightFor(arcaneStaff, arcanistContext)
        val vanguardBattleAxeWeight = resolved.weightFor(battleAxe, vanguardContext)
        val vanguardArcaneStaffWeight = resolved.weightFor(arcaneStaff, vanguardContext)

        assertTrue(arcanistArcaneStaffWeight > arcanistBattleAxeWeight)
        assertTrue(vanguardBattleAxeWeight > arcanistBattleAxeWeight)
        assertTrue(vanguardArcaneStaffWeight < arcanistArcaneStaffWeight)
    }

    @Test
    fun `templar build tags align frontline weapons without relying on profession name tags`() {
        val profile =
            LootProfileSchemaV3(
                id = "loot.templar.identity",
                schemaVersion = 3,
                tags = listOf("loot", "boss"),
                itemIds = listOf("battle_axe", "long_sword", "arcane_staff"),
                rewardBudget = 8,
                poolStrategy = LootPoolStrategy.FIXED_LIST,
                typeWeights = mapOf(ItemType.WEAPON to 4),
            )

        val resolved = resolver.resolve(profile)
        val templarContext =
            LootBaseSelectionContext(
                buildTags = setOf("holy", "smite", "melee", "frontline", "guard"),
            )
        val neutralContext = LootBaseSelectionContext.EMPTY
        val battleAxe = testItemBundle().baseItems.first { item -> item.id == "battle_axe" }
        val longSword = testItemBundle().baseItems.first { item -> item.id == "long_sword" }
        val arcaneStaff = testItemBundle().baseItems.first { item -> item.id == "arcane_staff" }

        val battleAxeWeight = resolved.weightFor(battleAxe, templarContext)
        val longSwordWeight = resolved.weightFor(longSword, templarContext)
        val arcaneStaffWeight = resolved.weightFor(arcaneStaff, templarContext)
        val neutralBattleAxeWeight = resolved.weightFor(battleAxe, neutralContext)

        assertTrue(longSwordWeight > arcaneStaffWeight)
        assertTrue(battleAxeWeight > neutralBattleAxeWeight)
    }

    private fun testItemBundle(): ItemDataBundle =
        ItemDataBundle(
            baseItems =
                listOf(
                    baseItem(id = "allowed_artifact_base", tags = setOf("zone_tag")),
                    baseItem(id = "excluded_artifact_base", tags = setOf("zone_tag")),
                    baseItem(id = "zone_potion", type = ItemType.CONSUMABLE, tags = setOf("zone_tag")),
                    baseItem(
                        id = "battle_axe",
                        tags = setOf("item", "weapon", "melee", "frontline", "stamina", "dominant_risk"),
                        dropWeight = 3,
                    ),
                    baseItem(
                        id = "long_sword",
                        tags = setOf("item", "weapon", "melee", "frontline", "guard", "discipline"),
                        dropWeight = 7,
                    ),
                    baseItem(
                        id = "arcane_staff",
                        tags = setOf("item", "weapon", "mana", "spell", "ranged", "channel"),
                        dropWeight = 8,
                    ),
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
        dropWeight: Int = 1,
    ): ItemBaseDef =
        ItemBaseDef(
            id = id,
            name = id,
            type = type,
            tags = tags,
            glyph = if (type == ItemType.CONSUMABLE) '!' else ')',
            colorHex = "#FFFFFF",
            dropWeight = dropWeight,
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
