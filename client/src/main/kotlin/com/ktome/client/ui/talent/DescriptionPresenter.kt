package com.ktome.client.ui.talent

import com.ktome.core.snapshot.DescriptionModelSnapshot
import com.ktome.core.snapshot.DescriptionValueSnapshot
import com.ktome.core.snapshot.TalentBreakpointPreviewSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.status.StatusDefinitions
import com.ktome.core.status.StatusEffectType
import com.ktome.core.talent.KeywordRegistry
import com.ktome.game.i18n.Localizer
import java.text.NumberFormat
import java.util.Locale

enum class DescriptionLineKind {
    PRIMARY,
    SECONDARY,
    KEYWORD,
    STATE,
}

data class DescriptionLine(
    val text: String,
    val kind: DescriptionLineKind,
)

object DescriptionPresenter {
    fun presentForTalent(
        localizer: Localizer,
        talent: TalentSlotSnapshot,
    ): List<String> =
        presentLines(
            localizer = localizer,
            model = talent.descriptionModel,
            nextBreakpoint = talent.nextBreakpointPreview,
            currentRank = talent.level,
            committedRank = talent.committedLevel,
            maxRank = talent.maxLevel,
            isMaxRank = talent.isMaxRank,
            hasPendingAllocation = talent.hasPendingAllocation,
        ).map(DescriptionLine::text)

    fun presentForReserveTalent(
        localizer: Localizer,
        talent: TalentReserveSnapshot,
    ): List<String> =
        presentReserveTalentLines(
            localizer = localizer,
            talent = talent,
        ).map(DescriptionLine::text)

    fun presentTalentLines(
        localizer: Localizer,
        talent: TalentSlotSnapshot,
    ): List<DescriptionLine> =
        presentLines(
            localizer = localizer,
            model = talent.descriptionModel,
            nextBreakpoint = talent.nextBreakpointPreview,
            currentRank = talent.level,
            committedRank = talent.committedLevel,
            maxRank = talent.maxLevel,
            isMaxRank = talent.isMaxRank,
            hasPendingAllocation = talent.hasPendingAllocation,
        )

    fun presentReserveTalentLines(
        localizer: Localizer,
        talent: TalentReserveSnapshot,
    ): List<DescriptionLine> =
        presentLines(
            localizer = localizer,
            model = talent.descriptionModel,
            nextBreakpoint = talent.nextBreakpointPreview,
            currentRank = talent.level,
            committedRank = talent.committedLevel,
            maxRank = talent.maxLevel,
            isMaxRank = talent.isMaxRank,
            hasPendingAllocation = talent.hasPendingAllocation,
        )

    private fun presentLines(
        localizer: Localizer,
        model: DescriptionModelSnapshot?,
        nextBreakpoint: TalentBreakpointPreviewSnapshot?,
        currentRank: Int,
        committedRank: Int,
        maxRank: Int,
        isMaxRank: Boolean,
        hasPendingAllocation: Boolean,
    ): List<DescriptionLine> {
        if (model == null) {
            return emptyList()
        }
        val lines = mutableListOf<DescriptionLine>()
        lines += DescriptionLine(renderModel(localizer, model), DescriptionLineKind.PRIMARY)
        lines += renderKeywordTooltips(localizer, model)
        if (hasPendingAllocation) {
            lines +=
                DescriptionLine(
                    localizer.text("ui.talent.pending_rank", "preview" to currentRank, "live" to committedRank),
                    DescriptionLineKind.STATE,
                )
        }
        if (isMaxRank) {
            lines += DescriptionLine(localizer.text("ui.talent.max_rank", "rank" to maxRank), DescriptionLineKind.STATE)
        } else if (nextBreakpoint != null) {
            lines +=
                DescriptionLine(
                    localizer.text("ui.talent.next_breakpoint", "rank" to nextBreakpoint.atRank),
                    DescriptionLineKind.SECONDARY,
                )
            nextBreakpoint.descriptionAddendumKey?.let { key ->
                lines += DescriptionLine(localizer.text(key), DescriptionLineKind.SECONDARY)
            }
            lines += DescriptionLine(renderModel(localizer, nextBreakpoint.model), DescriptionLineKind.SECONDARY)
        }
        return lines
    }

    private fun renderModel(
        localizer: Localizer,
        model: DescriptionModelSnapshot,
    ): String {
        val args =
            model.placeholders.entries.map { (name, value) ->
                name to renderValue(localizer, name, value)
            }
        val base = localizer.text(model.templateKey, *args.toTypedArray())
        return replaceKeywordMarkup(localizer, base)
    }

    private fun renderKeywordTooltips(
        localizer: Localizer,
        model: DescriptionModelSnapshot,
    ): List<DescriptionLine> =
        model.keywords.distinct().map { keywordId ->
            val semantic = KeywordRegistry.CORE.require(keywordId)
            DescriptionLine(
                localizer.text(
                    "ui.talent.keyword_line",
                    "keyword" to localizer.text(semantic.nameKey),
                    "desc" to localizer.text(semantic.tooltipKey),
                ),
                DescriptionLineKind.KEYWORD,
            )
        }

    private fun renderValue(
        localizer: Localizer,
        name: String,
        value: DescriptionValueSnapshot,
    ): String =
        when (value) {
            is DescriptionValueSnapshot.BooleanValue ->
                localizedLiteral(localizer, if (value.value) "ui.boolean.true" else "ui.boolean.false", value.value.toString())
            is DescriptionValueSnapshot.DecimalValue -> decimalFormatter(localizer).format(value.value)
            is DescriptionValueSnapshot.IntValue -> integerFormatter(localizer).format(value.value)
            is DescriptionValueSnapshot.StatusValue -> localizer.text(value.nameKey)
            is DescriptionValueSnapshot.TextValue ->
                when {
                    name == "damageType" -> localizer.text("damage_type.${value.value.lowercase()}.name")
                    name == "statusId" -> renderStatus(localizer, value.value)
                    else -> value.value
                }
        }

    private fun renderStatus(
        localizer: Localizer,
        statusId: String,
    ): String {
        val directText = localizer.text("status.${statusId.lowercase()}")
        if (!directText.startsWith("!!")) {
            return directText
        }
        val type = StatusEffectType.fromSchemaId(statusId)
        if (type != StatusEffectType.CUSTOM) {
            return localizer.text(StatusDefinitions.nameKey(type))
        }
        return statusId
    }

    private fun replaceKeywordMarkup(
        localizer: Localizer,
        text: String,
    ): String =
        KEYWORD_PATTERN.replace(text) { match ->
            val keywordId = match.groupValues[1]
            localizer.text(KeywordRegistry.CORE.require(keywordId).nameKey)
        }

    private fun integerFormatter(localizer: Localizer): NumberFormat =
        NumberFormat.getIntegerInstance(javaLocale(localizer)).apply {
            isGroupingUsed = false
        }

    private fun decimalFormatter(localizer: Localizer): NumberFormat =
        NumberFormat.getNumberInstance(javaLocale(localizer)).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
            isGroupingUsed = false
        }

    private fun javaLocale(localizer: Localizer): Locale =
        when (localizer.locale.id) {
            "en-US" -> Locale.US
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.forLanguageTag(localizer.locale.id)
        }

    private fun localizedLiteral(
        localizer: Localizer,
        key: String,
        fallback: String,
    ): String {
        val localized = localizer.text(key)
        return if (localized.startsWith("!!")) fallback else localized
    }

    private val KEYWORD_PATTERN = Regex("\\[\\[([a-z0-9_]+)]]")
}
