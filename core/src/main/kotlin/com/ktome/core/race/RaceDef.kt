package com.ktome.core.race

import com.ktome.core.profile.ClassUnlockState

data class RaceDef(
    val id: String,
    val nameKey: String,
    val descKey: String,
    val visualKey: String,
    val iconKey: String,
    val audioProfile: String,
    val schemaVersion: Int,
    val tags: List<String>,
    val statModifiers: RaceStatModifiers = RaceStatModifiers(),
    val talentTrees: List<String> = emptyList(),
    val startingTalents: List<String> = emptyList(),
    val initialUnlockState: ClassUnlockState = ClassUnlockState.RELEASE_UNLOCKED,
) {
    init {
        require(id.isNotBlank()) { "Race id must not be blank." }
        require(nameKey.isNotBlank()) { "Race '$id' nameKey must not be blank." }
        require(descKey.isNotBlank()) { "Race '$id' descKey must not be blank." }
    }
}

data class RaceStatModifiers(
    val str: Int = 0,
    val dex: Int = 0,
    val con: Int = 0,
    val wil: Int = 0,
    val hpDelta: Int = 0,
    val accuracyDelta: Int = 0,
    val evasionDelta: Int = 0,
    val speedDelta: Int = 0,
)
