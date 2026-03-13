package com.ktome.game

import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.item.ItemDataBundle
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate

internal data class GameContent(
    val talents: List<TalentDef>,
    val talentRegistry: TalentRegistry,
    val monsterCatalog: List<MonsterTemplate>,
    val itemBundle: ItemDataBundle,
    val bossDefinition: BossDefinition,
)
