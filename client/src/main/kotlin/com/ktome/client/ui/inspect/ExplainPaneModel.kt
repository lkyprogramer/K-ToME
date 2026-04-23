package com.ktome.client.ui.inspect

import com.ktome.client.ui.card.ModalCardModel
import com.ktome.client.ui.talent.DescriptionPresenter
import com.ktome.client.ui.talent.InspectDescriptionSurface
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.OverlayRenderSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.TerrainOverrideRenderSnapshot
import com.ktome.core.talent.KeywordRegistry

internal data class ExplainPaneModel(
    val card: ModalCardModel,
    val keywordChips: List<RenderTextTokenSnapshot> = emptyList(),
    val referenceChain: List<RenderTextTokenSnapshot> = emptyList(),
) {
    init {
        keywordChips.forEach { chip ->
            require(chip.key.isNotBlank()) { "ExplainPane keyword chip token must not be blank." }
        }
        referenceChain.forEach { token ->
            require(token.key.isNotBlank()) { "ExplainPane reference token must not be blank." }
        }
    }

    companion object {
        fun empty(): ExplainPaneModel =
            ExplainPaneModel(
                card = DescriptionPresenter.emptyExplainCard(),
            )

        fun fromCard(
            card: ModalCardModel,
            keywordIds: List<String> = emptyList(),
            referenceChain: List<RenderTextTokenSnapshot> = emptyList(),
        ): ExplainPaneModel =
            ExplainPaneModel(
                card = card,
                keywordChips =
                    keywordIds.distinct().map { keywordId ->
                        val semantic = KeywordRegistry.CORE.require(keywordId)
                        RenderTextTokenSnapshot(semantic.nameKey)
                    },
                referenceChain = referenceChain,
            )

        fun fromInspectSurface(
            actor: ActorRenderSnapshot?,
            item: ItemRenderSnapshot?,
            prop: PropRenderSnapshot?,
            terrainOverride: TerrainOverrideRenderSnapshot?,
            overlay: OverlayRenderSnapshot?,
        ): ExplainPaneModel {
            val surface =
                InspectDescriptionSurface(
                    actor = actor,
                    item = item,
                    prop = prop,
                    terrainOverride = terrainOverride,
                    overlay = overlay,
                )
            val card = DescriptionPresenter.inspectSurfaceCard(surface)
            val keywordIds = DescriptionPresenter.inspectSurfaceKeywordIds(surface)
            return fromCard(card = card, keywordIds = keywordIds)
        }
    }
}
