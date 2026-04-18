package com.ktome.build.testperf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ktome.build.testperf.TestPerfBuildService.TaskEventRecord;
import com.ktome.build.testperf.records.RunRecord;
import com.ktome.build.testperf.records.TaskRecord;
import com.ktome.build.testperf.records.VerificationRecord;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.api.flow.FlowAction;
import org.gradle.api.flow.FlowParameters;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.Input;

public abstract class TestPerfFinalizeFlowAction
        implements FlowAction<TestPerfFinalizeFlowAction.Parameters> {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Type METADATA_MAP_TYPE =
            new TypeToken<Map<String, TestPerfTaskMetadataRegistry.TaskMetadata>>() {}.getType();
    private static final TestPerfArtifactReader ARTIFACT_READER = new TestPerfArtifactReader();
    private static final TestPerfComparator COMPARATOR = new TestPerfComparator();
    private static final TestPerfReporter REPORTER = new TestPerfReporter();

    public interface Parameters extends FlowParameters {
        @ServiceReference
        Property<TestPerfBuildService> getBuildService();

        @Input
        Property<String> getMetadataJson();

        @Input
        Property<String> getStorageDir();

        @Input
        Property<Integer> getSchemaVersion();

        @Input
        Property<Integer> getHistoryLimit();

        @Input
        Property<String> getMode();

        @Input
        ListProperty<String> getRequestedTaskPaths();

        @Input
        Property<Boolean> getBuildSucceeded();

        @Input
        Property<Boolean> getBuildCacheEnabled();

        @Input
        Property<Boolean> getConfigurationCacheEnabled();

        @Input
        Property<Boolean> getParallelEnabled();

        @Input
        Property<Integer> getMaxWorkers();

        @Input
        Property<Integer> getJavaMajorVersion();

        @Input
        Property<String> getOsFamily();

        @Input
        Property<Boolean> getDaemonReused();

        @Input
        Property<Integer> getAvailableProcessors();

        @Input
        Property<Double> getSystemLoadAverage();
    }

    @Override
    public void execute(Parameters parameters) {
        String mode = parameters.getMode().get();
        if (TestPerfLaneManager.MODE_OFF.equals(mode)) {
            return;
        }
        Map<String, TestPerfTaskMetadataRegistry.TaskMetadata> metadataByTaskPath =
                GSON.fromJson(parameters.getMetadataJson().get(), METADATA_MAP_TYPE);
        if (metadataByTaskPath == null || metadataByTaskPath.isEmpty()) {
            return;
        }

        List<TaskEventRecord> monitoredEvents =
                parameters.getBuildService().get().snapshotTaskEvents().stream()
                        .filter(event -> metadataByTaskPath.containsKey(event.taskPath()))
                        .collect(Collectors.toCollection(ArrayList::new));
        if (monitoredEvents.isEmpty()) {
            return;
        }

        monitoredEvents.sort(Comparator.comparing(TaskEventRecord::taskPath));
        List<TaskRecord> taskRecords = new ArrayList<>();
        for (TaskEventRecord event : monitoredEvents) {
            TestPerfTaskMetadataRegistry.TaskMetadata metadata = metadataByTaskPath.get(event.taskPath());
            taskRecords.add(toTaskRecord(event, metadata));
        }

        List<String> requestedLeafTaskPaths =
                taskRecords.stream().map(TaskRecord::taskPath).distinct().sorted().toList();
        RunRecord.InvocationContext invocationContext =
                new RunRecord.InvocationContext(
                        parameters.getRequestedTaskPaths().getOrElse(List.of()),
                        requestedLeafTaskPaths,
                        parameters.getBuildCacheEnabled().get(),
                        parameters.getConfigurationCacheEnabled().get(),
                        parameters.getParallelEnabled().get(),
                        parameters.getMaxWorkers().get(),
                        parameters.getJavaMajorVersion().get(),
                        parameters.getOsFamily().get());

        boolean monitoredLeafFailure =
                taskRecords.stream().anyMatch(taskRecord -> "FAILED".equals(taskRecord.outcome()));
        boolean complete = !monitoredLeafFailure;
        RunRecord runRecord =
                new RunRecord(
                        parameters.getSchemaVersion().get(),
                        TestPerfLaneManager.newRunId(),
                        TestPerfLaneManager.computeLaneId(parameters.getSchemaVersion().get(), invocationContext),
                        invocationContext,
                        taskRecords,
                        complete,
                        parameters.getDaemonReused().get(),
                        parameters.getAvailableProcessors().get(),
                        parameters.getSystemLoadAverage().get(),
                        parameters.getBuildSucceeded().get(),
                        System.currentTimeMillis());

        TestPerfLaneManager laneManager =
                new TestPerfLaneManager(
                        parameters.getStorageDir().get(),
                        parameters.getHistoryLimit().get(),
                        COMPARATOR,
                        REPORTER);
        TestPerfLaneManager.WriteResult writeResult = laneManager.write(runRecord, mode);
        if (writeResult == TestPerfLaneManager.WriteResult.BASELINE_ESTABLISHED) {
            System.out.println("[testperf] baseline established for lane " + runRecord.laneId());
        } else if (writeResult == TestPerfLaneManager.WriteResult.REPORT_ONLY_SUMMARY) {
            System.out.println("[testperf] current-run summary written for lane " + runRecord.laneId());
        } else if (writeResult == TestPerfLaneManager.WriteResult.INCOMPLETE_BUILD) {
            System.out.println("[testperf] current build has failed monitored leaf tasks; baseline not updated");
        }
    }

    private static TaskRecord toTaskRecord(
            TaskEventRecord event,
            TestPerfTaskMetadataRegistry.TaskMetadata metadata) {
        boolean actualExecution =
                "SUCCESS".equals(event.outcome()) && !event.upToDate() && !event.fromCache();
        VerificationRecord verification =
                TestPerfTaskKinds.isVerification(metadata.kind())
                        ? ARTIFACT_READER.readVerification(metadata)
                        : null;
        TaskRecord.TestDetails tests =
                TestPerfTaskKinds.TEST.equals(metadata.kind()) ? ARTIFACT_READER.readTest(metadata) : null;
        if ("SKIPPED".equals(event.outcome())) {
            if (verification != null) {
                verification =
                        new VerificationRecord(
                                firstNonBlank(verification.domainId(), metadata.domainId()),
                                firstNonBlank(verification.tier(), metadata.tier()),
                                firstNonBlank(verification.nodeId(), metadata.nodeId()),
                                metadata.inputSnapshotHash(),
                                null,
                                null,
                                verification.reportOnly(),
                                null,
                                null,
                                null,
                                verification.workload());
            }
            tests = null;
        }
        return new TaskRecord(
                event.taskPath(),
                metadata.kind(),
                event.outcome(),
                Math.max(0L, event.endTime() - event.startTime()),
                actualExecution,
                event.upToDate(),
                event.fromCache(),
                event.incremental(),
                metadata.outputDir(),
                metadata.testResultsDir(),
                verification,
                tests,
                event.failureMessage());
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
