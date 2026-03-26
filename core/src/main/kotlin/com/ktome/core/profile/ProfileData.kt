package com.ktome.core.profile

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
        const val CURRENT_PROFILE_VERSION: Int = 2
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
        require(rulesetVersion.isNotBlank()) { "RunSummary.rulesetVersion must not be blank." }
        require(turnCount >= 0) { "RunSummary.turnCount must not be negative." }
        require(headlessTurnEquivalent >= 0) { "RunSummary.headlessTurnEquivalent must not be negative." }
        require(!victory || defeatReason == null) { "Victories must not carry a defeatReason." }
    }
}
