package com.ktome.tools.loot

import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemDataBundle
import com.ktome.core.item.ItemBaseDef
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.item.PassiveEffect
import com.ktome.core.item.kindId
import com.ktome.game.data.schema.BossEncounterSchemaV2
import com.ktome.game.data.schema.LootPoolStrategy
import com.ktome.game.data.schema.LootProfileSchemaV3
import com.ktome.game.data.schema.ProfessionBuildIdentitySchemaV1
import com.ktome.game.data.schema.ProfessionSchemaV2
import com.ktome.game.data.schema.RewardRoutingEntrySchemaV1
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.loot.FOUNDATION_DETERMINISTIC_RESCUE_UTILITY_BASE_IDS
import com.ktome.game.loot.foundationBuildIdentityProfessionIds
import com.ktome.game.loot.foundationMilestoneLootWeightByBaseIdFromPools
import com.ktome.game.loot.foundationMilestoneRewardSourceTier
import com.ktome.game.loot.LootBaseSelectionContext
import com.ktome.game.loot.LootProfileCandidatePool
import com.ktome.game.loot.LootProfileCandidatePoolResolver
import com.ktome.game.loot.MilestoneRewardRejectionReason
import com.ktome.game.loot.MilestoneRewardSelector
import com.ktome.game.loot.MilestoneRewardSelectionRequest
import com.ktome.game.loot.MilestoneRewardSelectorContext
import com.ktome.game.loot.isMilestoneRewardSuitableForProfession
import kotlinx.serialization.Serializable
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

@Serializable
data class SpecialTierPassiveFamilyDuplicate(
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

@Serializable
data class SpecialTierPassiveFamilyDuplicateSummary(
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

private fun specialTierPassiveFamily(passive: PassiveEffect?): String =
    when (passive) {
        is PassiveEffect.OnHitStatusProc -> "${passive.kindId()}:${passive.statusId}"
        is PassiveEffect.OnKillResourceRestore -> "${passive.kindId()}:${passive.resourceType.name}"
        is PassiveEffect.ConditionalStatBonus -> "${passive.kindId()}:${passive.condition.name}:${passive.statusId ?: "-"}:${statModifierSignature(passive.statModifier)}"
        is PassiveEffect.TerrainAffinityBonus -> "${passive.kindId()}:${passive.terrainTag.name}"
        is PassiveEffect.StatModifierEffect -> "${passive.kindId()}:${statModifierSignature(passive.statModifier)}"
        is PassiveEffect.DamageVsTag -> "${passive.kindId()}:${passive.tag}"
        is PassiveEffect.DamageVsStatus -> "${passive.kindId()}:${passive.statusId}"
        is PassiveEffect.DamageTypeBonus -> "${passive.kindId()}:${passive.type.name}"
        is PassiveEffect.ResistanceBonus -> "${passive.kindId()}:${passive.damageType.name}"
        is PassiveEffect.HpRegenPerTurn -> passive.kindId()
        null -> "NoPassive"
    }

private val FOUNDATION_PROFESSION_IDS: Set<String> = foundationBuildIdentityProfessionIds

@Serializable
data class CriticalRewardRoutingSourceSummary(
    val sourceId: String,
    val zoneId: String,
    val interactableId: String,
    val rewardSource: MilestoneRewardSource,
    val profileIds: List<String>,
    val fallbackBaseId: String,
    val hasNonFoundationProfile: Boolean,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("sourceId", sourceId)
            put("zoneId", zoneId)
            put("interactableId", interactableId)
            put("rewardSource", rewardSource.name)
            putJsonArray("profileIds") { profileIds.forEach { profileId -> add(JsonPrimitive(profileId)) } }
            put("fallbackBaseId", fallbackBaseId)
            put("hasNonFoundationProfile", hasNonFoundationProfile)
        }
}

@Serializable
data class ProfessionCapstoneSourceCoverage(
    val professionId: String,
    val rewardSource: MilestoneRewardSource,
    val covered: Boolean,
    val coveredSourceIds: List<String>,
    val culpritSourceIds: List<String>,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("professionId", professionId)
            put("rewardSource", rewardSource.name)
            put("covered", covered)
            putJsonArray("coveredSourceIds") { coveredSourceIds.forEach { sourceId -> add(JsonPrimitive(sourceId)) } }
            putJsonArray("culpritSourceIds") { culpritSourceIds.forEach { sourceId -> add(JsonPrimitive(sourceId)) } }
        }
}

@Serializable
data class RejectedCapstoneCandidateSummary(
    val professionId: String,
    val rewardSource: MilestoneRewardSource,
    val sourceId: String,
    val zoneId: String,
    val baseItemId: String,
    val rejectionReason: MilestoneRewardRejectionReason,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            put("professionId", professionId)
            put("rewardSource", rewardSource.name)
            put("sourceId", sourceId)
            put("zoneId", zoneId)
            put("baseItemId", baseItemId)
            put("rejectionReason", rejectionReason.name)
        }
}

@Serializable
data class RewardRoutingCoverageSummary(
    val criticalSources: List<CriticalRewardRoutingSourceSummary>,
    val professionSourceCoverage: List<ProfessionCapstoneSourceCoverage>,
    val topRejectedCapstoneCandidates: List<RejectedCapstoneCandidateSummary>,
) {
    val coveredSourcePairCount: Int
        get() = professionSourceCoverage.count(ProfessionCapstoneSourceCoverage::covered)

    val totalSourcePairCount: Int
        get() = professionSourceCoverage.size

    val professionCapstoneSourceCoverageRate: Double
        get() =
            if (professionSourceCoverage.isEmpty()) {
                1.0
            } else {
                coveredSourcePairCount.toDouble() / professionSourceCoverage.size.toDouble()
            }

    fun toJson(): JsonObject =
        buildJsonObject {
            put("coveredSourcePairCount", coveredSourcePairCount)
            put("totalSourcePairCount", totalSourcePairCount)
            put("professionCapstoneSourceCoverageRate", professionCapstoneSourceCoverageRate)
            putJsonArray("criticalSources") { criticalSources.forEach { summary -> add(summary.toJson()) } }
            putJsonArray("professionSourceCoverage") { professionSourceCoverage.forEach { summary -> add(summary.toJson()) } }
            putJsonArray("topRejectedCapstoneCandidates") { topRejectedCapstoneCandidates.forEach { summary -> add(summary.toJson()) } }
        }
}

internal fun computeRewardRoutingCoverageSummary(schemaCatalog: SchemaCatalog, itemBundle: ItemDataBundle): RewardRoutingCoverageSummary {
    val lootProfilesById = schemaCatalog.lootProfiles.associateBy(LootProfileSchemaV3::id)
    val professionsById = schemaCatalog.professions.associateBy(ProfessionSchemaV2::id)
    val itemSchemasById = schemaCatalog.itemBundle.items.associateBy { item -> item.id }
    val baseItemsById = itemBundle.baseItems.associateBy(ItemBaseDef::id)
    val zoneFloorBandById =
        schemaCatalog.zones.associate { zone ->
            zone.id to ((zone.recommendedLevel.max - 1) / 3 + 1).coerceIn(1, 5)
        }
    val resolver = LootProfileCandidatePoolResolver(itemBundle)
    val selector = MilestoneRewardSelector(itemBundle)
    val sources = buildIdentityCoverageSources(schemaCatalog, lootProfilesById)
    val sourcesByRewardSource = sources.groupBy(BuildIdentityCoverageSource::rewardSource)
    val resolvedPoolsBySourceId =
        sources.associate { source ->
            source.sourceId to source.profileIds.mapNotNull(lootProfilesById::get).map(resolver::resolve)
        }
    val criticalSources =
        schemaCatalog.rewardRoutingEntries.map { entry ->
            CriticalRewardRoutingSourceSummary(
                sourceId = routingSourceId(entry),
                zoneId = entry.zoneId,
                interactableId = entry.interactableId,
                rewardSource = rewardSourceFor(entry.grantMode),
                profileIds = entry.profileIds,
                fallbackBaseId = entry.fallbackBaseId,
                hasNonFoundationProfile = entry.profileIds.any { profileId -> !profileId.startsWith("loot.foundation.") },
            )
        }
    val coverage = mutableListOf<ProfessionCapstoneSourceCoverage>()
    val rejectedCandidates = mutableListOf<RejectedCapstoneCandidateSummary>()
    schemaCatalog.buildIdentities
        .sortedBy(ProfessionBuildIdentitySchemaV1::professionId)
        .forEach { identity ->
            val profession = requireNotNull(professionsById[identity.professionId]) {
                "Missing profession '${identity.professionId}' for reward routing coverage."
            }
            val selectionContext =
                LootBaseSelectionContext(
                    buildTags = identity.terminalIdentityTags.mapTo(linkedSetOf(), String::lowercase),
                    preferredProfessionTag = identity.professionId,
                )
            val replacementSlotPriority =
                buildList<EquipSlot> {
                    addAll(identity.preferredReplacementSlots)
                    EquipSlot.entries.filterNot(this::contains).forEach(::add)
                }
            val professionSuitability =
                { base: ItemBaseDef ->
                    isMilestoneRewardSuitableForProfession(base, profession, itemSchemasById::get)
                }
            identity.preferredRewardSources.sortedBy(MilestoneRewardSource::name).forEach { rewardSource ->
                val relevantSources = sourcesByRewardSource[rewardSource].orEmpty()
                val matchedSources = mutableListOf<String>()
                val culpritSources = mutableListOf<String>()
                relevantSources.forEach { source ->
                    val pools: List<LootProfileCandidatePool> = resolvedPoolsBySourceId.getValue(source.sourceId)
                    if (pools.isEmpty()) {
                        culpritSources += source.sourceId
                        return@forEach
                    }
                    val selection =
                        selector.select(
                            request =
                                MilestoneRewardSelectionRequest(
                                    candidateBaseIds = pools.flatMapTo(linkedSetOf()) { pool -> pool.allCandidateBaseIds }.toList(),
                                    selectorContext =
                                        MilestoneRewardSelectorContext(
                                            rewardSource = rewardSource,
                                            zoneId = source.zoneId,
                                            sourceTier = foundationMilestoneRewardSourceTier(rewardSource),
                                            effectiveFloorBand = zoneFloorBandById[source.zoneId] ?: 5,
                                        ),
                                    poolWeightByBaseId =
                                        foundationMilestoneLootWeightByBaseIdFromPools(
                                            baseItemById = baseItemsById::get,
                                            pools = pools,
                                            selectionContext = selectionContext,
                                        ),
                                    selectionContext = selectionContext,
                                    replacementSlotPriority = replacementSlotPriority,
                                    forbiddenBaseIds = FOUNDATION_DETERMINISTIC_RESCUE_UTILITY_BASE_IDS,
                                ),
                            professionSuitability = professionSuitability,
                            canSatisfyAffixes = { true },
                        )
                    val hasCapstoneCoverage =
                        selection.rankedCandidates.any { candidate ->
                            candidate.legal && candidate.baseItemId in identity.capstoneBaseIds
                        }
                    if (hasCapstoneCoverage) {
                        matchedSources += source.sourceId
                    } else {
                        culpritSources += source.sourceId
                    }
                    selection.rankedCandidates
                        .asSequence()
                        .filter { candidate -> !candidate.legal && candidate.baseItemId in identity.capstoneBaseIds }
                        .take(2)
                        .forEach { candidate ->
                            rejectedCandidates +=
                                RejectedCapstoneCandidateSummary(
                                    professionId = identity.professionId,
                                    rewardSource = rewardSource,
                                    sourceId = source.sourceId,
                                    zoneId = source.zoneId,
                                    baseItemId = candidate.baseItemId,
                                    rejectionReason = requireNotNull(candidate.rejectionReason),
                                )
                        }
                }
                coverage +=
                    ProfessionCapstoneSourceCoverage(
                        professionId = identity.professionId,
                        rewardSource = rewardSource,
                        covered = matchedSources.isNotEmpty(),
                        coveredSourceIds = matchedSources.distinct().sorted(),
                        culpritSourceIds = culpritSources.distinct().sorted(),
                    )
            }
        }
    return RewardRoutingCoverageSummary(
        criticalSources = criticalSources.sortedBy(CriticalRewardRoutingSourceSummary::sourceId),
        professionSourceCoverage = coverage.sortedWith(compareBy(ProfessionCapstoneSourceCoverage::professionId, ProfessionCapstoneSourceCoverage::rewardSource)),
        topRejectedCapstoneCandidates =
            rejectedCandidates
                .distinctBy { summary -> listOf(summary.professionId, summary.rewardSource.name, summary.sourceId, summary.baseItemId) }
                .sortedWith(compareBy(RejectedCapstoneCandidateSummary::professionId, RejectedCapstoneCandidateSummary::rewardSource, RejectedCapstoneCandidateSummary::sourceId, RejectedCapstoneCandidateSummary::baseItemId)),
    )
}

private data class BuildIdentityCoverageSource(
    val sourceId: String,
    val zoneId: String,
    val rewardSource: MilestoneRewardSource,
    val profileIds: List<String>,
)

private fun buildIdentityCoverageSources(
    schemaCatalog: SchemaCatalog,
    lootProfilesById: Map<String, LootProfileSchemaV3>,
): List<BuildIdentityCoverageSource> {
    val routingSources =
        schemaCatalog.rewardRoutingEntries.map { entry ->
            BuildIdentityCoverageSource(
                sourceId = routingSourceId(entry),
                zoneId = entry.zoneId,
                rewardSource = rewardSourceFor(entry.grantMode),
                profileIds = entry.profileIds,
            )
        }
    val bossSources =
        schemaCatalog.bossEncounters.map { encounter ->
            val zoneId =
                encounter.rewards
                    .mapNotNull(lootProfilesById::get)
                    .firstNotNullOfOrNull(LootProfileSchemaV3::canonicalZoneId)
                    ?: "unknown"
            BuildIdentityCoverageSource(
                sourceId = encounter.id,
                zoneId = zoneId,
                rewardSource = MilestoneRewardSource.BOSS,
                profileIds = encounter.rewards,
            )
        }
    return routingSources + bossSources
}

private fun rewardSourceFor(grantMode: com.ktome.game.data.schema.RewardRoutingGrantMode): MilestoneRewardSource =
    when (grantMode) {
        com.ktome.game.data.schema.RewardRoutingGrantMode.GROUND_CACHE -> MilestoneRewardSource.CACHE
        com.ktome.game.data.schema.RewardRoutingGrantMode.SUPPORT_GRANT -> MilestoneRewardSource.SUPPORT
    }

private fun routingSourceId(entry: RewardRoutingEntrySchemaV1): String =
    "${entry.zoneId}/${entry.interactableId}/${entry.grantMode.name}"
