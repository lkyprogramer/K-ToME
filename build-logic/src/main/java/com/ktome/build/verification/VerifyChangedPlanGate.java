package com.ktome.build.verification;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.StopExecutionException;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

public final class VerifyChangedPlanGate {
    private VerifyChangedPlanGate() {}

    public static void applyTo(Task task, Provider<RegularFile> taskPathsFile, String prepareTaskPath) {
        applyTo(task, taskPathsFile, taskPathsFile, prepareTaskPath);
    }

    public static void applyTo(
            Task task,
            Provider<RegularFile> verifyChangedTaskPathsFile,
            Provider<RegularFile> verifyChangedPreflightTaskPathsFile,
            String prepareTaskPath) {
        task.mustRunAfter(prepareTaskPath);
        task.doFirst(new Action<Task>() {
            @Override
            public void execute(Task ignored) {
                if (!shouldRun(task, verifyChangedTaskPathsFile, verifyChangedPreflightTaskPathsFile)) {
                    throw new StopExecutionException("verifyChanged plan gate skipped " + task.getPath());
                }
            }
        });
    }

    private static boolean shouldRun(
            Task task,
            Provider<RegularFile> verifyChangedTaskPathsFile,
            Provider<RegularFile> verifyChangedPreflightTaskPathsFile) {
        List<Provider<RegularFile>> selectedPlanFiles = selectedPlanFiles(task, verifyChangedTaskPathsFile, verifyChangedPreflightTaskPathsFile);
        if (selectedPlanFiles.isEmpty()) {
            return true;
        }
        if (taskExplicitlyRequested(task)) {
            return true;
        }
        for (Provider<RegularFile> taskPathsFile : selectedPlanFiles) {
            File requestedTasksFile = taskPathsFile.get().getAsFile();
            if (!requestedTasksFile.exists()) {
                continue;
            }
            try (Stream<String> lines = Files.lines(requestedTasksFile.toPath(), StandardCharsets.UTF_8)) {
                if (lines.map(String::trim).filter(line -> !line.isBlank()).anyMatch(task.getPath()::equals)) {
                    return true;
                }
            } catch (IOException exception) {
                throw new UncheckedIOException(
                        "Failed to read verifyChanged task plan from " + requestedTasksFile.getAbsolutePath(),
                        exception);
            }
        }
        return false;
    }

    private static List<Provider<RegularFile>> selectedPlanFiles(
            Task task,
            Provider<RegularFile> verifyChangedTaskPathsFile,
            Provider<RegularFile> verifyChangedPreflightTaskPathsFile) {
        Set<Provider<RegularFile>> selected = new LinkedHashSet<>();
        task.getProject().getGradle().getStartParameter().getTaskNames().forEach(taskNameArg -> {
            if (matchesTaskName(taskNameArg, "verifyChanged")) {
                selected.add(verifyChangedTaskPathsFile);
            }
            if (matchesTaskName(taskNameArg, "verifyChangedPreflight")) {
                selected.add(verifyChangedPreflightTaskPathsFile);
            }
        });
        return new ArrayList<>(selected);
    }

    private static boolean matchesTaskName(String actualTaskName, String expectedTaskName) {
        return actualTaskName.equals(expectedTaskName) || actualTaskName.endsWith(":" + expectedTaskName);
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
