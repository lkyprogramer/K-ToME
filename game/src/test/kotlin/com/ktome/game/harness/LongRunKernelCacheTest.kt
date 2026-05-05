package com.ktome.game.harness

import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LongRunKernelCacheTest {
    @TempDir
    lateinit var tempDir: Path

    private val json = Json {
        prettyPrint = true
        explicitNulls = false
    }

    @Test
    fun `fingerprint roots cover core runtime game runtime harness and data`() {
        val repoRoot = Path.of("").toAbsolutePath().normalize()

        val roots = LongRunKernelCache.inputFingerprintRoots(repoRoot)

        assertTrue(roots.contains(repoRoot.resolve("core/src/main/kotlin/com/ktome/core")))
        assertTrue(roots.contains(repoRoot.resolve("game/src/main/kotlin/com/ktome/game")))
        assertTrue(roots.contains(repoRoot.resolve("game/src/test/kotlin/com/ktome/game/harness")))
        assertTrue(roots.contains(repoRoot.resolve("game/src/main/resources/data")))
    }

    @Test
    fun `long run cache reuses unchanged shards when only one scenario is added`() {
        val cacheIsolation = abs(tempDir.fileName.toString().hashCode().toLong())
        val cachedSpecs =
            listOf(
                quickProbeSpec(name = "cache-reuse-route-probe-a-$cacheIsolation", seed = 2099041301L + cacheIsolation),
                quickProbeSpec(name = "cache-reuse-route-probe-b-$cacheIsolation", seed = 2099041302L + cacheIsolation),
            )
        val expandedSpecs =
            cachedSpecs + quickProbeSpec(name = "cache-reuse-route-probe-c-$cacheIsolation", seed = 2099041303L + cacheIsolation)

        val coldRun = LongRunKernelCache.execute(rootDir = tempDir.resolve("cold"), specs = cachedSpecs)
        val partialWarmRun = LongRunKernelCache.execute(rootDir = tempDir.resolve("partial-warm"), specs = expandedSpecs)

        assertEquals(0, coldRun.reusedShardCount)
        assertEquals(2, coldRun.shardCount)
        assertEquals(2, partialWarmRun.reusedShardCount)
        assertEquals(3, partialWarmRun.shardCount)
        assertEquals("MISS", partialWarmRun.cacheStatus)
        assertTrue(partialWarmRun.shardReportPaths.all(java.nio.file.Files::isRegularFile))
    }

    @Test
    fun `long run cache preserves visited secret zones on branch-inclusive shard hit`() {
        val cacheIsolation = abs(tempDir.fileName.toString().hashCode().toLong())
        val branchSpec =
            Phase4V4Pr06RouteDiversityCorpus
                .branchInclusiveSpecs(corpusId = "CACHE_SECRET_ROUND_TRIP_$cacheIsolation")
                .first()

        val coldRun = LongRunKernelCache.execute(rootDir = tempDir.resolve("secret-cold"), specs = listOf(branchSpec))
        val warmRun = LongRunKernelCache.execute(rootDir = tempDir.resolve("secret-warm"), specs = listOf(branchSpec))
        val coldReport = coldRun.reports.single()
        val warmReport = warmRun.reports.single()

        assertTrue(coldReport.visitedSecretZoneIds.isNotEmpty(), "Cold branch run must prove a runtime secret visit.")
        assertEquals(coldReport.visitedSecretZoneIds, warmReport.visitedSecretZoneIds)
        assertEquals(coldReport.routeToken, warmReport.routeToken)
        assertEquals(1, warmRun.reusedShardCount)
    }

    @Test
    fun `long run cache rejects stale shards without route token`() {
        val cacheIsolation = abs(tempDir.fileName.toString().hashCode().toLong())
        val branchSpec =
            Phase4V4Pr06RouteDiversityCorpus
                .branchInclusiveSpecs(corpusId = "CACHE_STALE_ROUTE_TOKEN_$cacheIsolation")
                .first()

        val coldRun = LongRunKernelCache.execute(rootDir = tempDir.resolve("stale-cold"), specs = listOf(branchSpec))
        val shardPath = coldRun.shardReportPaths.single()
        val payload = json.parseToJsonElement(Files.readString(shardPath)).jsonObject
        val stalePayload = JsonObject(payload.filterKeys { key -> key != "routeToken" })
        Files.writeString(shardPath, json.encodeToString(JsonElement.serializer(), stalePayload))

        assertThrows(IllegalStateException::class.java) {
            LongRunKernelCache.execute(rootDir = tempDir.resolve("stale-warm"), specs = listOf(branchSpec))
        }
    }

    private fun quickProbeSpec(
        name: String,
        seed: Long,
    ): ScenarioSpec =
        ScenarioSpec(
            name = name,
            seed = seed,
            zoneId = "greenwood_fringe",
            professionId = "rogue",
            zoneRoute = listOf("greenwood_fringe"),
            routeIndex = 0,
            scenarioType = ScenarioType.ROUTE_PROBE,
            maxTurns = 1,
            goal = ScenarioGoal.ReachFloor(1),
        )
}
