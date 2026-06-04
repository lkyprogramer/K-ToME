package com.ktome.client.golden

import com.ktome.client.assets.DarkUiMapVisualKeys
import com.ktome.client.render.RoomArtPlateTopologyContract
import com.ktome.client.render.RoomArtPlateTopologyDecision
import com.ktome.core.save.SaveManager
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.FoundationGameConfig
import com.ktome.game.GameModule
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@Tag("pr08RealProcgenProbe")
class Pr08RoomArtPlateD9RealProcgenProbeTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `dark uiux pr08 d9 real procgen roi probe writes distribution artifacts`() {
        val records = captureD9RealProcgenDistribution()
        val aggregate = D9Aggregate.from(records)
        val evidenceDir = d9EvidenceDir()

        assertTrue(records.size in 20..50, "D9 must use the documented 20-50 real-procgen sample range.")
        assertEquals(records.size, aggregate.totalSamples)
        assertTrue(records.all { record -> record.visibleCellCount > 0 })
        assertTrue(records.all { record -> record.connectedComponents >= 1 })
        assertTrue(
            records.map { record -> record.tilesetKey }.toSet().containsAll(D9_SUPPORTED_TILESETS),
            "D9 must cover every current PR08 room-art family, records=$records",
        )
        assertTrue(
            records.any { record -> record.decision == RoomArtPlateTopologyDecision.FULL_PLATE_SAFE },
            "D9 should retain at least one safe proof slice instead of only risk rows.",
        )
        assertTrue(
            records.any { record -> record.decision != RoomArtPlateTopologyDecision.FULL_PLATE_SAFE },
            "D9 should expose topology-risk rows before Direction A ROI/freeze decisions.",
        )
        assertTrue(Files.isRegularFile(evidenceDir.resolve("evidence-index.tsv")))
        assertTrue(Files.isRegularFile(evidenceDir.resolve("summary.md")))
    }

    private fun captureD9RealProcgenDistribution(): List<D9Record> {
        val sessionFactory = GameModule.newFoundationSessionFactory()
        val records =
            D9_PROBES.map { probe ->
                val session =
                    sessionFactory.newSession(
                        config =
                            FoundationGameConfig(
                                seed = probe.seed,
                                zoneId = probe.zoneId,
                                floor = probe.floor,
                                maxFloor = maxOf(2, probe.floor),
                                playerProfessionId = probe.professionId,
                                zoneRoute = FOUNDATION_ZONE_ROUTE,
                                routeIndex = FOUNDATION_ZONE_ROUTE.indexOf(probe.zoneId).coerceAtLeast(0),
                            ),
                        saveManager = SaveManager(tempDir.resolve(probe.label)),
                    )
                val snapshot = session.renderSnapshot()
                val family =
                    requireNotNull(DarkUiMapVisualKeys.roomArtPlateFamilyFor(snapshot.metadata.tilesetKey, snapshot.mapCells)) {
                        "D9 probe ${probe.label} expected a supported room-art family for tileset=${snapshot.metadata.tilesetKey}."
                    }
                val topology =
                    requireNotNull(RoomArtPlateTopologyContract.evaluate(family, snapshot.mapCells)) {
                        "D9 probe ${probe.label} expected visible room-art topology."
                    }
                D9Record(
                    label = probe.label,
                    seed = probe.seed,
                    zoneId = probe.zoneId,
                    floor = probe.floor,
                    professionId = probe.professionId,
                    tilesetKey = snapshot.metadata.tilesetKey,
                    boundsWidth = topology.shape.bounds.width,
                    boundsHeight = topology.shape.bounds.height,
                    visibleCellCount = topology.metrics.visibleCellCount,
                    fillPermille = topology.metrics.fillPermille,
                    aspectPermille = topology.metrics.aspectPermille,
                    connectedComponents = topology.connectedComponents,
                    decision = topology.decision,
                )
            }
        writeD9Evidence(records)
        return records
    }

    private fun writeD9Evidence(records: List<D9Record>) {
        val evidenceDir = d9EvidenceDir()
        Files.createDirectories(evidenceDir)
        Files.writeString(evidenceDir.resolve("evidence-index.tsv"), d9EvidenceIndex(records))
        Files.writeString(evidenceDir.resolve("summary.md"), d9Summary(records))
    }

    private fun d9EvidenceIndex(records: List<D9Record>): String =
        buildString {
            appendLine(
                "label\tseed\tzone\tfloor\tprofession\ttileset\tboundsWidth\tboundsHeight\tvisibleCellCount\t" +
                    "fillPermille\taspectPermille\tconnectedComponents\tdecision\tfailureReason",
            )
            records.forEach { record ->
                appendLine(
                    listOf(
                        record.label,
                        record.seed.toString(),
                        record.zoneId,
                        record.floor.toString(),
                        record.professionId,
                        record.tilesetKey,
                        record.boundsWidth.toString(),
                        record.boundsHeight.toString(),
                        record.visibleCellCount.toString(),
                        record.fillPermille.toString(),
                        record.aspectPermille.toString(),
                        record.connectedComponents.toString(),
                        record.decision.name,
                        record.failureReason,
                    ).joinToString(separator = "\t"),
                )
            }
        }

    private fun d9Summary(records: List<D9Record>): String {
        val aggregate = D9Aggregate.from(records)
        return buildString {
            appendLine("# Dark UI/UX PR08 D9 Real-Procgen ROI Probe")
            appendLine()
            appendLine("- sampleCount: ${aggregate.totalSamples}")
            appendLine("- fullPlateSafe: ${aggregate.fullPlateSafeCount}")
            appendLine("- fullPlateSafePermille: ${aggregate.fullPlateSafePermille}")
            appendLine("- topologyRisk: ${aggregate.topologyRiskCount}")
            appendLine("- evidenceSuggestedRoiDecision: ${aggregate.evidenceSuggestedRoiDecision}")
            appendLine()
            appendLine("## Decision Distribution")
            appendLine()
            appendLine("| decision | count |")
            appendLine("| --- | ---: |")
            aggregate.decisionCounts.forEach { (decision, count) ->
                appendLine("| $decision | $count |")
            }
            appendLine()
            appendLine("## Tileset Distribution")
            appendLine()
            appendLine("| tileset | count | safe | risk |")
            appendLine("| --- | ---: | ---: | ---: |")
            aggregate.tilesetCounts.forEach { (tileset, counts) ->
                appendLine("| $tileset | ${counts.total} | ${counts.safe} | ${counts.risk} |")
            }
            appendLine()
            appendLine(
                "The evidence-suggested ROI decision is structural input for the PR08 decision packet; it uses no unstated hit-rate threshold and does not rebaseline golden hashes or close all-map Direction A by itself.",
            )
        }
    }

    private fun d9EvidenceDir(): Path =
        Path.of(System.getProperty("ktome.repo.root", "."))
            .toAbsolutePath()
            .normalize()
            .resolve("client/build/reports/golden/dark-uiux-pr08-d9-real-procgen")

    private data class D9Probe(
        val zoneId: String,
        val floor: Int,
        val professionId: String,
        val sampleIndex: Int,
    ) {
        val seed: Long = 2026060900L + sampleIndex
        val label: String = "dark-uiux-pr08-d9-$sampleIndex-$zoneId-floor$floor-$professionId"
    }

    private data class D9Record(
        val label: String,
        val seed: Long,
        val zoneId: String,
        val floor: Int,
        val professionId: String,
        val tilesetKey: String,
        val boundsWidth: Int,
        val boundsHeight: Int,
        val visibleCellCount: Int,
        val fillPermille: Int,
        val aspectPermille: Int,
        val connectedComponents: Int,
        val decision: RoomArtPlateTopologyDecision,
    ) {
        val failureReason: String =
            when (decision) {
                RoomArtPlateTopologyDecision.FULL_PLATE_SAFE -> "safe"
                RoomArtPlateTopologyDecision.TOPOLOGY_RISK_DISCONNECTED -> "connectedComponents=$connectedComponents"
                RoomArtPlateTopologyDecision.TOPOLOGY_RISK_LOW_FILL -> "fillPermille=$fillPermille"
                RoomArtPlateTopologyDecision.TOPOLOGY_RISK_EXTREME_ASPECT -> "aspectPermille=$aspectPermille"
            }
    }

    private data class D9Aggregate(
        val totalSamples: Int,
        val fullPlateSafeCount: Int,
        val fullPlateSafePermille: Int,
        val topologyRiskCount: Int,
        val decisionCounts: Map<String, Int>,
        val tilesetCounts: Map<String, TilesetCounts>,
    ) {
        val evidenceSuggestedRoiDecision: String =
            when {
                topologyRiskCount == 0 -> "full-rollout"
                fullPlateSafeCount == 0 -> "stop-direction-a-expansion"
                else -> "hybrid-first-convergence"
            }

        companion object {
            fun from(records: List<D9Record>): D9Aggregate {
                val safeCount = records.count { record -> record.decision == RoomArtPlateTopologyDecision.FULL_PLATE_SAFE }
                val decisionCounts =
                    records
                        .groupingBy { record -> record.decision.name }
                        .eachCount()
                        .toSortedMap()
                val tilesetCounts =
                    records
                        .groupBy(D9Record::tilesetKey)
                        .toSortedMap()
                        .mapValues { (_, recordsForTileset) ->
                            val safe = recordsForTileset.count { record -> record.decision == RoomArtPlateTopologyDecision.FULL_PLATE_SAFE }
                            TilesetCounts(
                                total = recordsForTileset.size,
                                safe = safe,
                                risk = recordsForTileset.size - safe,
                            )
                        }
                return D9Aggregate(
                    totalSamples = records.size,
                    fullPlateSafeCount = safeCount,
                    fullPlateSafePermille = safeCount * 1000 / records.size.coerceAtLeast(1),
                    topologyRiskCount = records.size - safeCount,
                    decisionCounts = decisionCounts,
                    tilesetCounts = tilesetCounts,
                )
            }
        }
    }

    private data class TilesetCounts(
        val total: Int,
        val safe: Int,
        val risk: Int,
    )

    companion object {
        private val D9_SUPPORTED_TILESETS =
            setOf(
                DarkUiMapVisualKeys.RUINS_TILESET,
                DarkUiMapVisualKeys.FOREST_EDGE_TILESET,
                DarkUiMapVisualKeys.MINE_TILESET,
                DarkUiMapVisualKeys.SHADOW_DEPTHS_TILESET,
            )

        private val D9_ZONE_FLOORS =
            listOf(
                "shattered_outpost" to listOf(1, 2),
                "greenwood_fringe" to listOf(1, 2),
                "deep_iron_pit" to listOf(1, 2),
                "grey_gate_depths" to listOf(1, 2),
                "underground_river" to listOf(1, 2),
                "abyssal_temple" to listOf(1, 2),
                "abyssal_heart" to listOf(1),
            )

        private val D9_PROFESSIONS = listOf("vanguard", "rogue")

        private val D9_PROBES: List<D9Probe> =
            D9_ZONE_FLOORS
                .flatMap { (zoneId, floors) ->
                    floors.flatMap { floor ->
                        D9_PROFESSIONS.map { professionId ->
                            zoneId to (floor to professionId)
                        }
                    }
                }.mapIndexed { index, (zoneId, floorAndProfession) ->
                    D9Probe(
                        zoneId = zoneId,
                        floor = floorAndProfession.first,
                        professionId = floorAndProfession.second,
                        sampleIndex = index + 1,
                    )
                }
    }
}
