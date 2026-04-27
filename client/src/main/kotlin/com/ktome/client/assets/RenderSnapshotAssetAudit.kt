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
            resolveAudio(cell.terrainAudioProfile)
            cell.items.forEach { item ->
                auditItem(item)
            }
        }

        snapshot.props.forEach { prop ->
            resolveVisual(prop.visualKey)
            resolveAudio(prop.audioProfile)
        }

        snapshot.actors.forEach { actor ->
            resolveVisual(actor.visualKey)
            resolveAudio(actor.audioProfile)
            actor.bossVariant?.visualTintKey?.let(::resolveVisual)
            actor.bossVariant?.audioProfile?.let(::resolveAudio)
            actor.mutations.forEach { mutation ->
                resolveVisual(mutation.iconKey)
                resolveAudio(mutation.audioProfile)
            }
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
        snapshot.forEachTalentAssetReference(
            visual = { key -> resolveVisual(key) },
            iconVisual = { key -> resolveVisual(key) },
            audio = { key -> resolveAudio(key) },
        )
        snapshot.uiState.inventory.forEach { item ->
            auditItem(item.item)
        }
    }

    private fun auditItem(item: ItemRenderSnapshot) {
        resolveVisual(item.visualKey, item.assetContext("visualKey"))
        resolveVisual(item.iconKey, item.assetContext("iconKey"))
        resolveAudio(item.audioProfile, item.assetContext("audioProfile"))
    }

    private fun ItemRenderSnapshot.assetContext(field: String): String =
        "baseItemId=$baseItemId specialTemplateId=${specialTemplateId ?: "-"} field=$field"

    private fun resolveVisual(
        key: String?,
        context: String? = null,
    ) {
        if (!key.isNullOrBlank()) {
            val resolved = assets.visualResolver.resolve(key)
            require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
                "Render snapshot references unknown visual key '$key'${context?.let { value -> " ($value)" }.orEmpty()}."
            }
        }
    }

    private fun resolveAudio(
        key: String?,
        context: String? = null,
    ) {
        if (!key.isNullOrBlank()) {
            val resolved = assets.audioResolver.resolve(key)
            require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
                "Render snapshot references unknown audio key '$key'${context?.let { value -> " ($value)" }.orEmpty()}."
            }
        }
    }
}
