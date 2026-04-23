package com.ktome.client.ui.inspect

import com.ktome.client.ui.card.ModalCardAction
import com.ktome.client.ui.card.ModalCardModel
import com.ktome.client.ui.card.ModalCardReturnPolicy
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ExplainPaneModelTest {
    @Test
    fun `explain pane wraps modal card instead of raw renderer text`() {
        val card =
            ModalCardModel(
                stableKey = "inspect:test",
                title = RenderTextTokenSnapshot("talent.vanguard.charge.name"),
                iconKey = null,
                summary = null,
                detailLines = listOf(RenderTextTokenSnapshot("talent.vanguard.charge.desc")),
                primaryAction = ModalCardAction.CLOSE,
                secondaryAction = ModalCardAction.CLOSE,
                returnPolicy = ModalCardReturnPolicy.READ_ONLY_POPPABLE,
            )

        val pane = ExplainPaneModel.fromCard(card = card, keywordIds = listOf("damage", "stun"))

        assertEquals(card, pane.card)
        assertEquals(listOf("keyword.damage.name", "keyword.stun.name"), pane.keywordChips.map { token -> token.key })
    }

    @Test
    fun `unknown keyword chips fail fast`() {
        val card = ExplainPaneModel.empty().card

        assertThrows(IllegalArgumentException::class.java) {
            ExplainPaneModel.fromCard(card = card, keywordIds = listOf("missing_keyword"))
        }
    }

    @Test
    fun `inspect explain pane prioritizes active telegraph over actor occupying the same cell`() {
        val pane =
            ExplainPaneModel.fromInspectSurface(
                actor =
                    ActorRenderSnapshot(
                        entityId = 7,
                        x = 1,
                        y = 2,
                        visualKey = "actor.goblin",
                        nameKey = "actor.goblin.name",
                        isPlayer = false,
                    ),
                item = null,
                prop = null,
                terrainOverride = null,
                overlay =
                    OverlayRenderSnapshot(
                        id = "boss-slam",
                        visualKey = "telegraph.boss_slam",
                        previewTurns = 1,
                        dangerLevel = 4,
                        shape = OverlayShapeSnapshot.SINGLE_TILE,
                        sourceAbilityId = "boss_slam",
                        cells = listOf(GridPointSnapshot(1, 2)),
                        warningMessage = RenderTextTokenSnapshot("boss.warning.slam"),
                    ),
            )

        assertEquals("inspect:telegraph:boss-slam", pane.card.stableKey)
        assertEquals("boss.warning.slam", pane.card.title.key)
    }

    @Test
    fun `inspect actor explain pane keeps status presentation badges in modal card tokens`() {
        val pane =
            ExplainPaneModel.fromInspectSurface(
                actor =
                    ActorRenderSnapshot(
                        entityId = 9,
                        x = 1,
                        y = 2,
                        visualKey = "actor.goblin",
                        nameKey = "actor.goblin.name",
                        isPlayer = false,
                        statusEffects =
                            listOf(
                                StatusEffectRenderSnapshot(
                                    typeId = "guard",
                                    remainingTurns = 4,
                                    nameKey = "status.guard",
                                    stackCount = 3,
                                    stackCap = 5,
                                    category = StatusEffectCategorySnapshot.BUFF,
                                ),
                            ),
                    ),
                item = null,
                prop = null,
                terrainOverride = null,
                overlay = null,
            )

        val statusLine = pane.card.detailLines.single()
        assertEquals("ui.status.effect.line", statusLine.key)
        assertEquals("status.guard", statusLine.arguments.single { argument -> argument.name == "name" }.valueKey)
        assertEquals("3/5 4t", statusLine.arguments.single { argument -> argument.name == "badge" }.value)
        assertEquals(listOf("keyword.guard.name"), pane.keywordChips.map { token -> token.key })
    }
}
