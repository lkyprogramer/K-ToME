package com.ktome.game

import com.ktome.game.elites.BossVariantSelectionMode

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
    val playerRaceId: String = FOUNDATION_RACE_ID,
    val zoneRoute: List<String> = listOf(zoneId),
    val routeIndex: Int = 0,
    val bossVariantSelectionMode: BossVariantSelectionMode = BossVariantSelectionMode.AUTO,
    val preferredBossVariantId: String? = null,
)
