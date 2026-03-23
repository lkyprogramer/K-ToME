package com.ktome.game.data.schema

import com.ktome.core.talent.StatusEffectType
import com.ktome.core.item.AffixType
import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemType

data class SchemaCatalog(
    val professions: List<ProfessionSchemaV2>,
    val talents: List<TalentSchemaV2>,
    val talentTrees: List<TalentTreeSchemaV2>,
    val monsters: List<MonsterSchemaV2>,
    val bossEncounters: List<BossEncounterSchemaV2>,
    val zones: List<ZoneSchemaV2>,
    val interactables: List<InteractableSchemaV2>,
    val objectiveSets: List<ObjectiveSetSchemaV2>,
    val difficulties: List<DifficultySchemaV2>,
    val itemBundle: ItemBundleSchemaV2,
    val lootProfiles: List<LootProfileSchemaV2>,
    val tilesets: List<NamedSchemaRef>,
    val aiProfiles: List<AIProfileSchemaV2>,
    val arenas: List<NamedSchemaRef>,
    val ambientProfiles: List<NamedSchemaRef>,
    val visualKeys: Set<String>,
    val audioProfiles: Set<String>,
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

data class AIProfileSchemaV2(
    val id: String,
    val schemaVersion: Int,
    val talentPriority: List<String>,
    val skipRules: List<AITalentSkipRuleSchemaV2>,
    val triggers: List<AITriggerSchemaV2> = emptyList(),
)

data class AITalentSkipRuleSchemaV2(
    val talentId: String,
    val selfHasStatus: StatusEffectType,
)

enum class AITriggerConditionKindSchemaV2 {
    ON_COMBAT_START,
    HP_BELOW_RATIO,
}

enum class AITriggerActionKindSchemaV2 {
    FORCE_TALENT,
}

data class AITriggerSchemaV2(
    val triggerId: String,
    val condition: AITriggerConditionKindSchemaV2,
    val threshold: Double? = null,
    val action: AITriggerActionKindSchemaV2,
    val talentId: String,
    val postMessageKey: String? = null,
    val postMessageArgs: Map<String, String> = emptyMap(),
    val once: Boolean = false,
)

data class ProfessionSchemaV2(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val resourceType: String,
    val baseStats: SchemaStats,
    val combatProfile: SchemaCombatProfile,
    val statGrowth: SchemaStats,
    val startingResources: Map<String, Int>,
    val resourceCaps: Map<String, Int>,
    val talentTrees: List<String>,
    val startingTalents: List<String>,
    val startingKit: List<String>,
    val unlockCondition: String,
    val soloContract: String,
)

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
    val category: String,
    val damageType: String?,
    val powerDimension: String?,
    val kind: String,
    val cooldown: Int,
    val castTime: Int,
    val range: Int,
    val minRange: Int,
    val areaRadius: Int,
    val resourceCosts: Map<String, Int>,
    val unlockLevel: Int,
    val targeting: String,
    val requirements: TalentRequirementsSchemaV2,
    val levelEffects: Map<Int, TalentLevelEffectSchemaV2>,
    val keywords: List<String>,
    val callbacks: List<String>,
    val telegraph: String,
    val treeId: String,
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

data class TalentTreeSchemaV2(
    val id: String,
    val professionId: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val layout: String,
    val nodes: List<String>,
)

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
    val bossTemplateId: String,
    val arenaId: String,
    val phases: List<String>,
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
    val monsterPools: List<String>,
    val elitePools: List<String>,
    val bossEncounterId: String?,
    val objectiveSetId: String?,
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
    val minFloor: Int,
    val stats: SchemaStatModifier,
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
