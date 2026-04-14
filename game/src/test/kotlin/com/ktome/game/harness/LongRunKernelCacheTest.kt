package com.ktome.game.harness

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LongRunKernelCacheTest {
    @TempDir
    lateinit var tempDir: Path

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
        val cachedSpecs =
            listOf(
                quickProbeSpec(name = "cache-reuse-route-probe-a", seed = 2099041301L),
                quickProbeSpec(name = "cache-reuse-route-probe-b", seed = 2099041302L),
            )
        val expandedSpecs = cachedSpecs + quickProbeSpec(name = "cache-reuse-route-probe-c", seed = 2099041303L)

        val coldRun = LongRunKernelCache.execute(rootDir = tempDir.resolve("cold"), specs = cachedSpecs)
        val partialWarmRun = LongRunKernelCache.execute(rootDir = tempDir.resolve("partial-warm"), specs = expandedSpecs)

        assertEquals(0, coldRun.reusedShardCount)
        assertEquals(2, coldRun.shardCount)
        assertEquals(2, partialWarmRun.reusedShardCount)
        assertEquals(3, partialWarmRun.shardCount)
        assertEquals("MISS", partialWarmRun.cacheStatus)
        assertTrue(partialWarmRun.shardReportPaths.all(java.nio.file.Files::isRegularFile))
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
