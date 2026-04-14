package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Phase4AuthorityDocConsistencyTest {
    @Test
    fun `phase4 authority docs point phase4Report at the canonical unified aggregate`() {
        val whiteBoxFramework =
            Files.readString(
                repoRoot().resolve("docs/2026-04-04-unified-white-box-verification-framework.md"),
            )
        val phase4Guide =
            Files.readString(
                repoRoot().resolve("docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md"),
            )

        assertTrue(whiteBoxFramework.contains("tools/build/reports/verification/phase4/report-phase4-summary.{json,md}"))
        assertTrue(whiteBoxFramework.contains("phase4LegacyReport"))
        assertTrue(phase4Guide.contains("tools/build/reports/verification/phase4/report-phase4-summary.{json,md}"))
        assertTrue(phase4Guide.contains("phase4LegacyReport"))
    }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize()
}
