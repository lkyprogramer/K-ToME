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
            ui = 28,
            body = 24,
            caption = 20,
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
}
