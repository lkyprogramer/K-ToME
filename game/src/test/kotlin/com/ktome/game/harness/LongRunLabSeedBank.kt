package com.ktome.game.harness

internal object LongRunLabSeedBank {
    private val professionOrder = listOf("vanguard", "arcanist", "rogue", "templar")
    private val raceOrder = listOf("human", "elf", "dwarf")
    private const val fullRouteSeedBase = 20260330L
    private const val professionSeedStride = 10L

    fun fullRouteMatrixSeed(
        professionId: String,
        raceId: String,
    ): Long {
        val professionIndex = professionOrder.indexOf(professionId)
        require(professionIndex >= 0) { "Unknown professionId=$professionId for long-run seed bank." }
        val raceIndex = raceOrder.indexOf(raceId)
        require(raceIndex >= 0) { "Unknown raceId=$raceId for long-run seed bank." }
        return fullRouteSeedBase + professionIndex * professionSeedStride + raceIndex
    }
}
