package com.ktome.game.harness

import com.ktome.core.item.ConsumableEffect
import com.ktome.core.item.EquipSlot
import com.ktome.core.item.ItemType
import com.ktome.core.loot.RarityTier
import com.ktome.core.map.Point
import com.ktome.core.pathfinding.AStar
import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.game.AbyssalRuntimeKeys
import com.ktome.game.InventoryItemView
import com.ktome.game.PlayerCommand
import com.ktome.game.PrimaryStat
import com.ktome.game.TalentReserveView
import com.ktome.game.TalentSlotView

class SmokeBot : RunBot {
    override fun decide(observation: RunObservation): PlayerCommand {
        updateNavigationHistory(observation)
        updateThreatMemory(observation)
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
    private var lastThreatFloor: Int? = null
    private var lastThreatPosition: Point? = null
    private var lastThreatWasBoss: Boolean = false
    private val recentDroppedPositions = linkedMapOf<Point, Int>()
    private val visitedShopZones = linkedSetOf<String>()
    private val visitedSpecialShopIds = linkedSetOf<String>()
    private val recentPositions = ArrayDeque<Point>()

    private fun pickImmediateAction(observation: RunObservation): PlayerCommand? {
        observation.activeRouteSelection?.let { routeSelection ->
            return PlayerCommand.SelectRoute(preferredRouteIndex(routeSelection))
        }
        if (observation.activeShopId != null) {
            return preferredShopAction(observation) ?: run {
                if (observation.activeShopId == AbyssalRuntimeKeys.Temple.SHOP_NODE_ID) {
                    visitedSpecialShopIds += observation.activeShopId
                } else {
                    visitedShopZones += observation.zoneId
                }
                PlayerCommand.CloseShop
            }
        }
        if (hasPendingTalentDraft(observation)) {
            return PlayerCommand.ConfirmTalentDraft
        }
        expireRecentDroppedPositions(observation)
        if (observation.playerStatus.statPoints > 0) {
            return PlayerCommand.AssignStat(preferredStat(observation))
        }
        if (observation.playerStatus.talentPoints > 0) {
            preferredTalentUpgrade(observation, TalentTreeOwnerType.PROFESSION)?.let { talent -> return PlayerCommand.AssignTalent(talent.talentId) }
        }
        if (observation.playerStatus.raceTalentPoints > 0) {
            preferredTalentUpgrade(observation, TalentTreeOwnerType.RACE)?.let { talent -> return PlayerCommand.AssignTalent(talent.talentId) }
        }
        if (shouldRefreshLoadout(observation)) {
            LoadoutPlanner.preferredLoadoutCommand(observation)?.let { return it }
        }
        if (
            observation.inventoryItems.size < SMOKE_BOT_AUTO_PICKUP_LIMIT &&
            observation.visibleGroundItemPositions.any { it == observation.playerPosition } &&
            !shouldSkipFreshDropPickup(observation) &&
            canManageInventorySafely(observation)
        ) {
            return PlayerCommand.PickUp
        }
        preferredInventoryAction(observation)?.let { return it }
        if (observation.visibleInteractables.any { interactable -> interactable.position == observation.playerPosition && shouldInteract(observation, interactable) }) {
            return PlayerCommand.Interact
        }
        if (observation.canDescend) {
            return PlayerCommand.Descend
        }
        return null
    }

    private fun hasPendingTalentDraft(observation: RunObservation): Boolean =
        observation.talentSlots.any(TalentSlotView::hasPendingAllocation) ||
            observation.reserveTalents.any { talent -> talent.hasPendingAllocation }

    private fun shouldRefreshLoadout(observation: RunObservation): Boolean {
        if (observation.visibleHostilePositions.isEmpty() && observation.visibleBossPositions.isEmpty()) {
            return true
        }
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * 60
        if (lowHealth) {
            return false
        }
        if (hostilesWithin(observation, 1) > 0) {
            return false
        }
        if (hostilesWithin(observation, 2) >= 2) {
            return false
        }
        val nearestBossDistance = nearestBossDistance(observation)
        if (nearestBossDistance != null && nearestBossDistance <= 3) {
            return false
        }
        return true
    }

    private fun preferredStat(observation: RunObservation): PrimaryStat =
        when (observation.playerResource.typeId) {
            "MANA" -> PrimaryStat.WIL
            "ENERGY" -> PrimaryStat.DEX
            "POSITIVE_ENERGY" -> PrimaryStat.STR
            else -> PrimaryStat.STR
        }

    private fun preferredTalentUpgrade(
        observation: RunObservation,
        ownerType: TalentTreeOwnerType,
    ): TalentUpgradeCandidate? =
        (observation.talentSlots.map(::TalentUpgradeCandidate) + observation.reserveTalents.map(::TalentUpgradeCandidate))
            .filter { talent -> talent.ownerType == ownerType && talent.level < talent.maxLevel }
            .maxWithOrNull(
                compareBy<TalentUpgradeCandidate> { talentUpgradePriority(observation, it) }
                    .thenBy { -it.level }
                    .thenBy { it.sourcePriority },
            )

    private fun preferredInventoryAction(observation: RunObservation): PlayerCommand? {
        val bossVisible = observation.visibleBossPositions.isNotEmpty()
        val nearestBossDistance = nearestBossDistance(observation)
        val bossThreatClose = nearestBossDistance != null && nearestBossDistance <= 3
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
                    (adjacentHostiles > 0 || bossThreatClose) &&
                    !canEmergencyBlink &&
                    item.effect == ConsumableEffect.TELEPORT
            }
        if (escapeIndex >= 0) {
            return PlayerCommand.ActivateInventoryItem(escapeIndex)
        }
        availableInscription(observation, "phase_door")
            ?.takeIf { !it.requiresTarget }
            ?.takeIf {
                lowHealth && (adjacentHostiles > 0 || bossThreatClose) ||
                    (
                        observation.playerResource.typeId == "STAMINA" &&
                            bossVisible &&
                            nearestBossDistance != null &&
                            nearestBossDistance in 2..6 &&
                            observation.playerStatus.currentHp < observation.playerStatus.maxHp
                    )
            }?.let { inscription ->
                return PlayerCommand.UseInscription(inscription.hotkey)
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

        if (!canManageInventorySafely(observation)) {
            return null
        }

        inventoryCleanupCandidateIndex(observation)?.let { index ->
            recentDroppedPositions[observation.playerPosition] = observation.turnIndex
            return PlayerCommand.DropInventoryItem(index)
        }

        val equippedItemsBySlot =
            observation.inventoryItems
                .mapNotNull { item -> item.equippedSlot?.let { slot -> slot to item } }
                .toMap()
        val gearCandidate =
            observation.inventoryItems
                .asSequence()
                .filter { item ->
                    val targetSlot = item.slot
                    targetSlot != null &&
                        item.equippedSlot == null &&
                        shouldEquipGearItem(
                            observation = observation,
                            candidate = item,
                            equipped = equippedItemsBySlot[targetSlot],
                        )
                }.maxWithOrNull(
                    compareBy<InventoryItemView> { item -> gearEquipPriority(observation, item) }
                        .thenBy(InventoryItemView::index),
                )
        if (gearCandidate != null) {
            return PlayerCommand.ActivateInventoryItem(gearCandidate.index)
        }
        return null
    }

    private fun shouldEquipGearItem(
        observation: RunObservation,
        candidate: InventoryItemView,
        equipped: InventoryItemView?,
    ): Boolean {
        if (candidate.slot == null) {
            return false
        }
        if (equipped == null) {
            return true
        }
        val replacementThreshold =
            when (candidate.slot) {
                EquipSlot.WEAPON -> 80
                EquipSlot.OFF_HAND,
                EquipSlot.ARMOR,
                -> 40
            }
        return gearEquipPriority(observation, candidate) >= gearEquipPriority(observation, equipped) + replacementThreshold
    }

    private fun gearEquipPriority(
        observation: RunObservation,
        item: InventoryItemView,
    ): Int {
        val qualityScore =
            when (item.quality) {
                RarityTier.NORMAL -> 0
                RarityTier.MAGIC -> 30
                RarityTier.RARE -> 60
            }
        val desiredAffixes = desiredSynergyAffixIds(observation)
        val synergyMatchCount = item.affixIds.count(desiredAffixes::contains)
        val sustainMatchCount = item.affixIds.count(SUSTAIN_AFFIX_IDS::contains)
        val slotScore =
            when (item.slot) {
                EquipSlot.WEAPON -> if (synergyMatchCount > 0) 240 else 40
                EquipSlot.OFF_HAND -> 50
                EquipSlot.ARMOR -> 35
                null -> 0
            }
        return slotScore + qualityScore + item.affixIds.size * 10 + synergyMatchCount * 120 + sustainMatchCount * 15
    }

    private fun desiredSynergyAffixIds(observation: RunObservation): Set<String> =
        when (observation.playerResource.typeId) {
            "STAMINA" -> setOf("of_piercing")
            "MANA" -> setOf("of_flames", "of_frost")
            "ENERGY" -> setOf("of_precision", "of_shadow")
            "POSITIVE_ENERGY" -> setOf("of_smite")
            else -> emptySet()
        }

    private fun inventoryCleanupCandidateIndex(observation: RunObservation): Int? {
        if (!shouldPruneInventory(observation)) {
            return null
        }
        val extraConsumableIndex = extraConsumableDropIndex(observation)
        if (extraConsumableIndex != null) {
            return extraConsumableIndex
        }
        return observation.inventoryItems
            .asSequence()
            .filter { item -> item.equippedSlot == null && item.slot != null }
            .minWithOrNull(
                compareBy<InventoryItemView> { item -> gearKeepPriority(observation, item) }
                    .thenBy(InventoryItemView::index),
            )?.index
    }

    private fun shouldPruneInventory(observation: RunObservation): Boolean {
        if (observation.inventoryItems.size < INVENTORY_PRUNE_TRIGGER_SIZE) {
            return false
        }
        if (hostilesWithin(observation, 1) > 0) {
            return false
        }
        val nearestBossDistance = nearestBossDistance(observation)
        return nearestBossDistance == null || nearestBossDistance > 2
    }

    private fun extraConsumableDropIndex(observation: RunObservation): Int? {
        val keepBySignature =
            linkedMapOf<Pair<ConsumableEffect?, String?>, Int>().apply {
                put(ConsumableEffect.HEAL to null, 1)
                put(ConsumableEffect.TELEPORT to null, 1)
                put(ConsumableEffect.RESTORE_RESOURCE to observation.playerResource.typeId, 1)
            }
        val seenBySignature = linkedMapOf<Pair<ConsumableEffect?, String?>, Int>()
        observation.inventoryItems
            .asSequence()
            .filter { item -> item.equippedSlot == null && item.type == ItemType.CONSUMABLE }
            .sortedBy { item -> consumableKeepPriority(observation, item) }
            .forEach { item ->
                val signature = item.effect to item.resourceTypeId
                val kept = seenBySignature[signature] ?: 0
                val keepLimit = keepBySignature[signature] ?: 0
                if (kept >= keepLimit) {
                    return item.index
                }
                seenBySignature[signature] = kept + 1
            }
        return null
    }

    private fun gearKeepPriority(
        observation: RunObservation,
        item: InventoryItemView,
    ): Int {
        val desiredAffixes = desiredSynergyAffixIds(observation)
        val synergyMatchCount = item.affixIds.count(desiredAffixes::contains)
        val sustainMatchCount = item.affixIds.count(SUSTAIN_AFFIX_IDS::contains)
        val professionBaseScore =
            if (item.baseItemId in preferredWeaponBaseIds(observation)) {
                40
            } else {
                0
            }
        return gearEquipPriority(observation, item) + professionBaseScore + synergyMatchCount * 80 + sustainMatchCount * 20
    }

    private fun consumableKeepPriority(
        observation: RunObservation,
        item: InventoryItemView,
    ): Int =
        when (item.effect) {
            ConsumableEffect.HEAL -> 300
            ConsumableEffect.TELEPORT -> 260
            ConsumableEffect.RESTORE_RESOURCE ->
                if (item.resourceTypeId == observation.playerResource.typeId) {
                    240
                } else {
                    40
                }
            null -> 0
        }

    private fun preferredWeaponBaseIds(observation: RunObservation): Set<String> =
        when (observation.playerResource.typeId) {
            "STAMINA" -> setOf("forgebreaker_pick", "long_sword")
            "MANA" -> setOf("arcane_staff")
            "ENERGY" -> setOf("short_sword", "hunter_bow")
            "POSITIVE_ENERGY" -> setOf("long_sword", "war_maul")
            else -> emptySet()
        }

    private fun shouldSkipFreshDropPickup(observation: RunObservation): Boolean =
        recentDroppedPositions[observation.playerPosition]?.let { droppedAtTurn ->
            observation.turnIndex <= droppedAtTurn + RECENT_DROP_PICKUP_COOLDOWN_TURNS
        } == true

    private fun shouldAvoidDroppedGroundItem(
        observation: RunObservation,
        point: Point,
    ): Boolean =
        recentDroppedPositions[point]?.let { droppedAtTurn ->
            observation.turnIndex <= droppedAtTurn + RECENT_DROP_PICKUP_COOLDOWN_TURNS
        } == true

    private fun expireRecentDroppedPositions(observation: RunObservation) {
        recentDroppedPositions.entries.removeIf { (_, droppedAtTurn) ->
            observation.turnIndex > droppedAtTurn + RECENT_DROP_PICKUP_COOLDOWN_TURNS
        }
    }

    private fun preferredShopAction(observation: RunObservation): PlayerCommand? {
        val affordableRescueOffer =
            observation.activeShopOffers
                .asSequence()
                .filter { offer -> offer.purchasable && offer.price <= observation.shardBalance }
                .minWithOrNull(
                    compareBy<ObservedShopOffer> { offer -> rescueOfferPriority(offer) }
                        .thenBy(ObservedShopOffer::price)
                        .thenBy(ObservedShopOffer::index),
                )
                ?.takeIf { offer -> rescueOfferPriority(offer) < Int.MAX_VALUE }
        affordableRescueOffer?.let { offer -> return PlayerCommand.BuyShopOffer(offer.index) }
        val refreshOffer =
            observation.activeShopOffers
                .asSequence()
                .filter { offer ->
                    offer.purchasable &&
                        offer.price <= observation.shardBalance &&
                        "REFRESH_STOCK" in offer.tags &&
                        observation.shardBalance >= offer.price + 15
                }.minWithOrNull(compareBy<ObservedShopOffer>(ObservedShopOffer::price).thenBy(ObservedShopOffer::index))
        return refreshOffer?.let { offer -> PlayerCommand.BuyShopOffer(offer.index) }
    }

    private fun chooseEmergencyTalent(observation: RunObservation): PlayerCommand? {
        val adjacentHostiles = hostilesWithin(observation, 1)
        val nearbyHostiles = hostilesWithin(observation, 3)
        val bossVisible = observation.visibleBossPositions.isNotEmpty()
        val bossClose = observation.visibleBossPositions.any { boss -> boss.chebyshevDistanceTo(observation.playerPosition) <= 3 }
        val nearestBossDistance = nearestBossDistance(observation)
        val earlyBossPressure = hasEarlyBossPressure(observation, nearestBossDistance)
        val lowHealthThreshold =
            when (observation.playerResource.typeId) {
                "ENERGY" -> if (bossVisible) 40 else 55
                else -> if (bossVisible) 80 else 65
            }
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * lowHealthThreshold
        val criticalHealthThreshold =
            when (observation.playerResource.typeId) {
                "ENERGY" -> if (bossVisible) 28 else 35
                "MANA" -> if (bossVisible) 55 else 45
                else -> if (bossVisible) 65 else 50
            }
        val criticalHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * criticalHealthThreshold
        if (!lowHealth && adjacentHostiles < 1 && nearbyHostiles < 1 && !bossVisible) {
            return null
        }

        if (criticalHealth && nearbyHostiles > 0) {
            availableTalent(observation, "human_resolve")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "holy_light")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "divine_intervention")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "purify")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }

        if (observation.playerResource.typeId == "POSITIVE_ENERGY" && lowHealth && adjacentHostiles > 0) {
            availableTalent(observation, "human_resolve")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "holy_light")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }

        if (criticalHealth && adjacentHostiles > 0) {
            availableTalent(observation, "dwarf_forge_heart")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "dwarf_grit")
                ?.takeUnless { hasPlayerStatus(observation, "GUARD_STANCE_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "spell_parry")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "counter_seal")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "holy_shield")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "stealth")
                ?.takeUnless { hasPlayerStatus(observation, "STEALTH") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "blink")?.let { slot ->
                safeBlinkTarget(observation, slot)?.let { target ->
                    return PlayerCommand.UseTalent(slot.slot, target)
                }
            }
            availableTalent(observation, "elf_glade_step")?.let { slot ->
                safeBlinkTarget(observation, slot)?.let { target ->
                    return PlayerCommand.UseTalent(slot.slot, target)
                }
            }
            availableTalent(observation, "elf_scouting")?.let { slot ->
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
            availableTalent(observation, "elf_glade_step")?.let { slot ->
                safeBlinkTarget(observation, slot)?.let { target ->
                    return PlayerCommand.UseTalent(slot.slot, target)
                }
            }
            availableTalent(observation, "elf_scouting")?.let { slot ->
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
            availableTalent(observation, "elf_glade_step")?.let { slot ->
                safeBlinkTarget(observation, slot)?.let { target ->
                    return PlayerCommand.UseTalent(slot.slot, target)
                }
            }
            availableTalent(observation, "elf_scouting")?.let { slot ->
                safeBlinkTarget(observation, slot)?.let { target ->
                    return PlayerCommand.UseTalent(slot.slot, target)
                }
            }
        }

        val proactiveDefensePressure =
            lowHealth ||
                adjacentHostiles > 0 ||
                nearbyHostiles >= 2 ||
                (earlyBossPressure && observation.playerResource.typeId != "ENERGY") ||
                (bossVisible && observation.playerResource.typeId == "POSITIVE_ENERGY")
        if (proactiveDefensePressure) {
            availableTalent(observation, "dwarf_forge_heart")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "dwarf_grit")
                ?.takeUnless { hasPlayerStatus(observation, "GUARD_STANCE_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "spell_parry")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "counter_seal")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "arcane_shield")
                ?.takeUnless { hasPlayerStatus(observation, "ARCANE_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "flux_reversal")
                ?.takeUnless { hasPlayerStatus(observation, "ARCANE_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "holy_shield")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "guard_stance")
                ?.takeUnless { hasPlayerStatus(observation, "GUARD_STANCE_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            if (lowHealth || nearbyHostiles >= 2 || (earlyBossPressure && observation.playerResource.typeId != "ENERGY")) {
                availableTalent(observation, "stealth")
                    ?.takeUnless { hasPlayerStatus(observation, "STEALTH") }
                    ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            }
        }

        availableTalent(observation, "unyielding")
            ?.takeUnless { hasPlayerStatus(observation, "UNYIELDING_BUFF") }
            ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        availableTalent(observation, "pain_fuel")
            ?.takeUnless { hasPlayerStatus(observation, "war_cry_empower") }
            ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }

        if (adjacentHostiles >= 2) {
            availableTalent(observation, "smoke_bomb")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "holy_aura")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "frost_nova")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "aftershock")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
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
        val bossClose = observation.visibleBossPositions.any { boss -> boss.chebyshevDistanceTo(observation.playerPosition) <= 3 }
        val nearestBossDistance = nearestBossDistance(observation)
        val earlyBossPressure = hasEarlyBossPressure(observation, nearestBossDistance)
        val lowMana = observation.playerResource.typeId == "MANA" && observation.playerResource.current * 100 <= observation.playerResource.max * 50
        val lowHealthThreshold =
            when (observation.playerResource.typeId) {
                "ENERGY" -> if (bossVisible) 40 else 55
                else -> if (bossVisible) 80 else 65
            }
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * lowHealthThreshold
        val criticalHealthThreshold =
            when (observation.playerResource.typeId) {
                "ENERGY" -> if (bossVisible) 28 else 35
                else -> if (bossVisible) 65 else 50
            }
        val criticalHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * criticalHealthThreshold

        if (nearbyHostiles >= 2) {
            availableTalent(observation, "smoke_bomb")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "taunt")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "war_cry")
                ?.takeUnless { hasPlayerStatus(observation, "war_cry_empower") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "human_mastery")
                ?.takeUnless { hasPlayerStatus(observation, "war_cry_empower") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "intimidation")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }
        val proactiveDefensePressure =
            lowHealth ||
                adjacentHostiles > 0 ||
                nearbyHostiles >= 2 ||
                (earlyBossPressure && observation.playerResource.typeId != "ENERGY") ||
                (bossVisible && observation.playerResource.typeId == "POSITIVE_ENERGY")
        if (proactiveDefensePressure) {
            availableTalent(observation, "dwarf_forge_heart")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "dwarf_grit")
                ?.takeUnless { hasPlayerStatus(observation, "GUARD_STANCE_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "spell_parry")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "holy_shield")
                ?.takeUnless { hasPlayerStatus(observation, "HOLY_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "arcane_shield")
                ?.takeUnless { hasPlayerStatus(observation, "ARCANE_SHIELD_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            if (!lowHealth) {
                availableTalent(observation, "devotion")
                    ?.takeUnless { hasPlayerStatus(observation, "DEVOTION_BUFF") }
                    ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            }
            if (lowHealth || nearbyHostiles >= 2 || (earlyBossPressure && observation.playerResource.typeId != "ENERGY")) {
                availableTalent(observation, "stealth")
                    ?.takeUnless { hasPlayerStatus(observation, "STEALTH") }
                    ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            }
        }
        if (lowMana) {
            availableTalent(observation, "flux_anchor")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "balance_point")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
            availableTalent(observation, "mana_surge")
                ?.takeUnless { hasPlayerStatus(observation, "MANA_SURGE_BUFF") }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
        }
        if (hostilesWithin(observation, 2) >= 2) {
            availableTalent(observation, "flux_burst")?.let { slot -> return PlayerCommand.UseTalent(slot.slot) }
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
        if (observation.playerResource.typeId == "ENERGY" && criticalHealth && bossVisible) {
            return null
        }

        offensiveTalentOrder(observation).forEach { talentId ->
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
        val rememberedThreat = rememberedThreatPosition(observation)
        val hostile = nearestHostile(observation) ?: rememberedThreat ?: return null
        val distance = observation.playerPosition.chebyshevDistanceTo(hostile)
        val pursuingBoss =
            hostile in observation.visibleBossPositions ||
                (rememberedThreat != null && hostile == rememberedThreat && lastThreatWasBoss)
        if (
            rememberedThreat != null &&
            hostile == rememberedThreat &&
            !pursuingBoss &&
            shouldAbandonRememberedThreat(observation)
        ) {
            lastThreatPosition = null
            lastThreatWasBoss = false
            return null
        }
        val adjacentHostiles = hostilesWithin(observation, 1)
        val nearbyHostiles = hostilesWithin(observation, 3)
        val bossVisible = observation.visibleBossPositions.isNotEmpty()
        val lowHealthThreshold =
            when (observation.playerResource.typeId) {
                "ENERGY" -> if (bossVisible) 40 else 55
                else -> 65
            }
        val hasOffensiveTalent = offensiveTalentOrder(observation).any { talentId -> availableTalent(observation, talentId) != null }
        val lowHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * lowHealthThreshold
        val criticalHealthThreshold =
            when (observation.playerResource.typeId) {
                "ENERGY" -> if (bossVisible) 28 else 35
                else -> 50
            }
        val criticalHealth = observation.playerStatus.currentHp * 100 <= observation.playerStatus.maxHp * criticalHealthThreshold
        val isolatedBossPressure = bossVisible && adjacentHostiles == 0 && nearbyHostiles == 1
        val shouldSkirtTrashThreats =
            shouldPreserveRouteProgress(observation) &&
                !pursuingBoss &&
                distance <= 1 &&
                adjacentHostiles <= 1 &&
                nearbyHostiles <= 2 &&
                !lowHealth &&
                !criticalHealth
        if (shouldSkirtTrashThreats) {
            return null
        }
        val shouldRetreat =
            when (observation.playerResource.typeId) {
                "MANA" -> lowHealth && distance <= 2
                "POSITIVE_ENERGY" -> lowHealth && distance <= 2 && nearbyHostiles >= 2
                "ENERGY" -> {
                    val retreatHealth = if (isolatedBossPressure) criticalHealth else lowHealth
                    retreatHealth && (adjacentHostiles > 0 || nearbyHostiles >= 2)
                }
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
            availableTalent(observation, "elf_glade_step")
                ?.takeIf { slot -> hostile.isWithin(slot, observation.playerPosition) }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot, hostile) }
            availableTalent(observation, "elf_scouting")
                ?.takeIf { slot -> hostile.isWithin(slot, observation.playerPosition) }
                ?.let { slot -> return PlayerCommand.UseTalent(slot.slot, hostile) }
        }
        if (distance > 1 && shouldPreserveRouteProgress(observation)) {
            return null
        }
        if (distance > 1 && observation.playerResource.typeId == "POSITIVE_ENERGY" && !hasOffensiveTalent && !pursuingBoss) {
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
        if (!shouldDetourForGroundItems(observation)) {
            return null
        }
        val maxDetourDistance =
            if (shouldPreserveRouteProgress(observation) || observation.zoneId == "shattered_outpost") {
                1
            } else {
                MAX_ITEM_DETOUR_DISTANCE
            }
        if (observation.inventoryItems.size >= SMOKE_BOT_AUTO_PICKUP_LIMIT) {
            return null
        }
        val target =
            firstReachableTarget(
                observation,
                observation.visibleGroundItemPositions
                    .filter { itemPosition ->
                        !shouldAvoidDroppedGroundItem(observation, itemPosition) &&
                        itemPosition.chebyshevDistanceTo(observation.playerPosition) <= maxDetourDistance
                    }.sortedBy { it.chebyshevDistanceTo(observation.playerPosition) },
            ) ?: return null
        val nextStep = navigationStepToward(observation, target) ?: return null
        return PlayerCommand.Move(nextStep.deltaFrom(observation.playerPosition))
    }

    private fun chooseInteractablePath(observation: RunObservation): PlayerCommand? {
        val candidates =
            observation.visibleInteractables
                .filter { interactable -> shouldInteract(observation, interactable) }
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

        val roomCenterCandidates =
            observation.map.rooms
                .asSequence()
                .map { room -> room.center }
                .filter { point ->
                    point != observation.playerPosition &&
                        !observation.map.blocksMovement(point.x, point.y)
                }.sortedWith(
                    compareByDescending<Point> { point -> adjacentUnexploredCount(observation, point) }
                        .thenBy { point -> point.chebyshevDistanceTo(observation.playerPosition) }
                        .thenBy(Point::y)
                        .thenBy(Point::x),
                ).toList()
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
        val loopBreakCandidates =
            if (recentLoopDetected()) {
                val recentSet = recentPositions.toSet()
                observation.map.floorPoints()
                    .asSequence()
                    .filter { point ->
                        point != observation.playerPosition &&
                            !observation.map.blocksMovement(point.x, point.y) &&
                            point !in recentSet
                    }.sortedWith(
                        compareByDescending<Point> { point -> point.chebyshevDistanceTo(observation.playerPosition) }
                            .thenByDescending { point -> adjacentUnexploredCount(observation, point) }
                            .thenBy(Point::y)
                            .thenBy(Point::x),
                    ).toList()
            } else {
                emptyList()
            }
        val explorationTarget =
            firstReachableTarget(observation, loopBreakCandidates, avoidImmediateBacktrack = true)
                ?: firstReachableTarget(observation, roomCenterCandidates, avoidImmediateBacktrack = true)
                ?: firstReachableTarget(observation, frontierCandidates, avoidImmediateBacktrack = true)
                ?: firstReachableTarget(observation, unexploredCandidates, avoidImmediateBacktrack = true)
                ?: firstReachableTarget(observation, patrolCandidates, avoidImmediateBacktrack = true)
                ?: firstReachableTarget(observation, roomCenterCandidates)
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

    private fun updateThreatMemory(observation: RunObservation) {
        if (lastThreatFloor != observation.floor) {
            lastThreatFloor = observation.floor
            lastThreatPosition = null
            lastThreatWasBoss = false
        }

        nearestVisibleThreat(observation)?.let { threat ->
            lastThreatFloor = observation.floor
            lastThreatPosition = threat
            lastThreatWasBoss = threat in observation.visibleBossPositions
            return
        }

        val remembered = lastThreatPosition ?: return
        val rememberedVisibleAndEmpty = remembered in observation.visibleTiles && remembered !in visibleThreatPositions(observation)
        if (remembered == observation.playerPosition || (rememberedVisibleAndEmpty && shouldDropRememberedThreat(observation, remembered))) {
            lastThreatPosition = null
            lastThreatWasBoss = false
        }
    }

    private fun shouldDropRememberedThreat(
        observation: RunObservation,
        remembered: Point,
    ): Boolean =
        !lastThreatWasBoss ||
            observation.playerPosition.chebyshevDistanceTo(remembered) <= BOSS_MEMORY_CONFIRM_RADIUS

    private fun shouldAbandonRememberedThreat(observation: RunObservation): Boolean {
        if (lastThreatWasBoss || observation.visibleHostilePositions.isNotEmpty()) {
            return false
        }
        return recentLoopDetected()
    }

    private fun recentLoopDetected(): Boolean {
        val recentTail = recentPositions.toList().takeLast(8)
        return recentTail.size >= 8 && recentTail.distinct().size <= 4
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

    private fun nearestHostile(observation: RunObservation): Point? = nearestVisibleThreat(observation)

    private fun nearestVisibleThreat(observation: RunObservation): Point? =
        visibleThreatPositions(observation).minByOrNull { it.chebyshevDistanceTo(observation.playerPosition) }

    private fun rememberedThreatPosition(observation: RunObservation): Point? =
        lastThreatPosition
            ?.takeIf { lastThreatFloor == observation.floor }
            ?.takeIf { point -> point != observation.playerPosition }

    private fun hasPlayerStatus(
        observation: RunObservation,
        statusTypeId: String,
    ): Boolean =
        statusTypeId in observation.playerStatusTypeIds ||
            (
                statusTypeId == "HOLY_SHIELD_BUFF" &&
                    observation.playerStatusTypeIds.any { statusId -> statusId in AbyssalRuntimeKeys.WARD_STATUS_IDS }
            )

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

    private fun availableInscription(
        observation: RunObservation,
        inscriptionId: String,
    ): ObservedInscription? =
        observation.inscriptions.firstOrNull { inscription ->
            inscription.inscriptionId == inscriptionId &&
                inscription.cooldownRemaining <= 0
        }

    private fun nearestBossDistance(observation: RunObservation): Int? =
        observation.visibleBossPositions.minOfOrNull { boss ->
            boss.chebyshevDistanceTo(observation.playerPosition)
        }

    private fun hasEarlyBossPressure(
        observation: RunObservation,
        nearestBossDistance: Int? = nearestBossDistance(observation),
    ): Boolean =
        observation.zoneId == "shattered_outpost" &&
            observation.floor >= 2 &&
            nearestBossDistance != null &&
            nearestBossDistance <= 4

    private fun hostilesWithin(
        observation: RunObservation,
        radius: Int,
    ): Int = visibleThreatPositions(observation).count { hostile -> hostile.chebyshevDistanceTo(observation.playerPosition) <= radius }

    private fun clusterTarget(observation: RunObservation): Point? =
        visibleThreatPositions(observation).bestClusterTarget(playerPosition = observation.playerPosition)

    private fun List<Point>.bestClusterTarget(playerPosition: Point): Point? {
        var bestPoint: Point? = null
        var bestHostileCount = Int.MIN_VALUE
        var bestDistance = Int.MIN_VALUE
        var bestY = Int.MIN_VALUE
        var bestX = Int.MIN_VALUE
        for (candidate in this) {
            val hostileCount = hostilesAround(candidate, this, 1)
            val distance = candidate.chebyshevDistanceTo(playerPosition)
            if (
                hostileCount > bestHostileCount ||
                (hostileCount == bestHostileCount && distance > bestDistance) ||
                (hostileCount == bestHostileCount && distance == bestDistance && candidate.y > bestY) ||
                (hostileCount == bestHostileCount && distance == bestDistance && candidate.y == bestY && candidate.x > bestX)
            ) {
                bestPoint = candidate
                bestHostileCount = hostileCount
                bestDistance = distance
                bestY = candidate.y
                bestX = candidate.x
            }
        }
        return bestPoint
    }

    private fun hostilesAround(
        center: Point,
        hostiles: List<Point>,
        radius: Int,
    ): Int = hostiles.count { hostile -> hostile.chebyshevDistanceTo(center) <= radius }

    private fun safeBlinkTarget(
        observation: RunObservation,
        slot: TalentSlotView,
    ): Point? {
        val hostiles = visibleThreatPositions(observation)
        if (hostiles.isEmpty()) {
            return null
        }
        return observation.visibleTiles
            .asSequence()
            .filter { point ->
                point != observation.playerPosition &&
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

    private fun shouldInteract(
        observation: RunObservation,
        interactable: ObservedInteractable,
    ): Boolean =
        when {
            interactable.id == AbyssalRuntimeKeys.Temple.INTERACTABLE_ID ->
                observation.visibleHostilePositions.isEmpty() &&
                    observation.visibleBossPositions.isEmpty() &&
                    AbyssalRuntimeKeys.Temple.SHOP_NODE_ID !in visitedSpecialShopIds
            interactable.id == "merchant_stall" ->
                observation.zoneId !in visitedShopZones &&
                    observation.visibleHostilePositions.isEmpty() &&
                    observation.visibleBossPositions.isEmpty()

            interactable.interactionTags.any { tag -> tag in setOf("loot", "support", "gate") } -> true
            "objective" in interactable.interactionTags && "warning" !in interactable.interactionTags -> true
            else -> false
        }

    private fun shouldPreserveRouteProgress(observation: RunObservation): Boolean =
        observation.playerResource.typeId == "POSITIVE_ENERGY" &&
            observation.visibleBossPositions.isEmpty() &&
            !(lastThreatWasBoss && rememberedThreatPosition(observation) != null)

    private fun canManageInventorySafely(observation: RunObservation): Boolean {
        if (hostilesWithin(observation, 1) > 0) {
            return false
        }
        if (hostilesWithin(observation, 2) >= 2) {
            return false
        }
        return observation.visibleBossPositions.none { boss ->
            boss.chebyshevDistanceTo(observation.playerPosition) <= 4
        }
    }

    private fun shouldDetourForGroundItems(observation: RunObservation): Boolean {
        if (!canManageInventorySafely(observation)) {
            return false
        }
        if (observation.visibleHostilePositions.isNotEmpty() && shouldPreserveRouteProgress(observation)) {
            return false
        }
        return true
    }

    private fun unlockedTalentIds(observation: RunObservation): Set<String> =
        buildSet {
            observation.talentSlots.mapTo(this) { slot -> slot.talentId }
            observation.reserveTalents.mapTo(this) { talent -> talent.talentId }
        }

    private fun isSpellblade(observation: RunObservation): Boolean =
        unlockedTalentIds(observation).any(SPELLBLADE_TALENT_IDS::contains)

    private fun visibleThreatPositions(observation: RunObservation): List<Point> =
        (observation.visibleHostilePositions + observation.visibleBossPositions)
            .filterNot { point -> point == observation.playerPosition }
            .distinct()
            .sortedWith(compareBy<Point> { it.chebyshevDistanceTo(observation.playerPosition) }.thenBy(Point::y).thenBy(Point::x))

    private fun adjacentUnexploredCount(
        observation: RunObservation,
        point: Point,
    ): Int =
        Point.ALL_DIRECTIONS.count { delta ->
            val next = point + delta
            observation.map.isInBounds(next.x, next.y) &&
                !observation.map.blocksMovement(next.x, next.y) &&
                next !in observation.exploredTiles
        }

    private fun interactablePriority(interactable: ObservedInteractable): Int =
        when {
            interactable.id == "merchant_stall" -> 0
            "loot" in interactable.interactionTags -> 0
            "support" in interactable.interactionTags -> 1
            "gate" in interactable.interactionTags -> 2
            "objective" in interactable.interactionTags -> 3
            "warning" in interactable.interactionTags -> 4
            else -> 5
        }

    private fun rescueOfferPriority(offer: ObservedShopOffer): Int =
        when {
            "MOVEMENT" in offer.tags -> 0
            "CLEANSING" in offer.tags -> 1
            "PROTECTION" in offer.tags -> 2
            "RECOVERY" in offer.tags -> 3
            else -> Int.MAX_VALUE
        }

    private fun talentUpgradePriority(
        observation: RunObservation,
        talent: TalentUpgradeCandidate,
    ): Int =
        when (observation.playerResource.typeId) {
            "STAMINA" ->
                when (talent.talentId) {
                    "guard_stance" -> 120
                    "taunt" -> 96
                    "power_strike" -> 88
                    else -> baseTalentUpgradePriority(talent.talentId)
                }

            "MANA" ->
                when (talent.talentId) {
                    "blink" -> 120
                    "fireball" -> 88
                    else -> baseTalentUpgradePriority(talent.talentId)
                }

            "ENERGY" ->
                if (currentTalentLevel(observation, "shadow_bind") < 4) {
                    when (talent.talentId) {
                        "shadow_bind" -> 130
                        "shadowstep" -> if (currentTalentLevel(observation, "shadowstep") == 0) 129 else 72
                        "backstab" -> 34
                        "roll", "stealth", "poison_blade" -> 30
                        else -> 24
                    }
                } else {
                    when (talent.talentId) {
                        "shadow_bind" -> 120
                        "shadowstep" -> if (talent.level == 0) 118 else 72
                        "backstab" -> 74
                        "roll", "stealth", "poison_blade" -> 70
                        else -> baseTalentUpgradePriority(talent.talentId)
                    }
                }

            "POSITIVE_ENERGY" ->
                if (currentTalentLevel(observation, "holy_mark") < 4) {
                    when (talent.talentId) {
                        "holy_mark" -> 130
                        "holy_light" -> 94
                        "holy_strike" -> 42
                        "judgment_hammer" -> 38
                        "holy_shield", "devotion" -> 34
                        "purify" -> if (talent.level == 0) 32 else 28
                        else -> 24
                    }
                } else {
                    when (talent.talentId) {
                        "holy_mark" -> 120
                        "holy_light" -> 96
                        "holy_strike" -> 84
                        "judgment_hammer" -> 78
                        "holy_shield", "devotion" -> 74
                        "purify" -> if (talent.level == 0) 90 else 68
                        else -> baseTalentUpgradePriority(talent.talentId)
                    }
                }

            else -> baseTalentUpgradePriority(talent.talentId)
        }

    private fun currentTalentLevel(
        observation: RunObservation,
        talentId: String,
    ): Int =
        observation.talentSlots.firstOrNull { slot -> slot.talentId == talentId }?.level
            ?: observation.reserveTalents.firstOrNull { talent -> talent.talentId == talentId }?.level
            ?: 0

    private fun baseTalentUpgradePriority(talentId: String): Int =
        when (talentId) {
            "power_strike", "fireball", "ice_bolt" -> 100
            "backstab", "holy_strike", "holy_light" -> 100
            "arcane_edge", "mana_lunge" -> 100
            "runic_edge", "blink_strike" -> 98
            "fault_line", "pain_fuel" -> 97
            "riven_edge", "sunder_sigil" -> 96
            "linebreaker", "void_breach", "shadow_bind", "consecration" -> 95
            "shield_bash", "frost_nova", "arcane_shield" -> 90
            "judgment_hammer", "poison_blade", "holy_shield" -> 90
            "earthshaker", "inferno_orb", "eviscerate", "ritual_break" -> 85
            "aftershock", "flux_reversal" -> 84
            "spell_parry" -> 90
            "dwarf_grit", "elf_scouting" -> 85
            "guard_stance", "unyielding", "blink" -> 80
            "stealth", "shadowstep", "devotion", "holy_mark" -> 80
            "battlefield_command", "glacial_seal", "sanctuary", "ricochet_knives", "radiant_lance" -> 75
            "slaughter_drive", "balance_point", "pursuit_drive", "counter_seal" -> 74
            "human_resolve", "dwarf_forge_heart", "elf_glade_step" -> 75
            "sweeping_strike", "flame_wall", "ice_prison" -> 70
            "blade_flurry", "holy_aura", "deathblow" -> 70
            "cinder_burst", "shard_storm", "bulwark_march", "absolution", "beacon_of_zeal" -> 70
            "spell_rend", "flux_burst" -> 70
            "human_mastery" -> 65
            "war_cry", "intimidation", "sunder_armor", "mana_surge" -> 60
            "smoke_bomb", "roll", "purify", "divine_intervention" -> 60
            "flux_anchor" -> 60
            else -> 10
        }

    private fun offensiveTalentOrder(observation: RunObservation): List<String> =
        if (isSpellblade(observation)) {
            listOf(
                "sunder_sigil",
                "runic_edge",
                "blink_strike",
                "counter_seal",
                "spell_rend",
                "arcane_edge",
                "mana_lunge",
                "flux_burst",
            )
        } else if (observation.playerResource.typeId == "HATE") {
            listOf(
                "riven_edge",
                "fault_line",
                "aftershock",
                "pursuit_drive",
                "savage_hew",
                "rupture_wave",
                "reckless_slam",
                "blood_rush",
            )
        } else if (observation.playerResource.typeId == "MANA") {
            listOf(
                "fireball",
                "void_breach",
                "inferno_orb",
                "ice_bolt",
                "glacial_seal",
                "ice_prison",
                "shard_storm",
            )
        } else if (observation.playerResource.typeId == "STAMINA") {
            listOf(
                "linebreaker",
                "shield_bash",
                "power_strike",
                "taunt",
                "charge",
                "sunder_armor",
                "earthshaker",
            )
        } else if (observation.playerResource.typeId == "ENERGY") {
            listOf(
                "shadow_bind",
                "shadowstep",
                "eviscerate",
                "deathblow",
                "poison_blade",
                "backstab",
                "blade_flurry",
            )
        } else if (observation.playerResource.typeId == "POSITIVE_ENERGY") {
            listOf(
                "holy_mark",
                "judgment_hammer",
                "consecration",
                "holy_strike",
                "ritual_break",
                "holy_aura",
            )
        } else {
            listOf(
                "ice_prison",
                "shadow_bind",
                "linebreaker",
                "shadowstep",
                "consecration",
                "judgment_hammer",
                "shield_bash",
                "ritual_break",
                "earthshaker",
                "deathblow",
                "eviscerate",
                "poison_blade",
                "backstab",
                "holy_strike",
                "power_strike",
                "sunder_armor",
                "fireball",
                "ice_bolt",
                "charge",
            )
        }

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

    private data class TalentUpgradeCandidate(
        val talentId: String,
        val ownerType: TalentTreeOwnerType,
        val level: Int,
        val maxLevel: Int,
        val sourcePriority: Int,
    ) {
        constructor(slot: TalentSlotView) : this(
            talentId = slot.talentId,
            ownerType = slot.ownerType,
            level = slot.level,
            maxLevel = slot.maxLevel,
            sourcePriority = -slot.slot,
        )

        constructor(talent: TalentReserveView) : this(
            talentId = talent.talentId,
            ownerType = talent.ownerType,
            level = talent.level,
            maxLevel = talent.maxLevel,
            sourcePriority = Int.MIN_VALUE,
        )
    }

    private companion object {
        const val RECENT_POSITION_WINDOW: Int = 8
        const val MAX_ITEM_DETOUR_DISTANCE: Int = 3
        const val NAVIGATION_REPEAT_THRESHOLD: Int = 2
        const val BOSS_MEMORY_CONFIRM_RADIUS: Int = 2
        const val SMOKE_BOT_AUTO_PICKUP_LIMIT: Int = 10
        const val INVENTORY_PRUNE_TRIGGER_SIZE: Int = SMOKE_BOT_AUTO_PICKUP_LIMIT
        const val RECENT_DROP_PICKUP_COOLDOWN_TURNS: Int = 12
        val SUSTAIN_AFFIX_IDS: Set<String> = setOf("of_life", "of_regeneration", "of_cleansing")

        val SPELLBLADE_TALENT_IDS: Set<String> =
            setOf(
                "arcane_edge",
                "runic_edge",
                "sunder_sigil",
                "balance_point",
                "flux_reversal",
                "spell_rend",
                "flux_anchor",
                "flux_burst",
                "mana_lunge",
                "spell_parry",
                "blink_strike",
                "counter_seal",
            )
    }
}
