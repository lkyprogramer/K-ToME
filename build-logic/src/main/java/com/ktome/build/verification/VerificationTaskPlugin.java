package com.ktome.build.verification;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class VerificationTaskPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        VerificationRuntimeExtension extension =
                project.getExtensions().create("verificationRuntime", VerificationRuntimeExtension.class);

        project.getTasks().withType(AbstractVerificationExecTask.class).configureEach(task -> {
            task.setGroup(extension.getDefaultGroup().get());
            task.getMainClassName().convention(extension.getMainClassName());
        });
    }
}
