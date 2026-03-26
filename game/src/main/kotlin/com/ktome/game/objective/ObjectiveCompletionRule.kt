package com.ktome.game.objective

internal enum class ObjectiveCompletionRule {
    DEFEAT_ZONE_BOSS,
    EXPLORE_FLOOR_PAIR,
    SECURE_FORGE_PATH,
    ;

    companion object {
        fun fromSchemaId(id: String): ObjectiveCompletionRule =
            when (id) {
                "defeat_zone_boss" -> DEFEAT_ZONE_BOSS
                "explore_floor_pair" -> EXPLORE_FLOOR_PAIR
                "secure_forge_path" -> SECURE_FORGE_PATH
                else -> error("Unknown objective completionRule '$id'.")
            }
    }
}

internal enum class ObjectiveCompletionTrigger {
    PROGRESS_RECORDED,
    ZONE_EXIT,
    BOSS_DEFEAT,
}
