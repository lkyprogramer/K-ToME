package com.ktome.game.model

data class BossDefinition(
    val encounterId: String,
    val template: MonsterTemplate,
    val talentLevels: Map<String, Int>,
)
