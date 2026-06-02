package com.ktome.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DesktopLauncherTest {
    @Test
    fun `window spec defaults to desktop launcher baseline`() {
        assertEquals(
            DesktopLauncherWindowSpec(width = 1280, height = 800),
            desktopLauncherWindowSpec { null },
        )
    }

    @Test
    fun `window spec accepts whitebox materialization properties`() {
        assertEquals(
            DesktopLauncherWindowSpec(width = 1672, height = 941),
            desktopLauncherWindowSpec { key ->
                mapOf(
                    "ktome.window.width" to "1672",
                    "ktome.window.height" to "941",
                )[key]
            },
        )
    }

    @Test
    fun `window spec ignores invalid property values`() {
        assertEquals(
            DesktopLauncherWindowSpec(width = 1280, height = 800),
            desktopLauncherWindowSpec { key ->
                mapOf(
                    "ktome.window.width" to "-1",
                    "ktome.window.height" to "bad-height",
                )[key]
            },
        )
    }
}
