package com.ktome.client.ui.status

import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.core.snapshot.StatusEffectCategorySnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot

internal data class StatusHudFoldInteractionModel(
    val interactive: Boolean,
    val hint: String,
    val detailTitle: String,
    val detailBody: String,
)

internal data class StatusHudIconModel(
    val asset: ResolvedVisualAsset,
    val presentation: StatusPresentationModel,
    val isFoldBadge: Boolean = false,
    val hiddenPresentations: List<StatusPresentationModel> = emptyList(),
    val foldSummary: String? = null,
    val foldInteraction: StatusHudFoldInteractionModel? = null,
) {
    val badgeText: String
        get() = foldSummary ?: presentation.badgeText

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
                        statusIconKey(presentation),
                    )
                StatusHudIconModel(
                    asset = asset,
                    presentation = presentation,
                )
            }

    private fun statusIconKey(presentation: StatusPresentationModel): String {
        val fallbackKey = STATUS_ICON_KEY_PREFIX + presentation.typeId
        val iconKey = presentation.iconKey?.takeIf(String::isNotBlank) ?: fallbackKey
        return iconKey.takeIf { it.startsWith(STATUS_ICON_KEY_PREFIX) } ?: fallbackKey
    }
}
