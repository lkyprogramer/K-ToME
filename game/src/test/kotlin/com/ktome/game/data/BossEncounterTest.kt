package com.ktome.game.data

import com.ktome.core.ai.BossPhaseEventType
import java.lang.reflect.InvocationTargetException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BossEncounterTest {
    @Test
    fun `boss encounter schema keeps phase transition side effects on the unified telegraph contract`() {
        val encounter = DataLoader().loadSchemaCatalog().bossEncounters.first { it.id == "molten_giant_encounter" }
        val phase = encounter.phases.first { it.id == "phase_enraged" }

        assertEquals("orc.molten_giant", encounter.templateId)
        assertEquals("ai.boss.molten_giant.phase_enraged", phase.aiProfileId)
        assertTrue(phase.resetAiPhaseState)
        assertEquals(
            listOf(BossPhaseEventType.EMIT_EVENT, BossPhaseEventType.TELEGRAPH, BossPhaseEventType.INVULNERABLE),
            phase.onEnter.map { event -> event.type },
        )
        assertEquals(
            "molten_giant_phase_warning",
            phase.onEnter.first { event -> event.type == BossPhaseEventType.TELEGRAPH }.telegraphSpecId,
        )
    }

    @Test
    fun `loader rejects telegraph boss events without telegraph spec id`() {
        val loader = DataLoader()
        val parseMethod =
            DataLoader::class.java.getDeclaredMethod("parseBossPhaseEvent", Map::class.java, Set::class.java).apply {
                isAccessible = true
            }

        val error =
            assertThrows(InvocationTargetException::class.java) {
                parseMethod.invoke(
                    loader,
                    linkedMapOf("type" to "TELEGRAPH"),
                    setOf("molten_giant_phase_warning"),
                )
            }

        assertTrue(requireNotNull(error.cause?.message).contains("telegraphSpecId"))
    }
}
