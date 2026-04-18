package com.ktome.build.testperf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ktome.build.verification.VerificationReportTask;
import com.ktome.build.verification.VerificationTask;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import javax.inject.Inject;
import org.gradle.StartParameter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.flow.FlowProviders;
import org.gradle.api.flow.FlowScope;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.build.event.BuildEventsListenerRegistry;

public abstract class TestPerfPlugin implements Plugin<Project> {
    private static final long DAEMON_REUSED_UPTIME_THRESHOLD_MILLIS = 60_000L;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    @Inject
    protected abstract BuildEventsListenerRegistry getBuildEventsListenerRegistry();

    @Inject
    protected abstract FlowScope getFlowScope();

    @Inject
    protected abstract FlowProviders getFlowProviders();

    @Override
    public void apply(Project project) {
        if (project != project.getRootProject()) {
            return;
        }

        TestPerfExtension extension =
                project.getExtensions().create("testPerf", TestPerfExtension.class);
        extension.getStorageDir().convention(project.getLayout().getProjectDirectory().dir(".gradle/test-perf"));
        String resolvedMode = normalizeMode(project.findProperty("testperf.mode"));
        extension.getMode().convention(resolvedMode);

        if (TestPerfLaneManager.MODE_OFF.equals(resolvedMode)) {
            return;
        }

        TestPerfTaskMetadataRegistry registry = new TestPerfTaskMetadataRegistry();
        registerMonitoredTasks(project, registry);

        Provider<TestPerfBuildService> serviceProvider =
                project.getGradle()
                        .getSharedServices()
                        .registerIfAbsent(
                                "ktomeTestPerfListener",
                                TestPerfBuildService.class,
                                spec -> {});
        getBuildEventsListenerRegistry().onTaskCompletion(serviceProvider);

        StartParameter startParameter = project.getGradle().getStartParameter();
        Provider<String> metadataJson = project.provider(() -> GSON.toJson(registry.snapshot()));

        getFlowScope()
                .always(
                        TestPerfFinalizeFlowAction.class,
                        spec -> {
                            spec.getParameters().getBuildService().set(serviceProvider);
                            spec.getParameters().getMetadataJson().set(metadataJson);
                            spec.getParameters()
                                    .getStorageDir()
                                    .set(extension.getStorageDir().map(directory -> directory.getAsFile().getAbsolutePath()));
                            spec.getParameters().getSchemaVersion().set(extension.getSchemaVersion());
                            spec.getParameters().getHistoryLimit().set(extension.getHistoryLimit());
                            spec.getParameters().getMode().set(extension.getMode());
                            spec.getParameters().getRequestedTaskPaths().set(startParameter.getTaskNames());
                            spec.getParameters()
                                    .getBuildSucceeded()
                                    .set(getFlowProviders().getBuildWorkResult().map(result -> result.getFailure().isEmpty()));
                            spec.getParameters().getBuildCacheEnabled().set(startParameter.isBuildCacheEnabled());
                            spec.getParameters()
                                    .getConfigurationCacheEnabled()
                                    .set(startParameter.isConfigurationCacheRequested());
                            spec.getParameters()
                                    .getParallelEnabled()
                                    .set(startParameter.isParallelProjectExecutionEnabled());
                            spec.getParameters().getMaxWorkers().set(startParameter.getMaxWorkerCount());
                            spec.getParameters().getJavaMajorVersion().set(Runtime.version().feature());
                            spec.getParameters().getOsFamily().set(detectOsFamily());
                            spec.getParameters()
                                    .getDaemonReused()
                                    .set(ManagementFactory.getRuntimeMXBean().getUptime() >= DAEMON_REUSED_UPTIME_THRESHOLD_MILLIS);
                            spec.getParameters().getAvailableProcessors().set(Runtime.getRuntime().availableProcessors());
                            spec.getParameters().getSystemLoadAverage().set(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
                        });
    }

    private void registerMonitoredTasks(Project rootProject, TestPerfTaskMetadataRegistry registry) {
        rootProject.getRootProject().getAllprojects().forEach(project -> {
            project.getTasks()
                    .withType(Test.class)
                    .configureEach(task -> registry.registerTestTask(task));
            project.getTasks()
                    .withType(VerificationTask.class)
                    .configureEach(
                            task ->
                                    registry.registerVerificationTask(
                                            task, TestPerfTaskKinds.VERIFICATION_TASK));
            project.getTasks()
                    .withType(VerificationReportTask.class)
                    .configureEach(task ->
                            registry.registerVerificationTask(
                                    task, TestPerfTaskKinds.VERIFICATION_REPORT_TASK));
        });
    }

    private static String detectOsFamily() {
        String normalized = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
        if (normalized.contains("mac") || normalized.contains("darwin")) {
            return "macos";
        }
        if (normalized.contains("win")) {
            return "windows";
        }
        if (normalized.contains("nux") || normalized.contains("nix")) {
            return "linux";
        }
        return normalized.replaceAll("[^a-z0-9]+", "-");
    }

    private static String normalizeMode(Object rawMode) {
        if (rawMode == null) {
            return TestPerfLaneManager.MODE_LOCAL_BASELINE;
        }
        String normalized = rawMode.toString().trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case TestPerfLaneManager.MODE_LOCAL_BASELINE, TestPerfLaneManager.MODE_CI_REPORT_ONLY, TestPerfLaneManager.MODE_OFF -> normalized;
            default -> throw new IllegalArgumentException("Unsupported test perf mode: " + rawMode);
        };
    }
}
