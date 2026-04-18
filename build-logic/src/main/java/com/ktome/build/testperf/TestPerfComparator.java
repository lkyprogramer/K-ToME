package com.ktome.build.testperf;

import com.ktome.build.testperf.records.DiffRecord;
import com.ktome.build.testperf.records.RunRecord;
import com.ktome.build.testperf.records.TaskRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TestPerfComparator {
    private final TestPerfRootCauseAnalyzer rootCauseAnalyzer = new TestPerfRootCauseAnalyzer();

    public ComparisonResult compare(RunRecord current, List<RunRecord> baselineCandidates) {
        List<RunRecord> candidates = baselineCandidates == null ? List.of() : baselineCandidates;
        List<Map<String, TaskRecord>> candidateMaps = candidates.stream().map(this::indexByKey).toList();
        List<DiffRecord> diffs = new ArrayList<>();
        String previousRunId = candidates.isEmpty() ? null : candidates.get(0).runId();
        String previousComparableRunId = null;
        int previousComparableIndex = Integer.MAX_VALUE;
        for (TaskRecord task : current.tasks()) {
            String comparableKey = comparableKey(task);
            String taskBand = taskBand(task);
            BaselineMatch latestKeyMatch = null;
            BaselineMatch latestObservationMatch = null;
            BaselineMatch latestComparableMatch = null;
            for (int index = 0; index < candidateMaps.size(); index++) {
                TaskRecord candidateTask = candidateMaps.get(index).get(comparableKey);
                if (candidateTask == null) {
                    continue;
                }
                BaselineMatch currentMatch = new BaselineMatch(candidates.get(index), candidateTask, index);
                if (latestKeyMatch == null) {
                    latestKeyMatch = currentMatch;
                }
                if (latestObservationMatch == null && isObservedPair(task, candidateTask)) {
                    latestObservationMatch = currentMatch;
                }
                if (isFormalComparablePair(task, candidateTask)) {
                    latestComparableMatch = currentMatch;
                    break;
                }
            }
            if (latestComparableMatch == null && latestKeyMatch == null) {
                diffs.add(new DiffRecord(task.taskPath(), comparableKey, taskBand, "NO_BASELINE", null, task.durationMillis(), null, null, List.of("no prior comparable run"), false));
                continue;
            }

            if (latestComparableMatch == null && latestObservationMatch != null) {
                TaskRecord previousTask = latestObservationMatch.task();
                long absoluteDeltaMillis = task.durationMillis() - previousTask.durationMillis();
                double relativeDeltaPercent = previousTask.durationMillis() == 0 ? 0.0d : (absoluteDeltaMillis * 100.0d) / previousTask.durationMillis();
                List<String> hints =
                        rootCauseAnalyzer.analyze(task, previousTask, current.daemonReused(), current.systemLoadAverage(), current.availableProcessors());
                diffs.add(
                        new DiffRecord(
                                task.taskPath(),
                                comparableKey,
                                taskBand,
                                "INFO",
                                previousTask.durationMillis(),
                                task.durationMillis(),
                                absoluteDeltaMillis,
                                relativeDeltaPercent,
                                hints,
                                false));
                continue;
            }

            if (latestComparableMatch == null) {
                diffs.add(
                        new DiffRecord(
                                task.taskPath(),
                                comparableKey,
                                taskBand,
                                "INCOMPARABLE",
                                latestKeyMatch.task().durationMillis(),
                                task.durationMillis(),
                                null,
                                null,
                                List.of("one side did not produce a comparable sample"),
                                false));
                continue;
            }

            if (latestComparableMatch.index() < previousComparableIndex) {
                previousComparableIndex = latestComparableMatch.index();
                previousComparableRunId = latestComparableMatch.run().runId();
            }
            TaskRecord previousTask = latestComparableMatch.task();
            long absoluteDeltaMillis = task.durationMillis() - previousTask.durationMillis();
            double relativeDeltaPercent = previousTask.durationMillis() == 0 ? 0.0d : (absoluteDeltaMillis * 100.0d) / previousTask.durationMillis();
            List<String> hints =
                    rootCauseAnalyzer.analyze(task, previousTask, current.daemonReused(), current.systemLoadAverage(), current.availableProcessors());
            String classification = classify(taskBand, absoluteDeltaMillis, relativeDeltaPercent, hints);
            diffs.add(
                    new DiffRecord(
                            task.taskPath(),
                            comparableKey,
                            taskBand,
                            classification,
                            previousTask.durationMillis(),
                            task.durationMillis(),
                            absoluteDeltaMillis,
                            relativeDeltaPercent,
                            hints,
                            true));
        }

        return new ComparisonResult(previousRunId, previousComparableRunId, List.copyOf(diffs));
    }

    public String comparableKey(TaskRecord task) {
        if (task.verification() != null) {
            String nodeId = task.verification().nodeId() == null || task.verification().nodeId().isBlank() ? "default" : task.verification().nodeId();
            return task.kind()
                    + "|"
                    + task.taskPath()
                    + "|"
                    + task.verification().domainId()
                    + "|"
                    + task.verification().tier()
                    + "|"
                    + nodeId;
        }
        return task.kind() + "|" + task.taskPath();
    }

    public String taskBand(TaskRecord task) {
        if (task.verification() != null && task.verification().reportOnly()) {
            return TestPerfTaskBands.LIGHT_AGGREGATE;
        }
        if (task.verification() != null) {
            return TestPerfTaskBands.HEAVY_PRODUCER;
        }
        if (task.tests() != null && task.tests().bandHint() != null && !task.tests().bandHint().isBlank()) {
            return task.tests().bandHint();
        }
        return TestPerfTaskBands.SMALL_TEST;
    }

    private static String classify(String taskBand, long absoluteDeltaMillis, double relativeDeltaPercent, List<String> hints) {
        boolean stableSignalOnly = hints.stream().anyMatch(hint -> !"no common pattern".equals(hint));
        boolean workloadChanged = hints.contains("workload changed");
        if (TestPerfTaskBands.isHeavy(taskBand)) {
            if (absoluteDeltaMillis >= 30_000L && relativeDeltaPercent >= 50.0d) {
                return "ALERT";
            }
            if (absoluteDeltaMillis >= 5_000L && relativeDeltaPercent >= 25.0d) {
                return "WARN";
            }
            if (workloadChanged) {
                return "WARN";
            }
            return stableSignalOnly ? "INFO" : "NORMAL";
        }
        if (TestPerfTaskBands.LIGHT_AGGREGATE.equals(taskBand)) {
            if (absoluteDeltaMillis >= 10_000L && relativeDeltaPercent >= 100.0d) {
                return "WARN";
            }
            return stableSignalOnly ? "INFO" : "NORMAL";
        }
        return stableSignalOnly ? "INFO" : "NORMAL";
    }

    private Map<String, TaskRecord> indexByKey(RunRecord runRecord) {
        Map<String, TaskRecord> tasksByKey = new HashMap<>();
        for (TaskRecord task : runRecord.tasks()) {
            tasksByKey.put(comparableKey(task), task);
        }
        return tasksByKey;
    }

    private static boolean isFormalComparablePair(TaskRecord current, TaskRecord baseline) {
        return isActualExecutionSample(current) && isActualExecutionSample(baseline);
    }

    private static boolean isObservedPair(TaskRecord current, TaskRecord baseline) {
        return isObservedSample(current) && isObservedSample(baseline);
    }

    private static boolean isActualExecutionSample(TaskRecord task) {
        return "SUCCESS".equals(task.outcome()) && task.actualExecution();
    }

    private static boolean isObservedSample(TaskRecord task) {
        return "SUCCESS".equals(task.outcome()) && (task.actualExecution() || task.upToDate() || task.fromCache());
    }

    public record ComparisonResult(String previousRunId, String previousComparableRunId, List<DiffRecord> diffs) {}

    private record BaselineMatch(RunRecord run, TaskRecord task, int index) {}
}
