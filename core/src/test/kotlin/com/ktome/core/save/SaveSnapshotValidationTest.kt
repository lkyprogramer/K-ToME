package com.ktome.core.save

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SaveSnapshotValidationTest {
    @Test
    fun `entity snapshot accepts river current and crystal shard runtime states`() {
        assertDoesNotThrow {
            EntitySnapshot(
                id = 1,
                riverCurrentState =
                    RiverCurrentStateSnapshot(
                        laneCells = listOf(PointSnapshot(10, 10), PointSnapshot(10, 11)),
                        approachCells = listOf(PointSnapshot(10, 10)),
                        safeCells = listOf(PointSnapshot(10, 10), PointSnapshot(10, 11)),
                        pushDx = 1,
                        pushDy = 0,
                    ),
                crystalShardPressureState =
                    CrystalShardPressureStateSnapshot(
                        hazardCells = listOf(PointSnapshot(12, 8), PointSnapshot(12, 9)),
                        cycleIntervalTurns = 8,
                        telegraphTurns = 1,
                        activeTurns = 2,
                        damagePerTick = 6,
                        nextCycleTurn = 8,
                        phase = "IDLE",
                        phaseTurnsRemaining = 0,
                    ),
            )
        }
    }

    @Test
    fun `river current snapshot rejects missing approach cells`() {
        assertThrows(IllegalArgumentException::class.java) {
            RiverCurrentStateSnapshot(
                laneCells = listOf(PointSnapshot(4, 4)),
                approachCells = emptyList(),
                safeCells = listOf(PointSnapshot(4, 4)),
                pushDx = 1,
                pushDy = 0,
            ).validateOrThrow()
        }
    }

    @Test
    fun `crystal shard snapshot rejects invalid cycle metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            CrystalShardPressureStateSnapshot(
                hazardCells = listOf(PointSnapshot(6, 6)),
                cycleIntervalTurns = 0,
                telegraphTurns = 1,
                activeTurns = 2,
                damagePerTick = 6,
                nextCycleTurn = 0,
                phase = "ACTIVE",
                phaseTurnsRemaining = 1,
            ).validateOrThrow()
        }
    }

    @Test
    fun `entity snapshot accepts abyssal temple and void eruption runtime states`() {
        assertDoesNotThrow {
            EntitySnapshot(
                id = 1,
                abyssalTemplePressureState =
                    AbyssalTemplePressureStateSnapshot(
                        laneCells = listOf(PointSnapshot(10, 10), PointSnapshot(10, 11)),
                        corridorCells = listOf(PointSnapshot(11, 10), PointSnapshot(11, 11)),
                        cycleIntervalTurns = 12,
                        telegraphTurns = 1,
                        activeTurns = 2,
                        damagePerTick = 6,
                        suppressionTurnsOnStabilize = 4,
                        nextCycleTurn = 12,
                        phase = "IDLE",
                        suppressionTurnsRemaining = 0,
                    ),
                voidEruptionState =
                    VoidEruptionStateSnapshot(
                        hazardCells = listOf(PointSnapshot(14, 8), PointSnapshot(15, 8)),
                        cycleIntervalTurns = 10,
                        telegraphTurns = 1,
                        activeTurns = 2,
                        damagePerTick = 9,
                        stabilizedTurnsOnFocus = 4,
                        nextCycleTurn = 10,
                        phase = "TELEGRAPH",
                        phaseTurnsRemaining = 1,
                        stabilizedTurnsRemaining = 0,
                    ),
            )
        }
    }

    @Test
    fun `abyssal temple and void eruption snapshots reject invalid runtime contracts`() {
        assertThrows(IllegalArgumentException::class.java) {
            AbyssalTemplePressureStateSnapshot(
                laneCells = listOf(PointSnapshot(4, 4)),
                corridorCells = emptyList(),
                cycleIntervalTurns = 12,
                telegraphTurns = 1,
                activeTurns = 2,
                damagePerTick = 5,
                suppressionTurnsOnStabilize = 4,
                nextCycleTurn = 12,
                phase = "ACTIVE",
                phaseTurnsRemaining = 1,
            ).validateOrThrow()
        }

        assertThrows(IllegalArgumentException::class.java) {
            VoidEruptionStateSnapshot(
                hazardCells = listOf(PointSnapshot(8, 8)),
                cycleIntervalTurns = 10,
                telegraphTurns = 1,
                activeTurns = 2,
                damagePerTick = 7,
                stabilizedTurnsOnFocus = 0,
                nextCycleTurn = 10,
                phase = "ACTIVE",
                phaseTurnsRemaining = 1,
            ).validateOrThrow()
        }
    }
}
