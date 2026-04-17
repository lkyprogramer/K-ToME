package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Phase4AggregationManifestMigrationTest {
    @Test
    fun `phase4 manifest preserves the build wiring task and artifact order`() {
        val buildScript = Files.readString(repoRoot().resolve("tools/build.gradle.kts"))

        assertTrue(buildScript.contains("phase4AggregationManifest.artifactRelativePaths"))
        assertTrue(buildScript.contains("phase4AggregationManifest.taskPaths"))
        assertFalse(buildScript.contains("ktome.phase4.aggregationManifestMode"))
    }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize().let { path ->
                if (Files.isDirectory(path.resolve("tools"))) path else path.parent
            }

}
