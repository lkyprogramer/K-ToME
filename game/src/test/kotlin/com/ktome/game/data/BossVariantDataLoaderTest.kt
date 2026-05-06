package com.ktome.game.data

import com.ktome.core.ai.TriggerExpression
import com.ktome.core.ai.referenceIds
import com.ktome.game.contentpack.ContentPackSelection
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class BossVariantDataLoaderTest {
    @Test
    fun `data loader parses not trigger expressions from boss variant overlays`(
        @TempDir tempDir: Path,
    ) {
        val packRoot =
            writeBossVariantPack(
                tempDir = tempDir,
                triggerYaml =
                    """
                    not:
                      ref: zone.trigger.void_pressure_active
                    """.trimIndent(),
            )

        val catalog = DataLoader(packSelection = ContentPackSelection.of(packRoot)).loadSchemaCatalog()
        val trigger = catalog.bossVariants.first { variant -> variant.id == "boss.variant.fixture_loader_molten_glass" }.phaseOverrides.single().trigger

        val not = trigger as? TriggerExpression.Not
        assertEquals(setOf("zone.trigger.void_pressure_active"), not?.child?.referenceIds())
    }

    @Test
    fun `data loader rejects trigger expressions with multiple declared shapes`(
        @TempDir tempDir: Path,
    ) {
        val packRoot =
            writeBossVariantPack(
                tempDir = tempDir,
                triggerYaml =
                    """
                    ref: boss.trigger.hp_below_50
                    allOf:
                      - ref: boss.trigger.hp_below_50
                      - ref: zone.trigger.oil_or_fire_seen
                    """.trimIndent(),
            )

        val ex =
            assertThrows<IllegalArgumentException> {
                DataLoader(packSelection = ContentPackSelection.of(packRoot)).loadSchemaCatalog()
            }

        assertTrue(ex.message.orEmpty().contains("exactly one of ref, allOf, anyOf, or not"))
    }

    private fun writeBossVariantPack(
        tempDir: Path,
        triggerYaml: String,
    ): Path {
        val packRoot = tempDir.resolve("boss-variant-loader-pack")
        val dataDir = packRoot.resolve("data/boss-variants")
        val triggerBlock = triggerYaml.prependIndent("          ")
        Files.createDirectories(dataDir)
        Files.writeString(
            packRoot.resolve("manifest.yaml"),
            """
            |id: fixture.boss_variant_loader
            |version: 1.0.0
            |schemaVersion: 2
            |gameVersionRange: ">=0.4.0 <0.5.0"
            |namespace: fixture_boss_variant_loader
            |dependencies: []
            |localeBundles: []
            |overlays:
            |  - targetRef:
            |      registry: boss_variant
            |      id: boss.variant.fixture_loader_molten_glass
            |    op: ADD
            |    sourceFile: data/boss-variants/molten-glass.yaml
            """.trimMargin(),
        )
        Files.writeString(
            dataDir.resolve("molten-glass.yaml"),
            """
            |bossVariants:
            |  - id: boss.variant.fixture_loader_molten_glass
            |    baseEncounterId: molten_giant_encounter
            |    grantedMutations: [elite.ironhide, elite.emberblood]
            |    threatCost: 5
            |    lootProfileOverride: loot.deep_iron_pit.reward
            |    visualTintKey: vfx.boss.variant.molten_glass
            |    actionWeightProfileId: boss.variant.weight.molten_glass
            |    phaseOverrides:
            |      - phaseId: phase_enraged
            |        trigger:
$triggerBlock
            |        telegraphSpecId: molten_glass_phase_override_warning
            |        actionEmphasisIds: [linebreaker, earthshaker]
            |        onEnterEventKey: boss.variant.fixture_loader_molten_glass.phase_override.entered
            """.trimMargin(),
        )
        return packRoot
    }
}
