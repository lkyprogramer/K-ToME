package com.ktome.build.testperf;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ktome.build.testperf.records.TaskRecord;
import com.ktome.build.testperf.records.VerificationRecord;
import com.ktome.build.testperf.records.WorkloadRecord;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestPerfRootCauseAnalyzerTest {
    private final TestPerfRootCauseAnalyzer analyzer = new TestPerfRootCauseAnalyzer();

    @Test
    void reportsColdDaemonAndCacheFlipHints() {
        TaskRecord previous =
                new TaskRecord(
                        ":tools:hiddenContentHarness",
                        "VERIFICATION_TASK",
                        "SUCCESS",
                        100L,
                        true,
                        false,
                        false,
                        false,
                        "/tmp/out",
                        null,
                        verification("LOCAL_EXECUTION", "snapshot-a", null, 1),
                        null,
                        null);
        TaskRecord current =
                new TaskRecord(
                        ":tools:hiddenContentHarness",
                        "VERIFICATION_TASK",
                        "SUCCESS",
                        120L,
                        true,
                        false,
                        false,
                        false,
                        "/tmp/out",
                        null,
                        verification("FROM_CACHE", "snapshot-b", "/tmp/reuse", 2),
                        null,
                        null);

        List<String> hints = analyzer.analyze(current, previous, false, 12.0d, 8);
        assertTrue(hints.contains("cache status changed"));
        assertTrue(hints.contains("input snapshot changed"));
        assertTrue(hints.contains("artifact reuse changed"));
        assertTrue(hints.contains("workload changed"));
        assertTrue(hints.contains("cold daemon"));
        assertTrue(hints.contains("machine load high"));
    }

    @Test
    void toleratesMissingWorkloadRecord() {
        TaskRecord previous =
                new TaskRecord(
                        ":tools:hiddenContentHarness",
                        "VERIFICATION_TASK",
                        "SUCCESS",
                        100L,
                        true,
                        false,
                        false,
                        false,
                        "/tmp/out",
                        null,
                        new VerificationRecord(
                                "hidden",
                                "OWNER",
                                "hidden.owner",
                                "snapshot-a",
                                "snapshot-a",
                                "LOCAL_EXECUTION",
                                false,
                                1,
                                0,
                                100L,
                                null),
                        null,
                        null);
        TaskRecord current =
                new TaskRecord(
                        ":tools:hiddenContentHarness",
                        "VERIFICATION_TASK",
                        "SUCCESS",
                        120L,
                        true,
                        false,
                        false,
                        false,
                        "/tmp/out",
                        null,
                        new VerificationRecord(
                                "hidden",
                                "OWNER",
                                "hidden.owner",
                                "snapshot-a",
                                "snapshot-a",
                                "LOCAL_EXECUTION",
                                false,
                                1,
                                0,
                                120L,
                                null),
                        null,
                        null);

        List<String> hints = analyzer.analyze(current, previous, true, 0.2d, 8);
        assertTrue(hints.contains("no common pattern"));
    }

    @Test
    void reportsSlowTestMethodHintsForTestTasks() {
        TaskRecord current =
                new TaskRecord(
                        ":tools:whiteBoxLoot",
                        "TEST",
                        "SUCCESS",
                        505_240L,
                        true,
                        false,
                        false,
                        false,
                        "/tmp/out",
                        "/tmp/tests",
                        null,
                        new TaskRecord.TestDetails(
                                3,
                                0,
                                new WorkloadRecord("UNIT_TEST", 3, null, null, false, null, null, null),
                                "HEAVY_EVALUATION",
                                List.of(
                                        new TaskRecord.TestDetails.SlowTestMethod(
                                                "com.ktome.tools.loot.WhiteBoxLootRunnerTest",
                                                "slow owner assertion",
                                                239_219L),
                                        new TaskRecord.TestDetails.SlowTestMethod(
                                                "com.ktome.tools.loot.WhiteBoxLootRunnerTest",
                                                "slow strict baseline",
                                                264_547L))),
                        null);

        List<String> hints = analyzer.analyze(current, null, true, 0.2d, 8);

        assertTrue(hints.stream().anyMatch(hint -> hint.contains("slow tests:")));
        assertTrue(hints.stream().anyMatch(hint -> hint.contains("slow strict baseline=264547ms")));
    }

    private static VerificationRecord verification(String cacheStatus, String snapshotHash, String artifactReuseSource, Integer workloadCount) {
        return new VerificationRecord(
                "hidden",
                "OWNER",
                "hidden.owner",
                snapshotHash,
                snapshotHash,
                cacheStatus,
                false,
                1,
                0,
                100L,
                new WorkloadRecord("DETERMINISTIC_SCENARIO", workloadCount, List.of("DETERMINISTIC_SCENARIO"), "DETERMINISTIC_SCENARIO", false, null, artifactReuseSource, null));
    }
}
