package com.ktome.client.screen

import com.badlogic.gdx.utils.viewport.FitViewport
import com.ktome.client.render.TileRenderer
import com.ktome.core.save.SaveManager
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FoundationViewportSupportTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun usesDirectorGradePresentationCellScaleForFoundationRuntimeShell() {
        val source = foundationGameScreenSource()
        assertTrue(source.contains("private const val cellWidth = 42f"))
        assertTrue(source.contains("private const val cellHeight = 42f"))

        val metrics = TileRenderer.layoutMetrics(mapWidth = 90, mapHeight = 56, cellWidth = 42f, cellHeight = 42f)
        val visibleColumns = metrics.shell.cellAlignedMapBounds.width / 42
        val visibleRows = metrics.shell.cellAlignedMapBounds.height / 42

        assertEquals(0, metrics.shell.cellAlignedMapBounds.width % 42)
        assertEquals(0, metrics.shell.cellAlignedMapBounds.height % 42)
        assertEquals(19, visibleColumns, "Runtime map should stop reading as a distant overview board.")
        assertEquals(13, visibleRows, "Runtime map should use a tighter aperture so the dungeon material has first-read scale.")
    }

    @Test
    fun keepsWorldSizeFixedWhenSnapshotDimensionsChange() {
        val initialSnapshot =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "shattered_outpost", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("viewport-left")),
            ).renderSnapshot()
        val largerSnapshot =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "greenwood_fringe", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("viewport-right")),
            ).renderSnapshot()

        val viewport = FitViewport(FoundationViewportSupport.worldWidth(initialSnapshot), FoundationViewportSupport.worldHeight(initialSnapshot))

        assertFalse(FoundationViewportSupport.syncViewport(viewport, largerSnapshot, 1280, 720))
        assertEquals(FoundationViewportSupport.worldWidth(initialSnapshot), viewport.worldWidth)
        assertEquals(FoundationViewportSupport.worldHeight(initialSnapshot), viewport.worldHeight)
        assertFalse(FoundationViewportSupport.syncViewport(viewport, largerSnapshot, 1280, 720))
    }

    @Test
    fun keepsViewportWorldSizeFixedAcrossResize() {
        val snapshot =
            GameModule.newFoundationSession(
                saveManager = SaveManager(tempDir.resolve("viewport-resize")),
            ).renderSnapshot()
        val viewport = FitViewport(FoundationViewportSupport.worldWidth(snapshot), FoundationViewportSupport.worldHeight(snapshot))

        assertFalse(FoundationViewportSupport.syncViewport(viewport, snapshot, 1024, 768))

        assertEquals(1280f, viewport.worldWidth)
        assertEquals(800f, viewport.worldHeight)
    }

    @Test
    fun usesShellMapBoundsInsteadOfFullMapPixels() {
        val snapshot =
            GameModule.newFoundationSession(
                config = FoundationGameConfig(seed = 20260318L, zoneId = "greenwood_fringe", playerProfessionId = "vanguard"),
                saveManager = SaveManager(tempDir.resolve("viewport-shell-map")),
            ).renderSnapshot()
        val fullMapWidth = snapshot.metadata.width * 42f

        assertTrue(fullMapWidth > FoundationViewportSupport.worldWidth(snapshot) || snapshot.metadata.height * 42f > FoundationViewportSupport.worldHeight(snapshot))
        assertEquals(1280f, FoundationViewportSupport.worldWidth(snapshot))
        assertEquals(800f, FoundationViewportSupport.worldHeight(snapshot))
    }

    @Test
    fun ownsSingleSpriteBatchLifecycleAroundTileRenderer() {
        val sourcePath = sourcePath("client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt")
        val source = java.nio.file.Files.readString(sourcePath)

        assertFalse(source.contains(".begin()"))
        assertFalse(source.contains(".end()"))
    }

    private fun foundationGameScreenSource(): String =
        java.nio.file.Files.readString(sourcePath("client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt"))

    private fun sourcePath(repoRelativePath: String): Path {
        val cwd = Path.of("").toAbsolutePath()
        return listOf(
            cwd.resolve(repoRelativePath.removePrefix("client/")),
            cwd.resolve(repoRelativePath),
        ).first(java.nio.file.Files::exists)
    }
}
