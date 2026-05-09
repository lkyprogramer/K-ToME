package com.ktome.client.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import com.ktome.client.ui.token.UiDesignTokens

internal data class ScreenPanelBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = x + width
    val top: Float get() = y + height
}

internal data class StandaloneScreenLayout(
    val background: ScreenPanelBounds,
    val header: ScreenPanelBounds,
    val primaryActionStack: ScreenPanelBounds,
    val secondaryPanel: ScreenPanelBounds,
    val disabledDetailArea: ScreenPanelBounds,
    val footerHelp: ScreenPanelBounds,
)

internal data class ValidationSetupEntryPlacement(
    val x: Float,
    val baselineY: Float,
    val maxChars: Int,
)

internal enum class StandaloneDetailAreaMode {
    HIDDEN,
    VISIBLE,
}

internal data class StandaloneChromeRequest(
    val layout: StandaloneScreenLayout,
    val detailAreaMode: StandaloneDetailAreaMode,
)

internal class StandaloneScreenChrome : Disposable {
    private val whitePixel: Texture = solidTexture()

    fun draw(
        batch: SpriteBatch,
        request: StandaloneChromeRequest,
    ) {
        val layout = request.layout
        drawPanel(batch, layout.header, UiDesignTokens.color.surface.overlay.color(), UiDesignTokens.color.border.strong.color())
        drawPanel(batch, layout.primaryActionStack, UiDesignTokens.color.surface.raised.color(), UiDesignTokens.color.border.subtle.color())
        if (!layout.secondaryPanel.sameBounds(layout.primaryActionStack)) {
            drawPanel(batch, layout.secondaryPanel, UiDesignTokens.color.surface.raised.color(), UiDesignTokens.color.border.subtle.color())
        }
        if (request.detailAreaMode == StandaloneDetailAreaMode.VISIBLE) {
            drawPanel(batch, layout.disabledDetailArea, UiDesignTokens.color.surface.baseDim.color(), UiDesignTokens.color.status.badge.turns.color())
        }
        drawPanel(batch, layout.footerHelp, UiDesignTokens.color.surface.baseDim.color(), UiDesignTokens.color.border.subtle.color())
        batch.color = Color.WHITE
    }

    override fun dispose() {
        whitePixel.dispose()
    }

    private fun drawPanel(
        batch: SpriteBatch,
        bounds: ScreenPanelBounds,
        fill: Color,
        border: Color,
    ) {
        drawRect(batch, bounds.x, bounds.y, bounds.width, bounds.height, fill)
        drawOutline(batch, bounds, UiDesignTokens.stroke.thin, border)
    }

    private fun drawOutline(
        batch: SpriteBatch,
        bounds: ScreenPanelBounds,
        stroke: Float,
        color: Color,
    ) {
        drawRect(batch, bounds.x, bounds.y, bounds.width, stroke, color)
        drawRect(batch, bounds.x, bounds.top - stroke, bounds.width, stroke, color)
        drawRect(batch, bounds.x, bounds.y, stroke, bounds.height, color)
        drawRect(batch, bounds.right - stroke, bounds.y, stroke, bounds.height, color)
    }

    private fun drawRect(
        batch: SpriteBatch,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
    ) {
        batch.color = color
        batch.draw(whitePixel, x, y, width, height)
    }

    private fun solidTexture(): Texture =
        Pixmap(1, 1, Pixmap.Format.RGBA8888).let { pixmap ->
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            Texture(pixmap).also { texture ->
                texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
                pixmap.dispose()
            }
        }
}

internal object DarkStandaloneScreenLayout {
    const val width: Float = 960f
    const val height: Float = 540f
    const val marginX: Float = 80f
    const val headerTopY: Float = 472f
    const val footerBaselineY: Float = 42f
    const val validationEntryMinStepY: Float = 24f
    const val validationFooterControlsBaselineY: Float = 54f
    const val outcomeBodyTopPaddingY: Float = 18f
    const val outcomeBodyStepY: Float = 26f

    private const val VALIDATION_ENTRY_TOP_PADDING_Y = 10f
    private const val VALIDATION_ENTRY_COLUMN_GAP = 32f
    private const val VALIDATION_ENTRY_MAX_STEP_Y = 30f
    private const val VALIDATION_ENTRY_SINGLE_COLUMN_MAX_CHARS = 72
    private const val VALIDATION_ENTRY_TWO_COLUMN_MAX_CHARS = 34

    fun mainMenu(): StandaloneScreenLayout {
        val tokens = UiDesignTokens.fixed
        return StandaloneScreenLayout(
            background = ScreenPanelBounds(0f, 0f, tokens.standaloneWidth, tokens.standaloneHeight),
            header = ScreenPanelBounds(marginX, 380f, 800f, 112f),
            primaryActionStack = ScreenPanelBounds(marginX, 126f, tokens.actionStackWidth, tokens.actionStackHeight),
            secondaryPanel = ScreenPanelBounds(438f, 126f, 442f, 248f),
            disabledDetailArea = ScreenPanelBounds(marginX, 82f, 800f, 36f),
            footerHelp = ScreenPanelBounds(marginX, 28f, 800f, 44f),
        )
    }

    fun validationSetup(): StandaloneScreenLayout {
        val tokens = UiDesignTokens.fixed
        return StandaloneScreenLayout(
            background = ScreenPanelBounds(0f, 0f, tokens.standaloneWidth, tokens.standaloneHeight),
            header = ScreenPanelBounds(marginX, 402f, 800f, 90f),
            primaryActionStack = ScreenPanelBounds(marginX, 74f, tokens.validationListWidth, 252f),
            secondaryPanel = ScreenPanelBounds(marginX, 350f, tokens.validationListWidth, 50f),
            disabledDetailArea = ScreenPanelBounds(marginX, 328f, tokens.validationListWidth, 18f),
            footerHelp = ScreenPanelBounds(marginX, 28f, 800f, 44f),
        )
    }

    fun outcome(): StandaloneScreenLayout =
        StandaloneScreenLayout(
            background = ScreenPanelBounds(0f, 0f, width, height),
            header = ScreenPanelBounds(marginX, 408f, 800f, 84f),
            primaryActionStack = ScreenPanelBounds(marginX, 112f, 800f, 284f),
            secondaryPanel = ScreenPanelBounds(marginX, 112f, 800f, 284f),
            disabledDetailArea = ScreenPanelBounds(marginX, 72f, 800f, 34f),
            footerHelp = ScreenPanelBounds(marginX, 28f, 800f, 44f),
        )

    fun validationEntryPlacements(entryCount: Int): List<ValidationSetupEntryPlacement> {
        val layout = validationSetup()
        val columnCount =
            if (entryCount <= validationSingleColumnCapacity()) {
                1
            } else {
                2
            }
        val rowCount = ((entryCount + columnCount - 1) / columnCount).coerceAtLeast(1)
        val columnWidth =
            if (columnCount == 1) {
                layout.primaryActionStack.width
            } else {
                (layout.primaryActionStack.width - VALIDATION_ENTRY_COLUMN_GAP) / columnCount
            }
        val entryStepY = validationEntryRowStep(rowCount)
        val maxChars =
            if (columnCount == 1) {
                VALIDATION_ENTRY_SINGLE_COLUMN_MAX_CHARS
            } else {
                VALIDATION_ENTRY_TWO_COLUMN_MAX_CHARS
            }

        return List(entryCount) { index ->
            val column = index / rowCount
            val row = index % rowCount
            ValidationSetupEntryPlacement(
                x = layout.primaryActionStack.x + column * (columnWidth + VALIDATION_ENTRY_COLUMN_GAP),
                baselineY = layout.primaryActionStack.top - VALIDATION_ENTRY_TOP_PADDING_Y - row * entryStepY,
                maxChars = maxChars,
            )
        }
    }

    fun validationEntryRowStep(rowCount: Int): Float {
        val layout = validationSetup()
        val availableHeight = layout.primaryActionStack.height - VALIDATION_ENTRY_TOP_PADDING_Y * 2f
        return (availableHeight / rowCount.coerceAtLeast(1)).coerceIn(validationEntryMinStepY, VALIDATION_ENTRY_MAX_STEP_Y)
    }

    fun outcomeBodyLineBaselines(lineCount: Int): List<Float> {
        val layout = outcome()
        return List(lineCount.coerceAtMost(outcomeBodyLineCapacity())) { index ->
            layout.primaryActionStack.top - outcomeBodyTopPaddingY - index * outcomeBodyStepY
        }
    }

    fun outcomeBodyLineCapacity(): Int {
        val layout = outcome()
        val availableHeight = layout.primaryActionStack.height - outcomeBodyTopPaddingY
        return (availableHeight / outcomeBodyStepY).toInt().coerceAtLeast(1)
    }

    fun truncate(
        text: String,
        maxChars: Int,
    ): String {
        if (maxChars <= 0) {
            return ""
        }
        if (text.length <= maxChars) {
            return text
        }
        if (maxChars == 1) {
            return "…"
        }
        return text.take(maxChars - 1) + "…"
    }

    private fun validationSingleColumnCapacity(): Int {
        val layout = validationSetup()
        val availableHeight = layout.primaryActionStack.height - VALIDATION_ENTRY_TOP_PADDING_Y * 2f
        return (availableHeight / validationEntryMinStepY).toInt().coerceAtLeast(1)
    }
}

private fun ScreenPanelBounds.sameBounds(other: ScreenPanelBounds): Boolean =
    x == other.x &&
        y == other.y &&
        width == other.width &&
        height == other.height
