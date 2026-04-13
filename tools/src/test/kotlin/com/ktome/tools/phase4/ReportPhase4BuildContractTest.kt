package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ReportPhase4BuildContractTest {
    @Test
    fun `reportPhase4 task stays artifact only and does not depend on phase4Report`() {
        val lines = Files.readAllLines(repoRoot().resolve("tools/build.gradle.kts"))
        val startIndex = lines.indexOfFirst { line -> line.contains("tasks.register<Test>(\"reportPhase4\")") }
        check(startIndex >= 0) { "Missing reportPhase4 task registration in tools/build.gradle.kts." }
        val endIndex =
            (startIndex + 1 until lines.size)
                .firstOrNull { index -> lines[index].contains("tasks.register<Test>(\"reportPhase4Only\")") }
                ?: error("Missing reportPhase4Only task registration after reportPhase4.")
        check(endIndex > startIndex) { "Missing reportPhase4Only task registration after reportPhase4." }

        val reportPhase4Block = lines.subList(startIndex, endIndex).joinToString(separator = "\n")

        assertFalse(reportPhase4Block.contains("dependsOn(\"phase4Report\")"))
        assertFalse(reportPhase4Block.contains("dependsOn(\"phase4ReportOnly\")"))
    }

    @Test
    fun `phase4Report task stays artifact only and does not depend on producer tasks`() {
        val lines = Files.readAllLines(repoRoot().resolve("tools/build.gradle.kts"))
        val startIndex = lines.indexOfFirst { line -> line.contains("tasks.register<Test>(\"phase4Report\")") }
        check(startIndex >= 0) { "Missing phase4Report task registration in tools/build.gradle.kts." }
        val endIndex =
            (startIndex + 1 until lines.size)
                .firstOrNull { index -> lines[index].contains("tasks.register<Test>(\"phase4ReportOnly\")") }
                ?: error("Missing phase4ReportOnly task registration after phase4Report.")
        check(endIndex > startIndex) { "Missing phase4ReportOnly task registration after phase4Report." }

        val phase4ReportBlock = lines.subList(startIndex, endIndex).joinToString(separator = "\n")

        assertFalse(phase4ReportBlock.contains("dependsOn("))
    }

    @Test
    fun `phase4 domain artifact registry no longer routes through Phase4ReportRunner readers`() {
        val registrySource =
            Files.readString(
                repoRoot().resolve("tools/src/main/kotlin/com/ktome/tools/phase4/Phase4DomainArtifactRegistry.kt"),
            )

        assertFalse(registrySource.contains("Phase4ReportRunner::"))
    }

    @Test
    fun `tools build keeps game runtime classpath lazy instead of globally evaluating game`() {
        val buildScript = Files.readString(repoRoot().resolve("tools/build.gradle.kts"))

        assertFalse(buildScript.contains("evaluationDependsOn(\":game\")"))
    }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize()
}
