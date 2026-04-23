package com.ktome.client.input

import com.badlogic.gdx.Input.Keys
import com.ktome.client.ui.combat.CombatDecisionFeedbackKeys
import com.ktome.client.ui.combat.CombatDecisionFrame
import com.ktome.client.ui.combat.CombatDecisionFrameState
import com.ktome.client.ui.combat.CombatDecisionPhase
import com.ktome.client.ui.combat.CombatDecisionValidationFixtures
import com.ktome.client.ui.combat.CombatDecisionValidationSurface
import com.ktome.client.ui.layout.ModalFrame
import com.ktome.client.ui.layout.ModalFrameKind
import com.ktome.client.ui.layout.ModalFrameLocalState
import com.ktome.client.ui.layout.ModalStack
import com.ktome.client.ui.layout.PaneFocusAnchor
import com.ktome.client.ui.layout.PaneFocusController
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
    val modalFrames: List<ModalFrame> = emptyList(),
    val paneFocusAnchor: PaneFocusAnchor = PaneFocusAnchor.WORLD,
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
    val explainPaneOpen: Boolean = false,
    val validationCursor: ValidationOverlayCursor? = null,
    val validationPanel: ValidationOverlayPanelState? = null,
    val validationCombatDecisionSurface: CombatDecisionValidationSurface? = null,
    val uiMessageKey: String? = null,
    val debugMessageKey: String? = null,
) {
    val activeModalKind: ModalFrameKind?
        get() = modalFrames.lastOrNull()?.kind
}

class InputHandler(
    private val input: InputSource = GdxInputSource,
    private val validationOverlayAvailability: ValidationOverlayAvailability = ValidationOverlayAvailability.DISABLED,
    private val validationPreset: ValidationPreset = ValidationPreset.CUSTOM,
    private val validationRestartNextSeedEnabled: Boolean = false,
) {
    private val uiMessageDisplayFrames = 90
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
    private var combatDecisionState: CombatDecisionFrameState? = null
    private var validationCombatDecisionSurface: CombatDecisionValidationSurface? = null
    private var inspectCursor: Point? = null
    private var explainPaneOpen: Boolean = false
    private var validationCursor: ValidationOverlayCursor = ValidationOverlayCursor()
    private var heldMovementKey: Int? = null
    private var movementRepeatCountdown: Int = repeatInitialDelayFrames
    private val modalStack = ModalStack()
    private val paneFocusController = PaneFocusController()
    private var uiMessageKey: String? = null
    private var uiMessageFramesRemaining: Int = 0
    private var debugMessageKey: String? = null

    fun isMapMode(): Boolean = mode == UiMode.MAP

    fun overlayState(): OverlayState =
        OverlayState(
            mode = mode,
            modalFrames = modalStack.frames(),
            paneFocusAnchor = paneFocusController.currentAnchor,
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
            explainPaneOpen = explainPaneOpen,
            validationCursor =
                validationCursor.takeIf { validationOverlayAvailability == ValidationOverlayAvailability.ENABLED },
            validationCombatDecisionSurface = validationCombatDecisionSurface,
            uiMessageKey = uiMessageKey,
            debugMessageKey = debugMessageKey,
        )

    fun pollCommand(snapshot: RenderSnapshot): PlayerCommand? {
        advanceUiMessageFrame()
        debugMessageKey = null
        reconcileMode(snapshot)
        if (toggleValidationModeIfRequested(snapshot)) {
            return null
        }
        if (mode != UiMode.MAP) {
            resetMovementRepeat()
        }
        if (isSaveBinding()) {
            return pollSaveBinding()
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
                val target = command.target
                if (target == null) {
                    reconcileMode(snapshot)
                    return
                }

                if (consumed) {
                    clearTargeting()
                } else {
                    restoreRejectedTargetingState(
                        targetingSlot = command.slot,
                        targetingInscriptionHotkey = null,
                        targetingCursor = target,
                    )
                }
            }

            is PlayerCommand.UseInscription -> {
                val target = command.target
                if (target == null) {
                    reconcileMode(snapshot)
                    return
                }

                if (consumed) {
                    clearTargeting()
                } else {
                    restoreRejectedTargetingState(
                        targetingSlot = command.hotkey,
                        targetingInscriptionHotkey = command.hotkey,
                        targetingCursor = target,
                    )
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
            if (mode != UiMode.WORLD_MAP) {
                resetActiveModalForPassiveTakeover("ui.message.force-switch.world-map")
            }
            mode = UiMode.WORLD_MAP
            routeSelection = routeSelection.coerceIn(0, (activeRouteSelection.options.size - 1).coerceAtLeast(0))
            return
        }
        if (mode == UiMode.WORLD_MAP) {
            mode = UiMode.MAP
        }

        val activeShop = snapshot.uiState.activeShop
        if (activeShop != null) {
            if (mode != UiMode.SHOP) {
                resetActiveModalForPassiveTakeover("ui.message.force-switch.shop")
            }
            mode = UiMode.SHOP
            shopOfferSelection = shopOfferSelection.coerceIn(0, (activeShop.offers.size - 1).coerceAtLeast(0))
            inventorySelection = inventorySelection.coerceIn(0, (activeShop.sellEntries.size - 1).coerceAtLeast(0))
            return
        } else if (mode == UiMode.SHOP) {
            mode = UiMode.MAP
        }

        if (hasPendingStatAllocation(snapshot)) {
            if (mode != UiMode.STAT_ASSIGN) {
                resetActiveModalForPassiveTakeover("ui.message.force-switch.stat-assign")
            }
            mode = UiMode.STAT_ASSIGN
            return
        }

        when (mode) {
            UiMode.STAT_ASSIGN -> {
                mode = UiMode.MAP
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

        if (mode != UiMode.VALIDATION) {
            updateModeFromModalStack()
        }
    }

    private fun pollMapCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.BACKSPACE) || isOverlayCloseBinding()) {
            return null
        }

        if (input.isKeyJustPressed(Keys.TAB)) {
            paneFocusController.move(if (shiftPressed()) -1 else 1)
            return null
        }

        interactionCommandAtPlayer(snapshot)?.let { command ->
            if (isInteractBinding()) {
                return command
            }
        }

        if (isInteractBinding()) {
            openCombatDecisionFrame(snapshot)
            return null
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
            inventorySelection = inventorySelection.coerceAtMost((snapshot.uiState.inventory.size - 1).coerceAtLeast(0))
            openModalFrame(
                ModalFrame(
                    kind = ModalFrameKind.INVENTORY,
                    localState = ModalFrameLocalState(inventorySelection = inventorySelection),
                ),
            )
            resetMovementRepeat()
            return null
        }

        if (input.isKeyJustPressed(Keys.L)) {
            enterLoadoutEdit(snapshot)
            return null
        }

        if (input.isKeyJustPressed(Keys.X)) {
            inspectCursor = defaultInspectCursor(snapshot)
            explainPaneOpen = false
            openModalFrame(
                ModalFrame(
                    kind = ModalFrameKind.INSPECT,
                    localState = ModalFrameLocalState(inspectCursor = inspectCursor, explainPaneOpen = false),
                ),
            )
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
            openCombatDecisionFrameForAction(snapshot, "inscription:${inscription.hotkey}")
            resetMovementRepeat()
            return null
        }

        hotkeySlot()?.let { slot ->
            val talent = snapshot.uiState.talents.firstOrNull { it.slot == slot } ?: return null
            if (!talent.requiresTarget) {
                return PlayerCommand.UseTalent(slot)
            }

            openCombatDecisionFrameForAction(snapshot, "talent:$slot")
            resetMovementRepeat()
        }

        return null
    }

    private fun pollShopCommand(snapshot: RenderSnapshot): PlayerCommand? {
        val shop = snapshot.uiState.activeShop ?: return null
        if (input.isKeyJustPressed(Keys.I)) {
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
            if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
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
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
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
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S) || input.isKeyJustPressed(Keys.RIGHT) || input.isKeyJustPressed(Keys.D)) {
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
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.L)) {
            closeAllModalFrames()
            return null
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE) || isOverlayCloseBinding()) {
            closeCurrentModalFrame()
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleModalFocus()
            return null
        }

        when {
            input.isKeyJustPressed(Keys.NUM_1) -> loadoutSlotSelection = 1
            input.isKeyJustPressed(Keys.NUM_2) -> loadoutSlotSelection = 2
            input.isKeyJustPressed(Keys.NUM_3) -> loadoutSlotSelection = 3
            input.isKeyJustPressed(Keys.NUM_4) -> loadoutSlotSelection = 4
        }
        updateTopModalState { state -> state.copy(loadoutSlotSelection = loadoutSlotSelection) }

        val reserveSize = snapshot.uiState.reserveTalents.size
        if (reserveSize == 0) {
            return null
        }

        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            loadoutReserveSelection = (loadoutReserveSelection - 1).coerceAtLeast(0)
            updateTopModalState { state -> state.copy(loadoutReserveSelection = loadoutReserveSelection) }
            return null
        }

        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
            loadoutReserveSelection = (loadoutReserveSelection + 1).coerceAtMost(reserveSize - 1)
            updateTopModalState { state -> state.copy(loadoutReserveSelection = loadoutReserveSelection) }
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
        if (input.isKeyJustPressed(Keys.ESCAPE)) {
            closeAllModalFrames()
            return null
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE) && explainPaneOpen) {
            setExplainPaneOpen(false)
            return null
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE) || isOverlayCloseBinding() || input.isKeyJustPressed(Keys.X)) {
            closeCurrentModalFrame()
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleModalFocus()
            return null
        }
        if (input.isKeyJustPressed(Keys.SLASH)) {
            if (explainPaneOpen) {
                debugMessageKey = "DEBUG inspect.panel-help.invoked"
            } else {
                setExplainPaneOpen(true)
            }
            return null
        }
        if (
            input.isKeyJustPressed(Keys.I) ||
            input.isKeyJustPressed(Keys.J) ||
            input.isKeyJustPressed(Keys.K) ||
            input.isKeyJustPressed(Keys.L)
        ) {
            return null
        }

        val activeKind = modalStack.top()?.kind
        if (activeKind == ModalFrameKind.ITEM_COMPARE || activeKind == ModalFrameKind.COMBAT_DECISION) {
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
            updateTopModalState { state -> state.copy(inspectCursor = inspectCursor) }
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
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
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
        if (
            input.isKeyJustPressed(Keys.I) ||
            input.isKeyJustPressed(Keys.J) ||
            input.isKeyJustPressed(Keys.K) ||
            input.isKeyJustPressed(Keys.L)
        ) {
            debugMessageKey = "DEBUG validation.inspect-key.noop"
            return null
        }

        if (validationCursor.selectedSection == ValidationOverlaySection.PR05_COMBAT) {
            val directSelection = combatDigitSelection()
            if (directSelection != null && directSelection > 0) {
                val selection =
                    ValidationOverlaySelection(
                        preset = validationPreset,
                        restartNextSeedEnabled = validationRestartNextSeedEnabled,
                        section = ValidationOverlaySection.PR05_COMBAT,
                        index = directSelection - 1,
                        inspectCursor = cursor,
                )
                validationOverlayActionDescriptor(selection)?.combatDecisionSurface?.let { surface ->
                    validationCursor = validationCursor.copy(pr05CombatSelection = directSelection - 1)
                    openValidationCombatDecisionSurface(snapshot, surface)
                    return null
                }
            }
        }

        if (
            input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.SPACE) ||
            input.isKeyJustPressed(Keys.E)
        ) {
            val selection =
                ValidationOverlaySelection(
                    preset = validationPreset,
                    restartNextSeedEnabled = validationRestartNextSeedEnabled,
                    section = validationCursor.selectedSection,
                    index = validationCursor.actionIndex(validationCursor.selectedSection),
                    inspectCursor = cursor,
                )
            val descriptor = validationOverlayActionDescriptor(selection) ?: return null
            descriptor.combatDecisionSurface?.let { surface ->
                openValidationCombatDecisionSurface(snapshot, surface)
                return null
            }
            return PlayerCommand.Validation(
                descriptor.requireGameAction(cursor),
            )
        }
        return null
    }

    private fun pollInventoryCommand(snapshot: RenderSnapshot): PlayerCommand? {
        when (modalStack.top()?.kind) {
            ModalFrameKind.ITEM_DETAIL -> return pollItemDetailCommand(snapshot)
            ModalFrameKind.ITEM_COMPARE -> return pollDeferredModalCommand()
            else -> Unit
        }

        val inventorySize = snapshot.uiState.inventory.size
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.I)) {
            closeAllModalFrames()
            return null
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE) || isOverlayCloseBinding()) {
            closeCurrentModalFrame()
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleModalFocus()
            return null
        }

        if (inventorySize == 0) {
            return null
        }

        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            inventorySelection = (inventorySelection - 1).coerceAtLeast(0)
            updateTopModalState { state -> state.copy(inventorySelection = inventorySelection) }
            return null
        }

        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
            inventorySelection = (inventorySelection + 1).coerceAtMost(inventorySize - 1)
            updateTopModalState { state -> state.copy(inventorySelection = inventorySelection) }
            return null
        }

        if (
            input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.SPACE)
        ) {
            pushItemDetailFrame()
            return null
        }

        if (input.isKeyJustPressed(Keys.E)) {
            return PlayerCommand.ActivateInventoryItem(inventorySelection)
        }

        if (input.isKeyJustPressed(Keys.D)) {
            return PlayerCommand.DropInventoryItem(inventorySelection)
        }

        return null
    }

    private fun pollItemDetailCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.I)) {
            closeAllModalFrames()
            return null
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE) || isOverlayCloseBinding()) {
            closeCurrentModalFrame()
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleModalFocus()
            return null
        }
        if (input.isKeyJustPressed(Keys.X) || input.isKeyJustPressed(Keys.C)) {
            pushDeferredFrame(ModalFrameKind.ITEM_COMPARE)
            return null
        }
        if (input.isKeyJustPressed(Keys.E)) {
            val selectedEntry = snapshot.uiState.inventory.getOrNull(inventorySelection) ?: return null
            return PlayerCommand.ActivateInventoryItem(selectedEntry.index)
        }
        return null
    }

    private fun pollDeferredModalCommand(): PlayerCommand? {
        if (input.isKeyJustPressed(Keys.ESCAPE)) {
            closeAllModalFrames()
            return null
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE) || isOverlayCloseBinding()) {
            closeCurrentModalFrame()
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleModalFocus()
            return null
        }
        return null
    }

    private fun pollTargetingCommand(snapshot: RenderSnapshot): PlayerCommand? {
        if (modalStack.top()?.kind == ModalFrameKind.COMBAT_DECISION) {
            return pollCombatDecisionCommand(snapshot)
        }
        if (input.isKeyJustPressed(Keys.ESCAPE)) {
            closeAllModalFrames()
            return null
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE) || isOverlayCloseBinding()) {
            closeCurrentModalFrame()
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleModalFocus()
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
            updateTopModalState { state -> state.copy(targetingCursor = targetingCursor) }
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

    private fun pollCombatDecisionCommand(snapshot: RenderSnapshot): PlayerCommand? {
        validationCombatDecisionSurface?.let { surface ->
            return pollValidationCombatDecisionCommand(snapshot, surface)
        }
        val state = combatDecisionState ?: CombatDecisionFrame.initialState.also(::setCombatDecisionState)
        return when (state.phase) {
            CombatDecisionPhase.ACTION -> pollCombatDecisionActionPhase(snapshot, state)
            CombatDecisionPhase.METHOD -> pollCombatDecisionMethodPhase(snapshot, state)
            CombatDecisionPhase.TARGET -> pollCombatDecisionTargetPhase(snapshot, state)
        }
    }

    private fun pollValidationCombatDecisionCommand(
        snapshot: RenderSnapshot,
        surface: CombatDecisionValidationSurface,
    ): PlayerCommand? {
        val state = combatDecisionState ?: CombatDecisionValidationFixtures.initialState(surface).also(::setCombatDecisionState)
        val focusCount = CombatDecisionValidationFixtures.focusCount(surface, state)
        if (input.isKeyJustPressed(Keys.ESCAPE)) {
            closeAllModalFrames()
            return null
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE)) {
            when (state.phase) {
                CombatDecisionPhase.ACTION -> closeAllModalFrames()
                CombatDecisionPhase.METHOD -> {
                    setCombatDecisionState(CombatDecisionFrame.initialState, focusIndex = 0)
                }

                CombatDecisionPhase.TARGET -> {
                    if (surface == CombatDecisionValidationSurface.METHOD) {
                        setCombatDecisionState(
                            state.copy(phase = CombatDecisionPhase.METHOD, selectedMethodId = null, skippedMethod = false),
                            focusIndex = 0,
                        )
                    } else {
                        closeAllModalFrames()
                    }
                }
            }
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleCombatDecisionFocus(focusCount)
            return null
        }
        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            moveCombatDecisionFocus(focusCount, -1)
            return null
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
            moveCombatDecisionFocus(focusCount, 1)
            return null
        }
        if (state.phase == CombatDecisionPhase.TARGET) {
            val cursor = targetingCursor ?: CombatDecisionValidationFixtures.initialCursor(snapshot, surface)
            val movement = movementBindings.entries.firstOrNull { (key, _) -> input.isKeyJustPressed(key) }?.value
            if (movement != null) {
                targetingCursor =
                    Point(
                        x = (cursor.x + movement.x).coerceIn(0, snapshot.metadata.width - 1),
                        y = (cursor.y + movement.y).coerceIn(0, snapshot.metadata.height - 1),
                    )
                updateTopModalState { localState -> localState.copy(targetingCursor = targetingCursor) }
                return null
            }
        }

        val selectedIndex = combatDigitSelection()
        val confirmPressed = input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)
        if (selectedIndex == 0 || (selectedIndex == null && !confirmPressed)) {
            return null
        }
        return when (state.phase) {
            CombatDecisionPhase.ACTION -> {
                CombatDecisionValidationFixtures.disabledMessage(surface)?.let { messageKey ->
                    showUiMessage(messageKey)
                    return null
                }
                setCombatDecisionState(
                    CombatDecisionFrameState(
                        phase = CombatDecisionPhase.METHOD,
                        selectedActionId = CombatDecisionValidationFixtures.ACTION_ID,
                        selectedMethodId = null,
                        skippedMethod = false,
                    ),
                    focusIndex = 0,
                )
                null
            }

            CombatDecisionPhase.METHOD -> {
                setCombatDecisionState(
                    CombatDecisionFrameState(
                        phase = CombatDecisionPhase.TARGET,
                        selectedActionId = CombatDecisionValidationFixtures.ACTION_ID,
                        selectedMethodId = CombatDecisionValidationFixtures.METHOD_ID,
                        skippedMethod = false,
                    ),
                    focusIndex = 0,
                )
                targetingCursor = CombatDecisionValidationFixtures.legalTargetPoint(snapshot)
                updateTopModalState { localState -> localState.copy(targetingCursor = targetingCursor) }
                null
            }

            CombatDecisionPhase.TARGET -> {
                when (surface) {
                    CombatDecisionValidationSurface.NO_LEGAL_TARGET -> showUiMessage(CombatDecisionFeedbackKeys.NO_LEGAL_TARGET)
                    CombatDecisionValidationSurface.ILLEGAL_TARGET -> showUiMessage(CombatDecisionFeedbackKeys.ILLEGAL_TARGET)
                    else -> closeAllModalFrames()
                }
                null
            }
        }
    }

    private fun pollCombatDecisionActionPhase(
        snapshot: RenderSnapshot,
        state: CombatDecisionFrameState,
    ): PlayerCommand? {
        val actions = CombatDecisionFrame.availableActions(snapshot)
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.BACKSPACE)) {
            closeAllModalFrames()
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleCombatDecisionFocus(actions.size)
            return null
        }
        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            moveCombatDecisionFocus(actions.size, -1)
            return null
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
            moveCombatDecisionFocus(actions.size, 1)
            return null
        }

        val selectedIndex = combatDigitSelection()
        if (selectedIndex == 0) {
            return null
        }
        val actionIndex =
            when {
                selectedIndex != null -> selectedIndex - 1
                input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE) -> currentCombatDecisionFocus(actions.size)
                else -> null
            } ?: return null
        val action = actions.getOrNull(actionIndex) ?: return null
        val disabledReason = CombatDecisionFrame.disabledReasonKey(snapshot, action)
        if (disabledReason != null) {
            showUiMessage(disabledReason)
            return null
        }
        if (action.methodOptions.isEmpty()) {
            showUiMessage(CombatDecisionFeedbackKeys.DISABLED_NO_METHOD)
            return null
        }
        val nextState =
            if (action.methodOptions.size == 1) {
                CombatDecisionFrameState(
                    phase = CombatDecisionPhase.TARGET,
                    selectedActionId = action.id,
                    selectedMethodId = action.methodOptions.single().id,
                    skippedMethod = true,
                )
            } else {
                CombatDecisionFrameState(
                    phase = CombatDecisionPhase.METHOD,
                    selectedActionId = action.id,
                    selectedMethodId = null,
                    skippedMethod = false,
                )
            }
        setCombatDecisionState(nextState, focusIndex = 0)
        targetingCursor = CombatDecisionFrame.legalTargets(snapshot, action).firstOrNull()?.point ?: playerPosition(snapshot)
        updateTopModalState { localState -> localState.copy(targetingCursor = targetingCursor) }
        return null
    }

    private fun pollCombatDecisionMethodPhase(
        snapshot: RenderSnapshot,
        state: CombatDecisionFrameState,
    ): PlayerCommand? {
        val action = CombatDecisionFrame.selectedAction(snapshot, state) ?: return resetCombatDecisionToAction()
        val methods = action.methodOptions
        if (input.isKeyJustPressed(Keys.ESCAPE)) {
            closeAllModalFrames()
            return null
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE)) {
            setCombatDecisionState(CombatDecisionFrame.initialState, focusIndex = 0)
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleCombatDecisionFocus(methods.size)
            return null
        }
        if (input.isKeyJustPressed(Keys.UP) || input.isKeyJustPressed(Keys.W)) {
            moveCombatDecisionFocus(methods.size, -1)
            return null
        }
        if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
            moveCombatDecisionFocus(methods.size, 1)
            return null
        }
        val selectedIndex = combatDigitSelection()
        if (selectedIndex == 0) {
            return null
        }
        val methodIndex =
            when {
                selectedIndex != null -> selectedIndex - 1
                input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE) -> currentCombatDecisionFocus(methods.size)
                else -> null
            } ?: return null
        val method = methods.getOrNull(methodIndex) ?: return null
        setCombatDecisionState(
            CombatDecisionFrameState(
                phase = CombatDecisionPhase.TARGET,
                selectedActionId = action.id,
                selectedMethodId = method.id,
                skippedMethod = false,
            ),
            focusIndex = 0,
        )
        targetingCursor = CombatDecisionFrame.legalTargets(snapshot, action).firstOrNull()?.point ?: playerPosition(snapshot)
        updateTopModalState { localState -> localState.copy(targetingCursor = targetingCursor) }
        return null
    }

    private fun pollCombatDecisionTargetPhase(
        snapshot: RenderSnapshot,
        state: CombatDecisionFrameState,
    ): PlayerCommand? {
        val action = CombatDecisionFrame.selectedAction(snapshot, state) ?: return resetCombatDecisionToAction()
        val targets: List<com.ktome.client.ui.combat.CombatTargetOption> by lazy(LazyThreadSafetyMode.NONE) {
            CombatDecisionFrame.legalTargets(snapshot, action)
        }
        if (input.isKeyJustPressed(Keys.ESCAPE)) {
            closeAllModalFrames()
            return null
        }
        if (input.isKeyJustPressed(Keys.BACKSPACE)) {
            if (state.skippedMethod) {
                setCombatDecisionState(CombatDecisionFrame.initialState, focusIndex = 0)
            } else {
                setCombatDecisionState(
                    state.copy(phase = CombatDecisionPhase.METHOD, selectedMethodId = null, skippedMethod = false),
                    focusIndex = 0,
                )
            }
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleCombatDecisionFocus(targets.size)
            targetingCursor = targets.getOrNull(currentCombatDecisionFocus(targets.size))?.point ?: targetingCursor
            updateTopModalState { localState -> localState.copy(targetingCursor = targetingCursor) }
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
            updateTopModalState { localState -> localState.copy(targetingCursor = targetingCursor) }
            return null
        }

        val selectedIndex = combatDigitSelection()
        if (selectedIndex == 0) {
            return null
        }
        val confirmPressed = input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)
        if (selectedIndex == null && !confirmPressed) {
            return null
        }
        val target =
            when {
                selectedIndex != null -> {
                    if (selectedIndex > targets.size) {
                        return null
                    }
                    targets[selectedIndex - 1]
                }
                confirmPressed -> {
                    val confirmationCursor = targetingCursor ?: targets.firstOrNull()?.point ?: playerPosition(snapshot)
                    if (targets.isEmpty()) {
                        null
                    } else {
                        targets.firstOrNull { option -> option.point == confirmationCursor }
                            ?: targets.getOrNull(currentCombatDecisionFocus(targets.size))
                    }
                }
                else -> null
            }
        if (targets.isEmpty()) {
            showUiMessage(CombatDecisionFeedbackKeys.NO_LEGAL_TARGET)
            return null
        }
        val confirmationCursor = targetingCursor ?: targets.firstOrNull()?.point ?: playerPosition(snapshot)
        if (target == null || (action.requiresTarget && target.point != confirmationCursor && selectedIndex == null)) {
            showUiMessage(CombatDecisionFeedbackKeys.ILLEGAL_TARGET)
            return null
        }
        return action.command(target.point).also {
            closeAllModalFrames()
        }
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
        if (input.isKeyJustPressed(Keys.ESCAPE) || input.isKeyJustPressed(Keys.T)) {
            closeAllModalFrames()
            return null
        }
        if (isOverlayCloseBinding()) {
            closeCurrentModalFrame()
            return null
        }
        if (input.isKeyJustPressed(Keys.TAB)) {
            cycleModalFocus()
            return null
        }
        if (!canOpenTalentAllocation(snapshot)) {
            closeAllModalFrames()
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
                updateTopModalState { state -> state.copy(loadoutReserveSelection = loadoutReserveSelection) }
                return null
            }
            if (input.isKeyJustPressed(Keys.DOWN) || input.isKeyJustPressed(Keys.S)) {
                loadoutReserveSelection = (loadoutReserveSelection + 1).coerceAtMost(snapshot.uiState.reserveTalents.lastIndex)
                talentAssignFocus = TalentAssignFocus.RESERVE
                updateTopModalState { state -> state.copy(loadoutReserveSelection = loadoutReserveSelection) }
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
        targetingSlot = null
        targetingInscriptionHotkey = null
        targetingCursor = null
        validationCombatDecisionSurface = null
        removeModalFrames(ModalFrameKind.TARGETING, ModalFrameKind.COMBAT_DECISION)
    }

    private fun restoreRejectedTargetingState(
        targetingSlot: Int,
        targetingInscriptionHotkey: Int?,
        targetingCursor: Point,
    ) {
        this.targetingSlot = targetingSlot
        this.targetingInscriptionHotkey = targetingInscriptionHotkey
        this.targetingCursor = targetingCursor
        validationCombatDecisionSurface = null
        combatDecisionState =
            CombatDecisionFrameState(
                phase = CombatDecisionPhase.TARGET,
                selectedActionId =
                    if (targetingInscriptionHotkey != null) {
                        "inscription:$targetingInscriptionHotkey"
                    } else {
                        "talent:$targetingSlot"
                    },
                selectedMethodId = "default",
                skippedMethod = true,
            )
        val updateTargetingFrame: (ModalFrameLocalState) -> ModalFrameLocalState = { localState ->
            localState.copy(
                targetingSlot = targetingSlot,
                targetingInscriptionHotkey = targetingInscriptionHotkey,
                targetingCursor = targetingCursor,
                combatDecisionState = combatDecisionState,
            )
        }
        removeModalFrames(ModalFrameKind.TARGETING)
        if (modalStack.top()?.kind == ModalFrameKind.COMBAT_DECISION) {
            updateTopModalState(updateTargetingFrame)
        } else {
            openModalFrame(
                ModalFrame(
                    kind = ModalFrameKind.COMBAT_DECISION,
                    localState = updateTargetingFrame(ModalFrameLocalState()),
                ),
            )
        }
    }

    private fun enterTalentAssign(snapshot: RenderSnapshot) {
        loadoutSlotSelection = loadoutSlotSelection.coerceIn(1, PLAYER_ACTIVE_TALENT_SLOT_COUNT)
        loadoutReserveSelection = loadoutReserveSelection.coerceIn(0, (snapshot.uiState.reserveTalents.size - 1).coerceAtLeast(0))
        talentAssignFocus =
            if (snapshot.uiState.talents.isNotEmpty()) {
                TalentAssignFocus.ACTIVE
            } else {
                TalentAssignFocus.RESERVE
            }
        openModalFrame(
            ModalFrame(
                kind = ModalFrameKind.TALENT_ASSIGN,
                localState =
                    ModalFrameLocalState(
                        loadoutSlotSelection = loadoutSlotSelection,
                        loadoutReserveSelection = loadoutReserveSelection,
                    ),
            ),
        )
    }

    private fun clearValidation() {
        mode = UiMode.MAP
        modalStack.clear()
        validationCombatDecisionSurface = null
        explainPaneOpen = false
        paneFocusController.onPassiveTakeover()
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
        loadoutSlotSelection = loadoutSlotSelection.coerceIn(1, PLAYER_ACTIVE_TALENT_SLOT_COUNT)
        loadoutReserveSelection = loadoutReserveSelection.coerceIn(0, (snapshot.uiState.reserveTalents.size - 1).coerceAtLeast(0))
        openModalFrame(
            ModalFrame(
                kind = ModalFrameKind.LOADOUT_EDIT,
                localState =
                    ModalFrameLocalState(
                        loadoutSlotSelection = loadoutSlotSelection,
                        loadoutReserveSelection = loadoutReserveSelection,
                    ),
            ),
        )
    }

    private fun pollSaveBinding(): PlayerCommand? =
        when (modalStack.top()?.kind) {
            ModalFrameKind.TARGETING -> {
                showUiMessage("ui.message.save.blocked-in-targeting")
                null
            }

            ModalFrameKind.COMBAT_DECISION -> {
                showUiMessage("ui.message.save.blocked-in-combat-decision")
                null
            }

            else ->
                when (mode) {
                    UiMode.VALIDATION -> {
                        showUiMessage("ui.message.save.blocked-in-validation")
                        null
                    }

                    UiMode.TARGETING -> {
                        showUiMessage("ui.message.save.blocked-in-targeting")
                        null
                    }

                    else -> PlayerCommand.SaveGame
                }
        }

    private fun openModalFrame(frame: ModalFrame): Boolean {
        if (!modalStack.canPush()) {
            showUiMessage("ui.message.modal.stack-overflow")
            return false
        }
        modalStack.push(frame)
        paneFocusController.onModalOpened()
        updateModeFromModalStack()
        resetMovementRepeat()
        return true
    }

    private fun pushItemDetailFrame() {
        val selected = inventorySelection
        openModalFrame(
            ModalFrame(
                kind = ModalFrameKind.ITEM_DETAIL,
                localState = ModalFrameLocalState(inventorySelection = selected),
            ),
        )
    }

    private fun pushDeferredFrame(kind: ModalFrameKind) {
        openModalFrame(
            ModalFrame(
                kind = kind,
                localState = ModalFrameLocalState(inventorySelection = inventorySelection),
            ),
        )
    }

    private fun openCombatDecisionFrame(snapshot: RenderSnapshot): Boolean {
        val actions = CombatDecisionFrame.availableActions(snapshot)
        if (actions.isEmpty()) {
            showUiMessage(CombatDecisionFeedbackKeys.NO_AVAILABLE_ACTION)
            return false
        }
        validationCombatDecisionSurface = null
        combatDecisionState = CombatDecisionFrame.initialState
        targetingCursor = playerPosition(snapshot)
        return openModalFrame(
            ModalFrame(
                kind = ModalFrameKind.COMBAT_DECISION,
                localState =
                    ModalFrameLocalState(
                        targetingCursor = targetingCursor,
                        combatDecisionState = combatDecisionState,
                    ),
            ),
        )
    }

    private fun openValidationCombatDecisionSurface(
        snapshot: RenderSnapshot,
        surface: CombatDecisionValidationSurface,
    ): Boolean {
        modalStack.clear()
        validationCombatDecisionSurface = surface
        combatDecisionState = CombatDecisionValidationFixtures.initialState(surface)
        targetingCursor = CombatDecisionValidationFixtures.initialCursor(snapshot, surface)
        return openModalFrame(
            ModalFrame(
                kind = ModalFrameKind.COMBAT_DECISION,
                localState =
                    ModalFrameLocalState(
                        targetingCursor = targetingCursor,
                        combatDecisionState = combatDecisionState,
                    ),
            ),
        )
    }

    private fun openCombatDecisionFrameForAction(
        snapshot: RenderSnapshot,
        actionId: String,
    ): Boolean {
        val action = CombatDecisionFrame.availableActions(snapshot).firstOrNull { candidate -> candidate.id == actionId }
        if (action == null) {
            showUiMessage(CombatDecisionFeedbackKeys.NO_AVAILABLE_ACTION)
            return false
        }
        CombatDecisionFrame.disabledReasonKey(snapshot, action)?.let { reasonKey ->
            showUiMessage(reasonKey)
            return false
        }
        val methods = action.methodOptions
        if (methods.isEmpty()) {
            showUiMessage(CombatDecisionFeedbackKeys.DISABLED_NO_METHOD)
            return false
        }
        val actionIndex = action.commandIndex
        if (actionId.startsWith("inscription:")) {
            targetingSlot = actionIndex
            targetingInscriptionHotkey = actionIndex
        } else {
            targetingSlot = actionIndex
            targetingInscriptionHotkey = null
        }
        validationCombatDecisionSurface = null
        combatDecisionState =
            if (methods.size == 1) {
                CombatDecisionFrameState(
                    phase = CombatDecisionPhase.TARGET,
                    selectedActionId = action.id,
                    selectedMethodId = methods.single().id,
                    skippedMethod = true,
                )
            } else {
                CombatDecisionFrameState(
                    phase = CombatDecisionPhase.METHOD,
                    selectedActionId = action.id,
                    selectedMethodId = null,
                    skippedMethod = false,
                )
            }
        targetingCursor = CombatDecisionFrame.legalTargets(snapshot, action).firstOrNull()?.point ?: playerPosition(snapshot)
        return openModalFrame(
            ModalFrame(
                kind = ModalFrameKind.COMBAT_DECISION,
                localState =
                    ModalFrameLocalState(
                        targetingCursor = targetingCursor,
                        combatDecisionState = combatDecisionState,
                    ),
            ),
        )
    }

    private fun closeCurrentModalFrame() {
        val popped = modalStack.pop()
        val targetingFrameStillActive = modalStack.frames().any { frame -> frame.kind == ModalFrameKind.TARGETING }
        if (
            popped?.kind == ModalFrameKind.TARGETING ||
            (popped?.kind == ModalFrameKind.COMBAT_DECISION && !targetingFrameStillActive)
        ) {
            targetingSlot = null
            targetingInscriptionHotkey = null
            targetingCursor = null
            combatDecisionState = null
            validationCombatDecisionSurface = null
        }
        if (popped?.kind == ModalFrameKind.INSPECT) {
            inspectCursor = null
            explainPaneOpen = false
        }
        if (modalStack.isEmpty) {
            paneFocusController.onModalClosed()
        }
        updateModeFromModalStack()
        resetMovementRepeat()
    }

    private fun closeAllModalFrames() {
        modalStack.clear()
        targetingSlot = null
        targetingInscriptionHotkey = null
        targetingCursor = null
        combatDecisionState = null
        validationCombatDecisionSurface = null
        inspectCursor = null
        explainPaneOpen = false
        paneFocusController.onModalClosed()
        updateModeFromModalStack()
        resetMovementRepeat()
    }

    private fun resetActiveModalForPassiveTakeover(messageKey: String) {
        showUiMessage(messageKey)
        modalStack.clear()
        targetingSlot = null
        targetingInscriptionHotkey = null
        targetingCursor = null
        combatDecisionState = null
        validationCombatDecisionSurface = null
        inspectCursor = null
        explainPaneOpen = false
        paneFocusController.onPassiveTakeover()
        resetMovementRepeat()
    }

    private fun removeModalFrames(vararg kinds: ModalFrameKind) {
        if (modalStack.isEmpty) {
            mode = UiMode.MAP
            resetMovementRepeat()
            return
        }
        val removedKinds = kinds.toSet()
        val retained = modalStack.frames().filterNot { frame -> frame.kind in removedKinds }
        if (ModalFrameKind.COMBAT_DECISION in removedKinds) {
            validationCombatDecisionSurface = null
        }
        modalStack.clear()
        retained.forEach(modalStack::push)
        if (modalStack.isEmpty) {
            paneFocusController.onModalClosed()
        }
        updateModeFromModalStack()
        resetMovementRepeat()
    }

    private fun showUiMessage(messageKey: String) {
        uiMessageKey = messageKey
        uiMessageFramesRemaining = uiMessageDisplayFrames
    }

    private fun advanceUiMessageFrame() {
        if (uiMessageFramesRemaining <= 0) {
            uiMessageKey = null
            return
        }
        uiMessageFramesRemaining -= 1
        if (uiMessageFramesRemaining == 0) {
            uiMessageKey = null
        }
    }

    private fun cycleModalFocus() {
        val delta = if (shiftPressed()) -1 else 1
        updateTopModalState { state -> state.copy(focusIndex = Math.floorMod(state.focusIndex + delta, 3)) }
    }

    private fun cycleCombatDecisionFocus(size: Int) {
        moveCombatDecisionFocus(size, if (shiftPressed()) -1 else 1)
    }

    private fun moveCombatDecisionFocus(
        size: Int,
        delta: Int,
    ) {
        if (size <= 0) {
            updateTopModalState { state -> state.copy(focusIndex = 0) }
            return
        }
        updateTopModalState { state -> state.copy(focusIndex = Math.floorMod(state.focusIndex + delta, size)) }
    }

    private fun currentCombatDecisionFocus(size: Int): Int {
        if (size <= 0) {
            return 0
        }
        return modalStack.top()?.localState?.focusIndex?.coerceIn(0, size - 1) ?: 0
    }

    private fun updateTopModalState(transform: (ModalFrameLocalState) -> ModalFrameLocalState) {
        if (modalStack.isEmpty) {
            return
        }
        modalStack.replaceTop { frame -> frame.copy(localState = transform(frame.localState)) }
    }

    private fun setExplainPaneOpen(open: Boolean) {
        explainPaneOpen = open
        updateTopModalState { state -> state.copy(explainPaneOpen = open) }
    }

    private fun setCombatDecisionState(
        state: CombatDecisionFrameState,
        focusIndex: Int = currentCombatDecisionFocus(Int.MAX_VALUE),
    ) {
        combatDecisionState = state
        updateTopModalState { localState ->
            localState.copy(
                focusIndex = focusIndex.coerceAtLeast(0),
                combatDecisionState = state,
            )
        }
    }

    private fun resetCombatDecisionToAction(): PlayerCommand? {
        setCombatDecisionState(CombatDecisionFrame.initialState, focusIndex = 0)
        return null
    }

    private fun updateModeFromModalStack() {
        mode = modalStack.top()?.kind?.toUiMode() ?: UiMode.MAP
        if (mode != UiMode.INSPECT) {
            explainPaneOpen = false
        }
    }

    private fun ModalFrameKind.toUiMode(): UiMode =
        when (this) {
            ModalFrameKind.INVENTORY,
            ModalFrameKind.ITEM_DETAIL,
            ModalFrameKind.ITEM_COMPARE,
            -> UiMode.INVENTORY

            ModalFrameKind.LOADOUT_EDIT -> UiMode.LOADOUT_EDIT
            ModalFrameKind.TALENT_ASSIGN -> UiMode.TALENT_ASSIGN
            ModalFrameKind.INSPECT -> UiMode.INSPECT
            ModalFrameKind.TARGETING,
            ModalFrameKind.COMBAT_DECISION,
            -> UiMode.TARGETING
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
                modalStack.clear()
                paneFocusController.onPassiveTakeover()
                mode = UiMode.VALIDATION
                validationCursor = ValidationOverlayCursor()
                inspectCursor = inspectCursor ?: defaultInspectCursor(snapshot)
                explainPaneOpen = false
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

    private fun combatDigitSelection(): Int? =
        when {
            input.isKeyJustPressed(Keys.NUM_0) || input.isKeyJustPressed(Keys.NUMPAD_0) -> 0
            input.isKeyJustPressed(Keys.NUM_1) || input.isKeyJustPressed(Keys.NUMPAD_1) -> 1
            input.isKeyJustPressed(Keys.NUM_2) || input.isKeyJustPressed(Keys.NUMPAD_2) -> 2
            input.isKeyJustPressed(Keys.NUM_3) || input.isKeyJustPressed(Keys.NUMPAD_3) -> 3
            input.isKeyJustPressed(Keys.NUM_4) || input.isKeyJustPressed(Keys.NUMPAD_4) -> 4
            input.isKeyJustPressed(Keys.NUM_5) || input.isKeyJustPressed(Keys.NUMPAD_5) -> 5
            input.isKeyJustPressed(Keys.NUM_6) || input.isKeyJustPressed(Keys.NUMPAD_6) -> 6
            input.isKeyJustPressed(Keys.NUM_7) || input.isKeyJustPressed(Keys.NUMPAD_7) -> 7
            input.isKeyJustPressed(Keys.NUM_8) || input.isKeyJustPressed(Keys.NUMPAD_8) -> 8
            input.isKeyJustPressed(Keys.NUM_9) || input.isKeyJustPressed(Keys.NUMPAD_9) -> 9
            else -> null
        }

    private fun isInteractBinding(): Boolean =
        input.isKeyJustPressed(Keys.ENTER) ||
            input.isKeyJustPressed(Keys.NUMPAD_ENTER)

    private fun isDescendBinding(): Boolean = shiftPressed() && input.isKeyJustPressed(Keys.PERIOD)

    private fun isAscendBinding(): Boolean = shiftPressed() && input.isKeyJustPressed(Keys.COMMA)

    private fun controlPressed(): Boolean =
        input.isKeyPressed(Keys.CONTROL_LEFT) ||
            input.isKeyPressed(Keys.CONTROL_RIGHT) ||
            input.isKeyPressed(Keys.SYM)

    private fun shiftPressed(): Boolean = input.isKeyPressed(Keys.SHIFT_LEFT) || input.isKeyPressed(Keys.SHIFT_RIGHT)

    private fun hasPendingStatAllocation(snapshot: RenderSnapshot): Boolean = snapshot.uiState.playerStatus.statPoints > 0

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
