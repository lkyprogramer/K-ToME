package com.ktome.game.harness

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.game.PlayerCommand
import com.ktome.game.PlayerResourceView
import com.ktome.game.PlayerStatus
import com.ktome.game.TalentReserveView
import com.ktome.game.TalentSlotView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoadoutPlannerTest {
    private val map = GameMap.fromAscii(rows = listOf("....."), playerStart = Point(1, 0))

    @Test
    fun `mana loadout keeps cold single target answer slotted ahead of mana surge`() {
        val observation =
            RunObservation(
                floor = 1,
                turnIndex = 10,
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
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "MANA"),
                playerPosition = Point(1, 0),
                map = map,
                visibleTiles = map.floorPoints().toSet(),
                exploredTiles = map.floorPoints().toSet(),
                visibleHostilePositions = emptyList(),
                visibleBlockingPositions = emptySet(),
                visibleGroundItemPositions = emptyList(),
                visibleInteractables = emptyList(),
                knownDownstairsPositions = emptyList(),
                inventoryItems = emptyList(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "fireball"),
                        talentSlot(slot = 2, talentId = "blink", requiresTarget = true),
                        talentSlot(slot = 3, talentId = "arcane_shield"),
                        talentSlot(slot = 4, talentId = "mana_surge"),
                    ),
                reserveTalents =
                    listOf(
                        reserveTalent(talentId = "ice_bolt", requiresTarget = true),
                        reserveTalent(talentId = "mana_surge"),
                    ),
                canAscend = false,
                canDescend = false,
                runOutcome = RunOutcome.InProgress,
                messageLogTail = emptyList(),
                eventTail = emptyList(),
            )

        assertEquals(PlayerCommand.EquipTalentToSlot(4, "ice_bolt"), LoadoutPlanner.preferredLoadoutCommand(observation))
    }

    @Test
    fun `spellblade loadout prefers spellblade core kit over generic mana order`() {
        val observation =
            RunObservation(
                floor = 1,
                turnIndex = 10,
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
                playerResource = PlayerResourceView(current = 20, max = 20, typeId = "MANA"),
                playerPosition = Point(1, 0),
                map = map,
                visibleTiles = map.floorPoints().toSet(),
                exploredTiles = map.floorPoints().toSet(),
                visibleHostilePositions = emptyList(),
                visibleBlockingPositions = emptySet(),
                visibleGroundItemPositions = emptyList(),
                visibleInteractables = emptyList(),
                knownDownstairsPositions = emptyList(),
                inventoryItems = emptyList(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "arcane_edge", requiresTarget = true),
                        talentSlot(slot = 2, talentId = "mana_lunge", requiresTarget = true),
                        talentSlot(slot = 3, talentId = "flux_anchor"),
                        talentSlot(slot = 4, talentId = "spell_rend", requiresTarget = true),
                    ),
                reserveTalents =
                    listOf(
                        reserveTalent(talentId = "runic_edge", requiresTarget = true),
                        reserveTalent(talentId = "spell_parry"),
                        reserveTalent(talentId = "flux_burst"),
                    ),
                canAscend = false,
                canDescend = false,
                runOutcome = RunOutcome.InProgress,
                messageLogTail = emptyList(),
                eventTail = emptyList(),
            )

        assertEquals(PlayerCommand.EquipTalentToSlot(2, "runic_edge"), LoadoutPlanner.preferredLoadoutCommand(observation))
    }

    @Test
    fun `berserker loadout promotes pr11 talent depth into active slots`() {
        val observation =
            RunObservation(
                floor = 1,
                turnIndex = 14,
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 44, max = 100, typeId = "HATE"),
                playerPosition = Point(1, 0),
                map = map,
                visibleTiles = map.floorPoints().toSet(),
                exploredTiles = map.floorPoints().toSet(),
                visibleHostilePositions = emptyList(),
                visibleBlockingPositions = emptySet(),
                visibleGroundItemPositions = emptyList(),
                visibleInteractables = emptyList(),
                knownDownstairsPositions = emptyList(),
                inventoryItems = emptyList(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "savage_hew", resourceTypeId = "HATE", requiresTarget = true),
                        talentSlot(slot = 2, talentId = "blood_rush", resourceTypeId = "HATE", requiresTarget = true),
                        talentSlot(slot = 3, talentId = "kill_frenzy", resourceTypeId = "HATE"),
                        talentSlot(slot = 4, talentId = "last_stand", resourceTypeId = "HATE"),
                    ),
                reserveTalents =
                    listOf(
                        reserveTalent(talentId = "fault_line", resourceTypeId = "HATE", requiresTarget = true),
                        reserveTalent(talentId = "slaughter_drive", resourceTypeId = "HATE"),
                    ),
                canAscend = false,
                canDescend = false,
                runOutcome = RunOutcome.InProgress,
                messageLogTail = emptyList(),
                eventTail = emptyList(),
            )

        assertEquals(PlayerCommand.EquipTalentToSlot(2, "fault_line"), LoadoutPlanner.preferredLoadoutCommand(observation))
    }

    @Test
    fun `stamina loadout promotes pr09 control talents into active slots`() {
        val observation =
            RunObservation(
                floor = 1,
                turnIndex = 12,
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 36, max = 44, typeId = "STAMINA"),
                playerPosition = Point(1, 0),
                map = map,
                visibleTiles = map.floorPoints().toSet(),
                exploredTiles = map.floorPoints().toSet(),
                visibleHostilePositions = emptyList(),
                visibleBlockingPositions = emptySet(),
                visibleGroundItemPositions = emptyList(),
                visibleInteractables = emptyList(),
                knownDownstairsPositions = emptyList(),
                inventoryItems = emptyList(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "power_strike", resourceTypeId = "STAMINA", requiresTarget = true),
                        talentSlot(slot = 2, talentId = "shield_bash", resourceTypeId = "STAMINA", requiresTarget = true),
                        talentSlot(slot = 3, talentId = "charge", resourceTypeId = "STAMINA", requiresTarget = true),
                        talentSlot(slot = 4, talentId = "war_cry", resourceTypeId = "STAMINA"),
                    ),
                reserveTalents =
                    listOf(
                        reserveTalent(talentId = "linebreaker", resourceTypeId = "STAMINA", requiresTarget = true),
                        reserveTalent(talentId = "earthshaker", resourceTypeId = "STAMINA"),
                    ),
                canAscend = false,
                canDescend = false,
                runOutcome = RunOutcome.InProgress,
                messageLogTail = emptyList(),
                eventTail = emptyList(),
            )

        assertEquals(PlayerCommand.EquipTalentToSlot(3, "linebreaker"), LoadoutPlanner.preferredLoadoutCommand(observation))
    }

    @Test
    fun `energy loadout slots pr09 finisher ahead of legacy utility`() {
        val observation =
            RunObservation(
                floor = 1,
                turnIndex = 16,
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 60, max = 100, typeId = "ENERGY"),
                playerPosition = Point(1, 0),
                map = map,
                visibleTiles = map.floorPoints().toSet(),
                exploredTiles = map.floorPoints().toSet(),
                visibleHostilePositions = emptyList(),
                visibleBlockingPositions = emptySet(),
                visibleGroundItemPositions = emptyList(),
                visibleInteractables = emptyList(),
                knownDownstairsPositions = emptyList(),
                inventoryItems = emptyList(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "backstab", resourceTypeId = "ENERGY", requiresTarget = true),
                        talentSlot(slot = 2, talentId = "shadowstep", resourceTypeId = "ENERGY", requiresTarget = true),
                        talentSlot(slot = 3, talentId = "stealth", resourceTypeId = "ENERGY"),
                        talentSlot(slot = 4, talentId = "poison_blade", resourceTypeId = "ENERGY", requiresTarget = true),
                    ),
                reserveTalents =
                    listOf(
                        reserveTalent(talentId = "shadow_bind", resourceTypeId = "ENERGY", requiresTarget = true),
                        reserveTalent(talentId = "eviscerate", resourceTypeId = "ENERGY", requiresTarget = true),
                    ),
                canAscend = false,
                canDescend = false,
                runOutcome = RunOutcome.InProgress,
                messageLogTail = emptyList(),
                eventTail = emptyList(),
            )

        assertEquals(PlayerCommand.EquipTalentToSlot(2, "shadow_bind"), LoadoutPlanner.preferredLoadoutCommand(observation))
    }

    @Test
    fun `positive energy loadout keeps pr09 consecration online in the first four slots`() {
        val observation =
            RunObservation(
                floor = 1,
                turnIndex = 18,
                playerStatus = healthyStatus(),
                playerResource = PlayerResourceView(current = 48, max = 100, typeId = "POSITIVE_ENERGY"),
                playerPosition = Point(1, 0),
                map = map,
                visibleTiles = map.floorPoints().toSet(),
                exploredTiles = map.floorPoints().toSet(),
                visibleHostilePositions = emptyList(),
                visibleBlockingPositions = emptySet(),
                visibleGroundItemPositions = emptyList(),
                visibleInteractables = emptyList(),
                knownDownstairsPositions = emptyList(),
                inventoryItems = emptyList(),
                talentSlots =
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_strike", resourceTypeId = "POSITIVE_ENERGY", requiresTarget = true),
                        talentSlot(slot = 2, talentId = "holy_light", resourceTypeId = "POSITIVE_ENERGY"),
                        talentSlot(slot = 3, talentId = "judgment_hammer", resourceTypeId = "POSITIVE_ENERGY", requiresTarget = true),
                        talentSlot(slot = 4, talentId = "holy_shield", resourceTypeId = "POSITIVE_ENERGY"),
                    ),
                reserveTalents =
                    listOf(
                        reserveTalent(talentId = "consecration", resourceTypeId = "POSITIVE_ENERGY"),
                        reserveTalent(talentId = "sanctuary", resourceTypeId = "POSITIVE_ENERGY"),
                    ),
                canAscend = false,
                canDescend = false,
                runOutcome = RunOutcome.InProgress,
                messageLogTail = emptyList(),
                eventTail = emptyList(),
            )

        assertEquals(PlayerCommand.EquipTalentToSlot(3, "consecration"), LoadoutPlanner.preferredLoadoutCommand(observation))
    }

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

    private fun talentSlot(
        slot: Int,
        talentId: String,
        resourceTypeId: String = "MANA",
        requiresTarget: Boolean = false,
    ): TalentSlotView =
        TalentSlotView(
            slot = slot,
            talentId = talentId,
            name = talentId,
            level = 1,
            maxLevel = 5,
            resourceCost = 0,
            resourceTypeId = resourceTypeId,
            range = 6,
            minRange = 0,
            currentCooldown = 0,
            maxCooldown = 6,
            requiresTarget = requiresTarget,
        )

    private fun reserveTalent(
        talentId: String,
        resourceTypeId: String = "MANA",
        requiresTarget: Boolean = false,
    ): TalentReserveView =
        TalentReserveView(
            talentId = talentId,
            name = talentId,
            level = 1,
            maxLevel = 5,
            resourceCost = 0,
            resourceTypeId = resourceTypeId,
            range = 6,
            minRange = 0,
            currentCooldown = 0,
            maxCooldown = 6,
            requiresTarget = requiresTarget,
        )
}
