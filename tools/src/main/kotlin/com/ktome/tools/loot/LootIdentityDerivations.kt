package com.ktome.tools.loot

import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.StatModifier
import com.ktome.game.data.schema.LootPoolStrategy
import com.ktome.game.data.schema.LootProfileSchemaV3
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal val PR02_DYNAMIC_POOL_TARGET_PROFILE_IDS: Set<String> =
    linkedSetOf(
        "loot.shattered_outpost.cadence",
        "loot.bandit_camp.cadence",
        "loot.elven_ruins.cadence",
        "loot.molten_core.cadence",
        "loot.grey_gate_depths.cadence",
        "loot.crystal_cavern.cadence",
        "loot.grey_gate_depths.reward",
        "loot.underground_river.reward",
        "loot.abyssal_temple.reward",
        "loot.abyssal_heart.reward",
    )

internal data class DynamicPoolTargetProfileSummary(
    val profileId: String,
    val canonicalZoneId: String,
    val poolStrategy: LootPoolStrategy,
    val dynamic: Boolean,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("profileId", profileId)
            put("canonicalZoneId", canonicalZoneId)
            put("poolStrategy", poolStrategy.name)
            put("dynamic", dynamic)
        }
}

internal data class DynamicPoolCoverageSummary(
    val targetProfiles: List<DynamicPoolTargetProfileSummary>,
) {
    val dynamicProfileCount: Int
        get() = targetProfiles.count(DynamicPoolTargetProfileSummary::dynamic)

    val dynamicPoolCoverage: Double
        get() =
            if (targetProfiles.isEmpty()) {
                0.0
            } else {
                dynamicProfileCount.toDouble() / targetProfiles.size.toDouble()
            }

    fun toJson(): JsonObject =
        buildJsonObject {
            put("dynamicPoolCoverage", dynamicPoolCoverage)
            putJsonArray("dynamicPoolTargetProfiles") {
                targetProfiles.forEach { summary -> add(summary.toJson()) }
            }
        }
}

internal fun computeDynamicPoolCoverage(profiles: List<LootProfileSchemaV3>): DynamicPoolCoverageSummary {
    val profilesById = profiles.associateBy(LootProfileSchemaV3::id)
    val targetProfiles =
        PR02_DYNAMIC_POOL_TARGET_PROFILE_IDS.map { profileId ->
            val profile =
                requireNotNull(profilesById[profileId]) {
                    "Missing PR-02 dynamic pool target profile '$profileId'."
                }
            DynamicPoolTargetProfileSummary(
                profileId = profile.id,
                canonicalZoneId = profile.canonicalZoneId ?: "unknown",
                poolStrategy = profile.poolStrategy,
                dynamic = profile.poolStrategy == LootPoolStrategy.TAG_WEIGHTED,
            )
        }
    return DynamicPoolCoverageSummary(targetProfiles = targetProfiles)
}

internal data class SpecialTierPassiveFamilyDuplicate(
    val canonicalZoneId: String,
    val passiveFamily: String,
    val templateIds: List<String>,
    val itemIds: List<String>,
    val professionTags: List<String>,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("canonicalZoneId", canonicalZoneId)
            put("passiveFamily", passiveFamily)
            putJsonArray("templateIds") {
                templateIds.forEach { templateId -> add(JsonPrimitive(templateId)) }
            }
            putJsonArray("itemIds") {
                itemIds.forEach { itemId -> add(JsonPrimitive(itemId)) }
            }
            putJsonArray("professionTags") {
                professionTags.forEach { professionTag -> add(JsonPrimitive(professionTag)) }
            }
        }
}

internal data class SpecialTierPassiveFamilyDuplicateSummary(
    val duplicateFamilies: List<SpecialTierPassiveFamilyDuplicate>,
) {
    val duplicateFamilyCount: Int
        get() = duplicateFamilies.size

    val duplicatedZoneCount: Int
        get() = duplicateFamilies.mapTo(linkedSetOf(), SpecialTierPassiveFamilyDuplicate::canonicalZoneId).size

    fun toJson(): JsonObject =
        buildJsonObject {
            put("duplicateFamilyCount", duplicateFamilyCount)
            put("duplicatedZoneCount", duplicatedZoneCount)
            putJsonArray("duplicateFamilies") {
                duplicateFamilies.forEach { duplicate -> add(duplicate.toJson()) }
            }
        }
}

internal fun computeSpecialTierPassiveFamilyDuplicateSummary(itemBundle: ItemDataBundle): SpecialTierPassiveFamilyDuplicateSummary {
    val baseItemsById = itemBundle.baseItems.associateBy { item -> item.id }
    val duplicates =
        itemBundle.specialTemplates
            .flatMap { template ->
                val base = requireNotNull(baseItemsById[template.itemId]) { "Missing base item '${template.itemId}' for special template '${template.id}'." }
                val passiveFamily = specialTierPassiveFamily(base.passive)
                template.allowedZones.map { zoneId ->
                    Triple(zoneId, passiveFamily, template)
                }
            }.groupBy { (zoneId, passiveFamily, _) -> zoneId to passiveFamily }
            .values
            .mapNotNull { entries ->
                if (entries.size < 2) {
                    return@mapNotNull null
                }
                val zoneId = entries.first().first
                val passiveFamily = entries.first().second
                val templates = entries.map { (_, _, template) -> template }.sortedBy { template -> template.id }
                SpecialTierPassiveFamilyDuplicate(
                    canonicalZoneId = zoneId,
                    passiveFamily = passiveFamily,
                    templateIds = templates.map { template -> template.id },
                    itemIds = templates.map { template -> template.itemId },
                    professionTags =
                        templates
                            .flatMap { template -> template.tags }
                            .map(String::trim)
                            .filter { tag -> tag in FOUNDATION_PROFESSION_IDS }
                            .distinct()
                            .sorted(),
                )
            }.sortedWith(compareBy(SpecialTierPassiveFamilyDuplicate::canonicalZoneId, SpecialTierPassiveFamilyDuplicate::passiveFamily))
    return SpecialTierPassiveFamilyDuplicateSummary(duplicateFamilies = duplicates)
}

private fun specialTierPassiveFamily(passive: EquipmentPassive?): String =
    when (passive) {
        is EquipmentPassive.OnHitStatusProc -> "OnHitStatusProc:${passive.statusId}"
        is EquipmentPassive.OnKillResourceRestore -> "OnKillResourceRestore:${passive.resourceType.name}"
        is EquipmentPassive.ConditionalStatBonus -> "ConditionalStatBonus:${passive.condition.name}:${passive.statusId ?: "-"}:${statModifierSignature(passive.statModifier)}"
        is EquipmentPassive.TerrainAffinityBonus -> "TerrainAffinityBonus:${passive.terrainTag.name}"
        is EquipmentPassive.DamageVsTag -> "DamageVsTag:${passive.tag}"
        is EquipmentPassive.DamageVsStatus -> "DamageVsStatus:${passive.statusId}"
        is EquipmentPassive.DamageTypeBonus -> "DamageTypeBonus:${passive.type.name}"
        is EquipmentPassive.ResistanceBonus -> "ResistanceBonus:${passive.damageType.name}"
        is EquipmentPassive.HpRegenPerTurn -> "HpRegenPerTurn"
        null -> "NoPassive"
    }

private fun statModifierSignature(modifier: StatModifier): String =
    listOf(
        modifier.str,
        modifier.dex,
        modifier.con,
        modifier.wil,
        modifier.attack,
        modifier.defense,
        modifier.accuracy,
        modifier.evasion,
        modifier.speed,
        modifier.castSpeedRating,
        modifier.maxHp,
        modifier.maxStamina,
        modifier.hpRegen,
        modifier.staminaRegen,
        modifier.critChance,
        modifier.talentPower,
        modifier.attackMultiplierBonus,
        modifier.defenseMultiplierBonus,
    ).joinToString(separator = ":")

private val FOUNDATION_PROFESSION_IDS: Set<String> = setOf("vanguard", "arcanist", "rogue", "templar")
