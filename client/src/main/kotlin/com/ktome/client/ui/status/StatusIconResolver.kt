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
    private const val STATUS_ICON_KEY_PREFIX: String = "icon.status."

    fun resolveIcons(
        visualResolver: VisualManifestResolver,
        effects: List<StatusEffectRenderSnapshot>,
    ): List<StatusHudIconModel> =
        StatusPresentationBuilder
            .sorted(effects.map(StatusPresentationBuilder::build))
            .map { presentation ->
                val asset =
                    visualResolver.resolve(
                        presentation.iconKey ?: STATUS_ICON_KEY_PREFIX + presentation.typeId,
                    )
                StatusHudIconModel(
                    asset = asset,
                    presentation = presentation,
                )
            }
}
