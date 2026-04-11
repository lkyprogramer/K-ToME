package com.ktome.game

internal object RiverCrystalRuntimeKeys {
    object River {
        const val QUEST_ID: String = "quest.underground_river"
        const val OBJECTIVE_ID: String = "crossing"
        const val INTERACTABLE_ID: String = "river_ferry_anchor"
        const val CACHE_INTERACTABLE_ID: String = "crystal_cache_chest"
        const val SOURCE_ABILITY_ID: String = "zone.currents"
        const val PROGRESS_TOKEN: String = "underground_river.ferry_anchor_secured"
        const val PROGRESS_STEP_KEY: String = "objective.underground_river_crossing.step.ferry_anchor_secured"
        const val CACHE_PROGRESS_TOKEN: String = "underground_river.crystal_cache_opened"
        const val CACHE_PROGRESS_STEP_KEY: String = "objective.underground_river_crossing.step.crystal_cache_opened"
        const val DRAG_LOG_KEY: String = "log.zone.currents.drag"
        const val SAFE_LANE_LOG_KEY: String = "log.zone.currents.safe_lane"
    }

    object Crystal {
        const val QUEST_ID: String = "quest.crystal_cavern"
        const val OBJECTIVE_ID: String = "resonance"
        const val INTERACTABLE_ID: String = "crystal_resonance_node"
        const val SOURCE_ABILITY_ID: String = "zone.crystal_shards"
        const val PROGRESS_TOKEN: String = "crystal_cavern.node_attuned"
        const val PROGRESS_STEP_KEY: String = "objective.crystal_cavern_resonance.step.node_attuned"
        const val TELEGRAPH_LOG_KEY: String = "log.zone.crystal_shards.telegraph"
        const val ACTIVE_LOG_KEY: String = "log.zone.crystal_shards.active"
        const val SETTLED_LOG_KEY: String = "log.zone.crystal_shards.settled"
    }
}
