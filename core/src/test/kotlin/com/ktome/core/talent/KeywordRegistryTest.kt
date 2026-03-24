package com.ktome.core.talent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KeywordRegistryTest {
    @Test
    fun `core registry resolves formal keyword semantics`() {
        val semantics = KeywordRegistry.CORE.resolveAll(listOf("damage", "stun", "reposition"))

        assertEquals(
            listOf("keyword.damage.name", "keyword.stun.name", "keyword.reposition.name"),
            semantics.map(KeywordSemantic::nameKey),
        )
        assertEquals(
            listOf(
                KeywordSemanticType.OFFENSE,
                KeywordSemanticType.CONTROL,
                KeywordSemanticType.MOBILITY,
            ),
            semantics.map(KeywordSemantic::type),
        )
        assertTrue(semantics.all { semantic -> semantic.tooltipKey.startsWith("keyword.") })
    }
}
