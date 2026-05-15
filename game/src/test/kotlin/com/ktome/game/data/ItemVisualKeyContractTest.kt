package com.ktome.game.data

import com.ktome.game.i18n.GameLocale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemVisualKeyContractTest {
    @Test
    fun `dark uiux pr03 migrated official items consume item specific icon keys`() {
        val catalog = DataLoader(GameLocale.EN_US).loadSchemaCatalog()
        val items = catalog.itemBundle.items.associateBy { item -> item.id }
        val visualKeys = catalog.visualKeys
        val expected =
            mapOf(
                "hunter_bow" to "item.hunter_bow.icon",
                "war_maul" to "item.war_maul.icon",
                "forgebreaker_pick" to "item.forgebreaker_pick.icon",
                "bandit_trophy" to "item.bandit_trophy.icon",
                "emerald_charm" to "item.emerald_charm.icon",
                "seal_reliquary" to "item.seal_reliquary.icon",
                "sanctified_seal" to "item.sanctified_seal.icon",
                "energy_tonic" to "item.energy_tonic.icon",
                "consecrated_oil" to "item.consecrated_oil.icon",
            )

        expected.forEach { (baseItemId, expectedKey) ->
            val item = requireNotNull(items[baseItemId]) { "Missing item $baseItemId." }
            assertEquals(expectedKey, item.iconKey, baseItemId)
            assertEquals(expectedKey, item.visualKey, baseItemId)
        }

        assertTrue(visualKeys.containsAll(expected.values), "Missing visual keys: ${expected.values - visualKeys}")
    }
}
