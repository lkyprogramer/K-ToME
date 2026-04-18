package com.ktome.tools.phase4

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class Phase4AggregationInputRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("reportPhase4Fixture")
    @Tag("phase4AggregationInput")
    fun `phase4 aggregation inputs are cached and reusable across warm runs`() {
        val originalReportDir = System.getProperty("ktome.phase4.aggregate.reportDir")
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val tempReportDir = Files.createTempDirectory("ktome-phase4-aggregation-test")
        try {
            System.setProperty("ktome.phase4.aggregate.reportDir", tempReportDir.toString())
            Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempReportDir) {
                val coldRun = Phase4AggregationInputRunner.materialize()
                val warmRun = Phase4AggregationInputRunner.materialize()

                assertEquals(14, coldRun.summary.inputCount)
                assertEquals(14, warmRun.summary.inputCount)
                assertTrue(coldRun.summary.reusedInputCount in 0..14)
                assertEquals(14, warmRun.summary.reusedInputCount)
                assertEquals(0, warmRun.summary.regeneratedInputCount)
                assertEquals(14, warmRun.inputs.size)
                assertTrue(Files.exists(coldRun.summaryPath))
                assertTrue(Files.exists(warmRun.inputDir.resolve("terrainInteractionBatch.json")))

                val payload =
                    Json.parseToJsonElement(Files.readString(warmRun.inputDir.resolve("terrainInteractionBatch.json"))).jsonObject
                val evaluations = payload.getValue("evaluationResults").jsonArray
                val renderMetadata = payload.getValue("renderResult").jsonObject.getValue("metadata").jsonObject
                val lootPayload =
                    Json.parseToJsonElement(Files.readString(warmRun.inputDir.resolve("whiteBoxLoot.json"))).jsonObject
                val lootEvaluationIds =
                    lootPayload
                        .getValue("evaluationResults")
                        .jsonArray
                        .map { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content }

                assertTrue(evaluations.any { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "terrain.aggregateRelativeBaseline" })
                assertTrue(evaluations.any { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "terrain.perZoneLowerBound" })
                assertTrue(lootEvaluationIds.contains("loot.localRewardIdentity"))
                assertTrue(renderMetadata.containsKey("baselineFingerprints"))
                assertEquals("HIT", renderMetadata.getValue("cacheStatus").jsonPrimitive.content)
                assertEquals("true", renderMetadata.getValue("artifactReused").jsonPrimitive.content)
            }
        } finally {
            if (originalReportDir == null) {
                System.clearProperty("ktome.phase4.aggregate.reportDir")
            } else {
                System.setProperty("ktome.phase4.aggregate.reportDir", originalReportDir)
            }
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    @Tag("phase4AggregationInput")
    fun `terminal build baseline change only regenerates longrun aggregation input`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val originalReportDir = System.getProperty("ktome.phase4.aggregate.reportDir")
        val originalBaselineOverride = System.getProperty("ktome.phase4.ownerBaselineOverride.longRunLab")
        val tempReportDir = Files.createTempDirectory("ktome-phase4-aggregation-baseline-test")
        val terminalBuildBaselineCopy = tempReportDir.resolve("phase4-terminal-build-baseline.json")
        val criticalPathPacingBaselineCopy = tempReportDir.resolve("phase4-critical-path-pacing-baseline.json")
        Files.copy(fixtureRepoRoot.resolve(Phase4OwnerBaselineRegistry.TERMINAL_BUILD_BASELINE_RELATIVE_PATH), terminalBuildBaselineCopy)
        Files.copy(fixtureRepoRoot.resolve(Phase4OwnerBaselineRegistry.CRITICAL_PATH_PACING_BASELINE_RELATIVE_PATH), criticalPathPacingBaselineCopy)
        try {
            System.setProperty("ktome.phase4.aggregate.reportDir", tempReportDir.toString())
            System.setProperty(
                "ktome.phase4.ownerBaselineOverride.longRunLab",
                listOf(terminalBuildBaselineCopy, criticalPathPacingBaselineCopy).joinToString(separator = File.pathSeparator) { path ->
                    path.toString()
                },
            )
            Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempReportDir) {
                val firstRun = Phase4AggregationInputRunner.materialize()
                val firstLongRunPayload =
                    Json.parseToJsonElement(Files.readString(firstRun.inputDir.resolve("longRunLab.json"))).jsonObject
                val firstLongRunMetadata = firstLongRunPayload.getValue("renderResult").jsonObject.getValue("metadata").jsonObject
                val firstLongRunFingerprint = firstLongRunMetadata.getValue("sourceArtifactFingerprint").jsonPrimitive.content

                Phase4OwnerBaselineTestSupport.stampBaselineMetadata(
                    terminalBuildBaselineCopy,
                    marker = "report-only-longrun-terminal-build-baseline",
                )

                val secondRun = Phase4AggregationInputRunner.materialize()
                val secondLongRunPayload =
                    Json.parseToJsonElement(Files.readString(secondRun.inputDir.resolve("longRunLab.json"))).jsonObject
                val secondLongRunMetadata = secondLongRunPayload.getValue("renderResult").jsonObject.getValue("metadata").jsonObject
                val secondLootPayload =
                    Json.parseToJsonElement(Files.readString(secondRun.inputDir.resolve("whiteBoxLoot.json"))).jsonObject
                val secondLootMetadata = secondLootPayload.getValue("renderResult").jsonObject.getValue("metadata").jsonObject

                assertEquals(14, secondRun.summary.inputCount)
                assertEquals(13, secondRun.summary.reusedInputCount)
                assertEquals(1, secondRun.summary.regeneratedInputCount)
                assertEquals("MISS", secondLongRunMetadata.getValue("cacheStatus").jsonPrimitive.content)
                assertEquals("baseline-changed", secondLongRunMetadata.getValue("invalidationReason").jsonPrimitive.content)
                assertEquals(firstLongRunFingerprint, secondLongRunMetadata.getValue("sourceArtifactFingerprint").jsonPrimitive.content)
                assertEquals("HIT", secondLootMetadata.getValue("cacheStatus").jsonPrimitive.content)
                assertEquals("true", secondLootMetadata.getValue("artifactReused").jsonPrimitive.content)
            }
        } finally {
            if (originalReportDir == null) {
                System.clearProperty("ktome.phase4.aggregate.reportDir")
            } else {
                System.setProperty("ktome.phase4.aggregate.reportDir", originalReportDir)
            }
            if (originalBaselineOverride == null) {
                System.clearProperty("ktome.phase4.ownerBaselineOverride.longRunLab")
            } else {
                System.setProperty("ktome.phase4.ownerBaselineOverride.longRunLab", originalBaselineOverride)
            }
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    @Tag("phase4AggregationInput")
    fun `critical path pacing baseline change only regenerates longrun aggregation input`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val originalReportDir = System.getProperty("ktome.phase4.aggregate.reportDir")
        val originalBaselineOverride = System.getProperty("ktome.phase4.ownerBaselineOverride.longRunLab")
        val tempReportDir = Files.createTempDirectory("ktome-phase4-aggregation-critical-path-baseline-test")
        val terminalBuildBaselineCopy = tempReportDir.resolve("phase4-terminal-build-baseline.json")
        val criticalPathPacingBaselineCopy = tempReportDir.resolve("phase4-critical-path-pacing-baseline.json")
        Files.copy(fixtureRepoRoot.resolve(Phase4OwnerBaselineRegistry.TERMINAL_BUILD_BASELINE_RELATIVE_PATH), terminalBuildBaselineCopy)
        Files.copy(fixtureRepoRoot.resolve(Phase4OwnerBaselineRegistry.CRITICAL_PATH_PACING_BASELINE_RELATIVE_PATH), criticalPathPacingBaselineCopy)
        try {
            System.setProperty("ktome.phase4.aggregate.reportDir", tempReportDir.toString())
            System.setProperty(
                "ktome.phase4.ownerBaselineOverride.longRunLab",
                listOf(terminalBuildBaselineCopy, criticalPathPacingBaselineCopy).joinToString(separator = File.pathSeparator) { path ->
                    path.toString()
                },
            )
            Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempReportDir) {
                Phase4AggregationInputRunner.materialize()
                Phase4OwnerBaselineTestSupport.stampBaselineMetadata(
                    criticalPathPacingBaselineCopy,
                    marker = "report-only-longrun-critical-path-baseline",
                )

                val secondRun = Phase4AggregationInputRunner.materialize()
                val secondLongRunPayload =
                    Json.parseToJsonElement(Files.readString(secondRun.inputDir.resolve("longRunLab.json"))).jsonObject
                val secondLongRunMetadata = secondLongRunPayload.getValue("renderResult").jsonObject.getValue("metadata").jsonObject
                val secondLootPayload =
                    Json.parseToJsonElement(Files.readString(secondRun.inputDir.resolve("whiteBoxLoot.json"))).jsonObject
                val secondLootMetadata = secondLootPayload.getValue("renderResult").jsonObject.getValue("metadata").jsonObject

                assertEquals(14, secondRun.summary.inputCount)
                assertEquals(13, secondRun.summary.reusedInputCount)
                assertEquals(1, secondRun.summary.regeneratedInputCount)
                assertEquals("MISS", secondLongRunMetadata.getValue("cacheStatus").jsonPrimitive.content)
                assertEquals("baseline-changed", secondLongRunMetadata.getValue("invalidationReason").jsonPrimitive.content)
                assertEquals("HIT", secondLootMetadata.getValue("cacheStatus").jsonPrimitive.content)
                assertEquals("true", secondLootMetadata.getValue("artifactReused").jsonPrimitive.content)
            }
        } finally {
            if (originalReportDir == null) {
                System.clearProperty("ktome.phase4.aggregate.reportDir")
            } else {
                System.setProperty("ktome.phase4.aggregate.reportDir", originalReportDir)
            }
            if (originalBaselineOverride == null) {
                System.clearProperty("ktome.phase4.ownerBaselineOverride.longRunLab")
            } else {
                System.setProperty("ktome.phase4.ownerBaselineOverride.longRunLab", originalBaselineOverride)
            }
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    @Tag("phase4AggregationInput")
    fun `whitebox loot mismatch falls back to recomputed local reward identity evaluation`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val pairId = "deep_iron_pit:loot.deep_iron_slag_cache.secret->loot.deep_iron_pit.cadence"
        Phase4ReportFixtureTestSupport.mutateWhiteBoxLootCorpusMetrics(fixtureRepoRoot) { metrics ->
            Phase4ReportFixtureTestSupport.replaceMetricsFields(
                metrics = metrics,
                replacements =
                    mapOf(
                        "strictLocalIdentityViolationCount" to JsonPrimitive(1),
                        "strictLocalIdentityViolations" to
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("pairId", JsonPrimitive(pairId))
                                        put("zoneId", JsonPrimitive("deep_iron_pit"))
                                        put("pairType", JsonPrimitive(com.ktome.tools.loot.SECRET_VS_CADENCE_PAIR_TYPE))
                                        put("secretProfileId", JsonPrimitive("loot.deep_iron_slag_cache.secret"))
                                        put("comparedProfileId", JsonPrimitive("loot.deep_iron_pit.cadence"))
                                        put("overlap", JsonPrimitive(0.400))
                                        put("allowedMaxOverlap", JsonPrimitive(0.190))
                                    },
                                )
                            },
                    ),
            )
        }

        Phase4ReportFixtureTestSupport.withFixtureProperties(
            repoRoot = fixtureRepoRoot,
            aggregateReportDir = tempDir.resolve("phase4-aggregation-local-reward-fallback"),
        ) {
            val run = Phase4AggregationInputRunner.materialize()
            val lootPayload =
                Json.parseToJsonElement(Files.readString(run.inputDir.resolve("whiteBoxLoot.json"))).jsonObject
            val lootEvaluation =
                lootPayload.getValue("evaluationResults").jsonArray
                    .first { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "loot.localRewardIdentity" }
                    .jsonObject
            val cadenceEntry =
                lootEvaluation.getValue("entries").jsonArray
                    .first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "sameZoneSecretVsCadenceMaxOverlap" }
                    .jsonObject

            assertEquals("FAIL", lootEvaluation.getValue("verdict").jsonPrimitive.content)
            assertEquals("UNEXPECTED_REGRESSION", cadenceEntry.getValue("status").jsonPrimitive.content)
            assertTrue(cadenceEntry.getValue("note").jsonPrimitive.content.contains(pairId))
            assertTrue(cadenceEntry.getValue("note").jsonPrimitive.content.contains("0.190"))
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    @Tag("phase4AggregationInput")
    fun `whitebox loot duplicate family mismatch also falls back to recomputed local reward identity evaluation`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        Phase4ReportFixtureTestSupport.mutateWhiteBoxLootCorpusMetrics(fixtureRepoRoot) { metrics ->
            Phase4ReportFixtureTestSupport.replaceMetricsFields(
                metrics = metrics,
                replacements =
                    mapOf(
                        "specialTierPassiveFamilyDuplicateCount" to JsonPrimitive(1),
                        "specialTierPassiveFamilyDuplicateSummary" to
                            buildJsonObject {
                                put("duplicateFamilyCount", JsonPrimitive(1))
                                put("duplicatedZoneCount", JsonPrimitive(1))
                                put(
                                    "zones",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("canonicalZoneId", JsonPrimitive("deep_iron_pit"))
                                                put("duplicateFamilies", buildJsonArray { add(JsonPrimitive("OnKillResourceRestore")) })
                                            },
                                        )
                                    },
                                )
                            },
                    ),
            )
        }

        Phase4ReportFixtureTestSupport.withFixtureProperties(
            repoRoot = fixtureRepoRoot,
            aggregateReportDir = tempDir.resolve("phase4-aggregation-duplicate-family-fallback"),
        ) {
            val run = Phase4AggregationInputRunner.materialize()
            val lootPayload =
                Json.parseToJsonElement(Files.readString(run.inputDir.resolve("whiteBoxLoot.json"))).jsonObject
            val lootEvaluation =
                lootPayload.getValue("evaluationResults").jsonArray
                    .first { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "loot.localRewardIdentity" }
                    .jsonObject
            val duplicateEntry =
                lootEvaluation.getValue("entries").jsonArray
                    .first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "specialTierPassiveFamilyDuplicateCount" }
                    .jsonObject

            assertEquals("FAIL", lootEvaluation.getValue("verdict").jsonPrimitive.content)
            assertEquals("UNEXPECTED_REGRESSION", duplicateEntry.getValue("status").jsonPrimitive.content)
            assertTrue(duplicateEntry.getValue("note").jsonPrimitive.content.contains("duplicatedZones=1"))
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    @Tag("phase4AggregationInput")
    fun `whitebox loot source coverage mismatch fails aggregation with culprit sources`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        Phase4ReportFixtureTestSupport.mutateWhiteBoxLootCorpusMetrics(fixtureRepoRoot) { metrics ->
            Phase4ReportFixtureTestSupport.replaceMetricsField(
                metrics = metrics,
                key = "rewardRoutingCoverageSummary",
                value =
                    buildJsonObject {
                        put("coveredSourcePairCount", JsonPrimitive(5))
                        put("totalSourcePairCount", JsonPrimitive(6))
                        put("professionCapstoneSourceCoverageRate", JsonPrimitive(5.0 / 6.0))
                        put(
                            "criticalSources",
                            metrics.getValue("rewardRoutingCoverageSummary").jsonObject.getValue("criticalSources"),
                        )
                        put(
                            "professionSourceCoverage",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("professionId", JsonPrimitive("arcanist"))
                                        put("rewardSource", JsonPrimitive("CACHE"))
                                        put("covered", JsonPrimitive(false))
                                        put("coveredSourceIds", buildJsonArray {})
                                        put(
                                            "culpritSourceIds",
                                            buildJsonArray {
                                                add(JsonPrimitive("abyssal_temple/temple_ward_reliquary/GROUND_CACHE"))
                                            },
                                        )
                                    },
                                )
                                add(
                                    buildJsonObject {
                                        put("professionId", JsonPrimitive("rogue"))
                                        put("rewardSource", JsonPrimitive("CACHE"))
                                        put("covered", JsonPrimitive(true))
                                        put(
                                            "coveredSourceIds",
                                            buildJsonArray {
                                                add(JsonPrimitive("underground_river/crystal_cache_chest/GROUND_CACHE"))
                                            },
                                        )
                                        put("culpritSourceIds", buildJsonArray {})
                                    },
                                )
                            },
                        )
                        put(
                            "topRejectedCapstoneCandidates",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("professionId", JsonPrimitive("arcanist"))
                                        put("rewardSource", JsonPrimitive("CACHE"))
                                        put("sourceId", JsonPrimitive("abyssal_temple/temple_ward_reliquary/GROUND_CACHE"))
                                        put("zoneId", JsonPrimitive("abyssal_temple"))
                                        put("baseItemId", JsonPrimitive("unique_arcane_lexicon"))
                                        put("rejectionReason", JsonPrimitive("SOURCE_TIER_MISMATCH"))
                                    },
                                )
                            },
                        )
                    },
            )
        }

        Phase4ReportFixtureTestSupport.withFixtureProperties(
            repoRoot = fixtureRepoRoot,
            aggregateReportDir = tempDir.resolve("phase4-aggregation-source-coverage-fallback"),
        ) {
            val run = Phase4AggregationInputRunner.materialize()
            val lootPayload =
                Json.parseToJsonElement(Files.readString(run.inputDir.resolve("whiteBoxLoot.json"))).jsonObject
            val lootEvaluation =
                lootPayload.getValue("evaluationResults").jsonArray
                    .first { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "loot.localRewardIdentity" }
                    .jsonObject
            val sourceCoverageEntry =
                lootEvaluation.getValue("entries").jsonArray
                    .first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneSourceCoverage.reportOnly" }
                    .jsonObject

            assertEquals("FAIL", lootEvaluation.getValue("verdict").jsonPrimitive.content)
            assertEquals("UNEXPECTED_REGRESSION", sourceCoverageEntry.getValue("status").jsonPrimitive.content)
            assertEquals("5/6", sourceCoverageEntry.getValue("currentValueText").jsonPrimitive.content)
            assertTrue(sourceCoverageEntry.getValue("note").jsonPrimitive.content.contains("arcanist:CACHE"))
            assertTrue(
                sourceCoverageEntry.getValue("note").jsonPrimitive.content.contains(
                    "abyssal_temple/temple_ward_reliquary/GROUND_CACHE",
                ),
            )
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    @Tag("phase4AggregationInput")
    fun `whitebox loot missing reward routing coverage summary fails aggregation fast`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        Phase4ReportFixtureTestSupport.mutateWhiteBoxLootCorpusMetrics(fixtureRepoRoot) { metrics ->
            buildJsonObject {
                metrics.forEach { (key, value) ->
                    if (key != "rewardRoutingCoverageSummary") {
                        put(key, value)
                    }
                }
            }
        }

        Phase4ReportFixtureTestSupport.withFixtureProperties(
            repoRoot = fixtureRepoRoot,
            aggregateReportDir = tempDir.resolve("phase4-aggregation-missing-source-coverage-summary"),
        ) {
            val error =
                assertThrows(IllegalStateException::class.java) {
                    Phase4AggregationInputRunner.materialize()
                }
            assertTrue(error.message.orEmpty().contains("whiteBoxLoot.rewardRoutingCoverageSummary"))
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    @Tag("phase4AggregationInput")
    fun `terminal build evaluation fails when per profession capstone floors collapse`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val terminalBuildBaselinePath =
            fixtureRepoRoot.resolve(Phase4OwnerBaselineRegistry.TERMINAL_BUILD_BASELINE_RELATIVE_PATH)
        Phase4OwnerBaselineTestSupport.replaceExpectedMetricRange(
            path = terminalBuildBaselinePath,
            metricId = "professionCapstoneSeenRate",
            replacement =
                buildJsonObject {
                    put("metricId", JsonPrimitive("professionCapstoneSeenRate"))
                    put("minValue", JsonPrimitive(0.25))
                    put("notes", JsonPrimitive("Profession capstone visibility must stay above the PR-02 floor and cover every profession."))
                    put(
                        "metadata",
                        buildJsonObject {
                            put("perProfessionSeenMinCount", JsonPrimitive(1))
                        },
                    )
                },
        )
        Phase4OwnerBaselineTestSupport.replaceExpectedMetricRange(
            path = terminalBuildBaselinePath,
            metricId = "nonWeaponBuildPayoffRate",
            replacement =
                buildJsonObject {
                    put("metricId", JsonPrimitive("nonWeaponBuildPayoffRate"))
                    put("minValue", JsonPrimitive(0.25))
                    put("notes", JsonPrimitive("Non-weapon capstone payoff must be materially adopted in full-route runs."))
                },
        )
        Phase4ReportFixtureTestSupport.mutateLongRunSummary(fixtureRepoRoot) { payload ->
            buildJsonObject {
                payload.forEach { (key, value) ->
                    when (key) {
                        "professionCapstoneSeenRate" -> put(key, JsonPrimitive(0.25))
                        "professionCapstoneAdoptionRate" -> put(key, JsonPrimitive(0.0))
                        "nonWeaponBuildPayoffRate" -> put(key, JsonPrimitive(0.0))
                        "professionCapstoneBreakdown" ->
                            put(
                                key,
                                buildJsonObject {
                                    put(
                                        "arcanist",
                                        buildJsonObject {
                                            put("sampleCount", JsonPrimitive(3))
                                            put("seenCount", JsonPrimitive(0))
                                            put("adoptedCount", JsonPrimitive(0))
                                            put("nonWeaponPayoffCount", JsonPrimitive(0))
                                            put("seenItems", buildJsonObject {})
                                            put("adoptedItems", buildJsonObject {})
                                            put("nonWeaponPayoffItems", buildJsonObject {})
                                        },
                                    )
                                    put(
                                        "rogue",
                                        buildJsonObject {
                                            put("sampleCount", JsonPrimitive(3))
                                            put("seenCount", JsonPrimitive(1))
                                            put("adoptedCount", JsonPrimitive(0))
                                            put("nonWeaponPayoffCount", JsonPrimitive(0))
                                            put(
                                                "seenItems",
                                                buildJsonObject {
                                                    put("unique_thornpath_crook", JsonPrimitive(1))
                                                },
                                            )
                                            put("adoptedItems", buildJsonObject {})
                                            put("nonWeaponPayoffItems", buildJsonObject {})
                                        },
                                    )
                                    put(
                                        "templar",
                                        buildJsonObject {
                                            put("sampleCount", JsonPrimitive(3))
                                            put("seenCount", JsonPrimitive(1))
                                            put("adoptedCount", JsonPrimitive(0))
                                            put("nonWeaponPayoffCount", JsonPrimitive(0))
                                            put(
                                                "seenItems",
                                                buildJsonObject {
                                                    put("unique_vesper_chainmail", JsonPrimitive(1))
                                                },
                                            )
                                            put("adoptedItems", buildJsonObject {})
                                            put("nonWeaponPayoffItems", buildJsonObject {})
                                        },
                                    )
                                    put(
                                        "vanguard",
                                        buildJsonObject {
                                            put("sampleCount", JsonPrimitive(3))
                                            put("seenCount", JsonPrimitive(1))
                                            put("adoptedCount", JsonPrimitive(0))
                                            put("nonWeaponPayoffCount", JsonPrimitive(0))
                                            put(
                                                "seenItems",
                                                buildJsonObject {
                                                    put("unique_furnace_plate", JsonPrimitive(1))
                                                },
                                            )
                                            put("adoptedItems", buildJsonObject {})
                                            put("nonWeaponPayoffItems", buildJsonObject {})
                                        },
                                    )
                                },
                            )
                        else -> put(key, value)
                    }
                }
            }
        }

        Phase4ReportFixtureTestSupport.withFixtureProperties(
            repoRoot = fixtureRepoRoot,
            aggregateReportDir = tempDir.resolve("phase4-aggregation-terminal-build-regression"),
        ) {
            val run = Phase4AggregationInputRunner.materialize()
            val longRunPayload =
                Json.parseToJsonElement(Files.readString(run.inputDir.resolve("longRunLab.json"))).jsonObject
            val terminalBuildEvaluation =
                longRunPayload.getValue("evaluationResults").jsonArray
                    .first { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "longrun.terminalBuildIdentity" }
                    .jsonObject
            val seenEntry =
                terminalBuildEvaluation.getValue("entries").jsonArray
                    .first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneSeenRate" }
                    .jsonObject
            val adoptionEntry =
                terminalBuildEvaluation.getValue("entries").jsonArray
                    .first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneAdoptionRate" }
                    .jsonObject
            val adoptionFloorEntry =
                terminalBuildEvaluation.getValue("entries").jsonArray
                    .first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneAdoptionFloor.reportOnly" }
                    .jsonObject
            val nonWeaponEntry =
                terminalBuildEvaluation.getValue("entries").jsonArray
                    .first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "nonWeaponBuildPayoffRate" }
                    .jsonObject
            val nonWeaponFloorEntry =
                terminalBuildEvaluation.getValue("entries").jsonArray
                    .first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "nonWeaponBuildPayoffFloor.reportOnly" }
                    .jsonObject

            assertEquals("FAIL", terminalBuildEvaluation.getValue("verdict").jsonPrimitive.content)
            assertEquals("UNEXPECTED_REGRESSION", seenEntry.getValue("status").jsonPrimitive.content)
            assertEquals("UNEXPECTED_REGRESSION", adoptionEntry.getValue("status").jsonPrimitive.content)
            assertEquals("UNEXPECTED_REGRESSION", nonWeaponEntry.getValue("status").jsonPrimitive.content)
            assertEquals("APPROVED_DEBT", adoptionFloorEntry.getValue("status").jsonPrimitive.content)
            assertEquals("APPROVED_DEBT", nonWeaponFloorEntry.getValue("status").jsonPrimitive.content)
            assertTrue(seenEntry.getValue("note").jsonPrimitive.content.contains("arcanist"))
            assertTrue(adoptionFloorEntry.getValue("note").jsonPrimitive.content.contains("arcanist"))
            assertTrue(nonWeaponFloorEntry.getValue("note").jsonPrimitive.content.contains("arcanist"))
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    @Tag("phase4AggregationInput")
    fun `phase4 aggregation fails fast when longrun capstone metrics are missing from artifact`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        Phase4ReportFixtureTestSupport.mutateLongRunSummary(fixtureRepoRoot) { payload ->
            buildJsonObject {
                payload.forEach { (key, value) ->
                    if (key != "professionCapstoneSeenRate") {
                        put(key, value)
                    }
                }
            }
        }

        Phase4ReportFixtureTestSupport.withFixtureProperties(
            repoRoot = fixtureRepoRoot,
            aggregateReportDir = tempDir.resolve("phase4-aggregation-missing-capstone-metric"),
        ) {
            val error =
                assertThrows(IllegalStateException::class.java) {
                    Phase4AggregationInputRunner.materialize()
                }
            assertTrue(error.message.orEmpty().contains("longRunLab.professionCapstoneSeenRate"))
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    @Tag("phase4AggregationInput")
    fun `phase4 aggregation input v6 stores compact pacing details from shared evaluator`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)

        Phase4ReportFixtureTestSupport.withFixtureProperties(
            repoRoot = fixtureRepoRoot,
            aggregateReportDir = tempDir.resolve("phase4-aggregation-v6"),
        ) {
            val run = Phase4AggregationInputRunner.materialize()
            val summary =
                Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
            val longRunPayload =
                Json.parseToJsonElement(Files.readString(run.inputDir.resolve("longRunLab.json"))).jsonObject
            val pacingEvaluation =
                longRunPayload.getValue("evaluationResults").jsonArray
                    .first { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "longrun.criticalPathPacing" }
                    .jsonObject
            val objectiveEntry =
                pacingEvaluation.getValue("entries").jsonArray
                    .first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "avgObjectiveAcquireTurn" }
                    .jsonObject
            val objectiveDetails = objectiveEntry.getValue("details").jsonObject

            assertEquals("phase4-aggregation-input-v8", summary.getValue("contractVersion").jsonPrimitive.content)
            assertEquals("criticalPathPacing", objectiveDetails.getValue("sectionRef").jsonPrimitive.content)
            assertEquals("minimum", objectiveDetails.getValue("metricKind").jsonPrimitive.content)
            assertFalse(objectiveDetails.containsKey("fullRouteZoneTraversalDiagnostics"))
            assertFalse(objectiveDetails.containsKey("criticalPathZoneDesignAudit"))
            assertTrue(objectiveDetails.getValue("zoneFailures").jsonArray.isNotEmpty().not())
            assertEquals("false", objectiveDetails.getValue("sampleMissing").jsonPrimitive.content)
        }
    }

}
