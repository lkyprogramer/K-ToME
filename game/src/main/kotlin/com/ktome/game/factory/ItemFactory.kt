package com.ktome.game.factory

import com.ktome.core.ecs.DisplayColor
import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.Name
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.item.GroundItem
import com.ktome.core.item.ItemInstance
import com.ktome.core.map.Point

class ItemFactory {
    fun createGroundItem(
        world: World,
        item: ItemInstance,
        position: Point,
    ): com.ktome.core.ecs.EntityId {
        val itemId = world.createEntity()
        world.add(itemId, Position(position.x, position.y))
        world.add(itemId, Glyph(item.glyph))
        world.add(itemId, DisplayColor(item.colorHex))
        world.add(itemId, Name(item.name))
        world.add(itemId, item)
        world.add(itemId, GroundItem)
        return itemId
    }
}

