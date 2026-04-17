package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Phase4AggregationManifestMigrationTest {
    @Test
    fun `phase4 manifest preserves the build wiring task and artifact order`() {
        val buildScript = Files.readString(repoRoot().resolve("tools/build.gradle.kts"))

        assertEquals(
            Phase4AggregationManifestRuntime.taskPathsInOrder(),
            stringListLiteral(buildScript = buildScript, propertyName = "phase4LegacyLiteralProducerTaskPaths"),
        )
        assertEquals(
            Phase4AggregationManifestRuntime.artifactRelativePathsInOrder(),
            stringListLiteral(buildScript = buildScript, propertyName = "phase4LegacyLiteralProducerArtifactRelativePaths"),
        )
    }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize().let { path ->
                if (Files.isDirectory(path.resolve("tools"))) path else path.parent
            }

    private fun stringListLiteral(
        buildScript: String,
        propertyName: String,
    ): List<String> {
        val block =
            Regex(
                """val\s+$propertyName\s*=\s*listOf\((.*?)\n\s*\)""",
                setOf(RegexOption.DOT_MATCHES_ALL),
            ).find(buildScript)?.groupValues?.get(1)
                ?: error("Missing $propertyName declaration.")
        return Regex("\"([^\"]+)\"").findAll(block).map { match -> match.groupValues[1] }.toList()
    }
}
