package com.ktome.core.random

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RandomSourceTest {
    @Test
    fun `split mix random source is deterministic across seed and restored state`() {
        val seeded = SplitMix64RandomSource.fromSeed(42L)
        val sameSeed = SplitMix64RandomSource.fromSeed(42L)

        val firstDouble = seeded.nextDouble()
        val mirroredDouble = sameSeed.nextDouble()

        assertEquals(mirroredDouble, firstDouble)
        assertTrue(firstDouble in 0.0..1.0)

        val resumedState = seeded.snapshotState()
        val expectedNextInt = seeded.nextInt(fromInclusive = 3, untilExclusive = 9)
        val restored = SplitMix64RandomSource.fromState(resumedState)

        assertEquals(expectedNextInt, restored.nextInt(fromInclusive = 3, untilExclusive = 9))
    }

    @Test
    fun `split mix random source rejects invalid bounds`() {
        val random = SplitMix64RandomSource.fromSeed(7L)

        assertThrows(IllegalArgumentException::class.java) {
            random.nextInt(fromInclusive = 5, untilExclusive = 5)
        }
    }
}
