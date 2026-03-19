package com.ktome.client.render

import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.assets.VisualManifestResolver
import com.ktome.client.input.OverlayState
import com.ktome.client.input.UiMode
import com.ktome.core.snapshot.ActorRenderSnapshot
import com.ktome.core.snapshot.ActorRoleKindSnapshot
import com.ktome.core.snapshot.CellVisibilitySnapshot
import com.ktome.core.snapshot.InventoryEntrySnapshot
import com.ktome.core.snapshot.ItemRenderSnapshot
import com.ktome.core.snapshot.ItemStatModifierSnapshot
import com.ktome.core.snapshot.MapCellSnapshot
import com.ktome.core.snapshot.PropRenderSnapshot
import com.ktome.core.snapshot.RenderLogEventSnapshot
import com.ktome.core.snapshot.RenderSnapshot
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.game.i18n.Localizer

internal enum class TileTextTone {
    GOLD,
    WHITE,
    LIGHT_GRAY,
    CYAN,
    GRAY,
    GREEN,
    RED,
    BLUE,
}

internal data class TileVisualPlacement(
    val x: Int,
    val y: Int,
    val asset: ResolvedVisualAsset,
    val alpha: Float = 1f,
)

internal data class TileFogPlacement(
    val x: Int,
    val y: Int,
    val alpha: Float,
)

internal data class TileTextRow(
    val text: String,
    val tone: TileTextTone,
    val icon: ResolvedVisualAsset? = null,
    val selected: Boolean = false,
)

internal data class TileGaugeModel(
    val label: String,
    val current: Int,
    val max: Int,
    val tone: TileTextTone,
    val resourceTypeId: String,
) {
    val percent: Float =
        if (max <= 0) {
            0f
        } else {
            (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
        }

    val summary: String = "$label $current/$max"
}

internal data class TileHotbarSlotModel(
    val slot: Int,
    val label: String,
    val icon: ResolvedVisualAsset?,
    val accentIcon: ResolvedVisualAsset?,
    val resourceText: String,
    val cooldownText: String?,
)

internal data class TileHudModel(
    val playerName: String,
    val zoneName: String,
    val floorText: String,
    val hpGauge: TileGaugeModel,
    val resourceGauge: TileGaugeModel,
    val statusIcons: List<ResolvedVisualAsset>,
    val focusIcon: ResolvedVisualAsset?,
    val focusName: String?,
    val focusLines: List<String>,
    val hotbar: List<TileHotbarSlotModel>,
    val summaryText: String,
)

internal data class TileSidebarModel(
    val title: String,
    val rows: List<TileTextRow>,
)

internal data class TileRenderModel(
    val terrainTiles: List<TileVisualPlacement>,
    val propTiles: List<TileVisualPlacement>,
    val overlayTiles: List<TileVisualPlacement>,
    val actorTiles: List<TileVisualPlacement>,
    val fogTiles: List<TileFogPlacement>,
    val targetCursor: com.ktome.core.map.Point?,
    val inspectCursor: com.ktome.core.map.Point?,
    val hud: TileHudModel,
    val messageLines: List<String>,
    val sidebar: TileSidebarModel,
)

internal object TileRenderModelBuilder {
    fun build(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): TileRenderModel {
        val cellByPoint = snapshot.mapCells.associateBy { cell -> point(cell.x, cell.y) }
        val actorById = snapshot.actors.associateBy(ActorRenderSnapshot::entityId)
        val player = requireNotNull(snapshot.actors.singleOrNull { actor -> actor.isPlayer }) {
            "Expected a single player actor in render snapshot."
        }
        val terrainTiles =
            snapshot.mapCells
                .filter { cell -> cell.visibility != CellVisibilitySnapshot.HIDDEN }
                .map { cell ->
                TileVisualPlacement(
                    x = cell.x,
                    y = cell.y,
                    asset = resolveVisual(visualResolver, cell.terrainVisualKey),
                    alpha = if (cell.visibility == CellVisibilitySnapshot.EXPLORED) 0.72f else 1f,
                )
            }
        val fogTiles =
            snapshot.mapCells.mapNotNull { cell ->
                when (cell.visibility) {
                    CellVisibilitySnapshot.VISIBLE -> null
                    CellVisibilitySnapshot.EXPLORED -> TileFogPlacement(x = cell.x, y = cell.y, alpha = 0.42f)
                    CellVisibilitySnapshot.HIDDEN -> TileFogPlacement(x = cell.x, y = cell.y, alpha = 0.94f)
                }
            }
        val propTiles =
            snapshot.props.map { prop ->
                TileVisualPlacement(
                    x = prop.x,
                    y = prop.y,
                    asset = resolveVisual(visualResolver, prop.visualKey),
                )
            }
        val overlayTiles =
            snapshot.overlays.flatMap { overlay ->
                val asset = resolveVisual(visualResolver, overlay.visualKey)
                overlay.cells.map { cell ->
                    TileVisualPlacement(
                        x = cell.x,
                        y = cell.y,
                        asset = asset,
                        alpha = overlayAlpha(overlay.dangerLevel),
                    )
                }
            }
        val actorTiles =
            snapshot.actors
                .sortedBy { actor -> if (actor.isPlayer) 1 else 0 }
                .map { actor ->
                    TileVisualPlacement(
                        x = actor.x,
                        y = actor.y,
                        asset = resolveVisual(visualResolver, actor.visualKey),
                    )
                }

        return TileRenderModel(
            terrainTiles = terrainTiles,
            propTiles = propTiles,
            overlayTiles = overlayTiles,
            actorTiles = actorTiles,
            fogTiles = fogTiles,
            targetCursor = overlayState.targetingCursor,
            inspectCursor = overlayState.inspectCursor,
            hud = buildHud(localizer, visualResolver, snapshot, overlayState, player, actorById, cellByPoint),
            messageLines = snapshot.logEvents.map { event -> renderLogEvent(localizer, event) },
            sidebar = buildSidebar(localizer, visualResolver, snapshot, overlayState, player, actorById, cellByPoint),
        )
    }

    private fun buildHud(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        player: ActorRenderSnapshot,
        actorById: Map<Int, ActorRenderSnapshot>,
        cellByPoint: Map<com.ktome.core.map.Point, MapCellSnapshot>,
    ): TileHudModel {
        val playerStatus = snapshot.uiState.playerStatus
        val focusActor = focusedActor(snapshot, overlayState, actorById, cellByPoint)
        val resourceTone = resourceTone(playerStatus.resourceTypeId)
        val statusIcons = player.statusEffects.mapNotNull { effect -> effect.iconKey?.let { resolveVisual(visualResolver, it) } }
        val hotbar =
            snapshot.uiState.talents.map { talent ->
                TileHotbarSlotModel(
                    slot = talent.slot,
                    label = localizer.text(talent.nameKey),
                    icon = talent.iconKey?.let { resolveVisual(visualResolver, it) },
                    accentIcon = talent.damageTypeIconKey?.let { resolveVisual(visualResolver, it) },
                    resourceText = "${talent.resourceCost} ${localizer.text(talent.resourceLabelKey)}",
                    cooldownText =
                        if (talent.currentCooldown > 0) {
                            localizer.text("ui.sidebar.cooldown.short") + ":" + talent.currentCooldown
                        } else {
                            null
                        },
                )
            }
        val focusName = focusActor?.let { actor -> localizer.text(actor.nameKey) }
        val focusLines =
            focusActor?.let { actor ->
                listOf(
                    actorRole(localizer, actor),
                    "${localizer.text("ui.hud.hp.short")} ${actor.currentHp}/${actor.maxHp}",
                    "${localizer.text("ui.hud.attack.short")} ${actor.attack}  ${localizer.text("ui.hud.defense.short")} ${actor.defense}",
                ) + actor.statusEffects.map { effect -> formatStatusEffect(localizer, effect) }
            } ?: emptyList()

        return TileHudModel(
            playerName = localizer.text(player.nameKey),
            zoneName = localizer.text(snapshot.metadata.zoneNameKey),
            floorText = localizer.text("ui.hud.floor.short") + " ${snapshot.metadata.currentFloor}/${snapshot.metadata.maxFloor}",
            hpGauge =
                TileGaugeModel(
                    label = localizer.text("ui.hud.hp.short"),
                    current = playerStatus.currentHp,
                    max = playerStatus.maxHp,
                    tone = TileTextTone.RED,
                    resourceTypeId = "HEALTH",
                ),
            resourceGauge =
                TileGaugeModel(
                    label = localizer.text(playerStatus.resourceLabelKey),
                    current = playerStatus.currentResource,
                    max = playerStatus.maxResource,
                    tone = resourceTone,
                    resourceTypeId = playerStatus.resourceTypeId,
                ),
            statusIcons = statusIcons,
            focusIcon = focusActor?.let { actor -> resolveVisual(visualResolver, actor.visualKey) },
            focusName = focusName,
            focusLines = focusLines,
            hotbar = hotbar,
            summaryText =
                "${localizer.text("ui.hud.floor.short")} ${snapshot.metadata.currentFloor}/${snapshot.metadata.maxFloor}  " +
                    "${localizer.text("ui.hud.hp.short")} ${playerStatus.currentHp}/${playerStatus.maxHp}  " +
                    "${localizer.text(playerStatus.resourceLabelKey)} ${playerStatus.currentResource}/${playerStatus.maxResource}  " +
                    "${localizer.text("ui.hud.attack.short")} ${playerStatus.attack}  " +
                    "${localizer.text("ui.hud.defense.short")} ${playerStatus.defense}",
        )
    }

    private fun buildSidebar(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        player: ActorRenderSnapshot,
        actorById: Map<Int, ActorRenderSnapshot>,
        cellByPoint: Map<com.ktome.core.map.Point, MapCellSnapshot>,
    ): TileSidebarModel {
        val rows = mutableListOf<TileTextRow>()
        val title = TileRenderer.sidebarTitle(localizer, overlayState.mode)
        val playerCell = requireNotNull(cellByPoint[point(snapshot.metadata.playerX, snapshot.metadata.playerY)])

        when (overlayState.mode) {
            UiMode.MAP -> {
                rows += TileTextRow(localizer.text("ui.sidebar.equipment"), TileTextTone.GOLD)
                snapshot.uiState.equipment.forEach { equipment ->
                    val item = equipment.item
                    rows +=
                        TileTextRow(
                            text = "${slotLabel(localizer, equipment.slotId)}: ${item?.let { localizer.text(it.nameKey) } ?: "-"}",
                            tone = TileTextTone.WHITE,
                            icon = item?.iconKey?.let { resolveVisual(visualResolver, it) },
                        )
                }
                rows += TileTextRow(localizer.text("ui.sidebar.items"), TileTextTone.GOLD)
                if (playerCell.items.isEmpty()) {
                    rows += TileTextRow(localizer.text("ui.sidebar.empty"), TileTextTone.GRAY)
                } else {
                    playerCell.items.forEach { item ->
                        rows +=
                            TileTextRow(
                                text = localizer.text(item.nameKey),
                                tone = TileTextTone.WHITE,
                                icon = item.iconKey?.let { resolveVisual(visualResolver, it) },
                            )
                    }
                }
                rows += TileTextRow(localizer.text("ui.controls.map.inventory"), TileTextTone.LIGHT_GRAY)
                rows += TileTextRow(localizer.text("ui.controls.map.pick_up"), TileTextTone.LIGHT_GRAY)
                rows += TileTextRow(localizer.text("ui.controls.map.save"), TileTextTone.LIGHT_GRAY)
            }

            UiMode.INVENTORY -> {
                snapshot.uiState.inventory.forEach { entry ->
                    val label = "${entry.index + 1}. ${localizer.text(entry.item.nameKey)}"
                    val equipped = entry.equippedSlotId?.let { slotId -> " [${slotLabel(localizer, slotId)}]" }.orEmpty()
                    rows +=
                        TileTextRow(
                            text = label + equipped,
                            tone = if (entry.index == overlayState.inventorySelection) TileTextTone.CYAN else TileTextTone.WHITE,
                            icon = entry.item.iconKey?.let { resolveVisual(visualResolver, it) },
                            selected = entry.index == overlayState.inventorySelection,
                        )
                }
                if (snapshot.uiState.inventory.isEmpty()) {
                    rows += TileTextRow(localizer.text("ui.sidebar.empty"), TileTextTone.GRAY)
                } else {
                    val selectedItem = snapshot.uiState.inventory.getOrNull(overlayState.inventorySelection)?.item
                    selectedItem?.let { item ->
                        item.descKey?.let { descKey ->
                            rows += TileTextRow(localizer.text(descKey), TileTextTone.LIGHT_GRAY)
                        }
                        itemDetailLines(localizer, item).forEach { detail ->
                            rows += TileTextRow(detail, TileTextTone.LIGHT_GRAY)
                        }
                    }
                }
                rows += TileTextRow(localizer.text("ui.controls.inventory"), TileTextTone.LIGHT_GRAY)
            }

            UiMode.TARGETING -> {
                val cursor = overlayState.targetingCursor
                rows += TileTextRow(localizer.text("ui.targeting.slot", "slot" to overlayState.targetingSlot), TileTextTone.WHITE)
                rows +=
                    TileTextRow(
                        localizer.text("ui.targeting.cursor", "x" to (cursor?.x ?: "-"), "y" to (cursor?.y ?: "-")),
                        TileTextTone.WHITE,
                    )
                rows += TileTextRow(localizer.text("ui.controls.targeting"), TileTextTone.LIGHT_GRAY)
            }

            UiMode.INSPECT -> {
                val cursor = overlayState.inspectCursor ?: point(snapshot.metadata.playerX, snapshot.metadata.playerY)
                val cell = requireNotNull(cellByPoint[cursor])
                val actor = cell.actorEntityId?.let(actorById::get)

                rows += TileTextRow(localizer.text("ui.inspect.cursor", "x" to cursor.x, "y" to cursor.y), TileTextTone.WHITE)
                rows += TileTextRow("${visibilityLabel(localizer, cell.visibility)} ${terrainName(localizer, cell)}", TileTextTone.WHITE)
                actor?.let { inspected ->
                    rows +=
                        TileTextRow(
                            text = localizer.text(inspected.nameKey),
                            tone = TileTextTone.GOLD,
                            icon = resolveVisual(visualResolver, inspected.visualKey),
                        )
                    rows += TileTextRow(actorRole(localizer, inspected), TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.hud.hp.short")} ${inspected.currentHp}/${inspected.maxHp}", TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.hud.attack.short")} ${inspected.attack}  ${localizer.text("ui.hud.defense.short")} ${inspected.defense}", TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.hud.accuracy.short")} ${inspected.accuracy}  ${localizer.text("ui.hud.evasion.short")} ${inspected.evasion}", TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.stat.str")} ${inspected.strength}  ${localizer.text("ui.stat.dex")} ${inspected.dexterity}", TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.stat.con")} ${inspected.constitution}  ${localizer.text("ui.stat.wil")} ${inspected.willpower}", TileTextTone.WHITE)
                    rows += TileTextRow("${localizer.text("ui.hud.speed.short")} ${inspected.speed}", TileTextTone.WHITE)
                    inspected.statusEffects.forEach { effect ->
                        rows +=
                            TileTextRow(
                                text = formatStatusEffect(localizer, effect),
                                tone = TileTextTone.LIGHT_GRAY,
                                icon = effect.iconKey?.let { resolveVisual(visualResolver, it) },
                            )
                    }
                }
                if (cell.items.isNotEmpty()) {
                    rows += TileTextRow(localizer.text("ui.sidebar.items"), TileTextTone.GOLD)
                    cell.items.forEach { item ->
                        rows +=
                            TileTextRow(
                                text = localizer.text(item.nameKey),
                                tone = TileTextTone.WHITE,
                                icon = item.iconKey?.let { resolveVisual(visualResolver, it) },
                            )
                        item.descKey?.let { descKey ->
                            rows += TileTextRow(localizer.text(descKey), TileTextTone.LIGHT_GRAY)
                        }
                    }
                }
                cell.stairDirectionId?.let { directionId ->
                    rows +=
                        TileTextRow(
                            text = stairName(localizer, directionId),
                            tone = TileTextTone.LIGHT_GRAY,
                            icon = resolveVisual(visualResolver, stairVisualKey(directionId)),
                        )
                }
                if (actor == null && cell.items.isEmpty() && cell.stairDirectionId == null) {
                    rows +=
                        TileTextRow(
                            text =
                                when (cell.visibility) {
                                    CellVisibilitySnapshot.VISIBLE -> localizer.text("ui.inspect.no_visible_target")
                                    CellVisibilitySnapshot.EXPLORED -> localizer.text("ui.inspect.explored_not_visible")
                                    CellVisibilitySnapshot.HIDDEN -> localizer.text("ui.inspect.unknown_tile")
                                },
                            tone = TileTextTone.LIGHT_GRAY,
                        )
                }
                rows += TileTextRow(localizer.text("ui.controls.inspect"), TileTextTone.LIGHT_GRAY)
            }

            UiMode.STAT_ASSIGN -> {
                rows += TileTextRow(localizer.text("ui.sidebar.points", "value" to snapshot.uiState.playerStatus.statPoints), TileTextTone.WHITE)
                rows += TileTextRow("1. ${localizer.text("ui.stat.str")}", TileTextTone.WHITE)
                rows += TileTextRow("2. ${localizer.text("ui.stat.dex")}", TileTextTone.WHITE)
                rows += TileTextRow("3. ${localizer.text("ui.stat.con")}", TileTextTone.WHITE)
                rows += TileTextRow("4. ${localizer.text("ui.stat.wil")}", TileTextTone.WHITE)
            }

            UiMode.TALENT_ASSIGN -> {
                rows += TileTextRow(localizer.text("ui.sidebar.points", "value" to snapshot.uiState.playerStatus.talentPoints), TileTextTone.WHITE)
                snapshot.uiState.talents.forEach { talent ->
                    rows +=
                        TileTextRow(
                            text = "${talent.slot}. ${localizer.text(talent.nameKey)} ${talent.level}/${talent.maxLevel}",
                            tone = TileTextTone.WHITE,
                            icon = talent.iconKey?.let { resolveVisual(visualResolver, it) },
                        )
                }
                rows += TileTextRow(localizer.text("ui.controls.talent_assign"), TileTextTone.LIGHT_GRAY)
            }
        }

        return TileSidebarModel(title = title, rows = rows)
    }

    private fun focusedActor(
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        actorById: Map<Int, ActorRenderSnapshot>,
        cellByPoint: Map<com.ktome.core.map.Point, MapCellSnapshot>,
    ): ActorRenderSnapshot? {
        val focusPoint =
            when (overlayState.mode) {
                UiMode.INSPECT -> overlayState.inspectCursor
                UiMode.TARGETING -> overlayState.targetingCursor
                else -> null
            } ?: return null
        val actorId = cellByPoint[focusPoint]?.actorEntityId ?: return null
        return actorById[actorId]
    }

    private fun resourceTone(resourceTypeId: String): TileTextTone =
        when (resourceTypeId) {
            "MANA" -> TileTextTone.BLUE
            "ENERGY" -> TileTextTone.CYAN
            "POSITIVE_ENERGY" -> TileTextTone.GOLD
            else -> TileTextTone.GREEN
        }

    private fun overlayAlpha(dangerLevel: Int): Float =
        when {
            dangerLevel >= 3 -> 0.85f
            dangerLevel == 2 -> 0.65f
            else -> 0.45f
        }

    private fun resolveVisual(
        visualResolver: VisualManifestResolver,
        key: String,
    ): ResolvedVisualAsset {
        val resolved = visualResolver.resolve(key)
        require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
            "Tile render model requires exact visual key '$key'."
        }
        return resolved
    }

    private fun terrainName(
        localizer: Localizer,
        cell: MapCellSnapshot,
    ): String =
        when (cell.terrainTypeId) {
            "floor" -> localizer.text("tile.floor.name")
            "wall" -> localizer.text("tile.wall.name")
            else -> localizer.text("tile.unknown.name")
        }

    private fun actorRole(
        localizer: Localizer,
        actor: ActorRenderSnapshot,
    ): String =
        when (actor.roleKind ?: ActorRoleKindSnapshot.GENERIC) {
            ActorRoleKindSnapshot.PLAYER -> localizer.text("actor.player.role")
            ActorRoleKindSnapshot.BOSS -> localizer.text("actor.boss.role")
            ActorRoleKindSnapshot.MONSTER ->
                localizer.text(
                    "actor.monster.role",
                    "ai" to aiLabel(localizer, actor.aiTypeId),
                )
            ActorRoleKindSnapshot.GENERIC -> localizer.text("actor.generic.role")
        }

    private fun aiLabel(
        localizer: Localizer,
        aiTypeId: String?,
    ): String =
        when (aiTypeId) {
            "CHASE" -> localizer.text("ai.chase")
            "KITE" -> localizer.text("ai.kite")
            "PATROL" -> localizer.text("ai.patrol")
            else -> localizer.text("actor.generic.role")
        }

    private fun renderLogEvent(
        localizer: Localizer,
        event: RenderLogEventSnapshot,
    ): String =
        localizer.text(
            event.message.key,
            *event.message.arguments.map { argument -> argument.name to resolveArgument(localizer, argument) }.toTypedArray(),
        )

    private fun resolveArgument(
        localizer: Localizer,
        argument: RenderTextArgumentSnapshot,
    ): String = argument.valueKey?.let(localizer::text) ?: argument.value.orEmpty()

    private fun itemDetailLines(
        localizer: Localizer,
        item: ItemRenderSnapshot,
    ): List<String> =
        buildList {
            item.slotId?.let { slotId ->
                add(localizer.text("ui.inspect.slot", "slot" to slotLabel(localizer, slotId)))
            }
            addAll(statModifierLines(localizer, item.stats))
            when (item.effectTypeId) {
                "HEAL" -> add(localizer.text("ui.inspect.restore_hp", "amount" to item.magnitude))
                "TELEPORT" -> add(localizer.text("ui.inspect.teleport_random"))
                null -> Unit
            }
            if (isEmpty()) {
                add(localizer.text("ui.inspect.no_special_effect"))
            }
        }

    private fun statModifierLines(
        localizer: Localizer,
        modifier: ItemStatModifierSnapshot,
    ): List<String> =
        buildList {
            addModifier(localizer.text("ui.stat.str"), modifier.str)
            addModifier(localizer.text("ui.stat.dex"), modifier.dex)
            addModifier(localizer.text("ui.stat.con"), modifier.con)
            addModifier(localizer.text("ui.stat.wil"), modifier.wil)
            addModifier(localizer.text("ui.hud.attack.short"), modifier.attack)
            addModifier(localizer.text("ui.hud.defense.short"), modifier.defense)
            addModifier(localizer.text("ui.hud.accuracy.short"), modifier.accuracy)
            addModifier(localizer.text("ui.hud.evasion.short"), modifier.evasion)
            addModifier(localizer.text("ui.hud.speed.short"), modifier.speed)
            addModifier(localizer.text("ui.hud.hp.short"), modifier.maxHp)
            addModifier(localizer.text("ui.hud.stamina.short"), modifier.maxStamina)
            addDecimalModifier(localizer.text("ui.inspect.mod.hp_regen"), modifier.hpRegen)
            addDecimalModifier(localizer.text("ui.inspect.mod.stamina_regen"), modifier.staminaRegen)
            addPercentModifier(localizer.text("ui.inspect.mod.crit"), modifier.critChance)
            addPercentModifier(localizer.text("ui.inspect.mod.talent"), modifier.talentPower)
        }

    private fun MutableList<String>.addModifier(
        label: String,
        value: Int,
    ) {
        if (value != 0) {
            add("$label ${signed(value)}")
        }
    }

    private fun MutableList<String>.addDecimalModifier(
        label: String,
        value: Double,
    ) {
        if (value != 0.0) {
            add("$label ${signedDecimal(value)}")
        }
    }

    private fun MutableList<String>.addPercentModifier(
        label: String,
        value: Double,
    ) {
        if (value != 0.0) {
            add("$label ${signed((value * 100).toInt())}%")
        }
    }

    private fun formatStatusEffect(
        localizer: Localizer,
        effect: StatusEffectRenderSnapshot,
    ): String =
        localizer.text(
            "ui.inspect.effect.turns",
            "name" to statusEffectLabel(localizer, effect),
            "turns" to effect.remainingTurns,
        )

    private fun statusEffectLabel(
        localizer: Localizer,
        effect: StatusEffectRenderSnapshot,
    ): String = effect.nameKey?.let(localizer::text) ?: effect.typeId

    private fun stairName(
        localizer: Localizer,
        directionId: String,
    ): String =
        when (directionId) {
            "UP" -> localizer.text("stairs.up.name")
            "DOWN" -> localizer.text("stairs.down.name")
            else -> directionId
        }

    private fun stairVisualKey(directionId: String): String =
        when (directionId) {
            "UP" -> "prop.stairs.up"
            else -> "prop.stairs.down"
        }

    private fun visibilityLabel(
        localizer: Localizer,
        visibility: CellVisibilitySnapshot,
    ): String =
        when (visibility) {
            CellVisibilitySnapshot.VISIBLE -> localizer.text("ui.inspect.visible")
            CellVisibilitySnapshot.EXPLORED -> localizer.text("ui.inspect.explored")
            CellVisibilitySnapshot.HIDDEN -> localizer.text("ui.inspect.hidden")
        }

    private fun slotLabel(
        localizer: Localizer,
        slotId: String,
    ): String =
        when (slotId) {
            "WEAPON" -> localizer.text("ui.sidebar.weapon")
            "ARMOR" -> localizer.text("ui.sidebar.armor")
            else -> slotId
        }

    private fun point(
        x: Int,
        y: Int,
    ): com.ktome.core.map.Point = com.ktome.core.map.Point(x, y)

    private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

    private fun signedDecimal(value: Double): String {
        val normalized =
            if (value % 1.0 == 0.0) {
                value.toInt().toString()
            } else {
                "%.1f".format(value)
            }
        return if (value > 0) "+$normalized" else normalized
    }
}
