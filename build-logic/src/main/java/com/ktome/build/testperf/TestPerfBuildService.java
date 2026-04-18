package com.ktome.build.testperf;

import java.util.ArrayList;
import java.util.List;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.tooling.Failure;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;
import org.gradle.tooling.events.task.TaskFailureResult;
import org.gradle.tooling.events.task.TaskFinishEvent;
import org.gradle.tooling.events.task.TaskOperationResult;
import org.gradle.tooling.events.task.TaskSkippedResult;
import org.gradle.tooling.events.task.TaskSuccessResult;

public abstract class TestPerfBuildService
        implements BuildService<BuildServiceParameters.None>, OperationCompletionListener {
    private final List<TaskEventRecord> taskEvents = new ArrayList<>();

    @Override
    public synchronized void onFinish(FinishEvent event) {
        if (!(event instanceof TaskFinishEvent taskFinishEvent)) {
            return;
        }
        taskEvents.add(TaskEventRecord.from(taskFinishEvent));
    }

    public synchronized List<TaskEventRecord> snapshotTaskEvents() {
        return List.copyOf(taskEvents);
    }

    public record TaskEventRecord(
            String taskPath,
            long startTime,
            long endTime,
            String outcome,
            boolean upToDate,
            boolean fromCache,
            boolean incremental,
            String failureMessage) {
        static TaskEventRecord from(TaskFinishEvent event) {
            TaskOperationResult result = event.getResult();
            String outcome = "UNKNOWN";
            boolean upToDate = false;
            boolean fromCache = false;
            boolean incremental = false;
            String failureMessage = null;

            if (result instanceof TaskSuccessResult successResult) {
                outcome = "SUCCESS";
                upToDate = successResult.isUpToDate();
                fromCache = successResult.isFromCache();
                incremental = successResult.isIncremental();
            } else if (result instanceof TaskFailureResult failureResult) {
                outcome = "FAILED";
                incremental = failureResult.isIncremental();
                failureMessage = firstFailureMessage(failureResult);
            } else if (result instanceof TaskSkippedResult skippedResult) {
                outcome = "SKIPPED";
                failureMessage = skippedResult.getSkipMessage();
            }

            return new TaskEventRecord(
                    event.getDescriptor().getTaskPath(),
                    result.getStartTime(),
                    result.getEndTime(),
                    outcome,
                    upToDate,
                    fromCache,
                    incremental,
                    failureMessage);
        }

        private static String firstFailureMessage(TaskFailureResult failureResult) {
            if (failureResult.getFailures().isEmpty()) {
                return null;
            }
            Failure firstFailure = failureResult.getFailures().get(0);
            String message = firstFailure.getMessage();
            return message == null || message.isBlank() ? firstFailure.getDescription() : message;
        }
    }
}
