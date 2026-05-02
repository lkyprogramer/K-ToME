package com.ktome.game.harness

internal object LongRunLabSeedBank {
    private val professionOrder = listOf("vanguard", "arcanist", "rogue", "templar")
    private val raceOrder = listOf("human", "elf", "dwarf")
    private const val fullRouteSeedBase = 20260330L
    private const val professionSeedStride = 10L
    private val fullRouteSeedOverrides: Map<Pair<String, String>, Long> =
        mapOf(
            // PR-03 reward-identity pressure made the original human arcanist checkpoint smoke seed brittle.
            // Use the adjacent matrix seed that keeps the smoke slice on a blink-payoff route.
            ("arcanist" to "human") to 20260342L,
            // PR-03 loot/content density made the original dwarf arcanist world seed brittle before underground_river.
            // Reuse the adjacent arcanist matrix seed that still preserves the same route coverage shape.
            ("arcanist" to "dwarf") to 20260341L,
        )

    fun fullRouteMatrixSeed(
        professionId: String,
        raceId: String,
    ): Long {
        fullRouteSeedOverrides[professionId to raceId]?.let { overrideSeed ->
            return overrideSeed
        }
        val professionIndex = professionOrder.indexOf(professionId)
        require(professionIndex >= 0) { "Unknown professionId=$professionId for long-run seed bank." }
        val raceIndex = raceOrder.indexOf(raceId)
        require(raceIndex >= 0) { "Unknown raceId=$raceId for long-run seed bank." }
        return fullRouteSeedBase + professionIndex * professionSeedStride + raceIndex
    }
}
