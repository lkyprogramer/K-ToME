package com.ktome.game.model

data class BossDefinition(
    val template: MonsterTemplate,
    val talentLevels: Map<String, Int>,
)
