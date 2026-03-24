package com.ktome.core.ai

import com.ktome.core.combat.DamageType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ThreatRatingResolverTest {
    @Test
    fun `mid damage telegraphs get at least one turn of warning`() {
        val assessment =
            ThreatRatingResolver.assess(
                telegraphSpec =
                    TelegraphSpec(
                        id = "heavy_slash",
                        shape = TelegraphShape.CIRCLE,
                        previewTurns = 1,
                        dangerLevel = DangerLevel.LOW,
                        threatProfileId = "profile.mid",
                        radius = 1,
                    ),
                threatProfile =
                    ThreatProfileDef(
                        id = "profile.mid",
                        defenderArchetype = "frontliner",
                        levelBand = LevelBand(7, 10),
                        difficultyId = "normal",
                        expectedMaxHp = 80,
                        expectedArmor = 0,
                        expectedResistances = mapOf(DamageType.PHYSICAL to 0),
                    ),
                baseAttack = 28,
                damageMultiplier = 1.0,
                damageType = DamageType.PHYSICAL,
            )

        assertEquals(1, assessment.previewTurns)
        assertEquals(DangerLevel.HIGH, assessment.dangerLevel)
    }

    @Test
    fun `high damage telegraphs get at least two turns of warning`() {
        val assessment =
            ThreatRatingResolver.assess(
                telegraphSpec =
                    TelegraphSpec(
                        id = "ground_slam",
                        shape = TelegraphShape.CIRCLE,
                        previewTurns = 1,
                        dangerLevel = DangerLevel.HIGH,
                        threatProfileId = "profile.mid",
                        radius = 2,
                    ),
                threatProfile =
                    ThreatProfileDef(
                        id = "profile.mid",
                        defenderArchetype = "frontliner",
                        levelBand = LevelBand(7, 10),
                        difficultyId = "normal",
                        expectedMaxHp = 80,
                        expectedArmor = 6,
                        expectedResistances = mapOf(DamageType.PHYSICAL to 0),
                    ),
                baseAttack = 36,
                damageMultiplier = 2.0,
                damageType = DamageType.PHYSICAL,
            )

        assertEquals(2, assessment.previewTurns)
        assertEquals(DangerLevel.LETHAL, assessment.dangerLevel)
    }
}
