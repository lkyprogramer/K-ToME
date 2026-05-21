package com.ktome.core.snapshot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ItemRenderStackingTest {
    @Test
    fun `stackable render items return composed normalized identity`() {
        val item =
            renderItem(
                baseItemId = "healing_potion",
                typeId = "consumable",
                qualityTierId = "RARE",
                effectTypeId = "HEAL",
                resourceTypeId = "HEALTH",
                materialNameKey = "item.material.oak",
                magnitude = 12,
            )

        assertEquals(
            "healing_potion|CONSUMABLE|RARE|HEAL|HEALTH|item.material.oak|12",
            item.inventoryStackIdentity(),
        )
    }

    @Test
    fun `non stackable render item traits return null identity`() {
        val cases =
            listOf(
                renderItem(typeId = "WEAPON"),
                renderItem(slotId = "WEAPON"),
                renderItem(specialTemplateId = "unique.briarbound_bow", specialTierId = "UNIQUE"),
                renderItem(affixIds = listOf("briarhook")),
                renderItem(affixNameKeys = listOf("affix.briarhook.name")),
                renderItem(stats = ItemStatModifierSnapshot(str = 1)),
            )

        cases.forEach { item -> assertNull(item.inventoryStackIdentity()) }
    }

    @Test
    fun `identity fields produce distinct stack keys`() {
        val base = renderItem()
        val identities =
            listOf(
                base,
                base.copy(baseItemId = "mana_potion"),
                base.copy(qualityTierId = "RARE"),
                base.copy(effectTypeId = "RESTORE"),
                base.copy(resourceTypeId = "MANA"),
                base.copy(materialNameKey = "item.material.ash"),
                base.copy(magnitude = 2),
            ).map { item -> requireNotNull(item.inventoryStackIdentity()) }

        assertEquals(identities.size, identities.toSet().size)
    }

    private fun renderItem(
        baseItemId: String = "healing_potion",
        typeId: String = "CONSUMABLE",
        qualityTierId: String = "NORMAL",
        effectTypeId: String? = "HEAL",
        resourceTypeId: String? = "HEALTH",
        materialNameKey: String? = null,
        magnitude: Int = 1,
        slotId: String? = null,
        specialTemplateId: String? = null,
        specialTierId: String? = null,
        affixIds: List<String> = emptyList(),
        affixNameKeys: List<String> = emptyList(),
        stats: ItemStatModifierSnapshot = ItemStatModifierSnapshot(),
    ): ItemRenderSnapshot =
        ItemRenderSnapshot(
            baseItemId = baseItemId,
            nameKey = "item.$baseItemId.name",
            typeId = typeId,
            qualityTierId = qualityTierId,
            effectTypeId = effectTypeId,
            resourceTypeId = resourceTypeId,
            materialNameKey = materialNameKey,
            magnitude = magnitude,
            slotId = slotId,
            specialTemplateId = specialTemplateId,
            specialTierId = specialTierId,
            affixIds = affixIds,
            affixNameKeys = affixNameKeys,
            stats = stats,
        )
}
