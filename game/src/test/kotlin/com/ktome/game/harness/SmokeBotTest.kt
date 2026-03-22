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
                talentSlots =
                    listOf(
                        TalentSlotView(
                            slot = 3,
                            talentId = "blink",
                            name = "闪现",
                            level = 1,
                            maxLevel = 5,
                            resourceCost = 12,
                            resourceTypeId = "MANA",
                            range = 6,
                            minRange = 0,
                            currentCooldown = 0,
                            maxCooldown = 6,
                            requiresTarget = true,
                        ),
                    ),
            )

        val command = bot.decide(observation)
        assertTrue(command is PlayerCommand.Move, "Expected healthy mana class to keep navigating instead of blinking, but got $command.")
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
                talentSlots =
                    listOf(
                        TalentSlotView(
                            slot = 3,
                            talentId = "blink",
                            name = "闪现",
                            level = 1,
                            maxLevel = 5,
                            resourceCost = 12,
                            resourceTypeId = "MANA",
                            range = 6,
                            minRange = 0,
                            currentCooldown = 0,
                            maxCooldown = 6,
                            requiresTarget = true,
                        ),
                    ),
            )

        val command = bot.decide(observation)
        assertTrue(command is PlayerCommand.Move, "Single boss pressure at range should not trigger panic blink, but got $command.")
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
        visibleHostilePositions: List<Point> = emptyList(),
        visibleBossPositions: List<Point> = emptyList(),
        visibleTiles: Set<Point> = setOf(Point(1, 1)),
        exploredTiles: Set<Point> = setOf(Point(1, 1)),
        knownDownstairsPositions: List<Point> = emptyList(),
        map: GameMap = this.map,
        playerPosition: Point = Point(1, 1),
        talentSlots: List<TalentSlotView> = emptyList(),
    ): RunObservation =
        RunObservation(
            floor = 1,
            turnIndex = 10,
            playerStatus = playerStatus,
            playerResource = PlayerResourceView(current = 6, max = 20, typeId = "MANA"),
            playerPosition = playerPosition,
            map = map,
            visibleTiles = visibleTiles,
            exploredTiles = exploredTiles,
            visibleHostilePositions = visibleHostilePositions,
            visibleBossPositions = visibleBossPositions,
            visibleBlockingPositions = emptySet(),
            visibleGroundItemPositions = emptyList(),
            visibleInteractables = emptyList(),
            knownDownstairsPositions = knownDownstairsPositions,
            inventoryItems = inventoryItems,
            talentSlots = talentSlots,
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
}
