package com.ktome.game

internal object AbyssalRuntimeKeys {
    object Temple {
        const val QUEST_ID: String = "quest.abyssal_temple"
        const val OBJECTIVE_ID: String = "sanctum"
        const val INTERACTABLE_ID: String = "temple_ward_reliquary"
        const val SOURCE_ABILITY_ID: String = "zone.void_pressure"
        const val WARD_STATUS_ID: String = "zone.void_pressure.ward_protection"
        const val PROGRESS_TOKEN: String = "abyssal_temple.ward_reliquary_claimed"
        const val PROGRESS_STEP_KEY: String = "objective.abyssal_temple_sanctum.step.ward_reliquary_claimed"
        const val TELEGRAPH_LOG_KEY: String = "log.zone.void_pressure.telegraph"
        const val ACTIVE_LOG_KEY: String = "log.zone.void_pressure.active"
        const val STABILIZED_LOG_KEY: String = "log.zone.void_pressure.stabilized"
    }

    object Finale {
        const val QUEST_ID: String = "quest.abyssal_heart"
        const val OBJECTIVE_ID: String = "heart"
        const val INTERACTABLE_ID: String = "heart_ward_focus"
        const val SOURCE_ABILITY_ID: String = "zone.void_eruption"
        const val WARD_STATUS_ID: String = "zone.void_eruption.ward_protection"
        const val PROGRESS_TOKEN: String = "abyssal_heart.ward_stabilized"
        const val PROGRESS_STEP_KEY: String = "objective.abyssal_heart_finale.step.ward_stabilized"
        const val TELEGRAPH_LOG_KEY: String = "log.zone.void_eruption.telegraph"
        const val ACTIVE_LOG_KEY: String = "log.zone.void_eruption.active"
        const val STABILIZED_LOG_KEY: String = "log.zone.void_eruption.stabilized"
    }

    val WARD_STATUS_IDS: Set<String> =
        setOf(
            Temple.WARD_STATUS_ID,
            Finale.WARD_STATUS_ID,
        )
}
