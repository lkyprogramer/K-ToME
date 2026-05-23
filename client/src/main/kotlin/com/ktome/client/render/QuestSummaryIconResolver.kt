package com.ktome.client.render

import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.core.snapshot.RenderTextTokenSnapshot

internal object QuestSummaryIconResolver {
    private const val OBJECTIVE_MARKER_KEY: String = "icon.quest.objective_marker"

    private val objectiveLogKeys =
        setOf(
            "log.objective.activate",
            "log.objective.progress",
            "log.objective.advance",
            "log.objective.complete",
        )

    fun resolve(
        visualResolver: VisualManifestResolver,
        token: RenderTextTokenSnapshot?,
    ): ResolvedVisualAsset? =
        token
            ?.takeIf { candidate -> candidate.key in objectiveLogKeys }
            ?.let { visualResolver.resolve(OBJECTIVE_MARKER_KEY) }
}
