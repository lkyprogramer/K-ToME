package com.ktome.core.random

import java.lang.Math.floorMod
import kotlin.random.Random

interface RandomSource {
    fun nextDouble(): Double

    fun nextInt(fromInclusive: Int, untilExclusive: Int): Int

    companion object {
        fun from(random: Random): RandomSource = KotlinRandomSource(random)
    }
}

interface StatefulRandomSource : RandomSource {
    fun snapshotState(): Long
}

class KotlinRandomSource(
    private val random: Random,
) : RandomSource {
    override fun nextDouble(): Double = random.nextDouble()

    override fun nextInt(fromInclusive: Int, untilExclusive: Int): Int =
        random.nextInt(fromInclusive, untilExclusive)
}

class SplitMix64RandomSource private constructor(
    private var state: Long,
) : StatefulRandomSource {
    override fun nextDouble(): Double = ((nextLong() ushr 11).toDouble()) * DOUBLE_UNIT

    override fun nextInt(
        fromInclusive: Int,
        untilExclusive: Int,
    ): Int {
        require(fromInclusive < untilExclusive) {
            "Invalid bounds: $fromInclusive must be smaller than $untilExclusive."
        }

        val bound = untilExclusive.toLong() - fromInclusive.toLong()
        return fromInclusive + floorMod(nextLong(), bound).toInt()
    }

    override fun snapshotState(): Long = state

    private fun nextLong(): Long {
        state += GAMMA
        var value = state
        value = (value xor (value ushr 30)) * MULTIPLIER_ONE
        value = (value xor (value ushr 27)) * MULTIPLIER_TWO
        return value xor (value ushr 31)
    }

    companion object {
        private const val DOUBLE_UNIT: Double = 1.0 / (1L shl 53)
        private const val GAMMA: Long = -7046029254386353131L
        private const val MULTIPLIER_ONE: Long = -4658895280553007687L
        private const val MULTIPLIER_TWO: Long = -7723592293110705685L

        fun fromSeed(seed: Long): SplitMix64RandomSource = SplitMix64RandomSource(seed)

        fun fromState(state: Long): SplitMix64RandomSource = SplitMix64RandomSource(state)
    }
}
