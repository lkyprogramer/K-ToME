package com.ktome.build.verification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

@CacheableTask
public abstract class LegacyHarnessAdapterTask extends AbstractVerificationExecTask {
    public LegacyHarnessAdapterTask() {
        getCommand().convention("legacy-adapter");
        getCacheStatusOnWrite().convention("LEGACY_ADAPTER");
        getSelectedClasses().convention(Collections.emptyList());
        getSelectedTags().convention(Collections.emptyList());
    }

    @Optional
    @Input
    public abstract ListProperty<String> getSelectedClasses();

    @Optional
    @Input
    public abstract ListProperty<String> getSelectedTags();

    @Override
    protected List<String> additionalArguments() {
        List<String> arguments = new ArrayList<>();
        getSelectedClasses().get().stream()
                .sorted()
                .forEach(className -> Collections.addAll(arguments, "--select-class", className));
        getSelectedTags().get().stream()
                .sorted()
                .forEach(tag -> Collections.addAll(arguments, "--select-tag", tag));
        return arguments;
    }
}
