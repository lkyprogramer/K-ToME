package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class Phase4ReportFixtureTestSupportTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `prepare phase4 repo fixture copies the checked-in hermetic snapshot marker`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val markerPath = fixtureRepoRoot.resolve(".phase4-report-fixture-id")

        assertTrue(Files.exists(markerPath))
        assertEquals("phase4-report-fixture-v1", Files.readString(markerPath).trim())
    }
}
