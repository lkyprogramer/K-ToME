package com.ktome.build.testperf;

import com.ktome.build.verification.AbstractVerificationExecTask;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.testing.Test;

public final class TestPerfTaskMetadataRegistry {
    private final Map<String, TaskMetadataProvider> metadataProvidersByTaskPath = new LinkedHashMap<>();

    public void registerTestTask(Test task) {
        metadataProvidersByTaskPath.put(
                task.getPath(),
                () -> {
                    String bandHint = TestPerfPlainTestOptIn.bandHint(task);
                    if (bandHint == null) {
                        return null;
                    }
                    String testResultsDir = null;
                    if (task.getReports().getJunitXml().getRequired().getOrElse(true)) {
                        testResultsDir =
                                task.getReports().getJunitXml().getOutputLocation().getAsFile().get().getAbsolutePath();
                    }
                    return new TaskMetadata(
                            task.getPath(),
                            TestPerfTaskKinds.TEST,
                            firstOutputDir(task),
                            testResultsDir,
                            null,
                            null,
                            null,
                            null,
                            bandHint);
                });
    }

    public void registerVerificationTask(AbstractVerificationExecTask task, String kind) {
        metadataProvidersByTaskPath.put(
                task.getPath(),
                () ->
                        new TaskMetadata(
                                task.getPath(),
                                kind,
                                task.getOutputDir().getAsFile().get().getAbsolutePath(),
                                null,
                                getOptionalValue(task.getDomainId()),
                                getOptionalValue(task.getTier()),
                                getOptionalValue(task.getNodeId()),
                                getOptionalValue(task.getInputSnapshotHash()),
                                null));
    }

    public Map<String, TaskMetadata> snapshot() {
        Map<String, TaskMetadata> resolved = new LinkedHashMap<>();
        metadataProvidersByTaskPath.forEach(
                (taskPath, provider) -> {
                    TaskMetadata metadata = provider.resolve();
                    if (metadata != null) {
                        resolved.put(taskPath, metadata);
                    }
                });
        return Map.copyOf(resolved);
    }

    private static String firstOutputDir(Test task) {
        for (File file : task.getOutputs().getFiles().getFiles()) {
            if (file.isDirectory()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    private static String getOptionalValue(Property<String> property) {
        return property.isPresent() ? property.get() : null;
    }

    public record TaskMetadata(
            String taskPath,
            String kind,
            String outputDir,
            String testResultsDir,
            String domainId,
            String tier,
            String nodeId,
            String inputSnapshotHash,
            String bandHint) {}

    @FunctionalInterface
    private interface TaskMetadataProvider {
        TaskMetadata resolve();
    }
}
