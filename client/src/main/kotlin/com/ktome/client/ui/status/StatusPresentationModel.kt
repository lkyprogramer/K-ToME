package com.ktome.client.ui.status

import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.core.snapshot.TerrainOverrideRenderSnapshot
import kotlin.math.max

internal enum class StatusPresentationGroup(
    val labelKey: String,
) {
    BUFF("ui.status.group.buff"),
    DEBUFF("ui.status.group.debuff"),
    NEUTRAL("ui.status.group.neutral"),
    TELEGRAPH("ui.status.group.telegraph"),
    ZONE_EFFECT("ui.status.group.zone-effect"),
}

internal data class StatusPresentationModel(
    val typeId: String,
    val nameKey: String?,
    val iconKey: String?,
    val category: StatusEffectCategorySnapshot,
    val rawBadge: String,
    val priority: Int,
    val group: StatusPresentationGroup,
) {
    val badgeText: String = rawBadge
}

internal data class TelegraphStatusPresentationRequest(
    val typeId: String,
    val nameKey: String?,
    val iconKey: String?,
    val dangerLevel: Int,
    val previewTurnsRemaining: Int?,
)

internal object StatusPresentationBuilder {
    private const val BADGE_OVERFLOW_LIMIT: Int = 99

    fun build(effect: StatusEffectRenderSnapshot): StatusPresentationModel {
        val group = effect.category.toPresentationGroup()
        return StatusPresentationModel(
            typeId = effect.typeId,
            nameKey = effect.nameKey,
            iconKey = effect.iconKey,
            category = effect.category,
            rawBadge = statusBadge(effect),
            priority = statusPriority(group = group, stackCount = effect.stackCount, remainingTurns = effect.remainingTurns),
            group = group,
        )
    }

    fun buildZoneEffect(terrainOverride: TerrainOverrideRenderSnapshot): StatusPresentationModel {
        val dangerLevel = terrainOverride.zoneDangerLevel()
        return StatusPresentationModel(
            typeId = terrainOverride.sourceRuleId,
            nameKey = terrainOverride.ruleNameKey,
            iconKey = null,
            category = StatusEffectCategorySnapshot.NEUTRAL,
            rawBadge = turnBadge(terrainOverride.remainingTurns),
            priority = 650 + dangerLevel * 10,
            group = StatusPresentationGroup.ZONE_EFFECT,
        )
    }

    fun buildTelegraph(request: TelegraphStatusPresentationRequest): StatusPresentationModel =
        StatusPresentationModel(
            typeId = request.typeId,
            nameKey = request.nameKey,
            iconKey = request.iconKey,
            category = StatusEffectCategorySnapshot.NEUTRAL,
            rawBadge = telegraphBadge(request.previewTurnsRemaining),
            priority = telegraphPriority(dangerLevel = request.dangerLevel, previewTurnsRemaining = request.previewTurnsRemaining),
            group = StatusPresentationGroup.TELEGRAPH,
        )

    fun sorted(models: List<StatusPresentationModel>): List<StatusPresentationModel> =
        models.sortedWith(compareByDescending<StatusPresentationModel> { model -> model.priority }.thenBy { model -> model.typeId })

    fun statusBadge(effect: StatusEffectRenderSnapshot): String =
        buildList {
            val stackCap = effect.stackCap
            when {
                effect.stackCount > 1 &&
                    stackCap != null &&
                    effect.stackCount <= BADGE_OVERFLOW_LIMIT &&
                    stackCap <= BADGE_OVERFLOW_LIMIT ->
                    add("${effect.stackCount}/$stackCap")
                effect.stackCount > 1 -> add("x${badgeCount(effect.stackCount)}")
            }
            turnBadge(effect.remainingTurns).takeIf(String::isNotBlank)?.let(::add)
        }.joinToString(" ")

    fun telegraphBadge(previewTurnsRemaining: Int?): String =
        previewTurnsRemaining?.let { turns -> "${turns}t" }.orEmpty()

    fun telegraphPriority(
        dangerLevel: Int,
        previewTurnsRemaining: Int?,
    ): Int = 900 + dangerLevel * 20 + previewTurnsInverse(previewTurnsRemaining)

    private fun statusPriority(
        group: StatusPresentationGroup,
        stackCount: Int,
        remainingTurns: Int,
    ): Int {
        val stackWeight = stackCount.coerceIn(0, 10)
        val remainingTurnsWeight = max(0, 20 - remainingTurns.coerceIn(0, 20))
        return when (group) {
            StatusPresentationGroup.DEBUFF -> 700 + stackWeight + remainingTurnsWeight
            StatusPresentationGroup.BUFF -> 500 + stackWeight + remainingTurnsWeight
            StatusPresentationGroup.NEUTRAL -> 300 + stackWeight + remainingTurnsWeight
            StatusPresentationGroup.TELEGRAPH,
            StatusPresentationGroup.ZONE_EFFECT,
            -> error("Use dedicated priority formula for $group.")
        }
    }

    private fun previewTurnsInverse(previewTurnsRemaining: Int?): Int =
        previewTurnsRemaining?.let { turns -> max(0, 5 - turns) } ?: 0

    private fun turnBadge(remainingTurns: Int): String =
        when {
            remainingTurns > BADGE_OVERFLOW_LIMIT -> "${BADGE_OVERFLOW_LIMIT}+"
            remainingTurns > 0 -> "${remainingTurns}t"
            else -> ""
        }

    private fun badgeCount(value: Int): String =
        if (value > BADGE_OVERFLOW_LIMIT) {
            "${BADGE_OVERFLOW_LIMIT}+"
        } else {
            value.toString()
        }

    private fun StatusEffectCategorySnapshot.toPresentationGroup(): StatusPresentationGroup =
        when (this) {
            StatusEffectCategorySnapshot.BUFF -> StatusPresentationGroup.BUFF
            StatusEffectCategorySnapshot.DEBUFF -> StatusPresentationGroup.DEBUFF
            StatusEffectCategorySnapshot.NEUTRAL -> StatusPresentationGroup.NEUTRAL
        }

    private fun TerrainOverrideRenderSnapshot.zoneDangerLevel(): Int =
        when {
            tickDamage >= 10 -> 3
            tickDamage > 0 || conductsLightning -> 2
            else -> 1
        }
}
