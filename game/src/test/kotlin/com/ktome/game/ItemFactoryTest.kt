package com.ktome.game

import com.ktome.core.ecs.DisplayColor
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.ecs.has
import com.ktome.core.item.GroundItem
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemType
import com.ktome.game.factory.ItemFactory
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ItemFactoryTest {
    @Test
    fun `createGroundItem installs view and pickup components`() {
        val world = World()
        val factory = ItemFactory()

        val itemId =
            factory.createGroundItem(
                world = world,
                item =
                    ItemInstance(
                        baseId = "short_sword",
                        name = "Steel Short Sword",
                        type = ItemType.WEAPON,
                        slot = com.ktome.core.item.EquipSlot.WEAPON,
                        glyph = ')',
                        colorHex = "#C0C0C0",
                    ),
                position = Point(3, 4),
            )

        assertEquals(Point(3, 4), requireNotNull(world.get<Position>(itemId)).toPoint())
        assertEquals("Steel Short Sword", requireNotNull(world.get<Name>(itemId)).value)
        assertEquals("#C0C0C0", requireNotNull(world.get<DisplayColor>(itemId)).hex)
        assertTrue(world.has<GroundItem>(itemId))
    }
}
