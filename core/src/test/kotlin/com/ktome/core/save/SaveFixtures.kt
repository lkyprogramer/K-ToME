package com.ktome.core.save

import com.ktome.core.item.EquipSlot
import com.ktome.core.loot.RarityTier
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.profile.MilestoneRewardSummary
import com.ktome.core.resource.ResourcePoolSnapshot

internal object SaveFixtures {
    fun emptyScene(): SaveSnapshot =
        SaveSnapshot(
            timestampEpochMillis = 100L,
            worldSeed = 20260313L,
            currentZoneId = "foundation_dungeon",
            floorIndex = 1,
            mapWidth = 5,
            mapHeight = 5,
            fovRadius = 8,
            messageLogSize = 8,
            playerProfessionId = "foundation_hero",
            playerRaceId = "human",
            maxFloor = 3,
            turnCount = 0,
            player =
                PlayerSnapshot(
                    entity =
                        EntitySnapshot(
                            id = 1,
                            position = PointSnapshot(1, 1),
                            blocksMovement = true,
                            faction = "PLAYER",
                            stats = StatsSnapshot(str = 10, dex = 10, con = 10, wil = 10),
                            combatProfile = CombatProfileSnapshot(baseAttack = 5, baseDefense = 2, baseHp = 50, baseStamina = 40),
                            healthCurrent = 50,
                            energyCurrent = 0,
                            experience = ExperienceSnapshot(),
                            inventory = InventorySnapshot(capacity = 12),
                            equipment = EquipmentSnapshot(),
                            cooldowns = emptyMap(),
                            effects = emptyList(),
                            talentLoadout =
                                TalentLoadoutSnapshot(
                                    slotToTalentId = mapOf(1 to "power_strike"),
                                    talentLevels = mapOf("power_strike" to 1),
                                ),
                            resourcePools = listOf(ResourcePoolSnapshot(type = "STAMINA", current = 40, max = 40)),
                            isPlayerControlled = true,
                        ),
                ),
            floors =
                listOf(
                    FloorSnapshot(
                        floorIndex = 1,
                        map = MapSnapshot(rows = List(5) { "....." }, playerStart = PointSnapshot(1, 1)),
                        stairsDown = PointSnapshot(4, 4),
                        exploredTiles = listOf(PointSnapshot(1, 1)),
                        entities =
                            listOf(
                                EntitySnapshot(
                                    id = 9,
                                    position = PointSnapshot(4, 4),
                                    stair = StairSnapshot(direction = "DOWN"),
                                ),
                            ),
                    ),
                ),
        )

    fun activeCombatScene(): SaveSnapshot =
        emptyScene().copy(
            timestampEpochMillis = 200L,
            floorIndex = 2,
            maxFloor = 5,
            turnCount = 41,
            combatRandomState = 991L,
            sessionRandomState = 777L,
            pendingActionIds = listOf(1, 12),
            activeTurnActorId = 12,
            floors =
                listOf(
                    FloorSnapshot(
                        floorIndex = 2,
                        map = MapSnapshot(rows = listOf(".....", ".....", "..#..", ".....", "....."), playerStart = PointSnapshot(1, 1)),
                        stairsUp = PointSnapshot(0, 0),
                        stairsDown = PointSnapshot(4, 4),
                        exploredTiles = listOf(PointSnapshot(1, 1), PointSnapshot(2, 1), PointSnapshot(3, 1)),
                        entities =
                            listOf(
                                EntitySnapshot(
                                    id = 9,
                                    position = PointSnapshot(4, 4),
                                    stair = StairSnapshot(direction = "DOWN"),
                                ),
                                EntitySnapshot(
                                    id = 12,
                                    position = PointSnapshot(2, 1),
                                    blocksMovement = true,
                                    faction = "MONSTER",
                                    stats = StatsSnapshot(str = 8, dex = 9, con = 8, wil = 6),
                                    combatProfile = CombatProfileSnapshot(baseAttack = 4, baseDefense = 1, baseHp = 18, baseStamina = 0),
                                    healthCurrent = 7,
                                    energyCurrent = 60,
                                    experienceReward = 18,
                                    aiBehavior = AIBehaviorSnapshot(type = "CHASE", sightRadius = 8),
                                    monsterTemplateId = "goblin_skirmisher",
                                ),
                            ),
                    ),
                ),
        )

    fun resourceHeavyScene(): SaveSnapshot =
        emptyScene().copy(
            timestampEpochMillis = 300L,
            turnCount = 88,
            combatRandomState = 512L,
            sessionRandomState = 4096L,
            pendingActionIds = listOf(1),
            activeTurnActorId = 1,
            milestoneRewards =
                listOf(
                    MilestoneRewardSummary(
                        rewardSource = MilestoneRewardSource.ROUTE,
                        sourceId = "route.greenwood_fringe.deep_iron_pit",
                        zoneId = "greenwood_fringe",
                        baseItemId = "forgebreaker_pick",
                        equipSlot = EquipSlot.WEAPON,
                        qualityTier = RarityTier.MAGIC,
                        buildHashAtGrant = "vanguard#human#grant",
                        affixIds = listOf("sharp"),
                        equippedBaseItemIdBeforeReward = "short_sword",
                    ),
                ),
            player =
                PlayerSnapshot(
                    entity =
                        emptyScene().player.entity.copy(
                            energyCurrent = 100,
                            resourcePools = listOf(ResourcePoolSnapshot(type = "STAMINA", current = 23, max = 40)),
                            inventory = InventorySnapshot(capacity = 12, itemIds = listOf(21)),
                            equipment = EquipmentSnapshot(slots = mapOf("WEAPON" to 21)),
                            cooldowns = mapOf("power_strike" to 2),
                            effects =
                                listOf(
                                    ActiveEffectSnapshot(
                                        id = "checkpoint_buff",
                                        type = "WAR_CRY_BUFF",
                                        remainingTurns = 2,
                                        statModifiers = StatModifierSnapshot(attack = 1),
                                        skipNextDecay = true,
                                    ),
                                ),
                        ),
                    carriedEntities =
                        listOf(
                            EntitySnapshot(
                                id = 21,
                                itemState =
                                    ItemSnapshot(
                                        baseId = "short_sword",
                                        type = "WEAPON",
                                        slot = "WEAPON",
                                        quality = "MAGIC",
                                        materialId = "steel",
                                        affixIds = listOf("sharp"),
                                        stats = StatModifierSnapshot(attack = 3),
                                    ),
                            ),
                        ),
                ),
        )
}
