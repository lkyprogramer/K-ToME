package com.ktome.game.data.schema

import com.ktome.core.ai.AIProfile
import com.ktome.core.ai.BossPhaseDef
import com.ktome.core.ai.TelegraphSpec
import com.ktome.core.ai.ThreatProfileDef
import com.ktome.core.inscription.InscriptionDef
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
import com.ktome.core.world.GateCondition
import com.ktome.core.world.QuestProgress
import com.ktome.core.world.RouteReward
import com.ktome.core.world.WorldGraph
import com.ktome.core.world.ZoneConnection

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
    val objectiveSets: List<ObjectiveSetSchemaV2>,
    val difficulties: List<DifficultySchemaV2>,
    val itemBundle: ItemBundleSchemaV2,
    val lootProfiles: List<LootProfileSchemaV2>,
    val tilesets: List<NamedSchemaRef>,
    val aiProfiles: List<AIProfile>,
    val arenas: List<NamedSchemaRef>,
    val ambientProfiles: List<NamedSchemaRef>,
    val visualKeys: Set<String>,
    val audioProfiles: Set<String>,
)

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

data class LootProfileSchemaV2(
    val id: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val itemIds: List<String>,
)

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
)

data class InteractableSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val interactionTags: List<String>,
)

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
    val minFloor: Int,
    val stats: SchemaStatModifier,
    val blacklistTags: List<String> = emptyList(),
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
    val guaranteedDropIds: List<String>,
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
    val rescuePolicy: RescueInventoryPolicySchemaV2,
)

data class ShopOfferSchemaV2(
    val id: String,
    val itemBaseId: String? = null,
    val inscriptionId: String? = null,
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
    val damageType: String? = null,
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
