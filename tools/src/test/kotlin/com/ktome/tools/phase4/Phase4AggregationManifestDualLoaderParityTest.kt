package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

class Phase4AggregationManifestDualLoaderParityTest {
    @Test
    fun `runtime manifest loader preserves raw yaml task tuples and order`() {
        val yamlText = Files.readString(repoRoot().resolve("tools/src/main/resources/phase4/aggregation-manifest.yaml"))
        val runtimeTasks =
            Phase4AggregationManifestRuntime.parse(
                yamlText = yamlText,
                sourceDescription = "inline-manifest",
            ).tasks.map { task ->
                RawTaskTuple(
                    taskId = task.taskId,
                    taskPath = task.taskPath,
                    artifactRelativePath = task.artifactRelativePath,
                    role = task.role.name,
                )
            }

        assertEquals(rawYamlTasks(yamlText), runtimeTasks)
    }

    @Test
    fun `runtime manifest loader rejects unknown task fields`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                Phase4AggregationManifestRuntime.parse(
                    yamlText =
                        """
                        schemaVersion: phase4-aggregation-manifest-v1
                        phaseId: P4
                        tasks:
                          - taskId: mapgenSmoke
                            taskPath: :tools:mapgenSmoke
                            artifactRelativePath: tools/build/reports/phase4/mapgen/mapgen-smoke-summary.json
                            role: AGGREGATION_ONLY
                            unexpected: true
                        """.trimIndent(),
                    sourceDescription = "inline-manifest",
                )
            }

        assertEquals(true, exception.message.orEmpty().contains("unexpected"))
    }

    @Test
    fun `runtime manifest loader rejects duplicate artifact paths`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                Phase4AggregationManifestRuntime.parse(
                    yamlText =
                        """
                        schemaVersion: phase4-aggregation-manifest-v1
                        phaseId: P4
                        tasks:
                          - taskId: mapgenSmoke
                            taskPath: :tools:mapgenSmoke
                            artifactRelativePath: tools/build/reports/phase4/mapgen/mapgen-smoke-summary.json
                            role: AGGREGATION_ONLY
                          - taskId: solvabilityHarness
                            taskPath: :tools:solvabilityHarness
                            artifactRelativePath: tools/build/reports/phase4/mapgen/mapgen-smoke-summary.json
                            role: AGGREGATION_ONLY
                        """.trimIndent(),
                    sourceDescription = "inline-manifest",
                )
            }

        assertEquals(true, exception.message.orEmpty().contains("Duplicate Phase 4 aggregation manifest artifactRelativePath"))
    }

    private fun rawYamlTasks(yamlText: String): List<RawTaskTuple> {
        val root = Yaml().load<Map<String, Any?>>(yamlText)
        @Suppress("UNCHECKED_CAST")
        val tasks = root.getValue("tasks") as List<Map<String, Any?>>
        return tasks.map { task ->
            RawTaskTuple(
                taskId = task.getValue("taskId").toString(),
                taskPath = task.getValue("taskPath").toString(),
                artifactRelativePath = task.getValue("artifactRelativePath").toString(),
                role = task.getValue("role").toString(),
            )
        }
    }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize().let { path ->
                if (Files.isDirectory(path.resolve("tools"))) path else path.parent
            }

    private data class RawTaskTuple(
        val taskId: String,
        val taskPath: String,
        val artifactRelativePath: String,
        val role: String,
    )
}
