package com.ktome.game.harness

import com.ktome.core.combat.CombatResolutionTrace
import com.ktome.core.combat.CombatResult
import com.ktome.core.combat.DamageType
import com.ktome.core.combat.ElementInteractionRegistry
import com.ktome.core.combat.ElementInteractionResolution
import com.ktome.core.ecs.BlocksMovement
import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.DisplayColor
import com.ktome.core.ecs.Energy
import com.ktome.core.ecs.EliteMutationLoadout
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.ExperienceReward
import com.ktome.core.ecs.Faction
import com.ktome.core.ecs.FactionTag
import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.PreferredTerrainAffinity
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.ecs.remove
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import com.ktome.core.map.Point
import com.ktome.core.mapgen.TerrainOverride
import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.phase.Phase4ContractVersions
import com.ktome.core.save.SaveManager
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusLifecycle
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.EffectTracker
import com.ktome.game.FoundationGameConfig
import com.ktome.game.FoundationGameSession
import com.ktome.game.GameModule
import com.ktome.game.data.DataLoader
import com.ktome.game.terrainPreferenceImplemented
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class TerrainInteractionBatchTest {
    private companion object {
        private const val TERRAIN_METRIC_DEFINITION_VERSION: String = "phase4-terrain-v2"
        private const val TERRAIN_TAGGED_EXPOSURE_FORMULA: String = "taggedCombatCount / combatCount"
        private const val TERRAIN_INTERACTION_ENCOUNTER_FORMULA: String =
            "triggeredInteractionCombatCount / taggedCombatCount"
    }

    @TempDir
    lateinit var tempDir: Path

    private val preferredTerrainTagsByMutationId: Map<String, Set<TerrainTag>> =
        DataLoader().loadSchemaCatalog().eliteMutations.associate { mutation ->
            mutation.id to mutation.preferredTerrainTags.toSet()
        }

    @Test
    @Tag("terrainInteractionBatch")
    fun `terrain interaction batch writes structured white-box reports`() {
        val isolatedCases = isolatedCases()
        val provenanceCases = provenanceCases()
        val exposureProbe = runTerrainExposureProbe()
        val allCases = isolatedCases + provenanceCases
        val caseReports = allCases.map(TerrainBatchCaseRecord::report)
        val aggregates =
            listOf(
                aggregateFor(groupId = "isolated-corpus", cases = isolatedCases),
                aggregateFor(groupId = "provenance-corpus", cases = provenanceCases),
                aggregateFor(groupId = "corpus", cases = allCases, exposureProbe = exposureProbe),
            )
        val outputDir = whiteBoxSummaryReportDir("ktome.phase4.whitebox.terrain.reportDir", "terrain")
        val writeResult =
            WhiteBoxHarnessWriter.write(
                WhiteBoxHarnessWriteRequest(
                    domainId = "terrain",
                    outputDir = outputDir,
                    header =
                        whiteBoxPhase4Header(
                            harnessId = "terrainInteractionBatch",
                            corpusId = "P4_PR06_TERRAIN_WHITEBOX",
                            contractVersions =
                                listOf(
                                    "elementInteractionRegistry" to Phase4ContractVersions.ELEMENT_INTERACTION_REGISTRY_VERSION.toString(),
                                    "terrainOverride" to Phase4ContractVersions.TERRAIN_OVERRIDE_VERSION.toString(),
                                    "rewardLedger" to Phase4ContractVersions.REWARD_LEDGER_VERSION.toString(),
                                    "visualManifest" to "1",
                                    "audioManifest" to "1",
                                ),
                            seeds = (provenanceCases.mapNotNull { case -> case.joinKey.seed } + exposureProbe.seeds).distinct(),
                        ),
                    corpus =
                        WhiteBoxCorpusSpec(
                            corpusId = "P4_PR06_TERRAIN_WHITEBOX",
                            description = "Five isolated rule probes, three mapgen provenance probes, plus a 500-seed terrain exposure baseline for PR-06/OPT PR-01 terrain interaction.",
                            sampleCount = allCases.size,
                        ),
                    cases = caseReports,
                    aggregates = aggregates,
                ),
            )

        assertEquals(0, writeResult.failedAssertions, "terrainInteractionBatch left failed white-box assertions in ${writeResult.summaryPath}")
        assertTrue(writeResult.artifactCount >= allCases.size * 5, "terrainInteractionBatch should retain all requested artifacts.")
    }

    private fun isolatedCases(): List<TerrainBatchCaseRecord> =
        listOf(
            runIsolatedCase(
                ruleId = ElementInteractionRegistry.TERRAIN_LIGHTNING_WATER_CHAIN,
                damageType = DamageType.LIGHTNING,
                terrainTags = setOf(TerrainTag.WATER),
                requiresAdjacentSameTerrain = true,
                expectedAfterTags = setOf(TerrainTag.WATER),
                expectedSecondaryEffect = TerrainSecondaryEffect.CHAIN_DAMAGE,
            ),
            runIsolatedCase(
                ruleId = ElementInteractionRegistry.TERRAIN_FIRE_OIL_IGNITE,
                damageType = DamageType.FIRE,
                terrainTags = setOf(TerrainTag.OIL),
                expectedAfterTags = setOf(TerrainTag.OIL),
                expectedSecondaryEffect = TerrainSecondaryEffect.IGNITE_OVERRIDE,
            ),
            runIsolatedCase(
                ruleId = ElementInteractionRegistry.TERRAIN_COLD_WATER_FREEZE,
                damageType = DamageType.COLD,
                terrainTags = setOf(TerrainTag.WATER),
                expectedAfterTags = setOf(TerrainTag.ICE),
                expectedSecondaryEffect = TerrainSecondaryEffect.TERRAIN_TRANSFORM,
            ),
            runIsolatedCase(
                ruleId = ElementInteractionRegistry.TERRAIN_FIRE_ICE_MELT,
                damageType = DamageType.FIRE,
                terrainTags = setOf(TerrainTag.ICE),
                expectedAfterTags = setOf(TerrainTag.WATER),
                expectedSecondaryEffect = TerrainSecondaryEffect.MELT_AND_REMOVE_FREEZE,
                applyFreezeBeforeHit = true,
            ),
            runIsolatedCase(
                ruleId = ElementInteractionRegistry.TERRAIN_PHYSICAL_ICE_SLIP,
                damageType = DamageType.PHYSICAL,
                terrainTags = setOf(TerrainTag.ICE),
                expectedAfterTags = setOf(TerrainTag.ICE),
                expectedSecondaryEffect = TerrainSecondaryEffect.SLIP_STATUS,
            ),
        )

    private fun provenanceCases(): List<TerrainBatchCaseRecord> =
        listOf(
            runProvenanceCase(
                zoneId = "underground_river",
                desiredTag = TerrainTag.WATER,
                ruleId = ElementInteractionRegistry.TERRAIN_LIGHTNING_WATER_CHAIN,
                damageType = DamageType.LIGHTNING,
                requiresAdjacentSameTerrain = true,
                expectedAfterTags = setOf(TerrainTag.WATER),
                expectedSecondaryEffect = TerrainSecondaryEffect.CHAIN_DAMAGE,
                sourceMutationId = "elite.tidebound",
            ),
            runProvenanceCase(
                zoneId = "deep_iron_pit",
                desiredTag = TerrainTag.OIL,
                ruleId = ElementInteractionRegistry.TERRAIN_FIRE_OIL_IGNITE,
                damageType = DamageType.FIRE,
                expectedAfterTags = setOf(TerrainTag.OIL),
                expectedSecondaryEffect = TerrainSecondaryEffect.IGNITE_OVERRIDE,
            ),
            runProvenanceCase(
                zoneId = "crystal_cavern",
                desiredTag = TerrainTag.ICE,
                ruleId = ElementInteractionRegistry.TERRAIN_FIRE_ICE_MELT,
                damageType = DamageType.FIRE,
                expectedAfterTags = setOf(TerrainTag.WATER),
                expectedSecondaryEffect = TerrainSecondaryEffect.MELT_AND_REMOVE_FREEZE,
                applyFreezeBeforeHit = true,
            ),
        )

    private fun runIsolatedCase(
        ruleId: String,
        damageType: DamageType,
        terrainTags: Set<TerrainTag>,
        expectedAfterTags: Set<TerrainTag>,
        expectedSecondaryEffect: TerrainSecondaryEffect,
        requiresAdjacentSameTerrain: Boolean = false,
        applyFreezeBeforeHit: Boolean = false,
    ): TerrainBatchCaseRecord {
        val session = newSession(seed = 2026040601L + kotlin.math.abs(ruleId.hashCode().toLong()) % 1000L, zoneId = "grey_gate_depths")
        val cluster = findOpenCluster(session, sameTerrainCount = if (requiresAdjacentSameTerrain) 2 else 1)
        cluster.terrainPoints.forEach { point ->
            session.automationSetTerrainOverride(
                point = point,
                terrainOverride =
                    TerrainOverride(
                        terrainTags = terrainTags,
                        sourceRuleId = "isolated:$ruleId",
                        remainingTurns = 3,
                        conductsLightning = TerrainTag.WATER in terrainTags,
                    ),
            )
        }
        return executeTerrainCase(
            session = session,
            joinKey = WhiteBoxJoinKey(scenarioId = ruleId),
            ruleId = ruleId,
            damageType = damageType,
            targetPoint = cluster.terrainPoints.first(),
            adjacentPoint = cluster.terrainPoints.getOrNull(1),
            expectedAfterTags = expectedAfterTags,
            expectedSecondaryEffect = expectedSecondaryEffect,
            applyFreezeBeforeHit = applyFreezeBeforeHit,
            sourceMutationId = null,
            segment = "isolated-corpus",
            provenance = false,
        )
    }

    private fun runProvenanceCase(
        zoneId: String,
        desiredTag: TerrainTag,
        ruleId: String,
        damageType: DamageType,
        expectedAfterTags: Set<TerrainTag>,
        expectedSecondaryEffect: TerrainSecondaryEffect,
        requiresAdjacentSameTerrain: Boolean = false,
        applyFreezeBeforeHit: Boolean = false,
        sourceMutationId: String? = null,
    ): TerrainBatchCaseRecord {
        val located = locateMapgenCase(zoneId = zoneId, terrainTag = desiredTag, requiresAdjacentSameTerrain = requiresAdjacentSameTerrain)
        return executeTerrainCase(
            session = located.session,
            joinKey =
                WhiteBoxJoinKey(
                    seed = located.seed,
                    zoneId = zoneId,
                    floorIndex = located.session.currentFloor(),
                    scenarioId = ruleId,
                ),
            ruleId = ruleId,
            damageType = damageType,
            targetPoint = located.targetPoint,
            adjacentPoint = located.adjacentPoint,
            expectedAfterTags = expectedAfterTags,
            expectedSecondaryEffect = expectedSecondaryEffect,
            applyFreezeBeforeHit = applyFreezeBeforeHit,
            sourceMutationId = sourceMutationId,
            segment = "provenance-corpus",
            provenance = true,
        )
    }

    private fun executeTerrainCase(
        session: FoundationGameSession,
        joinKey: WhiteBoxJoinKey,
        ruleId: String,
        damageType: DamageType,
        targetPoint: Point,
        adjacentPoint: Point?,
        expectedAfterTags: Set<TerrainTag>,
        expectedSecondaryEffect: TerrainSecondaryEffect,
        applyFreezeBeforeHit: Boolean,
        sourceMutationId: String?,
        segment: String,
        provenance: Boolean,
    ): TerrainBatchCaseRecord {
        val prepared = prepareCombatants(session, targetPoint, adjacentPoint, sourceMutationId)
        if (applyFreezeBeforeHit) {
            applyFreeze(prepared.world, prepared.targetId)
        }
        val beforeTerrainTags = session.automationTerrainTagsAt(targetPoint)
        val beforeStateHash = session.automationTerrainStateHash()
        val targetHealthBefore = requireNotNull(prepared.world.get<Health>(prepared.targetId)).current
        val chainHealthBefore = prepared.chainId?.let { chainId -> requireNotNull(prepared.world.get<Health>(chainId)).current }
        val result =
            session.automationResolveTriggeredDamage(
                source = session.playerId,
                target = prepared.targetId,
                damageType = damageType,
                rawDamage = 18,
                traceId = "terrain-batch:${joinKey.scenarioId}:${joinKey.seed ?: 0L}",
                abilityId = "terrain_batch:${ruleId}",
            )
        val interaction = result.terrainInteraction
        val trace = requireNotNull(result.trace) { "terrainInteractionBatch requires a combat trace for ${joinKey.scenarioId}." }
        val targetHealthAfter = requireNotNull(prepared.world.get<Health>(prepared.targetId)).current
        val chainHealthAfter = prepared.chainId?.let { chainId -> requireNotNull(prepared.world.get<Health>(chainId)).current }
        val afterTerrainTags = session.automationTerrainTagsAt(targetPoint)
        val afterStateHash = session.automationTerrainStateHash()
        val targetOverride = session.automationTerrainOverrideAt(targetPoint)
        val sourceTags =
            buildSet {
                add("damage:${damageType.name.lowercase()}")
                sourceMutationId?.let { mutationId -> add("mutation:$mutationId") }
            }
        val preferredTerrainTags = sourceMutationId?.let(preferredTerrainTagsByMutationId::get).orEmpty()
        val adjacentTerrainTags =
            Point.ALL_DIRECTIONS
                .asSequence()
                .map { delta -> targetPoint + delta }
                .filter { point -> session.map.isInBounds(point.x, point.y) }
                .flatMap { point -> session.automationTerrainTagsAt(point).asSequence() }
                .toCollection(linkedSetOf())
        val terrainPreferenceImplemented =
            preferredTerrainTags.isEmpty() ||
                beforeTerrainTags.any(preferredTerrainTags::contains) ||
                adjacentTerrainTags.any(preferredTerrainTags::contains)
        val step9 = trace.steps.firstOrNull { step -> step.stepIndex == 9 }
        val step9ResolvedRuleId = step9?.outputs?.get("terrainInteractionRuleId")
        val childTraceIds = interaction?.childTraceIds.orEmpty()
        val mapSlice = renderMapSlice(session, center = targetPoint, targetId = prepared.targetId, chainId = prepared.chainId)
        val assertions =
            buildCaseAssertions(
                ruleId = ruleId,
                result = result,
                interaction = interaction,
                step9ResolvedRuleId = step9ResolvedRuleId,
                expectedAfterTags = expectedAfterTags,
                actualAfterTags = afterTerrainTags,
                targetOverride = targetOverride,
                targetHealthBefore = targetHealthBefore,
                targetHealthAfter = targetHealthAfter,
                chainHealthBefore = chainHealthBefore,
                chainHealthAfter = chainHealthAfter,
                expectedSecondaryEffect = expectedSecondaryEffect,
                sourceTags = sourceTags,
                preferredTerrainTags = preferredTerrainTags,
                terrainPreferenceImplemented = terrainPreferenceImplemented,
            )
        val artifacts = writeTerrainArtifacts(outputDir = whiteBoxSummaryReportDir("ktome.phase4.whitebox.terrain.reportDir", "terrain"), joinKey = joinKey, session = session, result = result, interaction = interaction, beforeTerrainTags = beforeTerrainTags, afterTerrainTags = afterTerrainTags, targetPoint = targetPoint, mapSlice = mapSlice, chainId = prepared.chainId)
        val report =
            WhiteBoxCaseReport(
                joinKey = joinKey,
                facts =
                    buildJsonObject {
                        put("ruleId", ruleId)
                        put("triggerDamageType", damageType.name)
                        putJsonArray("sourceTags") { sourceTags.sorted().forEach { value -> add(JsonPrimitive(value)) } }
                        putJsonArray("targetTerrainTagsBefore") { beforeTerrainTags.map { it.name }.sorted().forEach { value -> add(JsonPrimitive(value)) } }
                        putJsonArray("targetTerrainTagsAfter") { afterTerrainTags.map { it.name }.sorted().forEach { value -> add(JsonPrimitive(value)) } }
                        putJsonArray("adjacentTargetIds") { prepared.chainId?.value?.let { add(JsonPrimitive(it)) } }
                        putJsonArray("appliedStatusIds") { interaction?.appliedStatusIds.orEmpty().sorted().forEach { value -> add(JsonPrimitive(value)) } }
                        putJsonArray("removedStatusIds") { interaction?.removedStatusIds.orEmpty().sorted().forEach { value -> add(JsonPrimitive(value)) } }
                        putJsonArray("preferredTerrainTags") { preferredTerrainTags.map(TerrainTag::name).sorted().forEach { value -> add(JsonPrimitive(value)) } }
                        putJsonArray("adjacentTerrainTags") { adjacentTerrainTags.map(TerrainTag::name).sorted().forEach { value -> add(JsonPrimitive(value)) } }
                        put("terrainPreferenceImplemented", terrainPreferenceImplemented)
                        putJsonArray("createdTerrainOverrides") {
                            targetOverride?.let { override ->
                                add(
                                    buildJsonObject {
                                        put("x", targetPoint.x)
                                        put("y", targetPoint.y)
                                        putJsonArray("terrainTags") { override.terrainTags.map { it.name }.sorted().forEach { value -> add(JsonPrimitive(value)) } }
                                        put("sourceRuleId", override.sourceRuleId)
                                        put("remainingTurns", override.remainingTurns)
                                    },
                                )
                            }
                        }
                        put("step9TraceId", trace.traceId)
                        putJsonArray("childTraceIds") { childTraceIds.forEach { value -> add(JsonPrimitive(value)) } }
                        put("interactionDepth", interaction?.interactionDepth ?: 0)
                        put("provenance", provenance)
                    },
                fingerprints =
                    linkedMapOf(
                        "traceHash" to sha256(Json.encodeToString(trace)),
                        "terrainStateHashBefore" to beforeStateHash,
                        "terrainStateHashAfter" to afterStateHash,
                        "registryVersion" to Phase4ContractVersions.ELEMENT_INTERACTION_REGISTRY_VERSION.toString(),
                    ),
                assertions = assertions,
                artifacts = artifacts,
            )
        return TerrainBatchCaseRecord(
            segment = segment,
            joinKey = joinKey,
            resolvedRuleId = step9ResolvedRuleId,
            provenanceTerrainTags = if (provenance) beforeTerrainTags.mapTo(linkedSetOf()) { it.name } else emptySet(),
            preferredTerrainTags = preferredTerrainTags.mapTo(linkedSetOf(), TerrainTag::name),
            terrainPreferenceImplemented = terrainPreferenceImplemented,
            sourceTags = sourceTags,
            report = report,
        )
    }

    private fun buildCaseAssertions(
        ruleId: String,
        result: CombatResult,
        interaction: ElementInteractionResolution?,
        step9ResolvedRuleId: String?,
        expectedAfterTags: Set<TerrainTag>,
        actualAfterTags: Set<TerrainTag>,
        targetOverride: TerrainOverride?,
        targetHealthBefore: Int,
        targetHealthAfter: Int,
        chainHealthBefore: Int?,
        chainHealthAfter: Int?,
        expectedSecondaryEffect: TerrainSecondaryEffect,
        sourceTags: Set<String>,
        preferredTerrainTags: Set<TerrainTag>,
        terrainPreferenceImplemented: Boolean,
    ): List<WhiteBoxAssertionResult> {
        val actualAfterTagNames = actualAfterTags.map { it.name }.sorted()
        return listOf(
            WhiteBoxAssertionResult(
                ruleId = "terrain.case.execution_success",
                passed = result.hit && result.finalDamage > 0,
                message = "Triggered damage resolved successfully and landed damage on the target.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "terrain.case.rule_resolved",
                passed = interaction?.ruleId == ruleId,
                message = "Terrain interaction resolved the expected frozen rule id.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "terrain.case.step9_trace_hit",
                passed = step9ResolvedRuleId == ruleId,
                message = "Combat trace step 9 records the same terrain interaction rule id.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "terrain.case.terrain_transform",
                passed =
                    when (expectedSecondaryEffect) {
                        TerrainSecondaryEffect.CHAIN_DAMAGE,
                        TerrainSecondaryEffect.SLIP_STATUS,
                        -> actualAfterTags == expectedAfterTags
                        TerrainSecondaryEffect.IGNITE_OVERRIDE,
                        TerrainSecondaryEffect.TERRAIN_TRANSFORM,
                        TerrainSecondaryEffect.MELT_AND_REMOVE_FREEZE,
                        -> targetOverride != null && actualAfterTags == expectedAfterTags
                    },
                message = "Terrain tags/override reflect the PR-06 expected post-interaction state.",
                context =
                    buildJsonObject {
                        putJsonArray("expectedAfterTags") { expectedAfterTags.map { it.name }.sorted().forEach { value -> add(JsonPrimitive(value)) } }
                        putJsonArray("actualAfterTags") { actualAfterTagNames.forEach { value -> add(JsonPrimitive(value)) } }
                    },
            ),
            WhiteBoxAssertionResult(
                ruleId = "terrain.case.secondary_effect",
                passed =
                    when (expectedSecondaryEffect) {
                        TerrainSecondaryEffect.CHAIN_DAMAGE ->
                            chainHealthBefore != null && chainHealthAfter != null && chainHealthAfter < chainHealthBefore && targetHealthAfter < targetHealthBefore
                        TerrainSecondaryEffect.IGNITE_OVERRIDE ->
                            targetOverride?.tickDamageType == DamageType.FIRE && (targetOverride.tickDamage > 0)
                        TerrainSecondaryEffect.TERRAIN_TRANSFORM -> actualAfterTags == expectedAfterTags
                        TerrainSecondaryEffect.MELT_AND_REMOVE_FREEZE ->
                            "FREEZE" in interaction?.removedStatusIds.orEmpty() && targetOverride?.conductsLightning == true
                        TerrainSecondaryEffect.SLIP_STATUS ->
                            "STUN" in interaction?.appliedStatusIds.orEmpty()
                    },
                message = "The expected secondary terrain effect is observable in runtime state.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "terrain.case.depth_guardrail",
                passed = (interaction?.interactionDepth ?: 0) <= ElementInteractionRegistry.MAX_INTERACTION_DEPTH,
                message = "Interaction depth stays within the PR-06 cap of 2.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "terrain.case.formal_source_tags_retained",
                passed =
                    sourceTags.none { tag -> tag.startsWith("mutation:") || tag.startsWith("bossVariant:") } ||
                        sourceTags.any { tag -> tag.startsWith("mutation:elite.") || tag.startsWith("bossVariant:boss.variant.") },
                message = "Mutation/boss provenance, when present, retains the formal source-tag path into terrain interaction context.",
                context =
                    buildJsonObject {
                        putJsonArray("sourceTags") { sourceTags.sorted().forEach { value -> add(JsonPrimitive(value)) } }
                    },
            ),
            WhiteBoxAssertionResult(
                ruleId = "terrain.case.preferred_terrain_applied",
                passed = preferredTerrainTags.isEmpty() || terrainPreferenceImplemented,
                message = "Formal terrain-affinity mutations land on preferred terrain or an adjacent tactical tile.",
                context =
                    buildJsonObject {
                        putJsonArray("preferredTerrainTags") { preferredTerrainTags.map(TerrainTag::name).sorted().forEach { value -> add(JsonPrimitive(value)) } }
                    },
            ),
        )
    }

    private fun aggregateFor(
        groupId: String,
        cases: List<TerrainBatchCaseRecord>,
        exposureProbe: TerrainExposureProbeSummary? = null,
    ): WhiteBoxAggregateReport {
        val allRuleIds = cases.mapNotNull { record -> record.report.joinKey.scenarioId }.toSet()
        val failedCount = cases.sumOf { case -> failedAssertionCount(case.report.assertions) }
        val unresolvedRuleCount = cases.count { case -> case.resolvedRuleId == null }
        val provenanceTags = cases.flatMapTo(linkedSetOf()) { case -> case.provenanceTerrainTags }
        val formalSourceTagCount =
            cases.count { case ->
                case.sourceTags.any { tag -> tag.startsWith("mutation:") || tag.startsWith("bossVariant:") }
            }
        val preferredTerrainCaseCount = cases.count { case -> case.preferredTerrainTags.isNotEmpty() }
        val preferredTerrainImplementedCount =
            cases.count { case ->
                case.preferredTerrainTags.isNotEmpty() && case.terrainPreferenceImplemented
            }
        return WhiteBoxAggregateReport(
            groupId = groupId,
            sampleCount = cases.size,
            metrics =
                buildJsonObject {
                    put("caseCount", cases.size)
                    put("failedAssertionCount", failedCount)
                    put("unresolvedRuleCount", unresolvedRuleCount)
                    put("formalSourceTagCount", formalSourceTagCount)
                    put("preferredTerrainCaseCount", preferredTerrainCaseCount)
                    put("preferredTerrainImplementedCount", preferredTerrainImplementedCount)
                    put(
                        "preferredTerrainCaseImplementationRate",
                        if (preferredTerrainCaseCount == 0) {
                            0.0
                        } else {
                            preferredTerrainImplementedCount.toDouble() / preferredTerrainCaseCount.toDouble()
                        },
                    )
                    put("terrainMetricDefinitionVersion", TERRAIN_METRIC_DEFINITION_VERSION)
                    put("terrainTaggedCombatExposureFormula", TERRAIN_TAGGED_EXPOSURE_FORMULA)
                    put("terrainInteractionEncounterFormula", TERRAIN_INTERACTION_ENCOUNTER_FORMULA)
                    putJsonArray("coveredRuleIds") { allRuleIds.sorted().forEach { value -> add(JsonPrimitive(value)) } }
                    putJsonArray("provenanceTerrainTags") { provenanceTags.sorted().forEach { value -> add(JsonPrimitive(value)) } }
                    putJsonArray("preferredTerrainTagsSeen") {
                        cases.flatMapTo(linkedSetOf()) { case -> case.preferredTerrainTags }.sorted().forEach { value -> add(JsonPrimitive(value)) }
                    }
                    exposureProbe?.let { probe ->
                        put("terrainTaggedCombatExposureRate", probe.terrainTaggedCombatExposureRate)
                        put("terrainInteractionEncounterRate", probe.terrainInteractionEncounterRate)
                        put("combatCount", probe.combatCount)
                        put("taggedCombatCount", probe.taggedCombatCount)
                        put("triggeredInteractionCombatCount", probe.triggeredInteractionCombatCount)
                        put("preferredTerrainCombatCount", probe.preferredTerrainCombatCount)
                        put("preferredTerrainImplementedCombatCount", probe.preferredTerrainImplementedCombatCount)
                        put("preferredTerrainCombatImplementationRate", probe.preferredTerrainCombatImplementationRate)
                        put("decisionPathByCurrentMetrics", terrainDecisionPath(probe.terrainTaggedCombatExposureRate, probe.terrainInteractionEncounterRate))
                        putJsonObject("terrainCoverageByZone") {
                            probe.coverageByZone.forEach { (zoneId, coverage) ->
                                putJsonObject(zoneId) {
                                    put("combatCount", coverage.combatCount)
                                    put("taggedCombatCount", coverage.taggedCombatCount)
                                    put("triggeredInteractionCombatCount", coverage.triggeredInteractionCombatCount)
                                    put("preferredTerrainCombatCount", coverage.preferredTerrainCombatCount)
                                    put("preferredTerrainImplementedCombatCount", coverage.preferredTerrainImplementedCombatCount)
                                    put("preferredTerrainCombatImplementationRate", coverage.preferredTerrainCombatImplementationRate)
                                    put("terrainTaggedCombatExposureRate", coverage.terrainTaggedCombatExposureRate)
                                    put("terrainInteractionEncounterRate", coverage.terrainInteractionEncounterRate)
                                    putJsonArray("observedTerrainTags") {
                                        coverage.observedTerrainTags.sorted().forEach { terrainTag -> add(JsonPrimitive(terrainTag)) }
                                    }
                                    putJsonArray("observedPreferredTerrainTags") {
                                        coverage.observedPreferredTerrainTags.sorted().forEach { terrainTag -> add(JsonPrimitive(terrainTag)) }
                                    }
                                    putJsonArray("triggeredRuleIds") {
                                        coverage.triggeredRuleIds.sorted().forEach { ruleId -> add(JsonPrimitive(ruleId)) }
                                    }
                                }
                            }
                        }
                    }
                },
            assertions =
                listOfNotNull(
                    if (groupId == "isolated-corpus" || groupId == "corpus") {
                        WhiteBoxAssertionResult(
                            ruleId = "terrain.aggregate.all_rule_ids_covered",
                            passed = allTerrainRuleIds().all(allRuleIds::contains),
                            message = "All five frozen terrain interaction rule ids are covered.",
                        )
                    } else {
                        null
                    },
                    WhiteBoxAssertionResult(
                        ruleId = "terrain.aggregate.unresolved_rule_zero",
                        passed = unresolvedRuleCount == 0,
                        message = "No terrain interaction case leaves the rule unresolved.",
                    ),
                    WhiteBoxAssertionResult(
                        ruleId = "terrain.aggregate.all_enter_step9",
                        passed = cases.all { case -> case.resolvedRuleId != null },
                        message = "Every recorded interaction lands on CombatPipeline step 9.",
                    ),
                    if (groupId == "provenance-corpus" || groupId == "corpus") {
                        WhiteBoxAssertionResult(
                            ruleId = "terrain.aggregate.provenance_water_oil_ice",
                            passed = setOf("WATER", "OIL", "ICE").all(provenanceTags::contains),
                            message = "Mapgen provenance corpus covers WATER, OIL, and ICE terrain sources.",
                        )
                    } else {
                        null
                    },
                    if (groupId == "provenance-corpus" || groupId == "corpus") {
                        WhiteBoxAssertionResult(
                            ruleId = "terrain.aggregate.formal_source_tags_present",
                            passed = formalSourceTagCount > 0,
                            message = "At least one terrain interaction provenance case retains formal mutation/boss source tags.",
                        )
                    } else {
                        null
                    },
                    if (groupId == "provenance-corpus" || groupId == "corpus") {
                        WhiteBoxAssertionResult(
                            ruleId = "terrain.aggregate.preferred_terrain_implemented",
                            passed = preferredTerrainCaseCount == 0 || preferredTerrainCaseCount == preferredTerrainImplementedCount,
                            message = "Every provenance case with explicit terrain affinity lands on preferred terrain or an adjacent tactical tile.",
                        )
                    } else {
                        null
                    },
                    if (groupId == "corpus" && exposureProbe != null) {
                        WhiteBoxAssertionResult(
                            ruleId = "terrain.aggregate.exposure_rate",
                            passed = exposureProbe.terrainTaggedCombatExposureRate >= 0.10,
                            message = "Terrain-tagged combat exposure stays at or above the OPT PR-01 baseline threshold.",
                            context =
                                buildJsonObject {
                                    put("terrainTaggedCombatExposureRate", exposureProbe.terrainTaggedCombatExposureRate)
                                    put("combatCount", exposureProbe.combatCount)
                                    put("taggedCombatCount", exposureProbe.taggedCombatCount)
                                },
                        )
                    } else {
                        null
                    },
                    if (groupId == "corpus" && exposureProbe != null) {
                        WhiteBoxAssertionResult(
                            ruleId = "terrain.aggregate.zone_probe_nonempty",
                            passed = exposureProbe.coverageByZone.values.all { coverage -> coverage.combatCount > 0 },
                            message = "Each target zone contributes at least one direct-combat observation to the terrain exposure baseline.",
                            context =
                                buildJsonObject {
                                    putJsonObject("combatCountByZone") {
                                        exposureProbe.coverageByZone.forEach { (zoneId, coverage) ->
                                            put(zoneId, coverage.combatCount)
                                        }
                                    }
                                },
                        )
                    } else {
                        null
                    },
                ),
        )
    }

    private fun runTerrainExposureProbe(): TerrainExposureProbeSummary {
        val observations = mutableListOf<FoundationGameSession.TerrainCombatObservation>()
        val seeds = mutableListOf<Long>()
        TERRAIN_EXPOSURE_ZONE_IDS.forEachIndexed { zoneOrdinal, zoneId ->
            repeat(TERRAIN_EXPOSURE_SEEDS_PER_ZONE) { seedOrdinal ->
                val seed = TERRAIN_EXPOSURE_SEED_BASE + zoneOrdinal * TERRAIN_EXPOSURE_ZONE_SEED_BLOCK + seedOrdinal
                seeds += seed
                val session = newProbeSession(seed = seed, zoneId = zoneId)
                val bot = SmokeBot()
                val stallDetector = StallDetector(maxRepeats = 12)
                var turnIndex = 0
                var observation = RunObservationCapture.capture(session, turnIndex)
                while (turnIndex < TERRAIN_EXPOSURE_TURN_BUDGET && !observation.runOutcome.isTerminal && session.currentFloor() <= TERRAIN_EXPOSURE_MAX_FLOOR) {
                    val command =
                        routeProgressCommand(session, observation)
                            .takeIf { shouldPrioritizeTerrainProbeRouteProgress(observation) }
                            ?: bot.decide(observation)
                    val accepted = session.perform(command)
                    if (!accepted) {
                        break
                    }
                    if (command.consumesTurn()) {
                        turnIndex += 1
                    }
                    observation = RunObservationCapture.capture(session, turnIndex)
                    if (stallDetector.observe(observation) != null) {
                        break
                    }
                }
                observations += session.automationTerrainCombatObservations()
            }
        }
        val coverageByZone =
            TERRAIN_EXPOSURE_ZONE_IDS.associateWith { zoneId ->
                val zoneObservations = observations.filter { observation -> observation.zoneId == zoneId }
                TerrainZoneExposureSummary(
                    combatCount = zoneObservations.size,
                    taggedCombatCount = zoneObservations.count(FoundationGameSession.TerrainCombatObservation::isTerrainTagged),
                    triggeredInteractionCombatCount = zoneObservations.count(FoundationGameSession.TerrainCombatObservation::terrainInteractionTriggered),
                    preferredTerrainCombatCount = zoneObservations.count(FoundationGameSession.TerrainCombatObservation::hasPreferredTerrain),
                    preferredTerrainImplementedCombatCount =
                        zoneObservations.count { observation ->
                            observation.hasPreferredTerrain() && observation.preferredTerrainImplemented()
                        },
                    observedTerrainTags =
                        zoneObservations
                            .flatMapTo(linkedSetOf()) { observation ->
                                observation.attackerTerrainTags.map(TerrainTag::name) + observation.targetTerrainTags.map(TerrainTag::name)
                            },
                    observedPreferredTerrainTags =
                        zoneObservations
                            .flatMapTo(linkedSetOf()) { observation ->
                                observation.attackerPreferredTerrainTags.map(TerrainTag::name) +
                                    observation.targetPreferredTerrainTags.map(TerrainTag::name)
                            },
                    triggeredRuleIds =
                        zoneObservations
                            .mapNotNullTo(linkedSetOf(), FoundationGameSession.TerrainCombatObservation::terrainInteractionRuleId),
                )
            }
        return TerrainExposureProbeSummary(
            seeds = seeds,
            combatCount = observations.size,
            taggedCombatCount = observations.count(FoundationGameSession.TerrainCombatObservation::isTerrainTagged),
            triggeredInteractionCombatCount = observations.count(FoundationGameSession.TerrainCombatObservation::terrainInteractionTriggered),
            preferredTerrainCombatCount = observations.count(FoundationGameSession.TerrainCombatObservation::hasPreferredTerrain),
            preferredTerrainImplementedCombatCount =
                observations.count { observation ->
                    observation.hasPreferredTerrain() && observation.preferredTerrainImplemented()
                },
            coverageByZone = coverageByZone,
        )
    }

    private fun locateMapgenCase(
        zoneId: String,
        terrainTag: TerrainTag,
        requiresAdjacentSameTerrain: Boolean,
    ): LocatedMapgenCase {
        val seedBase = when (zoneId) {
            "underground_river" -> 2026040701L
            "deep_iron_pit" -> 2026040801L
            "crystal_cavern" -> 2026040901L
            else -> 2026041001L
        }
        repeat(24) { offset ->
            val seed = seedBase + offset
            val session = newSession(seed = seed, zoneId = zoneId)
            val terrainTags = session.automationTerrainTags()
            val ordered = terrainTags.keys.sortedWith(compareBy<Point> { it.y }.thenBy { it.x })
            val targetPoint =
                ordered.firstOrNull { point ->
                    terrainTag in terrainTags[point].orEmpty() &&
                        (!requiresAdjacentSameTerrain || Point.ALL_DIRECTIONS.any { delta ->
                            terrainTag in terrainTags[point + delta].orEmpty()
                        })
                } ?: return@repeat
            val adjacentPoint =
                if (!requiresAdjacentSameTerrain) {
                    null
                } else {
                    Point.ALL_DIRECTIONS
                        .map { delta -> targetPoint + delta }
                        .firstOrNull { point -> terrainTag in terrainTags[point].orEmpty() }
                }
            return LocatedMapgenCase(seed = seed, session = session, targetPoint = targetPoint, adjacentPoint = adjacentPoint)
        }
        error("Failed to locate mapgen terrain provenance case for $zoneId and tag $terrainTag.")
    }

    private fun prepareCombatants(
        session: FoundationGameSession,
        targetPoint: Point,
        adjacentPoint: Point?,
        sourceMutationId: String?,
    ): PreparedCombatants {
        val world = session.automationWorld()
        val liveMonsters =
            world.entitiesWith(Position::class, Health::class, MonsterTemplateId::class)
                .filter { entityId -> (world.get<Health>(entityId)?.current ?: 0) > 0 }
                .sortedBy(EntityId::value)
        val requiredCount = if (adjacentPoint != null) 2 else 1
        val monsters =
            if (liveMonsters.size >= requiredCount) {
                liveMonsters
            } else {
                liveMonsters + buildList {
                    repeat(requiredCount - liveMonsters.size) { index ->
                        add(createHarnessMonster(world, position = targetPoint, name = "Terrain Dummy ${index + 1}"))
                    }
                }
            }
        val targetId = monsters.first()
        val chainId = adjacentPoint?.let { monsters.drop(1).first() }
        requireNotNull(world.get<Position>(targetId)).moveTo(targetPoint)
        chainId?.let { entityId ->
            requireNotNull(world.get<Position>(entityId)).moveTo(requireNotNull(adjacentPoint))
        }
        sourceMutationId?.let { mutationId ->
            world.remove<EliteMutationLoadout>(session.playerId)
            world.add(session.playerId, EliteMutationLoadout(mutableListOf(mutationId)))
            val preferredTerrainTags = preferredTerrainTagsByMutationId[mutationId].orEmpty()
            if (preferredTerrainTags.isEmpty()) {
                world.remove<PreferredTerrainAffinity>(session.playerId)
            } else {
                world.add(session.playerId, PreferredTerrainAffinity(preferredTerrainTags))
            }
        } ?: run {
            world.remove<EliteMutationLoadout>(session.playerId)
            world.remove<PreferredTerrainAffinity>(session.playerId)
        }
        val playerPoint = openAdjacentWalkablePoint(session, targetPoint)
        session.automationMovePlayerTo(playerPoint)
        return PreparedCombatants(world = world, targetId = targetId, chainId = chainId)
    }

    private fun createHarnessMonster(
        world: World,
        position: Point,
        name: String,
    ): EntityId {
        val entityId = world.createEntity()
        val stats = Stats(str = 10, dex = 10, con = 10, wil = 10)
        val profile =
            CombatProfile(
                baseAttack = 5,
                baseDefense = 1,
                baseAccuracy = 12,
                baseEvasion = 4,
                baseSpeed = 100,
                baseHp = 48,
            )
        val derivedStats = StatsCalculator.calculate(stats, profile)
        world.add(entityId, Position(position.x, position.y))
        world.add(entityId, Glyph('d'))
        world.add(entityId, DisplayColor("#FFFFFF"))
        world.add(entityId, Name(name))
        world.add(entityId, MonsterTemplateId("harness.terrain_dummy"))
        world.add(entityId, FactionTag(Faction.MONSTER))
        world.add(entityId, BlocksMovement())
        world.add(entityId, stats)
        world.add(entityId, profile)
        world.add(entityId, derivedStats)
        world.add(entityId, Health(current = derivedStats.maxHp, max = derivedStats.maxHp))
        world.add(entityId, Energy())
        world.add(entityId, ExperienceReward(0))
        world.add(entityId, EffectTracker(ownerId = entityId))
        return entityId
    }

    private fun findOpenCluster(
        session: FoundationGameSession,
        sameTerrainCount: Int,
    ): TerrainPointCluster {
        val floorPoints = session.map.floorPoints().sortedWith(compareBy<Point> { it.y }.thenBy { it.x })
        val targetPoint =
            floorPoints.firstOrNull { point ->
                Point.ALL_DIRECTIONS.count { delta -> session.map.isInBounds(point.x + delta.x, point.y + delta.y) && !session.map[point + delta].blocksMovement } >= sameTerrainCount
            } ?: error("Unable to find an open cluster for isolated terrain interaction harness.")
        val adjacentPoints =
            Point.ALL_DIRECTIONS
                .map { delta -> targetPoint + delta }
                .filter { point -> session.map.isInBounds(point.x, point.y) && !session.map[point].blocksMovement }
                .take((sameTerrainCount - 1).coerceAtLeast(0))
        return TerrainPointCluster(terrainPoints = listOf(targetPoint) + adjacentPoints)
    }

    private fun openAdjacentWalkablePoint(
        session: FoundationGameSession,
        center: Point,
    ): Point =
        Point.ALL_DIRECTIONS
            .map { delta -> center + delta }
            .firstOrNull { point ->
                session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement
            } ?: error("Unable to find an open adjacent point near $center.")

    private fun applyFreeze(
        world: World,
        targetId: EntityId,
    ) {
        val tracker = world.get<EffectTracker>(targetId) ?: EffectTracker(ownerId = targetId).also { world.add(targetId, it) }
        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.fromSchemaId("FREEZE"),
                effectId = "terrain-batch:freeze:${targetId.value}",
                duration = 2,
                appliedTurn = 0,
            ),
        )
    }

    private fun writeTerrainArtifacts(
        outputDir: Path,
        joinKey: WhiteBoxJoinKey,
        session: FoundationGameSession,
        result: CombatResult,
        interaction: ElementInteractionResolution?,
        beforeTerrainTags: Set<TerrainTag>,
        afterTerrainTags: Set<TerrainTag>,
        targetPoint: Point,
        mapSlice: String,
        chainId: EntityId?,
    ) = listOf(
        WhiteBoxHarnessWriter.writeTextArtifact(
            outputDir = outputDir,
            joinKey = joinKey,
            artifactId = "interaction-trace-matrix",
            kind = "interaction_trace_matrix",
            fileName = "interaction-trace-matrix.md",
            summary = "CombatPipeline trace with the frozen step-9 terrain outputs.",
            content = renderTraceMatrix(requireNotNull(result.trace)),
            tags = listOf("trace", "step9"),
        ),
        WhiteBoxHarnessWriter.writeTextArtifact(
            outputDir = outputDir,
            joinKey = joinKey,
            artifactId = "terrain-state-table",
            kind = "terrain_state_table",
            fileName = "terrain-state-table.md",
            summary = "Terrain tags before/after the interaction plus runtime override state.",
            content =
                buildString {
                    appendLine("| point | before | after | override |")
                    appendLine("| --- | --- | --- | --- |")
                    appendLine(
                        "| ${targetPoint.x},${targetPoint.y} | ${beforeTerrainTags.map { it.name }.sorted().joinToString()} | " +
                            "${afterTerrainTags.map { it.name }.sorted().joinToString()} | ${session.automationTerrainOverrideAt(targetPoint)} |",
                    )
                },
            tags = listOf("terrain", "state"),
        ),
        WhiteBoxHarnessWriter.writeTextArtifact(
            outputDir = outputDir,
            joinKey = joinKey,
            artifactId = "conduction-target-table",
            kind = "conduction_target_table",
            fileName = "conduction-target-table.md",
            summary = "Bonus/chain child trace routing for the interaction.",
            content =
                buildString {
                    appendLine("| childTraceId | target | rawDamage |")
                    appendLine("| --- | --- | --- |")
                    interaction?.bonusTargetTrace?.let { trace ->
                        appendLine("| ${trace.traceId} | ${trace.targetId.value} | ${trace.rawDamage} |")
                    }
                    interaction?.chainTargets.orEmpty().forEach { trace ->
                        appendLine("| ${trace.traceId} | ${trace.targetId.value} | ${trace.rawDamage} |")
                    }
                    if (interaction == null) {
                        appendLine("| none | none | 0 |")
                    }
                },
            tags = listOf("chain", "damage"),
        ),
        WhiteBoxHarnessWriter.writeTextArtifact(
            outputDir = outputDir,
            joinKey = joinKey,
            artifactId = "semantic-summary",
            kind = "semantic_summary",
            fileName = "semantic-summary.md",
            summary = "Slip/melt/freeze semantics captured by the runtime result.",
            content =
                buildString {
                    appendLine("- removedStatusIds: ${interaction?.removedStatusIds.orEmpty().joinToString().ifBlank { "none" }}")
                    appendLine("- appliedStatusIds: ${interaction?.appliedStatusIds.orEmpty().joinToString().ifBlank { "none" }}")
                    appendLine("- childTraceIds: ${interaction?.childTraceIds.orEmpty().joinToString().ifBlank { "none" }}")
                    appendLine("- chainTargetPresent: ${chainId != null}")
                },
            tags = listOf("semantics"),
        ),
        WhiteBoxHarnessWriter.writeTextArtifact(
            outputDir = outputDir,
            joinKey = joinKey,
            artifactId = "map-slice",
            kind = "map_slice",
            fileName = "map-slice.txt",
            summary = "Readable local map slice around the interaction point.",
            content = mapSlice,
            tags = listOf("map", "slice"),
        ),
    )

    private fun renderMapSlice(
        session: FoundationGameSession,
        center: Point,
        targetId: EntityId,
        chainId: EntityId?,
    ): String {
        val world = session.automationWorld()
        return buildString {
            for (y in center.y - 2..center.y + 2) {
                for (x in center.x - 2..center.x + 2) {
                    val point = Point(x, y)
                    append(
                        when {
                            !session.map.isInBounds(x, y) -> ' '
                            point == session.playerPosition() -> '@'
                            world.get<Position>(targetId)?.toPoint() == point -> 'T'
                            chainId != null && world.get<Position>(chainId)?.toPoint() == point -> 'C'
                            TerrainTag.ICE in session.automationTerrainTagsAt(point) -> '*'
                            TerrainTag.WATER in session.automationTerrainTagsAt(point) -> '~'
                            TerrainTag.OIL in session.automationTerrainTagsAt(point) -> 'o'
                            session.map[point].blocksMovement -> '#'
                            else -> '.'
                        },
                    )
                }
                appendLine()
            }
        }
    }

    private fun renderTraceMatrix(trace: CombatResolutionTrace): String =
        buildString {
            appendLine("| step | name | flags | outputs |")
            appendLine("| --- | --- | --- | --- |")
            trace.steps.forEach { step ->
                appendLine(
                    "| ${step.stepIndex} | ${step.stepName} | ${step.flags.sorted().joinToString().ifBlank { "-" }} | " +
                        step.outputs.entries.joinToString(separator = "<br>") { (key, value) -> "$key=$value" }.ifBlank { "-" } +
                        " |",
                )
            }
        }

    private fun newSession(
        seed: Long,
        zoneId: String,
    ): FoundationGameSession =
        GameModule.newFoundationSession(
            config = FoundationGameConfig(seed = seed, zoneId = zoneId, playerProfessionId = "templar"),
            saveManager = SaveManager(tempDir.resolve("terrain-$zoneId-$seed")),
        )

    private fun newProbeSession(
        seed: Long,
        zoneId: String,
    ): FoundationGameSession =
        GameModule.newFoundationSession(
            config = FoundationGameConfig(seed = seed, zoneId = zoneId, playerProfessionId = "arcanist"),
            saveManager = SaveManager(tempDir.resolve("terrain-probe-$zoneId-$seed")),
        )

    private fun sha256(payload: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private data class PreparedCombatants(
    val world: World,
    val targetId: EntityId,
    val chainId: EntityId?,
)

private data class TerrainPointCluster(
    val terrainPoints: List<Point>,
)

private data class LocatedMapgenCase(
    val seed: Long,
    val session: FoundationGameSession,
    val targetPoint: Point,
    val adjacentPoint: Point?,
)

private data class TerrainBatchCaseRecord(
    val segment: String,
    val joinKey: WhiteBoxJoinKey,
    val resolvedRuleId: String?,
    val provenanceTerrainTags: Set<String>,
    val preferredTerrainTags: Set<String>,
    val terrainPreferenceImplemented: Boolean,
    val sourceTags: Set<String>,
    val report: WhiteBoxCaseReport,
)

private data class TerrainExposureProbeSummary(
    val seeds: List<Long>,
    val combatCount: Int,
    val taggedCombatCount: Int,
    val triggeredInteractionCombatCount: Int,
    val preferredTerrainCombatCount: Int,
    val preferredTerrainImplementedCombatCount: Int,
    val coverageByZone: Map<String, TerrainZoneExposureSummary>,
) {
    val terrainTaggedCombatExposureRate: Double
        get() = if (combatCount == 0) 0.0 else taggedCombatCount.toDouble() / combatCount.toDouble()

    val terrainInteractionEncounterRate: Double
        get() = if (taggedCombatCount == 0) 0.0 else triggeredInteractionCombatCount.toDouble() / taggedCombatCount.toDouble()

    val preferredTerrainCombatImplementationRate: Double
        get() =
            if (preferredTerrainCombatCount == 0) {
                0.0
            } else {
                preferredTerrainImplementedCombatCount.toDouble() / preferredTerrainCombatCount.toDouble()
            }
}

private data class TerrainZoneExposureSummary(
    val combatCount: Int,
    val taggedCombatCount: Int,
    val triggeredInteractionCombatCount: Int,
    val preferredTerrainCombatCount: Int,
    val preferredTerrainImplementedCombatCount: Int,
    val observedTerrainTags: Set<String>,
    val observedPreferredTerrainTags: Set<String>,
    val triggeredRuleIds: Set<String>,
) {
    val terrainTaggedCombatExposureRate: Double
        get() = if (combatCount == 0) 0.0 else taggedCombatCount.toDouble() / combatCount.toDouble()

    val terrainInteractionEncounterRate: Double
        get() = if (taggedCombatCount == 0) 0.0 else triggeredInteractionCombatCount.toDouble() / taggedCombatCount.toDouble()

    val preferredTerrainCombatImplementationRate: Double
        get() =
            if (preferredTerrainCombatCount == 0) {
                0.0
            } else {
                preferredTerrainImplementedCombatCount.toDouble() / preferredTerrainCombatCount.toDouble()
            }
}

private fun terrainDecisionPath(
    terrainTaggedCombatExposureRate: Double,
    terrainInteractionEncounterRate: Double,
): String =
    when {
        terrainTaggedCombatExposureRate < 0.20 -> "Path A"
        terrainInteractionEncounterRate < 0.15 -> "Path B"
        else -> "Path C"
    }

private enum class TerrainSecondaryEffect {
    CHAIN_DAMAGE,
    IGNITE_OVERRIDE,
    TERRAIN_TRANSFORM,
    MELT_AND_REMOVE_FREEZE,
    SLIP_STATUS,
}

private fun allTerrainRuleIds(): Set<String> =
    linkedSetOf(
        ElementInteractionRegistry.TERRAIN_LIGHTNING_WATER_CHAIN,
        ElementInteractionRegistry.TERRAIN_FIRE_OIL_IGNITE,
        ElementInteractionRegistry.TERRAIN_COLD_WATER_FREEZE,
        ElementInteractionRegistry.TERRAIN_FIRE_ICE_MELT,
        ElementInteractionRegistry.TERRAIN_PHYSICAL_ICE_SLIP,
    )

private val TERRAIN_EXPOSURE_ZONE_IDS: List<String> =
    listOf(
        // The exposure baseline only samples zones that produce repeatable encounter-driven combat in real runs.
        // abyssal_temple is excluded here because its current runtime is objective/pressure-driven and configures
        // zero regular monster spawns, which would collapse the "direct combat observation" denominator.
        "greenwood_fringe",
        "deep_iron_pit",
        "underground_river",
        "crystal_cavern",
    )

private const val TERRAIN_EXPOSURE_SEED_BASE: Long = 20260409010000L
private const val TERRAIN_EXPOSURE_ZONE_SEED_BLOCK: Long = 1_000L
private const val TERRAIN_EXPOSURE_SEEDS_PER_ZONE: Int = 125
private const val TERRAIN_EXPOSURE_TURN_BUDGET: Int = 36
private const val TERRAIN_EXPOSURE_MAX_FLOOR: Int = 2

private fun FoundationGameSession.TerrainCombatObservation.isTerrainTagged(): Boolean =
    attackerTerrainTags.isNotEmpty() || targetTerrainTags.isNotEmpty()

private fun FoundationGameSession.TerrainCombatObservation.hasPreferredTerrain(): Boolean =
    attackerPreferredTerrainTags.isNotEmpty() || targetPreferredTerrainTags.isNotEmpty()

private fun FoundationGameSession.TerrainCombatObservation.preferredTerrainImplemented(): Boolean =
    terrainPreferenceImplemented(
        preferredTerrainTags = attackerPreferredTerrainTags,
        directTerrainTags = attackerTerrainTags,
        adjacentTerrainTags = attackerAdjacentTerrainTags,
    ) &&
        terrainPreferenceImplemented(
            preferredTerrainTags = targetPreferredTerrainTags,
            directTerrainTags = targetTerrainTags,
            adjacentTerrainTags = targetAdjacentTerrainTags,
        )

private fun shouldPrioritizeTerrainProbeRouteProgress(observation: RunObservation): Boolean =
    observation.visibleBossPositions.isEmpty() &&
        observation.visibleHostilePositions.none { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) <= 2 }
