package com.ktome.build.verification;

import javax.inject.Inject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public class VerificationRuntimeExtension {
    private final Property<String> mainClassName;
    private final Property<String> defaultGroup;

    @Inject
    public VerificationRuntimeExtension(ObjectFactory objects) {
        this.mainClassName =
                objects.property(String.class).convention("com.ktome.tools.verification.VerificationCli");
        this.defaultGroup = objects.property(String.class).convention("verification");
    }

    public Property<String> getMainClassName() {
        return mainClassName;
    }

    public Property<String> getDefaultGroup() {
        return defaultGroup;
    }
}
