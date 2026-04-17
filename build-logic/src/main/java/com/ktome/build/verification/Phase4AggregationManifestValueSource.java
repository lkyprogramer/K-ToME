package com.ktome.build.verification;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

public abstract class Phase4AggregationManifestValueSource
        implements ValueSource<Phase4AggregationManifest, Phase4AggregationManifestValueSource.Parameters> {
    @Override
    public Phase4AggregationManifest obtain() {
        return new Phase4AggregationManifestLoader().load(getParameters().getManifestFile().get().getAsFile().toPath());
    }

    public interface Parameters extends ValueSourceParameters {
        RegularFileProperty getManifestFile();
    }
}
