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
        const val CURRENT_PROFILE_VERSION: Int = 1
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
        require(buildHash.isNotBlank()) { "RunSummary.buildHash must not be blank." }
        require(rulesetVersion.isNotBlank()) { "RunSummary.rulesetVersion must not be blank." }
        require(turnCount >= 0) { "RunSummary.turnCount must not be negative." }
        require(headlessTurnEquivalent >= 0) { "RunSummary.headlessTurnEquivalent must not be negative." }
        require(!victory || defeatReason == null) { "Victories must not carry a defeatReason." }
    }
}
