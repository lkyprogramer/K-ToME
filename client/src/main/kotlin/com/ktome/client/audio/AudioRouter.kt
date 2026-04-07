package com.ktome.client.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable
import com.ktome.client.assets.AudioManifestResolver
import com.ktome.client.assets.ResolvedAudioCue
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.game.PlayerCommand

fun interface AudioCueSink {
    fun emit(cue: ResolvedAudioCue)
}

object NoOpAudioCueSink : AudioCueSink {
    override fun emit(cue: ResolvedAudioCue) = Unit
}

fun interface BackgroundAudioSink {
    fun transitionTo(cue: ResolvedAudioCue?)
}

data class AudioSinkBindings(
    val cueSink: AudioCueSink,
    val backgroundSink: BackgroundAudioSink,
    val dispose: () -> Unit = {},
)

fun interface AudioSinkBindingsFactory {
    fun create(renderEnabled: Boolean): AudioSinkBindings
}

object NoOpBackgroundAudioSink : BackgroundAudioSink {
    override fun transitionTo(cue: ResolvedAudioCue?) = Unit
}

object DefaultAudioSinkBindingsFactory : AudioSinkBindingsFactory {
    override fun create(renderEnabled: Boolean): AudioSinkBindings =
        if (renderEnabled) {
            AudioSinkBindings(
                cueSink = GdxAudioCueSink,
                backgroundSink = GdxBackgroundAudioSink,
                dispose = {
                    GdxBackgroundAudioSink.dispose()
                    GdxAudioCueSink.dispose()
                },
            )
        } else {
            AudioSinkBindings(
                cueSink = NoOpAudioCueSink,
                backgroundSink = NoOpBackgroundAudioSink,
            )
        }
}

object GdxAudioCueSink : AudioCueSink, Disposable {
    private const val defaultVolume = 0.35f
    private val sounds = linkedMapOf<String, Sound>()

    override fun emit(cue: ResolvedAudioCue) {
        val audio = Gdx.audio ?: return
        val sound =
            sounds.getOrPut(cue.entry.sourcePath) {
                audio.newSound(Gdx.files.classpath(cue.entry.sourcePath))
            }
        sound.play(defaultVolume)
    }

    override fun dispose() {
        sounds.values.forEach(Sound::dispose)
        sounds.clear()
    }
}

object GdxBackgroundAudioSink : BackgroundAudioSink, Disposable {
    private const val musicVolume = 0.18f
    private const val ambienceVolume = 0.14f

    private var currentPath: String? = null
    private var currentMusic: Music? = null

    override fun transitionTo(cue: ResolvedAudioCue?) {
        val targetPath = cue?.entry?.sourcePath
        if (targetPath == currentPath) {
            return
        }

        currentMusic?.stop()
        currentMusic?.dispose()
        currentMusic = null
        currentPath = null

        if (cue == null) {
            return
        }

        val audio = Gdx.audio ?: return
        val music = audio.newMusic(Gdx.files.classpath(targetPath))
        music.isLooping = true
        music.volume = if (cue.entry.cueFamily == "music") musicVolume else ambienceVolume
        music.play()
        currentMusic = music
        currentPath = targetPath
    }

    override fun dispose() {
        currentMusic?.stop()
        currentMusic?.dispose()
        currentMusic = null
        currentPath = null
    }
}

class AudioRouter(
    private val audioResolver: AudioManifestResolver,
    private val sink: AudioCueSink = NoOpAudioCueSink,
    private val backgroundSink: BackgroundAudioSink = NoOpBackgroundAudioSink,
) {
    fun onMenuShown() {
        transitionBackdrop("audio.music.menu")
    }

    fun onMenuInteraction(
        selectionChanged: Boolean = false,
        localeToggled: Boolean = false,
        accepted: Boolean = false,
        rejected: Boolean = false,
    ) {
        if (selectionChanged || localeToggled) {
            play("audio.ui.hover")
        }
        if (accepted) {
            play("audio.ui.confirm")
        }
        if (rejected) {
            play("audio.ui.cancel")
        }
    }

    fun onOverlayStateChanged(
        previous: OverlayState,
        current: OverlayState,
    ) {
        if (previous.mode != current.mode) {
            val cueKey =
                if (current.mode == UiMode.MAP) {
                    "audio.ui.cancel"
                } else {
                    "audio.ui.confirm"
                }
            play(cueKey)
            return
        }

        val selectionChanged =
            previous.inventorySelection != current.inventorySelection ||
                previous.shopOfferSelection != current.shopOfferSelection ||
                previous.routeSelection != current.routeSelection ||
                previous.shopFocus != current.shopFocus ||
                previous.loadoutSlotSelection != current.loadoutSlotSelection ||
                previous.loadoutReserveSelection != current.loadoutReserveSelection ||
                previous.targetingCursor != current.targetingCursor ||
                previous.inspectCursor != current.inspectCursor
        if (selectionChanged) {
            play("audio.ui.hover")
        }
    }

    fun onReturnToMenu() {
        play("audio.ui.cancel")
    }

    fun onSnapshotUpdated(
        previous: RenderSnapshot?,
        current: RenderSnapshot,
    ) {
        transitionBackdrop(
            current.metadata.zoneAudioProfile
                .takeUnless(String::isBlank)
                ?: current.metadata.ambientProfile.takeUnless(String::isBlank),
        )
        val previousOverlayIds = previous?.overlays?.mapTo(hashSetOf(), OverlayRenderSnapshot::id).orEmpty()
        val overlayCue =
            current.overlays
            .filter { overlay -> overlay.id !in previousOverlayIds }
            .sortedWith(compareByDescending<OverlayRenderSnapshot> { it.dangerLevel }.thenBy(OverlayRenderSnapshot::id))
            .firstOrNull { overlay -> !overlay.audioProfile.isNullOrBlank() }
            ?.audioProfile
        if (overlayCue != null) {
            play(overlayCue)
            return
        }
        if (previous == null) {
            return
        }

        snapshotTransitionCueKeys(previous, current).forEach(::play)
    }

    fun onCommandResolved(
        previousSnapshot: RenderSnapshot,
        currentSnapshot: RenderSnapshot,
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        if (!consumed) {
            when (command) {
                is PlayerCommand.ActivateInventoryItem,
                is PlayerCommand.EquipTalentToSlot,
                PlayerCommand.Interact,
                PlayerCommand.Search,
                is PlayerCommand.UseTalent,
                PlayerCommand.Ascend,
                PlayerCommand.Descend,
                PlayerCommand.PickUp,
                -> play("audio.ui.cancel")

                else -> Unit
            }
            return
        }

        when (command) {
            is PlayerCommand.Move -> play(moveCueKey(previousSnapshot, currentSnapshot))
            is PlayerCommand.UseTalent -> talentCueKeys(previousSnapshot, currentSnapshot, command.slot).forEach(::play)
            is PlayerCommand.UseInscription -> play("audio.ui.confirm")
            is PlayerCommand.ActivateInventoryItem,
            is PlayerCommand.DropInventoryItem,
            PlayerCommand.Interact,
            PlayerCommand.Search,
            PlayerCommand.PickUp,
            -> play("audio.interactable.open")

            PlayerCommand.Ascend,
            PlayerCommand.Descend,
            -> play("audio.interactable.stairs")

            is PlayerCommand.BuyShopOffer,
            is PlayerCommand.SellInventoryItem,
            is PlayerCommand.SelectRoute,
            is PlayerCommand.EquipTalentToSlot,
            is PlayerCommand.AssignStat,
            is PlayerCommand.AssignTalent,
            is PlayerCommand.RespecTalentTree,
            PlayerCommand.ConfirmTalentDraft,
            PlayerCommand.RollbackTalentDraft,
            PlayerCommand.SaveGame,
            -> play("audio.ui.confirm")

            PlayerCommand.CloseShop -> play("audio.ui.cancel")

            PlayerCommand.Wait -> Unit
        }
    }

    private fun talentCueKeys(
        previousSnapshot: RenderSnapshot,
        currentSnapshot: RenderSnapshot,
        slot: Int,
    ): List<String> {
        val talent = currentSnapshot.uiState.talents.firstOrNull { candidate -> candidate.slot == slot }
        val addedLogKeys = newlyAddedLogKeys(previousSnapshot, currentSnapshot)
        return buildList {
            add(talentCueKey(currentSnapshot, slot))
            resourceSpendCueKey(previousSnapshot, currentSnapshot, talent)?.let(::add)
            damageCueKey(talent, addedLogKeys)?.let(::add)
        }
    }

    private fun talentCueKey(
        snapshot: RenderSnapshot,
        slot: Int,
    ): String {
        val talent = snapshot.uiState.talents.firstOrNull { candidate -> candidate.slot == slot }
        val fallback =
            when {
                talent == null -> "audio.spell.basic"
                talent.range <= 1 && !talent.requiresTarget -> "audio.melee.light"
                else -> "audio.spell.basic"
        }
        return talent?.audioProfile ?: fallback
    }

    private fun damageCueKey(
        talent: com.ktome.core.snapshot.TalentSlotSnapshot?,
        addedLogKeys: List<String>,
    ): String? {
        if (addedLogKeys.any(::isTalentMissLogKey)) {
            return null
        }
        if (addedLogKeys.none(::isTalentDamageLogKey)) {
            return null
        }
        return when (talent?.damageTypeIconKey) {
            "icon.damage_type.physical" -> "audio.damage.physical_hit"
            "icon.damage_type.fire" -> "audio.damage.fire_hit"
            "icon.damage_type.cold" -> "audio.damage.cold_hit"
            "icon.damage_type.lightning" -> "audio.damage.lightning_hit"
            "icon.damage_type.holy" -> "audio.damage.holy_hit"
            "icon.damage_type.shadow" -> "audio.damage.shadow_hit"
            else -> null
        }
    }

    private fun resourceSpendCueKey(
        previous: RenderSnapshot,
        current: RenderSnapshot,
        talent: com.ktome.core.snapshot.TalentSlotSnapshot?,
    ): String? =
        if (current.uiState.playerStatus.currentResource < previous.uiState.playerStatus.currentResource) {
            resourceCueKey(talent?.resourceTypeId ?: current.uiState.playerStatus.resourceTypeId, restoring = false)
        } else {
            null
        }

    private fun snapshotTransitionCueKeys(
        previous: RenderSnapshot,
        current: RenderSnapshot,
    ): List<String> {
        val cues = linkedSetOf<String>()
        newlyAddedMutationAudioProfile(previous, current)?.let(cues::add)
        newlyAddedBossVariantAudioProfile(previous, current)?.let(cues::add)
        newlyChangedTerrainAudioProfile(previous, current)?.let(cues::add)
        newlyAddedLogKeys(previous, current)
            .mapNotNull(::logCueKey)
            .forEach(cues::add)
        resourceRestoreCueKey(previous, current)?.let(cues::add)
        newlyAddedInventoryItemAudioProfile(previous, current)?.let(cues::add)
        newlyVisibleGroundItemAudioProfile(previous, current)?.let(cues::add)
        return cues.toList()
    }

    private fun newlyAddedLogKeys(
        previous: RenderSnapshot,
        current: RenderSnapshot,
    ): List<String> =
        firstAddedValues(
            previous = previous.logEvents.map { event -> event.message.key },
            current = current.logEvents.map { event -> event.message.key },
        )

    private fun logCueKey(messageKey: String): String? =
        when (messageKey) {
            "log.level_up" -> "audio.ui.level_up"
            "log.talent.unlock" -> "audio.ui.talent_unlock"
            "log.objective.progress", "log.objective.advance" -> "audio.objective.progress"
            "log.route.advance" -> "audio.route.transition"
            "log.victory", "log.victory.escape" -> "audio.route.complete"
            else -> null
        }

    private fun isTalentDamageLogKey(messageKey: String): Boolean =
        when (messageKey) {
            "log.talent.damage",
            "log.talent.damage_crit",
            -> true

            else -> false
        }

    private fun isTalentMissLogKey(messageKey: String): Boolean =
        when (messageKey) {
            "log.talent.miss" -> true

            else -> false
        }

    private fun resourceRestoreCueKey(
        previous: RenderSnapshot,
        current: RenderSnapshot,
    ): String? =
        if (current.uiState.playerStatus.currentResource > previous.uiState.playerStatus.currentResource) {
            resourceCueKey(current.uiState.playerStatus.resourceTypeId, restoring = true)
        } else {
            null
        }

    private fun resourceCueKey(
        resourceTypeId: String,
        restoring: Boolean,
    ): String? =
        when (resourceTypeId.uppercase()) {
            "MANA" -> if (restoring) "audio.resource.mana.restore" else "audio.resource.mana.spend"
            "STAMINA" -> if (restoring) "audio.resource.stamina.restore" else "audio.resource.stamina.spend"
            "ENERGY" -> if (restoring) "audio.resource.energy.restore" else "audio.resource.energy.spend"
            "POSITIVE_ENERGY" -> if (restoring) "audio.resource.positive_energy.restore" else "audio.resource.positive_energy.spend"
            else -> null
        }

    private fun moveCueKey(
        previousSnapshot: RenderSnapshot,
        currentSnapshot: RenderSnapshot,
    ): String =
        if (
            previousSnapshot.metadata.playerX != currentSnapshot.metadata.playerX ||
            previousSnapshot.metadata.playerY != currentSnapshot.metadata.playerY
        ) {
            currentSnapshot.playerTerrainAudioProfile() ?: "audio.footstep.default"
        } else {
            "audio.melee.light"
        }

    private fun newlyAddedMutationAudioProfile(
        previous: RenderSnapshot,
        current: RenderSnapshot,
    ): String? = firstAddedAudioProfile(previous.actorMutationAudioProfiles(), current.actorMutationAudioProfiles())

    private fun newlyAddedBossVariantAudioProfile(
        previous: RenderSnapshot,
        current: RenderSnapshot,
    ): String? = firstAddedAudioProfile(previous.actorBossVariantAudioProfiles(), current.actorBossVariantAudioProfiles())

    private fun newlyChangedTerrainAudioProfile(
        previous: RenderSnapshot,
        current: RenderSnapshot,
    ): String? {
        if (previous.metadata.zoneId != current.metadata.zoneId) {
            return null
        }
        if (previous.metadata.width != current.metadata.width || previous.metadata.height != current.metadata.height) {
            return null
        }
        current.mapCells.forEach { currentCell ->
            val previousCell = previous.mapCellAt(currentCell.x, currentCell.y) ?: return@forEach
            if (previousCell.visibility == com.ktome.core.snapshot.CellVisibilitySnapshot.HIDDEN) {
                return@forEach
            }
            if (previousCell.terrainAudioProfile != currentCell.terrainAudioProfile) {
                return currentCell.terrainAudioProfile
            }
        }
        return null
    }

    private fun play(key: String) {
        sink.emit(resolveExact(key))
    }

    private fun newlyAddedInventoryItemAudioProfile(
        previous: RenderSnapshot?,
        current: RenderSnapshot,
    ): String? =
        firstAddedAudioProfile(
            previous = previous?.uiState?.inventory.orEmpty().mapNotNull { entry -> entry.item.audioProfile },
            current = current.uiState.inventory.mapNotNull { entry -> entry.item.audioProfile },
        )

    private fun newlyVisibleGroundItemAudioProfile(
        previous: RenderSnapshot?,
        current: RenderSnapshot,
    ): String? =
        firstAddedAudioProfile(
            previous = previous.visibleGroundItemAudioProfiles(),
            current = current.visibleGroundItemAudioProfiles(),
        )

    private fun RenderSnapshot?.visibleGroundItemAudioProfiles(): List<String> =
        this?.mapCells
            .orEmpty()
            .flatMap { cell -> cell.items }
            .mapNotNull { item -> item.audioProfile }

    private fun RenderSnapshot.actorMutationAudioProfiles(): List<String> =
        actors
            .flatMap { actor -> actor.mutations }
            .mapNotNull { mutation -> mutation.audioProfile }

    private fun RenderSnapshot.actorBossVariantAudioProfiles(): List<String> =
        actors.mapNotNull { actor -> actor.bossVariant?.audioProfile }

    private fun RenderSnapshot.playerTerrainAudioProfile(): String? =
        mapCellAt(metadata.playerX, metadata.playerY)?.terrainAudioProfile

    private fun RenderSnapshot.mapCellAt(
        x: Int,
        y: Int,
    ) = if (x !in 0 until metadata.width || y !in 0 until metadata.height) {
        null
    } else {
        mapCells.getOrNull(y * metadata.width + x)?.takeIf { cell -> cell.x == x && cell.y == y }
            ?: mapCells.firstOrNull { cell -> cell.x == x && cell.y == y }
    }

    private fun firstAddedAudioProfile(
        previous: List<String>,
        current: List<String>,
    ): String? {
        return firstAddedValues(previous, current).firstOrNull()
    }

    private fun firstAddedValues(
        previous: List<String>,
        current: List<String>,
    ): List<String> {
        if (current.isEmpty()) {
            return emptyList()
        }
        val remaining = previous.groupingBy { it }.eachCount().toMutableMap()
        val added = mutableListOf<String>()
        current.forEach { audioProfile ->
            val previousCount = remaining[audioProfile] ?: 0
            if (previousCount == 0) {
                added += audioProfile
            } else {
                remaining[audioProfile] = previousCount - 1
            }
        }
        return added
    }

    private fun transitionBackdrop(key: String?) {
        backgroundSink.transitionTo(key?.let(::resolveExact))
    }

    private fun resolveExact(key: String): ResolvedAudioCue {
        val resolved = audioResolver.resolve(key)
        require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
            "Audio router requires exact cue key '$key'."
        }
        return resolved
    }
}
