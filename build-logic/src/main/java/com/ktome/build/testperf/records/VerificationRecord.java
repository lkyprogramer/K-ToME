package com.ktome.build.testperf.records;

public record VerificationRecord(
        String domainId,
        String tier,
        String nodeId,
        String inputSnapshotHash,
        String snapshotHash,
        String cacheStatus,
        boolean reportOnly,
        Integer totalTests,
        Integer failedTests,
        Long durationMillis,
        WorkloadRecord workload) {}
