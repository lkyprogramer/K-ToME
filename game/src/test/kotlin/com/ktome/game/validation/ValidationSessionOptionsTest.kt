package com.ktome.game.validation

import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.elites.BossVariantSelectionMode
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidationSessionOptionsTest {
    @Test
    fun `preset matrix keeps explicit phase4 config defaults stable`() {
        val expectedByPreset =
            mapOf(
                ValidationPreset.MAPGEN_DIFF to
                    ExpectedPresetConfig(
                        seed = 20260401L,
                        seedCorpus = listOf(20260401L, 20260402L, 20260403L, 20260404L, 20260405L),
                        zoneId = "greenwood_fringe",
                        floor = 2,
                        maxFloor = 2,
                        routeIndex = 1,
                        bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
                        preferredBossVariantId = null,
                    ),
                ValidationPreset.HIDDEN_CONTENT to
                    ExpectedPresetConfig(
                        seed = 20260409L,
                        seedCorpus = listOf(20260409L),
                        zoneId = "underground_river",
                        floor = 1,
                        maxFloor = 2,
                        routeIndex = 4,
                        bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
                        preferredBossVariantId = null,
                    ),
                ValidationPreset.TERRAIN_INTERACTION to
                    ExpectedPresetConfig(
                        seed = 20260410L,
                        seedCorpus = listOf(20260410L),
                        zoneId = "deep_iron_pit",
                        floor = 2,
                        maxFloor = 2,
                        routeIndex = 2,
                        bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
                        preferredBossVariantId = null,
                    ),
                ValidationPreset.ELITE_MUTATION to
                    ExpectedPresetConfig(
                        seed = 20260411L,
                        seedCorpus = listOf(20260411L),
                        zoneId = "deep_iron_pit",
                        floor = 1,
                        maxFloor = 2,
                        routeIndex = 2,
                        bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
                        preferredBossVariantId = null,
                    ),
                ValidationPreset.BOSS_VARIANT to
                    ExpectedPresetConfig(
                        seed = 20260412L,
                        seedCorpus = listOf(20260412L),
                        zoneId = "grey_gate_depths",
                        floor = 1,
                        maxFloor = 2,
                        routeIndex = 3,
                        bossVariantSelectionMode = BossVariantSelectionMode.FORCE_AVAILABLE,
                        preferredBossVariantId = null,
                    ),
                ValidationPreset.LOOT_LAB to
                    ExpectedPresetConfig(
                        seed = 20260413L,
                        seedCorpus = listOf(20260413L),
                        zoneId = "shattered_outpost",
                        floor = 1,
                        maxFloor = 2,
                        routeIndex = 0,
                        bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
                        preferredBossVariantId = null,
                    ),
                ValidationPreset.CONTENT_PACK to
                    ExpectedPresetConfig(
                        seed = 20260414L,
                        seedCorpus = listOf(20260414L),
                        zoneId = "underground_river",
                        floor = 1,
                        maxFloor = 2,
                        routeIndex = 4,
                        bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
                        preferredBossVariantId = null,
                    ),
                ValidationPreset.CUSTOM to
                    ExpectedPresetConfig(
                        seed = 20260312L,
                        seedCorpus = listOf(20260312L),
                        zoneId = "shattered_outpost",
                        floor = 1,
                        maxFloor = 2,
                        routeIndex = 0,
                        bossVariantSelectionMode = BossVariantSelectionMode.AUTO,
                        preferredBossVariantId = null,
                    ),
            )

        expectedByPreset.forEach { (preset, expected) ->
            val options = validationSessionOptionsForPreset(preset)

            assertEquals(preset, options.preset)
            assertEquals(expected.seed, options.foundationConfig.seed)
            assertEquals(expected.seedCorpus, options.seedCorpus)
            assertEquals(expected.zoneId, options.foundationConfig.zoneId)
            assertEquals(expected.floor, options.foundationConfig.floor)
            assertEquals(expected.maxFloor, options.foundationConfig.maxFloor)
            assertEquals(expected.routeIndex, options.foundationConfig.routeIndex)
            assertEquals(expected.bossVariantSelectionMode, options.foundationConfig.bossVariantSelectionMode)
            assertEquals(expected.preferredBossVariantId, options.foundationConfig.preferredBossVariantId)
        }
    }

    @Test
    fun `content pack preset preserves formal content pack selection`() {
        val sampleSelection =
            ContentPackSelection.of(
                Path.of("/tmp/sample.flooded_relics"),
            )

        val options =
            validationSessionOptionsForPreset(
                preset = ValidationPreset.CONTENT_PACK,
                contentPackSelection = sampleSelection,
            )

        assertEquals(sampleSelection, options.contentPackSelection)
        assertEquals("underground_river", options.foundationConfig.zoneId)
        assertEquals(BossVariantSelectionMode.DISABLED, options.foundationConfig.bossVariantSelectionMode)
        assertTrue(options.contentPackSelection.activePackRoots.isNotEmpty())
    }

    @Test
    fun `next seed in corpus wraps within the same fixed list`() {
        val options = validationSessionOptionsForPreset(ValidationPreset.MAPGEN_DIFF)

        assertEquals(20260402L, options.nextSeedInCorpus())
        assertEquals(
            20260401L,
            options.copy(
                foundationConfig = options.foundationConfig.copy(seed = 20260405L),
            ).nextSeedInCorpus(),
        )
        assertEquals(
            20260401L,
            options.copy(
                foundationConfig = options.foundationConfig.copy(seed = 99999999L),
            ).nextSeedInCorpus(),
        )
    }

    private data class ExpectedPresetConfig(
        val seed: Long,
        val seedCorpus: List<Long>,
        val zoneId: String,
        val floor: Int,
        val maxFloor: Int,
        val routeIndex: Int,
        val bossVariantSelectionMode: BossVariantSelectionMode,
        val preferredBossVariantId: String?,
    )
}
