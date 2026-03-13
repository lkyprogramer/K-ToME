package com.ktome.core.dungeon

import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DungeonManagerTest {
    @Test
    fun `transition down loads next floor and enters through upstairs`() {
        val manager =
            DungeonManager(
                maxFloor = 3,
                floorLoader = { floor ->
                    FloorState(
                        floor = floor,
                        stairsUp = if (floor > 1) Point(floor, 1) else null,
                        stairsDown = if (floor < 3) Point(floor, 3) else null,
                        payload = "floor-$floor",
                    )
                },
            )

        val transition = manager.transition(StairDirection.DOWN)

        assertEquals(1, transition.fromFloor)
        assertEquals(2, transition.toFloor)
        assertEquals(Point(2, 1), transition.entryPoint)
        assertEquals("floor-2", transition.state.payload)
        assertEquals(2, manager.currentFloor)
    }

    @Test
    fun `revisiting a floor preserves replaced payload`() {
        val manager =
            DungeonManager(
                maxFloor = 2,
                floorLoader = { floor ->
                    FloorState(
                        floor = floor,
                        stairsUp = if (floor == 2) Point(2, 1) else null,
                        stairsDown = if (floor == 1) Point(1, 2) else null,
                        payload = mutableListOf("seeded-$floor"),
                    )
                },
            )

        manager.transition(StairDirection.DOWN)
        val mutatedPayload = mutableListOf("visited-2")
        manager.replaceCurrentState(
            FloorState(
                floor = 2,
                stairsUp = Point(2, 1),
                payload = mutatedPayload,
            ),
        )

        manager.transition(StairDirection.UP)
        val backToFloorTwo = manager.transition(StairDirection.DOWN)

        assertEquals(mutatedPayload, backToFloorTwo.state.payload)
        assertEquals(setOf(1, 2), manager.knownFloors())
    }

    @Test
    fun `transition beyond dungeon bounds fails`() {
        val manager =
            DungeonManager(
                maxFloor = 1,
                floorLoader = { floor -> FloorState(floor = floor, payload = floor) },
            )

        assertThrows(IllegalArgumentException::class.java) {
            manager.transition(StairDirection.DOWN)
        }
    }
}
