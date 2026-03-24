package com.ktome.game

import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.item.ItemDataBundle
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.StatusSchemaV2
import com.ktome.game.i18n.Localizer
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate

internal data class GameContent(
    val talents: List<TalentDef>,
    val statuses: List<StatusSchemaV2>,
    val talentRegistry: TalentRegistry,
    val monsterCatalog: List<MonsterTemplate>,
    val itemBundle: ItemDataBundle,
    val bossDefinitions: Map<String, BossDefinition>,
    val schemaCatalog: SchemaCatalog,
    val localizer: Localizer,
) {
    fun bossDefinitionForZone(zoneId: String): BossDefinition? =
        schemaCatalog.zones.firstOrNull { zone -> zone.id == zoneId }
            ?.bossEncounterId
            ?.let { encounterId -> bossDefinitions[encounterId] }

    fun bossTemplateIds(): Set<String> = bossDefinitions.values.map { definition -> definition.template.id }.toSet()

    fun allMonsterTemplates(): List<MonsterTemplate> =
        (monsterCatalog + bossDefinitions.values.map(BossDefinition::template)).distinctBy(MonsterTemplate::id)

    fun statusSchemaFor(statusId: String): StatusSchemaV2? =
        statuses.firstOrNull { schema -> schema.id == statusId || schema.effectType == statusId }
}
