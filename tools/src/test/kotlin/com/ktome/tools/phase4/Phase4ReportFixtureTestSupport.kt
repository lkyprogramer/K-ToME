package com.ktome.tools.phase4

import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object Phase4ReportFixtureTestSupport {
    val json: Json = Json { prettyPrint = true; explicitNulls = false }
    private const val FIXTURE_SOURCE_ROOT: String = "tools/src/test/resources/fixtures/phase4-report/repo-root"
    private const val FIXTURE_MARKER_FILE: String = ".phase4-report-fixture-id"

    fun preparePhase4RepoFixture(
        tempDir: Path,
        includeLegacySummary: Boolean = false,
    ): Path {
        val sourceRepoRoot = VerificationCacheSupport.repoRoot()
        val fixtureRepoRoot = tempDir.resolve("repo-fixture")
        val fixtureSourceRoot = sourceRepoRoot.resolve(FIXTURE_SOURCE_ROOT)
        require(Files.exists(fixtureSourceRoot.resolve(FIXTURE_MARKER_FILE))) {
            "Missing hermetic Phase 4 report fixture marker under $FIXTURE_SOURCE_ROOT."
        }
        copyRecursively(
            source = fixtureSourceRoot,
            target = fixtureRepoRoot,
        )
        alignContentPackArtifactFreshness(fixtureRepoRoot)
        if (includeLegacySummary) {
            regenerateLegacyPhase4Summary(fixtureRepoRoot)
        }
        return fixtureRepoRoot
    }

    fun mutateWhiteBoxLootCorpusMetrics(
        repoRoot: Path,
        transform: (JsonObject) -> JsonObject,
    ) {
        val summaryPath = repoRoot.resolve("tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json")
        val payload = json.parseToJsonElement(Files.readString(summaryPath)).jsonObject
        val updatedAggregates =
            buildJsonArray {
                payload.getValue("aggregates").jsonArray.forEach { aggregate ->
                    val aggregateObject = aggregate.jsonObject
                    if (aggregateObject.getValue("groupId").jsonPrimitive.content == "corpus") {
                        add(
                            buildJsonObject {
                                aggregateObject.forEach { (key, value) ->
                                    if (key == "metrics") {
                                        put(key, transform(value.jsonObject))
                                    } else {
                                        put(key, value)
                                    }
                                }
                            },
                        )
                    } else {
                        add(aggregate)
                    }
                }
            }
        Files.writeString(
            summaryPath,
            json.encodeToString(
                JsonElement.serializer(),
                buildJsonObject {
                    payload.forEach { (key, value) ->
                        if (key == "aggregates") {
                            put(key, updatedAggregates)
                        } else {
                            put(key, value)
                        }
                    }
                },
            ),
        )
    }

    fun mutateWhiteBoxSolvabilityAggregates(
        repoRoot: Path,
        transform: (List<JsonObject>) -> List<JsonObject>,
    ) {
        val summaryPath = repoRoot.resolve("tools/build/reports/phase4/whitebox/solvability/whitebox-solvability-summary.json")
        val payload = json.parseToJsonElement(Files.readString(summaryPath)).jsonObject
        val aggregates = payload.getValue("aggregates").jsonArray.map(JsonElement::jsonObject)
        val updatedAggregates =
            buildJsonArray {
                transform(aggregates).forEach { aggregate ->
                    add(aggregate)
                }
            }
        Files.writeString(
            summaryPath,
            json.encodeToString(
                JsonElement.serializer(),
                buildJsonObject {
                    payload.forEach { (key, value) ->
                        if (key == "aggregates") {
                            put(key, updatedAggregates)
                        } else {
                            put(key, value)
                        }
                    }
                },
            ),
        )
    }

    fun mutateLongRunSummary(
        repoRoot: Path,
        transform: (JsonObject) -> JsonObject,
    ) {
        val summaryPath = repoRoot.resolve("build/reports/harness/long-run-full.json")
        val payload = json.parseToJsonElement(Files.readString(summaryPath)).jsonObject
        Files.writeString(summaryPath, json.encodeToString(JsonElement.serializer(), transform(payload)))
    }

    fun replaceMetricsField(
        metrics: JsonObject,
        key: String,
        value: JsonElement,
    ): JsonObject = replaceMetricsFields(metrics = metrics, replacements = mapOf(key to value))

    fun replaceMetricsFields(
        metrics: JsonObject,
        replacements: Map<String, JsonElement>,
    ): JsonObject =
        buildJsonObject {
            metrics.forEach { (key, value) ->
                put(key, replacements[key] ?: value)
            }
        }

    fun <T> withFixtureProperties(
        repoRoot: Path,
        aggregateReportDir: Path,
        block: () -> T,
    ): T {
        val originalRepoRoot = System.getProperty("ktome.repo.root")
        val originalReportDir = System.getProperty("ktome.phase4.aggregate.reportDir")
        val originalLegacyReportDir = System.getProperty("ktome.phase4.reportDir")
        try {
            System.setProperty("ktome.repo.root", repoRoot.toString())
            System.setProperty("ktome.phase4.aggregate.reportDir", aggregateReportDir.toString())
            System.setProperty("ktome.phase4.reportDir", repoRoot.resolve("tools/build/reports/phase4").toString())
            return block()
        } finally {
            if (originalRepoRoot == null) {
                System.clearProperty("ktome.repo.root")
            } else {
                System.setProperty("ktome.repo.root", originalRepoRoot)
            }
            if (originalReportDir == null) {
                System.clearProperty("ktome.phase4.aggregate.reportDir")
            } else {
                System.setProperty("ktome.phase4.aggregate.reportDir", originalReportDir)
            }
            if (originalLegacyReportDir == null) {
                System.clearProperty("ktome.phase4.reportDir")
            } else {
                System.setProperty("ktome.phase4.reportDir", originalLegacyReportDir)
            }
        }
    }

    private fun regenerateLegacyPhase4Summary(repoRoot: Path) {
        val originalRepoRoot = System.getProperty("ktome.repo.root")
        val originalLegacyReportDir = System.getProperty("ktome.phase4.reportDir")
        try {
            System.setProperty("ktome.repo.root", repoRoot.toString())
            System.setProperty("ktome.phase4.reportDir", repoRoot.resolve("tools/build/reports/phase4").toString())
            Phase4ReportRunner.run()
        } finally {
            if (originalRepoRoot == null) {
                System.clearProperty("ktome.repo.root")
            } else {
                System.setProperty("ktome.repo.root", originalRepoRoot)
            }
            if (originalLegacyReportDir == null) {
                System.clearProperty("ktome.phase4.reportDir")
            } else {
                System.setProperty("ktome.phase4.reportDir", originalLegacyReportDir)
            }
        }
    }

    private fun alignContentPackArtifactFreshness(repoRoot: Path) {
        val harnessSummaryPath = repoRoot.resolve("tools/build/reports/phase4/content-pack/content-pack-summary.json")
        val whiteBoxSummaryPath = repoRoot.resolve("tools/build/reports/phase4/whitebox/content-pack/whitebox-content-pack-summary.json")
        val harnessPayload = json.parseToJsonElement(Files.readString(harnessSummaryPath)).jsonObject
        val whiteBoxPayload = json.parseToJsonElement(Files.readString(whiteBoxSummaryPath)).jsonObject
        val alignedWhiteBoxPayload =
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
                                            harnessPayload.getValue("header").jsonObject.getValue("timestamp")
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
        Files.writeString(whiteBoxSummaryPath, json.encodeToString(JsonElement.serializer(), alignedWhiteBoxPayload))
    }

    private fun copyRecursively(
        source: Path,
        target: Path,
    ) {
        if (Files.isDirectory(source)) {
            Files.walk(source).use { paths ->
                paths.forEach { current ->
                    val destination = target.resolve(source.relativize(current).toString())
                    if (Files.isDirectory(current)) {
                        Files.createDirectories(destination)
                    } else {
                        Files.createDirectories(destination.parent)
                        Files.copy(current, destination)
                    }
                }
            }
        } else {
            Files.createDirectories(target.parent)
            Files.copy(source, target)
        }
    }
}
