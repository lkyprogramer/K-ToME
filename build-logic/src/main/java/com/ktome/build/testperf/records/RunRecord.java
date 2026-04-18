package com.ktome.build.testperf.records;

import java.util.List;

public record RunRecord(
        int schemaVersion,
        String runId,
        String laneId,
        InvocationContext invocationContext,
        List<TaskRecord> tasks,
        boolean complete,
        boolean daemonReused,
        int availableProcessors,
        Double systemLoadAverage,
        boolean buildSucceeded,
        long generatedAtEpochMillis) {
    public record InvocationContext(
            List<String> requestedTaskPaths,
            List<String> requestedLeafTaskPaths,
            boolean buildCacheEnabled,
            boolean configurationCacheEnabled,
            boolean parallelEnabled,
            int maxWorkers,
            int javaMajorVersion,
            String osFamily) {}
}
