package com.ktome.client.ui.talent

import com.ktome.core.talent.TalentTreeOwnerType

const val PR04_TALENT_ASSIGN_REFERENCE_IMAGE_PATH: String = "UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.png"

enum class TalentTreeNodeToneToken {
    TALENT_LOCKED,
    TALENT_LEARNABLE,
    TALENT_RESERVE,
    TALENT_ACTIVE,
}

enum class TalentAssignRowKind {
    TALENT_NODE,
}

enum class TalentTreeEdgeState {
    UNSATISFIED,
    SATISFIED,
    ACTIVE,
}

enum class TalentLegendItemKind {
    STATE_TONE,
    FOCUS,
    PENDING_OVERLAY,
}

enum class TalentAssignFooterHintKind {
    SELECT,
    SWITCH_TREE,
    LEARN,
    RESERVE,
    CLOSE,
}

enum class TalentDetailBlockKind {
    HERO_ICON,
    HEADER,
    RANK_AND_COST,
    PREREQUISITE,
    PREREQUISITE_FAILED,
    CURRENT_RANK_DETAIL,
    NEXT_RANK_PREVIEW,
    ACTIONS,
}

enum class TalentPreviewToneToken {
    PRIMARY,
    SECONDARY,
    POSITIVE,
    WARNING,
    LOCKED,
}

enum class TalentActiveSlotStripState {
    FILLED,
    EMPTY,
    PENDING_DIRECT_FILL,
    PENDING_REPLACE_TARGET,
}

enum class ActiveSlotChoiceModalItemKind {
    SLOT_FILLED,
    SLOT_EMPTY,
    SLOT_REPLACE_TARGET,
    RESERVE_ACTION,
}

data class TalentAssignPanelModel(
    val referenceImagePath: String,
    val header: TalentAssignHeaderModel,
    val sections: List<TalentAssignSectionModel>,
    val detail: TalentDetailPaneModel?,
    val legend: TalentAssignLegendModel,
    val footerHints: List<TalentAssignFooterHintModel>,
    val activeSlotChoiceModal: ActiveSlotChoiceModalModel?,
) {
    val footerHelpText: String =
        footerHints.joinToString("  ") { hint -> "${hint.keyText} ${hint.labelText}".trim() }
}

data class TalentAssignHeaderModel(
    val title: String,
    val professionPointText: String,
    val racePointText: String?,
)

data class TalentAssignSectionModel(
    val treeId: String,
    val ownerType: TalentTreeOwnerType,
    val treeOwnerId: String,
    val displayName: String,
    val nodeCountText: String,
    val iconKey: String?,
    val rows: List<TalentAssignTreeRowModel>,
    val edges: List<TalentTreeEdgeProjection>,
    val scroll: TalentAssignSectionScrollModel,
)

data class TalentAssignTreeRowModel(
    val kind: TalentAssignRowKind,
    val talentId: String,
    val treeId: String,
    val ownerType: TalentTreeOwnerType,
    val treeOwnerId: String,
    val indentLevel: Int,
    val connectorPrefix: String,
    val stateMarkerText: String,
    val stateIconKey: String?,
    val skillIconKey: String?,
    val displayName: String,
    val rankText: String,
    val toneToken: TalentTreeNodeToneToken,
    val pendingOverlay: Boolean,
    val focused: Boolean,
)

data class TalentTreeEdgeProjection(
    val fromTalentId: String,
    val toTalentId: String,
    val state: TalentTreeEdgeState,
)

data class TalentAssignSectionScrollModel(
    val verticalOffset: Int,
    val hasVerticalOverflow: Boolean,
)

data class TalentAssignLegendModel(
    val items: List<TalentAssignLegendItem>,
)

data class TalentAssignLegendItem(
    val kind: TalentLegendItemKind,
    val iconKey: String?,
    val label: String,
    val markerText: String?,
    val toneToken: TalentTreeNodeToneToken?,
)

data class TalentAssignFooterHintModel(
    val kind: TalentAssignFooterHintKind,
    val keyText: String,
    val labelText: String,
)

data class TalentDetailPaneModel(
    val talentId: String,
    val blocks: List<TalentDetailBlock>,
)

data class TalentDetailBlock(
    val kind: TalentDetailBlockKind,
    val iconKey: String?,
    val primaryText: String,
    val secondaryText: String?,
    val bodyLines: List<TalentPreviewLine>,
    val toneToken: TalentPreviewToneToken,
)

data class TalentPreviewLine(
    val label: String?,
    val value: String,
    val iconKey: String?,
    val toneToken: TalentPreviewToneToken,
)

data class TalentActiveSlotStripItem(
    val slot: Int,
    val talentId: String?,
    val primaryIconKey: String?,
    val primaryLabel: String,
    val secondaryLabel: String?,
    val state: TalentActiveSlotStripState,
    val focused: Boolean,
)

data class ActiveSlotChoiceModalModel(
    val title: String,
    val items: List<ActiveSlotChoiceModalItem>,
    val cancelHintText: String,
)

data class ActiveSlotChoiceModalItem(
    val hotkeyText: String,
    val kind: ActiveSlotChoiceModalItemKind,
    val slot: Int?,
    val iconKey: String?,
    val primaryLabel: String,
    val secondaryLabel: String?,
    val focused: Boolean,
)
