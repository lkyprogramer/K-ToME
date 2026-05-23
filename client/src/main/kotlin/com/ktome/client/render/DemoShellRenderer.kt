package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.render.layout.DemoSlotGridLayout
import com.ktome.client.render.layout.GameShellBounds
import com.ktome.client.ui.chrome.ChromeFrameAssetDraw
import com.ktome.client.ui.chrome.ChromeFrameAssets
import com.ktome.client.ui.chrome.ChromeFrameBounds
import com.ktome.client.ui.chrome.ChromeFrameDrawRequest
import com.ktome.client.ui.chrome.ChromeFrameDrawSink
import com.ktome.client.ui.chrome.ChromeFramePainter
import com.ktome.client.ui.chrome.ChromeFrameRectDraw
import com.ktome.client.ui.chrome.ChromeSurfaceKind
import com.ktome.client.ui.layout.PaneFocusAnchor
import com.ktome.client.ui.status.StatusHudRenderer
import com.ktome.client.ui.token.UiDesignTokens

internal object DemoShellRenderer {
    private const val SMALL_LINE_HEIGHT = 19f
    private const val OPERATION_LINE_HEIGHT = 15f
    private const val SECTION_TITLE_HEIGHT = 18f
    private const val TEXT_WIDTH_SAFETY = 0.9f

    fun renderOuterFrame(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        val chrome = frame.model.chromeAssets
        val bounds = frame.layout.demoShell.outerFrame
        canvas.drawRect(bounds.toTileBounds(), demoBase())
        chrome?.let { assets ->
            drawFrame(
                canvas = canvas,
                assets = assets.frameAssets.copy(body = assets.demoShell.outerFrame),
                bounds = bounds,
                fillColor = demoBase(),
                borderColor = demoStrongBorder(),
                alpha = 0.92f,
            )
        }
    }

    fun renderMapStageFrame(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        val chrome = frame.model.chromeAssets
        val bounds = frame.layout.demoShell.mapStage
        canvas.drawRect(bounds.toTileBounds(), color("050604", 0.94f))
        chrome?.let { assets ->
            drawFrame(
                canvas = canvas,
                assets = assets.frameAssets.copy(body = assets.demoShell.mapStageFrame),
                bounds = bounds,
                fillColor = color("050604", 0.9f),
                borderColor = UiDesignTokens.color.border.strong.color(),
                alpha = 0.88f,
            )
            val content = contentBounds(bounds, ChromeSurfaceKind.Panel)
            drawMapStageBackdrop(canvas, assets, content)
        }
    }

    private fun drawMapStageBackdrop(
        canvas: TileCanvas,
        assets: TileChromeAssets,
        bounds: GameShellBounds,
    ) {
        canvas.drawAsset(assets.demoShell.mapStageBackdrop, bounds.toTileBounds(), alpha = 0.06f)
        canvas.drawRect(bounds.toTileBounds(), color("030201", 0.72f))
    }

    fun renderShell(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        renderNavRail(canvas, frame)
        canvas.flushLayer(TileLayerFlushReason.SHELL_NAV_RAIL)
        renderRightPanel(canvas, frame)
        canvas.flushLayer(TileLayerFlushReason.SHELL_RIGHT_PANEL)
        renderHeroCard(canvas, frame)
        canvas.flushLayer(TileLayerFlushReason.SHELL_BOTTOM_HERO)
        renderActionDeck(canvas, frame)
        canvas.flushLayer(TileLayerFlushReason.SHELL_BOTTOM_ACTION_DECK)
        renderLogAndStats(canvas, frame)
        renderPaneFocus(canvas, frame)
        canvas.flushLayer(TileLayerFlushReason.SHELL_BOTTOM_LOG_DECK)
    }

    private fun renderNavRail(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        val chrome = frame.model.chromeAssets ?: return
        val layout = frame.layout.demoShell
            drawFrame(
                canvas = canvas,
                assets = chrome.frameAssets.copy(body = chrome.demoShell.navRailFrame),
                bounds = layout.navRail,
            fillColor = demoOverlay(),
            borderColor = demoSubtleBorder(),
            alpha = 0.9f,
        )
        val content = contentBounds(layout.navRail, ChromeSurfaceKind.Panel)
        canvas.drawRect(content.toTileBounds(), color("1A0E04", 0.58f))
        val items = frame.model.shell.demo.navItems
        DemoNavRailButtonLayout.resolve(layout.navRail, items.size).forEachIndexed { index, bounds ->
            val item = items[index]
            if (item.state == TileDemoNavItemState.SELECTED) {
                canvas.drawAsset(chrome.demoShell.navButtonActive, bounds.toTileBounds(), alpha = 0.92f)
            } else {
                canvas.drawRect(bounds.toTileBounds(), demoSlotEmpty())
            }
            val iconInset = bounds.width * 0.2f
            canvas.drawAsset(
                chrome.demoShell.navIcon(item.kind),
                tileBounds(bounds.x + iconInset, bounds.y + iconInset, bounds.width - iconInset * 2f, bounds.height - iconInset * 2f),
                alpha = 0.94f,
            )
        }
    }

    private fun renderRightPanel(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        val chrome = frame.model.chromeAssets ?: return
        val layout = frame.layout.demoShell.rightPanelLayout
        val bounds = frame.layout.demoShell.rightPanel
        val demo = frame.model.shell.demo
        drawFrame(
            canvas = canvas,
            assets = chrome.frameAssets,
            bounds = bounds,
            fillColor = demoOverlay(),
            borderColor = demoSubtleBorder(),
            alpha = 0.86f,
        )
        canvas.drawRect(tileBounds(bounds.x + 10f, bounds.y + 10f, bounds.width - 20f, bounds.height - 20f), color("1A0E04", 0.62f))
        drawEquipmentSection(
            canvas = canvas,
            chrome = chrome,
            bounds = layout.equipment,
            title = demo.rightEquipmentTitle,
            grid = layout.equipmentSlots,
            slots = demo.equipmentSlots,
        )
        drawInscriptionSection(
            canvas = canvas,
            chrome = chrome,
            bounds = layout.inscriptions,
            title = demo.rightInscriptionsTitle,
            grid = layout.inscriptionSlots,
            slots = demo.inscriptionSlots,
        )
        drawSection(canvas, chrome, layout.backpack, demo.rightBackpackTitle, layout.backpackSlots, demo.backpackSlots)
        drawBackpackPager(canvas, layout.backpackPager, demo.backpackPageLabel)
        drawOperationHintsSection(canvas, chrome, layout.operationHints, demo.rightOperationHintsTitle, demo.operationRows)
    }

    private fun drawSection(
        canvas: TileCanvas,
        chrome: TileChromeAssets,
        bounds: GameShellBounds,
        title: String,
        grid: DemoSlotGridLayout,
        slots: List<TileDemoSlotModel>,
    ) {
        drawSectionTitle(canvas, chrome, bounds, title)
        grid.slotBounds.forEachIndexed { index, slotBounds ->
            val slot = slots.getOrNull(index) ?: return@forEachIndexed
            drawSlotSurface(canvas, chrome, slotBounds, slot)
            drawSlotBadge(canvas, slotBounds, slot)
        }
    }

    private fun drawEquipmentSection(
        canvas: TileCanvas,
        chrome: TileChromeAssets,
        bounds: GameShellBounds,
        title: String,
        grid: DemoSlotGridLayout,
        slots: List<TileDemoSlotModel>,
    ) {
        drawSectionTitle(canvas, chrome, bounds, title)
        grid.slotBounds.forEachIndexed { index, slotBounds ->
            val slot = slots.getOrNull(index) ?: return@forEachIndexed
            drawSlotSurface(canvas, chrome, slotBounds, slot)
            drawSlotBadge(canvas, slotBounds, slot)
        }
    }

    private fun drawInscriptionSection(
        canvas: TileCanvas,
        chrome: TileChromeAssets,
        bounds: GameShellBounds,
        title: String,
        grid: DemoSlotGridLayout,
        slots: List<TileDemoSlotModel>,
    ) {
        drawSectionTitle(canvas, chrome, bounds, title)
        val columnGap = 10f
        val rowWidth = ((bounds.width - 24f - columnGap) / 2f).coerceAtLeast(grid.slotSide + 48f)
        val leftX = bounds.x + 12f
        val rightX = bounds.right - 12f - rowWidth
        grid.slotBounds.forEachIndexed { index, slotBounds ->
            val slot = slots.getOrNull(index) ?: return@forEachIndexed
            val rowX = if (index % 2 == 0) leftX else rightX
            val rowBounds = GameShellBounds(rowX, slotBounds.y, rowWidth, slotBounds.height)
            val rowSlotBounds = GameShellBounds(rowBounds.x + 6f, slotBounds.y, slotBounds.width, slotBounds.height)
            drawRowPlate(canvas, rowBounds)
            drawSlotSurface(canvas, chrome, rowSlotBounds, slot)
            val textX = rowSlotBounds.right + 6f
            drawBoundedText(
                canvas = canvas,
                text = inscriptionRowText(slot),
                x = textX,
                baselineY = rowSlotBounds.y + rowSlotBounds.height * 0.58f,
                maxWidth = rowBounds.right - textX - 6f,
                tone = if (slot.state == TileDemoSlotState.EMPTY) TileTextTone.GRAY else TileTextTone.LIGHT_GRAY,
            )
        }
    }

    private fun drawOperationHintsSection(
        canvas: TileCanvas,
        chrome: TileChromeAssets,
        bounds: GameShellBounds,
        title: String,
        rows: List<TileTextRow>,
    ) {
        drawSectionTitle(canvas, chrome, bounds, title)
        val plateBounds =
            GameShellBounds(
                x = bounds.x + 8f,
                y = bounds.y + 6f,
                width = bounds.width - 16f,
                height = (bounds.height - SECTION_TITLE_HEIGHT - 12f).coerceAtLeast(24f),
            )
        drawFrame(
            canvas = canvas,
            assets = chrome.frameAssets.copy(body = chrome.demoShell.commandHintPlate),
            bounds = plateBounds,
            fillColor = demoBaseDim(),
            borderColor = demoSubtleBorder(),
            alpha = 0.76f,
        )
        val content =
            GameShellBounds(
                x = plateBounds.x + 8f,
                y = plateBounds.y + 6f,
                width = plateBounds.width - 16f,
                height = plateBounds.height - 12f,
        )
        val rowCount = ((content.height - 4f) / OPERATION_LINE_HEIGHT).toInt().coerceAtLeast(1)
        val displayRows = compactVisualOperationRows(operationHintRows(rows), rowCount)
        displayRows.take(rowCount).forEachIndexed { index, row ->
            val baselineY = content.top - 5f - index * OPERATION_LINE_HEIGHT
            val rowBounds = GameShellBounds(content.x, baselineY - 12f, content.width, 15f)
            drawRowPlate(canvas, rowBounds)
            row.frame?.let { frame ->
                canvas.drawAsset(frame, rowBounds.toTileBounds(), alpha = 0.72f)
            }
            var textX = content.x + 4f
            row.forEachVisualIcon(limit = 3) { icon ->
                val iconSide = 13f
                canvas.drawAsset(icon, tileBounds(textX, baselineY - 11f, iconSide, iconSide), alpha = 0.94f)
                textX += iconSide + 3f
            }
            drawBoundedText(
                canvas = canvas,
                text = row.text,
                x = textX,
                baselineY = baselineY,
                maxWidth = (content.right - textX - 4f).coerceAtLeast(1f),
                tone = row.tone,
            )
        }
    }

    private fun compactVisualOperationRows(
        rows: List<TileTextRow>,
        maxRows: Int,
    ): List<TileTextRow> {
        if (rows.size <= maxRows) {
            return rows
        }
        val visualRows = rows.filter(TileTextRow::hasVisualIcons)
        if (visualRows.size <= 1) {
            return rows
        }
        val leadingRows = rows.takeWhile { row -> !row.hasVisualIcons() }.take(1)
        val availableVisualRows = (maxRows - leadingRows.size).coerceAtLeast(1)
        if (visualRows.size <= availableVisualRows) {
            return rows.take(maxRows)
        }
        val groupSize = ((visualRows.size + availableVisualRows - 1) / availableVisualRows).coerceAtLeast(1)
        return leadingRows + visualRows.chunked(groupSize).map(::compactVisualOperationGroup)
    }

    private fun compactVisualOperationGroup(rows: List<TileTextRow>): TileTextRow {
        val icons = mutableListOf<ResolvedVisualAsset>()
        rows.forEach { row -> row.forEachVisualIcon { icon -> icons += icon } }
        return TileTextRow(
            text = rows.joinToString("  ") { row -> row.text.substringBefore("[").trim() },
            tone = rows.first().tone,
            icon = icons.firstOrNull(),
            selected = rows.any(TileTextRow::selected),
            extraIcons = icons.drop(1),
            frame = rows.firstOrNull { row -> row.frame != null }?.frame,
        )
    }

    private fun operationHintRows(rows: List<TileTextRow>): List<TileTextRow> =
        rows
            .filter { row -> row.text.isNotBlank() }
            .let { filteredRows ->
                if (filteredRows.any { row -> row.hasVisualIcons() || row.frame != null }) {
                    filteredRows
                } else if (filteredRows.size >= 6) {
                    listOf(
                        TileTextRow("${filteredRows[0].text}    ${filteredRows[1].text}", TileTextTone.LIGHT_GRAY),
                        TileTextRow("${filteredRows[2].text}    ${filteredRows[3].text}", TileTextTone.LIGHT_GRAY),
                        TileTextRow("${filteredRows[4].text}    ${filteredRows[5].text}", TileTextTone.LIGHT_GRAY),
                    )
                } else if (filteredRows.size >= 5) {
                    listOf(
                        TileTextRow("${filteredRows[0].text}    ${filteredRows[1].text}", TileTextTone.LIGHT_GRAY),
                        TileTextRow("${filteredRows[2].text}    ${filteredRows[3].text}", TileTextTone.LIGHT_GRAY),
                        filteredRows[4],
                    )
                } else {
                    filteredRows.chunked(2).map { pair ->
                        TileTextRow(pair.joinToString("    ") { row -> row.text }, TileTextTone.LIGHT_GRAY)
                    }
                }
            }

    private fun inscriptionRowText(slot: TileDemoSlotModel): String =
        if (slot.detail.isNullOrBlank()) {
            slot.label
        } else {
            "${slot.label}.${slot.detail}"
        }

    private fun drawSlotSurface(
        canvas: TileCanvas,
        chrome: TileChromeAssets,
        slotBounds: GameShellBounds,
        slot: TileDemoSlotModel,
    ) {
        val slotAsset =
            slot.frame
                ?: when {
                    slot.selected -> chrome.slotSelected
                    slot.state == TileDemoSlotState.EMPTY -> chrome.slotEmpty
                    else -> chrome.slotEquipped
                }
        canvas.drawAsset(slotAsset, slotBounds.toTileBounds(), alpha = 0.82f)
        slot.qualityTierId?.let { qualityTierId ->
            drawQualityMarker(canvas, slotBounds, qualityTierId)
        }
        slot.icon?.let { icon ->
            val iconInset = slotBounds.width * 0.18f
            canvas.drawAsset(icon, tileBounds(slotBounds.x + iconInset, slotBounds.y + iconInset, slotBounds.width - iconInset * 2f, slotBounds.height - iconInset * 2f))
        }
        slot.quantityText?.takeIf(String::isNotBlank)?.let { quantity ->
            drawBoundedText(
                canvas = canvas,
                text = quantity,
                x = slotBounds.x + slotBounds.width * 0.52f,
                baselineY = slotBounds.y + 12f,
                maxWidth = slotBounds.width * 0.42f,
                tone = TileTextTone.GOLD,
            )
        }
    }

    private fun drawBackpackPager(
        canvas: TileCanvas,
        bounds: GameShellBounds,
        pageLabel: String,
    ) {
        if (pageLabel.isBlank()) {
            return
        }
        drawRowPlate(canvas, bounds)
        drawBoundedText(
            canvas = canvas,
            text = pageLabel,
            x = bounds.x + 6f,
            baselineY = bounds.y + 14f,
            maxWidth = bounds.width - 12f,
            tone = TileTextTone.LIGHT_GRAY,
        )
    }

    private fun renderHeroCard(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        val chrome = frame.model.chromeAssets ?: return
        val bounds = frame.layout.demoShell.bottomDeck.heroCard
        val content = contentBounds(bounds, ChromeSurfaceKind.Panel)
        drawFrame(
            canvas = canvas,
            assets = chrome.frameAssets.copy(body = chrome.demoShell.heroCardFrame),
            bounds = bounds,
            fillColor = demoRaised(),
            borderColor = demoSubtleBorder(),
            alpha = 0.9f,
        )
        canvas.drawRect(content.toTileBounds(), color("1A0E04", 0.56f))
        val crestSide =
            when {
                content.width >= 360f -> 112f
                content.width >= 240f -> 96f
                else -> 84f
            }.coerceAtMost(content.height - 18f)
        canvas.drawAsset(
            chrome.demoShell.heroCrestPlaceholder,
            tileBounds(content.x + 6f, content.y + (content.height - crestSide) / 2f, crestSide, crestSide),
        )
        val gaugeX = content.x + crestSide + 22f
        val gaugeWidth = (content.right - gaugeX).coerceAtLeast(1f)
        drawBoundedText(canvas, frame.textLayout.playerName, gaugeX, content.top - 8f, gaugeWidth, TileTextTone.GOLD)
        frame.model.shell.demo.heroSummaryLines.firstOrNull()?.let { floor ->
            drawBoundedText(canvas, floor, gaugeX, content.top - 28f, gaugeWidth, TileTextTone.LIGHT_GRAY)
        }
        val statLines = frame.model.shell.demo.heroSummaryLines.drop(3).take(2)
        if (statLines.isNotEmpty()) {
            val statsBaselineY = if (frame.model.hud.statusIcons.isEmpty()) content.y + 20f else content.y + 7f
            drawBoundedText(canvas, statLines.joinToString("   "), gaugeX, statsBaselineY, gaugeWidth, TileTextTone.LIGHT_GRAY)
        }
        val gauges = listOf(frame.model.hud.hpGauge, frame.model.hud.resourceGauge)
        val gaugeHeight = 14f
        val gaugeGap = 8f
        val firstGaugeY = (content.y + 48f).coerceAtMost(content.top - 76f)
        gauges.reversed().forEachIndexed { index, gauge ->
            val y = firstGaugeY + index * (gaugeHeight + gaugeGap)
            val icon = chrome.iconForGauge(gauge)
            val drawX =
                if (icon == null) {
                    gaugeX
                } else {
                    val iconSide = gaugeHeight + 4f
                    canvas.drawAsset(icon, tileBounds(gaugeX, y - 1f, iconSide, iconSide))
                    gaugeX + iconSide + 4f
                }
            val barWidth = (content.right - drawX).coerceAtLeast(1f)
            drawGauge(canvas, gauge, drawX, y, barWidth, gaugeHeight, showSummary = false)
            drawBoundedText(canvas, gauge.summary, drawX + 6f, y + gaugeHeight - 3f, barWidth - 12f, TileTextTone.WHITE)
        }
        renderStatusIcons(
            canvas = canvas,
            frame = frame,
            content = content,
            startX = gaugeX,
            iconY = content.y + 24f,
        )
    }

    private fun renderActionDeck(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        val chrome = frame.model.chromeAssets ?: return
        val deck = frame.layout.demoShell.bottomDeck.actionDeck
        drawFrame(
            canvas = canvas,
            assets = chrome.frameAssets.copy(body = chrome.demoShell.actionDeckFrame),
            bounds = deck,
            fillColor = demoRaised(),
            borderColor = demoSubtleBorder(),
            alpha = 0.88f,
        )
        frame.layout.demoShell.bottomDeck.actionSlotBounds.forEachIndexed { index, slotBounds ->
            val entry = frame.model.actionPanel.entries.getOrNull(index)
            val textPlateHeight = (slotBounds.height * 0.22f).coerceIn(24f, 30f)
            val textPlateBounds =
                GameShellBounds(
                    x = slotBounds.x,
                    y = slotBounds.y + 6f,
                    width = slotBounds.width,
                    height = textPlateHeight,
                )
            val iconSocketSide =
                minOf(slotBounds.width, slotBounds.height - textPlateHeight - 13f)
                    .coerceIn(38f, slotBounds.width)
            val iconSocketBounds =
                GameShellBounds(
                    x = slotBounds.x + (slotBounds.width - iconSocketSide) / 2f,
                    y = slotBounds.top - iconSocketSide - 5f,
                    width = iconSocketSide,
                    height = iconSocketSide,
                )
            canvas.drawAsset(if (entry == null) chrome.slotEmpty else chrome.slotEquipped, iconSocketBounds.toTileBounds(), alpha = 0.9f)
            entry?.icon?.let { icon ->
                val iconInset = iconSocketSide * 0.17f
                val iconSide = iconSocketSide - iconInset * 2f
                val iconX = iconSocketBounds.x + iconInset
                val iconY = iconSocketBounds.y + iconInset
                canvas.drawAsset(
                    icon,
                    tileBounds(iconX, iconY, iconSide, iconSide),
                )
            }
            val text = frame.textLayout.hotbar.getOrNull(index)
            if (text != null) {
                drawBoundedText(canvas, text.hotkey, iconSocketBounds.x + 7f, iconSocketBounds.top - 7f, iconSocketBounds.width - 14f, TileTextTone.GOLD)
                drawRowPlate(canvas, textPlateBounds)
                drawBoundedText(canvas, text.hotkey, textPlateBounds.x + 5f, textPlateBounds.y + textPlateHeight - 7f, 14f, TileTextTone.GOLD)
                drawBoundedText(canvas, text.label, textPlateBounds.x + 22f, textPlateBounds.y + textPlateHeight - 7f, textPlateBounds.width - 26f, TileTextTone.WHITE)
            }
        }
    }

    private fun renderLogAndStats(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        val chrome = frame.model.chromeAssets ?: return
        val log = frame.layout.demoShell.bottomDeck.logDeck
        drawFrame(
            canvas = canvas,
            assets = chrome.frameAssets.copy(body = chrome.demoShell.logDeckFrame),
            bounds = log,
            fillColor = demoOverlay(),
            borderColor = demoSubtleBorder(),
            alpha = 0.86f,
        )
        drawLogDeckSurface(canvas, log)
        val logContent = contentBounds(log, ChromeSurfaceKind.Panel)
        val rowCount = maxRows(logContent.height - 8f)
        frame.textLayout.messageLines
            .flatMap { line ->
                TileTextMetrics.wrapTextToWidth(line.text, logContent.width * TEXT_WIDTH_SAFETY, TileTextStyle.SMALL)
                    .map { text -> line.copy(text = text) }
            }
            .take(rowCount)
            .forEachIndexed { index, line ->
                val baselineY = logContent.top - 6f - index * SMALL_LINE_HEIGHT
                var textX = logContent.x + 8f
                line.icon?.let { icon ->
                    val iconSide = 14f
                    canvas.drawAsset(icon, tileBounds(textX, baselineY - 12f, iconSide, iconSide), alpha = 0.86f)
                    textX += iconSide + 5f
                }
                drawBoundedText(canvas, line.text, textX, baselineY, logContent.right - textX - 8f, line.tone)
            }
    }

    private fun drawLogDeckSurface(
        canvas: TileCanvas,
        bounds: GameShellBounds,
    ) {
        val edge = ChromeFramePainter.frameEdgeSize
        val body =
            GameShellBounds(
                x = bounds.x + edge,
                y = bounds.y + edge,
                width = (bounds.width - edge * 2f).coerceAtLeast(1f),
                height = (bounds.height - edge * 2f).coerceAtLeast(1f),
            )
        canvas.drawRect(body.toTileBounds(), color("070605", 0.9f))
        canvas.drawRect(tileBounds(body.x, body.top - 2f, body.width, 2f), color("1CB7C8", 0.24f))
        canvas.drawRect(tileBounds(body.x, body.y, 2f, body.height), color("B8873E", 0.18f))
    }

    private fun drawDivider(
        canvas: TileCanvas,
        chrome: TileChromeAssets,
        bounds: GameShellBounds,
    ) {
        canvas.drawAsset(chrome.demoShell.rightSectionDivider, tileBounds(bounds.x + 8f, bounds.top - 3f, bounds.width - 16f, 3f), alpha = 0.76f)
    }

    private fun drawSectionTitle(
        canvas: TileCanvas,
        chrome: TileChromeAssets,
        bounds: GameShellBounds,
        title: String,
    ) {
        drawDivider(canvas, chrome, bounds)
        drawBoundedText(canvas, title, bounds.x + 12f, bounds.top - 5f, bounds.width - 24f, TileTextTone.GOLD)
    }

    private fun drawRowPlate(
        canvas: TileCanvas,
        bounds: GameShellBounds,
    ) {
        canvas.drawRect(bounds.toTileBounds(), color("0A0D10", 0.72f))
        canvas.drawRect(tileBounds(bounds.x, bounds.y, 2f, bounds.height), color("B8873E", 0.42f))
        canvas.drawRect(tileBounds(bounds.x, bounds.top - 1f, bounds.width, 1f), color("6E5630", 0.32f))
    }

    private fun maxRows(height: Float): Int =
        (height / SMALL_LINE_HEIGHT).toInt().coerceAtLeast(1)

    private fun drawSlotBadge(
        canvas: TileCanvas,
        slotBounds: GameShellBounds,
        slot: TileDemoSlotModel,
    ) {
        if (slot.state == TileDemoSlotState.EMPTY || slot.label.isBlank()) {
            return
        }
        if (slot.icon != null && !slot.showBadge) {
            return
        }
        val badgeWidth = minOf(slotBounds.width - 6f, 22f + slot.label.length * 7f).coerceAtLeast(18f)
        val badgeBounds =
            GameShellBounds(
                x = slotBounds.x + 3f,
                y = slotBounds.y + 3f,
                width = badgeWidth,
                height = 14f,
            )
        drawRowPlate(canvas, badgeBounds)
        drawBoundedText(
            canvas = canvas,
            text = slot.label,
            x = badgeBounds.x + 4f,
            baselineY = badgeBounds.y + 11f,
            maxWidth = badgeBounds.width - 8f,
            tone = TileTextTone.GOLD,
        )
    }

    private fun drawQualityMarker(
        canvas: TileCanvas,
        slotBounds: GameShellBounds,
        qualityTierId: String,
    ) {
        val markerColor =
            when (qualityTierId.uppercase()) {
                "RARE" -> color("D8A73F", 0.82f)
                "MAGIC" -> color("446ED8", 0.72f)
                else -> color("C5C9C4", 0.42f)
            }
        canvas.drawRect(tileBounds(slotBounds.x + 3f, slotBounds.y + 3f, 3f, slotBounds.height - 6f), markerColor)
    }

    private fun drawGauge(
        canvas: TileCanvas,
        gauge: TileGaugeModel,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        showSummary: Boolean = true,
    ) {
        canvas.drawRect(tileBounds(x, y, width, height), UiDesignTokens.color.bar.background.color())
        canvas.drawRect(tileBounds(x + 2f, y + 2f, (width - 4f) * gauge.percent, height - 4f), gaugeColor(gauge))
        if (showSummary) {
            drawBoundedText(canvas, gauge.summary, x + 4f, y + height + 13f, width - 8f, TileTextTone.WHITE)
        }
    }

    private fun renderPaneFocus(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        val bounds =
            when (frame.paneFocusAnchor) {
                PaneFocusAnchor.WORLD -> frame.layout.demoShell.mapStage
                PaneFocusAnchor.CONTEXT -> frame.layout.demoShell.bottomDeck.logDeck
                PaneFocusAnchor.CHARACTER_ACTION -> frame.layout.demoShell.bottomDeck.actionDeck
                null -> return
            }
        drawRectOutline(
            canvas = canvas,
            bounds = bounds,
            stroke = UiDesignTokens.stroke.medium,
            color =
                if (frame.paneFocusAnchor == PaneFocusAnchor.WORLD) {
                    color("B8873E", 0.58f)
                } else {
                    demoFocus()
                },
        )
    }

    private fun drawRectOutline(
        canvas: TileCanvas,
        bounds: GameShellBounds,
        stroke: Float,
        color: Color,
    ) {
        canvas.drawRect(tileBounds(bounds.x, bounds.y, stroke, bounds.height), color)
        canvas.drawRect(tileBounds(bounds.right - stroke, bounds.y, stroke, bounds.height), color)
        canvas.drawRect(tileBounds(bounds.x, bounds.y, bounds.width, stroke), color)
        canvas.drawRect(tileBounds(bounds.x, bounds.top - stroke, bounds.width, stroke), color)
    }

    private fun renderStatusIcons(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
        content: GameShellBounds,
        startX: Float,
        iconY: Float,
    ) {
        val iconSide = 20f
        val iconStride = 34f
        val requiredWidth = frame.model.hud.statusIcons.size * iconStride - 2f
        var statusX = minOf(startX, content.right - requiredWidth).coerceAtLeast(content.x)
        frame.model.hud.statusIcons.forEach { icon ->
            if (statusX + iconSide + 2f > content.right || iconY + iconSide + 6f > content.top) {
                return@forEach
            }
            val nonInteractiveFold = icon.isFoldBadge && icon.foldInteraction?.interactive == false
            val accentColor =
                if (nonInteractiveFold) {
                    UiDesignTokens.color.text.disabled.color()
                } else {
                    StatusHudRenderer.accentColor(icon.category)
                }
            canvas.drawRect(tileBounds(statusX - 1f, iconY - 1f, iconSide + 2f, iconSide + 2f), accentColor)
            canvas.drawAsset(
                icon.asset,
                tileBounds(statusX, iconY, iconSide, iconSide),
                alpha = if (nonInteractiveFold) 0.52f else 1f,
            )
            val badgeText =
                if (icon.isFoldBadge) {
                    "+${icon.hiddenPresentations.size}"
                } else {
                    icon.badgeText
                }
            canvas.drawText(
                TileTextStyle.SMALL,
                badgeText,
                tilePosition(statusX, iconY - 4f),
                if (nonInteractiveFold) UiDesignTokens.color.text.disabled.color() else StatusHudRenderer.badgeColor(icon.category),
            )
            statusX += iconStride
        }
    }

    private fun gaugeColor(gauge: TileGaugeModel): Color =
        when (gauge.resourceTypeId) {
            "HEALTH" -> UiDesignTokens.color.bar.hp.color()
            "EXPERIENCE" -> UiDesignTokens.color.bar.experience.color()
            "STAMINA" -> color("2F8F46", 0.95f)
            else -> UiDesignTokens.color.bar.resource.color()
        }

    private fun drawBoundedText(
        canvas: TileCanvas,
        text: String,
        x: Float,
        baselineY: Float,
        maxWidth: Float,
        tone: TileTextTone,
    ) {
        if (text.isBlank() || maxWidth <= 0f) {
            return
        }
        canvas.drawText(
            TileTextStyle.SMALL,
            TileTextMetrics.truncateTextToWidth(text, maxWidth, TileTextStyle.SMALL),
            tilePosition(x, baselineY),
            toneColor(tone),
        )
    }

    private fun drawFrame(
        canvas: TileCanvas,
        assets: ChromeFrameAssets,
        bounds: GameShellBounds,
        fillColor: Color,
        borderColor: Color,
        alpha: Float,
    ) {
        ChromeFramePainter.drawFrame(
            sink = canvas.asChromeFrameSink(),
            request =
                ChromeFrameDrawRequest(
                    bounds = bounds.toChromeFrameBounds(),
                    assets = assets,
                    fillColor = fillColor,
                    borderColor = borderColor,
                    alpha = alpha,
                ),
        )
    }

    private fun contentBounds(
        bounds: GameShellBounds,
        kind: ChromeSurfaceKind,
    ): GameShellBounds =
        ChromeFramePainter.contentBounds(bounds.toChromeFrameBounds(), kind).toGameShellBounds()

    private fun GameShellBounds.toChromeFrameBounds(): ChromeFrameBounds =
        ChromeFrameBounds(x = x, y = y, width = width, height = height)

    private fun ChromeFrameBounds.toGameShellBounds(): GameShellBounds =
        GameShellBounds(x = x, y = y, width = width, height = height)

    private fun GameShellBounds.toTileBounds(): TileFloatBounds =
        tileBounds(x, y, width, height)

    private fun TileCanvas.asChromeFrameSink(): ChromeFrameDrawSink =
        object : ChromeFrameDrawSink {
            override fun drawRect(draw: ChromeFrameRectDraw) {
                val bounds = draw.bounds
                this@asChromeFrameSink.drawRect(tileBounds(bounds.x, bounds.y, bounds.width, bounds.height), draw.color)
            }

            override fun drawAsset(draw: ChromeFrameAssetDraw) {
                val bounds = draw.bounds
                this@asChromeFrameSink.drawAsset(draw.asset, tileBounds(bounds.x, bounds.y, bounds.width, bounds.height), draw.alpha)
            }
        }

    private fun toneColor(tone: TileTextTone): Color =
        when (tone) {
            TileTextTone.GOLD -> color("D99A2B", 0.9f)
            TileTextTone.WHITE -> color("E7E1D3", 1f)
            TileTextTone.LIGHT_GRAY -> color("AEB5BF", 1f)
            TileTextTone.CYAN -> demoFocus()
            TileTextTone.GRAY -> color("59616C", 1f)
            TileTextTone.GREEN -> UiDesignTokens.color.status.badge.stack.color()
            TileTextTone.RED -> UiDesignTokens.color.status.badge.turns.color()
            TileTextTone.BLUE -> UiDesignTokens.color.quality.magic.color()
            TileTextTone.MAGENTA -> UiDesignTokens.color.telegraph.lethal.color()
        }

    private fun demoBase(): Color = color("05070A", 1f)

    private fun demoBaseDim(): Color = color("1A0E04", 0.82f)

    private fun demoRaised(): Color = color("15100A", 0.96f)

    private fun demoOverlay(): Color = color("100C08", 0.93f)

    private fun demoSubtleBorder(): Color = color("4A3420", 0.86f)

    private fun demoStrongBorder(): Color = color("D99A2B", 0.54f)

    private fun demoSlotEmpty(): Color = color("0F0D0B", 0.92f)

    private fun demoFocus(): Color = color("1CB7C8", 1f)

    private fun color(
        hex: String,
        alpha: Float,
    ): Color = Color.valueOf(hex).also { color -> color.a = alpha }
}
