package com.ktome.game.contentpack

import com.ktome.game.data.DataLoader
import com.ktome.game.i18n.GameLocale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataLoaderContentPackTest {
    @Test
    fun `data loader merges add pack monster and pack local localization`() {
        val loader =
            DataLoader(
                locale = GameLocale.EN_US,
                packSelection = ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.addPackId)),
            )

        val catalog = loader.loadSchemaCatalog()
        val monster = requireNotNull(catalog.monsters.firstOrNull { entry -> entry.id == "fixture.add_monster.flooded_rat" })

        assertEquals("fixture_add_monster.monster.flooded_rat.name", monster.nameKey)
        assertEquals("fixture_add_monster.actor.flooded_rat", monster.visualKey)
        assertEquals("fixture_add_monster.audio.flooded_rat", monster.audioProfile)
        assertTrue(catalog.visualKeys.contains("fixture_add_monster.actor.flooded_rat"))
        assertTrue(catalog.audioProfiles.contains("fixture_add_monster.audio.flooded_rat"))
        assertFalse(loader.localizer.text(monster.nameKey).startsWith("!!"))
    }

    @Test
    fun `data loader applies replace pack over dependency pack`() {
        val loader =
            DataLoader(
                locale = GameLocale.EN_US,
                packSelection = ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.replacePackId)),
            )

        val catalog = loader.loadSchemaCatalog()
        val monster = requireNotNull(catalog.monsters.firstOrNull { entry -> entry.id == "fixture.add_monster.flooded_rat" })

        assertEquals("fixture_replace_monster.monster.flooded_rat.name", monster.nameKey)
        assertEquals("fixture_replace_monster.actor.flooded_rat", monster.visualKey)
        assertEquals("fixture_replace_monster.audio.flooded_rat", monster.audioProfile)
    }

    @Test
    fun `data loader rejects append runtime path and duplicate add without replace`() {
        val appendException =
            assertThrows(ContentPackLoadException::class.java) {
                DataLoader(
                    locale = GameLocale.EN_US,
                    packSelection = ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.appendPackId)),
                ).loadSchemaCatalog()
            }
        val duplicateException =
            assertThrows(ContentPackLoadException::class.java) {
                DataLoader(
                    locale = GameLocale.EN_US,
                    packSelection =
                        ContentPackFixtureCatalog.availableSelection(listOf(ContentPackFixtureCatalog.duplicateWithoutReplacePackId)),
                ).loadSchemaCatalog()
            }

        assertEquals(setOf("content-pack.overlay.runtime-op-forbidden"), appendException.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
        assertEquals(setOf("content-pack.overlay.add-conflict"), duplicateException.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
    }
}
