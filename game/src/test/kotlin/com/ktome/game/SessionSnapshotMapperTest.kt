package com.ktome.game

import com.ktome.core.dungeon.FloorState
import com.ktome.core.effect.AreaEffectEmitter
import com.ktome.core.effect.WorldEffect
import com.ktome.core.ecs.get
import com.ktome.core.item.EquipmentPassive
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemQuality
import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.mapgen.BspBackedMapgenPipeline
import com.ktome.core.mapgen.GeneratedFloor
import com.ktome.core.mapgen.GeneratedEntrance
import com.ktome.core.mapgen.NodeId
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.RoomInstance
import com.ktome.core.mapgen.RoomShape
import com.ktome.core.mapgen.TerrainTag
import com.ktome.core.mapgen.TopologyEdge
import com.ktome.core.mapgen.TopologyGraph
import com.ktome.core.mapgen.TopologyFingerprinting
import com.ktome.core.mapgen.TopologyNode
import com.ktome.core.mapgen.ZoneMapgenProfile
import com.ktome.core.mapgen.ZoneMapgenProfileResolver
import com.ktome.core.profile.MilestoneRewardSummary
import com.ktome.core.resource.ResourcePoolSnapshot
import com.ktome.core.save.ActiveEffectSnapshot
import com.ktome.core.save.AreaEffectEmitterSnapshot
import com.ktome.core.save.AiTriggerTrackerSnapshot
import com.ktome.core.save.EntitySnapshot
import com.ktome.core.save.EquipmentSnapshot
import com.ktome.core.save.FloorSnapshot
import com.ktome.core.save.FloorRewardStateSnapshot
import com.ktome.core.save.InvalidSaveException
import com.ktome.core.save.InventorySnapshot
import com.ktome.core.save.ItemSnapshot
import com.ktome.core.save.MapSnapshot
import com.ktome.core.save.PlayerSnapshot
import com.ktome.core.save.PointSnapshot
import com.ktome.core.save.SaveSnapshot
import com.ktome.core.save.StatModifierSnapshot
import com.ktome.core.save.TalentLoadoutSnapshot
import com.ktome.core.save.WorldEffectSnapshot
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.world.solvability.ContentRef
import com.ktome.core.world.solvability.DiscoveryPredicate
import com.ktome.core.world.solvability.DiscoveryPredicateType
import com.ktome.core.world.solvability.DiscoveryRule
import com.ktome.core.world.solvability.NodeAnchorId
import com.ktome.core.world.solvability.RegistryId
import com.ktome.core.world.solvability.SearchActionResult
import com.ktome.core.world.solvability.SearchBindingId
import com.ktome.core.world.solvability.SearchStateEntry
import com.ktome.game.data.DataLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionSnapshotMapperTest {
    @Test
    fun `floor snapshot rejects mismatched revealed entrance ids and search state`() {
        assertThrows(IllegalArgumentException::class.java) {
            FloorSnapshot(
                floorIndex = 1,
                zoneId = "greenwood_fringe",
                revealedEntranceIds = setOf(SearchBindingId("search.greenwood.secret_entrance")),
                searchState =
                    listOf(
                        SearchStateEntry(
                            bindingId = SearchBindingId("search.greenwood.secret_entrance"),
                            result = SearchActionResult.FAILED_CHECK,
                        ),
                    ),
                map = MapSnapshot(rows = listOf(".....", ".....", "....."), playerStart = PointSnapshot(1, 1)),
            )
        }
    }

    @Test
    fun `restore world preserves stamina resource pool current`() {
        val content = content()
        val player =
            PlayerSnapshot(
                entity =
                    EntitySnapshot(
                        id = 1,
                        position = PointSnapshot(1, 1),
                        stats = com.ktome.core.save.StatsSnapshot(str = 10, dex = 10, con = 10, wil = 10),
                        combatProfile =
                            com.ktome.core.save.CombatProfileSnapshot(
                                baseAttack = 5,
                                baseDefense = 2,
                                baseHp = 50,
                                baseStamina = 40,
                            ),
                        resourcePools =
                            listOf(
                                ResourcePoolSnapshot(type = "STAMINA", current = 23, max = 90),
                            ),
                        isPlayerControlled = true,
                    ),
            )
        val floor =
            FloorRuntimeState(
                map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1)),
                stairsDown = Point(4, 1),
                exploredTiles = linkedSetOf(Point(1, 1)),
                entities = mutableListOf(),
            )

        val world = SessionSnapshotMapper.restoreWorld(content, player, floor)
        val restoredPlayer = com.ktome.core.ecs.EntityId(1)
        val pool = requireNotNull(requireNotNull(world.get<com.ktome.core.resource.ResourcePools>(restoredPlayer)).pool(com.ktome.core.resource.ResourceType.STAMINA))

        assertEquals(23, pool.current)
        assertEquals(90, pool.max)
    }

    @Test
    fun `restore world preserves area and world effect carriers`() {
        val content = content()
        val player =
            PlayerSnapshot(
                entity =
                    EntitySnapshot(
                        id = 1,
                        position = PointSnapshot(1, 1),
                        stats = com.ktome.core.save.StatsSnapshot(str = 10, dex = 10, con = 10, wil = 10),
                        combatProfile =
                            com.ktome.core.save.CombatProfileSnapshot(
                                baseAttack = 5,
                                baseDefense = 2,
                                baseHp = 50,
                                baseStamina = 40,
                            ),
                        isPlayerControlled = true,
                    ),
            )
        val floor =
            FloorRuntimeState(
                map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1)),
                entities =
                    mutableListOf(
                        EntitySnapshot(
                            id = 21,
                            areaEffectEmitter =
                                AreaEffectEmitterSnapshot(
                                    emitterId = "poison_cloud",
                                    sourceEntityId = 1,
                                    affectedActorIds = listOf(1),
                                    emitterPriority = 10,
                                    effects = listOf(ActiveEffectSnapshot(id = "poison_area", type = "POISON", remainingTurns = 2)),
                                ),
                        ),
                        EntitySnapshot(
                            id = 22,
                            worldEffect =
                                WorldEffectSnapshot(
                                    effectId = "arena_aura",
                                    affectedActorIds = listOf(1),
                                    worldPriority = 30,
                                    effects = listOf(ActiveEffectSnapshot(id = "burn_world", type = "BURN", remainingTurns = 1)),
                                ),
                        ),
                    ),
            )

        val world = SessionSnapshotMapper.restoreWorld(content, player, floor)
        val restoredArea = requireNotNull(world.get<AreaEffectEmitter>(com.ktome.core.ecs.EntityId(21)))
        val restoredWorld = requireNotNull(world.get<WorldEffect>(com.ktome.core.ecs.EntityId(22)))

        assertEquals("poison_cloud", restoredArea.emitterId)
        assertEquals(listOf(com.ktome.core.ecs.EntityId(1)), restoredArea.affectedActorIds.toList())
        assertEquals("POISON", restoredArea.effects.single().schemaId)
        assertEquals("arena_aura", restoredWorld.effectId)
        assertEquals(listOf(com.ktome.core.ecs.EntityId(1)), restoredWorld.affectedActorIds.toList())
        assertEquals("BURN", restoredWorld.effects.single().schemaId)
    }

    @Test
    fun `to save snapshot canonicalizes collection ordering and preserves semantic ids`() {
        val player =
            PlayerSnapshot(
                entity =
                    EntitySnapshot(
                        id = 1,
                        position = PointSnapshot(1, 1),
                        cooldowns = linkedMapOf("zeta" to 1, "alpha" to 2),
                        effects =
                            listOf(
                                ActiveEffectSnapshot(id = "zeta", type = "war_cry_shaken", remainingTurns = 1),
                                ActiveEffectSnapshot(id = "alpha", type = "war_cry_empower", remainingTurns = 2),
                            ),
                        talentLoadout =
                            TalentLoadoutSnapshot(
                                slotToTalentId = linkedMapOf(2 to "sweep", 1 to "power_strike"),
                                talentLevels = linkedMapOf("zeta" to 2, "alpha" to 1),
                            ),
                        inventory = InventorySnapshot(capacity = 12, itemIds = listOf(31, 21)),
                        equipment = EquipmentSnapshot(slots = linkedMapOf("WEAPON" to 21, "ARMOR" to 22)),
                        isPlayerControlled = true,
                    ),
                carriedEntities =
                    listOf(
                        EntitySnapshot(
                            id = 31,
                            itemState =
                                ItemSnapshot(
                                    baseId = "dagger",
                                    type = "WEAPON",
                                    quality = "NORMAL",
                                    affixIds = listOf("keen", "agile"),
                                    stats = StatModifierSnapshot(attack = 1),
                                ),
                        ),
                        EntitySnapshot(
                            id = 21,
                            itemState =
                                ItemSnapshot(
                                    baseId = "short_sword",
                                    type = "WEAPON",
                                    quality = "MAGIC",
                                    affixIds = listOf("zeta", "alpha"),
                                    stats = StatModifierSnapshot(attack = 2),
                                ),
                        ),
                    ),
            )

        val floors =
            listOf(
                FloorState(
                    floor = 1,
                    stairsUp = null,
                    stairsDown = Point(4, 4),
                    payload =
                        FloorRuntimeState(
                            generatedFloor =
                                GeneratedFloor.compatibility(
                                    zoneId = "greenwood_fringe",
                                    floorIndex = 1,
                                    seed = 2026040101L,
                                    map = GameMap.fromAscii(rows = List(5) { "....." }, playerStart = Point(1, 1)),
                                    terrainTags = mapOf(Point(3, 1) to setOf(TerrainTag.WATER)),
                                ),
                            stairsDown = Point(4, 4),
                            rewardState =
                                FloorRewardStateSnapshot(
                                    meaningfulRewardSeenThisFloor = true,
                                    cadenceRewardGrantedThisFloor = true,
                                ),
                            exploredTiles = linkedSetOf(Point(3, 1), Point(1, 0), Point(1, 1)),
                            entities =
                                mutableListOf(
                                    EntitySnapshot(id = 11, position = PointSnapshot(3, 3), interactableId = "supply_crate"),
                                    EntitySnapshot(id = 9, position = PointSnapshot(4, 4)),
                                    EntitySnapshot(id = 7, position = PointSnapshot(2, 2)),
                                ),
                        ),
                ),
            )
        val milestoneRewards =
            listOf(
                MilestoneRewardSummary(
                    rewardSource = MilestoneRewardSource.ROUTE,
                    sourceId = "route.greenwood_fringe.deep_iron_pit",
                    zoneId = "greenwood_fringe",
                    baseItemId = "forgebreaker_pick",
                    equipSlot = EquipSlot.WEAPON,
                    qualityTier = ItemQuality.MAGIC,
                    buildHashAtGrant = "vanguard#human#grant",
                    affixIds = listOf("alpha"),
                    equippedBaseItemIdBeforeReward = "short_sword",
                ),
            )

        val snapshot =
            SessionSnapshotMapper.toSaveSnapshot(
                config =
                    FoundationGameConfig(
                        width = 5,
                        height = 5,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "vanguard",
                        zoneRoute = FOUNDATION_ZONE_ROUTE,
                        routeIndex = 1,
                    ),
                currentFloor = 1,
                turnCount = 12,
                headlessTurnEquivalent = 21,
                player = player,
                floors = floors,
                combatRandomState = null,
                sessionRandomState = null,
                milestoneRewards = milestoneRewards,
                pendingActionIds = listOf(1, 7),
                activeTurnActorId = 1,
            )

        assertEquals("greenwood_fringe", snapshot.currentZoneId)
        assertEquals(FOUNDATION_ZONE_ROUTE, snapshot.zoneRoute)
        assertEquals(1, snapshot.routeIndex)
        assertEquals("vanguard", snapshot.playerProfessionId)
        assertEquals(listOf(21, 31), snapshot.player.carriedEntities.map(EntitySnapshot::id))
        assertEquals(listOf("ARMOR", "WEAPON"), snapshot.player.entity.equipment?.slots?.keys?.toList())
        assertEquals(listOf("alpha", "zeta"), snapshot.player.entity.cooldowns?.keys?.toList())
        assertEquals(listOf("alpha", "zeta"), snapshot.player.entity.talentLoadout?.talentLevels?.keys?.toList())
        assertEquals(listOf(1, 2), snapshot.player.entity.talentLoadout?.slotToTalentId?.keys?.toList())
        assertEquals(listOf("alpha", "zeta"), snapshot.player.entity.effects?.map(ActiveEffectSnapshot::id))
        assertEquals(listOf("alpha", "zeta"), snapshot.player.carriedEntities.first().itemState?.affixIds)
        assertEquals(listOf(PointSnapshot(1, 0), PointSnapshot(1, 1), PointSnapshot(3, 1)), snapshot.floors.single().exploredTiles)
        assertEquals(
            FloorRewardStateSnapshot(
                meaningfulRewardSeenThisFloor = true,
                cadenceRewardGrantedThisFloor = true,
            ),
            snapshot.currentFloorRewardState,
        )
        assertEquals(snapshot.currentFloorRewardState, snapshot.floors.single().rewardState)
        assertEquals(listOf(7, 9, 11), snapshot.floors.single().entities.map(EntitySnapshot::id))
        assertEquals("supply_crate", snapshot.floors.single().entities.last().interactableId)
        assertEquals("greenwood_fringe", snapshot.floors.single().zoneId)
        assertEquals(2026040101L, snapshot.floors.single().floorSeed)
        assertEquals(
            TopologyFingerprinting.fingerprint(floors.single().payload.generatedFloor.topology),
            snapshot.floors.single().topologyFingerprint,
        )
        assertEquals(
            TopologyFingerprinting.terrainTagHash(floors.single().payload.generatedFloor.terrainTags),
            snapshot.floors.single().terrainTagHash,
        )
        assertEquals(milestoneRewards, snapshot.milestoneRewards)
    }

    @Test
    fun `from save snapshot restores zone and profession ids into config`() {
        val snapshot =
            SaveSnapshot(
                timestampEpochMillis = 1L,
                worldSeed = 20260316L,
                currentZoneId = "greenwood_fringe",
                zoneRoute = FOUNDATION_ZONE_ROUTE,
                routeIndex = 1,
                floorIndex = 2,
                mapWidth = 70,
                mapHeight = 45,
                fovRadius = 8,
                messageLogSize = 8,
                playerProfessionId = "rogue",
                playerRaceId = "human",
                maxFloor = 2,
                turnCount = 10,
                currentFloorRewardState =
                    FloorRewardStateSnapshot(
                        meaningfulRewardSeenThisFloor = true,
                        cadenceRewardGrantedThisFloor = true,
                    ),
                milestoneRewards =
                    listOf(
                        MilestoneRewardSummary(
                            rewardSource = MilestoneRewardSource.CACHE,
                            sourceId = "armory_gate",
                            zoneId = "greenwood_fringe",
                            baseItemId = "seal_reliquary",
                            equipSlot = EquipSlot.OFF_HAND,
                            qualityTier = ItemQuality.RARE,
                            buildHashAtGrant = "rogue#human#grant",
                            affixIds = listOf("shadowed"),
                            equippedBaseItemIdBeforeReward = "basic_shield",
                        ),
                    ),
                player =
                    PlayerSnapshot(
                        entity = EntitySnapshot(id = 1, position = PointSnapshot(1, 1), isPlayerControlled = true),
                    ),
                floors =
                    listOf(
                        com.ktome.core.save.FloorSnapshot(
                            floorIndex = 2,
                            map =
                                com.ktome.core.save.MapSnapshot(
                                    rows = List(45) { ".".repeat(70) },
                                    playerStart = PointSnapshot(1, 1),
                                ),
                        ),
                    ),
            )

        val restored = SessionSnapshotMapper.fromSaveSnapshot(snapshot)

        assertEquals("greenwood_fringe", restored.config.zoneId)
        assertEquals("rogue", restored.config.playerProfessionId)
        assertEquals(FOUNDATION_ZONE_ROUTE, restored.config.zoneRoute)
        assertEquals(1, restored.config.routeIndex)
        assertEquals(10, restored.headlessTurnEquivalent)
        assertEquals(snapshot.milestoneRewards, restored.milestoneRewards)
        assertEquals(snapshot.currentFloorRewardState, restored.floors.single().payload.rewardState)
    }

    @Test
    fun `from save snapshot regenerates generated floor metadata when phase4 metadata is present`() {
        val resolver =
            object : ZoneMapgenProfileResolver {
                override fun resolve(zoneId: String): ZoneMapgenProfile =
                    ZoneMapgenProfile(
                        id = "$zoneId.test",
                        zoneId = zoneId,
                        allowedBiomeFamilies = setOf("family.test"),
                        loopCountRange = 0..0,
                        vaultPool = emptySet(),
                        terrainTagWeights = mapOf(TerrainTag.WATER to 1.0f),
                        roomTagFilter = setOf("test_room"),
                    )
            }
        val pipeline = BspBackedMapgenPipeline(profileResolver = resolver)
        val generatedFloor = pipeline.run(com.ktome.core.mapgen.MapgenRequest(zoneId = "greenwood_fringe", floorIndex = 1, seed = 2026040102L, targetWidth = 32, targetHeight = 24))
        val player =
            PlayerSnapshot(
                entity = EntitySnapshot(id = 1, position = PointSnapshot.from(generatedFloor.map.playerStart), isPlayerControlled = true),
            )
        val floors =
            listOf(
                FloorState(
                    floor = 1,
                    payload =
                        FloorRuntimeState(
                            generatedFloor = generatedFloor,
                            entities = mutableListOf(),
                        ),
                ),
            )

        val snapshot =
            SessionSnapshotMapper.toSaveSnapshot(
                config =
                    FoundationGameConfig(
                        width = 32,
                        height = 24,
                        zoneId = "greenwood_fringe",
                        playerProfessionId = "vanguard",
                    ),
                currentFloor = 1,
                turnCount = 5,
                headlessTurnEquivalent = 5,
                player = player,
                floors = floors,
                combatRandomState = null,
                sessionRandomState = null,
                pendingActionIds = listOf(1),
                activeTurnActorId = 1,
            )

        val restored = SessionSnapshotMapper.fromSaveSnapshot(snapshot, mapgenPipeline = pipeline)

        assertEquals(generatedFloor.seed, restored.floors.single().payload.generatedFloor.seed)
        assertEquals(
            TopologyFingerprinting.fingerprint(generatedFloor.topology),
            restored.floors.single().payload.topologyFingerprint,
        )
        assertEquals(
            TopologyFingerprinting.terrainTagHash(generatedFloor.terrainTags),
            restored.floors.single().payload.terrainTagHash,
        )
    }

    @Test
    fun `from save snapshot wraps regeneration failures as invalid save`() {
        val savedMap = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val generatedFloor =
            GeneratedFloor.compatibility(
                zoneId = "broken_zone",
                floorIndex = 1,
                seed = 42L,
                map = savedMap,
            )
        val snapshot = phase4Snapshot(generatedFloor = generatedFloor)
        val pipeline =
            object : com.ktome.core.mapgen.MapgenPipeline {
                override fun run(request: com.ktome.core.mapgen.MapgenRequest): GeneratedFloor {
                    throw IllegalArgumentException("unknown zone")
                }
            }

        val exception =
            assertThrows(InvalidSaveException::class.java) {
                SessionSnapshotMapper.fromSaveSnapshot(snapshot, mapgenPipeline = pipeline)
            }

        assertTrue(requireNotNull(exception.message).contains("broken_zone#1"))
        assertNotNull(exception.cause)
        assertEquals("unknown zone", exception.cause?.message)
    }

    @Test
    fun `from save snapshot rejects regenerated player start mismatch`() {
        val savedMap = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val generatedFloor =
            GeneratedFloor.compatibility(
                zoneId = "greenwood_fringe",
                floorIndex = 1,
                seed = 99L,
                map = savedMap,
            )
        val snapshot = phase4Snapshot(generatedFloor = generatedFloor)
        val pipeline =
            object : com.ktome.core.mapgen.MapgenPipeline {
                override fun run(request: com.ktome.core.mapgen.MapgenRequest): GeneratedFloor =
                    GeneratedFloor.compatibility(
                        zoneId = request.zoneId,
                        floorIndex = request.floorIndex,
                        seed = request.seed,
                        map = GameMap.fromAscii(rows = savedMap.asGlyphRows(), playerStart = Point(2, 1)),
                    )
            }

        val exception =
            assertThrows(InvalidSaveException::class.java) {
                SessionSnapshotMapper.fromSaveSnapshot(snapshot, mapgenPipeline = pipeline)
            }

        assertTrue(requireNotNull(exception.message).contains("player-start"))
    }

    @Test
    fun `from save snapshot rejects hidden entrance binding drift`() {
        val generatedFloor = phase4HiddenEntranceFloor(bindingId = SearchBindingId("search.greenwood.hidden_cache"))
        val snapshot = phase4Snapshot(generatedFloor = generatedFloor)
        val pipeline =
            object : com.ktome.core.mapgen.MapgenPipeline {
                override fun run(request: com.ktome.core.mapgen.MapgenRequest): GeneratedFloor =
                    phase4HiddenEntranceFloor(
                        bindingId = SearchBindingId("search.greenwood.hidden_cache_v2"),
                        zoneId = request.zoneId,
                        floorIndex = request.floorIndex,
                        seed = request.seed,
                    )
            }

        val exception =
            assertThrows(InvalidSaveException::class.java) {
                SessionSnapshotMapper.fromSaveSnapshot(snapshot, mapgenPipeline = pipeline)
            }

        assertTrue(requireNotNull(exception.message).contains("hidden-entrance binding"))
    }

    @Test
    fun `restore item instance derives passive from base item schema instead of save payload`() {
        val content = content()
        val player =
            PlayerSnapshot(
                entity =
                    EntitySnapshot(
                        id = 1,
                        position = PointSnapshot(1, 1),
                        inventory = InventorySnapshot(capacity = 12, itemIds = listOf(21)),
                        isPlayerControlled = true,
                    ),
                carriedEntities =
                    listOf(
                        EntitySnapshot(
                            id = 21,
                            itemState =
                                ItemSnapshot(
                                    baseId = "bandit_trophy",
                                    type = "ARMOR",
                                    slot = "OFF_HAND",
                                    quality = "COMMON",
                                    stats = StatModifierSnapshot(dex = 1, accuracy = 1),
                                ),
                        ),
                    ),
            )
        val floor =
            FloorRuntimeState(
                map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1)),
                stairsDown = Point(4, 1),
                exploredTiles = linkedSetOf(Point(1, 1)),
                entities = mutableListOf(),
            )

        val world = SessionSnapshotMapper.restoreWorld(content, player, floor)
        val itemId = requireNotNull(world.get<com.ktome.core.item.Inventory>(com.ktome.core.ecs.EntityId(1))).itemIds.single()
        val restored = requireNotNull(world.get<com.ktome.core.item.ItemInstance>(itemId))

        assertTrue(restored.passive is EquipmentPassive.DamageVsTag)
        assertEquals("bandit", (restored.passive as EquipmentPassive.DamageVsTag).tag)
        assertEquals(0.15, (restored.passive as EquipmentPassive.DamageVsTag).bonusPercent, 0.0001)
    }

    @Test
    fun `restore world preserves ai trigger tracker state`() {
        val content = content()
        val player = PlayerSnapshot(entity = EntitySnapshot(id = 1, position = PointSnapshot(1, 1), isPlayerControlled = true))
        val floor =
            FloorRuntimeState(
                map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1)),
                exploredTiles = linkedSetOf(Point(1, 1)),
                entities =
                    mutableListOf(
                        EntitySnapshot(
                            id = 7,
                            position = PointSnapshot(3, 1),
                            monsterTemplateId = "cultist.dungeon_lord",
                            aiTriggerTracker =
                                AiTriggerTrackerSnapshot(
                                    consumedTriggerIds = listOf("dungeon_lord_opening_war_cry"),
                                    engagedInCombat = true,
                                ),
                        ),
                    ),
            )

        val world = SessionSnapshotMapper.restoreWorld(content, player, floor)
        val tracker = requireNotNull(world.get<com.ktome.core.ecs.AiTriggerTracker>(com.ktome.core.ecs.EntityId(7)))

        assertTrue(tracker.engagedInCombat)
        assertEquals(setOf("dungeon_lord_opening_war_cry"), tracker.consumedTriggerIds)
    }

    private fun content(): GameContent {
        val loader = DataLoader()
        val schemaCatalog = loader.loadSchemaCatalog()
        val talents = loader.loadTalentDefinitions()
        return GameContent(
            talents = talents,
            statuses = schemaCatalog.statuses,
            statusCatalog = loader.loadStatusCatalog(),
            talentRegistry = TalentRegistry().apply { registerAll(talents) },
            monsterCatalog = loader.loadMonsterCatalog().monsters,
            itemBundle = loader.loadItemBundle(),
            bossDefinitions = loader.loadBossDefinitions(),
            schemaCatalog = schemaCatalog,
            localizer = loader.localizer,
        )
    }

    private fun phase4Snapshot(generatedFloor: GeneratedFloor): SaveSnapshot {
        val player =
            PlayerSnapshot(
                entity = EntitySnapshot(id = 1, position = PointSnapshot.from(generatedFloor.map.playerStart), isPlayerControlled = true),
            )
        val floors =
            listOf(
                FloorState(
                    floor = generatedFloor.floorIndex,
                    payload =
                        FloorRuntimeState(
                            generatedFloor = generatedFloor,
                            entities = mutableListOf(),
                        ),
                ),
            )

        return SessionSnapshotMapper.toSaveSnapshot(
            config =
                FoundationGameConfig(
                    width = generatedFloor.map.width,
                    height = generatedFloor.map.height,
                    zoneId = generatedFloor.zoneId,
                    playerProfessionId = "vanguard",
                ),
            currentFloor = generatedFloor.floorIndex,
            turnCount = 1,
            headlessTurnEquivalent = 1,
            player = player,
            floors = floors,
            combatRandomState = null,
            sessionRandomState = null,
            pendingActionIds = listOf(1),
            activeTurnActorId = 1,
        )
    }

    private fun phase4HiddenEntranceFloor(
        bindingId: SearchBindingId,
        zoneId: String = "greenwood_fringe",
        floorIndex: Int = 1,
        seed: Long = 42L,
    ): GeneratedFloor {
        val map = GameMap.fromAscii(rows = List(5) { "....." }, playerStart = Point(2, 2))
        val startNodeId = NodeId("start")
        val secretNodeId = NodeId("secret")
        val entranceAnchorId = NodeAnchorId("critical.start")
        val targetAnchorId = NodeAnchorId("secret.greenwood.hidden_cache")
        val topology =
            TopologyGraph(
                nodes =
                    listOf(
                        TopologyNode(
                            id = startNodeId,
                            anchorId = entranceAnchorId,
                            roomDefId = "room.start",
                            pathClass = PathClass.CRITICAL_PATH,
                            tags = setOf("start"),
                        ),
                        TopologyNode(
                            id = secretNodeId,
                            anchorId = targetAnchorId,
                            roomDefId = "room.secret",
                            pathClass = PathClass.SECRET,
                            tags = setOf("secret"),
                        ),
                    ),
                edges = listOf(TopologyEdge(from = startNodeId, to = secretNodeId)),
                primaryPathNodeIds = listOf(startNodeId),
                optionalLoopCount = 0,
            )
        return GeneratedFloor.compatibility(
            zoneId = zoneId,
            floorIndex = floorIndex,
            seed = seed,
            map = map,
            topology = topology,
            rooms =
                listOf(
                    RoomInstance(
                        nodeId = startNodeId,
                        anchorId = entranceAnchorId,
                        roomDefId = "room.start",
                        x = 1,
                        y = 1,
                        width = 3,
                        height = 3,
                        shape = RoomShape.RECT,
                        pathClass = PathClass.CRITICAL_PATH,
                        tags = setOf("start"),
                    ),
                ),
            entrances =
                listOf(
                    GeneratedEntrance(
                        bindingId = bindingId,
                        fromNodeId = startNodeId,
                        targetNodeId = secretNodeId,
                        entranceAnchorId = entranceAnchorId,
                        targetAnchorId = targetAnchorId,
                        pathClass = PathClass.SECRET,
                        discoveryRule =
                            DiscoveryRule(
                                predicates =
                                    listOf(
                                        DiscoveryPredicate(
                                            type = DiscoveryPredicateType.PERCEPTION_CHECK,
                                            difficulty = 8,
                                        ),
                                    ),
                            ),
                        targetSecretZoneId = ContentRef(registry = RegistryId("secret_zone"), id = "greenwood_hidden_cache_stub"),
                    ),
                ),
        )
    }
}
