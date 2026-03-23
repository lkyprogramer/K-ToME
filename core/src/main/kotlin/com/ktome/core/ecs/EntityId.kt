package com.ktome.core.ecs

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class EntityId(val value: Int)
