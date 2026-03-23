package com.ktome.client.assets

import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.RenderSnapshot

class RenderSnapshotAssetAudit(
    private val assets: ClientAssetBundle,
) {
    fun audit(snapshot: RenderSnapshot) {
        resolveVisual(snapshot.metadata.zoneVisualKey)
        resolveAudio(snapshot.metadata.zoneAudioProfile)
        resolveAudio(snapshot.metadata.ambientProfile)

        snapshot.mapCells.forEach { cell ->
            resolveVisual(cell.terrainVisualKey)
            cell.items.forEach { item ->
                resolveVisual(item.visualKey)
                resolveVisual(item.iconKey)
                resolveAudio(item.audioProfile)
            }
        }

        snapshot.props.forEach { prop ->
            resolveVisual(prop.visualKey)
            resolveAudio(prop.audioProfile)
        }

        snapshot.actors.forEach { actor ->
            resolveVisual(actor.visualKey)
            resolveAudio(actor.audioProfile)
            actor.statusEffects.forEach { effect ->
                resolveVisual(effect.iconKey)
            }
        }

        snapshot.overlays.forEach { overlay ->
            resolveVisual(overlay.visualKey)
            resolveAudio(overlay.audioProfile)
        }

        snapshot.uiState.equipment.forEach { equipment ->
            equipment.item?.let(::auditItem)
        }
        snapshot.uiState.talents.forEach { talent ->
            resolveVisual(talent.visualKey)
            resolveVisual(talent.iconKey)
            resolveVisual(talent.damageTypeIconKey)
            resolveAudio(talent.audioProfile)
        }
        snapshot.uiState.reserveTalents.forEach { talent ->
            resolveVisual(talent.visualKey)
            resolveVisual(talent.iconKey)
            resolveVisual(talent.damageTypeIconKey)
            resolveAudio(talent.audioProfile)
        }
        snapshot.uiState.inventory.forEach { item ->
            auditItem(item.item)
        }
    }

    private fun auditItem(item: ItemRenderSnapshot) {
        resolveVisual(item.visualKey)
        resolveVisual(item.iconKey)
        resolveAudio(item.audioProfile)
    }

    private fun resolveVisual(key: String?) {
        if (!key.isNullOrBlank()) {
            val resolved = assets.visualResolver.resolve(key)
            require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
                "Render snapshot references unknown visual key '$key'."
            }
        }
    }

    private fun resolveAudio(key: String?) {
        if (!key.isNullOrBlank()) {
            val resolved = assets.audioResolver.resolve(key)
            require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
                "Render snapshot references unknown audio key '$key'."
            }
        }
    }
}
