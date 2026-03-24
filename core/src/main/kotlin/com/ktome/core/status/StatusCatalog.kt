package com.ktome.core.status

class StatusCatalog(
    definitions: Iterable<StatusEffectDef> = emptyList(),
) {
    private val customDefinitions: Map<String, StatusEffectDef> =
        definitions.associateBy(StatusEffectDef::id)

    fun definitionFor(statusId: String): StatusEffectDef =
        customDefinitions[statusId]
            ?: StatusDefinitions.definitionForSchemaId(statusId)
            ?: error("Missing status definition for schema id '$statusId'.")

    fun definitionOrNull(statusId: String): StatusEffectDef? =
        customDefinitions[statusId] ?: StatusDefinitions.definitionForSchemaId(statusId)

    companion object {
        val EMPTY: StatusCatalog = StatusCatalog()
    }
}
