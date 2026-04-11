package com.ktome.game.model

import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.Stats

data class MonsterCatalog(
    val monsters: List<MonsterTemplate>,
)

data class MonsterTemplate(
    val id: String,
    val name: String,
    val glyph: Char,
    val colorHex: String,
    val stats: Stats,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseAccuracy: Int = 10,
    val baseEvasion: Int = 5,
    val speed: Int,
    val ai: AIType,
    val expReward: Int,
    val spawnFloors: List<Int>,
    val spawnWeight: Int,
    val archetype: String = "unknown",
    val tags: List<String> = emptyList(),
    val visualKey: String = "",
    val iconKey: String = "",
    val audioProfile: String = "",
    val aiProfileId: String = "",
    val lootProfileId: String = "",
    val resistances: Map<DamageType, Int> = emptyMap(),
    val talentLevels: Map<String, Int> = emptyMap(),
)

internal fun MonsterTemplate.isEliteEncounterTemplate(): Boolean =
    "elite" in tags || lootProfileId.endsWith(".elite")
