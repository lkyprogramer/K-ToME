package com.ktome.game.harness

import com.ktome.game.PlayerCommand

fun interface RunBot {
    fun decide(observation: RunObservation): PlayerCommand?
}
