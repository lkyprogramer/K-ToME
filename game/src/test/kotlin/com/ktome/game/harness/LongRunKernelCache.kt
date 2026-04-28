package com.ktome.game.harness

import com.ktome.core.item.EquipSlot
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.loot.RarityTier
import com.ktome.core.profile.MilestoneRewardSummary
import com.ktome.core.run.RunOutcome
import com.ktome.core.world.ObjectiveState
import com.ktome.game.BreakpointPayoffObservation
import com.ktome.game.BreakpointPayoffSummary
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Comparator
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal data class LongRunKernelExecution(
    val reports: List<ScenarioReport>,
    val inputFingerprint: String,
    val cacheStatus: String,
    val reusedShardCount: Int,
    val shardCount: Int,
    val cacheRoot: Path,
    val shardReportPaths: List<Path>,
) {
    fun cacheMetadata(repoRoot: Path): JsonObject =
        buildJsonObject {
            put("contractVersion", LONGRUN_KERNEL_CACHE_VERSION)
            put("inputFingerprint", inputFingerprint)
            put("cacheStatus", cacheStatus)
            put("reusedShardCount", reusedShardCount)
            put("shardCount", shardCount)
            put("artifactReuseSource", repoRoot.relativize(cacheRoot.toAbsolutePath().normalize()).toString().replace('\\', '/'))
            putJsonArray("shardReportPaths") {
                shardReportPaths.forEach { shardReportPath ->
                    add(JsonPrimitive(repoRoot.relativize(shardReportPath.toAbsolutePath().normalize()).toString().replace('\\', '/')))
                }
            }
        }
}

internal object LongRunKernelCache {
    private val json: Json =
        Json {
            prettyPrint = true
            explicitNulls = false
        }

    fun execute(
        rootDir: Path,
        specs: List<ScenarioSpec>,
    ): LongRunKernelExecution {
        val repoRoot = repoRoot()
        val inputFingerprint = inputFingerprint(repoRoot)
        val cacheRoot = ensureDirectory(repoRoot.resolve("tools/build/verification-cache/kernels/longrun").resolve(inputFingerprint))
        val harness = HeadlessRunHarness(rootDir = rootDir)
        var reusedShardCount = 0
        val shardReportPaths = mutableListOf<Path>()
        val reports =
            specs
                .sortedBy(ScenarioSpec::name)
                .map { spec ->
                    val shardReportPath = cacheRoot.resolve("${scenarioShardId(spec)}.json")
                    shardReportPaths.add(shardReportPath)
                    if (Files.isRegularFile(shardReportPath)) {
                        reusedShardCount += 1
                        parseScenarioReport(json.parseToJsonElement(Files.readString(shardReportPath)).jsonObject)
                    } else {
                        val report = harness.run(spec)
                        Files.createDirectories(shardReportPath.parent)
                        Files.writeString(shardReportPath, json.encodeToString(kotlinx.serialization.json.JsonElement.serializer(), report.toJson()))
                        report
                    }
                }
        return LongRunKernelExecution(
            reports = reports,
            inputFingerprint = inputFingerprint,
            cacheStatus = if (reusedShardCount == specs.size) "HIT" else "MISS",
            reusedShardCount = reusedShardCount,
            shardCount = specs.size,
            cacheRoot = cacheRoot,
            shardReportPaths = shardReportPaths,
        )
    }

    private fun scenarioShardId(spec: ScenarioSpec): String =
        listOf(
            spec.scenarioType.reportValue,
            spec.professionId,
            spec.raceId,
            spec.seed.toString(),
            spec.routeIndex.toString(),
            sha256(
                buildString {
                    append(spec.zoneId)
                    append('|')
                    append(spec.zoneRoute.joinToString(separator = "->"))
                    append('|')
                    append(spec.maxTurns)
                    append('|')
                    append(goalKey(spec.goal))
                    append('|')
                    append(checkpointKey(spec.saveLoadCheckpoint))
                    append('|')
                    append(spec.assertions.joinToString(separator = "|", transform = ::assertionKey))
                },
            ).take(16),
        ).joinToString(separator = "__")

    private fun goalKey(goal: ScenarioGoal): String =
        when (goal) {
            is ScenarioGoal.ReachFloor -> "reach-floor:${goal.floor}"
            ScenarioGoal.ReachTerminal -> "reach-terminal"
            ScenarioGoal.Victory -> "victory"
            is ScenarioGoal.ReachFloorOrTerminal -> "reach-floor-or-terminal:${goal.floor}"
            is ScenarioGoal.ReachZoneAtLeastOrTerminal -> "reach-zone-or-terminal:${goal.zoneId}"
            is ScenarioGoal.SurviveTurns -> "survive-turns:${goal.turns}"
        }

    private fun checkpointKey(checkpoint: SaveLoadCheckpoint?): String =
        if (checkpoint == null) {
            "no-checkpoint"
        } else {
            "checkpoint:${checkpoint.floor}:${checkpoint.continueTurns}"
        }

    private fun assertionKey(assertion: ScenarioAssertion): String =
        when (assertion) {
            is ScenarioAssertion.ReachedFloorAtLeast -> "reached-floor-at-least:${assertion.floor}"
            ScenarioAssertion.NoStall -> "no-stall"
            ScenarioAssertion.NoFailure -> "no-failure"
            ScenarioAssertion.CheckpointRoundTrip -> "checkpoint-round-trip"
            ScenarioAssertion.Victory -> "victory"
            is ScenarioAssertion.FinalZoneAtLeast -> "final-zone-at-least:${assertion.zoneId}"
            is ScenarioAssertion.VisitedZone -> "visited-zone:${assertion.zoneId}"
        }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(value.toByteArray())
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun inputFingerprint(repoRoot: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputFingerprintRoots(repoRoot).forEach { path -> updateDigestForPath(digest = digest, path = path) }
        digest.update(LONGRUN_KERNEL_CACHE_VERSION.toByteArray())
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    internal fun inputFingerprintRoots(repoRoot: Path): List<Path> =
        listOf(
            repoRoot.resolve("core/src/main/kotlin/com/ktome/core"),
            repoRoot.resolve("game/src/main/kotlin/com/ktome/game"),
            repoRoot.resolve("game/src/test/kotlin/com/ktome/game/harness"),
            repoRoot.resolve("game/src/main/resources/data"),
        )

    private fun ensureDirectory(path: Path): Path {
        Files.createDirectories(path)
        return path
    }

    private fun repoRoot(): Path {
        val configured = System.getProperty("ktome.repo.root")
        return if (configured.isNullOrBlank()) Path.of(".").toAbsolutePath().normalize() else Path.of(configured).toAbsolutePath().normalize()
    }

    private fun updateDigestForPath(
        digest: MessageDigest,
        path: Path,
    ) {
        if (!Files.exists(path)) {
            digest.update("missing:${path.toAbsolutePath().normalize()}".toByteArray())
            return
        }
        Files.walk(path).use { paths ->
            paths
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .sorted(Comparator.comparing(Path::toString))
                .forEach { currentPath ->
                    digest.update(currentPath.toString().toByteArray())
                    when {
                        Files.isRegularFile(currentPath) -> digest.update(Files.readAllBytes(currentPath))
                        Files.isDirectory(currentPath) -> digest.update("dir:${currentPath.fileName}".toByteArray())
                    }
                }
        }
    }

    private fun parseScenarioReport(payload: JsonObject): ScenarioReport =
        ScenarioReport(
            name = payload.requiredString("name"),
            seed = payload.requiredString("seed").toLong(),
            zoneId = payload.requiredString("zoneId"),
            professionId = payload.requiredString("professionId"),
            raceId = payload.requiredString("raceId"),
            routeIndex = payload.requiredString("routeIndex").toInt(),
            finalZoneId = payload.requiredString("finalZoneId"),
            zoneRouteHash = payload.requiredString("zoneRouteHash"),
            zonePath = payload.requiredArray("zonePath").map { zoneId -> zoneId.jsonPrimitive.content },
            scenarioType = ScenarioType.entries.first { scenarioType -> scenarioType.reportValue == payload.requiredString("scenarioType") },
            success = payload.requiredString("success").toBooleanStrict(),
            outcome = parseRunOutcome(payload.requiredString("outcome")),
            floorReached = payload.requiredString("floorReached").toInt(),
            turns = payload.requiredString("turns").toInt(),
            headlessTurnEquivalent = payload.requiredString("headlessTurnEquivalent").toInt(),
            buildId = payload.requiredString("buildId"),
            phaseId = payload.requiredString("phaseId"),
            rulesetVersion = payload.requiredString("rulesetVersion"),
            traceSchemaVersion = payload.requiredString("traceSchemaVersion"),
            corpusId = payload.requiredString("corpusId"),
            localeId = payload.requiredString("localeId"),
            profileId = payload.requiredString("profileId"),
            buildHash = payload["buildHash"]?.jsonPrimitive?.contentOrNull,
            terminalWeaponBaseId = payload["terminalWeaponBaseId"]?.jsonPrimitive?.contentOrNull,
            breakpointPayoffs = payload.requiredArray("breakpointPayoffs").map { payoff -> parseBreakpointPayoff(payoff.jsonObject) },
            breakpointPayoffObservations = payload.requiredArray("breakpointPayoffObservations").map { observation -> parseBreakpointPayoffObservation(observation.jsonObject) },
            milestoneRewards = payload.requiredArray("milestoneRewards").map { reward -> parseMilestoneReward(reward.jsonObject) },
            cadenceRewardCount = payload.requiredString("cadenceRewardCount").toInt(),
            shopRefreshPurchaseCount = payload.requiredString("shopRefreshPurchaseCount").toInt(),
            lateRunReliquaryPurchaseCount = payload.requiredString("lateRunReliquaryPurchaseCount").toInt(),
            lateRunReliquaryVisitCount = payload.requiredString("lateRunReliquaryVisitCount").toInt(),
            lateRunReliquaryRefreshCount = payload.requiredString("lateRunReliquaryRefreshCount").toInt(),
            lateRunReliquaryItemPurchaseCount = payload.requiredString("lateRunReliquaryItemPurchaseCount").toInt(),
            lateRunReliquaryNonMandatoryPurchaseCount = payload.requiredString("lateRunReliquaryNonMandatoryPurchaseCount").toInt(),
            lateRunReliquaryShardSpent = payload.requiredString("lateRunReliquaryShardSpent").toInt(),
            lateRunReliquaryTagDistribution = payload.requiredObject("lateRunReliquaryTagDistribution").entries.associate { (tag, count) -> tag to count.jsonPrimitive.content.toInt() },
            affixSynergyActivationCount = payload.requiredString("affixSynergyActivationCount").toInt(),
            affixSynergyActivationDistribution = payload.requiredObject("affixSynergyActivationDistribution").entries.associate { (affixId, count) -> affixId to count.jsonPrimitive.content.toInt() },
            starterProfessionTalentCount = payload.optionalInt("starterProfessionTalentCount"),
            learnedTalentChoiceEventCount = payload.optionalInt("learnedTalentChoiceEventCount"),
            learnableNonStarterTalentCount = payload.optionalInt("learnableNonStarterTalentCount"),
            breakpointChoiceEventCount = payload.optionalInt("breakpointChoiceEventCount"),
            breakpointPreviewAvailable = payload.optionalBoolean("breakpointPreviewAvailable"),
            talentTreeInvestmentByTree = payload.optionalObject("talentTreeInvestmentByTree").entries.associate { (treeId, points) -> treeId to points.jsonPrimitive.content.toInt() },
            talentTreePrimaryInvestmentTreeId = payload["talentTreePrimaryInvestmentTreeId"]?.jsonPrimitive?.contentOrNull,
            talentTreePrimaryInvestmentPoints = payload.optionalInt("talentTreePrimaryInvestmentPoints"),
            multiTreeInvestmentAboveThreshold = payload.optionalBoolean("multiTreeInvestmentAboveThreshold"),
            talentReserveSwapCount = payload.optionalInt("talentReserveSwapCount"),
            rankBreakpointAdoptionByTalent = payload.optionalObject("rankBreakpointAdoptionByTalent").entries.associate { (talentId, count) -> talentId to count.jsonPrimitive.content.toInt() },
            autoLearnedNonStarterTalentCount = payload.optionalInt("autoLearnedNonStarterTalentCount"),
            startingInscriptionCount = payload.optionalInt("startingInscriptionCount"),
            inscriptionInstallCount = payload.optionalInt("inscriptionInstallCount"),
            inscriptionReplaceCount = payload.optionalInt("inscriptionReplaceCount"),
            fullSlotInscriptionPurchaseBlockedWithoutReplacementCount = payload.optionalInt("fullSlotInscriptionPurchaseBlockedWithoutReplacementCount"),
            inscriptionPurchaseCancelledAfterReplacementPrompt = payload.optionalInt("inscriptionPurchaseCancelledAfterReplacementPrompt"),
            shopPurchaseDeniedInsufficientGoldCount = payload.optionalInt("shopPurchaseDeniedInsufficientGoldCount"),
            shopInscriptionOfferSeenCount = payload.optionalInt("shopInscriptionOfferSeenCount"),
            shopInscriptionOfferPurchaseCount = payload.optionalInt("shopInscriptionOfferPurchaseCount"),
            terminalInscriptionLoadout = payload["terminalInscriptionLoadout"]?.jsonArray?.map { inscriptionId -> inscriptionId.jsonPrimitive.content }.orEmpty(),
            terminalInscriptionCategoryCounts = payload.optionalObject("terminalInscriptionCategoryCounts").entries.associate { (categoryId, count) -> categoryId to count.jsonPrimitive.content.toInt() },
            inscriptionReplaceReasonDistribution = payload.optionalObject("inscriptionReplaceReasonDistribution").entries.associate { (reason, count) -> reason to count.jsonPrimitive.content.toInt() },
            goalReached = payload.requiredString("goalReached").toBooleanStrict(),
            failureReason = payload["failureReason"]?.jsonPrimitive?.contentOrNull,
            stuckReason = payload["stuckReason"]?.jsonPrimitive?.contentOrNull,
            checkpointRoundTripVerified = payload.requiredString("checkpointRoundTripVerified").toBooleanStrict(),
            commandStats = payload["commandStats"]?.jsonObject?.entries?.associate { (command, count) -> command to count.jsonPrimitive.content.toInt() }.orEmpty(),
            zoneHeadlessMilestones = payload["zoneHeadlessMilestones"]?.jsonArray?.map { milestone -> parseZoneHeadlessMilestone(milestone.jsonObject) }.orEmpty(),
            zoneObjectiveSummaries = payload["zoneObjectiveSummaries"]?.jsonArray?.map { summary -> parseZoneObjectiveSummary(summary.jsonObject) }.orEmpty(),
            zoneTraversalDiagnostics = payload["zoneTraversalDiagnostics"]?.jsonArray?.map { diagnostic -> parseZoneTraversalDiagnostic(diagnostic.jsonObject) }.orEmpty(),
            captainEncounterTrace = payload["captainEncounterTrace"]?.jsonArray?.map { entry -> parseCaptainEncounterTrace(entry.jsonObject) }.orEmpty(),
            lastCommands = payload["lastCommands"]?.jsonArray?.map { command -> command.jsonPrimitive.content }.orEmpty(),
            lastMessages = payload["lastMessages"]?.jsonArray?.map { message -> message.jsonPrimitive.content }.orEmpty(),
            eventTail = payload["eventTail"]?.jsonArray?.map { event -> event.jsonPrimitive.content }.orEmpty(),
        )

    private fun parseBreakpointPayoff(payload: JsonObject): BreakpointPayoffSummary =
        BreakpointPayoffSummary(
            talentId = payload.requiredString("talentId"),
            treeId = payload.requiredString("treeId"),
            achievedRank = payload.requiredString("achievedRank").toInt(),
            breakpointRank = payload.requiredString("breakpointRank").toInt(),
            unlockedEffectKinds = payload.requiredArray("unlockedEffectKinds").map { effectKind -> effectKind.jsonPrimitive.content },
        )

    private fun parseBreakpointPayoffObservation(payload: JsonObject): BreakpointPayoffObservation =
        BreakpointPayoffObservation(
            talentId = payload.requiredString("talentId"),
            treeId = payload.requiredString("treeId"),
            achievedRank = payload.requiredString("achievedRank").toInt(),
            breakpointRank = payload.requiredString("breakpointRank").toInt(),
            unlockedEffectKinds = payload.requiredArray("unlockedEffectKinds").map { effectKind -> effectKind.jsonPrimitive.content },
            turnIndex = payload.requiredString("turnIndex").toInt(),
            headlessTurnEquivalent = payload.requiredString("headlessTurnEquivalent").toInt(),
            buildHashBeforeUnlock = payload.requiredString("buildHashBeforeUnlock"),
            buildHashAfterUnlock = payload.requiredString("buildHashAfterUnlock"),
            buildHashChanged = payload.requiredString("buildHashChanged").toBooleanStrict(),
        )

    private fun parseMilestoneReward(payload: JsonObject): MilestoneRewardSummary =
        MilestoneRewardSummary(
            rewardSource = MilestoneRewardSource.valueOf(payload.requiredString("rewardSource")),
            sourceId = payload.requiredString("sourceId"),
            zoneId = payload.requiredString("zoneId"),
            baseItemId = payload.requiredString("baseItemId"),
            equipSlot = EquipSlot.valueOf(payload.requiredString("equipSlot")),
            qualityTier = RarityTier.valueOf(payload.requiredString("qualityTier")),
            buildHashAtGrant = payload.requiredString("buildHashAtGrant"),
            affixIds = payload.requiredArray("affixIds").map { affixId -> affixId.jsonPrimitive.content },
            equippedBaseItemIdBeforeReward = payload["equippedBaseItemIdBeforeReward"]?.jsonPrimitive?.contentOrNull,
            equippedBaseItemIdAtRunEnd = payload["equippedBaseItemIdAtRunEnd"]?.jsonPrimitive?.contentOrNull,
            adoptedInFinalBuild = payload.requiredString("adoptedInFinalBuild").toBooleanStrict(),
        )

    private fun parseZoneHeadlessMilestone(payload: JsonObject): ZoneHeadlessMilestone =
        ZoneHeadlessMilestone(
            zoneId = payload.requiredString("zoneId"),
            turnIndex = payload.requiredString("turnIndex").toInt(),
            headlessTurnEquivalent = payload.requiredString("headlessTurnEquivalent").toInt(),
            deltaTurns = payload.requiredString("deltaTurns").toInt(),
            deltaHeadlessTurns = payload.requiredString("deltaHeadlessTurns").toInt(),
        )

    private fun parseZoneObjectiveSummary(payload: JsonObject): ZoneObjectiveSummary =
        ZoneObjectiveSummary(
            zoneId = payload.requiredString("zoneId"),
            questId = payload.requiredString("questId"),
            objectiveId = payload.requiredString("objectiveId"),
            state = ObjectiveState.valueOf(payload.requiredString("state")),
            completionFlagGranted = payload.requiredString("completionFlagGranted").toBooleanStrict(),
        )

    private fun parseZoneTraversalDiagnostic(payload: JsonObject): ZoneTraversalDiagnostic =
        ZoneTraversalDiagnostic(
            zoneId = payload.requiredString("zoneId"),
            visitCount = payload.requiredString("visitCount").toInt(),
            playerTurns = payload.requiredString("playerTurns").toInt(),
            enemyTurns = payload.requiredString("enemyTurns").toInt(),
            enemyTurnsPerPlayerTurn = payload.requiredString("enemyTurnsPerPlayerTurn").toDouble(),
            visibleHostileTurnCount = payload.requiredString("visibleHostileTurnCount").toInt(),
            liveHostileWindow = payload.requiredString("liveHostileWindow").toInt(),
            maxVisibleHostiles = payload.requiredString("maxVisibleHostiles").toInt(),
            objectiveAcquireTurn = payload["objectiveAcquireTurn"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            objectiveAcquireHeadlessTurnEquivalent = payload["objectiveAcquireHeadlessTurnEquivalent"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            objectiveStateAtExit = payload["objectiveStateAtExit"]?.jsonPrimitive?.contentOrNull?.let(ObjectiveState::valueOf),
        )

    private fun parseCaptainEncounterTrace(payload: JsonObject): CaptainEncounterTraceEntry =
        CaptainEncounterTraceEntry(
            turnIndex = payload.requiredString("turnIndex").toInt(),
            headlessTurnEquivalent = payload.requiredString("headlessTurnEquivalent").toInt(),
            floor = payload.requiredString("floor").toInt(),
            playerHp = payload.requiredString("playerHp").toInt(),
            playerMaxHp = payload.requiredString("playerMaxHp").toInt(),
            playerResourceCurrent = payload.requiredString("playerResourceCurrent").toInt(),
            playerResourceMax = payload.requiredString("playerResourceMax").toInt(),
            playerResourceTypeId = payload.requiredString("playerResourceTypeId"),
            captainHp = payload["captainHp"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            captainMaxHp = payload["captainMaxHp"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            captainDistance = payload["captainDistance"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            command = payload["command"]?.jsonPrimitive?.contentOrNull,
            recentMessages = payload.requiredArray("recentMessages").map { message -> message.jsonPrimitive.content },
            recentEvents = payload.requiredArray("recentEvents").map { event -> event.jsonPrimitive.content },
        )

    private fun parseRunOutcome(value: String): RunOutcome =
        when {
            value == "InProgress" -> RunOutcome.InProgress
            value.startsWith("Victory(") -> {
                val floor = value.substringAfter("floor=").substringBefore(',').toInt()
                val reason = value.substringAfter("reason=").substringBefore(')')
                RunOutcome.Victory(floor = floor, reason = reason)
            }

            value.startsWith("Defeat(") -> {
                val floor = value.substringAfter("floor=").substringBefore(',').toInt()
                val reason = value.substringAfter("reason=").substringBefore(')')
                RunOutcome.Defeat(floor = floor, reason = reason)
            }

            else -> error("Unsupported RunOutcome payload '$value'.")
        }

    private fun JsonObject.requiredString(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.requiredArray(key: String) = getValue(key).jsonArray

    private fun JsonObject.requiredObject(key: String) = getValue(key).jsonObject

    private fun JsonObject.optionalObject(key: String): JsonObject = this[key]?.jsonObject ?: JsonObject(emptyMap())

    private fun JsonObject.optionalInt(key: String): Int = this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0

    private fun JsonObject.optionalBoolean(key: String): Boolean = this[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
}

private const val LONGRUN_KERNEL_CACHE_VERSION: String = "phase4-v4-pr01-profession-tree-run-choice-v4"
