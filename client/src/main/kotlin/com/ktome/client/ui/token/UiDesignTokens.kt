package com.ktome.client.ui.token

import com.badlogic.gdx.graphics.Color

internal data class UiColorToken(
    val hex: String,
    val alpha: Float = 1f,
) {
    init {
        require(hex.matches(Regex("[0-9A-Fa-f]{6}"))) { "Color token must be a six-digit RGB hex value." }
        require(alpha in 0f..1f) { "Color token alpha must be in 0..1." }
    }

    private val cachedColor: Color = Color.valueOf(hex).also { color -> color.a = alpha }

    fun color(): Color = Color(cachedColor)

    fun hexString(): String = "#${hex.uppercase()}"
}

internal data class UiTextColors(
    val primary: UiColorToken,
    val secondary: UiColorToken,
    val disabled: UiColorToken,
)

internal data class UiSurfaceColors(
    val base: UiColorToken,
    val baseDim: UiColorToken,
    val raised: UiColorToken,
    val overlay: UiColorToken,
)

internal data class UiFocusColors(
    val ring: UiColorToken,
)

internal data class UiQualityColors(
    val normal: UiColorToken,
    val magic: UiColorToken,
    val rare: UiColorToken,
)

internal data class UiSpecialAccentColors(
    val unique: UiColorToken,
    val artifact: UiColorToken,
)

internal data class UiTelegraphColors(
    val low: UiColorToken,
    val moderate: UiColorToken,
    val high: UiColorToken,
    val lethal: UiColorToken,
) {
    fun forDangerLevel(dangerLevel: Int): UiColorToken =
        when {
            dangerLevel >= 4 -> lethal
            dangerLevel == 3 -> high
            dangerLevel == 2 -> moderate
            else -> low
        }
}

internal data class UiStatusBadgeColors(
    val stack: UiColorToken,
    val turns: UiColorToken,
    val cap: UiColorToken,
)

internal data class UiStatusColors(
    val buffAccent: UiColorToken,
    val debuffAccent: UiColorToken,
    val neutralAccent: UiColorToken,
    val badge: UiStatusBadgeColors,
)

internal data class UiBorderColors(
    val subtle: UiColorToken,
    val strong: UiColorToken,
)

internal data class UiSlotColors(
    val empty: UiColorToken,
    val filled: UiColorToken,
    val selected: UiColorToken,
)

internal data class UiBarColors(
    val hp: UiColorToken,
    val resource: UiColorToken,
    val secondaryResource: UiColorToken,
    val experience: UiColorToken,
    val background: UiColorToken,
)

internal data class UiTalentToneColors(
    val locked: UiColorToken,
    val learnable: UiColorToken,
    val reserve: UiColorToken,
    val active: UiColorToken,
)

internal data class UiMenuSelectionColors(
    val focused: UiColorToken,
    val disabled: UiColorToken,
    val normal: UiColorToken,
)

internal data class UiMenuColors(
    val selection: UiMenuSelectionColors,
)

internal data class UiDesignColors(
    val text: UiTextColors,
    val surface: UiSurfaceColors,
    val focus: UiFocusColors,
    val quality: UiQualityColors,
    val accent: UiSpecialAccentColors,
    val telegraph: UiTelegraphColors,
    val status: UiStatusColors,
    val border: UiBorderColors,
    val slot: UiSlotColors,
    val bar: UiBarColors,
    val talent: UiTalentToneColors,
    val menu: UiMenuColors,
)

internal data class UiSpacingTokens(
    val xs: Float,
    val sm: Float,
    val md: Float,
    val lg: Float,
    val xl: Float,
)

internal data class UiTypographyTokens(
    val ui: Int,
    val body: Int,
    val caption: Int,
    val title: Int,
)

internal data class UiAlphaTokens(
    val disabled: Float,
    val overlayDim: Float,
    val glass: Float,
)

internal data class UiRadiusTokens(
    val sm: Float,
    val md: Float,
    val lg: Float,
)

internal data class UiStrokeTokens(
    val thin: Float,
    val medium: Float,
    val thick: Float,
)

internal data class UiMotionTokens(
    val fastMs: Int,
    val mediumMs: Int,
    val slowMs: Int,
)

internal data class UiFixedDimensionTokens(
    val standaloneWidth: Float,
    val standaloneHeight: Float,
    val standaloneContentWidth: Float,
    val actionStackWidth: Float,
    val actionStackHeight: Float,
    val playerCreationSectionWidth: Float,
    val playerCreationSectionHeight: Float,
    val validationListWidth: Float,
    val validationListHeight: Float,
    val shellLeftRailMinWidth: Float,
    val shellRightPanelMinWidth: Float,
    val shellBottomHudHeight: Float,
    val shellPreferredWorldWidth: Float,
    val shellPreferredWorldHeight: Float,
    val shellMinWorldWidth: Float,
    val shellMinWorldHeight: Float,
    val cellSize: Int,
    val deadzoneHorizontalMinCells: Int,
    val deadzoneVerticalMinCells: Int,
    val deadzoneHorizontalRatio: Float,
    val deadzoneVerticalRatio: Float,
    val tooltipMaxWidth: Float,
    val tooltipMaxHeight: Float,
    val tooltipPadding: Float,
    val tooltipFlipMargin: Float,
    val modalMaxWidthCap: Float,
    val modalPadding: Float,
    val modalBackdropAlpha: Float,
    val rightPanelSectionCount: Int,
    val bottomHudSlotCount: Int,
)

internal object UiDesignTokens {
    private val textPrimary = UiColorToken("DDDDDD")
    private val textSecondary = UiColorToken("AAAAAA")
    private val textDisabled = UiColorToken("777777")
    private val menuDisabled = UiColorToken("777777", 0.48f)
    private val focusRing = UiColorToken("33CCDD")
    private val qualityMagic = UiColorToken("5C90D2")
    private val qualityRare = UiColorToken("CCAA33")
    private val uniqueAccent = UiColorToken("7B1FA2")

    val color: UiDesignColors =
        UiDesignColors(
            text =
                UiTextColors(
                    primary = textPrimary,
                    secondary = textSecondary,
                    disabled = textDisabled,
                ),
            surface =
                UiSurfaceColors(
                    base = UiColorToken("0B0D12"),
                    baseDim = UiColorToken("0B0D12", 0.82f),
                    raised = UiColorToken("171B24", 0.96f),
                    overlay = UiColorToken("101016", 0.93f),
                ),
            focus = UiFocusColors(ring = focusRing),
            quality =
                UiQualityColors(
                    normal = textPrimary,
                    magic = qualityMagic,
                    rare = qualityRare,
                ),
            accent =
                UiSpecialAccentColors(
                    unique = uniqueAccent,
                    artifact = UiColorToken("E6B84A"),
                ),
            telegraph =
                UiTelegraphColors(
                    low = UiColorToken("3A86FF"),
                    moderate = UiColorToken("F6C445"),
                    high = UiColorToken("E53935"),
                    lethal = uniqueAccent,
                ),
            status =
                UiStatusColors(
                    buffAccent = UiColorToken("1F6A3B", 0.78f),
                    debuffAccent = UiColorToken("7A2B25", 0.80f),
                    neutralAccent = UiColorToken("3A4353", 0.72f),
                    badge =
                        UiStatusBadgeColors(
                            stack = UiColorToken("7FE0A0"),
                            turns = UiColorToken("FF9A8D"),
                            cap = UiColorToken("D8DEE9"),
                        ),
                ),
            border =
                UiBorderColors(
                    subtle = UiColorToken("2B3342", 0.86f),
                    strong = UiColorToken("53627A", 0.94f),
                ),
            slot =
                UiSlotColors(
                    empty = UiColorToken("121620", 0.92f),
                    filled = UiColorToken("202837", 0.95f),
                    selected = focusRing,
                ),
            bar =
                UiBarColors(
                    hp = UiColorToken("C84646"),
                    resource = UiColorToken("4EA1D3"),
                    secondaryResource = UiColorToken("7FE0A0"),
                    experience = UiColorToken("C9A646"),
                    background = UiColorToken("080A0F", 0.88f),
                ),
            talent =
                UiTalentToneColors(
                    locked = UiColorToken("59616C"),
                    learnable = UiColorToken("1CB7C8"),
                    reserve = UiColorToken("D99A2B"),
                    active = UiColorToken("52C989"),
                ),
            menu =
                UiMenuColors(
                    selection =
                        UiMenuSelectionColors(
                            focused = focusRing,
                            disabled = menuDisabled,
                            normal = textPrimary,
                        ),
                ),
        )

    val spacing: UiSpacingTokens =
        UiSpacingTokens(
            xs = 4f,
            sm = 8f,
            md = 12f,
            lg = 18f,
            xl = 24f,
        )

    val typography: UiTypographyTokens =
        UiTypographyTokens(
            ui = 20,
            body = 16,
            caption = 14,
            title = 32,
        )

    val alpha: UiAlphaTokens =
        UiAlphaTokens(
            disabled = 0.48f,
            overlayDim = 0.82f,
            glass = 0.93f,
        )

    val radius: UiRadiusTokens =
        UiRadiusTokens(
            sm = 4f,
            md = 6f,
            lg = 8f,
        )

    val stroke: UiStrokeTokens =
        UiStrokeTokens(
            thin = 1f,
            medium = 2f,
            thick = 3f,
        )

    val motion: UiMotionTokens =
        UiMotionTokens(
            fastMs = 90,
            mediumMs = 140,
            slowMs = 220,
        )

    val fixed: UiFixedDimensionTokens =
        UiFixedDimensionTokens(
            standaloneWidth = 960f,
            standaloneHeight = 540f,
            standaloneContentWidth = 720f,
            actionStackWidth = 320f,
            actionStackHeight = 176f,
            playerCreationSectionWidth = 342f,
            playerCreationSectionHeight = 124f,
            validationListWidth = 720f,
            validationListHeight = 292f,
            shellLeftRailMinWidth = 184f,
            shellRightPanelMinWidth = 240f,
            shellBottomHudHeight = 224f,
            shellPreferredWorldWidth = 1280f,
            shellPreferredWorldHeight = 800f,
            shellMinWorldWidth = 1024f,
            shellMinWorldHeight = 768f,
            cellSize = 32,
            deadzoneHorizontalMinCells = 4,
            deadzoneVerticalMinCells = 3,
            deadzoneHorizontalRatio = 0.25f,
            deadzoneVerticalRatio = 0.25f,
            tooltipMaxWidth = 360f,
            tooltipMaxHeight = 200f,
            tooltipPadding = 8f,
            tooltipFlipMargin = 4f,
            modalMaxWidthCap = 640f,
            modalPadding = 18f,
            modalBackdropAlpha = 0.55f,
            rightPanelSectionCount = 4,
            bottomHudSlotCount = 4,
        )
}
