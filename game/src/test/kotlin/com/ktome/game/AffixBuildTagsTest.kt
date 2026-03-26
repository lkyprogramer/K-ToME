package com.ktome.game

import com.ktome.game.data.DataLoader
import com.ktome.game.i18n.GameLocale
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AffixBuildTagsTest {
    @Test
    fun `profession build tags include axes trees and unlocked talent semantics`() {
        val schemaCatalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        val profession = schemaCatalog.professions.first { profession -> profession.id == "arcanist" }

        val tags =
            professionAffixBuildTags(
                schemaCatalog = schemaCatalog,
                profession = profession,
                unlockedTalentIds = setOf("fireball", "ice_bolt", "arcane_shield"),
            )

        assertTrue("arcanist" in tags)
        assertTrue("mana" in tags)
        assertTrue("spell" in tags)
        assertTrue("fire" in tags)
        assertTrue("cold" in tags)
        assertTrue("arcane" in tags)
    }
}
