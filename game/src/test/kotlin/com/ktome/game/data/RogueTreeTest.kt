package com.ktome.game.data

import org.junit.jupiter.api.Test

class RogueTreeTest {
    @Test
    fun `rogue fixed three tree layout stays traversable`() {
        ProfessionTreeAssertions.assertFixedTreeLayout(
            professionId = "rogue",
            expectedTreeIds = listOf("rogue_assassination", "rogue_subtlety", "rogue_agility"),
        )
    }
}
