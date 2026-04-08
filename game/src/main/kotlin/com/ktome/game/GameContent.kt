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
import com.ktome.core.phase.PackId
import com.ktome.core.race.RaceDef
import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.status.StatusCatalog
import com.ktome.game.data.schema.LootProfileSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.StatusSchemaV2
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.i18n.Localizer
import com.ktome.game.elites.BossVariantRegistry
import com.ktome.game.elites.EliteMutationRegistry
import com.ktome.game.hidden.HiddenContentMapgenPipeline
import com.ktome.game.hidden.HiddenEventRegistry
import com.ktome.game.hidden.HiddenConditionKey
import com.ktome.game.hidden.HiddenEventRewardPayload
import com.ktome.game.hidden.LOOT_PROFILE_REGISTRY_ID
import com.ktome.game.hidden.MONSTER_REGISTRY_ID
import com.ktome.game.hidden.STATUS_REGISTRY_ID
import com.ktome.game.hidden.SecretZoneRegistry
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
    val hiddenEventRegistry: HiddenEventRegistry =
        HiddenEventRegistry(schemaCatalog.hiddenEvents.associateBy { event -> event.id })
    val secretZoneRegistry: SecretZoneRegistry =
        SecretZoneRegistry(schemaCatalog.secretZones.associateBy { zone -> zone.id.id })
    val actionWeightProfilesById: Map<String, com.ktome.game.elites.ActionWeightProfileDef> =
        schemaCatalog.actionWeightProfiles.associateBy { profile -> profile.id }
    val monsterTemplatesById: Map<String, MonsterTemplate> =
        (monsterCatalog + bossDefinitions.values.map(BossDefinition::template)).associateBy(MonsterTemplate::id)

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
        val hiddenEntrancePlans = schemaCatalog.zoneMapgenProfiles.flatMap { profile -> profile.hiddenEntrancePlans }
        val hiddenEntrancePlansByBindingId = hiddenEntrancePlans.associateBy { plan -> plan.bindingId }
        val hiddenEntrancePlansBySecretZoneId = hiddenEntrancePlans.associateBy { plan -> plan.targetSecretZoneId.id }
        require(hiddenEntrancePlansBySecretZoneId.size == hiddenEntrancePlans.size) {
            "Each hidden entrance plan must target a unique secret zone id."
        }
        schemaCatalog.secretZones.forEach { secretZone ->
            val hiddenEntrancePlan = requireNotNull(hiddenEntrancePlansBySecretZoneId[secretZone.id.id]) {
                "Secret zone '${secretZone.id.id}' is not targeted by any hidden entrance plan."
            }
            require(hiddenEntrancePlan.targetSecretZoneId == secretZone.id) {
                "Secret zone '${secretZone.id.id}' must be targeted by hidden entrance '${hiddenEntrancePlan.bindingId.value}'."
            }
            require(secretZone.entranceBindingId == hiddenEntrancePlan.entranceAnchorId) {
                "Secret zone '${secretZone.id.id}' must bind to hidden entrance anchor '${hiddenEntrancePlan.entranceAnchorId.value}'."
            }
            require(secretZone.entryRule == hiddenEntrancePlan.discoveryRule) {
                "Secret zone '${secretZone.id.id}' entryRule must match hidden entrance discoveryRule '${hiddenEntrancePlan.bindingId.value}'."
            }
            require(lootProfilesById.containsKey(secretZone.rewardProfileId.id)) {
                "Secret zone '${secretZone.id.id}' references unknown reward profile '${secretZone.rewardProfileId.id}'."
            }
            secretZone.guaranteedContent.forEach { contentRef ->
                when (contentRef.registry.value) {
                    "hidden_event" ->
                        require(hiddenEventRegistry.resolve(contentRef.id) != null) {
                            "Secret zone '${secretZone.id.id}' guaranteed content references unknown hidden event '${contentRef.id}'."
                        }

                    "monster" ->
                        require(monsterTemplatesById.containsKey(contentRef.id)) {
                            "Secret zone '${secretZone.id.id}' guaranteed content references unknown monster '${contentRef.id}'."
                        }

                    else -> error("Secret zone '${secretZone.id.id}' guaranteed content registry '${contentRef.registry.value}' is unsupported.")
                }
            }
        }
        schemaCatalog.hiddenEvents.forEach { hiddenEvent ->
            hiddenEvent.conditions.forEach { condition ->
                when (condition.key) {
                    HiddenConditionKey.SEARCH_BINDING_ID ->
                        require(hiddenEntrancePlansByBindingId.keys.any { bindingId -> bindingId.value == condition.expectedValue }) {
                            "Hidden event '${hiddenEvent.id}' references unknown search binding '${condition.expectedValue}'."
                        }

                    HiddenConditionKey.SECRET_ZONE_ID ->
                        require(secretZoneRegistry.resolve(condition.expectedValue) != null) {
                            "Hidden event '${hiddenEvent.id}' references unknown secret zone '${condition.expectedValue}'."
                        }

                    else -> Unit
                }
            }
            hiddenEvent.rewards.forEach { reward ->
                when (val payload = reward.payload) {
                    is HiddenEventRewardPayload.RevealSecretZone ->
                        require(hiddenEntrancePlansByBindingId.containsKey(payload.bindingId)) {
                            "Hidden event '${hiddenEvent.id}' reveal payload references unknown search binding '${payload.bindingId.value}'."
                        }

                    is HiddenEventRewardPayload.GrantBuff -> {
                        require(payload.statusRef.registry.value == STATUS_REGISTRY_ID) {
                            "Hidden event '${hiddenEvent.id}' buff payload must use registry '$STATUS_REGISTRY_ID'."
                        }
                        require(statusSchemaFor(payload.statusRef.id) != null) {
                            "Hidden event '${hiddenEvent.id}' references unknown status '${payload.statusRef.id}'."
                        }
                    }

                    is HiddenEventRewardPayload.LootProfile -> {
                        require(payload.lootProfileRef.registry.value == LOOT_PROFILE_REGISTRY_ID) {
                            "Hidden event '${hiddenEvent.id}' loot payload must use registry '$LOOT_PROFILE_REGISTRY_ID'."
                        }
                        require(lootProfilesById.containsKey(payload.lootProfileRef.id)) {
                            "Hidden event '${hiddenEvent.id}' references unknown loot profile '${payload.lootProfileRef.id}'."
                        }
                    }

                    is HiddenEventRewardPayload.TriggerEncounter -> {
                        require(payload.encounterRef.registry.value == MONSTER_REGISTRY_ID) {
                            "Hidden event '${hiddenEvent.id}' encounter payload must use registry '$MONSTER_REGISTRY_ID'."
                        }
                        require(monsterTemplatesById.containsKey(payload.encounterRef.id)) {
                            "Hidden event '${hiddenEvent.id}' references unknown monster '${payload.encounterRef.id}'."
                        }
                    }
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

    fun secretZone(contentRef: com.ktome.core.world.solvability.ContentRef) = secretZoneRegistry.resolve(contentRef)

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
