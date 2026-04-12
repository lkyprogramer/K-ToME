package com.ktome.build.verification;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

public abstract class AbstractVerificationExecTask extends DefaultTask {
    public AbstractVerificationExecTask() {
        getCacheStatusOnWrite().convention("LOCAL_EXECUTION");
        getExtraArguments().convention(Collections.emptyList());
        getSystemPropertiesMap().convention(Collections.emptyMap());
        getSourceInputs().setFrom(Collections.emptyList());
        getArtifactInputs().setFrom(Collections.emptyList());
    }

    @Input
    public abstract Property<String> getDomainId();

    @Input
    public abstract Property<String> getTier();

    @Optional
    @Input
    public abstract Property<String> getNodeId();

    @Optional
    @Input
    public abstract Property<String> getInputSnapshotHash();

    @Input
    public abstract Property<String> getCommand();

    @Input
    public abstract Property<String> getMainClassName();

    @Input
    public abstract Property<String> getCacheStatusOnWrite();

    @Optional
    @Input
    public abstract ListProperty<String> getExtraArguments();

    @Optional
    @Input
    public abstract MapProperty<String, String> getSystemPropertiesMap();

    @Classpath
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceInputs();

    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getArtifactInputs();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @TaskAction
    public final void executeVerification() {
        File outputDirectory = getOutputDir().get().getAsFile();
        outputDirectory.mkdirs();
        getExecOperations().javaexec(spec -> {
            spec.setClasspath(getRuntimeClasspath());
            spec.getMainClass().set(getMainClassName().get());
            spec.args(buildArguments(outputDirectory));
            spec.systemProperties(getSystemPropertiesMap().get());
        });
    }

    protected List<String> additionalArguments() {
        return Collections.emptyList();
    }

    private List<String> buildArguments(File outputDirectory) {
        List<String> arguments = new ArrayList<>();
        Collections.addAll(
                arguments,
                getCommand().get(),
                "--domain",
                getDomainId().get(),
                "--tier",
                getTier().get(),
                "--output-dir",
                outputDirectory.getAbsolutePath(),
                "--cache-status",
                getCacheStatusOnWrite().get());

        if (getInputSnapshotHash().isPresent()) {
            String snapshotHash = getInputSnapshotHash().get();
            if (!snapshotHash.isBlank()) {
                Collections.addAll(arguments, "--snapshot", snapshotHash);
            }
        }

        if (getNodeId().isPresent()) {
            String selectedNodeId = getNodeId().get();
            if (!selectedNodeId.isBlank()) {
                Collections.addAll(arguments, "--node-id", selectedNodeId);
            }
        }

        getArtifactInputs().getFiles().stream()
                .sorted(Comparator.comparing(File::getPath))
                .forEach(artifactInput ->
                        Collections.addAll(arguments, "--artifact-input", artifactInput.getAbsolutePath()));

        arguments.addAll(additionalArguments());
        arguments.addAll(getExtraArguments().get());
        return arguments;
    }
}
