package com.ktome.build.verification;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VerificationTaskPluginFunctionalTest {
    @TempDir
    Path tempDir;

    @Test
    void pluginAppliesDefaultConventions() throws IOException {
        writeBuild(
                """
                import com.ktome.build.verification.VerificationTask

                plugins {
                    id 'com.ktome.build.verification'
                }

                tasks.register('demo', VerificationTask) {
                    domainId.set('demo')
                    tier.set('PREFLIGHT')
                    inputSnapshotHash.set('snapshot')
                    runtimeClasspath.from(files())
                    sourceInputs.from(files())
                    outputDir.set(layout.buildDirectory.dir('reports/demo'))
                }

                tasks.register('printConfig') {
                    doLast {
                        def task = tasks.named('demo', VerificationTask).get()
                        println("GROUP=" + task.group)
                        println("MAIN=" + task.mainClassName.get())
                        println("CACHE=" + task.cacheStatusOnWrite.get())
                    }
                }
                """);

        String output = runner().withArguments("printConfig", "-q").build().getOutput();

        assertTrue(output.contains("GROUP=verification"));
        assertTrue(output.contains("MAIN=com.ktome.tools.verification.VerificationCli"));
        assertTrue(output.contains("CACHE=LOCAL_EXECUTION"));
    }

    @Test
    void pluginHonorsVerificationRuntimeOverrides() throws IOException {
        writeBuild(
                """
                import com.ktome.build.verification.VerificationTask

                plugins {
                    id 'com.ktome.build.verification'
                }

                verificationRuntime {
                    defaultGroup.set('custom-verification')
                    mainClassName.set('example.CustomMain')
                }

                tasks.register('demo', VerificationTask) {
                    domainId.set('demo')
                    tier.set('PREFLIGHT')
                    inputSnapshotHash.set('snapshot')
                    runtimeClasspath.from(files())
                    sourceInputs.from(files())
                    outputDir.set(layout.buildDirectory.dir('reports/demo'))
                }

                tasks.register('printConfig') {
                    doLast {
                        def task = tasks.named('demo', VerificationTask).get()
                        println("GROUP=" + task.group)
                        println("MAIN=" + task.mainClassName.get())
                    }
                }
                """);

        String output = runner().withArguments("printConfig", "-q").build().getOutput();

        assertTrue(output.contains("GROUP=custom-verification"));
        assertTrue(output.contains("MAIN=example.CustomMain"));
    }

    @Test
    void verifyChangedPlanGateSkipsTasksOutsideRequestedPlan() throws IOException {
        writeBuildWithVerifyChangedGate(":otherTask\n");

        var result = runner().withArguments("verifyChanged", "-q").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":prepareVerifyChangedPlan").getOutcome());
        assertTrue(Files.notExists(tempDir.resolve("build/watched.txt")));
    }

    @Test
    void verifyChangedPlanGateDoesNotBlockDirectTaskExecution() throws IOException {
        writeBuildWithVerifyChangedGate(":otherTask\n");

        var result = runner().withArguments("watched", "-q").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":watched").getOutcome());
        assertTrue(Files.exists(tempDir.resolve("build/watched.txt")));
    }

    @Test
    void verifyChangedPlanGateDoesNotSkipExplicitlyRequestedTaskInMixedInvocation() throws IOException {
        writeBuildWithVerifyChangedGate(":otherTask\n");

        var result = runner().withArguments("verifyChanged", "watched", "-q").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":prepareVerifyChangedPlan").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":watched").getOutcome());
        assertTrue(Files.exists(tempDir.resolve("build/watched.txt")));
    }

    @Test
    void verifyChangedPlanGateDoesNotSkipTaskRequestedThroughAliasInMixedInvocation() throws IOException {
        writeBuildWithVerifyChangedGate(":otherTask\n");

        var result = runner().withArguments("verifyChanged", "aliasGate", "-q").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":prepareVerifyChangedPlan").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":watched").getOutcome());
        assertTrue(Files.exists(tempDir.resolve("build/watched.txt")));
    }

    @Test
    void verifyChangedPlanGateDoesNotSkipTaskRequestedThroughRootQualifiedAliasInMixedInvocation() throws IOException {
        writeBuildWithVerifyChangedGate(":otherTask\n");

        var result = runner().withArguments("verifyChanged", ":aliasGate", "-q").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":prepareVerifyChangedPlan").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":watched").getOutcome());
        assertTrue(Files.exists(tempDir.resolve("build/watched.txt")));
    }

    @Test
    void verifyChangedPreflightUsesDedicatedTaskPlanFile() throws IOException {
        writeBuildWithVerifyChangedGate(":otherTask\n", ":watched\n");

        var result = runner().withArguments("verifyChangedPreflight", "-q").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":prepareVerifyChangedPlan").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":watched").getOutcome());
        assertTrue(Files.exists(tempDir.resolve("build/watched.txt")));
    }

    private GradleRunner runner() {
        return GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withPluginClasspath();
    }

    private void writeBuild(String buildScript) throws IOException {
        Files.writeString(tempDir.resolve("settings.gradle"), "rootProject.name = 'verification-task-plugin-functional-test'\n");
        Files.writeString(tempDir.resolve("build.gradle"), buildScript);
    }

    private void writeBuildWithVerifyChangedGate(String plannedTaskPaths) throws IOException {
        writeBuildWithVerifyChangedGate(plannedTaskPaths, plannedTaskPaths);
    }

    private void writeBuildWithVerifyChangedGate(String plannedTaskPaths, String plannedPreflightTaskPaths) throws IOException {
        writeBuild(
                """
                import com.ktome.build.verification.VerifyChangedPlanGate

                plugins {
                    id 'com.ktome.build.verification'
                }

                def verifyChangedTaskPathsFile = layout.buildDirectory.file('verification/verify-changed/task-paths.txt')
                def verifyChangedPreflightTaskPathsFile = layout.buildDirectory.file('verification/verify-changed/preflight-task-paths.txt')

                tasks.register('prepareVerifyChangedPlan') {
                    doLast {
                        def taskPathsFile = verifyChangedTaskPathsFile.get().asFile
                        def preflightTaskPathsFile = verifyChangedPreflightTaskPathsFile.get().asFile
                        taskPathsFile.parentFile.mkdirs()
                        taskPathsFile.text = %s
                        preflightTaskPathsFile.text = %s
                    }
                }

                tasks.register('watched') { task ->
                    doLast {
                        def marker = layout.buildDirectory.file('watched.txt').get().asFile
                        marker.parentFile.mkdirs()
                        marker.text = 'WATCHED'
                    }
                    VerifyChangedPlanGate.applyTo(task, verifyChangedTaskPathsFile, verifyChangedPreflightTaskPathsFile, 'prepareVerifyChangedPlan')
                }

                tasks.register('verifyChanged') {
                    dependsOn(tasks.named('prepareVerifyChangedPlan'))
                    dependsOn(tasks.named('watched'))
                }

                tasks.register('verifyChangedPreflight') {
                    dependsOn(tasks.named('prepareVerifyChangedPlan'))
                    dependsOn(tasks.named('watched'))
                }

                tasks.register('aliasGate') {
                    dependsOn(tasks.named('watched'))
                }
                """
                        .formatted(groovyQuoted(plannedTaskPaths), groovyQuoted(plannedPreflightTaskPaths)));
    }

    private static String groovyQuoted(String value) {
        return "\""
                + value
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                + "\"";
    }
}
