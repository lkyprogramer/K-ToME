package com.ktome.game.validation

import com.ktome.core.profile.ProfileData
import com.ktome.core.save.SaveManager
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.FoundationGameConfig
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.elites.BossVariantSelectionMode
import com.ktome.game.i18n.GameLocale

enum class ProfileRunPersistenceMode {
    WRITE_PROFILE_SUMMARY,
    NO_OP,
}

enum class ValidationPreset(
    val titleKey: String,
    val summaryKey: String,
) {
    MAPGEN_DIFF(
        titleKey = "ui.validation.preset.mapgen_diff",
        summaryKey = "ui.validation.preset.mapgen_diff.summary",
    ),
    HIDDEN_CONTENT(
        titleKey = "ui.validation.preset.hidden_content",
        summaryKey = "ui.validation.preset.hidden_content.summary",
    ),
    TERRAIN_INTERACTION(
        titleKey = "ui.validation.preset.terrain_interaction",
        summaryKey = "ui.validation.preset.terrain_interaction.summary",
    ),
    ELITE_MUTATION(
        titleKey = "ui.validation.preset.elite_mutation",
        summaryKey = "ui.validation.preset.elite_mutation.summary",
    ),
    BOSS_VARIANT(
        titleKey = "ui.validation.preset.boss_variant",
        summaryKey = "ui.validation.preset.boss_variant.summary",
    ),
    LOOT_LAB(
        titleKey = "ui.validation.preset.loot_lab",
        summaryKey = "ui.validation.preset.loot_lab.summary",
    ),
    CONTENT_PACK(
        titleKey = "ui.validation.preset.content_pack",
        summaryKey = "ui.validation.preset.content_pack.summary",
    ),
    CUSTOM(
        titleKey = "ui.validation.preset.custom",
        summaryKey = "ui.validation.preset.custom.summary",
    ),
}

data class ValidationSessionOptions(
    val preset: ValidationPreset = ValidationPreset.MAPGEN_DIFF,
    val foundationConfig: FoundationGameConfig = validationFoundationConfig(ValidationPreset.MAPGEN_DIFF),
    val seedCorpus: List<Long> = validationSeedCorpus(ValidationPreset.MAPGEN_DIFF),
    val contentPackSelection: ContentPackSelection = ContentPackSelection.EMPTY,
    val capabilities: ValidationCapabilitySet = ValidationCapabilitySet.ALL_ENABLED,
    val profileRunPersistenceMode: ProfileRunPersistenceMode = ProfileRunPersistenceMode.NO_OP,
    val scenarioId: ValidationScenarioId? = null,
    val scenarioRouteIndex: Int? = null,
    val scenarioEvidenceSummary: ValidationScenarioEvidenceSummary? = null,
)

data class ValidationSessionRequest(
    val saveManager: SaveManager = SaveManager(ValidationPaths.saveDir()),
    val locale: GameLocale = GameLocale.DEFAULT,
    val profile: ProfileData = ProfileData(),
    val options: ValidationSessionOptions = ValidationSessionOptions(),
)

fun validationSessionOptionsForPreset(
    preset: ValidationPreset,
    contentPackSelection: ContentPackSelection = ContentPackSelection.EMPTY,
): ValidationSessionOptions {
    val foundationConfig = validationFoundationConfig(preset)
    val seedCorpus = validationSeedCorpus(preset)
    return ValidationSessionOptions(
        preset = preset,
        foundationConfig = foundationConfig.copy(seed = seedCorpus.firstOrNull() ?: foundationConfig.seed),
        seedCorpus = seedCorpus,
        contentPackSelection = contentPackSelection,
    )
}

fun ValidationSessionOptions.nextSeedInCorpus(): Long {
    val corpus = seedCorpus.ifEmpty { listOf(foundationConfig.seed) }
    val currentIndex = corpus.indexOf(foundationConfig.seed)
    return if (currentIndex >= 0) {
        corpus[(currentIndex + 1) % corpus.size]
    } else {
        corpus.first()
    }
}

fun ValidationSessionOptions.hasMeaningfulNextSeedRestart(): Boolean =
    seedCorpus.size > 1 && foundationConfig.seed in seedCorpus

internal fun validationFoundationConfig(preset: ValidationPreset): FoundationGameConfig =
    when (preset) {
        ValidationPreset.MAPGEN_DIFF ->
            FoundationGameConfig(
                seed = 20260401L,
                zoneId = "greenwood_fringe",
                playerProfessionId = "rogue",
                floor = 2,
                zoneRoute = FOUNDATION_ZONE_ROUTE,
                routeIndex = 1,
                bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
            )

        ValidationPreset.HIDDEN_CONTENT ->
            FoundationGameConfig(
                seed = 20260409L,
                zoneId = "underground_river",
                playerProfessionId = "arcanist",
                floor = 1,
                zoneRoute = FOUNDATION_ZONE_ROUTE,
                routeIndex = 4,
                bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
            )

        ValidationPreset.TERRAIN_INTERACTION ->
            FoundationGameConfig(
                seed = 20260410L,
                zoneId = "deep_iron_pit",
                playerProfessionId = "vanguard",
                floor = 2,
                zoneRoute = FOUNDATION_ZONE_ROUTE,
                routeIndex = 2,
                bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
            )

        ValidationPreset.ELITE_MUTATION ->
            FoundationGameConfig(
                seed = 20260411L,
                zoneId = "deep_iron_pit",
                playerProfessionId = "vanguard",
                floor = 1,
                zoneRoute = FOUNDATION_ZONE_ROUTE,
                routeIndex = 2,
                bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
            )

        ValidationPreset.BOSS_VARIANT ->
            FoundationGameConfig(
                seed = 20260412L,
                zoneId = "grey_gate_depths",
                playerProfessionId = "vanguard",
                floor = 1,
                zoneRoute = FOUNDATION_ZONE_ROUTE,
                routeIndex = 3,
                bossVariantSelectionMode = BossVariantSelectionMode.FORCE_AVAILABLE,
            )

        ValidationPreset.LOOT_LAB ->
            FoundationGameConfig(
                seed = 20260413L,
                zoneId = "greenwood_fringe",
                playerProfessionId = "rogue",
                floor = 1,
                zoneRoute = FOUNDATION_ZONE_ROUTE,
                routeIndex = 1,
                bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
            )

        ValidationPreset.CONTENT_PACK ->
            FoundationGameConfig(
                seed = 20260414L,
                zoneId = "underground_river",
                playerProfessionId = "arcanist",
                floor = 1,
                zoneRoute = FOUNDATION_ZONE_ROUTE,
                routeIndex = 4,
                bossVariantSelectionMode = BossVariantSelectionMode.DISABLED,
            )

        ValidationPreset.CUSTOM -> FoundationGameConfig()
    }
