package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.core.map.Point
import com.ktome.core.snapshot.GridPointSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.TalentReserveSnapshot
import com.ktome.core.snapshot.TalentSlotSnapshot
import com.ktome.core.talent.TalentTreeOwnerType
import com.ktome.game.PrimaryStat
import com.ktome.game.PLAYER_ACTIVE_TALENT_SLOT_COUNT
import com.ktome.game.PlayerCommand
import com.ktome.game.validation.ValidationPreset

enum class UiMode {
    MAP,
    SHOP,
    WORLD_MAP,
    INVENTORY,
    LOADOUT_EDIT,
    TARGETING,
    INSPECT,
    VALIDATION,
    STAT_ASSIGN,
    TALENT_ASSIGN,
}

enum class TalentAssignFocus {
    ACTIVE,
    RESERVE,
}

enum class ShopFocus {
    BUY,
    SELL,
}

data class OverlayState(
    val mode: UiMode,
    val inventorySelection: Int = 0,
    val shopOfferSelection: Int = 0,
    val routeSelection: Int = 0,
    val shopFocus: ShopFocus = ShopFocus.BUY,
    val loadoutSlotSelection: Int = 1,
    val loadoutReserveSelection: Int = 0,
    val talentAssignFocus: TalentAssignFocus = TalentAssignFocus.ACTIVE,
    val targetingSlot: Int? = null,
    val targetingInscriptionHotkey: Int? = null,
    val targetingCursor: Point? = null,
    val inspectCursor: Point? = null,
    val validationCursor: ValidationOverlayCursor? = null,
    val validationPanel: ValidationOverlayPanelState? = null,
)

class InputHandler(
    private val input: InputSource = GdxInputSource,
    private val validationOverlayAvailability: ValidationOverlayAvailability = ValidationOverlayAvailability.DISABLED,
    private val validationPreset: ValidationPreset = ValidationPreset.CUSTOM,
    private val validationRestartNextSeedEnabled: Boolean = false,
) {
    private val overlayCloseBindings = listOf(Keys.F)
    private val repeatInitialDelayFrames = 12
    private val repeatIntervalFrames = 3
    private val movementBindings =
        linkedMapOf(
            Keys.Q to Point(-1, -1),
            Keys.W to Point(0, -1),
            Keys.E to Point(1, -1),
            Keys.A to Point(-1, 0),
            Keys.S to Point(0, 1),
            Keys.D to Point(1, 0),
            Keys.Z to Point(-1, 1),
            Keys.C to Point(1, 1),
            Keys.UP to Point(0, -1),
            Keys.DOWN to Point(0, 1),
            Keys.LEFT to Point(-1, 0),
            Keys.RIGHT to Point(1, 0),
            Keys.HOME to Point(-1, -1),
            Keys.PAGE_UP to Point(1, -1),
            Keys.END to Point(-1, 1),
            Keys.PAGE_DOWN to Point(1, 1),
            Keys.NUMPAD_7 to Point(-1, -1),
            Keys.NUMPAD_8 to Point(0, -1),
            Keys.NUMPAD_9 to Point(1, -1),
            Keys.NUMPAD_4 to Point(-1, 0),
            Keys.NUMPAD_6 to Point(1, 0),
            Keys.NUMPAD_1 to Point(-1, 1),
            Keys.NUMPAD_2 to Point(0, 1),
            Keys.NUMPAD_3 to Point(1, 1),
        )

    private val waitBindings = listOf(Keys.PERIOD, Keys.SPACE, Keys.NUMPAD_5)
    private var mode: UiMode = UiMode.MAP
    private var inventorySelection: Int = 0
    private var shopOfferSelection: Int = 0
    private var routeSelection: Int = 0
    private var shopFocus: ShopFocus = ShopFocus.BUY
    private var loadoutSlotSelection: Int = 1
    private var loadoutReserveSelection: Int = 0
    private var talentAssignFocus: TalentAssignFocus = TalentAssignFocus.ACTIVE
    private var targetingSlot: Int? = null
    private var targetingInscriptionHotkey: Int? = null
    private var targetingCursor: Point? = null
    private var inspectCursor: Point? = null
    private var validationCursor: ValidationOverlayCursor = ValidationOverlayCursor()
    private var heldMovementKey: Int? = null
    private var movementRepeatCountdown: Int = repeatInitialDelayFrames

    fun isMapMode(): Boolean = mode == UiMode.MAP

    fun overlayState(): OverlayState =
        OverlayState(
            mode = mode,
            inventorySelection = inventorySelection,
            shopOfferSelection = shopOfferSelection,
            routeSelection = routeSelection,
            shopFocus = shopFocus,
            loadoutSlotSelection = loadoutSlotSelection,
            loadoutReserveSelection = loadoutReserveSelection,
            talentAssignFocus = talentAssignFocus,
            targetingSlot = targetingSlot,
            targetingInscriptionHotkey = targetingInscriptionHotkey,
            targetingCursor = targetingCursor,
            inspectCursor = inspectCursor,
            validationCursor =
                validationCursor.takeIf { validationOverlayAvailability == ValidationOverlayAvailability.ENABLED },
        )

    fun pollCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (toggleValidationModeIfRequested(snapshot)) {
            return null
        }
        if (mode == UiMode.MAP && input.isKeyJustPressed(Keys.L)) {
            enterLoadoutEdit(snapshot)
            return null
        }
        if (mode == UiMode.MAP && input.isKeyJustPressed(Keys.X)) {
            mode = UiMode.INSPECT
            inspectCursor = defaultInspectCursor(snapshot)
            return null
        }

        reconcileMode(snapshot)
        if (mode != UiMode.MAP) {
            resetMovementRepeat()
        }
        return when (mode) {
            UiMode.MAP -> pollMapCommand(snapshot)
            UiMode.SHOP -> pollShopCommand(snapshot)
            UiMode.WORLD_MAP -> pollWorldMapCommand(snapshot)
            UiMode.INVENTORY -> pollInventoryCommand(snapshot)
            UiMode.LOADOUT_EDIT -> pollLoadoutCommand(snapshot)
            UiMode.TARGETING -> pollTargetingCommand(snapshot)
            UiMode.INSPECT -> pollInspectCommand(snapshot)
            UiMode.VALIDATION -> pollValidationCommand(snapshot)
            UiMode.STAT_ASSIGN -> pollStatAssignCommand(snapshot)
            UiMode.TALENT_ASSIGN -> pollTalentAssignCommand(snapshot)
        }
    }

    fun onCommandResult(
        snapshot: RenderSnapshot,
        command: PlayerCommand,
        consumed: Boolean,
    ) {
        when (command) {
            is PlayerCommand.UseTalent -> {
                if (command.target == null) {
                    reconcileMode(snapshot)
                    return
                }

                if (consumed) {
                    clearTargeting()
                } else {
                    mode = UiMode.TARGETING
                    targetingSlot = command.slot
                    targetingInscriptionHotkey = null
                    targetingCursor = command.target
                }
            }

            is PlayerCommand.UseInscription -> {
                if (command.target == null) {
                    reconcileMode(snapshot)
                    return
                }

                if (consumed) {
                    clearTargeting()
                } else {
                    mode = UiMode.TARGETING
                    targetingSlot = command.hotkey
                    targetingInscriptionHotkey = command.hotkey
                    targetingCursor = command.target
                }
            }

            is PlayerCommand.EquipTalentToSlot -> reconcileMode(snapshot)

            is PlayerCommand.Validation -> Unit

            is PlayerCommand.AssignStat,
            is PlayerCommand.AssignTalent,
            is PlayerCommand.RespecTalentTree,
            PlayerCommand.ConfirmTalentDraft,
            PlayerCommand.RollbackTalentDraft,
            PlayerCommand.SaveGame,
            PlayerCommand.Ascend,
            PlayerCommand.Descend,
            -> reconcileMode(snapshot)

            else -> Unit
        }
        reconcileMode(snapshot)
    }

    private fun reconcileMode(snapshot: RenderSnapshot) {
        val activeRouteSelection = snapshot.uiState.activeRouteSelection
        if (activeRouteSelection != null) {
            mode = UiMode.WORLD_MAP
            routeSelection = routeSelection.coerceIn(0, (activeRouteSelection.options.size - 1).coerceAtLeast(0))
            return
        }
        if (mode == UiMode.WORLD_MAP) {
            mode = UiMode.MAP
        }

        val activeShop = snapshot.uiState.activeShop
        if (activeShop != null) {
            if (mode == UiMode.MAP || mode == UiMode.SHOP) {
                mode = UiMode.SHOP
            }
            shopOfferSelection = shopOfferSelection.coerceIn(0, (activeShop.offers.size - 1).coerceAtLeast(0))
            inventorySelection = inventorySelection.coerceIn(0, (activeShop.sellEntries.size - 1).coerceAtLeast(0))
        } else if (mode == UiMode.SHOP) {
            mode = UiMode.MAP
        }

        when (mode) {
            UiMode.STAT_ASSIGN -> {
                if (!hasPendingStatAllocation(snapshot)) {
                    mode = UiMode.MAP
                }
            }

            UiMode.LOADOUT_EDIT -> {
                loadoutSlotSelection = loadoutSlotSelection.coerceIn(1, PLAYER_ACTIVE_TALENT_SLOT_COUNT)
                loadoutReserveSelection = loadoutReserveSelection.coerceIn(0, (snapshot.uiState.reserveTalents.size - 1).coerceAtLeast(0))
            }

            UiMode.TALENT_ASSIGN -> {
                loadoutSlotSelection = loadoutSlotSelection.coerceIn(1, PLAYER_ACTIVE_TALENT_SLOT_COUNT)
                loadoutReserveSelection = loadoutReserveSelection.coerceIn(0, (snapshot.uiState.reserveTalents.size - 1).coerceAtLeast(0))
                if (!canOpenTalentAllocation(snapshot)) {
                    mode = UiMode.MAP
                } else if (snapshot.uiState.talents.isEmpty() && snapshot.uiState.reserveTalents.isNotEmpty()) {
                    talentAssignFocus = TalentAssignFocus.RESERVE
                } else if (snapshot.uiState.reserveTalents.isEmpty()) {
                    talentAssignFocus = TalentAssignFocus.ACTIVE
                }
            }

            UiMode.SHOP,
            UiMode.WORLD_MAP,
            UiMode.MAP,
            UiMode.INVENTORY,
            UiMode.TARGETING,
            UiMode.INSPECT,
            UiMode.VALIDATION,
            -> Unit
        }

        if (mode == UiMode.VALIDATION) {
            inspectCursor =
                (inspectCursor ?: defaultInspectCursor(snapshot)).let { cursor ->
                    Point(
                        x = cursor.x.coerceIn(0, snapshot.metadata.width - 1),
                        y = cursor.y.coerceIn(0, snapshot.metadata.height - 1),
                    )
                }
        }

        if (hasPendingStatAllocation(snapshot) && mode == UiMode.MAP) {
            mode = UiMode.STAT_ASSIGN
        }
    }

    private fun pollMapCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (isSaveBinding()) {
            return PlayerCommand.SaveGame
        }

        interactionCommandAtPlayer(snapshot)?.let { command ->
            if (isInteractBinding()) {
                return command
            }
        }

        if (input.isKeyJustPressed(Keys.R)) {
            return PlayerCommand.Search
        }

        if (isDescendBinding()) {
            return PlayerCommand.Descend
        }

        if (isAscendBinding()) {
            return PlayerCommand.Ascend
        }

        val movement = pollMovementCommand()
        if (movement != null) {
            return PlayerCommand.Move(movement)
        }

        if (waitBindings.any(input::isKeyJustPressed)) {
            return PlayerCommand.Wait
        }

        if (input.isKeyJustPressed(Keys.G)) {
            return PlayerCommand.PickUp
        }

        if (input.isKeyJustPressed(Keys.I)) {
            mode = UiMode.INVENTORY
            inventorySelection = inventorySelection.coerceAtMost((snapshot.uiState.inventory.size - 1).coerceAtLeast(0))
            resetMovementRepeat()
            return null
        }

        if (input.isKeyJustPressed(Keys.T) && canOpenTalentAllocation(snapshot)) {
            enterTalentAssign(snapshot)
            return null
        }

        hotkeyInscription(snapshot)?.let { inscription ->
            if (!inscription.requiresTarget) {
                return PlayerCommand.UseInscription(inscription.hotkey)
            }
            mode = UiMode.TARGETING
            targetingSlot = inscription.hotkey
            targetingInscriptionHotkey = inscription.hotkey
            targetingCursor = playerPosition(snapshot)
            resetMovementRepeat()
            return null
        }

        hotkeySlot()?.let { slot ->
            val talent = snapshot.uiState.talents.firstOrNull { it.slot == slot } ?: return null
            if (!talent.requiresTarget) {
                return PlayerCommand.UseTalent(slot)
            }

            mode = UiMode.TARGETING
            targetingSlot = slot
            targetingInscriptionHotkey = null
            targetingCursor = defaultTargetCursor(snapshot)
            resetMovementRepeat()
        }

        return null
    }

    private fun pollShopCommand(snapshot: RenderSnapshot): PlayerCommand? {
        val shop = snapshot.uiState.activeShop ?: return null
        if (isOverlayCloseBinding() || input.isKeyJustPressed(Keys.I)) {
            return PlayerCommand.CloseShop
        }
        if (
            input.isKeyJustPressed(Keys.LEFT) ||
            input.isKeyJustPressed(Keys.RIGHT) ||
            input.isKeyJustPressed(Keys.TAB) ||
            input.isKeyJustPressed(Keys.A) ||
            input.isKeyJustPressed(Keys.D)
        ) {
            shopFocus = if (shopFocus == ShopFocus.BUY) ShopFocus.SELL else ShopFocus.BUY
            return null
        }
        if (shopFocus == ShopFocus.BUY) {
            if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
                shopOfferSelection = (shopOfferSelection - 1).coerceAtLeast(0)
                return null
            }
            if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.X)) {
                shopOfferSelection = (shopOfferSelection + 1).coerceAtMost((shop.offers.size - 1).coerceAtLeast(0))
                return null
            }
            if (
                input.isKeyJustPressed(Keys.ENTER) ||
                input.isKeyJustPressed(Keys.SPACE) ||
                input.isKeyJustPressed(Keys.E)
            ) {
                return PlayerCommand.BuyShopOffer(shopOfferSelection)
            }
            return null
        }

        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            inventorySelection = (inventorySelection - 1).coerceAtLeast(0)
            return null
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.X)) {
            inventorySelection = (inventorySelection + 1).coerceAtMost((shop.sellEntries.size - 1).coerceAtLeast(0))
            return null
        }
        if (
            input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.SPACE) ||
            input.isKeyJustPressed(Keys.E)
        ) {
            val sellEntry = shop.sellEntries.getOrNull(inventorySelection) ?: return null
            return PlayerCommand.SellInventoryItem(sellEntry.inventoryIndex)
        }
        return null
    }

    private fun pollWorldMapCommand(snapshot: RenderSnapshot): PlayerCommand? {
        val routePanel = snapshot.uiState.activeRouteSelection ?: return null
        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W) || input.isKeyJustPressed(Keys.LEFT) || input.isKeyJustPressed(Keys.A)) {
            routeSelection = (routeSelection - 1).coerceAtLeast(0)
            return null
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.X) || input.isKeyJustPressed(Keys.RIGHT) || input.isKeyJustPressed(Keys.D)) {
            routeSelection = (routeSelection + 1).coerceAtMost((routePanel.options.size - 1).coerceAtLeast(0))
            return null
        }
        when {
            input.isKeyJustPressed(Keys.NUM_1) -> return PlayerCommand.SelectRoute(0)
            input.isKeyJustPressed(Keys.NUM_2) -> return PlayerCommand.SelectRoute(1)
            input.isKeyJustPressed(Keys.NUM_3) -> return PlayerCommand.SelectRoute(2)
            input.isKeyJustPressed(Keys.NUM_4) -> return PlayerCommand.SelectRoute(3)
        }
        if (
            input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.SPACE) ||
            input.isKeyJustPressed(Keys.E)
        ) {
            return PlayerCommand.SelectRoute(routeSelection)
        }
        return null
    }

    private fun pollLoadoutCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (isOverlayCloseBinding() || input.isKeyJustPressed(Keys.L)) {
            clearLoadoutEdit()
            return null
        }

        when {
            input.isKeyJustPressed(Keys.NUM_1) -> loadoutSlotSelection = 1
            input.isKeyJustPressed(Keys.NUM_2) -> loadoutSlotSelection = 2
            input.isKeyJustPressed(Keys.NUM_3) -> loadoutSlotSelection = 3
            input.isKeyJustPressed(Keys.NUM_4) -> loadoutSlotSelection = 4
        }

        val reserveSize = snapshot.uiState.reserveTalents.size
        if (reserveSize == 0) {
            return null
        }

        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            loadoutReserveSelection = (loadoutReserveSelection - 1).coerceAtLeast(0)
            return null
        }

        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.X)) {
            loadoutReserveSelection = (loadoutReserveSelection + 1).coerceAtMost(reserveSize - 1)
            return null
        }

        if (
            input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.SPACE) ||
            input.isKeyJustPressed(Keys.E)
        ) {
            val talent = snapshot.uiState.reserveTalents.getOrNull(loadoutReserveSelection) ?: return null
            return PlayerCommand.EquipTalentToSlot(slot = loadoutSlotSelection, talentId = talent.talentId)
        }

        return null
    }

    private fun pollInspectCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (isOverlayCloseBinding() || input.isKeyJustPressed(Keys.X)) {
            clearInspect()
            return null
        }

        val cursor = inspectCursor ?: defaultInspectCursor(snapshot)
        val movement = movementBindings.entries.firstOrNull { (key, _) -> input.isKeyJustPressed(key) }?.value
        if (movement != null) {
            inspectCursor =
                Point(
                    x = (cursor.x + movement.x).coerceIn(0, snapshot.metadata.width - 1),
                    y = (cursor.y + movement.y).coerceIn(0, snapshot.metadata.height - 1),
                )
        }
        return null
    }

    private fun pollValidationCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.F9)) {
            clearValidation()
            return null
        }

        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            validationCursor = validationCursor.moveSection(delta = -1)
            return null
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.X)) {
            validationCursor = validationCursor.moveSection(delta = 1)
            return null
        }
        if (input.isKeyJustPressed(Keys.LEFT) || input.isKeyJustPressed(Keys.A)) {
            validationCursor =
                validationCursor.moveAction(
                    delta = -1,
                    actionCount = validationActionCount(validationCursor.selectedSection),
                )
            return null
        }
        if (input.isKeyJustPressed(Keys.RIGHT) || input.isKeyJustPressed(Keys.D)) {
            validationCursor =
                validationCursor.moveAction(
                    delta = 1,
                    actionCount = validationActionCount(validationCursor.selectedSection),
                )
            return null
        }

        val cursor = inspectCursor ?: defaultInspectCursor(snapshot)
        val inspectionDelta =
            when {
                input.isKeyJustPressed(Keys.I) -> Point(0, -1)
                input.isKeyJustPressed(Keys.K) -> Point(0, 1)
                input.isKeyJustPressed(Keys.J) -> Point(-1, 0)
                input.isKeyJustPressed(Keys.L) -> Point(1, 0)
                else -> null
            }
        if (inspectionDelta != null) {
            inspectCursor =
                Point(
                    x = (cursor.x + inspectionDelta.x).coerceIn(0, snapshot.metadata.width - 1),
                    y = (cursor.y + inspectionDelta.y).coerceIn(0, snapshot.metadata.height - 1),
                )
            return null
        }

        if (
            input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.SPACE) ||
            input.isKeyJustPressed(Keys.E)
        ) {
            return PlayerCommand.Validation(
                validationOverlayAction(
                    ValidationOverlaySelection(
                        preset = validationPreset,
                        restartNextSeedEnabled = validationRestartNextSeedEnabled,
                        section = validationCursor.selectedSection,
                        index = validationCursor.actionIndex(validationCursor.selectedSection),
                        inspectCursor = cursor,
                    ),
                ),
            )
        }
        return null
    }

    private fun pollInventoryCommand(snapshot: RenderSnapshot): PlayerCommand? {
        val inventorySize = snapshot.uiState.inventory.size
        if (isOverlayCloseBinding() || input.isKeyJustPressed(Keys.I)) {
            mode = UiMode.MAP
            resetMovementRepeat()
            return null
        }

        if (inventorySize == 0) {
            return null
        }

        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            inventorySelection = (inventorySelection - 1).coerceAtLeast(0)
            return null
        }

        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.X)) {
            inventorySelection = (inventorySelection + 1).coerceAtMost(inventorySize - 1)
            return null
        }

        if (
            input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.SPACE) ||
            input.isKeyJustPressed(Keys.E)
        ) {
            return PlayerCommand.ActivateInventoryItem(inventorySelection)
        }

        if (input.isKeyJustPressed(Keys.D)) {
            return PlayerCommand.DropInventoryItem(inventorySelection)
        }

        return null
    }

    private fun pollTargetingCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (isOverlayCloseBinding()) {
            clearTargeting()
            return null
        }

        val cursor = targetingCursor ?: playerPosition(snapshot)
        val movement = movementBindings.entries.firstOrNull { (key, _) -> input.isKeyJustPressed(key) }?.value
        if (movement != null) {
            targetingCursor =
                Point(
                    x = (cursor.x + movement.x).coerceIn(0, snapshot.metadata.width - 1),
                    y = (cursor.y + movement.y).coerceIn(0, snapshot.metadata.height - 1),
                )
            return null
        }

        if (input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)) {
            targetingInscriptionHotkey?.let { hotkey ->
                return PlayerCommand.UseInscription(hotkey, targetingCursor ?: playerPosition(snapshot))
            }
            return PlayerCommand.UseTalent(requireNotNull(targetingSlot), targetingCursor ?: playerPosition(snapshot))
        }

        return null
    }

    private fun pollStatAssignCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (!hasPendingStatAllocation(snapshot)) {
            mode = UiMode.MAP
            return null
        }

        return when {
            input.isKeyJustPressed(Keys.NUM_1) -> PlayerCommand.AssignStat(PrimaryStat.STR)
            input.isKeyJustPressed(Keys.NUM_2) -> PlayerCommand.AssignStat(PrimaryStat.DEX)
            input.isKeyJustPressed(Keys.NUM_3) -> PlayerCommand.AssignStat(PrimaryStat.CON)
            input.isKeyJustPressed(Keys.NUM_4) -> PlayerCommand.AssignStat(PrimaryStat.WIL)
            else -> null
        }
    }

    private fun pollTalentAssignCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (isOverlayCloseBinding() || input.isKeyJustPressed(Keys.T)) {
            mode = UiMode.MAP
            return null
        }
        if (!canOpenTalentAllocation(snapshot)) {
            mode = UiMode.MAP
            return null
        }
        if (input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)) {
            return PlayerCommand.ConfirmTalentDraft
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE) || input.isKeyJustPressed(Keys.DEL)) {
            return PlayerCommand.RollbackTalentDraft
        }
        if (input.isKeyJustPressed(Keys.R)) {
            return selectedTalentOwner(snapshot)?.let { owner ->
                PlayerCommand.RespecTalentTree(ownerType = owner.ownerType, treeOwnerId = owner.treeOwnerId)
            }
        }
        if (snapshot.uiState.reserveTalents.isNotEmpty()) {
            if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
                loadoutReserveSelection = (loadoutReserveSelection - 1).coerceAtLeast(0)
                talentAssignFocus = TalentAssignFocus.RESERVE
                return null
            }
            if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.X)) {
                loadoutReserveSelection = (loadoutReserveSelection + 1).coerceAtMost(snapshot.uiState.reserveTalents.lastIndex)
                talentAssignFocus = TalentAssignFocus.RESERVE
                return null
            }
            if (
                input.isKeyJustPressed(Keys.E) ||
                input.isKeyJustPressed(Keys.NUMPAD_ENTER)
            ) {
                selectedReserveTalent(snapshot)?.takeIf { talent -> availableTalentPoints(snapshot, talent.ownerType) > 0 }?.let { talent ->
                    talentAssignFocus = TalentAssignFocus.RESERVE
                    return PlayerCommand.AssignTalent(talent.talentId)
                }
            }
        }

        hotkeySlot()?.let { slot ->
            loadoutSlotSelection = slot
            talentAssignFocus = TalentAssignFocus.ACTIVE
            snapshot.uiState.talents
                .firstOrNull { talent -> talent.slot == slot }
                ?.takeIf { talent -> availableTalentPoints(snapshot, talent.ownerType) > 0 }
                ?.let { talent ->
                return PlayerCommand.AssignTalent(talent.talentId)
            }
            return null
        }

        return null
    }

    private fun hotkeySlot(): Int? =
        when {
            input.isKeyJustPressed(Keys.NUM_1) -> 1
            input.isKeyJustPressed(Keys.NUM_2) -> 2
            input.isKeyJustPressed(Keys.NUM_3) -> 3
            input.isKeyJustPressed(Keys.NUM_4) -> 4
            else -> null
        }

    private fun hotkeyInscription(snapshot: RenderSnapshot) =
        when {
            input.isKeyJustPressed(Keys.NUM_5) -> snapshot.uiState.inscriptions.firstOrNull { inscription -> inscription.hotkey == 5 }
            input.isKeyJustPressed(Keys.NUM_6) -> snapshot.uiState.inscriptions.firstOrNull { inscription -> inscription.hotkey == 6 }
            input.isKeyJustPressed(Keys.NUM_7) -> snapshot.uiState.inscriptions.firstOrNull { inscription -> inscription.hotkey == 7 }
            input.isKeyJustPressed(Keys.NUM_8) -> snapshot.uiState.inscriptions.firstOrNull { inscription -> inscription.hotkey == 8 }
            else -> null
        }

    private fun defaultTargetCursor(snapshot: RenderSnapshot): Point =
        snapshot.uiState.targetablePositions
            .firstOrNull()
            ?.toPoint()
            ?: playerPosition(snapshot)

    private fun defaultInspectCursor(snapshot: RenderSnapshot): Point = playerPosition(snapshot)

    private fun clearTargeting() {
        mode = UiMode.MAP
        targetingSlot = null
        targetingInscriptionHotkey = null
        targetingCursor = null
        resetMovementRepeat()
    }

    private fun clearLoadoutEdit() {
        mode = UiMode.MAP
        resetMovementRepeat()
    }

    private fun enterTalentAssign(snapshot: RenderSnapshot) {
        mode = UiMode.TALENT_ASSIGN
        loadoutSlotSelection = loadoutSlotSelection.coerceIn(1, PLAYER_ACTIVE_TALENT_SLOT_COUNT)
        loadoutReserveSelection = loadoutReserveSelection.coerceIn(0, (snapshot.uiState.reserveTalents.size - 1).coerceAtLeast(0))
        talentAssignFocus =
            if (snapshot.uiState.talents.isNotEmpty()) {
                TalentAssignFocus.ACTIVE
            } else {
                TalentAssignFocus.RESERVE
            }
        resetMovementRepeat()
    }

    private fun clearInspect() {
        mode = UiMode.MAP
        inspectCursor = null
        resetMovementRepeat()
    }

    private fun clearValidation() {
        mode = UiMode.MAP
        resetMovementRepeat()
    }

    private fun validationActionCount(section: ValidationOverlaySection): Int =
        validationOverlayActionDescriptors(
            scope =
                ValidationOverlayDescriptorScope(
                    preset = validationPreset,
                    restartMode =
                        if (validationRestartNextSeedEnabled) {
                            ValidationOverlayRestartMode.NEXT_SEED_ENABLED
                        } else {
                            ValidationOverlayRestartMode.SAME_PRESET_ONLY
                        },
                ),
            section = section,
        ).size

    private fun enterLoadoutEdit(snapshot: RenderSnapshot) {
        mode = UiMode.LOADOUT_EDIT
        loadoutSlotSelection = loadoutSlotSelection.coerceIn(1, PLAYER_ACTIVE_TALENT_SLOT_COUNT)
        loadoutReserveSelection = loadoutReserveSelection.coerceIn(0, (snapshot.uiState.reserveTalents.size - 1).coerceAtLeast(0))
        resetMovementRepeat()
    }

    private fun toggleValidationModeIfRequested(snapshot: RenderSnapshot): Boolean {
        if (input.isKeyJustPressed(Keys.F9).not()) {
            return false
        }
        if (validationOverlayAvailability != ValidationOverlayAvailability.ENABLED) {
            return false
        }
        when (mode) {
            UiMode.MAP,
            UiMode.INSPECT,
            -> {
                mode = UiMode.VALIDATION
                validationCursor = ValidationOverlayCursor()
                inspectCursor = inspectCursor ?: defaultInspectCursor(snapshot)
                resetMovementRepeat()
                return true
            }

            UiMode.VALIDATION -> {
                clearValidation()
                return true
            }

            UiMode.SHOP,
            UiMode.WORLD_MAP,
            UiMode.INVENTORY,
            UiMode.LOADOUT_EDIT,
            UiMode.TARGETING,
            UiMode.STAT_ASSIGN,
            UiMode.TALENT_ASSIGN,
            -> return false
        }
    }

    private fun isSaveBinding(): Boolean = controlPressed() && input.isKeyJustPressed(Keys.S)

    private fun isInteractBinding(): Boolean =
        input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.NUMPAD_ENTER)

    private fun isDescendBinding(): Boolean = shiftPressed() && input.isKeyJustPressed(Keys.PERIOD)

    private fun isAscendBinding(): Boolean = shiftPressed() && input.isKeyJustPressed(Keys.COMMA)

    private fun controlPressed(): Boolean = input.isKeyPressed(Keys.CONTROL_LEFT) || input.isKeyPressed(Keys.CONTROL_RIGHT)

    private fun shiftPressed(): Boolean = input.isKeyPressed(Keys.SHIFT_LEFT) || input.isKeyPressed(Keys.SHIFT_RIGHT)

    private fun hasPendingStatAllocation(snapshot: RenderSnapshot): Boolean = snapshot.uiState.playerStatus.statPoints > 0

    private fun hasPendingTalentAllocation(snapshot: RenderSnapshot): Boolean =
        snapshot.uiState.playerStatus.talentPoints > 0 ||
            snapshot.uiState.playerStatus.raceTalentPoints > 0 ||
            snapshot.uiState.talents.any(TalentSlotSnapshot::hasPendingAllocation) ||
            snapshot.uiState.reserveTalents.any(TalentReserveSnapshot::hasPendingAllocation)

    private fun canOpenTalentAllocation(snapshot: RenderSnapshot): Boolean =
        snapshot.uiState.talents.isNotEmpty() || snapshot.uiState.reserveTalents.isNotEmpty()

    private fun playerPosition(snapshot: RenderSnapshot): Point = Point(snapshot.metadata.playerX, snapshot.metadata.playerY)

    private fun selectedActiveTalent(snapshot: RenderSnapshot): TalentSlotSnapshot? =
        snapshot.uiState.talents.firstOrNull { talent -> talent.slot == loadoutSlotSelection }
            ?: snapshot.uiState.talents.firstOrNull()

    private fun selectedReserveTalent(snapshot: RenderSnapshot): TalentReserveSnapshot? =
        snapshot.uiState.reserveTalents.getOrNull(loadoutReserveSelection)

    private fun selectedTalentOwner(snapshot: RenderSnapshot): PlayerCommand.RespecTalentTree? =
        when (talentAssignFocus) {
            TalentAssignFocus.ACTIVE -> selectedActiveTalent(snapshot)?.toRespecCommand()
                ?: selectedReserveTalent(snapshot)?.toRespecCommand()

            TalentAssignFocus.RESERVE -> selectedReserveTalent(snapshot)?.toRespecCommand()
                ?: selectedActiveTalent(snapshot)?.toRespecCommand()
        }

    private fun TalentSlotSnapshot.toRespecCommand(): PlayerCommand.RespecTalentTree =
        PlayerCommand.RespecTalentTree(
            ownerType = parseOwnerType(ownerType),
            treeOwnerId = treeOwnerId,
        )

    private fun TalentReserveSnapshot.toRespecCommand(): PlayerCommand.RespecTalentTree =
        PlayerCommand.RespecTalentTree(
            ownerType = parseOwnerType(ownerType),
            treeOwnerId = treeOwnerId,
        )

    private fun parseOwnerType(ownerType: String): TalentTreeOwnerType =
        enumValueOf<TalentTreeOwnerType>(ownerType)

    private fun availableTalentPoints(
        snapshot: RenderSnapshot,
        ownerType: String,
    ): Int =
        when (parseOwnerType(ownerType)) {
            TalentTreeOwnerType.PROFESSION -> snapshot.uiState.playerStatus.talentPoints
            TalentTreeOwnerType.RACE -> snapshot.uiState.playerStatus.raceTalentPoints
        }

    private fun pollMovementCommand(): Point? {
        movementBindings.entries.firstOrNull { (key, _) -> input.isKeyJustPressed(key) }?.let { (key, delta) ->
            heldMovementKey = key
            movementRepeatCountdown = repeatInitialDelayFrames
            return delta
        }

        val key = heldMovementKey ?: return null
        if (!input.isKeyPressed(key)) {
            resetMovementRepeat()
            return null
        }

        movementRepeatCountdown -= 1
        if (movementRepeatCountdown > 0) {
            return null
        }

        movementRepeatCountdown = repeatIntervalFrames
        return movementBindings.getValue(key)
    }

    private fun resetMovementRepeat() {
        heldMovementKey = null
        movementRepeatCountdown = repeatInitialDelayFrames
    }

    private fun interactionCommandAtPlayer(snapshot: RenderSnapshot): PlayerCommand? {
        val playerPosition = playerPosition(snapshot)
        val interactable =
            snapshot.props.firstOrNull { prop -> prop.isInteractableAt(playerPosition) }
        if (interactable != null) {
            return PlayerCommand.Interact
        }
        val direction =
            snapshot.mapCells
                .firstOrNull { cell -> cell.x == playerPosition.x && cell.y == playerPosition.y }
                ?.stairDirectionId
                ?: snapshot.props
                    .firstOrNull { prop -> prop.isStairAt(playerPosition) }
                    ?.stairDirectionId

        return when (direction) {
            "UP" -> PlayerCommand.Ascend
            "DOWN" -> PlayerCommand.Descend
            else -> null
        }
    }

    private fun PropRenderSnapshot.isStairAt(point: Point): Boolean =
        propTypeId == "stairs" && x == point.x && y == point.y

    private fun PropRenderSnapshot.isInteractableAt(point: Point): Boolean =
        propTypeId != "stairs" && x == point.x && y == point.y

    private fun GridPointSnapshot.toPoint(): Point = Point(x, y)

    private fun isOverlayCloseBinding(): Boolean = overlayCloseBindings.any(input::isKeyJustPressed)
}
