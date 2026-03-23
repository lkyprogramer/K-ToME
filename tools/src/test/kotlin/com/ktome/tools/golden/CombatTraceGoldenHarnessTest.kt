package com.ktome.tools.golden

import com.ktome.core.combat.ApplicationPolicy
import com.ktome.core.combat.CallbackDecision
import com.ktome.core.combat.CallbackFlow
import com.ktome.core.combat.CombatCallbackPhase
import com.ktome.core.combat.CombatCorpusId
import com.ktome.core.combat.CombatPipeline
import com.ktome.core.combat.CombatResolutionTrace
import com.ktome.core.combat.DamageRequest
import com.ktome.core.combat.DamageType
import com.ktome.core.combat.ElementalInteraction
import com.ktome.core.combat.PipelineCallback
import com.ktome.core.combat.SaveDimension
import com.ktome.core.combat.StatusApplicationRequest
import com.ktome.core.combat.TraceEnvelope
import com.ktome.core.ecs.EntityId
import com.ktome.core.random.RandomSource
import com.ktome.game.harness.HarnessReportWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class CombatTraceGoldenHarnessTest {
    @Test
    @Tag("combatTraceGolden")
    fun `phase3 formula corpus matches the canonical versioned golden`() {
        val scenarios =
            listOf(
                scenario(
                    scenarioId = "basic_physical",
                    random = FixedRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0)),
                    request =
                        DamageRequest(
                            abilityId = "basic_attack",
                            traceId = "formula-basic-physical",
                            damageType = DamageType.PHYSICAL,
                            baseDamage = 12,
                            attackerAccuracy = 25,
                            targetEvasion = 0,
                            targetArmor = 40,
                            targetCurrentHp = 30,
                        ),
                ),
                scenario(
                    scenarioId = "crit_fire",
                    random = FixedRandomSource(doubles = listOf(0.0, 0.0), ints = listOf(0)),
                    request =
                        DamageRequest(
                            abilityId = "fire_crit",
                            traceId = "formula-crit-fire",
                            damageType = DamageType.FIRE,
                            baseDamage = 14,
                            attackerAccuracy = 30,
                            targetEvasion = 0,
                            attackerCritChance = 0.20,
                            targetResistance = 25,
                            targetCurrentHp = 40,
                        ),
                ),
                scenario(
                    scenarioId = "shield_absorb",
                    random = FixedRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0)),
                    request =
                        DamageRequest(
                            abilityId = "shield_absorb",
                            traceId = "formula-shield-absorb",
                            damageType = DamageType.PHYSICAL,
                            baseDamage = 11,
                            attackerAccuracy = 24,
                            targetEvasion = 0,
                            shieldPoints = 20,
                            targetCurrentHp = 30,
                        ),
                ),
                scenario(
                    scenarioId = "status_success",
                    random = FixedRandomSource(doubles = listOf(0.0, 0.99, 0.0), ints = listOf(0)),
                    request =
                        DamageRequest(
                            abilityId = "status_success",
                            traceId = "formula-status-success",
                            damageType = DamageType.PHYSICAL,
                            baseDamage = 10,
                            attackerAccuracy = 24,
                            targetEvasion = 0,
                            targetCurrentHp = 35,
                            statusApplication =
                                StatusApplicationRequest(
                                    statusId = "stun",
                                    duration = 2,
                                    applicationPolicy = ApplicationPolicy.HOSTILE_HIT_THEN_SAVE,
                                    saveDimension = SaveDimension.PHYSICAL,
                                    power = 30,
                                    save = 10,
                                ),
                        ),
                ),
                scenario(
                    scenarioId = "status_resisted",
                    random = FixedRandomSource(doubles = listOf(0.0, 0.99, 0.99), ints = listOf(0)),
                    request =
                        DamageRequest(
                            abilityId = "status_resisted",
                            traceId = "formula-status-resisted",
                            damageType = DamageType.COLD,
                            baseDamage = 10,
                            attackerAccuracy = 24,
                            targetEvasion = 0,
                            targetCurrentHp = 35,
                            statusApplication =
                                StatusApplicationRequest(
                                    statusId = "freeze",
                                    duration = 2,
                                    applicationPolicy = ApplicationPolicy.HOSTILE_HIT_THEN_SAVE,
                                    saveDimension = SaveDimension.SPELL,
                                    power = 18,
                                    save = 32,
                                ),
                        ),
                ),
                scenario(
                    scenarioId = "elemental_interaction",
                    random = FixedRandomSource(doubles = listOf(0.0, 0.99, 0.0), ints = listOf(0)),
                    request =
                        DamageRequest(
                            abilityId = "holy_mark",
                            traceId = "formula-elemental-interaction",
                            damageType = DamageType.HOLY,
                            baseDamage = 10,
                            attackerAccuracy = 25,
                            targetEvasion = 0,
                            targetCurrentHp = 35,
                            statusApplication =
                                StatusApplicationRequest(
                                    statusId = "bane",
                                    duration = 4,
                                    applicationPolicy = ApplicationPolicy.HOSTILE_HIT_THEN_SAVE,
                                    saveDimension = SaveDimension.SPELL,
                                    power = 30,
                                    save = 10,
                                ),
                            elementalInteraction = ElementalInteraction("holy_burst", "BANE_TRIGGERED", childTraceId = "child-formula-1"),
                            callbacks =
                                listOf(
                                    PipelineCallback(EntityId(2), "holy-thorns", CombatCallbackPhase.ON_DAMAGE_TAKEN, 100) {
                                        CallbackDecision(effect = "RETALIATE_LOGGED")
                                    },
                                ),
                        ),
                ),
            )

        val payload = buildGoldenPayload(scenarios)
        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "combat/formula/corpus",
            payload = payload,
            markdown =
                buildString {
                    appendLine("# Combat Trace Golden")
                    appendLine()
                    appendLine("| Scenario | Trace ID | Hit | Crit | Final Damage | Status |")
                    appendLine("| --- | --- | --- | --- | --- | --- |")
                    scenarios.forEach { scenario ->
                        appendLine(
                            "| ${scenario.scenarioId} | ${scenario.trace.traceId} | ${scenario.trace.result.hit} | " +
                                "${scenario.trace.result.critical} | ${scenario.trace.result.finalDamage} | ${scenario.statusReason()} |",
                        )
                    }
                },
        )

        maybeRecordGolden(payload)
        val expected = loadCanonicalGolden()
        assertEquals(expected, payload, "FORMULA corpus drifted from the canonical Phase 3 golden baseline.")
        assertTrue(scenarios.all { scenario -> scenario.envelope.corpusId == CombatCorpusId.FORMULA })
        assertTrue(scenarios.any { scenario -> scenario.trace.childTraceIds.isNotEmpty() })
    }

    private fun scenario(
        scenarioId: String,
        random: RandomSource,
        request: DamageRequest,
    ): NamedCombatTraceSnapshot {
        val outcome = CombatPipeline(random).resolve(request)
        check(outcome.envelope.corpusId == CombatCorpusId.FORMULA) {
            "Scenario $scenarioId produced a non-formula corpus envelope."
        }
        return NamedCombatTraceSnapshot(
            scenarioId = scenarioId,
            envelope = outcome.envelope,
            trace = outcome.trace,
            statusReason = outcome.statusApplication?.reasonTag,
        )
    }

    private fun buildGoldenPayload(scenarios: List<NamedCombatTraceSnapshot>): JsonElement {
        val sorted = scenarios.sortedBy(NamedCombatTraceSnapshot::scenarioId)
        val firstEnvelope = sorted.first().envelope
        return buildJsonObject {
            put("phaseId", firstEnvelope.phaseId)
            put("rulesetVersion", firstEnvelope.rulesetVersion)
            put("traceSchemaVersion", firstEnvelope.traceSchemaVersion)
            put("corpusId", firstEnvelope.corpusId.name)
            putJsonArray("scenarios") {
                sorted.forEach { scenario ->
                    add(
                        buildJsonObject {
                            put("scenarioId", scenario.scenarioId)
                            put("statusReason", scenario.statusReason)
                            put("envelope", json.encodeToJsonElement(TraceEnvelope.serializer(), scenario.envelope))
                            put("trace", json.encodeToJsonElement(CombatResolutionTrace.serializer(), scenario.trace))
                        },
                    )
                }
            }
        }
    }

    private fun loadCanonicalGolden(): JsonElement {
        javaClass.getResource("/golden/combat/formula/corpus.json")?.let { resource ->
            return json.parseToJsonElement(resource.readText())
        }
        val resourcePath = goldenResourcePath()
        check(Files.exists(resourcePath)) {
            "Missing canonical combat golden resource."
        }
        return json.parseToJsonElement(Files.readString(resourcePath))
    }

    private fun maybeRecordGolden(payload: JsonElement) {
        if (System.getProperty("ktome.updateCombatGolden") != "true") {
            return
        }
        val resourcePath = goldenResourcePath()
        Files.createDirectories(resourcePath.parent)
        Files.writeString(resourcePath, json.encodeToString(payload))
    }

    private data class NamedCombatTraceSnapshot(
        val scenarioId: String,
        val envelope: TraceEnvelope,
        val trace: CombatResolutionTrace,
        val statusReason: String? = null,
    ) {
        fun statusReason(): String = statusReason ?: "-"
    }

    private class FixedRandomSource(
        doubles: List<Double>,
        ints: List<Int>,
    ) : RandomSource {
        private val doubleValues = ArrayDeque(doubles)
        private val intValues = ArrayDeque(ints)

        override fun nextDouble(): Double = if (doubleValues.isEmpty()) 0.0 else doubleValues.removeFirst()

        override fun nextInt(
            fromInclusive: Int,
            untilExclusive: Int,
        ): Int {
            val next = if (intValues.isEmpty()) 0 else intValues.removeFirst()
            require(next in fromInclusive until untilExclusive) {
                "Queued int $next is outside [$fromInclusive, $untilExclusive)."
            }
            return next
        }
    }

    private companion object {
        val json: Json = Json {
            prettyPrint = true
            encodeDefaults = true
        }

        fun goldenResourcePath(): Path =
            Path
                .of(System.getProperty("ktome.repo.root", "."))
                .resolve("tools/src/test/resources/golden/combat/formula/corpus.json")
    }
}
