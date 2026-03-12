package com.ktome.game

import com.ktome.core.ecs.EntityId
import com.ktome.core.map.Point

sealed interface PlayerCommand {
    data class Move(val delta: Point) : PlayerCommand

    data object Wait : PlayerCommand
}

data class ActorView(
    val entityId: EntityId,
    val position: Point,
    val glyph: Char,
    val colorHex: String,
    val name: String,
    val isPlayer: Boolean,
)

data class PlayerStatus(
    val currentHp: Int,
    val maxHp: Int,
    val level: Int,
    val currentExperience: Int,
    val nextLevelRequirement: Int,
    val statPoints: Int,
    val talentPoints: Int,
)
