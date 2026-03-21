package com.ktome.core.item

import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.ecs.has
import com.ktome.core.map.Point
import com.ktome.core.resource.ResourcePool
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InventoryManagerTest {
    private val manager = InventoryManager()

    @Test
    fun `pick up moves ground item into inventory`() {
        val world = baseWorld()
        val player = createPlayer(world)
        val item = createItem(world, Point(1, 1), ItemType.WEAPON, EquipSlot.WEAPON)

        val result = manager.pickUp(world, player, item)

        assertTrue(result.success)
        assertEquals(listOf(item), requireNotNull(world.get<Inventory>(player)).itemIds)
        assertFalse(world.has<GroundItem>(item))
        assertEquals(null, world.get<Position>(item))
    }

    @Test
    fun `inventory capacity is enforced`() {
        val world = baseWorld()
        val player = createPlayer(world, capacity = 1)
        val firstItem = createItem(world, Point(1, 1), ItemType.WEAPON, EquipSlot.WEAPON)
        val secondItem = createItem(world, Point(1, 1), ItemType.ARMOR, EquipSlot.ARMOR)

        manager.pickUp(world, player, firstItem)
        val result = manager.pickUp(world, player, secondItem)

        assertFalse(result.success)
        assertEquals(1, requireNotNull(world.get<Inventory>(player)).itemIds.size)
    }

    @Test
    fun `equip assigns item to matching slot`() {
        val world = baseWorld()
        val player = createPlayer(world)
        val weapon = createItem(world, Point(1, 1), ItemType.WEAPON, EquipSlot.WEAPON)
        manager.pickUp(world, player, weapon)

        val result = manager.equip(world, player, 0)

        assertTrue(result.success)
        assertEquals(weapon, requireNotNull(world.get<Equipment>(player)).slots[EquipSlot.WEAPON])
        assertEquals(EquipSlot.WEAPON, manager.equippedSlotOf(world, player, weapon))
    }

    @Test
    fun `drop removes equipped item from pack and places it on ground`() {
        val world = baseWorld()
        val player = createPlayer(world)
        val armor = createItem(world, Point(1, 1), ItemType.ARMOR, EquipSlot.ARMOR)
        manager.pickUp(world, player, armor)
        manager.equip(world, player, 0)

        val result = manager.drop(world, player, 0, Point(3, 3))

        assertTrue(result.success)
        assertTrue(requireNotNull(world.get<Inventory>(player)).itemIds.isEmpty())
        assertEquals(null, requireNotNull(world.get<Equipment>(player)).slots[EquipSlot.ARMOR])
        assertEquals(Point(3, 3), requireNotNull(world.get<Position>(armor)).toPoint())
        assertTrue(world.has<GroundItem>(armor))
    }

    @Test
    fun `use consumable heals and destroys the item`() {
        val world = baseWorld()
        val player = createPlayer(world)
        requireNotNull(world.get<Health>(player)).current = 4
        val potion =
            createItem(
                world = world,
                position = Point(1, 1),
                type = ItemType.CONSUMABLE,
                slot = null,
                effect = ConsumableEffect.HEAL,
                magnitude = 6,
            )
        manager.pickUp(world, player, potion)

        val result = manager.useConsumable(world, player, 0)

        assertTrue(result.success)
        assertEquals(10, requireNotNull(world.get<Health>(player)).current)
        assertTrue(requireNotNull(world.get<Inventory>(player)).itemIds.isEmpty())
        assertFalse(world.isAlive(potion))
    }

    @Test
    fun `teleport consumable moves actor to destination`() {
        val world = baseWorld()
        val player = createPlayer(world)
        val scroll =
            createItem(
                world = world,
                position = Point(1, 1),
                type = ItemType.CONSUMABLE,
                slot = null,
                effect = ConsumableEffect.TELEPORT,
            )
        manager.pickUp(world, player, scroll)

        val result = manager.useConsumable(world, player, 0, Point(5, 4))

        assertTrue(result.success)
        assertEquals(Point(5, 4), requireNotNull(world.get<Position>(player)).toPoint())
    }

    @Test
    fun `resource consumable restores matching pool`() {
        val world = baseWorld()
        val player = createPlayer(world)
        world.add(
            player,
            ResourcePools(
                linkedMapOf(
                    ResourceType.MANA to ResourcePool(type = ResourceType.MANA, current = 12, max = 40),
                ),
            ),
        )
        val potion =
            createItem(
                world = world,
                position = Point(1, 1),
                type = ItemType.CONSUMABLE,
                slot = null,
                effect = ConsumableEffect.RESTORE_RESOURCE,
                resourceTypeId = ResourceType.MANA.name,
                magnitude = 15,
            )
        manager.pickUp(world, player, potion)

        val result = manager.useConsumable(world, player, 0)

        assertTrue(result.success)
        assertEquals(27, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.MANA)?.current)
        assertTrue(requireNotNull(world.get<Inventory>(player)).itemIds.isEmpty())
        assertFalse(world.isAlive(potion))
    }

    @Test
    fun `stamina consumable restores stamina pool and component together`() {
        val world = baseWorld()
        val player = createPlayer(world)
        requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.STAMINA)?.current = 3
        val potion =
            createItem(
                world = world,
                position = Point(1, 1),
                type = ItemType.CONSUMABLE,
                slot = null,
                effect = ConsumableEffect.RESTORE_RESOURCE,
                resourceTypeId = ResourceType.STAMINA.name,
                magnitude = 4,
            )
        manager.pickUp(world, player, potion)

        val result = manager.useConsumable(world, player, 0)

        assertTrue(result.success)
        assertEquals(7, requireNotNull(world.get<ResourcePools>(player)).pool(ResourceType.STAMINA)?.current)
        assertEquals(7, requireNotNull(world.get<Stamina>(player)).current)
    }

    private fun baseWorld(): World = World()

    private fun createPlayer(
        world: World,
        capacity: Int = 12,
    ): com.ktome.core.ecs.EntityId {
        val player = world.createEntity()
        world.add(player, Position(1, 1))
        world.add(player, Stats(str = 10, dex = 10, con = 10, wil = 10))
        world.add(player, CombatProfile(baseAttack = 5, baseDefense = 2))
        world.add(player, Health(current = 10, max = 10))
        world.add(player, Stamina(current = 10, max = 10))
        world.add(
            player,
            ResourcePools(
                linkedMapOf(
                    ResourceType.STAMINA to ResourcePool(type = ResourceType.STAMINA, current = 10, max = 10),
                ),
            ),
        )
        world.add(player, Inventory(capacity = capacity))
        world.add(player, Equipment())
        return player
    }

    private fun createItem(
        world: World,
        position: Point,
        type: ItemType,
        slot: EquipSlot?,
        effect: ConsumableEffect? = null,
        resourceTypeId: String? = null,
        magnitude: Int = 0,
    ): com.ktome.core.ecs.EntityId {
        val item = world.createEntity()
        world.add(item, Position(position.x, position.y))
        world.add(
            item,
            ItemInstance(
                baseId = "$type-item",
                name = "$type item",
                type = type,
                slot = slot,
                glyph = if (type == ItemType.CONSUMABLE) '!' else ')',
                colorHex = "#FFFFFF",
                effect = effect,
                resourceTypeId = resourceTypeId,
                magnitude = magnitude,
            ),
        )
        world.add(item, GroundItem)
        return item
    }
}
