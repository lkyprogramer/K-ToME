package com.ktome.core.dungeon

class DungeonManager<T>(
    private val maxFloor: Int,
    private val floorLoader: (Int) -> FloorState<T>,
    startFloor: Int = 1,
) {
    private val floors = linkedMapOf<Int, FloorState<T>>()

    var currentFloor: Int = startFloor
        private set

    init {
        require(maxFloor > 0) { "Dungeon must contain at least one floor." }
        require(startFloor in 1..maxFloor) { "Start floor $startFloor must be within 1..$maxFloor." }
    }

    fun currentState(): FloorState<T> = loadFloor(currentFloor)

    fun hasFloor(floor: Int): Boolean = floor in floors

    fun knownFloors(): Set<Int> = floors.keys.toSet()

    fun replaceCurrentState(state: FloorState<T>) {
        require(state.floor == currentFloor) {
            "Cannot replace current floor $currentFloor with floor ${state.floor}."
        }
        floors[currentFloor] = state
    }

    fun putState(state: FloorState<T>) {
        require(state.floor in 1..maxFloor) {
            "Floor ${state.floor} must be within 1..$maxFloor."
        }
        floors[state.floor] = state
    }

    fun stateOf(floor: Int): FloorState<T>? = floors[floor]

    fun transition(direction: StairDirection): DungeonTransition<T> {
        val currentState = currentState()
        val targetFloor = currentFloor + direction.deltaFloor()
        require(targetFloor in 1..maxFloor) {
            "Cannot move $direction from floor $currentFloor in a $maxFloor-floor dungeon."
        }

        when (direction) {
            StairDirection.UP -> requireNotNull(currentState.stairsUp) {
                "Floor $currentFloor has no upstairs."
            }
            StairDirection.DOWN -> requireNotNull(currentState.stairsDown) {
                "Floor $currentFloor has no downstairs."
            }
        }

        val targetState = loadFloor(targetFloor)
        currentFloor = targetFloor
        return DungeonTransition(
            fromFloor = currentState.floor,
            toFloor = targetFloor,
            entryPoint = targetState.entryPoint(direction),
            state = targetState,
        )
    }

    private fun loadFloor(floor: Int): FloorState<T> =
        floors.getOrPut(floor) {
            floorLoader(floor).also { state ->
                require(state.floor == floor) {
                    "Floor loader returned floor ${state.floor} for requested floor $floor."
                }
            }
        }
}
