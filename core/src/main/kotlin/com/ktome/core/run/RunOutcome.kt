package com.ktome.core.run

sealed interface RunOutcome {
    val isTerminal: Boolean

    data object InProgress : RunOutcome {
        override val isTerminal: Boolean = false
    }

    data class Victory(
        val floor: Int,
        val reason: String = "boss_defeated",
    ) : RunOutcome {
        override val isTerminal: Boolean = true
    }

    data class Defeat(
        val floor: Int,
        val reason: String = "player_died",
    ) : RunOutcome {
        override val isTerminal: Boolean = true
    }
}
