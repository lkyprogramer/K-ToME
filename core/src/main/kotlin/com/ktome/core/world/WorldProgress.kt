package com.ktome.core.world

import kotlinx.serialization.Serializable

@Serializable
data class WorldProgressDef(
    val questStates: Map<String, QuestProgress> = emptyMap(),
    val worldFlags: Set<String> = emptySet(),
    val unlockedRoutes: Set<String> = emptySet(),
    val defeatedBossIds: Set<String> = emptySet(),
    val claimedRouteRewards: Set<String> = emptySet(),
) {
    init {
        require(worldFlags.none(String::isBlank)) { "WorldProgressDef.worldFlags must not contain blank values." }
        require(unlockedRoutes.none(String::isBlank)) { "WorldProgressDef.unlockedRoutes must not contain blank values." }
        require(defeatedBossIds.none(String::isBlank)) { "WorldProgressDef.defeatedBossIds must not contain blank values." }
        require(claimedRouteRewards.none(String::isBlank)) { "WorldProgressDef.claimedRouteRewards must not contain blank values." }
    }

    fun satisfies(gate: GateCondition): Boolean =
        gate.requiredQuestId?.let(::isQuestCompleted) != false &&
            gate.requiredWorldFlag?.let(worldFlags::contains) != false &&
            gate.requiredBossKill?.let(defeatedBossIds::contains) != false

    fun isQuestCompleted(questId: String): Boolean {
        val quest = questStates[questId] ?: return false
        return quest.objectiveStates.isNotEmpty() &&
            quest.objectiveStates.values.all { state -> state == ObjectiveState.COMPLETED }
    }

    fun withUnlockedRoute(routeId: String): WorldProgressDef =
        copy(unlockedRoutes = unlockedRoutes + routeId)

    fun withWorldFlag(flag: String): WorldProgressDef =
        copy(worldFlags = worldFlags + flag)

    fun withDefeatedBoss(bossId: String): WorldProgressDef =
        copy(defeatedBossIds = defeatedBossIds + bossId)

    fun withClaimedRouteReward(routeRewardId: String): WorldProgressDef =
        copy(claimedRouteRewards = claimedRouteRewards + routeRewardId)

    fun withQuestProgress(
        questId: String,
        progress: QuestProgress,
    ): WorldProgressDef =
        copy(questStates = questStates + (questId to progress))
}

@Serializable
data class QuestProgress(
    val questId: String,
    val objectiveStates: Map<String, ObjectiveState> = emptyMap(),
    val completionFlags: Set<String> = emptySet(),
) {
    init {
        require(questId.isNotBlank()) { "QuestProgress.questId must not be blank." }
        require(completionFlags.none(String::isBlank)) { "QuestProgress.completionFlags must not contain blank values." }
    }
}

@Serializable
enum class ObjectiveState {
    LOCKED,
    AVAILABLE,
    IN_PROGRESS,
    COMPLETED,
}

@Serializable
data class GateCondition(
    val requiredQuestId: String? = null,
    val requiredWorldFlag: String? = null,
    val requiredBossKill: String? = null,
) {
    init {
        require(requiredQuestId?.isNotBlank() != false) { "GateCondition.requiredQuestId must not be blank when present." }
        require(requiredWorldFlag?.isNotBlank() != false) { "GateCondition.requiredWorldFlag must not be blank when present." }
        require(requiredBossKill?.isNotBlank() != false) { "GateCondition.requiredBossKill must not be blank when present." }
    }
}

@Serializable
enum class RewardClaimPolicy {
    ON_ROUTE_UNLOCK,
    ON_FIRST_ROUTE_CLEAR,
}

@Serializable
data class RouteReward(
    val routeId: String,
    val claimPolicy: RewardClaimPolicy = RewardClaimPolicy.ON_FIRST_ROUTE_CLEAR,
    val levelBandRef: String,
    val shardReward: Int,
    val guaranteedUtilityDropIds: List<String>,
    val milestoneRewardProfileIds: List<String> = emptyList(),
    val rescueTags: Set<String> = emptySet(),
) {
    init {
        require(routeId.isNotBlank()) { "RouteReward.routeId must not be blank." }
        require(levelBandRef.isNotBlank()) { "RouteReward.levelBandRef must not be blank." }
        require(shardReward >= 0) { "RouteReward.shardReward must not be negative." }
        require(guaranteedUtilityDropIds.none(String::isBlank)) {
            "RouteReward.guaranteedUtilityDropIds must not contain blank values."
        }
        require(milestoneRewardProfileIds.none(String::isBlank)) {
            "RouteReward.milestoneRewardProfileIds must not contain blank values."
        }
        require(rescueTags.none(String::isBlank)) { "RouteReward.rescueTags must not contain blank values." }
    }
}
