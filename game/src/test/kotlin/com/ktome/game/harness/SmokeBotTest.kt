package com.ktome.game.harness

import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.loot.RarityTier
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
    fun `low hp uses teleport scroll as escape when boss closes in without blink`() {
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
                visibleBossPositions = listOf(Point(4, 1)),
                visibleTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                exploredTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
            )

        assertEquals(PlayerCommand.ActivateInventoryItem(0), bot.decide(observation))
    }

    @Test
    fun `critical mana build blinks away from a single nearby elite threat`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus =
                    PlayerStatus(
                        currentHp = 9,
                        maxHp = 30,
                        level = 2,
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
                visibleHostilePositions = listOf(Point(7, 0)),
                visibleTiles = (0..10).mapTo(linkedSetOf()) { x -> Point(x, 0) },
                exploredTiles = (0..10).mapTo(linkedSetOf()) { x -> Point(x, 0) },
                map = longCorridorMap,
                playerPosition = Point(4, 0),
                talentSlots = manaLoadout(),
            )

        val command = bot.decide(observation)
        assertTrue(
            command is PlayerCommand.UseTalent && command.slot == 2 && command.target != null,
            "Expected critical mana build to blink away from a single nearby threat, actual=$command.",
        )
    }

    @Test
    fun `moderately injured vanguard keeps pressure on a lone close boss instead of spending teleport escape`() {
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
                playerStatus =
                    PlayerStatus(
                        currentHp = 22,
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
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                visibleHostilePositions = listOf(Point(4, 1)),
                visibleBossPositions = listOf(Point(4, 1)),
                visibleTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                exploredTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
            )

        assertFalse(
            bot.decide(observation) is PlayerCommand.ActivateInventoryItem,
            "Vanguard should keep boss pressure at moderate health instead of spending teleport escape.",
        )
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
    fun `gear management replaces equipped weapon when a synergy affix upgrade appears`() {
        val observation =
            observation(
                inventoryItems =
                    listOf(
                        InventoryItemView(
                            index = 0,
                            name = "Short Sword",
                            baseItemId = "short_sword",
                            type = ItemType.WEAPON,
                            slot = EquipSlot.WEAPON,
                            equippedSlot = EquipSlot.WEAPON,
                            quality = RarityTier.NORMAL,
                        ),
                        InventoryItemView(
                            index = 1,
                            name = "Short Sword of Shadow",
                            baseItemId = "short_sword",
                            type = ItemType.WEAPON,
                            slot = EquipSlot.WEAPON,
                            quality = RarityTier.MAGIC,
                            affixIds = listOf("of_shadow"),
                        ),
                    ),
                playerResource = PlayerResourceView(current = 60, max = 100, typeId = "ENERGY"),
            )

        assertEquals(PlayerCommand.ActivateInventoryItem(1), bot.decide(observation))
    }

    @Test
    fun `hate build does not auto equip a low value off hand into an empty slot`() {
        val observation =
            observation(
                inventoryItems =
                    listOf(
                        InventoryItemView(
                            index = 0,
                            name = "War Maul",
                            baseItemId = "war_maul",
                            type = ItemType.WEAPON,
                            slot = EquipSlot.WEAPON,
                            equippedSlot = EquipSlot.WEAPON,
                            quality = RarityTier.NORMAL,
                        ),
                        InventoryItemView(
                            index = 1,
                            name = "Basic Shield of Life",
                            baseItemId = "basic_shield",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.OFF_HAND,
                            quality = RarityTier.MAGIC,
                            affixIds = listOf("sentinel", "of_life"),
                        ),
                    ),
                playerResource = PlayerResourceView(current = 44, max = 100, typeId = "HATE"),
            )

        assertFalse(
            bot.decide(observation) is PlayerCommand.ActivateInventoryItem,
            "HATE builds should not fill an empty off-hand with a generic sustain shield by default.",
        )
    }

    @Test
    fun `mana gear management prefers offensive rare battle axe over affix padded long sword`() {
        val observation =
            observation(
                inventoryItems =
                    listOf(
                        InventoryItemView(
                            index = 0,
                            name = "Arcane Staff",
                            baseItemId = "arcane_staff",
                            type = ItemType.WEAPON,
                            slot = EquipSlot.WEAPON,
                            equippedSlot = EquipSlot.WEAPON,
                            quality = RarityTier.NORMAL,
                        ),
                        InventoryItemView(
                            index = 1,
                            name = "Battle Axe of Blackice",
                            baseItemId = "battle_axe",
                            type = ItemType.WEAPON,
                            slot = EquipSlot.WEAPON,
                            quality = RarityTier.RARE,
                            affixIds = listOf("warforged", "of_blackice", "vampiric"),
                        ),
                        InventoryItemView(
                            index = 2,
                            name = "Long Sword of Blackice",
                            baseItemId = "long_sword",
                            type = ItemType.WEAPON,
                            slot = EquipSlot.WEAPON,
                            quality = RarityTier.RARE,
                            affixIds = listOf("warforged", "of_storms", "sharp", "of_blackice"),
                        ),
                    ),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "MANA"),
            )

        assertEquals(PlayerCommand.ActivateInventoryItem(1), bot.decide(observation))
    }

    @Test
    fun `inventory cleanup does not immediately pick up the just dropped item`() {
        val crowdedInventory =
            listOf(
                InventoryItemView(index = 0, name = "Arcane Staff", baseItemId = "arcane_staff", type = ItemType.WEAPON, slot = EquipSlot.WEAPON, equippedSlot = EquipSlot.WEAPON, quality = RarityTier.NORMAL),
                InventoryItemView(index = 1, name = "Mana Potion A", type = ItemType.CONSUMABLE, effect = ConsumableEffect.RESTORE_RESOURCE, resourceTypeId = "MANA"),
                InventoryItemView(index = 2, name = "Mana Potion B", type = ItemType.CONSUMABLE, effect = ConsumableEffect.RESTORE_RESOURCE, resourceTypeId = "MANA"),
                InventoryItemView(index = 3, name = "Mana Potion C", type = ItemType.CONSUMABLE, effect = ConsumableEffect.RESTORE_RESOURCE, resourceTypeId = "MANA"),
                InventoryItemView(index = 4, name = "Healing Potion A", type = ItemType.CONSUMABLE, effect = ConsumableEffect.HEAL),
                InventoryItemView(index = 5, name = "Healing Potion B", type = ItemType.CONSUMABLE, effect = ConsumableEffect.HEAL),
                InventoryItemView(index = 6, name = "Healing Potion C", type = ItemType.CONSUMABLE, effect = ConsumableEffect.HEAL),
                InventoryItemView(index = 7, name = "Teleport Scroll A", type = ItemType.CONSUMABLE, effect = ConsumableEffect.TELEPORT),
                InventoryItemView(index = 8, name = "Teleport Scroll B", type = ItemType.CONSUMABLE, effect = ConsumableEffect.TELEPORT),
                InventoryItemView(index = 9, name = "Teleport Scroll C", type = ItemType.CONSUMABLE, effect = ConsumableEffect.TELEPORT),
                InventoryItemView(index = 10, name = "Battle Axe", baseItemId = "battle_axe", type = ItemType.WEAPON, slot = EquipSlot.WEAPON, quality = RarityTier.NORMAL),
            )

        val dropObservation =
            observation(
                inventoryItems = crowdedInventory,
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "MANA"),
                turnIndex = 10,
            )

        val firstCommand = bot.decide(dropObservation)
        assertTrue(firstCommand is PlayerCommand.DropInventoryItem, "Expected crowded inventory to be pruned, but got $firstCommand.")

        val pickupLoopObservation =
            observation(
                inventoryItems = crowdedInventory.dropLast(1),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "MANA"),
                visibleGroundItemPositions = listOf(Point(1, 1)),
                turnIndex = 11,
            )

        assertFalse(
            bot.decide(pickupLoopObservation) is PlayerCommand.PickUp,
            "Bot should not immediately pick up the same item it just dropped.",
        )
    }

    @Test
    fun `recent drop suppresses near full detours toward visible ground items`() {
        val crowdedInventory =
            listOf(
                InventoryItemView(index = 0, name = "Arcane Staff", baseItemId = "arcane_staff", type = ItemType.WEAPON, slot = EquipSlot.WEAPON, equippedSlot = EquipSlot.WEAPON, quality = RarityTier.NORMAL),
                InventoryItemView(index = 1, name = "Mana Potion A", type = ItemType.CONSUMABLE, effect = ConsumableEffect.RESTORE_RESOURCE, resourceTypeId = "MANA"),
                InventoryItemView(index = 2, name = "Mana Potion B", type = ItemType.CONSUMABLE, effect = ConsumableEffect.RESTORE_RESOURCE, resourceTypeId = "MANA"),
                InventoryItemView(index = 3, name = "Mana Potion C", type = ItemType.CONSUMABLE, effect = ConsumableEffect.RESTORE_RESOURCE, resourceTypeId = "MANA"),
                InventoryItemView(index = 4, name = "Healing Potion A", type = ItemType.CONSUMABLE, effect = ConsumableEffect.HEAL),
                InventoryItemView(index = 5, name = "Healing Potion B", type = ItemType.CONSUMABLE, effect = ConsumableEffect.HEAL),
                InventoryItemView(index = 6, name = "Healing Potion C", type = ItemType.CONSUMABLE, effect = ConsumableEffect.HEAL),
                InventoryItemView(index = 7, name = "Teleport Scroll A", type = ItemType.CONSUMABLE, effect = ConsumableEffect.TELEPORT),
                InventoryItemView(index = 8, name = "Teleport Scroll B", type = ItemType.CONSUMABLE, effect = ConsumableEffect.TELEPORT),
                InventoryItemView(index = 9, name = "Teleport Scroll C", type = ItemType.CONSUMABLE, effect = ConsumableEffect.TELEPORT),
                InventoryItemView(index = 10, name = "Battle Axe", baseItemId = "battle_axe", type = ItemType.WEAPON, slot = EquipSlot.WEAPON, quality = RarityTier.NORMAL),
            )

        val dropObservation =
            observation(
                inventoryItems = crowdedInventory,
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "MANA"),
                turnIndex = 10,
                map = longCorridorMap,
                playerPosition = Point(3, 0),
                visibleTiles = (0..10).mapTo(linkedSetOf()) { x -> Point(x, 0) },
                exploredTiles = (0..10).mapTo(linkedSetOf()) { x -> Point(x, 0) },
                knownDownstairsPositions = listOf(Point(0, 0)),
            )

        val firstCommand = bot.decide(dropObservation)
        assertTrue(firstCommand is PlayerCommand.DropInventoryItem, "Expected crowded inventory to be pruned, but got $firstCommand.")

        val detourObservation =
            observation(
                inventoryItems = crowdedInventory.dropLast(1),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "MANA"),
                turnIndex = 11,
                map = longCorridorMap,
                playerPosition = Point(3, 0),
                visibleTiles = (0..10).mapTo(linkedSetOf()) { x -> Point(x, 0) },
                exploredTiles = (0..10).mapTo(linkedSetOf()) { x -> Point(x, 0) },
                knownDownstairsPositions = listOf(Point(0, 0)),
                visibleGroundItemPositions = listOf(Point(4, 0)),
            )

        val command = bot.decide(detourObservation)
        assertFalse(
            command == PlayerCommand.Move(Point(1, 0)) || command == PlayerCommand.PickUp,
            "Bot should not detour back into visible ground loot immediately after pruning, actual=$command.",
        )
    }

    @Test
    fun `inventory at pickup cap prunes gear before skipping visible loot`() {
        val cappedInventory =
            listOf(
                InventoryItemView(index = 0, name = "Arcane Staff", baseItemId = "arcane_staff", type = ItemType.WEAPON, slot = EquipSlot.WEAPON, equippedSlot = EquipSlot.WEAPON, quality = RarityTier.RARE, affixIds = listOf("of_flames")),
                InventoryItemView(index = 1, name = "Chain Mail", baseItemId = "chain_mail", type = ItemType.ARMOR, slot = EquipSlot.ARMOR, equippedSlot = EquipSlot.ARMOR),
                InventoryItemView(index = 2, name = "Battle Axe A", baseItemId = "battle_axe", type = ItemType.WEAPON, slot = EquipSlot.WEAPON, quality = RarityTier.NORMAL),
                InventoryItemView(index = 3, name = "Battle Axe B", baseItemId = "battle_axe", type = ItemType.WEAPON, slot = EquipSlot.WEAPON, quality = RarityTier.NORMAL),
                InventoryItemView(index = 4, name = "Chain Mail A", baseItemId = "chain_mail", type = ItemType.ARMOR, slot = EquipSlot.ARMOR, quality = RarityTier.NORMAL),
                InventoryItemView(index = 5, name = "Chain Mail B", baseItemId = "chain_mail", type = ItemType.ARMOR, slot = EquipSlot.ARMOR, quality = RarityTier.NORMAL),
                InventoryItemView(index = 6, name = "Mana Potion", type = ItemType.CONSUMABLE, effect = ConsumableEffect.RESTORE_RESOURCE, resourceTypeId = "MANA"),
                InventoryItemView(index = 7, name = "Healing Potion", type = ItemType.CONSUMABLE, effect = ConsumableEffect.HEAL),
                InventoryItemView(index = 8, name = "Teleport Scroll", type = ItemType.CONSUMABLE, effect = ConsumableEffect.TELEPORT),
                InventoryItemView(index = 9, name = "Battle Axe C", baseItemId = "battle_axe", type = ItemType.WEAPON, slot = EquipSlot.WEAPON, quality = RarityTier.NORMAL),
            )

        val command =
            bot.decide(
                observation(
                    inventoryItems = cappedInventory,
                    playerStatus = healthyStatus(),
                    playerResource = PlayerResourceView(current = 20, max = 20, typeId = "MANA"),
                    visibleGroundItemPositions = listOf(Point(1, 1)),
                ),
            )

        assertTrue(
            command is PlayerCommand.DropInventoryItem,
            "Expected capped inventory to prune before leaving visible loot behind, actual=$command.",
        )
    }

    @Test
    fun `inventory housekeeping drops low value unequipped gear before boss reward windows close`() {
        val inventoryItems =
            buildList {
                add(
                    InventoryItemView(
                        index = 0,
                        name = "Templar Sword",
                        baseItemId = "long_sword",
                        type = ItemType.WEAPON,
                        slot = EquipSlot.WEAPON,
                        equippedSlot = EquipSlot.WEAPON,
                        quality = RarityTier.RARE,
                        affixIds = listOf("sanctified", "of_strength"),
                    ),
                )
                add(
                    InventoryItemView(
                        index = 1,
                        name = "Sanctified Seal",
                        baseItemId = "sanctified_seal",
                        type = ItemType.ARMOR,
                        slot = EquipSlot.OFF_HAND,
                        equippedSlot = EquipSlot.OFF_HAND,
                        quality = RarityTier.RARE,
                        affixIds = listOf("emberguard", "of_cleansing"),
                    ),
                )
                addAll(
                    listOf(
                        InventoryItemView(
                            index = 2,
                            name = "Chain Mail",
                            baseItemId = "chain_mail",
                            type = ItemType.ARMOR,
                            slot = EquipSlot.ARMOR,
                            equippedSlot = EquipSlot.ARMOR,
                            quality = RarityTier.RARE,
                            affixIds = listOf("emberguard", "of_life"),
                        )
                    ),
                )
                addAll(
                    (3..11).map { index ->
                        InventoryItemView(
                            index = index,
                            name = "Stock Item $index",
                            baseItemId = if (index % 2 == 0) "battle_axe" else "chain_mail",
                            type = if (index % 2 == 0) ItemType.WEAPON else ItemType.ARMOR,
                            slot = if (index % 2 == 0) EquipSlot.WEAPON else EquipSlot.ARMOR,
                            quality = RarityTier.NORMAL,
                        )
                    },
                )
            }
        val observation =
            observation(
                inventoryItems = inventoryItems,
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 60, max = 100, typeId = "POSITIVE_ENERGY"),
                visibleBossPositions = listOf(Point(9, 0)),
                visibleTiles = (0..9).mapTo(linkedSetOf()) { x -> Point(x, 0) },
                exploredTiles = (0..9).mapTo(linkedSetOf()) { x -> Point(x, 0) },
                map = longCorridorMap,
                playerPosition = Point(1, 0),
            )

        assertEquals(PlayerCommand.DropInventoryItem(3), bot.decide(observation))
    }

    @Test
    fun `active shop buys affordable rescue offer before closing`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                zoneId = "greenwood_fringe",
                shardBalance = 45,
                activeShopId = "greenwood_supply_post",
                activeShopOffers =
                    listOf(
                        ObservedShopOffer(index = 0, price = 18, tags = setOf("RECOVERY")),
                        ObservedShopOffer(index = 1, price = 42, tags = setOf("MOVEMENT")),
                    ),
            )

        assertEquals(PlayerCommand.BuyShopOffer(1), bot.decide(observation))
    }

    @Test
    fun `active shop skips unactionable rescue inscription and buys the next purchasable rescue offer`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                zoneId = "greenwood_fringe",
                shardBalance = 45,
                activeShopId = "greenwood_supply_post",
                activeShopOffers =
                    listOf(
                        ObservedShopOffer(index = 0, price = 18, tags = setOf("RECOVERY"), purchasable = true),
                        ObservedShopOffer(index = 1, price = 42, tags = setOf("MOVEMENT"), purchasable = false),
                    ),
            )

        assertEquals(PlayerCommand.BuyShopOffer(0), bot.decide(observation))
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
    fun `visible hostiles do not block zero cost loadout reconfiguration`() {
        val command =
            bot.decide(
                observation(
                    inventoryItems = emptyList(),
                    playerStatus = healthyStatus(),
                    playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                    visibleHostilePositions = listOf(Point(3, 1)),
                    visibleTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1)),
                    exploredTiles = setOf(Point(1, 1), Point(2, 1), Point(3, 1)),
                    talentSlots =
                        listOf(
                            talentSlot(slot = 1, talentId = "power_strike", resourceTypeId = "STAMINA", resourceCost = 8, range = 1, requiresTarget = true),
                            talentSlot(slot = 2, talentId = "shield_bash", resourceTypeId = "STAMINA", resourceCost = 8, range = 1, requiresTarget = true),
                            talentSlot(slot = 3, talentId = "war_cry", resourceTypeId = "STAMINA", resourceCost = 0, range = 0, requiresTarget = false),
                            talentSlot(slot = 4, talentId = "guard_stance", resourceTypeId = "STAMINA", resourceCost = 0, range = 0, requiresTarget = false),
                        ),
                    reserveTalents =
                        listOf(
                            reserveTalentView(talentId = "linebreaker").copy(
                                resourceTypeId = "STAMINA",
                                resourceCost = 10,
                                range = 1,
                                requiresTarget = true,
                            ),
                        ),
                ),
            )

        assertEquals(PlayerCommand.EquipTalentToSlot(3, "guard_stance"), command)
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
    fun `healthy rogue does not cast stealth only because a boss is visible`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "ENERGY"),
                visibleHostilePositions = listOf(Point(4, 1)),
                visibleBossPositions = listOf(Point(4, 1)),
                knownDownstairsPositions = listOf(Point(0, 1)),
                exploredTiles = setOf(Point(1, 1), Point(0, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                visibleTiles = setOf(Point(0, 1), Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "stealth", resourceTypeId = "ENERGY", resourceCost = 0, range = 0, requiresTarget = false),
                    ),
            )

        assertFalse(
            bot.decide(observation) is PlayerCommand.UseTalent,
            "Healthy rogue should not cast stealth just because a boss is visible.",
        )
    }

    @Test
    fun `low health rogue retreats from adjacent boss even when it is the only nearby threat`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus().copy(currentHp = 18, maxHp = 100),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "ENERGY"),
                map = corridorMap,
                playerPosition = Point(3, 0),
                visibleHostilePositions = listOf(Point(4, 0)),
                visibleBossPositions = listOf(Point(4, 0)),
                visibleTiles = corridorMap.floorPoints().toSet(),
                exploredTiles = corridorMap.floorPoints().toSet(),
                knownDownstairsPositions = listOf(Point(0, 0)),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "backstab", resourceTypeId = "ENERGY", resourceCost = 8, range = 1, requiresTarget = true),
                    ),
            )

        assertEquals(PlayerCommand.Move(Point(-1, 0)), bot.decide(observation))
    }

    @Test
    fun `moderately low health rogue keeps pressure on an isolated boss instead of kiting at range three`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus().copy(currentHp = 38, maxHp = 100),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "ENERGY"),
                map = longCorridorMap,
                playerPosition = Point(4, 0),
                visibleHostilePositions = listOf(Point(7, 0)),
                visibleBossPositions = listOf(Point(7, 0)),
                visibleTiles = longCorridorMap.floorPoints().toSet(),
                exploredTiles = longCorridorMap.floorPoints().toSet(),
                knownDownstairsPositions = listOf(Point(0, 0)),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "backstab", resourceTypeId = "ENERGY", resourceCost = 8, range = 1, requiresTarget = true),
                    ),
            )

        assertEquals(PlayerCommand.Move(Point(1, 0)), bot.decide(observation))
    }

    @Test
    fun `visible boss without hostile snapshot is still pursued`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                map = corridorMap,
                playerPosition = Point(3, 0),
                visibleBossPositions = listOf(Point(0, 0)),
                visibleTiles = corridorMap.floorPoints().toSet(),
                exploredTiles = corridorMap.floorPoints().toSet(),
            )

        assertEquals(PlayerCommand.Move(Point(-1, 0)), bot.decide(observation))
    }

    @Test
    fun `last seen threat keeps pursuit when it drops out of sight`() {
        val routeBot = SmokeBot()

        routeBot.decide(
            observation(
                inventoryItems = emptyList(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                map = longCorridorMap,
                playerPosition = Point(4, 0),
                visibleHostilePositions = listOf(Point(8, 0)),
                visibleTiles = setOf(Point(4, 0), Point(5, 0), Point(6, 0), Point(7, 0), Point(8, 0)),
                exploredTiles = setOf(Point(4, 0), Point(5, 0), Point(6, 0), Point(7, 0), Point(8, 0)),
            ),
        )

        val command =
            routeBot.decide(
                observation(
                    inventoryItems = emptyList(),
                    playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                    map = longCorridorMap,
                    playerPosition = Point(4, 0),
                    visibleTiles = setOf(Point(4, 0), Point(5, 0), Point(6, 0)),
                    exploredTiles = setOf(Point(4, 0), Point(5, 0), Point(6, 0)),
                ),
            )

        assertEquals(PlayerCommand.Move(Point(1, 0)), command)
    }

    @Test
    fun `low mana class does not recast mana surge while buff is active`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 8, max = 20, typeId = "MANA"),
                map = corridorMap,
                playerPosition = Point(3, 0),
                visibleHostilePositions = listOf(Point(6, 0)),
                visibleTiles = corridorMap.floorPoints().toSet(),
                exploredTiles = corridorMap.floorPoints().toSet(),
                playerStatusTypeIds = setOf("MANA_SURGE_BUFF"),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "mana_surge", resourceCost = 0, range = 0, requiresTarget = false),
                    ),
            )

        assertEquals(PlayerCommand.Move(Point(1, 0)), bot.decide(observation))
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
    fun `critical health human class uses human resolve before shield`() {
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
                map = corridorMap,
                playerPosition = Point(3, 0),
                visibleHostilePositions = listOf(Point(4, 0)),
                visibleTiles = corridorMap.floorPoints().toSet(),
                exploredTiles = corridorMap.floorPoints().toSet(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "human_resolve", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 0, range = 0, requiresTarget = false),
                        talentSlot(slot = 2, talentId = "holy_shield", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                    ),
            )

        assertEquals(PlayerCommand.UseTalent(1), bot.decide(observation))
    }

    @Test
    fun `low health templar heals before shielding under adjacent melee pressure`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus =
                    PlayerStatus(
                        currentHp = 18,
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

        assertEquals(PlayerCommand.UseTalent(1), bot.decide(observation))
    }

    @Test
    fun `low health dwarf uses grit when hostiles are nearby`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus().copy(currentHp = 16),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                map = corridorMap,
                playerPosition = Point(3, 0),
                visibleHostilePositions = listOf(Point(5, 0)),
                visibleTiles = corridorMap.floorPoints().toSet(),
                exploredTiles = corridorMap.floorPoints().toSet(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "dwarf_grit", resourceTypeId = "STAMINA", resourceCost = 0, range = 0, requiresTarget = false),
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
    fun `elf scouting is used to close on distant visible hostile`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                map = longCorridorMap,
                playerPosition = Point(4, 0),
                visibleHostilePositions = listOf(Point(8, 0)),
                visibleTiles = longCorridorMap.floorPoints().toSet(),
                exploredTiles = longCorridorMap.floorPoints().toSet(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "elf_scouting", resourceTypeId = "STAMINA", resourceCost = 0, range = 4, requiresTarget = true).copy(minRange = 1),
                    ),
            )

        assertEquals(PlayerCommand.UseTalent(1, Point(8, 0)), bot.decide(observation))
    }

    @Test
    fun `healthy vanguard does not recast guard stance on a lone visible boss`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                visibleHostilePositions = listOf(Point(4, 1)),
                visibleBossPositions = listOf(Point(4, 1)),
                knownDownstairsPositions = listOf(Point(0, 1)),
                exploredTiles = setOf(Point(1, 1), Point(0, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                visibleTiles = setOf(Point(0, 1), Point(1, 1), Point(2, 1), Point(3, 1), Point(4, 1)),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "guard_stance", resourceTypeId = "STAMINA", resourceCost = 0, range = 0, requiresTarget = false),
                    ),
            )

        assertFalse(
            bot.decide(observation) is PlayerCommand.UseTalent,
            "Healthy vanguard should not spend a turn on guard stance against a lone visible boss.",
        )
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
    fun `low health templar holds melee pressure against a single adjacent hostile when no emergency action is ready`() {
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

        assertEquals(PlayerCommand.Move(Point(1, 0)), bot.decide(observation))
    }

    @Test
    fun `low health templar holds melee pressure against a lone adjacent boss when no emergency action is ready`() {
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
                visibleBossPositions = listOf(Point(4, 0)),
                visibleTiles = corridorMap.floorPoints().toSet(),
                exploredTiles = corridorMap.floorPoints().toSet(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_light", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                        talentSlot(slot = 2, talentId = "holy_shield", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                    ),
            )

        assertEquals(PlayerCommand.Move(Point(1, 0)), bot.decide(observation))
    }

    @Test
    fun `templar without judgment hammer keeps route progress instead of chasing distant hostile`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 12, max = 20, typeId = "POSITIVE_ENERGY"),
                map = corridorMap,
                playerPosition = Point(3, 0),
                visibleHostilePositions = listOf(Point(6, 0)),
                visibleTiles = corridorMap.floorPoints().toSet(),
                exploredTiles = corridorMap.floorPoints().toSet(),
                knownDownstairsPositions = listOf(Point(0, 0)),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_strike", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 8, range = 1, requiresTarget = true),
                        talentSlot(slot = 2, talentId = "holy_light", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                    ),
            )

        assertEquals(PlayerCommand.Move(Point(-1, 0)), bot.decide(observation))
    }

    @Test
    fun `templar without ready offensive talent still chases a distant boss`() {
        val observation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 0, max = 20, typeId = "POSITIVE_ENERGY"),
                map = longCorridorMap,
                playerPosition = Point(4, 0),
                visibleBossPositions = listOf(Point(8, 0)),
                visibleTiles = longCorridorMap.floorPoints().toSet(),
                exploredTiles = longCorridorMap.floorPoints().toSet(),
                knownDownstairsPositions = listOf(Point(0, 0)),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_strike", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 8, range = 1, requiresTarget = true),
                        talentSlot(slot = 2, talentId = "holy_light", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                    ),
            )

        assertEquals(PlayerCommand.Move(Point(1, 0)), bot.decide(observation))
    }

    @Test
    fun `templar keeps boss pursuit after briefly losing sight until the last seen area is searched`() {
        val initialObservation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 0, max = 20, typeId = "POSITIVE_ENERGY"),
                map = longCorridorMap,
                playerPosition = Point(4, 0),
                visibleBossPositions = listOf(Point(8, 0)),
                visibleTiles = longCorridorMap.floorPoints().toSet(),
                exploredTiles = longCorridorMap.floorPoints().toSet(),
                knownDownstairsPositions = listOf(Point(0, 0)),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_strike", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 8, range = 1, requiresTarget = true),
                        talentSlot(slot = 2, talentId = "holy_light", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                    ),
            )
        assertEquals(PlayerCommand.Move(Point(1, 0)), bot.decide(initialObservation))

        val lostSightObservation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 0, max = 20, typeId = "POSITIVE_ENERGY"),
                map = longCorridorMap,
                playerPosition = Point(5, 0),
                visibleTiles = longCorridorMap.floorPoints().toSet(),
                exploredTiles = longCorridorMap.floorPoints().toSet(),
                knownDownstairsPositions = listOf(Point(0, 0)),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_strike", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 8, range = 1, requiresTarget = true),
                        talentSlot(slot = 2, talentId = "holy_light", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                    ),
            )
        assertEquals(PlayerCommand.Move(Point(1, 0)), bot.decide(lostSightObservation))

        val searchedObservation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 0, max = 20, typeId = "POSITIVE_ENERGY"),
                map = longCorridorMap,
                playerPosition = Point(7, 0),
                visibleTiles = longCorridorMap.floorPoints().toSet(),
                exploredTiles = longCorridorMap.floorPoints().toSet(),
                knownDownstairsPositions = listOf(Point(0, 0)),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_strike", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 8, range = 1, requiresTarget = true),
                        talentSlot(slot = 2, talentId = "holy_light", resourceTypeId = "POSITIVE_ENERGY", resourceCost = 12, range = 0, requiresTarget = false),
                    ),
            )

        assertEquals(PlayerCommand.Move(Point(-1, 0)), bot.decide(searchedObservation))
    }

    @Test
    fun `non boss remembered threat is dropped after repeated navigation loops`() {
        val pursuitTalents =
            listOf(
                talentSlot(slot = 1, talentId = "power_strike", resourceTypeId = "STAMINA", resourceCost = 8, range = 1, requiresTarget = true),
            )
        val initialObservation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                map = longCorridorMap,
                playerPosition = Point(4, 0),
                visibleHostilePositions = listOf(Point(8, 0)),
                visibleTiles = longCorridorMap.floorPoints().toSet(),
                exploredTiles = longCorridorMap.floorPoints().toSet(),
                talentSlots = pursuitTalents,
            )
        assertEquals(PlayerCommand.Move(Point(1, 0)), bot.decide(initialObservation))

        val loopPositions = listOf(Point(5, 0), Point(4, 0), Point(5, 0), Point(4, 0), Point(5, 0), Point(4, 0), Point(5, 0))
        loopPositions.dropLast(1).forEach { position ->
            bot.decide(
                observation(
                    inventoryItems = emptyList(),
                    playerStatus = healthyStatus(),
                    playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                    map = longCorridorMap,
                    playerPosition = position,
                    visibleTiles = longCorridorMap.floorPoints().toSet(),
                    exploredTiles = longCorridorMap.floorPoints().toSet(),
                    knownDownstairsPositions = listOf(Point(0, 0)),
                    talentSlots = pursuitTalents,
                ),
            )
        }

        val releaseObservation =
            observation(
                inventoryItems = emptyList(),
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "STAMINA"),
                map = longCorridorMap,
                playerPosition = loopPositions.last(),
                visibleTiles = longCorridorMap.floorPoints().toSet(),
                exploredTiles = longCorridorMap.floorPoints().toSet(),
                knownDownstairsPositions = listOf(Point(0, 0)),
                talentSlots = pursuitTalents,
            )

        assertEquals(PlayerCommand.Move(Point(-1, 0)), bot.decide(releaseObservation))
    }

    private fun observation(
        inventoryItems: List<InventoryItemView>,
        zoneId: String = "shattered_outpost",
        turnIndex: Int = 10,
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
        shardBalance: Int = 0,
        visibleHostilePositions: List<Point> = emptyList(),
        visibleBossPositions: List<Point> = emptyList(),
        visibleTiles: Set<Point> = setOf(Point(1, 1)),
        exploredTiles: Set<Point> = setOf(Point(1, 1)),
        knownDownstairsPositions: List<Point> = emptyList(),
        visibleGroundItemPositions: List<Point> = emptyList(),
        visibleInteractables: List<ObservedInteractable> = emptyList(),
        activeShopId: String? = null,
        activeShopOffers: List<ObservedShopOffer> = emptyList(),
        map: GameMap = this.map,
        playerPosition: Point = Point(1, 1),
        playerStatusTypeIds: Set<String> = emptySet(),
        talentSlots: List<TalentSlotView> = emptyList(),
        reserveTalents: List<com.ktome.game.TalentReserveView> = emptyList(),
    ): RunObservation =
        RunObservation(
            zoneId = zoneId,
            floor = 1,
            turnIndex = turnIndex,
            playerStatus = playerStatus,
            playerResource = playerResource,
            shardBalance = shardBalance,
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
            playerStatusTypeIds = playerStatusTypeIds,
            activeShopId = activeShopId,
            activeShopOffers = activeShopOffers,
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
