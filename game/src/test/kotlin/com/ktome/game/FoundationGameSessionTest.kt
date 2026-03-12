package com.ktome.game

import com.ktome.core.combat.CombatResolver
import com.ktome.core.ecs.AIType
import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.Experience
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemInstance
import com.ktome.core.item.ItemType
import com.ktome.core.item.StatModifier
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.random.RandomSource
import com.ktome.core.talent.TalentRegistry
import com.ktome.core.talent.TalentResolver
import com.ktome.core.talent.StatusEffectType
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.EntityFactory
import com.ktome.game.factory.ItemFactory
import com.ktome.game.model.MonsterTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FoundationGameSessionTest {
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

        assertTrue(consumed)
        assertFalse(world.isAlive(monsterId))
        assertEquals(2, requireNotNull(world.get<Experience>(playerId)).level)
        assertEquals(2, requireNotNull(world.get<Experience>(playerId)).unspentStatPoints)
        assertEquals(requireNotNull(world.get<Health>(playerId)).max, requireNotNull(world.get<Health>(playerId)).current)
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

        assertFalse(consumed)
        assertEquals(Point(1, 1), session.playerPosition())
        assertTrue(world.isAlive(monsterId))
        assertEquals(10, requireNotNull(world.get<Health>(monsterId)).current)
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

        assertTrue(consumed)
        val effect =
            requireNotNull(world.get<com.ktome.core.talent.EffectTracker>(playerId))
                .effects
                .single { it.type == StatusEffectType.WAR_CRY_BUFF }
        assertEquals(5, effect.remainingTurns)
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
}
