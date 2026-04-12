package com.ktome.build.verification;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.GradleRunner;
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

    private GradleRunner runner() {
        return GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withPluginClasspath();
    }

    private void writeBuild(String buildScript) throws IOException {
        Files.writeString(tempDir.resolve("settings.gradle"), "rootProject.name = 'verification-task-plugin-functional-test'\n");
        Files.writeString(tempDir.resolve("build.gradle"), buildScript);
    }
}
