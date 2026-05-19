package com.ktome.tools.loot

import com.ktome.core.item.StatModifier

internal fun statModifierSignature(modifier: StatModifier): String =
    listOf(
        modifier.str,
        modifier.dex,
        modifier.con,
        modifier.wil,
        modifier.attack,
        modifier.defense,
        modifier.accuracy,
        modifier.evasion,
        modifier.speed,
        modifier.castSpeedRating,
        modifier.maxHp,
        modifier.maxStamina,
        modifier.hpRegen,
        modifier.staminaRegen,
        modifier.critChance,
        modifier.talentPower,
        modifier.attackMultiplierBonus,
        modifier.defenseMultiplierBonus,
    ).joinToString(separator = ":")
