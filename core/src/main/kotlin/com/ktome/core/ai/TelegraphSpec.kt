package com.ktome.core.ai

enum class TelegraphShape {
    SINGLE_TILE,
    LINE,
    CIRCLE,
    CONE,
}

enum class DangerLevel(
    val level: Int,
) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
}

enum class TelegraphPattern {
    SINGLE_TARGET,
    LINE,
    AURA,
}

data class TelegraphSpec(
    val id: String,
    val shape: TelegraphShape,
    val previewTurns: Int,
    val dangerLevel: DangerLevel,
    val pattern: TelegraphPattern,
)
