package com.ktome.build.testperf;

import com.ktome.build.testperf.records.TaskRecord;
import com.ktome.build.testperf.records.WorkloadRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class TestPerfRootCauseAnalyzer {
    public List<String> analyze(TaskRecord current, TaskRecord previous, boolean daemonReused, Double systemLoadAverage, int availableProcessors) {
        List<String> hints = new ArrayList<>();
        if (current.verification() != null && previous != null && previous.verification() != null) {
            if (!Objects.equals(current.verification().cacheStatus(), previous.verification().cacheStatus())) {
                hints.add("cache status changed");
            }
            if (!Objects.equals(current.verification().inputSnapshotHash(), previous.verification().inputSnapshotHash())) {
                hints.add("input snapshot changed");
            }
            if (!Objects.equals(artifactReuseSource(current), artifactReuseSource(previous))) {
                if (artifactReuseSource(current) != null || artifactReuseSource(previous) != null) {
                    hints.add("artifact reuse changed");
                }
            }
            if (workloadChanged(workloadCount(current), workloadCount(previous))) {
                hints.add("workload changed");
            }
        }
        if (current.tests() != null && previous != null && previous.tests() != null) {
            if (workloadChanged(workloadCount(current), workloadCount(previous))) {
                hints.add("workload changed");
            }
        }
        String slowTestHint = slowTestHint(current);
        if (slowTestHint != null) {
            hints.add(slowTestHint);
        }
        if (!daemonReused) {
            hints.add("cold daemon");
        }
        if (systemLoadAverage != null && availableProcessors > 0 && systemLoadAverage >= availableProcessors) {
            hints.add("machine load high");
        }
        if (hints.isEmpty()) {
            hints.add("no common pattern");
        }
        return List.copyOf(hints);
    }

    private static String slowTestHint(TaskRecord current) {
        if (current == null || current.tests() == null || current.tests().slowTestMethods() == null || current.tests().slowTestMethods().isEmpty()) {
            return null;
        }
        String methods =
                current.tests().slowTestMethods().stream()
                        .limit(3)
                        .map(method -> method.className() + "." + method.name() + "=" + method.durationMillis() + "ms")
                        .collect(Collectors.joining(", "));
        return "slow tests: " + methods;
    }

    private static Integer workloadCount(TaskRecord task) {
        WorkloadRecord workload = workload(task);
        return workload == null ? null : workload.workloadCount();
    }

    private static String artifactReuseSource(TaskRecord task) {
        WorkloadRecord workload = workload(task);
        return workload == null ? null : workload.artifactReuseSource();
    }

    private static WorkloadRecord workload(TaskRecord task) {
        if (task == null) {
            return null;
        }
        if (task.verification() != null) {
            return task.verification().workload();
        }
        if (task.tests() != null) {
            return task.tests().workload();
        }
        return null;
    }

    private static boolean workloadChanged(Integer current, Integer previous) {
        if (current == null || previous == null || previous == 0) {
            return false;
        }
        return Math.abs((current - previous) / (double) previous) >= 0.10d;
    }
}
