package com.ktome.build.testperf;

public final class TestPerfTaskBands {
    public static final String HEAVY_PRODUCER = "HEAVY_PRODUCER";
    public static final String HEAVY_EVALUATION = "HEAVY_EVALUATION";
    public static final String LIGHT_AGGREGATE = "LIGHT_AGGREGATE";
    public static final String SMALL_TEST = "SMALL_TEST";

    private TestPerfTaskBands() {}

    public static boolean isHeavy(String band) {
        return HEAVY_PRODUCER.equals(band) || HEAVY_EVALUATION.equals(band);
    }
}
