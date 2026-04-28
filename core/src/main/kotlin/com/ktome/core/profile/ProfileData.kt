package com.ktome.core.profile

import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.item.EquipSlot
import com.ktome.core.loot.RarityTier
import kotlinx.serialization.Serializable

@Serializable
data class ProfileData(
    val profileVersion: Int = CURRENT_PROFILE_VERSION,
    val releaseUnlockedClasses: Set<String> = emptySet(),
    val runHistory: List<RunSummary> = emptyList(),
) {
    init {
        require(profileVersion == CURRENT_PROFILE_VERSION) {
            "Profile version $profileVersion must equal $CURRENT_PROFILE_VERSION."
        }
        require(releaseUnlockedClasses.none(String::isBlank)) { "releaseUnlockedClasses must not contain blank ids." }
    }

    companion object {
        const val CURRENT_PROFILE_VERSION: Int = 4
    }
}

@Serializable
data class MilestoneRewardSummary(
    val rewardSource: MilestoneRewardSource,
    val sourceId: String,
    val zoneId: String,
    val baseItemId: String,
    val equipSlot: EquipSlot,
    val qualityTier: RarityTier,
    val buildHashAtGrant: String,
    val affixIds: List<String> = emptyList(),
    val equippedBaseItemIdBeforeReward: String? = null,
    val equippedBaseItemIdAtRunEnd: String? = null,
    val adoptedInFinalBuild: Boolean = false,
) {
    init {
        require(sourceId.isNotBlank()) { "MilestoneRewardSummary.sourceId must not be blank." }
        require(zoneId.isNotBlank()) { "MilestoneRewardSummary.zoneId must not be blank." }
        require(baseItemId.isNotBlank()) { "MilestoneRewardSummary.baseItemId must not be blank." }
        require(buildHashAtGrant.isNotBlank()) { "MilestoneRewardSummary.buildHashAtGrant must not be blank." }
        require(affixIds.none(String::isBlank)) { "MilestoneRewardSummary.affixIds must not contain blank ids." }
        require(equippedBaseItemIdBeforeReward?.isNotBlank() != false) {
            "MilestoneRewardSummary.equippedBaseItemIdBeforeReward must not be blank."
        }
        require(equippedBaseItemIdAtRunEnd?.isNotBlank() != false) {
            "MilestoneRewardSummary.equippedBaseItemIdAtRunEnd must not be blank."
        }
        require(!adoptedInFinalBuild || equippedBaseItemIdAtRunEnd == baseItemId) {
            "Adopted milestone rewards must match equippedBaseItemIdAtRunEnd."
        }
    }
}

@Serializable
data class RunSummary(
    val seed: Long,
    val finishedAtEpochMillis: Long,
    val classId: String,
    val raceId: String,
    val finalZoneId: String,
    val turnCount: Int,
    val headlessTurnEquivalent: Int,
    val zoneRouteHash: String,
    val zonePath: List<String> = emptyList(),
    val defeatedBossIds: List<String> = emptyList(),
    val claimedRouteRewardIds: List<String> = emptyList(),
    val shardBalance: Int = 0,
    val buildHash: String,
    val milestoneRewards: List<MilestoneRewardSummary> = emptyList(),
    val startingInscriptionCount: Int = 0,
    val inscriptionInstallCount: Int = 0,
    val inscriptionReplaceCount: Int = 0,
    val rulesetVersion: String,
    val victory: Boolean,
    val defeatReason: String? = null,
) {
    init {
        require(classId.isNotBlank()) { "RunSummary.classId must not be blank." }
        require(raceId.isNotBlank()) { "RunSummary.raceId must not be blank." }
        require(finalZoneId.isNotBlank()) { "RunSummary.finalZoneId must not be blank." }
        require(zoneRouteHash.isNotBlank()) { "RunSummary.zoneRouteHash must not be blank." }
        require(zonePath.none(String::isBlank)) { "RunSummary.zonePath must not contain blank zone ids." }
        require(defeatedBossIds.none(String::isBlank)) { "RunSummary.defeatedBossIds must not contain blank ids." }
        require(claimedRouteRewardIds.none(String::isBlank)) { "RunSummary.claimedRouteRewardIds must not contain blank ids." }
        require(shardBalance >= 0) { "RunSummary.shardBalance must not be negative." }
        require(buildHash.isNotBlank()) { "RunSummary.buildHash must not be blank." }
        require(milestoneRewards.distinctBy { reward -> "${reward.rewardSource}:${reward.sourceId}" }.size == milestoneRewards.size) {
            "RunSummary.milestoneRewards must not contain duplicate entries."
        }
        require(startingInscriptionCount >= 0) { "RunSummary.startingInscriptionCount must not be negative." }
        require(inscriptionInstallCount >= 0) { "RunSummary.inscriptionInstallCount must not be negative." }
        require(inscriptionReplaceCount >= 0) { "RunSummary.inscriptionReplaceCount must not be negative." }
        require(rulesetVersion.isNotBlank()) { "RunSummary.rulesetVersion must not be blank." }
        require(turnCount >= 0) { "RunSummary.turnCount must not be negative." }
        require(headlessTurnEquivalent >= 0) { "RunSummary.headlessTurnEquivalent must not be negative." }
        require(!victory || defeatReason == null) { "Victories must not carry a defeatReason." }
    }
}
