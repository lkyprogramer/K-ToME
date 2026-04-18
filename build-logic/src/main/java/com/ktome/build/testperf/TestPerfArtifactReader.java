package com.ktome.build.testperf;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ktome.build.testperf.records.TaskRecord;
import com.ktome.build.testperf.records.VerificationRecord;
import com.ktome.build.testperf.records.WorkloadRecord;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;

public final class TestPerfArtifactReader {
    private static final Gson GSON = new Gson();

    public VerificationRecord readVerification(TestPerfTaskMetadataRegistry.TaskMetadata metadata) {
        JsonObject summary = readJsonObject(metadata.outputDir(), "summary.json");
        JsonObject details = readJsonObject(metadata.outputDir(), "metadata.json");

        WorkloadRecord workload =
                new WorkloadRecord(
                        getString(details, "workloadClass"),
                        getInteger(details, "workloadCount"),
                        getStringList(details, "declaredWorkloadClasses"),
                        getString(details, "selectedNodeWorkloadClass"),
                        getBoolean(summary, "reportOnly"),
                        getString(details, "sourceArtifactDir"),
                        getString(details, "artifactReuseSource"),
                        getLong(details, "evaluationDurationMillis"));

        return new VerificationRecord(
                firstNonBlank(getString(summary, "domainId"), getString(details, "domainId"), metadata.domainId()),
                firstNonBlank(getString(summary, "tier"), getString(details, "selectedTier"), metadata.tier()),
                firstNonBlank(getString(summary, "nodeId"), getString(details, "nodeId"), metadata.nodeId()),
                metadata.inputSnapshotHash(),
                getString(summary, "snapshotHash"),
                getString(summary, "cacheStatus"),
                firstNonNull(
                        getBoolean(summary, "reportOnly"),
                        TestPerfTaskKinds.isVerificationReport(metadata.kind())),
                getInteger(summary, "totalTests"),
                getInteger(summary, "failedTests"),
                getLong(summary, "durationMillis"),
                workload);
    }

    public TaskRecord.TestDetails readTest(TestPerfTaskMetadataRegistry.TaskMetadata metadata) {
        if (metadata.testResultsDir() == null) {
            return null;
        }
        Path testResultsDir = Path.of(metadata.testResultsDir());
        if (!Files.isDirectory(testResultsDir)) {
            return null;
        }

        int total = 0;
        int failed = 0;
        try {
            try (var paths = Files.list(testResultsDir)) {
                for (Path path : paths.filter(current -> current.getFileName().toString().endsWith(".xml")).toList()) {
                    String xml = Files.readString(path);
                    var builderFactory = DocumentBuilderFactory.newInstance();
                    builderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    var builder = builderFactory.newDocumentBuilder();
                    var document = builder.parse(new InputSource(new StringReader(xml)));
                    var suite = document.getDocumentElement();
                    total += Integer.parseInt(suite.getAttribute("tests"));
                    failed += Integer.parseInt(suite.getAttribute("failures"));
                    String errors = suite.getAttribute("errors");
                    if (!errors.isBlank()) {
                        failed += Integer.parseInt(errors);
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read junit xml results from " + testResultsDir, exception);
        }

        return new TaskRecord.TestDetails(
                total,
                failed,
                new WorkloadRecord(
                        "UNIT_TEST",
                        total,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null),
                metadata.bandHint());
    }

    private static JsonObject readJsonObject(String outputDir, String fileName) {
        if (outputDir == null) {
            return null;
        }
        Path path = Path.of(outputDir).resolve(fileName);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return GSON.fromJson(Files.readString(path), JsonObject.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }
    }

    private static String getString(JsonObject object, String fieldName) {
        if (object == null || !object.has(fieldName) || object.get(fieldName).isJsonNull()) {
            return null;
        }
        return object.get(fieldName).getAsString();
    }

    private static Integer getInteger(JsonObject object, String fieldName) {
        if (object == null || !object.has(fieldName) || object.get(fieldName).isJsonNull()) {
            return null;
        }
        return object.get(fieldName).getAsInt();
    }

    private static Long getLong(JsonObject object, String fieldName) {
        if (object == null || !object.has(fieldName) || object.get(fieldName).isJsonNull()) {
            return null;
        }
        return object.get(fieldName).getAsLong();
    }

    private static Boolean getBoolean(JsonObject object, String fieldName) {
        if (object == null || !object.has(fieldName) || object.get(fieldName).isJsonNull()) {
            return null;
        }
        return object.get(fieldName).getAsBoolean();
    }

    private static List<String> getStringList(JsonObject object, String fieldName) {
        if (object == null || !object.has(fieldName) || object.get(fieldName).isJsonNull()) {
            return null;
        }
        JsonArray array = object.getAsJsonArray(fieldName);
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Boolean firstNonNull(Boolean first, boolean fallback) {
        return first != null ? first : fallback;
    }
}
