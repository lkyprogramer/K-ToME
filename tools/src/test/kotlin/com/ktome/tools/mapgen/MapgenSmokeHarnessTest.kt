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
        assertEquals(run.distinctSeedCount, payload.getValue("summary").jsonObject.getValue("distinctSeedCount").jsonPrimitive.content.toInt())
        assertEquals("zh-CN", payload.getValue("header").jsonObject.getValue("locale").jsonPrimitive.content)
    }
}
