package com.ktome.game.data

import com.ktome.core.status.StatusDefinitions
import com.ktome.core.status.StatusEffectType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatusSchemaContractTest {
    private val loader = DataLoader()

    @Test
    fun `status schema stays aligned with runtime definitions`() {
        val statuses = loader.loadSchemaCatalog().statuses

        statuses.forEach { schema ->
            val type = StatusEffectType.fromSchemaId(schema.effectType)
            val definition = StatusDefinitions.definitionFor(type)

            assertEquals(type.schemaId, schema.id)
            assertEquals(definition.nameKey, schema.nameKey)
            assertEquals(definition.iconKey, schema.iconKey)
            assertEquals(definition.category.name, schema.category)
            assertEquals(definition.carrierKind.name, schema.carrierKind)
        }
    }
}
