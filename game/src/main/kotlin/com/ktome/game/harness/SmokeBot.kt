package com.ktome.game.harness

import com.ktome.core.item.ConsumableEffect
import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar
import com.ktome.game.PlayerCommand
import com.ktome.game.PrimaryStat
import com.ktome.game.TalentSlotView

class SmokeBot : RunBot {
    override fun decide(observation: RunObservation): PlayerCommand {
        updateNavigationHistory(observation)
        pickImmediateAction(observation)?.let { return it }
        chooseEmergencyTalent(observation)?.let { return it }
        chooseCombatTalent(observation)?.let { return it }
        chooseMeleeOrPursuit(observation)?.let { return it }
        chooseInteractablePath(observation)?.let { return it }
        chooseGroundItemPath(observation)?.let { return it }
        chooseStairOrExploreMove(observation)?.let { return it }
        return PlayerCommand.Wait
    }

    private var navigationFloor: Int? = null
    private var currentPosition: Point? = null
    private var previousPosition: Point? = null
    private val recentPositions = ArrayDeque<Point>()

    private fun pickImmediateAction(observation: RunObservation): PlayerCommand? {
        if (observation.playerStatus.statPoints > 0) {
            return PlayerCommand.AssignStat(preferredStat(observation))
        }
        if (observation.playerStatus.talentPoints > 0) {
            preferredTalentUpgrade(observation)?.let { slot -> return PlayerCommand.AssignTalent(slot.slot) }
        }
        if (observation.visibleHostilePositions.isEmpty() && observation.visibleBossPositions.isEmpty()) {
            LoadoutPlanner.preferredLoadoutCommand(observation)?.let { return it }
        }
        if (observation.inventoryItems.size < SMOKE_BOT_INVENTORY_CAPACITY && observation.visibleGroundItemPositions.any { it == observation.playerPosition }) {
            return PlayerCommand.PickUp
        }
        preferredInventoryAction(observation)?.let { return it }
        if (observation.visibleInteractables.any { interactable -> interactable.position == observation.playerPosition && shouldInteract(interactable) }) {
            return PlayerCommand.Interact
        }
        if (observation.canDescend) {
            return PlayerCommand.Descend
        }
        return null
    }

    private fun preferredStat(observation: RunObservation): PrimaryStat =
        when (observation.playerResource.typeId) {
            "MANA" -> PrimaryStat.WIL
            "ENERGY" -> PrimaryStat.DEX
            "POSITIVE_ENERGY" -> PrimaryStat.STR
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
        val bossVisible = observation.visibleBossPositions.isNotEmpty()
        val lowHealthThreshold =
            when (observation.playerResource.typeId) {
                "MANA" -> if (bossVisible) 92 else 85
                "ENERGY" -> 70
                else -> if (bossVisible) 85 else 75
            }
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * lowHealthThreshold
        val adjacentHostiles = hostilesWithin(observation, 1)
        val canEmergencyBlink = availableTalent(observation, "blink") != null
        val escapeIndex =
            observation.inventoryItems.indexOfFirst { item ->
                lowHealth &&
                    adjacentHostiles > 0 &&
                    !canEmergencyBlink &&
                    item.effect == ConsumableEffect.TELEPORT
            }
        if (escapeIndex >= 0) {
            return PlayerCommand.ActivateInventoryItem(escapeIndex)
        }
        val consumableIndex =
            observation.inventoryItems.indexOfFirst { item ->
                lowHealth &&
                    item.effect == ConsumableEffect.HEAL
            }
        if (consumableIndex >= 0) {
            return PlayerCommand.ActivateInventoryItem(consumableIndex)
        }

        val lowResourceThreshold =
            when (observation.playerResource.typeId) {
                "MANA" -> 55
                "ENERGY", "POSITIVE_ENERGY" -> 45
                else -> 35
            }
        val resourceRestoreIndex =
            observation.inventoryItems.indexOfFirst { item ->
                item.effect == ConsumableEffect.RESTORE_RESOURCE &&
                    item.resourceTypeId == observation.playerResource.typeId &&
                    observation.playerResource.current * 100 <= observation.playerResource.max * lowResourceThreshold &&
                    (bossVisible || adjacentHostiles > 0 || hostilesWithin(observation, 3) > 0)
            }
        if (resourceRestoreIndex >= 0) {
            return PlayerCommand.ActivateInventoryItem(resourceRestoreIndex)
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
        val nearbyHostiles = hostilesWithin(observation, 3)
        val bossVisible = observation.visibleBossPositions.isNotEmpty()
        val bossClose = observation.visibleBossPositions.any { boss -> boss.chebyshevDistanceTo(observation.playerPosition) <= 3 }
        val lowHealthThreshold = if (bossVisible) 80 else 65
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * lowHealthThreshold
        val criticalHealthThreshold =
            when (observation.playerResource.typeId) {
                "MANA" -> if (bossVisible) 55 else 45
                else -> if (bossVisible) 65 else 50
            }
        val criticalHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * criticalHealthThreshold
        if (!lowHealth && adjacentHostiles < 1 && nearbyHostiles < 1 && !bossVisible) {
            return null
        }

        if (criticalHealth && nearbyHostiles > 0) {
            availableTalent(observation, "holy_light")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "divine_intervention")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "purify")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }

        if (criticalHealth && adjacentHostiles > 0) {
            availableTalent(observation, "holy_shield")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "stealth")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "blink")?.let { slot ->
                safeBlinkTarget(observation, slot)?.let { target ->
                    return PlayerCommand.UseTalent(slot.slot, target)
                }
            }
            availableTalent(observation, "roll")?.let { slot ->
                safeBlinkTarget(observation, slot)?.let { target ->
                    return PlayerCommand.UseTalent(slot.slot, target)
                }
            }
        }

        if (observation.playerResource.typeId == "MANA" && criticalHealth && nearbyHostiles >= 2) {
            availableTalent(observation, "blink")?.let { slot ->
                safeBlinkTarget(observation, slot)?.let { target ->
                    return PlayerCommand.UseTalent(slot.slot, target)
                }
            }
        }

        if (observation.playerResource.typeId == "MANA" && (adjacentHostiles > 0 || (bossClose && lowHealth))) {
            availableTalent(observation, "blink")?.let { slot ->
                safeBlinkTarget(observation, slot)?.let { target ->
                    return PlayerCommand.UseTalent(slot.slot, target)
                }
            }
        }

        if (nearbyHostiles > 0 || bossVisible) {
            availableTalent(observation, "arcane_shield")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "holy_shield")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "guard_stance")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }

        availableTalent(observation, "unyielding")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }

        if (adjacentHostiles >= 2) {
            availableTalent(observation, "smoke_bomb")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "holy_aura")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "frost_nova")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            val adjacentTarget = nearestHostile(observation)?.takeIf { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) <= 1 }
            if (adjacentTarget != null) {
                availableTalent(observation, "blade_flurry")?.let { slot -> return PlayerCommand.UseTalent(slot.slot, adjacentTarget) }
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
        val bossVisible = observation.visibleBossPositions.isNotEmpty()
        val lowMana = observation.playerResource.typeId == "MANA" && observation.playerResource.current * 100 <= observation.playerResource.max * 50
        val lowHealthThreshold = if (bossVisible) 80 else 65
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * lowHealthThreshold

        if (nearbyHostiles >= 2) {
            availableTalent(observation, "smoke_bomb")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "war_cry")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "intimidation")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }
        if (nearbyHostiles >= 1 || bossVisible) {
            availableTalent(observation, "holy_shield")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "arcane_shield")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            if (!lowHealth) {
                availableTalent(observation, "devotion")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            }
            availableTalent(observation, "stealth")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }
        if (lowMana) {
            availableTalent(observation, "mana_surge")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }
        if (hostilesWithin(observation, 2) >= 2) {
            availableTalent(observation, "holy_aura")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "frost_nova")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }
        if (adjacentHostiles >= 2 && nearest.chebyshevDistanceTo(observation.playerPosition) <= 1) {
            availableTalent(observation, "blade_flurry")?.let { slot -> return PlayerCommand.UseTalent(slot.slot, nearest) }
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
        val distance = observation.playerPosition.chebyshevDistanceTo(hostile)
        val hasOffensiveTalent = offensiveTalentOrder.any { talentId -> availableTalent(observation, talentId) != null }
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 65
        val adjacentHostiles = hostilesWithin(observation, 1)
        val nearbyHostiles = hostilesWithin(observation, 3)
        val shouldRetreat =
            when (observation.playerResource.typeId) {
                "MANA" -> lowHealth && distance <= 2
                "POSITIVE_ENERGY" -> lowHealth && distance <= 2
                "ENERGY" -> lowHealth && adjacentHostiles > 0 && nearbyHostiles >= 2
                else -> false
            }
        if (shouldRetreat) {
            retreatStep(observation, hostile)?.let { retreat ->
                return PlayerCommand.Move(retreat.deltaFrom(observation.playerPosition))
            }
        }
        if (distance > 1) {
            availableTalent(observation, "roll")
                ?.takeIf { slot -> hostile.isWithin(slot, observation.playerPosition) }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot, hostile) }
        }
        if (distance > 1 && observation.playerResource.typeId == "POSITIVE_ENERGY" && !hasOffensiveTalent) {
            return null
        }
        if (distance > 4 && !hasOffensiveTalent) {
            return null
        }
        val delta = hostile.deltaFrom(observation.playerPosition)
        if (distance <= 1) {
            return PlayerCommand.Move(delta)
        }
        val nextStep = stepToward(observation, hostile) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }

    private fun chooseGroundItemPath(observation: RunObservation): PlayerCommand? {
        if (observation.inventoryItems.size >= SMOKE_BOT_INVENTORY_CAPACITY) {
            return null
        }
        val target =
            firstReachableTarget(
                observation,
                observation.visibleGroundItemPositions
                    .filter { itemPosition ->
                        itemPosition.chebyshevDistanceTo(observation.playerPosition) <= MAX_ITEM_DETOUR_DISTANCE
                    }.sortedBy { it.chebyshevDistanceTo(observation.playerPosition) },
            ) ?: return null
        val nextStep = navigationStepToward(observation, target) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }

    private fun chooseInteractablePath(observation: RunObservation): PlayerCommand? {
        val candidates =
            observation.visibleInteractables
                .filter(::shouldInteract)
                .sortedWith(compareBy<ObservedInteractable> { interactablePriority(it) }.thenBy { it.position.chebyshevDistanceTo(observation.playerPosition) })
                .map(ObservedInteractable::position)
        val target =
            firstReachableTarget(observation, candidates)
                ?: return null
        if (target == observation.playerPosition) {
            return PlayerCommand.Interact
        }
        val nextStep = navigationStepToward(observation, target) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }

    private fun chooseStairOrExploreMove(observation: RunObservation): PlayerCommand? {
        val knownDownstairs =
            firstReachableTarget(
                observation,
                observation.knownDownstairsPositions.sortedBy { it.chebyshevDistanceTo(observation.playerPosition) },
            )
        if (knownDownstairs != null) {
            val nextStep = navigationStepToward(observation, knownDownstairs) ?: return null
            return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
        }

        val frontierCandidates =
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
                .sortedBy { it.chebyshevDistanceTo(observation.playerPosition) }
                .toList()
        val unexploredCandidates =
            observation.map.floorPoints()
                .asSequence()
                .filter { point ->
                    point != observation.playerPosition &&
                        point !in observation.exploredTiles
                }.sortedBy { it.chebyshevDistanceTo(observation.playerPosition) }
                .toList()
        val patrolCandidates =
            observation.map.floorPoints()
                .asSequence()
                .filter { point ->
                    point != observation.playerPosition &&
                        !observation.map.blocksMovement(point.x, point.y)
                }.sortedWith(
                    compareBy<Point> { it.chebyshevDistanceTo(observation.playerPosition) }
                        .thenByDescending { it.y }
                        .thenByDescending { it.x },
                )
                .toList()
        val explorationTarget =
            firstReachableTarget(observation, frontierCandidates, avoidImmediateBacktrack = true)
                ?: firstReachableTarget(observation, unexploredCandidates, avoidImmediateBacktrack = true)
                ?: firstReachableTarget(observation, patrolCandidates, avoidImmediateBacktrack = true)
                ?: firstReachableTarget(observation, frontierCandidates)
                ?: firstReachableTarget(observation, unexploredCandidates)
                ?: firstReachableTarget(observation, patrolCandidates)
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

        val nextStep = navigationStepToward(observation, explorationTarget) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }

    private fun firstReachableTarget(
        observation: RunObservation,
        candidates: List<Point>,
        avoidImmediateBacktrack: Boolean = false,
    ): Point? =
        candidates
            .withIndex()
            .asSequence()
            .mapNotNull { indexedCandidate ->
                val candidate = indexedCandidate.value
                val nextStep =
                    if (candidate == observation.playerPosition) {
                        observation.playerPosition
                    } else {
                        navigationStepToward(observation, candidate)
                    }
                nextStep?.let { step ->
                    ReachableTarget(
                        candidate = candidate,
                        nextStep = step,
                        candidateOrder = indexedCandidate.index,
                    )
                }
            }.minWithOrNull(
                compareBy<ReachableTarget> { target ->
                    val avoidPosition = previousPosition.takeIf { avoidImmediateBacktrack }
                    if (target.nextStep == avoidPosition) 1 else 0
                }.thenBy { target -> recentVisitCount(target.nextStep) }
                    .thenBy { target -> recentVisitRecency(target.nextStep) }
                    .thenBy { target -> target.candidateOrder },
            )?.candidate

    private fun updateNavigationHistory(observation: RunObservation) {
        val playerPosition = observation.playerPosition
        if (navigationFloor != observation.floor) {
            navigationFloor = observation.floor
            currentPosition = playerPosition
            previousPosition = null
            recentPositions.clear()
            recentPositions.addLast(playerPosition)
            return
        }
        if (currentPosition == null) {
            currentPosition = playerPosition
            recentPositions.clear()
            recentPositions.addLast(playerPosition)
            return
        }
        if (currentPosition != playerPosition) {
            previousPosition = currentPosition
            currentPosition = playerPosition
            recentPositions.addLast(playerPosition)
            if (recentPositions.size > RECENT_POSITION_WINDOW) {
                recentPositions.removeFirst()
            }
        }
    }

    private fun recentVisitCount(position: Point): Int = recentPositions.count { recent -> recent == position }

    private fun recentVisitRecency(position: Point): Int =
        recentPositions
            .withIndex()
            .lastOrNull { (_, recent) -> recent == position }
            ?.index ?: -1

    private fun navigationStepToward(
        observation: RunObservation,
        target: Point,
    ): Point? {
        val primaryStep = stepToward(observation, target) ?: return null
        if (recentVisitCount(primaryStep) < NAVIGATION_REPEAT_THRESHOLD) {
            return primaryStep
        }
        val recentSoftBlocks =
            recentPositions
                .filter { position ->
                    position != observation.playerPosition &&
                        position != target
                }.toSet()
        if (recentSoftBlocks.isEmpty()) {
            return primaryStep
        }
        val alternateStep =
            AStar.findPath(
                map = observation.map,
                start = observation.playerPosition,
                goal = target,
                blocked = (observation.visibleBlockingPositions - target) + recentSoftBlocks,
            ).getOrNull(1)
        return alternateStep ?: primaryStep
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

    private fun retreatStep(
        observation: RunObservation,
        threat: Point,
    ): Point? {
        var bestPoint: Point? = null
        var bestThreatDistance = Int.MIN_VALUE
        var bestPlayerDistance = Int.MIN_VALUE
        var bestY = Int.MIN_VALUE
        var bestX = Int.MIN_VALUE

        Point.ALL_DIRECTIONS
            .asSequence()
            .map { delta -> observation.playerPosition + delta }
            .filter { point ->
                observation.map.isInBounds(point.x, point.y) &&
                    !observation.map.blocksMovement(point.x, point.y) &&
                    point !in observation.visibleBlockingPositions
            }.forEach { candidate ->
                val threatDistance = candidate.chebyshevDistanceTo(threat)
                val playerDistance = candidate.chebyshevDistanceTo(observation.playerPosition)
                val isBetter =
                    threatDistance > bestThreatDistance ||
                        (threatDistance == bestThreatDistance && playerDistance > bestPlayerDistance) ||
                        (threatDistance == bestThreatDistance && playerDistance == bestPlayerDistance && candidate.y > bestY) ||
                        (threatDistance == bestThreatDistance && playerDistance == bestPlayerDistance && candidate.y == bestY && candidate.x > bestX)
                if (isBetter) {
                    bestPoint = candidate
                    bestThreatDistance = threatDistance
                    bestPlayerDistance = playerDistance
                    bestY = candidate.y
                    bestX = candidate.x
                }
            }
        return bestPoint
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
            "backstab", "holy_strike", "holy_light" -> 100
            "shield_bash", "frost_nova", "arcane_shield" -> 90
            "judgment_hammer", "poison_blade", "holy_shield" -> 90
            "guard_stance", "unyielding", "blink" -> 80
            "stealth", "shadowstep", "devotion" -> 80
            "sweeping_strike", "flame_wall", "ice_prison" -> 70
            "blade_flurry", "holy_aura", "deathblow" -> 70
            "war_cry", "intimidation", "sunder_armor", "mana_surge" -> 60
            "smoke_bomb", "roll", "purify", "divine_intervention" -> 60
            else -> 10
        }

    private val offensiveTalentOrder =
        listOf(
            "ice_prison",
            "shadowstep",
            "judgment_hammer",
            "shield_bash",
            "deathblow",
            "poison_blade",
            "backstab",
            "holy_strike",
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

    private data class ReachableTarget(
        val candidate: Point,
        val nextStep: Point,
        val candidateOrder: Int,
    )

    private companion object {
        const val SMOKE_BOT_INVENTORY_CAPACITY: Int = 12
        const val RECENT_POSITION_WINDOW: Int = 8
        const val MAX_ITEM_DETOUR_DISTANCE: Int = 4
        const val NAVIGATION_REPEAT_THRESHOLD: Int = 2
    }
}
