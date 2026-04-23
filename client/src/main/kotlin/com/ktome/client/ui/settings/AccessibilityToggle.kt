package com.ktome.client.ui.settings

internal enum class AccessibilityDistinctionMethod {
    COLOR,
    SHAPE,
    MOTION,
    BADGE,
}

internal data class AccessibilityToggle(
    val highContrast: Boolean = false,
    val colorBlindSafe: Boolean = false,
    val reduceMotion: Boolean = false,
) {
    val distinctionMethods: Set<AccessibilityDistinctionMethod> =
        buildSet {
            if (!colorBlindSafe) {
                add(AccessibilityDistinctionMethod.COLOR)
            }
            add(AccessibilityDistinctionMethod.SHAPE)
            if (!reduceMotion) {
                add(AccessibilityDistinctionMethod.MOTION)
            }
            add(AccessibilityDistinctionMethod.BADGE)
        }

    val enabledLabelKeys: List<String> =
        buildList {
            if (highContrast) {
                add("ui.accessibility.high-contrast")
            }
            if (colorBlindSafe) {
                add("ui.accessibility.color-blind-safe")
            }
            if (reduceMotion) {
                add("ui.accessibility.reduce-motion")
            }
        }

    init {
        require(AccessibilityDistinctionMethod.SHAPE in distinctionMethods) {
            "Accessibility distinction must not depend on color alone."
        }
        require(AccessibilityDistinctionMethod.BADGE in distinctionMethods) {
            "Accessibility distinction must keep badge fallback available."
        }
    }

    fun overlayAlpha(baseAlpha: Float): Float =
        when {
            highContrast -> baseAlpha.coerceAtLeast(0.82f)
            reduceMotion -> baseAlpha.coerceAtLeast(0.74f)
            else -> baseAlpha
        }

    fun riskCueBadge(dangerLevel: Int): String? {
        if (dangerLevel <= 0) {
            return null
        }
        if (!colorBlindSafe && !reduceMotion) {
            return null
        }
        return when {
            dangerLevel >= 4 -> "!!!!"
            dangerLevel == 3 -> "!!!"
            dangerLevel == 2 -> "!!"
            else -> "!"
        }
    }

    companion object {
        fun fromSystemProperties(readProperty: (String) -> String? = System::getProperty): AccessibilityToggle =
            AccessibilityToggle(
                highContrast = readProperty("ktome.ui.a11y.highContrast").toBooleanStrictOrFalse(),
                colorBlindSafe = readProperty("ktome.ui.a11y.colorBlindSafe").toBooleanStrictOrFalse(),
                reduceMotion = readProperty("ktome.ui.a11y.reduceMotion").toBooleanStrictOrFalse(),
            )

        private fun String?.toBooleanStrictOrFalse(): Boolean = this?.equals("true", ignoreCase = true) == true
    }
}
