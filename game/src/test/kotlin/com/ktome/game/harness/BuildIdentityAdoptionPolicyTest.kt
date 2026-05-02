package com.ktome.game.harness

import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemType
import com.ktome.core.loot.RarityTier
import com.ktome.game.InventoryItemView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BuildIdentityAdoptionPolicyTest {
    @Test
    fun `off hand adoption scoring respects build identity capstones and preferred slots`() {
        assertEquals(
            70,
            BuildIdentityAdoptionPolicy.offHandSlotScore(
                resourceTypeId = "ENERGY",
                item = gearItem(baseItemId = "artifact_briar_heart", slot = EquipSlot.OFF_HAND),
            ),
        )
        assertEquals(
            60,
            BuildIdentityAdoptionPolicy.offHandSlotScore(
                resourceTypeId = "MANA",
                item = gearItem(baseItemId = "basic_shield", slot = EquipSlot.OFF_HAND),
            ),
        )
        assertEquals(
            50,
            BuildIdentityAdoptionPolicy.offHandSlotScore(
                resourceTypeId = "STAMINA",
                item = gearItem(baseItemId = "basic_shield", slot = EquipSlot.OFF_HAND),
            ),
        )
    }

    @Test
    fun `adoption policy keeps resource specific thresholds and weapon preferences centralized`() {
        assertEquals(100, BuildIdentityAdoptionPolicy.emptySlotEquipThreshold(resourceTypeId = "HATE", slot = EquipSlot.OFF_HAND))
        assertEquals(60, BuildIdentityAdoptionPolicy.emptySlotEquipThreshold(resourceTypeId = "ENERGY", slot = EquipSlot.OFF_HAND))
        assertEquals(
            45,
            BuildIdentityAdoptionPolicy.preferredWeaponScore(
                resourceTypeId = "ENERGY",
                item = gearItem(baseItemId = "short_sword", slot = EquipSlot.WEAPON),
            ),
        )
        assertEquals(
            -85,
            BuildIdentityAdoptionPolicy.preferredWeaponScore(
                resourceTypeId = "ENERGY",
                item = gearItem(baseItemId = "battle_axe", slot = EquipSlot.WEAPON),
            ),
        )
        assertEquals(
            -90,
            BuildIdentityAdoptionPolicy.preferredWeaponScore(
                resourceTypeId = "STAMINA",
                item = gearItem(baseItemId = "hunter_bow", slot = EquipSlot.WEAPON),
            ),
        )
        assertEquals(
            -260,
            BuildIdentityAdoptionPolicy.preferredWeaponScore(
                resourceTypeId = "MANA",
                item = gearItem(baseItemId = "forgebreaker_pick", slot = EquipSlot.WEAPON),
            ),
        )
        assertEquals(
            160,
            BuildIdentityAdoptionPolicy.preferredWeaponScore(
                resourceTypeId = "MANA",
                item = gearItem(baseItemId = "arcane_staff", slot = EquipSlot.WEAPON),
            ),
        )
        assertEquals(
            60,
            BuildIdentityAdoptionPolicy.preferredWeaponScore(
                resourceTypeId = "POSITIVE_ENERGY",
                item = gearItem(baseItemId = "long_sword", slot = EquipSlot.WEAPON),
            ),
        )
        assertEquals(
            -120,
            BuildIdentityAdoptionPolicy.preferredWeaponScore(
                resourceTypeId = "POSITIVE_ENERGY",
                item = gearItem(baseItemId = "hunter_bow", slot = EquipSlot.WEAPON),
            ),
        )
        assertEquals(
            95,
            BuildIdentityAdoptionPolicy.preferredWeaponScore(
                resourceTypeId = "POSITIVE_ENERGY",
                item = gearItem(baseItemId = "long_sword", slot = EquipSlot.WEAPON, affixIds = listOf("of_smite")),
            ),
        )
        assertEquals(
            -85,
            BuildIdentityAdoptionPolicy.preferredWeaponScore(
                resourceTypeId = "POSITIVE_ENERGY",
                item = gearItem(baseItemId = "hunter_bow", slot = EquipSlot.WEAPON, affixIds = listOf("of_smite")),
            ),
        )
    }

    private fun gearItem(
        baseItemId: String,
        slot: EquipSlot,
        affixIds: List<String> = emptyList(),
    ): InventoryItemView =
        InventoryItemView(
            index = 0,
            name = baseItemId,
            baseItemId = baseItemId,
            type = if (slot == EquipSlot.WEAPON) ItemType.WEAPON else ItemType.ARMOR,
            slot = slot,
            quality = RarityTier.NORMAL,
            affixIds = affixIds,
            effect = ConsumableEffect.HEAL,
        )
}
