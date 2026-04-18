package com.ktome.build.testperf;

import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.testing.Test;

public final class TestPerfPlainTestOptIn {
    private static final String BAND_HINT_PROPERTY = "ktome.testperf.band";

    private TestPerfPlainTestOptIn() {}

    public static void monitor(Test task, TestPerfPlainTestBand band) {
        task.getExtensions().getExtraProperties().set(BAND_HINT_PROPERTY, band.name());
    }

    public static String bandHint(Test task) {
        ExtraPropertiesExtension extraProperties = task.getExtensions().getExtraProperties();
        if (!extraProperties.has(BAND_HINT_PROPERTY)) {
            return null;
        }
        Object value = extraProperties.get(BAND_HINT_PROPERTY);
        if (value == null) {
            return null;
        }
        String normalized = value.toString().trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return TestPerfPlainTestBand.valueOf(normalized).name();
    }
}
