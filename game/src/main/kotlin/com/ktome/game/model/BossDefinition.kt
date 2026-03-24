package com.ktome.game.model

import com.ktome.core.ai.BossEncounter

data class BossDefinition(
    val encounterId: String,
    val encounter: BossEncounter,
    val template: MonsterTemplate,
    val talentLevels: Map<String, Int>,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
)
