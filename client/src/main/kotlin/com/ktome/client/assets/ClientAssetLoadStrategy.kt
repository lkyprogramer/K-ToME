package com.ktome.client.assets

import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.RenderSnapshot

internal data class AssetLoadStateSnapshot(
    val bootstrapLoaded: Boolean,
    val bootstrapDescriptor: BootstrapLoadDescriptor?,
    val sessionZoneId: String?,
    val sessionDescriptor: SessionLoadDescriptor?,
    val sessionVisualKeys: Set<String>,
    val sessionAudioKeys: Set<String>,
    val warmCacheDescriptor: WarmCacheDescriptor?,
    val warmVisualKeys: Set<String>,
    val warmAudioKeys: Set<String>,
)

internal data class BootstrapLoadDescriptor(
    val localeBundleIds: Set<String>,
    val fontResources: Set<String>,
    val menuVisualKeys: Set<String>,
    val menuAudioKeys: Set<String>,
)

internal data class SessionLoadDescriptor(
    val zoneId: String,
    val tilesetKey: String,
    val zoneVisualKey: String,
    val terrainVisualKeys: Set<String>,
    val actorVisualKeys: Set<String>,
    val iconVisualKeys: Set<String>,
    val statusIconKeys: Set<String>,
    val ambientAudioKeys: Set<String>,
    val interactionAudioKeys: Set<String>,
    val sessionAudioKeys: Set<String>,
)

internal data class WarmCacheDescriptor(
    val overlayVisualKeys: Set<String>,
    val overlayAudioKeys: Set<String>,
    val portraitVisualKeys: Set<String>,
    val highValueVfxKeys: Set<String>,
)

class ClientAssetLoadStrategy(
    private val assets: ClientAssetBundle,
) {
    private var bootstrapLoaded: Boolean = false
    private var bootstrapDescriptor: BootstrapLoadDescriptor? = null
    private var sessionZoneId: String? = null
    private var sessionDescriptor: SessionLoadDescriptor? = null
    private var sessionVisualKeys: Set<String> = emptySet()
    private var sessionAudioKeys: Set<String> = emptySet()
    private var warmCacheDescriptor: WarmCacheDescriptor? = null
    private var warmVisualKeys: Set<String> = emptySet()
    private var warmAudioKeys: Set<String> = emptySet()

    fun bootstrapLoad() {
        val descriptor =
            BootstrapLoadDescriptor(
                localeBundleIds = setOf("en-US", "zh-CN"),
                fontResources = setOf(ClientFontCatalog.UI_FONT_RESOURCE_ID),
                menuVisualKeys = emptySet(),
                menuAudioKeys = setOf("audio.music.menu", "audio.ui.confirm", "audio.ui.cancel", "audio.ui.hover"),
            )
        descriptor.menuVisualKeys.forEach(::resolveVisualExact)
        descriptor.menuAudioKeys.forEach(::resolveAudioExact)
        bootstrapLoaded = true
        bootstrapDescriptor = descriptor
    }

    fun sessionLoad(snapshot: RenderSnapshot) {
        bootstrapLoad()
        val visualKeys = linkedSetOf<String>()
        val audioKeys = linkedSetOf<String>()
        val terrainVisualKeys = linkedSetOf<String>()
        val actorVisualKeys = linkedSetOf<String>()
        val iconVisualKeys = linkedSetOf<String>()
        val statusIconKeys = linkedSetOf<String>()
        val ambientAudioKeys = linkedSetOf<String>()
        val interactionAudioKeys =
            linkedSetOf(
                "audio.ui.confirm",
                "audio.ui.cancel",
                "audio.ui.hover",
                "audio.footstep.default",
                "audio.interactable.open",
                "audio.interactable.stairs",
                "audio.melee.light",
                "audio.spell.basic",
            )

        visualKeys += snapshot.metadata.zoneVisualKey
        audioKeys += snapshot.metadata.zoneAudioProfile
        audioKeys += snapshot.metadata.ambientProfile
        ambientAudioKeys += snapshot.metadata.zoneAudioProfile
        ambientAudioKeys += snapshot.metadata.ambientProfile
        audioKeys += interactionAudioKeys

        snapshot.mapCells.forEach { cell ->
            visualKeys += cell.terrainVisualKey
            terrainVisualKeys += cell.terrainVisualKey
            cell.items.forEach { item -> collectItem(item, visualKeys, audioKeys) }
        }
        snapshot.props.forEach { prop ->
            visualKeys += prop.visualKey
            prop.audioProfile?.let(audioKeys::add)
        }
        snapshot.actors.forEach { actor ->
            visualKeys += actor.visualKey
            actorVisualKeys += actor.visualKey
            actor.audioProfile?.let(audioKeys::add)
            actor.statusEffects.forEach { effect ->
                effect.iconKey?.let {
                    visualKeys += it
                    statusIconKeys += it
                }
            }
        }
        snapshot.uiState.equipment.forEach { equipment ->
            equipment.item?.let { item ->
                collectItem(item, visualKeys, audioKeys)
                item.iconKey?.let(iconVisualKeys::add)
            }
        }
        snapshot.uiState.talents.forEach { talent ->
            talent.visualKey?.let(visualKeys::add)
            talent.iconKey?.let {
                visualKeys += it
                iconVisualKeys += it
            }
            talent.damageTypeIconKey?.let {
                visualKeys += it
                iconVisualKeys += it
            }
            talent.audioProfile?.let(audioKeys::add)
        }
        snapshot.uiState.inventory.forEach { entry ->
            collectItem(entry.item, visualKeys, audioKeys)
            entry.item.iconKey?.let(iconVisualKeys::add)
        }

        if (snapshot.metadata.zoneId == sessionZoneId &&
            visualKeys == sessionVisualKeys &&
            audioKeys == sessionAudioKeys
        ) {
            return
        }

        visualKeys.forEach(::resolveVisualExact)
        audioKeys.forEach(::resolveAudioExact)
        sessionZoneId = snapshot.metadata.zoneId
        sessionDescriptor =
            SessionLoadDescriptor(
                zoneId = snapshot.metadata.zoneId,
                tilesetKey = snapshot.metadata.tilesetKey,
                zoneVisualKey = snapshot.metadata.zoneVisualKey,
                terrainVisualKeys = terrainVisualKeys,
                actorVisualKeys = actorVisualKeys,
                iconVisualKeys = iconVisualKeys,
                statusIconKeys = statusIconKeys,
                ambientAudioKeys = ambientAudioKeys,
                interactionAudioKeys = interactionAudioKeys,
                sessionAudioKeys = audioKeys,
            )
        sessionVisualKeys = visualKeys
        sessionAudioKeys = audioKeys
        warmCacheDescriptor = null
        warmVisualKeys = emptySet()
        warmAudioKeys = emptySet()
    }

    fun warmCache(snapshot: RenderSnapshot) {
        bootstrapLoad()
        val visualKeys = snapshot.overlays.mapTo(linkedSetOf()) { overlay -> overlay.visualKey }
        val audioKeys =
            snapshot.overlays
                .mapNotNullTo(linkedSetOf()) { overlay -> overlay.audioProfile }

        if (visualKeys == warmVisualKeys && audioKeys == warmAudioKeys) {
            return
        }

        visualKeys.forEach(::resolveVisualExact)
        audioKeys.forEach(::resolveAudioExact)
        warmCacheDescriptor =
            WarmCacheDescriptor(
                overlayVisualKeys = visualKeys,
                overlayAudioKeys = audioKeys,
                portraitVisualKeys = emptySet(),
                highValueVfxKeys = visualKeys.filterTo(linkedSetOf()) { key -> key.startsWith("vfx.") },
            )
        warmVisualKeys = visualKeys
        warmAudioKeys = audioKeys
    }

    internal fun stateSnapshot(): AssetLoadStateSnapshot =
        AssetLoadStateSnapshot(
            bootstrapLoaded = bootstrapLoaded,
            bootstrapDescriptor = bootstrapDescriptor,
            sessionZoneId = sessionZoneId,
            sessionDescriptor = sessionDescriptor,
            sessionVisualKeys = sessionVisualKeys,
            sessionAudioKeys = sessionAudioKeys,
            warmCacheDescriptor = warmCacheDescriptor,
            warmVisualKeys = warmVisualKeys,
            warmAudioKeys = warmAudioKeys,
        )

    private fun collectItem(
        item: ItemRenderSnapshot,
        visualKeys: MutableSet<String>,
        audioKeys: MutableSet<String>,
    ) {
        item.visualKey?.let(visualKeys::add)
        item.iconKey?.let(visualKeys::add)
        item.audioProfile?.let(audioKeys::add)
    }

    private fun resolveVisualExact(key: String) {
        val resolved = assets.visualResolver.resolve(key)
        require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
            "Session asset load requires exact visual key '$key'."
        }
        assets.textureRepository.preload(resolved)
    }

    private fun resolveAudioExact(key: String) {
        val resolved = assets.audioResolver.resolve(key)
        require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
            "Session asset load requires exact audio key '$key'."
        }
    }
}
