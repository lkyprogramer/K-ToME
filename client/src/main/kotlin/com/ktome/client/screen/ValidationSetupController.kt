package com.ktome.client.screen

import com.badlogic.gdx.Input.Keys
import com.ktome.client.input.GdxInputSource
import com.ktome.client.input.InputSource
import com.ktome.core.profile.ClassPlayabilityState
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.PlayerCreationSelection
import com.ktome.game.PlayerCreationState
import com.ktome.game.contentpack.ContentPackSelection
import com.ktome.game.elites.BossVariantSelectionMode
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationSessionOptions
import com.ktome.game.validation.validationSessionOptionsForPreset

internal data class ValidationZoneOption(
    val id: String,
    val displayNameKey: String,
    val floorCount: Int,
)

internal enum class ValidationRouteMode {
    CURRENT_ZONE_ONLY,
    FOUNDATION_ROUTE,
}

internal enum class ValidationSetupEntryId {
    PRESET,
    PROFESSION,
    RACE,
    SEED,
    ZONE,
    FLOOR,
    ROUTE,
    ROUTE_INDEX,
    BOSS_VARIANT_MODE,
    PREFERRED_VARIANT,
    SAMPLE_CONTENT_PACK,
    START,
    CONTINUE,
    BACK,
}

internal sealed interface ValidationSetupAction {
    data class StartSession(
        val options: ValidationSessionOptions,
    ) : ValidationSetupAction

    data class ContinueSession(
        val options: ValidationSessionOptions,
    ) : ValidationSetupAction

    data object Back : ValidationSetupAction
}

internal data class ValidationSetupPollResult(
    val action: ValidationSetupAction? = null,
    val options: ValidationSessionOptions,
    val selectionChanged: Boolean = false,
    val rejected: Boolean = false,
)

internal data class ValidationSetupContext(
    val initialOptions: ValidationSessionOptions,
    val playerCreationState: PlayerCreationState,
    val zones: List<ValidationZoneOption>,
    val bossVariantIds: List<String>,
    val samplePackSelection: ContentPackSelection,
    val continueEnabled: Boolean,
    val notice: String? = null,
)

internal class ValidationSetupController(
    private val input: InputSource = GdxInputSource,
    private val context: ValidationSetupContext,
) {
    private val presets: List<ValidationPreset> = ValidationPreset.entries
    private val professionOptions =
        context.playerCreationState.professionOptions
            .filter { option -> option.playabilityState == ClassPlayabilityState.PLAYABLE }
            .ifEmpty { context.playerCreationState.professionOptions }
    private val raceOptions =
        context.playerCreationState.raceOptions
            .filter { option -> option.playabilityState == ClassPlayabilityState.PLAYABLE }
            .ifEmpty { context.playerCreationState.raceOptions }
    private val zoneOptions = context.zones
    private val preferredVariantOptions = listOf<String?>(null) + context.bossVariantIds
    private val entries: List<ValidationSetupEntryId> = ValidationSetupEntryId.entries
    private var selectedIndex: Int = 0
    private var presetIndex: Int = presets.indexOf(context.initialOptions.preset).takeIf { it >= 0 } ?: 0
    private var professionIndex: Int =
        professionOptions.indexOfFirst { option -> option.id == context.initialOptions.foundationConfig.playerProfessionId }.takeIf { it >= 0 } ?: 0
    private var raceIndex: Int =
        raceOptions.indexOfFirst { option -> option.id == context.initialOptions.foundationConfig.playerRaceId }.takeIf { it >= 0 } ?: 0
    private var zoneIndex: Int =
        zoneOptions.indexOfFirst { option -> option.id == context.initialOptions.foundationConfig.zoneId }.takeIf { it >= 0 } ?: 0
    private var routeMode: ValidationRouteMode = inferRouteMode(context.initialOptions)
    private var seed: Long = context.initialOptions.foundationConfig.seed
    private var floor: Int = context.initialOptions.foundationConfig.floor
    private var routeIndex: Int = context.initialOptions.foundationConfig.routeIndex
    private var bossVariantMode: BossVariantSelectionMode = context.initialOptions.foundationConfig.bossVariantSelectionMode
    private var preferredVariantIndex: Int =
        preferredVariantOptions.indexOf(context.initialOptions.foundationConfig.preferredBossVariantId).takeIf { it >= 0 } ?: 0
    private var samplePackEnabled: Boolean = context.initialOptions.contentPackSelection.activePackRoots.isNotEmpty()

    init {
        clampFloor()
        clampRouteIndex()
    }

    fun selectedEntry(): ValidationSetupEntryId = entries[selectedIndex]

    fun currentOptions(): ValidationSessionOptions {
        val preset = presets[presetIndex]
        val baseOptions =
            validationSessionOptionsForPreset(
                preset = preset,
                contentPackSelection = currentContentPackSelection(),
            )
        return baseOptions.copy(
            foundationConfig =
                baseOptions.foundationConfig.copy(
                    seed = seed,
                    zoneId = currentZone().id,
                    playerProfessionId = professionOptions[professionIndex].id,
                    playerRaceId = raceOptions[raceIndex].id,
                    floor = floor,
                    zoneRoute = currentZoneRoute(),
                    routeIndex = routeIndex,
                    bossVariantSelectionMode = bossVariantMode,
                    preferredBossVariantId = preferredVariantOptions[preferredVariantIndex],
                ),
        )
    }

    fun pollAction(): ValidationSetupPollResult {
        var selectionChanged = false
        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            selectedIndex = (selectedIndex - 1).floorMod(entries.size)
            selectionChanged = true
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
            selectedIndex = (selectedIndex + 1).floorMod(entries.size)
            selectionChanged = true
        }
        if (input.isKeyJustPressed(Keys.LEFT) || input.isKeyJustPressed(Keys.A)) {
            selectionChanged = adjustCurrentEntry(-1) || selectionChanged
        }
        if (input.isKeyJustPressed(Keys.RIGHT) || input.isKeyJustPressed(Keys.D)) {
            selectionChanged = adjustCurrentEntry(1) || selectionChanged
        }
        val options = currentOptions()
        if (input.isKeyJustPressed(Keys.ESCAPE)) {
            return ValidationSetupPollResult(
                action = ValidationSetupAction.Back,
                options = options,
                selectionChanged = selectionChanged,
            )
        }
        if (input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)) {
            return when (selectedEntry()) {
                ValidationSetupEntryId.START ->
                    ValidationSetupPollResult(
                        action = ValidationSetupAction.StartSession(options),
                        options = options,
                        selectionChanged = selectionChanged,
                    )

                ValidationSetupEntryId.CONTINUE ->
                    if (context.continueEnabled) {
                        ValidationSetupPollResult(
                            action = ValidationSetupAction.ContinueSession(options),
                            options = options,
                            selectionChanged = selectionChanged,
                        )
                    } else {
                        ValidationSetupPollResult(
                            options = options,
                            selectionChanged = selectionChanged,
                            rejected = true,
                        )
                    }

                ValidationSetupEntryId.BACK ->
                    ValidationSetupPollResult(
                        action = ValidationSetupAction.Back,
                        options = options,
                        selectionChanged = selectionChanged,
                    )

                else ->
                    ValidationSetupPollResult(
                        options = options,
                        selectionChanged = selectionChanged,
                    )
            }
        }
        return ValidationSetupPollResult(
            options = options,
            selectionChanged = selectionChanged,
        )
    }

    private fun adjustCurrentEntry(direction: Int): Boolean =
        when (selectedEntry()) {
            ValidationSetupEntryId.PRESET -> {
                val nextPresetIndex = (presetIndex + direction).floorMod(presets.size)
                val nextPreset = presets[nextPresetIndex]
                presetIndex = nextPresetIndex
                val defaultOptions =
                    validationSessionOptionsForPreset(
                        preset = nextPreset,
                        contentPackSelection = defaultContentPackSelection(nextPreset),
                    )
                val previousSelection = PlayerCreationSelection(professionOptions[professionIndex].id, raceOptions[raceIndex].id)
                seed = defaultOptions.foundationConfig.seed
                zoneIndex = zoneOptions.indexOfFirst { option -> option.id == defaultOptions.foundationConfig.zoneId }.takeIf { it >= 0 } ?: zoneIndex
                routeMode = inferRouteMode(defaultOptions)
                floor = defaultOptions.foundationConfig.floor
                routeIndex = defaultOptions.foundationConfig.routeIndex
                bossVariantMode = defaultOptions.foundationConfig.bossVariantSelectionMode
                preferredVariantIndex = preferredVariantOptions.indexOf(defaultOptions.foundationConfig.preferredBossVariantId).takeIf { it >= 0 } ?: 0
                samplePackEnabled = defaultOptions.contentPackSelection.activePackRoots.isNotEmpty()
                professionIndex = professionOptions.indexOfFirst { option -> option.id == previousSelection.professionId }.takeIf { it >= 0 } ?: professionIndex
                raceIndex = raceOptions.indexOfFirst { option -> option.id == previousSelection.raceId }.takeIf { it >= 0 } ?: raceIndex
                clampFloor()
                clampRouteIndex()
                true
            }

            ValidationSetupEntryId.PROFESSION -> {
                professionIndex = (professionIndex + direction).floorMod(professionOptions.size)
                true
            }

            ValidationSetupEntryId.RACE -> {
                raceIndex = (raceIndex + direction).floorMod(raceOptions.size)
                true
            }

            ValidationSetupEntryId.SEED -> {
                seed += direction.toLong()
                true
            }

            ValidationSetupEntryId.ZONE -> {
                zoneIndex = (zoneIndex + direction).floorMod(zoneOptions.size)
                if (routeMode == ValidationRouteMode.FOUNDATION_ROUTE && currentZone().id !in FOUNDATION_ZONE_ROUTE) {
                    routeMode = ValidationRouteMode.CURRENT_ZONE_ONLY
                }
                clampFloor()
                clampRouteIndex()
                true
            }

            ValidationSetupEntryId.FLOOR -> {
                floor = (floor + direction).coerceIn(1, currentZone().floorCount)
                true
            }

            ValidationSetupEntryId.ROUTE -> {
                routeMode =
                    when (routeMode) {
                        ValidationRouteMode.CURRENT_ZONE_ONLY -> ValidationRouteMode.FOUNDATION_ROUTE
                        ValidationRouteMode.FOUNDATION_ROUTE -> ValidationRouteMode.CURRENT_ZONE_ONLY
                    }
                if (routeMode == ValidationRouteMode.FOUNDATION_ROUTE && currentZone().id !in FOUNDATION_ZONE_ROUTE) {
                    routeMode = ValidationRouteMode.CURRENT_ZONE_ONLY
                }
                clampRouteIndex()
                true
            }

            ValidationSetupEntryId.ROUTE_INDEX -> {
                routeIndex = (routeIndex + direction).floorMod(currentZoneRoute().size)
                true
            }

            ValidationSetupEntryId.BOSS_VARIANT_MODE -> {
                val modes = BossVariantSelectionMode.entries
                val currentIndex = modes.indexOf(bossVariantMode).takeIf { it >= 0 } ?: 0
                bossVariantMode = modes[(currentIndex + direction).floorMod(modes.size)]
                if (bossVariantMode == BossVariantSelectionMode.DISABLED) {
                    preferredVariantIndex = 0
                }
                true
            }

            ValidationSetupEntryId.PREFERRED_VARIANT -> {
                preferredVariantIndex = (preferredVariantIndex + direction).floorMod(preferredVariantOptions.size)
                true
            }

            ValidationSetupEntryId.SAMPLE_CONTENT_PACK -> {
                samplePackEnabled = !samplePackEnabled
                true
            }

            ValidationSetupEntryId.START,
            ValidationSetupEntryId.CONTINUE,
            ValidationSetupEntryId.BACK -> false
        }

    private fun inferRouteMode(options: ValidationSessionOptions): ValidationRouteMode =
        if (options.foundationConfig.zoneRoute == FOUNDATION_ZONE_ROUTE) {
            ValidationRouteMode.FOUNDATION_ROUTE
        } else {
            ValidationRouteMode.CURRENT_ZONE_ONLY
        }

    private fun currentZone(): ValidationZoneOption = zoneOptions[zoneIndex]

    private fun currentZoneRoute(): List<String> =
        when (routeMode) {
            ValidationRouteMode.CURRENT_ZONE_ONLY -> listOf(currentZone().id)
            ValidationRouteMode.FOUNDATION_ROUTE ->
                if (currentZone().id in FOUNDATION_ZONE_ROUTE) {
                    FOUNDATION_ZONE_ROUTE
                } else {
                    listOf(currentZone().id)
                }
        }

    private fun currentContentPackSelection(): ContentPackSelection =
        if (samplePackEnabled) {
            context.samplePackSelection
        } else {
            ContentPackSelection.EMPTY
        }

    private fun defaultContentPackSelection(preset: ValidationPreset): ContentPackSelection =
        if (preset == ValidationPreset.CONTENT_PACK) {
            context.samplePackSelection
        } else {
            ContentPackSelection.EMPTY
        }

    private fun clampFloor() {
        floor = floor.coerceIn(1, currentZone().floorCount)
    }

    private fun clampRouteIndex() {
        routeIndex = routeIndex.coerceIn(0, currentZoneRoute().lastIndex.coerceAtLeast(0))
    }
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
