package com.ktome.client.ui.talent

import com.ktome.client.ui.card.ModalCardAction
import com.ktome.client.ui.card.ModalCardModel
import com.ktome.client.ui.card.ModalCardReturnPolicy
import com.ktome.client.ui.status.StatusPresentationBuilder
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.DescriptionModelSnapshot
import com.ktome.core.snapshot.DescriptionValueSnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.core.snapshot.TalentBreakpointPreviewSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.snapshot.TerrainOverrideRenderSnapshot
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

enum class DescriptionSurface {
    TALENT_ACTIVE,
    TALENT_RESERVE,
    INVENTORY_ITEM,
    SHOP_ITEM,
    INSPECT_OBJECT,
    COMBAT_ACTION,
    STATUS_EFFECT,
}

data class DescriptionLine(
    val text: String,
    val kind: DescriptionLineKind,
)

internal data class InspectDescriptionSurface(
    val actor: ActorRenderSnapshot?,
    val item: ItemRenderSnapshot?,
    val prop: PropRenderSnapshot?,
    val terrainOverride: TerrainOverrideRenderSnapshot?,
    val overlay: OverlayRenderSnapshot?,
)

object DescriptionPresenter {
    fun presentSurfaceLines(
        localizer: Localizer,
        model: DescriptionModelSnapshot?,
        surface: DescriptionSurface,
    ): List<DescriptionLine> {
        if (model == null) {
            return emptyList()
        }
        val primaryText = renderModel(localizer, model)
        val primaryLine =
            if (primaryText.isNotBlank()) {
                listOf(DescriptionLine(primaryText, primaryLineKind(surface)))
            } else {
                emptyList()
            }
        return primaryLine + renderKeywordTooltips(localizer, model)
    }

    fun presentInventoryItemLines(
        localizer: Localizer,
        model: DescriptionModelSnapshot?,
    ): List<DescriptionLine> = presentSurfaceLines(localizer, model, DescriptionSurface.INVENTORY_ITEM)

    fun presentShopItemLines(
        localizer: Localizer,
        model: DescriptionModelSnapshot?,
    ): List<DescriptionLine> = presentSurfaceLines(localizer, model, DescriptionSurface.SHOP_ITEM)

    fun presentInspectObjectLines(
        localizer: Localizer,
        model: DescriptionModelSnapshot?,
    ): List<DescriptionLine> = presentSurfaceLines(localizer, model, DescriptionSurface.INSPECT_OBJECT)

    fun presentCombatActionLines(
        localizer: Localizer,
        model: DescriptionModelSnapshot?,
    ): List<DescriptionLine> = presentSurfaceLines(localizer, model, DescriptionSurface.COMBAT_ACTION)

    fun presentStatusEffectLines(
        localizer: Localizer,
        model: DescriptionModelSnapshot?,
    ): List<DescriptionLine> = presentSurfaceLines(localizer, model, DescriptionSurface.STATUS_EFFECT)

    internal fun presentInventoryItemLines(
        localizer: Localizer,
        item: ItemRenderSnapshot,
    ): List<DescriptionLine> = presentItemSnapshotLines(localizer, item, DescriptionSurface.INVENTORY_ITEM)

    internal fun presentShopItemLines(
        localizer: Localizer,
        item: ItemRenderSnapshot,
    ): List<DescriptionLine> = presentItemSnapshotLines(localizer, item, DescriptionSurface.SHOP_ITEM)

    internal fun presentInspectItemLines(
        localizer: Localizer,
        item: ItemRenderSnapshot,
    ): List<DescriptionLine> = presentItemSnapshotLines(localizer, item, DescriptionSurface.INSPECT_OBJECT)

    internal fun presentInspectObjectLines(
        localizer: Localizer,
        tokens: List<RenderTextTokenSnapshot>,
    ): List<DescriptionLine> = presentTokenLines(localizer, tokens, DescriptionSurface.INSPECT_OBJECT)

    internal fun presentModalCardLines(
        localizer: Localizer,
        card: ModalCardModel,
        surface: DescriptionSurface,
    ): List<DescriptionLine> =
        presentTokenLines(
            localizer = localizer,
            tokens = listOfNotNull(card.summary) + card.detailLines + card.costLines + card.rewardLines + listOfNotNull(card.disabledReason),
            surface = surface,
        )

    internal fun presentStatusEffectLines(
        localizer: Localizer,
        effect: StatusEffectRenderSnapshot,
    ): List<DescriptionLine> {
        val text = renderTextToken(localizer, statusEffectLineToken(effect))
        return listOf(DescriptionLine(text, DescriptionLineKind.PRIMARY))
    }

    internal fun emptyExplainCard(): ModalCardModel =
        ModalCardModel(
            stableKey = "inspect:explain:empty",
            title = RenderTextTokenSnapshot("ui.explain.title"),
            iconKey = null,
            summary = null,
            detailLines = listOf(RenderTextTokenSnapshot("ui.explain.empty")),
            primaryAction = ModalCardAction.CLOSE,
            secondaryAction = ModalCardAction.CLOSE,
            returnPolicy = ModalCardReturnPolicy.READ_ONLY_POPPABLE,
        )

    internal fun inspectSurfaceCard(
        surface: InspectDescriptionSurface,
    ): ModalCardModel {
        val propNameKey = surface.prop?.nameKey
        return when {
            surface.overlay != null ->
                readOnlyCard(
                    stableKey = "inspect:telegraph:${surface.overlay.id}",
                    title = surface.overlay.warningMessage ?: RenderTextTokenSnapshot("ui.status.group.telegraph"),
                    detailLines = listOf(RenderTextTokenSnapshot("ui.status.group.telegraph")),
                )

            surface.terrainOverride != null ->
                readOnlyCard(
                    stableKey = "inspect:zone:${surface.terrainOverride.sourceRuleId}",
                    title = RenderTextTokenSnapshot(surface.terrainOverride.ruleNameKey),
                    detailLines = listOf(RenderTextTokenSnapshot("ui.status.group.zone-effect")),
                )

            surface.actor != null ->
                readOnlyCard(
                    stableKey = "inspect:actor:${surface.actor.entityId}",
                    title = RenderTextTokenSnapshot(surface.actor.nameKey),
                    detailLines =
                        surface.actor.statusEffects.map(::statusEffectLineToken) +
                            surface.actor.mutations.map { mutation -> RenderTextTokenSnapshot(mutation.nameKey) },
                )

            surface.item != null ->
                readOnlyCard(
                    stableKey = "inspect:item:${surface.item.baseItemId}",
                    title = surface.item.displayName ?: RenderTextTokenSnapshot(surface.item.nameKey),
                    detailLines = listOfNotNull(surface.item.descKey?.let(::RenderTextTokenSnapshot)) + surface.item.passiveDescriptions,
                )

            surface.prop != null && propNameKey != null ->
                readOnlyCard(
                    stableKey = "inspect:prop:${surface.prop.id}",
                    title = RenderTextTokenSnapshot(propNameKey),
                    detailLines = listOfNotNull(surface.prop.stateLabelKey, surface.prop.descKey).map(::RenderTextTokenSnapshot),
                )

            else -> emptyExplainCard()
        }
    }

    internal fun inspectSurfaceKeywordIds(
        localizer: Localizer,
        surface: InspectDescriptionSurface,
        card: ModalCardModel = inspectSurfaceCard(surface),
    ): List<String> =
        buildList {
            if (surface.overlay != null) {
                addKnownKeyword(this, "telegraph")
            }
            surface.actor?.statusEffects.orEmpty().forEach { effect ->
                statusKeywordCandidates(effect).forEach { keywordId ->
                    addKnownKeyword(this, keywordId)
                }
            }
            inspectCardKeywordIds(localizer, card).forEach(::add)
        }.distinct()

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
        lines += presentSurfaceLines(localizer, model, DescriptionSurface.TALENT_ACTIVE)
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
    ): List<DescriptionLine> = renderKeywordTooltips(localizer, model.keywords)

    private fun renderKeywordTooltips(
        localizer: Localizer,
        keywordIds: Iterable<String>,
    ): List<DescriptionLine> =
        keywordIds.distinct().map { keywordId ->
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

    private fun presentItemSnapshotLines(
        localizer: Localizer,
        item: ItemRenderSnapshot,
        surface: DescriptionSurface,
    ): List<DescriptionLine> =
        presentTokenLines(
            localizer = localizer,
            tokens =
                buildList {
                    item.descKey?.let { descKey -> add(RenderTextTokenSnapshot(descKey)) }
                    addAll(item.passiveDescriptions)
                },
            surface = surface,
        )

    private fun statusEffectLineToken(effect: StatusEffectRenderSnapshot): RenderTextTokenSnapshot {
        val presentation = StatusPresentationBuilder.build(effect)
        return if (presentation.badgeText.isBlank()) {
            effect.nameKey?.let(::RenderTextTokenSnapshot) ?: RenderTextTokenSnapshot(
                "ui.status.effect.name",
                listOf(RenderTextArgumentSnapshot(name = "name", value = effect.typeId)),
            )
        } else {
            RenderTextTokenSnapshot(
                key = "ui.status.effect.line",
                arguments =
                    listOf(
                        effect.nameKey?.let { nameKey ->
                            RenderTextArgumentSnapshot(name = "name", valueKey = nameKey)
                        } ?: RenderTextArgumentSnapshot(name = "name", value = effect.typeId),
                        RenderTextArgumentSnapshot(name = "badge", value = presentation.badgeText),
                    ),
            )
        }
    }

    private fun presentTokenLines(
        localizer: Localizer,
        tokens: List<RenderTextTokenSnapshot>,
        surface: DescriptionSurface,
    ): List<DescriptionLine> {
        val keywordIds = linkedSetOf<String>()
        val lines = mutableListOf<DescriptionLine>()
        tokens.forEach { token ->
            val rawText = renderTextToken(localizer, token)
            keywordIds += keywordIdsIn(rawText)
            val text = replaceKeywordMarkup(localizer, rawText)
            if (text.isNotBlank()) {
                val kind = if (lines.isEmpty()) primaryLineKind(surface) else DescriptionLineKind.SECONDARY
                lines += DescriptionLine(text, kind)
            }
        }
        return lines + renderKeywordTooltips(localizer, keywordIds)
    }

    private fun readOnlyCard(
        stableKey: String,
        title: RenderTextTokenSnapshot,
        detailLines: List<RenderTextTokenSnapshot>,
    ): ModalCardModel =
        ModalCardModel(
            stableKey = stableKey,
            title = title,
            iconKey = null,
            summary = null,
            detailLines = detailLines.ifEmpty { listOf(RenderTextTokenSnapshot("ui.explain.empty")) },
            primaryAction = ModalCardAction.CLOSE,
            secondaryAction = ModalCardAction.CLOSE,
            returnPolicy = ModalCardReturnPolicy.READ_ONLY_POPPABLE,
        )

    private fun primaryLineKind(surface: DescriptionSurface): DescriptionLineKind =
        when (surface) {
            DescriptionSurface.TALENT_ACTIVE,
            DescriptionSurface.TALENT_RESERVE,
            DescriptionSurface.INVENTORY_ITEM,
            DescriptionSurface.SHOP_ITEM,
            DescriptionSurface.INSPECT_OBJECT,
            DescriptionSurface.COMBAT_ACTION,
            DescriptionSurface.STATUS_EFFECT,
            -> DescriptionLineKind.PRIMARY
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

    private fun renderTextToken(
        localizer: Localizer,
        token: RenderTextTokenSnapshot,
    ): String =
        localizer.text(
            token.key,
            *token.arguments.map { argument -> argument.name to resolveArgument(localizer, argument) }.toTypedArray(),
        )

    private fun resolveArgument(
        localizer: Localizer,
        argument: RenderTextArgumentSnapshot,
    ): String =
        argument.value
            ?: argument.valueKey?.let(localizer::text)
            ?: argument.valueToken?.let { token -> renderTextToken(localizer, token) }
            ?: ""

    private fun replaceKeywordMarkup(
        localizer: Localizer,
        text: String,
    ): String =
        KEYWORD_PATTERN.replace(text) { match ->
            val keywordId = match.groupValues[1]
            localizer.text(KeywordRegistry.CORE.require(keywordId).nameKey)
        }

    private fun keywordIdsIn(text: String): List<String> =
        KEYWORD_PATTERN.findAll(text).map { match -> match.groupValues[1] }.toList()

    private fun inspectCardKeywordIds(
        localizer: Localizer,
        card: ModalCardModel,
    ): List<String> =
        (
            listOfNotNull(card.summary) +
                card.detailLines +
                card.costLines +
                card.rewardLines +
                listOfNotNull(card.disabledReason)
        )
            .flatMap { token -> keywordIdsIn(renderTextToken(localizer, token)) }

    private fun addKnownKeyword(
        keywordIds: MutableList<String>,
        keywordId: String,
    ) {
        if (KeywordRegistry.CORE.resolve(keywordId) != null) {
            keywordIds += keywordId
        }
    }

    private fun statusKeywordCandidates(effect: StatusEffectRenderSnapshot): List<String> =
        listOfNotNull(
            effect.typeId,
            effect.typeId.lowercase(),
            effect.nameKey?.removePrefix("status."),
        )

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
