package com.ktome.game.harness

import com.ktome.core.map.Point
import com.ktome.game.PlayerCommand
import com.ktome.game.PrimaryStat
import com.ktome.game.TalentSlotView

class SmokeBot : RunBot {
    override fun decide(observation: RunObservation): PlayerCommand {
        pickImmediateAction(observation)?.let { return it }

        chooseTalent(observation)?.let { return it }

        chooseMeleeOrPursuit(observation)?.let { return it }

        chooseGroundItemPath(observation)?.let { return it }

        chooseStairOrExploreMove(observation)?.let { return it }

        return PlayerCommand.Wait
    }

    private fun pickImmediateAction(observation: RunObservation): PlayerCommand? {
        if (observation.playerStatus.statPoints > 0) {
            return PlayerCommand.AssignStat(PrimaryStat.WIL)
        }
        if (observation.playerStatus.talentPoints > 0 && observation.talentSlots.isNotEmpty()) {
            return PlayerCommand.AssignTalent(observation.talentSlots.first().slot)
        }
        if (observation.visibleGroundItemPositions.any { it == observation.playerPosition }) {
            return PlayerCommand.PickUp
        }
        preferredInventoryAction(observation)?.let { return it }
        if (observation.canDescend) {
            return PlayerCommand.Descend
        }
        return null
    }

    private fun preferredInventoryAction(observation: RunObservation): PlayerCommand? {
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 60
        val consumableIndex =
            observation.inventoryItems.indexOfFirst { item ->
                item.type.name == "CONSUMABLE" && lowHealth
            }
        if (consumableIndex >= 0) {
            return PlayerCommand.ActivateInventoryItem(consumableIndex)
        }

        val hasEquippedWeapon = observation.inventoryItems.any { it.type.name == "WEAPON" && it.equippedSlot != null }
        val hasEquippedArmor = observation.inventoryItems.any { it.type.name == "ARMOR" && it.equippedSlot != null }
        val gearIndex =
            observation.inventoryItems.indexOfFirst { item ->
                item.equippedSlot == null &&
                    (
                        (item.type.name == "WEAPON" && !hasEquippedWeapon) ||
                            (item.type.name == "ARMOR" && !hasEquippedArmor)
                    )
            }
        if (gearIndex >= 0) {
            return PlayerCommand.ActivateInventoryItem(gearIndex)
        }
        return null
    }

    private fun chooseTalent(observation: RunObservation): PlayerCommand? {
        val visibleHostile = observation.visibleHostilePositions.firstOrNull() ?: return null
        val distance = observation.playerPosition.chebyshevDistanceTo(visibleHostile)

        observation.talentSlots
            .filter { it.currentCooldown <= 0 }
            .sortedWith(compareBy<TalentSlotView> { it.staminaCost }.thenBy { it.slot })
            .forEach { slot ->
                if (!slot.requiresTarget) {
                    return PlayerCommand.UseTalent(slot.slot)
                }
                if (distance in slot.minRange..slot.range) {
                    return PlayerCommand.UseTalent(slot = slot.slot, target = visibleHostile)
                }
            }
        return null
    }

    private fun chooseMeleeOrPursuit(observation: RunObservation): PlayerCommand? {
        val hostile = observation.visibleHostilePositions.minByOrNull { it.chebyshevDistanceTo(observation.playerPosition) } ?: return null
        val delta = hostile.deltaFrom(observation.playerPosition)
        if (observation.playerPosition.chebyshevDistanceTo(hostile) <= 1) {
            return PlayerCommand.Move(delta)
        }
        val nextStep = stepToward(observation, hostile) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }

    private fun chooseGroundItemPath(observation: RunObservation): PlayerCommand? {
        val target = observation.visibleGroundItemPositions.minByOrNull { it.chebyshevDistanceTo(observation.playerPosition) } ?: return null
        val nextStep = stepToward(observation, target) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }

    private fun chooseStairOrExploreMove(observation: RunObservation): PlayerCommand? {
        val knownDownstairs = observation.knownDownstairsPositions.minByOrNull { it.chebyshevDistanceTo(observation.playerPosition) }
        if (knownDownstairs != null) {
            val nextStep = stepToward(observation, knownDownstairs) ?: return null
            return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
        }

        val frontier =
            observation.exploredTiles
                .asSequence()
                .filter { point ->
                    !observation.map.blocksMovement(point.x, point.y) &&
                        Point.ALL_DIRECTIONS.any { delta ->
                            val next = point + delta
                            observation.map.isInBounds(next.x, next.y) &&
                                !observation.map.blocksMovement(next.x, next.y) &&
                                next !in observation.exploredTiles
                        }
                }
                .minByOrNull { it.chebyshevDistanceTo(observation.playerPosition) }
                ?: return null

        val nextStep = stepToward(observation, frontier) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }
}
