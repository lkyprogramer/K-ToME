package com.ktome.build.testperf.records;

import java.util.List;

public record WorkloadRecord(
        String workloadClass,
        Integer workloadCount,
        List<String> declaredWorkloadClasses,
        String selectedNodeWorkloadClass,
        Boolean reportOnly,
        String sourceArtifactDir,
        String artifactReuseSource,
        Long evaluationDurationMillis) {}
