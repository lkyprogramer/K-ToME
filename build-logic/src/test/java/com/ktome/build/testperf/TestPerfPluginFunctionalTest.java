package com.ktome.build.testperf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.ktome.build.testperf.records.RunRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestPerfPluginFunctionalTest {
    private static final Gson GSON = new Gson();

    @TempDir
    Path tempDir;

    @Test
    void rotatesCurrentRunIntoPreviousForSameLane() throws IOException {
        writeJavaProject(false);

        BuildResult first = runner().withArguments("demoTask", "-q").build();
        BuildResult second = runner().withArguments("demoTask", "-q").build();

        assertTrue(first.getOutput().contains("baseline established"));
        Path laneDir = singleLaneDir();
        RunRecord current = readRunRecord(laneDir.resolve("current.json"));
        RunRecord previous = readRunRecord(laneDir.resolve("previous.json"));
        assertEquals(2, current.schemaVersion());
        assertEquals("VERIFICATION_TASK", current.tasks().get(0).kind());
        assertTrue(List.of("SUCCESS", "UP_TO_DATE").contains(current.tasks().get(0).outcome()));
        assertEquals("demo", current.tasks().get(0).verification().domainId());
        assertEquals("OWNER", current.tasks().get(0).verification().tier());
        assertEquals(previous.laneId(), current.laneId());
        assertNotEquals(previous.runId(), current.runId());
        assertTrue(
                List.of(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE)
                        .contains(second.task(":demoTask").getOutcome()));
        assertTrue(Files.exists(laneDir.resolve("reports/latest.md")));
        String latestMarkdown = Files.readString(laneDir.resolve("reports/latest.md"));
        assertTrue(latestMarkdown.contains("Comparable Tasks"));
        assertTrue(latestMarkdown.contains("previousComparableRunId"));
        assertTrue(latestMarkdown.contains("Heavy Task Comparison Summary"));
        assertTrue(latestMarkdown.contains("requestedTaskPaths"));
        assertFalse(latestMarkdown.contains("## Comparable Tasks" + System.lineSeparator() + "- none"));
    }

    @Test
    void createsDifferentLaneDirectoriesForDifferentLeafSets() throws IOException {
        writeJavaProject(false);

        runner().withArguments("demoTask", "-q").build();
        runner().withArguments("demoTask", "secondTask", "-q").build();

        try (Stream<Path> stream = Files.list(tempDir.resolve(".gradle/test-perf/lanes"))) {
            assertEquals(2, stream.count());
        }
    }

    @Test
    void mixedAliasAndLeafInvocationDoesNotDuplicateLeafSamples() throws IOException {
        writeJavaProject(false);

        runner().withArguments("combo", "demoTask", "-q").build();

        RunRecord current = readRunRecord(singleLaneDir().resolve("current.json"));
        assertEquals(List.of(":demoTask", ":secondTask"), current.invocationContext().requestedLeafTaskPaths());
        assertEquals(2, current.tasks().size());
        assertEquals("VERIFICATION_REPORT_TASK", current.tasks().get(1).kind());
        assertEquals(true, current.tasks().get(1).verification().reportOnly());
    }

    @Test
    void helpDoesNotCreateLaneFiles() throws IOException {
        writeJavaProject(false);

        runner().withArguments("help", "-q").build();

        assertFalse(Files.exists(tempDir.resolve(".gradle/test-perf/lanes")));
    }

    @Test
    void plainTestRequiresExplicitOptInToEnterMonitoringSurface() throws IOException {
        writeJavaProject(false);

        runner().withArguments("ignoredPlainTest", "-q").build();
        assertFalse(Files.exists(tempDir.resolve(".gradle/test-perf/lanes")));

        BuildResult result = runner().withArguments("optInPlainTest", "-q").build();
        assertTrue(result.getOutput().contains("baseline established"));
        RunRecord current = readRunRecord(singleLaneDir().resolve("current.json"));
        assertEquals(List.of("optInPlainTest"), current.invocationContext().requestedTaskPaths());
        assertEquals("TEST", current.tasks().get(0).kind());
        assertEquals("HEAVY_EVALUATION", current.tasks().get(0).tests().bandHint());
    }

    @Test
    void failingMonitoredTaskWritesIncompleteRunWithoutCurrentBaseline() throws IOException {
        writeJavaProject(true);

        BuildResult result = runner().withArguments("demoTask", "-q").buildAndFail();

        assertTrue(result.getOutput().contains("baseline not updated"));
        Path laneDir = singleLaneDir();
        assertTrue(Files.exists(laneDir.resolve("current.incomplete.json")));
        assertTrue(Files.exists(laneDir.resolve("reports/latest.incomplete.md")));
        assertFalse(Files.exists(laneDir.resolve("current.json")));
    }

    @Test
    void supportsConfigurationCacheWithoutLaneCorruption() throws IOException {
        writeJavaProject(false);

        runner().withArguments("demoTask", "--configuration-cache", "-q").build();
        BuildResult second = runner().withArguments("demoTask", "--configuration-cache", "-q").build();

        Path laneDir = singleLaneDir();
        assertTrue(Files.exists(laneDir.resolve("previous.json")));
    }

    @Test
    void lightAggregateTaskDoesNotAppearInHeavySummary() throws IOException {
        writeJavaProject(false);

        runner().withArguments("secondTask", "-q").build();
        runner().withArguments("secondTask", "-q").build();

        Path laneDir = singleLaneDir();
        String latestMarkdown = Files.readString(laneDir.resolve("reports/latest.md"));
        assertTrue(latestMarkdown.contains("LIGHT_AGGREGATE"));
    }

    @Test
    void ciReportOnlyWritesIsolatedSummaryWithoutLocalBaselineRotation() throws IOException {
        writeJavaProject(false);

        runner().withArguments("demoTask", "-q").build();
        Path localLaneDir = singleLaneDir();
        RunRecord localCurrent = readRunRecord(localLaneDir.resolve("current.json"));

        BuildResult reportOnly = runner().withArguments("demoTask", "-Ptestperf.mode=ci-report-only", "-q").build();

        RunRecord localCurrentAfter = readRunRecord(localLaneDir.resolve("current.json"));
        assertEquals(localCurrent.runId(), localCurrentAfter.runId());
        assertFalse(Files.exists(localLaneDir.resolve("previous.json")));
        Path ciLaneDir = singleCiLaneDir();
        assertTrue(Files.exists(ciLaneDir.resolve("reports/latest.md")));
        assertFalse(Files.exists(ciLaneDir.resolve("current.json")));
        assertTrue(reportOnly.getOutput().contains("current-run summary written"));
        String ciSummary = Files.readString(ciLaneDir.resolve("reports/latest.md"));
        assertTrue(ciSummary.contains("mode: `ci-report-only`"));
        assertTrue(ciSummary.contains("Current Run Tasks"));
        assertTrue(ciSummary.contains("requestedTaskPaths"));
    }

    @Test
    void ciReportOnlyWorksWithoutExistingBaseline() throws IOException {
        writeJavaProject(false);

        BuildResult reportOnly = runner().withArguments("secondTask", "-Ptestperf.mode=ci-report-only", "-q").build();

        assertTrue(reportOnly.getOutput().contains("current-run summary written"));
        assertFalse(Files.exists(tempDir.resolve(".gradle/test-perf/lanes")));
        Path ciLaneDir = singleCiLaneDir();
        assertTrue(Files.exists(ciLaneDir.resolve("reports/latest.md")));
    }

    @Test
    void offModeSkipsAllTestPerfIo() throws IOException {
        writeJavaProject(false);

        runner().withArguments("secondTask", "-Ptestperf.mode=off", "-q").build();

        assertFalse(Files.exists(tempDir.resolve(".gradle/test-perf")));
    }

    private GradleRunner runner() {
        return GradleRunner.create().withProjectDir(tempDir.toFile()).withPluginClasspath();
    }

    private void writeJavaProject(boolean failingTest) throws IOException {
        Files.writeString(
                tempDir.resolve("settings.gradle"),
                """
                rootProject.name = 'testperf-functional-test'
                """);
        Files.writeString(
                tempDir.resolve("build.gradle"),
                """
                import com.ktome.build.verification.VerificationTask
                import com.ktome.build.verification.VerificationReportTask
                import com.ktome.build.testperf.TestPerfPlainTestBand
                import com.ktome.build.testperf.TestPerfPlainTestOptIn

                plugins {
                    id 'java'
                    id 'com.ktome.build.verification'
                    id 'com.ktome.build.testperf'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.12.2'
                }

                def verificationRuntimeClasspath = sourceSets.test.runtimeClasspath

                tasks.register('demoTask', VerificationTask) {
                    domainId.set('demo')
                    tier.set('OWNER')
                    nodeId.set('demo.owner')
                    inputSnapshotHash.set('snapshot-demo')
                    runtimeClasspath.from(verificationRuntimeClasspath)
                    sourceInputs.from(sourceSets.test.output.classesDirs)
                    outputDir.set(layout.buildDirectory.dir('reports/demo'))
                    mainClassName.set('example.DemoVerificationCli')
                    %s
                }

                tasks.register('secondTask', VerificationReportTask) {
                    domainId.set('second')
                    tier.set('PREFLIGHT')
                    nodeId.set('second.report')
                    inputSnapshotHash.set('snapshot-second')
                    runtimeClasspath.from(verificationRuntimeClasspath)
                    sourceInputs.from(sourceSets.test.output.classesDirs)
                    outputDir.set(layout.buildDirectory.dir('reports/second'))
                    mainClassName.set('example.DemoVerificationCli')
                }

                tasks.register('combo') {
                    dependsOn(tasks.named('demoTask'))
                    dependsOn(tasks.named('secondTask'))
                }

                tasks.register('ignoredPlainTest', Test) {
                    testClassesDirs = sourceSets.test.output.classesDirs
                    classpath = sourceSets.test.runtimeClasspath
                    useJUnitPlatform()
                    filter {
                        includeTestsMatching 'example.DoesNotExist'
                        failOnNoMatchingTests = false
                    }
                }

                tasks.register('optInPlainTest', Test) {
                    testClassesDirs = sourceSets.test.output.classesDirs
                    classpath = sourceSets.test.runtimeClasspath
                    useJUnitPlatform()
                    filter {
                        includeTestsMatching 'example.DoesNotExistEither'
                        failOnNoMatchingTests = false
                    }
                    TestPerfPlainTestOptIn.monitor(delegate as Test, TestPerfPlainTestBand.HEAVY_EVALUATION)
                }
                """
                        .formatted(failingTest ? "extraArguments.add('--fail')" : ""));
        Path testSourceDir = tempDir.resolve("src/test/java/example");
        Files.createDirectories(testSourceDir);
        Files.writeString(
                testSourceDir.resolve("DemoVerificationCli.java"),
                """
                package example;

                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.charset.StandardCharsets;

                public final class DemoVerificationCli {
                    public static void main(String[] args) throws Exception {
                        String outputDir = null;
                        for (int index = 0; index < args.length - 1; index++) {
                            if ("--output-dir".equals(args[index])) {
                                outputDir = args[index + 1];
                                break;
                            }
                        }
                        if (outputDir == null) {
                            throw new IllegalArgumentException("Missing --output-dir");
                        }
                        Path outputPath = Path.of(outputDir);
                        Files.createDirectories(outputPath);
                        Files.writeString(
                                outputPath.resolve("marker.txt"),
                                String.join(" ", args),
                                StandardCharsets.UTF_8);
                        for (String argument : args) {
                            if ("--fail".equals(argument)) {
                                throw new IllegalStateException("demo failure");
                            }
                        }
                        if ("true".equals(System.getProperty("demo.fail"))) {
                            throw new IllegalStateException("demo failure");
                        }
                    }
                }
                """
                        );
    }

    private Path singleLaneDir() throws IOException {
        try (Stream<Path> stream = Files.list(tempDir.resolve(".gradle/test-perf/lanes"))) {
            return stream.sorted(Comparator.comparing(Path::toString)).findFirst().orElseThrow();
        }
    }

    private Path singleCiLaneDir() throws IOException {
        try (Stream<Path> stream = Files.list(tempDir.resolve(".gradle/test-perf/ci"))) {
            return stream.sorted(Comparator.comparing(Path::toString)).findFirst().orElseThrow();
        }
    }

    private static RunRecord readRunRecord(Path path) throws IOException {
        return GSON.fromJson(Files.readString(path), RunRecord.class);
    }
}
