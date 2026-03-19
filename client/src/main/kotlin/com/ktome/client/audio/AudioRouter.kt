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
            current.metadata.ambientProfile
                .takeUnless(String::isBlank)
                ?: current.metadata.zoneAudioProfile.takeUnless(String::isBlank),
        )
        val previousOverlayIds = previous?.overlays?.mapTo(hashSetOf(), OverlayRenderSnapshot::id).orEmpty()
        current.overlays
            .filter { overlay -> overlay.id !in previousOverlayIds }
            .sortedWith(compareByDescending<OverlayRenderSnapshot> { it.dangerLevel }.thenBy(OverlayRenderSnapshot::id))
            .firstOrNull { overlay -> !overlay.audioProfile.isNullOrBlank() }
            ?.audioProfile
            ?.let(::play)
    }

    fun onCommandResolved(
        snapshot: RenderSnapshot,
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        if (!consumed) {
            when (command) {
                is PlayerCommand.ActivateInventoryItem,
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
            is PlayerCommand.Move -> play("audio.footstep.default")
            is PlayerCommand.UseTalent -> play(talentCueKey(snapshot, command.slot))
            is PlayerCommand.ActivateInventoryItem,
            PlayerCommand.PickUp,
            PlayerCommand.Ascend,
            PlayerCommand.Descend,
            -> play("audio.interactable.open")

            is PlayerCommand.AssignStat,
            is PlayerCommand.AssignTalent,
            PlayerCommand.SaveGame,
            -> play("audio.ui.confirm")

            PlayerCommand.Wait -> Unit
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

    private fun play(key: String) {
        sink.emit(resolveExact(key))
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
