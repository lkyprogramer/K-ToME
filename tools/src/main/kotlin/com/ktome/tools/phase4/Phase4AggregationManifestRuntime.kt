package com.ktome.tools.phase4

import org.yaml.snakeyaml.Yaml

internal enum class Phase4AggregationTaskRole {
    OWNER,
    AGGREGATION_ONLY,
}

internal data class Phase4AggregationManifestTask(
    val taskId: String,
    val taskPath: String,
    val artifactRelativePath: String,
    val role: Phase4AggregationTaskRole,
    val metricIds: List<String> = emptyList(),
)

internal data class Phase4AggregationManifestModel(
    val schemaVersion: String,
    val phaseId: String,
    val tasks: List<Phase4AggregationManifestTask>,
) {
    val ownerTasks: List<Phase4AggregationManifestTask>
        get() = tasks.filter { task -> task.role == Phase4AggregationTaskRole.OWNER }

    val aggregationOnlyTasks: List<Phase4AggregationManifestTask>
        get() = tasks.filter { task -> task.role == Phase4AggregationTaskRole.AGGREGATION_ONLY }
}

internal object Phase4AggregationManifestRuntime {
    private const val RESOURCE_PATH: String = "phase4/aggregation-manifest.yaml"
    private const val SCHEMA_VERSION: String = "phase4-aggregation-manifest-v1"
    private const val PHASE_ID: String = "P4"
    private val ROOT_KEYS: Set<String> = setOf("schemaVersion", "phaseId", "tasks")
    private val TASK_KEYS: Set<String> = setOf("taskId", "taskPath", "artifactRelativePath", "role", "metricIds")

    private val loadedManifest: Phase4AggregationManifestModel by lazy {
        val yamlText =
            checkNotNull(javaClass.classLoader.getResourceAsStream(RESOURCE_PATH)) {
                "Missing Phase 4 aggregation manifest resource '$RESOURCE_PATH'."
            }.bufferedReader().use { reader -> reader.readText() }
        parse(yamlText = yamlText, sourceDescription = RESOURCE_PATH)
    }

    fun manifest(): Phase4AggregationManifestModel = loadedManifest

    fun tasks(): List<Phase4AggregationManifestTask> = loadedManifest.tasks

    fun taskIdsInOrder(): List<String> = loadedManifest.tasks.map(Phase4AggregationManifestTask::taskId)

    fun taskPathsInOrder(): List<String> = loadedManifest.tasks.map(Phase4AggregationManifestTask::taskPath)

    fun artifactRelativePathsInOrder(): List<String> = loadedManifest.tasks.map(Phase4AggregationManifestTask::artifactRelativePath)

    fun ownerTaskIds(): Set<String> = loadedManifest.ownerTasks.mapTo(linkedSetOf(), Phase4AggregationManifestTask::taskId)

    fun aggregationOnlyTaskIds(): Set<String> =
        loadedManifest.aggregationOnlyTasks.mapTo(linkedSetOf(), Phase4AggregationManifestTask::taskId)

    internal fun parse(
        yamlText: String,
        sourceDescription: String,
    ): Phase4AggregationManifestModel {
        val root = requireStringMap(node = Yaml().load<Any?>(yamlText), description = sourceDescription)
        rejectUnknownKeys(map = root, allowedKeys = ROOT_KEYS, description = sourceDescription)
        val schemaVersion = requiredString(root, "schemaVersion", sourceDescription)
        require(schemaVersion == SCHEMA_VERSION) {
            "Unsupported Phase 4 aggregation manifest schemaVersion '$schemaVersion' at $sourceDescription."
        }
        val phaseId = requiredString(root, "phaseId", sourceDescription)
        require(phaseId == PHASE_ID) {
            "Unsupported Phase 4 aggregation manifest phaseId '$phaseId' at $sourceDescription."
        }
        val rawTasks = requireList(node = root["tasks"], description = "$sourceDescription.tasks")
        require(rawTasks.isNotEmpty()) {
            "Phase 4 aggregation manifest must declare at least one task at $sourceDescription."
        }

        val taskIds = linkedSetOf<String>()
        val taskPaths = linkedSetOf<String>()
        val artifactRelativePaths = linkedSetOf<String>()
        val tasks =
            rawTasks.mapIndexed { index, rawTask ->
                val taskDescription = "$sourceDescription.tasks[$index]"
                val taskNode = requireStringMap(node = rawTask, description = taskDescription)
                rejectUnknownKeys(map = taskNode, allowedKeys = TASK_KEYS, description = taskDescription)
                val taskId = requiredString(taskNode, "taskId", taskDescription)
                val taskPath = requiredString(taskNode, "taskPath", taskDescription)
                val artifactRelativePath = requiredString(taskNode, "artifactRelativePath", taskDescription)
                val roleName = requiredString(taskNode, "role", taskDescription)
                val metricIds = optionalStringList(taskNode, "metricIds", taskDescription)
                require(taskPath.startsWith(":tools:") || taskPath.startsWith(":game:")) {
                    "Phase 4 aggregation manifest taskPath must target :tools:* or :game:* at $taskDescription."
                }
                val expectedTaskId = taskPath.substringAfterLast(':')
                require(taskId == expectedTaskId) {
                    "Phase 4 aggregation manifest taskId '$taskId' does not match taskPath '$taskPath' at $taskDescription."
                }
                require(taskIds.add(taskId)) {
                    "Duplicate Phase 4 aggregation manifest taskId '$taskId' at $taskDescription."
                }
                require(taskPaths.add(taskPath)) {
                    "Duplicate Phase 4 aggregation manifest taskPath '$taskPath' at $taskDescription."
                }
                require(artifactRelativePaths.add(artifactRelativePath)) {
                    "Duplicate Phase 4 aggregation manifest artifactRelativePath '$artifactRelativePath' at $taskDescription."
                }
                require(metricIds.size == metricIds.toSet().size) {
                    "Duplicate Phase 4 aggregation manifest metricIds at $taskDescription."
                }
                Phase4AggregationManifestTask(
                    taskId = taskId,
                    taskPath = taskPath,
                    artifactRelativePath = artifactRelativePath,
                    role =
                        runCatching { Phase4AggregationTaskRole.valueOf(roleName) }
                            .getOrElse { cause ->
                                throw IllegalArgumentException(
                                    "Unsupported Phase 4 aggregation manifest role '$roleName' at $taskDescription.",
                                    cause,
                                )
                            },
                    metricIds = metricIds,
                )
            }
        return Phase4AggregationManifestModel(schemaVersion = schemaVersion, phaseId = phaseId, tasks = tasks)
    }

    private fun rejectUnknownKeys(
        map: Map<String, Any?>,
        allowedKeys: Set<String>,
        description: String,
    ) {
        map.keys.forEach { key ->
            require(key in allowedKeys) {
                "Unsupported key '$key' in $description."
            }
        }
    }

    private fun requireStringMap(
        node: Any?,
        description: String,
    ): Map<String, Any?> {
        require(node is Map<*, *>) {
            "Expected $description to be a YAML object."
        }
        require(node.keys.all { key -> key is String }) {
            "Expected $description keys to be strings."
        }
        @Suppress("UNCHECKED_CAST")
        return node as Map<String, Any?>
    }

    private fun requireList(
        node: Any?,
        description: String,
    ): List<Any?> {
        require(node is List<*>) {
            "Expected $description to be a YAML list."
        }
        return node
    }

    private fun requiredString(
        map: Map<String, Any?>,
        key: String,
        description: String,
    ): String {
        val value = map[key]
        require(value is String) {
            "Expected $description.$key to be a string."
        }
        return value
    }

    private fun optionalStringList(
        map: Map<String, Any?>,
        key: String,
        description: String,
    ): List<String> {
        val node = map[key] ?: return emptyList()
        val values = requireList(node = node, description = "$description.$key")
        return values.mapIndexed { index, value ->
            require(value is String && value.isNotBlank()) {
                "Expected $description.$key[$index] to be a non-blank string."
            }
            value
        }
    }
}
