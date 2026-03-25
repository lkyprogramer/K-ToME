package com.ktome.game.telegraph

import com.ktome.core.ai.TelegraphSpec
import com.ktome.core.ai.ThreatProfileDef

class TelegraphRegistry(
    private val definitions: Map<String, TelegraphSpec>,
) {
    fun resolve(id: String): TelegraphSpec? = definitions[id]

    fun require(id: String): TelegraphSpec =
        requireNotNull(resolve(id)) { "Missing telegraph spec '$id'." }

    fun all(): List<TelegraphSpec> = definitions.values.sortedBy(TelegraphSpec::id)
}

class ThreatProfileRegistry(
    private val definitions: Map<String, ThreatProfileDef>,
) {
    fun resolve(id: String): ThreatProfileDef? = definitions[id]

    fun require(id: String): ThreatProfileDef =
        requireNotNull(resolve(id)) { "Missing threat profile '$id'." }

    fun all(): List<ThreatProfileDef> = definitions.values.sortedBy(ThreatProfileDef::id)
}
