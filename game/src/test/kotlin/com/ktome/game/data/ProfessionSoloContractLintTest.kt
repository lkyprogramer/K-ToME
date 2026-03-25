package com.ktome.game.data

import org.junit.jupiter.api.Test

class ProfessionSoloContractLintTest {
    @Test
    fun `all profession solo contract tags remain non empty`() {
        ProfessionTreeAssertions.assertSoloContractLint()
    }
}
