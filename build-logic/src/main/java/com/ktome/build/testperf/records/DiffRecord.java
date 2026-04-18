package com.ktome.build.testperf.records;

import java.util.List;

public record DiffRecord(
        String taskPath,
        String comparableKey,
        String taskBand,
        String classification,
        Long previousDurationMillis,
        Long currentDurationMillis,
        Long absoluteDeltaMillis,
        Double relativeDeltaPercent,
        List<String> hints,
        boolean comparable) {}
