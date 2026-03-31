package com.ktome.game.harness

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.run.RunOutcome
import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.game.PlayerCommand
import com.ktome.game.PlayerResourceView
import com.ktome.game.PlayerStatus
import com.ktome.game.TalentReserveView
import com.ktome.game.TalentSlotView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SmokeBotTalentAllocationTest {
    private val bot = SmokeBot()
    private val map = GameMap.fromAscii(rows = listOf("@"))

    @Test
    fun `smoke bot spends profession talent points only on profession talents`() {
        val command =
            bot.decide(
                observation(
                    playerStatus = healthyStatus(talentPoints = 1, raceTalentPoints = 0),
                    talentSlots =
                        listOf(
                            talentSlot(slot = 1, talentId = "savage_hew", ownerType = TalentTreeOwnerType.PROFESSION),
                            talentSlot(slot = 2, talentId = "human_resolve", ownerType = TalentTreeOwnerType.RACE),
                        ),
                ),
            )

        assertEquals(PlayerCommand.AssignTalent("savage_hew"), command)
    }

    @Test
    fun `smoke bot spends race talent points only on race talents`() {
        val command =
            bot.decide(
                observation(
                    playerStatus = healthyStatus(talentPoints = 0, raceTalentPoints = 1),
                    talentSlots =
                        listOf(
                            talentSlot(slot = 1, talentId = "savage_hew", ownerType = TalentTreeOwnerType.PROFESSION),
                            talentSlot(slot = 2, talentId = "human_resolve", ownerType = TalentTreeOwnerType.RACE),
                        ),
                ),
            )

        assertEquals(PlayerCommand.AssignTalent("human_resolve"), command)
    }

    private fun observation(
        playerStatus: PlayerStatus,
        talentSlots: List<TalentSlotView>,
        reserveTalents: List<TalentReserveView> = emptyList(),
    ): RunObservation =
        RunObservation(
            floor = 1,
            turnIndex = 10,
            playerStatus = playerStatus,
            playerResource = PlayerResourceView(current = 40, max = 100, typeId = "HATE"),
            playerPosition = Point.ZERO,
            map = map,
            visibleTiles = setOf(Point.ZERO),
            exploredTiles = setOf(Point.ZERO),
            visibleHostilePositions = emptyList(),
            visibleBlockingPositions = emptySet(),
            visibleGroundItemPositions = emptyList(),
            visibleInteractables = emptyList(),
            knownDownstairsPositions = emptyList(),
            inventoryItems = emptyList(),
            talentSlots = talentSlots,
            reserveTalents = reserveTalents,
            canAscend = false,
            canDescend = false,
            runOutcome = RunOutcome.InProgress,
            messageLogTail = emptyList(),
            eventTail = emptyList(),
        )

    private fun healthyStatus(
        talentPoints: Int,
        raceTalentPoints: Int,
    ): PlayerStatus =
        PlayerStatus(
            currentHp = 30,
            maxHp = 30,
            level = 4,
            currentExperience = 0,
            nextLevelRequirement = 10,
            statPoints = 0,
            talentPoints = talentPoints,
            raceTalentPoints = raceTalentPoints,
            attack = 6,
            defense = 4,
            accuracy = 5,
            evasion = 3,
            speed = 100,
        )

    private fun talentSlot(
        slot: Int,
        talentId: String,
        ownerType: TalentTreeOwnerType,
    ): TalentSlotView =
        TalentSlotView(
            slot = slot,
            talentId = talentId,
            name = talentId,
            ownerType = ownerType,
            level = 1,
            maxLevel = 5,
            resourceCost = 0,
            resourceTypeId = "HATE",
            range = 1,
            minRange = 0,
            currentCooldown = 0,
            maxCooldown = 6,
            requiresTarget = false,
        )
}
