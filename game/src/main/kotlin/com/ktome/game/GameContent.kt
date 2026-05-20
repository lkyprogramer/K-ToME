package com.ktome.game

import com.ktome.core.ai.AIProfile
import com.ktome.core.inscription.InscriptionDef
import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.mapgen.BspBackedMapgenPipeline
import com.ktome.core.mapgen.HybridTopologyMapgenPipeline
import com.ktome.core.mapgen.MapgenContentCatalog
import com.ktome.core.mapgen.MapgenPipeline
import com.ktome.core.mapgen.ZoneMapgenProfile
import com.ktome.core.mapgen.ZoneMapgenProfileResolver
import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.mapgen.ZoneRewardProfileResolver
import com.ktome.core.phase.PackId
import com.ktome.core.race.RaceDef
import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.status.StatusCatalog
import com.ktome.game.data.schema.ItemSchemaV2
import com.ktome.game.data.schema.LootProfileSchemaV3
import com.ktome.game.data.schema.MonsterSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.StatusSchemaV2
import com.ktome.game.data.schema.TalentSchemaV2
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.i18n.Localizer
import com.ktome.game.contentpack.ContentPackKeyResolutionSummary
import com.ktome.game.contentpack.ContentPackOverlaySummary
import com.ktome.game.elites.BossVariantPhaseOverrideContracts
import com.ktome.game.elites.BossVariantRegistry
import com.ktome.game.elites.EliteMutationRegistry
import com.ktome.game.hidden.HiddenContentMapgenPipeline
import com.ktome.game.hidden.HiddenEventRegistry
import com.ktome.game.hidden.SecretZoneRegistry
import com.ktome.game.loot.FoundationProfessionBuildIdentity
import com.ktome.game.loot.foundationBuildIdentityMapFor
import com.ktome.game.loot.LootProfileCandidatePool
import com.ktome.game.loot.LootProfileCandidatePoolResolver
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterTemplate
import com.ktome.game.model.isEliteEncounterTemplate
import com.ktome.game.telegraph.TelegraphRegistry
import com.ktome.game.telegraph.ThreatProfileRegistry

private object EmptyZoneMapgenProfileResolver : ZoneMapgenProfileResolver {
    override fun resolve(
        zoneId: String,
        floorIndex: Int,
    ): ZoneMapgenProfile =
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
    val activePackIds: List<PackId> = emptyList(),
    val activePackManifestVersions: Map<PackId, String> = emptyMap(),
    val activePackOverlaySummaries: List<ContentPackOverlaySummary> = emptyList(),
    val activePackKeyResolutionSummary: ContentPackKeyResolutionSummary = ContentPackKeyResolutionSummary(),
    val telegraphRegistry: TelegraphRegistry = TelegraphRegistry(schemaCatalog.telegraphSpecs.associateBy { spec -> spec.id }),
    val threatProfileRegistry: ThreatProfileRegistry = ThreatProfileRegistry(schemaCatalog.threatProfiles.associateBy { profile -> profile.id }),
    val zoneMapgenProfileResolver: ZoneMapgenProfileResolver = EmptyZoneMapgenProfileResolver,
    val zoneRewardProfileResolver: ZoneRewardProfileResolver = EmptyZoneRewardProfileResolver,
    val mapgenContentCatalog: MapgenContentCatalog? = null,
    val mapgenPipeline: MapgenPipeline =
        HiddenContentMapgenPipeline(
            delegate =
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
            secretZoneRegistry = SecretZoneRegistry(schemaCatalog.secretZones.associateBy { zone -> zone.id.id }),
        ),
) {
    val aiProfilesById: Map<String, AIProfile> = schemaCatalog.aiProfiles.associateBy(AIProfile::id)
    val buildIdentityByProfessionId: Map<String, FoundationProfessionBuildIdentity> = foundationBuildIdentityMapFor(schemaCatalog)
    val racesById: Map<String, RaceDef> = races.associateBy(RaceDef::id)
    val lootProfilesById: Map<String, LootProfileSchemaV3> = schemaCatalog.lootProfiles.associateBy(LootProfileSchemaV3::id)
    val lootProfileCandidatePoolResolver: LootProfileCandidatePoolResolver = LootProfileCandidatePoolResolver(itemBundle)
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
    val hiddenEventRegistry: HiddenEventRegistry =
        HiddenEventRegistry(schemaCatalog.hiddenEvents.associateBy { event -> event.id })
    val secretZoneRegistry: SecretZoneRegistry =
        SecretZoneRegistry(schemaCatalog.secretZones.associateBy { zone -> zone.id.id })
    val actionWeightProfilesById: Map<String, com.ktome.game.elites.ActionWeightProfileDef> =
        schemaCatalog.actionWeightProfiles.associateBy { profile -> profile.id }
    private val monsterCatalogById: Map<String, MonsterTemplate> = monsterCatalog.associateBy(MonsterTemplate::id)
    private val firstEliteEncounterTemplate: MonsterTemplate? =
        monsterCatalog.firstOrNull { template -> template.isEliteEncounterTemplate() }
    private val allMonsterTemplateList: List<MonsterTemplate> =
        (monsterCatalog + bossDefinitions.values.map(BossDefinition::template)).distinctBy(MonsterTemplate::id)
    val monsterTemplatesById: Map<String, MonsterTemplate> =
        (monsterCatalog + bossDefinitions.values.map(BossDefinition::template)).associateBy(MonsterTemplate::id)
    private val allMonsterTemplatesById: Map<String, MonsterTemplate> =
        allMonsterTemplateList.associateBy(MonsterTemplate::id)
    private val monsterSchemasById: Map<String, MonsterSchemaV2> =
        schemaCatalog.monsters.associateBy(MonsterSchemaV2::id)
    private val itemSchemasById: Map<String, ItemSchemaV2> =
        schemaCatalog.itemBundle.items.associateBy(ItemSchemaV2::id)
    private val talentSchemasById: Map<String, TalentSchemaV2> =
        schemaCatalog.talents.associateBy(TalentSchemaV2::id)

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
            val encounter = requireNotNull(schemaCatalog.bossEncounters.firstOrNull { encounter -> encounter.id == variant.baseEncounterId })
            val phaseIds = encounter.phases.mapTo(linkedSetOf()) { phase -> phase.id }
            val allowedActionIds = baseEncounterActionIds(variant.baseEncounterId)
            BossVariantPhaseOverrideContracts.validateReferences(
                variant = variant,
                phaseIds = phaseIds,
                telegraphIds = telegraphRegistry.all().mapTo(linkedSetOf()) { telegraph -> telegraph.id },
                allowedActionIds = allowedActionIds,
            )
        }
        Phase4StaticContentValidator.validateHiddenContentContracts(
            schemaCatalog = schemaCatalog,
            lootProfilesById = lootProfilesById,
            monsterTemplatesById = monsterTemplatesById,
            statuses = statuses,
        )
    }

    fun validateEliteMutationContracts() {
        schemaCatalog.eliteMutations.forEach { mutation ->
            mutation.grantedTalents.forEach { grantedTalent ->
                require(talentRegistry.get(grantedTalent.talentId) != null) {
                    "Elite mutation '${mutation.id}' references unknown granted talent '${grantedTalent.talentId}'."
                }
            }
            mutation.aiProfileOverlay?.let { profileId ->
                require(profileId in aiProfilesById) {
                    "Elite mutation '${mutation.id}' references unknown AI profile overlay '$profileId'."
                }
            }
            mutation.auraStatusId?.let { statusId ->
                require(statusSchemaFor(statusId) != null) {
                    "Elite mutation '${mutation.id}' references unknown aura status '$statusId'."
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
        allMonsterTemplateList

    fun monsterCatalogTemplate(templateId: String): MonsterTemplate? = monsterCatalogById[templateId]

    fun monsterTemplate(templateId: String): MonsterTemplate? = allMonsterTemplatesById[templateId]

    fun eliteEncounterTemplateFallback(): MonsterTemplate? = firstEliteEncounterTemplate

    fun monsterSchema(templateId: String): MonsterSchemaV2? = monsterSchemasById[templateId]

    fun itemSchema(baseItemId: String): ItemSchemaV2? = itemSchemasById[baseItemId]

    fun talentSchema(talentId: String): TalentSchemaV2? = talentSchemasById[talentId]

    fun statusSchemaFor(statusId: String): StatusSchemaV2? =
        statuses.firstOrNull { schema -> schema.id == statusId || schema.effectType == statusId }

    fun telegraphSpecFor(telegraphRef: String?): com.ktome.core.ai.TelegraphSpec? =
        telegraphRef?.let(telegraphRegistry::resolve)

    fun aiProfile(profileId: String?): AIProfile? =
        profileId?.let(aiProfilesById::get)

    fun lootProfile(profileId: String): LootProfileSchemaV3? = lootProfilesById[profileId]

    fun lootProfileCandidatePool(profileId: String): LootProfileCandidatePool? =
        lootProfile(profileId)?.let(lootProfileCandidatePoolResolver::resolve)

    fun secretZone(contentRef: com.ktome.core.world.solvability.ContentRef) = secretZoneRegistry.resolve(contentRef)

    fun preferredTerrainTagsForMutations(mutationIds: Iterable<String>): Set<TerrainTag> =
        mutationIds
            .asSequence()
            .mapNotNull(eliteMutationRegistry::resolve)
            .flatMap { mutation -> mutation.preferredTerrainTags.asSequence() }
            .toCollection(linkedSetOf())

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
