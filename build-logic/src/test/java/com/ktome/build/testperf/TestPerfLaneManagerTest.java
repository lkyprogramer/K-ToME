package com.ktome.build.testperf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.ktome.build.testperf.records.RunRecord;
import com.ktome.build.testperf.records.TaskRecord;
import com.ktome.build.testperf.records.VerificationRecord;
import com.ktome.build.testperf.records.WorkloadRecord;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestPerfLaneManagerTest {
    private static final Gson GSON = new Gson();

    @TempDir
    Path tempDir;

    @Test
    void rotatesCurrentRunIntoPreviousOnSecondSuccessfulWrite() throws IOException {
        TestPerfLaneManager laneManager = new TestPerfLaneManager(tempDir.toString());
        RunRecord.InvocationContext invocationContext =
                new RunRecord.InvocationContext(List.of("hiddenContentHarness"), List.of(":tools:hiddenContentHarness"), true, false, false, 1, 21, "linux");
        String laneId = TestPerfLaneManager.computeLaneId(1, invocationContext);

        assertEquals(
                TestPerfLaneManager.WriteResult.BASELINE_ESTABLISHED,
                laneManager.write(runRecord("run-1", laneId, invocationContext, true)));
        assertEquals(
                TestPerfLaneManager.WriteResult.UPDATED_BASELINE,
                laneManager.write(runRecord("run-2", laneId, invocationContext, true)));

        Path laneDir = tempDir.resolve("lanes").resolve(laneId);
        RunRecord current = GSON.fromJson(Files.readString(laneDir.resolve("current.json")), RunRecord.class);
        RunRecord previous = GSON.fromJson(Files.readString(laneDir.resolve("previous.json")), RunRecord.class);
        assertEquals("run-2", current.runId());
        assertEquals("run-1", previous.runId());
        assertTrue(Files.exists(laneDir.resolve("reports/latest.md")));
    }

    @Test
    void incompleteWriteDoesNotReplaceSuccessfulBaseline() throws IOException {
        TestPerfLaneManager laneManager = new TestPerfLaneManager(tempDir.toString());
        RunRecord.InvocationContext invocationContext =
                new RunRecord.InvocationContext(List.of("hiddenContentHarness"), List.of(":tools:hiddenContentHarness"), true, false, false, 1, 21, "linux");
        String laneId = TestPerfLaneManager.computeLaneId(1, invocationContext);

        laneManager.write(runRecord("run-1", laneId, invocationContext, true));
        assertEquals(
                TestPerfLaneManager.WriteResult.INCOMPLETE_BUILD,
                laneManager.write(runRecord("run-2", laneId, invocationContext, false)));

        Path laneDir = tempDir.resolve("lanes").resolve(laneId);
        assertTrue(Files.exists(laneDir.resolve("current.json")));
        assertTrue(Files.exists(laneDir.resolve("current.incomplete.json")));
        assertTrue(Files.exists(laneDir.resolve("reports/latest.incomplete.md")));
        RunRecord current = GSON.fromJson(Files.readString(laneDir.resolve("current.json")), RunRecord.class);
        assertEquals("run-1", current.runId());
    }

    @Test
    void skipsWriteWhenLaneLockIsAlreadyHeld() throws IOException {
        TestPerfLaneManager laneManager = new TestPerfLaneManager(tempDir.toString());
        RunRecord.InvocationContext invocationContext =
                new RunRecord.InvocationContext(List.of("hiddenContentHarness"), List.of(":tools:hiddenContentHarness"), true, false, false, 1, 21, "linux");
        String laneId = TestPerfLaneManager.computeLaneId(1, invocationContext);
        Path laneDir = tempDir.resolve("lanes").resolve(laneId);
        Files.createDirectories(laneDir);
        Path lockPath = laneDir.resolve(".lock");

        try (FileChannel channel =
                        FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock ignored = channel.lock()) {
            assertEquals(
                    TestPerfLaneManager.WriteResult.LOCK_SKIPPED,
                    laneManager.write(runRecord("run-1", laneId, invocationContext, true)));
        }

        assertFalse(Files.exists(laneDir.resolve("current.json")));
    }

    @Test
    void schemaVersionChangeCreatesDifferentLaneAndPreventsBaselineReuse() {
        RunRecord.InvocationContext invocationContext =
                new RunRecord.InvocationContext(List.of("hiddenContentHarness"), List.of(":tools:hiddenContentHarness"), true, false, false, 1, 21, "linux");

        assertFalse(TestPerfLaneManager.computeLaneId(1, invocationContext).equals(TestPerfLaneManager.computeLaneId(2, invocationContext)));
    }

    private static RunRecord runRecord(
            String runId,
            String laneId,
            RunRecord.InvocationContext invocationContext,
            boolean complete) {
        return new RunRecord(
                2,
                runId,
                laneId,
                invocationContext,
                List.of(
                        new TaskRecord(
                                ":tools:hiddenContentHarness",
                                "VERIFICATION_TASK",
                                complete ? "SUCCESS" : "FAILED",
                                1234L,
                                complete,
                                false,
                                false,
                                false,
                                "/tmp/out",
                                null,
                                new VerificationRecord(
                                        "hidden",
                                        "OWNER",
                                        "hidden.owner",
                                        "snapshot",
                                        "snapshot",
                                        "LOCAL_EXECUTION",
                                        false,
                                        1,
                                        0,
                                        1234L,
                                        new WorkloadRecord(
                                                "STATIC_GRAPH",
                                                null,
                                                List.of("STATIC_GRAPH"),
                                                "STATIC_GRAPH",
                                                false,
                                                null,
                                                null,
                                                null)),
                                null,
                                complete ? null : "boom")),
                complete,
                true,
                8,
                0.2d,
                complete,
                1L);
    }
}
