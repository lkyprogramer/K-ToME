package com.ktome.game.data

import com.ktome.core.ai.BossPhaseEventType
import org.junit.jupiter.api.Assertions.assertEquals
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
}
