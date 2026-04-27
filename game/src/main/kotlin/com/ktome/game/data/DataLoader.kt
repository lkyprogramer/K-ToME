package com.ktome.game.data

import com.ktome.core.ai.AIAction
import com.ktome.core.ai.AIActionType
import com.ktome.core.ai.AICondition
import com.ktome.core.ai.AIConditionScope
import com.ktome.core.ai.AIDefaultBehavior
import com.ktome.core.ai.AIProfile
import com.ktome.core.ai.AISelectionPolicy
import com.ktome.core.ai.BossEncounter
import com.ktome.core.ai.BossPhaseDef
import com.ktome.core.ai.BossPhaseEvent
import com.ktome.core.ai.BossPhaseEventType
import com.ktome.core.ai.BossPhaseTransitionTiming
import com.ktome.core.ai.CounterplayTag
import com.ktome.core.ai.DangerLevel
import com.ktome.core.ai.LevelBand
import com.ktome.core.ai.TelegraphShape
import com.ktome.core.ai.TelegraphSpec
import com.ktome.core.ai.TelegraphStage
import com.ktome.core.ai.ThreatProfileDef
import com.ktome.core.combat.ApplicationPolicy
import com.ktome.core.combat.DamageType
import com.ktome.core.combat.SaveDimension
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.Stats
import com.ktome.core.phase.PackId
import com.ktome.core.inscription.InscriptionCategory
import com.ktome.core.inscription.InscriptionDef
import com.ktome.core.inscription.InscriptionEffect
import com.ktome.core.item.AffixDef
import com.ktome.core.item.AffixEquipType
import com.ktome.core.item.AffixType
import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.item.ItemType
import com.ktome.core.item.MaterialDef
import com.ktome.core.item.PassiveCondition
import com.ktome.core.item.SpecialItemTemplate
import com.ktome.core.item.StatModifier
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTier
import com.ktome.core.mapgen.BiomeFamilyDef
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.PatternRoomDef
import com.ktome.core.mapgen.PatternTemplateDef
import com.ktome.core.mapgen.RoomDef
import com.ktome.core.mapgen.RoomShape
import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.mapgen.VaultDef
import com.ktome.core.mapgen.VaultTemplateDef
import com.ktome.core.mapgen.ZoneMapgenProfile
import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.world.solvability.ContentRef
import com.ktome.core.world.solvability.DiscoveryPredicate
import com.ktome.core.world.solvability.DiscoveryPredicateType
import com.ktome.core.world.solvability.DiscoveryRule
import com.ktome.core.world.solvability.KeyType
import com.ktome.core.world.solvability.NodeAnchorId
import com.ktome.core.world.solvability.RegistryId
import com.ktome.core.world.solvability.RuleCombinator
import com.ktome.core.world.solvability.SearchBindingId
import com.ktome.core.economy.AffordableRescueSlotPolicy
import com.ktome.core.economy.RescueInventoryPolicy
import com.ktome.core.economy.ShopNode
import com.ktome.core.economy.ShopOffer
import com.ktome.core.economy.ShopServiceType
import com.ktome.core.profession.ProfessionTier
import com.ktome.core.profession.ReleaseUnlockCondition
import com.ktome.core.profession.SoloContractDef
import com.ktome.core.profile.ClassUnlockState
import com.ktome.core.race.RaceDef
import com.ktome.core.race.RaceStatModifiers
import com.ktome.core.resource.DecayPolicy
import com.ktome.core.resource.EquilibriumAffinity
import com.ktome.core.resource.ResourceAxis
import com.ktome.core.resource.ResourceProfileRef
import com.ktome.core.resource.ResourceRegenProfile
import com.ktome.core.resource.ResourceType
import com.ktome.core.status.EffectCarrierKind
import com.ktome.core.status.EffectCategory
import com.ktome.core.status.RemoteRemovalPolicy
import com.ktome.core.status.ReplacePolicy
import com.ktome.core.status.StackingRule
import com.ktome.core.status.StatusCatalog
import com.ktome.core.status.StatusEffectDef
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusDefinitions
import com.ktome.core.talent.AssociatedStatusEffect
import com.ktome.core.talent.ActionCost
import com.ktome.core.talent.CleanseEffect
import com.ktome.core.talent.DisplacementType
import com.ktome.core.talent.EffectOp
import com.ktome.core.talent.EffectTargetScope
import com.ktome.core.talent.EffectTrigger
import com.ktome.core.talent.KeywordRegistry
import com.ktome.core.talent.ResourceCost
import com.ktome.core.talent.ScalingDef
import com.ktome.core.talent.TalentDef
import com.ktome.core.talent.TalentAiHints
import com.ktome.core.talent.TalentBreakpoint
import com.ktome.core.talent.TalentCategory
import com.ktome.core.talent.TalentLevelEffect
import com.ktome.core.talent.TalentPrerequisite
import com.ktome.core.talent.TalentRole
import com.ktome.core.talent.TalentTargeting
import com.ktome.core.talent.TalentTargetingType
import com.ktome.core.world.GateCondition
import com.ktome.core.world.ObjectiveState
import com.ktome.core.world.QuestProgress
import com.ktome.core.world.RewardClaimPolicy
import com.ktome.core.world.RouteReward
import com.ktome.core.world.WorldGraph
import com.ktome.core.world.ZoneConnection
import com.ktome.game.data.schema.AffordableRescueSlotPolicySchemaV2
import com.ktome.game.data.schema.AffixSchemaV2
import com.ktome.game.data.schema.BossEncounterSchemaV2
import com.ktome.game.data.schema.DifficultySchemaV2
import com.ktome.game.data.schema.EquipmentPassiveSchemaV2
import com.ktome.game.data.schema.InteractableSchemaV2
import com.ktome.game.data.schema.ItemBundleSchemaV2
import com.ktome.game.data.schema.ItemSchemaV2
import com.ktome.game.data.schema.LootPoolStrategy
import com.ktome.game.data.schema.LootProfileSchemaV3
import com.ktome.game.data.schema.MaterialSchemaV2
import com.ktome.game.data.schema.MonsterSchemaV2
import com.ktome.game.data.schema.NamedSchemaRef
import com.ktome.game.data.schema.ObjectiveSetSchemaV2
import com.ktome.game.data.schema.ObjectiveInteractablePlacementSchemaV2
import com.ktome.game.data.schema.ProfessionBuildIdentityReportOnlyFloorsSchemaV1
import com.ktome.game.data.schema.ProfessionBuildIdentitySchemaV1
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.QuestProgressSchemaV2
import com.ktome.game.data.schema.RewardRoutingEntrySchemaV1
import com.ktome.game.data.schema.RewardRoutingGrantMode
import com.ktome.game.data.schema.ResourceCostSchemaV2
import com.ktome.game.data.schema.RescueInventoryPolicySchemaV2
import com.ktome.game.data.schema.RouteRewardSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.SchemaCombatProfile
import com.ktome.game.data.schema.SchemaLevelRange
import com.ktome.game.data.schema.SchemaMapSize
import com.ktome.game.data.schema.SchemaOffset
import com.ktome.game.data.schema.SchemaFloorMapgenProfileBinding
import com.ktome.game.data.schema.SchemaStatModifier
import com.ktome.game.data.schema.SchemaStats
import com.ktome.game.data.schema.ShopNodeSchemaV2
import com.ktome.game.data.schema.ShopOfferSchemaV2
import com.ktome.game.data.schema.SpecialItemTemplateSchemaV2
import com.ktome.game.data.schema.StatusSchemaV2
import com.ktome.game.data.schema.AssociatedStatusEffectSchemaV2
import com.ktome.game.data.schema.CleanseEffectSchemaV2
import com.ktome.game.data.schema.IntRangeSchemaV2
import com.ktome.game.data.schema.TalentAiHintsSchemaV2
import com.ktome.game.data.schema.TalentBreakpointSchemaV2
import com.ktome.game.data.schema.TalentLevelEffectSchemaV2
import com.ktome.game.data.schema.TalentPrerequisiteSchemaV2
import com.ktome.game.data.schema.TalentRequirementsSchemaV2
import com.ktome.game.data.schema.TalentSchemaV2
import com.ktome.game.data.schema.TalentTargetingSchemaV2
import com.ktome.game.data.schema.TalentTreeSchemaV2
import com.ktome.game.data.schema.WorldGraphSchemaV2
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.data.schema.ZoneConnectionSchemaV2
import com.ktome.game.TalentProgression
import com.ktome.game.contentpack.ContentPackResources
import com.ktome.game.contentpack.ContentPackLoadException
import com.ktome.game.contentpack.ContentPackRuntimeResolver
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.contentpack.OverlayOp
import com.ktome.game.contentpack.ResolvedContentPack
import com.ktome.game.contentpack.ResolvedContentPackSelection
import com.ktome.game.elites.ActionWeightProfileDef
import com.ktome.game.elites.BossVariantDef
import com.ktome.game.elites.EliteMutationConfig
import com.ktome.game.elites.EliteMutationDef
import com.ktome.game.elites.MutationKind
import com.ktome.game.elites.MutationRef
import com.ktome.game.elites.MutationStatModifierDef
import com.ktome.game.elites.MutationTier
import com.ktome.game.elites.StatModifierRef
import com.ktome.game.elites.TalentGrantRef
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.i18n.Localizer
import com.ktome.game.hidden.HiddenConditionKey
import com.ktome.game.hidden.HiddenEventCondition
import com.ktome.game.hidden.HiddenEventDef
import com.ktome.game.hidden.HiddenEventReward
import com.ktome.game.hidden.HiddenEventRewardKey
import com.ktome.game.hidden.HiddenEventRewardPayload
import com.ktome.game.hidden.HiddenTriggerType
import com.ktome.game.hidden.ReturnBridgePolicy
import com.ktome.game.hidden.SecretZoneDef
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterCatalog
import com.ktome.game.model.MonsterTemplate
import java.nio.file.Files
import java.nio.file.Path
import org.yaml.snakeyaml.Yaml

class DataLoader(
    private val locale: GameLocale = GameLocale.EN_US,
    private val packSelection: ContentPackSelection = ContentPackSelection.EMPTY,
    private val preResolvedContentPackSelection: ResolvedContentPackSelection? = null,
    localizationBundleProvider: (() -> LocalizationBundle)? = null,
) {
    @Deprecated("Binary compatibility bridge for cached legacy DataLoader call sites.", level = DeprecationLevel.HIDDEN)
    constructor(
        locale: GameLocale = GameLocale.EN_US,
        localizationBundleProvider: () -> LocalizationBundle = { LocalizationBundle.load() },
    ) : this(
        locale = locale,
        packSelection = ContentPackSelection.EMPTY,
        preResolvedContentPackSelection = ResolvedContentPackSelection.EMPTY,
        localizationBundleProvider = localizationBundleProvider,
    )

    companion object {
        private const val LOOT_PROFILE_SCHEMA_VERSION: Int = 3
        private const val REWARD_ROUTING_SCHEMA_VERSION: Int = 1
        private const val BUILD_IDENTITY_SCHEMA_VERSION: Int = 1

        val REMOVED_LEGACY_EFFECT_FIELDS =
            setOf(
                "stunDuration",
                "armorBreakDuration",
                "buffDuration",
                "buffMagnitude",
                "debuffMagnitude",
                "debuffDuration",
            )

        internal fun loadBaseSchemaCatalogForContentPackLint(
            locale: GameLocale = GameLocale.EN_US,
        ): SchemaCatalog =
            // Content-pack lint must snapshot only the base catalog; resolving packs here would recurse back into the resolver.
            DataLoader(
                locale = locale,
                packSelection = ContentPackSelection.EMPTY,
                preResolvedContentPackSelection = ResolvedContentPackSelection.EMPTY,
            ).loadBaseSchemaCatalog()
    }

    private val resolvedContentPackSelectionDelegate: Lazy<ResolvedContentPackSelection> =
        lazy(LazyThreadSafetyMode.NONE) {
            if (preResolvedContentPackSelection != null) {
                preResolvedContentPackSelection
            } else if (packSelection.isEmpty) {
                ResolvedContentPackSelection.EMPTY
            } else {
                ContentPackRuntimeResolver.resolve(packSelection)
            }
        }
    private val localizationBundleProviderDelegate: Lazy<() -> LocalizationBundle> =
        lazy(LazyThreadSafetyMode.NONE) {
            localizationBundleProvider ?: {
                if (resolvedContentPackSelection.isEmpty()) {
                    LocalizationBundle.load()
                } else {
                    ContentPackResources.loadMergedLocalizationBundle(
                        selection = resolvedContentPackSelection,
                        resourceLoader = { path -> com.ktome.game.i18n.ClasspathTextResources.read(LocalizationBundle::class.java, path) },
                    )
                }
            }
        }
    private val localizerDelegate: Lazy<Localizer> =
        lazy(LazyThreadSafetyMode.NONE) {
            localizationBundleProviderDelegate.value().translator(locale)
        }

    val localizer: Localizer
        get() = localizerDelegate.value

    val activePackIds: List<PackId>
        get() = resolvedContentPackSelection.activePackIds

    val activePackManifestVersions: Map<PackId, String>
        get() = resolvedContentPackSelection.activePackManifestVersions

    private val resolvedContentPackSelection: ResolvedContentPackSelection
        get() = resolvedContentPackSelectionDelegate.value

    fun loadSchemaCatalog(): SchemaCatalog {
        val baseCatalog = loadBaseSchemaCatalog()
        val resolvedSelection = resolvedContentPackSelection
        if (resolvedSelection.isEmpty()) {
            return baseCatalog
        }
        return applyContentPackOverlays(baseCatalog, resolvedSelection)
    }

    private fun loadBaseSchemaCatalog(): SchemaCatalog {
        val telegraphSpecs = parseTelegraphSpecs(loadYamlMap("/data/telegraph/index.yaml"))
        val telegraphIds = telegraphSpecs.map(TelegraphSpec::id).toSet()
        val threatProfiles = parseThreatProfiles(loadYamlMap("/data/telegraph/threat_profiles/index.yaml"))
        val worldGraphSchema = parseWorldGraphSchema(loadYamlMap("/data/world/world_graph.yaml"))
        val eliteRoot = loadYamlMap("/data/elites/index.yaml")
        val bossVariantRoot = loadYamlMap("/data/boss-variants/index.yaml")
        val hiddenEventRoot = loadYamlMap("/data/events/index.yaml")
        val secretZoneRoot = loadYamlMap("/data/secret-zones/index.yaml")
        val roomDefs = parseRoomDefs(loadYamlMap("/data/mapgen/rooms/index.yaml"))
        val patternTemplatesAndRooms = parsePatternTemplatesAndRooms(loadYamlMap("/data/mapgen/patterns/index.yaml"))
        val vaultTemplatesAndVaults = parseVaultTemplatesAndVaults(loadYamlMap("/data/mapgen/vaults/index.yaml"))
        val biomeFamilies = parseBiomeFamilies(loadYamlMap("/data/mapgen/biomes/index.yaml"))
        val zoneMapgenRoot = loadYamlMap("/data/mapgen/zones/index.yaml")
        val zoneMapgenProfiles = parseZoneMapgenProfiles(zoneMapgenRoot)
        val zoneRewardProfiles = parseZoneRewardProfiles(zoneMapgenRoot)
        val mutationStatModifiers = parseMutationStatModifierDefs(eliteRoot)
        val eliteMutationConfig = parseEliteMutationConfig(eliteRoot)
        val eliteMutations = parseEliteMutationDefs(eliteRoot)
        val actionWeightProfiles = parseActionWeightProfiles(bossVariantRoot)
        val bossVariants = parseBossVariants(bossVariantRoot)
        return SchemaCatalog(
            professions = parseProfessionSchemas(loadYamlMap("/data/professions/index.yaml")),
            races = parseRaceDefs(loadYamlMap("/data/races/index.yaml")),
            inscriptions = parseInscriptionDefs(loadYamlMap("/data/inscriptions/index.yaml")),
            statuses = parseStatusSchemas(loadYamlMap("/data/statuses/index.yaml")),
            talents = parseTalentSchemas(loadYamlMap("/data/talents/index.yaml"), telegraphIds),
            talentTrees = parseTalentTreeSchemas(loadYamlMap("/data/talents/index.yaml")),
            monsters = parseMonsterSchemas(loadYamlMap("/data/monsters/index.yaml")),
            bossEncounters = parseBossSchemas(loadYamlMap("/data/bosses/index.yaml"), telegraphIds),
            telegraphSpecs = telegraphSpecs,
            threatProfiles = threatProfiles,
            zones = parseZoneSchemas(loadYamlMap("/data/zones/index.yaml")),
            worldGraph = worldGraphSchema.toRuntime(),
            routeRewards = parseRouteRewardSchemas(loadYamlMap("/data/world/world_graph.yaml")).map { schema -> schema.toRuntime() },
            questProgressions = parseQuestProgressSchemas(loadYamlMap("/data/world/quests.yaml")).map { schema -> schema.toRuntime() },
            shopNodes = parseShopNodeSchemas(loadYamlMap("/data/shops/index.yaml")).map { schema -> schema.toRuntime() },
            interactables = parseInteractableSchemas(loadYamlMap("/data/interactables/index.yaml")),
            rewardRoutingEntries = parseRewardRoutingEntries(loadYamlMap("/data/reward-routing/index.yaml")),
            buildIdentities = parseBuildIdentities(loadYamlMap("/data/build-identity/index.yaml")),
            objectiveSets = parseObjectiveSetSchemas(loadYamlMap("/data/objectives/index.yaml")),
            difficulties = parseDifficultySchemas(loadYamlMap("/data/difficulties/index.yaml")),
            itemBundle = parseItemBundleSchemas(loadYamlMap("/data/items/index.yaml")),
            lootProfiles = parseLootProfileSchemas(loadYamlMap("/data/loot/index.yaml")),
            eliteMutationConfig = eliteMutationConfig,
            mutationStatModifiers = mutationStatModifiers,
            eliteMutations = eliteMutations,
            bossVariants = bossVariants,
            actionWeightProfiles = actionWeightProfiles,
            hiddenEvents = parseHiddenEventDefs(hiddenEventRoot),
            secretZones = parseSecretZoneDefs(secretZoneRoot),
            tilesets = parseNamedSchemaRefs(loadYamlMap("/data/tilesets/index.yaml"), "tilesets"),
            aiProfiles = parseAiProfiles(loadYamlMap("/data/ai/index.yaml")),
            arenas = parseNamedSchemaRefs(loadYamlMap("/data/arenas/index.yaml"), "arenas"),
            ambientProfiles = parseNamedSchemaRefs(loadYamlMap("/data/ambient/index.yaml"), "ambientProfiles"),
            roomDefs = roomDefs,
            patternTemplates = patternTemplatesAndRooms.first,
            patternRooms = patternTemplatesAndRooms.second,
            vaultTemplates = vaultTemplatesAndVaults.first,
            vaults = vaultTemplatesAndVaults.second,
            biomeFamilies = biomeFamilies,
            zoneMapgenProfiles = zoneMapgenProfiles,
            zoneRewardProfiles = zoneRewardProfiles,
            visualKeys = parseStringIdSet(loadYamlMap("/data/visuals/index.yaml"), "visuals"),
            audioProfiles = parseStringIdSet(loadYamlMap("/data/audio/index.yaml"), "audioProfiles"),
        ).also(::validateProfessionTreeRunChoiceContract)
    }

    private fun validateProfessionTreeRunChoiceContract(catalog: SchemaCatalog) {
        val talentsById = catalog.talents.associateBy(TalentSchemaV2::id)
        val treesById = catalog.talentTrees.associateBy(TalentTreeSchemaV2::id)
        catalog.professions.forEach { profession ->
            profession.talentTrees.forEach { treeId ->
                require('.' !in treeId) {
                    "Profession '${profession.id}' talent tree '$treeId' must keep repository-native underscore ids; dot-format ids are not allowed."
                }
            }
            val isFrozen = "frozen" in profession.tags
            if (isFrozen) {
                require(profession.startingTalents.isEmpty()) {
                    "Frozen profession '${profession.id}' must not materialize starter profession talents."
                }
                return@forEach
            }
            require(profession.startingTalents.size == 3) {
                "Profession '${profession.id}' must start with exactly 3 learned profession talents for phase4-v4-pr01."
            }
            val professionTreeIds = profession.talentTrees.toSet()
            profession.startingTalents.forEach { talentId ->
                val talent = requireNotNull(talentsById[talentId]) {
                    "Profession '${profession.id}' references unknown starter talent '$talentId'."
                }
                require(talent.treeId in professionTreeIds) {
                    "Profession '${profession.id}' starter talent '$talentId' must belong to one of its profession trees."
                }
            }
            val treeNodeIds = profession.talentTrees.flatMap { treeId -> treesById[treeId]?.nodes.orEmpty() }
            require(treeNodeIds.any { talentId -> talentId !in profession.startingTalents }) {
                "Profession '${profession.id}' must expose at least one non-starter learnable talent."
            }
            validateProfessionTreePrerequisites(
                profession = profession,
                talentsById = talentsById,
                treesById = treesById,
            )
        }
    }

    private fun validateProfessionTreePrerequisites(
        profession: ProfessionSchemaV2,
        talentsById: Map<String, TalentSchemaV2>,
        treesById: Map<String, TalentTreeSchemaV2>,
    ) {
        profession.talentTrees.forEach { treeId ->
            val tree = requireNotNull(treesById[treeId]) {
                "Profession '${profession.id}' references unknown talent tree '$treeId'."
            }
            val tier2Nodes = tree.nodes.filter { talentId -> TalentProgression.talentNodeTier(tree, talentId) == 2 }
            val tier3Nodes = tree.nodes.filter { talentId -> TalentProgression.talentNodeTier(tree, talentId) == 3 }
            tier2Nodes.forEach { talentId ->
                require(maxPrerequisiteRank(talentsById, talentId) >= 2) {
                    "Profession tree '$treeId' Tier 2 talent '$talentId' must declare a specified prerequisite with minRank >= 2 for phase4-v4-pr01."
                }
            }
            tier3Nodes.forEach { talentId ->
                require(maxPrerequisiteRank(talentsById, talentId) >= 3) {
                    "Profession tree '$treeId' Tier 3 talent '$talentId' must declare a specified prerequisite with minRank >= 3 for phase4-v4-pr01."
                }
            }
        }
    }

    private fun maxPrerequisiteRank(
        talentsById: Map<String, TalentSchemaV2>,
        talentId: String,
    ): Int =
        talentsById[talentId]
            ?.requirements
            ?.talentPrereqs
            ?.maxOfOrNull(TalentPrerequisiteSchemaV2::minRank)
            ?: 0

    private fun applyContentPackOverlays(
        baseCatalog: SchemaCatalog,
        selection: ResolvedContentPackSelection,
    ): SchemaCatalog {
        val hiddenEventsById = baseCatalog.hiddenEvents.associateByTo(linkedMapOf(), HiddenEventDef::id)
        val secretZonesById = baseCatalog.secretZones.associateByTo(linkedMapOf()) { secretZone -> secretZone.id.id }
        val lootProfilesById = baseCatalog.lootProfiles.associateByTo(linkedMapOf(), LootProfileSchemaV3::id)
        val monstersById = baseCatalog.monsters.associateByTo(linkedMapOf(), MonsterSchemaV2::id)
        val itemsById = baseCatalog.itemBundle.items.associateByTo(linkedMapOf(), ItemSchemaV2::id)
        val materialsById = baseCatalog.itemBundle.materials.associateByTo(linkedMapOf(), MaterialSchemaV2::id)
        val affixesById = baseCatalog.itemBundle.affixes.associateByTo(linkedMapOf(), AffixSchemaV2::id)
        val specialTemplatesById =
            (baseCatalog.itemBundle.uniqueTemplates + baseCatalog.itemBundle.artifactTemplates)
                .associateByTo(linkedMapOf(), SpecialItemTemplateSchemaV2::id)
        val mutationStatModifiersById =
            baseCatalog.mutationStatModifiers.associateByTo(linkedMapOf(), MutationStatModifierDef::id)
        val eliteMutationsById = baseCatalog.eliteMutations.associateByTo(linkedMapOf(), EliteMutationDef::id)
        val bossVariantsById = baseCatalog.bossVariants.associateByTo(linkedMapOf(), BossVariantDef::id)
        val actionWeightProfilesById =
            baseCatalog.actionWeightProfiles.associateByTo(linkedMapOf(), ActionWeightProfileDef::id)

        selection.orderedPacks.forEach { pack ->
            pack.manifest.overlays.forEach { overlay ->
                when (val payload = parsePackOverlayPayload(pack = pack, overlay = overlay)) {
                    is ParsedPackOverlayPayload.HiddenEvent ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = hiddenEventsById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.SecretZone ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id.id,
                            entries = secretZonesById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.LootProfile ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = lootProfilesById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.Monster ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = monstersById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.Item ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = itemsById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.Material ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = materialsById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.Affix ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = affixesById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.SpecialItemTemplate ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = specialTemplatesById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.MutationStatModifier ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = mutationStatModifiersById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.EliteMutation ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = eliteMutationsById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.BossVariant ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = bossVariantsById,
                            entry = payload.entry,
                        )

                    is ParsedPackOverlayPayload.ActionWeightProfile ->
                        applyMapOverlay(
                            pack = pack,
                            overlay = overlay,
                            entryId = payload.entry.id,
                            entries = actionWeightProfilesById,
                            entry = payload.entry,
                        )
                }
            }
        }

        val mergedVisualKeys = linkedSetOf<String>().apply { addAll(baseCatalog.visualKeys); addAll(ContentPackResources.collectVisualKeys(selection)) }
        val mergedAudioProfiles = linkedSetOf<String>().apply { addAll(baseCatalog.audioProfiles); addAll(ContentPackResources.collectAudioKeys(selection)) }
        val mergedItemBundle =
            ItemBundleSchemaV2(
                materials = materialsById.values.toList(),
                affixes = affixesById.values.toList(),
                items = itemsById.values.toList(),
                uniqueTemplates =
                    specialTemplatesById.values
                        .filter { template -> template.specialTier == SpecialTier.UNIQUE },
                artifactTemplates =
                    specialTemplatesById.values
                        .filter { template -> template.specialTier == SpecialTier.ARTIFACT },
            )
        return baseCatalog.copy(
            hiddenEvents = hiddenEventsById.values.toList(),
            secretZones = secretZonesById.values.toList(),
            lootProfiles = lootProfilesById.values.toList(),
            monsters = monstersById.values.toList(),
            itemBundle = mergedItemBundle,
            mutationStatModifiers = mutationStatModifiersById.values.toList(),
            eliteMutations = eliteMutationsById.values.toList(),
            bossVariants = bossVariantsById.values.toList(),
            actionWeightProfiles = actionWeightProfilesById.values.toList(),
            visualKeys = mergedVisualKeys,
            audioProfiles = mergedAudioProfiles,
        )
    }

    private fun parsePackOverlayPayload(
        pack: ResolvedContentPack,
        overlay: com.ktome.game.contentpack.OverlayEntry,
    ): ParsedPackOverlayPayload {
        if (overlay.op == OverlayOp.APPEND || overlay.op == OverlayOp.DENY) {
            throw packLoadException(
                code = "content-pack.overlay.runtime-op-forbidden",
                message = "Runtime overlay path only supports ADD and REPLACE; got ${overlay.op.name}.",
                pack = pack,
                overlay = overlay,
            )
        }
        val sourcePath = pack.resolvePath(overlay.sourceFile)
        val root = loadYamlMap(sourcePath)
        return when (overlay.targetRef.registry.value) {
            "hidden_event" ->
                ParsedPackOverlayPayload.HiddenEvent(
                    entry =
                        requireSinglePackEntry(
                            pack = pack,
                            overlay = overlay,
                            sourcePath = sourcePath,
                            entries = parseHiddenEventDefs(root),
                            idOf = HiddenEventDef::id,
                        ),
                )

            "secret_zone" ->
                ParsedPackOverlayPayload.SecretZone(
                    entry =
                        requireSinglePackEntry(
                            pack = pack,
                            overlay = overlay,
                            sourcePath = sourcePath,
                            entries = parseSecretZoneDefs(root),
                            idOf = { secretZone -> secretZone.id.id },
                        ),
                )

            "loot_profile" ->
                requireSinglePackEntry(
                    pack = pack,
                    overlay = overlay,
                    sourcePath = sourcePath,
                    entries = parseLootProfileSchemaHeaders(root),
                    idOf = LootProfileSchemaHeader::id,
                ).let { header ->
                    if (header.schemaVersion != LOOT_PROFILE_SCHEMA_VERSION) {
                        throw packLoadException(
                            code = "content-pack.loot-profile.schema-version-mismatch",
                            message = "Loot profile overlay '${header.id}' must use schemaVersion=$LOOT_PROFILE_SCHEMA_VERSION; got ${header.schemaVersion}.",
                            pack = pack,
                            overlay = overlay,
                            sourcePath = sourcePath,
                            details =
                                mapOf(
                                    "packId" to pack.id.value,
                                    "targetProfileId" to header.id,
                                    "actualSchemaVersion" to header.schemaVersion.toString(),
                                    "expectedSchemaVersion" to LOOT_PROFILE_SCHEMA_VERSION.toString(),
                                ),
                        )
                    }
                    ParsedPackOverlayPayload.LootProfile(
                        entry =
                            requireSinglePackEntry(
                                pack = pack,
                                overlay = overlay,
                                sourcePath = sourcePath,
                                entries = parseLootProfileSchemas(root),
                                idOf = LootProfileSchemaV3::id,
                            ),
                    )
                }

            "monster" ->
                ParsedPackOverlayPayload.Monster(
                    entry =
                        requireSinglePackEntry(
                            pack = pack,
                            overlay = overlay,
                            sourcePath = sourcePath,
                            entries = parseMonsterSchemas(root),
                            idOf = MonsterSchemaV2::id,
                        ),
                )

            "item" ->
                ParsedPackOverlayPayload.Item(
                    entry = requireSingleItemBundleEntry(pack, overlay, sourcePath, root, ItemBundleSection.ITEM),
                )

            "material" ->
                ParsedPackOverlayPayload.Material(
                    entry = requireSingleItemBundleEntry(pack, overlay, sourcePath, root, ItemBundleSection.MATERIAL),
                )

            "affix" ->
                ParsedPackOverlayPayload.Affix(
                    entry = requireSingleItemBundleEntry(pack, overlay, sourcePath, root, ItemBundleSection.AFFIX),
                )

            "special_item_template" ->
                ParsedPackOverlayPayload.SpecialItemTemplate(
                    entry = requireSingleItemBundleEntry(pack, overlay, sourcePath, root, ItemBundleSection.SPECIAL_TEMPLATE),
                )

            "mutation_stat_modifier" ->
                ParsedPackOverlayPayload.MutationStatModifier(
                    entry =
                        requireSinglePackEntry(
                            pack = pack,
                            overlay = overlay,
                            sourcePath = sourcePath,
                            entries = parseMutationStatModifierDefs(root),
                            idOf = MutationStatModifierDef::id,
                        ),
                )

            "elite_mutation" ->
                ParsedPackOverlayPayload.EliteMutation(
                    entry =
                        requireSinglePackEntry(
                            pack = pack,
                            overlay = overlay,
                            sourcePath = sourcePath,
                            entries = parseEliteMutationDefs(root),
                            idOf = EliteMutationDef::id,
                        ),
                )

            "boss_variant" ->
                ParsedPackOverlayPayload.BossVariant(
                    entry =
                        requireSinglePackEntry(
                            pack = pack,
                            overlay = overlay,
                            sourcePath = sourcePath,
                            entries = parseBossVariants(root),
                            idOf = BossVariantDef::id,
                        ),
                )

            "action_weight_profile" ->
                ParsedPackOverlayPayload.ActionWeightProfile(
                    entry =
                        requireSinglePackEntry(
                            pack = pack,
                            overlay = overlay,
                            sourcePath = sourcePath,
                            entries = parseActionWeightProfiles(root),
                            idOf = ActionWeightProfileDef::id,
                        ),
                )

            else ->
                throw packLoadException(
                    code = "content-pack.overlay.registry-unsupported",
                    message = "Registry '${overlay.targetRef.registry.value}' is not supported by the Phase 4 runtime loader.",
                    pack = pack,
                    overlay = overlay,
                    sourcePath = sourcePath,
                )
        }
    }

    private fun <T> requireSinglePackEntry(
        pack: ResolvedContentPack,
        overlay: com.ktome.game.contentpack.OverlayEntry,
        sourcePath: Path,
        entries: List<T>,
        idOf: (T) -> String,
    ): T {
        if (entries.size != 1) {
            throw packLoadException(
                code = "content-pack.overlay.source-entry-count",
                message = "Pack overlay sources must declare exactly one formal entry; found ${entries.size}.",
                pack = pack,
                overlay = overlay,
                sourcePath = sourcePath,
            )
        }
        val entry = entries.single()
        if (idOf(entry) != overlay.targetRef.id) {
            throw packLoadException(
                code = "content-pack.overlay.source-id-mismatch",
                message = "Overlay source id '${idOf(entry)}' must match targetRef id '${overlay.targetRef.id}'.",
                pack = pack,
                overlay = overlay,
                sourcePath = sourcePath,
            )
        }
        return entry
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> requireSingleItemBundleEntry(
        pack: ResolvedContentPack,
        overlay: com.ktome.game.contentpack.OverlayEntry,
        sourcePath: Path,
        root: Map<String, Any?>,
        section: ItemBundleSection,
    ): T {
        val bundle = parseItemBundleSchemas(root)
        val entryCount =
            bundle.materials.size +
                bundle.affixes.size +
                bundle.items.size +
                bundle.uniqueTemplates.size +
                bundle.artifactTemplates.size
        if (entryCount != 1) {
            throw packLoadException(
                code = "content-pack.overlay.item-bundle-entry-count",
                message = "Pack item-bundle sources must declare exactly one formal entry; found $entryCount.",
                pack = pack,
                overlay = overlay,
                sourcePath = sourcePath,
            )
        }
        val entry: Any =
            when (section) {
                ItemBundleSection.ITEM -> bundle.items.singleOrNull()
                ItemBundleSection.MATERIAL -> bundle.materials.singleOrNull()
                ItemBundleSection.AFFIX -> bundle.affixes.singleOrNull()
                ItemBundleSection.SPECIAL_TEMPLATE -> (bundle.uniqueTemplates + bundle.artifactTemplates).singleOrNull()
            } ?: throw packLoadException(
                code = "content-pack.overlay.item-bundle-section",
                message = "Overlay registry '${overlay.targetRef.registry.value}' must declare exactly one matching item-bundle entry.",
                pack = pack,
                overlay = overlay,
                sourcePath = sourcePath,
            )
        val entryId =
            when (entry) {
                is ItemSchemaV2 -> entry.id
                is MaterialSchemaV2 -> entry.id
                is AffixSchemaV2 -> entry.id
                is SpecialItemTemplateSchemaV2 -> entry.id
                else -> error("Unsupported item-bundle entry type '${entry::class.simpleName}'.")
            }
        if (entryId != overlay.targetRef.id) {
            throw packLoadException(
                code = "content-pack.overlay.source-id-mismatch",
                message = "Overlay source id '$entryId' must match targetRef id '${overlay.targetRef.id}'.",
                pack = pack,
                overlay = overlay,
                sourcePath = sourcePath,
            )
        }
        return entry as T
    }

    private fun <T> applyMapOverlay(
        pack: ResolvedContentPack,
        overlay: com.ktome.game.contentpack.OverlayEntry,
        entryId: String,
        entries: LinkedHashMap<String, T>,
        entry: T,
    ) {
        when (overlay.op) {
            OverlayOp.ADD -> {
                if (entries.containsKey(entryId)) {
                    throw packLoadException(
                        code = "content-pack.overlay.add-conflict",
                        message = "ADD overlay for '${overlay.targetRef.registry.value}:$entryId' conflicts with an existing entry. Use REPLACE explicitly.",
                        pack = pack,
                        overlay = overlay,
                    )
                }
                entries[entryId] = entry
            }

            OverlayOp.REPLACE -> {
                if (!entries.containsKey(entryId)) {
                    throw packLoadException(
                        code = "content-pack.overlay.replace-missing-target",
                        message = "REPLACE overlay for '${overlay.targetRef.registry.value}:$entryId' requires an existing entry.",
                        pack = pack,
                        overlay = overlay,
                    )
                }
                entries[entryId] = entry
            }

            OverlayOp.APPEND,
            OverlayOp.DENY,
            -> throw packLoadException(
                code = "content-pack.overlay.runtime-op-forbidden",
                message = "Runtime overlay path only supports ADD and REPLACE; got ${overlay.op.name}.",
                pack = pack,
                overlay = overlay,
            )
        }
    }

    private fun packLoadException(
        code: String,
        message: String,
        pack: ResolvedContentPack,
        overlay: com.ktome.game.contentpack.OverlayEntry,
        sourcePath: Path? = null,
        details: Map<String, String> = emptyMap(),
    ): ContentPackLoadException =
        ContentPackLoadException(
            listOf(
                com.ktome.game.contentpack.ContentPackDiagnostic(
                    code = code,
                    message = message,
                    packId = pack.id,
                    targetRef = overlay.targetRef,
                    sourcePath = sourcePath?.toString() ?: pack.manifestPath.toString(),
                    details = details,
                ),
            ),
        )

    private sealed interface ParsedPackOverlayPayload {
        data class HiddenEvent(
            val entry: com.ktome.game.hidden.HiddenEventDef,
        ) : ParsedPackOverlayPayload

        data class SecretZone(
            val entry: com.ktome.game.hidden.SecretZoneDef,
        ) : ParsedPackOverlayPayload

        data class LootProfile(
            val entry: LootProfileSchemaV3,
        ) : ParsedPackOverlayPayload

        data class Monster(
            val entry: MonsterSchemaV2,
        ) : ParsedPackOverlayPayload

        data class Item(
            val entry: ItemSchemaV2,
        ) : ParsedPackOverlayPayload

        data class Material(
            val entry: MaterialSchemaV2,
        ) : ParsedPackOverlayPayload

        data class Affix(
            val entry: AffixSchemaV2,
        ) : ParsedPackOverlayPayload

        data class SpecialItemTemplate(
            val entry: SpecialItemTemplateSchemaV2,
        ) : ParsedPackOverlayPayload

        data class MutationStatModifier(
            val entry: MutationStatModifierDef,
        ) : ParsedPackOverlayPayload

        data class EliteMutation(
            val entry: EliteMutationDef,
        ) : ParsedPackOverlayPayload

        data class BossVariant(
            val entry: BossVariantDef,
        ) : ParsedPackOverlayPayload

        data class ActionWeightProfile(
            val entry: ActionWeightProfileDef,
        ) : ParsedPackOverlayPayload
    }

    private enum class ItemBundleSection {
        ITEM,
        MATERIAL,
        AFFIX,
        SPECIAL_TEMPLATE,
    }

    fun loadMonsterCatalog(): MonsterCatalog {
        val catalog = loadSchemaCatalog()
        return MonsterCatalog(
            monsters = catalog.monsters.map { schema -> schema.toRuntimeMonster(localizer) },
        )
    }

    fun loadItemBundle(): ItemDataBundle {
        val catalog = loadSchemaCatalog()
        return ItemDataBundle(
            baseItems = catalog.itemBundle.items.map { schema -> schema.toRuntimeItem(localizer) },
            materials = catalog.itemBundle.materials.map { schema -> schema.toRuntimeMaterial(localizer) },
            affixes = catalog.itemBundle.affixes.map { schema -> schema.toRuntimeAffix(localizer) },
            specialTemplates =
                (catalog.itemBundle.uniqueTemplates + catalog.itemBundle.artifactTemplates)
                    .map { schema -> schema.toRuntimeSpecialItemTemplate() },
        )
    }

    fun loadTalentDefinitions(): List<TalentDef> {
        val catalog = loadSchemaCatalog()
        return catalog.talents.map { schema ->
            val defaultRestoreResourceType = schema.resourceCosts.firstOrNull()?.let { cost -> ResourceType.fromId(cost.axis) }
            schema.toRuntimeTalent(defaultRestoreResourceType = defaultRestoreResourceType)
        }
    }

    fun loadStatusCatalog(): StatusCatalog {
        val catalog = loadSchemaCatalog()
        return StatusCatalog(catalog.statuses.map { schema -> schema.toRuntimeStatusDefinition() })
    }

    fun loadBossDefinitions(): Map<String, BossDefinition> {
        val catalog = loadSchemaCatalog()
        return catalog.bossEncounters.associate { encounter ->
            val template =
                requireNotNull(catalog.monsters.firstOrNull { monster -> monster.id == encounter.templateId }) {
                    "Boss encounter '${encounter.id}' references unknown monster '${encounter.templateId}'."
                }
            encounter.id to
                BossDefinition(
                    encounterId = encounter.id,
                    encounter =
                        BossEncounter(
                            id = encounter.id,
                            templateId = encounter.templateId,
                            phases = encounter.phases,
                        ),
                    template = template.toRuntimeMonster(localizer),
                    talentLevels = template.talents,
                    nameKey = encounter.nameKey,
                    descKey = encounter.descKey,
                    visualKey = encounter.visualKey,
                    iconKey = encounter.iconKey,
                    audioProfile = encounter.audioProfile,
                )
        }
    }

    private fun loadYamlMap(resourcePath: String): Map<String, Any?> {
        val normalizedPath = resourcePath.removePrefix("/")
        val stream =
            javaClass.getResourceAsStream(resourcePath)
                ?: javaClass.classLoader.getResourceAsStream(normalizedPath)
                ?: Thread.currentThread().contextClassLoader?.getResourceAsStream(normalizedPath)
            ?: error("YAML resource not found: $resourcePath")
        val root = stream.use { input -> Yaml().load<Map<String, Any?>>(input) }
        return root ?: error("YAML root must not be null: $resourcePath")
    }

    private fun loadYamlMap(resourcePath: Path): Map<String, Any?> {
        val root = Files.newBufferedReader(resourcePath).use { reader -> Yaml().load<Map<String, Any?>>(reader) }
        return root ?: error("YAML root must not be null: $resourcePath")
    }

    private fun parseProfessionSchemas(root: Map<String, Any?>): List<ProfessionSchemaV2> =
        root.requiredList("professions").map { entry ->
            val profession = entry.requiredMap()
            ProfessionSchemaV2(
                id = profession.requiredString("id"),
                nameKey = profession.requiredString("nameKey"),
                descKey = profession.requiredString("descKey"),
                resourceHintKey = profession.requiredString("resourceHintKey"),
                visualKey = profession.requiredString("visualKey"),
                iconKey = profession.requiredString("iconKey"),
                audioProfile = profession.requiredString("audioProfile"),
                schemaVersion = profession.requiredInt("schemaVersion"),
                tags = profession.optionalStringList("tags"),
                tier = profession.optionalString("tier")?.uppercase()?.let(ProfessionTier::valueOf) ?: ProfessionTier.BASE,
                resourceProfiles =
                    profession.requiredList("resourceProfiles").map { rawProfile ->
                        parseResourceProfileRef(rawProfile.requiredMap())
                    },
                primarySpendAxis = ResourceAxis.fromId(profession.requiredString("primarySpendAxis").uppercase()),
                stateAxis = profession.optionalString("stateAxis")?.uppercase()?.let(ResourceAxis::fromId),
                baseStats = profession.requiredMap("baseStats").toSchemaStats(),
                combatProfile = profession.requiredMap("combatProfile").toSchemaCombatProfile(),
                statGrowth = profession.requiredMap("statGrowth").toSchemaStats(),
                talentTrees = profession.optionalStringList("talentTrees"),
                startingTalents = profession.optionalStringList("startingTalents"),
                startingKit = profession.optionalStringList("startingKit"),
                initialUnlockState =
                    profession.optionalString("initialUnlockState")
                        ?.uppercase()
                        ?.let(ClassUnlockState::valueOf)
                        ?: ClassUnlockState.RELEASE_UNLOCKED,
                releaseUnlockCondition = profession.optionalMap("releaseUnlockCondition")?.let(::parseReleaseUnlockCondition),
                soloContract = parseSoloContract(profession.requiredMap("soloContract")),
            )
        }

    private fun parseResourceProfileRef(profile: Map<*, *>): ResourceProfileRef =
        ResourceProfileRef(
            axis = ResourceAxis.fromId(profile.requiredString("axis").uppercase()),
            initialCurrent = profile.requiredInt("initialCurrent"),
            max = profile.requiredInt("max"),
            regenProfile = profile.optionalMap("regen")?.let(::parseResourceRegenProfile) ?: ResourceRegenProfile.None,
            stableMin = profile.optionalNullableInt("stableMin"),
            stableMax = profile.optionalNullableInt("stableMax"),
        )

    private fun parseResourceRegenProfile(raw: Map<*, *>): ResourceRegenProfile {
        val type = raw.requiredString("type").uppercase()
        return when (type) {
            "PER_TURN" -> ResourceRegenProfile.PerTurn(raw.requiredInt("amount"))
            "ON_HIT" -> ResourceRegenProfile.OnHit(raw.requiredInt("amount"))
            "ON_DAMAGE_TAKEN" -> ResourceRegenProfile.OnDamageTaken(raw.requiredDouble("percent"))
            "ON_KILL" -> ResourceRegenProfile.OnKill(raw.requiredInt("amount"))
            "DECAY" ->
                ResourceRegenProfile.Decay(
                    DecayPolicy(
                        amountPerTurn = raw.requiredInt("amountPerTurn"),
                        outOfCombatOnly = raw.optionalBoolean("outOfCombatOnly", default = true),
                    ),
                )

            "COMPOSITE" ->
                ResourceRegenProfile.Composite(
                    raw.requiredList("entries").map { entry ->
                        parseResourceRegenProfile(entry.requiredMap())
                    },
                )

            "NONE" -> ResourceRegenProfile.None
            else -> error("Unknown resource regen profile type '$type'.")
        }
    }

    private fun parseReleaseUnlockCondition(raw: Map<*, *>): ReleaseUnlockCondition {
        val type = raw.requiredString("type").uppercase()
        return when (type) {
            "REQUIRE_PROFESSION_CLEARED" ->
                ReleaseUnlockCondition.RequireProfessionCleared(
                    professionId = raw.requiredString("professionId"),
                )

            else -> error("Unknown release unlock condition type '$type'.")
        }
    }

    private fun parseSoloContract(raw: Map<*, *>): SoloContractDef =
        SoloContractDef(
            offenseTags = raw.requiredStringList("offenseTags"),
            defenseTags = raw.requiredStringList("defenseTags"),
            mobilityTags = raw.requiredStringList("mobilityTags"),
            aoeAnswerTags = raw.requiredStringList("aoeAnswerTags"),
            bossAnswerTags = raw.requiredStringList("bossAnswerTags"),
            panicAnswerTags = raw.requiredStringList("panicAnswerTags"),
        )

    private fun parseRaceDefs(root: Map<String, Any?>): List<RaceDef> =
        root.requiredList("races").map { entry ->
            val race = entry.requiredMap()
            RaceDef(
                id = race.requiredString("id"),
                nameKey = race.requiredString("nameKey"),
                descKey = race.requiredString("descKey"),
                visualKey = race.requiredString("visualKey"),
                iconKey = race.requiredString("iconKey"),
                audioProfile = race.requiredString("audioProfile"),
                schemaVersion = race.requiredInt("schemaVersion"),
                tags = race.optionalStringList("tags"),
                statModifiers =
                    race.optionalMap("statModifiers")?.let { modifiers ->
                        RaceStatModifiers(
                            str = modifiers.optionalInt("str"),
                            dex = modifiers.optionalInt("dex"),
                            con = modifiers.optionalInt("con"),
                            wil = modifiers.optionalInt("wil"),
                            hpDelta = modifiers.optionalInt("hpDelta"),
                            accuracyDelta = modifiers.optionalInt("accuracyDelta"),
                            evasionDelta = modifiers.optionalInt("evasionDelta"),
                            speedDelta = modifiers.optionalInt("speedDelta"),
                        )
                    } ?: RaceStatModifiers(),
                talentTrees = race.optionalStringList("talentTrees"),
                startingTalents = race.optionalStringList("startingTalents"),
                initialUnlockState =
                    race.optionalString("initialUnlockState")
                        ?.uppercase()
                        ?.let(ClassUnlockState::valueOf)
                        ?: ClassUnlockState.RELEASE_UNLOCKED,
            )
        }

    private fun parseInscriptionDefs(root: Map<String, Any?>): List<InscriptionDef> =
        root.requiredList("inscriptions").map { entry ->
            val inscription = entry.requiredMap()
            InscriptionDef(
                id = inscription.requiredString("id"),
                nameKey = inscription.requiredString("nameKey"),
                descKey = inscription.requiredString("descKey"),
                iconKey = inscription.requiredString("iconKey"),
                category = InscriptionCategory.valueOf(inscription.requiredString("category").uppercase()),
                cooldown = inscription.requiredInt("cooldown"),
                tier = inscription.requiredInt("tier"),
                effect = parseInscriptionEffect(inscription.requiredMap("effect")),
            )
        }

    private fun parseInscriptionEffect(raw: Map<*, *>): InscriptionEffect {
        val type = raw.requiredString("type").uppercase()
        return when (type) {
            "HEAL" ->
                InscriptionEffect.Heal(
                    amount = raw.optionalInt("amount"),
                    percentMax = raw.optionalDouble("percentMax", default = 0.0),
                )

            "TELEPORT" ->
                InscriptionEffect.Teleport(
                    range = raw.requiredInt("range"),
                    controlled = raw.optionalBoolean("controlled"),
                )

            "SHIELD" ->
                InscriptionEffect.Shield(
                    amount = raw.requiredInt("amount"),
                    duration = raw.requiredInt("duration"),
                )

            "CLEANSE" ->
                InscriptionEffect.Cleanse(
                    count = raw.requiredInt("count"),
                    alsoHeal = raw.optionalInt("alsoHeal"),
                )

            "DAMAGE_BOOST" ->
                InscriptionEffect.DamageBoost(
                    multiplier = raw.requiredDouble("multiplier"),
                    duration = raw.requiredInt("duration"),
                    damageType = raw.optionalString("damageType")?.uppercase()?.let(DamageType::valueOf),
                )

            else -> error("Unknown inscription effect type '$type'.")
        }
    }

    private fun parseStatusSchemas(root: Map<String, Any?>): List<StatusSchemaV2> =
        root.requiredList("statuses").map { entry ->
            val status = entry.requiredMap()
            StatusSchemaV2(
                id = status.requiredString("id"),
                effectType = status.requiredString("effectType"),
                nameKey = status.requiredString("nameKey"),
                descKey = status.requiredString("descKey"),
                visualKey = status.requiredString("visualKey"),
                iconKey = status.requiredString("iconKey"),
                audioProfile = status.requiredString("audioProfile"),
                schemaVersion = status.requiredInt("schemaVersion"),
                tags = status.optionalStringList("tags"),
                category = status.requiredString("category"),
                carrierKind = status.requiredString("carrierKind"),
                stackingRule = status.optionalString("stackingRule"),
                stackCap = status.optionalNullableInt("stackCap"),
                replacePolicy = status.optionalString("replacePolicy"),
                uniquenessKey = status.optionalString("uniquenessKey"),
                exclusiveGroup = status.optionalString("exclusiveGroup"),
                sourceScopedUnique = status.optionalBoolean("sourceScopedUnique"),
                dispellable = status.optionalNullableBoolean("dispellable"),
                remoteRemovalPolicy = status.optionalString("remoteRemovalPolicy"),
                breaksOnActualDamage = status.optionalBoolean("breaksOnActualDamage"),
                consumedOnDamageType = status.optionalString("consumedOnDamageType"),
                consumedDamageMultiplier = status.optionalNullableDouble("consumedDamageMultiplier"),
                stats = status.optionalMap("stats")?.toSchemaStatModifier() ?: SchemaStatModifier(),
            )
        }

    private fun parseTalentSchemas(
        root: Map<String, Any?>,
        telegraphIds: Set<String>,
    ): List<TalentSchemaV2> =
        root.requiredList("talents").map { entry ->
            val talent = entry.requiredMap()
            val requirements =
                talent.optionalMap("requirements")?.let { map ->
                    TalentRequirementsSchemaV2(
                        talentPrereqs =
                            map.optionalList("talentPrereqs").map { prereq ->
                                val parsed = prereq.requiredMap()
                                TalentPrerequisiteSchemaV2(
                                    talentId = parsed.requiredString("talentId"),
                                    minRank = parsed.requiredInt("minRank"),
                                )
                            },
                    )
                } ?: TalentRequirementsSchemaV2()
            val levelEffects =
                talent.requiredMap("levelEffects").entries.associate { (rawLevel, rawEffect) ->
                    rawLevel.toString().toInt() to parseTalentLevelEffect(rawEffect.requiredMap())
                }
            val targeting =
                TalentTargetingSchemaV2(
                    type = talent.requiredString("targeting"),
                    range = talent.requiredInt("range"),
                    minRange = talent.optionalInt("minRange"),
                    areaRadius = talent.optionalInt("areaRadius"),
                )
            val breakpoints =
                talent.optionalList("breakpoints").map { rawBreakpoint ->
                    val breakpoint = rawBreakpoint.requiredMap()
                    TalentBreakpointSchemaV2(
                        atRank = breakpoint.requiredInt("atRank"),
                        descriptionAddendumKey = breakpoint.optionalString("descriptionAddendumKey"),
                    )
                }
            val resourceCosts =
                talent.optionalIntMap("resourceCosts").entries.map { (axis, amount) ->
                    ResourceCostSchemaV2(axis = axis, amount = amount)
                }
            val keywords = talent.optionalStringList("keywords")
            KeywordRegistry.CORE.resolveAll(keywords)
            val telegraphRef = talent.optionalString("telegraphRef")
            telegraphRef?.let { reference ->
                require(reference in telegraphIds) {
                    "Talent '${talent.requiredString("id")}' references unknown telegraph '$reference'."
                }
            }
            val aiHints =
                talent.optionalMap("aiHints")?.let { hints ->
                    TalentAiHintsSchemaV2(
                        role = hints.requiredString("role"),
                        preferredRange =
                            hints.optionalList("preferredRange").takeIf { values -> values.size == 2 }?.let { values ->
                                IntRangeSchemaV2(
                                    start = values[0].requiredIntValue(),
                                    endInclusive = values[1].requiredIntValue(),
                                )
                            },
                        isSustainToggle = hints.optionalBoolean("isSustainToggle"),
                    )
                }

            TalentSchemaV2(
                id = talent.requiredString("id"),
                nameKey = talent.requiredString("nameKey"),
                descKey = talent.requiredString("descKey"),
                visualKey = talent.requiredString("visualKey"),
                iconKey = talent.requiredString("iconKey"),
                audioProfile = talent.requiredString("audioProfile"),
                schemaVersion = talent.requiredInt("schemaVersion"),
                tags = talent.optionalStringList("tags"),
                maxPoints = talent.requiredInt("maxPoints"),
                tier = talent.optionalInt("tier", 1),
                category = talent.requiredString("category"),
                damageType = talent.optionalString("damageType"),
                powerDimension = talent.optionalString("powerDimension"),
                kind = talent.requiredString("kind"),
                cooldown = talent.requiredInt("cooldown"),
                castTime = talent.requiredString("castTime"),
                targeting = targeting,
                resourceCosts = resourceCosts,
                unlockLevel = talent.optionalInt("unlockLevel", 1),
                requirements = requirements,
                levelEffects = levelEffects,
                breakpoints = breakpoints,
                keywords = keywords,
                callbacks = talent.optionalStringList("callbacks"),
                telegraphRef = telegraphRef,
                equilibriumAffinity = talent.optionalString("equilibriumAffinity"),
                aiHints = aiHints,
                treeId = talent.requiredString("treeId"),
            )
        }

    private fun parseTalentTreeSchemas(root: Map<String, Any?>): List<TalentTreeSchemaV2> =
        root.requiredList("talentTrees").map { entry ->
            val tree = entry.requiredMap()
            val professionId = tree.optionalString("professionId") ?: ""
            val raceId = tree.optionalString("raceId")
            check((professionId.isNotBlank()) xor (raceId != null)) {
                "Talent tree '${tree.requiredString("id")}' must declare exactly one owner via professionId or raceId."
            }
            TalentTreeSchemaV2(
                id = tree.requiredString("id"),
                professionId = professionId,
                raceId = raceId,
                nameKey = tree.requiredString("nameKey"),
                descKey = tree.requiredString("descKey"),
                visualKey = tree.requiredString("visualKey"),
                iconKey = tree.requiredString("iconKey"),
                audioProfile = tree.requiredString("audioProfile"),
                schemaVersion = tree.requiredInt("schemaVersion"),
                tags = tree.optionalStringList("tags"),
                layout = tree.requiredString("layout"),
                nodes = tree.optionalStringList("nodes"),
            )
        }

    private fun parseMonsterSchemas(root: Map<String, Any?>): List<MonsterSchemaV2> =
        root.requiredList("monsters").map { entry ->
            val monster = entry.requiredMap()
            MonsterSchemaV2(
                id = monster.requiredString("id"),
                nameKey = monster.requiredString("nameKey"),
                descKey = monster.requiredString("descKey"),
                visualKey = monster.requiredString("visualKey"),
                iconKey = monster.requiredString("iconKey"),
                audioProfile = monster.requiredString("audioProfile"),
                schemaVersion = monster.requiredInt("schemaVersion"),
                tags = monster.optionalStringList("tags"),
                archetype = monster.requiredString("archetype"),
                glyph = monster.requiredString("glyph").single(),
                colorHex = monster.requiredString("color"),
                stats = monster.requiredMap("stats").toSchemaStats(),
                baseHp = monster.requiredInt("baseHp"),
                baseAttack = monster.requiredInt("baseAttack"),
                baseDefense = monster.requiredInt("baseDefense"),
                baseAccuracy = monster.optionalInt("baseAccuracy", 10),
                baseEvasion = monster.optionalInt("baseEvasion", 5),
                speed = monster.requiredInt("speed"),
                ai = monster.requiredString("ai"),
                aiProfileId = monster.requiredString("aiProfileId"),
                lootProfileId = monster.requiredString("lootProfileId"),
                resistances = monster.optionalIntMap("resistances"),
                talents = monster.optionalIntMap("talents"),
                expReward = monster.requiredInt("expReward"),
                spawnFloors = monster.requiredIntList("spawnFloors"),
                spawnWeight = monster.requiredInt("spawnWeight"),
            )
        }

    private fun parseBossSchemas(
        root: Map<String, Any?>,
        telegraphIds: Set<String>,
    ): List<BossEncounterSchemaV2> =
        root.requiredList("bossEncounters").map { entry ->
            val boss = entry.requiredMap()
            BossEncounterSchemaV2(
                id = boss.requiredString("id"),
                nameKey = boss.requiredString("nameKey"),
                descKey = boss.requiredString("descKey"),
                visualKey = boss.requiredString("visualKey"),
                iconKey = boss.requiredString("iconKey"),
                audioProfile = boss.requiredString("audioProfile"),
                schemaVersion = boss.requiredInt("schemaVersion"),
                tags = boss.optionalStringList("tags"),
                templateId = boss.requiredString("templateId"),
                arenaId = boss.requiredString("arenaId"),
                phases =
                    boss.optionalList("phases").also { phases ->
                        require(phases.isNotEmpty()) {
                            "Boss encounter '${boss.requiredString("id")}' must declare at least one phase."
                        }
                    }.map { rawPhase ->
                        parseBossPhaseDef(rawPhase.requiredMap(), telegraphIds)
                    },
                rewards = boss.optionalStringList("rewards"),
            )
        }

    private fun parseZoneSchemas(root: Map<String, Any?>): List<ZoneSchemaV2> =
        root.requiredList("zones").map { entry ->
            val zone = entry.requiredMap()
            ZoneSchemaV2(
                id = zone.requiredString("id"),
                nameKey = zone.requiredString("nameKey"),
                descKey = zone.requiredString("descKey"),
                visualKey = zone.requiredString("visualKey"),
                iconKey = zone.requiredString("iconKey"),
                audioProfile = zone.requiredString("audioProfile"),
                schemaVersion = zone.requiredInt("schemaVersion"),
                tags = zone.optionalStringList("tags"),
                biome = zone.requiredString("biome"),
                mapgenProfileId = zone.optionalString("mapgenProfileId"),
                mapgenProfileBindings =
                    zone.optionalList("mapgenProfileBindings").map { rawBinding ->
                        val binding = rawBinding.requiredMap()
                        SchemaFloorMapgenProfileBinding(
                            floorIndex = binding.requiredInt("floorIndex"),
                            profileId = binding.requiredString("profileId"),
                        )
                    },
                rewardProfileId = zone.optionalString("rewardProfileId"),
                floorCount = zone.requiredInt("floorCount"),
                mapSize = zone.requiredMap("mapSize").toSchemaMapSize(),
                recommendedLevel = zone.requiredMap("recommendedLevel").toSchemaLevelRange(),
                environmentTheme = zone.requiredString("environmentTheme"),
                specialMechanics = zone.optionalStringList("specialMechanics"),
                tilesetKey = zone.requiredString("tilesetKey"),
                ambientProfile = zone.requiredString("ambientProfile"),
                worldRole = zone.requiredString("worldRole"),
                monsterPools = zone.optionalStringList("monsterPools"),
                elitePools = zone.optionalStringList("elitePools"),
                bossEncounterId = zone.optionalString("bossEncounterId"),
                objectiveSetId = zone.optionalString("objectiveSetId"),
                shopNodeId = zone.optionalString("shopNodeId"),
                uniqueContentTag = zone.optionalString("uniqueContentTag"),
            )
        }

    private fun parseRoomDefs(root: Map<String, Any?>): List<RoomDef> =
        root.requiredList("rooms").map { entry ->
            val room = entry.requiredMap()
            RoomDef(
                id = room.requiredString("id"),
                shape = RoomShape.valueOf(room.requiredString("shape")),
                widthRange = room.requiredMap("widthRange").toIntRange(),
                heightRange = room.requiredMap("heightRange").toIntRange(),
                tags = room.optionalStringList("tags").toSet(),
            )
        }

    private fun parsePatternTemplatesAndRooms(root: Map<String, Any?>): Pair<List<PatternTemplateDef>, List<PatternRoomDef>> {
        val patternRooms =
            root.requiredList("patternRooms").map { entry ->
                val pattern = entry.requiredMap()
                PatternRoomDef(
                    id = pattern.requiredString("id"),
                    baseRoomId = pattern.requiredString("baseRoomId"),
                    patternId = pattern.requiredString("patternId"),
                    requiredTags = pattern.optionalStringList("requiredTags").toSet(),
                    spawnWeight = pattern.requiredInt("spawnWeight"),
                )
            }
        val templates = loadMapgenTemplates(ids = patternRooms.map(PatternRoomDef::patternId), resourceDirectory = "/data/mapgen/patterns", factory = ::PatternTemplateDef)
        return templates to patternRooms
    }

    private fun parseVaultTemplatesAndVaults(root: Map<String, Any?>): Pair<List<VaultTemplateDef>, List<VaultDef>> {
        val vaults =
            root.requiredList("vaults").map { entry ->
                val vault = entry.requiredMap()
                VaultDef(
                    id = vault.requiredString("id"),
                    templateId = vault.requiredString("templateId"),
                    pathClass = PathClass.valueOf(vault.requiredString("pathClass")),
                    threatBudget = vault.requiredInt("threatBudget"),
                    rewardBudget = vault.requiredInt("rewardBudget"),
                    allowOnBiomeFamilies = vault.requiredStringList("allowOnBiomeFamilies").toSet(),
                    requiredTerrainTags =
                        vault.optionalStringList("requiredTerrainTags")
                            .map(TerrainTag::valueOf)
                            .toSet(),
                )
            }
        val templates = loadMapgenTemplates(ids = vaults.map(VaultDef::templateId), resourceDirectory = "/data/mapgen/vaults", factory = ::VaultTemplateDef)
        return templates to vaults
    }

    private fun parseBiomeFamilies(root: Map<String, Any?>): List<BiomeFamilyDef> =
        root.requiredList("biomeFamilies").map { entry ->
            val biome = entry.requiredMap()
            BiomeFamilyDef(
                id = biome.requiredString("id"),
                primaryTileSet = biome.requiredString("primaryTileSet"),
                secondaryTileSet = biome.optionalString("secondaryTileSet"),
                terrainTagWeights =
                    biome.optionalMap("terrainTagWeights")
                        ?.entries
                        ?.associate { (tag, value) ->
                            TerrainTag.valueOf(tag.toString()) to value.requiredFloat()
                        }
                        ?: emptyMap(),
                allowedRoomTags = biome.requiredStringList("allowedRoomTags").toSet(),
            )
        }

    private fun parseZoneMapgenProfiles(root: Map<String, Any?>): List<ZoneMapgenProfile> =
        root.requiredList("zoneMapgenProfiles").map { entry ->
            val profile = entry.requiredMap()
            ZoneMapgenProfile(
                id = profile.requiredString("id"),
                zoneId = profile.requiredString("zoneId"),
                allowedBiomeFamilies = profile.requiredStringList("allowedBiomeFamilies").toSet(),
                loopCountRange = profile.requiredMap("loopCountRange").toIntRange(),
                vaultPool = profile.optionalStringList("vaultPool").toSet(),
                terrainTagWeights =
                    profile.optionalMap("terrainTagWeights")
                        ?.entries
                        ?.associate { (tag, value) ->
                            TerrainTag.valueOf(tag.toString()) to value.requiredFloat()
                        }
                        ?: emptyMap(),
                roomTagFilter = profile.optionalStringList("roomTagFilter").toSet(),
                keyGatePlans =
                    profile.optionalList("keyGatePlans").map { gateEntry ->
                        val gate = gateEntry.requiredMap()
                        com.ktome.core.mapgen.KeyGatePlan(
                            id = gate.requiredString("id"),
                            fromAnchorId = NodeAnchorId(gate.requiredString("fromAnchorId")),
                            toAnchorId = NodeAnchorId(gate.requiredString("toAnchorId")),
                            grantedByAnchorId = NodeAnchorId(gate.requiredString("grantedByAnchorId")),
                            keyType = KeyType.valueOf(gate.requiredString("keyType").uppercase()),
                            keyId = gate.requiredString("keyId"),
                        )
                    },
                hiddenEntrancePlans =
                    profile.optionalList("hiddenEntrancePlans").map { entranceEntry ->
                        val entrance = entranceEntry.requiredMap()
                        com.ktome.core.mapgen.HiddenEntrancePlan(
                            bindingId = SearchBindingId(entrance.requiredString("bindingId")),
                            sourceAnchorId = NodeAnchorId(entrance.requiredString("sourceAnchorId")),
                            entranceAnchorId = NodeAnchorId(entrance.requiredString("entranceAnchorId")),
                            targetAnchorId = NodeAnchorId(entrance.requiredString("targetAnchorId")),
                            targetSecretZoneId = entrance.requiredMap("targetSecretZoneId").toContentRef(),
                            discoveryRule = entrance.requiredMap("discoveryRule").toDiscoveryRule(),
                            pathClass =
                                entrance.optionalString("pathClass")
                                    ?.uppercase()
                                    ?.let(PathClass::valueOf)
                                    ?: PathClass.SECRET,
                        )
                    },
            )
        }

    private fun parseZoneRewardProfiles(root: Map<String, Any?>): List<ZoneRewardProfile> =
        root.requiredList("zoneRewardProfiles").map { entry ->
            val profile = entry.requiredMap()
            ZoneRewardProfile(
                id = profile.requiredString("id"),
                zoneId = profile.requiredString("zoneId"),
                rarityBonus = profile.requiredFloat("rarityBonus"),
                qualityBonus = profile.requiredInt("qualityBonus"),
                baseRewardBudget = profile.requiredInt("baseRewardBudget"),
            )
        }

    private fun parseHiddenEventDefs(root: Map<String, Any?>): List<HiddenEventDef> =
        root.requiredList("events").map { entry ->
            val event = entry.requiredMap()
            HiddenEventDef(
                id = event.requiredString("id"),
                triggerType = HiddenTriggerType.valueOf(event.requiredString("triggerType").uppercase()),
                conditions =
                    event.requiredList("conditions").map { conditionEntry ->
                        val condition = conditionEntry.requiredMap()
                        HiddenEventCondition(
                            key = HiddenConditionKey.valueOf(condition.requiredString("key").uppercase()),
                            expectedValue = condition.requiredString("expectedValue"),
                        )
                    },
                rewards =
                    event.requiredList("rewards").map { rewardEntry ->
                        val reward = rewardEntry.requiredMap()
                        val key = HiddenEventRewardKey.valueOf(reward.requiredString("key").uppercase())
                        HiddenEventReward(
                            key = key,
                            payload = parseHiddenEventRewardPayload(key = key, reward = reward.requiredMap("payload")),
                        )
                    },
                grantedDiscoveryTags = event.optionalStringList("grantedDiscoveryTags").toSet(),
                optionalOnly = event.optionalBoolean("optionalOnly", default = true),
            )
        }

    private fun parseHiddenEventRewardPayload(
        key: HiddenEventRewardKey,
        reward: Map<*, *>,
    ): HiddenEventRewardPayload =
        when (key) {
            HiddenEventRewardKey.REVEAL_SECRET_ZONE ->
                HiddenEventRewardPayload.RevealSecretZone(
                    bindingId = SearchBindingId(reward.requiredString("bindingId")),
                )

            HiddenEventRewardKey.GRANT_BUFF ->
                HiddenEventRewardPayload.GrantBuff(
                    statusRef = reward.requiredMap("statusRef").toContentRef(),
                    durationTurns = reward.requiredInt("durationTurns"),
                    magnitude = reward.optionalDouble("magnitude", 0.0),
                )

            HiddenEventRewardKey.SECRET_ZONE_REWARD -> HiddenEventRewardPayload.SecretZoneReward

            HiddenEventRewardKey.LOOT_PROFILE ->
                HiddenEventRewardPayload.LootProfile(
                    lootProfileRef = reward.requiredMap("lootProfileRef").toContentRef(),
                )

            HiddenEventRewardKey.TRIGGER_ENCOUNTER ->
                HiddenEventRewardPayload.TriggerEncounter(
                    encounterRef = reward.requiredMap("encounterRef").toContentRef(),
                    threatCost = reward.optionalInt("threatCost"),
                )
        }

    private fun parseSecretZoneDefs(root: Map<String, Any?>): List<SecretZoneDef> =
        root.requiredList("secretZones").map { entry ->
            val zone = entry.requiredMap()
            SecretZoneDef(
                id = zone.requiredMap("id").toContentRef(),
                nameKey = zone.requiredString("nameKey"),
                descKey = zone.requiredString("descKey"),
                visualKey = zone.requiredString("visualKey"),
                iconKey = zone.requiredString("iconKey"),
                audioProfile = zone.requiredString("audioProfile"),
                schemaVersion = zone.requiredInt("schemaVersion"),
                tags = zone.optionalStringList("tags"),
                entryRule = zone.requiredMap("entryRule").toDiscoveryRule(),
                pathClass =
                    zone.optionalString("pathClass")
                        ?.uppercase()
                        ?.let(PathClass::valueOf)
                        ?: PathClass.SECRET,
                rewardProfileId = zone.requiredMap("rewardProfileId").toContentRef(),
                guaranteedContent = zone.optionalList("guaranteedContent").map { contentEntry -> contentEntry.requiredMap().toContentRef() },
                entranceBindingId = NodeAnchorId(zone.requiredString("entranceBindingId")),
                returnBridgePolicy = ReturnBridgePolicy.valueOf(zone.requiredString("returnBridgePolicy").uppercase()),
                returnBridgeAnchorTag = zone.optionalString("returnBridgeAnchorTag"),
            )
        }

    private fun Map<*, *>.toDiscoveryRule(): DiscoveryRule =
        DiscoveryRule(
            combinator = optionalString("combinator")?.uppercase()?.let(RuleCombinator::valueOf) ?: RuleCombinator.AND,
            predicates =
                requiredList("predicates").map { predicateEntry ->
                    predicateEntry.requiredMap().toDiscoveryPredicate()
                },
        )

    private fun Map<*, *>.toDiscoveryPredicate(): DiscoveryPredicate =
        DiscoveryPredicate(
            type = DiscoveryPredicateType.valueOf(requiredString("type").uppercase()),
            difficulty = optionalNullableInt("difficulty"),
            requiredTag = optionalString("requiredTag"),
        )

    private fun Map<*, *>.toContentRef(): ContentRef =
        ContentRef(
            registry = RegistryId(requiredString("registry")),
            id = requiredString("id"),
        )

    private fun loadMapgenTemplateRows(
        resourcePath: String,
        expectedId: String,
    ): List<String> {
        val template = loadYamlMap(resourcePath)
        val actualId = template.requiredString("id")
        require(actualId == expectedId) {
            "Mapgen template file '$resourcePath' declared id '$actualId', expected '$expectedId'."
        }
        return template.requiredStringList("rows")
    }

    private fun <T> loadMapgenTemplates(
        ids: List<String>,
        resourceDirectory: String,
        factory: (String, List<String>) -> T,
    ): List<T> =
        ids
            .distinct()
            .map { templateId ->
                factory(
                    templateId,
                    loadMapgenTemplateRows(
                        resourcePath = "$resourceDirectory/$templateId.yaml",
                        expectedId = templateId,
                    ),
                )
            }

    private fun parseWorldGraphSchema(root: Map<String, Any?>): WorldGraphSchemaV2 {
        val world = root.requiredMap("worldGraph")
        return WorldGraphSchemaV2(
            startZoneId = world.requiredString("startZoneId"),
            connections =
                world.requiredList("connections").map { entry ->
                    val connection = entry.requiredMap()
                    ZoneConnectionSchemaV2(
                        id = connection.requiredString("id"),
                        fromZoneId = connection.requiredString("fromZoneId"),
                        toZoneId = connection.requiredString("toZoneId"),
                        isBidirectional = connection.optionalBoolean("isBidirectional", default = true),
                        gate =
                            GateCondition(
                                requiredQuestId = connection.optionalString("requiredQuestId"),
                                requiredWorldFlag = connection.optionalString("requiredWorldFlag"),
                                requiredBossKill = connection.optionalString("requiredBossKill"),
                            ),
                    )
                },
        )
    }

    private fun parseRouteRewardSchemas(root: Map<String, Any?>): List<RouteRewardSchemaV2> =
        root.optionalList("routeRewards").map { entry ->
            val reward = entry.requiredMap()
            RouteRewardSchemaV2(
                routeId = reward.requiredString("routeId"),
                claimPolicy = reward.requiredString("claimPolicy"),
                levelBandRef = reward.requiredString("levelBandRef"),
                shardReward = reward.requiredInt("shardReward"),
                guaranteedUtilityDropIds = reward.optionalStringList("guaranteedUtilityDropIds"),
                milestoneRewardProfileIds = reward.optionalStringList("milestoneRewardProfileIds"),
                rescueTags = reward.optionalStringList("rescueTags"),
            )
        }

    private fun parseQuestProgressSchemas(root: Map<String, Any?>): List<QuestProgressSchemaV2> =
        root.requiredList("quests").map { entry ->
            val quest = entry.requiredMap()
            QuestProgressSchemaV2(
                questId = quest.requiredString("questId"),
                objectiveStates =
                    quest.optionalMap("objectiveStates")
                        ?.entries
                        ?.associate { (objectiveId, state) -> objectiveId.toString() to state.toString() }
                        .orEmpty(),
                completionFlags = quest.optionalStringList("completionFlags"),
            )
        }

    private fun parseShopNodeSchemas(root: Map<String, Any?>): List<ShopNodeSchemaV2> =
        root.requiredList("shops").map { entry ->
            val shop = entry.requiredMap()
            ShopNodeSchemaV2(
                id = shop.requiredString("id"),
                zoneId = shop.requiredString("zoneId"),
                nameKey = shop.requiredString("nameKey"),
                inventory =
                    shop.requiredList("inventory").map { rawOffer ->
                        rawOffer.requiredMap().toShopOfferSchema()
                    },
                refreshInventory =
                    shop.optionalList("refreshInventory").map { rawOffer ->
                        rawOffer.requiredMap().toShopOfferSchema()
                    },
                rescuePolicy =
                    shop.requiredMap("rescuePolicy").let { policy ->
                        RescueInventoryPolicySchemaV2(
                            guaranteedTags = policy.optionalStringList("guaranteedTags"),
                            affordability =
                                policy.requiredMap("affordability").let { affordability ->
                                    AffordableRescueSlotPolicySchemaV2(
                                        checkpointId = affordability.requiredString("checkpointId"),
                                        expectedShardBudgetByCheckpoint = affordability.requiredInt("expectedShardBudgetByCheckpoint"),
                                        mandatoryAffordableItemCount = affordability.requiredInt("mandatoryAffordableItemCount"),
                                        requiredAffordableTags = affordability.optionalStringList("requiredAffordableTags"),
                                    )
                                },
                        )
                    },
            )
        }

    private fun Map<*, *>.toShopOfferSchema(): ShopOfferSchemaV2 =
        ShopOfferSchemaV2(
            id = requiredString("id"),
            itemBaseId = optionalString("itemBaseId"),
            inscriptionId = optionalString("inscriptionId"),
            serviceType = optionalString("serviceType"),
            price = requiredInt("price"),
            tags = optionalStringList("tags"),
        )

    private fun parseInteractableSchemas(root: Map<String, Any?>): List<InteractableSchemaV2> =
        root.requiredList("interactables").map { entry ->
            val interactable = entry.requiredMap()
            InteractableSchemaV2(
                id = interactable.requiredString("id"),
                nameKey = interactable.requiredString("nameKey"),
                descKey = interactable.requiredString("descKey"),
                visualKey = interactable.requiredString("visualKey"),
                audioProfile = interactable.requiredString("audioProfile"),
                schemaVersion = interactable.requiredInt("schemaVersion"),
                tags = interactable.optionalStringList("tags"),
                interactionTags = interactable.optionalStringList("interactionTags"),
                shopNodeId = interactable.optionalString("shopNodeId"),
            )
        }

    private fun parseRewardRoutingEntries(root: Map<String, Any?>): List<RewardRoutingEntrySchemaV1> =
        root.requiredList("rewardRoutingEntries").map { entry ->
            val routing = entry.requiredMap()
            RewardRoutingEntrySchemaV1(
                zoneId = routing.requiredString("zoneId"),
                interactableId = routing.requiredString("interactableId"),
                grantMode = RewardRoutingGrantMode.valueOf(routing.requiredString("grantMode")),
                schemaVersion = routing.requiredInt("schemaVersion"),
                profileIds = routing.optionalStringList("profileIds"),
                fallbackBaseId = routing.requiredString("fallbackBaseId"),
            ).also { parsed ->
                require(parsed.schemaVersion == REWARD_ROUTING_SCHEMA_VERSION) {
                    "Reward routing entry '${parsed.zoneId}/${parsed.interactableId}/${parsed.grantMode.name}' must use schemaVersion=$REWARD_ROUTING_SCHEMA_VERSION but got ${parsed.schemaVersion}."
                }
            }
        }

    private fun parseBuildIdentities(root: Map<String, Any?>): List<ProfessionBuildIdentitySchemaV1> =
        root.requiredList("buildIdentities").map { entry ->
            val identity = entry.requiredMap()
            ProfessionBuildIdentitySchemaV1(
                professionId = identity.requiredString("professionId"),
                schemaVersion = identity.requiredInt("schemaVersion"),
                capstoneBaseIds = identity.optionalStringList("capstoneBaseIds"),
                nonWeaponCapstoneBaseIds = identity.optionalStringList("nonWeaponCapstoneBaseIds"),
                preferredRewardSources =
                    identity.optionalStringList("preferredRewardSources").map { source ->
                        MilestoneRewardSource.valueOf(source)
                    },
                preferredReplacementSlots =
                    identity.optionalStringList("preferredReplacementSlots").map { slot ->
                        EquipSlot.valueOf(slot)
                    },
                terminalIdentityTags = identity.optionalStringList("terminalIdentityTags"),
                reportOnlyFloors =
                    identity.requiredMap("reportOnlyFloors").let { floors ->
                        ProfessionBuildIdentityReportOnlyFloorsSchemaV1(
                            seenMinCount = floors.requiredInt("seenMinCount"),
                            adoptionMinCount = floors.requiredInt("adoptionMinCount"),
                            nonWeaponMinCount = floors.requiredInt("nonWeaponMinCount"),
                        )
                    },
            ).also { parsed ->
                require(parsed.schemaVersion == BUILD_IDENTITY_SCHEMA_VERSION) {
                    "Build identity '${parsed.professionId}' must use schemaVersion=$BUILD_IDENTITY_SCHEMA_VERSION but got ${parsed.schemaVersion}."
                }
            }
        }

    private fun parseObjectiveSetSchemas(root: Map<String, Any?>): List<ObjectiveSetSchemaV2> =
        root.requiredList("objectiveSets").map { entry ->
            val objective = entry.requiredMap()
            ObjectiveSetSchemaV2(
                id = objective.requiredString("id"),
                nameKey = objective.requiredString("nameKey"),
                descKey = objective.requiredString("descKey"),
                schemaVersion = objective.requiredInt("schemaVersion"),
                tags = objective.optionalStringList("tags"),
                interactables = objective.optionalStringList("interactables"),
                placements =
                    objective.optionalList("placements").map { placementEntry ->
                        val placement = placementEntry.requiredMap()
                        ObjectiveInteractablePlacementSchemaV2(
                            interactableId = placement.requiredString("interactableId"),
                            floor = placement.requiredInt("floor"),
                            anchor = placement.requiredString("anchor"),
                            offset = placement.optionalMap("offset")?.toSchemaOffset() ?: SchemaOffset(),
                        )
                    },
                completionRule = objective.requiredString("completionRule"),
                linkedQuestId = objective.optionalString("linkedQuestId"),
                questObjectiveId = objective.optionalString("questObjectiveId"),
            )
        }

    private fun parseDifficultySchemas(root: Map<String, Any?>): List<DifficultySchemaV2> =
        root.requiredList("difficulties").map { entry ->
            val difficulty = entry.requiredMap()
            DifficultySchemaV2(
                id = difficulty.requiredString("id"),
                nameKey = difficulty.requiredString("nameKey"),
                descKey = difficulty.requiredString("descKey"),
                visualKey = difficulty.requiredString("visualKey"),
                iconKey = difficulty.requiredString("iconKey"),
                audioProfile = difficulty.requiredString("audioProfile"),
                schemaVersion = difficulty.requiredInt("schemaVersion"),
                tags = difficulty.optionalStringList("tags"),
                monsterHpMultiplier = difficulty.requiredDouble("monsterHpMultiplier"),
                monsterDamageMultiplier = difficulty.requiredDouble("monsterDamageMultiplier"),
                xpMultiplier = difficulty.requiredDouble("xpMultiplier"),
                lootRarityBonus = difficulty.requiredDouble("lootRarityBonus"),
                prerequisites = difficulty.optionalStringList("prerequisites"),
            )
        }

    private fun parseLootProfileSchemaHeaders(root: Map<String, Any?>): List<LootProfileSchemaHeader> =
        root.requiredList("lootProfiles").map { entry ->
            val profile = entry.requiredMap()
            LootProfileSchemaHeader(
                id = profile.requiredString("id"),
                schemaVersion = profile.requiredInt("schemaVersion"),
            )
        }

    private fun parseLootProfileSchemas(root: Map<String, Any?>): List<LootProfileSchemaV3> =
        root.requiredList("lootProfiles").map { entry ->
            val profile = entry.requiredMap()
            LootProfileSchemaV3(
                id = profile.requiredString("id"),
                schemaVersion = profile.requiredInt("schemaVersion"),
                tags = profile.optionalStringList("tags"),
                itemIds = profile.optionalStringList("itemIds"),
                rewardBudget = profile.requiredInt("rewardBudget"),
                canonicalZoneId = profile.optionalString("canonicalZoneId"),
                poolStrategy = LootPoolStrategy.valueOf(profile.requiredString("poolStrategy")),
                itemTagFilter = profile.optionalStringList("itemTagFilter"),
                excludeIds = profile.optionalStringList("excludeIds"),
                typeWeights = profile.optionalItemTypeWeightMap("typeWeights"),
                slotBias = profile.optionalEquipSlotWeightMap("slotBias"),
                specialTemplateTagPreference = profile.optionalStringList("specialTemplateTagPreference"),
                affixTagPreference = profile.optionalStringList("affixTagPreference"),
            ).also { parsed ->
                require(parsed.schemaVersion == LOOT_PROFILE_SCHEMA_VERSION) {
                    "Loot profile '${parsed.id}' must use schemaVersion=$LOOT_PROFILE_SCHEMA_VERSION but got ${parsed.schemaVersion}."
                }
            }
        }

    private data class LootProfileSchemaHeader(
        val id: String,
        val schemaVersion: Int,
    )

    private fun parseTelegraphSpecs(root: Map<String, Any?>): List<TelegraphSpec> =
        root.requiredList("telegraphSpecs").map { entry ->
            val spec = entry.requiredMap()
            TelegraphSpec(
                id = spec.requiredString("id"),
                shape = TelegraphShape.valueOf(spec.requiredString("shape")),
                previewTurns = spec.requiredInt("previewTurns"),
                dangerLevel = DangerLevel.valueOf(spec.requiredString("dangerLevel")),
                threatProfileId = spec.requiredString("threatProfileId"),
                radius = spec.optionalNullableInt("radius"),
                length = spec.optionalNullableInt("length"),
                angle = spec.optionalNullableInt("angle"),
                counterplayTags =
                    spec.optionalStringList("counterplayTags").map { tag ->
                        CounterplayTag.valueOf(tag)
                    },
                stages =
                    spec.optionalList("stages").map { rawStage ->
                        val stage = rawStage.requiredMap()
                        TelegraphStage(
                            id = stage.requiredString("id"),
                            durationTurns = stage.requiredInt("durationTurns"),
                        )
                    },
            )
        }

    private fun parseThreatProfiles(root: Map<String, Any?>): List<ThreatProfileDef> =
        root.requiredList("threatProfiles").map { entry ->
            val profile = entry.requiredMap()
            ThreatProfileDef(
                id = profile.requiredString("id"),
                defenderArchetype = profile.requiredString("defenderArchetype"),
                levelBand =
                    profile.requiredMap("levelBand").let { levelBand ->
                        LevelBand(
                            min = levelBand.requiredInt("min"),
                            max = levelBand.requiredInt("max"),
                        )
                    },
                difficultyId = profile.requiredString("difficultyId"),
                expectedMaxHp = profile.requiredInt("expectedMaxHp"),
                expectedArmor = profile.optionalInt("expectedArmor"),
                expectedResistances =
                    profile.optionalIntMap("expectedResistances").entries.associate { (typeId, value) ->
                        DamageType.valueOf(typeId) to value
                    },
            )
        }

    private fun parseAiProfiles(root: Map<String, Any?>): List<AIProfile> =
        root.requiredList("aiProfiles").map { entry ->
            val profile = entry.requiredMap()
            val id = profile.requiredString("id")
            require(profile.requiredInt("schemaVersion") == 2) {
                "AI profile '$id' must use schemaVersion 2."
            }
            AIProfile(
                id = id,
                perceptionRange = profile.requiredInt("perceptionRange"),
                useLastKnownPosition = profile.optionalBoolean("useLastKnownPosition", true),
                defaultBehavior = AIDefaultBehavior.valueOf(profile.requiredString("defaultBehavior")),
                selectionPolicy = AISelectionPolicy.valueOf(profile.requiredString("selectionPolicy")),
                actions =
                    profile.optionalList("actions").also { actions ->
                        require(actions.isNotEmpty()) {
                            "AI profile '$id' must declare at least one action."
                        }
                    }.map { rawAction ->
                        parseAiAction(rawAction.requiredMap())
                    },
            )
        }

    private fun parseAiAction(action: Map<*, *>): AIAction =
        AIAction(
            id = action.requiredString("id"),
            type = AIActionType.valueOf(action.requiredString("type")),
            orderKey = action.optionalNullableInt("orderKey"),
            weight = action.optionalNullableDouble("weight"),
            condition = action.optionalMap("condition")?.let(::parseAiCondition),
            abilityId = action.optionalString("abilityId"),
        )

    private fun parseAiCondition(condition: Map<*, *>): AICondition =
        when (condition.requiredString("type")) {
            "TARGET_VISIBLE" -> AICondition.TargetVisible
            "TARGET_DISTANCE_LESS_THAN" ->
                AICondition.TargetDistanceLessThan(
                    distance = condition.requiredInt("distance"),
                )
            "TARGET_DISTANCE_AT_MOST" ->
                AICondition.TargetDistanceAtMost(
                    distance = condition.requiredInt("distance"),
                )
            "TARGET_DISTANCE_BETWEEN" ->
                AICondition.TargetDistanceBetween(
                    minDistance = condition.requiredInt("minDistance"),
                    maxDistance = condition.requiredInt("maxDistance"),
                )
            "TARGET_HP_BELOW" ->
                AICondition.TargetHpBelow(
                    threshold = condition.requiredDouble("threshold"),
                )
            "HP_BELOW" ->
                AICondition.HpBelow(
                    threshold = condition.requiredDouble("threshold"),
                )
            "HAS_STATUS" ->
                AICondition.HasStatus(
                    statusId = condition.requiredString("statusId"),
                    scope = condition.optionalString("scope")?.let(AIConditionScope::valueOf) ?: AIConditionScope.SELF,
                )
            "TALENT_READY" ->
                AICondition.TalentReady(
                    talentId = condition.requiredString("talentId"),
                )
            "TURN_COUNT_MODULO" ->
                AICondition.TurnCountModulo(
                    divisor = condition.requiredInt("divisor"),
                    remainder = condition.requiredInt("remainder"),
                )
            "AND" ->
                AICondition.And(
                    conditions = condition.optionalList("conditions").map { nested -> parseAiCondition(nested.requiredMap()) },
                )
            "OR" ->
                AICondition.Or(
                    conditions = condition.optionalList("conditions").map { nested -> parseAiCondition(nested.requiredMap()) },
                )
            "NOT" ->
                AICondition.Not(
                    condition = parseAiCondition(condition.requiredMap("condition")),
                )
            else -> error("Unsupported AI condition type '${condition.requiredString("type")}'.")
        }

    private fun parseBossPhaseDef(
        phase: Map<*, *>,
        telegraphIds: Set<String>,
    ): BossPhaseDef =
        BossPhaseDef(
            id = phase.requiredString("id"),
            hpThreshold = phase.optionalNullableDouble("hpThreshold"),
            hpEnd = phase.optionalNullableDouble("hpEnd"),
            turnCount = phase.optionalNullableInt("turnCount"),
            requiredStatus = phase.optionalString("requiredStatus"),
            aiProfileId = phase.requiredString("aiProfileId"),
            onEnter =
                phase.optionalList("onEnter").map { rawEvent ->
                    parseBossPhaseEvent(rawEvent.requiredMap(), telegraphIds)
                },
            resetAiPhaseState = phase.optionalBoolean("resetAiPhaseState"),
            transitionTiming =
                phase.optionalString("transitionTiming")?.let(BossPhaseTransitionTiming::valueOf)
                    ?: BossPhaseTransitionTiming.START_OF_TURN,
        )

    private fun parseBossPhaseEvent(
        event: Map<*, *>,
        telegraphIds: Set<String>,
    ): BossPhaseEvent {
        val eventType = BossPhaseEventType.valueOf(event.requiredString("type"))
        val telegraphSpecId =
            when (eventType) {
                BossPhaseEventType.TELEGRAPH ->
                    requireNotNull(event.optionalString("telegraphSpecId")) {
                        "Boss phase TELEGRAPH event must declare telegraphSpecId."
                    }

                else -> event.optionalString("telegraphSpecId")
            }
        if (telegraphSpecId != null) {
            require(telegraphSpecId in telegraphIds) {
                "Boss phase event references unknown telegraph '$telegraphSpecId'."
            }
        }
        return BossPhaseEvent(
            type = eventType,
            telegraphSpecId = telegraphSpecId,
            invulnerableTurns = event.optionalNullableInt("invulnerableTurns"),
            messageKey = event.optionalString("messageKey"),
        )
    }

    private fun parseItemBundleSchemas(root: Map<String, Any?>): ItemBundleSchemaV2 =
        ItemBundleSchemaV2(
            materials = root.requiredList("materials").map { entry ->
                val material = entry.requiredMap()
                MaterialSchemaV2(
                    id = material.requiredString("id"),
                    nameKey = material.requiredString("nameKey"),
                    descKey = material.requiredString("descKey"),
                    visualKey = material.requiredString("visualKey"),
                    iconKey = material.requiredString("iconKey"),
                    audioProfile = material.requiredString("audioProfile"),
                    schemaVersion = material.requiredInt("schemaVersion"),
                    tags = material.optionalStringList("tags"),
                    minFloor = material.requiredInt("minFloor"),
                    stats = material.optionalMap("stats")?.toSchemaStatModifier() ?: SchemaStatModifier(),
                )
            },
            affixes = root.requiredList("affixes").map { entry ->
                val affix = entry.requiredMap()
                AffixSchemaV2(
                    id = affix.requiredString("id"),
                    nameKey = affix.requiredString("nameKey"),
                    descKey = affix.requiredString("descKey"),
                    visualKey = affix.requiredString("visualKey"),
                    iconKey = affix.requiredString("iconKey"),
                    audioProfile = affix.requiredString("audioProfile"),
                    schemaVersion = affix.requiredInt("schemaVersion"),
                    tags = affix.optionalStringList("tags"),
                    type = AffixType.valueOf(affix.requiredString("type")),
                    equipType = AffixEquipType.valueOf(affix.requiredString("equipType")),
                    tier = affix.requiredInt("tier"),
                    cost = affix.requiredInt("cost"),
                    affixFamily = affix.requiredString("affixFamily"),
                    exclusiveGroup = affix.optionalString("exclusiveGroup"),
                    minFloor = affix.requiredInt("minFloor"),
                    stats = affix.requiredMap("stats").toSchemaStatModifier(),
                    blacklistTags = affix.optionalStringList("blacklistTags"),
                    passive = affix.optionalMap("passive")?.toEquipmentPassiveSchema(),
                )
            },
            items = root.requiredList("items").map { entry ->
                val item = entry.requiredMap()
                ItemSchemaV2(
                    id = item.requiredString("id"),
                    nameKey = item.requiredString("nameKey"),
                    descKey = item.requiredString("descKey"),
                    visualKey = item.requiredString("visualKey"),
                    iconKey = item.requiredString("iconKey"),
                    audioProfile = item.requiredString("audioProfile"),
                    schemaVersion = item.requiredInt("schemaVersion"),
                    tags = item.optionalStringList("tags"),
                    type = ItemType.valueOf(item.requiredString("type")),
                    slot = item.optionalString("slot")?.let(EquipSlot::valueOf),
                    glyph = item.requiredString("glyph").single(),
                    colorHex = item.requiredString("color"),
                    baseAttack = item.optionalNullableInt("baseAttack"),
                    baseDefense = item.optionalNullableInt("baseDefense"),
                    stats = item.optionalMap("stats")?.toSchemaStatModifier() ?: SchemaStatModifier(),
                    materials = item.optionalStringList("materials"),
                    dropFloors = item.requiredIntList("dropFloors"),
                    dropWeight = item.requiredInt("dropWeight"),
                    effect = item.optionalString("effect")?.let(ConsumableEffect::valueOf),
                    resourceTypeId = item.optionalString("resourceTypeId"),
                    magnitude = item.optionalInt("magnitude"),
                    passive = item.optionalMap("passive")?.toEquipmentPassiveSchema(),
                )
            },
            uniqueTemplates =
                root.optionalList("uniqueTemplates").map { entry ->
                    entry.requiredMap().toSpecialItemTemplateSchema(SpecialTier.UNIQUE)
                },
            artifactTemplates =
                root.optionalList("artifactTemplates").map { entry ->
                    entry.requiredMap().toSpecialItemTemplateSchema(SpecialTier.ARTIFACT)
                },
        )

    private fun parseNamedSchemaRefs(
        root: Map<String, Any?>,
        key: String,
    ): List<NamedSchemaRef> =
        root.requiredList(key).map { entry ->
            val named = entry.requiredMap()
            NamedSchemaRef(
                id = named.requiredString("id"),
                schemaVersion = named.requiredInt("schemaVersion"),
            )
        }

    private fun parseStringIdSet(
        root: Map<String, Any?>,
        key: String,
    ): Set<String> =
        root.requiredList(key).mapTo(linkedSetOf()) { entry ->
            entry.requiredMap().requiredString("id")
        }

    private fun parseTalentLevelEffect(effect: Map<*, *>): TalentLevelEffectSchemaV2 {
        validateRemovedLegacyEffectFields(effect)
        val parsed =
            TalentLevelEffectSchemaV2(
                damageMultiplier = effect.optionalDouble("damageMultiplier", 1.0),
                knockback = effect.optionalInt("knockback"),
                rangeBonus = effect.optionalInt("rangeBonus"),
                healFraction = effect.optionalDouble("healFraction", 0.0),
                resourceRestoreFraction = effect.optionalDouble("resourceRestoreFraction", 0.0),
                associatedEffects =
                    effect.optionalList("associatedEffects").map { entry ->
                        val configuredEffect = entry.requiredMap()
                        AssociatedStatusEffectSchemaV2(
                            effectId = configuredEffect.requiredString("effectId"),
                            effectType = configuredEffect.requiredString("effectType"),
                            trigger = configuredEffect.optionalString("trigger") ?: "ON_CAST",
                            targetScope = configuredEffect.optionalString("targetScope") ?: "SELF",
                            applicationPolicy = configuredEffect.requiredString("applicationPolicy"),
                            saveDimension = configuredEffect.optionalString("saveDimension"),
                            duration = configuredEffect.optionalInt("duration"),
                            magnitude = configuredEffect.optionalDouble("magnitude", 0.0),
                        ).also(::validateAssociatedEffectSchema)
                    },
                cleanseEffect =
                    effect.optionalMap("cleanseEffect")?.let { configuredCleanse ->
                        CleanseEffectSchemaV2(
                            effectId = configuredCleanse.optionalString("effectId") ?: "cleanse",
                            trigger = configuredCleanse.optionalString("trigger") ?: "ON_CAST",
                            targetScope = configuredCleanse.optionalString("targetScope") ?: "SELF",
                            applicationPolicy = configuredCleanse.optionalString("applicationPolicy") ?: "INSTANT_ACTION",
                            maxEffectsRemoved = configuredCleanse.optionalInt("maxEffectsRemoved", 1),
                        )
                    },
            )
        return parsed
    }

    private fun validateRemovedLegacyEffectFields(effect: Map<*, *>) {
        val configuredLegacyFields =
            REMOVED_LEGACY_EFFECT_FIELDS.filter { fieldName ->
                effect[fieldName] != null
            }
        require(configuredLegacyFields.isEmpty()) {
            "Talent effect uses removed legacy fields ${configuredLegacyFields.joinToString()}; " +
                "use associatedEffects/cleanseEffect plus healFraction/resourceRestoreFraction instead."
        }
    }

    private fun validateAssociatedEffectSchema(effect: AssociatedStatusEffectSchemaV2) {
        val applicationPolicy = ApplicationPolicy.valueOf(effect.applicationPolicy)
        require(!applicationPolicy.requiresSave() || effect.saveDimension != null) {
            "Associated effect ${effect.effectId} requires saveDimension for $applicationPolicy."
        }
    }

    private fun Map<*, *>.toSchemaCombatProfile(): SchemaCombatProfile =
        SchemaCombatProfile(
            baseAttack = requiredInt("baseAttack"),
            baseDefense = requiredInt("baseDefense"),
            baseAccuracy = optionalInt("baseAccuracy", 10),
            baseEvasion = optionalInt("baseEvasion", 5),
            baseSpeed = optionalInt("baseSpeed", 100),
            baseHp = requiredInt("baseHp"),
            baseStamina = optionalInt("baseStamina", 40),
            baseHpRegen = optionalDouble("baseHpRegen", 1.0),
        )

    private fun MonsterSchemaV2.toRuntimeMonster(localizer: Localizer): MonsterTemplate =
        MonsterTemplate(
            id = id,
            name = localizer.text(nameKey),
            glyph = glyph,
            colorHex = colorHex,
            stats = Stats(str = stats.str, dex = stats.dex, con = stats.con, wil = stats.wil),
            baseHp = baseHp,
            baseAttack = baseAttack,
            baseDefense = baseDefense,
            baseAccuracy = baseAccuracy,
            baseEvasion = baseEvasion,
            speed = speed,
            ai = AIType.valueOf(ai),
            expReward = expReward,
            spawnFloors = spawnFloors,
            spawnWeight = spawnWeight,
            archetype = archetype,
            tags = tags,
            visualKey = visualKey,
            iconKey = iconKey,
            audioProfile = audioProfile,
            aiProfileId = aiProfileId,
            lootProfileId = lootProfileId,
            resistances =
                resistances.entries
                    .associate { (damageTypeId, value) -> DamageType.valueOf(damageTypeId) to value }
                    .toMap(linkedMapOf()),
            talentLevels = talents,
        )

    private fun ItemSchemaV2.toRuntimeItem(localizer: Localizer): ItemBaseDef {
        val baseStats =
            when (type) {
                ItemType.WEAPON -> StatModifier(attack = baseAttack ?: 0)
                ItemType.ARMOR -> StatModifier(defense = baseDefense ?: 0)
                ItemType.CONSUMABLE -> StatModifier()
            } + stats.toRuntimeStatModifier()
        return ItemBaseDef(
            id = id,
            name = localizer.text(nameKey),
            type = type,
            slot = slot,
            tags = tags.toSet(),
            glyph = glyph,
            colorHex = colorHex,
            baseStats = baseStats,
            allowedMaterials = materials,
            dropFloors = dropFloors,
            dropWeight = dropWeight,
            effect = effect,
            resourceTypeId = resourceTypeId,
            magnitude = magnitude,
            passive = passive?.toRuntimePassive(),
        )
    }

    private fun Map<*, *>.toEquipmentPassiveSchema(): EquipmentPassiveSchemaV2 =
        EquipmentPassiveSchemaV2(
            kind = requiredString("kind"),
            tag = optionalString("tag"),
            statusId = optionalString("statusId"),
            resourceType = optionalString("resourceType"),
            condition = optionalString("condition"),
            terrainTag = optionalString("terrainTag"),
            damageType = optionalString("damageType"),
            statModifier = optionalMap("statModifier")?.toSchemaStatModifier(),
            chance = optionalDouble("chance", 0.0),
            duration = optionalInt("duration"),
            magnitude = optionalDouble("magnitude", 0.0),
            bonusPercent = optionalDouble("bonusPercent", 0.0),
            amount = optionalInt("amount"),
        )

    private fun EquipmentPassiveSchemaV2.toRuntimePassive(): EquipmentPassive =
        when (kind) {
            "OnHitStatusProc" ->
                EquipmentPassive.OnHitStatusProc(
                    statusId = canonicalStatusId(requireNotNull(statusId) { "OnHitStatusProc passive requires 'statusId'." }),
                    chance = chance.also { resolvedChance ->
                        require(resolvedChance in 0.0..1.0) { "OnHitStatusProc chance must be between 0 and 1." }
                    },
                    duration = duration.also { resolvedDuration ->
                        require(resolvedDuration > 0) { "OnHitStatusProc duration must be positive." }
                    },
                    magnitude = magnitude,
                )

            "OnKillResourceRestore" ->
                EquipmentPassive.OnKillResourceRestore(
                    resourceType = ResourceType.fromId(requireNotNull(resourceType) { "OnKillResourceRestore passive requires 'resourceType'." }),
                    amount = amount.also { resolvedAmount ->
                        require(resolvedAmount > 0) { "OnKillResourceRestore amount must be positive." }
                    },
                )

            "ConditionalStatBonus" ->
                EquipmentPassive.ConditionalStatBonus(
                    condition =
                        PassiveCondition.valueOf(
                            requireNotNull(condition) { "ConditionalStatBonus passive requires 'condition'." },
                        ),
                    statModifier =
                        requireNotNull(statModifier) { "ConditionalStatBonus passive requires 'statModifier'." }
                            .toRuntimeStatModifier(),
                    statusId =
                        statusId?.let { rawStatusId ->
                            canonicalStatusId(rawStatusId)
                        },
                )

            "TerrainAffinityBonus" ->
                EquipmentPassive.TerrainAffinityBonus(
                    terrainTag =
                        TerrainTag.valueOf(
                            requireNotNull(terrainTag) { "TerrainAffinityBonus passive requires 'terrainTag'." },
                        ),
                    statModifier =
                        requireNotNull(statModifier) { "TerrainAffinityBonus passive requires 'statModifier'." }
                            .toRuntimeStatModifier(),
                )

            "DamageVsTag" ->
                EquipmentPassive.DamageVsTag(
                    tag = requireNotNull(tag) { "DamageVsTag passive requires 'tag'." },
                    bonusPercent = bonusPercent,
                )

            "DamageVsStatus" ->
                EquipmentPassive.DamageVsStatus(
                    statusId = canonicalStatusId(requireNotNull(statusId) { "DamageVsStatus passive requires 'statusId'." }),
                    bonusPercent = bonusPercent,
                )

            "HpRegenPerTurn" ->
                EquipmentPassive.HpRegenPerTurn(
                    amount = amount,
                )

            "DamageTypeBonus" ->
                EquipmentPassive.DamageTypeBonus(
                    type = DamageType.valueOf(requireNotNull(damageType) { "DamageTypeBonus passive requires 'damageType'." }),
                    bonusPercent = bonusPercent,
                )

            "ResistanceBonus" ->
                EquipmentPassive.ResistanceBonus(
                    damageType = DamageType.valueOf(requireNotNull(damageType) { "ResistanceBonus passive requires 'damageType'." }),
                    amount = amount,
                )

            else -> error("Unsupported equipment passive kind '$kind'.")
        }

    private fun MaterialSchemaV2.toRuntimeMaterial(localizer: Localizer): MaterialDef =
        MaterialDef(
            id = id,
            name = localizer.text(nameKey),
            minFloor = minFloor,
            statModifiers = stats.toRuntimeStatModifier(),
        )

    private fun AffixSchemaV2.toRuntimeAffix(localizer: Localizer): AffixDef =
        AffixDef(
            id = id,
            name = localizer.text(nameKey),
            type = type,
            equipType = equipType,
            tier = tier,
            cost = cost,
            affixFamily = affixFamily,
            exclusiveGroup = exclusiveGroup,
            statModifiers = stats.toRuntimeStatModifier(),
            minFloor = minFloor,
            tags = tags.toSet(),
            blacklistTags = blacklistTags.toSet(),
            passive = passive?.toRuntimePassive(),
        )

    private fun SpecialItemTemplateSchemaV2.toRuntimeSpecialItemTemplate(): SpecialItemTemplate =
        SpecialItemTemplate(
            id = id,
            itemId = itemId,
            specialTier = specialTier,
            nameKey = nameKey,
            descKey = descKey,
            visualKey = visualKey,
            iconKey = iconKey,
            audioProfile = audioProfile,
            schemaVersion = schemaVersion,
            tags = tags.toSet(),
            allowedSourceTiers = allowedSourceTiers.toSet(),
            allowedZones = allowedZones.toSet(),
            fixedAffixIds = fixedAffixIds,
            fixedMaterialId = fixedMaterialId,
        )

    private fun WorldGraphSchemaV2.toRuntime(): WorldGraph =
        WorldGraph(
            startZoneId = startZoneId,
            connections = connections.map { connection -> connection.toRuntime() },
        )

    private fun ZoneConnectionSchemaV2.toRuntime(): ZoneConnection =
        ZoneConnection(
            id = id,
            fromZoneId = fromZoneId,
            toZoneId = toZoneId,
            isBidirectional = isBidirectional,
            gate = gate,
        )

    private fun RouteRewardSchemaV2.toRuntime(): RouteReward =
        RouteReward(
            routeId = routeId,
            claimPolicy = RewardClaimPolicy.valueOf(claimPolicy),
            levelBandRef = levelBandRef,
            shardReward = shardReward,
            guaranteedUtilityDropIds = guaranteedUtilityDropIds,
            milestoneRewardProfileIds = milestoneRewardProfileIds,
            rescueTags = rescueTags.toSet(),
        )

    private fun QuestProgressSchemaV2.toRuntime(): QuestProgress =
        QuestProgress(
            questId = questId,
            objectiveStates =
                objectiveStates.mapValues { (_, stateId) ->
                    ObjectiveState.valueOf(stateId)
                },
            completionFlags = completionFlags.toSet(),
        )

    private fun ShopNodeSchemaV2.toRuntime(): ShopNode =
        ShopNode(
            id = id,
            zoneId = zoneId,
            nameKey = nameKey,
            inventory = inventory.map { offer -> offer.toRuntime() },
            refreshInventory = refreshInventory.map { offer -> offer.toRuntime() },
            rescuePolicy = rescuePolicy.toRuntime(),
        )

    private fun ShopOfferSchemaV2.toRuntime(): ShopOffer =
        ShopOffer(
            id = id,
            itemBaseId = itemBaseId,
            inscriptionId = inscriptionId,
            serviceType = serviceType?.let(ShopServiceType::valueOf),
            price = price,
            tags = tags.toSet(),
        )

    private fun RescueInventoryPolicySchemaV2.toRuntime(): RescueInventoryPolicy =
        RescueInventoryPolicy(
            guaranteedTags = guaranteedTags.toSet(),
            affordability = affordability.toRuntime(),
        )

    private fun AffordableRescueSlotPolicySchemaV2.toRuntime(): AffordableRescueSlotPolicy =
        AffordableRescueSlotPolicy(
            checkpointId = checkpointId,
            expectedShardBudgetByCheckpoint = expectedShardBudgetByCheckpoint,
            mandatoryAffordableItemCount = mandatoryAffordableItemCount,
            requiredAffordableTags = requiredAffordableTags.toSet(),
        )

    private fun TalentSchemaV2.toRuntimeTalent(defaultRestoreResourceType: ResourceType?): TalentDef =
        TalentDef(
            id = id,
            nameKey = nameKey,
            descriptionTemplateKey = descKey,
            iconKey = iconKey,
            visualKey = visualKey,
            audioProfile = audioProfile,
            maxRank = maxPoints,
            tier = tier,
            category = TalentCategory.valueOf(category),
            damageType = damageType?.let(DamageType::valueOf) ?: DamageType.PHYSICAL,
            powerDimension = powerDimension?.let(SaveDimension::valueOf),
            resourceCosts = resourceCosts.map { cost -> ResourceCost(type = ResourceType.fromId(cost.axis), amount = cost.amount) },
            cooldown = cooldown,
            actionCost = castTime.toActionCost(),
            targetingDef = targeting.toRuntimeTargeting(),
            levelEffects =
                levelEffects.mapValues { (_, effect) ->
                    TalentLevelEffect(
                        damageMultiplier = effect.damageMultiplier,
                        knockback = effect.knockback,
                        rangeBonus = effect.rangeBonus,
                        healFraction = effect.healFraction,
                        resourceRestoreFraction = effect.resourceRestoreFraction,
                        associatedEffects = effect.associatedEffects.map { associatedEffect -> associatedEffect.toRuntime() },
                        cleanseEffect = effect.cleanseEffect?.toRuntime(),
                        effectOps =
                            effect.toEffectOps(
                                defaultDamageType = damageType?.let(DamageType::valueOf),
                                defaultRestoreResourceType = defaultRestoreResourceType,
                            ),
                    )
                },
            prerequisites = requirements.talentPrereqs.map { prereq -> TalentPrerequisite(prereq.talentId, prereq.minRank) },
            breakpoints =
                if (breakpoints.isNotEmpty()) {
                    breakpoints
                        .sortedBy(TalentBreakpointSchemaV2::atRank)
                        .map { breakpoint ->
                            TalentBreakpoint(
                                atRank = breakpoint.atRank,
                                descriptionAddendumKey = breakpoint.descriptionAddendumKey,
                                unlockedEffects =
                                    levelEffects[breakpoint.atRank]
                                        ?.toEffectOps(
                                            defaultDamageType = damageType?.let(DamageType::valueOf),
                                            defaultRestoreResourceType = defaultRestoreResourceType,
                                        ).orEmpty(),
                            )
                        }
                } else {
                    inferBreakpoints(
                        rankEffects = levelEffects,
                        defaultDamageType = damageType?.let(DamageType::valueOf),
                        defaultRestoreResourceType = defaultRestoreResourceType,
                    )
                },
            keywords = keywords,
            aiHints = aiHints?.toRuntime(),
            telegraphRef = telegraphRef,
            equilibriumAffinity = equilibriumAffinity?.uppercase()?.let(EquilibriumAffinity::valueOf) ?: EquilibriumAffinity.NEUTRAL,
            callbacks = callbacks,
            treeId = treeId,
            unlockLevel = unlockLevel,
        )

    private fun AssociatedStatusEffectSchemaV2.toRuntime(): AssociatedStatusEffect =
        AssociatedStatusEffect(
            effectId = effectId,
            statusId = canonicalStatusId(effectType),
            trigger = EffectTrigger.valueOf(trigger),
            targetScope = EffectTargetScope.valueOf(targetScope),
            applicationPolicy = ApplicationPolicy.valueOf(applicationPolicy),
            saveDimension = saveDimension?.let(SaveDimension::valueOf),
            duration = duration,
            magnitude = magnitude,
        )

    private fun CleanseEffectSchemaV2.toRuntime(): CleanseEffect =
        CleanseEffect(
            effectId = effectId,
            trigger = EffectTrigger.valueOf(trigger),
            targetScope = EffectTargetScope.valueOf(targetScope),
            applicationPolicy = ApplicationPolicy.valueOf(applicationPolicy),
            maxEffectsRemoved = maxEffectsRemoved,
        )

    private fun TalentLevelEffectSchemaV2.toEffectOps(
        defaultDamageType: DamageType?,
        defaultRestoreResourceType: ResourceType?,
    ): List<EffectOp> =
        buildList {
            if (damageMultiplier > 0.0) {
                add(
                    EffectOp.Damage(
                        damageType = defaultDamageType,
                        scaling = ScalingDef(attackMultiplier = damageMultiplier),
                    ),
                )
            }
            if (healFraction > 0.0) {
                add(EffectOp.Heal(maxHpFraction = healFraction))
            }
            if (resourceRestoreFraction > 0.0) {
                defaultRestoreResourceType?.let { resourceType ->
                    add(
                        EffectOp.ResourceRestore(
                            type = resourceType,
                            fraction = resourceRestoreFraction,
                        ),
                    )
                }
            }
            if (knockback > 0) {
                add(EffectOp.Displacement(type = DisplacementType.PUSH, distance = knockback))
            }
            associatedEffects.forEach { effect ->
                add(
                    EffectOp.ApplyStatus(
                        statusId = canonicalStatusId(effect.effectType),
                        duration = effect.duration,
                        applicationPolicy = ApplicationPolicy.valueOf(effect.applicationPolicy),
                        trigger = EffectTrigger.valueOf(effect.trigger),
                        targetScope = EffectTargetScope.valueOf(effect.targetScope),
                        saveDimension = effect.saveDimension?.let(SaveDimension::valueOf),
                        magnitude = effect.magnitude,
                    ),
                )
            }
        }

    private fun TalentAiHintsSchemaV2.toRuntime(): TalentAiHints =
        TalentAiHints(
            role = TalentRole.valueOf(role),
            preferredRange = preferredRange?.let { range -> range.start..range.endInclusive },
            isSustainToggle = isSustainToggle,
        )

    private fun canonicalStatusId(effectType: String): String =
        StatusDefinitions.definitionForSchemaId(effectType)?.id
            ?: StatusEffectType.fromSchemaId(effectType)
                .takeUnless { type -> type == StatusEffectType.CUSTOM }
                ?.schemaId
            ?: effectType

    private fun TalentTargetingSchemaV2.toRuntimeTargeting(): TalentTargeting =
        TalentTargeting(
            type = TalentTargetingType.valueOf(type),
            range = range,
            minRange = minRange,
            areaRadius = areaRadius,
            requiresLineOfSight = requiresLineOfSight,
            friendlyFire = friendlyFire,
        )

    private fun StatusSchemaV2.toRuntimeStatusDefinition(): StatusEffectDef {
        val engineType = StatusEffectType.fromSchemaId(effectType)
        val fallbackDefinition = StatusDefinitions.definitionForSchemaId(effectType)
        return StatusEffectDef(
            id = id,
            type = if (engineType == StatusEffectType.CUSTOM) StatusEffectType.CUSTOM else engineType,
            category = EffectCategory.valueOf(category),
            nameKey = nameKey,
            iconKey = iconKey,
            stackingRule = stackingRule?.let(StackingRule::valueOf) ?: fallbackDefinition?.stackingRule ?: StackingRule.REFRESH_DURATION,
            stackCap = stackCap ?: fallbackDefinition?.stackCap ?: 1,
            replacePolicy = replacePolicy?.let(ReplacePolicy::valueOf) ?: fallbackDefinition?.replacePolicy ?: ReplacePolicy.REFRESH_DURATION,
            uniquenessKey = uniquenessKey ?: fallbackDefinition?.uniquenessKey,
            exclusiveGroup = exclusiveGroup ?: fallbackDefinition?.exclusiveGroup,
            sourceScopedUnique = sourceScopedUnique || (fallbackDefinition?.sourceScopedUnique == true),
            dispellable = dispellable ?: fallbackDefinition?.dispellable ?: engineType.dispellable,
            remoteRemovalPolicy =
                remoteRemovalPolicy?.let(RemoteRemovalPolicy::valueOf)
                    ?: fallbackDefinition?.remoteRemovalPolicy
                    ?: RemoteRemovalPolicy.ACTOR_CLEANSE_REMOVABLE,
            carrierKind = EffectCarrierKind.valueOf(carrierKind),
            statModifier = stats.toRuntimeStatModifier().takeUnless { modifier -> modifier == StatModifier.ZERO } ?: fallbackDefinition?.statModifier ?: StatModifier.ZERO,
            breaksOnActualDamage = breaksOnActualDamage || (fallbackDefinition?.breaksOnActualDamage == true),
            consumedOnDamageType = consumedOnDamageType?.let(DamageType::valueOf) ?: fallbackDefinition?.consumedOnDamageType,
            consumedDamageMultiplier = consumedDamageMultiplier ?: fallbackDefinition?.consumedDamageMultiplier ?: 1.0,
        )
    }

    private fun inferBreakpoints(
        rankEffects: Map<Int, TalentLevelEffectSchemaV2>,
        defaultDamageType: DamageType?,
        defaultRestoreResourceType: ResourceType?,
    ): List<TalentBreakpoint> {
        val sortedRanks = rankEffects.keys.sorted()
        return sortedRanks.mapNotNull { rank ->
            if (rank == sortedRanks.first()) {
                return@mapNotNull null
            }
            val currentOps = rankEffects.getValue(rank).toEffectOps(defaultDamageType, defaultRestoreResourceType)
            val previousOps = rankEffects.getValue(rank - 1).toEffectOps(defaultDamageType, defaultRestoreResourceType)
            val previousSignatures = previousOps.map(::breakpointSignature).toSet()
            val unlockedEffects = currentOps.filter { effect -> breakpointSignature(effect) !in previousSignatures }
            if (unlockedEffects.isEmpty()) {
                return@mapNotNull null
            }
            TalentBreakpoint(
                atRank = rank,
                unlockedEffects = unlockedEffects,
            )
        }
    }

    private fun breakpointSignature(effect: EffectOp): String =
        when (effect) {
            is EffectOp.Damage -> "damage:${effect.damageType?.name ?: "default"}"
            is EffectOp.Heal -> "heal"
            is EffectOp.ApplyStatus ->
                "apply_status:${effect.statusId}:${effect.targetScope.name}:${effect.trigger.name}:${effect.applicationPolicy.name}"
            is EffectOp.ResourceRestore -> "resource_restore:${effect.type.name}"
            is EffectOp.Displacement -> "displacement:${effect.type.name}:${effect.targetScope.name}"
            is EffectOp.StatModifier -> "stat_modifier:${statModifierAxes(effect.modifier)}:${effect.targetScope.name}"
        }

    private fun statModifierAxes(modifier: StatModifier): String =
        buildList {
            if (modifier.str != 0) add("str")
            if (modifier.dex != 0) add("dex")
            if (modifier.con != 0) add("con")
            if (modifier.wil != 0) add("wil")
            if (modifier.attack != 0) add("attack")
            if (modifier.defense != 0) add("defense")
            if (modifier.accuracy != 0) add("accuracy")
            if (modifier.evasion != 0) add("evasion")
            if (modifier.speed != 0) add("speed")
            if (modifier.maxHp != 0) add("maxHp")
            if (modifier.maxStamina != 0) add("maxStamina")
            if (modifier.hpRegen != 0.0) add("hpRegen")
            if (modifier.staminaRegen != 0.0) add("staminaRegen")
            if (modifier.critChance != 0.0) add("critChance")
            if (modifier.talentPower != 0.0) add("talentPower")
            if (modifier.attackMultiplierBonus != 0.0) add("attackMultiplierBonus")
            if (modifier.defenseMultiplierBonus != 0.0) add("defenseMultiplierBonus")
        }.sorted().joinToString(separator = "|")

    private fun String.toActionCost(): ActionCost =
        when (uppercase()) {
            "INSTANT" -> ActionCost.INSTANT
            "QUICK" -> ActionCost.QUICK
            "STANDARD" -> ActionCost.STANDARD
            "HEAVY" -> ActionCost.HEAVY
            else -> error("Unsupported action cost alias '$this'.")
        }

    private fun SchemaStatModifier.toRuntimeStatModifier(): StatModifier =
        StatModifier(
            str = str,
            dex = dex,
            con = con,
            wil = wil,
            attack = attack,
            defense = defense,
            accuracy = accuracy,
            evasion = evasion,
            speed = speed,
            castSpeedRating = castSpeedRating,
            maxHp = maxHp,
            maxStamina = maxStamina,
            hpRegen = hpRegen,
            staminaRegen = staminaRegen,
            critChance = critChance,
            talentPower = talentPower,
            attackMultiplierBonus = attackMultiplierBonus,
            defenseMultiplierBonus = defenseMultiplierBonus,
        )

    private fun Map<*, *>.toSchemaStats(): SchemaStats =
        SchemaStats(
            str = requiredInt("str"),
            dex = requiredInt("dex"),
            con = requiredInt("con"),
            wil = requiredInt("wil"),
        )

    private fun Map<*, *>.toSchemaStatModifier(): SchemaStatModifier =
        SchemaStatModifier(
            str = optionalInt("str"),
            dex = optionalInt("dex"),
            con = optionalInt("con"),
            wil = optionalInt("wil"),
            attack = optionalInt("attack"),
            defense = optionalInt("defense"),
            accuracy = optionalInt("accuracy"),
            evasion = optionalInt("evasion"),
            speed = optionalInt("speed"),
            castSpeedRating = optionalInt("castSpeedRating"),
            maxHp = optionalInt("maxHp"),
            maxStamina = optionalInt("maxStamina"),
            hpRegen = optionalDouble("hpRegen", 0.0),
            staminaRegen = optionalDouble("staminaRegen", 0.0),
            critChance = optionalDouble("critChance", 0.0),
            talentPower = optionalDouble("talentPower", 0.0),
            attackMultiplierBonus = optionalDouble("attackMultiplierBonus", 0.0),
            defenseMultiplierBonus = optionalDouble("defenseMultiplierBonus", 0.0),
        )

    private fun Map<*, *>.toSpecialItemTemplateSchema(specialTier: SpecialTier): SpecialItemTemplateSchemaV2 =
        SpecialItemTemplateSchemaV2(
            id = requiredString("id"),
            itemId = requiredString("itemId"),
            specialTier = specialTier,
            nameKey = requiredString("nameKey"),
            descKey = requiredString("descKey"),
            visualKey = requiredString("visualKey"),
            iconKey = requiredString("iconKey"),
            audioProfile = requiredString("audioProfile"),
            schemaVersion = requiredInt("schemaVersion"),
            tags = optionalStringList("tags"),
            allowedSourceTiers =
                requiredList("allowedSourceTiers").map { raw ->
                    SourceTier.valueOf(raw.toString())
                },
            allowedZones = requiredList("allowedZones").map { raw -> raw.toString() },
            fixedAffixIds = optionalStringList("fixedAffixIds"),
            fixedMaterialId = optionalString("fixedMaterialId"),
        )

    private fun Map<*, *>.toSchemaMapSize(): SchemaMapSize =
        SchemaMapSize(
            width = requiredInt("width"),
            height = requiredInt("height"),
        )

    private fun Map<*, *>.toSchemaLevelRange(): SchemaLevelRange =
        SchemaLevelRange(
            min = requiredInt("min"),
            max = requiredInt("max"),
        )

    private fun Map<*, *>.toSchemaOffset(): SchemaOffset =
        SchemaOffset(
            x = optionalInt("x"),
            y = optionalInt("y"),
        )

    private fun parseEliteMutationConfig(root: Map<String, Any?>): EliteMutationConfig =
        EliteMutationConfig(
            maxMutationsPerElite = root.optionalMap("eliteConfig")?.optionalInt("maxMutationsPerElite", 2) ?: 2,
        )

    private fun parseMutationStatModifierDefs(root: Map<String, Any?>): List<MutationStatModifierDef> =
        root.optionalList("mutationStatModifiers").map { raw ->
            val modifier = raw.requiredMap()
            MutationStatModifierDef(
                id = modifier.requiredString("id"),
                statModifier = modifier.optionalMap("stats")?.toSchemaStatModifier()?.toRuntimeStatModifier() ?: StatModifier.ZERO,
                resistances =
                    modifier.optionalIntMap("resistances").entries.associateTo(linkedMapOf()) { (damageType, value) ->
                        DamageType.valueOf(damageType) to value
                    },
            )
        }

    private fun parseEliteMutationDefs(root: Map<String, Any?>): List<EliteMutationDef> =
        root.optionalList("eliteMutations").map { raw ->
            val mutation = raw.requiredMap()
            EliteMutationDef(
                id = mutation.requiredString("id"),
                kind = MutationKind.valueOf(mutation.requiredString("kind")),
                tier = MutationTier.valueOf(mutation.requiredString("tier")),
                threatCost = mutation.requiredInt("threatCost"),
                nameKey = mutation.requiredString("nameKey"),
                iconKey = mutation.requiredString("iconKey"),
                applyToTags = mutation.requiredStringList("applyToTags").toSet(),
                minFloor = mutation.requiredInt("minFloor"),
                maxFloor = mutation.optionalNullableInt("maxFloor"),
                allowedZones = mutation.optionalStringList("allowedZones").toSet(),
                preferredTerrainTags = mutation.explicitStringList("preferredTerrainTags").map(TerrainTag::valueOf),
                statModifiers = mutation.optionalStringList("statModifiers").map(::StatModifierRef),
                grantedTalents = mutation.optionalStringList("grantedTalents").map(::TalentGrantRef),
                aiProfileOverlay = mutation.optionalString("aiProfileOverlay"),
                incompatibleWith = mutation.optionalStringList("incompatibleWith").toSet(),
                auraStatusId = mutation.optionalString("auraStatusId"),
                auraRadius = mutation.optionalInt("auraRadius"),
                auraDuration = mutation.optionalInt("auraDuration", 1),
                auraMagnitude = mutation.optionalDouble("auraMagnitude", 0.0),
            )
        }

    private fun parseActionWeightProfiles(root: Map<String, Any?>): List<ActionWeightProfileDef> =
        root.optionalList("actionWeightProfiles").map { raw ->
            val profile = raw.requiredMap()
            ActionWeightProfileDef(
                id = profile.requiredString("id"),
                actionWeights =
                    profile.requiredMap("actionWeights").entries.associate { (actionId, weight) ->
                        actionId.toString() to weight.requiredFloat().toDouble()
                    },
            )
        }

    private fun parseBossVariants(root: Map<String, Any?>): List<BossVariantDef> =
        root.optionalList("bossVariants").map { raw ->
            val variant = raw.requiredMap()
            BossVariantDef(
                id = variant.requiredString("id"),
                baseEncounterId = variant.requiredString("baseEncounterId"),
                grantedMutations = variant.optionalStringList("grantedMutations").map(::MutationRef),
                threatCost = variant.requiredInt("threatCost"),
                lootProfileOverride = variant.optionalString("lootProfileOverride"),
                visualTintKey = variant.optionalString("visualTintKey"),
                actionWeightProfileId = variant.optionalString("actionWeightProfileId"),
            )
        }
}

private fun Any?.requiredMap(): Map<*, *> =
    this as? Map<*, *> ?: error("Entry must be a map.")

private fun Map<*, *>.requiredList(key: String): List<Any?> =
    this[key] as? List<Any?> ?: error("Missing list entry '$key'")

private fun Map<*, *>.requiredMap(key: String): Map<*, *> =
    this[key] as? Map<*, *> ?: error("Missing map entry '$key'")

private fun Map<*, *>.optionalMap(key: String): Map<*, *>? =
    this[key] as? Map<*, *>

private fun Map<*, *>.optionalList(key: String): List<Any?> =
    (this[key] as? List<Any?>).orEmpty()

private fun Map<*, *>.requiredString(key: String): String =
    this[key]?.toString()?.takeIf(String::isNotBlank) ?: error("Missing string entry '$key'")

private fun Map<*, *>.optionalString(key: String): String? =
    this[key]?.toString()?.takeIf(String::isNotBlank)

private fun Map<*, *>.requiredInt(key: String): Int =
    when (val value = this[key]) {
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toInt()
        else -> error("Missing integer entry '$key'")
    }

private fun Map<*, *>.optionalInt(
    key: String,
    default: Int = 0,
): Int =
    when (val value = this[key]) {
        null -> default
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toInt()
        else -> error("Entry '$key' must be numeric")
    }

private fun Map<*, *>.optionalNullableInt(key: String): Int? =
    when (val value = this[key]) {
        null -> null
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toInt()
        else -> error("Entry '$key' must be numeric")
    }

private fun Map<*, *>.requiredIntList(key: String): List<Int> =
    (this[key] as? List<*>)?.map { value ->
        when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toInt()
            else -> error("List '$key' must contain only integers")
        }
    } ?: error("Missing integer list '$key'")

private fun Map<*, *>.optionalStringList(key: String): List<String> =
    (this[key] as? List<*>)?.map { value -> value?.toString() ?: error("List '$key' cannot contain nulls") } ?: emptyList()

private fun Map<*, *>.requiredStringList(key: String): List<String> =
    optionalStringList(key).ifEmpty { error("Missing string list '$key'") }

private fun Map<*, *>.explicitStringList(key: String): List<String> =
    if (containsKey(key)) {
        optionalStringList(key)
    } else {
        error("Missing string list '$key'")
    }

private fun Map<*, *>.optionalIntMap(key: String): Map<String, Int> =
    optionalMap(key)?.entries?.associate { (rawKey, rawValue) ->
        rawKey.toString() to
            when (rawValue) {
                is Int -> rawValue
                is Number -> rawValue.toInt()
                is String -> rawValue.toInt()
                else -> error("Entry '$key' must contain numeric values")
            }
    } ?: emptyMap()

private fun Map<*, *>.optionalItemTypeWeightMap(key: String): Map<ItemType, Int> =
    optionalIntMap(key).mapKeys { (rawKey, _) -> ItemType.valueOf(rawKey) }

private fun Map<*, *>.optionalEquipSlotWeightMap(key: String): Map<EquipSlot, Int> =
    optionalIntMap(key).mapKeys { (rawKey, _) -> EquipSlot.valueOf(rawKey) }

private fun Map<*, *>.optionalStringMap(key: String): Map<String, String> =
    optionalMap(key)?.entries?.associate { (rawKey, rawValue) ->
        rawKey.toString() to (rawValue?.toString()?.takeIf(String::isNotBlank) ?: error("Entry '$key' must contain non-blank string values"))
    } ?: emptyMap()

private fun Map<*, *>.requiredDouble(key: String): Double =
    when (val value = this[key]) {
        is Double -> value
        is Float -> value.toDouble()
        is Number -> value.toDouble()
        is String -> value.toDouble()
        else -> error("Missing numeric entry '$key'")
    }

private fun Map<*, *>.requiredFloat(key: String): Float =
    requiredDouble(key).toFloat()

private fun Any?.requiredFloat(): Float =
    when (this) {
        is Float -> this
        is Double -> this.toFloat()
        is Number -> this.toFloat()
        is String -> this.toFloat()
        else -> error("Entry must be numeric.")
    }

private fun Map<*, *>.toIntRange(): IntRange =
    when {
        containsKey("start") && containsKey("endInclusive") ->
            requiredInt("start")..requiredInt("endInclusive")

        containsKey("min") && containsKey("max") ->
            requiredInt("min")..requiredInt("max")

        else -> error("Range map must contain either start/endInclusive or min/max.")
    }

private fun Map<*, *>.optionalDouble(
    key: String,
    default: Double,
): Double =
    when (val value = this[key]) {
        null -> default
        is Double -> value
        is Float -> value.toDouble()
        is Number -> value.toDouble()
        is String -> value.toDouble()
        else -> error("Entry '$key' must be numeric")
    }

private fun Map<*, *>.optionalNullableDouble(key: String): Double? =
    when (val value = this[key]) {
        null -> null
        is Double -> value
        is Float -> value.toDouble()
        is Number -> value.toDouble()
        is String -> value.toDouble()
        else -> error("Entry '$key' must be numeric")
    }

private fun Map<*, *>.optionalNullableBoolean(key: String): Boolean? =
    when (val value = this[key]) {
        null -> null
        is Boolean -> value
        is String -> value.toBooleanStrict()
        else -> error("Entry '$key' must be boolean")
    }

private fun Map<*, *>.optionalBoolean(
    key: String,
    default: Boolean = false,
): Boolean =
    when (val value = this[key]) {
        null -> default
        is Boolean -> value
        is String -> value.toBooleanStrict()
        else -> error("Entry '$key' must be boolean")
    }

private fun Any?.requiredIntValue(): Int =
    when (this) {
        is Int -> this
        is Number -> toInt()
        is String -> toInt()
        else -> error("Expected integer value, got '$this'.")
    }
