package com.ktome.game.telegraph

import com.ktome.core.ai.DangerLevel
import com.ktome.core.ai.TelegraphPattern
import com.ktome.core.ai.TelegraphShape
import com.ktome.core.ai.TelegraphSpec

class TelegraphRegistry(
    private val definitions: Map<String, TelegraphSpec>,
) {
    fun resolve(id: String): TelegraphSpec? = definitions[id]

    fun require(id: String): TelegraphSpec =
        requireNotNull(resolve(id)) { "Missing telegraph spec '$id'." }

    fun all(): List<TelegraphSpec> = definitions.values.sortedBy(TelegraphSpec::id)
}

object FoundationTelegraphRegistry {
    val CORE: TelegraphRegistry =
        TelegraphRegistry(
            listOf(
                TelegraphSpec(
                    id = "melee_single",
                    shape = TelegraphShape.SINGLE_TILE,
                    previewTurns = 1,
                    dangerLevel = DangerLevel.MEDIUM,
                    pattern = TelegraphPattern.SINGLE_TARGET,
                ),
                TelegraphSpec(
                    id = "charge_lane",
                    shape = TelegraphShape.LINE,
                    previewTurns = 1,
                    dangerLevel = DangerLevel.MEDIUM,
                    pattern = TelegraphPattern.LINE,
                ),
                TelegraphSpec(
                    id = "self_buff_aura",
                    shape = TelegraphShape.CIRCLE,
                    previewTurns = 1,
                    dangerLevel = DangerLevel.MEDIUM,
                    pattern = TelegraphPattern.AURA,
                ),
            ).associateBy(TelegraphSpec::id),
        )
}
