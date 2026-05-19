package com.ktome.game.contentpack

import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.LootPoolStrategy
import com.ktome.game.hidden.HiddenEventRewardPayload
import com.ktome.game.i18n.GameLocale
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataLoaderContentPackTest {
    @TempDir
    lateinit var tempDir: Path

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
        val baseSecretZone = requireNotNull(catalog.secretZones.firstOrNull { zone -> zone.id.id == "underground_river_crystal_rift" })
        val secretZone = requireNotNull(catalog.secretZones.firstOrNull { zone -> zone.id.id == "sample.flooded_relics.secret_zone.flooded_reliquary" })
        val hiddenEvent = requireNotNull(catalog.hiddenEvents.firstOrNull { event -> event.id == "sample.flooded_relics.hidden_event.flooded_reliquary.reward" })
        val lootProfile = requireNotNull(catalog.lootProfiles.firstOrNull { profile -> profile.id == "sample.flooded_relics.loot.flooded_reliquary.secret" })
        val undergroundProfile = requireNotNull(catalog.zoneMapgenProfiles.firstOrNull { profile -> profile.zoneId == "underground_river" })
        val specialTemplateIds =
            (catalog.itemBundle.uniqueTemplates + catalog.itemBundle.artifactTemplates)
                .map { template -> template.id }
                .filter { templateId -> templateId.startsWith("sample.flooded_relics.") }
                .toSet()

        assertEquals("zone.secret.underground_river_crystal_rift.name", baseSecretZone.nameKey)
        assertEquals("sample_flooded_relics.zone.flooded_reliquary.name", secretZone.nameKey)
        assertEquals("sample_flooded_relics.zone.flooded_reliquary.visual", secretZone.visualKey)
        assertEquals("sample_flooded_relics.audio.zone.flooded_reliquary", secretZone.audioProfile)
        assertEquals(true, secretZone.secretZoneSelector?.secondarySlot)
        assertEquals("hidden.critical.adjacent", secretZone.entranceBindingId.value)
        assertTrue(hiddenEvent.rewards.any { reward -> reward.payload is HiddenEventRewardPayload.SecretZoneReward })
        assertEquals("sample.flooded_relics.loot.flooded_reliquary.secret", secretZone.rewardProfileId.id)
        assertTrue(
            undergroundProfile.hiddenEntrancePlans.any { plan ->
                plan.bindingId.value == "sample.flooded_relics.search.flooded_reliquary" &&
                    plan.sourceAnchorId.value == "hidden.critical.adjacent" &&
                    plan.targetSecretZoneId.id == "sample.flooded_relics.secret_zone.flooded_reliquary"
            },
        )
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
        val secretZone = requireNotNull(catalog.secretZones.firstOrNull { zone -> zone.id.id == "sample.flooded_relics.secret_zone.flooded_reliquary" })

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
    fun `data loader rejects talent overlay registry remains unsupported`() {
        val packRoot = tempDir.resolve("unsupported-talent-overlay-pack")
        Files.createDirectories(packRoot.resolve("data/talents"))
        Files.writeString(
            packRoot.resolve("manifest.yaml"),
            """
            |id: fixture.unsupported_talent_overlay
            |version: 1.0.0
            |schemaVersion: ${ContentPackManifest.SCHEMA_VERSION}
            |gameVersionRange: ">=0.4.0 <0.5.0"
            |namespace: fixture_unsupported_talent_overlay
            |dependencies: []
            |overlays:
            |  - targetRef:
            |      registry: talent
            |      id: unyielding
            |    op: ADD
            |    sourceFile: data/talents/unyielding.yaml
            |
            """.trimMargin(),
        )
        Files.writeString(
            packRoot.resolve("data/talents/unyielding.yaml"),
            """
            |talents: []
            |
            """.trimMargin(),
        )

        val exception =
            assertThrows(ContentPackLoadException::class.java) {
                DataLoader(
                    locale = GameLocale.EN_US,
                    packSelection = ContentPackSelection.of(packRoot),
                ).loadSchemaCatalog()
            }

        assertEquals(setOf("content-pack.overlay.registry-unsupported"), exception.diagnostics.map { diagnostic -> diagnostic.code }.toSet())
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

    @Test
    fun `active pack key summary reports invalid locale bundle with bundle path`() {
        val packRoot = tempDir.resolve("bad-locale-pack")
        Files.createDirectories(packRoot.resolve("i18n"))
        Files.writeString(
            packRoot.resolve("manifest.yaml"),
            """
            |id: fixture.bad_locale
            |version: 0.1.0
            |schemaVersion: ${ContentPackManifest.SCHEMA_VERSION}
            |gameVersionRange: ">=0.4.0"
            |namespace: fixture_bad_locale
            |dependencies: []
            |overlays: []
            |localeBundles:
            |  - i18n/en-US.json
            |
            """.trimMargin(),
        )
        Files.writeString(packRoot.resolve("i18n/en-US.json"), "{not-json")
        val loader =
            DataLoader(
                locale = GameLocale.EN_US,
                packSelection = ContentPackSelection.of(packRoot),
            )

        val exception = assertThrows(ContentPackLoadException::class.java) {
            loader.activePackKeyResolutionSummary
        }
        val diagnostic = exception.diagnostics.single()

        assertEquals("content-pack.locale-bundle.load-failure", diagnostic.code)
        assertEquals("fixture.bad_locale", diagnostic.packId?.value)
        assertTrue(diagnostic.sourcePath.orEmpty().endsWith("i18n/en-US.json"), diagnostic.sourcePath)
        assertTrue(diagnostic.details["exceptionType"].orEmpty().isNotBlank(), diagnostic.details.toString())
    }
}
