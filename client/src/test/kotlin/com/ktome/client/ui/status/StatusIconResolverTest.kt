package com.ktome.client.ui.status

import com.ktome.client.assets.ManifestLogSink
import com.ktome.client.assets.ManifestPrefixRule
import com.ktome.client.assets.VisualManifest
import com.ktome.client.assets.VisualManifestEntry
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatusIconResolverTest {
    @Test
    fun `null status icon uses visual fallback instead of dropping status`() {
        val fallbackMessages = mutableListOf<String>()
        val icons =
            StatusIconResolver.resolveIcons(
                visualResolver = sampleResolver(fallbackMessages::add),
                effects =
                    listOf(
                        StatusEffectRenderSnapshot(
                            typeId = "guard",
                            remainingTurns = 3,
                            iconKey = "icon.status.guard",
                        ),
                        StatusEffectRenderSnapshot(
                            typeId = "missing_status_icon",
                            remainingTurns = 2,
                            iconKey = null,
                        ),
                    ),
            )

        assertEquals(
            setOf("guard", "missing_status_icon"),
            icons.map { icon -> icon.presentation.typeId }.toSet(),
        )
        val missingIcon = icons.single { icon -> icon.presentation.typeId == "missing_status_icon" }.asset
        assertEquals("icon.status.missing_status_icon", missingIcon.requestedKey)
        assertEquals("missing_visual", missingIcon.resolvedKey)
        assertTrue(missingIcon.matchedByPrefix)
        assertTrue(missingIcon.fallbackUsed)
        assertTrue(fallbackMessages.single().contains("icon.status.missing_status_icon"))
    }

    @Test
    fun `unknown status icon uses manifest fallback without dropping status`() {
        val fallbackMessages = mutableListOf<String>()
        val icons =
            StatusIconResolver.resolveIcons(
                visualResolver = sampleResolver(fallbackMessages::add),
                effects =
                    listOf(
                        StatusEffectRenderSnapshot(
                            typeId = "unknown",
                            remainingTurns = 1,
                            iconKey = "icon.status.unknown",
                        ),
                    ),
            )

        val icon = icons.single()
        assertEquals("unknown", icon.presentation.typeId)
        assertEquals("icon.status.unknown", icon.asset.requestedKey)
        assertEquals("missing_visual", icon.asset.resolvedKey)
        assertTrue(icon.asset.matchedByPrefix)
        assertTrue(icon.asset.fallbackUsed)
        assertTrue(fallbackMessages.single().contains("icon.status.unknown"))
    }

    private fun sampleResolver(log: (String) -> Unit): VisualManifestResolver =
        VisualManifestResolver(
            manifest =
                VisualManifest(
                    manifestVersion = 1,
                    styleTag = "test-style",
                    fallbackKey = "missing_visual",
                    prefixRules = listOf(ManifestPrefixRule(prefix = "icon.status.", targetKey = "missing_visual")),
                    entries =
                        listOf(
                            VisualManifestEntry(
                                key = "missing_visual",
                                category = "debug",
                                rawOutputPath = "debug/missing_visual.png",
                                footprint = "ui",
                            ),
                            VisualManifestEntry(
                                key = "icon.status.guard",
                                category = "icon_status",
                                rawOutputPath = "dark-v1/icons/icon_status_guard.png",
                                footprint = "ui",
                            ),
                        ),
                ),
            logSink = ManifestLogSink(log),
        )
}
