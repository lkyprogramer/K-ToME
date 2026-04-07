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
import com.ktome.game.data.schema.LootProfileSchemaV2
import com.ktome.game.data.schema.StatusSchemaV2
import com.ktome.game.i18n.Localizer
import com.ktome.game.elites.BossVariantRegistry
import com.ktome.game.elites.EliteMutationRegistry
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate
import com.ktome.game.telegraph.TelegraphRegistry
import com.ktome.game.telegraph.ThreatProfileRegistry
import com.ktome.game.data.schema.ZoneSchemaV2

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

internal class RoutedMapgenPipeline(
    zones: Collection<ZoneSchemaV2>,
    private val migratedZonePipeline: MapgenPipeline,
    private val compatibilityPipeline: MapgenPipeline,
) : MapgenPipeline {
    private val migratedZoneIds: Set<String> =
        zones.asSequence()
            .filter { zone -> zone.mapgenProfileId != null }
            .map(ZoneSchemaV2::id)
            .toSet()

    override fun run(request: com.ktome.core.mapgen.MapgenRequest): com.ktome.core.mapgen.GeneratedFloor =
        if (request.zoneId in migratedZoneIds) {
            migratedZonePipeline.run(request)
        } else {
            compatibilityPipeline.run(request)
        }
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
            RoutedMapgenPipeline(
                zones = schemaCatalog.zones,
                migratedZonePipeline =
                    HybridTopologyMapgenPipeline(
                        profileResolver = zoneMapgenProfileResolver,
                        contentCatalog = catalog,
                    ),
                compatibilityPipeline = BspBackedMapgenPipeline(profileResolver = zoneMapgenProfileResolver),
            )
        } ?: BspBackedMapgenPipeline(profileResolver = zoneMapgenProfileResolver),
) {
    val aiProfilesById: Map<String, AIProfile> = schemaCatalog.aiProfiles.associateBy(AIProfile::id)
    val racesById: Map<String, RaceDef> = races.associateBy(RaceDef::id)
    val lootProfilesById: Map<String, LootProfileSchemaV2> = schemaCatalog.lootProfiles.associateBy(LootProfileSchemaV2::id)
    val eliteMutationRegistry: EliteMutationRegistry =
        EliteMutationRegistry(
            config = schemaCatalog.eliteMutationConfig,
            statModifiersById = schemaCatalog.mutationStatModifiers.associateBy { modifier -> modifier.id },
            definitionsById = schemaCatalog.eliteMutations.associateBy { mutation -> mutation.id },
        )
    val bossVariantRegistry: BossVariantRegistry =
        BossVariantRegistry(
            variantsByBaseEncounterId = schemaCatalog.bossVariants.groupBy { variant -> variant.baseEncounterId },
            variantsById = schemaCatalog.bossVariants.associateBy { variant -> variant.id },
        )
    val actionWeightProfilesById: Map<String, com.ktome.game.elites.ActionWeightProfileDef> =
        schemaCatalog.actionWeightProfiles.associateBy { profile -> profile.id }

    init {
        schemaCatalog.bossVariants.forEach { variant ->
            require(bossDefinitions.containsKey(variant.baseEncounterId)) {
                "Boss variant '${variant.id}' references unknown base encounter '${variant.baseEncounterId}'."
            }
            variant.grantedMutations.forEach { mutationRef ->
                require(eliteMutationRegistry.resolve(mutationRef.mutationId) != null) {
                    "Boss variant '${variant.id}' references unknown mutation '${mutationRef.mutationId}'."
                }
            }
            variant.lootProfileOverride?.let { lootProfileId ->
                require(lootProfileId in lootProfilesById) {
                    "Boss variant '${variant.id}' references unknown loot profile '$lootProfileId'."
                }
            }
            require(variant.actionWeightProfileId == null || variant.actionWeightProfileId in actionWeightProfilesById) {
                "Boss variant '${variant.id}' references unknown action-weight profile '${variant.actionWeightProfileId}'."
            }
            variant.actionWeightProfileId?.let { actionWeightProfileId ->
                val weightProfile = requireNotNull(actionWeightProfilesById[actionWeightProfileId])
                val allowedActionIds = baseEncounterActionIds(variant.baseEncounterId)
                val unknownActionIds = weightProfile.actionWeights.keys - allowedActionIds
                require(unknownActionIds.isEmpty()) {
                    "Boss variant '${variant.id}' action-weight profile '$actionWeightProfileId' references unknown base-encounter actions ${unknownActionIds.sorted()}."
                }
            }
        }
    }

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

    fun lootProfile(profileId: String): LootProfileSchemaV2? = lootProfilesById[profileId]

    private fun baseEncounterActionIds(baseEncounterId: String): Set<String> =
        schemaCatalog.bossEncounters
            .firstOrNull { encounter -> encounter.id == baseEncounterId }
            ?.phases
            .orEmpty()
            .flatMap { phase ->
                requireNotNull(aiProfilesById[phase.aiProfileId]) {
                    "Boss phase '${phase.id}' in encounter '$baseEncounterId' references unknown AI profile '${phase.aiProfileId}'."
                }.actions.map { action -> action.id }
            }.toSet()
}
