package com.ktome.tools.verification

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class VerifyChangedBuildContractTest {
    @Test
    fun `root verifyChanged task paths stay aligned with routed verification domains`() {
        val buildScript = Files.readString(repoRoot().resolve("build.gradle.kts"))
        val actualTaskPaths = buildScript.readStringList(name = "verifyChangedTaskPaths")
        val expectedTaskPaths =
            buildSet {
                add(":tools:prepareVerifyChangedPlan")
                VerificationTaskRegistry.registeredImpactSpecs().forEach { spec ->
                    addAll(spec.preflightTaskPaths)
                    addAll(spec.ownerTaskPaths)
                }
            }

        assertEquals(expectedTaskPaths, actualTaskPaths.toSet())
        assertFalse(actualTaskPaths.contains(":tools:phase4ReportOnly"))
    }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize()

    private fun String.readStringList(name: String): List<String> {
        val match =
            Regex("""val\s+$name\s*=\s*listOf\((.*?)\)""", setOf(RegexOption.DOT_MATCHES_ALL))
                .find(this)
                ?: error("Missing list declaration '$name' in root build script.")
        return Regex("\"([^\"]+)\"")
            .findAll(match.groupValues[1])
            .map { result -> result.groupValues[1] }
            .toList()
    }
}
