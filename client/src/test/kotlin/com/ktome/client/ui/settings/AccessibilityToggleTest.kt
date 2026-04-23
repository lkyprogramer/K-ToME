package com.ktome.client.ui.settings

import com.ktome.client.telegraph.TelegraphRenderer
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.OverlayShapeSnapshot
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccessibilityToggleTest {
    @Test
    fun `defaults are all off and keep color shape motion and badge`() {
        val toggle = AccessibilityToggle()

        assertFalse(toggle.highContrast)
        assertFalse(toggle.colorBlindSafe)
        assertFalse(toggle.reduceMotion)
        assertEquals(
            setOf(
                AccessibilityDistinctionMethod.COLOR,
                AccessibilityDistinctionMethod.SHAPE,
                AccessibilityDistinctionMethod.MOTION,
                AccessibilityDistinctionMethod.BADGE,
            ),
            toggle.distinctionMethods,
        )
    }

    @Test
    fun `color blind safe plus reduce motion never relies on color or motion`() {
        val toggle = AccessibilityToggle(colorBlindSafe = true, reduceMotion = true)

        assertEquals(
            setOf(AccessibilityDistinctionMethod.SHAPE, AccessibilityDistinctionMethod.BADGE),
            toggle.distinctionMethods,
        )
    }

    @Test
    fun `system properties are the startup entrypoint`() {
        val toggle =
            AccessibilityToggle.fromSystemProperties { key ->
                mapOf(
                    "ktome.ui.a11y.highContrast" to "true",
                    "ktome.ui.a11y.colorBlindSafe" to "true",
                    "ktome.ui.a11y.reduceMotion" to "true",
                )[key]
            }

        assertTrue(toggle.highContrast)
        assertTrue(toggle.colorBlindSafe)
        assertTrue(toggle.reduceMotion)
        assertEquals(0.82f, toggle.overlayAlpha(0.4f))
    }

    @Test
    fun `renderer applies high contrast accessibility state`() {
        assertEquals(0.82f, TelegraphRenderer.alpha(dangerLevel = 1, accessibility = AccessibilityToggle(highContrast = true)))
    }

    @Test
    fun `renderer emits non color risk cue when color blind safe is enabled`() {
        val row =
            TelegraphRenderer
                .tileRows(
                    localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                    overlays = listOf(testOverlay(dangerLevel = 4)),
                    accessibility = AccessibilityToggle(colorBlindSafe = true),
                ).single()

        assertTrue(row.text.contains("!!!!"))
        assertTrue(row.text.contains("Lethal"))
    }

    @Test
    fun `reduce motion keeps a stronger static overlay alpha`() {
        assertEquals(0.74f, TelegraphRenderer.alpha(dangerLevel = 1, accessibility = AccessibilityToggle(reduceMotion = true)))
        assertTrue(
            TelegraphRenderer
                .tileRows(
                    localizer = LocalizationBundle.load().translator(GameLocale.EN_US),
                    overlays = listOf(testOverlay(dangerLevel = 2)),
                    accessibility = AccessibilityToggle(reduceMotion = true),
                ).single()
                .text
                .contains("!!"),
        )
    }

    @Test
    fun `non positive danger levels do not emit risk cue badges`() {
        assertEquals(null, AccessibilityToggle(colorBlindSafe = true).riskCueBadge(0))
        assertEquals(null, AccessibilityToggle(reduceMotion = true).riskCueBadge(-1))
    }

    private fun testOverlay(dangerLevel: Int): OverlayRenderSnapshot =
        OverlayRenderSnapshot(
            id = "a11y-test",
            visualKey = "telegraph.test",
            previewTurns = 1,
            dangerLevel = dangerLevel,
            shape = OverlayShapeSnapshot.SINGLE_TILE,
            sourceAbilityId = "test_telegraph",
            cells = listOf(GridPointSnapshot(0, 0)),
        )
}
