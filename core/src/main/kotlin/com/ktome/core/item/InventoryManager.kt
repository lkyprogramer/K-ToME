package com.ktome.core.item

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.ecs.has
import com.ktome.core.ecs.remove
import com.ktome.core.map.Point
import com.ktome.core.resource.ResourcePools
import com.ktome.core.resource.ResourceType

sealed interface InventoryOperationResult {
    val success: Boolean
    val message: String
    val code: InventoryOperationCode

    data class Success(
        override val code: InventoryOperationCode,
        override val message: String,
        val itemId: EntityId,
        val itemName: String,
        val itemQuality: ItemQuality? = null,
        val itemBaseId: String? = null,
        val itemMaterialId: String? = null,
        val itemAffixIds: List<String> = emptyList(),
        val slot: EquipSlot? = null,
    ) : InventoryOperationResult {
        override val success: Boolean = true
    }

    data class Failure(
        override val code: InventoryOperationCode,
        override val message: String,
        val itemName: String? = null,
        val itemQuality: ItemQuality? = null,
        val itemBaseId: String? = null,
        val itemMaterialId: String? = null,
        val itemAffixIds: List<String> = emptyList(),
        val slot: EquipSlot? = null,
    ) : InventoryOperationResult {
        override val success: Boolean = false
    }
}

enum class InventoryOperationCode {
    PICK_UP,
    DROP,
    EQUIP,
    REMOVE,
    CONSUME_USE,
    CONSUME_READ,
    NOT_ITEM,
    NOT_ON_GROUND,
    PACK_FULL,
    PACK_SLOT_EMPTY,
    CANNOT_EQUIP,
    NOTHING_EQUIPPED,
    NOT_CONSUMABLE,
    NO_TELEPORT_DESTINATION,
    NO_RESOURCE_POOL,
}

class InventoryManager {
    fun pickUp(
        world: World,
        entity: EntityId,
        item: EntityId,
    ): InventoryOperationResult {
        val inventory = inventoryOf(world, entity)
        val itemInstance = world.get<ItemInstance>(item)
            ?: return InventoryOperationResult.Failure(
                code = InventoryOperationCode.NOT_ITEM,
                message = "That is not an item.",
            )

        if (!world.has<GroundItem>(item)) {
            return InventoryOperationResult.Failure(
                code = InventoryOperationCode.NOT_ON_GROUND,
                message = "${itemInstance.name} is not on the ground.",
                itemName = itemInstance.name,
                itemQuality = itemInstance.quality,
                itemBaseId = itemInstance.baseId,
                itemMaterialId = itemInstance.materialId,
                itemAffixIds = itemInstance.affixes.map(AffixDef::id),
            )
        }
        if (inventory.itemIds.size >= inventory.capacity) {
            return InventoryOperationResult.Failure(
                code = InventoryOperationCode.PACK_FULL,
                message = "Your pack is full.",
            )
        }

        inventory.itemIds += item
        world.remove<GroundItem>(item)
        world.remove<Position>(item)
        return InventoryOperationResult.Success(
            code = InventoryOperationCode.PICK_UP,
            message = "You pick up ${itemInstance.name}.",
            itemId = item,
            itemName = itemInstance.name,
            itemQuality = itemInstance.quality,
            itemBaseId = itemInstance.baseId,
            itemMaterialId = itemInstance.materialId,
            itemAffixIds = itemInstance.affixes.map(AffixDef::id),
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
            ?: return InventoryOperationResult.Failure(
                code = InventoryOperationCode.PACK_SLOT_EMPTY,
                message = "That pack slot is empty.",
            )
        val item = requireNotNull(world.get<ItemInstance>(itemId))

        equippedSlotOf(world, entity, itemId)?.let { slot ->
            equipmentOf(world, entity).slots.remove(slot)
        }

        inventory.itemIds.removeAt(itemIndex)
        world.add(itemId, Position(dropPosition.x, dropPosition.y))
        world.add(itemId, GroundItem)
        return InventoryOperationResult.Success(
            code = InventoryOperationCode.DROP,
            message = "You drop ${item.name}.",
            itemId = itemId,
            itemName = item.name,
            itemQuality = item.quality,
            itemBaseId = item.baseId,
            itemMaterialId = item.materialId,
            itemAffixIds = item.affixes.map(AffixDef::id),
        )
    }

    fun equip(
        world: World,
        entity: EntityId,
        itemIndex: Int,
    ): InventoryOperationResult {
        val inventory = inventoryOf(world, entity)
        val itemId = inventory.itemIds.getOrNull(itemIndex)
            ?: return InventoryOperationResult.Failure(
                code = InventoryOperationCode.PACK_SLOT_EMPTY,
                message = "That pack slot is empty.",
            )
        val item = requireNotNull(world.get<ItemInstance>(itemId))
        val slot =
            item.slot
                ?: return InventoryOperationResult.Failure(
                    code = InventoryOperationCode.CANNOT_EQUIP,
                    message = "${item.name} cannot be equipped.",
                    itemName = item.name,
                    itemQuality = item.quality,
                    itemBaseId = item.baseId,
                    itemMaterialId = item.materialId,
                    itemAffixIds = item.affixes.map(AffixDef::id),
                )

        equipmentOf(world, entity).slots[slot] = itemId
        return InventoryOperationResult.Success(
            code = InventoryOperationCode.EQUIP,
            message = "You equip ${item.name}.",
            itemId = itemId,
            itemName = item.name,
            itemQuality = item.quality,
            itemBaseId = item.baseId,
            itemMaterialId = item.materialId,
            itemAffixIds = item.affixes.map(AffixDef::id),
            slot = slot,
        )
    }

    fun unequip(
        world: World,
        entity: EntityId,
        slot: EquipSlot,
    ): InventoryOperationResult {
        val itemId = equipmentOf(world, entity).slots.remove(slot)
            ?: return InventoryOperationResult.Failure(
                code = InventoryOperationCode.NOTHING_EQUIPPED,
                message = "Nothing is equipped in $slot.",
                slot = slot,
            )
        val item = requireNotNull(world.get<ItemInstance>(itemId))
        return InventoryOperationResult.Success(
            code = InventoryOperationCode.REMOVE,
            message = "You remove ${item.name}.",
            itemId = itemId,
            itemName = item.name,
            itemQuality = item.quality,
            itemBaseId = item.baseId,
            itemMaterialId = item.materialId,
            itemAffixIds = item.affixes.map(AffixDef::id),
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
            ?: return InventoryOperationResult.Failure(
                code = InventoryOperationCode.PACK_SLOT_EMPTY,
                message = "That pack slot is empty.",
            )
        val item = requireNotNull(world.get<ItemInstance>(itemId))
        val effect =
            item.effect
                ?: return InventoryOperationResult.Failure(
                    code = InventoryOperationCode.NOT_CONSUMABLE,
                    message = "${item.name} is not consumable.",
                    itemName = item.name,
                    itemQuality = item.quality,
                    itemBaseId = item.baseId,
                    itemMaterialId = item.materialId,
                    itemAffixIds = item.affixes.map(AffixDef::id),
                )

        when (effect) {
            ConsumableEffect.HEAL -> {
                val health = requireNotNull(world.get<Health>(entity)) { "Missing Health for $entity" }
                health.current = (health.current + item.magnitude).coerceAtMost(health.max)
            }

            ConsumableEffect.TELEPORT -> {
                val destination =
                    teleportDestination
                        ?: return InventoryOperationResult.Failure(
                            code = InventoryOperationCode.NO_TELEPORT_DESTINATION,
                            message = "No teleport destination is available.",
                        )
                val position = requireNotNull(world.get<Position>(entity)) { "Missing Position for $entity" }
                position.moveTo(destination)
            }

            ConsumableEffect.RESTORE_RESOURCE -> {
                val resourceTypeId =
                    item.resourceTypeId
                        ?: return InventoryOperationResult.Failure(
                            code = InventoryOperationCode.NO_RESOURCE_POOL,
                            message = "${item.name} has no resource type configured.",
                            itemName = item.name,
                            itemQuality = item.quality,
                            itemBaseId = item.baseId,
                            itemMaterialId = item.materialId,
                            itemAffixIds = item.affixes.map(AffixDef::id),
                        )
                val resourceType = ResourceType.fromId(resourceTypeId)
                val pool =
                    world.get<ResourcePools>(entity)?.pool(resourceType)
                        ?: return InventoryOperationResult.Failure(
                            code = InventoryOperationCode.NO_RESOURCE_POOL,
                            message = "No ${resourceType.name} pool is available.",
                            itemName = item.name,
                            itemQuality = item.quality,
                            itemBaseId = item.baseId,
                            itemMaterialId = item.materialId,
                            itemAffixIds = item.affixes.map(AffixDef::id),
                        )
                pool.restore(item.magnitude)
            }
        }

        equippedSlotOf(world, entity, itemId)?.let { slot ->
            equipmentOf(world, entity).slots.remove(slot)
        }
        inventory.itemIds.removeAt(itemIndex)
        world.destroyEntity(itemId)
        return InventoryOperationResult.Success(
            code =
                when (effect) {
                    ConsumableEffect.HEAL -> InventoryOperationCode.CONSUME_USE
                    ConsumableEffect.TELEPORT -> InventoryOperationCode.CONSUME_READ
                    ConsumableEffect.RESTORE_RESOURCE -> InventoryOperationCode.CONSUME_USE
                },
            message =
                when (effect) {
                    ConsumableEffect.HEAL -> "You use ${item.name}."
                    ConsumableEffect.TELEPORT -> "You read ${item.name}."
                    ConsumableEffect.RESTORE_RESOURCE -> "You use ${item.name}."
                },
            itemId = itemId,
            itemName = item.name,
            itemQuality = item.quality,
            itemBaseId = item.baseId,
            itemMaterialId = item.materialId,
            itemAffixIds = item.affixes.map(AffixDef::id),
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
