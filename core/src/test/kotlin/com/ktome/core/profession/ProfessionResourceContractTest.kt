package com.ktome.core.profession

import com.ktome.core.profile.ClassUnlockState
import com.ktome.core.resource.DecayPolicy
import com.ktome.core.resource.ResourceAxis
import com.ktome.core.resource.ResourceProfileRef
import com.ktome.core.resource.ResourceRegenProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProfessionResourceContractTest {
    @Test
    fun `single axis profession allows null state axis`() {
        val profession =
            ProfessionDef(
                id = "vanguard",
                tier = ProfessionTier.BASE,
                resourceProfiles =
                    listOf(
                        ResourceProfileRef(
                            axis = ResourceAxis.STAMINA,
                            initialCurrent = 40,
                            max = 40,
                            regenProfile = ResourceRegenProfile.PerTurn(3),
                        ),
                    ),
                primarySpendAxis = ResourceAxis.STAMINA,
                stateAxis = null,
                initialUnlockState = ClassUnlockState.RELEASE_UNLOCKED,
                soloContract = soloContract(),
            )

        assertNull(profession.stateAxis)
        assertEquals(ResourceAxis.STAMINA, profession.primarySpendAxis)
    }

    @Test
    fun `dual axis profession keeps both primary and state axes in resource profiles`() {
        val profession =
            ProfessionDef(
                id = "spellblade",
                tier = ProfessionTier.ADVANCED,
                resourceProfiles =
                    listOf(
                        ResourceProfileRef(
                            axis = ResourceAxis.MANA,
                            initialCurrent = 50,
                            max = 50,
                            regenProfile = ResourceRegenProfile.PerTurn(2),
                        ),
                        ResourceProfileRef(
                            axis = ResourceAxis.EQUILIBRIUM,
                            initialCurrent = 50,
                            max = 100,
                            stableMin = 30,
                            stableMax = 70,
                        ),
                    ),
                primarySpendAxis = ResourceAxis.MANA,
                stateAxis = ResourceAxis.EQUILIBRIUM,
                initialUnlockState = ClassUnlockState.DEV_UNLOCKED,
                releaseUnlockCondition = ReleaseUnlockCondition.RequireProfessionCleared("arcanist"),
                soloContract = soloContract(),
            )

        assertEquals(ResourceAxis.MANA, profession.primarySpendAxis)
        assertEquals(ResourceAxis.EQUILIBRIUM, profession.stateAxis)
        assertEquals(2, profession.resourceProfiles.size)
    }

    @Test
    fun `berserker hate contract can reuse shared decay policy`() {
        val decayPolicy = DecayPolicy(amountPerTurn = 8, outOfCombatOnly = true)
        val profession =
            ProfessionDef(
                id = "berserker",
                tier = ProfessionTier.ADVANCED,
                resourceProfiles =
                    listOf(
                        ResourceProfileRef(
                            axis = ResourceAxis.HATE,
                            initialCurrent = 0,
                            max = 100,
                            regenProfile =
                                ResourceRegenProfile.Composite(
                                    listOf(
                                        ResourceRegenProfile.OnDamageTaken(0.15),
                                        ResourceRegenProfile.OnHit(6),
                                        ResourceRegenProfile.OnKill(12),
                                        ResourceRegenProfile.Decay(decayPolicy),
                                    ),
                                ),
                        ),
                    ),
                primarySpendAxis = ResourceAxis.HATE,
                stateAxis = ResourceAxis.HATE,
                initialUnlockState = ClassUnlockState.DEV_UNLOCKED,
                releaseUnlockCondition = ReleaseUnlockCondition.RequireProfessionCleared("vanguard"),
                soloContract = soloContract(),
            )

        val regen = profession.resourceProfiles.single().regenProfile as ResourceRegenProfile.Composite
        assertEquals(decayPolicy, (regen.entries.last() as ResourceRegenProfile.Decay).policy)
    }

    private fun soloContract(): SoloContractDef =
        SoloContractDef(
            offenseTags = listOf("single_target"),
            defenseTags = listOf("guard"),
            mobilityTags = listOf("dash"),
            aoeAnswerTags = listOf("sweep"),
            bossAnswerTags = listOf("burst"),
            panicAnswerTags = listOf("shield"),
        )
}
