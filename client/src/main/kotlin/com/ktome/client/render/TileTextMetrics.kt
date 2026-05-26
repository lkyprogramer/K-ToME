package com.ktome.client.render

internal data class TileTextLine(
    val text: String,
    val tone: TileTextTone = TileTextTone.WHITE,
)

internal object TileTextMetrics {
    fun approximateCharWidth(style: TileTextStyle): Float =
        when (style) {
            TileTextStyle.TITLE -> 22f
            TileTextStyle.UI -> 15f
            TileTextStyle.SMALL -> 12f
            TileTextStyle.CAPTION -> 10f
        }

    fun approximateLineHeight(style: TileTextStyle): Float =
        when (style) {
            TileTextStyle.TITLE -> 38f
            TileTextStyle.UI -> 24f
            TileTextStyle.SMALL -> 19f
            TileTextStyle.CAPTION -> 16f
        }

    fun maxCharsForWidth(
        maxWidth: Float,
        style: TileTextStyle,
    ): Int = (maxWidth / approximateCharWidth(style)).toInt().coerceAtLeast(1)

    fun approximateTextWidth(
        text: String,
        style: TileTextStyle,
    ): Float = text.sumOf { char -> approximateCharWidth(style, char).toDouble() }.toFloat()

    fun truncateTextToWidth(
        text: String,
        maxWidth: Float,
        style: TileTextStyle,
    ): String {
        if (maxWidth <= 0f) {
            return ""
        }
        if (approximateTextWidth(text, style) <= maxWidth) {
            return text
        }
        val ellipsis = "…"
        val ellipsisWidth = approximateTextWidth(ellipsis, style)
        if (ellipsisWidth > maxWidth) {
            return ""
        }
        var width = 0f
        val builder = StringBuilder()
        for (char in text) {
            val nextWidth = approximateCharWidth(style, char)
            if (width + nextWidth + ellipsisWidth > maxWidth) {
                break
            }
            builder.append(char)
            width += nextWidth
        }
        return builder.append(ellipsis).toString()
    }

    fun truncateText(
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

    fun wrapText(
        text: String,
        firstLineMaxChars: Int,
        continuationMaxChars: Int,
    ): List<String> {
        val lines = mutableListOf<String>()
        var remaining = text
        var maxChars = firstLineMaxChars.coerceAtLeast(1)
        while (remaining.length > maxChars) {
            val splitIndex = wrapSplitIndex(remaining, maxChars)
            lines += remaining.take(splitIndex).trimEnd()
            remaining = remaining.drop(splitIndex).trimStart()
            maxChars = continuationMaxChars.coerceAtLeast(1)
        }
        if (remaining.isNotBlank()) {
            lines += remaining
        }
        return lines.ifEmpty { listOf("") }
    }

    fun wrapTextToWidth(
        text: String,
        maxWidth: Float,
        style: TileTextStyle,
    ): List<String> {
        if (text.isBlank()) {
            return listOf("")
        }
        val lines = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty()) {
            val splitIndex = wrapSplitIndexForWidth(remaining, maxWidth, style)
            lines += remaining.take(splitIndex).trimEnd()
            remaining = remaining.drop(splitIndex).trimStart()
        }
        return lines.ifEmpty { listOf("") }
    }

    private fun wrapSplitIndex(
        text: String,
        maxChars: Int,
    ): Int {
        val searchEnd = (maxChars + 1).coerceAtMost(text.length)
        val whitespaceSplit = text.take(searchEnd).indexOfLast(Char::isWhitespace)
        return if (whitespaceSplit > 0) {
            whitespaceSplit
        } else {
            maxChars.coerceAtLeast(1)
        }
    }

    private fun wrapSplitIndexForWidth(
        text: String,
        maxWidth: Float,
        style: TileTextStyle,
    ): Int {
        var width = 0f
        var index = 0
        var lastWhitespaceIndex = -1
        while (index < text.length) {
            val char = text[index]
            val nextWidth = approximateCharWidth(style, char)
            if (width + nextWidth > maxWidth) {
                return if (lastWhitespaceIndex > 0) lastWhitespaceIndex + 1 else index.coerceAtLeast(1)
            }
            width += nextWidth
            if (char.isWhitespace()) {
                lastWhitespaceIndex = index
            }
            index += 1
        }
        return text.length
    }

    private fun approximateCharWidth(
        style: TileTextStyle,
        char: Char,
    ): Float {
        val base = approximateCharWidth(style)
        return when {
            char.isWhitespace() -> base * 0.4f
            char.code < 0x80 -> base * 0.55f
            isWideGlyph(char) -> base * 1.25f
            else -> base
        }
    }

    private fun isWideGlyph(char: Char): Boolean =
        char.code in 0x2E80..0xA4CF ||
            char.code in 0xAC00..0xD7AF ||
            char.code in 0xF900..0xFAFF ||
            char.code in 0xFE10..0xFE19 ||
            char.code in 0xFE30..0xFE6F ||
            char.code in 0xFF00..0xFF60 ||
            char.code in 0xFFE0..0xFFE6
}
