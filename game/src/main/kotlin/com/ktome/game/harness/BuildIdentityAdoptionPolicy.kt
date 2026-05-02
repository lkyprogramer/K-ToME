package com.ktome.game.harness

import com.ktome.core.item.EquipSlot
import com.ktome.game.FOUNDATION_SYNERGY_AFFIX_IDS
import com.ktome.game.InventoryItemView
import com.ktome.game.data.DataLoader
import com.ktome.game.loot.FoundationProfessionBuildIdentity
import com.ktome.game.loot.foundationBuildIdentityByProfessionId
import com.ktome.game.loot.foundationBuildIdentityForResourceType

object BuildIdentityAdoptionPolicy {
    private val foundationProfessionIds: Set<String>
        get() = foundationBuildIdentityByProfessionId.keys

    private val itemTagsByBaseItemId: Map<String, Set<String>> by lazy {
        DataLoader().loadItemBundle().baseItems.associate { item ->
            item.id to item.tags.mapTo(linkedSetOf()) { tag -> tag.trim().lowercase() }
        }
    }

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

    fun isProfessionCapstone(
        resourceTypeId: String,
        item: InventoryItemView,
    ): Boolean {
        val identity = foundationBuildIdentityForResourceType(resourceTypeId) ?: return false
        return item.baseItemId in identity.capstoneBaseIds ||
            item.baseItemId in identity.nonWeaponCapstoneBaseIds
    }

    fun isOtherProfessionCapstone(
        resourceTypeId: String,
        item: InventoryItemView,
    ): Boolean {
        val identity = foundationBuildIdentityForResourceType(resourceTypeId) ?: return false
        return foundationBuildIdentityByProfessionId.values
            .filter { candidate -> candidate.professionId != identity.professionId }
            .any { candidate ->
                item.baseItemId in candidate.capstoneBaseIds ||
                    item.baseItemId in candidate.nonWeaponCapstoneBaseIds
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
                    "forgebreaker_pick" -> 150
                    "battle_axe" -> 45
                    "long_sword" -> 130
                    "hunter_bow" -> -90
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
                    "arcane_staff" -> 160
                    "battle_axe" -> -260
                    "war_maul" -> -260
                    "forgebreaker_pick" -> -260
                    "long_sword" -> -220
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
            "POSITIVE_ENERGY" -> buildIdentityWeaponScore(resourceTypeId = resourceTypeId, item = item)
            else -> 0
        }
    }

    private fun buildIdentityWeaponScore(
        resourceTypeId: String,
        item: InventoryItemView,
    ): Int {
        val identity = foundationBuildIdentityForResourceType(resourceTypeId) ?: return 0
        val itemTags = itemTagsByBaseItemId[item.baseItemId].orEmpty()
        val otherProfessionPenalty =
            if (itemTags.any { tag -> tag in foundationProfessionIds && tag != identity.professionId }) {
                -120
            } else {
                0
            }
        return otherProfessionPenalty +
            dominantRiskPenalty(identity, itemTags) +
            professionTagBonus(identity, itemTags) +
            terminalIdentityTagBonus(identity, itemTags) +
            synergyAffixBonus(identity, item)
    }

    private fun dominantRiskPenalty(
        identity: FoundationProfessionBuildIdentity,
        itemTags: Set<String>,
    ): Int =
        if ("dominant_risk" in itemTags && identity.professionId != "vanguard") {
            -90
        } else {
            0
        }

    private fun professionTagBonus(
        identity: FoundationProfessionBuildIdentity,
        itemTags: Set<String>,
    ): Int =
        if (identity.professionId in itemTags) {
            45
        } else {
            0
        }

    private fun terminalIdentityTagBonus(
        identity: FoundationProfessionBuildIdentity,
        itemTags: Set<String>,
    ): Int = (itemTags.count(identity.terminalIdentityTags::contains) * 30).coerceAtMost(90)

    private fun synergyAffixBonus(
        identity: FoundationProfessionBuildIdentity,
        item: InventoryItemView,
    ): Int {
        val synergyAffixIds = FOUNDATION_SYNERGY_AFFIX_IDS[identity.professionId].orEmpty()
        return if (item.affixIds.any(synergyAffixIds::contains)) {
            35
        } else {
            0
        }
    }
}
