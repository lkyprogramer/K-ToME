package com.ktome.game

import com.ktome.core.ai.AIProfile
import com.ktome.core.inscription.InscriptionDef
import com.ktome.core.mapgen.BspBackedMapgenPipeline
import com.ktome.core.mapgen.HybridTopologyMapgenPipeline
import com.ktome.core.mapgen.MapgenContentCatalog
import com.ktome.core.mapgen.MapgenPipeline
import com.ktome.core.mapgen.ZoneMapgenProfile
import com.ktome.core.mapgen.ZoneMapgenProfileResolver
import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.mapgen.ZoneRewardProfileResolver
import com.ktome.core.race.RaceDef
import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.status.StatusCatalog
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.StatusSchemaV2
import com.ktome.game.i18n.Localizer
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate
import com.ktome.game.telegraph.TelegraphRegistry
import com.ktome.game.telegraph.ThreatProfileRegistry

private object EmptyZoneMapgenProfileResolver : ZoneMapgenProfileResolver {
    override fun resolve(zoneId: String): ZoneMapgenProfile =
        ZoneMapgenProfile(
            id = "$zoneId.compatibility",
            zoneId = zoneId,
            allowedBiomeFamilies = setOf("fallback.compatibility"),
            loopCountRange = 0..0,
            vaultPool = emptySet(),
            terrainTagWeights = emptyMap(),
            roomTagFilter = emptySet(),
        )
}

private object EmptyZoneRewardProfileResolver : ZoneRewardProfileResolver {
    override fun resolve(zoneId: String): ZoneRewardProfile =
        ZoneRewardProfile(
            id = "$zoneId.compatibility",
            zoneId = zoneId,
            rarityBonus = 0.0f,
            qualityBonus = 0,
            baseRewardBudget = 0,
        )
}

internal data class GameContent(
    val talents: List<TalentDef>,
    val statuses: List<StatusSchemaV2>,
    val statusCatalog: StatusCatalog,
    val talentRegistry: TalentRegistry,
    val races: List<RaceDef> = emptyList(),
    val inscriptions: List<InscriptionDef> = emptyList(),
    val monsterCatalog: List<MonsterTemplate>,
    val itemBundle: ItemDataBundle,
    val bossDefinitions: Map<String, BossDefinition>,
    val schemaCatalog: SchemaCatalog,
    val localizer: Localizer,
    val telegraphRegistry: TelegraphRegistry = TelegraphRegistry(schemaCatalog.telegraphSpecs.associateBy { spec -> spec.id }),
    val threatProfileRegistry: ThreatProfileRegistry = ThreatProfileRegistry(schemaCatalog.threatProfiles.associateBy { profile -> profile.id }),
    val zoneMapgenProfileResolver: ZoneMapgenProfileResolver = EmptyZoneMapgenProfileResolver,
    val zoneRewardProfileResolver: ZoneRewardProfileResolver = EmptyZoneRewardProfileResolver,
    val mapgenContentCatalog: MapgenContentCatalog? = null,
    val mapgenPipeline: MapgenPipeline =
        mapgenContentCatalog?.let { catalog ->
            HybridTopologyMapgenPipeline(
                profileResolver = zoneMapgenProfileResolver,
                contentCatalog = catalog,
            )
        } ?: BspBackedMapgenPipeline(profileResolver = zoneMapgenProfileResolver),
) {
    val aiProfilesById: Map<String, AIProfile> = schemaCatalog.aiProfiles.associateBy(AIProfile::id)
    val racesById: Map<String, RaceDef> = races.associateBy(RaceDef::id)

    fun bossDefinitionForZone(zoneId: String): BossDefinition? =
        schemaCatalog.zones.firstOrNull { zone -> zone.id == zoneId }
            ?.bossEncounterId
            ?.let { encounterId -> bossDefinitions[encounterId] }

    fun bossTemplateIds(): Set<String> = bossDefinitions.values.map { definition -> definition.template.id }.toSet()

    fun allMonsterTemplates(): List<MonsterTemplate> =
        (monsterCatalog + bossDefinitions.values.map(BossDefinition::template)).distinctBy(MonsterTemplate::id)

    fun statusSchemaFor(statusId: String): StatusSchemaV2? =
        statuses.firstOrNull { schema -> schema.id == statusId || schema.effectType == statusId }

    fun telegraphSpecFor(telegraphRef: String?): com.ktome.core.ai.TelegraphSpec? =
        telegraphRef?.let(telegraphRegistry::resolve)

    fun aiProfile(profileId: String?): AIProfile? =
        profileId?.let(aiProfilesById::get)
}
