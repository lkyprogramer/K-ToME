package com.ktome.client.render.layout

internal data class InventoryWorkbenchLayoutRequest(
    val viewportWidth: Int,
    val viewportHeight: Int,
)

internal data class InventoryWorkbenchLayout(
    val root: GameShellBounds,
    val content: GameShellBounds,
    val equipmentColumn: GameShellBounds,
    val backpackColumn: GameShellBounds,
    val detailColumn: GameShellBounds,
    val footer: GameShellBounds,
    val equipmentSlotBounds: List<GameShellBounds>,
    val backpackCellBounds: List<GameShellBounds>,
    val detailMaxLines: Int,
)

internal object InventoryWorkbenchLayoutSolver {
    private const val gridColumns = 6
    private const val gridRows = 4

    fun resolve(request: InventoryWorkbenchLayoutRequest): InventoryWorkbenchLayout {
        val profile = InventoryWorkbenchLayoutProfile.forViewport(request.viewportWidth, request.viewportHeight)
        val rootLeft = if (profile.compact) 62f else 84f
        val rootRightPadding = if (profile.compact) 22f else 28f
        val rootBottom = if (profile.compact) 104f else 126f
        val rootTopPadding = if (profile.compact) 18f else 26f
        val root =
            GameShellBounds(
                x = rootLeft,
                y = rootBottom,
                width = (request.viewportWidth.toFloat() - rootLeft - rootRightPadding).coerceAtLeast(0f),
                height = (request.viewportHeight.toFloat() - rootBottom - rootTopPadding).coerceAtLeast(0f),
            )
        val padding = if (profile.compact) 22f else 30f
        val gutter = profile.gutter
        val content =
            GameShellBounds(
                x = root.x + padding,
                y = root.y + padding + profile.footerHeight + gutter,
                width = (root.width - padding * 2f).coerceAtLeast(0f),
                height = (root.height - padding * 2f - profile.footerHeight - gutter).coerceAtLeast(0f),
            )
        val columnGutter = minOf(gutter, content.width / 2f).coerceAtLeast(0f)
        val availableColumnsWidth = (content.width - columnGutter * 2f).coerceAtLeast(0f)
        val minColumnsWidth = profile.equipmentMinWidth + profile.backpackMinWidth + profile.detailMinWidth
        val (equipmentWidth, backpackWidth, detailWidth) =
            if (availableColumnsWidth < minColumnsWidth) {
                val scale = (availableColumnsWidth / minColumnsWidth).coerceIn(0f, 1f)
                Triple(
                    profile.equipmentMinWidth * scale,
                    profile.backpackMinWidth * scale,
                    profile.detailMinWidth * scale,
                )
            } else {
                var equipment = (content.width * 0.24f).coerceIn(profile.equipmentMinWidth, 330f)
                var backpack = (content.width * 0.36f).coerceIn(profile.backpackMinWidth, 540f)
                val detail = (availableColumnsWidth - equipment - backpack).coerceAtLeast(profile.detailMinWidth)
                var overflow = equipment + backpack + detail - availableColumnsWidth
                if (overflow > 0f) {
                    val backpackReduction = minOf(overflow, backpack - profile.backpackMinWidth)
                    backpack -= backpackReduction
                    overflow -= backpackReduction
                }
                if (overflow > 0f) {
                    val equipmentReduction = minOf(overflow, equipment - profile.equipmentMinWidth)
                    equipment -= equipmentReduction
                }
                Triple(equipment, backpack, detail)
            }
        val equipmentColumn = GameShellBounds(content.x, content.y, equipmentWidth, content.height)
        val backpackColumn = GameShellBounds(equipmentColumn.right + columnGutter, content.y, backpackWidth, content.height)
        val detailColumn = GameShellBounds(backpackColumn.right + columnGutter, content.y, detailWidth, content.height)
        val footer =
            GameShellBounds(
                x = content.x,
                y = root.y + padding,
                width = content.width,
                height = profile.footerHeight,
            )
        return InventoryWorkbenchLayout(
            root = root,
            content = content,
            equipmentColumn = equipmentColumn,
            backpackColumn = backpackColumn,
            detailColumn = detailColumn,
            footer = footer,
            equipmentSlotBounds = equipmentSlotBounds(equipmentColumn, profile),
            backpackCellBounds = backpackCellBounds(backpackColumn, profile),
            detailMaxLines = profile.detailMaxLines,
        )
    }

    private fun equipmentSlotBounds(
        column: GameShellBounds,
        profile: InventoryWorkbenchLayoutProfile,
    ): List<GameShellBounds> {
        val nominalTopReserve = if (profile.compact) 62f else 72f
        val gap = minOf(if (profile.compact) 10f else 12f, column.width / 4f).coerceAtLeast(0f)
        val sidePadding = minOf(24f, (column.width - gap * 2f).coerceAtLeast(0f))
        val maxSlotSize = ((column.width - gap * 2f - sidePadding) / 3f).coerceAtLeast(0f)
        val minSlotSize = if (profile.compact) 50f else 58f
        val slotSize =
            if (maxSlotSize >= minSlotSize) {
                maxSlotSize.coerceAtMost(if (profile.compact) 58f else 72f)
            } else {
                maxSlotSize
            }
        val gridWidth = slotSize * 3f + gap * 2f
        val gridHeight = slotSize * 3f + gap * 2f
        val topReserve = minOf(nominalTopReserve, (column.height - gridHeight).coerceAtLeast(0f))
        val startX = column.x + (column.width - gridWidth) / 2f
        val startY = column.top - topReserve - slotSize
        return (0 until 9).map { index ->
            val row = index / 3
            val columnIndex = index % 3
            GameShellBounds(
                x = startX + columnIndex * (slotSize + gap),
                y = startY - row * (slotSize + gap),
                width = slotSize,
                height = slotSize,
            )
        }
    }

    private fun backpackCellBounds(
        column: GameShellBounds,
        profile: InventoryWorkbenchLayoutProfile,
    ): List<GameShellBounds> {
        val nominalTopReserve = if (profile.compact) 74f else 82f
        val horizontalGap = minOf(if (profile.compact) 8f else 10f, column.width / gridColumns.toFloat()).coerceAtLeast(0f)
        val verticalGap = minOf(if (profile.compact) 8f else 10f, column.height / gridRows.toFloat()).coerceAtLeast(0f)
        val sidePadding = minOf(20f, (column.width - horizontalGap * (gridColumns - 1)).coerceAtLeast(0f))
        val bottomReserve = minOf(74f, (column.height - verticalGap * (gridRows - 1)).coerceAtLeast(0f))
        val maxSlotByWidth = ((column.width - horizontalGap * (gridColumns - 1) - sidePadding) / gridColumns).coerceAtLeast(0f)
        val maxSlotByHeight = ((column.height - nominalTopReserve - verticalGap * (gridRows - 1) - bottomReserve) / gridRows).coerceAtLeast(0f)
        val maxSlotSize = minOf(maxSlotByWidth, maxSlotByHeight)
        val slotSize =
            if (maxSlotSize >= profile.cellMinSize) {
                maxSlotSize.coerceAtMost(profile.cellMaxSize)
            } else {
                maxSlotSize
            }
        val gridWidth = slotSize * gridColumns + horizontalGap * (gridColumns - 1)
        val gridHeight = slotSize * gridRows + verticalGap * (gridRows - 1)
        val topReserve = minOf(nominalTopReserve, (column.height - gridHeight).coerceAtLeast(0f))
        val startX = column.x + (column.width - gridWidth) / 2f
        val startY = column.top - topReserve - slotSize
        return (0 until gridColumns * gridRows).map { index ->
            val row = index / gridColumns
            val columnIndex = index % gridColumns
            GameShellBounds(
                x = startX + columnIndex * (slotSize + horizontalGap),
                y = startY - row * (slotSize + verticalGap),
                width = slotSize,
                height = slotSize,
            )
        }
    }

    private data class InventoryWorkbenchLayoutProfile(
        val compact: Boolean,
        val equipmentMinWidth: Float,
        val backpackMinWidth: Float,
        val detailMinWidth: Float,
        val gutter: Float,
        val footerHeight: Float,
        val cellMinSize: Float,
        val cellMaxSize: Float,
        val detailMaxLines: Int,
    ) {
        companion object {
            fun forViewport(
                viewportWidth: Int,
                viewportHeight: Int,
            ): InventoryWorkbenchLayoutProfile =
                when {
                    viewportWidth >= 1600 && viewportHeight >= 900 ->
                        InventoryWorkbenchLayoutProfile(
                            compact = false,
                            equipmentMinWidth = 300f,
                            backpackMinWidth = 520f,
                            detailMinWidth = 400f,
                            gutter = 16f,
                            footerHeight = 64f,
                            cellMinSize = 64f,
                            cellMaxSize = 72f,
                            detailMaxLines = 7,
                        )

                    viewportWidth >= 1200 ->
                        InventoryWorkbenchLayoutProfile(
                            compact = true,
                            equipmentMinWidth = 240f,
                            backpackMinWidth = 440f,
                            detailMinWidth = 320f,
                            gutter = 14f,
                            footerHeight = 60f,
                            cellMinSize = 58f,
                            cellMaxSize = 64f,
                            detailMaxLines = 6,
                        )

                    else ->
                        InventoryWorkbenchLayoutProfile(
                            compact = true,
                            equipmentMinWidth = 190f,
                            backpackMinWidth = 384f,
                            detailMinWidth = 280f,
                            gutter = 10f,
                            footerHeight = 56f,
                            cellMinSize = 52f,
                            cellMaxSize = 56f,
                            detailMaxLines = 5,
                        )
                }
        }
    }
}
