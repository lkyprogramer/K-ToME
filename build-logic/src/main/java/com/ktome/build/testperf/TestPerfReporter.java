package com.ktome.build.testperf;

import com.ktome.build.testperf.records.DiffRecord;
import com.ktome.build.testperf.records.RunRecord;
import java.util.List;
import java.util.Locale;

public final class TestPerfReporter {
    public String renderComplete(RunRecord runRecord, TestPerfComparator.ComparisonResult comparisonResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Test Task Performance Report").append(System.lineSeparator()).append(System.lineSeparator());
        appendInvocationContext(
                builder,
                runRecord,
                List.of(
                        entry("previousRunId", comparisonResult.previousRunId()),
                        entry("previousComparableRunId", comparisonResult.previousComparableRunId()),
                        entry("daemonReused", runRecord.daemonReused())));

        renderSection(
                builder,
                "Comparable Tasks",
                comparisonResult.diffs().stream()
                        .filter(diff -> !"NO_BASELINE".equals(diff.classification()) && !"INCOMPARABLE".equals(diff.classification()))
                        .toList());
        renderSection(builder, "Warnings / Alerts", comparisonResult.diffs().stream().filter(diff -> "WARN".equals(diff.classification()) || "ALERT".equals(diff.classification())).toList());
        renderSection(builder, "Cache And Artifact Reuse Signals", comparisonResult.diffs().stream().filter(diff -> diff.hints().stream().anyMatch(hint -> hint.contains("cache") || hint.contains("artifact"))).toList());
        renderSection(
                builder,
                "Non-comparable Samples",
                comparisonResult.diffs().stream()
                        .filter(diff -> "NO_BASELINE".equals(diff.classification()) || "INCOMPARABLE".equals(diff.classification()))
                        .toList());
        renderSection(
                builder,
                "Heavy Task Comparison Summary",
                comparisonResult.diffs().stream()
                        .filter(diff -> TestPerfTaskBands.isHeavy(diff.taskBand()))
                        .toList());
        return builder.toString();
    }

    public String renderIncomplete(RunRecord runRecord) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Test Task Performance Report").append(System.lineSeparator()).append(System.lineSeparator());
        appendInvocationContext(builder, runRecord, List.of(entry("status", "INCOMPLETE")), false);
        builder.append("## Non-comparable Samples").append(System.lineSeparator());
        runRecord.tasks().forEach(task -> builder.append("- `").append(task.taskPath()).append("` | ").append(task.outcome()).append(System.lineSeparator()));
        return builder.toString();
    }

    public String renderCurrentRunSummary(RunRecord runRecord) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Test Task Performance Report").append(System.lineSeparator()).append(System.lineSeparator());
        appendInvocationContext(
                builder,
                runRecord,
                List.of(entry("mode", "ci-report-only"), entry("daemonReused", runRecord.daemonReused())));
        builder.append("## Current Run Tasks").append(System.lineSeparator());
        if (runRecord.tasks().isEmpty()) {
            builder.append("- none").append(System.lineSeparator());
            return builder.toString();
        }
        for (var task : runRecord.tasks()) {
            builder.append("- `").append(task.taskPath()).append("`");
            builder.append(" | `").append(task.outcome()).append('`');
            builder.append(" | `").append(task.durationMillis()).append("ms`");
            builder.append(" | `").append(task.kind()).append('`');
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    public String renderConsoleSummary(RunRecord runRecord, TestPerfComparator.ComparisonResult comparisonResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("[testperf] lane=").append(runRecord.laneId());
        builder.append(" previous=").append(comparisonResult.previousRunId() == null ? "none" : comparisonResult.previousRunId());
        builder.append(" previousComparable=")
                .append(comparisonResult.previousComparableRunId() == null ? "none" : comparisonResult.previousComparableRunId());
        builder.append(System.lineSeparator());
        comparisonResult
                .diffs()
                .forEach(
                        diff ->
                                builder.append("[testperf] ")
                                        .append(diff.taskPath())
                                        .append(" -> ")
                                        .append(diff.classification())
                                        .append(" (")
                                        .append(diff.taskBand())
                                        .append(")")
                                        .append(System.lineSeparator()));
        return builder.toString();
    }

    public String renderConsoleCurrentRunSummary(RunRecord runRecord) {
        StringBuilder builder = new StringBuilder();
        builder.append("[testperf] lane=").append(runRecord.laneId()).append(" mode=ci-report-only");
        builder.append(System.lineSeparator());
        runRecord
                .tasks()
                .forEach(
                        task ->
                                builder.append("[testperf] ")
                                        .append(task.taskPath())
                                        .append(" -> ")
                                        .append(task.outcome())
                                        .append(" (")
                                        .append(task.kind())
                                        .append(")")
                                        .append(System.lineSeparator()));
        return builder.toString();
    }

    private static void renderSection(StringBuilder builder, String title, List<DiffRecord> diffs) {
        builder.append("## ").append(title).append(System.lineSeparator());
        if (diffs.isEmpty()) {
            builder.append("- none").append(System.lineSeparator()).append(System.lineSeparator());
            return;
        }
        for (DiffRecord diff : diffs) {
            builder.append("- `").append(diff.taskPath()).append("`");
            builder.append(" | `").append(diff.classification()).append('`');
            builder.append(" | `").append(diff.taskBand()).append('`');
            if (diff.previousDurationMillis() != null && diff.currentDurationMillis() != null && diff.absoluteDeltaMillis() != null) {
                builder.append(" | prev=`").append(diff.previousDurationMillis()).append("ms`");
                builder.append(" current=`").append(diff.currentDurationMillis()).append("ms`");
                builder.append(" delta=`").append(diff.absoluteDeltaMillis()).append("ms`");
                if (diff.relativeDeltaPercent() != null) {
                    builder.append(" rel=`").append(String.format(Locale.ROOT, "%.1f%%", diff.relativeDeltaPercent())).append('`');
                }
            }
            builder.append(System.lineSeparator());
            builder.append("  - hints: ").append(String.join(", ", diff.hints())).append(System.lineSeparator());
        }
        builder.append(System.lineSeparator());
    }

    private static void appendInvocationContext(
            StringBuilder builder,
            RunRecord runRecord,
            List<ReportEntry> extraEntries) {
        appendInvocationContext(builder, runRecord, extraEntries, true);
    }

    private static void appendInvocationContext(
            StringBuilder builder,
            RunRecord runRecord,
            List<ReportEntry> extraEntries,
            boolean includeRequestedLeafTaskPaths) {
        builder.append("## Invocation Context").append(System.lineSeparator());
        for (ReportEntry entry : extraEntries) {
            appendEntry(builder, entry.label(), entry.value());
        }
        appendEntry(builder, "runId", runRecord.runId());
        appendEntry(builder, "laneId", runRecord.laneId());
        appendEntry(builder, "requestedTaskPaths", String.join(", ", runRecord.invocationContext().requestedTaskPaths()));
        if (includeRequestedLeafTaskPaths) {
            appendEntry(
                    builder,
                    "requestedLeafTaskPaths",
                    String.join(", ", runRecord.invocationContext().requestedLeafTaskPaths()));
        }
        builder.append(System.lineSeparator());
    }

    private static void appendEntry(StringBuilder builder, String label, Object value) {
        builder.append("- ").append(label).append(": `").append(value == null ? "none" : value).append('`').append(System.lineSeparator());
    }

    private static ReportEntry entry(String label, Object value) {
        return new ReportEntry(label, value);
    }

    private record ReportEntry(String label, Object value) {}
}
