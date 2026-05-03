package com.ktome.tools.hidden

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.tools.verification.VerificationCacheSupport
import java.nio.file.Files
import java.util.Locale
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal object OrganicHiddenProbeArtifactWriter {
    private const val SUMMARY_FILE: String = "organic-hidden-probe-summary.json"
    private const val EVENTS_FILE: String = "organic-hidden-probe-events.jsonl"
    private const val MARKDOWN_FILE: String = "organic-hidden-probe-summary.md"

    fun writeSummaryArtifacts(request: OrganicHiddenProbeArtifactWriteRequest): OrganicHiddenProbeArtifactPaths {
        val outputDir = request.outputDir
        val header = request.header
        val summary = request.summary
        validate(summary)
        val summaryPath = outputDir.resolve(SUMMARY_FILE)
        val eventsPath = outputDir.resolve(EVENTS_FILE)
        Files.writeString(
            summaryPath,
            request.json.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(
                    header = header,
                    summary = summary,
                    kernelCacheMetadata = request.kernelCacheMetadata,
                    probeBotId = request.probeBotId,
                    probeTurnBudget = request.probeTurnBudget,
                    probeMaxFloor = request.probeMaxFloor,
                ),
            ),
        )
        VerificationCacheSupport.mergeJsonlFiles(targetPath = eventsPath, sourcePaths = request.shardEventPaths)
        val markdownPath = outputDir.resolve(MARKDOWN_FILE)
        Files.writeString(markdownPath, renderMarkdown(summary = summary, header = header))
        return OrganicHiddenProbeArtifactPaths(
            summaryPath = summaryPath,
            eventsPath = eventsPath,
            markdownPath = markdownPath,
        )
    }

    internal fun buildSummaryPayload(
        header: HarnessReportHeader,
        summary: OrganicHiddenProbeSummary,
        kernelCacheMetadata: JsonObject,
        probeBotId: String,
        probeTurnBudget: Int,
        probeMaxFloor: Int,
    ): JsonObject =
        buildJsonObject {
            put("header", header.toJson())
            put("kernelCache", kernelCacheMetadata)
            putJsonObject("summary") {
                put("scriptedVerification", false)
                put("primerActionUsedCount", 0)
                put("totalCases", summary.totalCases)
                put("distinctSeedCount", summary.distinctSeedCount)
                put("runtimeFailureCount", summary.runtimeFailureCount)
                put("searchAttemptCount", summary.searchAttemptCount)
                put("runsWithSearchActionCount", summary.runsWithSearchActionCount)
                put("searchActionUseCount", summary.searchActionUseCount)
                put("searchActionUseRate", summary.searchActionUseRate)
                put("runsWithSearchPromptCount", summary.runsWithSearchPromptCount)
                put("searchPromptVisibleCount", summary.searchPromptVisibleCount)
                put("searchPromptVisibilityRate", summary.searchPromptVisibilityRate)
                put("zoneSearchPromptVisibility", summary.zoneSearchPromptVisibility)
                put("discoveryWithoutPrimerCount", summary.discoveryWithoutPrimerCount)
                put("leadDiscoveryCount", summary.discoveryWithoutPrimerCount)
                put("leadDiscoveryRate", summary.leadDiscoveryRate)
                put("topZoneLeadShare", summary.topZoneLeadShare)
                put("topZoneLeadShareZoneId", summary.topZoneLeadShareZoneId)
                put("topZoneLeadShareDenominator", summary.topZoneLeadShareDenominator)
                put("secretConversionCount", summary.secretZoneEntryCount)
                put("secretConversionRate", summary.secretConversionRate)
                put("secretZoneSearchConversionRate", summary.secretZoneSearchConversionRate)
                put("secretZoneEntryCount", summary.secretZoneEntryCount)
                put("secretZoneEntryRate", summary.secretZoneEntryRate)
                put("averageFirstHiddenDiscoveryTurn", summary.averageFirstHiddenDiscoveryTurn)
                put("averageFirstSecretZoneEntryTurn", summary.averageFirstSecretZoneEntryTurn)
                put("firstHiddenDiscoveryTurnP50", summary.firstHiddenDiscoveryTurnP50)
                put("firstHiddenDiscoveryTurnP90", summary.firstHiddenDiscoveryTurnP90)
                put("firstSecretZoneEntryTurnP50", summary.firstSecretZoneEntryTurnP50)
                put("firstSecretZoneEntryTurnP90", summary.firstSecretZoneEntryTurnP90)
                putJsonArray("professionIds") {
                    summary.professionIds.forEach { professionId -> add(JsonPrimitive(professionId)) }
                }
                putJsonArray("raceIds") {
                    summary.raceIds.forEach { raceId -> add(JsonPrimitive(raceId)) }
                }
                put("comboCount", summary.combinations.size)
                put("seedsPerZoneCombo", summary.seedsPerZoneCombo)
                put("searchPromptRequired", true)
                put("reactiveSearchOnly", true)
                put("perZoneSecretEntryMinRate", summary.perZoneSecretEntryMinRate)
                putJsonArray("failingSecretEntryZoneIds") {
                    summary.failingSecretEntryZoneIds.forEach { zoneId -> add(JsonPrimitive(zoneId)) }
                }
                put("probeBotId", probeBotId)
                put("probeTurnBudget", probeTurnBudget)
                put("probeMaxFloor", probeMaxFloor)
            }
            putJsonObject("zones") {
                summary.zoneBreakdown.forEach { (zoneId, metrics) ->
                    putJsonObject(zoneId) {
                        put("caseCount", metrics.caseCount)
                        put("runsWithSearchActionCount", metrics.runsWithSearchActionCount)
                        put("searchActionUseCount", metrics.searchActionUseCount)
                        put("searchActionUseRate", metrics.searchActionUseRate)
                        put("runsWithSearchPromptCount", metrics.runsWithSearchPromptCount)
                        put("searchPromptVisibleCount", metrics.searchPromptVisibleCount)
                        put("searchPromptVisibilityRate", metrics.searchPromptVisibilityRate)
                        put("searchRevealCount", metrics.searchRevealCount)
                        put("slagCueEligibleRoomCount", metrics.slagCueEligibleRoomCount)
                        put("slagCueCandidateCount", metrics.slagCueCandidateCount)
                        put("slagCueDensityPerEligibleRoom", metrics.slagCueDensityPerEligibleRoom)
                        put("discoveryWithoutPrimerCount", metrics.discoveryWithoutPrimerCount)
                        put("leadDiscoveryRate", metrics.leadDiscoveryRate)
                        put("secretZoneEntryCount", metrics.secretZoneEntryCount)
                        put("secretZoneEntryRate", metrics.secretZoneEntryRate)
                        put("secretConversionRate", metrics.secretConversionRate)
                        put("secretZoneSearchConversionRate", metrics.secretZoneSearchConversionRate)
                        put("averageFirstHiddenDiscoveryTurn", metrics.averageFirstHiddenDiscoveryTurn)
                        put("averageFirstSecretZoneEntryTurn", metrics.averageFirstSecretZoneEntryTurn)
                        put("firstHiddenDiscoveryTurnP50", metrics.firstHiddenDiscoveryTurnP50)
                        put("firstHiddenDiscoveryTurnP90", metrics.firstHiddenDiscoveryTurnP90)
                        put("firstSecretZoneEntryTurnP50", metrics.firstSecretZoneEntryTurnP50)
                        put("firstSecretZoneEntryTurnP90", metrics.firstSecretZoneEntryTurnP90)
                    }
                }
            }
            putJsonArray("combinations") {
                summary.combinations.forEach { combination ->
                    add(
                        buildJsonObject {
                            put("professionId", combination.professionId)
                            put("raceId", combination.raceId)
                            put("caseCount", combination.caseCount)
                            put("runsWithSearchActionCount", combination.runsWithSearchActionCount)
                            put("searchActionUseCount", combination.searchActionUseCount)
                            put("searchActionUseRate", combination.searchActionUseRate)
                            put("runsWithSearchPromptCount", combination.runsWithSearchPromptCount)
                            put("searchPromptVisibleCount", combination.searchPromptVisibleCount)
                            put("searchPromptVisibilityRate", combination.searchPromptVisibilityRate)
                            put("searchRevealCount", combination.searchRevealCount)
                            put("slagCueEligibleRoomCount", combination.slagCueEligibleRoomCount)
                            put("slagCueCandidateCount", combination.slagCueCandidateCount)
                            put("slagCueDensityPerEligibleRoom", combination.slagCueDensityPerEligibleRoom)
                            put("discoveryWithoutPrimerCount", combination.discoveryWithoutPrimerCount)
                            put("leadDiscoveryRate", combination.leadDiscoveryRate)
                            put("secretZoneEntryCount", combination.secretZoneEntryCount)
                            put("secretZoneEntryRate", combination.secretZoneEntryRate)
                            put("secretConversionRate", combination.secretConversionRate)
                            put("secretZoneSearchConversionRate", combination.secretZoneSearchConversionRate)
                            put("averageFirstHiddenDiscoveryTurn", combination.averageFirstHiddenDiscoveryTurn)
                            put("averageFirstSecretZoneEntryTurn", combination.averageFirstSecretZoneEntryTurn)
                            put("firstHiddenDiscoveryTurnP50", combination.firstHiddenDiscoveryTurnP50)
                            put("firstHiddenDiscoveryTurnP90", combination.firstHiddenDiscoveryTurnP90)
                            put("firstSecretZoneEntryTurnP50", combination.firstSecretZoneEntryTurnP50)
                            put("firstSecretZoneEntryTurnP90", combination.firstSecretZoneEntryTurnP90)
                        },
                    )
                }
            }
            putJsonObject("zoneDiscoveryDistribution") {
                summary.zoneDiscoveryDistribution.forEach { (zoneId, rate) -> put(zoneId, rate) }
            }
            putJsonObject("secretZoneDiscoveryDistribution") {
                summary.secretZoneDiscoveryDistribution.forEach { (secretZoneId, rate) -> put(secretZoneId, rate) }
            }
            putJsonObject("perZoneSecretConversionFloor.reportOnly") {
                summary.perZoneSecretConversionFloorReportOnly.forEach { (zoneId, rate) -> put(zoneId, rate) }
            }
            putJsonObject("secretZoneSearchConversionFloor.reportOnly") {
                summary.secretZoneSearchConversionFloorReportOnly.forEach { (zoneId, rate) -> put(zoneId, rate) }
            }
            putJsonObject("perZoneSearchUseFloor.reportOnly") {
                summary.perZoneSearchUseFloorReportOnly.forEach { (zoneId, rate) -> put(zoneId, rate) }
            }
            putJsonObject("slagCueDensityPerEligibleRoom.reportOnly") {
                summary.slagCueDensityPerEligibleRoomReportOnly.forEach { (zoneId, density) -> put(zoneId, density) }
            }
            putJsonArray("notes") {
                add(JsonPrimitive("organicHiddenProbe samples the released 4 profession x 3 race matrix only."))
                add(JsonPrimitive("organicHiddenProbe uses 11 fixed seeds per zone x profession x race combination."))
                add(JsonPrimitive("organicHiddenProbe never uses primer actions or direct reveal APIs."))
                add(JsonPrimitive("organicHiddenProbe uses only RunObservation-visible prompts, interactables, and exploration state for navigation decisions."))
                add(JsonPrimitive("organicHiddenProbe treats a visible search prompt as the highest-priority clue and searches even when nearby hostiles remain visible."))
                add(JsonPrimitive("organicHiddenProbe measures real session/bot discovery and is allowed to fail during the initial owner-metric hardening pass."))
            }
        }

    internal fun renderMarkdown(
        summary: OrganicHiddenProbeSummary,
        header: HarnessReportHeader,
    ): String =
        buildString {
            appendLine("# organicHiddenProbe")
            appendLine()
            appendLine("- buildId: `${header.buildId}`")
            appendLine("- phaseId: `${header.phaseId}`")
            appendLine("- locale: `${header.locale}`")
            appendLine("- totalCases: `${summary.totalCases}`")
            appendLine("- distinctSeedCount: `${summary.distinctSeedCount}`")
            appendLine("- scriptedVerification: `false`")
            appendLine("- primerActionUsedCount: `0`")
            appendLine("- runtimeFailureCount: `${summary.runtimeFailureCount}`")
            appendLine("- searchActionUseRate: `${formatPercent(summary.searchActionUseRate)}`")
            appendLine("- zoneSearchPromptVisibility: `${formatPercent(summary.zoneSearchPromptVisibility)}`")
            appendLine("- leadDiscoveryRate: `${formatPercent(summary.leadDiscoveryRate)}`")
            appendLine("- topZoneLeadShare: `${formatPercent(summary.topZoneLeadShare)}` `${summary.topZoneLeadShareZoneId ?: "n/a"}` denominator `${summary.topZoneLeadShareDenominator}`")
            appendLine("- secretConversionRate: `${formatPercent(summary.secretConversionRate)}`")
            appendLine("- secretZoneSearchConversionRate: `${formatPercent(summary.secretZoneSearchConversionRate)}`")
            appendLine("- secretZoneEntryRate: `${formatPercent(summary.secretZoneEntryRate)}`")
            appendLine("- perZoneSecretEntryMinRate: `${formatPercent(summary.perZoneSecretEntryMinRate)}`")
            appendLine("- failingSecretEntryZoneIds: `${summary.failingSecretEntryZoneIds.joinToString().ifBlank { "none" }}`")
            appendLine("- firstHiddenDiscoveryTurnP50/P90: `${formatNullableTurn(summary.firstHiddenDiscoveryTurnP50)} / ${formatNullableTurn(summary.firstHiddenDiscoveryTurnP90)}`")
            appendLine("- firstSecretZoneEntryTurnP50/P90: `${formatNullableTurn(summary.firstSecretZoneEntryTurnP50)} / ${formatNullableTurn(summary.firstSecretZoneEntryTurnP90)}`")
            appendLine("- releasedProfessionIds: `${summary.professionIds.joinToString()}`")
            appendLine("- releasedRaceIds: `${summary.raceIds.joinToString()}`")
            appendLine("- comboCount: `${summary.combinations.size}`")
            appendLine("- seedsPerZoneCombo: `${summary.seedsPerZoneCombo}`")
            appendLine("- searchPromptRequired: `true`")
            appendLine("- reactiveSearchOnly: `true`")
            appendLine()
            appendLine("## PR04 Report-Only Floors")
            appendLine("| metric | zoneId | value |")
            appendLine("| --- | --- | --- |")
            summary.perZoneSecretConversionFloorReportOnly.forEach { (zoneId, rate) ->
                appendLine("| `perZoneSecretConversionFloor.reportOnly` | `$zoneId` | ${formatPercent(rate)} |")
            }
            summary.secretZoneSearchConversionFloorReportOnly.forEach { (zoneId, rate) ->
                appendLine("| `secretZoneSearchConversionFloor.reportOnly` | `$zoneId` | ${formatPercent(rate)} |")
            }
            summary.perZoneSearchUseFloorReportOnly.forEach { (zoneId, rate) ->
                appendLine("| `perZoneSearchUseFloor.reportOnly` | `$zoneId` | ${formatPercent(rate)} |")
            }
            summary.slagCueDensityPerEligibleRoomReportOnly.forEach { (zoneId, density) ->
                appendLine("| `slagCueDensityPerEligibleRoom.reportOnly` | `$zoneId` | ${String.format(Locale.US, "%.2f", density)} |")
            }
            appendLine()
            appendLine("## Zone Discovery Distribution")
            appendLine("| zoneId | discoveryShare | leadDiscoveryRate | searchPromptVisibility | searchUseRate | secretEntryRate | secretConversionRate | searchConversionRate | firstHidden P50/P90 |")
            appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
            summary.zoneBreakdown.forEach { (zoneId, metrics) ->
                appendLine(
                    "| `$zoneId` | ${formatPercent(summary.zoneDiscoveryDistribution.getValue(zoneId))} | " +
                        "${formatPercent(metrics.leadDiscoveryRate)} | ${formatPercent(metrics.searchPromptVisibilityRate)} | " +
                        "${formatPercent(metrics.searchActionUseRate)} | ${formatPercent(metrics.secretZoneEntryRate)} | " +
                        "${formatPercent(metrics.secretConversionRate)} | ${formatPercent(metrics.secretZoneSearchConversionRate)} | " +
                        "${formatNullableTurn(metrics.firstHiddenDiscoveryTurnP50)} / ${formatNullableTurn(metrics.firstHiddenDiscoveryTurnP90)} |",
                )
            }
            appendLine()
            appendLine("## Secret-Zone Discovery Distribution")
            appendLine("| secretZoneId | entryShare |")
            appendLine("| --- | --- |")
            summary.secretZoneDiscoveryDistribution.forEach { (secretZoneId, share) ->
                appendLine("| `$secretZoneId` | ${formatPercent(share)} |")
            }
            appendLine()
            appendLine("## Combination Breakdown")
            appendLine("| profession | race | cases | leadDiscoveryRate | searchPromptVisibility | searchUseRate | secretEntryRate | secretConversionRate | firstHidden P50/P90 | firstSecret P50/P90 |")
            appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
            summary.combinations.forEach { combination ->
                appendLine(
                    "| `${combination.professionId}` | `${combination.raceId}` | `${combination.caseCount}` | " +
                        "${formatPercent(combination.leadDiscoveryRate)} | ${formatPercent(combination.searchPromptVisibilityRate)} | " +
                        "${formatPercent(combination.searchActionUseRate)} | " +
                        "${formatPercent(combination.secretZoneEntryRate)} | ${formatPercent(combination.secretConversionRate)} | " +
                        "${formatNullableTurn(combination.firstHiddenDiscoveryTurnP50)} / ${formatNullableTurn(combination.firstHiddenDiscoveryTurnP90)} | " +
                        "${formatNullableTurn(combination.firstSecretZoneEntryTurnP50)} / ${formatNullableTurn(combination.firstSecretZoneEntryTurnP90)} |",
                )
            }
            appendLine()
            appendLine("## Notes")
            appendLine("- organicHiddenProbe never uses primer actions or direct reveal APIs.")
            appendLine("- organicHiddenProbe uses only RunObservation-visible prompts, interactables, and exploration state for navigation decisions.")
            appendLine("- organicHiddenProbe only issues `Search` when `searchPromptAvailable=true` and now prioritizes that prompt even if nearby hostiles remain visible.")
            appendLine("- organicHiddenProbe is a standalone owner artifact; this Markdown is intended for direct review without requiring the phase aggregate.")
        }

    private fun validate(summary: OrganicHiddenProbeSummary) {
        require(summary.totalCases > 0) { "organicHiddenProbe artifact writer requires at least one case." }
        require(summary.professionIds.isNotEmpty()) { "organicHiddenProbe artifact writer requires professionIds." }
        require(summary.raceIds.isNotEmpty()) { "organicHiddenProbe artifact writer requires raceIds." }
        require(summary.zoneBreakdown.isNotEmpty()) { "organicHiddenProbe artifact writer requires zone breakdown." }
        require(summary.combinations.isNotEmpty()) { "organicHiddenProbe artifact writer requires combination breakdown." }
    }
}

private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value * 100.0)

private fun formatNullableTurn(value: Int?): String = value?.toString() ?: "n/a"
