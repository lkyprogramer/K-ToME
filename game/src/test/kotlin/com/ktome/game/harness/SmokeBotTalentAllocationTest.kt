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

    @Test
    fun `smoke bot prioritizes documented foundation breakpoint talents once they are available`() {
        val cases =
            listOf(
                Triple(
                    "STAMINA",
                    listOf(
                        talentSlot(slot = 1, talentId = "power_strike", ownerType = TalentTreeOwnerType.PROFESSION),
                        talentSlot(slot = 2, talentId = "guard_stance", ownerType = TalentTreeOwnerType.PROFESSION),
                    ),
                    "guard_stance",
                ),
                Triple(
                    "MANA",
                    listOf(
                        talentSlot(slot = 1, talentId = "fireball", ownerType = TalentTreeOwnerType.PROFESSION),
                        talentSlot(slot = 2, talentId = "blink", ownerType = TalentTreeOwnerType.PROFESSION),
                    ),
                    "blink",
                ),
                Triple(
                    "ENERGY",
                    listOf(
                        talentSlot(slot = 1, talentId = "backstab", ownerType = TalentTreeOwnerType.PROFESSION),
                        talentSlot(slot = 2, talentId = "shadow_bind", ownerType = TalentTreeOwnerType.PROFESSION),
                    ),
                    "shadow_bind",
                ),
                Triple(
                    "POSITIVE_ENERGY",
                    listOf(
                        talentSlot(slot = 1, talentId = "holy_strike", ownerType = TalentTreeOwnerType.PROFESSION),
                        talentSlot(slot = 2, talentId = "holy_mark", ownerType = TalentTreeOwnerType.PROFESSION),
                    ),
                    "holy_mark",
                ),
            )

        cases.forEach { (resourceTypeId, talentSlots, expectedTalentId) ->
            val command =
                bot.decide(
                    observation(
                        playerStatus = healthyStatus(talentPoints = 1, raceTalentPoints = 0),
                        playerResource = PlayerResourceView(current = 40, max = 100, typeId = resourceTypeId),
                        talentSlots = talentSlots,
                    ),
                )

            assertEquals(PlayerCommand.AssignTalent(expectedTalentId), command)
        }
    }

    @Test
    fun `smoke bot unlocks rogue shadowstep before spending filler points when shadow bind path is gated`() {
        val command =
            bot.decide(
                observation(
                    playerStatus = healthyStatus(talentPoints = 1, raceTalentPoints = 0),
                    playerResource = PlayerResourceView(current = 40, max = 100, typeId = "ENERGY"),
                    talentSlots =
                        listOf(
                            talentSlot(slot = 1, talentId = "backstab", ownerType = TalentTreeOwnerType.PROFESSION),
                        ),
                    reserveTalents =
                        listOf(
                            reserveTalent(talentId = "shadowstep", ownerType = TalentTreeOwnerType.PROFESSION),
                            reserveTalent(talentId = "poison_blade", ownerType = TalentTreeOwnerType.PROFESSION),
                        ),
                ),
            )

        assertEquals(PlayerCommand.AssignTalent("shadowstep"), command)
    }

    private fun observation(
        playerStatus: PlayerStatus,
        playerResource: PlayerResourceView = PlayerResourceView(current = 40, max = 100, typeId = "HATE"),
        talentSlots: List<TalentSlotView>,
        reserveTalents: List<TalentReserveView> = emptyList(),
    ): RunObservation =
        RunObservation(
            floor = 1,
            turnIndex = 10,
            playerStatus = playerStatus,
            playerResource = playerResource,
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

    private fun reserveTalent(
        talentId: String,
        ownerType: TalentTreeOwnerType,
    ): TalentReserveView =
        TalentReserveView(
            talentId = talentId,
            name = talentId,
            ownerType = ownerType,
            level = 0,
            maxLevel = 5,
            resourceCost = 0,
            resourceTypeId = "ENERGY",
            range = 1,
            minRange = 0,
            currentCooldown = 0,
            maxCooldown = 6,
            requiresTarget = false,
        )
}
