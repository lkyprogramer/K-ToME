package com.ktome.build.testperf;

public final class TestPerfTaskKinds {
    public static final String TEST = "TEST";
    public static final String VERIFICATION_TASK = "VERIFICATION_TASK";
    public static final String VERIFICATION_REPORT_TASK = "VERIFICATION_REPORT_TASK";

    private TestPerfTaskKinds() {}

    public static boolean isVerification(String kind) {
        return VERIFICATION_TASK.equals(kind) || VERIFICATION_REPORT_TASK.equals(kind);
    }

    public static boolean isVerificationReport(String kind) {
        return VERIFICATION_REPORT_TASK.equals(kind);
    }
}
