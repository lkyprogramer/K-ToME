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
import com.ktome.core.item.ItemType
import com.ktome.core.item.MaterialDef
import com.ktome.core.item.StatModifier
import com.ktome.core.economy.AffordableRescueSlotPolicy
import com.ktome.core.economy.RescueInventoryPolicy
import com.ktome.core.economy.ShopNode
import com.ktome.core.economy.ShopOffer
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
import com.ktome.game.data.schema.LootProfileSchemaV2
import com.ktome.game.data.schema.MaterialSchemaV2
import com.ktome.game.data.schema.MonsterSchemaV2
import com.ktome.game.data.schema.NamedSchemaRef
import com.ktome.game.data.schema.ObjectiveSetSchemaV2
import com.ktome.game.data.schema.ObjectiveInteractablePlacementSchemaV2
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.QuestProgressSchemaV2
import com.ktome.game.data.schema.ResourceCostSchemaV2
import com.ktome.game.data.schema.RescueInventoryPolicySchemaV2
import com.ktome.game.data.schema.RouteRewardSchemaV2
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.SchemaCombatProfile
import com.ktome.game.data.schema.SchemaLevelRange
import com.ktome.game.data.schema.SchemaMapSize
import com.ktome.game.data.schema.SchemaOffset
import com.ktome.game.data.schema.SchemaStatModifier
import com.ktome.game.data.schema.SchemaStats
import com.ktome.game.data.schema.ShopNodeSchemaV2
import com.ktome.game.data.schema.ShopOfferSchemaV2
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
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.i18n.Localizer
import com.ktome.game.model.BossDefinition
import com.ktome.game.model.MonsterCatalog
import com.ktome.game.model.MonsterTemplate
import org.yaml.snakeyaml.Yaml

class DataLoader(
    private val locale: GameLocale = GameLocale.EN_US,
    private val localizationBundle: LocalizationBundle = LocalizationBundle.load(),
) {
    private companion object {
        val REMOVED_LEGACY_EFFECT_FIELDS =
            setOf(
                "stunDuration",
                "armorBreakDuration",
                "buffDuration",
                "buffMagnitude",
                "debuffMagnitude",
                "debuffDuration",
            )
    }

    val localizer: Localizer = localizationBundle.translator(locale)

    fun loadSchemaCatalog(): SchemaCatalog {
        val telegraphSpecs = parseTelegraphSpecs(loadYamlMap("/data/telegraph/index.yaml"))
        val telegraphIds = telegraphSpecs.map(TelegraphSpec::id).toSet()
        val threatProfiles = parseThreatProfiles(loadYamlMap("/data/telegraph/threat_profiles/index.yaml"))
        val worldGraphSchema = parseWorldGraphSchema(loadYamlMap("/data/world/world_graph.yaml"))
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
            objectiveSets = parseObjectiveSetSchemas(loadYamlMap("/data/objectives/index.yaml")),
            difficulties = parseDifficultySchemas(loadYamlMap("/data/difficulties/index.yaml")),
            itemBundle = parseItemBundleSchemas(loadYamlMap("/data/items/index.yaml")),
            lootProfiles = parseLootProfileSchemas(loadYamlMap("/data/loot/index.yaml")),
            tilesets = parseNamedSchemaRefs(loadYamlMap("/data/tilesets/index.yaml"), "tilesets"),
            aiProfiles = parseAiProfiles(loadYamlMap("/data/ai/index.yaml")),
            arenas = parseNamedSchemaRefs(loadYamlMap("/data/arenas/index.yaml"), "arenas"),
            ambientProfiles = parseNamedSchemaRefs(loadYamlMap("/data/ambient/index.yaml"), "ambientProfiles"),
            visualKeys = parseStringIdSet(loadYamlMap("/data/visuals/index.yaml"), "visuals"),
            audioProfiles = parseStringIdSet(loadYamlMap("/data/audio/index.yaml"), "audioProfiles"),
        )
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
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: error("YAML resource not found: $resourcePath")
        val root = stream.use { input -> Yaml().load<Map<String, Any?>>(input) }
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
                        val offer = rawOffer.requiredMap()
                        ShopOfferSchemaV2(
                            id = offer.requiredString("id"),
                            itemBaseId = offer.optionalString("itemBaseId"),
                            inscriptionId = offer.optionalString("inscriptionId"),
                            price = offer.requiredInt("price"),
                            tags = offer.optionalStringList("tags"),
                        )
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
            )
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

    private fun parseLootProfileSchemas(root: Map<String, Any?>): List<LootProfileSchemaV2> =
        root.requiredList("lootProfiles").map { entry ->
            val profile = entry.requiredMap()
            LootProfileSchemaV2(
                id = profile.requiredString("id"),
                schemaVersion = profile.requiredInt("schemaVersion"),
                tags = profile.optionalStringList("tags"),
                itemIds = profile.optionalStringList("itemIds"),
            )
        }

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
                    minFloor = affix.requiredInt("minFloor"),
                    stats = affix.requiredMap("stats").toSchemaStatModifier(),
                    blacklistTags = affix.optionalStringList("blacklistTags"),
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
            damageType = optionalString("damageType"),
            bonusPercent = optionalDouble("bonusPercent", 0.0),
            amount = optionalInt("amount"),
        )

    private fun EquipmentPassiveSchemaV2.toRuntimePassive(): EquipmentPassive =
        when (kind) {
            "DamageVsTag" ->
                EquipmentPassive.DamageVsTag(
                    tag = requireNotNull(tag) { "DamageVsTag passive requires 'tag'." },
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
            statModifiers = stats.toRuntimeStatModifier(),
            minFloor = minFloor,
            tags = tags.toSet(),
            blacklistTags = blacklistTags.toSet(),
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
            rescuePolicy = rescuePolicy.toRuntime(),
        )

    private fun ShopOfferSchemaV2.toRuntime(): ShopOffer =
        ShopOffer(
            id = id,
            itemBaseId = itemBaseId,
            inscriptionId = inscriptionId,
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
            maxHp = optionalInt("maxHp"),
            maxStamina = optionalInt("maxStamina"),
            hpRegen = optionalDouble("hpRegen", 0.0),
            staminaRegen = optionalDouble("staminaRegen", 0.0),
            critChance = optionalDouble("critChance", 0.0),
            talentPower = optionalDouble("talentPower", 0.0),
            attackMultiplierBonus = optionalDouble("attackMultiplierBonus", 0.0),
            defenseMultiplierBonus = optionalDouble("defenseMultiplierBonus", 0.0),
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
