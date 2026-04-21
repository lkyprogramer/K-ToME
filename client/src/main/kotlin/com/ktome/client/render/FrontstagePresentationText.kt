package com.ktome.client.render

import com.ktome.core.snapshot.FrontstageActionCategorySnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot

private const val FRONTSTAGE_PRESENTATION_LIMIT: Int = 5

internal enum class FrontstagePresentationKind {
    MUTATION,
    TERRAIN,
    SEARCH,
    SECRET,
    PASSIVE,
}

internal data class FrontstagePresentationEntry(
    val token: RenderTextTokenSnapshot,
    val kind: FrontstagePresentationKind,
)

internal fun frontstagePresentationEntries(snapshot: RenderSnapshot): List<FrontstagePresentationEntry> =
    buildList {
        snapshot.uiState.frontstageReadability.mutationHighlights.forEach { token ->
            add(FrontstagePresentationEntry(token = token, kind = FrontstagePresentationKind.MUTATION))
        }
        snapshot.uiState.frontstageReadability.terrainHighlights.forEach { token ->
            add(FrontstagePresentationEntry(token = token, kind = FrontstagePresentationKind.TERRAIN))
        }
        snapshot.uiState.frontstageReadability.recentActionCues.forEach { cue ->
            add(FrontstagePresentationEntry(token = cue.message, kind = cue.category.presentationKind()))
        }
    }.take(FRONTSTAGE_PRESENTATION_LIMIT)

private fun FrontstageActionCategorySnapshot.presentationKind(): FrontstagePresentationKind =
    when (this) {
        FrontstageActionCategorySnapshot.SEARCH -> FrontstagePresentationKind.SEARCH
        FrontstageActionCategorySnapshot.SECRET -> FrontstagePresentationKind.SECRET
        FrontstageActionCategorySnapshot.PASSIVE -> FrontstagePresentationKind.PASSIVE
    }
