package com.ktome.core.ecs

import kotlin.reflect.KClass

class World {
    private val entities = linkedSetOf<EntityId>()
    private val componentStores = mutableMapOf<KClass<*>, MutableMap<EntityId, Any>>()
    private val systems = mutableListOf<RegisteredSystem>()
    private var nextEntityValue = 1
    private var nextSystemOrder = 0

    fun createEntity(): EntityId {
        val entityId = EntityId(nextEntityValue++)
        entities += entityId
        return entityId
    }

    fun destroyEntity(id: EntityId) {
        if (!entities.remove(id)) {
            return
        }

        componentStores.values.forEach { store ->
            store.remove(id)
        }
    }

    fun isAlive(id: EntityId): Boolean = id in entities

    fun <T : Any> addComponent(id: EntityId, component: T) {
        require(isAlive(id)) { "Cannot add a component to a destroyed entity: $id" }
        componentStores.getOrPut(component::class) { linkedMapOf() }[id] = component
    }

    fun <T : Any> removeComponent(id: EntityId, type: KClass<T>) {
        componentStores[type]?.remove(id)
    }

    fun <T : Any> getComponent(id: EntityId, type: KClass<T>): T? {
        val component = componentStores[type]?.get(id) ?: return null
        @Suppress("UNCHECKED_CAST")
        return component as T
    }

    fun <T : Any> hasComponent(id: EntityId, type: KClass<T>): Boolean =
        componentStores[type]?.containsKey(id) == true

    fun entitiesWith(vararg types: KClass<*>): List<EntityId> {
        if (types.isEmpty()) {
            return entities.toList()
        }

        if (types.any { componentStores[it].isNullOrEmpty() }) {
            return emptyList()
        }

        return entities.filter { entityId ->
            types.all { type -> componentStores[type]?.containsKey(entityId) == true }
        }
    }

    fun addSystem(system: GameSystem) {
        systems += RegisteredSystem(nextSystemOrder++, system)
    }

    fun update() {
        systems.sortedWith(compareBy<RegisteredSystem> { it.system.priority }.thenBy { it.order })
            .forEach { registeredSystem ->
                registeredSystem.system.update(this)
            }
    }

    private data class RegisteredSystem(
        val order: Int,
        val system: GameSystem,
    )
}

inline fun <reified T : Any> World.add(id: EntityId, component: T) {
    addComponent(id, component)
}

inline fun <reified T : Any> World.get(id: EntityId): T? = getComponent(id, T::class)

inline fun <reified T : Any> World.has(id: EntityId): Boolean = hasComponent(id, T::class)

inline fun <reified T : Any> World.remove(id: EntityId) {
    removeComponent(id, T::class)
}
