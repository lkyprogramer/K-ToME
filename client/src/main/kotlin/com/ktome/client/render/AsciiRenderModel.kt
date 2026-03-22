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
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.core.snapshot.StatusEffectRenderSnapshot
import com.ktome.game.i18n.Localizer

internal enum class AsciiTextTone {
    GOLD,
    WHITE,
    LIGHT_GRAY,
    CYAN,
    GRAY,
}

internal data class AsciiGlyphPlacement(
    val x: Int,
    val y: Int,
    val glyph: Char,
    val colorHex: String,
)

internal data class AsciiTextLine(
    val text: String,
    val tone: AsciiTextTone,
)

internal data class AsciiRenderModel(
    val terrainGlyphs: List<AsciiGlyphPlacement>,
    val propGlyphs: List<AsciiGlyphPlacement>,
    val overlayGlyphs: List<AsciiGlyphPlacement>,
    val actorGlyphs: List<AsciiGlyphPlacement>,
    val targetCursor: com.ktome.core.map.Point?,
    val inspectCursor: com.ktome.core.map.Point?,
    val hudText: String,
    val messageLines: List<String>,
    val sidebarLines: List<AsciiTextLine>,
)

internal object AsciiRenderModelBuilder {
    fun build(
        localizer: Localizer,
        visualResolver: VisualManifestResolver,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
    ): AsciiRenderModel {
        val cellByPoint = snapshot.mapCells.associateBy { cell -> point(cell.x, cell.y) }
        val actorById = snapshot.actors.associateBy(ActorRenderSnapshot::entityId)
        val terrainGlyphs =
            snapshot.mapCells
                .filter { cell -> cell.visibility != CellVisibilitySnapshot.HIDDEN }
                .map { cell ->
                    val presentation = terrainPresentation(visualResolver, cell)
                    AsciiGlyphPlacement(
                        x = cell.x,
                        y = cell.y,
                        glyph = presentation.glyph,
                        colorHex = presentation.colorHex,
                    )
                }
        val propGlyphs =
            snapshot.props.map { prop ->
                val presentation = propPresentation(visualResolver, prop)
                AsciiGlyphPlacement(
                    x = prop.x,
                    y = prop.y,
                    glyph = presentation.glyph,
                    colorHex = presentation.colorHex,
                )
            }
        val overlayGlyphs =
            snapshot.overlays.flatMap { overlay ->
                val presentation = overlayPresentation(visualResolver, overlay)
                overlay.cells.map { cell ->
                    AsciiGlyphPlacement(
                        x = cell.x,
                        y = cell.y,
                        glyph = presentation.glyph,
                        colorHex = presentation.colorHex,
                    )
                }
            }
        val actorGlyphs =
            snapshot.actors
                .sortedBy { actor -> if (actor.isPlayer) 1 else 0 }
                .map { actor ->
                    val presentation = actorPresentation(visualResolver, actor)
                    AsciiGlyphPlacement(
                        x = actor.x,
                        y = actor.y,
                        glyph = presentation.glyph,
                        colorHex = presentation.colorHex,
                    )
                }

        return AsciiRenderModel(
            terrainGlyphs = terrainGlyphs,
            propGlyphs = propGlyphs,
            overlayGlyphs = overlayGlyphs,
            actorGlyphs = actorGlyphs,
            targetCursor = overlayState.targetingCursor,
            inspectCursor = overlayState.inspectCursor,
            hudText = AsciiRenderer.hudText(localizer, snapshot),
            messageLines = snapshot.logEvents.map { event -> renderLogEvent(localizer, event) },
            sidebarLines = buildSidebarLines(localizer, snapshot, overlayState, cellByPoint, actorById),
        )
    }

    private fun buildSidebarLines(
        localizer: Localizer,
        snapshot: RenderSnapshot,
        overlayState: OverlayState,
        cellByPoint: Map<com.ktome.core.map.Point, MapCellSnapshot>,
        actorById: Map<Int, ActorRenderSnapshot>,
    ): List<AsciiTextLine> {
        val lines = mutableListOf<AsciiTextLine>()
        val playerCell = requireNotNull(cellByPoint[point(snapshot.metadata.playerX, snapshot.metadata.playerY)])

        lines += AsciiTextLine(localizer.text("ui.sidebar.equipment"), AsciiTextTone.GOLD)
        snapshot.uiState.equipment.forEach { equipment ->
            val itemName = equipment.item?.let { item -> renderItemDisplay(localizer, item) } ?: "-"
            lines += AsciiTextLine("${slotLabel(localizer, equipment.slotId)}: $itemName", AsciiTextTone.WHITE)
        }

        lines += blankLine()
        lines += AsciiTextLine(localizer.text("ui.sidebar.talents"), AsciiTextTone.GOLD)
        snapshot.uiState.talents.forEach { talent ->
            val state =
                if (talent.currentCooldown > 0) {
                    "${localizer.text("ui.sidebar.cooldown.short")}:${talent.currentCooldown}"
                } else {
                    localizer.text("ui.sidebar.ready")
            }
            lines += AsciiTextLine(
                "${talent.slot}.${localizer.text(talent.nameKey)} L${talent.level}/${talent.maxLevel} [$state]${talentTargetSuffix(localizer, talent.requiresTarget, talent.range)}",
                AsciiTextTone.WHITE,
            )
            lines += AsciiTextLine(
                "   ${talentUsageSummary(localizer, talent.resourceCost, talent.resourceLabelKey, talent.requiresTarget, talent.range)}",
                AsciiTextTone.WHITE,
            )
        }

        lines += blankLine()
        lines += AsciiTextLine(localizer.text("ui.sidebar.ground"), AsciiTextTone.GOLD)
        if (playerCell.items.isEmpty()) {
            lines += AsciiTextLine("-", AsciiTextTone.WHITE)
        } else {
            playerCell.items.forEach { item ->
                lines += AsciiTextLine(renderItemDisplay(localizer, item), AsciiTextTone.WHITE)
            }
        }

        when (overlayState.mode) {
            UiMode.MAP -> {
                lines += blankLine()
                lines += AsciiTextLine(localizer.text("ui.controls.map.pick_up"), AsciiTextTone.LIGHT_GRAY)
                lines += AsciiTextLine(localizer.text("ui.controls.map.inventory"), AsciiTextTone.LIGHT_GRAY)
                lines += AsciiTextLine(localizer.text("ui.controls.map.save"), AsciiTextTone.LIGHT_GRAY)
                if (snapshot.uiState.talents.isNotEmpty()) {
                    lines += AsciiTextLine(localizer.text("ui.controls.map.use_talent"), AsciiTextTone.LIGHT_GRAY)
                }
                if (snapshot.uiState.talents.any { talent -> talent.requiresTarget }) {
                    lines += AsciiTextLine(localizer.text("ui.controls.map.target_talent"), AsciiTextTone.LIGHT_GRAY)
                }
                if (playerCell.stairDirectionId == "DOWN") {
                    lines += AsciiTextLine(localizer.text("ui.controls.map.descend"), AsciiTextTone.LIGHT_GRAY)
                }
                if (playerCell.stairDirectionId == "UP") {
                    lines += AsciiTextLine(localizer.text("ui.controls.map.ascend"), AsciiTextTone.LIGHT_GRAY)
                }
                if (snapshot.uiState.playerStatus.talentPoints > 0) {
                    lines += AsciiTextLine(localizer.text("ui.controls.map.spend_talent"), AsciiTextTone.LIGHT_GRAY)
                }
                if (snapshot.uiState.playerStatus.statPoints > 0) {
                    lines += AsciiTextLine(localizer.text("ui.controls.map.spend_stat"), AsciiTextTone.LIGHT_GRAY)
                }
            }

            UiMode.INVENTORY -> {
                lines += blankLine()
                lines += AsciiTextLine(AsciiRenderer.sidebarTitle(localizer, UiMode.INVENTORY), AsciiTextTone.GOLD)
                snapshot.uiState.inventory.forEach { item ->
                    val equipped = item.equippedSlotId?.let { slotId -> " [${slotLabel(localizer, slotId)}]" } ?: ""
                    lines += AsciiTextLine(
                        "${item.index + 1}. ${renderItemDisplay(localizer, item.item)}$equipped",
                        if (item.index == overlayState.inventorySelection) AsciiTextTone.CYAN else AsciiTextTone.WHITE,
                    )
                }
                if (snapshot.uiState.inventory.isEmpty()) {
                    lines += AsciiTextLine(localizer.text("ui.sidebar.empty"), AsciiTextTone.GRAY)
                }
                lines += AsciiTextLine(localizer.text("ui.controls.inventory"), AsciiTextTone.LIGHT_GRAY)
            }

            UiMode.TARGETING -> {
                lines += blankLine()
                lines += AsciiTextLine(AsciiRenderer.sidebarTitle(localizer, UiMode.TARGETING), AsciiTextTone.GOLD)
                lines += AsciiTextLine(localizer.text("ui.targeting.slot", "slot" to overlayState.targetingSlot), AsciiTextTone.WHITE)
                val cursor = overlayState.targetingCursor
                lines += AsciiTextLine(
                    localizer.text("ui.targeting.cursor", "x" to (cursor?.x ?: "-"), "y" to (cursor?.y ?: "-")),
                    AsciiTextTone.WHITE,
                )
                lines += AsciiTextLine(localizer.text("ui.controls.targeting"), AsciiTextTone.LIGHT_GRAY)
            }

            UiMode.INSPECT -> {
                lines += blankLine()
                val cursor = overlayState.inspectCursor ?: point(snapshot.metadata.playerX, snapshot.metadata.playerY)
                val cell = requireNotNull(cellByPoint[cursor])
                val actor = cell.actorEntityId?.let(actorById::get)

                lines += AsciiTextLine(AsciiRenderer.sidebarTitle(localizer, UiMode.INSPECT), AsciiTextTone.GOLD)
                lines += AsciiTextLine(localizer.text("ui.inspect.cursor", "x" to cursor.x, "y" to cursor.y), AsciiTextTone.WHITE)
                lines += AsciiTextLine(
                    "${visibilityLabel(localizer, cell.visibility)} ${terrainName(localizer, cell)}",
                    AsciiTextTone.WHITE,
                )

                actor?.let { inspected ->
                    lines += AsciiTextLine(localizer.text(inspected.nameKey), AsciiTextTone.GOLD)
                    lines += AsciiTextLine(actorRole(localizer, inspected), AsciiTextTone.WHITE)
                    lines += AsciiTextLine(
                        "${localizer.text("ui.hud.hp.short")} ${inspected.currentHp}/${inspected.maxHp}",
                        AsciiTextTone.WHITE,
                    )
                    lines += AsciiTextLine(
                        "${localizer.text("ui.hud.attack.short")} ${inspected.attack}  ${localizer.text("ui.hud.defense.short")} ${inspected.defense}",
                        AsciiTextTone.WHITE,
                    )
                    lines += AsciiTextLine(
                        "${localizer.text("ui.hud.accuracy.short")} ${inspected.accuracy}  ${localizer.text("ui.hud.evasion.short")} ${inspected.evasion}",
                        AsciiTextTone.WHITE,
                    )
                    lines += AsciiTextLine(
                        "${localizer.text("ui.stat.str")} ${inspected.strength}  ${localizer.text("ui.stat.dex")} ${inspected.dexterity}",
                        AsciiTextTone.WHITE,
                    )
                    lines += AsciiTextLine(
                        "${localizer.text("ui.stat.con")} ${inspected.constitution}  ${localizer.text("ui.stat.wil")} ${inspected.willpower}",
                        AsciiTextTone.WHITE,
                    )
                    lines += AsciiTextLine("${localizer.text("ui.hud.speed.short")} ${inspected.speed}", AsciiTextTone.WHITE)
                    inspected.statusEffects.forEach { effect ->
                        lines += AsciiTextLine(formatStatusEffect(localizer, effect), AsciiTextTone.LIGHT_GRAY)
                    }
                }

                if (cell.items.isNotEmpty()) {
                    lines += AsciiTextLine(localizer.text("ui.sidebar.items"), AsciiTextTone.GOLD)
                    cell.items.forEach { item ->
                        lines += AsciiTextLine(renderItemDisplay(localizer, item), AsciiTextTone.WHITE)
                        itemDetailLines(localizer, item).forEach { detail ->
                            lines += AsciiTextLine(detail, AsciiTextTone.LIGHT_GRAY)
                        }
                    }
                }

                cell.stairDirectionId?.let { directionId ->
                    lines += AsciiTextLine(stairName(localizer, directionId), AsciiTextTone.LIGHT_GRAY)
                }

                if (actor == null && cell.items.isEmpty() && cell.stairDirectionId == null) {
                    lines += AsciiTextLine(
                        when (cell.visibility) {
                            CellVisibilitySnapshot.VISIBLE -> localizer.text("ui.inspect.no_visible_target")
                            CellVisibilitySnapshot.EXPLORED -> localizer.text("ui.inspect.explored_not_visible")
                            CellVisibilitySnapshot.HIDDEN -> localizer.text("ui.inspect.unknown_tile")
                        },
                        AsciiTextTone.LIGHT_GRAY,
                    )
                }

                lines += AsciiTextLine(localizer.text("ui.controls.inspect"), AsciiTextTone.LIGHT_GRAY)
            }

            UiMode.STAT_ASSIGN -> {
                lines += blankLine()
                lines += AsciiTextLine(AsciiRenderer.sidebarTitle(localizer, UiMode.STAT_ASSIGN), AsciiTextTone.GOLD)
                lines += AsciiTextLine(
                    localizer.text("ui.sidebar.points", "value" to snapshot.uiState.playerStatus.statPoints),
                    AsciiTextTone.WHITE,
                )
                lines += AsciiTextLine("1. ${localizer.text("ui.stat.str")}", AsciiTextTone.WHITE)
                lines += AsciiTextLine("2. ${localizer.text("ui.stat.dex")}", AsciiTextTone.WHITE)
                lines += AsciiTextLine("3. ${localizer.text("ui.stat.con")}", AsciiTextTone.WHITE)
                lines += AsciiTextLine("4. ${localizer.text("ui.stat.wil")}", AsciiTextTone.WHITE)
            }

            UiMode.TALENT_ASSIGN -> {
                lines += blankLine()
                lines += AsciiTextLine(AsciiRenderer.sidebarTitle(localizer, UiMode.TALENT_ASSIGN), AsciiTextTone.GOLD)
                lines += AsciiTextLine(
                    localizer.text("ui.sidebar.points", "value" to snapshot.uiState.playerStatus.talentPoints),
                    AsciiTextTone.WHITE,
                )
                snapshot.uiState.talents.forEach { talent ->
                    lines += AsciiTextLine(
                        "${talent.slot}. ${localizer.text(talent.nameKey)} ${talent.level}/${talent.maxLevel}${talentTargetSuffix(localizer, talent.requiresTarget, talent.range)}",
                        AsciiTextTone.WHITE,
                    )
                }
                lines += AsciiTextLine(localizer.text("ui.controls.talent_assign"), AsciiTextTone.LIGHT_GRAY)
            }
        }

        return lines
    }

    private fun terrainPresentation(
        visualResolver: VisualManifestResolver,
        cell: MapCellSnapshot,
    ): AsciiPresentation {
        val fallback =
            when {
                cell.visibility == CellVisibilitySnapshot.HIDDEN -> AsciiPresentation(' ', "#000000")
                cell.terrainTypeId == "wall" -> AsciiPresentation('#', "#D3D3D3")
                else -> AsciiPresentation('.', "#FFFFFF")
            }
        val visiblePresentation = presentationFrom(resolveVisual(visualResolver, cell.terrainVisualKey), fallback)
        return if (cell.visibility == CellVisibilitySnapshot.EXPLORED) {
            visiblePresentation.copy(colorHex = dimColor(visiblePresentation.colorHex))
        } else {
            visiblePresentation
        }
    }

    private fun propPresentation(
        visualResolver: VisualManifestResolver,
        prop: PropRenderSnapshot,
    ): AsciiPresentation {
        val fallback =
            when (prop.stairDirectionId) {
                "UP" -> AsciiPresentation('<', "#D7E7FF")
                "DOWN" -> AsciiPresentation('>', "#D7E7FF")
                else -> AsciiPresentation('?', "#FFFFFF")
            }
        return presentationFrom(resolveVisual(visualResolver, prop.visualKey), fallback)
    }

    private fun overlayPresentation(
        visualResolver: VisualManifestResolver,
        overlay: com.ktome.core.snapshot.OverlayRenderSnapshot,
    ): AsciiPresentation {
        val fallbackColor =
            when {
                overlay.dangerLevel >= 3 -> "#FF2400"
                overlay.dangerLevel == 2 -> "#FF8C00"
                else -> "#FFD700"
            }
        return presentationFrom(resolveVisual(visualResolver, overlay.visualKey), AsciiPresentation('!', fallbackColor))
    }

    private fun actorPresentation(
        visualResolver: VisualManifestResolver,
        actor: ActorRenderSnapshot,
    ): AsciiPresentation {
        val fallback =
            when {
                actor.isPlayer -> AsciiPresentation('@', "#FFD700")
                actor.roleKind == ActorRoleKindSnapshot.BOSS -> AsciiPresentation('B', "#FF5555")
                actor.roleKind == ActorRoleKindSnapshot.MONSTER -> AsciiPresentation('m', "#FFFFFF")
                else -> AsciiPresentation('?', "#FFFFFF")
            }
        return presentationFrom(resolveVisual(visualResolver, actor.visualKey), fallback)
    }

    private fun resolveVisual(
        visualResolver: VisualManifestResolver,
        key: String,
    ): ResolvedVisualAsset {
        val resolved = visualResolver.resolve(key)
        require(!resolved.fallbackUsed && !resolved.matchedByPrefix) {
            "ASCII render model requires exact visual key '$key'."
        }
        return resolved
    }

    private fun presentationFrom(
        resolved: ResolvedVisualAsset,
        fallback: AsciiPresentation,
    ): AsciiPresentation =
        AsciiPresentation(
            glyph = resolved.entry.asciiGlyph?.firstOrNull() ?: fallback.glyph,
            colorHex = resolved.entry.asciiColorHex ?: fallback.colorHex,
        )

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
    ): String =
        argument.valueToken?.let { token -> renderTextToken(localizer, token) }
            ?: argument.valueKey?.let(localizer::text)
            ?: argument.value.orEmpty()

    private fun renderItemDisplay(
        localizer: Localizer,
        item: ItemRenderSnapshot,
    ): String = item.displayName?.let { token -> renderTextToken(localizer, token) } ?: localizer.text(item.nameKey)

    private fun talentUsageSummary(
        localizer: Localizer,
        resourceCost: Int,
        resourceLabelKey: String,
        requiresTarget: Boolean,
        range: Int,
    ): String {
        val resourceText = "$resourceCost ${localizer.text(resourceLabelKey)}"
        if (!requiresTarget) {
            return resourceText
        }
        return "$resourceText · ${localizer.text("ui.sidebar.target.range", "range" to range)}"
    }

    private fun talentTargetSuffix(
        localizer: Localizer,
        requiresTarget: Boolean,
        range: Int,
    ): String =
        if (requiresTarget) {
            " [${localizer.text("ui.sidebar.target.range", "range" to range)}]"
        } else {
            ""
        }

    private fun renderTextToken(
        localizer: Localizer,
        token: RenderTextTokenSnapshot,
    ): String =
        localizer.text(
            token.key,
            *token.arguments.map { argument -> argument.name to resolveArgument(localizer, argument) }.toTypedArray(),
        )

    private fun itemDetailLines(
        localizer: Localizer,
        item: ItemRenderSnapshot,
    ): List<String> =
        buildList {
            item.slotId?.let { slotId ->
                add(localizer.text("ui.inspect.slot", "slot" to slotLabel(localizer, slotId)))
            }
            item.qualityNameKey?.let { qualityNameKey ->
                add(localizer.text("ui.inspect.quality", "quality" to localizer.text(qualityNameKey)))
            }
            item.materialNameKey?.let { materialNameKey ->
                add(localizer.text("ui.inspect.material", "material" to localizer.text(materialNameKey)))
            }
            item.affixNameKeys.forEach { affixNameKey ->
                add(localizer.text("ui.inspect.affix", "affix" to localizer.text(affixNameKey)))
            }
            item.passiveDescriptions.forEach { token ->
                add(renderTextToken(localizer, token))
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
            "name" to statusEffectLabel(localizer, effect.typeId),
            "turns" to effect.remainingTurns,
        )

    private fun statusEffectLabel(
        localizer: Localizer,
        typeId: String,
    ): String =
        when (typeId) {
            "STUNNED" -> localizer.text("status.stunned")
            "ARMOR_BREAK" -> localizer.text("status.armor_break")
            "WAR_CRY_BUFF" -> localizer.text("status.war_cry_buff")
            "WAR_CRY_DEBUFF" -> localizer.text("status.war_cry_debuff")
            else -> typeId
        }

    private fun stairName(
        localizer: Localizer,
        directionId: String,
    ): String =
        when (directionId) {
            "UP" -> localizer.text("stairs.up.name")
            "DOWN" -> localizer.text("stairs.down.name")
            else -> directionId
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

    private fun blankLine(): AsciiTextLine = AsciiTextLine("", AsciiTextTone.WHITE)

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

    private fun dimColor(colorHex: String): String {
        val normalized = colorHex.removePrefix("#")
        val red = normalized.substring(0, 2).toInt(16)
        val green = normalized.substring(2, 4).toInt(16)
        val blue = normalized.substring(4, 6).toInt(16)
        return "#%02X%02X%02X".format(red / 2, green / 2, blue / 2)
    }
}

private data class AsciiPresentation(
    val glyph: Char,
    val colorHex: String,
)
