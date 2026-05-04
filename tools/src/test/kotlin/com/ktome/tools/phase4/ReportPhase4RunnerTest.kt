package com.ktome.tools.phase4

import com.ktome.tools.mapgen.WhiteBoxSolvabilityFailLane
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
            val debugArtifactPath = tempDir.resolve("aggregate-happy-path").resolve("build-identity-debug.json")

            assertTrue(Files.exists(run.summaryPath), "Expected reportPhase4 summary report at ${run.summaryPath}")
            assertTrue(Files.exists(run.markdownPath), "Expected reportPhase4 markdown report at ${run.markdownPath}")
            assertTrue(Files.exists(debugArtifactPath), "Expected build-identity debug artifact at $debugArtifactPath")

            val payload = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
            val markdown = Files.readString(run.markdownPath)
            val debugArtifact = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(debugArtifactPath)).jsonObject
            val inputs = payload.getValue("inputs").jsonArray
            val ownerMetrics = payload.getValue("ownerMetrics").jsonArray
            val metricCatalog = payload.getValue("metricCatalog").jsonArray
            val sections = payload.getValue("sections").jsonObject

            assertEquals("report-phase4-v2", payload.getValue("schemaVersion").jsonPrimitive.content)
            assertEquals("P4", payload.getValue("phaseId").jsonPrimitive.content)
            assertEquals("14", payload.getValue("inputCount").jsonPrimitive.content)
            assertEquals("67", payload.getValue("ownerMetricCount").jsonPrimitive.content)
            assertEquals(
                ownerMetrics.count { metric -> metric.jsonObject.getValue("status").jsonPrimitive.content == "UNEXPECTED_REGRESSION" }.toString(),
                payload.getValue("unexpectedRegressionCount").jsonPrimitive.content,
            )
            assertEquals(
                ownerMetrics.count { metric -> metric.jsonObject.getValue("status").jsonPrimitive.content == "APPROVED_DEBT" }.toString(),
                payload.getValue("approvedDebtCount").jsonPrimitive.content,
            )
            assertEquals(
                ownerMetrics.count { metric -> metric.jsonObject.getValue("status").jsonPrimitive.content == "IMPROVEMENT" }.toString(),
                payload.getValue("improvedDebtCount").jsonPrimitive.content,
            )
            assertTrue(payload.containsKey("domainCacheHitRate"))
            assertTrue(payload.containsKey("artifactReuseRate"))
            assertTrue(payload.containsKey("topInvalidationReasons"))
            assertEquals(14, inputs.size)
            assertEquals(67, ownerMetrics.size)

            val terrainInput =
                inputs.first { input -> input.jsonObject.getValue("sourceTaskId").jsonPrimitive.content == "terrainInteractionBatch" }.jsonObject
            val terrainMetric =
                ownerMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "terrainInteractionEncounterRate.aggregate" }.jsonObject
            val bossMetric =
                ownerMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "phaseTransitionObservedRatio" }.jsonObject
            val bossPhaseOverrideMetric =
                ownerMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "bossVariantPhaseOverrideRuntimeTriggerCoverage" }.jsonObject
            val bossPhaseOverrideActionMetric =
                ownerMetrics.first { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "bossVariantPhaseOverrideActionDistinctCount.reportOnly" }.jsonObject
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
            assertEquals("BUDGET_THRESHOLD", bossMetric.getValue("baselineMode").jsonPrimitive.content)
            assertEquals("bossHarness", bossMetric.getValue("sourceTaskId").jsonPrimitive.content)
            assertEquals("PASS", bossMetric.getValue("status").jsonPrimitive.content)
            assertEquals("bossHarness", bossPhaseOverrideMetric.getValue("sourceTaskId").jsonPrimitive.content)
            assertEquals("PASS", bossPhaseOverrideMetric.getValue("status").jsonPrimitive.content)
            assertTrue(bossPhaseOverrideActionMetric.getValue("currentValueText").jsonPrimitive.content.contains("min "))
            assertTrue(bossPhaseOverrideActionMetric.getValue("currentValue").jsonObject.containsKey("countsByVariant"))
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
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "leadDiscoveryRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "secretConversionRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "frontstageHighPriorityCueRetainedRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "frontstageCueDedupAppliedCount" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "frontstageCueExpiryParity" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "frontstageSecretCueVisibilityRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "frontstageSearchCueVisibilityRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "zoneHookCoverage" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "topZoneLeadShare" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "zoneSearchPromptVisibility" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "secretZoneRewardAuthorityViolations" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "topFiveAffixExposureShare" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneSeenRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneAdoptionRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "nonWeaponBuildPayoffRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneSourceCoverage.reportOnly" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneAdoptionFloor" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "nonWeaponBuildPayoffFloor" })
            assertFalse(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneAdoptionFloor.reportOnly" })
            assertFalse(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "nonWeaponBuildPayoffFloor.reportOnly" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "milestoneRewardAdoptionDelta" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "milestoneRewardSlotBalance.maxSlotShare" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "milestoneRewardSlotBalance.WEAPON" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "milestoneRewardSlotBalance.OFF_HAND" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "milestoneRewardSlotBalance.ARMOR" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "milestoneRewardSlotBalance.ACCESSORY" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "milestoneRewardSlotBalance.CONSUMABLE_OR_UTILITY" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "starterInscriptionMaxCount" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "fullSlotInscriptionPurchaseBlockedWithoutReplacementCount" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "inscriptionInstallOrReplaceRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "inscriptionReplacementProbeSuccessCount" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "terminalInscriptionLoadoutDiversity" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "inscriptionCategoryCountDistribution" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "shopInscriptionOfferConversionRate" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "inscriptionReplaceReasonDistribution" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "phaseTransitionObservedRatio" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "variantTraceDivergenceRatio" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "minVariantActionTraceDivergenceScore" })
            assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "bossVariantBasePhaseCountMin" })
            assertEquals(
                criticalPathSection.getValue("criticalPathZoneIds").jsonArray.size,
                criticalPathSection.getValue("designAudit").jsonArray.size,
            )
            assertTrue(markdown.contains("## Scripted vs Organic Hidden"))
            assertTrue(markdown.contains("## Local Reward Identity"))
            assertTrue(markdown.contains("## Critical Path Pacing"))
            assertTrue(markdown.contains("### Critical Path Design Audit"))
            assertTrue(markdown.contains("## Boss Phase Identity"))
            assertTrue(markdown.contains("secret reward identity summaries"))
            assertTrue(markdown.contains("rewardStructureKeys"))
            assertTrue(markdown.contains("loot.deep_iron_slag_cache.secret"))
            assertTrue(markdown.contains("leadDiscoveryRate"))
            assertTrue(markdown.contains("secretConversionRate"))
            assertTrue(markdown.contains("secretZoneRewardAuthorityViolations"))
            assertTrue(markdown.contains("share of total discoveries"))
            assertTrue(markdown.contains("share of total secret-zone entries"))
            assertTrue(markdown.contains("searchPromptRequired"))
            assertTrue(markdown.contains("criticalPathZoneIds"))
            assertTrue(markdown.contains("criticalPathCombatFloorSatisfied"))
            assertTrue(debugArtifact.containsKey("rewardSourceSelections"))
            assertTrue(debugArtifact.containsKey("topRejectedCapstoneCandidates"))
            assertTrue(debugArtifact.containsKey("perProfessionSourceCoverage"))
            assertTrue(
                debugArtifact.getValue("topRejectedCapstoneCandidates").jsonArray.all { candidate ->
                    candidate.jsonObject.containsKey("rejectionReason")
                },
            )
            assertTrue(
                debugArtifact.getValue("perProfessionSourceCoverage").jsonArray.all { coverage ->
                    coverage.jsonObject.containsKey("culpritSourceIds")
                },
            )

            if (compareLegacy) {
                assertNotNull(run.comparisonPath)
                val comparison = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(run.comparisonPath!!)).jsonObject
                assertEquals("0", comparison.getValue("mismatchCount").jsonPrimitive.content)
                assertEquals("67", comparison.getValue("metricCount").jsonPrimitive.content)
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
            Phase4ReportFixtureTestSupport.replaceMetricsFields(
                metrics = metrics,
                replacements = emptyMap(),
            ).let { baselineMetrics ->
                buildJsonObject {
                    baselineMetrics.forEach { (key, value) ->
                        if (key != "dynamicPoolCoverage") {
                            put(key, value)
                        }
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
    fun `reportPhase4 fails fast when reward routing coverage summary is missing from whitebox loot artifact`() {
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

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir.resolve("aggregate-missing-reward-routing-coverage")) {
            val error =
                assertThrows(IllegalStateException::class.java) {
                    ReportPhase4Runner.run(compareLegacy = false)
                }
            assertTrue(error.message.orEmpty().contains("whiteBoxLoot.rewardRoutingCoverageSummary"))
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    fun `reportPhase4 fails fast when organic top-zone evidence is missing from artifact`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val summaryPath = fixtureRepoRoot.resolve("tools/build/reports/phase4/hidden/organic-hidden-probe-summary.json")
        val payload = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(summaryPath)).jsonObject
        val updatedPayload =
            buildJsonObject {
                payload.forEach { (key, value) ->
                    if (key == "summary") {
                        put(
                            key,
                            buildJsonObject {
                                value.jsonObject.forEach { (summaryKey, summaryValue) ->
                                    if (summaryKey != "topZoneLeadShare") {
                                        put(summaryKey, summaryValue)
                                    }
                                }
                            },
                        )
                    } else {
                        put(key, value)
                    }
                }
            }
        Files.writeString(
            summaryPath,
            Phase4ReportFixtureTestSupport.json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                updatedPayload,
            ),
        )

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir.resolve("aggregate-missing-organic-top-share")) {
            val error =
                assertThrows(IllegalStateException::class.java) {
                    ReportPhase4Runner.run(compareLegacy = false)
                }
            assertTrue(error.message.orEmpty().contains("organicHiddenProbe.topZoneLeadShare"))
        }
    }

    @Test
    @Tag("reportPhase4Fixture")
    fun `reportPhase4 fails fast when whitebox solvability lane aggregate is missing from artifact`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        Phase4ReportFixtureTestSupport.mutateWhiteBoxSolvabilityAggregates(fixtureRepoRoot) { aggregates ->
            aggregates.filterNot { aggregate -> aggregate.getValue("groupId").jsonPrimitive.content == "${WhiteBoxSolvabilityFailLane.LANE_ID}:corpus" }
        }

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir.resolve("aggregate-missing-solvability-lane")) {
            val error =
                assertThrows(IllegalStateException::class.java) {
                    ReportPhase4Runner.run(compareLegacy = false)
                }
            assertTrue(error.message.orEmpty().contains("${WhiteBoxSolvabilityFailLane.LANE_ID}:corpus"))
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
    fun `reportPhase4 fails fast when content pack artifacts are semantically aligned but freshness diverges`() {
        val fixtureRepoRoot = Phase4ReportFixtureTestSupport.preparePhase4RepoFixture(tempDir)
        val whiteBoxSummaryPath =
            fixtureRepoRoot.resolve("tools/build/reports/phase4/whitebox/content-pack/whitebox-content-pack-summary.json")
        val whiteBoxPayload = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(whiteBoxSummaryPath)).jsonObject
        val staleWhiteBoxPayload =
            buildJsonObject {
                whiteBoxPayload.forEach { (key, value) ->
                    if (key == "header") {
                        put(
                            key,
                            buildJsonObject {
                                value.jsonObject.forEach { (headerKey, headerValue) ->
                                    put(
                                        headerKey,
                                        if (headerKey == "timestamp") {
                                            JsonPrimitive("1999-12-31T23:59:59Z")
                                        } else {
                                            headerValue
                                        },
                                    )
                                }
                            },
                        )
                    } else {
                        put(key, value)
                    }
                }
            }
        Files.writeString(
            whiteBoxSummaryPath,
            Phase4ReportFixtureTestSupport.json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                staleWhiteBoxPayload,
            ),
        )

        Phase4ReportFixtureTestSupport.withFixtureProperties(repoRoot = fixtureRepoRoot, aggregateReportDir = tempDir.resolve("aggregate-stale-content-pack")) {
            val error =
                assertThrows(IllegalStateException::class.java) {
                    ReportPhase4Runner.run(compareLegacy = false)
                }
            assertTrue(error.message.orEmpty().contains("freshness"))
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
