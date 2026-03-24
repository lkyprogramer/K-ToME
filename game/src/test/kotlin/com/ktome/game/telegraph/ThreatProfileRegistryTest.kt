package com.ktome.game.telegraph

import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThreatProfileRegistryTest {
    @Test
    fun `registry resolves threat profiles in stable id order`() {
        val profiles = DataLoader().loadSchemaCatalog().threatProfiles
        val registry = ThreatProfileRegistry(profiles.associateBy { profile -> profile.id })

        assertEquals(profiles.map { profile -> profile.id }.sorted(), registry.all().map { profile -> profile.id })
        assertEquals("frontliner", requireNotNull(registry.resolve("threat.frontliner.early")).defenderArchetype)
        assertEquals("normal", requireNotNull(registry.resolve("threat.frontliner.mid")).difficultyId)
    }

    @Test
    fun `registry fails fast for unknown threat profiles`() {
        val registry = ThreatProfileRegistry(emptyMap())
        val error = assertThrows(IllegalArgumentException::class.java) { registry.require("threat.missing") }
        assertTrue(requireNotNull(error.message).contains("Missing threat profile"))
    }
}
