package com.ktome.tools.verification

import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ScopeCoverageLintTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("scopeCoverageLint")
    fun `scope coverage lint proves critical phase4 entry points stay covered`() {
        System.setProperty("ktome.phase4.scopeCoverage.reportDir", tempDir.toString())
        val run = ScopeCoverageLintRunner.run()
        val cases =
            Json.parseToJsonElement(run.summaryPath.readText())
                .jsonObject
                .getValue("cases")
                .jsonArray
                .map { element -> element.jsonObject }
        val coreCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "core_phase4_owner_false_negative" }
        val dataLoaderCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "data_loader_false_negative" }
        val foundationSessionCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "foundation_session_false_negative" }
        val mapgenOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "mapgen_owner_scope" }
        val solvabilityOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "solvability_owner_scope" }
        val terrainOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "terrain_owner_scope" }
        val bossOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "boss_owner_scope" }
        val organicHiddenOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "organic_hidden_owner_scope" }
        val longrunOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "longrun_owner_scope" }
        val scriptedHiddenOwnerBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "scripted_hidden_owner_baseline_scope" }
        val organicHiddenOwnerBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "organic_hidden_owner_baseline_scope" }
        val lootOwnerBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "loot_owner_baseline_scope" }
        val terrainOwnerBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "terrain_owner_baseline_scope" }
        val longrunOwnerBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "longrun_owner_baseline_scope" }
        val phase4ReportOnlyCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "phase4_report_only_scope" }

        assertTrue(run.caseCount >= 20)
        assertTrue(coreCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "mapgen" })
        assertTrue(coreCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:whiteBoxMapgen" })
        assertTrue(dataLoaderCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "content-pack" })
        assertTrue(foundationSessionCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "boss" })
        assertTrue(mapgenOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:whiteBoxMapgen" })
        assertTrue(solvabilityOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:whiteBoxSolvability" })
        assertTrue(terrainOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:terrainInteractionBatch" })
        assertTrue(bossOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:bossHarness" })
        assertTrue(organicHiddenOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:organicHiddenProbe" })
        assertTrue(longrunOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":game:longRunLab" })
        assertTrue(scriptedHiddenOwnerBaselineCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(scriptedHiddenOwnerBaselineCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:hiddenContentHarness" })
        assertTrue(organicHiddenOwnerBaselineCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(organicHiddenOwnerBaselineCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:organicHiddenProbe" })
        assertTrue(lootOwnerBaselineCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:whiteBoxLoot" })
        assertTrue(lootOwnerBaselineCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:lootBalanceLab" })
        assertTrue(terrainOwnerBaselineCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(terrainOwnerBaselineCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:terrainInteractionBatch" })
        assertTrue(longrunOwnerBaselineCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(longrunOwnerBaselineCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":game:longRunLab" })
        assertTrue(phase4ReportOnlyCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(phase4ReportOnlyCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "organic-hidden" })
        assertTrue(dataLoaderCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:phase4ReportOnly" })
    }
}
