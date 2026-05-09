package com.ktome.client.render

internal data class TileTextLine(
    val text: String,
    val tone: TileTextTone = TileTextTone.WHITE,
)

internal object TileTextMetrics {
    fun approximateCharWidth(style: TileTextStyle): Float =
        when (style) {
            TileTextStyle.UI -> 18f
            TileTextStyle.SMALL -> 16f
        }

    fun approximateLineHeight(style: TileTextStyle): Float =
        when (style) {
            TileTextStyle.UI -> 30f
            TileTextStyle.SMALL -> 24f
        }

    fun maxCharsForWidth(
        maxWidth: Float,
        style: TileTextStyle,
    ): Int = (maxWidth / approximateCharWidth(style)).toInt().coerceAtLeast(1)

    fun truncateTextToWidth(
        text: String,
        maxWidth: Float,
        style: TileTextStyle,
    ): String = truncateText(text, maxCharsForWidth(maxWidth, style))

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
}
