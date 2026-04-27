package com.ktome.client.ui.talent

import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.client.ui.layout.ModalFrame
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.core.snapshot.DescriptionModelSnapshot
import com.ktome.core.snapshot.DescriptionValueSnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.PlayerStatusSnapshot
import com.ktome.core.snapshot.RenderUiStateSnapshot
import com.ktome.core.snapshot.TalentNodeStateSnapshot
import com.ktome.core.snapshot.TalentTreeNodeSnapshot
import com.ktome.core.snapshot.TalentTreeSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TalentSidebarPresenterTest {
    private val localizer = LocalizationBundle.load().translator(GameLocale.EN_US)

    @Test
    fun `node lines preserve glyph roles selection and icon keys`() {
        val lines =
            TalentSidebarPresenter.present(
                localizer = localizer,
                uiState =
                    uiState(
                        talentTrees =
                            listOf(
                                talentTree(
                                    iconKey = "icon.tree.vanguard_arms",
                                    nodes =
                                        listOf(
                                            talentNode(
                                                talentId = "locked",
                                                state = TalentNodeStateSnapshot.LOCKED,
                                                iconKey = "icon.skill.locked",
                                            ),
                                            talentNode(
                                                talentId = "learnable",
                                                state = TalentNodeStateSnapshot.LEARNABLE,
                                                iconKey = "icon.skill.learnable",
                                            ),
                                            talentNode(
                                                talentId = "reserve",
                                                state = TalentNodeStateSnapshot.LEARNED_RESERVE,
                                                rank = 1,
                                                iconKey = "icon.skill.reserve",
                                            ),
                                            talentNode(
                                                talentId = "active",
                                                state = TalentNodeStateSnapshot.LEARNED_ACTIVE,
                                                rank = 1,
                                                iconKey = "icon.skill.active",
                                            ),
                                        ),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.TALENT_ASSIGN, talentTreeSelection = 1),
            )

        val treeHeader = lines.first { line -> line.role == TalentSidebarLineRole.TREE_HEADER }
        assertEquals("Arms", treeHeader.text)
        assertEquals("icon.tree.vanguard_arms", treeHeader.iconKey)

        val nodeLines = lines.filter { line -> line.role.name.startsWith("NODE_") }
        assertEquals(
            listOf(
                TalentSidebarLineRole.NODE_LOCKED,
                TalentSidebarLineRole.NODE_LEARNABLE,
                TalentSidebarLineRole.NODE_LEARNED_RESERVE,
                TalentSidebarLineRole.NODE_LEARNED_ACTIVE,
            ),
            nodeLines.map(TalentSidebarLine::role),
        )
        assertEquals(listOf("[x]", "[+]", "[r]", "[*]"), nodeLines.map { line -> line.text.substringBefore(" ") })
        assertEquals(listOf(false, true, false, false), nodeLines.map(TalentSidebarLine::selected))
        assertEquals(
            listOf("icon.skill.locked", "icon.skill.learnable", "icon.skill.reserve", "icon.skill.active"),
            nodeLines.map(TalentSidebarLine::iconKey),
        )
    }

    @Test
    fun `active slot choice modal lines appear before tree rows`() {
        val lines =
            TalentSidebarPresenter.present(
                localizer = localizer,
                uiState = uiState(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge"))))),
                overlayState =
                    OverlayState(
                        mode = UiMode.TALENT_ASSIGN,
                        modalFrames = listOf(ModalFrame(ModalFrameKind.TALENT_ASSIGN), ModalFrame(ModalFrameKind.ACTIVE_TALENT_SLOT_CHOICE)),
                    ),
            )

        val modalLines = lines.dropWhile { line -> line.role == TalentSidebarLineRole.POINTS }.take(4)
        assertEquals(
            listOf(
                "Active slot choice required",
                "The new active talent can replace a slot or stay in reserve before points are spent.",
                "1-4 Replace an active slot",
                "R Keep learned talent in reserve, Esc cancel",
            ),
            modalLines.map(TalentSidebarLine::text),
        )
        assertEquals(
            listOf(
                TalentSidebarLineRole.ACTION_TITLE,
                TalentSidebarLineRole.FOOTER,
                TalentSidebarLineRole.ACTION_HINT,
                TalentSidebarLineRole.ACTION_HINT,
            ),
            modalLines.map(TalentSidebarLine::role),
        )
        assertEquals(TalentSidebarLineRole.TREE_HEADER, lines[lines.indexOf(modalLines.last()) + 1].role)
    }

    @Test
    fun `expanded preview includes description lines and usage summary`() {
        val lines =
            TalentSidebarPresenter.present(
                localizer = localizer,
                uiState =
                    uiState(
                        talentTrees =
                            listOf(
                                talentTree(
                                    nodes =
                                        listOf(
                                            talentNode(
                                                talentId = "charge",
                                                resourceCost = 10,
                                                range = 5,
                                                minRange = 1,
                                                requiresTarget = true,
                                                descriptionModel =
                                                    DescriptionModelSnapshot(
                                                        templateKey = "talent.vanguard.charge.desc",
                                                        placeholders =
                                                            mapOf(
                                                                "minRange" to DescriptionValueSnapshot.IntValue(1),
                                                                "range" to DescriptionValueSnapshot.IntValue(5),
                                                                "damagePercent" to DescriptionValueSnapshot.IntValue(130),
                                                            ),
                                                        keywords = listOf("damage"),
                                                    ),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                overlayState = OverlayState(mode = UiMode.TALENT_ASSIGN, talentTreeSelection = 0, talentTreePreviewExpanded = true),
            )

        assertTrue(lines.any { line -> line.role == TalentSidebarLineRole.DESCRIPTION_PRIMARY && line.text.contains("Rush a foe from 1 to 5 tiles away") })
        assertTrue(lines.any { line -> line.role == TalentSidebarLineRole.DESCRIPTION_PRIMARY && line.text == "10 STA · AIM 5" })
    }

    @Test
    fun `collapsed preview shows collapsed footer line`() {
        val lines =
            TalentSidebarPresenter.present(
                localizer = localizer,
                uiState = uiState(talentTrees = listOf(talentTree(nodes = listOf(talentNode("charge"))))),
                overlayState = OverlayState(mode = UiMode.TALENT_ASSIGN, talentTreeSelection = 0, talentTreePreviewExpanded = false),
            )

        assertTrue(
            lines.any { line ->
                line.role == TalentSidebarLineRole.FOOTER &&
                    line.text == "Preview collapsed. Press P to expand."
            },
        )
    }

    private fun uiState(talentTrees: List<TalentTreeSnapshot>): RenderUiStateSnapshot =
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
                    talentPoints = 2,
                    attack = 7,
                    defense = 5,
                    accuracy = 6,
                    evasion = 4,
                    speed = 100,
                ),
            equipment = emptyList(),
            talents = emptyList(),
            talentTrees = talentTrees,
            inventory = emptyList(),
            targetablePositions = listOf(GridPointSnapshot(0, 0)),
        )

    private fun talentTree(
        iconKey: String? = null,
        nodes: List<TalentTreeNodeSnapshot>,
    ): TalentTreeSnapshot =
        TalentTreeSnapshot(
            treeId = "vanguard_arms",
            treeOwnerId = "vanguard",
            nameKey = "talent_tree.vanguard_arms.name",
            descKey = "talent_tree.vanguard_arms.desc",
            iconKey = iconKey,
            nodes = nodes,
        )

    private fun talentNode(
        talentId: String,
        state: TalentNodeStateSnapshot = TalentNodeStateSnapshot.LEARNABLE,
        rank: Int = 0,
        iconKey: String? = "icon.skill.vanguard.charge",
        resourceCost: Int = 8,
        range: Int = 3,
        minRange: Int = 1,
        requiresTarget: Boolean = true,
        descriptionModel: DescriptionModelSnapshot? = null,
    ): TalentTreeNodeSnapshot =
        TalentTreeNodeSnapshot(
            talentId = talentId,
            treeId = "vanguard_arms",
            treeOwnerId = "vanguard",
            nameKey = "talent.vanguard.charge.name",
            descKey = "talent.vanguard.charge.desc",
            iconKey = iconKey,
            state = state,
            rank = rank,
            maxRank = 5,
            unlockLevel = 1,
            resourceCost = resourceCost,
            resourceLabelKey = "ui.hud.stamina.short",
            range = range,
            minRange = minRange,
            currentCooldown = 0,
            maxCooldown = 4,
            requiresTarget = requiresTarget,
            descriptionModel = descriptionModel,
        )
}
