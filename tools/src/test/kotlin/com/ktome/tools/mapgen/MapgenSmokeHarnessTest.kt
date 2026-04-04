package com.ktome.tools.mapgen

import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class MapgenSmokeHarnessTest {
    @Test
    @Tag("mapgenSmoke")
    fun `mapgen smoke writes fixed reports and keeps baseline green`() {
        val run = MapgenSmokeRunner.run()

        assertEquals(0, run.failureCount, "mapgenSmoke recorded failures; inspect ${run.summaryPath}")
        assertEquals(0, run.emptyMapCount, "mapgenSmoke recorded empty maps; inspect ${run.summaryPath}")
        assertEquals(0, run.unreachableCount, "mapgenSmoke recorded unreachable critical paths; inspect ${run.summaryPath}")
        assertTrue(run.totalCases >= 500, "mapgenSmoke should cover at least 500 deterministic requests.")
        assertTrue(run.distinctSeedCount >= 500, "mapgenSmoke should cover at least 500 distinct deterministic seeds.")
        assertTrue(Files.exists(run.summaryPath), "Expected summary report at ${run.summaryPath}")
        assertTrue(Files.exists(run.seedsPath), "Expected per-seed report at ${run.seedsPath}")
        val payload = Json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val summary = payload.getValue("summary").jsonObject
        assertEquals(run.distinctSeedCount, summary.getValue("distinctSeedCount").jsonPrimitive.content.toInt())
        assertEquals("zh-CN", payload.getValue("header").jsonObject.getValue("locale").jsonPrimitive.content)
        assertTrue(summary.containsKey("vaultRewardBudgetBuckets"), "Summary should expose vault reward budget buckets.")
        assertTrue(summary.containsKey("vaultThreatBudgetBuckets"), "Summary should expose vault threat budget buckets.")
        assertTrue(summary.containsKey("biomeFamilyUsage"), "Summary should expose biome family usage.")
        assertTrue(summary.containsKey("biomeMixCounts"), "Summary should expose biome family mix counts.")
        assertTrue(summary.containsKey("maxLoopEdgeRatio"), "Summary should expose loop edge ratio statistics.")
        assertTrue(summary.containsKey("averageLoopEdgeRatio"), "Summary should expose average loop edge ratio statistics.")
        assertTrue(summary.getValue("casesWithVaults").jsonPrimitive.content.toInt() > 0, "Hybrid planner should place at least one vault across the smoke corpus.")
    }
}
