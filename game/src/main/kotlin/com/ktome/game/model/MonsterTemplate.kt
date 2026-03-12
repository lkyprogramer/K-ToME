package com.ktome.game.model

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
    val speed: Int,
    val ai: AIType,
    val expReward: Int,
    val spawnFloors: List<Int>,
    val spawnWeight: Int,
)
