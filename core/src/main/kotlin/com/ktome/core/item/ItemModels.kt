package com.ktome.core.item

import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.EntityId
import com.ktome.core.loot.RarityTier

enum class EquipSlot {
    WEAPON,
    OFF_HAND,
    ARMOR,
}

enum class ItemType {
    WEAPON,
    ARMOR,
    CONSUMABLE,
}

enum class AffixType {
    PREFIX,
    SUFFIX,
}

enum class AffixEquipType {
    WEAPON,
    ARMOR,
}

enum class ConsumableEffect {
    HEAL,
    TELEPORT,
    RESTORE_RESOURCE,
}

sealed interface EquipmentPassive {
    data class DamageVsTag(
        val tag: String,
        val bonusPercent: Double,
    ) : EquipmentPassive

    data class DamageVsStatus(
        val statusId: String,
        val bonusPercent: Double,
    ) : EquipmentPassive

    data class HpRegenPerTurn(
        val amount: Int,
    ) : EquipmentPassive

    data class DamageTypeBonus(
        val type: DamageType,
        val bonusPercent: Double,
    ) : EquipmentPassive

    data class ResistanceBonus(
        val damageType: DamageType,
        val amount: Int,
    ) : EquipmentPassive
}

data class StatModifier(
    val str: Int = 0,
    val dex: Int = 0,
    val con: Int = 0,
    val wil: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val accuracy: Int = 0,
    val evasion: Int = 0,
    val speed: Int = 0,
    val maxHp: Int = 0,
    val maxStamina: Int = 0,
    val hpRegen: Double = 0.0,
    val staminaRegen: Double = 0.0,
    val critChance: Double = 0.0,
    val talentPower: Double = 0.0,
    val attackMultiplierBonus: Double = 0.0,
    val defenseMultiplierBonus: Double = 0.0,
) {
    operator fun plus(other: StatModifier): StatModifier =
        StatModifier(
            str = str + other.str,
            dex = dex + other.dex,
            con = con + other.con,
            wil = wil + other.wil,
            attack = attack + other.attack,
            defense = defense + other.defense,
            accuracy = accuracy + other.accuracy,
            evasion = evasion + other.evasion,
            speed = speed + other.speed,
            maxHp = maxHp + other.maxHp,
            maxStamina = maxStamina + other.maxStamina,
            hpRegen = hpRegen + other.hpRegen,
            staminaRegen = staminaRegen + other.staminaRegen,
            critChance = critChance + other.critChance,
            talentPower = talentPower + other.talentPower,
            attackMultiplierBonus = attackMultiplierBonus + other.attackMultiplierBonus,
            defenseMultiplierBonus = defenseMultiplierBonus + other.defenseMultiplierBonus,
        )

    companion object {
        val ZERO: StatModifier = StatModifier()
    }
}

data class AffixDef(
    val id: String,
    val name: String,
    val type: AffixType,
    val equipType: AffixEquipType = AffixEquipType.WEAPON,
    val tier: Int = 1,
    val statModifiers: StatModifier,
    val minFloor: Int = 1,
    val tags: Set<String> = emptySet(),
    val blacklistTags: Set<String> = emptySet(),
    val passive: EquipmentPassive? = null,
)

data class MaterialDef(
    val id: String,
    val name: String,
    val minFloor: Int = 1,
    val statModifiers: StatModifier = StatModifier(),
)

data class ItemBaseDef(
    val id: String,
    val name: String,
    val type: ItemType,
    val slot: EquipSlot? = null,
    val tags: Set<String> = emptySet(),
    val glyph: Char,
    val colorHex: String,
    val baseStats: StatModifier = StatModifier(),
    val allowedMaterials: List<String> = emptyList(),
    val dropFloors: List<Int> = listOf(1),
    val dropWeight: Int = 1,
    val effect: ConsumableEffect? = null,
    val resourceTypeId: String? = null,
    val magnitude: Int = 0,
    val passive: EquipmentPassive? = null,
)

data class ItemDataBundle(
    val baseItems: List<ItemBaseDef>,
    val materials: List<MaterialDef>,
    val affixes: List<AffixDef>,
)

data class ItemInstance(
    val baseId: String,
    val name: String,
    val type: ItemType,
    val slot: EquipSlot? = null,
    val glyph: Char,
    val colorHex: String,
    val quality: RarityTier = RarityTier.NORMAL,
    val materialId: String? = null,
    val materialName: String? = null,
    val affixes: List<AffixDef> = emptyList(),
    val stats: StatModifier = StatModifier(),
    val effect: ConsumableEffect? = null,
    val resourceTypeId: String? = null,
    val magnitude: Int = 0,
    val passive: EquipmentPassive? = null,
)

fun RarityTier.defaultAffixCount(): Int =
    when (this) {
        RarityTier.NORMAL -> 0
        RarityTier.MAGIC -> 1
        RarityTier.RARE -> 2
    }

data class Inventory(
    val capacity: Int = 12,
    val itemIds: MutableList<EntityId> = mutableListOf(),
)

data class Equipment(
    val slots: MutableMap<EquipSlot, EntityId> = linkedMapOf(),
)

data object GroundItem
