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
                        reserveTalent(talentId = "spell_parry"),
                        reserveTalent(talentId = "flux_burst"),
                    ),
                canAscend = false,
                canDescend = false,
                runOutcome = RunOutcome.InProgress,
                messageLogTail = emptyList(),
                eventTail = emptyList(),
            )

        assertEquals(PlayerCommand.EquipTalentToSlot(3, "spell_parry"), LoadoutPlanner.preferredLoadoutCommand(observation))
    }

    private fun talentSlot(
        slot: Int,
        talentId: String,
        requiresTarget: Boolean = false,
    ): TalentSlotView =
        TalentSlotView(
            slot = slot,
            talentId = talentId,
            name = talentId,
            level = 1,
            maxLevel = 5,
            resourceCost = 0,
            resourceTypeId = "MANA",
            range = 6,
            minRange = 0,
            currentCooldown = 0,
            maxCooldown = 6,
            requiresTarget = requiresTarget,
        )

    private fun reserveTalent(
        talentId: String,
        requiresTarget: Boolean = false,
    ): TalentReserveView =
        TalentReserveView(
            talentId = talentId,
            name = talentId,
            level = 1,
            maxLevel = 5,
            resourceCost = 0,
            resourceTypeId = "MANA",
            range = 6,
            minRange = 0,
            currentCooldown = 0,
            maxCooldown = 6,
            requiresTarget = requiresTarget,
        )
}
