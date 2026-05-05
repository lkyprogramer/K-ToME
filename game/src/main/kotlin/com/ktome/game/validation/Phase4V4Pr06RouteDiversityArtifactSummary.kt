package com.ktome.game.validation

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object Phase4V4Pr06WhiteboxProperties {
    const val PRIMARY_RESULT: String = "ktome.phase4.v4.pr06.primaryResult"
    const val EVIDENCE_RESULT: String = "ktome.phase4.v4.pr06.evidenceResult"
}

internal object Phase4V4Pr06RouteDiversityArtifactSummary {
    private val json = Json { ignoreUnknownKeys = true }
    private val canonicalReportPath = Path.of("tools/build/reports/verification/phase4/report-phase4-summary.json")
    private val longRunProducerPath = Path.of("build/reports/harness/long-run-full.json")
    private val verifyChangedPlanPath = Path.of("build/verification/verify-changed/verify-changed-plan.json")

    fun primaryResultText(): String =
        System.getProperty(Phase4V4Pr06WhiteboxProperties.PRIMARY_RESULT)
            ?.takeIf(String::isNotBlank)
            ?: load(repoRoot()).primaryResultText()

    fun evidenceResultText(): String =
        System.getProperty(Phase4V4Pr06WhiteboxProperties.EVIDENCE_RESULT)
            ?.takeIf(String::isNotBlank)
            ?: load(repoRoot()).evidenceResultText()

    private fun load(repoRoot: Path): Snapshot {
        val canonicalReport = repoRoot.resolve(canonicalReportPath)
        val longRunProducer = repoRoot.resolve(longRunProducerPath)
        val verifyChangedPlan = repoRoot.resolve(verifyChangedPlanPath)
        val canonicalPayload = readJsonObject(canonicalReport)
        val producerPayload = readJsonObject(longRunProducer)
        val routeDiversity =
            canonicalPayload?.objectOrNull("sections")?.objectOrNull("routeDiversity")
                ?: canonicalPayload?.objectOrNull("routeDiversity")
                ?: producerPayload
        val artifactStatus =
            artifactStatus(
                canonicalReport = canonicalReport,
                longRunProducer = longRunProducer,
                routeDiversity = routeDiversity,
            )
        val verifyChangedTasks =
            readJsonObject(verifyChangedPlan)
                ?.stringList("requestedTaskPaths")
                .orEmpty()
        return Snapshot(
            artifactStatus = artifactStatus,
            sourcePath = if (canonicalPayload != null) canonicalReportPath else longRunProducerPath,
            producerArtifactStatus = if (Files.isRegularFile(longRunProducer)) "loaded" else "missing",
            verifyChangedArtifactStatus = if (Files.isRegularFile(verifyChangedPlan)) "loaded" else "missing",
            verifyChangedTasks = verifyChangedTasks,
            routeDiversity = routeDiversity,
        )
    }

    private fun repoRoot(): Path {
        val configured = System.getProperty("ktome.repo.root")
        return if (configured.isNullOrBlank()) {
            Path.of(".").toAbsolutePath().normalize()
        } else {
            Path.of(configured).toAbsolutePath().normalize()
        }
    }

    private fun artifactStatus(
        canonicalReport: Path,
        longRunProducer: Path,
        routeDiversity: JsonObject?,
    ): String =
        when {
            routeDiversity == null -> "unavailable"
            !Files.isRegularFile(canonicalReport) -> "producerOnly"
            Files.isRegularFile(longRunProducer) &&
                Files.getLastModifiedTime(longRunProducer).toMillis() >
                Files.getLastModifiedTime(canonicalReport).toMillis() -> "stale"
            else -> "loaded"
        }

    private fun readJsonObject(path: Path): JsonObject? =
        if (!Files.isRegularFile(path)) {
            null
        } else {
            runCatching { json.parseToJsonElement(Files.readString(path)).jsonObject }.getOrNull()
        }

    private data class Snapshot(
        val artifactStatus: String,
        val sourcePath: Path,
        val producerArtifactStatus: String,
        val verifyChangedArtifactStatus: String,
        val verifyChangedTasks: List<String>,
        val routeDiversity: JsonObject?,
    ) {
        fun primaryResultText(): String {
            val payload = routeDiversity
                ?: return "artifactStatus=unavailable; artifact=$sourcePath; producerArtifactStatus=$producerArtifactStatus"
            val scenarioDistribution = payload.intMap("scenarioTypeDistribution")
            val routeHashDistribution = payload.intMap("zoneRouteHashDistribution")
            val diversity = payload.objectOrNull("zoneRouteHashDiversity")
            val topHashShare = diversity?.doubleValue("topHashShare") ?: 0.0
            val routeTokenSample = payload.stringList("routeTokenSample")
            val secretSamples = routeTokenSample.secretSamples()
            return "artifactStatus=$artifactStatus; " +
                "producerArtifactStatus=$producerArtifactStatus; " +
                "scenarioTypeDistribution=${scenarioDistribution.toCompactText()}; " +
                "zoneRouteHashDistribution=${routeHashDistribution.summaryText()}; " +
                "topHashShare=$topHashShare<=0.40; " +
                "branchInclusiveRoutes=${secretSamples.size}(secret:${secretSamples.joinToString("|")})"
        }

        fun evidenceResultText(): String {
            val payload = routeDiversity
                ?: return "artifactStatus=unavailable; artifact=$sourcePath; producerArtifactStatus=$producerArtifactStatus; verifyChangedArtifactStatus=$verifyChangedArtifactStatus"
            val scenarioDistribution = payload.intMap("scenarioTypeDistribution")
            val diversity = payload.objectOrNull("zoneRouteHashDiversity")
            val topHashShare = diversity?.doubleValue("topHashShare") ?: 1.0
            return "artifactStatus=$artifactStatus; " +
                "producerArtifactStatus=$producerArtifactStatus; " +
                "full_route=${scenarioDistribution.getOrDefault("full_route", 0)}; " +
                "branch_inclusive=${scenarioDistribution.getOrDefault("branch_inclusive", 0)}; " +
                "route_probe=${scenarioDistribution.getOrDefault("route_probe", 0)}; " +
                "late_route_probe=${scenarioDistribution.getOrDefault("late_route_probe", 0)}; " +
                "topHashShare<=0.40:${topHashShare <= 0.4}; " +
                "verifyChangedArtifactStatus=$verifyChangedArtifactStatus; " +
                "verifyChangedTasks=${verifyChangedTasks.summaryText()}"
        }
    }

    private fun JsonObject.objectOrNull(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.stringList(key: String): List<String> =
        (this[key] as? JsonArray)
            ?.mapNotNull { element -> element.jsonPrimitive.contentOrNull }
            .orEmpty()

    private fun JsonObject.intMap(key: String): Map<String, Int> =
        objectOrNull(key)
            ?.entries
            ?.associate { (name, count) -> name to (count.jsonPrimitive.intOrNull ?: 0) }
            .orEmpty()

    private fun JsonObject.intValue(key: String): Int = this[key]?.jsonPrimitive?.intOrNull ?: 0

    private fun JsonObject.doubleValue(key: String): Double = this[key]?.jsonPrimitive?.doubleOrNull ?: 0.0

    private fun Map<String, Int>.toCompactText(): String =
        entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "$key=$value" }

    private fun Map<String, Int>.summaryText(): String {
        val total = values.sum()
        val maxCount = values.maxOrNull() ?: 0
        return "${size}_hashes,max=$maxCount/$total"
    }

    private fun List<String>.secretSamples(): List<String> =
        asSequence()
            .filter { token -> "secret:" in token }
            .mapNotNull { token ->
                token
                    .split(">")
                    .firstOrNull { segment -> segment.startsWith("secret:") }
                    ?.removePrefix("secret:")
            }
            .distinct()
            .take(4)
            .toList()

    private fun List<String>.summaryText(): String {
        val representativeTasks =
            listOf(
                ":game:longRunLab",
                ":tools:scopeCoverageLint",
                ":tools:reportPhase4Only",
                ":tools:maintainabilityLint",
            ).filter { task -> task in this }
        return "${size}_tasks(${representativeTasks.joinToString("|")})"
    }
}
