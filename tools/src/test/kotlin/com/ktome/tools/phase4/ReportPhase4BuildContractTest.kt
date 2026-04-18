package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReportPhase4BuildContractTest {
    @Test
    fun `phase4 aggregate helper keeps reportPhase4 artifact only semantics`() {
        val buildScript = Files.readString(repoRoot().resolve("tools/build.gradle.kts"))
        val helperInvocation = helperInvocation(buildScript, taskName = "reportPhase4")

        assertTrue(buildScript.contains("fun registerPhase4AggregateTask("))
        assertTrue(buildScript.contains("Phase4AggregationManifestValueSource"))
        assertTrue(buildScript.contains("Phase4TaskPathResolver.resolve"))
        assertTrue(buildScript.contains("val phase4AggregateProducerArtifactRelativePaths ="))
        assertTrue(buildScript.contains("phase4AggregationManifest.artifactRelativePaths"))
        assertTrue(buildScript.contains("val phase4AggregateProducerTaskPaths ="))
        assertTrue(buildScript.contains("phase4AggregationManifest.taskPaths"))
        assertFalse(buildScript.contains("ktome.phase4.aggregationManifestMode"))
        assertFalse(buildScript.contains("phase4LegacyLiteralProducerArtifactRelativePaths"))
        assertFalse(buildScript.contains("phase4LegacyLiteralProducerTaskPaths"))
        assertTrue(buildScript.contains("includeTags(includeTag)"))
        assertTrue(buildScript.contains("if (includeTag == \"reportPhase4\")"))
        assertTrue(buildScript.contains("excludeTags(\"phase4AggregationInput\")"))
        assertTrue(buildScript.contains("excludeTags(\"reportPhase4Fixture\")"))
        assertTrue(buildScript.contains("systemProperty(\"ktome.repo.root\", rootProject.projectDir.absolutePath)"))
        assertTrue(buildScript.contains("inputs.files(producerInputs)"))
        assertTrue(buildScript.contains("dependsOn(additionalDependsOn)"))
        assertTrue(buildScript.contains("mustRunAfter(producerTasks)"))
        assertTrue(helperInvocation.contains("includeTag = \"reportPhase4\""))
        assertTrue(helperInvocation.contains("outputDir = unifiedPhase4ReportDir"))
        assertTrue(helperInvocation.contains("producerInputs = phase4AggregateProducerInputs"))
        assertTrue(helperInvocation.contains("producerTasks = phase4AggregateProducerTasks"))
        assertTrue(helperInvocation.contains("aggregateReportDir = unifiedPhase4ReportDir"))
        assertTrue(helperInvocation.contains("legacyReportDir = legacyPhase4ReportDir"))
        assertTrue(helperInvocation.contains("additionalDependsOn = listOf(tasks.named(\"phase4LegacyReport\"))"))
        assertTrue(helperInvocation.contains("additionalMustRunAfter = listOf(tasks.named(\"phase4LegacyReportOnly\"), soloClearLabProducerTask)"))
        assertTrue(helperInvocation.contains("additionalInputs = files(legacyPhase4SummaryInput)"))
        assertTrue(helperInvocation.contains("compareLegacy = true"))
        assertFalse(helperInvocation.contains("phase4ReportOnly"))
        assertFalse(helperInvocation.contains("additionalDependsOn = listOf(tasks.named(\"phase4LegacyReportOnly\"))"))
    }

    @Test
    fun `phase4Report task is cut over to the unified aggregate via the shared helper`() {
        val buildScript = Files.readString(repoRoot().resolve("tools/build.gradle.kts"))
        val helperInvocation = helperInvocation(buildScript, taskName = "phase4Report")

        assertTrue(helperInvocation.contains("includeTag = \"reportPhase4\""))
        assertTrue(helperInvocation.contains("outputDir = unifiedPhase4ReportDir"))
        assertTrue(helperInvocation.contains("producerInputs = phase4AggregateProducerInputs"))
        assertTrue(helperInvocation.contains("producerTasks = phase4AggregateProducerTasks"))
        assertTrue(helperInvocation.contains("additionalMustRunAfter = listOf(soloClearLabProducerTask)"))
        assertTrue(helperInvocation.contains("aggregateReportDir = unifiedPhase4ReportDir"))
        assertTrue(helperInvocation.contains("compareLegacy = false"))
        assertFalse(helperInvocation.contains("legacyReportDir = legacyPhase4ReportDir"))
        assertFalse(helperInvocation.contains("includeTag = \"phase4LegacyReport\""))
    }

    @Test
    fun `phase4LegacyReport task remains available as isolated manual fallback via the shared helper`() {
        val buildScript = Files.readString(repoRoot().resolve("tools/build.gradle.kts"))
        val helperInvocation = helperInvocation(buildScript, taskName = "phase4LegacyReport")

        assertTrue(helperInvocation.contains("includeTag = \"phase4LegacyReport\""))
        assertTrue(helperInvocation.contains("outputDir = legacyPhase4ReportDir"))
        assertTrue(helperInvocation.contains("producerInputs = phase4AggregateProducerInputs"))
        assertTrue(helperInvocation.contains("producerTasks = phase4AggregateProducerTasks"))
        assertTrue(helperInvocation.contains("legacyReportDir = legacyPhase4ReportDir"))
        assertFalse(helperInvocation.contains("aggregateReportDir = unifiedPhase4ReportDir"))
        assertFalse(helperInvocation.contains("compareLegacy = true"))
    }

    @Test
    fun `phase4 domain artifact registry no longer routes through Phase4ReportRunner readers`() {
        val registrySource =
            Files.readString(
                repoRoot().resolve("tools/src/main/kotlin/com/ktome/tools/phase4/Phase4DomainArtifactRegistry.kt"),
            )

        assertFalse(registrySource.contains("Phase4ReportRunner::"))
        assertFalse(registrySource.contains("private data class Phase4TaskDescriptor"))
        assertFalse(registrySource.contains("relativeSourcePath ="))
        assertFalse(registrySource.contains("aggregationOnly = true"))
        assertTrue(registrySource.contains("Phase4AggregationManifestRuntime.tasks()"))
        assertTrue(registrySource.contains("taskReadersById"))
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

    private fun helperInvocation(
        buildScript: String,
        taskName: String,
    ): String =
        Regex(
            """registerPhase4AggregateTask\(\s*name\s*=\s*"$taskName",(.*?)\n\)""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        ).find(buildScript)?.value
            ?: error("Missing registerPhase4AggregateTask invocation for '$taskName'.")
}
