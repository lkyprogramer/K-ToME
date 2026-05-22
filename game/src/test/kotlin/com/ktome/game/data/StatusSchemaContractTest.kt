package com.ktome.game.data

import com.ktome.core.status.StatusDefinitions
import com.ktome.core.status.StatusEffectType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatusSchemaContractTest {
    private val loader = DataLoader()

    @Test
    fun `status schema stays aligned with runtime definitions`() {
        val statuses = loader.loadSchemaCatalog().statuses
        val runtimeCatalog = loader.loadStatusCatalog()

        statuses.forEach { schema ->
            val type = StatusEffectType.fromSchemaId(schema.effectType)
            val definition = runtimeCatalog.definitionFor(schema.id)

            if (type != StatusEffectType.CUSTOM) {
                assertEquals(type.schemaId, schema.id)
                assertEquals(StatusDefinitions.definitionFor(type).nameKey, schema.nameKey)
            }
            assertEquals(definition.nameKey, schema.nameKey)
            assertEquals(definition.iconKey, schema.iconKey)
            assertEquals(definition.category.name, schema.category)
            assertEquals(definition.carrierKind.name, schema.carrierKind)
        }
    }

    @Test
    fun `status schema visual keys stay in the status icon family`() {
        val offFamilyKeys =
            loader.loadSchemaCatalog().statuses.flatMap { schema ->
                listOf(
                    "${schema.id}.visualKey" to schema.visualKey,
                    "${schema.id}.iconKey" to schema.iconKey,
                ).filterNot { (_, key) -> key.startsWith("icon.status.") }
            }

        assertTrue(
            offFamilyKeys.isEmpty(),
            "Status visual/icon keys must use icon.status.* keys: $offFamilyKeys",
        )
    }
}
