package com.ktome.game.harness

import com.ktome.core.item.EquipSlot
import com.ktome.game.InventoryItemView
import com.ktome.game.loot.foundationBuildIdentityForResourceType

object BuildIdentityAdoptionPolicy {
    fun emptySlotEquipThreshold(
        resourceTypeId: String,
        slot: EquipSlot?,
    ): Int =
        when (slot) {
            EquipSlot.WEAPON,
            EquipSlot.ARMOR,
            -> 0
            EquipSlot.OFF_HAND ->
                when (resourceTypeId) {
                    "HATE" -> 100
                    else -> 60
                }
            null -> Int.MAX_VALUE
        }

    fun offHandSlotScore(
        resourceTypeId: String,
        item: InventoryItemView,
    ): Int {
        val identity = foundationBuildIdentityForResourceType(resourceTypeId)
        if (resourceTypeId == "HATE") {
            return 0
        }
        if (item.baseItemId in identity?.nonWeaponCapstoneBaseIds.orEmpty()) {
            return 70
        }
        return when {
            EquipSlot.OFF_HAND in identity?.preferredReplacementSlots.orEmpty() -> 60
            resourceTypeId == "MANA" -> 45
            else -> 50
        }
    }

    fun preferredWeaponScore(
        resourceTypeId: String,
        item: InventoryItemView,
    ): Int {
        if (item.slot != EquipSlot.WEAPON) {
            return 0
        }
        return when (resourceTypeId) {
            "STAMINA" ->
                when (item.baseItemId) {
                    "forgebreaker_pick" -> 55
                    "battle_axe" -> 45
                    "long_sword" -> 35
                    "war_maul" -> 15
                    else -> 0
                }
            "HATE" ->
                when (item.baseItemId) {
                    "war_maul" -> 60
                    "battle_axe" -> 12
                    else -> 0
                }
            "MANA" ->
                when (item.baseItemId) {
                    "arcane_staff" -> 45
                    "battle_axe" -> -95
                    "war_maul" -> -90
                    "forgebreaker_pick" -> -70
                    "long_sword" -> -25
                    else -> 0
                }
            "ENERGY" ->
                when (item.baseItemId) {
                    "short_sword" -> 45
                    "hunter_bow" -> 42
                    "battle_axe" -> -85
                    "war_maul" -> -90
                    "forgebreaker_pick" -> -65
                    "long_sword" -> -20
                    else -> 0
                }
            "POSITIVE_ENERGY" ->
                when (item.baseItemId) {
                    "long_sword" -> 42
                    "battle_axe" -> 22
                    "war_maul" -> 8
                    "forgebreaker_pick" -> 10
                    else -> 0
                }
            else -> 0
        }
    }
}
