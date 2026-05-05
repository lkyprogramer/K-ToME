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
        val presentationOnlySnapshotCase =
            cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "presentation_only_item_snapshot_scope" }
        val dataLoaderCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "data_loader_false_negative" }
        val keywordRegistryCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "keyword_registry_scope" }
        val foundationSessionCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "foundation_session_false_negative" }
        val maintainabilityGovernanceCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "maintainability_governance_scope" }
        val maintainabilityBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "maintainability_baseline_scope" }
        val mapgenOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "mapgen_owner_scope" }
        val solvabilityOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "solvability_owner_scope" }
        val terrainOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "terrain_owner_scope" }
        val bossOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "boss_owner_scope" }
        val organicHiddenOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "organic_hidden_owner_scope" }
        val longrunOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "longrun_owner_scope" }
        val routeHashLongrunOwnerCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "route_hash_longrun_owner_scope" }
        val pr06LongrunRuntimeOwnerCases =
            listOf(
                "affix_build_tags_longrun_owner_scope",
                "build_identity_adoption_policy_longrun_owner_scope",
                "milestone_reward_selector_longrun_owner_scope",
            ).map { caseId ->
                cases.first { case -> case.getValue("caseId").jsonPrimitive.content == caseId }
            }
        val pr06LongrunDataOwnerCases =
            listOf(
                "loot_data_longrun_owner_scope",
                "items_data_longrun_owner_scope",
                "build_identity_data_longrun_owner_scope",
                "secret_zones_data_longrun_owner_scope",
                "hidden_events_data_longrun_owner_scope",
                "mapgen_zones_data_longrun_owner_scope",
            ).map { caseId ->
                cases.first { case -> case.getValue("caseId").jsonPrimitive.content == caseId }
            }
        val talentSidebarPresentationOnlyCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "talent_sidebar_presentation_only_scope" }
        val scriptedHiddenOwnerBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "scripted_hidden_owner_baseline_scope" }
        val organicHiddenOwnerBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "organic_hidden_owner_baseline_scope" }
        val lootOwnerBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "loot_owner_baseline_scope" }
        val terrainOwnerBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "terrain_owner_baseline_scope" }
        val longrunOwnerBaselineCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "longrun_owner_baseline_scope" }
        val phase4ReportOnlyCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "phase4_report_only_scope" }
        val phase4ReportHelperCase = cases.first { case -> case.getValue("caseId").jsonPrimitive.content == "phase4_report_helper_scope" }

        assertTrue(run.caseCount >= 36)
        assertTrue(coreCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "mapgen" })
        assertTrue(coreCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "maintainability" })
        assertTrue(coreCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:whiteBoxMapgen" })
        assertTrue(coreCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:maintainabilityLint" })
        assertTrue(presentationOnlySnapshotCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "maintainability" })
        assertTrue(presentationOnlySnapshotCase.getValue("impactedDomainIds").jsonArray.none { domain -> domain.jsonPrimitive.content == "loot" })
        assertTrue(presentationOnlySnapshotCase.getValue("impactedDomainIds").jsonArray.none { domain -> domain.jsonPrimitive.content == "boss" })
        assertTrue(presentationOnlySnapshotCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:maintainabilityLint" })
        assertTrue(presentationOnlySnapshotCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:lootBalanceLab" })
        assertTrue(presentationOnlySnapshotCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":game:longRunLab" })
        assertTrue(dataLoaderCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "content-pack" })
        assertTrue(dataLoaderCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:whiteBoxContentPack" })
        assertTrue(keywordRegistryCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "keywordRegistry" })
        assertTrue(keywordRegistryCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:keywordRegistryLint" })
        assertTrue(foundationSessionCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "boss" })
        assertTrue(foundationSessionCase.getValue("impactedDomainIds").jsonArray.none { domain -> domain.jsonPrimitive.content == "loot" })
        assertTrue(foundationSessionCase.getValue("impactedDomainIds").jsonArray.none { domain -> domain.jsonPrimitive.content == "hidden" })
        assertTrue(foundationSessionCase.getValue("requestedPreflightTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:bossHarness" })
        assertTrue(foundationSessionCase.getValue("requestedPreflightTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":game:longRunLab" })
        assertTrue(maintainabilityGovernanceCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "maintainability" })
        assertTrue(maintainabilityGovernanceCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:maintainabilityLint" })
        assertTrue(maintainabilityGovernanceCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(maintainabilityBaselineCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "maintainability" })
        assertTrue(maintainabilityBaselineCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:maintainabilityLint" })
        assertTrue(mapgenOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:whiteBoxMapgen" })
        assertTrue(solvabilityOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:whiteBoxSolvability" })
        assertTrue(terrainOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:terrainInteractionBatch" })
        assertTrue(bossOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:bossHarness" })
        assertTrue(organicHiddenOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:organicHiddenProbe" })
        assertTrue(longrunOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":game:longRunLab" })
        assertTrue(routeHashLongrunOwnerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":game:longRunLab" })
        pr06LongrunRuntimeOwnerCases.forEach { ownerCase ->
            assertTrue(ownerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":game:longRunLab" })
            assertTrue(ownerCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "longrun" })
        }
        pr06LongrunDataOwnerCases.forEach { ownerCase ->
            assertTrue(ownerCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":game:longRunLab" })
            assertTrue(ownerCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "longrun" })
        }
        assertTrue(talentSidebarPresentationOnlyCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":game:longRunLab" })
        assertTrue(talentSidebarPresentationOnlyCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "maintainability" })
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
        assertTrue(longrunOwnerBaselineCase.getValue("requestedPreflightTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(phase4ReportOnlyCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(phase4ReportOnlyCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "organic-hidden" })
        assertTrue(phase4ReportOnlyCase.getValue("requestedPreflightTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(phase4ReportHelperCase.getValue("requestedTaskPaths").jsonArray.any { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(phase4ReportHelperCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "longrun" })
        assertTrue(phase4ReportHelperCase.getValue("impactedDomainIds").jsonArray.any { domain -> domain.jsonPrimitive.content == "loot" })
        assertTrue(phase4ReportHelperCase.getValue("requestedPreflightTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:reportPhase4Only" })
        assertTrue(dataLoaderCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:phase4ReportOnly" })
        assertTrue(phase4ReportOnlyCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:phase4LegacyReport" })
        assertTrue(phase4ReportOnlyCase.getValue("requestedTaskPaths").jsonArray.none { task -> task.jsonPrimitive.content == ":tools:phase4LegacyReportOnly" })
    }
}
