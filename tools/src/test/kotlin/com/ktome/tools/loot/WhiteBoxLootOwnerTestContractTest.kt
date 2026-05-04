package com.ktome.tools.loot

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WhiteBoxLootOwnerTestContractTest {
    @Test
    fun `whiteBoxLoot owner tests do not execute the loot producer kernel`() {
        val repoRoot = repoRoot()
        val ownerTestSource =
            Files.readString(repoRoot.resolve("tools/src/test/kotlin/com/ktome/tools/loot/WhiteBoxLootRunnerTest.kt"))
        val cacheContractSource =
            Files.readString(repoRoot.resolve("tools/src/test/kotlin/com/ktome/tools/loot/WhiteBoxLootCacheContractTest.kt"))

        assertFalse(ownerTestSource.contains("LootBalanceLabRunner.run("))
        assertFalse(ownerTestSource.contains("LootLabKernel.execute("))
        assertTrue(cacheContractSource.contains("@Tag(\"lootVerificationCacheContract\")"))
        assertTrue(cacheContractSource.contains("LootBalanceLabRunner.run("))
    }

    private fun repoRoot(): Path {
        val configured = Path.of(System.getProperty("ktome.repo.root", ".")).toAbsolutePath().normalize()
        return if (Files.exists(configured.resolve("tools/src/test/kotlin/com/ktome/tools/loot/WhiteBoxLootRunnerTest.kt"))) {
            configured
        } else {
            configured.parent
        }
    }
}
