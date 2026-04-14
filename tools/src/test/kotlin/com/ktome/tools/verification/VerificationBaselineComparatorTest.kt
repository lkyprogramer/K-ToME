package com.ktome.tools.verification

import java.nio.file.Path
import kotlin.io.path.writeText
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificationBaselineComparatorTest {
    @Test
    fun `approved debt comparator distinguishes approved debt unexpected regression and improvement`() {
        val baseline =
            VerificationBaseline(
                schemaVersion = VERIFICATION_BASELINE_SCHEMA_VERSION,
                baselineId = "loot-local-identity-v1",
                domainId = "loot",
                mode = BaselineMode.APPROVED_DEBT_SET,
                metricDefinitionVersion = "phase4-loot-v1",
                approvedDebtKeys = listOf("pair.a", "pair.b"),
                ceilings = listOf(VerificationBaselineCeiling(key = "pair.a", maxValue = 0.75)),
            )

        val result =
            VerificationBaselineComparator.compareApprovedDebtSet(
                domainId = "loot",
                evaluationId = "loot.localIdentity",
                baseline = baseline,
                actualDebtKeys = setOf("pair.a", "pair.c"),
                actualDebtValues = mapOf("pair.a" to 0.72, "pair.c" to 0.91),
            )

        assertEquals(EvaluationVerdict.FAIL, result.verdict)
        assertEquals(1, result.approvedDebtCount)
        assertEquals(1, result.unexpectedRegressionCount)
        assertEquals(1, result.improvedDebtCount)
        assertEquals(
            mapOf(
                "pair.a" to EvaluationEntryStatus.APPROVED_DEBT,
                "pair.b" to EvaluationEntryStatus.IMPROVEMENT,
                "pair.c" to EvaluationEntryStatus.UNEXPECTED_REGRESSION,
            ),
            result.entries.associate { entry -> entry.metricId to entry.status },
        )
    }

    @Test
    fun `expected failure comparator treats cleared expected codes as improvements`() {
        val baseline =
            VerificationBaseline(
                schemaVersion = VERIFICATION_BASELINE_SCHEMA_VERSION,
                baselineId = "content-pack-expected-failures-v1",
                domainId = "content-pack",
                mode = BaselineMode.EXPECTED_FAILURE_CODE_SET,
                metricDefinitionVersion = "phase4-content-pack-v1",
                expectedFailureCodes =
                    listOf(
                        "content-pack.overlay.runtime-op-forbidden",
                        "content-pack.version-range.conflict",
                    ),
            )

        val result =
            VerificationBaselineComparator.compareExpectedFailureCodeSet(
                domainId = "content-pack",
                evaluationId = "content-pack.owner",
                baseline = baseline,
                actualFailureCodes = setOf("content-pack.overlay.runtime-op-forbidden"),
            )

        assertEquals(EvaluationVerdict.PASS, result.verdict)
        assertEquals(1, result.expectedFailureCount)
        assertEquals(1, result.improvedDebtCount)
        assertEquals(
            mapOf(
                "content-pack.overlay.runtime-op-forbidden" to EvaluationEntryStatus.EXPECTED_FAILURE,
                "content-pack.version-range.conflict" to EvaluationEntryStatus.IMPROVEMENT,
            ),
            result.entries.associate { entry -> entry.metricId to entry.status },
        )
    }

    @Test
    fun `relative baseline comparator applies target relative increase`() {
        val baseline =
            VerificationBaseline(
                schemaVersion = VERIFICATION_BASELINE_SCHEMA_VERSION,
                baselineId = "terrain-baseline-v1",
                domainId = "terrain",
                mode = BaselineMode.RELATIVE_BASELINE,
                metricDefinitionVersion = "phase4-terrain-v2",
                expectedMetricRanges =
                    listOf(
                        VerificationExpectedMetricRange(
                            metricId = "terrainInteractionEncounterRate.aggregate",
                            baselineValue = 0.1258741258741259,
                            targetRelativeIncrease = 0.3,
                        ),
                    ),
            )

        val result =
            VerificationBaselineComparator.compareRelativeBaseline(
                domainId = "terrain",
                evaluationId = "terrain.aggregate",
                baseline = baseline,
                actualMetrics = mapOf("terrainInteractionEncounterRate.aggregate" to 0.1700000000000000),
                currentValueTexts = mapOf("terrainInteractionEncounterRate.aggregate" to "17.0% (17/100)"),
                currentValueElements = mapOf("terrainInteractionEncounterRate.aggregate" to JsonPrimitive(0.1700000000000000)),
            )

        assertEquals(EvaluationVerdict.PASS, result.verdict)
        assertEquals(0, result.unexpectedRegressionCount)
        assertEquals(">= 0.163636", result.entries.single().targetText)
    }

    @Test
    fun `relative baseline comparator treats target relative decrease as an upper bound only`() {
        val baseline =
            VerificationBaseline(
                schemaVersion = VERIFICATION_BASELINE_SCHEMA_VERSION,
                baselineId = "dominance-baseline-v1",
                domainId = "longrun",
                mode = BaselineMode.RELATIVE_BASELINE,
                metricDefinitionVersion = "phase4-owner-metrics-v1",
                expectedMetricRanges =
                    listOf(
                        VerificationExpectedMetricRange(
                            metricId = "crossProfessionTopWeaponDominance",
                            baselineValue = 0.6,
                            targetRelativeDecrease = 0.1,
                        ),
                    ),
            )

        val result =
            VerificationBaselineComparator.compareRelativeBaseline(
                domainId = "longrun",
                evaluationId = "longrun.dominance",
                baseline = baseline,
                actualMetrics = mapOf("crossProfessionTopWeaponDominance" to 0.5),
                currentValueTexts = mapOf("crossProfessionTopWeaponDominance" to "50.0%"),
                currentValueElements = mapOf("crossProfessionTopWeaponDominance" to JsonPrimitive(0.5)),
            )

        assertEquals(EvaluationVerdict.PASS, result.verdict)
        assertEquals("<= 0.540000", result.entries.single().targetText)
    }

    @Test
    fun `budget threshold comparator enforces min and max guards`() {
        val baseline =
            VerificationBaseline(
                schemaVersion = VERIFICATION_BASELINE_SCHEMA_VERSION,
                baselineId = "terminal-build-identity-v1",
                domainId = "longrun",
                mode = BaselineMode.BUDGET_THRESHOLD,
                metricDefinitionVersion = "phase4-owner-metrics-v1",
                expectedMetricRanges =
                    listOf(
                        VerificationExpectedMetricRange(
                            metricId = "terminalWeaponBaseDiversity",
                            minValue = 3.0,
                        ),
                        VerificationExpectedMetricRange(
                            metricId = "crossProfessionTopWeaponDominance",
                            maxValue = 0.5,
                        ),
                    ),
            )

        val result =
            VerificationBaselineComparator.compareBudgetThreshold(
                domainId = "longrun",
                evaluationId = "longrun.owner",
                baseline = baseline,
                actualMetrics =
                    mapOf(
                        "terminalWeaponBaseDiversity" to 3.0,
                        "crossProfessionTopWeaponDominance" to 0.625,
                    ),
                currentValueTexts =
                    mapOf(
                        "terminalWeaponBaseDiversity" to "3",
                        "crossProfessionTopWeaponDominance" to "62.5% (5/8)",
                    ),
            )

        assertEquals(EvaluationVerdict.FAIL, result.verdict)
        assertEquals(
            mapOf(
                "terminalWeaponBaseDiversity" to EvaluationEntryStatus.PASS,
                "crossProfessionTopWeaponDominance" to EvaluationEntryStatus.UNEXPECTED_REGRESSION,
            ),
            result.entries.associate { entry -> entry.metricId to entry.status },
        )
    }

    @Test
    fun `budget threshold comparator supports strict upper bounds`() {
        val baseline =
            VerificationBaseline(
                schemaVersion = VERIFICATION_BASELINE_SCHEMA_VERSION,
                baselineId = "loot-local-identity-v1",
                domainId = "loot",
                mode = BaselineMode.BUDGET_THRESHOLD,
                metricDefinitionVersion = "phase4-owner-metrics-v1",
                expectedMetricRanges =
                    listOf(
                        VerificationExpectedMetricRange(
                            metricId = "sameZoneSecretVsCadenceMaxOverlap",
                            maxValue = 0.50,
                            maxInclusive = true,
                        ),
                    ),
            )

        val result =
            VerificationBaselineComparator.compareBudgetThreshold(
                domainId = "loot",
                evaluationId = "loot.localRewardIdentity",
                baseline = baseline,
                actualMetrics = mapOf("sameZoneSecretVsCadenceMaxOverlap" to 0.50),
                currentValueTexts = mapOf("sameZoneSecretVsCadenceMaxOverlap" to "0.500"),
            )

        assertEquals(EvaluationVerdict.PASS, result.verdict)
        assertEquals(EvaluationEntryStatus.PASS, result.entries.single().status)
        assertEquals("<= 0.500000", result.entries.single().targetText)
    }

    @Test
    fun `terrain unified baseline file parses with schema version and relative mode`() {
        val baseline =
            VerificationBaseline.read(
                Path
                    .of(System.getProperty("ktome.repo.root"))
                    .resolve(
                        Path.of(
                            "docs",
                            "review",
                            "phase4",
                            "opt",
                            "baselines",
                            "2026-04-09-opt-pr01-terrain-metrics-baseline-unified.json",
                        ),
                    ),
            )

        assertEquals(VERIFICATION_BASELINE_SCHEMA_VERSION, baseline.schemaVersion)
        assertEquals("terrain", baseline.domainId)
        assertEquals(BaselineMode.RELATIVE_BASELINE, baseline.mode)
        assertTrue(baseline.expectedMetricRanges.any { range -> range.metricId == "terrainInteractionEncounterRate.aggregate" })
        assertEquals(JsonObject(emptyMap()), baseline.metadata)
    }

    @Test
    fun `verification baseline read fails when schema version is omitted`() {
        val baselineFile =
            java.nio.file.Files.createTempFile("verification-baseline-without-schema-version", ".json")
        baselineFile.writeText(
            """
            {
              "baselineId": "missing-schema-version",
              "domainId": "loot",
              "mode": "STRICT_ZERO_FAILURE",
              "metricDefinitionVersion": "phase4-owner-metrics-v1"
            }
            """.trimIndent(),
        )

        val error = assertThrows(IllegalArgumentException::class.java) { VerificationBaseline.read(baselineFile) }
        assertTrue(error.message!!.contains("schemaVersion"))
    }

    @Test
    fun `loot local reward identity baseline encodes strict upper bounds`() {
        val baseline =
            VerificationBaseline.read(
                Path
                    .of(System.getProperty("ktome.repo.root"))
                    .resolve(
                        Path.of(
                            "docs",
                            "review",
                            "phase4",
                            "opt",
                            "baselines",
                            "2026-04-12-phase4-loot-local-reward-identity-baseline.json",
                        ),
                    ),
            )

        val cadenceRange = baseline.expectedMetricRange("sameZoneSecretVsCadenceMaxOverlap")
        val rewardRange = baseline.expectedMetricRange("sameZoneSecretVsRewardMaxOverlap")

        assertEquals(true, cadenceRange?.maxInclusive)
        assertEquals(true, rewardRange?.maxInclusive)
    }
}
