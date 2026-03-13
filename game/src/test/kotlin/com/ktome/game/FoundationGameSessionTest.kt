package com.ktome.game

import com.ktome.core.combat.CombatResolver
import com.ktome.core.ecs.AIBehavior
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.MonsterTemplateId
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stair
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.ecs.remove
import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemType
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.random.RandomSource
import com.ktome.core.save.SaveManager
import com.ktome.core.talent.ActiveEffect
import com.ktome.core.talent.EffectTracker
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.talent.TalentResolver
import com.ktome.core.talent.StatusEffectType
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import com.ktome.game.model.MonsterTemplate
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FoundationGameSessionTest {
    @TempDir
    lateinit var tempDir: Path

    private val dataLoader = DataLoader()
    private val talents = dataLoader.loadTalentDefinitions()
    private val talentRegistry = TalentRegistry().apply { registerAll(talents) }

    @Test
    fun `player kill grants experience and level up`() {
        val map = GameMap.fromAscii(
            rows = listOf(
                ".....",
                ".....",
                ".....",
            ),
            playerStart = Point(1, 1),
        )
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        val monsterId = factory.createMonster(
            world = world,
            template = MonsterTemplate(
                id = "training_dummy",
                name = "Training Dummy",
                glyph = 'd',
                colorHex = "#AAAAAA",
                stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 0, wil = 0),
                baseHp = 1,
                baseAttack = 1,
                baseDefense = 0,
                speed = 90,
                ai = AIType.CHASE,
                expReward = 180,
                spawnFloors = listOf(1),
                spawnWeight = 1,
            ),
            position = Point(2, 1),
        )

        val session = FoundationGameSession(
            config = FoundationGameConfig(width = 5, height = 3),
            map = map,
            world = world,
            playerId = playerId,
            combatResolver = combatResolver(doubleValue = 0.0, intValue = 2),
            talentRegistry = talentRegistry,
            talentResolver = TalentResolver(talentRegistry, combatResolver(doubleValue = 0.0, intValue = 2)),
            sessionRandom = fixedRandom(0.0, 2),
        )

        val consumed = session.perform(PlayerCommand.Move(Point(1, 0)))
        val runtimeWorld = runtimeWorld(session)

        assertTrue(consumed)
        assertFalse(runtimeWorld.isAlive(monsterId))
        assertEquals(2, requireNotNull(runtimeWorld.get<Experience>(playerId)).level)
        assertEquals(2, requireNotNull(runtimeWorld.get<Experience>(playerId)).unspentStatPoints)
        assertEquals(requireNotNull(runtimeWorld.get<Health>(playerId)).max, requireNotNull(runtimeWorld.get<Health>(playerId)).current)
        assertTrue(session.messageLog().any { it.contains("gain 180 experience") })
        assertTrue(session.messageLog().any { it.contains("advance to level 2") })
    }

    @Test
    fun `invalid long delta cannot trigger melee attack`() {
        val map = GameMap.fromAscii(
            rows = listOf(
                ".....",
                ".....",
                ".....",
            ),
            playerStart = Point(1, 1),
        )
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        val monsterId = factory.createMonster(
            world = world,
            template = MonsterTemplate(
                id = "remote_dummy",
                name = "Remote Dummy",
                glyph = 'd',
                colorHex = "#AAAAAA",
                stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 0, wil = 0),
                baseHp = 10,
                baseAttack = 1,
                baseDefense = 0,
                speed = 90,
                ai = AIType.CHASE,
                expReward = 10,
                spawnFloors = listOf(1),
                spawnWeight = 1,
            ),
            position = Point(3, 1),
        )

        val session = FoundationGameSession(
            config = FoundationGameConfig(width = 5, height = 3),
            map = map,
            world = world,
            playerId = playerId,
            combatResolver = CombatResolver(fixedRandom(0.0, 2)),
            talentRegistry = talentRegistry,
            talentResolver = TalentResolver(talentRegistry, CombatResolver(fixedRandom(0.0, 2))),
            sessionRandom = fixedRandom(0.0, 2),
        )

        val consumed = session.perform(PlayerCommand.Move(Point(2, 0)))
        val runtimeWorld = runtimeWorld(session)

        assertFalse(consumed)
        assertEquals(Point(1, 1), session.playerPosition())
        assertTrue(runtimeWorld.isAlive(monsterId))
        assertEquals(10, requireNotNull(runtimeWorld.get<Health>(monsterId)).current)
    }

    @Test
    fun `single input only consumes one queued player action`() {
        val map = GameMap.fromAscii(
            rows = listOf(
                ".......",
                ".......",
                ".......",
            ),
            playerStart = Point(1, 1),
        )
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        val boostedStats = requireNotNull(world.get<DerivedStats>(playerId)).copy(speed = 200)
        world.add(playerId, boostedStats)

        val session = FoundationGameSession(
            config = FoundationGameConfig(width = 7, height = 3),
            map = map,
            world = world,
            playerId = playerId,
            combatResolver = CombatResolver(fixedRandom(0.0, 2)),
            talentRegistry = talentRegistry,
            talentResolver = TalentResolver(talentRegistry, CombatResolver(fixedRandom(0.0, 2))),
            sessionRandom = fixedRandom(0.0, 2),
        )

        val firstConsumed = session.perform(PlayerCommand.Move(Point(1, 0)))

        assertTrue(firstConsumed)
        assertEquals(Point(2, 1), session.playerPosition())

        val secondConsumed = session.perform(PlayerCommand.Move(Point(1, 0)))

        assertTrue(secondConsumed)
        assertEquals(Point(3, 1), session.playerPosition())
    }

    @Test
    fun `pick up and equip updates inventory state`() {
        val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val world = World()
        val factory = EntityFactory()
        val itemFactory = ItemFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        itemFactory.createGroundItem(
            world = world,
            item =
                ItemInstance(
                    baseId = "short_sword",
                    name = "Steel Short Sword",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#C0C0C0",
                    stats = StatModifier(attack = 5),
                ),
            position = Point(1, 1),
        )

        val session = session(world, map, playerId)

        assertTrue(session.perform(PlayerCommand.PickUp))
        assertEquals(1, session.inventoryItems().size)
        assertTrue(session.perform(PlayerCommand.ActivateInventoryItem(0)))
        assertEquals(EquipSlot.WEAPON, session.inventoryItems().single().equippedSlot)
        assertTrue(session.playerStatus().attack > 25)
    }

    @Test
    fun `using a talent consumes stamina and starts cooldown`() {
        val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        factory.createMonster(
            world = world,
            template =
                MonsterTemplate(
                    id = "dummy",
                    name = "Dummy",
                    glyph = 'd',
                    colorHex = "#AAAAAA",
                    stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 0, wil = 0),
                    baseHp = 40,
                    baseAttack = 1,
                    baseDefense = 0,
                    speed = 90,
                    ai = AIType.CHASE,
                    expReward = 10,
                    spawnFloors = listOf(1),
                    spawnWeight = 1,
                ),
            position = Point(2, 1),
        )
        val session = session(world, map, playerId)

        val consumed = session.perform(PlayerCommand.UseTalent(slot = 1, target = Point(2, 1)))

        assertTrue(consumed)
        assertTrue(session.playerStatus().currentStamina < session.playerStatus().maxStamina)
        assertEquals(2, session.talentSlots().first { it.slot == 1 }.currentCooldown)
    }

    @Test
    fun `self applied war cry keeps full duration after player turn ends`() {
        val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        val session = session(world, map, playerId)

        val consumed = session.perform(PlayerCommand.UseTalent(slot = 4))
        val runtimeWorld = runtimeWorld(session)

        assertTrue(consumed)
        val effect =
            requireNotNull(runtimeWorld.get<com.ktome.core.talent.EffectTracker>(playerId))
                .effects
                .single { it.type == StatusEffectType.WAR_CRY_BUFF }
        assertEquals(5, effect.remainingTurns)
    }

    @Test
    fun `descending stairs advances floor and auto saves`() {
        val saveManager = SaveManager(tempDir.resolve("floor-save"))
        val session = GameModule.newFoundationSession(saveManager = saveManager)

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))

        assertTrue(session.perform(PlayerCommand.Descend))
        assertEquals(2, session.currentFloor())
        assertTrue(saveManager.hasSave())
    }

    @Test
    fun `manual save and continue restore floor and inventory`() {
        val saveManager = SaveManager(tempDir.resolve("load-save"))
        val session = GameModule.newFoundationSession(saveManager = saveManager)
        val world = runtimeWorld(session)
        ItemFactory().createGroundItem(
            world = world,
            item =
                ItemInstance(
                    baseId = "test_blade",
                    name = "Test Blade",
                    type = ItemType.WEAPON,
                    slot = EquipSlot.WEAPON,
                    glyph = ')',
                    colorHex = "#E0E0E0",
                    stats = StatModifier(attack = 3),
                ),
            position = session.playerPosition(),
        )

        assertTrue(session.perform(PlayerCommand.PickUp))
        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
        assertTrue(session.perform(PlayerCommand.Descend))
        assertTrue(session.perform(PlayerCommand.SaveGame))

        val loaded = GameModule.loadFoundationSession(saveManager)

        assertNotNull(loaded)
        assertEquals(2, loaded?.currentFloor())
        assertTrue(loaded?.inventoryItems()?.any { it.name == "Test Blade" } == true)
    }

    @Test
    fun `descending auto save captures post turn state instead of pre commit snapshot`() {
        val saveManager = SaveManager(tempDir.resolve("checkpoint-save"))
        val session = GameModule.newFoundationSession(saveManager = saveManager)
        requireNotNull(runtimeWorld(session).get<EffectTracker>(session.playerId)).effects +=
            ActiveEffect(
                id = "checkpoint_buff",
                name = "Checkpoint Buff",
                type = StatusEffectType.WAR_CRY_BUFF,
                remainingTurns = 3,
                statModifiers = StatModifier(attack = 1),
            )

        movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))

        assertTrue(session.perform(PlayerCommand.Descend))
        val liveEffect =
            requireNotNull(runtimeWorld(session).get<EffectTracker>(session.playerId))
                .effects
                .single { effect -> effect.id == "checkpoint_buff" }
        val loaded = requireNotNull(GameModule.loadFoundationSession(saveManager))
        val restoredEffect =
            requireNotNull(runtimeWorld(loaded).get<EffectTracker>(loaded.playerId))
                .effects
                .single { effect -> effect.id == "checkpoint_buff" }

        assertEquals(2, liveEffect.remainingTurns)
        assertEquals(liveEffect.remainingTurns, restoredEffect.remainingTurns)
        assertEquals(session.currentFloor(), loaded.currentFloor())
        assertEquals(session.playerPosition(), loaded.playerPosition())
    }

    @Test
    fun `save and load preserve future teleport randomness`() {
        val config = FoundationGameConfig(seed = 20260313L)
        val baseline = GameModule.newFoundationSession(config = config, saveManager = SaveManager(tempDir.resolve("teleport-baseline")))
        val persistedSaveManager = SaveManager(tempDir.resolve("teleport-persisted"))
        val persisted = GameModule.newFoundationSession(config = config, saveManager = persistedSaveManager)

        addTeleportScrolls(baseline, count = 2)
        addTeleportScrolls(persisted, count = 2)
        repeat(2) {
            assertTrue(baseline.perform(PlayerCommand.PickUp))
            assertTrue(persisted.perform(PlayerCommand.PickUp))
        }
        assertTrue(baseline.perform(PlayerCommand.ActivateInventoryItem(0)))
        assertTrue(persisted.perform(PlayerCommand.ActivateInventoryItem(0)))
        assertEquals(baseline.playerPosition(), persisted.playerPosition())

        assertTrue(persisted.perform(PlayerCommand.SaveGame))
        val loaded = requireNotNull(GameModule.loadFoundationSession(persistedSaveManager))

        assertTrue(baseline.perform(PlayerCommand.ActivateInventoryItem(0)))
        assertTrue(loaded.perform(PlayerCommand.ActivateInventoryItem(0)))
        assertEquals(baseline.playerPosition(), loaded.playerPosition())
    }

    @Test
    fun `save and load preserve future combat randomness`() {
        val config = FoundationGameConfig(seed = 20260314L)
        val baseline = GameModule.newFoundationSession(config = config, saveManager = SaveManager(tempDir.resolve("combat-baseline")))
        val persistedSaveManager = SaveManager(tempDir.resolve("combat-persisted"))
        val persisted = GameModule.newFoundationSession(config = config, saveManager = persistedSaveManager)

        val baselineDummy = installCombatDummy(baseline)
        val persistedDummy = installCombatDummy(persisted)
        val attackDelta = requireNotNull(runtimeWorld(baseline).get<Position>(baselineDummy)).toPoint() - baseline.playerPosition()

        assertTrue(baseline.perform(PlayerCommand.Move(attackDelta)))
        assertTrue(persisted.perform(PlayerCommand.Move(attackDelta)))
        assertEquals(monsterHp(baseline, baselineDummy), monsterHp(persisted, persistedDummy))

        assertTrue(persisted.perform(PlayerCommand.SaveGame))
        val loaded = requireNotNull(GameModule.loadFoundationSession(persistedSaveManager))
        val loadedDummy = requireNotNull(entityByTemplateId(loaded, "rng_dummy"))
        val loadedAttackDelta = requireNotNull(runtimeWorld(loaded).get<Position>(loadedDummy)).toPoint() - loaded.playerPosition()

        assertTrue(baseline.perform(PlayerCommand.Move(attackDelta)))
        assertTrue(loaded.perform(PlayerCommand.Move(loadedAttackDelta)))
        assertEquals(monsterHp(baseline, baselineDummy), monsterHp(loaded, loadedDummy))
    }

    @Test
    fun `killing floor five boss ends run in victory and clears save`() {
        val saveManager = SaveManager(tempDir.resolve("boss-save"))
        val session = GameModule.newFoundationSession(saveManager = saveManager)

        repeat(4) {
            movePlayerTo(session, stairPoint(session, com.ktome.core.dungeon.StairDirection.DOWN))
            assertTrue(session.perform(PlayerCommand.Descend))
        }
        val world = runtimeWorld(session)
        val bossId =
            world.entitiesWith(MonsterTemplateId::class)
                .single { entityId -> requireNotNull(world.get<MonsterTemplateId>(entityId)).value == "dungeon_lord" }
        requireNotNull(world.get<Health>(bossId)).current = 1
        val bossPosition = requireNotNull(world.get<Position>(bossId)).toPoint()
        val attackOrigin = if (bossPosition.x > 0) Point(bossPosition.x - 1, bossPosition.y) else Point(bossPosition.x + 1, bossPosition.y)
        movePlayerTo(session, attackOrigin)

        assertTrue(session.perform(PlayerCommand.Move(bossPosition - attackOrigin)))
        assertTrue(session.isVictory())
        assertFalse(saveManager.hasSave())
    }

    @Test
    fun `player death deletes existing save`() {
        val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
        val world = World()
        val factory = EntityFactory()
        val playerId = factory.createPlayer(world, Point(1, 1), talents)
        factory.createMonster(
            world = world,
            template =
                MonsterTemplate(
                    id = "killer",
                    name = "Killer",
                    glyph = 'k',
                    colorHex = "#FF0000",
                    stats = com.ktome.core.ecs.Stats(str = 10, dex = 1, con = 1, wil = 1),
                    baseHp = 10,
                    baseAttack = 10,
                    baseDefense = 0,
                    speed = 90,
                    ai = AIType.CHASE,
                    expReward = 0,
                    spawnFloors = listOf(1),
                    spawnWeight = 1,
                ),
            position = Point(2, 1),
        )
        requireNotNull(world.get<Health>(playerId)).current = 1

        val session =
            FoundationGameSession(
                config = FoundationGameConfig(width = 5, height = 3),
                map = map,
                world = world,
                playerId = playerId,
                combatResolver = combatResolver(doubleValue = 0.0, intValue = 2),
                talentRegistry = talentRegistry,
                talentResolver = TalentResolver(talentRegistry, combatResolver(doubleValue = 0.0, intValue = 2)),
                sessionRandom = fixedRandom(0.0, 2),
            )
        val sessionSaveManager = sessionSaveManager(session)

        assertTrue(session.perform(PlayerCommand.SaveGame))
        assertTrue(sessionSaveManager.hasSave())
        repeat(3) {
            if (!session.isGameOver()) {
                assertTrue(session.perform(PlayerCommand.Wait))
            }
        }
        assertTrue(session.isGameOver())
        assertFalse(sessionSaveManager.hasSave())
    }

    private fun fixedRandom(
        doubleValue: Double,
        intValue: Int,
    ): RandomSource =
        object : RandomSource {
            override fun nextDouble(): Double = doubleValue

            override fun nextInt(
                fromInclusive: Int,
                untilExclusive: Int,
            ): Int = intValue
        }

    private fun combatResolver(
        doubleValue: Double,
        intValue: Int,
    ): CombatResolver = CombatResolver(fixedRandom(doubleValue, intValue))

    private fun session(
        world: World,
        map: GameMap,
        playerId: com.ktome.core.ecs.EntityId,
    ): FoundationGameSession {
        val combatResolver = combatResolver(0.0, 2)
        return FoundationGameSession(
            config = FoundationGameConfig(width = map.width, height = map.height),
            map = map,
            world = world,
            playerId = playerId,
            combatResolver = combatResolver,
            talentRegistry = talentRegistry,
            talentResolver = TalentResolver(talentRegistry, combatResolver),
            sessionRandom = fixedRandom(0.0, 2),
        )
    }

    private fun runtimeWorld(session: FoundationGameSession): World {
        val field = FoundationGameSession::class.java.getDeclaredField("world")
        field.isAccessible = true
        return field.get(session) as World
    }

    private fun movePlayerTo(
        session: FoundationGameSession,
        point: Point,
    ) {
        requireNotNull(runtimeWorld(session).get<Position>(session.playerId)).moveTo(point)
    }

    private fun stairPoint(
        session: FoundationGameSession,
        direction: com.ktome.core.dungeon.StairDirection,
    ): Point {
        val world = runtimeWorld(session)
        return world.entitiesWith(Position::class, Stair::class)
            .first { entityId -> requireNotNull(world.get<Stair>(entityId)).direction == direction }
            .let { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
    }

    private fun addTeleportScrolls(
        session: FoundationGameSession,
        count: Int,
    ) {
        val world = runtimeWorld(session)
        val itemFactory = ItemFactory()
        repeat(count) { index ->
            itemFactory.createGroundItem(
                world = world,
                item =
                    ItemInstance(
                        baseId = "teleport_scroll_$index",
                        name = "Teleport Scroll $index",
                        type = ItemType.CONSUMABLE,
                        glyph = '?',
                        colorHex = "#89CFF0",
                        effect = ConsumableEffect.TELEPORT,
                    ),
                position = session.playerPosition(),
            )
        }
    }

    private fun installCombatDummy(session: FoundationGameSession): com.ktome.core.ecs.EntityId {
        val world = runtimeWorld(session)
        world.entitiesWith(MonsterTemplateId::class).forEach(world::destroyEntity)
        val dummyPosition = findOpenAdjacentPoint(session, session.playerPosition())
        val dummyId =
            EntityFactory().createMonster(
                world = world,
                template =
                    MonsterTemplate(
                        id = "rng_dummy",
                        name = "RNG Dummy",
                        glyph = 'd',
                        colorHex = "#AAAAAA",
                        stats = com.ktome.core.ecs.Stats(str = 1, dex = 1, con = 1, wil = 1),
                        baseHp = 200,
                        baseAttack = 1,
                        baseDefense = 0,
                        speed = 90,
                        ai = AIType.CHASE,
                        expReward = 0,
                        spawnFloors = listOf(session.currentFloor()),
                        spawnWeight = 1,
                    ),
                position = dummyPosition,
            )
        world.remove<AIBehavior>(dummyId)
        return dummyId
    }

    private fun monsterHp(
        session: FoundationGameSession,
        entityId: com.ktome.core.ecs.EntityId,
    ): Int = requireNotNull(runtimeWorld(session).get<Health>(entityId)).current

    private fun entityByTemplateId(
        session: FoundationGameSession,
        templateId: String,
    ): com.ktome.core.ecs.EntityId? {
        val world = runtimeWorld(session)
        return world.entitiesWith(MonsterTemplateId::class)
            .firstOrNull { entityId -> requireNotNull(world.get<MonsterTemplateId>(entityId)).value == templateId }
    }

    private fun findOpenAdjacentPoint(
        session: FoundationGameSession,
        center: Point,
    ): Point {
        val world = runtimeWorld(session)
        val occupied =
            world.entitiesWith(Position::class)
                .map { entityId -> requireNotNull(world.get<Position>(entityId)).toPoint() }
                .toSet()

        return Point.ALL_DIRECTIONS
            .map { delta -> center + delta }
            .first { point ->
                session.map.isInBounds(point.x, point.y) &&
                    !session.map[point].blocksMovement &&
                    point !in occupied
            }
    }

    private fun sessionSaveManager(session: FoundationGameSession): SaveManager {
        val field = FoundationGameSession::class.java.getDeclaredField("saveManager")
        field.isAccessible = true
        return field.get(session) as SaveManager
    }
}
