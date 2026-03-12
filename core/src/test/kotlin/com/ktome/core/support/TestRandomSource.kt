package com.ktome.core.support

import com.ktome.core.random.RandomSource
import java.util.ArrayDeque

class TestRandomSource(
    doubles: List<Double> = emptyList(),
    ints: List<Int> = emptyList(),
    private val defaultDouble: Double = 0.0,
    private val defaultInt: Int = 0,
) : RandomSource {
    private val doubleValues = ArrayDeque(doubles)
    private val intValues = ArrayDeque(ints)

    override fun nextDouble(): Double = if (doubleValues.isEmpty()) defaultDouble else doubleValues.removeFirst()

    override fun nextInt(
        fromInclusive: Int,
        untilExclusive: Int,
    ): Int {
        val next = if (intValues.isEmpty()) defaultInt else intValues.removeFirst()
        require(next in fromInclusive until untilExclusive) {
            "Queued int $next is outside [$fromInclusive, $untilExclusive)."
        }
        return next
    }
}
