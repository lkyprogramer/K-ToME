package com.ktome.client.render

import com.ktome.client.render.layout.DemoShellLayout
import com.ktome.client.render.layout.DemoShellLayoutRequest
import com.ktome.client.render.layout.DemoShellLayoutSolver
import com.ktome.client.render.layout.GameShellBounds
import com.ktome.game.PLAYER_ACTIVE_TALENT_SLOT_COUNT
import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DemoShellLayoutTest {
    @Test
    fun `standard 1280 by 800 demo shell keeps map dominant with right panel and modal safety`() {
        val layout = resolve(1280, 800)

        assertProfile(
            layout = layout,
            viewportWidth = 1280f,
            viewportHeight = 800f,
            minMapWidthRatio = 0.58f,
            minMapToRightRatio = 2.05f,
            maxNavWidth = 64f,
            maxNavWidthRatio = 0.06f,
            rightPanelWidthRange = 0.23f..0.30f,
            modalWidthRange = 0.55f..0.80f,
            modalHeightRange = 0.55f..0.78f,
        )
        assertSlotStructure(layout, expectedSlotSide = 42f, expectedActionSlotSide = 68f, expectedActionColumns = 4)
    }

    @Test
    fun `minimum 1024 by 768 demo shell keeps all scaffold regions visible`() {
        val layout = resolve(1024, 768)

        assertProfile(
            layout = layout,
            viewportWidth = 1024f,
            viewportHeight = 768f,
            minMapWidthRatio = 0.55f,
            minMapToRightRatio = 1.85f,
            maxNavWidth = 64f,
            maxNavWidthRatio = 0.08f,
            rightPanelWidthRange = 0.27f..0.35f,
            modalWidthRange = 0.55f..0.85f,
            modalHeightRange = 0.55f..0.85f,
        )
        assertSlotStructure(layout, expectedSlotSide = 38f, expectedActionSlotSide = 72f, expectedActionColumns = 2)
    }

    @Test
    fun `demo aspect 1672 by 941 keeps map stage closest to the reference composition`() {
        val layout = resolve(1672, 941)

        assertProfile(
            layout = layout,
            viewportWidth = 1672f,
            viewportHeight = 941f,
            minMapWidthRatio = 0.62f,
            minMapToRightRatio = 2.05f,
            maxNavWidth = 72f,
            maxNavWidthRatio = 0.05f,
            rightPanelWidthRange = 0.24f..0.30f,
            modalWidthRange = 0.50f..0.78f,
            modalHeightRange = 0.55f..0.78f,
        )
        assertSlotStructure(layout, expectedSlotSide = 56f, expectedActionSlotSide = 96f, expectedActionColumns = 4)
    }

    @Test
    fun `demo aspect equipment sockets occupy a director grade paper doll width`() {
        val layout = resolve(1672, 941)
        val right = layout.rightPanelLayout
        val left = right.equipmentSlots.slotBounds.minOf { slot -> slot.x }
        val rightEdge = right.equipmentSlots.slotBounds.maxOf { slot -> slot.right }
        val stanceRatio = (rightEdge - left) / right.equipment.width

        assertTrue(
            stanceRatio >= 0.55f,
            "equipment sockets should occupy enough of the right panel width to read as a paper-doll loadout, not a narrow centered stack; stanceRatio=$stanceRatio",
        )
    }

    @Test
    fun `demo aspect right panel keeps equipment rack compact enough for utility density`() {
        val layout = resolve(1672, 941)
        val right = layout.rightPanelLayout
        val equipmentRatio = right.equipment.height / layout.rightPanel.height
        val utilityHeight = right.inscriptions.height + right.backpack.height + right.operationHints.height

        assertTrue(
            equipmentRatio <= 0.39f,
            "equipment rack should stay compact enough for inscriptions backpack and commands to read as dense utility UI; equipmentRatio=$equipmentRatio",
        )
        assertTrue(
            right.equipmentSlots.slotSide <= 60f,
            "demo-aspect equipment sockets should not inflate into showcase tiles; slotSide=${right.equipmentSlots.slotSide}",
        )
        assertTrue(
            utilityHeight >= right.equipment.height * 1.32f,
            "right panel should give utility sections more total vertical density than the equipment rack; utilityHeight=$utilityHeight equipmentHeight=${right.equipment.height}",
        )
    }

    @Test
    fun `large tall viewport keeps standard density instead of inflating demo aspect chrome`() {
        val layout = resolve(1980, 1280)

        assertProfile(
            layout = layout,
            viewportWidth = 1980f,
            viewportHeight = 1280f,
            minMapWidthRatio = 0.68f,
            minMapToRightRatio = 2.9f,
            maxNavWidth = 64f,
            maxNavWidthRatio = 0.04f,
            rightPanelWidthRange = 0.22f..0.26f,
            modalWidthRange = 0.50f..0.78f,
            modalHeightRange = 0.55f..0.78f,
        )
        assertSlotStructure(layout, expectedSlotSide = 52f, expectedActionSlotSide = 84f, expectedActionColumns = 4)
    }

    private fun resolve(
        width: Int,
        height: Int,
    ): DemoShellLayout =
        DemoShellLayoutSolver.resolve(
            DemoShellLayoutRequest(
                viewportWidth = width,
                viewportHeight = height,
                cellSize = 32,
            ),
        )

    private fun assertProfile(
        layout: DemoShellLayout,
        viewportWidth: Float,
        viewportHeight: Float,
        minMapWidthRatio: Float,
        minMapToRightRatio: Float,
        maxNavWidth: Float,
        maxNavWidthRatio: Float,
        rightPanelWidthRange: ClosedFloatingPointRange<Float>,
        modalWidthRange: ClosedFloatingPointRange<Float>,
        modalHeightRange: ClosedFloatingPointRange<Float>,
    ) {
        assertEquals(GameShellBounds(0f, 0f, viewportWidth, viewportHeight), layout.outerFrame)
        assertFalse(layout.navRail.overlaps(layout.mapStage))
        assertFalse(layout.mapStage.overlaps(layout.rightPanel))
        assertFalse(layout.bottomDeck.bounds.overlaps(layout.mapStage))
        assertFalse(layout.bottomDeck.bounds.overlaps(layout.navRail))
        assertFalse(layout.bottomDeck.bounds.overlaps(layout.rightPanel))
        assertEquals(layout.bottomDeck.bounds.top, layout.navRail.y)
        assertEquals(layout.mapStage.height, layout.navRail.height)
        assertEquals(viewportHeight, layout.navRail.top)
        assertEquals(0f, layout.rightPanel.y)
        assertEquals(viewportHeight, layout.rightPanel.height)
        assertEquals(0f, layout.bottomDeck.bounds.x)
        assertEquals(layout.rightPanel.x, layout.bottomDeck.bounds.width)

        assertTrue(layout.mapStage.width / viewportWidth >= minMapWidthRatio)
        assertTrue(layout.mapStage.width / layout.rightPanel.width >= minMapToRightRatio)
        assertTrue(layout.navRail.width <= maxNavWidth)
        assertTrue(layout.navRail.width / viewportWidth <= maxNavWidthRatio)
        assertTrue(layout.rightPanel.width / viewportWidth in rightPanelWidthRange)

        val modal = layout.modalSafeBounds
        val modalBounds = GameShellBounds(modal.left.toFloat(), modal.bottom.toFloat(), modal.width.toFloat(), modal.height.toFloat())
        assertTrue(modal.width / viewportWidth in modalWidthRange)
        assertTrue(modal.height / viewportHeight in modalHeightRange)
        assertTrue(modal.left >= layout.navRail.right.toInt())
        assertTrue(modal.right <= layout.rightPanel.x.toInt())
        assertTrue(modal.bottom >= layout.bottomDeck.bounds.top.toInt())
        assertFalse(modalBounds.overlaps(layout.navRail))
        assertFalse(modalBounds.overlaps(layout.rightPanel))
        assertFalse(modalBounds.overlaps(layout.bottomDeck.bounds))

        val mapCenterX = layout.mapStage.x + layout.mapStage.width / 2f
        val mapCenterY = layout.mapStage.y + layout.mapStage.height / 2f
        assertTrue(abs((modal.left + modal.right) / 2f - mapCenterX) <= 1f)
        assertTrue(abs((modal.bottom + modal.top) / 2f - mapCenterY) <= 1f)
        assertEquals(0, layout.mapContentBounds.width % 32)
        assertEquals(0, layout.mapContentBounds.height % 32)
    }

    private fun assertSlotStructure(
        layout: DemoShellLayout,
        expectedSlotSide: Float,
        expectedActionSlotSide: Float,
        expectedActionColumns: Int,
    ) {
        val right = layout.rightPanelLayout
        assertEquals(2, right.equipmentSlots.columns)
        assertEquals(5, right.equipmentSlots.rows)
        assertEquals(9, right.equipmentSlots.slotBounds.size)
        val equipmentColumnCenters = right.equipmentSlots.slotBounds.take(2).map { slot -> slot.x + slot.width / 2f }
        assertTrue(
            equipmentColumnCenters[1] - equipmentColumnCenters[0] >= right.equipmentSlots.slotSide * 1.62f,
            "equipment sockets should use a wide paper-doll stance instead of a compact two-column stack",
        )
        assertEquals(2, right.inscriptionSlots.columns)
        assertEquals(4, right.inscriptionSlots.rows)
        assertEquals(8, right.inscriptionSlots.slotBounds.size)
        assertEquals(4, right.backpackSlots.columns)
        assertEquals(2, right.backpackSlots.rows)
        assertTrue(right.backpackPager.y >= right.backpack.y)
        assertTrue(right.backpackPager.top <= right.backpack.top)
        listOf(right.equipmentSlots, right.inscriptionSlots, right.backpackSlots).forEach { grid ->
            assertTrue(grid.slotSide >= expectedSlotSide)
            grid.slotBounds.forEach { slot ->
                assertTrue(slot.width >= expectedSlotSide)
                assertTrue(slot.height >= expectedSlotSide)
                assertTrue(slot.x >= grid.bounds.x)
                assertTrue(slot.right <= grid.bounds.right)
                assertTrue(slot.y >= grid.bounds.y)
                assertTrue(slot.top <= grid.bounds.top)
            }
        }

        assertEquals(PLAYER_ACTIVE_TALENT_SLOT_COUNT, layout.bottomDeck.actionSlotBounds.size)
        assertEquals(expectedActionColumns, layout.bottomDeck.actionSlotBounds.map { slot -> slot.x }.distinct().size)
        assertTrue(layout.bottomDeck.actionDeck.right <= layout.bottomDeck.logDeck.x)
        assertTrue(layout.bottomDeck.logDeck.width >= 180f)
        layout.bottomDeck.actionSlotBounds.forEach { slot ->
            assertTrue(slot.width >= expectedActionSlotSide)
            assertTrue(slot.height >= expectedActionSlotSide)
            assertTrue(slot.x >= layout.bottomDeck.actionDeck.x)
            assertTrue(slot.right <= layout.bottomDeck.actionDeck.right)
            assertTrue(slot.y >= layout.bottomDeck.actionDeck.y)
            assertTrue(slot.top <= layout.bottomDeck.actionDeck.top)
        }
    }
}
