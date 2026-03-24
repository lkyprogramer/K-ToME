package com.ktome.game.harness

import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.ItemType
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.game.InventoryItemView
import com.ktome.game.PlayerCommand
import com.ktome.game.PlayerResourceView
import com.ktome.game.PlayerStatus
import com.ktome.game.TalentSlotView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmokeBotTest {
    private val bot = SmokeBot()
    private val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))
    private val corridorMap = GameMap.fromAscii(rows = listOf("......."), playerStart = Point(3, 0))
    private val longCorridorMap = GameMap.fromAscii(rows = listOf("..........."), playerStart = Point(4, 0))

    @Test
    fun `low hp uses healing consumable`() {
        val observation =
            observation(
                inventoryItems =
                    listOf(
                        InventoryItemView(
                            index = 0,
                            name = "治疗药水",
                            type = ItemType.CONSUMABLE,
                            effect = ConsumableEffect.HEAL,
                        ),
                    ),
            )

        assertEquals(PlayerCommand.ActivateInventoryItem(0), bot.decide(observation))
    }

    @Test
    fun `low hp does not spend mana potion as emergency heal`() {
        val observation =
            observation(
                inventoryItems =
                    listOf(
                        InventoryItemView(
                            index = 0,
                            name = "法力药水",
                            type = ItemType.CONSUMABLE,
                            effect = ConsumableEffect.RESTORE_RESOURCE,
                        ),
                    ),
            )

        assertNull(bot.decide(observation).takeIf { it is PlayerCommand.ActivateInventoryItem })
    }

    @Test
    fun `low hp uses teleport scroll as escape when adjacent hostiles close in`() {
        val observation =
            observation(
                inventoryItems =
                    listOf(
                        InventoryItemView(
                            index = 0,
                            name = "传送卷轴",
                            type = ItemType.CONSUMABLE,
                            effect = ConsumableEffect.TELEPORT,
                        ),
                    ),
                visibleHostilePositions = listOf(Point(2, 1)),
            )

        assertEquals(PlayerCommand.ActivateInventoryItem(0), bot.decide(observation))
    }

    @Test
    fun `low hp heals before interacting with an objective on the same tile`() {
        val observation =
            observation(
                inventoryItems =
                    listOf(
                        InventoryItemView(
                            index = 0,
                            name = "治疗药水",
                            type = ItemType.CONSUMABLE,
                            effect = ConsumableEffect.HEAL,
                        ),
                    ),
                visibleInteractables =
                    listOf(
                        ObservedInteractable(
                            id = "boss_gate",
                            position = Point(1, 1),
                            interactionTags = setOf("objective"),
                        ),
                    ),
            )

        assertEquals(PlayerCommand.ActivateInventoryItem(0), bot.decide(observation))
    }

    @Test
    fun `far visible boss does not override route navigation for mana class`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus =
                    PlayerStatus(
                        currentHp = 30,
                        maxHp = 30,
                        level = 1,
                        currentExperience = 0,
                        nextLevelRequirement = 10,
                        statPoints = 0,
                        talentPoints = 0,
                        attack = 6,
                        defense = 4,
                        accuracy = 5,
                        evasion = 3,
                        speed = 100,
                    ),
                visibleBossPositions = listOf(Point(4, 1)),
                knownDownstairsPositions = listOf(Point(0, 1)),
                exploredTiles = setOf(Point(1, 1), Point(0, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                visibleTiles = setOf(Point(0, 1), Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
            )

        val command = bot.decide(observation)

        assertFalse(command is PlayerCommand.UseTalent, "Healthy arcanist should not spend blink just because a boss is visible.")
    }

    @Test
    fun `healthy mana class does not kite a nearby boss by default`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                visibleHostilePositions = listOf(Point(3, 1)),
                visibleBossPositions = listOf(Point(3, 1)),
                visibleTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1)),
                exploredTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1)),
            )

        val command = bot.decide(observation)
        assertTrue(command is PlayerCommand.Move)
        val destination = observation.playerPosition + (command as PlayerCommand.Move).delta
        assertTrue(
            destination.chebyshevDistanceTo(Point(3, 1)) < observation.playerPosition.chebyshevDistanceTo(Point(3, 1)),
            "Expected nearby boss engagement to reduce distance, but command was $command.",
        )
    }

    @Test
    fun `exploration avoids immediate backtrack when another branch is available`() {
        val routeBot = SmokeBot()

        routeBot.decide(
            observation(
                inventoryItems = emptyList(),
                map = corridorMap,
                playerPosition = Point(2, 0),
                playerStatus = healthyStatus(),
                visibleTiles = setOf(Point(1, 0), Point(2, 0), Point(3, 0)),
                exploredTiles = setOf(Point(1, 0), Point(2, 0), Point(3, 0)),
            ),
        )

        val command =
            routeBot.decide(
                observation(
                    inventoryItems = emptyList(),
                    map = corridorMap,
                    playerPosition = Point(3, 0),
                    playerStatus = healthyStatus(),
                    visibleTiles = setOf(Point(2, 0), Point(3, 0), Point(4, 0)),
                    exploredTiles = setOf(Point(2, 0), Point(3, 0), Point(4, 0)),
                ),
            )

        assertEquals(PlayerCommand.Move(Point(1, 0)), command)
    }

    @Test
    fun `patrol exploration avoids stepping back into recent short cycle when alternatives exist`() {
        val routeBot = SmokeBot()
        val openMap =
            GameMap.fromAscii(
                rows =
                    listOf(
                        "...",
                        "...",
                        "...",
                    ),
                playerStart = Point(1, 1),
            )
        val exploredTiles = openMap.floorPoints().toSet()

        listOf(
            Point(1, 1),
            Point(2, 1),
            Point(2, 2),
            Point(1, 2),
            Point(1, 1),
        ).forEach { position ->
            routeBot.decide(
                observation(
                    inventoryItems = emptyList(),
                    map = openMap,
                    playerPosition = position,
                    playerStatus = healthyStatus(),
                    visibleTiles = exploredTiles,
                    exploredTiles = exploredTiles,
                ),
            )
        }

        val command =
            routeBot.decide(
                observation(
                    inventoryItems = emptyList(),
                    map = openMap,
                    playerPosition = Point(1, 1),
                    playerStatus = healthyStatus(),
                    visibleTiles = exploredTiles,
                    exploredTiles = exploredTiles,
                ),
            )

        assertTrue(command is PlayerCommand.Move)
        val destination = Point(1, 1) + (command as PlayerCommand.Move).delta
        assertFalse(
            destination in setOf(Point(2, 1), Point(2, 2), Point(1, 2)),
            "Expected patrol move to break the recent short cycle, but got $command.",
        )
    }

    @Test
    fun `distant visible loot does not override downstairs navigation`() {
        val command =
            bot.decide(
                observation(
                    inventoryItems = emptyList(),
                    map = longCorridorMap,
                    playerPosition = Point(4, 0),
                    playerStatus = healthyStatus(),
                    visibleTiles = longCorridorMap.floorPoints().toSet(),
                    exploredTiles = longCorridorMap.floorPoints().toSet(),
                    knownDownstairsPositions = listOf(Point(0, 0)),
                    visibleGroundItemPositions = listOf(Point(10, 0)),
                ),
            )

        assertEquals(PlayerCommand.Move(Point(-1, 0)), command)
    }

    @Test
    fun `pending talent draft is confirmed before exploration actions`() {
        val command =
            bot.decide(
                observation(
                    inventoryItems = emptyList(),
                    playerStatus = healthyStatus(),
                    talentSlots =
                        listOf(
                            talentSlot(
                                slot = 1,
                                talentId = "fireball",
                                resourceCost = 8,
                                range = 6,
                                requiresTarget = true,
                            ).copy(
                                level = 2,
                                committedLevel = 1,
                                hasPendingAllocation = true,
                            ),
                        ),
                    knownDownstairsPositions = listOf(Point(0, 1)),
                    visibleTiles = setOf(Point(0, 1), Point(1, 1)),
                    exploredTiles = setOf(Point(0, 1), Point(1, 1)),
                ),
            )

        assertEquals(PlayerCommand.ConfirmTalentDraft, command)
    }

    @Test
    fun `talent upgrade priority can target reserve talents directly`() {
        val command =
            bot.decide(
                observation(
                    inventoryItems = emptyList(),
                    playerStatus = healthyStatus().copy(talentPoints = 1),
                    talentSlots =
                        listOf(
                            talentSlot(slot = 1, talentId = "fireball", resourceCost = 8, range = 6, requiresTarget = true).copy(level = 5, maxLevel = 5),
                        ),
                    reserveTalents =
                        listOf(
                            reserveTalentView(talentId = "mana_surge"),
                        ),
                ),
            )

        assertEquals(PlayerCommand.AssignTalent("mana_surge"), command)
    }

    @Test
    fun `healthy mana class does not blink only because boss is visible`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                visibleHostilePositions = listOf(Point(4, 1)),
                visibleBossPositions = listOf(Point(4, 1)),
                knownDownstairsPositions = listOf(Point(0, 1)),
                exploredTiles = setOf(Point(1, 1), Point(0, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                visibleTiles = setOf(Point(0, 1), Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                talentSlots = manaLoadout(),
            )

        val command = bot.decide(observation)
        assertFalse(
            command is PlayerCommand.UseTalent && command.slot == 2,
            "Expected healthy mana class to avoid blink just because a boss is visible, but got $command.",
        )
    }

    @Test
    fun `low health mana class does not panic blink against a single boss at range`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus =
                    PlayerStatus(
                        currentHp = 14,
                        maxHp = 30,
                        level = 1,
                        currentExperience = 0,
                        nextLevelRequirement = 10,
                        statPoints = 0,
                        talentPoints = 0,
                        attack = 6,
                        defense = 4,
                        accuracy = 5,
                        evasion = 3,
                        speed = 100,
                    ),
                visibleHostilePositions = listOf(Point(4, 1)),
                visibleBossPositions = listOf(Point(4, 1)),
                visibleTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                exploredTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                talentSlots = manaLoadout(),
            )

        val command = bot.decide(observation)
        assertFalse(
            command is PlayerCommand.UseTalent && command.slot == 2,
            "Single boss pressure at range should not trigger panic blink, but got $command.",
        )
    }

    @Test
    fun `critical health templar heals before casting devotion under ranged pressure`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus =
                    PlayerStatus(
                        currentHp = 12,
                        maxHp = 30,
                        level = 1,
                        currentExperience = 0,
                        nextLevelRequirement = 10,
                        statPoints = 0,
                        talentPoints = 0,
                        attack = 6,
                        defense = 4,
                        accuracy = 5,
                        evasion = 3,
                        speed = 100,
                    ),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "POSITIVE_ENERGY"),
                visibleHostilePositions = listOf(Point(4, 1)),
                visibleTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                exploredTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_light", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                        talentSlot(slot = 2, talentId = "devotion", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 0, range = 0, requiresTarget = false),
                    ),
            )

        assertEquals(PlayerCommand.UseTalent(1), bot.decide(observation))
    }

    @Test
    fun `critical health mana class blinks away from multiple nearby hostiles`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus =
                    PlayerStatus(
                        currentHp = 11,
                        maxHp = 30,
                        level = 1,
                        currentExperience = 0,
                        nextLevelRequirement = 10,
                        statPoints = 0,
                        talentPoints = 0,
                        attack = 6,
                        defense = 4,
                        accuracy = 5,
                        evasion = 3,
                        speed = 100,
                    ),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "MANA"),
                visibleHostilePositions = listOf(Point(4, 1), Point(4, 2)),
                visibleTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1), Point(4, 2), Point(1, 2), Point(2, 2), Point(3, 2)),
                exploredTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1), Point(4, 2), Point(1, 2), Point(2, 2), Point(3, 2)),
                talentSlots = manaLoadout(),
            )

        val command = bot.decide(observation)

        assertTrue(command is PlayerCommand.UseTalent && command.slot == 2, "Expected critical mana bot to blink away from multiple hostiles, but got $command.")
    }

    @Test
    fun `low health mana class retreats from nearby hostile when no combat talent is ready`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus =
                    PlayerStatus(
                        currentHp = 14,
                        maxHp = 30,
                        level = 1,
                        currentExperience = 0,
                        nextLevelRequirement = 10,
                        statPoints = 0,
                        talentPoints = 0,
                        attack = 6,
                        defense = 4,
                        accuracy = 5,
                        evasion = 3,
                        speed = 100,
                    ),
                playerResource = PlayerResourceView(current = 10, max = 20, typeId = "MANA"),
                map = corridorMap,
                playerPosition = Point(3, 0),
                visibleHostilePositions = listOf(Point(5, 0)),
                visibleTiles = corridorMap.floorPoints().toSet(),
                exploredTiles = corridorMap.floorPoints().toSet(),
                talentSlots = emptyList(),
            )

        assertEquals(PlayerCommand.Move(Point(-1, 0)), bot.decide(observation))
    }

    @Test
    fun `low health templar retreats under mixed melee and ranged pressure when no emergency action is ready`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus =
                    PlayerStatus(
                        currentHp = 16,
                        maxHp = 30,
                        level = 1,
                        currentExperience = 0,
                        nextLevelRequirement = 10,
                        statPoints = 0,
                        talentPoints = 0,
                        attack = 6,
                        defense = 4,
                        accuracy = 5,
                        evasion = 3,
                        speed = 100,
                    ),
                playerResource = PlayerResourceView(current = 8, max = 20, typeId = "POSITIVE_ENERGY"),
                map = corridorMap,
                playerPosition = Point(3, 0),
                visibleHostilePositions = listOf(Point(4, 0), Point(6, 0)),
                visibleTiles = corridorMap.floorPoints().toSet(),
                exploredTiles = corridorMap.floorPoints().toSet(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_light", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                        talentSlot(slot = 2, talentId = "holy_shield", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                    ),
            )

        assertEquals(PlayerCommand.Move(Point(-1, 0)), bot.decide(observation))
    }

    @Test
    fun `low health templar retreats from adjacent hostile when no emergency action is ready`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus =
                    PlayerStatus(
                        currentHp = 16,
                        maxHp = 30,
                        level = 1,
                        currentExperience = 0,
                        nextLevelRequirement = 10,
                        statPoints = 0,
                        talentPoints = 0,
                        attack = 6,
                        defense = 4,
                        accuracy = 5,
                        evasion = 3,
                        speed = 100,
                    ),
                playerResource = PlayerResourceView(current = 8, max = 20, typeId = "POSITIVE_ENERGY"),
                map = corridorMap,
                playerPosition = Point(3, 0),
                visibleHostilePositions = listOf(Point(4, 0)),
                visibleTiles = corridorMap.floorPoints().toSet(),
                exploredTiles = corridorMap.floorPoints().toSet(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_light", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                        talentSlot(slot = 2, talentId = "holy_shield", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                    ),
            )

        assertEquals(PlayerCommand.Move(Point(-1, 0)), bot.decide(observation))
    }

    private fun observation(
        inventoryItems: List<InventoryItemView>,
        playerStatus: PlayerStatus =
            PlayerStatus(
                currentHp = 10,
                maxHp = 30,
                level = 1,
                currentExperience = 0,
                nextLevelRequirement = 10,
                statPoints = 0,
                talentPoints = 0,
                attack = 6,
                defense = 4,
                accuracy = 5,
                evasion = 3,
                speed = 100,
            ),
        playerResource: PlayerResourceView = PlayerResourceView(current = 6, max = 20, typeId = "MANA"),
        visibleHostilePositions: List<Point> = emptyList(),
        visibleBossPositions: List<Point> = emptyList(),
        visibleTiles: Set<Point> = setOf(Point(1, 1)),
        exploredTiles: Set<Point> = setOf(Point(1, 1)),
        knownDownstairsPositions: List<Point> = emptyList(),
        visibleGroundItemPositions: List<Point> = emptyList(),
        visibleInteractables: List<ObservedInteractable> = emptyList(),
        map: GameMap = this.map,
        playerPosition: Point = Point(1, 1),
        talentSlots: List<TalentSlotView> = emptyList(),
        reserveTalents: List<com.ktome.game.TalentReserveView> = emptyList(),
    ): RunObservation =
        RunObservation(
            floor = 1,
            turnIndex = 10,
            playerStatus = playerStatus,
            playerResource = playerResource,
            playerPosition = playerPosition,
            map = map,
            visibleTiles = visibleTiles,
            exploredTiles = exploredTiles,
            visibleHostilePositions = visibleHostilePositions,
            visibleBossPositions = visibleBossPositions,
            visibleBlockingPositions = emptySet(),
            visibleGroundItemPositions = visibleGroundItemPositions,
            visibleInteractables = visibleInteractables,
            knownDownstairsPositions = knownDownstairsPositions,
            inventoryItems = inventoryItems,
            talentSlots = talentSlots,
            reserveTalents = reserveTalents,
            canAscend = false,
            canDescend = false,
            runOutcome = RunOutcome.InProgress,
            messageLogTail = emptyList(),
            eventTail = emptyList(),
        )

    private fun healthyStatus(): PlayerStatus =
        PlayerStatus(
            currentHp = 30,
            maxHp = 30,
            level = 1,
            currentExperience = 0,
            nextLevelRequirement = 10,
            statPoints = 0,
            talentPoints = 0,
            attack = 6,
            defense = 4,
            accuracy = 5,
            evasion = 3,
            speed = 100,
        )

    private fun manaLoadout(): List<TalentSlotView> =
        listOf(
            talentSlot(slot = 1, talentId = "fireball", resourceCost = 8, range = 6, requiresTarget = true),
            talentSlot(slot = 2, talentId = "blink", resourceCost = 12, range = 6, requiresTarget = true),
            talentSlot(slot = 3, talentId = "ice_prison", resourceCost = 10, range = 6, requiresTarget = true),
            talentSlot(slot = 4, talentId = "arcane_shield", resourceCost = 0, range = 0, requiresTarget = false),
        )

    private fun talentSlot(
        slot: Int,
        talentId: String,
        resourceTypeId: String = "MANA",
        resourceCost: Int,
        range: Int,
        requiresTarget: Boolean,
    ): TalentSlotView =
        TalentSlotView(
            slot = slot,
            talentId = talentId,
            name = talentId,
            level = 1,
            maxLevel = 5,
            resourceCost = resourceCost,
            resourceTypeId = resourceTypeId,
            range = range,
            minRange = 0,
            currentCooldown = 0,
            maxCooldown = 6,
            requiresTarget = requiresTarget,
        )

    private fun reserveTalentView(
        talentId: String,
        level: Int = 1,
        maxLevel: Int = 5,
    ): com.ktome.game.TalentReserveView =
        com.ktome.game.TalentReserveView(
            talentId = talentId,
            name = talentId,
            level = level,
            maxLevel = maxLevel,
            resourceCost = 0,
            resourceTypeId = "MANA",
            range = 0,
            minRange = 0,
            currentCooldown = 0,
            maxCooldown = 6,
            requiresTarget = false,
        )
}
