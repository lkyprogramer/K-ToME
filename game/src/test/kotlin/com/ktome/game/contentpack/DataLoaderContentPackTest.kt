package com.ktome.game.contentpack

import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.LootPoolStrategy
import com.ktome.game.hidden.HiddenEventRewardPayload
import com.ktome.game.i18n.GameLocale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DataLoaderContentPackTest {
    @Test
    fun `data loader merges official sample pack secret zone reward authority and special templates`() {
        val loader =
            DataLoader(
                locale = GameLocale.EN_US,
                packSelection =
                    ContentPackFixtureCatalog.selection(
                        activePackRoots = listOf(ContentPackFixtureCatalog.samplePackRoot()),
                    ),
            )

        val catalog = loader.loadSchemaCatalog()
        val secretZone = requireNotNull(catalog.secretZones.firstOrNull { zone -> zone.id.id == "underground_river_crystal_rift" })
        val hiddenEvent = requireNotNull(catalog.hiddenEvents.firstOrNull { event -> event.id == "hidden.event.underground_river.crystal_rift.reward" })
        val lootProfile = requireNotNull(catalog.lootProfiles.firstOrNull { profile -> profile.id == "sample.flooded_relics.loot.flooded_reliquary.secret" })
        val specialTemplateIds =
            (catalog.itemBundle.uniqueTemplates + catalog.itemBundle.artifactTemplates)
                .map { template -> template.id }
                .filter { templateId -> templateId.startsWith("sample.flooded_relics.") }
                .toSet()

        assertEquals("sample_flooded_relics.zone.flooded_reliquary.name", secretZone.nameKey)
        assertEquals("sample_flooded_relics.zone.flooded_reliquary.visual", secretZone.visualKey)
        assertEquals("sample_flooded_relics.audio.zone.flooded_reliquary", secretZone.audioProfile)
        assertTrue(hiddenEvent.rewards.any { reward -> reward.payload is HiddenEventRewardPayload.SecretZoneReward })
        assertEquals("sample.flooded_relics.loot.flooded_reliquary.secret", secretZone.rewardProfileId.id)
        assertEquals(7, lootProfile.rewardBudget)
        assertEquals(3, lootProfile.schemaVersion)
        assertEquals("underground_river", lootProfile.canonicalZoneId)
        assertEquals(LootPoolStrategy.TAG_WEIGHTED, lootProfile.poolStrategy)
        assertEquals(listOf("underground_river"), lootProfile.itemTagFilter)
        assertEquals(emptyList<String>(), lootProfile.excludeIds)
        assertEquals(mapOf("WEAPON" to 2, "ARMOR" to 2, "CONSUMABLE" to 1), lootProfile.typeWeights.mapKeys { it.key.name })
        assertEquals(mapOf("OFF_HAND" to 3, "WEAPON" to 2, "ARMOR" to 1), lootProfile.slotBias.mapKeys { it.key.name })
        assertEquals(listOf("underground_river"), lootProfile.specialTemplateTagPreference)
        assertEquals(listOf("underground_river"), lootProfile.affixTagPreference)
        assertEquals(
            setOf(
                "sample.flooded_relics.unique.floodtide_lantern",
                "sample.flooded_relics.artifact.tideglass_echo",
            ),
            specialTemplateIds,
        )
        assertTrue(catalog.visualKeys.contains("sample_flooded_relics.item.tideglass_echo.visual"))
        assertTrue(catalog.audioProfiles.contains("sample_flooded_relics.audio.item.tideglass_echo"))
        assertFalse(loader.localizer.text(secretZone.nameKey).startsWith("!!"))
    }

    @Test
    fun `data loader applies fixture precedence pack over sample pack dependency`() {
        val loader =
            DataLoader(
                locale = GameLocale.EN_US,
                packSelection =
                    ContentPackFixtureCatalog.selection(
                        activePackRoots = listOf(ContentPackFixtureCatalog.fixturePackRoot(ContentPackFixtureCatalog.samplePrecedenceFixturePackId)),
                        availablePackRoots =
                            listOf(
                                ContentPackFixtureCatalog.samplePackRoot(),
                                ContentPackFixtureCatalog.fixturePackRoot(ContentPackFixtureCatalog.samplePrecedenceFixturePackId),
                            ),
                    ),
            )

        val catalog = loader.loadSchemaCatalog()
        val secretZone = requireNotNull(catalog.secretZones.firstOrNull { zone -> zone.id.id == "underground_river_crystal_rift" })

        assertEquals("fixture_sample_flooded_relics_override.zone.flooded_reliquary.name", secretZone.nameKey)
        assertEquals("fixture_sample_flooded_relics_override.zone.flooded_reliquary.visual", secretZone.visualKey)
        assertEquals("fixture_sample_flooded_relics_override.audio.zone.flooded_reliquary", secretZone.audioProfile)
    }

    @Test
    fun `data loader preserves split special and affix preferences from fixture overlay`() {
        val loader =
            DataLoader(
                locale = GameLocale.EN_US,
                packSelection =
                    ContentPackFixtureCatalog.selection(
                        activePackRoots = listOf(ContentPackFixtureCatalog.fixturePackRoot(ContentPackFixtureCatalog.sampleBiasSplitFixturePackId)),
                        availablePackRoots =
                            listOf(
                                ContentPackFixtureCatalog.samplePackRoot(),
                                ContentPackFixtureCatalog.fixturePackRoot(ContentPackFixtureCatalog.sampleBiasSplitFixturePackId),
                            ),
                    ),
            )

        val catalog = loader.loadSchemaCatalog()
        val lootProfile = requireNotNull(catalog.lootProfiles.firstOrNull { profile -> profile.id == "sample.flooded_relics.loot.flooded_reliquary.secret" })

        assertEquals(listOf("underground_river"), lootProfile.specialTemplateTagPreference)
        assertEquals(listOf("water"), lootProfile.affixTagPreference)
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

    @Test
    fun `data loader rejects legacy v2 loot profiles with dedicated structured diagnostic`() {
        val exception =
            assertThrows(ContentPackLoadException::class.java) {
                DataLoader(
                    locale = GameLocale.EN_US,
                    packSelection =
                        ContentPackFixtureCatalog.availableSelection(
                            listOf(ContentPackFixtureCatalog.legacyV2LootProfilePackId),
                        ),
                ).loadSchemaCatalog()
            }

        assertEquals(setOf("content-pack.loot-profile.schema-version-mismatch"), exception.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
        val diagnostic = exception.diagnostics.single()
        assertEquals(ContentPackFixtureCatalog.legacyV2LootProfilePackId.value, diagnostic.details["packId"])
        assertEquals("loot.foundation.common", diagnostic.details["targetProfileId"])
        assertEquals("2", diagnostic.details["actualSchemaVersion"])
        assertEquals("3", diagnostic.details["expectedSchemaVersion"])
    }
}
