package com.ktome.build.verification;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

public final class VerifyChangedPlanGate {
    private VerifyChangedPlanGate() {}

    public static void applyTo(Task task, Provider<RegularFile> taskPathsFile, String prepareTaskPath) {
        task.mustRunAfter(prepareTaskPath);
        task.onlyIf(ignored -> shouldRun(task, taskPathsFile));
    }

    private static boolean shouldRun(Task task, Provider<RegularFile> taskPathsFile) {
        if (!verifyChangedRequested(task)) {
            return true;
        }
        if (taskExplicitlyRequested(task)) {
            return true;
        }
        File requestedTasksFile = taskPathsFile.get().getAsFile();
        if (!requestedTasksFile.exists()) {
            return false;
        }
        try (Stream<String> lines = Files.lines(requestedTasksFile.toPath(), StandardCharsets.UTF_8)) {
            return lines
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .anyMatch(task.getPath()::equals);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to read verifyChanged task plan from " + requestedTasksFile.getAbsolutePath(),
                    exception);
        }
    }

    private static boolean verifyChangedRequested(Task task) {
        return task.getProject().getGradle().getStartParameter().getTaskNames().stream()
                .anyMatch(taskName -> taskName.equals("verifyChanged") || taskName.endsWith(":verifyChanged"));
    }

    private static boolean taskExplicitlyRequested(Task task) {
        String taskPath = task.getPath();
        String relativeTaskPath = taskPath.startsWith(":") ? taskPath.substring(1) : taskPath;
        String taskName = task.getName();
        return task.getProject().getGradle().getStartParameter().getTaskNames().stream()
                .anyMatch(taskNameArg ->
                        taskNameArg.equals(taskPath)
                                || taskNameArg.equals(relativeTaskPath)
                                || taskNameArg.equals(taskName));
    }
}
