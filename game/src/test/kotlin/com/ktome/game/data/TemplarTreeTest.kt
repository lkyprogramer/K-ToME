package com.ktome.game.data

import org.junit.jupiter.api.Test

class TemplarTreeTest {
    @Test
    fun `templar fixed three tree layout stays traversable`() {
        ProfessionTreeAssertions.assertFixedTreeLayout(
            professionId = "templar",
            expectedTreeIds = listOf("templar_smite", "templar_grace", "templar_faith"),
        )
    }
}
