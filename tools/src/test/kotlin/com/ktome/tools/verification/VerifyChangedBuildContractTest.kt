package com.ktome.tools.verification

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
                    spec.inputScopes.forEach { scope -> addAll(scope.requestedTaskPaths) }
                }
            }

        assertEquals(expectedTaskPaths, actualTaskPaths.toSet())
        assertFalse(actualTaskPaths.contains(":tools:phase4ReportOnly"))
        assertFalse(actualTaskPaths.contains(":tools:phase4LegacyReport"))
        assertFalse(actualTaskPaths.contains(":tools:phase4LegacyReportOnly"))
    }

    @Test
    fun `root verifyOwner task paths stay aligned with routed phase4 owner domains`() {
        val buildScript = Files.readString(repoRoot().resolve("build.gradle.kts"))
        val actualTaskPaths = buildScript.readStringList(name = "verifyOwnerTaskPaths")
        val expectedTaskPaths =
            VerificationTaskRegistry
                .registeredImpactSpecs()
                .filter { spec -> "phase4" in spec.phaseIds }
                .flatMap { spec -> spec.ownerTaskPaths }
                .toSet()

        assertEquals(expectedTaskPaths, actualTaskPaths.toSet())
    }

    @Test
    fun `tools verifyChanged plan gate applies to every routed owner task including white box content pack`() {
        val buildScript = Files.readString(repoRoot().resolve("tools/build.gradle.kts"))

        assertTrue(buildScript.contains("""tasks.named("whiteBoxContentPack")"""))
        assertTrue(buildScript.contains("""VerifyChangedPlanGate.applyTo(this, verifyChangedTaskPathsFile, "prepareVerifyChangedPlan")"""))
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
