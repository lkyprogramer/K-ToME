package com.ktome.client.ui.talent

import com.ktome.client.input.OverlayState
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.TalentNodeStateSnapshot
import com.ktome.core.snapshot.TalentTreeNodeSnapshot
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
    ): List<TalentSidebarLine> {
        val lines = mutableListOf<TalentSidebarLine>()
        lines +=
            TalentSidebarLine(
                text = localizer.text("ui.sidebar.talent_points", "value" to uiState.playerStatus.talentPoints),
                role = TalentSidebarLineRole.POINTS,
            )
        if (uiState.playerStatus.raceTalentPoints > 0) {
            lines +=
                TalentSidebarLine(
                    text = localizer.text("ui.sidebar.race_talent_points", "value" to uiState.playerStatus.raceTalentPoints),
                    role = TalentSidebarLineRole.POINTS,
                )
        }
        if (overlayState.activeModalKind == ModalFrameKind.ACTIVE_TALENT_SLOT_CHOICE) {
            lines += TalentSidebarLine(localizer.text("ui.talent.active_slot_choice.title"), TalentSidebarLineRole.ACTION_TITLE)
            lines += TalentSidebarLine(localizer.text("ui.talent.active_slot_choice.body"), TalentSidebarLineRole.FOOTER)
            lines += TalentSidebarLine(localizer.text("ui.talent.active_slot_choice.replace_slots"), TalentSidebarLineRole.ACTION_HINT)
            lines += TalentSidebarLine(localizer.text("ui.talent.active_slot_choice.reserve"), TalentSidebarLineRole.ACTION_HINT)
        }

        val treeNodes = uiState.talentTrees.flatMap { tree -> tree.nodes }
        val selectedTalentId = treeNodes.getOrNull(overlayState.talentTreeSelection)?.talentId
        uiState.talentTrees.forEach { tree ->
            lines +=
                TalentSidebarLine(
                    text = localizer.text(tree.nameKey),
                    role = TalentSidebarLineRole.TREE_HEADER,
                    iconKey = tree.iconKey,
                )
            tree.nodes.forEach { node ->
                lines +=
                    TalentSidebarLine(
                        text = talentTreeNodeLabel(localizer, node),
                        role = node.state.toSidebarLineRole(),
                        iconKey = node.iconKey,
                        selected = selectedTalentId == node.talentId,
                    )
            }
        }

        treeNodes.getOrNull(overlayState.talentTreeSelection)?.let { node ->
            if (overlayState.talentTreePreviewExpanded) {
                DescriptionPresenter.presentTalentTreeNodeLines(localizer, node).forEach { line ->
                    lines += TalentSidebarLine(text = line.text, role = line.kind.toSidebarLineRole())
                }
                lines +=
                    TalentSidebarLine(
                        text =
                            talentUsageSummary(
                                localizer = localizer,
                                resourceCost = node.resourceCost,
                                resourceLabelKey = node.resourceLabelKey,
                                requiresTarget = node.requiresTarget,
                                range = node.range,
                            ),
                        role = TalentSidebarLineRole.DESCRIPTION_PRIMARY,
                    )
            } else {
                lines += TalentSidebarLine(localizer.text("ui.talent.tree.preview_collapsed"), TalentSidebarLineRole.FOOTER)
            }
        }
        lines += TalentSidebarLine(localizer.text("ui.talent.tree.footer.active_slots_from_slot_panel"), TalentSidebarLineRole.FOOTER)
        lines += TalentSidebarLine(localizer.text("ui.controls.talent_assign"), TalentSidebarLineRole.FOOTER)
        return lines
    }

    private fun talentTreeNodeLabel(
        localizer: Localizer,
        node: TalentTreeNodeSnapshot,
    ): String = "[${node.state.stateGlyph()}] ${localizer.text(node.nameKey)} ${talentRankLabel(node.rank, node.committedRank, node.maxRank)}"

    private fun TalentNodeStateSnapshot.stateGlyph(): String =
        when (this) {
            TalentNodeStateSnapshot.LOCKED -> "x"
            TalentNodeStateSnapshot.LEARNABLE -> "+"
            TalentNodeStateSnapshot.LEARNED_RESERVE -> "r"
            TalentNodeStateSnapshot.LEARNED_ACTIVE -> "*"
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

    private fun talentRankLabel(
        level: Int,
        committedLevel: Int,
        maxLevel: Int,
    ): String =
        if (level == committedLevel) {
            "$level/$maxLevel"
        } else {
            "$committedLevel->$level/$maxLevel"
        }

    private fun talentUsageSummary(
        localizer: Localizer,
        resourceCost: Int,
        resourceLabelKey: String,
        requiresTarget: Boolean,
        range: Int,
    ): String {
        val resourceText = "$resourceCost ${localizer.text(resourceLabelKey)}"
        if (!requiresTarget) {
            return resourceText
        }
        return "$resourceText · ${localizer.text("ui.sidebar.target.range", "range" to range)}"
    }
}
