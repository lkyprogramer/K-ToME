package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ReportPhase4RunnerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("reportPhase4Fixture")
    fun `reportPhase4 builds artifact only aggregate and optional legacy comparison`() {
        val compareLegacy = System.getProperty("ktome.phase4.aggregate.compareLegacy")?.toBooleanStrictOrNull() ?: false
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir, includeLegacySummary = compareLegacy)

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir.resolve("aggregate-happy-path")) {
            val run = ReportPhase4Runner.run(compareLegacy = compareLegacy)

            assertTrue(Files.exists(run.summaryPath), "Expected reportPhase4 summary report at ${run.summaryPath}")
            assertTrue(Files.exists(run.markdownPath), "Expected reportPhase4 markdown report at ${run.markdownPath}")

            val payload = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
            val markdown = Files.readString(run.markdownPath)
            val inputs = payload.getValue("inputs").jsonArray
            val ownerMetrics = payload.getValue("ownerMetrics").jsonArray
            val metricCatalog = payload.getValue("metricCatalog").jsonArray
            val sections = payload.getValue("sections").jsonObject

            assertEquals("report-phase4-v2", payload.getValue("schemaVersion").jsonPrimitive.content)
            assertEquals("P4", payload.getValue("phaseId").jsonPrimitive.content)
            assertEquals("14", payload.getValue("inputCount").jsonPrimitive.content)
            assertEquals("18", payload.getValue("ownerMetricCount").jsonPrimitive.content)
            assertEquals("0", payload.getValue("unexpectedRegressionCount").jsonPrimitive.content)
            assertEquals("0", payload.getValue("approvedDebtCount").jsonPrimitive.content)
            assertEquals("0", payload.getValue("improvedDebtCount").jsonPrimitive.content)
            assertTrue(payload.containsKey("domainCacheHitRate"))
            assertTrue(payload.containsKey("artifactReuseRate"))
            assertTrue(payload.containsKey("topInvalidationReasons"))
            assertEquals(14, inputs.size)
            assertEquals(18, ownerMetrics.size)

            val terrainInput =
                inputs.first { input -> input.jsonObject.getValue("sourceTaskId").jsonPrimitive.content == "terrainInteractionBatch" }.jsonObject
            val terrainMetric =
                ownerMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "terrainInteractionEncounterRate.aggregate" }.jsonObject
            val lootMetric =
                ownerMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "sameZoneSecretVsCadenceMaxOverlap" }.jsonObject
            val objectiveMetric =
                ownerMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "avgObjectiveAcquireTurn" }.jsonObject
            val satisfiedMetric =
                ownerMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "criticalPathCombatFloorSatisfied" }.jsonObject
            val lootCatalogMetric =
                metricCatalog.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "sameZoneSecretVsCadenceMaxOverlap" }.jsonObject
            val lootInput =
                inputs.first { input -> input.jsonObject.getValue("sourceTaskId").jsonPrimitive.content == "whiteBoxLoot" }.jsonObject
            val organicHiddenInput =
                inputs.first { input -> input.jsonObject.getValue("sourceTaskId").jsonPrimitive.content == "organicHiddenProbe" }.jsonObject
            val criticalPathSection = sections.getValue("criticalPathPacing").jsonObject

            assertTrue(terrainInput.getValue("evaluationResults").jsonArray.size >= 3)
            assertEquals("RELATIVE_BASELINE", terrainMetric.getValue("baselineMode").jsonPrimitive.content)
            assertEquals("BUDGET_THRESHOLD", lootMetric.getValue("baselineMode").jsonPrimitive.content)
            assertEquals("PASS", lootMetric.getValue("status").jsonPrimitive.content)
            assertEquals("<= 0.500", lootMetric.getValue("target").jsonPrimitive.content)
            assertEquals(lootMetric.getValue("target").jsonPrimitive.content, lootCatalogMetric.getValue("target").jsonPrimitive.content)
            assertTrue(terrainInput.getValue("renderResult").jsonObject.getValue("metadata").jsonObject.containsKey("cacheStatus"))
            assertTrue(terrainInput.getValue("renderResult").jsonObject.getValue("metadata").jsonObject.containsKey("sourceArtifactFingerprint"))
            assertTrue(lootInput.getValue("kernelResult").jsonObject.getValue("metrics").jsonObject.containsKey("secretProfileIdentitySummaries"))
            assertTrue(
                lootInput.getValue("kernelResult").jsonObject.getValue("metrics").jsonObject
                    .getValue("secretProfileIdentitySummaries")
                    .jsonArray
                    .all { summary -> summary.jsonObject.containsKey("canonicalZoneId") },
            )
            assertTrue(
                lootInput.getValue("kernelResult").jsonObject.getValue("metrics").jsonObject
                    .getValue("secretProfileIdentitySummaries")
                    .jsonArray
                    .none { summary -> summary.jsonObject.containsKey("zoneId") },
            )
            assertTrue(organicHiddenInput.getValue("kernelResult").jsonObject.getValue("metrics").jsonObject.containsKey("zoneDiscoveryDistribution"))
            assertTrue(organicHiddenInput.getValue("kernelResult").jsonObject.getValue("metrics").jsonObject.containsKey("secretZoneDiscoveryDistribution"))
            assertTrue(organicHiddenInput.getValue("kernelResult").jsonObject.getValue("metrics").jsonObject.containsKey("searchPromptRequired"))
            assertTrue(sections.containsKey("criticalPathPacing"))
            assertEquals("criticalPathPacing", objectiveMetric.getValue("details").jsonObject.getValue("sectionRef").jsonPrimitive.content)
            assertEquals("criticalPathPacing", satisfiedMetric.getValue("details").jsonObject.getValue("sectionRef").jsonPrimitive.content)
            assertEquals(
                criticalPathSection.getValue("designAudit"),
                satisfiedMetric.getValue("details").jsonObject.getValue("designAudit"),
            )
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "dynamicPoolCoverage" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "specialTierPassiveFamilyDuplicateCount" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneSeenRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneAdoptionRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "nonWeaponBuildPayoffRate" })
            assertEquals(
                criticalPathSection.getValue("criticalPathZoneIds").jsonArray.size,
                criticalPathSection.getValue("designAudit").jsonArray.size,
            )
            assertTrue(markdown.contains("## Scripted vs Organic Hidden"))
            assertTrue(markdown.contains("## Local Reward Identity"))
            assertTrue(markdown.contains("## Critical Path Pacing"))
            assertTrue(markdown.contains("### Critical Path Design Audit"))
            assertTrue(markdown.contains("secret reward identity summaries"))
            assertTrue(markdown.contains("rewardStructureKeys"))
            assertTrue(markdown.contains("loot.deep_iron_slag_cache.secret"))
            assertTrue(markdown.contains("share of total discoveries"))
            assertTrue(markdown.contains("share of total secret-zone entries"))
            assertTrue(markdown.contains("searchPromptRequired"))
            assertTrue(markdown.contains("criticalPathZoneIds"))
            assertTrue(markdown.contains("criticalPathCombatFloorSatisfied"))

            if (compareLegacy) {
                assertNotNull(run.comparisonPath)
                val comparison = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(run.comparisonPath!!)).jsonObject
                assertEquals("0", comparison.getValue("mismatchCount").jsonPrimitive.content)
                assertEquals("18", comparison.getValue("metricCount").jsonPrimitive.content)
            } else {
                assertNull(run.comparisonPath)
            }
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    fun `reportPhase4 removes stale legacy comparison artifact on canonical runs`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val staleComparisonPath = tempDir.resolve("report-phase4-legacy-comparison.json")
        Files.writeString(staleComparisonPath, """{"stale":true}""")

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir) {
            val run = ReportPhase4Runner.run(compareLegacy = false)

            assertTrue(Files.exists(run.summaryPath))
            assertFalse(Files.exists(staleComparisonPath), "Canonical phase4Report run should delete stale parity artifacts from the default output directory.")
            assertNull(run.comparisonPath)
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    fun `reportPhase4 fails fast when canonical aggregate sees legacy loot identity summaries`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        Phase4ReportFixtureTestSupport.mutateWhiteBoxLootCorpusMetrics(fixtureRepoRoot) { metrics ->
            val summaries = metrics.getValue("secretProfileIdentitySummaries").jsonArray
            val updatedSummaries =
                buildJsonArray {
                    summaries.forEachIndexed { index, element ->
                        val summary = element.jsonObject
                        if (index == 0) {
                            add(
                                buildJsonObject {
                                    summary.forEach { (key, value) ->
                                        if (key != "canonicalZoneId") {
                                            put(key, value)
                                        }
                                    }
                                    put("zoneId", summary.getValue("canonicalZoneId"))
                                },
                            )
                        } else {
                            add(element)
                        }
                    }
                }
            Phase4ReportFixtureTestSupport.replaceMetricsField(
                metrics = metrics,
                key = "secretProfileIdentitySummaries",
                value = updatedSummaries,
            )
        }

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir.resolve("aggregate-fail-fast")) {
            val error =
                assertThrows(IllegalStateException::class.java) {
                    ReportPhase4Runner.run(compareLegacy = false)
                }
            assertTrue(error.message.orEmpty().contains("canonicalZoneId"))
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    fun `reportPhase4 fails fast when whitebox loot owner metrics are missing from artifact`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        Phase4ReportFixtureTestSupport.mutateWhiteBoxLootCorpusMetrics(fixtureRepoRoot) { metrics ->
            buildJsonObject {
                metrics.forEach { (key, value) ->
                    if (key != "dynamicPoolCoverage") {
                        put(key, value)
                    }
                }
            }
        }

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir.resolve("aggregate-missing-loot-owner-metric")) {
            val error =
                assertThrows(IllegalStateException::class.java) {
                    ReportPhase4Runner.run(compareLegacy = false)
                }
            assertTrue(error.message.orEmpty().contains("whiteBoxLoot.dynamicPoolCoverage"))
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    fun `reportPhase4 propagates strict local identity violations as unexpected regression`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val pairId = "deep_iron_pit:loot.deep_iron_slag_cache.secret->loot.deep_iron_pit.cadence"
        Phase4ReportFixtureTestSupport.mutateWhiteBoxLootCorpusMetrics(fixtureRepoRoot) { metrics ->
            val updatedSummaries =
                buildJsonArray {
                    metrics.getValue("secretProfileIdentitySummaries").jsonArray.forEach { element ->
                        val summary = element.jsonObject
                        if (summary.getValue("profileId").jsonPrimitive.content == "loot.deep_iron_slag_cache.secret") {
                            add(
                                buildJsonObject {
                                    summary.forEach { (key, value) ->
                                        when (key) {
                                            "strictAllowedMaxOverlap" -> put(key, JsonPrimitive(0.190))
                                            "strictViolationPairIds" ->
                                                put(
                                                    key,
                                                    buildJsonArray {
                                                        add(JsonPrimitive(pairId))
                                                    },
                                                )

                                            else -> put(key, value)
                                        }
                                    }
                                },
                            )
                        } else {
                            add(element)
                        }
                    }
                }
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
                        "secretProfileIdentitySummaries" to updatedSummaries,
                    ),
            )
        }

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir.resolve("aggregate-strict-violation")) {
            val run = ReportPhase4Runner.run(compareLegacy = false)
            val payload = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
            val markdown = Files.readString(run.markdownPath)
            val lootCadenceEntry =
                payload.getValue("inputs").jsonArray
                    .first { input -> input.jsonObject.getValue("sourceTaskId").jsonPrimitive.content == "whiteBoxLoot" }
                    .jsonObject
                    .getValue("evaluationResults").jsonArray
                    .first { evaluation -> evaluation.jsonObject.getValue("evaluationId").jsonPrimitive.content == "loot.localRewardIdentity" }
                    .jsonObject
                    .getValue("entries").jsonArray
                    .first { entry -> entry.jsonObject.getValue("metricId").jsonPrimitive.content == "sameZoneSecretVsCadenceMaxOverlap" }
                    .jsonObject
            val ownerMetric =
                payload.getValue("ownerMetrics").jsonArray
                    .first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "sameZoneSecretVsCadenceMaxOverlap" }
                    .jsonObject

            assertEquals("UNEXPECTED_REGRESSION", lootCadenceEntry.getValue("status").jsonPrimitive.content)
            assertTrue(lootCadenceEntry.getValue("note").jsonPrimitive.content.contains(pairId))
            assertEquals("UNEXPECTED_REGRESSION", ownerMetric.getValue("status").jsonPrimitive.content)
            assertTrue(ownerMetric.getValue("note").jsonPrimitive.content.contains(pairId))
            assertTrue(markdown.contains("strictLocalIdentityViolations"))
            assertTrue(markdown.contains(pairId))
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    fun `reportPhase4 treats missing critical path zone diagnostics as regression instead of crashing`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val longRunSummaryPath = fixtureRepoRoot.resolve("build/reports/harness/long-run-full.json")
        val payload = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(longRunSummaryPath)).jsonObject
        val updatedDiagnostics =
            buildJsonObject {
                payload.getValue("fullRouteZoneTraversalDiagnostics").jsonObject.forEach { (zoneId, diagnostic) ->
                    if (zoneId != "grey_gate_depths") {
                        put(zoneId, diagnostic)
                    }
                }
            }
        Files.writeString(
            longRunSummaryPath,
            Phase4ReportFixtureTestSupport.json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                buildJsonObject {
                    payload.forEach { (key, value) ->
                        if (key == "fullRouteZoneTraversalDiagnostics") {
                            put(key, updatedDiagnostics)
                        } else {
                            put(key, value)
                        }
                    }
                },
            ),
        )

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir.resolve("aggregate-missing-zone")) {
            val run = ReportPhase4Runner.run(compareLegacy = false)
            val ownerMetrics =
                Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject.getValue("ownerMetrics").jsonArray
            val objectiveMetric =
                ownerMetrics
                    .first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "avgObjectiveAcquireTurn" }
                    .jsonObject
            val satisfiedMetric =
                ownerMetrics
                    .first { metric ->
                        metric.jsonObject.getValue("metricId").jsonPrimitive.content == "criticalPathCombatFloorSatisfied"
                    }.jsonObject

            assertEquals("UNEXPECTED_REGRESSION", objectiveMetric.getValue("status").jsonPrimitive.content)
            assertTrue(objectiveMetric.getValue("currentValueText").jsonPrimitive.content.contains("min=n/a"))
            assertTrue(
                objectiveMetric.getValue("currentValue").jsonObject.getValue("failingZones").jsonArray.any { zone ->
                    zone.jsonPrimitive.content == "grey_gate_depths"
                },
            )
            assertEquals("UNEXPECTED_REGRESSION", satisfiedMetric.getValue("status").jsonPrimitive.content)
            assertTrue(
                satisfiedMetric.getValue("currentValue").jsonObject.getValue("failingZones").jsonArray.any { zone ->
                    zone.jsonPrimitive.content == "grey_gate_depths"
                },
            )
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    fun `reportPhase4 treats missing critical path objective samples as regression`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val longRunSummaryPath = fixtureRepoRoot.resolve("build/reports/harness/long-run-full.json")
        val payload = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(longRunSummaryPath)).jsonObject
        val updatedDiagnostics =
            buildJsonObject {
                payload.getValue("fullRouteZoneTraversalDiagnostics").jsonObject.forEach { (zoneId, diagnostic) ->
                    put(
                        zoneId,
                        if (zoneId == "grey_gate_depths") {
                            buildJsonObject {
                                diagnostic.jsonObject.forEach { (key, value) ->
                                    if (key != "avgObjectiveAcquireTurn") {
                                        put(key, value)
                                    }
                                }
                            }
                        } else {
                            diagnostic
                        },
                    )
                }
            }
        Files.writeString(
            longRunSummaryPath,
            Phase4ReportFixtureTestSupport.json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                buildJsonObject {
                    payload.forEach { (key, value) ->
                        if (key == "fullRouteZoneTraversalDiagnostics") {
                            put(key, updatedDiagnostics)
                        } else {
                            put(key, value)
                        }
                    }
                },
            ),
        )

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir.resolve("aggregate-missing-objective")) {
            val run = ReportPhase4Runner.run(compareLegacy = false)
            val objectiveMetric =
                Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
                    .getValue("ownerMetrics")
                    .jsonArray
                    .first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "avgObjectiveAcquireTurn" }
                    .jsonObject

            assertEquals("UNEXPECTED_REGRESSION", objectiveMetric.getValue("status").jsonPrimitive.content)
            assertTrue(objectiveMetric.getValue("currentValueText").jsonPrimitive.content.contains("min=n/a"))
            assertTrue(
                objectiveMetric.getValue("currentValue").jsonObject.getValue("failingZones").jsonArray.any { zone ->
                    zone.jsonPrimitive.content == "grey_gate_depths"
                },
            )
        }
    }
}
