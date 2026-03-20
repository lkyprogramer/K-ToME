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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SmokeBotTest {
    private val bot = SmokeBot()
    private val map = GameMap.fromAscii(rows = listOf(".....", ".....", "....."), playerStart = Point(1, 1))

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

    private fun observation(
        inventoryItems: List<InventoryItemView>,
        visibleHostilePositions: List<Point> = emptyList(),
    ): RunObservation =
        RunObservation(
            floor = 1,
            turnIndex = 10,
            playerStatus =
                PlayerStatus(
                    currentHp = 10,
                    maxHp = 30,
                    currentStamina = 8,
                    maxStamina = 12,
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
            playerResource = PlayerResourceView(current = 6, max = 20, typeId = "MANA"),
            playerPosition = Point(1, 1),
            map = map,
            visibleTiles = setOf(Point(1, 1)),
            exploredTiles = setOf(Point(1, 1)),
            visibleHostilePositions = visibleHostilePositions,
            visibleBlockingPositions = emptySet(),
            visibleGroundItemPositions = emptyList(),
            visibleInteractables = emptyList(),
            knownDownstairsPositions = emptyList(),
            inventoryItems = inventoryItems,
            talentSlots = emptyList(),
            canAscend = false,
            canDescend = false,
            runOutcome = RunOutcome.InProgress,
            messageLogTail = emptyList(),
            eventTail = emptyList(),
        )
}
