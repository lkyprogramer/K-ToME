package com.ktome.client.ui.hud

import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.i18n.Localizer

internal data class ResourceGaugeModel(
    val label: String,
    val current: Int,
    val max: Int,
    val resourceTypeId: String,
    val stableMin: Int? = null,
    val stableMax: Int? = null,
) {
    val percent: Float =
        if (max <= 0) {
            0f
        } else {
            (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        }

    val summary: String = "$label $current/$max"

    val stableStartPercent: Float? =
        if (max <= 0 || stableMin == null || stableMax == null) {
            null
        } else {
            (stableMin.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        }

    val stableEndPercent: Float? =
        if (max <= 0 || stableMin == null || stableMax == null) {
            null
        } else {
            (stableMax.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        }
}

internal data class ResourceHudModel(
    val primaryGauge: ResourceGaugeModel,
    val secondaryGauge: ResourceGaugeModel? = null,
    val summaryText: String,
)

internal object ResourceHud {
    fun build(
        localizer: Localizer,
        snapshot: RenderSnapshot,
    ): ResourceHudModel {
        val status = snapshot.uiState.playerStatus
        val primaryGauge =
            gauge(
                localizer = localizer,
                labelKey = status.resourceLabelKey,
                current = status.currentResource,
                max = status.maxResource,
                resourceTypeId = status.resourceTypeId,
                stableMin = status.resourceStableMin,
                stableMax = status.resourceStableMax,
            )
        val secondaryGauge =
            status.secondaryResourceLabelKey?.let { labelKey ->
                gauge(
                    localizer = localizer,
                    labelKey = labelKey,
                    current = status.secondaryResourceCurrent ?: 0,
                    max = status.secondaryResourceMax ?: 0,
                    resourceTypeId = status.secondaryResourceTypeId ?: status.resourceTypeId,
                    stableMin = status.secondaryResourceStableMin,
                    stableMax = status.secondaryResourceStableMax,
                )
            }

        return ResourceHudModel(
            primaryGauge = primaryGauge,
            secondaryGauge = secondaryGauge,
            summaryText = buildTileSummary(localizer, snapshot, status, primaryGauge),
        )
    }

    fun inlineHudText(
        localizer: Localizer,
        snapshot: RenderSnapshot,
    ): String {
        val status = snapshot.uiState.playerStatus
        val hud = build(localizer, snapshot)
        val raceTalentText =
            if (status.raceTalentPoints > 0) {
                "  ${localizer.text("ui.hud.race_talent.short")} ${status.raceTalentPoints}"
            } else {
                ""
            }
        val secondaryText =
            hud.secondaryGauge
                ?.let { gauge -> "  ${gauge.summary}${stableRangeSuffix(gauge)}" }
                .orEmpty()
        return "${localizer.text("ui.hud.floor.short")} ${snapshot.metadata.currentFloor}/${snapshot.metadata.maxFloor}  " +
            "${localizer.text("ui.hud.hp.short")} ${status.currentHp}/${status.maxHp}  " +
            "${hud.primaryGauge.summary}$secondaryText  " +
            "${localizer.text("ui.hud.attack.short")} ${status.attack}  " +
            "${localizer.text("ui.hud.defense.short")} ${status.defense}  " +
            "${localizer.text("ui.hud.level.short")} ${status.level}  " +
            "${localizer.text("ui.hud.xp.short")} ${status.currentExperience}/${status.nextLevelRequirement}  " +
            "${localizer.text("ui.hud.stat.short")} ${status.statPoints}  " +
            "${localizer.text("ui.hud.talent.short")} ${status.talentPoints}$raceTalentText"
    }

    private fun buildTileSummary(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        status: PlayerStatusSnapshot,
        primaryGauge: ResourceGaugeModel,
    ): String =
        "${localizer.text("ui.hud.floor.short")} ${snapshot.metadata.currentFloor}/${snapshot.metadata.maxFloor}  " +
            "${localizer.text("ui.hud.hp.short")} ${status.currentHp}/${status.maxHp}  " +
            primaryGauge.summary +
            "  ${localizer.text("ui.hud.attack.short")} ${status.attack}  " +
            "${localizer.text("ui.hud.defense.short")} ${status.defense}" +
            if (status.raceTalentPoints > 0) {
                "  ${localizer.text("ui.hud.race_talent.short")} ${status.raceTalentPoints}"
            } else {
                ""
            }

    private fun gauge(
        localizer: Localizer,
        labelKey: String,
        current: Int,
        max: Int,
        resourceTypeId: String,
        stableMin: Int? = null,
        stableMax: Int? = null,
    ): ResourceGaugeModel =
        ResourceGaugeModel(
            label = localizer.text(labelKey),
            current = current,
            max = max,
            resourceTypeId = resourceTypeId,
            stableMin = stableMin,
            stableMax = stableMax,
        )

    private fun stableRangeSuffix(gauge: ResourceGaugeModel): String =
        if (gauge.stableMin == null || gauge.stableMax == null) {
            ""
        } else {
            " [${gauge.stableMin}-${gauge.stableMax}]"
        }
}
