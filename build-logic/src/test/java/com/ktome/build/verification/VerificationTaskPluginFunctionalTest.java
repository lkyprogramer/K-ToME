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
    void verifyChangedPlanGateDoesNotSkipDependencyRequestedThroughProjectQualifiedAliasInMixedInvocation() throws IOException {
        writeMultiProjectBuildWithVerifyChangedGate(":otherTask\n");

        var result = runner().withArguments("verifyChanged", ":tools:whiteBoxContentPack", "-q").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":prepareVerifyChangedPlan").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":tools:contentPackHarness").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":tools:whiteBoxContentPack").getOutcome());
        assertTrue(Files.exists(tempDir.resolve("tools/build/content-pack.txt")));
        assertTrue(Files.exists(tempDir.resolve("tools/build/whitebox-content-pack.txt")));
    }

    @Test
    void verifyChangedPlanGateIgnoresUnrelatedCrossProjectAliasInMixedInvocation() throws IOException {
        writeMultiProjectBuildWithUnrelatedClientAlias(":otherTask\n");

        var result = runner().withArguments("verifyChanged", "clientSmoke", "-q").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":prepareVerifyChangedPlan").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":client:clientSmoke").getOutcome());
        assertTrue(Files.exists(tempDir.resolve("client/build/client-smoke.txt")));
        assertTrue(Files.notExists(tempDir.resolve("tools/build/content-pack.txt")));
    }

    @Test
    void verifyChangedPlanGateDoesNotSkipTargetBehindRootAggregateAliasInMixedInvocation() throws IOException {
        writeMultiProjectBuildWithRootAggregateAlias(":otherTask\n");

        var result = runner().withArguments("verifyChanged", "preReleaseAcceptance", "-q").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":prepareVerifyChangedPlan").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":tools:contractLint").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":contractLint").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":preReleaseAcceptance").getOutcome());
        assertTrue(Files.exists(tempDir.resolve("tools/build/contract-lint.txt")));
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

    private void writeMultiProjectBuildWithVerifyChangedGate(String plannedTaskPaths) throws IOException {
        Files.writeString(
                tempDir.resolve("settings.gradle"),
                """
                rootProject.name = 'verification-task-plugin-functional-test'
                include 'tools'
                """);
        Files.createDirectories(tempDir.resolve("tools"));
        Files.writeString(tempDir.resolve("tools/build.gradle"), "");
        Files.writeString(
                tempDir.resolve("build.gradle"),
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

                project(':tools') {
                    tasks.register('contentPackHarness') { task ->
                        doLast {
                            def marker = layout.buildDirectory.file('content-pack.txt').get().asFile
                            marker.parentFile.mkdirs()
                            marker.text = 'CONTENT_PACK'
                        }
                        VerifyChangedPlanGate.applyTo(task, verifyChangedTaskPathsFile, verifyChangedPreflightTaskPathsFile, ':prepareVerifyChangedPlan')
                    }

                    tasks.register('whiteBoxContentPack') {
                        dependsOn(tasks.named('contentPackHarness'))
                        doLast {
                            def marker = layout.buildDirectory.file('whitebox-content-pack.txt').get().asFile
                            marker.parentFile.mkdirs()
                            marker.text = 'WHITEBOX_CONTENT_PACK'
                        }
                    }
                }

                tasks.register('verifyChanged') {
                    dependsOn(tasks.named('prepareVerifyChangedPlan'))
                    dependsOn(project(':tools').tasks.named('contentPackHarness'))
                }
                """
                        .formatted(groovyQuoted(plannedTaskPaths), groovyQuoted(plannedTaskPaths)));
    }

    private void writeMultiProjectBuildWithUnrelatedClientAlias(String plannedTaskPaths) throws IOException {
        Files.writeString(
                tempDir.resolve("settings.gradle"),
                """
                rootProject.name = 'verification-task-plugin-functional-test'
                include 'tools'
                include 'client'
                """);
        Files.createDirectories(tempDir.resolve("tools"));
        Files.createDirectories(tempDir.resolve("client"));
        Files.writeString(tempDir.resolve("tools/build.gradle"), "");
        Files.writeString(tempDir.resolve("client/build.gradle"), "");
        Files.writeString(
                tempDir.resolve("build.gradle"),
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

                project(':tools') {
                    tasks.register('contentPackHarness') { task ->
                        doLast {
                            def marker = layout.buildDirectory.file('content-pack.txt').get().asFile
                            marker.parentFile.mkdirs()
                            marker.text = 'CONTENT_PACK'
                        }
                        VerifyChangedPlanGate.applyTo(task, verifyChangedTaskPathsFile, verifyChangedPreflightTaskPathsFile, ':prepareVerifyChangedPlan')
                    }
                }

                project(':client') {
                    tasks.register('clientSmoke') {
                        doLast {
                            def marker = layout.buildDirectory.file('client-smoke.txt').get().asFile
                            marker.parentFile.mkdirs()
                            marker.text = 'CLIENT_SMOKE'
                        }
                    }
                }

                tasks.register('clientSmoke') {
                    dependsOn(':client:clientSmoke')
                }

                tasks.register('verifyChanged') {
                    dependsOn(tasks.named('prepareVerifyChangedPlan'))
                    dependsOn(project(':tools').tasks.named('contentPackHarness'))
                }
                """
                        .formatted(groovyQuoted(plannedTaskPaths), groovyQuoted(plannedTaskPaths)));
    }

    private void writeMultiProjectBuildWithRootAggregateAlias(String plannedTaskPaths) throws IOException {
        Files.writeString(
                tempDir.resolve("settings.gradle"),
                """
                rootProject.name = 'verification-task-plugin-functional-test'
                include 'tools'
                """);
        Files.createDirectories(tempDir.resolve("tools"));
        Files.writeString(tempDir.resolve("tools/build.gradle"), "");
        Files.writeString(
                tempDir.resolve("build.gradle"),
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

                project(':tools') {
                    tasks.register('contractLint') { task ->
                        doLast {
                            def marker = layout.buildDirectory.file('contract-lint.txt').get().asFile
                            marker.parentFile.mkdirs()
                            marker.text = 'CONTRACT_LINT'
                        }
                        VerifyChangedPlanGate.applyTo(task, verifyChangedTaskPathsFile, verifyChangedPreflightTaskPathsFile, ':prepareVerifyChangedPlan')
                    }
                }

                tasks.register('contractLint') {
                    dependsOn(project(':tools').tasks.named('contractLint'))
                }

                tasks.register('preReleaseAcceptance') {
                    dependsOn(tasks.named('contractLint'))
                }

                tasks.register('verifyChanged') {
                    dependsOn(tasks.named('prepareVerifyChangedPlan'))
                    dependsOn(project(':tools').tasks.named('contractLint'))
                }
                """
                        .formatted(groovyQuoted(plannedTaskPaths), groovyQuoted(plannedTaskPaths)));
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
