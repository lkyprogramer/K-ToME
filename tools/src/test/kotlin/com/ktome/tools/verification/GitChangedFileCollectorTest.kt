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
    fun `presentation-only snapshot diff classifier accepts item tier snapshot changes`() {
        val renderSnapshotDiff =
            """
            diff --git a/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt b/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt
            @@ -464,0 +465 @@ data class ItemRenderSnapshot(
            +    val specialTierId: String? = null,
            @@ -482 +483,11 @@ data class ItemRenderSnapshot(
            -)
            +) {
            +    init {
            +        val validSpecialTiers = setOf("UNIQUE", "ARTIFACT")
            +        require(specialTierId == null || specialTierId in validSpecialTiers) {
            +            "ItemRenderSnapshot.specialTierId must be UNIQUE or ARTIFACT when present."
            +        }
            +        require((specialTemplateId == null) == (specialTierId == null)) {
            +            "ItemRenderSnapshot requires specialTemplateId and specialTierId to be present together."
            +        }
            +    }
            +}
            """.trimIndent()
        val foundationSessionDiff =
            """
            diff --git a/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt b/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
            @@ -3898,0 +3899 @@ class FoundationGameSession internal constructor(
            +            specialTierId = item.specialTemplateId?.let(content.itemBundle::specialTemplate)?.specialTier?.name,
            """.trimIndent()

        assertTrue(
            GitChangedFileCollector.isPresentationOnlySnapshotDiff(
                VerificationImpactHints.RENDER_SNAPSHOT_PATH,
                renderSnapshotDiff,
            ),
        )
        assertTrue(
            GitChangedFileCollector.isPresentationOnlySnapshotDiff(
                VerificationImpactHints.FOUNDATION_GAME_SESSION_PATH,
                foundationSessionDiff,
            ),
        )
    }

    @Test
    fun `presentation-only snapshot diff classifier rejects unrelated snapshot changes`() {
        val diff =
            """
            diff --git a/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt b/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt
            @@ -16 +16 @@ data class RenderSnapshot(
            +    val debugRuleState: String? = null,
            """.trimIndent()

        assertEquals(
            false,
            GitChangedFileCollector.isPresentationOnlySnapshotDiff(
                VerificationImpactHints.RENDER_SNAPSHOT_PATH,
                diff,
            ),
        )
    }

    @Test
    fun `presentation-only snapshot diff classifier rejects presentation field deletion`() {
        val diff =
            """
            diff --git a/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt b/core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt
            @@ -465 +465,0 @@ data class ItemRenderSnapshot(
            -    val specialTierId: String? = null,
            """.trimIndent()

        assertEquals(
            false,
            GitChangedFileCollector.isPresentationOnlySnapshotDiff(
                VerificationImpactHints.RENDER_SNAPSHOT_PATH,
                diff,
            ),
        )
    }

    @Test
    fun `presentation-only snapshot diff classifier rejects session mapping mutation`() {
        val diff =
            """
            diff --git a/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt b/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt
            @@ -3899 +3899 @@ class FoundationGameSession internal constructor(
            -            specialTierId = oldSpecialTierId,
            +            specialTierId = item.specialTemplateId?.let(content.itemBundle::specialTemplate)?.specialTier?.name,
            """.trimIndent()

        assertEquals(
            false,
            GitChangedFileCollector.isPresentationOnlySnapshotDiff(
                VerificationImpactHints.FOUNDATION_GAME_SESSION_PATH,
                diff,
            ),
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
