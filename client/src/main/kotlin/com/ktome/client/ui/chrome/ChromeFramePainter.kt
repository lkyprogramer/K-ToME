package com.ktome.client.ui.chrome

import com.badlogic.gdx.graphics.Color
import com.ktome.client.assets.ResolvedVisualAsset

internal data class ChromeFrameBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = x + width
    val top: Float get() = y + height
}

internal data class ChromeFrameInsets(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
)

internal data class ChromeFrameAssets(
    val body: ResolvedVisualAsset,
    val cornerTopLeft: ResolvedVisualAsset,
    val cornerTopRight: ResolvedVisualAsset,
    val cornerBottomLeft: ResolvedVisualAsset,
    val cornerBottomRight: ResolvedVisualAsset,
    val edgeTop: ResolvedVisualAsset,
    val edgeRight: ResolvedVisualAsset,
    val edgeBottom: ResolvedVisualAsset,
    val edgeLeft: ResolvedVisualAsset,
)

internal data class ChromeFrameAssetDraw(
    val asset: ResolvedVisualAsset,
    val bounds: ChromeFrameBounds,
    val alpha: Float,
)

internal data class ChromeFrameRectDraw(
    val bounds: ChromeFrameBounds,
    val color: Color,
)

internal interface ChromeFrameDrawSink {
    fun drawRect(draw: ChromeFrameRectDraw)

    fun drawAsset(draw: ChromeFrameAssetDraw)
}

internal data class ChromeFrameDrawRequest(
    val bounds: ChromeFrameBounds,
    val assets: ChromeFrameAssets,
    val fillColor: Color,
    val borderColor: Color,
    val alpha: Float = 0.86f,
)

internal enum class ChromeSurfaceKind {
    Panel,
    Slot,
    Tooltip,
    Modal,
    FooterHint,
}

internal object ChromeFramePainter {
    internal const val frameEdgeSize: Float = 18f
    private const val MIN_INNER_SIZE = 1f

    fun contentInsets(kind: ChromeSurfaceKind): ChromeFrameInsets =
        when (kind) {
            ChromeSurfaceKind.Panel ->
                ChromeFrameInsets(
                    left = 22f,
                    right = 22f,
                    top = 22f,
                    bottom = 18f,
                )

            ChromeSurfaceKind.Slot ->
                ChromeFrameInsets(
                    left = 10f,
                    right = 10f,
                    top = 10f,
                    bottom = 10f,
                )

            ChromeSurfaceKind.Tooltip ->
                ChromeFrameInsets(
                    left = 22f,
                    right = 22f,
                    top = 22f,
                    bottom = 18f,
                )

            ChromeSurfaceKind.Modal ->
                ChromeFrameInsets(
                    left = 24f,
                    right = 24f,
                    top = 24f,
                    bottom = 20f,
                )

            ChromeSurfaceKind.FooterHint ->
                ChromeFrameInsets(
                    left = 12f,
                    right = 12f,
                    top = 8f,
                    bottom = 8f,
                )
        }

    fun contentBounds(
        bounds: ChromeFrameBounds,
        kind: ChromeSurfaceKind,
    ): ChromeFrameBounds {
        val insets = contentInsets(kind)
        val width = (bounds.width - insets.left - insets.right).coerceAtLeast(MIN_INNER_SIZE)
        val height = (bounds.height - insets.top - insets.bottom).coerceAtLeast(MIN_INNER_SIZE)
        return ChromeFrameBounds(
            x = bounds.x + insets.left,
            y = bounds.y + insets.bottom,
            width = width,
            height = height,
        )
    }

    fun drawFrame(
        sink: ChromeFrameDrawSink,
        request: ChromeFrameDrawRequest,
    ) {
        val bounds = request.bounds
        sink.drawRect(ChromeFrameRectDraw(bounds = bounds, color = request.fillColor))
        if (bounds.width <= 0f || bounds.height <= 0f) {
            return
        }

        val edge = frameEdgeSize(bounds)
        val bodyBounds =
            ChromeFrameBounds(
                x = bounds.x + edge,
                y = bounds.y + edge,
                width = (bounds.width - edge * 2f).coerceAtLeast(MIN_INNER_SIZE),
                height = (bounds.height - edge * 2f).coerceAtLeast(MIN_INNER_SIZE),
            )
        drawAsset(sink, request.assets.body, bodyBounds, request.alpha)
        drawAsset(sink, request.assets.edgeTop, ChromeFrameBounds(bounds.x + edge, bounds.top - edge, bodyBounds.width, edge), request.alpha)
        drawAsset(sink, request.assets.edgeBottom, ChromeFrameBounds(bounds.x + edge, bounds.y, bodyBounds.width, edge), request.alpha)
        drawAsset(sink, request.assets.edgeLeft, ChromeFrameBounds(bounds.x, bounds.y + edge, edge, bodyBounds.height), request.alpha)
        drawAsset(sink, request.assets.edgeRight, ChromeFrameBounds(bounds.right - edge, bounds.y + edge, edge, bodyBounds.height), request.alpha)
        drawAsset(sink, request.assets.cornerTopLeft, ChromeFrameBounds(bounds.x, bounds.top - edge, edge, edge), request.alpha)
        drawAsset(sink, request.assets.cornerTopRight, ChromeFrameBounds(bounds.right - edge, bounds.top - edge, edge, edge), request.alpha)
        drawAsset(sink, request.assets.cornerBottomLeft, ChromeFrameBounds(bounds.x, bounds.y, edge, edge), request.alpha)
        drawAsset(sink, request.assets.cornerBottomRight, ChromeFrameBounds(bounds.right - edge, bounds.y, edge, edge), request.alpha)
        drawOutline(sink, bounds, request.borderColor)
    }

    private fun frameEdgeSize(bounds: ChromeFrameBounds): Float =
        minOf(frameEdgeSize, bounds.width / 3f, bounds.height / 3f).coerceAtLeast(4f)

    private fun drawAsset(
        sink: ChromeFrameDrawSink,
        asset: ResolvedVisualAsset,
        bounds: ChromeFrameBounds,
        alpha: Float,
    ) {
        sink.drawAsset(ChromeFrameAssetDraw(asset = asset, bounds = bounds, alpha = alpha))
    }

    private fun drawOutline(
        sink: ChromeFrameDrawSink,
        bounds: ChromeFrameBounds,
        color: Color,
    ) {
        val stroke = 1f
        sink.drawRect(ChromeFrameRectDraw(ChromeFrameBounds(bounds.x, bounds.y, bounds.width, stroke), color))
        sink.drawRect(ChromeFrameRectDraw(ChromeFrameBounds(bounds.x, bounds.top - stroke, bounds.width, stroke), color))
        sink.drawRect(ChromeFrameRectDraw(ChromeFrameBounds(bounds.x, bounds.y, stroke, bounds.height), color))
        sink.drawRect(ChromeFrameRectDraw(ChromeFrameBounds(bounds.right - stroke, bounds.y, stroke, bounds.height), color))
    }
}
