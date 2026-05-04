package com.ktome.build.testperf.records;

import java.util.List;

public record TaskRecord(
        String taskPath,
        String kind,
        String outcome,
        long durationMillis,
        boolean actualExecution,
        boolean upToDate,
        boolean fromCache,
        boolean incremental,
        String outputDir,
        String testResultsDir,
        VerificationRecord verification,
        TestDetails tests,
        String failureMessage) {
    public record TestDetails(
            Integer total,
            Integer failed,
            WorkloadRecord workload,
            String bandHint,
            List<SlowTestMethod> slowTestMethods) {
        public record SlowTestMethod(
                String className,
                String name,
                long durationMillis) {}
    }
}
