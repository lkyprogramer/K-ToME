package com.ktome.build.verification;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public final class Phase4AggregationManifest implements Serializable {
    public static final String SCHEMA_VERSION = "phase4-aggregation-manifest-v1";
    public static final String PHASE_ID = "P4";

    private final String schemaVersion;
    private final String phaseId;
    private final List<TaskEntry> tasks;

    public Phase4AggregationManifest(String schemaVersion, String phaseId, List<TaskEntry> tasks) {
        this.schemaVersion = Objects.requireNonNull(schemaVersion, "schemaVersion");
        this.phaseId = Objects.requireNonNull(phaseId, "phaseId");
        this.tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks"));
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getPhaseId() {
        return phaseId;
    }

    public List<TaskEntry> getTasks() {
        return tasks;
    }

    public List<String> getTaskPaths() {
        return tasks.stream().map(TaskEntry::getTaskPath).toList();
    }

    public List<String> getArtifactRelativePaths() {
        return tasks.stream().map(TaskEntry::getArtifactRelativePath).toList();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Phase4AggregationManifest manifest)) {
            return false;
        }
        return schemaVersion.equals(manifest.schemaVersion)
                && phaseId.equals(manifest.phaseId)
                && tasks.equals(manifest.tasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaVersion, phaseId, tasks);
    }

    @Override
    public String toString() {
        return "Phase4AggregationManifest{"
                + "schemaVersion='" + schemaVersion + '\''
                + ", phaseId='" + phaseId + '\''
                + ", tasks=" + tasks
                + '}';
    }

    public enum TaskRole {
        OWNER,
        AGGREGATION_ONLY
    }

    public static final class TaskEntry implements Serializable {
        private final String taskId;
        private final String taskPath;
        private final String artifactRelativePath;
        private final TaskRole role;

        public TaskEntry(
                String taskId,
                String taskPath,
                String artifactRelativePath,
                TaskRole role) {
            this.taskId = Objects.requireNonNull(taskId, "taskId");
            this.taskPath = Objects.requireNonNull(taskPath, "taskPath");
            this.artifactRelativePath =
                    Objects.requireNonNull(artifactRelativePath, "artifactRelativePath");
            this.role = Objects.requireNonNull(role, "role");
        }

        public String getTaskId() {
            return taskId;
        }

        public String getTaskPath() {
            return taskPath;
        }

        public String getArtifactRelativePath() {
            return artifactRelativePath;
        }

        public TaskRole getRole() {
            return role;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TaskEntry taskEntry)) {
                return false;
            }
            return taskId.equals(taskEntry.taskId)
                    && taskPath.equals(taskEntry.taskPath)
                    && artifactRelativePath.equals(taskEntry.artifactRelativePath)
                    && role == taskEntry.role;
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId, taskPath, artifactRelativePath, role);
        }

        @Override
        public String toString() {
            return "TaskEntry{"
                    + "taskId='" + taskId + '\''
                    + ", taskPath='" + taskPath + '\''
                    + ", artifactRelativePath='" + artifactRelativePath + '\''
                    + ", role=" + role
                    + '}';
        }
    }
}
