package com.ktome.build.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class Phase4AggregationManifestTest {
    private final Phase4AggregationManifestLoader loader = new Phase4AggregationManifestLoader();

    @Test
    void loadsCheckedInManifestAndPreservesCanonicalTaskOrder() {
        Phase4AggregationManifest manifest = loader.load(repoRoot().resolve("tools/src/main/resources/phase4/aggregation-manifest.yaml"));

        assertEquals(Phase4AggregationManifest.SCHEMA_VERSION, manifest.getSchemaVersion());
        assertEquals(Phase4AggregationManifest.PHASE_ID, manifest.getPhaseId());
        assertEquals(
                List.of(
                        "mapgenSmoke",
                        "solvabilityHarness",
                        "hiddenContentHarness",
                        "organicHiddenProbe",
                        "contentPackHarness",
                        "bossHarness",
                        "longRunLab",
                        "terrainInteractionBatch",
                        "whiteBoxMapgen",
                        "whiteBoxSolvability",
                        "lootBalanceLab",
                        "whiteBoxLoot",
                        "whiteBoxHiddenContent",
                        "whiteBoxContentPack"),
                manifest.getTasks().stream().map(Phase4AggregationManifest.TaskEntry::getTaskId).toList());
    }

    @Test
    void rejectsUnknownTaskFields() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                loader.load(
                                        """
                                        schemaVersion: phase4-aggregation-manifest-v1
                                        phaseId: P4
                                        tasks:
                                          - taskId: mapgenSmoke
                                            taskPath: :tools:mapgenSmoke
                                            artifactRelativePath: tools/build/reports/phase4/mapgen/mapgen-smoke-summary.json
                                            role: AGGREGATION_ONLY
                                            unknownField: true
                                        """,
                                        "inline-test"));

        assertTrue(exception.getMessage().contains("unknownField"));
    }

    @Test
    void rejectsTaskIdAndTaskPathDrift() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                loader.load(
                                        """
                                        schemaVersion: phase4-aggregation-manifest-v1
                                        phaseId: P4
                                        tasks:
                                          - taskId: wrongId
                                            taskPath: :tools:mapgenSmoke
                                            artifactRelativePath: tools/build/reports/phase4/mapgen/mapgen-smoke-summary.json
                                            role: AGGREGATION_ONLY
                                        """,
                                        "inline-test"));

        assertTrue(exception.getMessage().contains("does not match taskPath"));
    }

    private Path repoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        return Files.isDirectory(current.resolve("tools")) ? current : current.getParent();
    }
}
