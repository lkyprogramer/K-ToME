package com.ktome.tools.loot

import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.LootPoolStrategy
import com.ktome.game.data.schema.LootProfileLocalIdentityCategory
import com.ktome.game.data.schema.LootProfileSchemaV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LootProfileLocalIdentityTest {
    @Test
    fun `canonical zone id comes from schema instead of inferred profile prefixes`() {
        val profile =
            DataLoader()
                .loadSchemaCatalog()
                .lootProfiles
                .single { candidate -> candidate.id == "loot.deep_iron_slag_cache.secret" }

        assertEquals("deep_iron_pit", profile.canonicalZoneId)
        assertEquals("deep_iron_pit", profile.localIdentityMetadata().canonicalZoneId)
        assertEquals(LootProfileLocalIdentityCategory.SECRET, profile.localIdentityMetadata().category)
    }

    @Test
    fun `secret cadence reward profiles must declare canonical zone id`() {
        assertThrows(IllegalArgumentException::class.java) {
            LootProfileSchemaV3(
                id = "loot.secret.missing_zone",
                schemaVersion = 3,
                tags = listOf("loot", "secret"),
                itemIds = listOf("healing_potion"),
                rewardBudget = 1,
                poolStrategy = LootPoolStrategy.FIXED_LIST,
            )
        }
    }
}
