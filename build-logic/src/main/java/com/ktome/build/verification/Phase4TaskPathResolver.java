package com.ktome.build.verification;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

public final class Phase4TaskPathResolver {
    private Phase4TaskPathResolver() {
    }

    public static TaskProvider<? extends Task> resolve(Project rootProject, String taskPath) {
        int separatorIndex = taskPath.lastIndexOf(':');
        if (separatorIndex <= 0 || separatorIndex == taskPath.length() - 1) {
            throw new IllegalArgumentException("Invalid Phase 4 aggregation manifest taskPath '" + taskPath + "'.");
        }
        String projectPath = taskPath.substring(0, separatorIndex);
        String taskName = taskPath.substring(separatorIndex + 1);
        return rootProject.project(projectPath).getTasks().named(taskName);
    }
}
