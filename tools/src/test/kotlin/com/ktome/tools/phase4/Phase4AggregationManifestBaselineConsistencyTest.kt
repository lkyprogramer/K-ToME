package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Phase4AggregationManifestBaselineConsistencyTest {
    @Test
    fun `phase4 owner baseline inputs stay aligned with build wiring`() {
        val buildScript = Files.readString(repoRoot().resolve("tools/build.gradle.kts"))
        val baselinePaths = stringListLiteral(buildScript = buildScript, propertyName = "phase4OwnerBaselineInputs")

        assertEquals(
            Phase4OwnerBaselineRegistry.allOwnerBaselinePaths().toList(),
            baselinePaths,
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
                """val\s+$propertyName\s*=\s*rootProject\.files\((.*?)\n\s*\)""",
                setOf(RegexOption.DOT_MATCHES_ALL),
            ).find(buildScript)?.groupValues?.get(1)
                ?: error("Missing $propertyName declaration.")
        return Regex("\"([^\"]+)\"").findAll(block).map { match -> match.groupValues[1] }.toList()
    }
}
