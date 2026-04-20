package com.ktome.game.hidden

import com.ktome.core.mapgen.GeneratedEntrance
import com.ktome.core.world.solvability.ContentRef

class HiddenEventRegistry(
    private val eventsById: Map<String, HiddenEventDef>,
) {
    private val eventsByTrigger: Map<HiddenTriggerType, List<HiddenEventDef>> =
        eventsById.values
            .groupBy(HiddenEventDef::triggerType)
            .mapValues { (_, events) -> events.sortedBy(HiddenEventDef::id) }

    fun all(): List<HiddenEventDef> = eventsById.values.sortedBy(HiddenEventDef::id)

    fun resolve(id: String): HiddenEventDef? = eventsById[id]

    fun eventsForTrigger(triggerType: HiddenTriggerType): List<HiddenEventDef> =
        eventsByTrigger[triggerType].orEmpty()
}

class SecretZoneRegistry(
    private val zonesById: Map<String, SecretZoneDef>,
) {
    fun all(): List<SecretZoneDef> = zonesById.values.sortedBy { zone -> zone.id.id }

    fun resolve(id: String): SecretZoneDef? = zonesById[id]

    fun resolve(contentRef: ContentRef): SecretZoneDef? =
        if (contentRef.registry.value == SECRET_ZONE_REGISTRY_ID) {
            zonesById[contentRef.id]
        } else {
            null
        }

    fun resolveForEntrance(entrance: GeneratedEntrance): SecretZoneDef? = resolve(entrance.targetSecretZoneId)
}
