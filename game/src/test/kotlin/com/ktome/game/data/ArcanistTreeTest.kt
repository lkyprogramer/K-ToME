package com.ktome.game.data

import org.junit.jupiter.api.Test

class ArcanistTreeTest {
    @Test
    fun `arcanist fixed three tree layout stays traversable`() {
        ProfessionTreeAssertions.assertFixedTreeLayout(
            professionId = "arcanist",
            expectedTreeIds = listOf("arcanist_flame", "arcanist_frost", "arcanist_arcane"),
        )
    }
}
