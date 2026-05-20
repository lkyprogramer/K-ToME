package com.ktome.core.item

import com.ktome.core.combat.DamageType
import com.ktome.core.ecs.EntityId
import com.ktome.core.loot.AffixCost
import com.ktome.core.loot.RarityTier
import com.ktome.core.loot.SourceTier
import com.ktome.core.loot.SpecialTier
import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.resource.ResourceType
import java.util.concurrent.ConcurrentHashMap

enum class EquipSlot {
    WEAPON,
    OFF_HAND,
    ARMOR,
}

enum class ItemType {
    WEAPON,
    ARMOR,
    CONSUMABLE,
}

enum class AffixType {
    PREFIX,
    SUFFIX,
}

enum class AffixEquipType {
    WEAPON,
    ARMOR,
}

enum class ConsumableEffect {
    HEAL,
    TELEPORT,
    RESTORE_RESOURCE,
}

sealed interface PassiveEffect {
    data class OnHitStatusProc(
        val statusId: String,
        val chance: Double,
        val duration: Int,
        val magnitude: Double = 0.0,
    ) : PassiveEffect {
        init {
            require(statusId.isNotBlank()) { "OnHitStatusProc.statusId must not be blank." }
            require(chance in 0.0..1.0) { "OnHitStatusProc.chance must be between 0 and 1." }
            require(duration > 0) { "OnHitStatusProc.duration must be positive." }
        }
    }

    data class OnKillResourceRestore(
        val resourceType: ResourceType,
        val amount: Int,
    ) : PassiveEffect {
        init {
            require(amount > 0) { "OnKillResourceRestore.amount must be positive." }
        }
    }

    data class ConditionalStatBonus(
        val condition: PassiveCondition,
        val statModifier: StatModifier,
        val statusId: String? = null,
    ) : PassiveEffect {
        init {
            require(statusId?.isNotBlank() != false) {
                "ConditionalStatBonus.statusId must not be blank when present."
            }
            require(condition != PassiveCondition.SELF_HAS_STATUS || statusId != null) {
                "ConditionalStatBonus SELF_HAS_STATUS requires statusId."
            }
        }
    }

    data class TerrainAffinityBonus(
        val terrainTag: TerrainTag,
        val statModifier: StatModifier,
    ) : PassiveEffect

    data class StatModifierEffect(
        val statModifier: StatModifier,
    ) : PassiveEffect

    data class DamageVsTag(
        val tag: String,
        val bonusPercent: Double,
    ) : PassiveEffect

    data class DamageVsStatus(
        val statusId: String,
        val bonusPercent: Double,
    ) : PassiveEffect

    data class HpRegenPerTurn(
        val amount: Int,
    ) : PassiveEffect

    data class DamageTypeBonus(
        val type: DamageType,
        val bonusPercent: Double,
    ) : PassiveEffect

    data class ResistanceBonus(
        val damageType: DamageType,
        val amount: Int,
    ) : PassiveEffect
}

enum class PassiveSourceKind {
    EQUIPMENT,
    TALENT,
}

data class PassiveSource(
    val kind: PassiveSourceKind,
    val sourceId: String,
    val sourceTemplateId: String,
    val itemEntityId: EntityId? = null,
    val affixId: String? = null,
    val sourceSpecialTemplateId: String? = null,
    val talentRank: Int? = null,
    val passive: PassiveEffect,
) {
    init {
        require(sourceId.isNotBlank()) { "PassiveSource.sourceId must not be blank." }
        require(sourceTemplateId.isNotBlank()) { "PassiveSource.sourceTemplateId must not be blank." }
        require(affixId?.isNotBlank() != false) { "PassiveSource.affixId must not be blank when present." }
        require(sourceSpecialTemplateId?.isNotBlank() != false) {
            "PassiveSource.sourceSpecialTemplateId must not be blank when present."
        }
        require(talentRank == null || talentRank > 0) { "PassiveSource.talentRank must be positive when present." }
    }
}

object PassiveEffectKindIds {
    const val ON_HIT_STATUS_PROC: String = "OnHitStatusProc"
    const val ON_KILL_RESOURCE_RESTORE: String = "OnKillResourceRestore"
    const val CONDITIONAL_STAT_BONUS: String = "ConditionalStatBonus"
    const val TERRAIN_AFFINITY_BONUS: String = "TerrainAffinityBonus"
    const val STAT_MODIFIER: String = "StatModifier"
    const val DAMAGE_VS_TAG: String = "DamageVsTag"
    const val DAMAGE_VS_STATUS: String = "DamageVsStatus"
    const val HP_REGEN_PER_TURN: String = "HpRegenPerTurn"
    const val DAMAGE_TYPE_BONUS: String = "DamageTypeBonus"
    const val RESISTANCE_BONUS: String = "ResistanceBonus"
}

fun PassiveEffect.kindId(): String =
    when (this) {
        is PassiveEffect.OnHitStatusProc -> PassiveEffectKindIds.ON_HIT_STATUS_PROC
        is PassiveEffect.OnKillResourceRestore -> PassiveEffectKindIds.ON_KILL_RESOURCE_RESTORE
        is PassiveEffect.ConditionalStatBonus -> PassiveEffectKindIds.CONDITIONAL_STAT_BONUS
        is PassiveEffect.TerrainAffinityBonus -> PassiveEffectKindIds.TERRAIN_AFFINITY_BONUS
        is PassiveEffect.StatModifierEffect -> PassiveEffectKindIds.STAT_MODIFIER
        is PassiveEffect.DamageVsTag -> PassiveEffectKindIds.DAMAGE_VS_TAG
        is PassiveEffect.DamageVsStatus -> PassiveEffectKindIds.DAMAGE_VS_STATUS
        is PassiveEffect.HpRegenPerTurn -> PassiveEffectKindIds.HP_REGEN_PER_TURN
        is PassiveEffect.DamageTypeBonus -> PassiveEffectKindIds.DAMAGE_TYPE_BONUS
        is PassiveEffect.ResistanceBonus -> PassiveEffectKindIds.RESISTANCE_BONUS
    }

enum class PassiveCondition {
    HP_BELOW_50,
    HP_BELOW_30,
    HP_ABOVE_80,
    SELF_HAS_STATUS,
}

data class StatModifier(
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
) {
    operator fun plus(other: StatModifier): StatModifier =
        StatModifier(
            str = str + other.str,
            dex = dex + other.dex,
            con = con + other.con,
            wil = wil + other.wil,
            attack = attack + other.attack,
            defense = defense + other.defense,
            accuracy = accuracy + other.accuracy,
            evasion = evasion + other.evasion,
            speed = speed + other.speed,
            castSpeedRating = castSpeedRating + other.castSpeedRating,
            maxHp = maxHp + other.maxHp,
            maxStamina = maxStamina + other.maxStamina,
            hpRegen = hpRegen + other.hpRegen,
            staminaRegen = staminaRegen + other.staminaRegen,
            critChance = critChance + other.critChance,
            talentPower = talentPower + other.talentPower,
            attackMultiplierBonus = attackMultiplierBonus + other.attackMultiplierBonus,
            defenseMultiplierBonus = defenseMultiplierBonus + other.defenseMultiplierBonus,
        )

    companion object {
        val ZERO: StatModifier = StatModifier()
    }
}

data class AffixDef(
    val id: String,
    val name: String,
    val type: AffixType,
    val equipType: AffixEquipType = AffixEquipType.WEAPON,
    val tier: Int = 1,
    val cost: Int,
    val affixFamily: String,
    val exclusiveGroup: String? = null,
    val statModifiers: StatModifier,
    val minFloor: Int = 1,
    val tags: Set<String> = emptySet(),
    val blacklistTags: Set<String> = emptySet(),
    val passive: PassiveEffect? = null,
    val phase: String = "P4",
)

fun AffixDef.toAffixCost(): AffixCost =
    AffixCost(
        affixId = id,
        cost = cost,
        affixFamily = affixFamily,
        exclusiveGroup = exclusiveGroup,
        slotTags =
            setOf(
                equipType.name.lowercase(),
                type.name.lowercase(),
            ),
        phase = phase,
    )

data class MaterialDef(
    val id: String,
    val name: String,
    val minFloor: Int = 1,
    val statModifiers: StatModifier = StatModifier(),
)

data class ItemBaseDef(
    val id: String,
    val name: String,
    val type: ItemType,
    val slot: EquipSlot? = null,
    val tags: Set<String> = emptySet(),
    val glyph: Char,
    val colorHex: String,
    val baseStats: StatModifier = StatModifier(),
    val allowedMaterials: List<String> = emptyList(),
    val dropFloors: List<Int> = listOf(1),
    val dropWeight: Int = 1,
    val effect: ConsumableEffect? = null,
    val resourceTypeId: String? = null,
    val magnitude: Int = 0,
    val passive: PassiveEffect? = null,
)

data class SpecialItemTemplate(
    val id: String,
    val itemId: String,
    val specialTier: SpecialTier,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: Set<String>,
    val allowedSourceTiers: Set<SourceTier>,
    val allowedZones: Set<String>,
    val fixedAffixIds: List<String> = emptyList(),
    val fixedMaterialId: String? = null,
) {
    init {
        require(id.isNotBlank()) { "SpecialItemTemplate.id must not be blank." }
        require(itemId.isNotBlank()) { "SpecialItemTemplate.itemId must not be blank." }
        require(nameKey.isNotBlank()) { "SpecialItemTemplate.nameKey must not be blank." }
        require(descKey.isNotBlank()) { "SpecialItemTemplate.descKey must not be blank." }
        require(visualKey.isNotBlank()) { "SpecialItemTemplate.visualKey must not be blank." }
        require(iconKey.isNotBlank()) { "SpecialItemTemplate.iconKey must not be blank." }
        require(audioProfile.isNotBlank()) { "SpecialItemTemplate.audioProfile must not be blank." }
        require(schemaVersion > 0) { "SpecialItemTemplate.schemaVersion must be positive." }
        require(tags.none(String::isBlank)) { "SpecialItemTemplate.tags must not contain blank values." }
        require(allowedSourceTiers.isNotEmpty()) { "SpecialItemTemplate.allowedSourceTiers must not be empty." }
        require(allowedZones.isNotEmpty()) { "SpecialItemTemplate.allowedZones must not be empty." }
        require(allowedZones.none(String::isBlank)) { "SpecialItemTemplate.allowedZones must not contain blank ids." }
        require(fixedAffixIds.none(String::isBlank)) { "SpecialItemTemplate.fixedAffixIds must not contain blank ids." }
        require(fixedMaterialId?.isNotBlank() != false) {
            "SpecialItemTemplate.fixedMaterialId must not be blank when present."
        }
    }
}

data class ItemDataBundle(
    val baseItems: List<ItemBaseDef>,
    val materials: List<MaterialDef>,
    val affixes: List<AffixDef>,
    val specialTemplates: List<SpecialItemTemplate> = emptyList(),
) {
    private val baseItemsById: Map<String, ItemBaseDef> = baseItems.associateBy(ItemBaseDef::id)
    private val materialsById: Map<String, MaterialDef> = materials.associateBy(MaterialDef::id)
    private val affixesById: Map<String, AffixDef> = affixes.associateBy(AffixDef::id)
    private val specialTemplatesById: Map<String, SpecialItemTemplate> = specialTemplates.associateBy(SpecialItemTemplate::id)
    private val specialTemplateByItemId: Map<String, SpecialItemTemplate> = specialTemplates.associateBy(SpecialItemTemplate::itemId)
    private val normalBaseItemsByFloorBand = ConcurrentHashMap<Int, List<ItemBaseDef>>()
    private val eligibleSpecialTemplateIdsByKey = ConcurrentHashMap<SpecialTemplateEligibilityCacheKey, Set<String>>()

    fun baseItem(id: String): ItemBaseDef? = baseItemsById[id]

    fun material(id: String): MaterialDef? = materialsById[id]

    fun affix(id: String): AffixDef? = affixesById[id]

    fun specialTemplate(id: String): SpecialItemTemplate? = specialTemplatesById[id]

    fun specialTemplatesFor(tier: SpecialTier): List<SpecialItemTemplate> =
        specialTemplates.filter { template -> template.specialTier == tier }

    fun specialTemplateForItemId(itemId: String): SpecialItemTemplate? = specialTemplateByItemId[itemId]

    fun normalBaseItemsForFloorBand(floorBand: Int): List<ItemBaseDef> =
        normalBaseItemsByFloorBand.computeIfAbsent(floorBand) { resolvedFloorBand ->
            baseItems.filter { item ->
                item.type != ItemType.CONSUMABLE &&
                    specialTemplateByItemId[item.id] == null &&
                    resolvedFloorBand in item.dropFloors
            }
        }

    fun materialsFor(
        allowedMaterialIds: Collection<String>,
        floorBand: Int,
    ): List<MaterialDef> =
        allowedMaterialIds
            .asSequence()
            .mapNotNull(materialsById::get)
            .filter { material -> floorBand >= material.minFloor }
            .toList()

    fun eligibleSpecialTemplateIds(
        zoneId: String,
        sourceTier: SourceTier,
        allowedSpecialTiers: Set<SpecialTier>,
    ): Set<String> =
        eligibleSpecialTemplateIdsByKey.computeIfAbsent(
            SpecialTemplateEligibilityCacheKey(
                zoneId = zoneId,
                sourceTier = sourceTier,
                allowedSpecialTiers = allowedSpecialTiers.toSet(),
            ),
        ) { key ->
            specialTemplates
                .asSequence()
                .filter { template -> template.specialTier in key.allowedSpecialTiers }
                .filter { template -> key.sourceTier in template.allowedSourceTiers }
                .filter { template -> key.zoneId in template.allowedZones }
                .mapTo(linkedSetOf(), SpecialItemTemplate::id)
        }

    private data class SpecialTemplateEligibilityCacheKey(
        val zoneId: String,
        val sourceTier: SourceTier,
        val allowedSpecialTiers: Set<SpecialTier>,
    )
}

data class ItemInstance(
    val baseId: String,
    val name: String,
    val type: ItemType,
    val slot: EquipSlot? = null,
    val glyph: Char,
    val colorHex: String,
    val quality: RarityTier = RarityTier.NORMAL,
    val materialId: String? = null,
    val materialName: String? = null,
    val affixes: List<AffixDef> = emptyList(),
    val stats: StatModifier = StatModifier(),
    val effect: ConsumableEffect? = null,
    val resourceTypeId: String? = null,
    val magnitude: Int = 0,
    val passive: PassiveEffect? = null,
    val specialTemplateId: String? = null,
)

fun RarityTier.defaultAffixCount(): Int =
    when (this) {
        RarityTier.NORMAL -> 0
        RarityTier.MAGIC -> 1
        RarityTier.RARE -> 2
    }

fun RarityTier.minimumAffixCount(): Int =
    when (this) {
        RarityTier.NORMAL -> 0
        RarityTier.MAGIC -> 1
        RarityTier.RARE -> 2
    }

fun RarityTier.maximumAffixCount(): Int =
    when (this) {
        RarityTier.NORMAL -> 0
        RarityTier.MAGIC -> 2
        RarityTier.RARE -> 4
    }

data class Inventory(
    val capacity: Int = 12,
    val itemIds: MutableList<EntityId> = mutableListOf(),
)

data class Equipment(
    val slots: MutableMap<EquipSlot, EntityId> = linkedMapOf(),
)

data object GroundItem
