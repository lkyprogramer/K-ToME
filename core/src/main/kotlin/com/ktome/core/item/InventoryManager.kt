package com.ktome.core.item

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.ecs.has
import com.ktome.core.ecs.remove
import com.ktome.core.map.Point

sealed interface InventoryOperationResult {
    val success: Boolean
    val message: String

    data class Success(
        override val message: String,
        val itemId: EntityId,
        val itemName: String,
        val slot: EquipSlot? = null,
    ) : InventoryOperationResult {
        override val success: Boolean = true
    }

    data class Failure(
        override val message: String,
    ) : InventoryOperationResult {
        override val success: Boolean = false
    }
}

class InventoryManager {
    fun pickUp(
        world: World,
        entity: EntityId,
        item: EntityId,
    ): InventoryOperationResult {
        val inventory = inventoryOf(world, entity)
        val itemInstance = world.get<ItemInstance>(item)
            ?: return InventoryOperationResult.Failure("That is not an item.")

        if (!world.has<GroundItem>(item)) {
            return InventoryOperationResult.Failure("${itemInstance.name} is not on the ground.")
        }
        if (inventory.itemIds.size >= inventory.capacity) {
            return InventoryOperationResult.Failure("Your pack is full.")
        }

        inventory.itemIds += item
        world.remove<GroundItem>(item)
        world.remove<Position>(item)
        return InventoryOperationResult.Success(
            message = "You pick up ${itemInstance.name}.",
            itemId = item,
            itemName = itemInstance.name,
        )
    }

    fun drop(
        world: World,
        entity: EntityId,
        itemIndex: Int,
        dropPosition: Point,
    ): InventoryOperationResult {
        val inventory = inventoryOf(world, entity)
        val itemId = inventory.itemIds.getOrNull(itemIndex)
            ?: return InventoryOperationResult.Failure("That pack slot is empty.")
        val item = requireNotNull(world.get<ItemInstance>(itemId))

        equippedSlotOf(world, entity, itemId)?.let { slot ->
            equipmentOf(world, entity).slots.remove(slot)
        }

        inventory.itemIds.removeAt(itemIndex)
        world.add(itemId, Position(dropPosition.x, dropPosition.y))
        world.add(itemId, GroundItem)
        return InventoryOperationResult.Success(
            message = "You drop ${item.name}.",
            itemId = itemId,
            itemName = item.name,
        )
    }

    fun equip(
        world: World,
        entity: EntityId,
        itemIndex: Int,
    ): InventoryOperationResult {
        val inventory = inventoryOf(world, entity)
        val itemId = inventory.itemIds.getOrNull(itemIndex)
            ?: return InventoryOperationResult.Failure("That pack slot is empty.")
        val item = requireNotNull(world.get<ItemInstance>(itemId))
        val slot = item.slot ?: return InventoryOperationResult.Failure("${item.name} cannot be equipped.")

        equipmentOf(world, entity).slots[slot] = itemId
        return InventoryOperationResult.Success(
            message = "You equip ${item.name}.",
            itemId = itemId,
            itemName = item.name,
            slot = slot,
        )
    }

    fun unequip(
        world: World,
        entity: EntityId,
        slot: EquipSlot,
    ): InventoryOperationResult {
        val itemId = equipmentOf(world, entity).slots.remove(slot)
            ?: return InventoryOperationResult.Failure("Nothing is equipped in $slot.")
        val item = requireNotNull(world.get<ItemInstance>(itemId))
        return InventoryOperationResult.Success(
            message = "You remove ${item.name}.",
            itemId = itemId,
            itemName = item.name,
            slot = slot,
        )
    }

    fun useConsumable(
        world: World,
        entity: EntityId,
        itemIndex: Int,
        teleportDestination: Point? = null,
    ): InventoryOperationResult {
        val inventory = inventoryOf(world, entity)
        val itemId = inventory.itemIds.getOrNull(itemIndex)
            ?: return InventoryOperationResult.Failure("That pack slot is empty.")
        val item = requireNotNull(world.get<ItemInstance>(itemId))
        val effect = item.effect ?: return InventoryOperationResult.Failure("${item.name} is not consumable.")

        when (effect) {
            ConsumableEffect.HEAL -> {
                val health = requireNotNull(world.get<Health>(entity)) { "Missing Health for $entity" }
                health.current = (health.current + item.magnitude).coerceAtMost(health.max)
            }

            ConsumableEffect.TELEPORT -> {
                val destination = teleportDestination ?: return InventoryOperationResult.Failure("No teleport destination is available.")
                val position = requireNotNull(world.get<Position>(entity)) { "Missing Position for $entity" }
                position.moveTo(destination)
            }
        }

        equippedSlotOf(world, entity, itemId)?.let { slot ->
            equipmentOf(world, entity).slots.remove(slot)
        }
        inventory.itemIds.removeAt(itemIndex)
        world.destroyEntity(itemId)
        return InventoryOperationResult.Success(
            message =
                when (effect) {
                    ConsumableEffect.HEAL -> "You use ${item.name}."
                    ConsumableEffect.TELEPORT -> "You read ${item.name}."
                },
            itemId = itemId,
            itemName = item.name,
        )
    }

    fun equippedSlotOf(
        world: World,
        entity: EntityId,
        itemId: EntityId,
    ): EquipSlot? =
        equipmentOf(world, entity).slots.entries.firstOrNull { (_, equippedId) -> equippedId == itemId }?.key

    fun inventoryOf(
        world: World,
        entity: EntityId,
    ): Inventory = requireNotNull(world.get<Inventory>(entity)) { "Missing Inventory for $entity" }

    private fun equipmentOf(
        world: World,
        entity: EntityId,
    ): Equipment = requireNotNull(world.get<Equipment>(entity)) { "Missing Equipment for $entity" }
}

