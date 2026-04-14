package com.ktome.tools.hidden

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.core.map.Point
import com.ktome.core.save.SaveManager
import com.ktome.core.world.solvability.SearchActionResult
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import com.ktome.game.PlayerCommand
import com.ktome.game.harness.RunBot
import com.ktome.game.harness.RunObservation
import com.ktome.game.harness.RunObservationCapture
import com.ktome.game.harness.SmokeBot
import com.ktome.game.harness.commandName
import com.ktome.game.harness.consumesTurn
import com.ktome.game.i18n.GameLocale
import com.ktome.tools.mapgen.phase4HarnessHeader
import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class OrganicHiddenProbeRun(
    val totalCases: Int,
    val runtimeFailureCount: Int,
    val summaryPath: Path,
    val eventsPath: Path,
)

private data class OrganicHiddenProbeCaseSpec(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
)

private data class OrganicHiddenProbeCaseResult(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val turnCount: Int,
    val searchAttemptCount: Int,
    val searchActionUseCount: Int,
    val searchRevealCount: Int,
    val hiddenEventIds: List<String>,
    val secretZoneIds: List<String>,
    val firstHiddenDiscoveryTurn: Int?,
    val firstSecretZoneEntryTurn: Int?,
    val lastCommands: List<String>,
    val finalZoneId: String,
    val finalFloor: Int,
    val runtimeFailure: String? = null,
) {
    val discoveredWithoutPrimer: Boolean
        get() = searchRevealCount > 0 || hiddenEventIds.isNotEmpty() || secretZoneIds.isNotEmpty()

    val secretZoneEntered: Boolean
        get() = secretZoneIds.isNotEmpty()

    fun toJson(header: HarnessReportHeader): JsonObject =
        buildJsonObject {
            put("buildId", header.buildId)
            put("phaseId", header.phaseId)
            put("locale", header.locale)
            put("contentSchemaVersion", header.contentSchemaVersion)
            put("searchRuleVersion", header.searchRuleVersion)
            put("secretRuleVersion", header.secretRuleVersion)
            put("zoneId", zoneId)
            put("floorIndex", floorIndex)
            put("seed", seed)
            put("turnCount", turnCount)
            put("searchAttemptCount", searchAttemptCount)
            put("searchActionUseCount", searchActionUseCount)
            put("searchRevealCount", searchRevealCount)
            put("discoveredWithoutPrimer", discoveredWithoutPrimer)
            put("secretZoneEntered", secretZoneEntered)
            put("firstHiddenDiscoveryTurn", firstHiddenDiscoveryTurn)
            put("firstSecretZoneEntryTurn", firstSecretZoneEntryTurn)
            put("finalZoneId", finalZoneId)
            put("finalFloor", finalFloor)
            putJsonArray("hiddenEventIds") {
                hiddenEventIds.forEach { hiddenEventId -> add(JsonPrimitive(hiddenEventId)) }
            }
            putJsonArray("secretZoneIds") {
                secretZoneIds.forEach { secretZoneId -> add(JsonPrimitive(secretZoneId)) }
            }
            putJsonArray("lastCommands") {
                lastCommands.forEach { command -> add(JsonPrimitive(command)) }
            }
            runtimeFailure?.let { failure -> put("runtimeFailure", failure) }
        }
}

private data class OrganicHiddenProbeShardSpec(
    val zoneId: String,
    val floorIndex: Int,
    val shardIndex: Int,
    val cases: List<OrganicHiddenProbeCaseSpec>,
) {
    val shardId: String
        get() = "$zoneId:$floorIndex:$shardIndex"
}

private data class OrganicHiddenProbeZoneMetrics(
    val caseCount: Int,
    val runsWithSearchActionCount: Int,
    val searchActionUseCount: Int,
    val discoveryWithoutPrimerCount: Int,
    val secretZoneEntryCount: Int,
    val averageFirstHiddenDiscoveryTurn: Double?,
    val averageFirstSecretZoneEntryTurn: Double?,
) {
    val searchActionUseRate: Double
        get() = if (caseCount == 0) 0.0 else runsWithSearchActionCount.toDouble() / caseCount.toDouble()

    val organicHiddenDiscoveryRate: Double
        get() = if (caseCount == 0) 0.0 else discoveryWithoutPrimerCount.toDouble() / caseCount.toDouble()

    val secretZoneEntryRate: Double
        get() = if (caseCount == 0) 0.0 else secretZoneEntryCount.toDouble() / caseCount.toDouble()
}

private data class OrganicHiddenProbeSummary(
    val totalCases: Int,
    val distinctSeedCount: Int,
    val runtimeFailureCount: Int,
    val searchAttemptCount: Int,
    val runsWithSearchActionCount: Int,
    val searchActionUseCount: Int,
    val discoveryWithoutPrimerCount: Int,
    val secretZoneEntryCount: Int,
    val averageFirstHiddenDiscoveryTurn: Double?,
    val averageFirstSecretZoneEntryTurn: Double?,
    val zoneBreakdown: Map<String, OrganicHiddenProbeZoneMetrics>,
) {
    val searchActionUseRate: Double
        get() = if (totalCases == 0) 0.0 else runsWithSearchActionCount.toDouble() / totalCases.toDouble()

    val organicHiddenDiscoveryRate: Double
        get() = if (totalCases == 0) 0.0 else discoveryWithoutPrimerCount.toDouble() / totalCases.toDouble()

    val secretZoneEntryRate: Double
        get() = if (totalCases == 0) 0.0 else secretZoneEntryCount.toDouble() / totalCases.toDouble()
}

object OrganicHiddenProbeRunner {
    const val HARNESS_ID: String = "organicHiddenProbe"

    private const val SUMMARY_FILE: String = "organic-hidden-probe-summary.json"
    private const val EVENTS_FILE: String = "organic-hidden-probe-events.jsonl"
    private const val ORGANIC_HIDDEN_KERNEL_CACHE_VERSION: String = "uvr-pr05-organic-hidden-kernel-v2"
    private const val FLOOR_INDEX: Int = 1
    private const val SEED_BASE: Long = 20260411010000L
    private const val ZONE_SEED_BLOCK: Long = 1_000L
    private const val SEEDS_PER_ZONE: Int = 125
    private const val CASES_PER_SHARD: Int = 25
    private const val TURN_BUDGET: Int = 48
    private const val MAX_FLOOR: Int = 1
    private val json: Json = Json { prettyPrint = true }
    private val zoneIds: List<String> =
        listOf(
            "greenwood_fringe",
            "deep_iron_pit",
            "underground_river",
            "abyssal_temple",
        )

    fun run(): OrganicHiddenProbeRun {
        val repoRoot = VerificationCacheSupport.repoRoot()
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val tempSaveRoot = outputDir.resolve("tmp")
        Files.createDirectories(tempSaveRoot)
        val cases =
            zoneIds.flatMapIndexed { zoneOrdinal, zoneId ->
                (0 until SEEDS_PER_ZONE).map { seedOrdinal ->
                    OrganicHiddenProbeCaseSpec(
                        zoneId = zoneId,
                        floorIndex = FLOOR_INDEX,
                        seed = SEED_BASE + zoneOrdinal * ZONE_SEED_BLOCK + seedOrdinal,
                    )
                }
            }
        val header = phase4HarnessHeader(harnessId = HARNESS_ID, seedList = cases.map(OrganicHiddenProbeCaseSpec::seed))
        val kernelCache = VerificationCacheSupport.cacheDirs(domainId = "organic-hidden", repoRoot = repoRoot)
        val inputFingerprint = VerificationCacheSupport.sha256Files(organicHiddenFingerprintInputs(repoRoot))
        val kernelRoot = VerificationCacheSupport.ensureDirectory(kernelCache.kernelDir.resolve(inputFingerprint))
        var reusedShardCount = 0
        val shardSpecs = buildShardSpecs(cases)
        val shardEventPaths = mutableListOf<Path>()
        val results =
            shardSpecs
                .flatMap { shardSpec ->
                    val shardDir = VerificationCacheSupport.ensureDirectory(kernelRoot.resolve(shardSpec.shardId))
                    val shardSummaryPath = shardDir.resolve("summary.json")
                    val shardEventsPath = shardDir.resolve("events.jsonl")
                    shardEventPaths.add(shardEventsPath)
                    if (Files.isRegularFile(shardSummaryPath) && Files.isRegularFile(shardEventsPath)) {
                        reusedShardCount += 1
                        readShardResults(shardEventsPath)
                    } else {
                        val shardResults =
                            shardSpec.cases.map { caseSpec -> executeCase(caseSpec = caseSpec, tempSaveRoot = tempSaveRoot) }
                        writeShardResults(shardSummaryPath = shardSummaryPath, shardEventsPath = shardEventsPath, header = header, shardSpec = shardSpec, results = shardResults)
                        shardResults
                    }
                }.sortedWith(compareBy(OrganicHiddenProbeCaseResult::zoneId, OrganicHiddenProbeCaseResult::seed))
        val summary = summarize(results)
        val summaryPath = outputDir.resolve(SUMMARY_FILE)
        val eventsPath = outputDir.resolve(EVENTS_FILE)
        Files.writeString(
            summaryPath,
            json.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(
                    header = header,
                    summary = summary,
                    kernelCacheMetadata =
                        buildJsonObject {
                            put("contractVersion", ORGANIC_HIDDEN_KERNEL_CACHE_VERSION)
                            put("inputFingerprint", inputFingerprint)
                            put("cacheStatus", if (reusedShardCount == shardSpecs.size) "HIT" else "MISS")
                            put("reusedShardCount", reusedShardCount)
                            put("shardCount", shardSpecs.size)
                            putJsonArray("shardEventPaths") {
                                shardEventPaths.forEach { shardEventPath ->
                                    add(JsonPrimitive(VerificationCacheSupport.relativeToRepo(shardEventPath, repoRoot)))
                                }
                            }
                        },
                ),
            ),
        )
        VerificationCacheSupport.mergeJsonlFiles(targetPath = eventsPath, sourcePaths = shardEventPaths)
        return OrganicHiddenProbeRun(
            totalCases = results.size,
            runtimeFailureCount = summary.runtimeFailureCount,
            summaryPath = summaryPath,
            eventsPath = eventsPath,
        )
    }

    private fun executeCase(
        caseSpec: OrganicHiddenProbeCaseSpec,
        tempSaveRoot: Path,
    ): OrganicHiddenProbeCaseResult {
        val bot = OrganicHiddenProbeBot(delegate = SmokeBot())
        val commandTail = ArrayDeque<String>()
        return try {
            val session =
                GameModule.newFoundationSession(
                    config =
                        FoundationGameConfig(
                            seed = caseSpec.seed,
                            zoneId = caseSpec.zoneId,
                            floor = caseSpec.floorIndex,
                            playerProfessionId = "arcanist",
                        ),
                    saveManager = SaveManager(tempSaveRoot.resolve("organic-${caseSpec.zoneId}-${caseSpec.seed}")),
                    locale = GameLocale.EN_US,
                )
            val stallDetector = ProbeStallDetector(maxRepeats = 12)
            var turnCount = 0
            var observation = RunObservationCapture.capture(session, turnCount)
            var firstHiddenDiscoveryTurn: Int? = null
            var firstSecretZoneEntryTurn: Int? = null
            var issuedSearchAttemptCount = 0
            var acceptedSearchActionCount = 0
            val cumulativeRevealedBindings = linkedSetOf<String>()
            val cumulativeHiddenEventIds = linkedSetOf<String>()
            val cumulativeSecretZoneIds = linkedSetOf<String>()

            while (turnCount < TURN_BUDGET && !observation.runOutcome.isTerminal && session.currentFloor() <= MAX_FLOOR) {
                val command = bot.decide(observation) ?: PlayerCommand.Wait
                if (command == PlayerCommand.Search) {
                    issuedSearchAttemptCount += 1
                }
                val accepted = session.perform(command)
                if (!accepted) {
                    if (command == PlayerCommand.Search) {
                        bot.recordRejectedSearch(observation)
                        continue
                    }
                    return OrganicHiddenProbeCaseResult(
                        zoneId = caseSpec.zoneId,
                        floorIndex = caseSpec.floorIndex,
                        seed = caseSpec.seed,
                        turnCount = turnCount,
                        searchAttemptCount = issuedSearchAttemptCount,
                        searchActionUseCount = acceptedSearchActionCount,
                        searchRevealCount = cumulativeRevealedBindings.size,
                        hiddenEventIds = cumulativeHiddenEventIds.toList(),
                        secretZoneIds = cumulativeSecretZoneIds.toList(),
                        firstHiddenDiscoveryTurn = firstHiddenDiscoveryTurn,
                        firstSecretZoneEntryTurn = firstSecretZoneEntryTurn,
                        lastCommands = commandTail.toList(),
                        finalZoneId = session.config.zoneId,
                        finalFloor = session.currentFloor(),
                        runtimeFailure = "Command rejected: ${command.commandName()}",
                    )
                }
                if (command == PlayerCommand.Search) {
                    acceptedSearchActionCount += 1
                    bot.recordAcceptedSearch(observation)
                }
                if (command.consumesTurn()) {
                    turnCount += 1
                }
                commandTail.addLast(command.commandName())
                while (commandTail.size > 12) {
                    commandTail.removeFirst()
                }
                observation = RunObservationCapture.capture(session, turnCount)
                val probeState = currentProbeState(session)
                cumulativeRevealedBindings += probeState.revealedBindingIds
                cumulativeHiddenEventIds += probeState.hiddenEventIds
                cumulativeSecretZoneIds += probeState.secretZoneIds
                val discovered =
                    cumulativeRevealedBindings.isNotEmpty() ||
                        cumulativeHiddenEventIds.isNotEmpty() ||
                        cumulativeSecretZoneIds.isNotEmpty()
                if (firstHiddenDiscoveryTurn == null && discovered) {
                    firstHiddenDiscoveryTurn = turnCount
                }
                if (firstSecretZoneEntryTurn == null && cumulativeSecretZoneIds.isNotEmpty()) {
                    firstSecretZoneEntryTurn = turnCount
                }
                if (cumulativeSecretZoneIds.isNotEmpty()) {
                    break
                }
                if (stallDetector.observe(observation.signature()) != null) {
                    break
                }
            }

            OrganicHiddenProbeCaseResult(
                zoneId = caseSpec.zoneId,
                floorIndex = caseSpec.floorIndex,
                seed = caseSpec.seed,
                turnCount = turnCount,
                searchAttemptCount = issuedSearchAttemptCount,
                searchActionUseCount = acceptedSearchActionCount,
                searchRevealCount = cumulativeRevealedBindings.size,
                hiddenEventIds = cumulativeHiddenEventIds.toList().sorted(),
                secretZoneIds = cumulativeSecretZoneIds.toList().sorted(),
                firstHiddenDiscoveryTurn = firstHiddenDiscoveryTurn,
                firstSecretZoneEntryTurn = firstSecretZoneEntryTurn,
                lastCommands = commandTail.toList(),
                finalZoneId = session.config.zoneId,
                finalFloor = session.currentFloor(),
            )
        } catch (exception: Exception) {
            OrganicHiddenProbeCaseResult(
                zoneId = caseSpec.zoneId,
                floorIndex = caseSpec.floorIndex,
                seed = caseSpec.seed,
                turnCount = 0,
                searchAttemptCount = 0,
                searchActionUseCount = 0,
                searchRevealCount = 0,
                hiddenEventIds = emptyList(),
                secretZoneIds = emptyList(),
                firstHiddenDiscoveryTurn = null,
                firstSecretZoneEntryTurn = null,
                lastCommands = commandTail.toList(),
                finalZoneId = caseSpec.zoneId,
                finalFloor = caseSpec.floorIndex,
                runtimeFailure = exception.message ?: exception::class.simpleName.orEmpty(),
            )
        }
    }

    private fun summarize(results: List<OrganicHiddenProbeCaseResult>): OrganicHiddenProbeSummary {
        val zoneBreakdown =
            results.groupBy(OrganicHiddenProbeCaseResult::zoneId)
                .mapValues { (_, zoneResults) ->
                    OrganicHiddenProbeZoneMetrics(
                        caseCount = zoneResults.size,
                        runsWithSearchActionCount = zoneResults.count { result -> result.searchActionUseCount > 0 },
                        searchActionUseCount = zoneResults.sumOf(OrganicHiddenProbeCaseResult::searchActionUseCount),
                        discoveryWithoutPrimerCount = zoneResults.count(OrganicHiddenProbeCaseResult::discoveredWithoutPrimer),
                        secretZoneEntryCount = zoneResults.count(OrganicHiddenProbeCaseResult::secretZoneEntered),
                        averageFirstHiddenDiscoveryTurn = averageOfNullable(zoneResults.map(OrganicHiddenProbeCaseResult::firstHiddenDiscoveryTurn)),
                        averageFirstSecretZoneEntryTurn = averageOfNullable(zoneResults.map(OrganicHiddenProbeCaseResult::firstSecretZoneEntryTurn)),
                    )
                }.toSortedMap()
        return OrganicHiddenProbeSummary(
            totalCases = results.size,
            distinctSeedCount = results.map(OrganicHiddenProbeCaseResult::seed).distinct().size,
            runtimeFailureCount = results.count { result -> result.runtimeFailure != null },
            searchAttemptCount = results.sumOf(OrganicHiddenProbeCaseResult::searchAttemptCount),
            runsWithSearchActionCount = results.count { result -> result.searchActionUseCount > 0 },
            searchActionUseCount = results.sumOf(OrganicHiddenProbeCaseResult::searchActionUseCount),
            discoveryWithoutPrimerCount = results.count(OrganicHiddenProbeCaseResult::discoveredWithoutPrimer),
            secretZoneEntryCount = results.count(OrganicHiddenProbeCaseResult::secretZoneEntered),
            averageFirstHiddenDiscoveryTurn = averageOfNullable(results.map(OrganicHiddenProbeCaseResult::firstHiddenDiscoveryTurn)),
            averageFirstSecretZoneEntryTurn = averageOfNullable(results.map(OrganicHiddenProbeCaseResult::firstSecretZoneEntryTurn)),
            zoneBreakdown = zoneBreakdown,
        )
    }

    private fun buildSummaryPayload(
        header: HarnessReportHeader,
        summary: OrganicHiddenProbeSummary,
        kernelCacheMetadata: JsonObject,
    ): JsonObject =
        buildJsonObject {
            put("header", header.toJson())
            put("kernelCache", kernelCacheMetadata)
            putJsonObject("summary") {
                put("scriptedVerification", false)
                put("primerActionUsedCount", 0)
                put("totalCases", summary.totalCases)
                put("distinctSeedCount", summary.distinctSeedCount)
                put("runtimeFailureCount", summary.runtimeFailureCount)
                put("searchAttemptCount", summary.searchAttemptCount)
                put("runsWithSearchActionCount", summary.runsWithSearchActionCount)
                put("searchActionUseCount", summary.searchActionUseCount)
                put("searchActionUseRate", summary.searchActionUseRate)
                put("discoveryWithoutPrimerCount", summary.discoveryWithoutPrimerCount)
                put("organicHiddenDiscoveryRate", summary.organicHiddenDiscoveryRate)
                put("secretZoneEntryCount", summary.secretZoneEntryCount)
                put("secretZoneEntryRate", summary.secretZoneEntryRate)
                put("averageFirstHiddenDiscoveryTurn", summary.averageFirstHiddenDiscoveryTurn)
                put("averageFirstSecretZoneEntryTurn", summary.averageFirstSecretZoneEntryTurn)
                put("probeBotId", "organic-hidden-probe-bot-v4")
                put("probeTurnBudget", TURN_BUDGET)
                put("probeMaxFloor", MAX_FLOOR)
            }
            putJsonObject("zones") {
                summary.zoneBreakdown.forEach { (zoneId, metrics) ->
                    putJsonObject(zoneId) {
                        put("caseCount", metrics.caseCount)
                        put("runsWithSearchActionCount", metrics.runsWithSearchActionCount)
                        put("searchActionUseCount", metrics.searchActionUseCount)
                        put("searchActionUseRate", metrics.searchActionUseRate)
                        put("discoveryWithoutPrimerCount", metrics.discoveryWithoutPrimerCount)
                        put("organicHiddenDiscoveryRate", metrics.organicHiddenDiscoveryRate)
                        put("secretZoneEntryCount", metrics.secretZoneEntryCount)
                        put("secretZoneEntryRate", metrics.secretZoneEntryRate)
                        put("averageFirstHiddenDiscoveryTurn", metrics.averageFirstHiddenDiscoveryTurn)
                        put("averageFirstSecretZoneEntryTurn", metrics.averageFirstSecretZoneEntryTurn)
                    }
                }
            }
            putJsonArray("notes") {
                add(JsonPrimitive("organicHiddenProbe never uses primer actions or direct reveal APIs."))
                add(JsonPrimitive("organicHiddenProbe uses only RunObservation-visible prompts, interactables, and exploration state for navigation decisions."))
                add(JsonPrimitive("organicHiddenProbe measures real session/bot discovery and is allowed to fail during the initial owner-metric hardening pass."))
            }
        }

    private fun buildShardSpecs(cases: List<OrganicHiddenProbeCaseSpec>): List<OrganicHiddenProbeShardSpec> =
        cases.groupBy(OrganicHiddenProbeCaseSpec::zoneId)
            .toSortedMap()
            .flatMap { (zoneId, zoneCases) ->
                zoneCases
                    .sortedBy(OrganicHiddenProbeCaseSpec::seed)
                    .chunked(CASES_PER_SHARD)
                    .mapIndexed { shardIndex, shardCases ->
                        OrganicHiddenProbeShardSpec(
                            zoneId = zoneId,
                            floorIndex = FLOOR_INDEX,
                            shardIndex = shardIndex,
                            cases = shardCases,
                        )
                    }
            }

    private fun organicHiddenFingerprintInputs(repoRoot: Path): List<Path> =
        listOf(
            repoRoot.resolve("core/src/main/kotlin/com/ktome/core"),
            repoRoot.resolve("tools/src/main/kotlin/com/ktome/tools/hidden"),
            repoRoot.resolve("game/src/main/kotlin/com/ktome/game"),
            repoRoot.resolve("game/src/main/resources/data/events"),
            repoRoot.resolve("game/src/main/resources/data/secret-zones"),
            repoRoot.resolve("game/src/main/resources/data/mapgen"),
        )

    private fun writeShardResults(
        shardSummaryPath: Path,
        shardEventsPath: Path,
        header: HarnessReportHeader,
        shardSpec: OrganicHiddenProbeShardSpec,
        results: List<OrganicHiddenProbeCaseResult>,
    ) {
        Files.createDirectories(shardSummaryPath.parent)
        Files.writeString(
            shardSummaryPath,
            json.encodeToString(
                JsonElement.serializer(),
                buildJsonObject {
                    put("header", header.toJson())
                    put("shardId", shardSpec.shardId)
                    put("zoneId", shardSpec.zoneId)
                    put("floorIndex", shardSpec.floorIndex)
                    put("caseCount", results.size)
                },
            ),
        )
        Files.writeString(
            shardEventsPath,
            results.joinToString(separator = "\n") { result -> Json.encodeToString(JsonElement.serializer(), result.toJson(header)) } + "\n",
        )
    }

    private fun readShardResults(shardEventsPath: Path): List<OrganicHiddenProbeCaseResult> =
        Files.readAllLines(shardEventsPath)
            .filter(String::isNotBlank)
            .map { line -> json.parseToJsonElement(line).jsonObject.toOrganicHiddenProbeCaseResult() }

}

private fun JsonObject.toOrganicHiddenProbeCaseResult(): OrganicHiddenProbeCaseResult =
    OrganicHiddenProbeCaseResult(
        zoneId = getValue("zoneId").jsonPrimitive.content,
        floorIndex = getValue("floorIndex").jsonPrimitive.content.toInt(),
        seed = getValue("seed").jsonPrimitive.content.toLong(),
        turnCount = getValue("turnCount").jsonPrimitive.content.toInt(),
        searchAttemptCount = getValue("searchAttemptCount").jsonPrimitive.content.toInt(),
        searchActionUseCount = getValue("searchActionUseCount").jsonPrimitive.content.toInt(),
        searchRevealCount = getValue("searchRevealCount").jsonPrimitive.content.toInt(),
        hiddenEventIds = getValue("hiddenEventIds").jsonArray.map { hiddenEventId -> hiddenEventId.jsonPrimitive.content },
        secretZoneIds = getValue("secretZoneIds").jsonArray.map { secretZoneId -> secretZoneId.jsonPrimitive.content },
        firstHiddenDiscoveryTurn = this["firstHiddenDiscoveryTurn"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
        firstSecretZoneEntryTurn = this["firstSecretZoneEntryTurn"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
        lastCommands = getValue("lastCommands").jsonArray.map { command -> command.jsonPrimitive.content },
        finalZoneId = getValue("finalZoneId").jsonPrimitive.content,
        finalFloor = getValue("finalFloor").jsonPrimitive.content.toInt(),
        runtimeFailure = this["runtimeFailure"]?.jsonPrimitive?.contentOrNull,
    )

private data class OrganicProbeState(
    val revealedBindingIds: Set<String>,
    val hiddenEventIds: Set<String>,
    val secretZoneIds: Set<String>,
)

private fun currentProbeState(session: com.ktome.game.FoundationGameSession): OrganicProbeState =
    OrganicProbeState(
        revealedBindingIds =
            session.automationSearchState()
                .asSequence()
                .filter { entry -> entry.result == SearchActionResult.REVEALED }
                .map { entry -> entry.bindingId.value }
                .toCollection(linkedSetOf()),
        hiddenEventIds = session.automationConsumedHiddenEventIds().toCollection(linkedSetOf()),
        secretZoneIds = session.automationVisitedSecretZoneIds().mapTo(linkedSetOf()) { secretZoneId -> secretZoneId.id },
    )

private class ProbeStallDetector(
    private val maxRepeats: Int,
) {
    private var lastSignature: String? = null
    private var repeatCount: Int = 0

    fun observe(signature: String): String? {
        if (signature == lastSignature) {
            repeatCount += 1
        } else {
            lastSignature = signature
            repeatCount = 1
        }
        return if (repeatCount >= maxRepeats) {
            "Repeated state $signature for $repeatCount observations."
        } else {
            null
        }
    }
}

private fun RunObservation.signature(): String =
    buildString {
        append(zoneId)
        append('|')
        append(floor)
        append('|')
        append(playerPosition.x)
        append(',')
        append(playerPosition.y)
        append('|')
        append(playerStatus.currentHp)
        append('/')
        append(playerStatus.maxHp)
        append('|')
        append(playerResource.typeId)
        append(':')
        append(playerResource.current)
        append('/')
        append(playerResource.max)
        append('|')
        append(visibleHostilePositions.size)
        append('|')
        append(visibleBossPositions.size)
        append('|')
        append(searchableInteractableSignature())
    }

private fun RunObservation.searchableInteractableSignature(): String =
    visibleInteractables
        .sortedWith(compareBy({ it.position.y }, { it.position.x }, { it.id }))
        .joinToString(separator = ";") { interactable -> "${interactable.id}@${interactable.position.x},${interactable.position.y}" }

internal class OrganicHiddenProbeBot(
    private val delegate: RunBot,
) : RunBot {
    private val searchedPositionsByFloor = mutableMapOf<Int, MutableSet<Point>>()
    private val searchCountByFloor = mutableMapOf<Int, Int>()
    private var lastAcceptedSearchTurn: Int? = null
    private var lastRejectedSearchKey: RejectedSearchKey? = null

    override fun decide(observation: RunObservation): PlayerCommand? = searchCommand(observation) ?: delegate.decide(observation)

    internal fun searchCommand(observation: RunObservation): PlayerCommand? = if (shouldSearch(observation)) PlayerCommand.Search else null

    internal fun recordAcceptedSearch(observation: RunObservation) {
        searchedPositionsByFloor.getOrPut(observation.floor) { linkedSetOf() } += observation.playerPosition
        searchCountByFloor[observation.floor] = (searchCountByFloor[observation.floor] ?: 0) + 1
        lastAcceptedSearchTurn = observation.turnIndex
        lastRejectedSearchKey = null
    }

    internal fun recordRejectedSearch(observation: RunObservation) {
        lastRejectedSearchKey =
            RejectedSearchKey(
                floor = observation.floor,
                position = observation.playerPosition,
                turnIndex = observation.turnIndex,
            )
    }

    private fun shouldSearch(observation: RunObservation): Boolean {
        if (!observation.searchPromptAvailable) {
            return false
        }
        if (
            lastRejectedSearchKey ==
                RejectedSearchKey(
                    floor = observation.floor,
                    position = observation.playerPosition,
                    turnIndex = observation.turnIndex,
                )
        ) {
            return false
        }
        if (observation.activeRouteSelection != null || observation.activeShopId != null || observation.canDescend) {
            return false
        }
        if (observation.visibleBossPositions.isNotEmpty()) {
            return false
        }
        if (observation.visibleHostilePositions.any { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) <= 2 }) {
            return false
        }
        val acceptedSearchTurn = lastAcceptedSearchTurn
        if (acceptedSearchTurn != null && observation.turnIndex - acceptedSearchTurn < 4) {
            return false
        }
        if ((searchCountByFloor[observation.floor] ?: 0) >= 6) {
            return false
        }
        if (observation.playerPosition in searchedPositionsByFloor.getOrPut(observation.floor) { linkedSetOf() }) {
            return false
        }
        return true
    }

    private data class RejectedSearchKey(
        val floor: Int,
        val position: Point,
        val turnIndex: Int,
    )
}

private fun averageOfNullable(values: List<Int?>): Double? {
    val present = values.filterNotNull()
    return if (present.isEmpty()) {
        null
    } else {
        present.average()
    }
}
