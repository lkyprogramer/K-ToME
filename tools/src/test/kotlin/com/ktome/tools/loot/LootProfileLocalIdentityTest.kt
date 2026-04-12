package com.ktome.tools.loot

import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LootProfileLocalIdentityTest {
    @Test
    fun `canonical zone id matches current deep iron content ids`() {
        val profile =
            DataLoader()
                .loadSchemaCatalog()
                .lootProfiles
                .single { candidate -> candidate.id == "loot.deep_iron_slag_cache.secret" }

        assertEquals("deep_iron_pit", profile.localIdentityMetadata().zoneId)
    }
}
