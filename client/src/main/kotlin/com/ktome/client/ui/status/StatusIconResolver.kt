package com.ktome.client.ui.status

import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot

internal data class StatusHudIconModel(
    val asset: ResolvedVisualAsset,
    val presentation: StatusPresentationModel,
) {
    val badgeText: String
        get() = presentation.badgeText

    val category: StatusEffectCategorySnapshot
        get() = presentation.category
}

internal object StatusIconResolver {
    fun resolveIcons(
        visualResolver: VisualManifestResolver,
        effects: List<StatusEffectRenderSnapshot>,
    ): List<StatusHudIconModel> =
        StatusPresentationBuilder
            .sorted(effects.map(StatusPresentationBuilder::build))
            .mapNotNull { presentation ->
                presentation.iconKey?.let(visualResolver::resolve)?.let { asset ->
                    StatusHudIconModel(
                        asset = asset,
                        presentation = presentation,
                    )
                }
            }
}
