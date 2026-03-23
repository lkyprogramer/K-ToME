package com.ktome.core.map

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomTest {
    @Test
    fun `contains includes all room edges and excludes outside points`() {
        val room = Room(x = 10, y = 5, width = 4, height = 3)

        assertTrue(room.contains(Point(10, 5)))
        assertTrue(room.contains(Point(13, 7)))
        assertTrue(room.contains(room.center))

        assertFalse(room.contains(Point(9, 5)))
        assertFalse(room.contains(Point(14, 7)))
        assertFalse(room.contains(Point(13, 8)))
    }
}
