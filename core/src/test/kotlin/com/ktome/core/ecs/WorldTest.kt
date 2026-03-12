package com.ktome.core.ecs

import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorldTest {
    @Test
    fun `create destroy and component lifecycle remain consistent`() {
        val world = World()
        val entity = world.createEntity()

        assertTrue(world.isAlive(entity))
        world.add(entity, Position(2, 3))

        assertTrue(world.has<Position>(entity))
        assertEquals(Point(2, 3), world.get<Position>(entity)?.toPoint())

        world.remove<Position>(entity)
        assertFalse(world.has<Position>(entity))
        assertNull(world.get<Position>(entity))

        world.destroyEntity(entity)
        assertFalse(world.isAlive(entity))
    }

    @Test
    fun `entitiesWith only returns entities that satisfy every component type`() {
        val world = World()
        val player = world.createEntity()
        val wall = world.createEntity()
        val orphan = world.createEntity()

        world.add(player, Position(1, 1))
        world.add(player, Glyph('@'))
        world.add(wall, Position(3, 4))

        val matchingEntities = world.entitiesWith(Position::class, Glyph::class)

        assertEquals(listOf(player), matchingEntities)
        assertEquals(2, world.entitiesWith(Position::class).size)
        assertTrue(world.entitiesWith(PlayerControlled::class).isEmpty())
        assertEquals(listOf(player, wall, orphan), world.entitiesWith())
    }

    @Test
    fun `systems run by priority and preserve registration order on ties`() {
        val world = World()
        val updateOrder = mutableListOf<String>()

        world.addSystem(recordingSystem("late-1", priority = 10, updateOrder))
        world.addSystem(recordingSystem("early", priority = -5, updateOrder))
        world.addSystem(recordingSystem("late-2", priority = 10, updateOrder))

        world.update()

        assertEquals(listOf("early", "late-1", "late-2"), updateOrder)
    }

    private fun recordingSystem(
        name: String,
        priority: Int,
        updates: MutableList<String>,
    ): GameSystem =
        object : GameSystem {
            override val priority: Int = priority

            override fun update(world: World) {
                updates += name
            }
        }
}
