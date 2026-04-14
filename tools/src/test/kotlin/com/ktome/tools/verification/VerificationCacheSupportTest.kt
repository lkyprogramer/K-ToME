package com.ktome.tools.verification

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class VerificationCacheSupportTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `sha256Files invalidates when nested directory content changes`() {
        val root = tempDir.resolve("verification-cache-inputs")
        val nestedDir = root.resolve("nested")
        val payloadFile = nestedDir.resolve("payload.txt")
        Files.createDirectories(nestedDir)
        Files.writeString(payloadFile, "alpha")

        val before = VerificationCacheSupport.sha256Files(listOf(root))

        Files.writeString(payloadFile, "beta")

        val after = VerificationCacheSupport.sha256Files(listOf(root))

        assertNotEquals(before, after)
    }

    @Test
    fun `mergeJsonlFiles preserves source order and drops blank lines`() {
        val shardA = tempDir.resolve("a.jsonl")
        val shardB = tempDir.resolve("b.jsonl")
        val merged = tempDir.resolve("merged").resolve("payload.jsonl")
        Files.writeString(shardA, "{\"id\":1}\n\n{\"id\":2}\n")
        Files.writeString(shardB, "{\"id\":3}\n")

        VerificationCacheSupport.mergeJsonlFiles(
            targetPath = merged,
            sourcePaths = listOf(shardA, shardB),
        )

        assertEquals(listOf("{\"id\":1}", "{\"id\":2}", "{\"id\":3}"), Files.readAllLines(merged))
    }

    @Test
    fun `mergeJsonlFiles fails fast when any shard file is missing`() {
        val shardA = tempDir.resolve("a.jsonl")
        val missing = tempDir.resolve("missing.jsonl")
        val merged = tempDir.resolve("merged").resolve("payload.jsonl")
        Files.writeString(shardA, "{\"id\":1}\n")

        assertThrows(IllegalArgumentException::class.java) {
            VerificationCacheSupport.mergeJsonlFiles(
                targetPath = merged,
                sourcePaths = listOf(shardA, missing),
            )
        }
    }
}
