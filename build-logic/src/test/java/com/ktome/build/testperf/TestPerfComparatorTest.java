package com.ktome.build.testperf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ktome.build.testperf.records.DiffRecord;
import com.ktome.build.testperf.records.RunRecord;
import com.ktome.build.testperf.records.TaskRecord;
import com.ktome.build.testperf.records.VerificationRecord;
import com.ktome.build.testperf.records.WorkloadRecord;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestPerfComparatorTest {
    private final TestPerfComparator comparator = new TestPerfComparator();

    @Test
    void classifiesHeavyRegressionAsAlert() {
        RunRecord previous = runRecord(task(":tools:whiteBoxLoot", "TEST", 40_000L, true, false, false, null, testDetails(3, 0)), "previous");
        RunRecord current = runRecord(task(":tools:whiteBoxLoot", "TEST", 80_000L, true, false, false, null, testDetails(3, 0)), "current");

        DiffRecord diff = comparator.compare(current, List.of(previous)).diffs().get(0);
        assertEquals("HEAVY_EVALUATION", diff.taskBand());
        assertEquals("ALERT", diff.classification());
    }

    @Test
    void classifiesReportOnlyAggregateAsLightAggregate() {
        VerificationRecord verification =
                new VerificationRecord(
                        "contractLint",
                        "PREFLIGHT",
                        "contractLint.staticGraph",
                        "input",
                        "snapshot",
                        "REPORT_ONLY_REBUILD",
                        true,
                        7,
                        0,
                        100L,
                        new WorkloadRecord("STATIC_GRAPH", null, List.of("STATIC_GRAPH"), "STATIC_GRAPH", true, "/tmp/source", null, null));
        RunRecord previous =
                runRecord(task(":tools:verifyContractLintPreflightReport", "VERIFICATION_REPORT_TASK", 100L, true, false, false, verification, null), "previous");
        RunRecord current =
                runRecord(task(":tools:verifyContractLintPreflightReport", "VERIFICATION_REPORT_TASK", 150L, true, false, false, verification, null), "current");

        DiffRecord diff = comparator.compare(current, List.of(previous)).diffs().get(0);
        assertEquals("LIGHT_AGGREGATE", diff.taskBand());
        assertEquals("NORMAL", diff.classification());
    }

    @Test
    void treatsSuccessfulUpToDateSampleAsComparableObservation() {
        RunRecord previous = runRecord(task(":tools:whiteBoxLoot", "TEST", 100L, false, true, false, null, testDetails(3, 0)), "previous");
        RunRecord current = runRecord(task(":tools:whiteBoxLoot", "TEST", 120L, false, true, false, null, testDetails(3, 0)), "current");

        DiffRecord diff = comparator.compare(current, List.of(previous)).diffs().get(0);
        assertEquals("INFO", diff.classification());
        assertEquals(false, diff.comparable());
    }

    @Test
    void fallsBackToOlderComparableRunWhenLatestRunIsNotComparable() {
        RunRecord latest = runRecord(task(":tools:hiddenContentHarness", "VERIFICATION_TASK", 90L, false, false, false, verification("hidden"), null, "SKIPPED"), "latest");
        RunRecord comparable = runRecord(task(":tools:hiddenContentHarness", "VERIFICATION_TASK", 100L, true, false, false, verification("hidden"), null), "comparable");
        RunRecord current = runRecord(task(":tools:hiddenContentHarness", "VERIFICATION_TASK", 110L, true, false, false, verification("hidden"), null), "current");

        TestPerfComparator.ComparisonResult result = comparator.compare(current, List.of(latest, comparable));
        DiffRecord diff = result.diffs().get(0);
        assertEquals("latest", result.previousRunId());
        assertEquals("comparable", result.previousComparableRunId());
        assertEquals("NORMAL", diff.classification());
        assertTrue(diff.comparable());
    }

    @Test
    void marksSkippedSampleAsIncomparable() {
        RunRecord previous = runRecord(task(":tools:hiddenContentHarness", "VERIFICATION_TASK", 100L, true, false, false, verification("hidden"), null), "previous");
        RunRecord current = runRecord(task(":tools:hiddenContentHarness", "VERIFICATION_TASK", 10L, false, false, false, verification("hidden"), null, "SKIPPED"), "current");

        DiffRecord diff = comparator.compare(current, List.of(previous)).diffs().get(0);
        assertEquals("INCOMPARABLE", diff.classification());
    }

    @Test
    void classifiesHeavyWorkloadOnlyChangeAsWarn() {
        RunRecord previous = runRecord(task(":tools:whiteBoxLoot", "TEST", 1_000L, true, false, false, null, testDetails(10, 0)), "previous");
        RunRecord current = runRecord(task(":tools:whiteBoxLoot", "TEST", 1_100L, true, false, false, null, testDetails(12, 0)), "current");

        DiffRecord diff = comparator.compare(current, List.of(previous)).diffs().get(0);
        assertEquals("WARN", diff.classification());
    }

    private static RunRecord runRecord(TaskRecord task, String runId) {
        return new RunRecord(
                2,
                runId,
                "lane",
                new RunRecord.InvocationContext(List.of(task.taskPath()), List.of(task.taskPath()), true, false, true, 10, 21, "macos"),
                List.of(task),
                true,
                true,
                8,
                0.2d,
                true,
                1L);
    }

    private static TaskRecord task(
            String taskPath,
            String kind,
            long duration,
            boolean actualExecution,
            boolean upToDate,
            boolean fromCache,
            VerificationRecord verification,
            TaskRecord.TestDetails tests) {
        return task(taskPath, kind, duration, actualExecution, upToDate, fromCache, verification, tests, "SUCCESS");
    }

    private static TaskRecord task(
            String taskPath,
            String kind,
            long duration,
            boolean actualExecution,
            boolean upToDate,
            boolean fromCache,
            VerificationRecord verification,
            TaskRecord.TestDetails tests,
            String outcome) {
        return new TaskRecord(taskPath, kind, outcome, duration, actualExecution, upToDate, fromCache, false, "/tmp/out", "/tmp/tests", verification, tests, null);
    }

    private static VerificationRecord verification(String domainId) {
        return new VerificationRecord(
                domainId,
                "OWNER",
                domainId + ".owner",
                "input",
                "snapshot",
                "LOCAL_EXECUTION",
                false,
                1,
                0,
                100L,
                new WorkloadRecord("DETERMINISTIC_SCENARIO", null, List.of("DETERMINISTIC_SCENARIO"), "DETERMINISTIC_SCENARIO", false, null, null, null));
    }

    private static TaskRecord.TestDetails testDetails(int total, int failed) {
        return new TaskRecord.TestDetails(
                total,
                failed,
                new WorkloadRecord("UNIT_TEST", total, null, null, false, null, null, null),
                "HEAVY_EVALUATION",
                List.of());
    }
}
