package com.ktome.core.combat

import com.ktome.core.support.TestRandomSource
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CombatTraceGoldenTest {
    @Test
    fun `formula trace snapshot keeps the phase3 envelope and serializes stably`() {
        val outcome =
            CombatPipeline(TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0))).resolve(
                DamageRequest(
                    abilityId = "golden_fireball",
                    traceId = "golden-1",
                    damageType = DamageType.FIRE,
                    baseDamage = 10,
                    attackerAccuracy = 25,
                    targetEvasion = 0,
                    targetResistance = 50,
                    targetCurrentHp = 30,
                ),
            )

        val snapshot = CombatTraceSnapshot(outcome.envelope, outcome.trace)
        val json = Json { prettyPrint = true }.encodeToString(CombatTraceSnapshot.serializer(), snapshot)

        assertEquals(CombatRuleset.PHASE_ID, outcome.envelope.phaseId)
        assertEquals(CombatCorpusId.FORMULA, outcome.envelope.corpusId)
        assertTrue(json.contains("\"traceId\": \"golden-1\""))
        assertTrue(json.contains("\"corpusId\": \"FORMULA\""))
        assertTrue(json.contains("\"stepName\": \"FINAL_DAMAGE_APPLICATION\""))
    }
}
