package com.ktome.client.ui.status

import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot

internal data class StatusHudIconModel(
    val asset: ResolvedVisualAsset,
    val badgeText: String,
    val category: StatusEffectCategorySnapshot,
)

internal object StatusIconResolver {
    fun resolveIcons(
        visualResolver: VisualManifestResolver,
        effects: List<StatusEffectRenderSnapshot>,
    ): List<StatusHudIconModel> =
        effects
            .sortedBy { effect -> if (effect.category == StatusEffectCategorySnapshot.BUFF) 0 else 1 }
            .mapNotNull { effect ->
                effect.iconKey?.let(visualResolver::resolve)?.let { asset ->
                    StatusHudIconModel(
                        asset = asset,
                        badgeText = StatusHudRenderer.renderCompact(effect),
                        category = effect.category,
                    )
                }
            }
}
