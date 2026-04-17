package com.ktome.tools.phase4

import java.lang.reflect.InvocationTargetException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.net.URLClassLoader
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.StandardJavaFileManager
import javax.tools.ToolProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

class Phase4AggregationManifestDualLoaderParityTest {
    @Test
    fun `checked in manifest stays in parity between build logic and runtime loaders`() {
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
        val buildLogicTasks = buildLogicLoaderHandle.parse(yamlText = yamlText, sourceDescription = "inline-manifest")

        assertEquals(rawYamlTasks(yamlText), runtimeTasks)
        assertEquals(runtimeTasks, buildLogicTasks)
    }

    @Test
    fun `build logic and runtime loaders reject unknown task fields with the same diagnostic`() {
        assertRejectedWithSameMessage(
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
            expectedMessageFragment = "unexpected",
        )
    }

    @Test
    fun `build logic and runtime loaders reject duplicate artifact paths with the same diagnostic`() {
        assertRejectedWithSameMessage(
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
            expectedMessageFragment = "Duplicate Phase 4 aggregation manifest artifactRelativePath",
        )
    }

    @Test
    fun `build logic and runtime loaders reject task id drift with the same diagnostic`() {
        assertRejectedWithSameMessage(
            yamlText =
                """
                schemaVersion: phase4-aggregation-manifest-v1
                phaseId: P4
                tasks:
                  - taskId: wrongId
                    taskPath: :tools:mapgenSmoke
                    artifactRelativePath: tools/build/reports/phase4/mapgen/mapgen-smoke-summary.json
                    role: AGGREGATION_ONLY
                """.trimIndent(),
            expectedMessageFragment = "does not match taskPath",
        )
    }

    private fun assertRejectedWithSameMessage(
        yamlText: String,
        expectedMessageFragment: String,
    ) {
        val runtimeException =
            assertThrows(IllegalArgumentException::class.java) {
                Phase4AggregationManifestRuntime.parse(
                    yamlText = yamlText,
                    sourceDescription = "inline-manifest",
                )
            }
        val buildLogicException =
            assertThrows(IllegalArgumentException::class.java) {
                buildLogicLoaderHandle.parse(
                    yamlText = yamlText,
                    sourceDescription = "inline-manifest",
                )
            }

        assertEquals(true, runtimeException.message.orEmpty().contains(expectedMessageFragment))
        assertEquals(runtimeException.message, buildLogicException.message)
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

    private class BuildLogicLoaderHandle(
        private val loaderClass: Class<*>,
        private val manifestClass: Class<*>,
        private val taskEntryClass: Class<*>,
    ) {
        private val parseMethod =
            loaderClass.getDeclaredMethod("load", String::class.java, String::class.java).apply {
                isAccessible = true
            }
        private val getTasksMethod = manifestClass.getMethod("getTasks")
        private val getTaskIdMethod = taskEntryClass.getMethod("getTaskId")
        private val getTaskPathMethod = taskEntryClass.getMethod("getTaskPath")
        private val getArtifactRelativePathMethod = taskEntryClass.getMethod("getArtifactRelativePath")
        private val getRoleMethod = taskEntryClass.getMethod("getRole")

        fun parse(
            yamlText: String,
            sourceDescription: String,
        ): List<RawTaskTuple> {
            val manifest = invokeParse(yamlText = yamlText, sourceDescription = sourceDescription)
            @Suppress("UNCHECKED_CAST")
            val tasks = getTasksMethod.invoke(manifest) as List<Any?>
            return tasks.map { task ->
                RawTaskTuple(
                    taskId = getTaskIdMethod.invoke(task).toString(),
                    taskPath = getTaskPathMethod.invoke(task).toString(),
                    artifactRelativePath = getArtifactRelativePathMethod.invoke(task).toString(),
                    role = getRoleMethod.invoke(task).toString(),
                )
            }
        }

        private fun invokeParse(
            yamlText: String,
            sourceDescription: String,
        ): Any? =
            try {
                parseMethod.invoke(loaderClass.getDeclaredConstructor().newInstance(), yamlText, sourceDescription)
            } catch (exception: InvocationTargetException) {
                val cause = exception.targetException
                if (cause is RuntimeException) {
                    throw cause
                }
                throw exception
            }
    }

    private companion object {
        val buildLogicLoaderHandle: BuildLogicLoaderHandle by lazy {
            val repoRoot = repoRootStatic()
            val outputDir = Files.createTempDirectory("phase4-build-logic-loader")
            val compiler =
                checkNotNull(ToolProvider.getSystemJavaCompiler()) {
                    "JDK compiler is required to verify build-logic/runtime manifest parity."
                }
            val diagnostics = DiagnosticCollector<JavaFileObject>()
            val javaFiles =
                listOf(
                    repoRoot.resolve("build-logic/src/main/java/com/ktome/build/verification/Phase4AggregationManifest.java"),
                    repoRoot.resolve("build-logic/src/main/java/com/ktome/build/verification/Phase4AggregationManifestLoader.java"),
                ).map(Path::toFile)

            compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8).use { fileManager ->
                compileBuildLogicSources(
                    compiler = compiler,
                    fileManager = fileManager,
                    diagnostics = diagnostics,
                    outputDir = outputDir,
                    javaFiles = javaFiles,
                )
            }

            val classLoader = URLClassLoader(arrayOf(outputDir.toUri().toURL()), Phase4AggregationManifestDualLoaderParityTest::class.java.classLoader)
            BuildLogicLoaderHandle(
                loaderClass = Class.forName("com.ktome.build.verification.Phase4AggregationManifestLoader", true, classLoader),
                manifestClass = Class.forName("com.ktome.build.verification.Phase4AggregationManifest", true, classLoader),
                taskEntryClass = Class.forName("com.ktome.build.verification.Phase4AggregationManifest\$TaskEntry", true, classLoader),
            )
        }

        private fun compileBuildLogicSources(
            compiler: javax.tools.JavaCompiler,
            fileManager: StandardJavaFileManager,
            diagnostics: DiagnosticCollector<JavaFileObject>,
            outputDir: Path,
            javaFiles: List<java.io.File>,
        ) {
            val compilationSucceeded =
                compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    listOf("-classpath", System.getProperty("java.class.path"), "-d", outputDir.toString()),
                    null,
                    fileManager.getJavaFileObjectsFromFiles(javaFiles),
                ).call()
            require(compilationSucceeded) {
                diagnostics.diagnostics.joinToString(separator = System.lineSeparator()) { diagnostic ->
                    diagnostic.toString()
                }
            }
        }

        private fun repoRootStatic(): Path =
            System.getProperty("ktome.repo.root")
                ?.let(Path::of)
                ?: Path.of("").toAbsolutePath().normalize().let { path ->
                    if (Files.isDirectory(path.resolve("tools"))) path else path.parent
                }
    }
}
