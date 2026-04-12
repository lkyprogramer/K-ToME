package com.ktome.build.verification;

import org.gradle.api.tasks.CacheableTask;

@CacheableTask
public abstract class VerificationTask extends AbstractVerificationExecTask {
    public VerificationTask() {
        getCommand().convention("run");
    }
}
