package com.ktome.tools.contentpack

import com.ktome.game.contentpack.ContentPackFixtureCatalog
import com.ktome.game.contentpack.ContentPackLoadException
import com.ktome.game.contentpack.ContentPackRuntimeResolver
import com.ktome.game.data.DataLoader
import com.ktome.game.i18n.GameLocale
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ContentPackPreflightCaseResult(
    val caseId: String,
    val stage: String,
    val success: Boolean,
    val resolvedOrder: List<String>,
    val overlayOps: List<String>,
    val diagnosticCodes: List<String>,
)

@Serializable
data class ContentPackPreflightSummary(
    val caseCount: Int,
    val failureCount: Int,
    val cases: List<ContentPackPreflightCaseResult>,
)

data class ContentPackPreflightRun(
    val caseCount: Int,
    val failureCount: Int,
    val summaryPath: Path,
)

object ContentPackPreflightRunner {
    private const val SUMMARY_FILE_NAME: String = "content-pack-preflight-summary.json"
    private val json: Json = Json { prettyPrint = true }

    fun run(): ContentPackPreflightRun {
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val cases =
            listOf(
                runSamplePackCase(),
                runPrecedenceFixtureCase(),
                runDuplicateAddCase(),
                runLegacySchemaMismatchCase(),
                runNamespaceCollisionCase(),
                runVersionConflictCase(),
                runSamePriorityDuplicateCase(),
            )
        val summary =
            ContentPackPreflightSummary(
                caseCount = cases.size,
                failureCount = cases.count { result -> !result.success },
                cases = cases,
            )
        val summaryPath = outputDir.resolve(SUMMARY_FILE_NAME)
        Files.writeString(summaryPath, json.encodeToString(summary))
        return ContentPackPreflightRun(
            caseCount = summary.caseCount,
            failureCount = summary.failureCount,
            summaryPath = summaryPath,
        )
    }

    private fun runSamplePackCase(): ContentPackPreflightCaseResult {
        val selection =
            ContentPackFixtureCatalog.selection(
                activePackRoots = listOf(ContentPackFixtureCatalog.samplePackRoot()),
                availablePackRoots = listOf(ContentPackFixtureCatalog.samplePackRoot()),
            )
        val resolved = ContentPackRuntimeResolver.resolve(selection)
        DataLoader(
            locale = GameLocale.EN_US,
            packSelection = selection,
            preResolvedContentPackSelection = resolved,
        ).loadSchemaCatalog()
        return ContentPackPreflightCaseResult(
            caseId = "official_sample_pack",
            stage = "resolver+data-loader",
            success = true,
            resolvedOrder = resolved.activePackIds.map { packId -> packId.value },
            overlayOps = resolved.orderedPacks.flatMap { pack -> pack.manifest.overlays.map { overlay -> overlay.op.name } },
            diagnosticCodes = emptyList(),
        )
    }

    private fun runPrecedenceFixtureCase(): ContentPackPreflightCaseResult {
        val harnessSpec = ContentPackFixtureCatalog.harnessSpec(ContentPackFixtureCatalog.samplePackId)
        val precedenceScenario = harnessSpec.dualPackScenarios.single()
        val selection =
            ContentPackFixtureCatalog.selection(
                activePackRoots = listOf(ContentPackFixtureCatalog.fixturePackRoot(ContentPackFixtureCatalog.samplePrecedenceFixturePackId)),
                availablePackRoots =
                    listOf(
                        ContentPackFixtureCatalog.samplePackRoot(),
                        ContentPackFixtureCatalog.fixturePackRoot(ContentPackFixtureCatalog.samplePrecedenceFixturePackId),
                    ),
            )
        val resolved = ContentPackRuntimeResolver.resolve(selection)
        DataLoader(
            locale = GameLocale.EN_US,
            packSelection = selection,
            preResolvedContentPackSelection = resolved,
        ).loadSchemaCatalog()
        return ContentPackPreflightCaseResult(
            caseId = "precedence_fixture",
            stage = "resolver+data-loader",
            success =
                resolved.activePackIds.map { packId -> packId.value } == precedenceScenario.expectedOrder.map { packId -> packId.value } &&
                    resolved.orderedPacks
                        .flatMap { pack -> pack.manifest.overlays.map { overlay -> overlay.op } }
                        .filter { overlayOp -> overlayOp.name != "ADD" }
                        .map { overlayOp -> overlayOp.name } == precedenceScenario.expectedOps.map { overlayOp -> overlayOp.name },
            resolvedOrder = resolved.activePackIds.map { packId -> packId.value },
            overlayOps = resolved.orderedPacks.flatMap { pack -> pack.manifest.overlays.map { overlay -> overlay.op.name } },
            diagnosticCodes = emptyList(),
        )
    }

    private fun runDuplicateAddCase(): ContentPackPreflightCaseResult =
        runDataLoaderFailureCase(
            caseId = "duplicate_add_without_replace",
            packIds = listOf(ContentPackFixtureCatalog.duplicateWithoutReplacePackId),
            expectedDiagnosticCodes = setOf("content-pack.overlay.add-conflict"),
        )

    private fun runLegacySchemaMismatchCase(): ContentPackPreflightCaseResult =
        runDataLoaderFailureCase(
            caseId = "legacy_v2_loot_profile",
            packIds = listOf(ContentPackFixtureCatalog.legacyV2LootProfilePackId),
            expectedDiagnosticCodes = setOf("content-pack.loot-profile.schema-version-mismatch"),
        )

    private fun runNamespaceCollisionCase(): ContentPackPreflightCaseResult =
        runResolverFailureCase(
            caseId = "namespace_collision",
            packIds =
                listOf(
                    ContentPackFixtureCatalog.namespaceCollisionLeftId,
                    ContentPackFixtureCatalog.namespaceCollisionRightId,
                ),
            expectedDiagnosticCodes = setOf("content-pack.namespace.collision"),
        )

    private fun runVersionConflictCase(): ContentPackPreflightCaseResult =
        runResolverFailureCase(
            caseId = "version_conflict",
            packIds = listOf(ContentPackFixtureCatalog.versionConflictPackId),
            expectedDiagnosticCodes = setOf("content-pack.version-range.conflict"),
        )

    private fun runSamePriorityDuplicateCase(): ContentPackPreflightCaseResult =
        runResolverFailureCase(
            caseId = "same_priority_duplicate_target",
            packIds =
                listOf(
                    ContentPackFixtureCatalog.samePriorityLeftId,
                    ContentPackFixtureCatalog.samePriorityRightId,
                ),
            expectedDiagnosticCodes = setOf("content-pack.overlay.same-priority-duplicate-target"),
        )

    private fun runResolverFailureCase(
        caseId: String,
        packIds: List<com.ktome.core.phase.PackId>,
        expectedDiagnosticCodes: Set<String>,
    ): ContentPackPreflightCaseResult {
        val exception =
            kotlin.runCatching {
                ContentPackRuntimeResolver.resolve(ContentPackFixtureCatalog.availableSelection(packIds))
            }.exceptionOrNull() as? ContentPackLoadException
                ?: error("Expected ContentPackRuntimeResolver.resolve to fail for case '$caseId'.")
        val actualDiagnosticCodes = ContentPackHarnessRunner.classifyFailure(exception).diagnosticCodes.toSortedSet()
        return ContentPackPreflightCaseResult(
            caseId = caseId,
            stage = "resolver",
            success = actualDiagnosticCodes == expectedDiagnosticCodes,
            resolvedOrder = emptyList(),
            overlayOps = emptyList(),
            diagnosticCodes = actualDiagnosticCodes.toList(),
        )
    }

    private fun runDataLoaderFailureCase(
        caseId: String,
        packIds: List<com.ktome.core.phase.PackId>,
        expectedDiagnosticCodes: Set<String>,
    ): ContentPackPreflightCaseResult {
        val exception =
            kotlin.runCatching {
                DataLoader(
                    locale = GameLocale.EN_US,
                    packSelection = ContentPackFixtureCatalog.availableSelection(packIds),
                ).loadSchemaCatalog()
            }.exceptionOrNull() as? ContentPackLoadException
                ?: error("Expected DataLoader.loadSchemaCatalog to fail for case '$caseId'.")
        val actualDiagnosticCodes = ContentPackHarnessRunner.classifyFailure(exception).diagnosticCodes.toSortedSet()
        return ContentPackPreflightCaseResult(
            caseId = caseId,
            stage = "data-loader",
            success = actualDiagnosticCodes == expectedDiagnosticCodes,
            resolvedOrder = emptyList(),
            overlayOps = emptyList(),
            diagnosticCodes = actualDiagnosticCodes.toList(),
        )
    }

    private fun reportDir(): Path =
        Path.of(
            requireNotNull(System.getProperty("ktome.phase4.contentPack.preflight.reportDir")) {
                "ktome.phase4.contentPack.preflight.reportDir system property is required for content-pack preflight output."
            },
        )
}
