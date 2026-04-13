package com.ktome.tools.verification

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GitChangedFileCollectorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `name-status parser keeps deletions and both sides of renames`() {
        val paths =
            GitChangedFileCollector.parseNameStatusPaths(
                """
                D	game/src/main/resources/data/loot/old-index.yaml
                R100	game/src/main/resources/data/hidden/old.yaml	game/src/main/resources/data/hidden/new.yaml
                M	tools/src/main/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzer.kt
                """.trimIndent(),
            )

        assertEquals(
            listOf(
                "game/src/main/resources/data/loot/old-index.yaml",
                "game/src/main/resources/data/hidden/old.yaml",
                "game/src/main/resources/data/hidden/new.yaml",
                "tools/src/main/kotlin/com/ktome/tools/verification/VerificationImpactAnalyzer.kt",
            ),
            paths,
        )
    }

    @Test
    fun `collector falls back to existing local branch when preferred base ref is missing`() {
        runGit(tempDir, "init")
        runGit(tempDir, "checkout", "-b", "main")
        runGit(tempDir, "config", "user.name", "Codex")
        runGit(tempDir, "config", "user.email", "codex@example.com")
        Files.writeString(tempDir.resolve("tracked.txt"), "v1")
        runGit(tempDir, "add", "tracked.txt")
        runGit(tempDir, "commit", "-m", "init")
        Files.writeString(tempDir.resolve("tracked.txt"), "v2")

        val collected = GitChangedFileCollector.collect(tempDir, preferredBaseRef = "refs/does-not-exist")

        assertTrue(collected.notes.any { note -> note.contains("using 'main' instead") })
        assertTrue(collected.changedFiles.contains("tracked.txt"))
    }

    private fun runGit(
        workdir: Path,
        vararg args: String,
    ) {
        val process =
            ProcessBuilder(listOf("git", *args))
                .directory(workdir.toFile())
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().use { reader -> reader.readText() }
        require(process.waitFor() == 0) {
            "git ${args.joinToString(" ")} failed: $output"
        }
    }
}
