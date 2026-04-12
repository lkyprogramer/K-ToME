package com.ktome.game

import com.ktome.game.data.DataLoader
import com.ktome.game.i18n.GameLocale
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AffixBuildTagsTest {
    @Test
    fun `profession build tags include base class axes trees and unlocked talent semantics`() {
        val schemaCatalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        data class Expectation(
            val professionId: String,
            val talentRanks: Map<String, Int>,
            val expectedTags: Set<String>,
        )

        val expectations =
            listOf(
                Expectation(
                    professionId = "vanguard",
                    talentRanks =
                        linkedMapOf(
                            "guard_stance" to 4,
                            "taunt" to 4,
                        ),
                    expectedTags = setOf("vanguard", "stamina", "guard", "hold_line"),
                ),
                Expectation(
                    professionId = "arcanist",
                    talentRanks =
                        linkedMapOf(
                            "fireball" to 1,
                            "ice_bolt" to 1,
                            "blink" to 4,
                        ),
                    expectedTags = setOf("arcanist", "mana", "spell", "fire", "cold", "teleport", "mana_tempo"),
                ),
                Expectation(
                    professionId = "rogue",
                    talentRanks =
                        linkedMapOf(
                            "shadowstep" to 2,
                            "shadow_bind" to 4,
                        ),
                    expectedTags = setOf("rogue", "energy", "marked", "crit", "execute"),
                ),
                Expectation(
                    professionId = "templar",
                    talentRanks =
                        linkedMapOf(
                            "holy_mark" to 3,
                            "purify" to 4,
                        ),
                    expectedTags = setOf("templar", "positive_energy", "bane", "holy", "cleanse", "holy_shield", "melee", "frontline", "guard"),
                ),
            )

        expectations.forEach { expectation ->
            val profession = schemaCatalog.professions.first { profession -> profession.id == expectation.professionId }
            val tags =
                professionAffixBuildTags(
                    schemaCatalog = schemaCatalog,
                    profession = profession,
                    talentRanks = expectation.talentRanks,
                )

            expectation.expectedTags.forEach { tag ->
                assertTrue(tag in tags, "Expected ${expectation.professionId} build tags to include $tag, actual=$tags")
            }
        }
    }
}
