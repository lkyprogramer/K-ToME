package com.ktome.game

const val FOUNDATION_ZONE_ID: String = "shattered_outpost"
const val FOUNDATION_PROFESSION_ID: String = "vanguard"
const val FOUNDATION_BOSS_TEMPLATE_ID: String = "cultist.dungeon_lord"
val FOUNDATION_ZONE_ROUTE: List<String> =
    listOf(
        "shattered_outpost",
        "greenwood_fringe",
        "deep_iron_pit",
        "grey_gate_depths",
    )

data class FoundationGameConfig(
    val width: Int = 80,
    val height: Int = 50,
    val seed: Long = 20260312L,
    val fovRadius: Int = 8,
    val floor: Int = 1,
    val maxFloor: Int = 2,
    val messageLogSize: Int = 8,
    val zoneId: String = FOUNDATION_ZONE_ID,
    val playerProfessionId: String = FOUNDATION_PROFESSION_ID,
    val zoneRoute: List<String> = listOf(zoneId),
    val routeIndex: Int = 0,
)
