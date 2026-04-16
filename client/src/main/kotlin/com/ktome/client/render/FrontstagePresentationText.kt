package com.ktome.client.render

import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot

private const val FRONTSTAGE_PRESENTATION_LIMIT: Int = 5

internal enum class FrontstagePresentationKind {
    MUTATION,
    TERRAIN,
    ACTION,
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
        snapshot.uiState.frontstageReadability.recentActionHighlights.forEach { token ->
            add(FrontstagePresentationEntry(token = token, kind = FrontstagePresentationKind.ACTION))
        }
    }.take(FRONTSTAGE_PRESENTATION_LIMIT)
