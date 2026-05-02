package com.ktome.game.data.schema

import com.ktome.core.ai.AIProfile
import com.ktome.core.ai.BossPhaseDef
import com.ktome.core.ai.TelegraphSpec
import com.ktome.core.ai.ThreatProfileDef
import com.ktome.core.inscription.InscriptionDef
import com.ktome.core.mapgen.BiomeFamilyDef
import com.ktome.core.mapgen.PatternRoomDef
import com.ktome.core.mapgen.PatternTemplateDef
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.RoomDef
import com.ktome.core.mapgen.VaultDef
import com.ktome.core.mapgen.VaultTemplateDef
import com.ktome.core.mapgen.ZoneMapgenProfile
import com.ktome.core.mapgen.ZoneRewardProfile
import com.ktome.core.economy.AffordableRescueSlotPolicy
import com.ktome.core.economy.RescueInventoryPolicy
import com.ktome.core.economy.ShopNode
import com.ktome.core.economy.ShopOffer
import com.ktome.core.profession.ProfessionTier
import com.ktome.core.profession.ReleaseUnlockCondition
import com.ktome.core.profession.SoloContractDef
import com.ktome.core.profile.ClassUnlockState
import com.ktome.core.race.RaceDef
import com.ktome.core.resource.ResourceAxis
import com.ktome.core.resource.ResourceProfileRef
import com.ktome.core.resource.ResourceType
import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.core.item.AffixEquipType
import com.ktome.core.item.AffixType
import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemType
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTier
import com.ktome.core.world.GateCondition
import com.ktome.core.world.QuestProgress
import com.ktome.core.world.RouteReward
import com.ktome.core.world.WorldGraph
import com.ktome.core.world.ZoneConnection
import com.ktome.core.world.solvability.ContentRef
import com.ktome.core.world.solvability.DiscoveryRule
import com.ktome.core.world.solvability.SearchBindingId
import com.ktome.game.elites.ActionWeightProfileDef
import com.ktome.game.elites.BossVariantDef
import com.ktome.game.elites.EliteMutationConfig
import com.ktome.game.elites.EliteMutationDef
import com.ktome.game.elites.MutationStatModifierDef
import com.ktome.game.hidden.HiddenEventDef
import com.ktome.game.hidden.ReturnBridgePolicy
import com.ktome.game.hidden.SecretZoneDef

data class SchemaCatalog(
    val professions: List<ProfessionSchemaV2>,
    val races: List<RaceDef> = emptyList(),
    val inscriptions: List<InscriptionDef> = emptyList(),
    val statuses: List<StatusSchemaV2>,
    val talents: List<TalentSchemaV2>,
    val talentTrees: List<TalentTreeSchemaV2>,
    val monsters: List<MonsterSchemaV2>,
    val bossEncounters: List<BossEncounterSchemaV2>,
    val telegraphSpecs: List<TelegraphSpec>,
    val threatProfiles: List<ThreatProfileDef>,
    val zones: List<ZoneSchemaV2>,
    val worldGraph: WorldGraph = WorldGraph(startZoneId = "shattered_outpost", connections = emptyList()),
    val routeRewards: List<RouteReward> = emptyList(),
    val questProgressions: List<QuestProgress> = emptyList(),
    val shopNodes: List<ShopNode> = emptyList(),
    val interactables: List<InteractableSchemaV2>,
    val rewardRoutingEntries: List<RewardRoutingEntrySchemaV1> = emptyList(),
    val buildIdentities: List<ProfessionBuildIdentitySchemaV1> = emptyList(),
    val objectiveSets: List<ObjectiveSetSchemaV2>,
    val difficulties: List<DifficultySchemaV2>,
    val itemBundle: ItemBundleSchemaV2,
    val lootProfiles: List<LootProfileSchemaV3>,
    val eliteMutationConfig: EliteMutationConfig = EliteMutationConfig(),
    val mutationStatModifiers: List<MutationStatModifierDef> = emptyList(),
    val eliteMutations: List<EliteMutationDef> = emptyList(),
    val bossVariants: List<BossVariantDef> = emptyList(),
    val actionWeightProfiles: List<ActionWeightProfileDef> = emptyList(),
    val hiddenEvents: List<HiddenEventDef> = emptyList(),
    val secretZones: List<SecretZoneDef> = emptyList(),
    val tilesets: List<NamedSchemaRef>,
    val aiProfiles: List<AIProfile>,
    val arenas: List<NamedSchemaRef>,
    val ambientProfiles: List<NamedSchemaRef>,
    val roomDefs: List<RoomDef> = emptyList(),
    val patternTemplates: List<PatternTemplateDef> = emptyList(),
    val patternRooms: List<PatternRoomDef> = emptyList(),
    val vaultTemplates: List<VaultTemplateDef> = emptyList(),
    val vaults: List<VaultDef> = emptyList(),
    val biomeFamilies: List<BiomeFamilyDef> = emptyList(),
    val zoneMapgenProfiles: List<ZoneMapgenProfile> = emptyList(),
    val zoneRewardProfiles: List<ZoneRewardProfile> = emptyList(),
    val visualKeys: Set<String>,
    val audioProfiles: Set<String>,
) {
    init {
        val zoneIds = zones.mapTo(linkedSetOf(), ZoneSchemaV2::id)
        val interactableIds = interactables.mapTo(linkedSetOf(), InteractableSchemaV2::id)
        val lootProfileIds = lootProfiles.mapTo(linkedSetOf(), LootProfileSchemaV3::id)
        val itemIds = itemBundle.items.mapTo(linkedSetOf(), ItemSchemaV2::id)
        val itemSchemasById = itemBundle.items.associateBy(ItemSchemaV2::id)
        val foundationProfessionsById =
            professions
                .filter { profession ->
                    profession.tier == ProfessionTier.BASE &&
                        profession.tags.any { tag -> normalizeSchemaTag(tag) == "foundation" }
                }.associateBy(ProfessionSchemaV2::id)
        val duplicateRoutingKeys =
            rewardRoutingEntries
                .groupBy { entry -> RewardRoutingEntryKey(entry.zoneId, entry.interactableId, entry.grantMode) }
                .filterValues { entries -> entries.size > 1 }
                .keys
        require(duplicateRoutingKeys.isEmpty()) {
            "Duplicate reward routing entries found for ${duplicateRoutingKeys.sortedBy(RewardRoutingEntryKey::sortKey).joinToString { key -> key.sortKey() }}."
        }
        rewardRoutingEntries.forEach { entry ->
            require(entry.zoneId in zoneIds) {
                "Reward routing entry '${entry.zoneId}/${entry.interactableId}/${entry.grantMode.name}' references unknown zone '${entry.zoneId}'."
            }
            require(entry.interactableId in interactableIds) {
                "Reward routing entry '${entry.zoneId}/${entry.interactableId}/${entry.grantMode.name}' references unknown interactable '${entry.interactableId}'."
            }
            val unknownProfileIds = entry.profileIds.filterNot(lootProfileIds::contains)
            require(unknownProfileIds.isEmpty()) {
                "Reward routing entry '${entry.zoneId}/${entry.interactableId}/${entry.grantMode.name}' references unknown loot profiles ${unknownProfileIds.joinToString()}."
            }
            require(entry.fallbackBaseId in itemIds) {
                "Reward routing entry '${entry.zoneId}/${entry.interactableId}/${entry.grantMode.name}' references unknown fallback item '${entry.fallbackBaseId}'."
            }
        }
        val duplicateBuildIdentityProfessionIds =
            buildIdentities
                .groupBy(ProfessionBuildIdentitySchemaV1::professionId)
                .filterValues { entries -> entries.size > 1 }
                .keys
        require(duplicateBuildIdentityProfessionIds.isEmpty()) {
            "Duplicate build identity entries found for ${duplicateBuildIdentityProfessionIds.sorted().joinToString()}."
        }
        buildIdentities.forEach { identity ->
            require(identity.professionId in foundationProfessionsById) {
                "Build identity entry '${identity.professionId}' must reference a foundation base profession."
            }
            val unknownCapstoneIds = identity.capstoneBaseIds.filterNot(itemIds::contains)
            require(unknownCapstoneIds.isEmpty()) {
                "Build identity entry '${identity.professionId}' references unknown capstone item ids ${unknownCapstoneIds.joinToString()}."
            }
            val unknownNonWeaponPayoffIds = identity.nonWeaponCapstoneBaseIds.filterNot(itemIds::contains)
            require(unknownNonWeaponPayoffIds.isEmpty()) {
                "Build identity entry '${identity.professionId}' references unknown non-weapon payoff item ids ${unknownNonWeaponPayoffIds.joinToString()}."
            }
            identity.capstoneBaseIds.forEach { itemId ->
                val item = requireNotNull(itemSchemasById[itemId])
                val tags = item.tags.mapTo(linkedSetOf(), ::normalizeSchemaTag)
                require("capstone" in tags && identity.professionId in tags) {
                    "Build identity capstone '$itemId' must carry profession '${identity.professionId}' and 'capstone' tags."
                }
            }
            identity.nonWeaponCapstoneBaseIds.forEach { itemId ->
                val item = requireNotNull(itemSchemasById[itemId])
                val tags = item.tags.mapTo(linkedSetOf(), ::normalizeSchemaTag)
                require("non_weapon_capstone" in tags && item.slot != EquipSlot.WEAPON) {
                    "Build identity non-weapon capstone '$itemId' must carry 'non_weapon_capstone' tag and avoid WEAPON slot."
                }
            }
        }
    }
}

private data class RewardRoutingEntryKey(
    val zoneId: String,
    val interactableId: String,
    val grantMode: RewardRoutingGrantMode,
) {
    fun sortKey(): String = "$zoneId/$interactableId/${grantMode.name}"
}

data class StatusSchemaV2(
    val id: String,
    val effectType: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val category: String,
    val carrierKind: String,
    val stackingRule: String? = null,
    val stackCap: Int? = null,
    val replacePolicy: String? = null,
    val uniquenessKey: String? = null,
    val exclusiveGroup: String? = null,
    val sourceScopedUnique: Boolean = false,
    val dispellable: Boolean? = null,
    val remoteRemovalPolicy: String? = null,
    val breaksOnActualDamage: Boolean = false,
    val consumedOnDamageType: String? = null,
    val consumedDamageMultiplier: Double? = null,
    val stats: SchemaStatModifier = SchemaStatModifier(),
)

data class NamedSchemaRef(
    val id: String,
    val schemaVersion: Int,
)

enum class LootPoolStrategy {
    FIXED_LIST,
    TAG_WEIGHTED,
}

enum class LootProfileLocalIdentityCategory(
    val token: String,
) {
    SECRET("secret"),
    CADENCE("cadence"),
    REWARD("reward"),
    OTHER("other"),
    ;

    val requiresCanonicalZoneId: Boolean
        get() = this != OTHER
}

fun LootProfileSchemaV3.localIdentityCategory(): LootProfileLocalIdentityCategory =
    when {
        "secret" in tags -> LootProfileLocalIdentityCategory.SECRET
        "cadence" in tags -> LootProfileLocalIdentityCategory.CADENCE
        "reward" in tags -> LootProfileLocalIdentityCategory.REWARD
        else -> LootProfileLocalIdentityCategory.OTHER
    }

data class LootProfileSchemaV3(
    val id: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val itemIds: List<String>,
    val rewardBudget: Int,
    val canonicalZoneId: String? = null,
    val poolStrategy: LootPoolStrategy,
    val itemTagFilter: List<String> = emptyList(),
    val excludeIds: List<String> = emptyList(),
    val typeWeights: Map<ItemType, Int> = emptyMap(),
    val slotBias: Map<EquipSlot, Int> = emptyMap(),
    val specialTemplateTagPreference: List<String> = emptyList(),
    val affixTagPreference: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "LootProfileSchemaV3.id must not be blank." }
        require(schemaVersion > 0) { "LootProfileSchemaV3.schemaVersion must be positive." }
        require(tags.none(String::isBlank)) { "LootProfileSchemaV3.tags must not contain blanks." }
        require(itemIds.none(String::isBlank)) { "LootProfileSchemaV3.itemIds must not contain blanks." }
        require(rewardBudget >= 0) { "LootProfileSchemaV3.rewardBudget must not be negative." }
        require(canonicalZoneId == null || canonicalZoneId.isNotBlank()) {
            "LootProfileSchemaV3.canonicalZoneId must not be blank when provided."
        }
        require(itemTagFilter.none(String::isBlank)) { "LootProfileSchemaV3.itemTagFilter must not contain blanks." }
        require(excludeIds.none(String::isBlank)) { "LootProfileSchemaV3.excludeIds must not contain blanks." }
        require(typeWeights.values.all { weight -> weight > 0 }) { "LootProfileSchemaV3.typeWeights must be positive." }
        require(slotBias.values.all { weight -> weight > 0 }) { "LootProfileSchemaV3.slotBias must be positive." }
        require(specialTemplateTagPreference.none(String::isBlank)) {
            "LootProfileSchemaV3.specialTemplateTagPreference must not contain blanks."
        }
        require(affixTagPreference.none(String::isBlank)) {
            "LootProfileSchemaV3.affixTagPreference must not contain blanks."
        }
        val localIdentityCategory = localIdentityCategory()
        require(!localIdentityCategory.requiresCanonicalZoneId || canonicalZoneId != null) {
            "Loot profile '$id' tagged as ${localIdentityCategory.token} must declare canonicalZoneId."
        }
    }
}

data class ProfessionSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val resourceHintKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val tier: ProfessionTier = ProfessionTier.BASE,
    val resourceProfiles: List<ResourceProfileRef> =
        listOf(
            ResourceProfileRef(
                axis = ResourceAxis.STAMINA,
                initialCurrent = 100,
                max = 100,
            ),
        ),
    val primarySpendAxis: ResourceAxis,
    val stateAxis: ResourceAxis? = null,
    val baseStats: SchemaStats,
    val combatProfile: SchemaCombatProfile,
    val statGrowth: SchemaStats,
    val talentTrees: List<String>,
    val startingTalents: List<String>,
    val startingKit: List<String>,
    val startingInscriptions: List<String> = emptyList(),
    val initialUnlockState: ClassUnlockState = ClassUnlockState.RELEASE_UNLOCKED,
    val releaseUnlockCondition: ReleaseUnlockCondition? = null,
    val soloContract: SoloContractDef =
        SoloContractDef(
            offenseTags = listOf("offense"),
            defenseTags = listOf("defense"),
            mobilityTags = listOf("mobility"),
            aoeAnswerTags = listOf("aoe"),
            bossAnswerTags = listOf("boss"),
            panicAnswerTags = listOf("panic"),
        ),
) {
    val resourceType: String
        get() = requireNotNull(primarySpendAxis.asResourceTypeOrNull()) {
            "Profession '$id' primary axis '$primarySpendAxis' must resolve to a spendable resource type."
        }.name

    val startingResources: Map<String, Int>
        get() =
            resourceProfiles
                .mapNotNull { profile -> profile.resourceType?.let { type -> type.name to profile.initialCurrent } }
                .toMap(linkedMapOf())

    val resourceCaps: Map<String, Int>
        get() =
            resourceProfiles
                .mapNotNull { profile -> profile.resourceType?.let { type -> type.name to profile.max } }
                .toMap(linkedMapOf())

    fun resourceProfile(axis: ResourceAxis): ResourceProfileRef? =
        resourceProfiles.firstOrNull { profile -> profile.axis == axis }
}

data class SchemaCombatProfile(
    val baseAttack: Int,
    val baseDefense: Int,
    val baseAccuracy: Int = 10,
    val baseEvasion: Int = 5,
    val baseSpeed: Int = 100,
    val baseHp: Int = 50,
    val baseStamina: Int = 40,
    val baseHpRegen: Double = 1.0,
)

data class TalentSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val maxPoints: Int,
    val tier: Int,
    val category: String,
    val damageType: String?,
    val powerDimension: String?,
    val kind: String,
    val cooldown: Int,
    val castTime: String,
    val targeting: TalentTargetingSchemaV2,
    val resourceCosts: List<ResourceCostSchemaV2>,
    val unlockLevel: Int,
    val requirements: TalentRequirementsSchemaV2,
    val levelEffects: Map<Int, TalentLevelEffectSchemaV2>,
    val breakpoints: List<TalentBreakpointSchemaV2>,
    val keywords: List<String>,
    val callbacks: List<String>,
    val telegraphRef: String? = null,
    val equilibriumAffinity: String? = null,
    val aiHints: TalentAiHintsSchemaV2?,
    val treeId: String,
)

data class TalentTargetingSchemaV2(
    val type: String,
    val range: Int,
    val minRange: Int,
    val areaRadius: Int,
    val requiresLineOfSight: Boolean = true,
    val friendlyFire: Boolean = false,
)

data class ResourceCostSchemaV2(
    val axis: String,
    val amount: Int,
)

data class TalentRequirementsSchemaV2(
    val talentPrereqs: List<TalentPrerequisiteSchemaV2> = emptyList(),
)

data class TalentPrerequisiteSchemaV2(
    val talentId: String,
    val minRank: Int,
)

data class TalentLevelEffectSchemaV2(
    val damageMultiplier: Double = 1.0,
    val knockback: Int = 0,
    val rangeBonus: Int = 0,
    val healFraction: Double = 0.0,
    val resourceRestoreFraction: Double = 0.0,
    val associatedEffects: List<AssociatedStatusEffectSchemaV2> = emptyList(),
    val cleanseEffect: CleanseEffectSchemaV2? = null,
)

data class AssociatedStatusEffectSchemaV2(
    val effectId: String,
    val effectType: String,
    val trigger: String,
    val targetScope: String,
    val applicationPolicy: String,
    val saveDimension: String? = null,
    val duration: Int = 0,
    val magnitude: Double = 0.0,
)

data class CleanseEffectSchemaV2(
    val effectId: String = "cleanse",
    val trigger: String = "ON_CAST",
    val targetScope: String = "SELF",
    val applicationPolicy: String = "INSTANT_ACTION",
    val maxEffectsRemoved: Int = 1,
)

data class TalentAiHintsSchemaV2(
    val role: String,
    val preferredRange: IntRangeSchemaV2? = null,
    val isSustainToggle: Boolean = false,
)

data class IntRangeSchemaV2(
    val start: Int,
    val endInclusive: Int,
)

data class TalentBreakpointSchemaV2(
    val atRank: Int,
    val descriptionAddendumKey: String? = null,
)

data class TalentTreeSchemaV2(
    val id: String,
    val professionId: String = "",
    val raceId: String? = null,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val layout: String,
    val nodes: List<String>,
) {
    fun ownerRef(): com.ktome.game.TalentTreeOwnerRef =
        if (raceId != null) {
            com.ktome.game.TalentTreeOwnerRef(
                ownerType = TalentTreeOwnerType.RACE,
                treeOwnerId = raceId,
            )
        } else {
            com.ktome.game.TalentTreeOwnerRef(
                ownerType = TalentTreeOwnerType.PROFESSION,
                treeOwnerId = professionId,
            )
        }
}

data class MonsterSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val archetype: String,
    val glyph: Char,
    val colorHex: String,
    val stats: SchemaStats,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseAccuracy: Int,
    val baseEvasion: Int,
    val speed: Int,
    val ai: String,
    val aiProfileId: String,
    val lootProfileId: String,
    val resistances: Map<String, Int>,
    val talents: Map<String, Int>,
    val expReward: Int,
    val spawnFloors: List<Int>,
    val spawnWeight: Int,
)

data class BossEncounterSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val templateId: String,
    val arenaId: String,
    val phases: List<BossPhaseDef>,
    val rewards: List<String>,
)

data class ZoneSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val biome: String,
    val floorCount: Int,
    val mapSize: SchemaMapSize,
    val recommendedLevel: SchemaLevelRange,
    val environmentTheme: String,
    val specialMechanics: List<String>,
    val tilesetKey: String,
    val ambientProfile: String,
    val worldRole: String,
    val monsterPools: List<String>,
    val elitePools: List<String>,
    val bossEncounterId: String?,
    val objectiveSetId: String?,
    val shopNodeId: String? = null,
    val uniqueContentTag: String? = null,
    val mapgenProfileId: String? = null,
    val mapgenProfileBindings: List<SchemaFloorMapgenProfileBinding> = emptyList(),
    val rewardProfileId: String? = null,
) {
    init {
        require(mapgenProfileBindings.distinctBy(SchemaFloorMapgenProfileBinding::floorIndex).size == mapgenProfileBindings.size) {
            "ZoneSchemaV2.mapgenProfileBindings must not declare duplicate floor indices for zone '$id'."
        }
        require(mapgenProfileBindings.all { binding -> binding.floorIndex in 1..floorCount }) {
            "ZoneSchemaV2.mapgenProfileBindings must stay within the floor range for zone '$id'."
        }
    }

    fun resolvedMapgenProfileId(floorIndex: Int): String? =
        mapgenProfileBindings
            .firstOrNull { binding -> binding.floorIndex == floorIndex }
            ?.profileId
            ?: mapgenProfileId
}

data class SchemaFloorMapgenProfileBinding(
    val floorIndex: Int,
    val profileId: String,
) {
    init {
        require(floorIndex > 0) { "SchemaFloorMapgenProfileBinding.floorIndex must be positive." }
        require(profileId.isNotBlank()) { "SchemaFloorMapgenProfileBinding.profileId must not be blank." }
    }
}

data class InteractableSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val interactionTags: List<String>,
    val shopNodeId: String? = null,
)

enum class RewardRoutingGrantMode {
    GROUND_CACHE,
    SUPPORT_GRANT,
}

data class RewardRoutingEntrySchemaV1(
    val zoneId: String,
    val interactableId: String,
    val grantMode: RewardRoutingGrantMode,
    val schemaVersion: Int,
    val profileIds: List<String>,
    val fallbackBaseId: String,
) {
    init {
        require(zoneId.isNotBlank()) { "RewardRoutingEntrySchemaV1.zoneId must not be blank." }
        require(interactableId.isNotBlank()) { "RewardRoutingEntrySchemaV1.interactableId must not be blank." }
        require(schemaVersion > 0) { "RewardRoutingEntrySchemaV1.schemaVersion must be positive." }
        require(profileIds.none(String::isBlank)) { "RewardRoutingEntrySchemaV1.profileIds must not contain blanks." }
        require(fallbackBaseId.isNotBlank()) { "RewardRoutingEntrySchemaV1.fallbackBaseId must not be blank." }
    }
}

data class ProfessionBuildIdentityFloorsSchemaV1(
    val seenMinCount: Int,
    val adoptionMinCount: Int,
    val nonWeaponMinCount: Int,
) {
    init {
        require(seenMinCount >= 0) { "ProfessionBuildIdentityFloorsSchemaV1.seenMinCount must not be negative." }
        require(adoptionMinCount >= 0) { "ProfessionBuildIdentityFloorsSchemaV1.adoptionMinCount must not be negative." }
        require(nonWeaponMinCount >= 0) { "ProfessionBuildIdentityFloorsSchemaV1.nonWeaponMinCount must not be negative." }
    }
}

data class ProfessionBuildIdentitySchemaV1(
    val professionId: String,
    val schemaVersion: Int,
    val capstoneBaseIds: List<String>,
    val nonWeaponCapstoneBaseIds: List<String>,
    val preferredRewardSources: List<MilestoneRewardSource>,
    val preferredReplacementSlots: List<EquipSlot>,
    val terminalIdentityTags: List<String>,
    val buildIdentityFloors: ProfessionBuildIdentityFloorsSchemaV1,
) {
    init {
        require(professionId.isNotBlank()) { "ProfessionBuildIdentitySchemaV1.professionId must not be blank." }
        require(schemaVersion > 0) { "ProfessionBuildIdentitySchemaV1.schemaVersion must be positive." }
        require(capstoneBaseIds.isNotEmpty()) { "ProfessionBuildIdentitySchemaV1.capstoneBaseIds must not be empty." }
        require(capstoneBaseIds.none(String::isBlank)) { "ProfessionBuildIdentitySchemaV1.capstoneBaseIds must not contain blanks." }
        require(nonWeaponCapstoneBaseIds.none(String::isBlank)) {
            "ProfessionBuildIdentitySchemaV1.nonWeaponCapstoneBaseIds must not contain blanks."
        }
        require(preferredRewardSources.isNotEmpty()) {
            "ProfessionBuildIdentitySchemaV1.preferredRewardSources must not be empty."
        }
        require(preferredRewardSources.size == preferredRewardSources.toSet().size) {
            "ProfessionBuildIdentitySchemaV1.preferredRewardSources must not contain duplicates."
        }
        require(preferredReplacementSlots.isNotEmpty()) {
            "ProfessionBuildIdentitySchemaV1.preferredReplacementSlots must not be empty."
        }
        require(preferredReplacementSlots.size == preferredReplacementSlots.toSet().size) {
            "ProfessionBuildIdentitySchemaV1.preferredReplacementSlots must not contain duplicates."
        }
        require(terminalIdentityTags.none(String::isBlank)) {
            "ProfessionBuildIdentitySchemaV1.terminalIdentityTags must not contain blanks."
        }
    }
}

private fun normalizeSchemaTag(tag: String): String = tag.trim().lowercase()

data class ObjectiveSetSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val interactables: List<String>,
    val placements: List<ObjectiveInteractablePlacementSchemaV2>,
    val completionRule: String,
    val linkedQuestId: String? = null,
    val questObjectiveId: String? = null,
)

data class ObjectiveInteractablePlacementSchemaV2(
    val interactableId: String,
    val floor: Int,
    val anchor: String,
    val offset: SchemaOffset = SchemaOffset(),
)

data class SchemaOffset(
    val x: Int = 0,
    val y: Int = 0,
)

data class DifficultySchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val monsterHpMultiplier: Double,
    val monsterDamageMultiplier: Double,
    val xpMultiplier: Double,
    val lootRarityBonus: Double,
    val prerequisites: List<String>,
)

data class ItemBundleSchemaV2(
    val materials: List<MaterialSchemaV2>,
    val affixes: List<AffixSchemaV2>,
    val items: List<ItemSchemaV2>,
    val uniqueTemplates: List<SpecialItemTemplateSchemaV2> = emptyList(),
    val artifactTemplates: List<SpecialItemTemplateSchemaV2> = emptyList(),
)

data class MaterialSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val minFloor: Int,
    val stats: SchemaStatModifier,
)

data class AffixSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val type: AffixType,
    val equipType: AffixEquipType,
    val tier: Int,
    val cost: Int,
    val affixFamily: String,
    val exclusiveGroup: String? = null,
    val minFloor: Int,
    val stats: SchemaStatModifier,
    val blacklistTags: List<String> = emptyList(),
    val passive: EquipmentPassiveSchemaV2? = null,
)

data class SpecialItemTemplateSchemaV2(
    val id: String,
    val itemId: String,
    val specialTier: SpecialTier,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val allowedSourceTiers: List<SourceTier>,
    val allowedZones: List<String>,
    val fixedAffixIds: List<String> = emptyList(),
    val fixedMaterialId: String? = null,
)

data class WorldGraphSchemaV2(
    val startZoneId: String,
    val connections: List<ZoneConnectionSchemaV2>,
)

data class ZoneConnectionSchemaV2(
    val id: String,
    val fromZoneId: String,
    val toZoneId: String,
    val isBidirectional: Boolean = true,
    val gate: GateCondition = GateCondition(),
)

data class RouteRewardSchemaV2(
    val routeId: String,
    val claimPolicy: String,
    val levelBandRef: String,
    val shardReward: Int,
    val guaranteedUtilityDropIds: List<String>,
    val milestoneRewardProfileIds: List<String>,
    val rescueTags: List<String>,
)

data class QuestProgressSchemaV2(
    val questId: String,
    val objectiveStates: Map<String, String>,
    val completionFlags: List<String>,
)

data class ShopNodeSchemaV2(
    val id: String,
    val zoneId: String,
    val nameKey: String,
    val inventory: List<ShopOfferSchemaV2>,
    val refreshInventory: List<ShopOfferSchemaV2> = emptyList(),
    val rescuePolicy: RescueInventoryPolicySchemaV2,
)

data class ShopOfferSchemaV2(
    val id: String,
    val itemBaseId: String? = null,
    val inscriptionId: String? = null,
    val serviceType: String? = null,
    val price: Int,
    val tags: List<String> = emptyList(),
)

data class RescueInventoryPolicySchemaV2(
    val guaranteedTags: List<String>,
    val affordability: AffordableRescueSlotPolicySchemaV2,
)

data class AffordableRescueSlotPolicySchemaV2(
    val checkpointId: String,
    val expectedShardBudgetByCheckpoint: Int,
    val mandatoryAffordableItemCount: Int,
    val requiredAffordableTags: List<String>,
)

data class EquipmentPassiveSchemaV2(
    val kind: String,
    val tag: String? = null,
    val statusId: String? = null,
    val resourceType: String? = null,
    val condition: String? = null,
    val terrainTag: String? = null,
    val damageType: String? = null,
    val statModifier: SchemaStatModifier? = null,
    val chance: Double = 0.0,
    val duration: Int = 0,
    val magnitude: Double = 0.0,
    val bonusPercent: Double = 0.0,
    val amount: Int = 0,
)

data class ItemSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val type: ItemType,
    val slot: EquipSlot? = null,
    val glyph: Char,
    val colorHex: String,
    val baseAttack: Int? = null,
    val baseDefense: Int? = null,
    val stats: SchemaStatModifier = SchemaStatModifier(),
    val materials: List<String> = emptyList(),
    val dropFloors: List<Int> = emptyList(),
    val dropWeight: Int = 1,
    val effect: ConsumableEffect? = null,
    val resourceTypeId: String? = null,
    val magnitude: Int = 0,
    val passive: EquipmentPassiveSchemaV2? = null,
)

data class SchemaStats(
    val str: Int,
    val dex: Int,
    val con: Int,
    val wil: Int,
)

data class SchemaStatModifier(
    val str: Int = 0,
    val dex: Int = 0,
    val con: Int = 0,
    val wil: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val accuracy: Int = 0,
    val evasion: Int = 0,
    val speed: Int = 0,
    val castSpeedRating: Int = 0,
    val maxHp: Int = 0,
    val maxStamina: Int = 0,
    val hpRegen: Double = 0.0,
    val staminaRegen: Double = 0.0,
    val critChance: Double = 0.0,
    val talentPower: Double = 0.0,
    val attackMultiplierBonus: Double = 0.0,
    val defenseMultiplierBonus: Double = 0.0,
)

data class SchemaMapSize(
    val width: Int,
    val height: Int,
)

data class SchemaLevelRange(
    val min: Int,
    val max: Int,
)
