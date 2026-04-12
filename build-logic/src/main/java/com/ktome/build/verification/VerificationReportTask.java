package com.ktome.build.verification;

import org.gradle.api.tasks.CacheableTask;

@CacheableTask
public abstract class VerificationReportTask extends AbstractVerificationExecTask {
    public VerificationReportTask() {
        getCommand().convention("report");
        getCacheStatusOnWrite().convention("REPORT_ONLY_REBUILD");
    }
}
