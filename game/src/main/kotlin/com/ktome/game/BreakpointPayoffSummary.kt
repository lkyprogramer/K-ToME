package com.ktome.game

data class BreakpointPayoffSummary(
    val talentId: String,
    val treeId: String,
    val achievedRank: Int,
    val breakpointRank: Int,
    val unlockedEffectKinds: List<String>,
)

data class BreakpointPayoffObservation(
    val talentId: String,
    val treeId: String,
    val achievedRank: Int,
    val breakpointRank: Int,
    val unlockedEffectKinds: List<String>,
    val turnIndex: Int,
    val headlessTurnEquivalent: Int,
    val buildHashBeforeUnlock: String,
    val buildHashAfterUnlock: String,
    val buildHashChanged: Boolean,
)
