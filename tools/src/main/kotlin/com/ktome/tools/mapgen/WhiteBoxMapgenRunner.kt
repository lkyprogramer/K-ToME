package com.ktome.tools.mapgen

import com.ktome.core.harness.whitebox.ArtifactRetentionPolicy
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxAggregateRule
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCaseRule
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import com.ktome.core.map.Point
import com.ktome.core.mapgen.GeneratedFloor
import com.ktome.core.mapgen.TerrainTag
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.tools.whitebox.WhiteBoxDomainWriteRequest
import com.ktome.tools.whitebox.WhiteBoxReportWriter
import com.ktome.tools.whitebox.WhiteBoxRuleEvaluator
import com.ktome.tools.whitebox.toVerificationReportHeader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class WhiteBoxMapgenRun(
    val totalCases: Int,
    val failedAssertions: Int,
    val summaryPath: Path,
    val casesPath: Path,
    val reportPath: Path,
)

object WhiteBoxMapgenRunner {
    const val HARNESS_ID: String = "whiteBoxMapgen"
    private const val DOMAIN_ID: String = "mapgen"
    private const val SEEDS_PER_FLOOR: Int = 5
    private const val CORPUS_ID: String = "P4_PR03_MAPGEN_WHITEBOX"
    private const val DIFFERENCE_THRESHOLD: Int = 3
    private const val LOOP_RATIO_MIN: Double = 0.15
    private const val LOOP_RATIO_MAX: Double = 0.35
    private val roomSymbols: List<Char> = ('0'..'9') + ('a'..'z') + ('A'..'Z')
    private val caseRules: List<WhiteBoxCaseRule<WhiteBoxMapgenCaseData>> =
        listOf(
            WhiteBoxCaseRule { caseData ->
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "mapgen.case.execution_success",
                        passed = caseData.smokeResult.error == null,
                        message = caseData.smokeResult.error ?: "Pipeline execution succeeded.",
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "mapgen.case.primary_path_reachable",
                        passed = caseData.smokeResult.criticalPathReachable,
                        message =
                            if (caseData.smokeResult.criticalPathReachable) {
                                "Critical path is walkable across room centers."
                            } else {
                                "Critical path is not walkable across room centers."
                            },
                        context = buildJsonObject { put("topologyFingerprint", caseData.smokeResult.topologyFingerprint) },
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                val floor = caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "mapgen.case.room_bounds",
                        passed =
                            floor.rooms.all { room ->
                                floor.map.isInBounds(room.x, room.y) &&
                                    floor.map.isInBounds(room.x + room.width - 1, room.y + room.height - 1)
                            },
                        message = "All room instances stay within map bounds.",
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "mapgen.case.loop_ratio",
                        passed =
                            caseData.smokeResult.topologySummary.optionalLoopCount == 0 ||
                                caseData.smokeResult.topologySummary.loopEdgeRatio in LOOP_RATIO_MIN..LOOP_RATIO_MAX,
                        message = "Loop edge ratio stays within the PR-02 contract when optional loops exist.",
                        context =
                            buildJsonObject {
                                put("optionalLoopCount", caseData.smokeResult.topologySummary.optionalLoopCount)
                                put("loopEdgeRatio", caseData.smokeResult.topologySummary.loopEdgeRatio)
                            },
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "mapgen.case.vault_not_on_critical_path",
                        passed = caseData.smokeResult.vaultPlacements.none { placement -> placement.pathClass == "CRITICAL_PATH" },
                        message = "Vault placements never land on CRITICAL_PATH.",
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "mapgen.case.critical_vault_reward_zero",
                        passed =
                            caseData.smokeResult.vaultPlacements.none { placement ->
                                placement.pathClass == "CRITICAL_PATH" && placement.rewardBudget != 0
                            },
                        message = "Critical-path vault reward budget stays at 0.",
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "mapgen.case.biome_family_count",
                        passed = caseData.smokeResult.biomeFamilies.distinct().size <= 2,
                        message = "Generated floor uses at most 2 biome families.",
                        context = buildJsonObject { put("biomeFamilyCount", caseData.smokeResult.biomeFamilies.distinct().size) },
                    ),
                )
            },
        )
    private val zoneAggregateRules: List<WhiteBoxAggregateRule<WhiteBoxMapgenCaseData>> =
        listOf(
            WhiteBoxAggregateRule { zoneCases ->
                val metrics = zoneMetrics(zoneCases)
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "mapgen.aggregate.difference_categories",
                        passed = metrics.intValue("differenceCategoryCount") >= DIFFERENCE_THRESHOLD,
                        message = "Zone exposes at least $DIFFERENCE_THRESHOLD perceptible difference categories.",
                        context = metrics,
                    ),
                )
            },
        )
    private val corpusAggregateRules: List<WhiteBoxAggregateRule<WhiteBoxMapgenCaseData>> =
        listOf(
            WhiteBoxAggregateRule { caseData ->
                val metrics = corpusMetrics(caseData)
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "mapgen.aggregate.vault_presence",
                        passed = metrics.intValue("casesWithVaults") > 0,
                        message = "Corpus includes at least one case with a vault placement.",
                        context = metrics,
                    ),
                    WhiteBoxAssertionResult(
                        ruleId = "mapgen.aggregate.pattern_room_presence",
                        passed = metrics.intValue("casesWithPatternRoom") > 0,
                        message = "Corpus includes at least one case with a pattern room.",
                        context = metrics,
                    ),
                )
            },
        )

    fun run(): WhiteBoxMapgenRun {
        val outputDir = reportDir()
        Files.createDirectories(outputDir)

        val executionContext = MapgenSmokeRunner.loadExecutionContext()
        val upgradedZones = executionContext.schemaCatalog.zones.filter(ZoneSchemaV2::isPhase4Upgraded).sortedBy(ZoneSchemaV2::id)
        val cases = MapgenSmokeRunner.buildCases(upgradedZones, seedsPerFloor = SEEDS_PER_FLOOR)
        val distinctSeedList = cases.map { case -> case.request.seed }.distinct()
        require(distinctSeedList.size == cases.size) {
            "whiteBoxMapgen corpus must keep a one-to-one seed corpus; got ${distinctSeedList.size} distinct seeds for ${cases.size} cases."
        }
        val header =
            phase4HarnessHeader(harnessId = HARNESS_ID, seedList = distinctSeedList)
                .toVerificationReportHeader(corpusId = CORPUS_ID)
        val corpus =
            WhiteBoxCorpusSpec(
                corpusId = CORPUS_ID,
                description = "First 5 deterministic mapgen seeds per floor for the 4 Phase 4 upgraded zones after PR-03.",
                sampleCount = cases.size,
            )

        val caseData =
            cases.map { testCase ->
                val executedCase = MapgenSmokeRunner.executeCase(executionContext, testCase)
                val smokeResult = executedCase.toCaseResult()
                WhiteBoxMapgenCaseData(
                    executedCase = executedCase,
                    smokeResult = smokeResult,
                )
            }
        val caseReports =
            caseData.map { caseDataEntry ->
                val assertions = caseAssertions(caseDataEntry)
                val artifacts =
                    if (
                        caseDataEntry.executedCase.generatedFloor != null &&
                        WhiteBoxReportWriter.shouldWriteArtifacts(
                            retentionPolicy = ArtifactRetentionPolicy.ALL,
                            joinKey = caseDataEntry.joinKey,
                            assertions = assertions,
                        )
                    ) {
                        writeArtifacts(
                            outputDir = outputDir,
                            caseData = caseDataEntry,
                        )
                    } else {
                        emptyList()
                    }
                WhiteBoxCaseReport(
                    joinKey = caseDataEntry.joinKey,
                    facts = caseFacts(caseDataEntry),
                    fingerprints = fingerprints(caseDataEntry),
                    assertions = assertions,
                    artifacts = artifacts,
                )
            }
        val aggregates = buildAggregates(caseData)
        val writeResult =
            WhiteBoxReportWriter.write(
                WhiteBoxDomainWriteRequest(
                    domainId = DOMAIN_ID,
                    outputDir = outputDir,
                    header = header,
                    corpus = corpus,
                    cases = caseReports,
                    aggregates = aggregates,
                    retentionPolicy = ArtifactRetentionPolicy.ALL,
                ),
            )
        return WhiteBoxMapgenRun(
            totalCases = caseReports.size,
            failedAssertions = writeResult.failedAssertions,
            summaryPath = writeResult.summaryPath,
            casesPath = writeResult.casesPath,
            reportPath = writeResult.reportPath,
        )
    }

    private fun caseAssertions(caseData: WhiteBoxMapgenCaseData): List<WhiteBoxAssertionResult> =
        WhiteBoxRuleEvaluator.evaluateCaseRules(caseData, caseRules)

    private fun buildAggregates(caseData: List<WhiteBoxMapgenCaseData>): List<WhiteBoxAggregateReport> {
        val byZone = caseData.groupBy { data -> data.smokeResult.zoneId }
        val zoneAggregates =
            byZone.toSortedMap().map { (zoneId, zoneCases) ->
                WhiteBoxAggregateReport(
                    groupId = zoneId,
                    sampleCount = zoneCases.size,
                    metrics = zoneMetrics(zoneCases),
                    assertions = WhiteBoxRuleEvaluator.evaluateAggregateRules(zoneCases, zoneAggregateRules),
                )
            }
        val corpusAggregate =
            WhiteBoxAggregateReport(
                groupId = "corpus",
                sampleCount = caseData.size,
                metrics = corpusMetrics(caseData),
                assertions = WhiteBoxRuleEvaluator.evaluateAggregateRules(caseData, corpusAggregateRules),
            )
        return zoneAggregates + corpusAggregate
    }

    private fun zoneMetrics(zoneCases: List<WhiteBoxMapgenCaseData>): JsonObject {
        val topologyDistinct = zoneCases.map { data -> data.smokeResult.topologyFingerprint }.distinct().size
        val biomeMixDistinct = zoneCases.map { data -> data.smokeResult.biomeFamilies }.distinct().size
        val vaultLayoutDistinct = zoneCases.map { data -> data.smokeResult.vaultPlacements }.distinct().size
        val terrainDistinct = zoneCases.map { data -> data.smokeResult.terrainTagDistribution }.distinct().size
        val patternCountDistinct = zoneCases.map { data -> data.smokeResult.topologySummary.patternRoomCount }.distinct().size
        val entranceLayoutDistinct =
            zoneCases.map { data ->
                data.executedCase.generatedFloor
                    ?.entrances
                    ?.sortedBy { entrance -> entrance.bindingId.value }
                    ?.map { entrance ->
                        "${entrance.bindingId.value}:${entrance.targetNodeId.value}:${entrance.resolvedReturnBridgeNodeId.value}"
                    } ?: emptyList()
            }.distinct().size
        val differenceCategories =
            listOf(
                topologyDistinct > 1,
                biomeMixDistinct > 1,
                vaultLayoutDistinct > 1,
                terrainDistinct > 1,
                patternCountDistinct > 1,
                entranceLayoutDistinct > 1,
            ).count { differs -> differs }
        return buildJsonObject {
            put("distinctTopologyCount", topologyDistinct)
            put("distinctBiomeMixCount", biomeMixDistinct)
            put("distinctVaultLayoutCount", vaultLayoutDistinct)
            put("distinctTerrainDistributionCount", terrainDistinct)
            put("distinctPatternRoomCount", patternCountDistinct)
            put("distinctEntranceLayoutCount", entranceLayoutDistinct)
            put("differenceCategoryCount", differenceCategories)
        }
    }

    private fun corpusMetrics(caseData: List<WhiteBoxMapgenCaseData>): JsonObject =
        buildJsonObject {
            put("casesWithVaults", caseData.count { data -> data.smokeResult.vaultPlacements.isNotEmpty() })
            put("casesWithPatternRoom", caseData.count { data -> data.smokeResult.topologySummary.patternRoomCount > 0 })
            put("maxLoopEdgeRatio", caseData.maxOfOrNull { data -> data.smokeResult.topologySummary.loopEdgeRatio } ?: 0.0)
            put("maxRoomCount", caseData.maxOfOrNull { data -> data.smokeResult.topologySummary.roomCount } ?: 0)
        }

    private fun caseFacts(caseData: WhiteBoxMapgenCaseData): JsonObject {
        val smokeResult = caseData.smokeResult
        return buildJsonObject {
            put("seed", smokeResult.seed)
            put("zoneId", smokeResult.zoneId)
            put("floorIndex", smokeResult.floorIndex)
            put("pipelineId", smokeResult.pipelineId)
            put("durationMillis", smokeResult.durationMillis)
            put("criticalPathReachable", smokeResult.criticalPathReachable)
            put("topologyFingerprint", smokeResult.topologyFingerprint)
            putJsonObject("topologySummary") {
                put("nodeCount", smokeResult.topologySummary.nodeCount)
                put("edgeCount", smokeResult.topologySummary.edgeCount)
                put("primaryPathLength", smokeResult.topologySummary.primaryPathLength)
                put("optionalLoopCount", smokeResult.topologySummary.optionalLoopCount)
                put("loopEdgeCount", smokeResult.topologySummary.loopEdgeCount)
                put("loopEdgeRatio", smokeResult.topologySummary.loopEdgeRatio)
                put("roomCount", smokeResult.topologySummary.roomCount)
                put("patternRoomCount", smokeResult.topologySummary.patternRoomCount)
                put("vaultPlacementCount", smokeResult.topologySummary.vaultPlacementCount)
            }
            putJsonArray("biomeFamilies") {
                smokeResult.biomeFamilies.forEach { familyId -> add(JsonPrimitive(familyId)) }
            }
            putJsonObject("terrainTagDistribution") {
                smokeResult.terrainTagDistribution.toSortedMap().forEach { (tag, count) -> put(tag, count) }
            }
            putJsonArray("vaultPlacements") {
                smokeResult.vaultPlacements.forEach { placement ->
                    add(
                        buildJsonObject {
                            put("vaultId", placement.vaultId)
                            put("pathClass", placement.pathClass)
                            put("rewardBudget", placement.rewardBudget)
                            put("threatBudget", placement.threatBudget)
                        },
                    )
                }
            }
            putJsonObject("rewardProfile") {
                put("id", smokeResult.rewardProfile.id)
                put("rarityBonus", smokeResult.rewardProfile.rarityBonus)
                put("qualityBonus", smokeResult.rewardProfile.qualityBonus)
                put("baseRewardBudget", smokeResult.rewardProfile.baseRewardBudget)
            }
            smokeResult.error?.let { error -> put("error", error) }
        }
    }

    private fun fingerprints(caseData: WhiteBoxMapgenCaseData): Map<String, String> =
        buildMap {
            val smokeResult = caseData.smokeResult
            put("topology", smokeResult.topologyFingerprint.ifBlank { "missing" })
            put(
                "terrainDistribution",
                smokeResult.terrainTagDistribution.toSortedMap().entries.joinToString(separator = "|") { (tag, count) -> "$tag=$count" },
            )
            put(
                "vaultLayout",
                smokeResult.vaultPlacements.joinToString(separator = "|") { placement ->
                    "${placement.vaultId}:${placement.pathClass}:${placement.rewardBudget}:${placement.threatBudget}"
                }.ifBlank { "none" },
            )
            put(
                "entranceLayout",
                caseData.executedCase.generatedFloor
                    ?.entrances
                    ?.sortedBy { entrance -> entrance.bindingId.value }
                    ?.joinToString(separator = "|") { entrance ->
                        "${entrance.bindingId.value}:${entrance.targetNodeId.value}:${entrance.resolvedReturnBridgeNodeId.value}"
                    }.orEmpty().ifBlank { "none" },
            )
        }

    private fun writeArtifacts(
        outputDir: Path,
        caseData: WhiteBoxMapgenCaseData,
    ): List<com.ktome.core.harness.whitebox.WhiteBoxArtifact> {
        val floor = requireNotNull(caseData.executedCase.generatedFloor)
        return listOf(
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = caseData.joinKey,
                artifactId = "base-map",
                kind = "map",
                fileName = "base-map.txt",
                summary = "Raw ASCII floor map.",
                content = floor.map.asGlyphRows().joinToString(separator = "\n"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = caseData.joinKey,
                artifactId = "room-overlay",
                kind = "overlay",
                fileName = "room-overlay.txt",
                summary = "Room ownership overlay with stable room labels.",
                content = renderRoomOverlay(floor),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = caseData.joinKey,
                artifactId = "semantic-overlay",
                kind = "overlay",
                fileName = "semantic-overlay.txt",
                summary = "Semantic overlay for vaults, patterns, entrances, and terrain tags.",
                content = renderSemanticOverlay(floor),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = caseData.joinKey,
                artifactId = "legend",
                kind = "legend",
                fileName = "legend.md",
                summary = "Legend and structural summary for the generated floor.",
                content = renderLegend(caseData),
                tags = listOf("markdown"),
            ),
        )
    }

    private fun renderRoomOverlay(floor: GeneratedFloor): String {
        val labels = floor.rooms.sortedBy { room -> room.nodeId.value }.mapIndexed { index, room ->
            room.nodeId to roomSymbols.getOrElse(index) { '?' }
        }.toMap()
        val rows = MutableList(floor.map.height) { y -> floor.map.asGlyphRows()[y].toCharArray() }
        for (y in 0 until floor.map.height) {
            for (x in 0 until floor.map.width) {
                if (floor.map.blocksMovement(x, y)) {
                    continue
                }
                val point = Point(x, y)
                val room = floor.roomAt(point)
                rows[y][x] = room?.let { instance -> labels[instance.nodeId] } ?: '.'
            }
        }
        return rows.joinToString(separator = "\n") { row -> row.concatToString() }
    }

    private fun renderSemanticOverlay(floor: GeneratedFloor): String {
        val vaultNodeIds = floor.vaultPlacements.map { placement -> placement.nodeId }.toSet()
        val patternNodeIds = floor.rooms.filter { room -> room.patternId != null }.map { room -> room.nodeId }.toSet()
        val entranceRoomNodeIds =
            floor.entrances.mapNotNull { entrance -> floor.roomByAnchor(entrance.entranceAnchorId)?.nodeId }.toSet()
        val rows = MutableList(floor.map.height) { y -> floor.map.asGlyphRows()[y].toCharArray() }
        for (y in 0 until floor.map.height) {
            for (x in 0 until floor.map.width) {
                if (floor.map.blocksMovement(x, y)) {
                    continue
                }
                val point = Point(x, y)
                rows[y][x] =
                    terrainSymbol(floor, point)
                        ?: floor.roomAt(point)?.let { room ->
                            when {
                                room.nodeId in vaultNodeIds -> 'V'
                                room.nodeId in patternNodeIds -> 'P'
                                room.nodeId in entranceRoomNodeIds -> 'S'
                                else -> '.'
                            }
                        } ?: '.'
            }
        }
        return rows.joinToString(separator = "\n") { row -> row.concatToString() }
    }

    private fun terrainSymbol(
        floor: GeneratedFloor,
        point: Point,
    ): Char? {
        val tags = floor.terrainTags[point].orEmpty()
        return when {
            TerrainTag.ICE in tags -> '*'
            TerrainTag.OIL in tags -> 'o'
            TerrainTag.WATER in tags -> '~'
            else -> null
        }
    }

    private fun renderLegend(caseData: WhiteBoxMapgenCaseData): String {
        val floor = requireNotNull(caseData.executedCase.generatedFloor)
        val roomEntries = floor.rooms.sortedBy { room -> room.nodeId.value }
        val roomLabelMap = roomEntries.mapIndexed { index, room -> room to roomSymbols.getOrElse(index) { '?' } }
        return buildString {
            appendLine("# Legend")
            appendLine("- zoneId: `${caseData.smokeResult.zoneId}`")
            appendLine("- floorIndex: `${caseData.smokeResult.floorIndex}`")
            appendLine("- seed: `${caseData.smokeResult.seed}`")
            appendLine("- topologyFingerprint: `${caseData.smokeResult.topologyFingerprint}`")
            appendLine("- rewardProfile: `${caseData.smokeResult.rewardProfile.id}`")
            appendLine()
            appendLine("## Topology Summary")
            appendLine("- nodeCount: `${caseData.smokeResult.topologySummary.nodeCount}`")
            appendLine("- edgeCount: `${caseData.smokeResult.topologySummary.edgeCount}`")
            appendLine("- primaryPathLength: `${caseData.smokeResult.topologySummary.primaryPathLength}`")
            appendLine("- optionalLoopCount: `${caseData.smokeResult.topologySummary.optionalLoopCount}`")
            appendLine("- loopEdgeRatio: `${caseData.smokeResult.topologySummary.loopEdgeRatio}`")
            appendLine()
            appendLine("## Room Labels")
            roomLabelMap.forEach { (room, label) ->
                appendLine(
                    "- `$label` = `${room.nodeId.value}` (`${room.pathClass.name}`, roomDef=`${room.roomDefId}`, biome=${room.biomeFamilyId ?: "none"}, pattern=${room.patternId ?: "none"})",
                )
            }
            appendLine()
            appendLine("## Vault Placements")
            if (caseData.smokeResult.vaultPlacements.isEmpty()) {
                appendLine("- none")
            } else {
                caseData.smokeResult.vaultPlacements.forEach { placement ->
                    appendLine(
                        "- `${placement.vaultId}` pathClass=`${placement.pathClass}` reward=`${placement.rewardBudget}` threat=`${placement.threatBudget}` terrain=${placement.requiredTerrainTags}",
                    )
                }
            }
            appendLine()
            appendLine("## Entrances")
            if (floor.entrances.isEmpty()) {
                appendLine("- none")
            } else {
                floor.entrances.sortedBy { entrance -> entrance.bindingId.value }.forEach { entrance ->
                    appendLine(
                        "- `${entrance.bindingId.value}` anchor=`${entrance.entranceAnchorId.value}` targetNode=`${entrance.targetNodeId.value}` returnBridge=`${entrance.resolvedReturnBridgeNodeId.value}`",
                    )
                }
            }
            appendLine()
            appendLine("## Terrain Distribution")
            if (caseData.smokeResult.terrainTagDistribution.isEmpty()) {
                appendLine("- none")
            } else {
                caseData.smokeResult.terrainTagDistribution.toSortedMap().forEach { (tag, count) ->
                    appendLine("- `$tag` = `$count`")
                }
            }
            appendLine()
            appendLine("## Semantic Symbols")
            appendLine("- `V` = vault room")
            appendLine("- `P` = pattern room")
            appendLine("- `S` = room with hidden entrance anchor")
            appendLine("- `~` = WATER")
            appendLine("- `o` = OIL")
            appendLine("- `*` = ICE")
        }
    }

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.whitebox.mapgen.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4", "whitebox", DOMAIN_ID)
        } else {
            Path.of(configured)
        }
    }
}

private data class WhiteBoxMapgenCaseData(
    val executedCase: MapgenExecutedCase,
    val smokeResult: MapgenCaseResult,
) {
    val joinKey: WhiteBoxJoinKey
        get() =
            WhiteBoxJoinKey(
                seed = smokeResult.seed,
                zoneId = smokeResult.zoneId,
                floorIndex = smokeResult.floorIndex,
            )
}

private fun JsonObject.intValue(key: String): Int = (getValue(key) as JsonPrimitive).content.toInt()
