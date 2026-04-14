package com.ktome.tools.loot

import com.ktome.game.data.DataLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LootPreflightPairSummary(
    val pairId: String,
    val overlap: Double,
    val pairType: String? = null,
    val leftIsSubsetOfRight: Boolean,
    val rightIsSubsetOfLeft: Boolean,
    val sharedBaseIds: List<String>,
    val leftOnlyBaseIds: List<String>,
    val rightOnlyBaseIds: List<String>,
    val explicitVsTagMatched: LootPreflightPairSourceSummary,
    val culpritReasons: List<String>,
)

@Serializable
data class LootPreflightPairSourceSummary(
    val leftExplicitOnlyBaseIds: List<String>,
    val leftTagMatchedOnlyBaseIds: List<String>,
    val leftExplicitAndTagMatchedBaseIds: List<String>,
    val rightExplicitOnlyBaseIds: List<String>,
    val rightTagMatchedOnlyBaseIds: List<String>,
    val rightExplicitAndTagMatchedBaseIds: List<String>,
)

@Serializable
data class LootPreflightSummary(
    val profileCount: Int,
    val pairCount: Int,
    val culpritPairCount: Int,
    val culpritPairs: List<LootPreflightPairSummary>,
)

data class LootPreflightRun(
    val profileCount: Int,
    val pairCount: Int,
    val culpritPairCount: Int,
    val summaryPath: Path,
    val detailsPath: Path,
)

object LootPreflightRunner {
    internal const val SUMMARY_FILE_NAME: String = "loot-preflight-summary.json"
    internal const val DETAILS_FILE_NAME: String = "loot-preflight-pairs.json"
    private val json: Json = Json { prettyPrint = true }

    fun run(): LootPreflightRun {
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val loader = DataLoader()
        val schemaCatalog = loader.loadSchemaCatalog()
        val analysis =
            LootProfileStructureAnalyzer.analyze(
                profiles = schemaCatalog.lootProfiles,
                itemBundle = loader.loadItemBundle(),
            )
        val culpritPairs = analysis.culpritPairs.map { pairDiff -> pairDiff.toSummary() }
        val allPairs = analysis.pairDiffs.map { pairDiff -> pairDiff.toSummary() }
        val summary =
            LootPreflightSummary(
                profileCount = analysis.profileCount,
                pairCount = analysis.pairDiffs.size,
                culpritPairCount = culpritPairs.size,
                culpritPairs = culpritPairs,
            )
        val summaryPath = outputDir.resolve(SUMMARY_FILE_NAME)
        val detailsPath = outputDir.resolve(DETAILS_FILE_NAME)
        Files.writeString(summaryPath, json.encodeToString(summary))
        Files.writeString(detailsPath, json.encodeToString(allPairs))
        return LootPreflightRun(
            profileCount = summary.profileCount,
            pairCount = summary.pairCount,
            culpritPairCount = summary.culpritPairCount,
            summaryPath = summaryPath,
            detailsPath = detailsPath,
        )
    }

    private fun reportDir(): Path =
        Path.of(
            requireNotNull(System.getProperty("ktome.phase4.loot.preflight.reportDir")) {
                "ktome.phase4.loot.preflight.reportDir system property is required for loot preflight output."
            },
        )

    internal fun summaryPath(reportDir: Path = reportDir()): Path = reportDir.resolve(SUMMARY_FILE_NAME)

    internal fun detailsPath(reportDir: Path = reportDir()): Path = reportDir.resolve(DETAILS_FILE_NAME)

    internal fun readSummary(reportDir: Path = reportDir()): LootPreflightSummary? {
        val summaryPath = summaryPath(reportDir)
        if (!Files.isRegularFile(summaryPath)) {
            return null
        }
        return json.decodeFromString(Files.readString(summaryPath))
    }

    internal fun readAllPairs(reportDir: Path = reportDir()): List<LootPreflightPairSummary>? {
        val detailsPath = detailsPath(reportDir)
        if (!Files.isRegularFile(detailsPath)) {
            return null
        }
        return json.decodeFromString(Files.readString(detailsPath))
    }

    private fun LootProfilePairDiff.toSummary(): LootPreflightPairSummary =
        LootPreflightPairSummary(
            pairId = pairId,
            overlap = overlap,
            pairType = pairType,
            leftIsSubsetOfRight = leftIsSubsetOfRight,
            rightIsSubsetOfLeft = rightIsSubsetOfLeft,
            sharedBaseIds = sharedBaseIds,
            leftOnlyBaseIds = leftOnlyBaseIds,
            rightOnlyBaseIds = rightOnlyBaseIds,
            explicitVsTagMatched =
                LootPreflightPairSourceSummary(
                    leftExplicitOnlyBaseIds = explicitVsTagMatched.left.explicitOnlyBaseIds,
                    leftTagMatchedOnlyBaseIds = explicitVsTagMatched.left.tagMatchedOnlyBaseIds,
                    leftExplicitAndTagMatchedBaseIds = explicitVsTagMatched.left.explicitAndTagMatchedBaseIds,
                    rightExplicitOnlyBaseIds = explicitVsTagMatched.right.explicitOnlyBaseIds,
                    rightTagMatchedOnlyBaseIds = explicitVsTagMatched.right.tagMatchedOnlyBaseIds,
                    rightExplicitAndTagMatchedBaseIds = explicitVsTagMatched.right.explicitAndTagMatchedBaseIds,
                ),
            culpritReasons = culpritReasons,
        )
}
