package com.ktome.client.screen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DesktopLauncherTitleFormatterTest {
    @Test
    fun `title omits unavailable session segments`() {
        assertEquals("K-ToME", DesktopLauncherTitleFormatter.format())
        assertEquals("K-ToME · zh-CN", DesktopLauncherTitleFormatter.format(DesktopLauncherTitleContext(localeId = "zh-CN")))
        assertEquals(
            "K-ToME · zh-CN · 20260421",
            DesktopLauncherTitleFormatter.format(DesktopLauncherTitleContext(localeId = "zh-CN", seed = 20260421L)),
        )
        assertEquals(
            "K-ToME · zh-CN · 20260421 · standard",
            DesktopLauncherTitleFormatter.format(
                DesktopLauncherTitleContext(
                    localeId = "zh-CN",
                    seed = 20260421L,
                    saveSlot = "standard",
                ),
            ),
        )
    }
}
