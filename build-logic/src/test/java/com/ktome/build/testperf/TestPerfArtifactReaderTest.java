package com.ktome.build.testperf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ktome.build.testperf.records.TaskRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestPerfArtifactReaderTest {
    private final TestPerfArtifactReader reader = new TestPerfArtifactReader();

    @TempDir
    Path tempDir;

    @Test
    void verificationArtifactsOverrideTaskInputFallback() throws Exception {
        Path outputDir = tempDir.resolve("verification");
        Files.createDirectories(outputDir);
        Files.writeString(
                outputDir.resolve("summary.json"),
                """
                {
                  "domainId": "contractLint",
                  "tier": "PREFLIGHT",
                  "snapshotHash": "artifact-snapshot",
                  "cacheStatus": "REPORT_ONLY_REBUILD",
                  "nodeId": "contractLint.staticGraph",
                  "totalTests": 7,
                  "failedTests": 0,
                  "durationMillis": 898,
                  "reportOnly": true
                }
                """);
        Files.writeString(
                outputDir.resolve("metadata.json"),
                """
                {
                  "domainId": "contractLint",
                  "selectedTier": "PREFLIGHT",
                  "nodeId": "contractLint.staticGraph",
                  "workloadClass": "STATIC_GRAPH",
                  "declaredWorkloadClasses": ["STATIC_GRAPH"],
                  "selectedNodeWorkloadClass": "STATIC_GRAPH",
                  "sourceArtifactDir": "/tmp/source"
                }
                """);

        var verification =
                reader.readVerification(
                        new TestPerfTaskMetadataRegistry.TaskMetadata(
                                ":tools:verifyContractLintPreflightReport",
                                "VERIFICATION_REPORT_TASK",
                                outputDir.toString(),
                                null,
                                "fallbackDomain",
                                "OWNER",
                                "fallback.node",
                                "input-snapshot",
                                null));

        assertEquals("contractLint", verification.domainId());
        assertEquals("PREFLIGHT", verification.tier());
        assertEquals("contractLint.staticGraph", verification.nodeId());
        assertEquals("input-snapshot", verification.inputSnapshotHash());
        assertEquals("artifact-snapshot", verification.snapshotHash());
        assertEquals("REPORT_ONLY_REBUILD", verification.cacheStatus());
        assertEquals(true, verification.reportOnly());
        assertEquals(7, verification.totalTests());
        assertEquals("STATIC_GRAPH", verification.workload().workloadClass());
        assertEquals("/tmp/source", verification.workload().sourceArtifactDir());
    }

    @Test
    void verificationReaderFallsBackWhenOptionalArtifactFieldsAreMissing() throws Exception {
        Path outputDir = tempDir.resolve("fallback");
        Files.createDirectories(outputDir);
        Files.writeString(
                outputDir.resolve("summary.json"),
                """
                {
                  "domainId": "hidden",
                  "tier": "OWNER",
                  "nodeId": "hidden.owner",
                  "snapshotHash": "snapshot-hidden",
                  "cacheStatus": "LOCAL_EXECUTION",
                  "totalTests": 2,
                  "failedTests": 0,
                  "durationMillis": 1200,
                  "reportOnly": false
                }
                """);
        Files.writeString(
                outputDir.resolve("metadata.json"),
                """
                {
                  "domainId": "hidden",
                  "selectedTier": "OWNER",
                  "nodeId": "hidden.owner",
                  "workloadClass": "STATIC_GRAPH"
                }
                """);

        var verification =
                reader.readVerification(
                        new TestPerfTaskMetadataRegistry.TaskMetadata(
                                ":tools:hiddenContentHarness",
                                "VERIFICATION_TASK",
                                outputDir.toString(),
                                null,
                                "hidden",
                                "OWNER",
                                "hidden.owner",
                                "input-snapshot",
                                null));

        assertEquals("hidden", verification.domainId());
        assertEquals("OWNER", verification.tier());
        assertEquals("hidden.owner", verification.nodeId());
        assertEquals("input-snapshot", verification.inputSnapshotHash());
        assertNull(verification.workload().declaredWorkloadClasses());
        assertNull(verification.workload().selectedNodeWorkloadClass());
    }

    @Test
    void junitXmlResultsProduceUnitTestTotals() throws Exception {
        Path resultsDir = tempDir.resolve("test-results");
        Files.createDirectories(resultsDir);
        Files.writeString(
                resultsDir.resolve("TEST-demo.xml"),
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="demo" tests="3" failures="1" errors="0"/>
                """);
        Files.writeString(
                resultsDir.resolve("TEST-extra.xml"),
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="extra" tests="2" failures="0" errors="1"/>
                """);

        TaskRecord.TestDetails tests =
                reader.readTest(
                        new TestPerfTaskMetadataRegistry.TaskMetadata(
                                ":tools:whiteBoxLoot",
                                "TEST",
                                tempDir.toString(),
                                resultsDir.toString(),
                                null,
                                null,
                                null,
                                null,
                                "HEAVY_EVALUATION"));

        assertNotNull(tests);
        assertEquals(5, tests.total());
        assertEquals(2, tests.failed());
        assertEquals("UNIT_TEST", tests.workload().workloadClass());
        assertEquals(5, tests.workload().workloadCount());
        assertEquals("HEAVY_EVALUATION", tests.bandHint());
    }
}
