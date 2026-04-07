package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class Phase4ReportRun(
    val taskCount: Int,
    val failedTaskCount: Int,
    val summaryPath: Path,
    val markdownPath: Path,
)

private val phase4Json: Json = Json { prettyPrint = true }

private data class Phase4AggregateReport(
    val phaseId: String,
    val generatedAt: String,
    val buildId: String? = null,
    val locale: String? = null,
    val taskCount: Int,
    val passedTaskCount: Int,
    val failedTaskCount: Int,
    val tasks: List<Phase4TaskAggregate>,
)

private data class Phase4TaskAggregate(
    val taskId: String,
    val status: String,
    val sourcePath: String,
    val buildId: String? = null,
    val locale: String? = null,
    val metrics: JsonObject,
)

private data class Phase4TaskDescriptor(
    val relativeSourcePath: String,
    val reader: (repoRoot: Path, sourcePath: Path, payload: JsonObject) -> Phase4TaskAggregate,
) {
    fun read(repoRoot: Path): Phase4TaskAggregate {
        val sourcePath = repoRoot.resolve(relativeSourcePath)
        val payload = readPhase4Json(sourcePath)
        return reader(repoRoot, sourcePath, payload)
    }
}

object Phase4ReportRunner {
    private const val SUMMARY_FILE: String = "phase4-summary.json"
    private const val MARKDOWN_FILE: String = "phase4-summary.md"
    private val json: Json = Json { prettyPrint = true }
    private val taskDescriptors: List<Phase4TaskDescriptor> =
        listOf(
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/mapgen/mapgen-smoke-summary.json",
                reader = ::readMapgenSmoke,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/solvability/solvability-summary.json",
                reader = ::readSolvabilityHarness,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/hidden/hidden-content-summary.json",
                reader = ::readHiddenContentHarness,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "build/reports/harness/boss-harness.json",
                reader = ::readBossHarness,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/terrain/whitebox-terrain-summary.json",
                reader = ::readTerrainInteractionBatch,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/mapgen/whitebox-mapgen-summary.json",
                reader = ::readWhiteBoxMapgen,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/solvability/whitebox-solvability-summary.json",
                reader = ::readWhiteBoxSolvability,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/loot/loot-balance-summary.json",
                reader = ::readLootBalanceLab,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/loot/whitebox-loot-summary.json",
                reader = ::readWhiteBoxLoot,
            ),
            Phase4TaskDescriptor(
                relativeSourcePath = "tools/build/reports/phase4/whitebox/hidden/whitebox-hidden-content-summary.json",
                reader = ::readWhiteBoxHiddenContent,
            ),
        )

    fun run(): Phase4ReportRun {
        val repoRoot = repoRoot()
        val outputDir = reportDir()
        Files.createDirectories(outputDir)

        val taskReports = taskDescriptors.map { descriptor -> descriptor.read(repoRoot) }
        val failedTaskCount = taskReports.count { task -> task.status == "FAIL" }
        val aggregate =
            Phase4AggregateReport(
                phaseId = "P4",
                generatedAt = Instant.now().toString(),
                buildId = taskReports.firstNotNullOfOrNull { task -> task.buildId },
                locale = taskReports.firstNotNullOfOrNull { task -> task.locale },
                taskCount = taskReports.size,
                passedTaskCount = taskReports.count { task -> task.status == "PASS" },
                failedTaskCount = failedTaskCount,
                tasks = taskReports,
            )
        val summaryPath = outputDir.resolve(SUMMARY_FILE)
        val markdownPath = outputDir.resolve(MARKDOWN_FILE)
        Files.writeString(summaryPath, json.encodeToString(JsonElement.serializer(), aggregate.toJson()))
        Files.writeString(markdownPath, renderMarkdown(aggregate))
        return Phase4ReportRun(
            taskCount = aggregate.taskCount,
            failedTaskCount = aggregate.failedTaskCount,
            summaryPath = summaryPath,
            markdownPath = markdownPath,
        )
    }

    private fun readMapgenSmoke(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failureCount = summary.intValue("failureCount")
        val emptyMapCount = summary.intValue("emptyMapCount")
        val unreachableCount = summary.intValue("unreachableCount")
        return Phase4TaskAggregate(
            taskId = "mapgenSmoke",
            status = if (failureCount == 0 && emptyMapCount == 0 && unreachableCount == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("totalCases", summary.intValue("totalCases"))
                    put("distinctSeedCount", summary.intValue("distinctSeedCount"))
                    put("failureCount", failureCount)
                    put("emptyMapCount", emptyMapCount)
                    put("unreachableCount", unreachableCount)
                    put("p95GenerationMillis", summary.intValue("p95GenerationMillis"))
                },
        )
    }

    private fun readSolvabilityHarness(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failureCount = summary.intValue("failureCount")
        val criticalPathFailureCount = summary.intValue("criticalPathFailureCount")
        return Phase4TaskAggregate(
            taskId = "solvabilityHarness",
            status = if (failureCount == 0 && criticalPathFailureCount == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("totalCases", summary.intValue("totalCases"))
                    put("distinctSeedCount", summary.intValue("distinctSeedCount"))
                    put("failureCount", failureCount)
                    put("criticalPathFailureCount", criticalPathFailureCount)
                    put("casesWithBacktrackProof", summary.intValue("casesWithBacktrackProof"))
                    put("casesWithSecretReveal", summary.intValue("casesWithSecretReveal"))
                    put("casesWithSearchFailure", summary.intValue("casesWithSearchFailure"))
                },
        )
    }

    private fun readHiddenContentHarness(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failureCount = summary.intValue("failureCount")
        return Phase4TaskAggregate(
            taskId = "hiddenContentHarness",
            status = if (failureCount == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("totalCases", summary.intValue("totalCases"))
                    put("distinctSeedCount", summary.intValue("distinctSeedCount"))
                    put("failureCount", failureCount)
                    put("caseFailureCount", summary.intValue("caseFailureCount"))
                    put("aggregateFailureCount", summary.intValue("aggregateFailureCount"))
                    put("hiddenEventTriggerCount", summary.intValue("hiddenEventTriggerCount"))
                    put("hiddenEventTriggerRate", summary.doubleValue("hiddenEventTriggerRate"))
                    put("secretZoneDiscoveryCount", summary.intValue("secretZoneDiscoveryCount"))
                    put("secretZoneDiscoveryRate", summary.doubleValue("secretZoneDiscoveryRate"))
                    put("explicitSearchRevealCount", summary.intValue("explicitSearchRevealCount"))
                    put("searchFailureCount", summary.intValue("searchFailureCount"))
                    put("zeroHiddenEventZoneCount", summary.intValue("zeroHiddenEventZoneCount"))
                    put("zeroSecretZoneZoneCount", summary.intValue("zeroSecretZoneZoneCount"))
                    put("criticalPathFailureCount", summary.intValue("criticalPathFailureCount"))
                    put("triggerContextFailureCount", summary.intValue("triggerContextFailureCount"))
                    put("secretRewardNodeMissingCount", summary.intValue("secretRewardNodeMissingCount"))
                    put("rewardBudgetFailureCount", summary.intValue("rewardBudgetFailureCount"))
                    put("threatBudgetFailureCount", summary.intValue("threatBudgetFailureCount"))
                    put("searchFailureBlockingCount", summary.intValue("searchFailureBlockingCount"))
                    put("proofMismatchCount", summary.intValue("proofMismatchCount"))
                    put("runtimeReturnDestinationMismatchCount", summary.intValue("runtimeReturnDestinationMismatchCount"))
                },
        )
    }

    private fun readBossHarness(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val reports = payload.getValue("reports").jsonArray
        val pairReports = payload["pairReports"]?.jsonArray.orEmpty()
        val whiteBoxSourcePath = repoRoot.resolve("tools/build/reports/phase4/whitebox/boss/whitebox-boss-summary.json")
        val whiteBoxPayload = readPhase4Json(whiteBoxSourcePath)
        val whiteBoxSummary = whiteBoxPayload.getValue("summary").jsonObject
        val whiteBoxFailedAssertions = whiteBoxSummary.intValue("failedAssertions")
        val whiteBoxFirstFailedJoinKey = whiteBoxPayload["firstFailedJoinKey"]
        val failureCount =
            reports.count { element -> !element.jsonObject.getValue("success").jsonPrimitive.content.toBooleanStrict() } +
                pairReports.count { element -> !element.jsonObject.getValue("success").jsonPrimitive.content.toBooleanStrict() }
        val aiTraceCountTotal = reports.sumOf { element -> element.jsonObject.intValue("aiTraceCount") }
        val bossTraceCountTotal = reports.sumOf { element -> element.jsonObject.intValue("bossTraceCount") }
        val distinctTemplateCount = reports.map { element -> element.jsonObject.stringValue("templateId") }.distinct().size
        val variantCount = reports.count { element -> "variantId" in element.jsonObject }
        val phaseGraphStructuralDiffCount = pairReports.sumOf { element -> element.jsonObject.intValue("phaseGraphStructuralDiffCount") }
        return Phase4TaskAggregate(
            taskId = "bossHarness",
            status = if (failureCount == 0 && whiteBoxFailedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            metrics =
                buildJsonObject {
                    put("scriptVersion", payload.getValue("scriptVersion").jsonPrimitive.content)
                    put("reportCount", reports.size)
                    put("pairCount", pairReports.size)
                    put("failureCount", failureCount)
                    put("distinctTemplateCount", distinctTemplateCount)
                    put("variantCount", variantCount)
                    put("aiTraceCountTotal", aiTraceCountTotal)
                    put("bossTraceCountTotal", bossTraceCountTotal)
                    put("phaseGraphStructuralDiffCount", phaseGraphStructuralDiffCount)
                    put("whiteBoxFailedAssertions", whiteBoxFailedAssertions)
                    put("whiteBoxFailedCaseCount", whiteBoxPayload.intValue("failedCaseCount"))
                    put("whiteBoxFailedAggregateCount", whiteBoxPayload.intValue("failedAggregateCount"))
                    put("whiteBoxArtifactCount", whiteBoxSummary.intValue("artifactCount"))
                    put("whiteBoxSummaryPath", relativize(repoRoot, whiteBoxSourcePath))
                    whiteBoxFirstFailedJoinKey?.let { joinKey -> put("whiteBoxFirstFailedJoinKey", joinKey.toString()) }
                },
        )
    }

    private fun readTerrainInteractionBatch(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failedAssertions = summary.intValue("failedAssertions")
        return Phase4TaskAggregate(
            taskId = "terrainInteractionBatch",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                    put("failedCaseCount", payload.intValue("failedCaseCount"))
                    put("failedAggregateCount", payload.intValue("failedAggregateCount"))
                    payload["firstFailedJoinKey"]?.let { joinKey -> put("firstFailedJoinKey", joinKey.toString()) }
                },
        )
    }

    private fun readWhiteBoxMapgen(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failedAssertions = summary.intValue("failedAssertions")
        return Phase4TaskAggregate(
            taskId = "whiteBoxMapgen",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                },
        )
    }

    private fun readWhiteBoxSolvability(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failedAssertions = summary.intValue("failedAssertions")
        return Phase4TaskAggregate(
            taskId = "whiteBoxSolvability",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                },
        )
    }

    private fun readLootBalanceLab(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val clamp = payload.getValue("magicFindClampComparison").jsonObject
        val failedExpectationCount = summary.intValue("failedExpectationCount")
        return Phase4TaskAggregate(
            taskId = "lootBalanceLab",
            status = if (failedExpectationCount == 0 && clamp.getValue("withinTolerance").jsonPrimitive.content.toBooleanStrict()) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("matrixCount", summary.intValue("matrixCount"))
                    put("totalRolls", summary.intValue("totalRolls"))
                    put("failedExpectationCount", failedExpectationCount)
                    put("rarePityActivations", summary.intValue("rarePityActivations"))
                    put("uniquePityActivations", summary.intValue("uniquePityActivations"))
                    put("maxMagicRateDrift", summary.getValue("maxMagicRateDrift").jsonPrimitive.content.toDouble())
                    put("maxRareRateDrift", summary.getValue("maxRareRateDrift").jsonPrimitive.content.toDouble())
                    put("maxUniqueRelativeError", summary.getValue("maxUniqueRelativeError").jsonPrimitive.content.toDouble())
                    put("maxArtifactRelativeError", summary.getValue("maxArtifactRelativeError").jsonPrimitive.content.toDouble())
                    put("clampWithinTolerance", clamp.getValue("withinTolerance").jsonPrimitive.content.toBooleanStrict())
                    put("clampMaxDistributionDelta", clamp.getValue("maxDistributionDelta").jsonPrimitive.content.toDouble())
                },
        )
    }

    private fun readWhiteBoxLoot(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failedAssertions = summary.intValue("failedAssertions")
        return Phase4TaskAggregate(
            taskId = "whiteBoxLoot",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                },
        )
    }

    private fun readWhiteBoxHiddenContent(
        repoRoot: Path,
        sourcePath: Path,
        payload: JsonObject,
    ): Phase4TaskAggregate {
        val header = payload.getValue("header").jsonObject
        val summary = payload.getValue("summary").jsonObject
        val failedAssertions = summary.intValue("failedAssertions")
        return Phase4TaskAggregate(
            taskId = "whiteBoxHiddenContent",
            status = if (failedAssertions == 0) "PASS" else "FAIL",
            sourcePath = relativize(repoRoot, sourcePath),
            buildId = header.stringValue("buildId"),
            locale = header.stringValue("locale"),
            metrics =
                buildJsonObject {
                    put("caseCount", summary.intValue("caseCount"))
                    put("aggregateCount", summary.intValue("aggregateCount"))
                    put("failedAssertions", failedAssertions)
                    put("artifactCount", summary.intValue("artifactCount"))
                    put("failedCaseCount", payload.intValue("failedCaseCount"))
                    put("failedAggregateCount", payload.intValue("failedAggregateCount"))
                    payload["firstFailedJoinKey"]?.let { joinKey -> put("firstFailedJoinKey", joinKey.toString()) }
                },
        )
    }

    private fun renderMarkdown(report: Phase4AggregateReport): String =
        buildString {
            appendLine("# Phase 4 Report")
            appendLine()
            appendLine("- generatedAt: `${report.generatedAt}`")
            report.buildId?.let { buildId -> appendLine("- buildId: `${buildId}`") }
            report.locale?.let { locale -> appendLine("- locale: `${locale}`") }
            appendLine("- taskCount: `${report.taskCount}`")
            appendLine("- passedTaskCount: `${report.passedTaskCount}`")
            appendLine("- failedTaskCount: `${report.failedTaskCount}`")
            appendLine()
            appendLine("## Tasks")
            report.tasks.forEach { task ->
                appendLine("### `${task.taskId}` - ${task.status}")
                appendLine("- sourcePath: `${task.sourcePath}`")
                appendLine("```json")
                appendLine(json.encodeToString(JsonObject.serializer(), task.metrics))
                appendLine("```")
            }
        }

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4")
        } else {
            Path.of(configured)
        }
    }

    private fun repoRoot(): Path {
        val configured = System.getProperty("ktome.repo.root")
        return if (configured.isNullOrBlank()) Path.of(".").toAbsolutePath().normalize() else Path.of(configured).toAbsolutePath().normalize()
    }

    private fun relativize(
        repoRoot: Path,
        path: Path,
    ): String = repoRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/')
}

private fun Phase4AggregateReport.toJson(): JsonObject =
    buildJsonObject {
        put("phaseId", phaseId)
        put("generatedAt", generatedAt)
        buildId?.let { value -> put("buildId", value) }
        locale?.let { value -> put("locale", value) }
        put("taskCount", taskCount)
        put("passedTaskCount", passedTaskCount)
        put("failedTaskCount", failedTaskCount)
        putJsonArray("tasks") {
            tasks.forEach { task -> add(task.toJson()) }
        }
    }

private fun Phase4TaskAggregate.toJson(): JsonObject =
    buildJsonObject {
        put("taskId", taskId)
        put("status", status)
        put("sourcePath", sourcePath)
        buildId?.let { value -> put("buildId", value) }
        locale?.let { value -> put("locale", value) }
        putJsonObject("metrics") {
            metrics.forEach { (key, value) -> put(key, value) }
        }
    }

private fun readPhase4Json(path: Path): JsonObject {
    check(Files.exists(path)) { "Missing phase4 report source: $path" }
    return phase4Json.parseToJsonElement(Files.readString(path)).jsonObject
}

private fun JsonObject.intValue(key: String): Int = (getValue(key) as JsonPrimitive).content.toInt()

private fun JsonObject.doubleValue(key: String): Double = (getValue(key) as JsonPrimitive).content.toDouble()

private fun JsonObject.stringValue(key: String): String = (getValue(key) as JsonPrimitive).content
