package com.ktome.game.data

import org.junit.jupiter.api.Test

class VanguardTreeTest {
    @Test
    fun `vanguard fixed three tree layout stays traversable`() {
        ProfessionTreeAssertions.assertFixedTreeLayout(
            professionId = "vanguard",
            expectedTreeIds = listOf("vanguard_arms", "vanguard_shield", "vanguard_warcry"),
        )
    }
}
