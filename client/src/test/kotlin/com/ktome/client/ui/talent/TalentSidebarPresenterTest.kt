package com.ktome.client.ui.talent

import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.ui.layout.ModalFrame
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.core.snapshot.DescriptionModelSnapshot
import com.ktome.core.snapshot.DescriptionValueSnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.PassiveDetailDeltaLineSnapshot
import com.ktome.core.snapshot.PassiveDetailLineKindSnapshot
import com.ktome.core.snapshot.PassiveDetailLineSnapshot
import com.ktome.core.snapshot.PassiveDetailLineToneSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.TalentActiveSlotChoiceRequirementSnapshot
import com.ktome.core.snapshot.TalentPassiveDetailSnapshot
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
import com.ktome.core.save.SaveManager
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class TalentSidebarPresenterTest {
    private val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)

    @Test
    fun pr04BuildsTalentAssignPanelModel() {
        val panel =
            panel(
                talentTrees =
                    listOf(
                        talentTree(
                            iconKey = "icon.tree.vanguard_arms",
                            nodes =
                                listOf(
                                    talentNode("locked", state = TalentNodeStateSnapshot.LOCKED, iconKey = "icon.skill.locked"),
                                    talentNode("learnable", state = TalentNodeStateSnapshot.LEARNABLE, iconKey = "icon.skill.learnable"),
                                    talentNode("reserve", state = TalentNodeStateSnapshot.LEARNED_RESERVE, rank = 1, iconKey = "icon.skill.reserve"),
                                    talentNode("active", state = TalentNodeStateSnapshot.LEARNED_ACTIVE, rank = 1, iconKey = "icon.skill.active"),
                                ),
                        ),
                    ),
                overlayState = OverlayState(mode = UiMode.TALENT_ASSIGN, talentTreeSelection = 1),
            )

        assertEquals(PR04_TALENT_ASSIGN_REFERENCE_IMAGE_PATH, panel.referenceImagePath)
        assertEquals("Talent Assignment", panel.header.title)
        assertEquals("Profession Talent Points: 2", panel.header.professionPointText)
        assertNull(panel.header.racePointText)
        assertEquals("Arms", panel.sections.single().displayName)
        assertEquals("4/4", panel.sections.single().nodeCountText)
        assertEquals("icon.tree.vanguard_arms", panel.sections.single().iconKey)
        assertEquals(
            listOf("[x]", "[+]", "[r]", "[*]"),
            panel.sections.single().rows.map(TalentAssignTreeRowModel::stateMarkerText),
        )
        assertEquals(
            listOf(
                TalentTreeNodeToneToken.TALENT_LOCKED,
                TalentTreeNodeToneToken.TALENT_LEARNABLE,
                TalentTreeNodeToneToken.TALENT_RESERVE,
                TalentTreeNodeToneToken.TALENT_ACTIVE,
            ),
            panel.sections.single().rows.map(TalentAssignTreeRowModel::toneToken),
        )
        assertEquals(listOf(false, true, false, false), panel.sections.single().rows.map(TalentAssignTreeRowModel::focused))
        assertNotNull(panel.detail)
    }

    @Test
    fun talentAssignPanelModelDoesNotMixNodeAndNonNodeStateFields() {
        val panel = panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge")))))

        assertEquals(listOf(TalentAssignRowKind.TALENT_NODE), panel.sections.single().rows.map(TalentAssignTreeRowModel::kind))
        assertTrue(panel.sections.single().rows.none { row -> row.displayName == panel.sections.single().displayName })
        assertTrue(panel.sections.single().rows.all { row -> row.stateIconKey == null })
    }

    @Test
    fun pr04HeaderPointTextsComeFromSnapshotNotPresenterRecompute() {
        val panel =
            panel(
                talentPoints = 7,
                raceTalentPoints = 3,
                talentTrees =
                    listOf(
                        talentTree(nodes = listOf(talentNode("charge"))),
                        talentTree(
                            treeId = "shalore_moon",
                            ownerType = TalentTreeOwnerType.RACE,
                            treeOwnerId = "shalore",
                            nodes =
                                listOf(
                                    talentNode(
                                        "moon_blessing",
                                        treeId = "shalore_moon",
                                        ownerType = TalentTreeOwnerType.RACE,
                                        treeOwnerId = "shalore",
                                    ),
                                ),
                        ),
                    ),
            )

        assertEquals("Profession Talent Points: 7", panel.header.professionPointText)
        assertEquals("Race Talent Points: 3", panel.header.racePointText)
    }

    @Test
    fun pr04SectionNodeCountTextUsesVisibleOverTotal() {
        val panel = panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge"), talentNode("war_cry")))))

        assertEquals("2/2", panel.sections.single().nodeCountText)
    }

    @Test
    fun pr04PendingRowsRenderCommittedToPreviewRankTransition() {
        val panel =
            panel(
                talentTrees =
                    listOf(
                        talentTree(
                            nodes =
                                listOf(
                                    talentNode(
                                        "charge",
                                        rank = 1,
                                        committedRank = 0,
                                        hasPendingAllocation = true,
                                    ),
                                ),
                        ),
                    ),
            )

        assertEquals("0 -> 1/5", panel.sections.single().rows.single().rankText)
    }

    @Test
    fun pr04StateMarkerAndStateIconKeyAreMutuallyExclusivePerRow() {
        val panel =
            panel(
                talentTrees =
                    listOf(
                        talentTree(
                            nodes =
                                listOf(
                                    talentNode("locked", state = TalentNodeStateSnapshot.LOCKED),
                                    talentNode("learnable", state = TalentNodeStateSnapshot.LEARNABLE),
                                    talentNode("reserve", state = TalentNodeStateSnapshot.LEARNED_RESERVE, rank = 1),
                                    talentNode("active", state = TalentNodeStateSnapshot.LEARNED_ACTIVE, rank = 1),
                                ),
                        ),
                    ),
            )

        assertTrue(panel.sections.single().rows.all { row -> row.stateMarkerText.isNotBlank() && row.stateIconKey == null })
    }

    @Test
    fun pr04FooterHelpTextMirrorsReferenceKeyboardStrip() {
        val panel = panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge")))))
        val actions = requireNotNull(panel.detail).blocks.single { block -> block.kind == TalentDetailBlockKind.ACTIONS }

        assertEquals(
            listOf(
                TalentAssignFooterHintKind.SELECT to ("Up/Down" to "select"),
                TalentAssignFooterHintKind.SWITCH_TREE to ("Left/Right" to "switch tree"),
                TalentAssignFooterHintKind.LEARN to ("Enter" to "learn"),
                TalentAssignFooterHintKind.RESERVE to ("R" to "reserve"),
                TalentAssignFooterHintKind.CLOSE to ("Esc" to "close"),
            ),
            panel.footerHints.map { hint -> hint.kind to (hint.keyText to hint.labelText) },
        )
        assertEquals("Up/Down select  Left/Right switch tree  Enter learn  R reserve  Esc close", panel.footerHelpText)
        assertTrue(actions.bodyLines.any { line -> line.label == "Enter" && line.value == "Learn" })
    }

    @Test
    fun passiveTalentAssignActionsDoNotShowReserveCommands() {
        val panel =
            panel(
                talentTrees =
                    listOf(
                        talentTree(
                            nodes =
                                listOf(
                                    talentNode(
                                        "unyielding",
                                        category = TalentCategory.PASSIVE,
                                        requiresTarget = false,
                                        range = 0,
                                        minRange = 0,
                                    ),
                                ),
                        ),
                    ),
            )
        val actions = requireNotNull(panel.detail).blocks.single { block -> block.kind == TalentDetailBlockKind.ACTIONS }

        assertFalse(panel.footerHints.any { hint -> hint.kind == TalentAssignFooterHintKind.RESERVE })
        assertFalse(actions.bodyLines.any { line -> line.label == "R" || line.value == "Reserve" })
        assertEquals(listOf("Enter" to "Learn", "Esc" to "Back"), actions.bodyLines.map { line -> line.label to line.value })
    }

    @Test
    fun lockedPassiveTalentAssignActionsStayLockedWithoutReserveCommands() {
        val panel =
            panel(
                talentTrees =
                    listOf(
                        talentTree(
                            nodes =
                                listOf(
                                    talentNode(
                                        "unyielding",
                                        state = TalentNodeStateSnapshot.LOCKED,
                                        category = TalentCategory.PASSIVE,
                                        requiresTarget = false,
                                        range = 0,
                                        minRange = 0,
                                        lockReasons =
                                            listOf(
                                                TalentNodeLockReasonSnapshot(
                                                    type = TalentNodeLockReasonTypeSnapshot.LEVEL,
                                                    messageKey = "ui.talent.tree.lock.level",
                                                    requiredLevel = 5,
                                                    currentLevel = 1,
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )
        val actions = requireNotNull(panel.detail).blocks.single { block -> block.kind == TalentDetailBlockKind.ACTIONS }

        assertEquals(listOf("Enter" to "Locked", "Esc" to "Back"), actions.bodyLines.map { line -> line.label to line.value })
        assertFalse(panel.footerHints.any { hint -> hint.kind == TalentAssignFooterHintKind.RESERVE })
    }

    @Test
    fun pr04TalentAssignLayoutCentralizesPointerAndRendererGeometry() {
        val layout =
            TalentAssignPanelLayoutSolver.resolveForViewport(
                TalentAssignPanelModalBoundsRequest(
                    viewportWidth = 1280f,
                    viewportHeight = 800f,
                ),
            )

        assertEquals(18, layout.body.list.visibleSlots)
        assertTrue(250f >= layout.body.list.bounds.x && 250f < layout.body.list.bounds.right)
        assertTrue(624f >= layout.body.list.bounds.y && 624f < layout.body.list.bounds.top)
        assertTrue(704f >= layout.body.scrollbar.bounds.x && 704f < layout.body.scrollbar.bounds.right)
        assertTrue(120f >= layout.body.scrollbar.bounds.y && 120f < layout.body.scrollbar.bounds.top)
        assertEquals(7, TalentAssignPanelLayoutSolver.resolveListViewport(TalentAssignListViewportRequest(25, 24, 18)).firstVisibleIndex)
    }

    @Test
    fun pr04SectionsRowsContainOnlyTalentNodes() {
        val panel = panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge"), talentNode("war_cry")))))

        assertEquals(2, panel.sections.single().rows.size)
        assertTrue(panel.sections.single().rows.all { row -> row.kind == TalentAssignRowKind.TALENT_NODE })
    }

    @Test
    fun pr04RowsFollowSnapshotOrderAndPrerequisiteIndentation() {
        val panel = panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge"), talentNode("war_cry"), talentNode("sunder")))))

        assertEquals(listOf("charge", "war_cry", "sunder"), panel.sections.single().rows.map(TalentAssignTreeRowModel::talentId))
        assertEquals(listOf(0, 0, 0), panel.sections.single().rows.map(TalentAssignTreeRowModel::indentLevel))
    }

    @Test
    fun pr04RowsHaveEmptyConnectorPrefixWhenPrerequisiteSourceDeferred() {
        val panel = panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge"), talentNode("war_cry")))))

        assertTrue(panel.sections.single().rows.all { row -> row.connectorPrefix.isEmpty() })
        assertTrue(panel.sections.single().edges.isEmpty())
    }

    @Test
    fun pr04EdgeStateReflectsBothEndpointStates() {
        val panel =
            panel(
                talentTrees =
                    listOf(
                        talentTree(
                            nodes =
                                listOf(
                                    talentNode("charge", state = TalentNodeStateSnapshot.LEARNED_ACTIVE, rank = 1),
                                    talentNode("war_cry", state = TalentNodeStateSnapshot.LEARNABLE),
                                ),
                        ),
                    ),
            )

        assertTrue(panel.sections.single().edges.isEmpty())
    }

    @Test
    fun selectedNodeUsesFullTreeOwnerIdentity() {
        val identity =
            TalentTreeSelectionIdentity(
                talentId = "shared",
                treeId = "race_tree",
                ownerType = TalentTreeOwnerType.RACE,
                treeOwnerId = "shalore",
            )
        val panel =
            panel(
                talentTrees =
                    listOf(
                        talentTree(treeId = "profession_tree", nodes = listOf(talentNode("shared", treeId = "profession_tree"))),
                        talentTree(
                            treeId = "race_tree",
                            ownerType = TalentTreeOwnerType.RACE,
                            treeOwnerId = "shalore",
                            nodes =
                                listOf(
                                    talentNode(
                                        "shared",
                                        treeId = "race_tree",
                                        ownerType = TalentTreeOwnerType.RACE,
                                        treeOwnerId = "shalore",
                                    ),
                                ),
                        ),
                    ),
                overlayState = OverlayState(mode = UiMode.TALENT_ASSIGN, talentTreeSelectionIdentity = identity),
            )

        assertFalse(panel.sections[0].rows.single().focused)
        assertTrue(panel.sections[1].rows.single().focused)
    }

    @Test
    fun rendersRaceTreeRowsWithoutFilteringSnapshotTrees() {
        val panel =
            panel(
                raceTalentPoints = 1,
                talentTrees =
                    listOf(
                        talentTree(nodes = listOf(talentNode("charge"))),
                        talentTree(
                            treeId = "shalore_moon",
                            ownerType = TalentTreeOwnerType.RACE,
                            treeOwnerId = "shalore",
                            nodes =
                                listOf(
                                    talentNode(
                                        "moon_blessing",
                                        treeId = "shalore_moon",
                                        ownerType = TalentTreeOwnerType.RACE,
                                        treeOwnerId = "shalore",
                                    ),
                                ),
                        ),
                    ),
            )

        assertEquals(listOf(TalentTreeOwnerType.PROFESSION, TalentTreeOwnerType.RACE), panel.sections.map(TalentAssignSectionModel::ownerType))
        assertEquals("Race Talent Points: 1", panel.header.racePointText)
    }

    @Test
    fun pr04LegendIncludesFourStateToneAndFocusAndIncludesPendingOnlyWhenAnyRowIsPending() {
        val plainPanel = panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge")))))
        val pendingPanel =
            panel(
                talentTrees =
                    listOf(
                        talentTree(
                            nodes =
                                listOf(
                                    talentNode(
                                        "charge",
                                        rank = 1,
                                        committedRank = 0,
                                        hasPendingAllocation = true,
                                    ),
                                ),
                        ),
                    ),
            )

        assertEquals(
            listOf(
                TalentTreeNodeToneToken.TALENT_LEARNABLE,
                TalentTreeNodeToneToken.TALENT_LOCKED,
                TalentTreeNodeToneToken.TALENT_ACTIVE,
                TalentTreeNodeToneToken.TALENT_RESERVE,
            ),
            plainPanel.legend.items.filter { item -> item.kind == TalentLegendItemKind.STATE_TONE }.mapNotNull(TalentAssignLegendItem::toneToken),
        )
        assertTrue(plainPanel.legend.items.any { item -> item.kind == TalentLegendItemKind.FOCUS })
        assertFalse(plainPanel.legend.items.any { item -> item.kind == TalentLegendItemKind.PENDING_OVERLAY })
        assertTrue(pendingPanel.legend.items.any { item -> item.kind == TalentLegendItemKind.PENDING_OVERLAY })
    }

    @Test
    fun pr04ActiveSlotChoiceModalHasFiveItemsAndCancelIsFooter() {
        val modal = requireNotNull(activeSlotChoicePanel().activeSlotChoiceModal)

        assertEquals(5, modal.items.size)
        assertEquals(4, modal.items.count { item -> item.slot != null })
        assertEquals(ActiveSlotChoiceModalItemKind.RESERVE_ACTION, modal.items.last().kind)
        assertEquals("Esc cancel", modal.cancelHintText)
    }

    @Test
    fun pr04ActiveSlotChoiceModalMarksAllFourSlotsAsReplaceTargetsAndFocusedOnFirst() {
        val modal = requireNotNull(activeSlotChoicePanel().activeSlotChoiceModal)

        assertEquals(
            List(4) { ActiveSlotChoiceModalItemKind.SLOT_REPLACE_TARGET },
            modal.items.take(4).map(ActiveSlotChoiceModalItem::kind),
        )
        assertEquals(listOf(true, false, false, false), modal.items.take(4).map(ActiveSlotChoiceModalItem::focused))
    }

    @Test
    fun pr04ActiveSlotChoiceModalOnlyOpensWhenSnapshotSignalsSlotChoiceRequired() {
        val activeChoiceOverlay =
            OverlayState(
                mode = UiMode.TALENT_ASSIGN,
                modalFrames = listOf(ModalFrame(ModalFrameKind.TALENT_ASSIGN), ModalFrame(ModalFrameKind.ACTIVE_TALENT_SLOT_CHOICE)),
            )
        val panelWithoutSignal =
            panel(
                talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge")))),
                talents = activeTalents(),
                overlayState = activeChoiceOverlay,
            )
        val panelWithModal = activeSlotChoicePanel()

        assertNull(panelWithoutSignal.activeSlotChoiceModal)
        assertNotNull(panelWithModal.activeSlotChoiceModal)
    }

    @Test
    fun pr04PreviewShowsCurrentRankDetailBeforeNextRankPreview() {
        val blocks =
            requireNotNull(
                panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge", rank = 2))))).detail,
            ).blocks

        assertTrue(
            blocks.indexOfFirst { block -> block.kind == TalentDetailBlockKind.CURRENT_RANK_DETAIL } <
                blocks.indexOfFirst { block -> block.kind == TalentDetailBlockKind.NEXT_RANK_PREVIEW },
        )
    }

    @Test
    fun pr04UnlearnedTalentCurrentDetailUsesPreviewRankOne() {
        val currentBlock =
            requireNotNull(panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge", rank = 0))))).detail)
                .blocks
                .single { block -> block.kind == TalentDetailBlockKind.CURRENT_RANK_DETAIL }

        assertEquals("Current Rank Detail (Preview Rank 1)", currentBlock.primaryText)
    }

    @Test
    fun pr04PreviewBlocksFollowFixedOrdering() {
        val blocks =
            requireNotNull(
                panel(
                    talentTrees =
                        listOf(
                            talentTree(
                                nodes =
                                    listOf(
                                        talentNode(
                                            "war_cry",
                                            state = TalentNodeStateSnapshot.LOCKED,
                                            lockReasons =
                                                listOf(
                                                    TalentNodeLockReasonSnapshot(
                                                        type = TalentNodeLockReasonTypeSnapshot.PREREQUISITE_RANK,
                                                        messageKey = "ui.talent.tree.lock.prerequisite",
                                                        talentId = "charge",
                                                        talentNameKey = "talent.vanguard.charge.name",
                                                        requiredRank = 2,
                                                    ),
                                                ),
                                            prerequisites =
                                                listOf(
                                                    TalentNodePrerequisiteSnapshot(
                                                        talentId = "charge",
                                                        talentNameKey = "talent.vanguard.charge.name",
                                                        treeId = "vanguard_arms",
                                                        requiredRank = 2,
                                                        currentRank = 0,
                                                        satisfied = false,
                                                    ),
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                ).detail,
            ).blocks.map(TalentDetailBlock::kind)

        assertEquals(
            listOf(
                TalentDetailBlockKind.HERO_ICON,
                TalentDetailBlockKind.HEADER,
                TalentDetailBlockKind.RANK_AND_COST,
                TalentDetailBlockKind.PREREQUISITE,
                TalentDetailBlockKind.PREREQUISITE_FAILED,
                TalentDetailBlockKind.CURRENT_RANK_DETAIL,
                TalentDetailBlockKind.NEXT_RANK_PREVIEW,
                TalentDetailBlockKind.ACTIONS,
            ),
            blocks,
        )
    }

    @Test
    fun pr04DetailBlockPrimaryAndBodyFieldsMatchKindTable() {
        val detail = requireNotNull(panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge"))))).detail)
        val header = detail.blocks.single { block -> block.kind == TalentDetailBlockKind.HEADER }
        val rankAndCost = detail.blocks.single { block -> block.kind == TalentDetailBlockKind.RANK_AND_COST }
        val actions = detail.blocks.single { block -> block.kind == TalentDetailBlockKind.ACTIONS }

        assertEquals("Charge", header.primaryText)
        assertEquals("Learnable", header.secondaryText)
        assertEquals("Rank 0 -> 1 / 5", rankAndCost.primaryText)
        assertEquals("Cost 1 Profession Talent Point", rankAndCost.secondaryText)
        assertTrue(rankAndCost.bodyLines.isEmpty())
        assertEquals("", actions.primaryText)
        assertTrue(actions.bodyLines.any { line -> line.label == "Enter" })
    }

    @Test
    fun pr04RequirementBlockIncludesUnlockLevelWhenRequired() {
        val detail =
            requireNotNull(
                panel(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge", unlockLevel = 3))))).detail,
            )
        val prerequisite = detail.blocks.single { block -> block.kind == TalentDetailBlockKind.PREREQUISITE }

        assertTrue(prerequisite.bodyLines.any { line -> line.label == "Level requirement" && line.value == "3" })
    }

    @Test
    fun pr04CurrentRankDetailUsesStructuredMetricsInsteadOfBreakpointTeaser() {
        val currentBlock =
            requireNotNull(
                panel(
                    talentTrees =
                        listOf(
                            talentTree(
                                nodes =
                                    listOf(
                                        talentNode(
                                            "charge",
                                            range = 5,
                                            descriptionModel =
                                                DescriptionModelSnapshot(
                                                    templateKey = "talent.vanguard.charge.desc",
                                                    placeholders =
                                                        mapOf(
                                                            "minRange" to DescriptionValueSnapshot.IntValue(1),
                                                            "range" to DescriptionValueSnapshot.IntValue(5),
                                                            "radius" to DescriptionValueSnapshot.IntValue(1),
                                                            "damagePercent" to DescriptionValueSnapshot.IntValue(130),
                                                        ),
                                                    keywords = listOf("damage"),
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                ).detail,
            ).blocks.single { block -> block.kind == TalentDetailBlockKind.CURRENT_RANK_DETAIL }

        assertTrue(currentBlock.bodyLines.any { line -> line.label == "Type" && line.value == "Active" })
        assertTrue(currentBlock.bodyLines.any { line -> line.label == "Range" && line.value == "1-5 tiles" })
        assertTrue(currentBlock.bodyLines.any { line -> line.label == "Radius" && line.value == "1" })
        assertTrue(currentBlock.bodyLines.any { line -> line.label == "Cost" && line.value == "8 STA" })
        assertTrue(currentBlock.bodyLines.any { line -> line.label == "Cooldown" && line.value == "4 turns" })
        assertTrue(currentBlock.bodyLines.any { line -> line.label == "Damage" && line.value == "130% weapon damage" })
        assertTrue(currentBlock.bodyLines.any { line -> line.label == "Description" && line.value == "Can be placed in active slots" })
        assertTrue(currentBlock.bodyLines.none { line -> line.value.contains("breakpoint", ignoreCase = true) })
    }

    @Test
    fun passiveDetailShowsCurrentAndNextPassiveEffectsFromTypedSnapshot() {
        val detail =
            requireNotNull(
                panel(
                    talentTrees =
                        listOf(
                            talentTree(
                                nodes =
                                    listOf(
                                        talentNode(
                                            "unyielding",
                                            category = TalentCategory.PASSIVE,
                                            requiresTarget = false,
                                            range = 0,
                                            minRange = 0,
                                            passiveDetail =
                                                TalentPassiveDetailSnapshot(
                                                    currentLines =
                                                        listOf(
                                                            PassiveDetailLineSnapshot(
                                                                lineKind = PassiveDetailLineKindSnapshot.STAT_MODIFIER,
                                                                labelKey = "ui.talent.passive.detail.kind.stat_modifier",
                                                                valueToken =
                                                                    RenderTextTokenSnapshot(
                                                                        key = "ui.talent.passive.detail.stat_modifier",
                                                                        arguments =
                                                                            listOf(
                                                                                RenderTextArgumentSnapshot(name = "statId", valueKey = "ui.hud.hp.short"),
                                                                                RenderTextArgumentSnapshot(name = "value", value = "+10"),
                                                                            ),
                                                                    ),
                                                                diagnosticArgs = mapOf("statId" to "futureMaxHpRawId", "value" to "+10"),
                                                                sortKey = "000:004:maxHp",
                                                                tone = PassiveDetailLineToneSnapshot.SECONDARY,
                                                                diagnosticEffectKind = "StatModifier",
                                                            ),
                                                        ),
                                                    nextLines =
                                                        listOf(
                                                            PassiveDetailDeltaLineSnapshot(
                                                                lineKind = PassiveDetailLineKindSnapshot.STAT_MODIFIER,
                                                                labelKey = "ui.talent.passive.detail.kind.stat_modifier",
                                                                valueToken =
                                                                    RenderTextTokenSnapshot(
                                                                        key = "ui.talent.passive.detail.stat_modifier.delta",
                                                                        arguments =
                                                                            listOf(
                                                                                RenderTextArgumentSnapshot(name = "statId", valueKey = "ui.hud.hp.short"),
                                                                                RenderTextArgumentSnapshot(name = "value", value = "+16"),
                                                                                RenderTextArgumentSnapshot(name = "before", value = "+10"),
                                                                                RenderTextArgumentSnapshot(name = "after", value = "+16"),
                                                                            ),
                                                                    ),
                                                                diagnosticArgs =
                                                                    mapOf(
                                                                        "statId" to "futureMaxHpRawId",
                                                                        "value" to "+16",
                                                                        "before" to "+10",
                                                                        "after" to "+16",
                                                                    ),
                                                                sortKey = "000:004:maxHp",
                                                                tone = PassiveDetailLineToneSnapshot.POSITIVE,
                                                                diagnosticEffectKind = "StatModifier",
                                                            ),
                                                        ),
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                ).detail,
            )

        val current = detail.blocks.single { block -> block.kind == TalentDetailBlockKind.CURRENT_RANK_DETAIL }
        val next = detail.blocks.single { block -> block.kind == TalentDetailBlockKind.NEXT_RANK_PREVIEW }

        assertTrue(current.bodyLines.any { line -> line.label == "Stats" && line.value == "HP +10" })
        assertTrue(next.bodyLines.any { line -> line.label == "Stats" && line.value == "HP +10 -> +16" })
    }

    @Test
    fun pr04DetailHeroPrefersFullSkillIconOverReferenceCrop() {
        val detail =
            requireNotNull(
                panel(
                    talentTrees =
                        listOf(
                            talentTree(
                                nodes =
                                    listOf(
                                        talentNode(
                                            "sweeping_strike",
                                            iconKey = "talent.vanguard.sweeping_strike.icon",
                                        ),
                                    ),
                            ),
                        ),
                ).detail,
            )
        val hero = detail.blocks.single { block -> block.kind == TalentDetailBlockKind.HERO_ICON }

        assertEquals("talent.vanguard.sweeping_strike.icon", hero.iconKey)
    }

    @Test
    fun pr04NextRankPreviewListsNumericUpgradeBenefitsInBlueTone() {
        val nextPreview =
            requireNotNull(
                panel(
                    talentTrees =
                        listOf(
                            talentTree(
                                nodes =
                                    listOf(
                                        talentNode(
                                            "mana_surge",
                                            requiresTarget = false,
                                            range = 0,
                                            minRange = 0,
                                            descriptionModel =
                                                descriptionModel(
                                                    damagePercent = 100,
                                                    range = 0,
                                                    radius = 1,
                                                    statusDuration = 3,
                                                    statusMagnitude = 10,
                                                    resourceRestorePercent = 20,
                                                    resourceRestoreAmount = 4,
                                                    healPercent = 6,
                                                    knockback = 0,
                                                ),
                                            nextRankDescriptionModel =
                                                descriptionModel(
                                                    damagePercent = 115,
                                                    range = 0,
                                                    radius = 2,
                                                    statusDuration = 4,
                                                    statusMagnitude = 15,
                                                    resourceRestorePercent = 25,
                                                    resourceRestoreAmount = 6,
                                                    healPercent = 8,
                                                    knockback = 1,
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                ).detail,
            ).blocks.single { block -> block.kind == TalentDetailBlockKind.NEXT_RANK_PREVIEW }

        assertTrue(nextPreview.bodyLines.any { line -> line.label == "Damage" && line.value == "increases to 115% weapon damage" })
        assertTrue(nextPreview.bodyLines.any { line -> line.label == "Radius" && line.value == "increases to 2" })
        assertTrue(nextPreview.bodyLines.any { line -> line.label == "Duration" && line.value == "increases to 4 turns" })
        assertTrue(nextPreview.bodyLines.any { line -> line.label == "Power" && line.value == "increases to +15%" })
        assertTrue(nextPreview.bodyLines.any { line -> line.label == "Resource restore" && line.value == "increases to 25%" })
        assertTrue(nextPreview.bodyLines.any { line -> line.label == "Resource restore" && line.value == "increases to +6" })
        assertTrue(nextPreview.bodyLines.any { line -> line.label == "Healing" && line.value == "increases to 8%" })
        assertTrue(nextPreview.bodyLines.any { line -> line.label == "Knockback" && line.value == "increases to 1 tiles" })
        assertTrue(
            nextPreview.bodyLines
                .filter { line -> line.label in setOf("Damage", "Radius", "Duration", "Power", "Resource restore", "Healing", "Knockback") }
                .all { line -> line.toneToken == TalentPreviewToneToken.POSITIVE },
        )
    }

    @Test
    fun pr04RealFoundationProfessionTreesUseFormalIconsAndStructuredUpgradeBenefits(
        @TempDir tempDir: Path,
    ) {
        val upgradeLabels =
            setOf(
                "Damage",
                "Range",
                "Radius",
                "Duration",
                "Power",
                "Resource restore",
                "Healing",
                "Movement / knockback",
                "Knockback",
                "Stats",
                "Resistance",
                "On Kill",
                "Condition",
                "Recovery",
                "Trigger",
                "Terrain",
            )
        listOf("vanguard", "arcanist", "rogue", "templar").forEachIndexed { professionIndex, professionId ->
            val session =
                GameModule.newFoundationSession(
                    config = FoundationGameConfig(seed = 20260517L + professionIndex, playerProfessionId = professionId),
                    saveManager = SaveManager(tempDir.resolve(professionId)),
                )
            val snapshot = session.renderSnapshot()
            val flatNodes = snapshot.uiState.talentTrees.flatMap(TalentTreeSnapshot::nodes)
            val panel = panel(talentTrees = snapshot.uiState.talentTrees, talents = snapshot.uiState.talents)
            val formalRows = panel.sections.flatMap(TalentAssignSectionModel::rows)

            assertTrue(
                formalRows.none { row -> row.skillIconKey?.startsWith("dark.uiux.pr04.talent.") == true },
                "$professionId should use formal skill icons instead of PR04 reference crops.",
            )
            flatNodes.forEachIndexed { nodeIndex, node ->
                val detail =
                    requireNotNull(
                        panel(
                            talentTrees = snapshot.uiState.talentTrees,
                            talents = snapshot.uiState.talents,
                            overlayState = OverlayState(mode = UiMode.TALENT_ASSIGN, talentTreeSelection = nodeIndex),
                        ).detail,
                    )
                val nextPreview = detail.blocks.single { block -> block.kind == TalentDetailBlockKind.NEXT_RANK_PREVIEW }
                val positiveUpgradeLines =
                    nextPreview.bodyLines.filter { line ->
                        line.toneToken == TalentPreviewToneToken.POSITIVE && line.label in upgradeLabels
                    }

                assertTrue(
                    positiveUpgradeLines.isNotEmpty(),
                    "$professionId/${node.talentId} must expose at least one structured blue upgrade benefit.",
                )
            }
        }
    }

    @Test
    fun pr04NextRankMilestoneUsesGameplayKnockbackInsteadOfReferenceRadiusMock() {
        val nextPreview =
            requireNotNull(
                panel(
                    talentTrees =
                        listOf(
                            talentTree(
                                nodes =
                                    listOf(
                                        talentNode(
                                            "power_strike",
                                            nextBreakpointPreview =
                                                TalentBreakpointPreviewSnapshot(
                                                    atRank = 5,
                                                    model =
                                                        DescriptionModelSnapshot(
                                                            templateKey = "talent.breakpoint.displacement",
                                                            placeholders = mapOf("displacementDistance" to DescriptionValueSnapshot.IntValue(1)),
                                                        ),
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                ).detail,
            ).blocks.single { block -> block.kind == TalentDetailBlockKind.NEXT_RANK_PREVIEW }

        assertTrue(nextPreview.bodyLines.any { line -> line.label == "Milestone" && line.value == "Rank 5: knockback +1" })
        assertFalse(nextPreview.bodyLines.any { line -> line.value.contains("radius +1", ignoreCase = true) })
    }

    @Test
    fun presentAdapterRemainsDebugOnlyAndUsesPanelRows() {
        val lines =
            TalentSidebarPresenter.present(
                localizer = localizer,
                uiState = uiState(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge"))))),
                overlayState = OverlayState(mode = UiMode.TALENT_ASSIGN),
            )

        assertTrue(lines.any { line -> line.role == TalentSidebarLineRole.TREE_HEADER && line.text == "Arms  1/1" })
        assertTrue(lines.any { line -> line.role == TalentSidebarLineRole.NODE_LEARNABLE && line.text == "[+] Charge 0/5" })
    }

    private fun activeSlotChoicePanel(): TalentAssignPanelModel =
        panel(
            talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge")))),
            talents = activeTalents(),
            activeTalentSlotChoiceRequirement =
                TalentActiveSlotChoiceRequirementSnapshot(
                    candidateTalentId = "charge",
                    ownerType = TalentTreeOwnerType.PROFESSION.name,
                    treeOwnerId = "vanguard",
                ),
            overlayState =
                OverlayState(
                    mode = UiMode.TALENT_ASSIGN,
                    modalFrames = listOf(ModalFrame(ModalFrameKind.TALENT_ASSIGN), ModalFrame(ModalFrameKind.ACTIVE_TALENT_SLOT_CHOICE)),
                ),
        )

    private fun panel(
        talentTrees: List<TalentTreeSnapshot>,
        talents: List<TalentSlotSnapshot> = emptyList(),
        overlayState: OverlayState = OverlayState(mode = UiMode.TALENT_ASSIGN),
        talentPoints: Int = 2,
        raceTalentPoints: Int = 0,
        activeTalentSlotChoiceRequirement: TalentActiveSlotChoiceRequirementSnapshot? = null,
    ): TalentAssignPanelModel =
        TalentSidebarPresenter.presentPanel(
            localizer = localizer,
            uiState =
                uiState(
                    talentTrees = talentTrees,
                    talents = talents,
                    talentPoints = talentPoints,
                    raceTalentPoints = raceTalentPoints,
                    activeTalentSlotChoiceRequirement = activeTalentSlotChoiceRequirement,
                ),
            overlayState = overlayState,
        )

    private fun uiState(
        talentTrees: List<TalentTreeSnapshot>,
        talents: List<TalentSlotSnapshot> = emptyList(),
        talentPoints: Int = 2,
        raceTalentPoints: Int = 0,
        activeTalentSlotChoiceRequirement: TalentActiveSlotChoiceRequirementSnapshot? = null,
    ): RenderUiStateSnapshot =
        RenderUiStateSnapshot(
            playerStatus =
                PlayerStatusSnapshot(
                    currentHp = 24,
                    maxHp = 24,
                    currentResource = 12,
                    maxResource = 12,
                    resourceLabelKey = "ui.hud.stamina.short",
                    level = 1,
                    currentExperience = 0,
                    nextLevelRequirement = 12,
                    statPoints = 0,
                    talentPoints = talentPoints,
                    raceTalentPoints = raceTalentPoints,
                    attack = 7,
                    defense = 5,
                    accuracy = 6,
                    evasion = 4,
                    speed = 100,
                ),
            equipment = emptyList(),
            talents = talents,
            talentTrees = talentTrees,
            activeTalentSlotChoiceRequirement = activeTalentSlotChoiceRequirement,
            inventory = emptyList(),
            targetablePositions = listOf(GridPointSnapshot(0, 0)),
        )

    private fun talentTree(
        treeId: String = "vanguard_arms",
        ownerType: TalentTreeOwnerType = TalentTreeOwnerType.PROFESSION,
        treeOwnerId: String = "vanguard",
        iconKey: String? = null,
        nodes: List<TalentTreeNodeSnapshot>,
    ): TalentTreeSnapshot =
        TalentTreeSnapshot(
            treeId = treeId,
            ownerType = ownerType.name,
            treeOwnerId = treeOwnerId,
            nameKey = "talent_tree.vanguard_arms.name",
            descKey = "talent_tree.vanguard_arms.desc",
            iconKey = iconKey,
            nodes = nodes,
        )

    private fun talentNode(
        talentId: String,
        treeId: String = "vanguard_arms",
        ownerType: TalentTreeOwnerType = TalentTreeOwnerType.PROFESSION,
        treeOwnerId: String = "vanguard",
        state: TalentNodeStateSnapshot = TalentNodeStateSnapshot.LEARNABLE,
        category: TalentCategory = TalentCategory.ACTIVE,
        rank: Int = 0,
        committedRank: Int = rank,
        iconKey: String? = "icon.skill.vanguard.charge",
        resourceCost: Int = 8,
        range: Int = 3,
        minRange: Int = 1,
        requiresTarget: Boolean = true,
        descriptionModel: DescriptionModelSnapshot? = null,
        nextRankDescriptionModel: DescriptionModelSnapshot? = null,
        nextBreakpointPreview: TalentBreakpointPreviewSnapshot? = null,
        prerequisites: List<TalentNodePrerequisiteSnapshot> = emptyList(),
        lockReasons: List<TalentNodeLockReasonSnapshot> = emptyList(),
        hasPendingAllocation: Boolean = false,
        unlockLevel: Int = 1,
        passiveDetail: TalentPassiveDetailSnapshot? = null,
    ): TalentTreeNodeSnapshot =
        TalentTreeNodeSnapshot(
            talentId = talentId,
            treeId = treeId,
            ownerType = ownerType.name,
            treeOwnerId = treeOwnerId,
            nameKey = "talent.vanguard.charge.name",
            descKey = "talent.vanguard.charge.desc",
            iconKey = iconKey,
            category = category,
            state = state,
            rank = rank,
            committedRank = committedRank,
            maxRank = 5,
            unlockLevel = unlockLevel,
            resourceCost = resourceCost,
            resourceLabelKey = "ui.hud.stamina.short",
            range = range,
            minRange = minRange,
            currentCooldown = 0,
            maxCooldown = 4,
            requiresTarget = requiresTarget,
            descriptionModel = descriptionModel,
            nextRankDescriptionModel = nextRankDescriptionModel,
            nextBreakpointPreview = nextBreakpointPreview,
            passiveDetail = passiveDetail,
            prerequisites = prerequisites,
            lockReasons = lockReasons,
            hasPendingAllocation = hasPendingAllocation,
        )

    private fun descriptionModel(
        damagePercent: Int,
        range: Int,
        radius: Int,
        statusDuration: Int,
        statusMagnitude: Int,
        resourceRestorePercent: Int,
        resourceRestoreAmount: Int = 0,
        healPercent: Int = 0,
        knockback: Int = 0,
    ): DescriptionModelSnapshot =
        DescriptionModelSnapshot(
            templateKey = "talent.arcanist.mana_surge.desc",
            placeholders =
                mapOf(
                    "damagePercent" to DescriptionValueSnapshot.IntValue(damagePercent),
                    "range" to DescriptionValueSnapshot.IntValue(range),
                    "radius" to DescriptionValueSnapshot.IntValue(radius),
                    "statusDuration" to DescriptionValueSnapshot.IntValue(statusDuration),
                    "statusMagnitude" to DescriptionValueSnapshot.IntValue(statusMagnitude),
                    "resourceRestorePercent" to DescriptionValueSnapshot.IntValue(resourceRestorePercent),
                    "resourceRestoreAmount" to DescriptionValueSnapshot.IntValue(resourceRestoreAmount),
                    "healPercent" to DescriptionValueSnapshot.IntValue(healPercent),
                    "knockback" to DescriptionValueSnapshot.IntValue(knockback),
                ),
        )

    private fun activeTalents(): List<TalentSlotSnapshot> =
        (1..4).map { slot ->
            TalentSlotSnapshot(
                slot = slot,
                talentId = "active_$slot",
                nameKey = "talent.vanguard.charge.name",
                iconKey = "icon.skill.vanguard.charge",
                level = 1,
                maxLevel = 5,
                resourceCost = 8,
                resourceLabelKey = "ui.hud.stamina.short",
                range = 3,
                minRange = 1,
                currentCooldown = 0,
                maxCooldown = 4,
                requiresTarget = true,
            )
        }
}
