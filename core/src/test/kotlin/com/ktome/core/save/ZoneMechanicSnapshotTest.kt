package com.ktome.core.save

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ZoneMechanicSnapshotTest {
    private val codec = SaveCodec()

    @Test
    fun `save codec round trip preserves zone mechanic runtime snapshots`() {
        val base = SaveFixtures.emptyScene()
        val floor = base.floors.single()
        val snapshot =
            base.copy(
                floors =
                    listOf(
                        floor.copy(
                            entities =
                                floor.entities +
                                    listOf(
                                        EntitySnapshot(
                                            id = 30,
                                            patrolPressureState =
                                                PatrolPressureStateSnapshot(
                                                    spawnTemplateIds = listOf("beast.rat_scavenger"),
                                                    maxHostiles = 4,
                                                    waveLimit = 3,
                                                    checkIntervalTurns = 20,
                                                    spawnSeed = 20260330L,
                                                    nextCheckHeadlessTurn = 40,
                                                    wavesSpawned = 1,
                                                    floorHintShown = true,
                                                ),
                                        ),
                                        EntitySnapshot(
                                            id = 31,
                                            position = PointSnapshot(2, 2),
                                            ambushLaneTrigger =
                                                AmbushLaneTriggerSnapshot(
                                                    triggerId = "ambush_lane:1:0",
                                                    spawnTemplateIds = listOf("bandit.archer", "bandit.raider"),
                                                    spawnPoints = listOf(PointSnapshot(3, 2), PointSnapshot(3, 3)),
                                                ),
                                        ),
                                        EntitySnapshot(
                                            id = 32,
                                            furnacePressureState =
                                                FurnacePressureStateSnapshot(
                                                    hazardCells = listOf(PointSnapshot(1, 3), PointSnapshot(2, 3)),
                                                    cycleIntervalTurns = 30,
                                                    telegraphTurns = 1,
                                                    activeTurns = 2,
                                                    damagePerTick = 6,
                                                    nextCycleTurn = 30,
                                                    phase = "TELEGRAPH",
                                                    phaseTurnsRemaining = 1,
                                                ),
                                        ),
                                    ),
                        ),
                    ),
            )

        val restored = codec.decode(codec.encode(snapshot))

        assertEquals(snapshot, restored)
    }

    @Test
    fun `zone mechanic snapshots reject invalid contracts`() {
        assertThrows(IllegalArgumentException::class.java) {
            PatrolPressureStateSnapshot(
                spawnTemplateIds = emptyList(),
                maxHostiles = 4,
                waveLimit = 3,
                checkIntervalTurns = 20,
                spawnSeed = 1L,
                nextCheckHeadlessTurn = 0,
            ).validateOrThrow()
        }

        assertThrows(IllegalArgumentException::class.java) {
            AmbushLaneTriggerSnapshot(
                triggerId = "",
                spawnTemplateIds = listOf("bandit.archer"),
                spawnPoints = listOf(PointSnapshot(1, 1)),
            ).validateOrThrow()
        }

        assertThrows(IllegalArgumentException::class.java) {
            FurnacePressureStateSnapshot(
                hazardCells = emptyList(),
                cycleIntervalTurns = 30,
                telegraphTurns = 1,
                activeTurns = 2,
                damagePerTick = 6,
                nextCycleTurn = 0,
                phase = "",
            ).validateOrThrow()
        }
    }
}
