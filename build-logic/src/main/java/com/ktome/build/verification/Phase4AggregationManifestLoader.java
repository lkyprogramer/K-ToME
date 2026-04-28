package com.ktome.build.verification;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

public final class Phase4AggregationManifestLoader {
    private static final Set<String> ROOT_KEYS = Set.of("schemaVersion", "phaseId", "tasks");
    private static final Set<String> TASK_KEYS =
            Set.of("taskId", "taskPath", "artifactRelativePath", "role", "metricIds");

    private final Yaml yaml = new Yaml();

    public Phase4AggregationManifest load(Path manifestPath) {
        Objects.requireNonNull(manifestPath, "manifestPath");
        try {
            return load(Files.readString(manifestPath), manifestPath.toString());
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read Phase 4 aggregation manifest at " + manifestPath + ".", exception);
        }
    }

    Phase4AggregationManifest load(String yamlText, String sourceDescription) {
        Map<String, Object> root = requireMap(yaml.load(yamlText), sourceDescription);
        rejectUnknownKeys(root, ROOT_KEYS, sourceDescription);
        String schemaVersion = requiredString(root, "schemaVersion", sourceDescription);
        require(
                Phase4AggregationManifest.SCHEMA_VERSION.equals(schemaVersion),
                "Unsupported Phase 4 aggregation manifest schemaVersion '" + schemaVersion + "' at " + sourceDescription + ".");
        String phaseId = requiredString(root, "phaseId", sourceDescription);
        require(
                Phase4AggregationManifest.PHASE_ID.equals(phaseId),
                "Unsupported Phase 4 aggregation manifest phaseId '" + phaseId + "' at " + sourceDescription + ".");

        List<?> rawTasks = requireList(root.get("tasks"), sourceDescription + ".tasks");
        require(!rawTasks.isEmpty(), "Phase 4 aggregation manifest must declare at least one task at " + sourceDescription + ".");

        List<Phase4AggregationManifest.TaskEntry> tasks = new ArrayList<>();
        Set<String> taskIds = new LinkedHashSet<>();
        Set<String> taskPaths = new LinkedHashSet<>();
        Set<String> artifactPaths = new LinkedHashSet<>();
        for (int index = 0; index < rawTasks.size(); index++) {
            String taskDescription = sourceDescription + ".tasks[" + index + "]";
            Map<String, Object> taskNode = requireMap(rawTasks.get(index), taskDescription);
            rejectUnknownKeys(taskNode, TASK_KEYS, taskDescription);

            String taskId = requiredString(taskNode, "taskId", taskDescription);
            String taskPath = requiredString(taskNode, "taskPath", taskDescription);
            String artifactRelativePath = requiredString(taskNode, "artifactRelativePath", taskDescription);
            String roleName = requiredString(taskNode, "role", taskDescription);
            List<String> metricIds = optionalStringList(taskNode, "metricIds", taskDescription);
            Phase4AggregationManifest.TaskRole role;
            try {
                role = Phase4AggregationManifest.TaskRole.valueOf(roleName);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unsupported Phase 4 aggregation manifest role '" + roleName + "' at " + taskDescription + ".",
                        exception);
            }

            require(
                    taskPath.startsWith(":tools:") || taskPath.startsWith(":game:"),
                    "Phase 4 aggregation manifest taskPath must target :tools:* or :game:* at " + taskDescription + ".");
            String expectedTaskId = taskPath.substring(taskPath.lastIndexOf(':') + 1);
            require(
                    taskId.equals(expectedTaskId),
                    "Phase 4 aggregation manifest taskId '" + taskId + "' does not match taskPath '" + taskPath + "' at " + taskDescription + ".");
            require(taskIds.add(taskId), "Duplicate Phase 4 aggregation manifest taskId '" + taskId + "' at " + taskDescription + ".");
            require(taskPaths.add(taskPath), "Duplicate Phase 4 aggregation manifest taskPath '" + taskPath + "' at " + taskDescription + ".");
            require(
                    artifactPaths.add(artifactRelativePath),
                    "Duplicate Phase 4 aggregation manifest artifactRelativePath '" + artifactRelativePath + "' at " + taskDescription + ".");
            require(
                    new LinkedHashSet<>(metricIds).size() == metricIds.size(),
                    "Duplicate Phase 4 aggregation manifest metricIds at " + taskDescription + ".");
            tasks.add(new Phase4AggregationManifest.TaskEntry(taskId, taskPath, artifactRelativePath, role, metricIds));
        }
        return new Phase4AggregationManifest(schemaVersion, phaseId, tasks);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requireMap(Object node, String description) {
        require(node instanceof Map<?, ?>, "Expected " + description + " to be a YAML object.");
        return (Map<String, Object>) node;
    }

    private static List<?> requireList(Object node, String description) {
        require(node instanceof List<?>, "Expected " + description + " to be a YAML list.");
        return (List<?>) node;
    }

    private static void rejectUnknownKeys(
            Map<String, Object> map,
            Set<String> allowedKeys,
            String description) {
        for (Object key : map.keySet()) {
            require(key instanceof String, "Expected " + description + " keys to be strings.");
            require(
                    allowedKeys.contains(key),
                    "Unsupported key '" + key + "' in " + description + ".");
        }
    }

    private static String requiredString(Map<String, Object> map, String key, String description) {
        Object value = map.get(key);
        require(value instanceof String, "Expected " + description + "." + key + " to be a string.");
        return (String) value;
    }

    private static List<String> optionalStringList(Map<String, Object> map, String key, String description) {
        Object node = map.get(key);
        if (node == null) {
            return List.of();
        }
        List<?> rawValues = requireList(node, description + "." + key);
        List<String> values = new ArrayList<>();
        for (int index = 0; index < rawValues.size(); index++) {
            Object value = rawValues.get(index);
            require(
                    value instanceof String && !((String) value).isBlank(),
                    "Expected " + description + "." + key + "[" + index + "] to be a non-blank string.");
            values.add((String) value);
        }
        return values;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
