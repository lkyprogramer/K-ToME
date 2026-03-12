package com.ktome.core.random

import kotlin.random.Random

interface RandomSource {
    fun nextDouble(): Double

    fun nextInt(fromInclusive: Int, untilExclusive: Int): Int

    companion object {
        fun from(random: Random): RandomSource = KotlinRandomSource(random)
    }
}

class KotlinRandomSource(
    private val random: Random,
) : RandomSource {
    override fun nextDouble(): Double = random.nextDouble()

    override fun nextInt(fromInclusive: Int, untilExclusive: Int): Int =
        random.nextInt(fromInclusive, untilExclusive)
}
