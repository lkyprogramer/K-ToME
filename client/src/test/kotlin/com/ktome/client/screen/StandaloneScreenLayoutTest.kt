package com.ktome.client.screen

import com.ktome.client.ui.token.UiDesignTokens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StandaloneScreenLayoutTest {
    @Test
    fun `main menu standalone layout reserves action stack secondary panel disabled detail and footer`() {
        val layout = DarkStandaloneScreenLayout.mainMenu()

        assertTrue(layout.primaryActionStack.right <= layout.secondaryPanel.x)
        assertTrue(layout.disabledDetailArea.y >= layout.footerHelp.top)
        assertTrue(layout.disabledDetailArea.top <= layout.primaryActionStack.y)
        assertTrue(layout.secondaryPanel.top <= layout.header.y)
        assertDoesNotOverlap(layout.primaryActionStack, layout.secondaryPanel)
        assertDoesNotOverlap(layout.disabledDetailArea, layout.footerHelp)
    }

    @Test
    fun `validation setup layout keeps list and active pack summary above footer`() {
        val layout = DarkStandaloneScreenLayout.validationSetup()
        val entryCount = 15
        val placements = DarkStandaloneScreenLayout.validationEntryPlacements(entryCount)
        val columnCount = placements.map { placement -> placement.x }.distinct().size

        assertEquals(entryCount, placements.size)
        assertEquals(2, columnCount)
        assertTrue(layout.primaryActionStack.y >= layout.footerHelp.top)
        assertTrue(layout.primaryActionStack.top <= layout.disabledDetailArea.y)
        assertTrue(layout.disabledDetailArea.top <= layout.secondaryPanel.y)
        assertTrue(DarkStandaloneScreenLayout.validationFooterControlsBaselineY < layout.footerHelp.top)
        assertTrue(DarkStandaloneScreenLayout.validationFooterControlsBaselineY > layout.footerHelp.y)
        placements.forEach { placement ->
            assertTrue(placement.x >= layout.primaryActionStack.x)
            assertTrue(placement.x < layout.primaryActionStack.right)
            assertTrue(placement.baselineY >= layout.primaryActionStack.y + DarkStandaloneScreenLayout.validationEntryMinStepY)
            assertTrue(placement.baselineY <= layout.primaryActionStack.top)
        }
        placements
            .groupBy { placement -> placement.x }
            .values
            .forEach { columnPlacements ->
                columnPlacements.zipWithNext().forEach { (current, next) ->
                    assertTrue(current.baselineY - next.baselineY >= DarkStandaloneScreenLayout.validationEntryMinStepY)
                }
            }
        assertDoesNotOverlap(layout.primaryActionStack, layout.disabledDetailArea)
        assertDoesNotOverlap(layout.disabledDetailArea, layout.secondaryPanel)
        assertDoesNotOverlap(layout.primaryActionStack, layout.footerHelp)
    }

    @Test
    fun `outcome body baselines are capped inside the primary action stack`() {
        val layout = DarkStandaloneScreenLayout.outcome()
        val baselines = DarkStandaloneScreenLayout.outcomeBodyLineBaselines(lineCount = 20)

        assertEquals(DarkStandaloneScreenLayout.outcomeBodyLineCapacity(), baselines.size)
        assertTrue(baselines.isNotEmpty())
        baselines.forEach { baseline ->
            assertTrue(baseline <= layout.primaryActionStack.top)
            assertTrue(baseline >= layout.primaryActionStack.y + DarkStandaloneScreenLayout.outcomeBodyStepY)
        }
        baselines.zipWithNext().forEach { (current, next) ->
            assertEquals(DarkStandaloneScreenLayout.outcomeBodyStepY, current - next)
        }
    }

    @Test
    fun `talent tones match the art bible contract consumed by later talent UI`() {
        val talent = UiDesignTokens.color.talent

        assertEquals("#59616C", talent.locked.hexString())
        assertEquals("#1CB7C8", talent.learnable.hexString())
        assertEquals("#D99A2B", talent.reserve.hexString())
        assertEquals("#52C989", talent.active.hexString())
    }

    private fun assertDoesNotOverlap(
        first: ScreenPanelBounds,
        second: ScreenPanelBounds,
    ) {
        assertTrue(
            first.x >= second.right ||
                first.right <= second.x ||
                first.y >= second.top ||
                first.top <= second.y,
            "Expected $first not to overlap $second.",
        )
    }
}
