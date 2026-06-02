package com.ktome.client.render

import com.badlogic.gdx.graphics.Color
import com.ktome.client.assets.ResolvedVisualAsset
import com.ktome.client.render.layout.DemoBottomDeckLayout
import com.ktome.client.render.layout.DemoRightPanelLayout
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
    private const val CAPTION_LINE_HEIGHT = 16f
    private const val OPERATION_LINE_HEIGHT = 15f
    private const val SECTION_TITLE_HEIGHT = 18f
    private const val TEXT_WIDTH_SAFETY = 0.9f
    private const val BOTTOM_CHILD_FRAME_ALPHA = 0.74f

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
        canvas.drawRect(bounds.toTileBounds(), color("0B0703", 0.96f))
        chrome?.let { assets ->
            drawFrame(
                canvas = canvas,
                assets = assets.frameAssets.copy(body = assets.demoShell.mapStageFrame),
                bounds = bounds,
                fillColor = color("0B0703", 0.9f),
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
        canvas.drawAsset(assets.demoShell.mapStageBackdrop, bounds.toTileBounds(), alpha = 1f)
        canvas.drawRect(bounds.toTileBounds(), color("1B0E04", 0.14f))
        drawAmbientDungeonStonework(canvas, bounds)
        canvas.drawRect(
            tileBounds(
                bounds.x + bounds.width * 0.16f,
                bounds.y + bounds.height * 0.18f,
                bounds.width * 0.62f,
                bounds.height * 0.58f,
            ),
            color("2A1708", 0.10f),
        )
        drawStageFeather(canvas, bounds)
    }

    private fun drawAmbientDungeonStonework(
        canvas: TileCanvas,
        bounds: GameShellBounds,
    ) {
        val tileSize = 96f
        var row = 0
        var y = bounds.y
        while (y < bounds.top) {
            var column = 0
            var x = bounds.x
            while (x < bounds.right) {
                val width = minOf(tileSize, bounds.right - x)
                val height = minOf(tileSize, bounds.top - y)
                val variant = (row * 13 + column * 7) % 11
                val baseAlpha = if ((row + column) % 2 == 0) 0.024f else 0.016f
                canvas.drawRect(tileBounds(x + 2f, y + 2f, width - 4f, height - 4f), color("15110B", baseAlpha))
                if (variant % 3 == 0) {
                    canvas.drawRect(tileBounds(x + 14f, y + 22f, (width - 28f).coerceAtLeast(4f), 1f), color("2A1A0D", 0.026f))
                }
                if (variant % 5 == 0) {
                    canvas.drawRect(tileBounds(x + 22f, y + 16f, 18f, 1f), color("C49B61", 0.018f))
                    canvas.drawRect(tileBounds(x + 30f, y + 14f, 1f, 12f), color("050604", 0.022f))
                }
                x += tileSize
                column += 1
            }
            y += tileSize
            row += 1
        }
        canvas.drawRect(
            tileBounds(
                bounds.x + bounds.width * 0.08f,
                bounds.y + bounds.height * 0.08f,
                bounds.width * 0.84f,
                bounds.height * 0.84f,
            ),
            color("050604", 0.18f),
        )
    }

    fun renderFrontstageSurface(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        val surface = frame.model.frontstageSurface ?: return
        val chrome = frame.model.chromeAssets ?: return
        val stageContent = contentBounds(frame.layout.demoShell.mapStage, ChromeSurfaceKind.Panel)
        val horizontalMargin = if (stageContent.width >= 920f) 46f else 28f
        val width = (stageContent.width - horizontalMargin * 2f).coerceAtLeast(320f).coerceAtMost(980f)
        val maxHeight =
            when (surface.kind) {
                TileFrontstageSurfaceKind.SHOP,
                TileFrontstageSurfaceKind.SHOP_REPLACEMENT,
                -> 560f
                TileFrontstageSurfaceKind.ROUTE_SELECTION,
                TileFrontstageSurfaceKind.STAT_ASSIGN,
                TileFrontstageSurfaceKind.REWARD,
                -> 500f
            }
        val height = (stageContent.height * 0.72f).coerceAtLeast(300f).coerceAtMost(minOf(maxHeight, stageContent.height - 36f).coerceAtLeast(260f))
        val bounds =
            GameShellBounds(
                x = stageContent.x + (stageContent.width - width) / 2f,
                y = stageContent.y + (stageContent.height - height) / 2f,
                width = width,
                height = height,
            )
        drawFrame(
            canvas = canvas,
            assets = chrome.frameAssets.copy(body = chrome.modalBody),
            bounds = bounds,
            fillColor = color("05070A", 0.94f),
            borderColor = demoStrongBorder(),
            alpha = 0.9f,
        )
        drawFrontstageSurfaceBody(canvas, bounds)
        val content = contentBounds(bounds, ChromeSurfaceKind.Modal)
        val headerHeight = 76f
        surface.eyebrow?.let { eyebrow ->
            drawBoundedText(canvas, eyebrow, content.x, content.top - 10f, content.width * 0.42f, TileTextTone.LIGHT_GRAY)
        }
        drawBoundedText(canvas, surface.title, content.x, content.top - 34f, content.width * 0.68f, TileTextTone.GOLD, style = TileTextStyle.UI)
        surface.summary?.let { summary ->
            drawBoundedText(canvas, summary, content.x, content.top - 58f, content.width * 0.80f, TileTextTone.LIGHT_GRAY)
        }
        val columnsBounds =
            GameShellBounds(
                x = content.x,
                y = content.y + 42f,
                width = content.width,
                height = (content.height - headerHeight - 44f).coerceAtLeast(120f),
            )
        drawFrontstageColumns(canvas, surface, columnsBounds)
        drawFrontstageFooter(canvas, surface.footerRows, GameShellBounds(content.x, content.y, content.width, 32f))
    }

    private fun drawFrontstageSurfaceBody(
        canvas: TileCanvas,
        bounds: GameShellBounds,
    ) {
        canvas.drawRect(tileBounds(bounds.x + 10f, bounds.y + 10f, bounds.width - 20f, bounds.height - 20f), color("080604", 0.86f))
        canvas.drawRect(tileBounds(bounds.x + 18f, bounds.top - 52f, bounds.width - 36f, 1f), color("D99A2B", 0.28f))
        canvas.drawRect(tileBounds(bounds.x + 18f, bounds.y + 42f, bounds.width - 36f, 1f), color("1CB7C8", 0.16f))
        canvas.drawRect(tileBounds(bounds.x + 10f, bounds.y + 10f, 2f, bounds.height - 20f), color("B8873E", 0.22f))
        canvas.drawRect(tileBounds(bounds.right - 12f, bounds.y + 10f, 2f, bounds.height - 20f), color("050604", 0.42f))
    }

    private fun drawFrontstageColumns(
        canvas: TileCanvas,
        surface: TileFrontstageSurfaceModel,
        bounds: GameShellBounds,
    ) {
        if (surface.columns.isEmpty()) {
            return
        }
        if (surface.columns.size == 1) {
            drawFrontstageGridColumn(canvas, surface, bounds, surface.columns.single())
            return
        }
        val gap = 12f
        val columnWidth = ((bounds.width - gap * (surface.columns.size - 1)) / surface.columns.size).coerceAtLeast(80f)
        surface.columns.forEachIndexed { index, column ->
            val columnBounds =
                GameShellBounds(
                    x = bounds.x + index * (columnWidth + gap),
                    y = bounds.y,
                    width = columnWidth,
                    height = bounds.height,
                )
            drawFrontstageColumn(canvas, surface.kind, columnBounds, column)
        }
    }

    private fun drawFrontstageGridColumn(
        canvas: TileCanvas,
        surface: TileFrontstageSurfaceModel,
        bounds: GameShellBounds,
        column: TileFrontstageColumnModel,
    ) {
        drawColumnPlate(canvas, bounds)
        drawBoundedText(canvas, column.title, bounds.x + 12f, bounds.top - 10f, bounds.width - 24f, TileTextTone.GOLD)
        val cards = column.cards
        if (cards.isEmpty()) {
            return
        }
        val preferredColumns =
            when (surface.kind) {
                TileFrontstageSurfaceKind.STAT_ASSIGN -> 4
                TileFrontstageSurfaceKind.ROUTE_SELECTION -> 3
                TileFrontstageSurfaceKind.REWARD -> 2
                else -> 2
            }.coerceAtMost(cards.size).coerceAtLeast(1)
        val gridGap = 10f
        val cardArea =
            GameShellBounds(
                x = bounds.x + 10f,
                y = bounds.y + 10f,
                width = bounds.width - 20f,
                height = bounds.height - 44f,
            )
        val rows = (cards.size + preferredColumns - 1) / preferredColumns
        val cardWidth = ((cardArea.width - gridGap * (preferredColumns - 1)) / preferredColumns).coerceAtLeast(80f)
        val cardHeight = ((cardArea.height - gridGap * (rows - 1)) / rows).coerceIn(88f, 154f)
        cards.forEachIndexed { index, card ->
            val columnIndex = index % preferredColumns
            val rowIndex = index / preferredColumns
            drawFrontstageCard(
                canvas = canvas,
                card = card,
                bounds =
                    GameShellBounds(
                        x = cardArea.x + columnIndex * (cardWidth + gridGap),
                        y = cardArea.top - (rowIndex + 1) * cardHeight - rowIndex * gridGap,
                        width = cardWidth,
                        height = cardHeight,
                    ),
            )
        }
    }

    private fun drawFrontstageColumn(
        canvas: TileCanvas,
        kind: TileFrontstageSurfaceKind,
        bounds: GameShellBounds,
        column: TileFrontstageColumnModel,
    ) {
        drawColumnPlate(canvas, bounds)
        drawBoundedText(canvas, column.title, bounds.x + 12f, bounds.top - 10f, bounds.width - 24f, TileTextTone.GOLD)
        val cards = column.cards
        if (cards.isEmpty()) {
            return
        }
        val cardGap = 8f
        val cardArea =
            GameShellBounds(
                x = bounds.x + 8f,
                y = bounds.y + 10f,
                width = bounds.width - 16f,
                height = bounds.height - 42f,
            )
        val preferredHeight =
            when (kind) {
                TileFrontstageSurfaceKind.SHOP_REPLACEMENT -> if (cards.size == 1) cardArea.height else 96f
                TileFrontstageSurfaceKind.SHOP -> 76f
                else -> 92f
            }
        val fittedHeight = ((cardArea.height - cardGap * (cards.size - 1)) / cards.size).coerceAtMost(preferredHeight).coerceAtLeast(54f)
        cards.forEachIndexed { index, card ->
            val cardTop = cardArea.top - index * (fittedHeight + cardGap)
            if (cardTop - fittedHeight < cardArea.y - 1f) {
                return@forEachIndexed
            }
            drawFrontstageCard(
                canvas = canvas,
                card = card,
                bounds = GameShellBounds(cardArea.x, cardTop - fittedHeight, cardArea.width, fittedHeight),
            )
        }
    }

    private fun drawColumnPlate(
        canvas: TileCanvas,
        bounds: GameShellBounds,
    ) {
        canvas.drawRect(bounds.toTileBounds(), color("0A0D10", 0.72f))
        canvas.drawRect(tileBounds(bounds.x, bounds.top - 2f, bounds.width, 2f), color("B8873E", 0.22f))
        canvas.drawRect(tileBounds(bounds.x, bounds.y, 1f, bounds.height), color("5D4A31", 0.34f))
        canvas.drawRect(tileBounds(bounds.right - 1f, bounds.y, 1f, bounds.height), color("050604", 0.38f))
    }

    private fun drawFrontstageCard(
        canvas: TileCanvas,
        card: TileFrontstageCardModel,
        bounds: GameShellBounds,
    ) {
        val bodyAlpha = if (card.disabled) 0.42f else 0.82f
        canvas.drawRect(bounds.toTileBounds(), color("05070A", bodyAlpha))
        card.frame?.let { frameAsset ->
            canvas.drawAsset(frameAsset, bounds.toTileBounds(), alpha = if (card.disabled) 0.44f else 0.74f)
        }
        val accent = if (card.selected) demoFocus() else toneColor(card.tone)
        canvas.drawRect(tileBounds(bounds.x, bounds.y, 3f, bounds.height), accent.also { color -> color.a = if (card.selected) 0.86f else 0.48f })
        canvas.drawRect(tileBounds(bounds.x, bounds.top - 1f, bounds.width, 1f), color("8A6B42", if (card.selected) 0.52f else 0.26f))
        if (card.selected) {
            canvas.drawRect(tileBounds(bounds.x + 3f, bounds.y, bounds.width - 3f, bounds.height), color("1CB7C8", 0.08f))
        }
        val iconSide = minOf(36f, bounds.height - 18f).coerceAtLeast(22f)
        var textX = bounds.x + 10f
        card.icon?.let { icon ->
            canvas.drawRect(tileBounds(bounds.x + 8f, bounds.top - iconSide - 8f, iconSide, iconSide), color("080604", 0.82f))
            canvas.drawAsset(icon, tileBounds(bounds.x + 11f, bounds.top - iconSide - 5f, iconSide - 6f, iconSide - 6f), alpha = if (card.disabled) 0.48f else 0.96f)
            textX = bounds.x + iconSide + 18f
        }
        card.typeIcon?.let { icon ->
            canvas.drawAsset(icon, tileBounds(bounds.right - 42f, bounds.top - 38f, 24f, 24f), alpha = 0.86f)
        }
        card.stateIcon?.let { icon ->
            canvas.drawAsset(icon, tileBounds(bounds.right - 22f, bounds.top - 28f, 16f, 16f), alpha = 0.86f)
        }
        drawBoundedText(canvas, card.title, textX, bounds.top - 12f, bounds.right - textX - 12f, card.tone)
        card.summary?.let { summary ->
            drawBoundedText(canvas, summary, textX, bounds.top - 31f, bounds.right - textX - 12f, TileTextTone.LIGHT_GRAY)
        }
        var baseline = bounds.top - 50f
        card.detailRows.take(3).forEach { row ->
            if (baseline < bounds.y + 12f) {
                return@forEach
            }
            var rowTextX = textX
            row.icon?.let { icon ->
                canvas.drawAsset(icon, tileBounds(rowTextX, baseline - 12f, 12f, 12f), alpha = 0.86f)
                rowTextX += 16f
            }
            drawBoundedText(canvas, row.text, rowTextX, baseline, bounds.right - rowTextX - 12f, row.tone)
            baseline -= 17f
        }
    }

    private fun drawFrontstageFooter(
        canvas: TileCanvas,
        rows: List<TileTextRow>,
        bounds: GameShellBounds,
    ) {
        if (rows.isEmpty()) {
            return
        }
        drawRowPlate(canvas, bounds)
        var cursorX = bounds.x + 8f
        rows.take(3).forEach { row ->
            val width = (TileTextMetrics.approximateTextWidth(row.text, TileTextStyle.SMALL) + 18f).coerceIn(76f, bounds.width * 0.42f)
            if (cursorX + width > bounds.right - 8f) {
                return@forEach
            }
            drawBoundedText(canvas, row.text, cursorX + 8f, bounds.y + 21f, width - 16f, row.tone)
            cursorX += width + 10f
        }
    }

    fun renderShell(
        canvas: TileCanvas,
        frame: ShellRenderFrame,
    ) {
        renderNavRail(canvas, frame)
        canvas.flushLayer(TileLayerFlushReason.SHELL_NAV_RAIL)
        renderRightPanel(canvas, frame)
        canvas.flushLayer(TileLayerFlushReason.SHELL_RIGHT_PANEL)
        drawBottomHudFoundation(canvas, frame.layout.demoShell.bottomDeck)
        renderHeroCard(canvas, frame)
        canvas.flushLayer(TileLayerFlushReason.SHELL_BOTTOM_HERO)
        renderActionDeck(canvas, frame)
        canvas.flushLayer(TileLayerFlushReason.SHELL_BOTTOM_ACTION_DECK)
        renderLogAndStats(canvas, frame)
        drawBottomHudCohesionRails(canvas, frame.layout.demoShell.bottomDeck)
        renderPaneFocus(canvas, frame)
        canvas.flushLayer(TileLayerFlushReason.SHELL_BOTTOM_LOG_DECK)
    }

    private fun drawBottomHudFoundation(
        canvas: TileCanvas,
        bottom: DemoBottomDeckLayout,
    ) {
        val hero = bottom.heroCard
        val action = bottom.actionDeck
        val log = bottom.logDeck
        val slabX = (hero.x - 4f).coerceAtLeast(bottom.bounds.x + 4f)
        val slabRight = (log.right + 4f).coerceAtMost(bottom.bounds.right - 4f)
        val slabY = (hero.y - 2f).coerceAtLeast(bottom.bounds.y + 3f)
        val slabHeight = (hero.height + 4f).coerceAtMost(bottom.bounds.top - slabY - 3f).coerceAtLeast(1f)
        val slabWidth = (slabRight - slabX).coerceAtLeast(1f)
        canvas.drawRect(tileBounds(slabX, slabY, slabWidth, slabHeight), color("050604", 0.144f))
        canvas.drawRect(
            tileBounds(slabX + 12f, slabY + 10f, (slabWidth - 24f).coerceAtLeast(1f), (slabHeight - 20f).coerceAtLeast(1f)),
            color("241307", 0.054f),
        )

        val topRailY = (hero.top - 5f).coerceIn(bottom.bounds.y + 3f, bottom.bounds.top - 5f)
        canvas.drawRect(tileBounds(slabX + 2f, topRailY, (slabWidth - 4f).coerceAtLeast(1f), 4f), color("0B0A08", 0.118f))
        canvas.drawRect(tileBounds(slabX + 20f, topRailY + 3f, (slabWidth - 40f).coerceAtLeast(1f), 1f), color("D99A2B", 0.072f))
        canvas.drawRect(tileBounds(slabX + 28f, slabY + 6f, (slabWidth - 56f).coerceAtLeast(1f), 1f), color("1CB7C8", 0.050f))
        canvas.drawRect(tileBounds(slabX + 12f, slabY + 3f, (slabWidth - 24f).coerceAtLeast(1f), 2f), color("050604", 0.22f))

        listOf((hero.right + action.x) / 2f, (action.right + log.x) / 2f).forEach { centerX ->
            val postY = hero.y + 14f
            val postHeight = (hero.height - 28f).coerceAtLeast(1f)
            canvas.drawRect(tileBounds(centerX - 2.5f, postY, 5f, postHeight), color("0B0A08", 0.126f))
            canvas.drawRect(tileBounds(centerX - 0.5f, postY + 12f, 1f, (postHeight - 24f).coerceAtLeast(1f)), color("D99A2B", 0.056f))
            canvas.drawRect(tileBounds(centerX - 6f, topRailY + 1f, 12f, 2f), color("1CB7C8", 0.052f))
        }
    }

    private fun drawBottomHudCohesionRails(
        canvas: TileCanvas,
        bottom: DemoBottomDeckLayout,
    ) {
        val hero = bottom.heroCard
        val action = bottom.actionDeck
        val log = bottom.logDeck
        val railX = (hero.x - 2f).coerceAtLeast(bottom.bounds.x + 4f)
        val railRight = (log.right + 2f).coerceAtMost(bottom.bounds.right - 4f)
        val railWidth = (railRight - railX).coerceAtLeast(1f)
        val topRailY = (hero.top - 8f).coerceIn(bottom.bounds.y + 6f, bottom.bounds.top - 8f)
        val bottomRailY = (hero.y + 6f).coerceIn(bottom.bounds.y + 4f, bottom.bounds.top - 10f)

        canvas.drawRect(tileBounds(railX, topRailY, railWidth, 5f), color("0B0A08", 0.182f))
        canvas.drawRect(tileBounds(railX + 24f, topRailY + 4f, (railWidth - 48f).coerceAtLeast(1f), 1f), color("D99A2B", 0.116f))
        canvas.drawRect(tileBounds(railX + 34f, topRailY + 1f, (railWidth - 68f).coerceAtLeast(1f), 1f), color("1CB7C8", 0.046f))
        canvas.drawRect(tileBounds(railX, bottomRailY, railWidth, 4f), color("0B0A08", 0.158f))
        canvas.drawRect(tileBounds(railX + 22f, bottomRailY + 3f, (railWidth - 44f).coerceAtLeast(1f), 1f), color("B8873E", 0.086f))

        drawBottomHudBridgePlates(
            canvas = canvas,
            hero = hero,
            action = action,
            log = log,
        )
        drawBottomHudSeamLockPlates(
            canvas = canvas,
            hero = hero,
            action = action,
            log = log,
        )
        drawBottomHudCommandSpine(
            canvas = canvas,
            hero = hero,
            log = log,
        )

        listOf((hero.right + action.x) / 2f, (action.right + log.x) / 2f).forEach { seamX ->
            canvas.drawRect(tileBounds(seamX - 8f, topRailY - 1f, 16f, 7f), color("2A1A0E", 0.146f))
            canvas.drawRect(tileBounds(seamX - 4f, topRailY + 2f, 8f, 2f), color("D99A2B", 0.102f))
            canvas.drawRect(tileBounds(seamX - 5f, bottomRailY - 1f, 10f, 6f), color("050604", 0.132f))
        }
    }

    private fun drawBottomHudCommandSpine(
        canvas: TileCanvas,
        hero: GameShellBounds,
        log: GameShellBounds,
    ) {
        val spineX = hero.x + 18f
        val spineRight = log.right - 18f
        val spineWidth = (spineRight - spineX).coerceAtLeast(1f)
        val spineY = hero.y + 14f
        val spineHeight = 18f
        canvas.drawRect(tileBounds(spineX, spineY, spineWidth, spineHeight), color("050604", 0.214f))
        canvas.drawRect(tileBounds(spineX + 18f, spineY + spineHeight - 3f, (spineWidth - 36f).coerceAtLeast(1f), 2f), color("D99A2B", 0.108f))
        canvas.drawRect(tileBounds(spineX + 26f, spineY + 4f, (spineWidth - 52f).coerceAtLeast(1f), 1f), color("1CB7C8", 0.052f))
        canvas.drawRect(tileBounds(spineX + 14f, spineY + 7f, 2f, spineHeight - 10f), color("2B3542", 0.142f))
        canvas.drawRect(tileBounds(spineRight - 16f, spineY + 7f, 2f, spineHeight - 10f), color("2B3542", 0.142f))
    }

    private fun drawBottomHudSeamLockPlates(
        canvas: TileCanvas,
        hero: GameShellBounds,
        action: GameShellBounds,
        log: GameShellBounds,
    ) {
        listOf((hero.right + action.x) / 2f, (action.right + log.x) / 2f).forEach { centerX ->
            val lockWidth = 40f
            val lockY = hero.y + 12f
            val lockHeight = (hero.height - 24f).coerceAtLeast(1f)
            val lockX = centerX - lockWidth / 2f
            canvas.drawRect(tileBounds(lockX, lockY, lockWidth, lockHeight), color("050604", 0.318f))
            canvas.drawRect(tileBounds(lockX + 5f, lockY + 8f, lockWidth - 10f, 2f), color("D99A2B", 0.136f))
            canvas.drawRect(tileBounds(lockX + 5f, lockY + lockHeight - 10f, lockWidth - 10f, 2f), color("D99A2B", 0.136f))
            canvas.drawRect(tileBounds(centerX - 1f, lockY + 16f, 2f, lockHeight - 32f), color("2B3542", 0.172f))
            canvas.drawRect(tileBounds(lockX + 9f, lockY + lockHeight * 0.52f, lockWidth - 18f, 1f), color("1CB7C8", 0.072f))
        }
    }

    private fun drawBottomHudBridgePlates(
        canvas: TileCanvas,
        hero: GameShellBounds,
        action: GameShellBounds,
        log: GameShellBounds,
    ) {
        listOf((hero.right + action.x) / 2f, (action.right + log.x) / 2f).forEach { centerX ->
            val bridgeWidth = 36f
            val bridgeY = hero.y + 16f
            val bridgeHeight = (hero.height - 32f).coerceAtLeast(1f)
            val bridgeX = centerX - bridgeWidth / 2f
            canvas.drawRect(tileBounds(bridgeX, bridgeY, bridgeWidth, bridgeHeight), color("0B0A08", 0.171f))
            canvas.drawRect(tileBounds(bridgeX + 5f, bridgeY + 8f, bridgeWidth - 10f, 3f), color("D99A2B", 0.109f))
            canvas.drawRect(tileBounds(bridgeX + 5f, bridgeY + bridgeHeight - 11f, bridgeWidth - 10f, 3f), color("D99A2B", 0.109f))
            canvas.drawRect(tileBounds(bridgeX + 8f, bridgeY + bridgeHeight * 0.50f, bridgeWidth - 16f, 2f), color("1CB7C8", 0.074f))
            canvas.drawRect(tileBounds(bridgeX + 4f, bridgeY + 16f, 2f, bridgeHeight - 32f), color("2B3542", 0.122f))
            canvas.drawRect(tileBounds(bridgeX + bridgeWidth - 6f, bridgeY + 16f, 2f, bridgeHeight - 32f), color("2B3542", 0.122f))
        }
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
        val buttonBounds = DemoNavRailButtonLayout.resolve(layout.navRail, items.size)
        drawNavRailBackbone(canvas, layout.navRail, content, buttonBounds)
        buttonBounds.forEachIndexed { index, bounds ->
            val item = items[index]
            val selected = item.state == TileDemoNavItemState.SELECTED
            drawNavButtonSocket(canvas, bounds, selected)
            if (selected) {
                canvas.drawAsset(chrome.demoShell.navButtonActive, bounds.toTileBounds(), alpha = 0.92f)
            }
            val iconInset = bounds.width * if (selected) 0.055f else 0.06f
            canvas.drawAsset(
                chrome.demoShell.navIcon(item.kind),
                tileBounds(bounds.x + iconInset, bounds.y + iconInset, bounds.width - iconInset * 2f, bounds.height - iconInset * 2f),
                alpha = 0.94f,
            )
        }
    }

    private fun drawNavRailBackbone(
        canvas: TileCanvas,
        rail: GameShellBounds,
        content: GameShellBounds,
        buttonBounds: List<GameShellBounds>,
    ) {
        val centerX = rail.x + rail.width / 2f
        val backboneY = content.y + 10f
        val backboneHeight = (content.height - 20f).coerceAtLeast(1f)
        canvas.drawRect(tileBounds(centerX - 12f, backboneY, 24f, backboneHeight), color("050604", 0.32f))
        canvas.drawRect(
            tileBounds(centerX - 2f, backboneY + 18f, 4f, (backboneHeight - 36f).coerceAtLeast(1f)),
            color("B8873E", 0.18f),
        )
        canvas.drawRect(
            tileBounds(centerX - 0.5f, backboneY + 28f, 1f, (backboneHeight - 56f).coerceAtLeast(1f)),
            color("D99A2B", 0.10f),
        )
        buttonBounds.forEach { bounds ->
            val centerY = bounds.y + bounds.height / 2f
            val shelfWidth = (bounds.width + 10f).coerceAtMost(rail.width - 6f)
            val shelfX = centerX - shelfWidth / 2f
            canvas.drawRect(tileBounds(shelfX, centerY - 1.5f, shelfWidth, 3f), color("050604", 0.145f))
            canvas.drawRect(
                tileBounds((bounds.x + 6f).coerceAtLeast(rail.x + 8f), centerY + 2f, (bounds.width - 12f).coerceAtLeast(8f), 1f),
                color("D99A2B", 0.070f),
            )
        }
    }

    private fun drawNavButtonSocket(
        canvas: TileCanvas,
        bounds: GameShellBounds,
        selected: Boolean,
    ) {
        if (selected) {
            canvas.drawRect(
                tileBounds(bounds.x - 5f, bounds.y + 7f, 4f, bounds.height - 14f),
                color("D99A2B", 0.255f),
            )
            canvas.drawRect(
                tileBounds(bounds.right + 1f, bounds.y + 10f, 2f, bounds.height - 20f),
                color("1CB7C8", 0.118f),
            )
            canvas.drawRect(
                tileBounds(bounds.x - 3f, bounds.y - 3f, bounds.width + 6f, bounds.height + 6f),
                color("1CB7C8", 0.08f),
            )
        }
        canvas.drawRect(
            tileBounds(bounds.x + 2f, bounds.y + 2f, bounds.width - 4f, bounds.height - 4f),
            color("050604", if (selected) 0.62f else 0.42f),
        )
        canvas.drawRect(
            tileBounds(bounds.x + 5f, bounds.y + 5f, bounds.width - 10f, bounds.height - 10f),
            color("1C1711", if (selected) 0.20f else 0.12f),
        )
        canvas.drawRect(
            tileBounds(bounds.x + 5f, bounds.y + 5f, bounds.width - 10f, bounds.height - 10f),
            color("050604", 0.244f),
        )
        canvas.drawRect(
            tileBounds(bounds.x + 9f, bounds.top - 12f, bounds.width - 18f, 2f),
            color("D99A2B", if (selected) 0.188f else 0.106f),
        )
        canvas.drawRect(
            tileBounds(bounds.x + 10f, bounds.y + 9f, bounds.width - 20f, 1f),
            color("1CB7C8", if (selected) 0.082f else 0.044f),
        )
        canvas.drawRect(tileBounds(bounds.x, bounds.top - 2f, bounds.width, 2f), color("D99A2B", if (selected) 0.32f else 0.16f))
        canvas.drawRect(tileBounds(bounds.x + 4f, bounds.y + 2f, bounds.width - 8f, 1f), color("050604", 0.28f))
        canvas.drawRect(tileBounds(bounds.x, bounds.y, 2f, bounds.height), color("B8873E", if (selected) 0.24f else 0.13f))
        canvas.drawRect(tileBounds(bounds.right - 2f, bounds.y, 2f, bounds.height), color("050604", 0.34f))
        val rivetAlpha = if (selected) 0.22f else 0.14f
        listOf(
            bounds.x + 4f to bounds.y + 4f,
            bounds.right - 8f to bounds.y + 4f,
            bounds.x + 4f to bounds.top - 8f,
            bounds.right - 8f to bounds.top - 8f,
        ).forEach { (x, y) ->
            canvas.drawRect(tileBounds(x, y, 4f, 4f), color("D99A2B", rivetAlpha))
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
            alpha = 0.78f,
        )
        canvas.drawRect(tileBounds(bounds.x + 10f, bounds.y + 10f, bounds.width - 20f, bounds.height - 20f), color("0A0806", 0.94f))
        drawRightPanelDepth(canvas, bounds)
        drawRightUtilityChassis(canvas, layout)
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
        drawBackpackSection(canvas, chrome, layout.backpack, demo.rightBackpackTitle, layout.backpackSlots, demo.backpackSlots)
        drawBackpackOperationBridge(canvas, layout)
        drawBackpackPager(canvas, layout.backpackPager, demo.backpackPageLabel)
        drawOperationHintsSection(canvas, chrome, layout.operationHints, demo.rightOperationHintsTitle, demo.operationRows)
    }

    private fun drawEquipmentSection(
        canvas: TileCanvas,
        chrome: TileChromeAssets,
        bounds: GameShellBounds,
        title: String,
        grid: DemoSlotGridLayout,
        slots: List<TileDemoSlotModel>,
    ) {
        drawRightSectionSurface(canvas, bounds)
        drawEquipmentRigBackdrop(canvas, bounds, grid)
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
        drawRightSectionSurface(canvas, bounds)
        drawInscriptionLedgerBackdrop(canvas, bounds, grid)
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
            drawInscriptionRowPlate(canvas, rowBounds, slot.state == TileDemoSlotState.EMPTY)
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

    private fun drawBackpackSection(
        canvas: TileCanvas,
        chrome: TileChromeAssets,
        bounds: GameShellBounds,
        title: String,
        grid: DemoSlotGridLayout,
        slots: List<TileDemoSlotModel>,
    ) {
        drawRightSectionSurface(canvas, bounds)
        drawBackpackTrayBackdrop(canvas, bounds, grid)
        drawSectionTitle(canvas, chrome, bounds, title)
        grid.slotBounds.forEachIndexed { index, slotBounds ->
            val slot = slots.getOrNull(index) ?: return@forEachIndexed
            drawSlotSurface(canvas, chrome, slotBounds, slot)
            drawSlotBadge(canvas, slotBounds, slot)
        }
    }

    private fun drawOperationHintsSection(
        canvas: TileCanvas,
        chrome: TileChromeAssets,
        bounds: GameShellBounds,
        title: String,
        rows: List<TileTextRow>,
    ) {
        drawRightSectionSurface(canvas, bounds)
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
            alpha = 0.64f,
        )
        val content =
            GameShellBounds(
                x = plateBounds.x + 8f,
                y = plateBounds.y + 6f,
                width = plateBounds.width - 16f,
                height = plateBounds.height - 12f,
        )
        val hintRows = rows.filter { row -> row.text.isNotBlank() }
        if (hintRows.none { row -> row.hasVisualIcons() || row.frame != null }) {
            drawCompactOperationCommandMatrix(canvas, content, hintRows)
            return
        }
        val rowCount = ((content.height - 4f) / OPERATION_LINE_HEIGHT).toInt().coerceAtLeast(1)
        val displayRows = compactVisualOperationRows(operationHintRows(hintRows), rowCount)
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

    private fun drawCompactOperationCommandMatrix(
        canvas: TileCanvas,
        content: GameShellBounds,
        rows: List<TileTextRow>,
    ) {
        if (rows.isEmpty()) {
            return
        }
        val columnCount = if (content.width >= 220f) 2 else 1
        val columnGap = 12f
        val rowHeight = OPERATION_LINE_HEIGHT
        val columnWidth = ((content.width - columnGap * (columnCount - 1)) / columnCount).coerceAtLeast(1f)
        val visibleRows = ((content.height - 2f) / rowHeight).toInt().coerceAtLeast(1)
        val visibleCommands = rows.take(visibleRows * columnCount)
        val occupiedRows = ((visibleCommands.size + columnCount - 1) / columnCount).coerceAtLeast(1)
        drawOperationCommandDock(canvas, content, columnCount, columnWidth, columnGap, occupiedRows, rowHeight)
        visibleCommands.forEachIndexed { index, row ->
            val command = operationCommandParts(row.text)
            val column = index % columnCount
            val rowIndex = index / columnCount
            val baselineY = content.top - 5f - rowIndex * rowHeight
            val commandX = content.x + column * (columnWidth + columnGap)
            val keyWidth =
                (TileTextMetrics.approximateTextWidth(command.keyText, TileTextStyle.CAPTION) + 12f)
                    .coerceIn(30f, minOf(92f, columnWidth * 0.45f).coerceAtLeast(30f))
            val keyBounds = GameShellBounds(commandX, baselineY - 10f, keyWidth, 13f)
            canvas.drawRect(keyBounds.toTileBounds(), color("050604", 0.46f))
            canvas.drawRect(tileBounds(keyBounds.x, keyBounds.y, 2f, keyBounds.height), color("B8873E", 0.26f))
            canvas.drawRect(tileBounds(keyBounds.x + 3f, keyBounds.top - 1f, keyBounds.width - 6f, 1f), color("D99A2B", 0.12f))
            drawOperationKeyplateRivets(canvas, keyBounds)
            canvas.drawText(
                TileTextStyle.CAPTION,
                TileTextMetrics.truncateTextToWidth(command.keyText, keyBounds.width - 12f, TileTextStyle.CAPTION),
                tilePosition(keyBounds.x + 6f, baselineY),
                color("D99A2B", 0.70f),
            )
            if (command.labelText.isNotBlank()) {
                val labelX = keyBounds.right + 6f
                drawMutedSmallText(
                    canvas = canvas,
                    text = command.labelText,
                    x = labelX,
                    baselineY = baselineY,
                    maxWidth = (commandX + columnWidth - labelX).coerceAtLeast(1f),
                    style = TileTextStyle.CAPTION,
                )
            }
        }
    }

    private fun drawOperationCommandDock(
        canvas: TileCanvas,
        content: GameShellBounds,
        columnCount: Int,
        columnWidth: Float,
        columnGap: Float,
        occupiedRows: Int,
        rowHeight: Float,
    ) {
        val dockTop = content.top - 1f
        val firstKeyY = content.top - 15f
        val dockY = (firstKeyY - (occupiedRows - 1) * rowHeight - 4f).coerceAtLeast(content.y + 1f)
        val dockHeight = (dockTop - dockY).coerceAtLeast(1f)
        val dockBounds =
            GameShellBounds(
                x = content.x + 1f,
                y = dockY,
                width = (content.width - 2f).coerceAtLeast(1f),
                height = dockHeight,
            )
        canvas.drawRect(dockBounds.toTileBounds(), color("050604", 0.156f))
        canvas.drawRect(tileBounds(dockBounds.x + 4f, dockBounds.top - 2f, dockBounds.width - 8f, 2f), color("B8873E", 0.106f))
        canvas.drawRect(tileBounds(dockBounds.x + 8f, dockBounds.y + 3f, dockBounds.width - 16f, 1f), color("1CB7C8", 0.052f))
        if (columnCount > 1) {
            val railX = content.x + columnWidth + columnGap / 2f - 1f
            canvas.drawRect(
                tileBounds(railX, dockBounds.y + 4f, 2f, (dockBounds.height - 8f).coerceAtLeast(1f)),
                color("B8873E", 0.096f),
            )
            canvas.drawRect(
                tileBounds(railX + 3f, dockBounds.y + 7f, 1f, (dockBounds.height - 14f).coerceAtLeast(1f)),
                color("050604", 0.34f),
            )
        }
    }

    private fun drawOperationKeyplateRivets(
        canvas: TileCanvas,
        keyBounds: GameShellBounds,
    ) {
        val rivetY = keyBounds.y + (keyBounds.height - 4f) / 2f
        canvas.drawRect(tileBounds(keyBounds.x + 3f, rivetY, 4f, 4f), color("C49B61", 0.124f))
        canvas.drawRect(tileBounds(keyBounds.right - 7f, rivetY, 4f, 4f), color("C49B61", 0.124f))
    }

    private fun operationCommandParts(text: String): OperationCommandParts {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return OperationCommandParts("", "")
        }
        val whitespaceIndex = trimmed.indexOfFirst(Char::isWhitespace)
        val colonIndex = trimmed.indexOfFirst { char -> char == ':' || char == '：' }
        val splitIndex =
            when {
                colonIndex > 0 && (whitespaceIndex < 0 || colonIndex < whitespaceIndex) -> colonIndex
                whitespaceIndex > 0 -> whitespaceIndex
                else -> -1
            }
        if (splitIndex < 0) {
            return OperationCommandParts(trimmed, "")
        }
        val keyText = trimmed.substring(0, splitIndex).trim().trimEnd(':', '：')
        val labelText = trimmed.substring(splitIndex + 1).trim().trimStart(':', '：').trim()
        return OperationCommandParts(keyText, labelText)
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

    private fun drawRightSectionSurface(
        canvas: TileCanvas,
        bounds: GameShellBounds,
    ) {
        val body =
            GameShellBounds(
                x = bounds.x + 8f,
                y = bounds.y + 6f,
                width = (bounds.width - 16f).coerceAtLeast(1f),
                height = (bounds.height - 10f).coerceAtLeast(1f),
        )
        canvas.drawRect(body.toTileBounds(), color("050604", 0.22f))
        val materialBottom = body.y + 9f
        val materialTop = body.top - SECTION_TITLE_HEIGHT - 7f
        val materialHeight = (materialTop - materialBottom).coerceAtLeast(1f)
        val materialWell =
            GameShellBounds(
                x = body.x + 4f,
                y = body.y + 8f,
                width = (body.width - 8f).coerceAtLeast(1f),
                height = (body.height - SECTION_TITLE_HEIGHT - 14f).coerceAtLeast(1f),
            )
        canvas.drawRect(materialWell.toTileBounds(), color("0A0806", 0.315f))
        canvas.drawRect(tileBounds(materialWell.x + 6f, materialWell.top - 2f, materialWell.width - 12f, 2f), color("D99A2B", 0.102f))
        canvas.drawRect(tileBounds(materialWell.x + 8f, materialWell.y + 3f, materialWell.width - 16f, 1f), color("1CB7C8", 0.054f))
        listOf(0.23f, 0.49f, 0.72f).forEachIndexed { index, ratio ->
            val inset = if (index % 2 == 0) 14f else 20f
            canvas.drawRect(
                tileBounds(
                    x = body.x + inset,
                    y = materialBottom + materialHeight * ratio,
                    width = (body.width - inset * 2f).coerceAtLeast(1f),
                    height = 2.5f,
                ),
                color("13100D", 0.052f),
            )
        }
        val striationHeight = (materialHeight - 12f).coerceAtLeast(1f)
        listOf(0.22f, 0.78f).forEach { ratio ->
            canvas.drawRect(
                tileBounds(
                    x = body.x + body.width * ratio,
                    y = materialBottom + 6f,
                    width = 2.5f,
                    height = striationHeight,
                ),
                color("261A12", 0.046f),
            )
        }
        listOf(0.16f to 0.18f, 0.82f to 0.29f, 0.31f to 0.63f, 0.68f to 0.80f).forEachIndexed { index, (xRatio, yRatio) ->
            val size = if (index % 2 == 0) 4f else 3.5f
            canvas.drawRect(
                tileBounds(
                    x = body.x + 10f + (body.width - 22f).coerceAtLeast(1f) * xRatio,
                    y = materialBottom + (materialHeight - 8f).coerceAtLeast(1f) * yRatio,
                    width = size,
                    height = size,
                ),
                color("6E5630", 0.038f),
            )
        }
        canvas.drawRect(tileBounds(body.x, body.top - SECTION_TITLE_HEIGHT - 1f, body.width, 1f), color("D99A2B", 0.18f))
        canvas.drawRect(tileBounds(body.x, body.y, body.width, 1f), color("050604", 0.34f))
        canvas.drawRect(tileBounds(body.x, body.y, 1.5f, body.height), color("B8873E", 0.14f))
        canvas.drawRect(tileBounds(body.right - 1.5f, body.y, 1.5f, body.height), color("050604", 0.28f))
    }

    private fun drawInscriptionLedgerBackdrop(
        canvas: TileCanvas,
        bounds: GameShellBounds,
        grid: DemoSlotGridLayout,
    ) {
        if (grid.slotBounds.isEmpty()) {
            return
        }
        val columnGap = 10f
        val rowWidth = ((bounds.width - 24f - columnGap) / 2f).coerceAtLeast(grid.slotSide + 48f)
        val leftX = bounds.x + 12f
        val rightX = bounds.right - 12f - rowWidth
        val slotTop = grid.slotBounds.maxOf { slot -> slot.top }
        val slotBottom = grid.slotBounds.minOf { slot -> slot.y }
        val spineY = (slotBottom - 4f).coerceAtLeast(bounds.y + 8f)
        val spineHeight =
            (slotTop - spineY + 8f)
                .coerceAtMost(bounds.top - SECTION_TITLE_HEIGHT - spineY - 4f)
                .coerceAtLeast(1f)
        val rowCentersY = grid.slotBounds.map { slot -> slot.y + slot.height / 2f }.distinct().sorted()
        listOf(leftX + 2f, rightX + 2f).forEach { spineX ->
            canvas.drawRect(tileBounds(spineX, spineY, 3f, spineHeight), color("0B0A08", 0.112f))
            canvas.drawRect(tileBounds(spineX + 1f, spineY + 8f, 1f, spineHeight - 16f), color("D99A2B", 0.052f))
            rowCentersY.forEach { centerY ->
                canvas.drawRect(tileBounds(spineX - 1f, centerY - 2.5f, 5f, 5f), color("D99A2B", 0.128f))
                canvas.drawRect(tileBounds(spineX + 0.5f, centerY - 1f, 2f, 2f), color("050604", 0.22f))
            }
        }
    }

    private fun drawBackpackTrayBackdrop(
        canvas: TileCanvas,
        bounds: GameShellBounds,
        grid: DemoSlotGridLayout,
    ) {
        if (grid.slotBounds.isEmpty()) {
            return
        }
        val slotTop = grid.slotBounds.maxOf { slot -> slot.top }
        val slotBottom = grid.slotBounds.minOf { slot -> slot.y }
        val trayX = bounds.x + 38f
        val trayWidth = (bounds.width - 76f).coerceAtLeast(1f)
        val trayY = (slotBottom - 8f).coerceAtLeast(bounds.y + 8f)
        val trayHeight =
            (slotTop - trayY + 10f)
                .coerceAtMost(bounds.top - SECTION_TITLE_HEIGHT - trayY - 4f)
                .coerceAtLeast(1f)
        canvas.drawRect(tileBounds(trayX, trayY, trayWidth, trayHeight), color("050604", 0.20f))
        canvas.drawRect(tileBounds(trayX + 8f, trayY + trayHeight - 2f, trayWidth - 16f, 2f), color("D99A2B", 0.074f))
        val rowCentersY = grid.slotBounds.map { slot -> slot.y + slot.height / 2f }.distinct().sorted()
        rowCentersY.forEach { centerY ->
            canvas.drawRect(tileBounds(trayX, centerY - 1.5f, trayWidth, 3f), color("0B0A08", 0.092f))
            canvas.drawRect(tileBounds(trayX + 16f, centerY + 2f, trayWidth - 32f, 1f), color("B8873E", 0.046f))
        }
        val strapHeight = (slotTop - slotBottom + 16f).coerceAtLeast(grid.slotSide * 1.60f)
        val strapY = (slotBottom - 8f).coerceAtLeast(bounds.y + 8f)
        listOf(trayX + 22f, trayX + trayWidth - 25f).forEach { strapX ->
            canvas.drawRect(tileBounds(strapX, strapY, 3f, strapHeight), color("2A1A0E", 0.118f))
            canvas.drawRect(tileBounds(strapX + 1f, strapY + 8f, 1f, strapHeight - 16f), color("D99A2B", 0.050f))
        }
    }

    private fun drawBackpackOperationBridge(
        canvas: TileCanvas,
        layout: DemoRightPanelLayout,
    ) {
        val pager = layout.backpackPager
        val bridgeBottom = layout.operationHints.top - 2f
        val bridgeTop = pager.top + 2f
        val bridgeHeight = (bridgeTop - bridgeBottom).coerceAtLeast(1f)
        val cradle =
            GameShellBounds(
                x = pager.x - 4f,
                y = pager.y - 4f,
                width = pager.width + 8f,
                height = pager.height + 8f,
            )
        canvas.drawRect(cradle.toTileBounds(), color("050604", 0.205f))
        canvas.drawRect(tileBounds(cradle.x + 10f, cradle.top - 2f, cradle.width - 20f, 2f), color("D99A2B", 0.134f))
        canvas.drawRect(tileBounds(cradle.x + 14f, cradle.y + 3f, cradle.width - 28f, 1f), color("1CB7C8", 0.052f))
        canvas.drawRect(tileBounds(pager.x + 8f, layout.operationHints.top - 3f, pager.width - 16f, 3f), color("0B0A08", 0.126f))
        listOf(pager.x + 18f, pager.right - 22f).forEach { postX ->
            canvas.drawRect(tileBounds(postX, bridgeBottom, 4f, bridgeHeight), color("2A1A0E", 0.142f))
            canvas.drawRect(tileBounds(postX + 1f, bridgeBottom + 6f, 1.5f, (bridgeHeight - 12f).coerceAtLeast(1f)), color("D99A2B", 0.058f))
            canvas.drawRect(tileBounds(postX - 1f, pager.y - 2f, 6f, 4f), color("C49B61", 0.118f))
            canvas.drawRect(tileBounds(postX - 1f, layout.operationHints.top - 4f, 6f, 4f), color("C49B61", 0.118f))
        }
    }

    private fun drawEquipmentRigBackdrop(
        canvas: TileCanvas,
        bounds: GameShellBounds,
        grid: DemoSlotGridLayout,
    ) {
        if (grid.slotBounds.isEmpty()) {
            return
        }
        val slotTop = grid.slotBounds.maxOf { slot -> slot.top }
        val slotBottom = grid.slotBounds.minOf { slot -> slot.y }
        val rigY = (slotBottom - 12f).coerceAtLeast(bounds.y + 8f)
        val rigHeight =
            (slotTop - slotBottom + 24f)
                .coerceAtMost(bounds.top - SECTION_TITLE_HEIGHT - rigY - 4f)
                .coerceAtLeast(1f)
        val rig =
            GameShellBounds(
                x = bounds.x + 18f,
                y = rigY,
                width = (bounds.width - 36f).coerceAtLeast(1f),
                height = rigHeight,
        )
        canvas.drawRect(rig.toTileBounds(), color("050604", 0.40f))
        canvas.drawRect(tileBounds(rig.x + 10f, rig.top - 2f, rig.width - 20f, 2f), color("D99A2B", 0.22f))
        canvas.drawRect(tileBounds(rig.x + 10f, rig.y + 2f, rig.width - 20f, 1.5f), color("1CB7C8", 0.09f))
        canvas.drawRect(tileBounds(rig.x + rig.width / 2f - 1f, rig.y + 16f, 2f, rig.height - 32f), color("B8873E", 0.18f))
        canvas.drawRect(tileBounds(rig.x + 22f, rig.y + rig.height * 0.52f, rig.width - 44f, 2f), color("D99A2B", 0.12f))
        canvas.drawRect(tileBounds(rig.x + 36f, rig.y + rig.height * 0.26f, rig.width - 72f, 1.5f), color("050604", 0.34f))
        grid.slotBounds.forEach { slot ->
            val well =
                GameShellBounds(
                    x = slot.x - 4f,
                    y = slot.y - 4f,
                    width = slot.width + 8f,
                    height = slot.height + 8f,
                )
            canvas.drawRect(well.toTileBounds(), color("0C0906", 0.34f))
            canvas.drawRect(tileBounds(well.x + 5f, well.top - 2f, well.width - 10f, 2f), color("D99A2B", 0.128f))
            canvas.drawRect(tileBounds(well.x + 6f, well.y + 3f, well.width - 12f, 1f), color("050604", 0.30f))
        }
        val slotCentersX = grid.slotBounds.map { slot -> slot.x + slot.width / 2f }.distinct().sorted()
        val slotCentersY = grid.slotBounds.map { slot -> slot.y + slot.height / 2f }.distinct().sorted()
        if (slotCentersX.size >= 2 && slotCentersY.size >= 3) {
            val leftRailX = slotCentersX.first()
            val rightRailX = slotCentersX.last()
            val railY = (slotBottom - 8f).coerceAtLeast(bounds.y + 12f)
            val railHeight =
                (slotTop - railY + 8f)
                    .coerceAtMost(bounds.top - SECTION_TITLE_HEIGHT - railY - 8f)
                    .coerceAtLeast(1f)
            val leftSlotEdge = grid.slotBounds.minOf { slot -> slot.x }
            val rightSlotEdge = grid.slotBounds.maxOf { slot -> slot.right }
            val scaffoldCenterX = (leftSlotEdge + rightSlotEdge) / 2f
            val shoulderY = (slotCentersY[slotCentersY.lastIndex - 1] - 10f).coerceAtMost(rig.top - 28f)
            val shoulderWidth =
                (rightSlotEdge - leftSlotEdge + 32f)
                    .coerceAtMost(bounds.width * 0.60f)
            val shoulderX = scaffoldCenterX - shoulderWidth / 2f
            grid.slotBounds.take(8).chunked(2).forEachIndexed { index, rowSlots ->
                val rowLeft = rowSlots.minOf { slot -> slot.x } - 8f
                val rowRight = rowSlots.maxOf { slot -> slot.right } + 8f
                val rowBottom = rowSlots.minOf { slot -> slot.y } - 4f
                val bayHeight = grid.slotSide + 8f
                canvas.drawRect(
                    tileBounds(
                        x = rowLeft.coerceAtLeast(bounds.x + 10f),
                        y = rowBottom.coerceAtLeast(bounds.y + 8f),
                        width = (rowRight - rowLeft).coerceAtMost(bounds.width - 20f),
                        height = bayHeight,
                    ),
                    color("0B0907", 0.168f),
                )
                canvas.drawRect(
                    tileBounds(
                        x = rowLeft + 10f,
                        y = rowBottom + bayHeight - 2f,
                        width = (rowRight - rowLeft - 20f).coerceAtLeast(1f),
                        height = 1.5f,
                    ),
                    color("D99A2B", if (index == 0) 0.072f else 0.052f),
                )
                canvas.drawRect(
                    tileBounds(
                        x = rowLeft + 14f,
                        y = rowBottom + 3f,
                        width = (rowRight - rowLeft - 28f).coerceAtLeast(1f),
                        height = 1f,
                    ),
                    color("050604", 0.26f),
                )
            }
            canvas.drawRect(
                tileBounds(shoulderX, shoulderY, shoulderWidth, 22f),
                color("090604", 0.218f),
            )
            canvas.drawRect(
                tileBounds(shoulderX + 12f, shoulderY + 5f, shoulderWidth - 24f, 1.5f),
                color("D99A2B", 0.068f),
            )
            val torsoHeight = (railHeight * 0.44f).coerceAtLeast(72f)
            val torsoY = (railY + railHeight * 0.24f).coerceAtMost(rig.top - torsoHeight - 18f)
            canvas.drawRect(
                tileBounds(scaffoldCenterX - 27f, torsoY, 54f, torsoHeight),
                color("080604", 0.236f),
            )
            canvas.drawRect(
                tileBounds(scaffoldCenterX - 1f, torsoY + 7f, 2f, (torsoHeight - 14f).coerceAtLeast(1f)),
                color("D99A2B", 0.062f),
            )
            val sidePlateY = railY + railHeight * 0.18f
            val sidePlateHeight = (railHeight * 0.52f).coerceAtLeast(74f)
            listOf(leftSlotEdge - 30f, rightSlotEdge + 6f).forEach { plateX ->
                canvas.drawRect(tileBounds(plateX, sidePlateY, 24f, sidePlateHeight), color("11100C", 0.204f))
                canvas.drawRect(tileBounds(plateX + 10f, sidePlateY + 10f, 1.5f, sidePlateHeight - 20f), color("B8873E", 0.064f))
            }
            listOf(leftRailX, rightRailX).forEach { railCenterX ->
                canvas.drawRect(tileBounds(railCenterX - 1.5f, railY, 3f, railHeight), color("0B0A08", 0.144f))
                canvas.drawRect(tileBounds(railCenterX - 0.5f, railY + 10f, 1f, railHeight - 20f), color("D99A2B", 0.064f))
            }
            slotCentersY.forEach { centerY ->
                canvas.drawRect(tileBounds(leftSlotEdge + 8f, centerY - 1.5f, rightSlotEdge - leftSlotEdge - 16f, 3f), color("050604", 0.086f))
                canvas.drawRect(tileBounds(leftSlotEdge + 22f, centerY + 2f, rightSlotEdge - leftSlotEdge - 44f, 1f), color("B8873E", 0.044f))
            }
            slotCentersY.forEach { centerY ->
                listOf(leftRailX, rightRailX).forEach { railCenterX ->
                    canvas.drawRect(tileBounds(railCenterX - 2.5f, centerY - 2.5f, 5f, 5f), color("D99A2B", 0.132f))
                    canvas.drawRect(tileBounds(railCenterX - 1f, centerY - 1f, 2f, 2f), color("050604", 0.20f))
                }
            }
        }
    }

    private fun drawRightUtilityChassis(
        canvas: TileCanvas,
        layout: DemoRightPanelLayout,
    ) {
        val top = layout.inscriptions.top
        val bottom = layout.operationHints.y
        val height = (top - bottom).coerceAtLeast(1f)
        val bounds =
            GameShellBounds(
                x = layout.inscriptions.x + 12f,
                y = bottom,
                width = (layout.inscriptions.width - 24f).coerceAtLeast(1f),
                height = height,
            )
        canvas.drawRect(tileBounds(bounds.x + 2f, bounds.y + 3f, bounds.width - 4f, bounds.height - 6f), color("050604", 0.124f))
        listOf(bounds.x + 18f, bounds.right - 21f).forEach { railX ->
            canvas.drawRect(tileBounds(railX, bounds.y + 8f, 3f, bounds.height - 16f), color("2A1A0E", 0.166f))
            canvas.drawRect(tileBounds(railX + 1f, bounds.y + 18f, 1f, bounds.height - 36f), color("D99A2B", 0.054f))
        }
        listOf(layout.inscriptions.y, layout.backpack.top + 2f, layout.operationHints.top - 4f).forEach { railY ->
            if (railY > bounds.y + 8f && railY < bounds.top - 8f) {
                canvas.drawRect(tileBounds(bounds.x + 14f, railY, bounds.width - 28f, 3f), color("0B0A08", 0.104f))
                canvas.drawRect(tileBounds(bounds.x + 28f, railY + 2f, bounds.width - 56f, 1f), color("B8873E", 0.046f))
            }
        }
    }

    private fun drawInscriptionRowPlate(
        canvas: TileCanvas,
        bounds: GameShellBounds,
        empty: Boolean,
    ) {
        canvas.drawRect(bounds.toTileBounds(), color("05070A", if (empty) 0.58f else 0.76f))
        canvas.drawRect(tileBounds(bounds.x, bounds.y, 3f, bounds.height), color("B8873E", 0.50f))
        canvas.drawRect(tileBounds(bounds.x + 3f, bounds.top - 1f, bounds.width - 3f, 1f), color("6E5630", 0.28f))
        canvas.drawRect(tileBounds(bounds.x + 3f, bounds.y, bounds.width - 3f, 1f), color("050604", 0.36f))
        canvas.drawRect(tileBounds(bounds.right - 1f, bounds.y + 3f, 1f, bounds.height - 6f), color("050604", 0.30f))
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
        val slotAlpha =
            when {
                slot.selected -> 1f
                slot.state == TileDemoSlotState.EMPTY -> 0.94f
                else -> 0.98f
            }
        canvas.drawAsset(slotAsset, slotBounds.toTileBounds(), alpha = slotAlpha)
        if (slot.state == TileDemoSlotState.EMPTY) {
            drawEmptySlotInterior(canvas, slotBounds, slot.visualOnly)
        } else {
            drawFilledSlotInterior(canvas, slotBounds, slot.selected)
        }
        slot.qualityTierId?.let { qualityTierId ->
            drawQualityMarker(canvas, slotBounds, qualityTierId)
        }
        slot.icon?.let { icon ->
            val iconInset = slotBounds.width * 0.055f
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

    private fun drawFilledSlotInterior(
        canvas: TileCanvas,
        slotBounds: GameShellBounds,
        selected: Boolean,
    ) {
        val inset = 5f
        val inner =
            GameShellBounds(
                x = slotBounds.x + inset,
                y = slotBounds.y + inset,
                width = (slotBounds.width - inset * 2f).coerceAtLeast(1f),
                height = (slotBounds.height - inset * 2f).coerceAtLeast(1f),
            )
        val accentAlpha = if (selected) 0.32f else 0.20f
        canvas.drawRect(inner.toTileBounds(), color("0A0806", 0.24f))
        canvas.drawRect(tileBounds(inner.x + 4f, inner.y + 4f, inner.width - 8f, inner.height - 8f), color("1C1711", 0.16f))
        canvas.drawRect(tileBounds(inner.x, inner.top - 2f, inner.width, 2f), color("D99A2B", accentAlpha))
        canvas.drawRect(tileBounds(inner.x + 2f, inner.y + 2f, inner.width - 4f, 2f), color("050604", 0.23f))
        canvas.drawRect(tileBounds(inner.x, inner.y, 2f, inner.height), color("5D4324", 0.21f))
        canvas.drawRect(tileBounds(inner.right - 2f, inner.y, 2f, inner.height), color("050604", 0.24f))
    }

    private fun drawEmptySlotInterior(
        canvas: TileCanvas,
        slotBounds: GameShellBounds,
        visualOnly: Boolean,
    ) {
        if (visualOnly) {
            val authoredPlate =
                GameShellBounds(
                    x = slotBounds.x + 3f,
                    y = slotBounds.y + 3f,
                    width = (slotBounds.width - 6f).coerceAtLeast(1f),
                    height = (slotBounds.height - 6f).coerceAtLeast(1f),
                )
            canvas.drawRect(authoredPlate.toTileBounds(), color("070605", 0.56f))
            canvas.drawRect(tileBounds(authoredPlate.x + 4f, authoredPlate.top - 2f, authoredPlate.width - 8f, 2f), color("D99A2B", 0.15f))
            canvas.drawRect(tileBounds(authoredPlate.x + 5f, authoredPlate.y + 3f, authoredPlate.width - 10f, 1f), color("050604", 0.28f))
        }
        val inset = 6f
        val inner =
            GameShellBounds(
                x = slotBounds.x + inset,
                y = slotBounds.y + inset,
                width = (slotBounds.width - inset * 2f).coerceAtLeast(1f),
                height = (slotBounds.height - inset * 2f).coerceAtLeast(1f),
            )
        canvas.drawRect(inner.toTileBounds(), color("050604", if (visualOnly) 0.34f else 0.24f))
        canvas.drawRect(tileBounds(inner.x, inner.top - 1f, inner.width, 1f), color("B8873E", if (visualOnly) 0.16f else 0.11f))
        canvas.drawRect(tileBounds(inner.x, inner.y, 1f, inner.height), color("4E3B22", if (visualOnly) 0.18f else 0.12f))
        canvas.drawRect(tileBounds(inner.right - 1f, inner.y, 1f, inner.height), color("050604", if (visualOnly) 0.22f else 0.14f))
        canvas.drawRect(tileBounds(inner.x + 2f, inner.y + 2f, inner.width - 4f, 1f), color("050604", if (visualOnly) 0.18f else 0.10f))
        val socketInset = 4f
        val socket =
            GameShellBounds(
                x = inner.x + socketInset,
                y = inner.y + socketInset,
                width = (inner.width - socketInset * 2f).coerceAtLeast(1f),
                height = (inner.height - socketInset * 2f).coerceAtLeast(1f),
            )
        canvas.drawRect(socket.toTileBounds(), color("020303", if (visualOnly) 0.43f else 0.31f))
        canvas.drawRect(tileBounds(socket.x + 2f, socket.top - 2f, socket.width - 4f, 2f), color("050604", if (visualOnly) 0.36f else 0.25f))
        canvas.drawRect(tileBounds(socket.x + 3f, socket.y + 2f, socket.width - 6f, 1.5f), color("8A6E3E", if (visualOnly) 0.13f else 0.08f))
        canvas.drawRect(tileBounds(socket.x + 2f, socket.top - 6f, 5f, 2f), color("D99A2B", if (visualOnly) 0.18f else 0.11f))
        canvas.drawRect(tileBounds(socket.right - 7f, socket.top - 6f, 5f, 2f), color("D99A2B", if (visualOnly) 0.14f else 0.09f))
        canvas.drawRect(tileBounds(socket.x + 2f, socket.y + 4f, 2f, 5f), color("050604", if (visualOnly) 0.30f else 0.18f))
        canvas.drawRect(tileBounds(socket.right - 4f, socket.y + 4f, 2f, 5f), color("050604", if (visualOnly) 0.30f else 0.18f))
        if (visualOnly) {
            val socketGlyph =
                GameShellBounds(
                    x = inner.x + inner.width * 0.22f,
                    y = inner.y + inner.height * 0.19f,
                    width = inner.width * 0.56f,
                    height = inner.height * 0.58f,
                )
            canvas.drawRect(socketGlyph.toTileBounds(), color("090806", 0.20f))
            drawRectOutline(
                canvas = canvas,
                bounds = socketGlyph,
                stroke = 1f,
                color = color("4F412B", 0.34f),
            )
            canvas.drawRect(tileBounds(socketGlyph.x + 4f, socketGlyph.y + 4f, socketGlyph.width - 8f, 1f), color("8A6E3E", 0.17f))
            canvas.drawRect(tileBounds(socketGlyph.x + 4f, socketGlyph.top - 6f, socketGlyph.width - 8f, 2f), color("050604", 0.30f))
            canvas.drawRect(tileBounds(socketGlyph.x + 4f, socketGlyph.y + socketGlyph.height * 0.34f, 2f, socketGlyph.height * 0.32f), color("7C6A4A", 0.22f))
            canvas.drawRect(tileBounds(socketGlyph.right - 6f, socketGlyph.y + socketGlyph.height * 0.34f, 2f, socketGlyph.height * 0.32f), color("7C6A4A", 0.19f))
            canvas.drawRect(tileBounds(socketGlyph.x + 5f, socketGlyph.top - 9f, 5f, 2f), color("D99A2B", 0.16f))
            canvas.drawRect(tileBounds(socketGlyph.right - 10f, socketGlyph.top - 9f, 5f, 2f), color("050604", 0.24f))
            return
        }
        if (slotBounds.width >= 48f) {
            val glyphScale = 0.46f
            val backingAlpha = 0.18f
            val outlineAlpha = 0.44f
            val metalAlpha = 0.32f
            val shadowAlpha = 0.24f
            val glyphWidth = (inner.width * glyphScale).coerceAtLeast(10f)
            val glyphHeight = (inner.height * glyphScale).coerceAtLeast(10f)
            val glyphX = inner.x + (inner.width - glyphWidth) / 2f
            val glyphY = inner.y + (inner.height - glyphHeight) / 2f
            val motif = ((slotBounds.x / 8f).toInt() + (slotBounds.y / 8f).toInt()) % 4
            canvas.drawRect(tileBounds(glyphX - 2f, glyphY - 2f, glyphWidth + 4f, glyphHeight + 4f), color("0A0806", backingAlpha))
            canvas.drawRect(tileBounds(glyphX + 2f, glyphY + 2f, glyphWidth - 4f, glyphHeight - 4f), color("1B1710", 0.12f))
            drawRectOutline(
                canvas = canvas,
                bounds = GameShellBounds(glyphX, glyphY, glyphWidth, glyphHeight),
                stroke = 1f,
                color = color("7C6A4A", outlineAlpha),
            )
            canvas.drawRect(tileBounds(glyphX + 2f, glyphY + glyphHeight - 3f, glyphWidth - 4f, 1f), color("D99A2B", 0.18f))
            when (motif) {
                0 -> {
                    canvas.drawRect(tileBounds(glyphX + glyphWidth * 0.46f, glyphY + 4f, 3f, glyphHeight - 8f), color("A88A52", metalAlpha))
                    canvas.drawRect(tileBounds(glyphX + glyphWidth * 0.26f, glyphY + glyphHeight - 10f, glyphWidth * 0.50f, 3f), color("D99A2B", metalAlpha * 0.56f))
                    canvas.drawRect(tileBounds(glyphX + glyphWidth * 0.28f, glyphY + glyphHeight - 8f, glyphWidth * 0.44f, 2f), color("050604", shadowAlpha))
                }
                1 -> {
                    canvas.drawRect(tileBounds(glyphX + 5f, glyphY + glyphHeight * 0.46f, glyphWidth - 10f, 3f), color("A88A52", metalAlpha * 0.9f))
                    canvas.drawRect(tileBounds(glyphX + glyphWidth * 0.44f, glyphY + 5f, 3f, glyphHeight - 10f), color("050604", shadowAlpha * 0.75f))
                    canvas.drawRect(tileBounds(glyphX + glyphWidth * 0.25f, glyphY + glyphHeight * 0.30f, glyphWidth * 0.50f, 3f), color("D99A2B", metalAlpha * 0.42f))
                }
                2 -> {
                    canvas.drawRect(tileBounds(glyphX + 5f, glyphY + 5f, glyphWidth - 10f, 3f), color("A88A52", metalAlpha * 0.84f))
                    canvas.drawRect(tileBounds(glyphX + 6f, glyphY + glyphHeight - 9f, glyphWidth - 12f, 4f), color("050604", shadowAlpha * 0.93f))
                    canvas.drawRect(tileBounds(glyphX + 8f, glyphY + glyphHeight * 0.46f, glyphWidth - 16f, 3f), color("A88A52", metalAlpha * 0.52f))
                }
                else -> {
                    canvas.drawRect(tileBounds(glyphX + glyphWidth * 0.30f, glyphY + 7f, glyphWidth * 0.40f, glyphHeight - 14f), color("A88A52", metalAlpha * 0.63f))
                    canvas.drawRect(tileBounds(glyphX + glyphWidth * 0.40f, glyphY + 5f, glyphWidth * 0.20f, glyphHeight - 10f), color("D99A2B", metalAlpha * 0.63f))
                    canvas.drawRect(tileBounds(glyphX + glyphWidth * 0.22f, glyphY + glyphHeight * 0.40f, glyphWidth * 0.56f, 3f), color("050604", shadowAlpha * 0.58f))
                }
            }
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
            alpha = BOTTOM_CHILD_FRAME_ALPHA,
        )
        canvas.drawRect(content.toTileBounds(), color("120A04", 0.72f))
        canvas.drawRect(
            tileBounds(content.x + 8f, content.y + 8f, content.width - 16f, content.height - 16f),
            color("050604", 0.24f),
        )
        val crestSide =
            when {
                content.width >= 360f -> 112f
                content.width >= 240f -> 96f
                else -> 84f
            }.coerceAtMost(content.height - 18f)
        val crestX = content.x + 6f
        val crestY = content.y + (content.height - crestSide) / 2f
        drawHeroHeraldicPillar(
            canvas = canvas,
            content = content,
            crestX = crestX,
            crestSide = crestSide,
        )
        canvas.drawRect(
            tileBounds(crestX - 3f, crestY - 3f, crestSide + 6f, crestSide + 6f),
            color("050604", 0.38f),
        )
        canvas.drawAsset(
            chrome.demoShell.heroCrestPlaceholder,
            tileBounds(crestX, crestY, crestSide, crestSide),
        )
        drawHeroLevelBadge(canvas, frame.model.shell.demo.heroLevelText, crestX, crestY, crestSide)
        val gaugeX = content.x + crestSide + 22f
        val gaugeWidth = (content.right - gaugeX).coerceAtLeast(1f)
        drawBoundedText(canvas, frame.textLayout.playerName, gaugeX, content.top - 8f, gaugeWidth, TileTextTone.GOLD)
        frame.model.shell.demo.heroSummaryLines.firstOrNull()?.let { floor ->
            drawBoundedText(canvas, floor, gaugeX, content.top - 28f, gaugeWidth, TileTextTone.LIGHT_GRAY)
        }
        val statLines = frame.model.shell.demo.heroSummaryLines.drop(3).take(2)
        if (statLines.isNotEmpty()) {
            val statsY = if (frame.model.hud.statusIcons.isEmpty()) content.y + 16f else content.y + 5f
            drawHeroStatChips(canvas, statLines, gaugeX, statsY, gaugeWidth)
        }
        val gauges = listOf(frame.model.hud.hpGauge, frame.model.hud.resourceGauge)
        val gaugeHeight = 18f
        val gaugeGap = 8f
        val firstGaugeY = (content.y + 52f).coerceAtMost(content.top - 86f)
        val gaugeWellY = firstGaugeY - 10f
        val gaugeWellHeight = gaugeHeight * gauges.size + gaugeGap * (gauges.size - 1) + 20f
        canvas.drawRect(
            tileBounds(gaugeX - 10f, gaugeWellY, gaugeWidth + 20f, gaugeWellHeight),
            color("050604", 0.62f),
        )
        canvas.drawRect(
            tileBounds(gaugeX - 8f, gaugeWellY + gaugeWellHeight - 2f, gaugeWidth + 16f, 1f),
            color("D99A2B", 0.24f),
        )
        canvas.drawRect(tileBounds(gaugeX - 8f, gaugeWellY + 2f, gaugeWidth + 16f, 1f), color("1CB7C8", 0.12f))
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
            drawBoundedText(canvas, gauge.summary, drawX + 6f, y + gaugeHeight - 4f, barWidth - 12f, TileTextTone.WHITE)
        }
        renderStatusIcons(
            canvas = canvas,
            frame = frame,
            content = content,
            startX = gaugeX,
            iconY = content.y + 34f,
        )
    }

    private fun drawHeroHeraldicPillar(
        canvas: TileCanvas,
        content: GameShellBounds,
        crestX: Float,
        crestSide: Float,
    ) {
        val pillarX = content.x + 4f
        val pillarY = content.y + 10f
        val pillarWidth = (crestX - pillarX + crestSide + 10f).coerceAtMost(content.width * 0.42f)
        val pillarHeight = (content.height - 20f).coerceAtLeast(1f)
        val pillar = GameShellBounds(pillarX, pillarY, pillarWidth, pillarHeight)
        canvas.drawRect(pillar.toTileBounds(), color("050604", 0.54f))
        canvas.drawRect(tileBounds(pillar.x + 6f, pillar.y + 7f, pillar.width - 12f, pillar.height - 14f), color("1A0D05", 0.30f))
        canvas.drawRect(tileBounds(pillar.x + 8f, pillar.top - 10f, pillar.width - 16f, 3f), color("D99A2B", 0.18f))
        canvas.drawRect(tileBounds(pillar.x + 8f, pillar.y + 7f, pillar.width - 16f, 2f), color("050604", 0.34f))
        canvas.drawRect(tileBounds(pillar.x + 5f, pillar.y + 18f, 2f, pillar.height - 36f), color("2B3542", 0.24f))
        canvas.drawRect(tileBounds(pillar.right - 7f, pillar.y + 18f, 2f, pillar.height - 36f), color("2B3542", 0.20f))
        canvas.drawRect(tileBounds(pillar.x + 12f, pillar.y + pillar.height * 0.50f, pillar.width - 24f, 2f), color("1CB7C8", 0.10f))
    }

    private fun drawHeroStatChips(
        canvas: TileCanvas,
        statLines: List<String>,
        x: Float,
        y: Float,
        width: Float,
    ) {
        if (statLines.isEmpty() || width <= 0f) {
            return
        }
        val gap = 8f
        val chipWidth = ((width - gap * (statLines.size - 1)) / statLines.size).coerceAtLeast(1f)
        statLines.forEachIndexed { index, text ->
            val chipX = x + index * (chipWidth + gap)
            canvas.drawRect(tileBounds(chipX, y, chipWidth, 22f), color("050604", 0.42f))
            canvas.drawRect(tileBounds(chipX, y + 21f, chipWidth, 1f), color("B8873E", 0.26f))
            drawBoundedText(canvas, text, chipX + 8f, y + 15f, chipWidth - 16f, TileTextTone.LIGHT_GRAY)
        }
    }

    private fun drawHeroLevelBadge(
        canvas: TileCanvas,
        levelText: String,
        crestX: Float,
        crestY: Float,
        crestSide: Float,
    ) {
        if (levelText.isBlank()) {
            return
        }
        val badgeSide = (crestSide * 0.26f).coerceIn(24f, 32f)
        val badgeX = crestX + 4f
        val badgeY = crestY + 4f
        val badgeText = levelText.substringAfterLast(' ').ifBlank { levelText }
        val badgeBounds = GameShellBounds(badgeX, badgeY, badgeSide, badgeSide)
        canvas.drawRect(badgeBounds.toTileBounds(), color("050604", 0.82f))
        canvas.drawRect(tileBounds(badgeX + 3f, badgeY + 3f, badgeSide - 6f, badgeSide - 6f), color("261304", 0.62f))
        drawRectOutline(canvas, badgeBounds, stroke = 1f, color = color("D99A2B", 0.54f))
        canvas.drawRect(tileBounds(badgeX + 5f, badgeY + badgeSide - 7f, badgeSide - 10f, 1f), color("1CB7C8", 0.28f))
        drawBoundedText(canvas, badgeText, badgeX + 7f, badgeY + badgeSide - 9f, badgeSide - 14f, TileTextTone.GOLD)
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
            alpha = BOTTOM_CHILD_FRAME_ALPHA,
        )
        drawActionDeckSurface(canvas, deck)
        drawActionCommandConsole(
            canvas = canvas,
            deck = deck,
            slotBounds = frame.layout.demoShell.bottomDeck.actionSlotBounds,
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
            val socketPadBounds =
                GameShellBounds(
                    x = iconSocketBounds.x - 4f,
                    y = iconSocketBounds.y - 4f,
                    width = iconSocketBounds.width + 8f,
                    height = iconSocketBounds.height + 8f,
                )
            if (entry != null) {
                drawActionSlotSilhouetteChrome(canvas, slotBounds, iconSocketBounds)
            }
            val isReserveSocket = entry == null
            canvas.drawRect(socketPadBounds.toTileBounds(), color("050604", if (isReserveSocket) 0.14f else 0.30f))
            canvas.drawRect(
                tileBounds(socketPadBounds.x + 4f, socketPadBounds.top - 2f, socketPadBounds.width - 8f, 2f),
                color("D99A2B", if (isReserveSocket) 0.04f else 0.12f),
            )
            canvas.drawRect(
                tileBounds(socketPadBounds.x + 6f, socketPadBounds.y + 3f, socketPadBounds.width - 12f, 1f),
                color("1CB7C8", if (isReserveSocket) 0.025f else 0.07f),
            )
            if (!isReserveSocket) {
                canvas.drawRect(
                    tileBounds(
                        slotBounds.x + slotBounds.width / 2f - 0.5f,
                        textPlateBounds.top + 1f,
                        1f,
                        (iconSocketBounds.y - textPlateBounds.top - 1f).coerceAtLeast(1f),
                    ),
                    color("B8873E", 0.07f),
                )
            }
            canvas.drawAsset(
                if (isReserveSocket) chrome.slotEmpty else chrome.slotEquipped,
                iconSocketBounds.toTileBounds(),
                alpha = if (isReserveSocket) 0.36f else 0.9f,
            )
            if (entry != null) {
                drawActionIconSubjectAccents(canvas, iconSocketBounds)
            } else {
                drawActionReserveSocketInterior(canvas, iconSocketBounds)
            }
            entry?.icon?.let { icon ->
                val iconInset = iconSocketSide * 0.07f
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
                drawBoundedText(
                    canvas = canvas,
                    text = text.hotkey,
                    x = iconSocketBounds.x + 7f,
                    baselineY = iconSocketBounds.top - 7f,
                    maxWidth = iconSocketBounds.width - 14f,
                    tone = TileTextTone.GOLD,
                    style = TileTextStyle.CAPTION,
                )
                drawRowPlate(canvas, textPlateBounds)
                canvas.drawRect(tileBounds(textPlateBounds.x + 4f, textPlateBounds.top - 2f, textPlateBounds.width - 8f, 2f), color("1CB7C8", 0.16f))
                drawBoundedText(
                    canvas = canvas,
                    text = text.hotkey,
                    x = textPlateBounds.x + 5f,
                    baselineY = textPlateBounds.y + textPlateHeight - 8f,
                    maxWidth = 14f,
                    tone = TileTextTone.GOLD,
                    style = TileTextStyle.CAPTION,
                )
                drawBoundedText(
                    canvas = canvas,
                    text = text.label,
                    x = textPlateBounds.x + 22f,
                    baselineY = textPlateBounds.y + textPlateHeight - 8f,
                    maxWidth = textPlateBounds.width - 26f,
                    tone = TileTextTone.WHITE,
                    style = TileTextStyle.CAPTION,
                )
            }
        }
    }

    private fun drawActionReserveSocketInterior(
        canvas: TileCanvas,
        iconSocketBounds: GameShellBounds,
    ) {
        val inset = iconSocketBounds.width * 0.24f
        val socket =
            GameShellBounds(
                x = iconSocketBounds.x + inset,
                y = iconSocketBounds.y + inset,
                width = (iconSocketBounds.width - inset * 2f).coerceAtLeast(1f),
                height = (iconSocketBounds.height - inset * 2f).coerceAtLeast(1f),
            )
        canvas.drawRect(socket.toTileBounds(), color("050604", 0.18f))
        drawRectOutline(canvas, socket, stroke = 1f, color = color("4F412B", 0.14f))
        canvas.drawRect(tileBounds(socket.x + 5f, socket.top - 7f, socket.width - 10f, 2f), color("050604", 0.18f))
        canvas.drawRect(tileBounds(socket.x + 5f, socket.y + 5f, socket.width - 10f, 1f), color("D99A2B", 0.045f))
        canvas.drawRect(tileBounds(socket.x + 4f, socket.y + socket.height * 0.36f, 2f, socket.height * 0.28f), color("2B3542", 0.07f))
        canvas.drawRect(tileBounds(socket.right - 6f, socket.y + socket.height * 0.36f, 2f, socket.height * 0.28f), color("2B3542", 0.07f))
    }

    private fun drawActionSlotSilhouetteChrome(
        canvas: TileCanvas,
        slotBounds: GameShellBounds,
        iconSocketBounds: GameShellBounds,
    ) {
        val stageInsetX = slotBounds.width * 0.08f
        val stageWidth = (slotBounds.width - stageInsetX * 2f).coerceAtLeast(1f)
        val stageHeight = (iconSocketBounds.height + 12f).coerceAtMost(slotBounds.height * 0.62f)
        val stageY =
            (iconSocketBounds.y - 5f)
                .coerceAtLeast(slotBounds.y + slotBounds.height * 0.30f)
                .coerceAtMost(slotBounds.top - stageHeight - 7f)
        val stage =
            GameShellBounds(
                x = slotBounds.x + stageInsetX,
                y = stageY,
                width = stageWidth,
                height = stageHeight,
            )
        canvas.drawRect(stage.toTileBounds(), color("050604", 0.236f))
        val lipInset = stage.width * 0.08f
        val lipWidth = (stage.width - lipInset * 2f).coerceAtLeast(1f)
        canvas.drawRect(tileBounds(stage.x + lipInset, stage.y + 5f, lipWidth, 3f), color("D99A2B", 0.174f))
        canvas.drawRect(tileBounds(stage.x + lipInset, stage.top - 7f, lipWidth, 3f), color("D99A2B", 0.174f))
        val jambHeight = stage.height * 0.66f
        val jambY = stage.y + stage.height * 0.17f
        canvas.drawRect(tileBounds(stage.x + 6f, jambY, 3f, jambHeight), color("2B3542", 0.151f))
        canvas.drawRect(tileBounds(stage.right - 9f, jambY, 3f, jambHeight), color("2B3542", 0.151f))
        canvas.drawRect(tileBounds(stage.x + 11f, stage.y + 11f, stage.width - 22f, 1f), color("1CB7C8", 0.076f))
        canvas.drawRect(tileBounds(stage.x + 11f, stage.top - 12f, stage.width - 22f, 1f), color("D99A2B", 0.092f))
        canvas.drawRect(tileBounds(stage.x + 8f, stage.top - 10f, 5f, 5f), color("B8873E", 0.138f))
        canvas.drawRect(tileBounds(stage.right - 13f, stage.top - 10f, 5f, 5f), color("B8873E", 0.138f))
    }

    private fun drawActionIconSubjectAccents(
        canvas: TileCanvas,
        iconSocketBounds: GameShellBounds,
    ) {
        val subjectInset = iconSocketBounds.width * 0.15f
        val subjectX = iconSocketBounds.x + subjectInset
        val subjectY = iconSocketBounds.y + subjectInset
        val subjectWidth = (iconSocketBounds.width - subjectInset * 2f).coerceAtLeast(1f)
        val subjectHeight = (iconSocketBounds.height - subjectInset * 2f).coerceAtLeast(1f)
        val pedestalX = iconSocketBounds.x + iconSocketBounds.width * 0.08f
        val pedestalY = iconSocketBounds.y + iconSocketBounds.height * 0.10f
        val pedestalWidth = iconSocketBounds.width * 0.84f
        val pedestalHeight = iconSocketBounds.height * 0.68f
        canvas.drawRect(tileBounds(pedestalX, pedestalY, pedestalWidth, pedestalHeight), color("050604", 0.278f))
        canvas.drawRect(
            tileBounds(pedestalX + pedestalWidth * 0.12f, pedestalY + pedestalHeight * 0.76f, pedestalWidth * 0.76f, 4f),
            color("D99A2B", 0.232f),
        )
        canvas.drawRect(
            tileBounds(pedestalX + pedestalWidth * 0.18f, pedestalY + 6f, pedestalWidth * 0.64f, 2f),
            color("1CB7C8", 0.082f),
        )
        canvas.drawRect(
            tileBounds(pedestalX + 4f, pedestalY + pedestalHeight * 0.24f, 2f, pedestalHeight * 0.50f),
            color("2B3542", 0.156f),
        )
        canvas.drawRect(
            tileBounds(pedestalX + pedestalWidth - 6f, pedestalY + pedestalHeight * 0.24f, 2f, pedestalHeight * 0.50f),
            color("2B3542", 0.156f),
        )
        canvas.drawRect(tileBounds(subjectX, subjectY, subjectWidth, subjectHeight), color("050604", 0.184f))
        canvas.drawRect(
            tileBounds(subjectX, subjectY + subjectHeight * 0.74f, subjectWidth, 3f),
            color("D99A2B", 0.286f),
        )
        canvas.drawRect(
            tileBounds(subjectX + subjectWidth * 0.12f, subjectY + subjectHeight * 0.16f, 2f, subjectHeight * 0.68f),
            color("D99A2B", 0.193f),
        )
        canvas.drawRect(
            tileBounds(subjectX + subjectWidth * 0.84f, subjectY + subjectHeight * 0.18f, 1.5f, subjectHeight * 0.60f),
            color("1CB7C8", 0.108f),
        )
        canvas.drawRect(
            tileBounds(subjectX + subjectWidth * 0.18f, subjectY + 4f, subjectWidth * 0.64f, 1f),
            color("D99A2B", 0.106f),
        )
    }

    private fun drawActionCommandConsole(
        canvas: TileCanvas,
        deck: GameShellBounds,
        slotBounds: List<GameShellBounds>,
    ) {
        if (slotBounds.isEmpty()) {
            return
        }
        val first = slotBounds.first()
        val last = slotBounds.last()
        val spanX = (first.x - 8f).coerceAtLeast(deck.x + 12f)
        val spanRight = (last.right + 8f).coerceAtMost(deck.right - 12f)
        val spanWidth = (spanRight - spanX).coerceAtLeast(1f)
        val plinthY = first.y + 3f
        val plinthHeight = 30f.coerceAtMost(first.height - 12f)
        canvas.drawRect(tileBounds(spanX, plinthY, spanWidth, plinthHeight), color("050604", 0.168f))
        canvas.drawRect(tileBounds(spanX + 10f, plinthY + plinthHeight - 2f, spanWidth - 20f, 2f), color("D99A2B", 0.092f))
        canvas.drawRect(tileBounds(spanX + 18f, plinthY + 4f, spanWidth - 36f, 1f), color("1CB7C8", 0.052f))

        val railCenterY = first.y + first.height * 0.56f
        canvas.drawRect(tileBounds(spanX + 8f, railCenterY - 2f, spanWidth - 16f, 4f), color("0B0A08", 0.112f))
        canvas.drawRect(tileBounds(spanX + 22f, railCenterY + 2f, spanWidth - 44f, 1f), color("B8873E", 0.050f))
        slotBounds.forEach { bounds ->
            val centerX = bounds.x + bounds.width / 2f
            canvas.drawRect(tileBounds(centerX - 2.5f, railCenterY - 2.5f, 5f, 5f), color("D99A2B", 0.136f))
            canvas.drawRect(tileBounds(centerX - 1f, railCenterY - 1f, 2f, 2f), color("050604", 0.24f))
        }
    }

    private fun drawActionDeckSurface(
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
        canvas.drawRect(body.toTileBounds(), color("050604", 0.64f))
        canvas.drawRect(tileBounds(body.x + 10f, body.top - 3f, body.width - 20f, 2f), color("D99A2B", 0.24f))
        canvas.drawRect(tileBounds(body.x + 10f, body.y + 3f, body.width - 20f, 1f), color("1CB7C8", 0.14f))
        canvas.drawRect(tileBounds(body.x + 8f, body.y + 10f, 2f, body.height - 20f), color("B8873E", 0.18f))
        canvas.drawRect(tileBounds(body.right - 10f, body.y + 10f, 1f, body.height - 20f), color("050604", 0.42f))
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
            alpha = BOTTOM_CHILD_FRAME_ALPHA,
        )
        drawLogDeckSurface(canvas, log)
        val logContent = contentBounds(log, ChromeSurfaceKind.Panel)
        val logTextStartX = logContent.x + 24f
        val rowCount = ((logContent.height - 8f) / CAPTION_LINE_HEIGHT).toInt().coerceAtLeast(1)
        val visibleLines =
            frame.textLayout.messageLines
            .flatMap { line ->
                TileTextMetrics.wrapTextToWidth(
                    line.text,
                    (logContent.right - logTextStartX - 8f) * TEXT_WIDTH_SAFETY,
                    TileTextStyle.CAPTION,
                )
                    .map { text -> line.copy(text = text) }
            }
            .take(rowCount)
        drawLogEventRowPlates(canvas, logContent, visibleLines)
        drawLogLedgerGutter(canvas, logContent, visibleLines)
        visibleLines.forEachIndexed { index, line ->
            val baselineY = logContent.top - 8f - index * CAPTION_LINE_HEIGHT
            var textX = logTextStartX
            line.icon?.let { icon ->
                val iconSide = 14f
                canvas.drawAsset(icon, tileBounds(textX, baselineY - 12f, iconSide, iconSide), alpha = 0.86f)
                textX += iconSide + 5f
            }
            drawBoundedText(
                canvas = canvas,
                text = line.text,
                x = textX,
                baselineY = baselineY,
                maxWidth = logContent.right - textX - 8f,
                tone = line.tone,
                style = TileTextStyle.CAPTION,
            )
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
        canvas.drawRect(tileBounds(body.x, body.top - 2f, body.width, 2f), color("D99A2B", 0.18f))
        canvas.drawRect(tileBounds(body.x, body.y, 2f, body.height), color("B8873E", 0.18f))
        canvas.drawRect(tileBounds(body.x + 12f, body.y + 12f, 3f, body.height - 24f), color("050604", 0.34f))
        canvas.drawRect(tileBounds(body.x + 13f, body.y + 20f, 1f, body.height - 40f), color("1CB7C8", 0.10f))
        canvas.drawRect(tileBounds(body.x + 16f, body.top - 36f, 1f, 24f), color("D99A2B", 0.34f))
    }

    private fun drawLogEventRowPlates(
        canvas: TileCanvas,
        content: GameShellBounds,
        lines: List<TileMessageLine>,
    ) {
        if (lines.isEmpty()) {
            return
        }
        val rowX = content.x + 22f
        val rowWidth = (content.width - 38f).coerceAtLeast(1f)
        lines.forEachIndexed { index, line ->
            val baselineY = content.top - 8f - index * CAPTION_LINE_HEIGHT
            val rowY = baselineY - 13f
            val rowHeight = 16f
            canvas.drawRect(tileBounds(rowX, rowY, rowWidth, rowHeight), color("050604", 0.327f))
            canvas.drawRect(tileBounds(rowX, rowY + 2f, 3f, rowHeight - 4f), color(logLedgerAccentHex(line.tone), 0.252f))
            canvas.drawRect(tileBounds(rowX + 7f, rowY + 3f, (rowWidth - 14f).coerceAtLeast(1f), rowHeight - 5f), color("0B0A08", 0.205f))
            canvas.drawRect(tileBounds(rowX + rowWidth - 10f, rowY + 3f, 2f, rowHeight - 6f), color("1CB7C8", 0.142f))
            canvas.drawRect(tileBounds(rowX + 7f, rowY + 2f, (rowWidth - 14f).coerceAtLeast(1f), 1f), color("D99A2B", 0.045f))
            canvas.drawRect(tileBounds(rowX + 10f, rowY + rowHeight - 4f, (rowWidth - 24f).coerceAtLeast(1f), 1f), color("D99A2B", 0.074f))
            canvas.drawRect(tileBounds(rowX + 5f, rowY + rowHeight - 1f, (rowWidth - 10f).coerceAtLeast(1f), 1f), color("050604", 0.22f))
        }
    }

    private fun drawLogLedgerGutter(
        canvas: TileCanvas,
        content: GameShellBounds,
        lines: List<TileMessageLine>,
    ) {
        if (lines.isEmpty()) {
            return
        }
        val spineX = content.x + 11f
        val spineY = content.y + 12f
        val spineHeight = (content.height - 24f).coerceAtLeast(1f)
        canvas.drawRect(tileBounds(spineX, spineY, 3f, spineHeight), color("050604", 0.34f))
        canvas.drawRect(tileBounds(spineX + 1f, spineY + 6f, 1f, (spineHeight - 12f).coerceAtLeast(1f)), color("1CB7C8", 0.10f))
        lines.forEachIndexed { index, line ->
            val baselineY = content.top - 8f - index * CAPTION_LINE_HEIGHT
            val tickY = baselineY - 5f
            canvas.drawRect(tileBounds(spineX - 2f, tickY, 7f, 2f), color(logLedgerAccentHex(line.tone), 0.118f))
        }
        canvas.drawRect(tileBounds(content.x + 22f, content.y + 5f, (content.width - 46f).coerceAtLeast(1f), 16f), color("050604", 0.18f))
        val scrollX = content.right - 5f
        canvas.drawRect(tileBounds(scrollX, content.y + 12f, 2f, spineHeight), color("050604", 0.28f))
        canvas.drawRect(tileBounds(scrollX - 1f, content.y + 14f, 4f, 28f.coerceAtMost(spineHeight)), color("1CB7C8", 0.092f))
    }

    private fun drawStageFeather(
        canvas: TileCanvas,
        bounds: GameShellBounds,
    ) {
        val edge = 16f
        canvas.drawRect(tileBounds(bounds.x, bounds.y, edge, bounds.height), color("050604", 0.28f))
        canvas.drawRect(tileBounds(bounds.right - edge, bounds.y, edge, bounds.height), color("050604", 0.28f))
        canvas.drawRect(tileBounds(bounds.x, bounds.y, bounds.width, edge), color("050604", 0.24f))
        canvas.drawRect(tileBounds(bounds.x, bounds.top - edge, bounds.width, edge), color("050604", 0.24f))
        canvas.drawRect(tileBounds(bounds.x + edge, bounds.top - 2f, bounds.width - edge * 2f, 2f), color("D99A2B", 0.22f))
        canvas.drawRect(tileBounds(bounds.x + edge, bounds.y, bounds.width - edge * 2f, 2f), color("D99A2B", 0.16f))
    }

    private fun drawRightPanelDepth(
        canvas: TileCanvas,
        bounds: GameShellBounds,
    ) {
        canvas.drawRect(tileBounds(bounds.x + 12f, bounds.top - 44f, bounds.width - 24f, 1.5f), color("D99A2B", 0.24f))
        canvas.drawRect(tileBounds(bounds.x + 12f, bounds.y + 44f, bounds.width - 24f, 1.5f), color("1CB7C8", 0.14f))
        canvas.drawRect(tileBounds(bounds.x + 10f, bounds.y + 10f, 2f, bounds.height - 20f), color("B8873E", 0.26f))
        canvas.drawRect(tileBounds(bounds.right - 12f, bounds.y + 10f, 2f, bounds.height - 20f), color("050604", 0.42f))
        canvas.drawRect(tileBounds(bounds.x + 26f, bounds.y + bounds.height * 0.58f, bounds.width - 52f, bounds.height * 0.18f), color("251307", 0.18f))
        canvas.drawRect(tileBounds(bounds.x + 34f, bounds.y + bounds.height * 0.61f, bounds.width - 68f, 2f), color("D99A2B", 0.12f))
        canvas.drawRect(tileBounds(bounds.x + 34f, bounds.y + bounds.height * 0.69f, bounds.width - 68f, 2f), color("050604", 0.18f))
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
        canvas.drawRect(tileBounds(x - 3f, y - 2f, width + 6f, height + 4f), color("050604", 0.345f))
        canvas.drawRect(tileBounds(x - 1f, y - 1f, width + 2f, 1f), color("050604", 0.42f))
        canvas.drawRect(tileBounds(x + 4f, y + height + 1f, (width - 8f).coerceAtLeast(1f), 2f), color("D99A2B", 0.18f))
        canvas.drawRect(tileBounds(x + 5f, y + 2f, (width - 10f).coerceAtLeast(1f), 1f), color("1CB7C8", 0.08f))
        val gaugeAccent = Color(gaugeColor(gauge)).also { color -> color.a = 0.24f }
        canvas.drawRect(tileBounds(x - 6f, y + 3f, 2f, height - 6f), gaugeAccent)
        canvas.drawRect(tileBounds(x, y, width, height), UiDesignTokens.color.bar.background.color())
        val fillWidth = (width - 4f) * gauge.percent
        canvas.drawRect(tileBounds(x + 2f, y + 2f, fillWidth, height - 4f), gaugeColor(gauge))
        canvas.drawRect(tileBounds(x + 4f, y + 3f, (fillWidth - 6f).coerceAtLeast(1f), 1f), color("E7E1D3", 0.09f))
        canvas.drawRect(tileBounds(x + 3f, y + height - 3f, (width - 6f).coerceAtLeast(1f), 2f), color("050604", 0.18f))
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
        style: TileTextStyle = TileTextStyle.SMALL,
    ) {
        if (text.isBlank() || maxWidth <= 0f) {
            return
        }
        canvas.drawText(
            style,
            TileTextMetrics.truncateTextToWidth(text, maxWidth, style),
            tilePosition(x, baselineY),
            toneColor(tone),
        )
    }

    private fun drawMutedSmallText(
        canvas: TileCanvas,
        text: String,
        x: Float,
        baselineY: Float,
        maxWidth: Float,
        style: TileTextStyle = TileTextStyle.SMALL,
    ) {
        if (text.isBlank() || maxWidth <= 0f) {
            return
        }
        canvas.drawText(
            style,
            TileTextMetrics.truncateTextToWidth(text, maxWidth, style),
            tilePosition(x, baselineY),
            color("AEB5BF", if (style == TileTextStyle.CAPTION) 0.58f else 0.62f),
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

    private fun logLedgerAccentHex(tone: TileTextTone): String =
        when (tone) {
            TileTextTone.GOLD -> "D99A2B"
            TileTextTone.CYAN -> "1CB7C8"
            TileTextTone.GREEN -> "52C989"
            TileTextTone.RED -> "B64242"
            TileTextTone.BLUE -> "7B5CE1"
            TileTextTone.MAGENTA -> "7B5CE1"
            TileTextTone.WHITE,
            TileTextTone.LIGHT_GRAY,
            TileTextTone.GRAY,
            -> "B8873E"
        }

    private fun demoBase(): Color = color("05070A", 1f)

    private fun demoBaseDim(): Color = color("1A0E04", 0.82f)

    private fun demoRaised(): Color = color("15100A", 0.96f)

    private fun demoOverlay(): Color = color("100C08", 0.93f)

    private fun demoSubtleBorder(): Color = color("4A3420", 0.86f)

    private fun demoStrongBorder(): Color = color("D99A2B", 0.54f)

    private fun demoSlotEmpty(): Color = color("0F0D0B", 0.92f)

    private fun demoFocus(): Color = color("1CB7C8", 1f)

    private data class OperationCommandParts(
        val keyText: String,
        val labelText: String,
    )

    private fun color(
        hex: String,
        alpha: Float,
    ): Color = Color.valueOf(hex).also { color -> color.a = alpha }
}
