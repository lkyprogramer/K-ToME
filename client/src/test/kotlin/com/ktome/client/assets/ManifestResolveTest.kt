package com.ktome.client.assets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManifestResolveTest {
    @Test
    fun `runtime manifests load from bundled resources`() {
        val assets = ClientAssetBundleLoader.load()

        assertEquals("ktome-middle-fantasy-painterly-tile-v1", assets.visualManifest.styleTag)
        assertEquals("audio.fallback.silence", assets.audioManifest.fallbackKey)
    }

    @Test
    fun `exact visual key resolves without fallback`() {
        val resolver = ClientAssetBundleLoader.load().visualResolver

        val resolved = resolver.resolve("actor.vanguard")

        assertTrue(resolver.canResolve("actor.vanguard"))
        assertEquals("actor.vanguard", resolved.resolvedKey)
        assertFalse(resolved.fallbackUsed)
        assertFalse(resolved.matchedByPrefix)
    }

    @Test
    fun `canResolve only accepts exact manifest keys`() {
        val assets = ClientAssetBundleLoader.load()

        assertFalse(assets.visualResolver.canResolve("icon.missing.example"))
        assertFalse(assets.audioResolver.canResolve("audio.missing.example"))
    }

    @Test
    fun `unknown visual icon resolves through prefix fallback`() {
        val messages = mutableListOf<String>()
        val resolver =
            VisualManifestResolver(
                manifest = VisualManifestResourceLoader.load(),
                logSink = ManifestLogSink { message -> messages += message },
            )

        val resolved = resolver.resolve("icon.missing.example")

        assertEquals("missing_visual", resolved.resolvedKey)
        assertTrue(resolved.fallbackUsed)
        assertTrue(resolved.matchedByPrefix)
        assertTrue(messages.any { message -> message.contains("icon.missing.example") })
    }

    @Test
    fun `unknown audio key resolves through fallback family`() {
        val messages = mutableListOf<String>()
        val resolver =
            AudioManifestResolver(
                manifest = AudioManifestResourceLoader.load(),
                logSink = ManifestLogSink { message -> messages += message },
            )

        val resolved = resolver.resolve("audio.missing.example")

        assertEquals("audio.fallback.silence", resolved.resolvedKey)
        assertTrue(resolved.fallbackUsed)
        assertTrue(messages.any { message -> message.contains("audio.missing.example") })
    }
}
