package com.ktome.build.testperf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ktome.build.testperf.records.RunRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public final class TestPerfLaneManager {
    public static final String MODE_LOCAL_BASELINE = "local-baseline";
    public static final String MODE_CI_REPORT_ONLY = "ci-report-only";
    public static final String MODE_OFF = "off";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path storageDir;
    private final int historyLimit;
    private final TestPerfComparator comparator;
    private final TestPerfReporter reporter;

    public TestPerfLaneManager(String storageDir) {
        this(storageDir, 20, new TestPerfComparator(), new TestPerfReporter());
    }

    public TestPerfLaneManager(String storageDir, int historyLimit, TestPerfComparator comparator, TestPerfReporter reporter) {
        this.storageDir = Path.of(storageDir);
        this.historyLimit = historyLimit;
        this.comparator = comparator;
        this.reporter = reporter;
    }

    public WriteResult write(RunRecord runRecord, String mode) {
        if (runRecord.invocationContext().requestedLeafTaskPaths().isEmpty()) {
            return WriteResult.NO_MONITORED_TASKS;
        }
        if (MODE_OFF.equals(mode)) {
            return WriteResult.BYPASSED;
        }

        if (MODE_CI_REPORT_ONLY.equals(mode)) {
            return writeReportOnlySummary(runRecord);
        }

        Path laneDir = storageDir.resolve("lanes").resolve(runRecord.laneId());
        try {
            Files.createDirectories(laneDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create lane directory " + laneDir, exception);
        }

        Path lockPath = laneDir.resolve(".lock");
        try (FileChannel channel =
                        FileChannel.open(
                                lockPath,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE);
                FileLock ignored = channel.tryLock()) {
            if (ignored == null) {
                return WriteResult.LOCK_SKIPPED;
            }
            if (!runRecord.complete()) {
                writeJson(laneDir.resolve("current.incomplete.json"), runRecord);
                String markdown = reporter.renderIncomplete(runRecord);
                writeText(laneDir.resolve("reports/latest.incomplete.md"), markdown);
                return WriteResult.INCOMPLETE_BUILD;
            }

            Path currentPath = laneDir.resolve("current.json");
            Path previousPath = laneDir.resolve("previous.json");
            if (Files.exists(currentPath)) {
                Files.move(currentPath, previousPath, StandardCopyOption.REPLACE_EXISTING);
            }
            writeJson(currentPath, runRecord);
            Path historyDir = laneDir.resolve("history");
            Files.createDirectories(historyDir);
            writeJson(historyDir.resolve(runRecord.runId() + ".json"), runRecord);
            TestPerfComparator.ComparisonResult comparisonResult =
                    comparator.compare(
                            runRecord,
                            readHistoryRuns(historyDir, runRecord.runId(), runRecord.schemaVersion()));
            String markdown = reporter.renderComplete(runRecord, comparisonResult);
            Path reportsDir = laneDir.resolve("reports");
            writeText(reportsDir.resolve("latest.md"), markdown);
            writeText(reportsDir.resolve(runRecord.runId() + ".md"), markdown);
            cleanupOldFiles(historyDir, "*.json");
            cleanupOldFiles(reportsDir, "*.md", "latest.md", "latest.incomplete.md");
            System.out.print(reporter.renderConsoleSummary(runRecord, comparisonResult));
            return Files.exists(previousPath) ? WriteResult.UPDATED_BASELINE : WriteResult.BASELINE_ESTABLISHED;
        } catch (OverlappingFileLockException exception) {
            return WriteResult.LOCK_SKIPPED;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to update test perf lane " + laneDir, exception);
        }
    }

    public WriteResult write(RunRecord runRecord) {
        return write(runRecord, MODE_LOCAL_BASELINE);
    }

    public static String computeLaneId(int schemaVersion, RunRecord.InvocationContext context) {
        String canonical =
                "schemaVersion="
                        + schemaVersion
                        + "|"
                        + String.join("|", context.requestedLeafTaskPaths())
                        + "|buildCache="
                        + context.buildCacheEnabled()
                        + "|configurationCache="
                        + context.configurationCacheEnabled()
                        + "|parallel="
                        + context.parallelEnabled()
                        + "|maxWorkers="
                        + context.maxWorkers()
                        + "|java="
                        + context.javaMajorVersion()
                        + "|os="
                        + context.osFamily();
        return sha256(canonical);
    }

    public static String newRunId() {
        return Instant.now().toString().replace(":", "-");
    }

    private static void writeJson(Path path, RunRecord runRecord) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(runRecord), StandardCharsets.UTF_8);
    }

    private static RunRecord readJson(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, RunRecord.class);
        }
    }

    private WriteResult writeReportOnlySummary(RunRecord runRecord) {
        Path reportDir = storageDir.resolve("ci").resolve(runRecord.laneId()).resolve("reports");
        try {
            String markdown = reporter.renderCurrentRunSummary(runRecord);
            writeText(reportDir.resolve("latest.md"), markdown);
            writeText(reportDir.resolve(runRecord.runId() + ".md"), markdown);
            cleanupOldFiles(reportDir, "*.md", "latest.md");
            System.out.print(reporter.renderConsoleCurrentRunSummary(runRecord));
            return WriteResult.REPORT_ONLY_SUMMARY;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write test perf report-only summary " + reportDir, exception);
        }
    }

    private static List<RunRecord> readHistoryRuns(Path historyDir, String currentRunId, int expectedSchemaVersion)
            throws IOException {
        if (!Files.isDirectory(historyDir)) {
            return List.of();
        }
        try (var stream = Files.list(historyDir)) {
            return stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                    .filter(path -> !path.getFileName().toString().equals(currentRunId + ".json"))
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                    .map(
                            path -> {
                                try {
                                    RunRecord runRecord = readJson(path);
                                    return runRecord.schemaVersion() == expectedSchemaVersion ? runRecord : null;
                                } catch (IOException exception) {
                                    throw new IllegalStateException("Unable to read test perf history " + path, exception);
                                }
                            })
                    .filter(runRecord -> runRecord != null)
                    .toList();
        }
    }

    private static void writeText(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void cleanupOldFiles(Path directory, String glob, String... preserveNames) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        List<String> preserved = List.of(preserveNames);
        try (var stream = Files.list(directory)) {
            List<Path> files =
                    stream.filter(path -> Files.isRegularFile(path))
                            .filter(path -> matchesSuffix(path, glob))
                            .filter(path -> !preserved.contains(path.getFileName().toString()))
                            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                            .toList();
            int overflow = files.size() - historyLimit;
            for (int index = 0; index < overflow; index++) {
                Files.deleteIfExists(files.get(index));
            }
        }
    }

    private static boolean matchesSuffix(Path path, String glob) {
        if ("*.json".equals(glob)) {
            return path.getFileName().toString().endsWith(".json");
        }
        if ("*.md".equals(glob)) {
            return path.getFileName().toString().endsWith(".md");
        }
        return true;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public enum WriteResult {
        NO_MONITORED_TASKS,
        LOCK_SKIPPED,
        BASELINE_ESTABLISHED,
        UPDATED_BASELINE,
        INCOMPLETE_BUILD,
        REPORT_ONLY_SUMMARY,
        BYPASSED,
    }
}
