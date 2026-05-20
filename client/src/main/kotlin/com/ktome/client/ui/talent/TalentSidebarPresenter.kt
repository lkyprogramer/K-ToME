package com.ktome.client.ui.talent

import com.ktome.client.input.OverlayState
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.core.snapshot.DescriptionModelSnapshot
import com.ktome.core.snapshot.DescriptionValueSnapshot
import com.ktome.core.snapshot.PassiveDetailDeltaLineSnapshot
import com.ktome.core.snapshot.PassiveDetailLineSnapshot
import com.ktome.core.snapshot.PassiveDetailLineToneSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.TalentBreakpointPreviewSnapshot
import com.ktome.core.snapshot.TalentNodeLockReasonSnapshot
import com.ktome.core.snapshot.TalentNodeLockReasonTypeSnapshot
import com.ktome.core.snapshot.TalentNodePrerequisiteSnapshot
import com.ktome.core.snapshot.TalentNodeStateSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.snapshot.TalentTreeNodeSnapshot
import com.ktome.core.snapshot.TalentTreeSnapshot
import com.ktome.core.talent.TalentCategory
import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.game.PLAYER_ACTIVE_TALENT_SLOT_COUNT
import com.ktome.game.i18n.Localizer

enum class TalentSidebarLineRole {
    POINTS,
    ACTION_TITLE,
    ACTION_HINT,
    TREE_HEADER,
    NODE_LOCKED,
    NODE_LEARNABLE,
    NODE_LEARNED_RESERVE,
    NODE_LEARNED_ACTIVE,
    DESCRIPTION_PRIMARY,
    DESCRIPTION_SECONDARY,
    DESCRIPTION_STATE,
    FOOTER,
}

data class TalentSidebarLine(
    val text: String,
    val role: TalentSidebarLineRole,
    val iconKey: String? = null,
    val selected: Boolean = false,
)

object TalentSidebarPresenter {
    fun present(
        localizer: Localizer,
        uiState: RenderUiStateSnapshot,
        overlayState: OverlayState,
    ): List<TalentSidebarLine> = presentPanel(localizer, uiState, overlayState).toSidebarLines()

    fun presentPanel(
        localizer: Localizer,
        uiState: RenderUiStateSnapshot,
        overlayState: OverlayState,
    ): TalentAssignPanelModel {
        val flatNodes = uiState.talentTrees.flatMap(TalentTreeSnapshot::nodes)
        val preferredIdentity =
            overlayState.talentTreeSelectionIdentity ?: flatNodes.getOrNull(overlayState.talentTreeSelection)?.toTalentTreeSelectionIdentity()
        val selectedNode =
            preferredIdentity?.let { identity -> flatNodes.firstOrNull { node -> node.toTalentTreeSelectionIdentity() == identity } }
                ?: flatNodes.getOrNull(overlayState.talentTreeSelection)
        val selectedIdentity = selectedNode?.toTalentTreeSelectionIdentity()
        val raceTreeVisible = uiState.talentTrees.any { tree -> parseOwnerType(tree.ownerType) == TalentTreeOwnerType.RACE }
        val sections =
            uiState.talentTrees.map { tree ->
                val ownerType = parseOwnerType(tree.ownerType)
                val rowProjections = projectTreeRows(tree.nodes)
                val nodesByTalentId = tree.nodes.associateBy(TalentTreeNodeSnapshot::talentId)
                TalentAssignSectionModel(
                    treeId = tree.treeId,
                    ownerType = ownerType,
                    treeOwnerId = tree.treeOwnerId,
                    displayName = localizer.text(tree.nameKey),
                    nodeCountText = "${tree.nodes.size}/${tree.nodes.size}",
                    iconKey = tree.iconKey,
                    rows =
                        tree.nodes.map { node ->
                            TalentAssignTreeRowModel(
                                kind = TalentAssignRowKind.TALENT_NODE,
                                talentId = node.talentId,
                                treeId = node.treeId,
                                ownerType = parseOwnerType(node.ownerType),
                                treeOwnerId = node.treeOwnerId,
                                indentLevel = rowProjections.getValue(node.talentId).depth,
                                connectorPrefix = rowProjections.getValue(node.talentId).connectorPrefix,
                                stateMarkerText = "[${node.state.stateGlyph()}]",
                                stateIconKey = null,
                                skillIconKey = talentAssignIconKey(node.talentId, node.iconKey),
                                displayName = localizer.text(node.nameKey),
                                rankText = talentRankLabel(node.committedRank, node.rank, node.maxRank),
                                toneToken = node.state.toToneToken(),
                                pendingOverlay = node.hasPendingAllocation,
                                focused = selectedIdentity != null && node.toTalentTreeSelectionIdentity() == selectedIdentity,
                            )
                        },
                    edges = rowProjections.values.mapNotNull { projection -> projection.toEdge(nodesByTalentId) },
                    scroll = TalentAssignSectionScrollModel(verticalOffset = 0, hasVerticalOverflow = false),
                )
            }
        return TalentAssignPanelModel(
            referenceImagePath = PR04_TALENT_ASSIGN_REFERENCE_IMAGE_PATH,
            header =
                TalentAssignHeaderModel(
                    title = localizer.text("ui.talent.assign.title"),
                    professionPointText = localizer.text("ui.talent.assign.profession_points", "value" to uiState.playerStatus.talentPoints),
                    racePointText =
                        if (raceTreeVisible || uiState.playerStatus.raceTalentPoints > 0) {
                            localizer.text("ui.talent.assign.race_points", "value" to uiState.playerStatus.raceTalentPoints)
                        } else {
                            null
                        },
            ),
            sections = sections,
            detail = selectedNode?.let { node -> detailPane(localizer, node) },
            legend = legend(localizer, sections.any { section -> section.rows.any(TalentAssignTreeRowModel::pendingOverlay) }),
            footerHints = footerHints(localizer, selectedNode),
            activeSlotChoiceModal =
                if (
                    overlayState.activeModalKind == ModalFrameKind.ACTIVE_TALENT_SLOT_CHOICE &&
                    uiState.activeTalentSlotChoiceRequirement != null
                ) {
                    activeSlotChoiceModal(localizer, uiState.talents)
                } else {
                    null
                },
        )
    }

    fun presentFromPanel(panel: TalentAssignPanelModel): List<TalentSidebarLine> = panel.toSidebarLines()

    private data class TalentRowProjection(
        val talentId: String,
        val depth: Int,
        val connectorPrefix: String,
        val parentPrerequisite: TalentNodePrerequisiteSnapshot?,
    ) {
        fun toEdge(nodesByTalentId: Map<String, TalentTreeNodeSnapshot>): TalentTreeEdgeProjection? {
            val prerequisite = parentPrerequisite ?: return null
            val parent = nodesByTalentId[prerequisite.talentId] ?: return null
            val child = nodesByTalentId[talentId] ?: return null
            return TalentTreeEdgeProjection(
                fromTalentId = parent.talentId,
                toTalentId = child.talentId,
                state =
                    when {
                        !prerequisite.satisfied -> TalentTreeEdgeState.UNSATISFIED
                        parent.state.isLearned() && child.state.isLearned() -> TalentTreeEdgeState.ACTIVE
                        else -> TalentTreeEdgeState.SATISFIED
                    },
            )
        }
    }

    private fun projectTreeRows(nodes: List<TalentTreeNodeSnapshot>): Map<String, TalentRowProjection> {
        val nodeById = nodes.associateBy(TalentTreeNodeSnapshot::talentId)
        val orderById = nodes.withIndex().associate { (index, node) -> node.talentId to index }
        val parentById = linkedMapOf<String, TalentNodePrerequisiteSnapshot>()
        val depthById = linkedMapOf<String, Int>()
        nodes.forEach { node ->
            val parentPrerequisite =
                node.prerequisites
                    .filter { prerequisite -> prerequisite.treeId == node.treeId && prerequisite.talentId in nodeById }
                    .maxWithOrNull(
                        compareBy<TalentNodePrerequisiteSnapshot> { prerequisite -> depthById[prerequisite.talentId] ?: 0 }
                            .thenBy { prerequisite -> orderById[prerequisite.talentId] ?: -1 },
                    )
            if (parentPrerequisite != null) {
                parentById[node.talentId] = parentPrerequisite
            }
            depthById[node.talentId] = parentPrerequisite?.let { prerequisite -> (depthById[prerequisite.talentId] ?: 0) + 1 } ?: 0
        }
        val childrenByParent =
            parentById.entries
                .groupBy({ entry -> entry.value.talentId }, { entry -> entry.key })
                .mapValues { (_, children) -> children.sortedBy { childId -> orderById[childId] ?: Int.MAX_VALUE } }
        return nodes.associate { node ->
            node.talentId to
                TalentRowProjection(
                    talentId = node.talentId,
                    depth = depthById[node.talentId] ?: 0,
                    connectorPrefix = connectorPrefix(node.talentId, parentById, childrenByParent),
                    parentPrerequisite = parentById[node.talentId],
                )
        }
    }

    private fun connectorPrefix(
        talentId: String,
        parentById: Map<String, TalentNodePrerequisiteSnapshot>,
        childrenByParent: Map<String, List<String>>,
    ): String {
        val ancestors = ancestorChain(talentId, parentById)
        if (ancestors.isEmpty()) {
            return ""
        }
        val ancestorPrefix =
            ancestors.dropLast(1).joinToString("") { ancestorId ->
                val ancestorParentId = parentById[ancestorId]?.talentId
                val siblingIds = ancestorParentId?.let(childrenByParent::get).orEmpty()
                if (siblingIds.indexOf(ancestorId) in 0 until siblingIds.lastIndex) {
                    "│   "
                } else {
                    "    "
                }
            }
        val parentId = ancestors.last()
        val siblings = childrenByParent[parentId].orEmpty()
        val connector = if (siblings.indexOf(talentId) in 0 until siblings.lastIndex) "├─ " else "└─ "
        return ancestorPrefix + connector
    }

    private fun ancestorChain(
        talentId: String,
        parentById: Map<String, TalentNodePrerequisiteSnapshot>,
    ): List<String> {
        val reversed = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        var currentId = talentId
        while (seen.add(currentId)) {
            val parentId = parentById[currentId]?.talentId ?: break
            reversed += parentId
            currentId = parentId
        }
        return reversed.asReversed()
    }

    private fun TalentAssignPanelModel.toSidebarLines(): List<TalentSidebarLine> {
        val lines = mutableListOf<TalentSidebarLine>()
        lines += TalentSidebarLine(header.professionPointText, TalentSidebarLineRole.POINTS)
        header.racePointText?.let { text -> lines += TalentSidebarLine(text, TalentSidebarLineRole.POINTS) }
        activeSlotChoiceModal?.let { modal ->
            lines += TalentSidebarLine(modal.title, TalentSidebarLineRole.ACTION_TITLE)
            modal.items.forEach { item ->
                lines +=
                    TalentSidebarLine(
                        text = "${item.hotkeyText} ${item.primaryLabel}${item.secondaryLabel?.let { secondary -> " · $secondary" }.orEmpty()}",
                        role = TalentSidebarLineRole.ACTION_HINT,
                        iconKey = item.iconKey,
                        selected = item.focused,
                    )
            }
            lines += TalentSidebarLine(modal.cancelHintText, TalentSidebarLineRole.FOOTER)
        }
        sections.forEach { section ->
            lines += TalentSidebarLine("${section.displayName}  ${section.nodeCountText}", TalentSidebarLineRole.TREE_HEADER, section.iconKey)
            section.rows.forEach { row ->
                lines +=
                    TalentSidebarLine(
                        text = "${"  ".repeat(row.indentLevel)}${row.connectorPrefix}${row.stateMarkerText} ${row.displayName} ${row.rankText}",
                        role = row.toneToken.toSidebarLineRole(),
                        iconKey = row.skillIconKey,
                        selected = row.focused,
                    )
            }
        }
        detail?.blocks.orEmpty().forEach { block ->
            if (block.primaryText.isNotBlank()) {
                lines += TalentSidebarLine(block.primaryText, block.toneToken.toSidebarLineRole(), block.iconKey)
            }
            block.secondaryText?.let { text -> lines += TalentSidebarLine(text, TalentSidebarLineRole.DESCRIPTION_SECONDARY) }
            block.bodyLines.forEach { line ->
                val labelPrefix = line.label?.let { label -> "$label: " }.orEmpty()
                lines += TalentSidebarLine(labelPrefix + line.value, line.toneToken.toSidebarLineRole(), line.iconKey)
            }
        }
        lines += TalentSidebarLine(footerHelpText, TalentSidebarLineRole.FOOTER)
        legend.items.forEach { item ->
            lines += TalentSidebarLine(listOfNotNull(item.markerText, item.label).joinToString(" "), item.kind.toSidebarLineRole(), item.iconKey)
        }
        return lines
    }

    private fun detailPane(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): TalentDetailPaneModel =
        TalentDetailPaneModel(
            talentId = node.talentId,
            blocks =
                buildList {
                    add(
                        TalentDetailBlock(
                            kind = TalentDetailBlockKind.HERO_ICON,
                            iconKey = talentAssignHeroIconKey(node.talentId, node.iconKey),
                            primaryText = "",
                            secondaryText = null,
                            bodyLines = emptyList(),
                            toneToken = TalentPreviewToneToken.PRIMARY,
                        ),
                    )
                    add(
                        TalentDetailBlock(
                            kind = TalentDetailBlockKind.HEADER,
                            iconKey = null,
                            primaryText = localizer.text(node.nameKey),
                            secondaryText = localizer.text(node.state.stateLabelKey()),
                            bodyLines = emptyList(),
                            toneToken = node.state.toPreviewToneToken(),
                        ),
                    )
                    add(
                        TalentDetailBlock(
                            kind = TalentDetailBlockKind.RANK_AND_COST,
                            iconKey = null,
                            primaryText = talentRankTransitionText(localizer, node),
                            secondaryText =
                                localizer.text(
                                    "ui.talent.assign.detail.cost",
                                    "cost" to talentPointCost(node),
                                    "pointType" to pointTypeText(localizer, parseOwnerType(node.ownerType)),
                                ),
                            bodyLines = emptyList(),
                            toneToken = TalentPreviewToneToken.SECONDARY,
                        ),
                    )
                    prerequisiteBlock(localizer, node)?.let(::add)
                    failedPrerequisiteBlock(localizer, node.lockReasons)?.let(::add)
                    add(currentRankDetailBlock(localizer, node))
                    add(nextRankPreviewBlock(localizer, node))
                    add(actionsBlock(localizer, node))
                },
        )

    private fun prerequisiteBlock(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): TalentDetailBlock? {
        val prerequisiteLines =
            node.prerequisites
                .map { prerequisite ->
                    TalentPreviewLine(
                        label = prerequisite.talentNameKey?.let(localizer::text) ?: localizer.text("ui.talent.tree.lock.unknown_talent"),
                        value = prerequisite.requiredRankText(localizer),
                        iconKey = null,
                        toneToken = if (prerequisite.satisfied) TalentPreviewToneToken.SECONDARY else TalentPreviewToneToken.WARNING,
                    )
                }
        val unlockLines = unlockRequirementLines(localizer, node)
        val inlinePrerequisite = node.prerequisites.singleOrNull()
        val primaryText =
            inlinePrerequisite?.let { prerequisite ->
                localizer.text(
                    "ui.talent.assign.detail.prerequisite_inline",
                    "talent" to (prerequisite.talentNameKey?.let(localizer::text) ?: localizer.text("ui.talent.tree.lock.unknown_talent")),
                    "rank" to prerequisite.requiredRank,
                    "max" to (prerequisite.requiredMaxRank ?: prerequisite.requiredRank),
                )
            } ?: localizer.text("ui.talent.assign.detail.prerequisite")
        val lines =
            if (inlinePrerequisite == null) {
                prerequisiteLines + unlockLines
            } else {
                unlockLines
            }
        if (lines.isEmpty()) {
            if (inlinePrerequisite == null) {
                return null
            }
        }
        return TalentDetailBlock(
            kind = TalentDetailBlockKind.PREREQUISITE,
            iconKey = null,
            primaryText = primaryText,
            secondaryText = null,
            bodyLines = lines,
            toneToken = TalentPreviewToneToken.SECONDARY,
        )
    }

    private fun failedPrerequisiteBlock(
        localizer: Localizer,
        lockReasons: List<TalentNodeLockReasonSnapshot>,
    ): TalentDetailBlock? {
        if (lockReasons.isEmpty()) {
            return null
        }
        return TalentDetailBlock(
            kind = TalentDetailBlockKind.PREREQUISITE_FAILED,
            iconKey = null,
            primaryText = localizer.text("ui.talent.assign.detail.failed_prerequisite"),
            secondaryText = null,
            bodyLines =
                lockReasons.map { reason ->
                    TalentPreviewLine(
                        label = null,
                        value = lockReasonText(localizer, reason),
                        iconKey = null,
                        toneToken = TalentPreviewToneToken.WARNING,
                    )
                },
            toneToken = TalentPreviewToneToken.WARNING,
        )
    }

    private fun currentRankDetailBlock(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): TalentDetailBlock {
        val titleKey =
            when {
                node.rank == 0 -> "ui.talent.assign.detail.current_preview_one"
                node.hasPendingAllocation && node.rank != node.committedRank -> "ui.talent.assign.detail.current_preview_rank"
                else -> "ui.talent.assign.detail.current_rank"
            }
        return TalentDetailBlock(
            kind = TalentDetailBlockKind.CURRENT_RANK_DETAIL,
            iconKey = null,
            primaryText = localizer.text(titleKey, "rank" to node.rank.coerceAtLeast(1)),
            secondaryText = null,
            bodyLines = currentDetailLines(localizer, node),
            toneToken = TalentPreviewToneToken.PRIMARY,
        )
    }

    private fun nextRankPreviewBlock(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): TalentDetailBlock =
        if (node.isMaxRank || node.rank >= node.maxRank) {
            TalentDetailBlock(
                kind = TalentDetailBlockKind.NEXT_RANK_PREVIEW,
                iconKey = null,
                primaryText = localizer.text("ui.talent.assign.detail.max_rank_preview"),
                secondaryText = null,
                bodyLines = emptyList(),
                toneToken = TalentPreviewToneToken.POSITIVE,
            )
        } else {
            TalentDetailBlock(
                kind = TalentDetailBlockKind.NEXT_RANK_PREVIEW,
                iconKey = null,
                primaryText = localizer.text("ui.talent.assign.detail.next_rank_preview"),
                secondaryText = null,
                bodyLines = nextRankPreviewLines(localizer, node),
                toneToken = TalentPreviewToneToken.SECONDARY,
            )
        }

    private fun nextRankPreviewLines(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): List<TalentPreviewLine> {
        if (node.category == TalentCategory.PASSIVE) {
            return passiveNextRankPreviewLines(localizer, node)
        }
        val nextRank = (node.rank.coerceAtLeast(1) + 1).coerceAtMost(node.maxRank)
        val upgradeLines = nextRankUpgradeLines(localizer, node, nextRank)
        return buildList {
            if (upgradeLines.isEmpty()) {
                add(
                    TalentPreviewLine(
                        label = localizer.text("ui.talent.assign.detail.rank"),
                        value = "${node.rank} -> $nextRank/${node.maxRank}",
                        iconKey = null,
                        toneToken = TalentPreviewToneToken.SECONDARY,
                    ),
                )
            }
            addAll(upgradeLines)
            node.nextBreakpointPreview?.let { preview ->
                add(
                    TalentPreviewLine(
                        label = localizer.text("ui.talent.assign.detail.milestone"),
                        value = breakpointPreviewValue(localizer, preview),
                        iconKey = null,
                        toneToken = TalentPreviewToneToken.POSITIVE,
                    ),
                )
            }
            if (node.resourceCost > 0) {
                add(
                    TalentPreviewLine(
                        label = null,
                        value =
                            localizer.text(
                                "ui.talent.assign.detail.upgrade_same_cost",
                                "cost" to node.resourceCost,
                                "resource" to localizer.text(node.resourceLabelKey),
                            ),
                        iconKey = null,
                        toneToken = TalentPreviewToneToken.SECONDARY,
                    ),
                )
            }
        }
    }

    private fun breakpointPreviewValue(
        localizer: Localizer,
        preview: TalentBreakpointPreviewSnapshot,
    ): String =
        when (preview.model.templateKey) {
            "talent.breakpoint.displacement" ->
                localizer.text(
                    "ui.talent.assign.detail.breakpoint.displacement",
                    "rank" to preview.atRank,
                    "distance" to (preview.model.intPlaceholder("displacementDistance") ?: 1),
                )
            else ->
                localizer.text(
                    "ui.talent.assign.detail.milestone_value",
                    "rank" to preview.atRank,
                    "effect" to
                        (
                            preview.descriptionAddendumKey?.let(localizer::text)
                                ?: nextDescriptionPreviewLine(localizer, preview.model)?.value
                                ?: localizer.text("ui.talent.assign.detail.rank_level", "rank" to preview.atRank)
                        ),
                )
        }

    private fun nextRankUpgradeLines(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
        nextRank: Int,
    ): List<TalentPreviewLine> {
        val currentModel = node.descriptionModel
        val nextModel = node.nextRankDescriptionModel ?: return emptyList()
        val lines = mutableListOf<TalentPreviewLine>()
        appendRangeUpgradeLine(localizer, node, currentModel, nextModel, lines)
        appendDamageUpgradeLine(localizer, currentModel, nextModel, lines)
        appendNumericUpgradeLine(
            localizer = localizer,
            currentModel = currentModel,
            nextModel = nextModel,
            placeholder = "radius",
            labelKey = "ui.talent.assign.detail.radius",
            formatValue = Int::toString,
            lines = lines,
        )
        appendNumericUpgradeLine(
            localizer = localizer,
            currentModel = currentModel,
            nextModel = nextModel,
            placeholder = "statusDuration",
            labelKey = "ui.talent.assign.detail.duration",
            formatValue = { value -> localizer.text("ui.talent.assign.detail.turns", "value" to value) },
            lines = lines,
        )
        appendNumericUpgradeLine(
            localizer = localizer,
            currentModel = currentModel,
            nextModel = nextModel,
            placeholder = "statusMagnitude",
            labelKey = "ui.talent.assign.detail.power",
            formatValue = { value -> "+$value%" },
            lines = lines,
        )
        appendNumericUpgradeLine(
            localizer = localizer,
            currentModel = currentModel,
            nextModel = nextModel,
            placeholder = "resourceRestorePercent",
            labelKey = "ui.talent.assign.detail.resource_restore",
            formatValue = { value -> "$value%" },
            lines = lines,
        )
        appendNumericUpgradeLine(
            localizer = localizer,
            currentModel = currentModel,
            nextModel = nextModel,
            placeholder = "resourceRestoreAmount",
            labelKey = "ui.talent.assign.detail.resource_restore",
            formatValue = { value -> "+$value" },
            lines = lines,
        )
        appendNumericUpgradeLine(
            localizer = localizer,
            currentModel = currentModel,
            nextModel = nextModel,
            placeholder = "healPercent",
            labelKey = "ui.talent.assign.detail.healing",
            formatValue = { value -> "$value%" },
            lines = lines,
        )
        appendNumericUpgradeLine(
            localizer = localizer,
            currentModel = currentModel,
            nextModel = nextModel,
            placeholder = "displacementDistance",
            labelKey = "ui.talent.assign.detail.displacement",
            formatValue = { value -> localizer.text("ui.talent.assign.detail.range.cells", "range" to value) },
            lines = lines,
        )
        appendNumericUpgradeLine(
            localizer = localizer,
            currentModel = currentModel,
            nextModel = nextModel,
            placeholder = "knockback",
            labelKey = "ui.talent.assign.detail.knockback",
            formatValue = { value -> localizer.text("ui.talent.assign.detail.range.cells", "range" to value) },
            lines = lines,
        )
        if (lines.isEmpty()) {
            lines += nextRankEffectSummaryLines(localizer, node, nextModel)
        }
        if (lines.isEmpty()) {
            nextDescriptionPreviewLine(localizer, nextModel)?.let(lines::add)
        }
        if (lines.isNotEmpty()) {
            lines.add(
                0,
                TalentPreviewLine(
                    label = localizer.text("ui.talent.assign.detail.rank"),
                    value = localizer.text("ui.talent.assign.detail.rank_level", "rank" to nextRank),
                    iconKey = null,
                    toneToken = TalentPreviewToneToken.SECONDARY,
                ),
            )
        }
        return lines
    }

    private fun nextRankEffectSummaryLines(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
        nextModel: DescriptionModelSnapshot,
    ): List<TalentPreviewLine> =
        buildList {
            if (node.requiresTarget) {
                add(
                    metricLine(
                        localizer = localizer,
                        labelKey = "ui.talent.assign.detail.range",
                        value =
                            rangeText(
                                localizer = localizer,
                                minRange = nextModel.intPlaceholder("minRange") ?: node.minRange,
                                maxRange = nextModel.intPlaceholder("range") ?: node.range,
                            ),
                        toneToken = TalentPreviewToneToken.POSITIVE,
                    ),
                )
            }
            nextModel.intPlaceholder("radius")?.takeIf { radius -> radius > 0 }?.let { radius ->
                add(
                    metricLine(
                        localizer = localizer,
                        labelKey = "ui.talent.assign.detail.radius",
                        value = radius.toString(),
                        toneToken = TalentPreviewToneToken.POSITIVE,
                    ),
                )
            }
            addAll(effectMetricLines(localizer, nextModel, TalentPreviewToneToken.POSITIVE))
        }.distinctBy { line -> line.label to line.value }

    private fun appendDamageUpgradeLine(
        localizer: Localizer,
        currentModel: DescriptionModelSnapshot?,
        nextModel: DescriptionModelSnapshot,
        lines: MutableList<TalentPreviewLine>,
    ) {
        val placeholder =
            when {
                nextModel.intPlaceholder("damagePercent") != null -> "damagePercent"
                nextModel.intPlaceholder("damageMultiplier") != null -> "damageMultiplier"
                else -> return
            }
        appendNumericUpgradeLine(
            localizer = localizer,
            currentModel = currentModel,
            nextModel = nextModel,
            placeholder = placeholder,
            labelKey = "ui.talent.assign.detail.damage",
            formatValue = { value -> localizer.text("ui.talent.assign.detail.damage_value", "value" to value) },
            lines = lines,
        )
    }

    private fun appendRangeUpgradeLine(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
        currentModel: DescriptionModelSnapshot?,
        nextModel: DescriptionModelSnapshot,
        lines: MutableList<TalentPreviewLine>,
    ) {
        val currentMinRange = currentModel.intPlaceholder("minRange") ?: node.minRange
        val currentMaxRange = currentModel.intPlaceholder("range") ?: node.range
        val nextMinRange = nextModel.intPlaceholder("minRange") ?: currentMinRange
        val nextMaxRange = nextModel.intPlaceholder("range") ?: currentMaxRange
        if (nextMinRange == currentMinRange && nextMaxRange == currentMaxRange) {
            return
        }
        lines +=
            upgradeLine(
                localizer = localizer,
                labelKey = "ui.talent.assign.detail.range",
                value = rangeText(localizer, nextMinRange, nextMaxRange),
            )
    }

    private fun appendNumericUpgradeLine(
        localizer: Localizer,
        currentModel: DescriptionModelSnapshot?,
        nextModel: DescriptionModelSnapshot,
        placeholder: String,
        labelKey: String,
        formatValue: (Int) -> String,
        lines: MutableList<TalentPreviewLine>,
    ) {
        val nextValue = nextModel.intPlaceholder(placeholder) ?: return
        val currentValue = currentModel.intPlaceholder(placeholder) ?: 0
        if (nextValue == currentValue) {
            return
        }
        lines += upgradeLine(localizer, labelKey, formatValue(nextValue))
    }

    private fun upgradeLine(
        localizer: Localizer,
        labelKey: String,
        value: String,
    ): TalentPreviewLine =
        TalentPreviewLine(
            label = localizer.text(labelKey),
            value = localizer.text("ui.talent.assign.detail.upgrade_to", "value" to value),
            iconKey = null,
            toneToken = TalentPreviewToneToken.POSITIVE,
        )

    private fun nextDescriptionPreviewLine(
        localizer: Localizer,
        nextModel: DescriptionModelSnapshot,
    ): TalentPreviewLine? =
        DescriptionPresenter
            .presentSurfaceLines(localizer, nextModel, DescriptionSurface.TALENT_ACTIVE)
            .firstOrNull { line -> line.kind != DescriptionLineKind.KEYWORD && !line.text.isBreakpointTeaser() }
            ?.let { line ->
                TalentPreviewLine(
                    label = line.kind.currentDetailLabel(localizer),
                    value = line.text,
                    iconKey = null,
                    toneToken = TalentPreviewToneToken.POSITIVE,
                )
            }

    private fun actionsBlock(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): TalentDetailBlock {
        val actionLines =
            buildList {
                add(
                    TalentPreviewLine(
                        label = "Enter",
                        value =
                            when {
                                node.state == TalentNodeStateSnapshot.LOCKED -> localizer.text("ui.talent.assign.action.locked")
                                node.isMaxRank -> localizer.text("ui.talent.assign.action.max_rank")
                                else -> localizer.text("ui.talent.assign.action.learn")
                            },
                        iconKey = null,
                        toneToken = if (node.state == TalentNodeStateSnapshot.LOCKED) TalentPreviewToneToken.LOCKED else TalentPreviewToneToken.POSITIVE,
                    ),
                )
                if (node.category != TalentCategory.PASSIVE) {
                    add(TalentPreviewLine("R", localizer.text("ui.talent.assign.action.reserve"), null, TalentPreviewToneToken.SECONDARY))
                }
                add(TalentPreviewLine("Esc", localizer.text("ui.talent.assign.action.back"), null, TalentPreviewToneToken.SECONDARY))
            }
        return TalentDetailBlock(
            kind = TalentDetailBlockKind.ACTIONS,
            iconKey = null,
            primaryText = "",
            secondaryText = null,
            bodyLines = actionLines,
            toneToken = TalentPreviewToneToken.SECONDARY,
        )
    }

    private fun currentDetailLines(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): List<TalentPreviewLine> {
        if (node.category == TalentCategory.PASSIVE) {
            return passiveCurrentDetailLines(localizer, node)
        }
        val descriptionLines =
            DescriptionPresenter.presentSurfaceLines(
                localizer = localizer,
                model = node.descriptionModel,
                surface = DescriptionSurface.TALENT_ACTIVE,
            ).filterNot { line -> line.kind == DescriptionLineKind.KEYWORD || line.text.isBreakpointTeaser() }
        val metricLines = currentEffectMetricLines(localizer, node.descriptionModel)
        val fallbackLines =
            if (metricLines.isEmpty()) {
                descriptionPreviewLines(localizer, descriptionLines)
            } else {
                emptyList()
            }
        return (structuredCurrentDetailLines(localizer, node) + metricLines + fallbackLines + activeSlotNoteLine(localizer, node))
            .distinctBy { line -> line.label to line.value }
    }

    private fun passiveCurrentDetailLines(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): List<TalentPreviewLine> =
        node.passiveDetail
            ?.currentLines
            ?.map { line -> line.toPreviewLine(localizer) }
            .orEmpty()

    private fun passiveNextRankPreviewLines(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): List<TalentPreviewLine> =
        node.passiveDetail
            ?.nextLines
            ?.map { line -> line.toPreviewLine(localizer) }
            .orEmpty()

    private fun activeSlotNoteLine(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): List<TalentPreviewLine> =
        if (node.category == TalentCategory.ACTIVE || node.category == TalentCategory.SUSTAINED) {
            listOf(
                TalentPreviewLine(
                    label = localizer.text("ui.talent.assign.detail.description"),
                    value = localizer.text("ui.talent.assign.detail.active_slot_note"),
                    iconKey = null,
                    toneToken = TalentPreviewToneToken.SECONDARY,
                ),
            )
        } else {
            emptyList()
        }

    private fun structuredCurrentDetailLines(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): List<TalentPreviewLine> =
        buildList {
            add(detailLine(localizer, "ui.talent.assign.detail.type", localizer.text(node.category.labelKey())))
            if (node.requiresTarget) {
                add(detailLine(localizer, "ui.talent.assign.detail.range", rangeText(localizer, node.minRange, node.range)))
            }
            node.descriptionModel.intPlaceholder("radius")?.takeIf { radius -> radius > 0 }?.let { radius ->
                add(detailLine(localizer, "ui.talent.assign.detail.radius", radius.toString()))
            }
            if (node.resourceCost > 0) {
                add(detailLine(localizer, "ui.talent.assign.detail.resource_cost", "${node.resourceCost} ${localizer.text(node.resourceLabelKey)}"))
            }
            if (node.maxCooldown > 0) {
                add(detailLine(localizer, "ui.talent.assign.detail.cooldown", localizer.text("ui.talent.assign.detail.turns", "value" to node.maxCooldown)))
            }
        }

    private fun currentEffectMetricLines(
        localizer: Localizer,
        model: DescriptionModelSnapshot?,
    ): List<TalentPreviewLine> = effectMetricLines(localizer, model, TalentPreviewToneToken.SECONDARY)

    private fun effectMetricLines(
        localizer: Localizer,
        model: DescriptionModelSnapshot?,
        toneToken: TalentPreviewToneToken,
    ): List<TalentPreviewLine> {
        val lines = mutableListOf<TalentPreviewLine>()
        val nonDamageLines =
            buildList {
                model.textPlaceholder("statusId")?.let { statusId ->
                    add(
                        metricLine(
                            localizer = localizer,
                            labelKey = "ui.talent.assign.detail.status",
                            value = localizer.text("status.$statusId"),
                            toneToken = toneToken,
                        ),
                    )
                }
                model.intPlaceholder("statusDuration")?.takeIf { duration -> duration > 0 }?.let { duration ->
                    add(
                        metricLine(
                            localizer = localizer,
                            labelKey = "ui.talent.assign.detail.duration",
                            value = localizer.text("ui.talent.assign.detail.turns", "value" to duration),
                            toneToken = toneToken,
                        ),
                    )
                }
                model.intPlaceholder("statusMagnitude")?.takeIf { magnitude -> magnitude > 0 }?.let { magnitude ->
                    add(metricLine(localizer, "ui.talent.assign.detail.power", "+$magnitude%", toneToken))
                }
                model.intPlaceholder("resourceRestorePercent")?.takeIf { value -> value > 0 }?.let { value ->
                    add(metricLine(localizer, "ui.talent.assign.detail.resource_restore", "$value%", toneToken))
                }
                model.intPlaceholder("resourceRestoreAmount")?.takeIf { value -> value > 0 }?.let { value ->
                    add(metricLine(localizer, "ui.talent.assign.detail.resource_restore", "+$value", toneToken))
                }
                model.intPlaceholder("healPercent")?.takeIf { value -> value > 0 }?.let { value ->
                    add(metricLine(localizer, "ui.talent.assign.detail.healing", "$value%", toneToken))
                }
                model.intPlaceholder("displacementDistance")?.takeIf { value -> value > 0 }?.let { value ->
                    add(
                        metricLine(
                            localizer = localizer,
                            labelKey = "ui.talent.assign.detail.displacement",
                            value = localizer.text("ui.talent.assign.detail.range.cells", "range" to value),
                            toneToken = toneToken,
                        ),
                    )
                }
                model.intPlaceholder("knockback")?.takeIf { value -> value > 0 }?.let { value ->
                    add(
                        metricLine(
                            localizer = localizer,
                            labelKey = "ui.talent.assign.detail.knockback",
                            value = localizer.text("ui.talent.assign.detail.range.cells", "range" to value),
                            toneToken = toneToken,
                        ),
                    )
                }
            }
        meaningfulDamageValue(localizer, model, hasNonDamageMetrics = nonDamageLines.isNotEmpty())?.let { damage ->
            lines +=
                metricLine(
                    localizer = localizer,
                    labelKey = "ui.talent.assign.detail.damage",
                    value = localizer.text("ui.talent.assign.detail.damage_value", "value" to damage),
                    toneToken = toneToken,
                )
        }
        lines += nonDamageLines
        return lines
    }

    private fun meaningfulDamageValue(
        localizer: Localizer,
        model: DescriptionModelSnapshot?,
        hasNonDamageMetrics: Boolean,
    ): Int? {
        val damage = currentDamageValue(model)?.takeIf { value -> value > 0 } ?: return null
        if (damage != DEFAULT_DAMAGE_PERCENT) {
            return damage
        }
        if (model.referencesAnyPlaceholder(localizer, "damagePercent", "damageMultiplier")) {
            return damage
        }
        return if (hasNonDamageMetrics) null else damage
    }

    private fun DescriptionModelSnapshot?.referencesAnyPlaceholder(
        localizer: Localizer,
        vararg names: String,
    ): Boolean {
        val templateKey = this?.templateKey ?: return false
        val template = localizer.text(templateKey)
        return names.any { name -> "{$name}" in template }
    }

    private fun currentDamageValue(model: DescriptionModelSnapshot?): Int? =
        model.intPlaceholder("damagePercent") ?: model.intPlaceholder("damageMultiplier")

    private fun descriptionPreviewLines(
        localizer: Localizer,
        lines: List<DescriptionLine>,
    ): List<TalentPreviewLine> =
        lines.map { line ->
            TalentPreviewLine(
                label = line.kind.currentDetailLabel(localizer),
                value = line.text,
                iconKey = null,
                toneToken = line.kind.toPreviewToneToken(),
            )
        }

    private fun DescriptionLineKind.currentDetailLabel(localizer: Localizer): String =
        when (this) {
            DescriptionLineKind.PRIMARY -> localizer.text("ui.talent.assign.detail.effect")
            DescriptionLineKind.SECONDARY,
            DescriptionLineKind.KEYWORD,
            -> localizer.text("ui.talent.assign.detail.description")

            DescriptionLineKind.STATE -> localizer.text("ui.talent.assign.detail.status")
        }

    private fun detailLine(
        localizer: Localizer,
        labelKey: String,
        value: String,
    ): TalentPreviewLine =
        metricLine(localizer, labelKey, value, TalentPreviewToneToken.SECONDARY)

    private fun metricLine(
        localizer: Localizer,
        labelKey: String,
        value: String,
        toneToken: TalentPreviewToneToken,
    ): TalentPreviewLine =
        TalentPreviewLine(
            label = localizer.text(labelKey),
            value = value,
            iconKey = null,
            toneToken = toneToken,
        )

    private fun activeSlotChoiceModal(
        localizer: Localizer,
        talents: List<TalentSlotSnapshot>,
    ): ActiveSlotChoiceModalModel {
        val bySlot = talents.associateBy(TalentSlotSnapshot::slot)
        return ActiveSlotChoiceModalModel(
            title = localizer.text("ui.talent.active_slot_choice.title"),
            items =
                (1..PLAYER_ACTIVE_TALENT_SLOT_COUNT).map { slot ->
                    val talent = bySlot[slot]
                    ActiveSlotChoiceModalItem(
                        hotkeyText = slot.toString(),
                        kind =
                            if (talent == null) {
                                ActiveSlotChoiceModalItemKind.SLOT_EMPTY
                            } else {
                                ActiveSlotChoiceModalItemKind.SLOT_REPLACE_TARGET
                            },
                        slot = slot,
                        iconKey = talent?.iconKey,
                        primaryLabel = talent?.nameKey?.let(localizer::text) ?: localizer.text("ui.talent.active_slot_choice.empty_slot"),
                        secondaryLabel = talent?.let { localizer.text("ui.talent.active_slot_choice.replace_target") },
                        focused = slot == 1,
                    )
                } +
                    ActiveSlotChoiceModalItem(
                        hotkeyText = "R",
                        kind = ActiveSlotChoiceModalItemKind.RESERVE_ACTION,
                        slot = null,
                        iconKey = null,
                        primaryLabel = localizer.text("ui.talent.active_slot_choice.reserve_action"),
                        secondaryLabel = localizer.text("ui.talent.active_slot_choice.reserve_action_hint"),
                        focused = false,
                    ),
            cancelHintText = localizer.text("ui.talent.active_slot_choice.cancel_hint"),
        )
    }

    private fun legend(
        localizer: Localizer,
        hasPendingOverlay: Boolean,
    ): TalentAssignLegendModel =
        TalentAssignLegendModel(
            items =
                listOf(
                    TalentAssignLegendItem(TalentLegendItemKind.STATE_TONE, null, localizer.text("ui.talent.assign.legend.learnable"), "[+]", TalentTreeNodeToneToken.TALENT_LEARNABLE),
                    TalentAssignLegendItem(TalentLegendItemKind.STATE_TONE, null, localizer.text("ui.talent.assign.legend.locked"), "[x]", TalentTreeNodeToneToken.TALENT_LOCKED),
                    TalentAssignLegendItem(TalentLegendItemKind.STATE_TONE, null, localizer.text("ui.talent.assign.legend.active"), "[*]", TalentTreeNodeToneToken.TALENT_ACTIVE),
                    TalentAssignLegendItem(TalentLegendItemKind.STATE_TONE, null, localizer.text("ui.talent.assign.legend.reserve"), "[r]", TalentTreeNodeToneToken.TALENT_RESERVE),
                    TalentAssignLegendItem(TalentLegendItemKind.FOCUS, null, localizer.text("ui.talent.assign.legend.focus"), null, null),
                ) +
                    if (hasPendingOverlay) {
                        listOf(TalentAssignLegendItem(TalentLegendItemKind.PENDING_OVERLAY, null, localizer.text("ui.talent.assign.legend.pending"), null, null))
                    } else {
                        emptyList()
                    },
        )

    private fun footerHints(
        localizer: Localizer,
        selectedNode: TalentTreeNodeSnapshot?,
    ): List<TalentAssignFooterHintModel> =
        buildList {
            add(
                TalentAssignFooterHintModel(
                    TalentAssignFooterHintKind.SELECT,
                    localizer.text("ui.talent.assign.footer.select.key"),
                    localizer.text("ui.talent.assign.footer.select.label"),
                ),
            )
            add(
                TalentAssignFooterHintModel(
                    TalentAssignFooterHintKind.SWITCH_TREE,
                    localizer.text("ui.talent.assign.footer.switch_tree.key"),
                    localizer.text("ui.talent.assign.footer.switch_tree.label"),
                ),
            )
            add(
                TalentAssignFooterHintModel(
                    TalentAssignFooterHintKind.LEARN,
                    localizer.text("ui.talent.assign.footer.learn.key"),
                    localizer.text("ui.talent.assign.footer.learn.label"),
                ),
            )
            if (selectedNode?.category != TalentCategory.PASSIVE) {
                add(
                    TalentAssignFooterHintModel(
                        TalentAssignFooterHintKind.RESERVE,
                        localizer.text("ui.talent.assign.footer.reserve.key"),
                        localizer.text("ui.talent.assign.footer.reserve.label"),
                    ),
                )
            }
            add(
                TalentAssignFooterHintModel(
                    TalentAssignFooterHintKind.CLOSE,
                    localizer.text("ui.talent.assign.footer.close.key"),
                    localizer.text("ui.talent.assign.footer.close.label"),
                ),
            )
        }

    private fun TalentNodeStateSnapshot.stateGlyph(): String =
        when (this) {
            TalentNodeStateSnapshot.LOCKED -> "x"
            TalentNodeStateSnapshot.LEARNABLE -> "+"
            TalentNodeStateSnapshot.LEARNED_RESERVE -> "r"
            TalentNodeStateSnapshot.LEARNED_ACTIVE -> "*"
        }

    private fun TalentNodeStateSnapshot.toToneToken(): TalentTreeNodeToneToken =
        when (this) {
            TalentNodeStateSnapshot.LOCKED -> TalentTreeNodeToneToken.TALENT_LOCKED
            TalentNodeStateSnapshot.LEARNABLE -> TalentTreeNodeToneToken.TALENT_LEARNABLE
            TalentNodeStateSnapshot.LEARNED_RESERVE -> TalentTreeNodeToneToken.TALENT_RESERVE
            TalentNodeStateSnapshot.LEARNED_ACTIVE -> TalentTreeNodeToneToken.TALENT_ACTIVE
        }

    private fun TalentNodeStateSnapshot.isLearned(): Boolean =
        when (this) {
            TalentNodeStateSnapshot.LEARNED_ACTIVE,
            TalentNodeStateSnapshot.LEARNED_RESERVE,
            -> true

            TalentNodeStateSnapshot.LOCKED,
            TalentNodeStateSnapshot.LEARNABLE,
            -> false
        }

    private fun TalentNodeStateSnapshot.toSidebarLineRole(): TalentSidebarLineRole =
        when (this) {
            TalentNodeStateSnapshot.LOCKED -> TalentSidebarLineRole.NODE_LOCKED
            TalentNodeStateSnapshot.LEARNABLE -> TalentSidebarLineRole.NODE_LEARNABLE
            TalentNodeStateSnapshot.LEARNED_RESERVE -> TalentSidebarLineRole.NODE_LEARNED_RESERVE
            TalentNodeStateSnapshot.LEARNED_ACTIVE -> TalentSidebarLineRole.NODE_LEARNED_ACTIVE
        }

    private fun DescriptionLineKind.toSidebarLineRole(): TalentSidebarLineRole =
        when (this) {
            DescriptionLineKind.SECONDARY -> TalentSidebarLineRole.DESCRIPTION_SECONDARY
            DescriptionLineKind.STATE -> TalentSidebarLineRole.DESCRIPTION_STATE
            DescriptionLineKind.PRIMARY,
            DescriptionLineKind.KEYWORD,
            -> TalentSidebarLineRole.DESCRIPTION_PRIMARY
        }

    private fun TalentTreeNodeToneToken.toSidebarLineRole(): TalentSidebarLineRole =
        when (this) {
            TalentTreeNodeToneToken.TALENT_LOCKED -> TalentSidebarLineRole.NODE_LOCKED
            TalentTreeNodeToneToken.TALENT_LEARNABLE -> TalentSidebarLineRole.NODE_LEARNABLE
            TalentTreeNodeToneToken.TALENT_RESERVE -> TalentSidebarLineRole.NODE_LEARNED_RESERVE
            TalentTreeNodeToneToken.TALENT_ACTIVE -> TalentSidebarLineRole.NODE_LEARNED_ACTIVE
        }

    private fun TalentPreviewToneToken.toSidebarLineRole(): TalentSidebarLineRole =
        when (this) {
            TalentPreviewToneToken.SECONDARY,
            TalentPreviewToneToken.LOCKED,
            -> TalentSidebarLineRole.DESCRIPTION_SECONDARY

            TalentPreviewToneToken.PRIMARY,
            TalentPreviewToneToken.POSITIVE,
            TalentPreviewToneToken.WARNING,
            -> TalentSidebarLineRole.DESCRIPTION_PRIMARY
        }

    private fun TalentLegendItemKind.toSidebarLineRole(): TalentSidebarLineRole =
        when (this) {
            TalentLegendItemKind.STATE_TONE -> TalentSidebarLineRole.FOOTER
            TalentLegendItemKind.FOCUS -> TalentSidebarLineRole.ACTION_TITLE
            TalentLegendItemKind.PENDING_OVERLAY -> TalentSidebarLineRole.ACTION_HINT
        }

    private fun DescriptionLineKind.toPreviewToneToken(): TalentPreviewToneToken =
        when (this) {
            DescriptionLineKind.PRIMARY -> TalentPreviewToneToken.PRIMARY
            DescriptionLineKind.SECONDARY,
            DescriptionLineKind.KEYWORD,
            -> TalentPreviewToneToken.SECONDARY

            DescriptionLineKind.STATE -> TalentPreviewToneToken.WARNING
        }

    private fun talentRankLabel(
        committedLevel: Int,
        currentLevel: Int,
        maxLevel: Int,
    ): String =
        if (currentLevel != committedLevel) {
            "$committedLevel -> $currentLevel/$maxLevel"
        } else {
            "$committedLevel/$maxLevel"
        }

    private fun unlockRequirementLines(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): List<TalentPreviewLine> =
        if (node.unlockLevel > 1) {
            listOf(
                TalentPreviewLine(
                    label = localizer.text("ui.talent.assign.detail.unlock_level"),
                    value = node.unlockLevel.toString(),
                    iconKey = null,
                    toneToken = TalentPreviewToneToken.SECONDARY,
                ),
            )
        } else {
            emptyList()
        }

    private fun TalentNodePrerequisiteSnapshot.requiredRankText(localizer: Localizer): String =
        requiredMaxRank?.let { maxRank ->
            localizer.text("ui.talent.assign.detail.required_rank_fraction", "rank" to requiredRank, "max" to maxRank)
        } ?: localizer.text("ui.talent.assign.detail.required_rank", "rank" to requiredRank)

    private fun talentRankTransitionText(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): String {
        val nextRank = talentPreviewRank(node)
        return localizer.text(
            "ui.talent.assign.detail.rank_transition",
            "from" to node.committedRank,
            "to" to nextRank,
            "max" to node.maxRank,
        )
    }

    private fun talentPreviewRank(node: TalentTreeNodeSnapshot): Int =
        if (node.rank == node.committedRank && node.rank < node.maxRank) {
            node.rank + 1
        } else {
            node.rank
        }

    private fun talentPointCost(node: TalentTreeNodeSnapshot): Int =
        (talentPreviewRank(node) - node.committedRank).coerceAtLeast(0)

    private fun pointTypeText(
        localizer: Localizer,
        ownerType: TalentTreeOwnerType,
    ): String =
        when (ownerType) {
            TalentTreeOwnerType.PROFESSION -> localizer.text("ui.talent.assign.point_type.profession")
            TalentTreeOwnerType.RACE -> localizer.text("ui.talent.assign.point_type.race")
        }

    private fun rangeText(
        localizer: Localizer,
        minRange: Int,
        maxRange: Int,
    ): String =
        when {
            minRange <= 0 && maxRange <= 1 -> localizer.text("ui.talent.assign.detail.range.adjacent")
            minRange == maxRange -> localizer.text("ui.talent.assign.detail.range.cells", "range" to maxRange)
            else -> localizer.text("ui.talent.assign.detail.range.cells_span", "min" to minRange, "max" to maxRange)
        }

    private fun PassiveDetailLineSnapshot.toPreviewLine(localizer: Localizer): TalentPreviewLine =
        TalentPreviewLine(
            label = localizer.text(labelKey),
            value = renderTextToken(localizer, valueToken),
            iconKey = null,
            toneToken = tone.toTalentTone(),
        )

    private fun PassiveDetailDeltaLineSnapshot.toPreviewLine(localizer: Localizer): TalentPreviewLine =
        TalentPreviewLine(
            label = localizer.text(labelKey),
            value = renderTextToken(localizer, valueToken),
            iconKey = null,
            toneToken = tone.toTalentTone(),
        )

    private fun renderTextToken(
        localizer: Localizer,
        token: RenderTextTokenSnapshot,
    ): String =
        localizer.text(
            token.key,
            *token.arguments.map { argument -> argument.name to resolveTextArgument(localizer, argument) }.toTypedArray(),
        )

    private fun resolveTextArgument(
        localizer: Localizer,
        argument: RenderTextArgumentSnapshot,
    ): String =
        argument.value
            ?: argument.valueKey?.let(localizer::text)
            ?: argument.valueToken?.let { token -> renderTextToken(localizer, token) }
            ?: ""

    private fun PassiveDetailLineToneSnapshot.toTalentTone(): TalentPreviewToneToken =
        when (this) {
            PassiveDetailLineToneSnapshot.SECONDARY -> TalentPreviewToneToken.SECONDARY
            PassiveDetailLineToneSnapshot.POSITIVE -> TalentPreviewToneToken.POSITIVE
            PassiveDetailLineToneSnapshot.WARNING -> TalentPreviewToneToken.WARNING
        }

    private fun DescriptionModelSnapshot?.intPlaceholder(name: String): Int? =
        when (val value = this?.placeholders?.get(name)) {
            is DescriptionValueSnapshot.IntValue -> value.value
            else -> null
        }

    private fun DescriptionModelSnapshot?.textPlaceholder(name: String): String? =
        when (val value = this?.placeholders?.get(name)) {
            is DescriptionValueSnapshot.TextValue -> value.value
            else -> null
        }

    private fun String.isBreakpointTeaser(): Boolean =
        contains("breakpoint", ignoreCase = true) || contains("断点")

    private fun lockReasonText(
        localizer: Localizer,
        reason: TalentNodeLockReasonSnapshot,
    ): String {
        val args =
            when (reason.type) {
                TalentNodeLockReasonTypeSnapshot.LEVEL ->
                    arrayOf("level" to (reason.requiredLevel ?: 1).toString())

                TalentNodeLockReasonTypeSnapshot.PREREQUISITE_RANK ->
                    arrayOf(
                        "talent" to localizer.text(reason.talentNameKey ?: "ui.talent.tree.lock.unknown_talent"),
                        "rank" to (reason.requiredRank ?: 1).toString(),
                    )

                TalentNodeLockReasonTypeSnapshot.TREE_INVESTMENT ->
                    arrayOf("points" to (reason.requiredPoints ?: 0).toString())

                TalentNodeLockReasonTypeSnapshot.CROSS_TREE_INVESTMENT -> emptyArray()
            }
        return localizer.text(reason.messageKey, *args)
    }

    private fun TalentNodeStateSnapshot.stateLabelKey(): String =
        when (this) {
            TalentNodeStateSnapshot.LOCKED -> "ui.talent.assign.state.locked"
            TalentNodeStateSnapshot.LEARNABLE -> "ui.talent.assign.state.learnable"
            TalentNodeStateSnapshot.LEARNED_RESERVE -> "ui.talent.assign.state.reserve"
            TalentNodeStateSnapshot.LEARNED_ACTIVE -> "ui.talent.assign.state.active"
        }

    private fun TalentNodeStateSnapshot.toPreviewToneToken(): TalentPreviewToneToken =
        when (this) {
            TalentNodeStateSnapshot.LOCKED -> TalentPreviewToneToken.LOCKED
            TalentNodeStateSnapshot.LEARNABLE -> TalentPreviewToneToken.POSITIVE
            TalentNodeStateSnapshot.LEARNED_RESERVE,
            TalentNodeStateSnapshot.LEARNED_ACTIVE,
            -> TalentPreviewToneToken.PRIMARY
        }

    private fun TalentCategory.labelKey(): String =
        when (this) {
            TalentCategory.ACTIVE -> "ui.talent.assign.category.active"
            TalentCategory.PASSIVE -> "ui.talent.assign.category.passive"
            TalentCategory.SUSTAINED -> "ui.talent.assign.category.sustained"
        }

    private fun parseOwnerType(ownerType: String): TalentTreeOwnerType = enumValueOf(ownerType)

    private fun talentAssignIconKey(
        talentId: String,
        fallbackIconKey: String?,
    ): String? = fallbackIconKey ?: pr04ReferenceTalentIconKey(talentId)

    private fun talentAssignHeroIconKey(
        talentId: String,
        fallbackIconKey: String?,
    ): String? = fallbackIconKey ?: pr04ReferenceTalentIconKey(talentId)

    private fun pr04ReferenceTalentIconKey(talentId: String): String? =
        when (talentId) {
            "power_strike",
            "sweeping_strike",
            "linebreaker",
            "earthshaker",
            "charge",
            "sunder_armor",
            "shield_bash",
            "taunt",
            "guard_stance",
            "iron_wall",
            "bulwark_march",
            "war_cry",
            "rallying_banner",
            "battlefield_command",
            "intimidation",
            "unyielding",
            -> "dark.uiux.pr04.talent.vanguard.$talentId.icon"

            else -> null
        }

    private const val DEFAULT_DAMAGE_PERCENT = 100
}
