package com.ktome.client.render.layout

import com.ktome.game.PLAYER_ACTIVE_TALENT_SLOT_COUNT
import kotlin.math.floor
import kotlin.math.roundToInt

internal data class DemoShellLayoutRequest(
    val viewportWidth: Int,
    val viewportHeight: Int,
    val cellSize: Int,
)

internal data class DemoSlotGridLayout(
    val bounds: GameShellBounds,
    val columns: Int,
    val rows: Int,
    val slotSide: Float,
    val slotBounds: List<GameShellBounds>,
)

internal data class DemoRightPanelLayout(
    val equipment: GameShellBounds,
    val inscriptions: GameShellBounds,
    val backpack: GameShellBounds,
    val backpackPager: GameShellBounds,
    val operationHints: GameShellBounds,
    val equipmentSlots: DemoSlotGridLayout,
    val inscriptionSlots: DemoSlotGridLayout,
    val backpackSlots: DemoSlotGridLayout,
)

internal data class DemoBottomDeckLayout(
    val bounds: GameShellBounds,
    val heroCard: GameShellBounds,
    val actionDeck: GameShellBounds,
    val actionSlotBounds: List<GameShellBounds>,
    val logDeck: GameShellBounds,
)

internal data class DemoShellLayout(
    val outerFrame: GameShellBounds,
    val navRail: GameShellBounds,
    val mapStage: GameShellBounds,
    val mapContentBounds: RectInt,
    val mapInnerPadding: InsetsInt,
    val rightPanel: GameShellBounds,
    val rightPanelLayout: DemoRightPanelLayout,
    val bottomDeck: DemoBottomDeckLayout,
    val modalSafeBounds: ModalSafeBounds,
)

internal object DemoShellLayoutSolver {
    private const val STANDARD_BREAKPOINT_WIDTH = 1200
    private const val DEMO_ASPECT_BREAKPOINT_WIDTH = 1500
    private const val RIGHT_SECTION_TITLE_HEIGHT = 18f
    private const val RIGHT_BACKPACK_PAGER_HEIGHT = 24f

    fun resolve(request: DemoShellLayoutRequest): DemoShellLayout {
        require(request.viewportWidth > 0) { "Demo shell viewport width must be positive." }
        require(request.viewportHeight > 0) { "Demo shell viewport height must be positive." }
        require(request.cellSize > 0) { "Demo shell cell size must be positive." }

        val width = request.viewportWidth.toFloat()
        val height = request.viewportHeight.toFloat()
        val profile = profileFor(request.viewportWidth, request.viewportHeight)
        val isLargeStandardViewport = profile == DemoShellProfile.STANDARD && width >= 1500f
        val navRailWidth = if (profile == DemoShellProfile.DEMO_ASPECT) 64f else 56f
        val panelGap = if (profile == DemoShellProfile.MINIMUM) 10f else 12f
        val rightPanelRatio = if (profile == DemoShellProfile.DEMO_ASPECT) 0.25f else 0.28f
        val rightPanelWidth =
            (width * rightPanelRatio).coerceIn(
                minimumValue =
                    when (profile) {
                        DemoShellProfile.MINIMUM -> 288f
                        DemoShellProfile.STANDARD -> 304f
                        DemoShellProfile.DEMO_ASPECT -> 320f
                    },
                maximumValue =
                    when {
                        profile == DemoShellProfile.DEMO_ASPECT -> 420f
                        isLargeStandardViewport -> 460f
                        else -> 360f
                    },
            )
        val bottomDeckHeight =
            when (profile) {
                DemoShellProfile.MINIMUM -> maxOf(164f, height * 0.22f)
                DemoShellProfile.STANDARD -> maxOf(if (isLargeStandardViewport) 200f else 180f, height * 0.225f)
                DemoShellProfile.DEMO_ASPECT -> maxOf(208f, height * 0.22f)
            }
        val rightPanelX = width - rightPanelWidth
        val mapStageX = navRailWidth + panelGap
        val mapStageWidth = (rightPanelX - panelGap - mapStageX).coerceAtLeast(request.cellSize.toFloat())
        val contentHeight = (height - bottomDeckHeight).coerceAtLeast(request.cellSize.toFloat())
        val mapStage = GameShellBounds(mapStageX, bottomDeckHeight, mapStageWidth, contentHeight)
        val mapInset = if (profile == DemoShellProfile.MINIMUM) 14 else 18
        val mapContent = cellAlignedBounds(mapStage, request.cellSize, mapInset)
        val rightPanel = GameShellBounds(rightPanelX, 0f, rightPanelWidth, height)
        val bottomDeck =
            bottomDeckLayout(
                bounds = GameShellBounds(0f, 0f, rightPanelX, bottomDeckHeight),
                profile = profile,
            )
        val rightPanelLayout =
            rightPanelLayout(
                bounds = rightPanel,
                profile = profile,
            )
        val modalSafeBounds =
            modalSafeBounds(
                mapStage = mapStage,
                navRailRight = navRailWidth,
                rightPanelLeft = rightPanel.x,
                bottomDeckTop = bottomDeck.bounds.top,
                viewportWidth = width,
                viewportHeight = height,
            )

        return DemoShellLayout(
            outerFrame = GameShellBounds(0f, 0f, width, height),
            navRail = GameShellBounds(0f, bottomDeckHeight, navRailWidth, contentHeight),
            mapStage = mapStage,
            mapContentBounds = mapContent,
            mapInnerPadding =
                InsetsInt(
                    left = (mapContent.x - mapStage.x).roundToInt(),
                    right = (mapStage.right - mapContent.right).roundToInt(),
                    top = (mapStage.top - mapContent.top).roundToInt(),
                    bottom = (mapContent.y - mapStage.y).roundToInt(),
                ),
            rightPanel = rightPanel,
            rightPanelLayout = rightPanelLayout,
            bottomDeck = bottomDeck,
            modalSafeBounds = modalSafeBounds,
        )
    }

    private fun profileFor(
        viewportWidth: Int,
        viewportHeight: Int,
    ): DemoShellProfile {
        val aspectRatio = viewportWidth.toFloat() / viewportHeight.toFloat()
        return when {
            viewportWidth >= DEMO_ASPECT_BREAKPOINT_WIDTH && aspectRatio >= 1.68f -> DemoShellProfile.DEMO_ASPECT
            viewportWidth < STANDARD_BREAKPOINT_WIDTH -> DemoShellProfile.MINIMUM
            else -> DemoShellProfile.STANDARD
        }
    }

    private fun cellAlignedBounds(
        bounds: GameShellBounds,
        cellSize: Int,
        inset: Int,
    ): RectInt {
        val contentX = bounds.x + inset
        val contentY = bounds.y + inset
        val contentWidth = (bounds.width - inset * 2f).coerceAtLeast(cellSize.toFloat())
        val contentHeight = (bounds.height - inset * 2f).coerceAtLeast(cellSize.toFloat())
        val alignedWidth = floor(contentWidth / cellSize).toInt().coerceAtLeast(1) * cellSize
        val alignedHeight = floor(contentHeight / cellSize).toInt().coerceAtLeast(1) * cellSize
        val alignedX = (contentX + (contentWidth - alignedWidth) / 2f).roundToInt()
        val alignedY = (contentY + (contentHeight - alignedHeight) / 2f).roundToInt()
        return RectInt(alignedX, alignedY, alignedWidth, alignedHeight)
    }

    private fun bottomDeckLayout(
        bounds: GameShellBounds,
        profile: DemoShellProfile,
    ): DemoBottomDeckLayout {
        val margin = 8f
        val sectionGap = if (profile == DemoShellProfile.DEMO_ASPECT) 8f else 6f
        val isLargeStandardBounds = profile == DemoShellProfile.STANDARD && bounds.width >= 1300f
        val contentY = margin
        val contentHeight = (bounds.height - margin * 2f).coerceAtLeast(120f)
        val heroWidth =
            when (profile) {
                DemoShellProfile.MINIMUM -> 260f
                DemoShellProfile.STANDARD -> if (isLargeStandardBounds) 380f else 300f
                DemoShellProfile.DEMO_ASPECT -> 420f
            }
        val actionColumns =
            when (profile) {
                DemoShellProfile.DEMO_ASPECT -> PLAYER_ACTIVE_TALENT_SLOT_COUNT.coerceAtMost(4)
                DemoShellProfile.STANDARD -> PLAYER_ACTIVE_TALENT_SLOT_COUNT.coerceAtMost(4)
                DemoShellProfile.MINIMUM -> PLAYER_ACTIVE_TALENT_SLOT_COUNT.coerceAtMost(2)
            }.coerceAtLeast(1)
        val actionRows = (PLAYER_ACTIVE_TALENT_SLOT_COUNT + actionColumns - 1) / actionColumns
        val actionSlotSide =
            when (profile) {
                DemoShellProfile.MINIMUM -> 72f
                DemoShellProfile.STANDARD -> if (isLargeStandardBounds) 84f else 78f
                DemoShellProfile.DEMO_ASPECT -> 96f
            }
        val actionSlotHeight =
            when (profile) {
                DemoShellProfile.MINIMUM -> actionSlotSide
                DemoShellProfile.STANDARD -> if (isLargeStandardBounds) 126f else 116f
                DemoShellProfile.DEMO_ASPECT -> 132f
            }
        val actionDeckWidth = actionSlotSide * actionColumns + sectionGap * (actionColumns - 1)
        val logMinWidth =
            when (profile) {
                DemoShellProfile.MINIMUM -> 180f
                DemoShellProfile.STANDARD -> if (isLargeStandardBounds) 300f else 220f
                DemoShellProfile.DEMO_ASPECT -> 300f
            }
        val hero = GameShellBounds(bounds.x + margin, contentY, heroWidth, contentHeight)
        val actionDeck = GameShellBounds(hero.right + sectionGap, contentY, actionDeckWidth, contentHeight)
        val actionSlots =
            List(PLAYER_ACTIVE_TALENT_SLOT_COUNT) { index ->
                val column = index % actionColumns
                val row = index / actionColumns
                GameShellBounds(
                    x = actionDeck.x + column * (actionSlotSide + sectionGap),
                    y =
                        actionDeck.y +
                            (actionDeck.height - (actionRows * actionSlotHeight + (actionRows - 1) * sectionGap)).coerceAtLeast(0f) / 2f +
                            (actionRows - row - 1) * (actionSlotHeight + sectionGap),
                    width = actionSlotSide,
                    height = actionSlotHeight,
                )
            }
        val logX = actionDeck.right + sectionGap
        val log = GameShellBounds(logX, contentY, (bounds.right - margin - logX).coerceAtLeast(1f), contentHeight)
        return DemoBottomDeckLayout(
            bounds = bounds,
            heroCard = hero,
            actionDeck = actionDeck,
            actionSlotBounds = actionSlots,
            logDeck = log,
        )
    }

    private fun rightPanelLayout(
        bounds: GameShellBounds,
        profile: DemoShellProfile,
    ): DemoRightPanelLayout {
        val sectionGap = 6f
        val isLargeStandardBounds = profile == DemoShellProfile.STANDARD && bounds.width >= 400f
        val utilitySlotSide =
            when (profile) {
                DemoShellProfile.MINIMUM -> 38f
                DemoShellProfile.STANDARD -> if (isLargeStandardBounds) 52f else 42f
                DemoShellProfile.DEMO_ASPECT -> 56f
            }
        val equipmentSlotSide =
            when (profile) {
                DemoShellProfile.MINIMUM -> utilitySlotSide
                DemoShellProfile.STANDARD -> if (isLargeStandardBounds) 60f else 50f
                DemoShellProfile.DEMO_ASPECT -> 64f
            }
        val backpackRows = 2
        val availableSectionHeight = (bounds.height - sectionGap * 3f).coerceAtLeast(1f)
        val equipmentHeight =
            (availableSectionHeight * 0.42f)
                .coerceAtLeast(RIGHT_SECTION_TITLE_HEIGHT + equipmentSlotSide * 5f + 8f * 4f + 12f)
        val inscriptionHeight =
            (availableSectionHeight * 0.26f)
                .coerceAtLeast(RIGHT_SECTION_TITLE_HEIGHT + utilitySlotSide * 4f + sectionGap * 3f + 10f)
        val backpackHeight =
            (availableSectionHeight * 0.15f)
                .coerceAtLeast(
            RIGHT_SECTION_TITLE_HEIGHT +
                utilitySlotSide * backpackRows +
                sectionGap * (backpackRows - 1) +
                RIGHT_BACKPACK_PAGER_HEIGHT +
                10f,
                )
        val operationHeight =
            (
                availableSectionHeight -
                    equipmentHeight -
                    inscriptionHeight -
                    backpackHeight
            ).coerceAtLeast(84f)
        var top = bounds.top
        fun nextSection(height: Float): GameShellBounds {
            val section = GameShellBounds(bounds.x, top - height, bounds.width, height)
            top = section.y - sectionGap
            return section
        }
        val equipment = nextSection(equipmentHeight)
        val inscriptions = nextSection(inscriptionHeight)
        val backpack = nextSection(backpackHeight)
        val backpackPager =
            GameShellBounds(
                x = backpack.x + 8f,
                y = backpack.y + 4f,
                width = backpack.width - 16f,
                height = RIGHT_BACKPACK_PAGER_HEIGHT - 4f,
            )
        val operation = GameShellBounds(bounds.x, bounds.y, bounds.width, minOf(operationHeight, (top - bounds.y).coerceAtLeast(0f)))
        return DemoRightPanelLayout(
            equipment = equipment,
            inscriptions = inscriptions,
            backpack = backpack,
            backpackPager = backpackPager,
            operationHints = operation,
            equipmentSlots = equipmentSlotGrid(equipment = equipment, slotSide = equipmentSlotSide),
            inscriptionSlots = inscriptionSlotGrid(inscriptions, slotSide = utilitySlotSide),
            backpackSlots = slotGrid(backpack, columns = 4, rows = backpackRows, slotSide = utilitySlotSide, bottomInset = RIGHT_BACKPACK_PAGER_HEIGHT),
        )
    }

    private fun inscriptionSlotGrid(
        bounds: GameShellBounds,
        slotSide: Float,
    ): DemoSlotGridLayout {
        val gap = 6f
        val columns = 2
        val rows = 4
        val columnGap = 10f
        val gridHeight = rows * slotSide + (rows - 1) * gap
        val top = bounds.top - RIGHT_SECTION_TITLE_HEIGHT - 4f
        val rowWidth = ((bounds.width - 24f - columnGap) / 2f).coerceAtLeast(slotSide)
        val startX = bounds.x + 12f
        val startY = (top - gridHeight).coerceAtLeast(bounds.y + 4f)
        val slots =
            List(rows * columns) { index ->
                val row = index / columns
                val column = index % columns
                GameShellBounds(
                    x = startX + column * (rowWidth + columnGap),
                    y = startY + (rows - row - 1) * (slotSide + gap),
                    width = slotSide,
                    height = slotSide,
                )
            }
        return DemoSlotGridLayout(bounds = bounds, columns = columns, rows = rows, slotSide = slotSide, slotBounds = slots)
    }

    private fun equipmentSlotGrid(
        equipment: GameShellBounds,
        slotSide: Float,
    ): DemoSlotGridLayout {
        val gap = 8f
        val columns = 2
        val rows = 5
        val bounds = equipment
        val gridWidth = (slotSide * columns + gap).coerceAtMost(bounds.width - 24f)
        val gridHeight = rows * slotSide + (rows - 1) * gap
        val top = equipment.top - RIGHT_SECTION_TITLE_HEIGHT - 4f
        val leftX = bounds.x + ((bounds.width - gridWidth) / 2f).coerceAtLeast(8f)
        val rightX = leftX + slotSide + gap
        val centerX = bounds.x + (bounds.width - slotSide) / 2f
        val startY = (top - gridHeight).coerceAtLeast(bounds.y + 4f)
        fun rowY(row: Int): Float = startY + (rows - row - 1) * (slotSide + gap)
        val slots =
            listOf(
                GameShellBounds(leftX, rowY(0), slotSide, slotSide),
                GameShellBounds(rightX, rowY(0), slotSide, slotSide),
                GameShellBounds(leftX, rowY(1), slotSide, slotSide),
                GameShellBounds(rightX, rowY(1), slotSide, slotSide),
                GameShellBounds(leftX, rowY(2), slotSide, slotSide),
                GameShellBounds(rightX, rowY(2), slotSide, slotSide),
                GameShellBounds(leftX, rowY(3), slotSide, slotSide),
                GameShellBounds(rightX, rowY(3), slotSide, slotSide),
                GameShellBounds(centerX, rowY(4), slotSide, slotSide),
            )
        return DemoSlotGridLayout(bounds = bounds, columns = columns, rows = rows, slotSide = slotSide, slotBounds = slots)
    }

    private fun slotGrid(
        bounds: GameShellBounds,
        columns: Int,
        rows: Int,
        slotSide: Float,
        topInset: Float = 0f,
        bottomInset: Float = 0f,
    ): DemoSlotGridLayout {
        val gap = 6f
        val gridWidth = columns * slotSide + (columns - 1) * gap
        val gridHeight = rows * slotSide + (rows - 1) * gap
        val startX = bounds.x + ((bounds.width - gridWidth) / 2f).coerceAtLeast(8f)
        val top = bounds.top - RIGHT_SECTION_TITLE_HEIGHT - 4f - topInset
        val startY = (top - gridHeight).coerceAtLeast(bounds.y + 4f + bottomInset)
        val slots =
            List(rows * columns) { index ->
                val row = index / columns
                val column = index % columns
                GameShellBounds(
                    x = startX + column * (slotSide + gap),
                    y = startY + (rows - row - 1) * (slotSide + gap),
                    width = slotSide,
                    height = slotSide,
                )
            }
        return DemoSlotGridLayout(bounds = bounds, columns = columns, rows = rows, slotSide = slotSide, slotBounds = slots)
    }

    private fun modalSafeBounds(
        mapStage: GameShellBounds,
        navRailRight: Float,
        rightPanelLeft: Float,
        bottomDeckTop: Float,
        viewportWidth: Float,
        viewportHeight: Float,
    ): ModalSafeBounds {
        val centerX = mapStage.x + mapStage.width / 2f
        val centerY = mapStage.y + mapStage.height / 2f
        val width = (viewportWidth * 0.62f).coerceAtMost(mapStage.width).coerceAtLeast(viewportWidth * 0.55f)
        val height = (viewportHeight * 0.62f).coerceAtMost(mapStage.height).coerceAtLeast(viewportHeight * 0.55f)
        val left = (centerX - width / 2f).roundToInt().coerceAtLeast(navRailRight.roundToInt())
        val right = (centerX + width / 2f).roundToInt().coerceAtMost(rightPanelLeft.roundToInt())
        val bottom = (centerY - height / 2f).roundToInt().coerceAtLeast(bottomDeckTop.roundToInt())
        val top = (centerY + height / 2f).roundToInt().coerceAtMost(viewportHeight.roundToInt())
        return ModalSafeBounds(left = left, right = right, bottom = bottom, top = top)
    }

    private enum class DemoShellProfile {
        MINIMUM,
        STANDARD,
        DEMO_ASPECT,
    }
}
