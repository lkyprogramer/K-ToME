package com.ktome.build.testperf;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import javax.inject.Inject;

public abstract class TestPerfExtension {
    @Inject
    public TestPerfExtension() {
        getSchemaVersion().convention(2);
        getHistoryLimit().convention(20);
        getMode().convention(TestPerfLaneManager.MODE_LOCAL_BASELINE);
    }

    public abstract DirectoryProperty getStorageDir();

    public abstract Property<Integer> getSchemaVersion();

    public abstract Property<Integer> getHistoryLimit();

    public abstract Property<String> getMode();
}
