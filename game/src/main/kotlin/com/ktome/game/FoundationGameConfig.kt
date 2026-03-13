package com.ktome.game

data class FoundationGameConfig(
    val width: Int = 80,
    val height: Int = 50,
    val seed: Long = 20260312L,
    val fovRadius: Int = 8,
    val floor: Int = 1,
    val maxFloor: Int = 5,
    val messageLogSize: Int = 8,
)
