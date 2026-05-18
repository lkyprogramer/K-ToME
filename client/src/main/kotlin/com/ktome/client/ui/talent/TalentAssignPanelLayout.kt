package com.ktome.client.ui.talent

import com.ktome.client.ui.chrome.ChromeFrameBounds
import com.ktome.client.ui.chrome.ChromeFramePainter
import com.ktome.client.ui.chrome.ChromeSurfaceKind

internal data class TalentAssignPanelModalBoundsRequest(
    val viewportWidth: Float,
    val viewportHeight: Float,
)

internal data class TalentAssignPanelLayoutRequest(
    val modalBounds: ChromeFrameBounds,
)

internal data class TalentAssignPanelLayoutModel(
    val modal: TalentAssignModalLayout,
    val header: TalentAssignHeaderLayout,
    val body: TalentAssignBodyLayout,
    val footer: TalentAssignFooterLayout,
)

internal data class TalentAssignModalLayout(
    val frameBounds: ChromeFrameBounds,
    val contentBounds: ChromeFrameBounds,
)

internal data class TalentAssignHeaderLayout(
    val textX: Float,
    val titleBaseline: Float,
    val pointsBaseline: Float,
    val pointsWidth: Float,
)

internal data class TalentAssignBodyLayout(
    val list: TalentAssignListLayout,
    val right: TalentAssignRightLayout,
    val scrollbar: TalentAssignScrollbarLayout,
    val dividerX: Float,
)

internal data class TalentAssignListLayout(
    val bounds: ChromeFrameBounds,
    val visibleSlots: Int,
)

internal data class TalentAssignRightLayout(
    val columnBounds: ChromeFrameBounds,
    val detailBounds: ChromeFrameBounds,
    val activeSlotChoiceBounds: ChromeFrameBounds,
)

internal data class TalentAssignScrollbarLayout(
    val bounds: ChromeFrameBounds,
)

internal data class TalentAssignFooterLayout(
    val bounds: ChromeFrameBounds,
    val baseline: Float,
)

internal data class TalentAssignListViewportRequest(
    val totalSlots: Int,
    val focusedIndex: Int?,
    val visibleSlots: Int,
)

internal data class TalentAssignListViewportModel(
    val totalSlots: Int,
    val firstVisibleIndex: Int,
    val visibleSlots: Int,
) {
    val maxFirstVisibleIndex: Int = (totalSlots - visibleSlots).coerceAtLeast(0)
    val hasOverflow: Boolean = totalSlots > visibleSlots
    val endExclusiveIndex: Int = (firstVisibleIndex + visibleSlots).coerceAtMost(totalSlots)
}

internal object TalentAssignPanelLayoutSolver {
    const val rowStep: Float = 32f
    const val scrollbarArrowHeight: Float = 20f

    private const val modalHorizontalPadding = 4f
    private const val modalVerticalPadding = 24f
    private const val minModalWidth = 720f
    private const val minModalHeight = 520f
    private const val bodyBottomOffset = 46f
    private const val bodyTopOffset = 64f
    private const val bodyWidthInset = 8f
    private const val leftColumnFraction = 0.55f
    private const val minLeftColumnWidth = 280f
    private const val minRightColumnWidth = 180f
    private const val columnGap = 18f
    private const val rightDetailInset = 24f

    fun modalBounds(request: TalentAssignPanelModalBoundsRequest): ChromeFrameBounds =
        ChromeFrameBounds(
            x = modalHorizontalPadding,
            y = modalVerticalPadding,
            width = (request.viewportWidth - modalHorizontalPadding * 2f).coerceAtLeast(minModalWidth),
            height = (request.viewportHeight - modalVerticalPadding * 2f).coerceAtLeast(minModalHeight),
        )

    fun resolveForViewport(request: TalentAssignPanelModalBoundsRequest): TalentAssignPanelLayoutModel =
        resolve(TalentAssignPanelLayoutRequest(modalBounds(request)))

    fun resolve(request: TalentAssignPanelLayoutRequest): TalentAssignPanelLayoutModel {
        val modalBounds = request.modalBounds
        val contentBounds = ChromeFramePainter.contentBounds(modalBounds, ChromeSurfaceKind.Modal)
        val footerTop = contentBounds.y + bodyBottomOffset
        val bodyTop = contentBounds.top - bodyTopOffset
        val bodyWidth = contentBounds.width - bodyWidthInset
        val leftWidth = (bodyWidth * leftColumnFraction).coerceAtLeast(minLeftColumnWidth)
        val rightX = contentBounds.x + leftWidth + columnGap
        val rightWidth = (bodyWidth - leftWidth - columnGap).coerceAtLeast(minRightColumnWidth)
        val dividerX = contentBounds.x + leftWidth + columnGap / 2f
        val bodyHeight = (bodyTop - footerTop).coerceAtLeast(rowStep)
        val listBounds = ChromeFrameBounds(contentBounds.x, footerTop, leftWidth - 6f, bodyHeight)
        val rightColumnBounds = ChromeFrameBounds(rightX, footerTop, rightWidth, bodyHeight)
        val detailBottom = contentBounds.y + 34f
        val detailBounds =
            ChromeFrameBounds(
                x = rightX + rightDetailInset,
                y = detailBottom,
                width = (rightWidth - rightDetailInset * 1.5f).coerceAtLeast(1f),
                height = (bodyTop - detailBottom).coerceAtLeast(1f),
            )
        return TalentAssignPanelLayoutModel(
            modal = TalentAssignModalLayout(frameBounds = modalBounds, contentBounds = contentBounds),
            header =
                TalentAssignHeaderLayout(
                    textX = contentBounds.x + 28f,
                    titleBaseline = contentBounds.top + 18f,
                    pointsBaseline = contentBounds.top - 28f,
                    pointsWidth = contentBounds.width - 28f,
                ),
            body =
                TalentAssignBodyLayout(
                    list = TalentAssignListLayout(bounds = listBounds, visibleSlots = (bodyHeight / rowStep).toInt().coerceAtLeast(1)),
                    right =
                        TalentAssignRightLayout(
                            columnBounds = rightColumnBounds,
                            detailBounds = detailBounds,
                            activeSlotChoiceBounds = ChromeFrameBounds(rightX, footerTop + 52f, rightWidth, 190f),
                        ),
                    scrollbar = TalentAssignScrollbarLayout(bounds = ChromeFrameBounds(dividerX - 8f, footerTop, 14f, bodyHeight)),
                    dividerX = dividerX,
                ),
            footer =
                TalentAssignFooterLayout(
                    bounds = ChromeFrameBounds(contentBounds.x + 20f, contentBounds.y, contentBounds.width - 40f, 30f),
                    baseline = contentBounds.y + 13f,
                ),
        )
    }

    fun resolveListViewport(request: TalentAssignListViewportRequest): TalentAssignListViewportModel {
        val maxFirstVisibleIndex = (request.totalSlots - request.visibleSlots).coerceAtLeast(0)
        val centeredIndex = request.focusedIndex?.let { index -> index - request.visibleSlots / 2 } ?: 0
        return TalentAssignListViewportModel(
            totalSlots = request.totalSlots,
            firstVisibleIndex = centeredIndex.coerceIn(0, maxFirstVisibleIndex),
            visibleSlots = request.visibleSlots,
        )
    }
}
