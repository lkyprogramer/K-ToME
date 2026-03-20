package com.ktome.game.harness

import com.ktome.core.map.Point
import com.ktome.game.PlayerCommand
import com.ktome.game.PrimaryStat
import com.ktome.game.TalentSlotView

class SmokeBot : RunBot {
    override fun decide(observation: RunObservation): PlayerCommand {
        pickImmediateAction(observation)?.let { return it }
        chooseEmergencyTalent(observation)?.let { return it }
        chooseCombatTalent(observation)?.let { return it }
        chooseMeleeOrPursuit(observation)?.let { return it }
        chooseInteractablePath(observation)?.let { return it }
        chooseGroundItemPath(observation)?.let { return it }
        chooseStairOrExploreMove(observation)?.let { return it }
        return PlayerCommand.Wait
    }

    private fun pickImmediateAction(observation: RunObservation): PlayerCommand? {
        if (observation.playerStatus.statPoints > 0) {
            return PlayerCommand.AssignStat(preferredStat(observation))
        }
        if (observation.playerStatus.talentPoints > 0) {
            preferredTalentUpgrade(observation)?.let { slot -> return PlayerCommand.AssignTalent(slot.slot) }
        }
        if (observation.visibleGroundItemPositions.any { it == observation.playerPosition }) {
            return PlayerCommand.PickUp
        }
        if (observation.visibleInteractables.any { interactable -> interactable.position == observation.playerPosition && shouldInteract(interactable) }) {
            return PlayerCommand.Interact
        }
        preferredInventoryAction(observation)?.let { return it }
        if (observation.canDescend) {
            return PlayerCommand.Descend
        }
        return null
    }

    private fun preferredStat(observation: RunObservation): PrimaryStat =
        when (observation.playerResource.typeId) {
            "MANA" -> PrimaryStat.WIL
            else -> PrimaryStat.STR
        }

    private fun preferredTalentUpgrade(observation: RunObservation): TalentSlotView? =
        observation.talentSlots
            .filter { slot -> slot.level < slot.maxLevel }
            .maxWithOrNull(
                compareBy<TalentSlotView> { talentUpgradePriority(it.talentId) }
                    .thenBy { -it.level }
                    .thenBy { -it.slot },
            )

    private fun preferredInventoryAction(observation: RunObservation): PlayerCommand? {
        val criticalHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 45
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 60
        val consumableIndex =
            observation.inventoryItems.indexOfFirst { item ->
                item.type.name == "CONSUMABLE" && (lowHealth || criticalHealth)
            }
        if (consumableIndex >= 0) {
            return PlayerCommand.ActivateInventoryItem(consumableIndex)
        }

        val equippedSlots =
            observation.inventoryItems
                .mapNotNull { item -> item.equippedSlot }
                .toSet()
        val gearIndex =
            observation.inventoryItems.indexOfFirst { item ->
                val targetSlot = item.slot
                targetSlot != null &&
                    item.equippedSlot == null &&
                    targetSlot !in equippedSlots
            }
        if (gearIndex >= 0) {
            return PlayerCommand.ActivateInventoryItem(gearIndex)
        }
        return null
    }

    private fun chooseEmergencyTalent(observation: RunObservation): PlayerCommand? {
        val adjacentHostiles = hostilesWithin(observation, 1)
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 45
        if (!lowHealth && adjacentHostiles < 2) {
            return null
        }

        if (lowHealth && adjacentHostiles > 0) {
            availableTalent(observation, "blink")?.let { slot ->
                safeBlinkTarget(observation, slot)?.let { target ->
                    return PlayerCommand.UseTalent(slot.slot, target)
                }
            }
        }

        availableTalent(observation, "unyielding")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        availableTalent(observation, "arcane_shield")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        availableTalent(observation, "guard_stance")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }

        if (adjacentHostiles >= 2) {
            availableTalent(observation, "frost_nova")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            val adjacentTarget = nearestHostile(observation)?.takeIf { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) <= 1 }
            if (adjacentTarget != null) {
                availableTalent(observation, "sweeping_strike")?.let { slot -> return PlayerCommand.UseTalent(slot.slot, adjacentTarget) }
            }
        }

        return null
    }

    private fun chooseCombatTalent(observation: RunObservation): PlayerCommand? {
        val nearest = nearestHostile(observation) ?: return null
        val clusterTarget = clusterTarget(observation) ?: nearest
        val adjacentHostiles = hostilesWithin(observation, 1)
        val nearbyHostiles = hostilesWithin(observation, 3)
        val lowMana = observation.playerResource.typeId == "MANA" && observation.playerResource.current * 100 <= observation.playerResource.max * 35

        if (nearbyHostiles >= 2) {
            availableTalent(observation, "war_cry")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "intimidation")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }
        if (lowMana) {
            availableTalent(observation, "mana_surge")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }
        if (hostilesWithin(observation, 2) >= 2) {
            availableTalent(observation, "frost_nova")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }
        if (adjacentHostiles >= 2 && nearest.chebyshevDistanceTo(observation.playerPosition) <= 1) {
            availableTalent(observation, "sweeping_strike")?.let { slot -> return PlayerCommand.UseTalent(slot.slot, nearest) }
        }
        if (hostilesAround(clusterTarget, observation.visibleHostilePositions, 1) >= 2) {
            availableTalent(observation, "flame_wall")?.takeIf { slot -> clusterTarget.isWithin(slot, observation.playerPosition) }?.let { slot ->
                return PlayerCommand.UseTalent(slot.slot, clusterTarget)
            }
        }

        offensiveTalentOrder.forEach { talentId ->
            val slot = availableTalent(observation, talentId) ?: return@forEach
            if (!slot.requiresTarget) {
                return PlayerCommand.UseTalent(slot.slot)
            }
            if (nearest.isWithin(slot, observation.playerPosition)) {
                return PlayerCommand.UseTalent(slot.slot, nearest)
            }
        }
        return null
    }

    private fun chooseMeleeOrPursuit(observation: RunObservation): PlayerCommand? {
        val hostile = nearestHostile(observation) ?: return null
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

    private fun chooseInteractablePath(observation: RunObservation): PlayerCommand? {
        val target =
            observation.visibleInteractables
                .filter(::shouldInteract)
                .sortedWith(compareBy<ObservedInteractable> { interactablePriority(it) }.thenBy { it.position.chebyshevDistanceTo(observation.playerPosition) })
                .firstOrNull()
                ?.position
                ?: return null
        if (target == observation.playerPosition) {
            return PlayerCommand.Interact
        }
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
        val explorationTarget =
            frontier
                ?: observation.map.floorPoints()
                    .asSequence()
                    .filter { point ->
                        point != observation.playerPosition &&
                            point !in observation.exploredTiles
                    }
                    .minByOrNull { it.chebyshevDistanceTo(observation.playerPosition) }
                ?: return null

        if (explorationTarget == observation.playerPosition) {
            val adjacentUnexplored =
                Point.ALL_DIRECTIONS
                    .asSequence()
                    .map { delta -> observation.playerPosition + delta }
                    .filter { point ->
                        observation.map.isInBounds(point.x, point.y) &&
                            !observation.map.blocksMovement(point.x, point.y) &&
                            point !in observation.exploredTiles
                    }.minByOrNull { point -> point.chebyshevDistanceTo(observation.playerPosition) }
            if (adjacentUnexplored != null) {
                return PlayerCommand.Move(adjacentUnexplored.deltaFrom(observation.playerPosition))
            }
        }

        val nextStep = stepToward(observation, explorationTarget) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }

    private fun nearestHostile(observation: RunObservation): Point? =
        observation.visibleHostilePositions.minByOrNull { it.chebyshevDistanceTo(observation.playerPosition) }

    private fun availableTalent(
        observation: RunObservation,
        talentId: String,
    ): TalentSlotView? =
        observation.talentSlots
            .firstOrNull { slot ->
                slot.talentId == talentId &&
                    slot.currentCooldown <= 0 &&
                    slot.resourceTypeId == observation.playerResource.typeId &&
                    slot.resourceCost <= observation.playerResource.current
            }

    private fun hostilesWithin(
        observation: RunObservation,
        radius: Int,
    ): Int = observation.visibleHostilePositions.count { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) <= radius }

    private fun clusterTarget(observation: RunObservation): Point? =
        observation.visibleHostilePositions
            .maxWithOrNull(
                compareBy<Point> { hostilesAround(it, observation.visibleHostilePositions, 1) }
                    .thenByDescending { it.chebyshevDistanceTo(observation.playerPosition) }
                    .thenByDescending { it.y }
                    .thenByDescending { it.x },
            )

    private fun hostilesAround(
        center: Point,
        hostiles: List<Point>,
        radius: Int,
    ): Int = hostiles.count { hostile -> hostile.chebyshevDistanceTo(center) <= radius }

    private fun safeBlinkTarget(
        observation: RunObservation,
        slot: TalentSlotView,
    ): Point? {
        val hostiles = observation.visibleHostilePositions
        if (hostiles.isEmpty()) {
            return null
        }
        return observation.visibleTiles
            .asSequence()
            .filter { point ->
                !observation.map.blocksMovement(point.x, point.y) &&
                    point !in observation.visibleBlockingPositions &&
                    point.isWithin(slot, observation.playerPosition)
            }
            .maxWithOrNull(
                compareBy<Point> { candidate -> hostiles.minOfOrNull { hostile -> hostile.chebyshevDistanceTo(candidate) } ?: 0 }
                    .thenBy { candidate -> candidate.chebyshevDistanceTo(observation.playerPosition) }
                    .thenBy { it.y }
                    .thenBy { it.x },
            )
    }

    private fun shouldInteract(interactable: ObservedInteractable): Boolean =
        when {
            interactable.interactionTags.any { tag -> tag in setOf("loot", "support", "gate") } -> true
            "objective" in interactable.interactionTags && "warning" !in interactable.interactionTags -> true
            else -> false
        }

    private fun interactablePriority(interactable: ObservedInteractable): Int =
        when {
            "loot" in interactable.interactionTags -> 0
            "support" in interactable.interactionTags -> 1
            "gate" in interactable.interactionTags -> 2
            "objective" in interactable.interactionTags -> 3
            "warning" in interactable.interactionTags -> 4
            else -> 5
        }

    private fun talentUpgradePriority(talentId: String): Int =
        when (talentId) {
            "power_strike", "fireball", "ice_bolt" -> 100
            "shield_bash", "frost_nova", "arcane_shield" -> 90
            "guard_stance", "unyielding", "blink" -> 80
            "sweeping_strike", "flame_wall", "ice_prison" -> 70
            "war_cry", "intimidation", "sunder_armor", "mana_surge" -> 60
            else -> 10
        }

    private val offensiveTalentOrder =
        listOf(
            "ice_prison",
            "shield_bash",
            "power_strike",
            "sunder_armor",
            "fireball",
            "ice_bolt",
            "charge",
        )

    private fun Point.isWithin(
        slot: TalentSlotView,
        origin: Point,
    ): Boolean {
        val distance = chebyshevDistanceTo(origin)
        return distance in slot.minRange..slot.range
    }
}
