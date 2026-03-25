package com.ktome.client.telegraph

internal object TelegraphStyle {
    fun overlayAlpha(dangerLevel: Int): Float = TelegraphRenderer.alpha(dangerLevel)

    fun fallbackColorHex(dangerLevel: Int): String = TelegraphRenderer.fallbackColorHex(dangerLevel)
}
