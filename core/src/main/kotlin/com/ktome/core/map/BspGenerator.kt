package com.ktome.core.map

import kotlin.math.min
import kotlin.random.Random

data class BspConfig(
    val width: Int = 80,
    val height: Int = 50,
    val minLeafSize: Int = 12,
    val minRoomSize: Int = 6,
    val maxRoomSize: Int = 12,
    val maxDepth: Int = 5,
) {
    init {
        require(width >= minLeafSize + 2) { "Map width is too small for the requested BSP settings." }
        require(height >= minLeafSize + 2) { "Map height is too small for the requested BSP settings." }
        require(minLeafSize >= minRoomSize + 2) { "Leaf size must allow a room plus a one-tile border." }
        require(maxRoomSize >= minRoomSize) { "Max room size must be >= min room size." }
        require(maxDepth > 0) { "maxDepth must be positive." }
    }
}

class BspGenerator(
    private val seed: Long,
    private val config: BspConfig = BspConfig(),
) {
    fun generate(): GameMap {
        val random = Random(seed)
        val rootLeaf = Leaf(
            x = 1,
            y = 1,
            width = config.width - 2,
            height = config.height - 2,
            depth = 0,
        )
        val leaves = splitRecursively(rootLeaf, random)
        val rooms = leaves.map { createRoom(it, random) }
        val builder = GameMap.Builder(config.width, config.height)

        rooms.forEach(builder::carveRoom)
        rooms.zipWithNext().forEach { (firstRoom, secondRoom) ->
            carveCorridor(builder, firstRoom.center, secondRoom.center, random)
        }

        return builder.build(
            rooms = rooms,
            playerStart = rooms.first().center,
        )
    }

    private fun splitRecursively(
        leaf: Leaf,
        random: Random,
    ): List<Leaf> {
        val children = split(leaf, random) ?: return listOf(leaf)
        return splitRecursively(children.first, random) + splitRecursively(children.second, random)
    }

    private fun split(
        leaf: Leaf,
        random: Random,
    ): Pair<Leaf, Leaf>? {
        if (leaf.depth >= config.maxDepth) {
            return null
        }

        val canSplitHorizontally = leaf.height >= config.minLeafSize * 2
        val canSplitVertically = leaf.width >= config.minLeafSize * 2
        if (!canSplitHorizontally && !canSplitVertically) {
            return null
        }

        val splitHorizontally = when {
            canSplitHorizontally && !canSplitVertically -> true
            !canSplitHorizontally && canSplitVertically -> false
            leaf.width > leaf.height && leaf.width / leaf.height.toDouble() >= 1.25 -> false
            leaf.height > leaf.width && leaf.height / leaf.width.toDouble() >= 1.25 -> true
            else -> random.nextBoolean()
        }

        return if (splitHorizontally) {
            val splitPoint = random.nextInt(config.minLeafSize, leaf.height - config.minLeafSize + 1)
            Leaf(leaf.x, leaf.y, leaf.width, splitPoint, leaf.depth + 1) to
                Leaf(leaf.x, leaf.y + splitPoint, leaf.width, leaf.height - splitPoint, leaf.depth + 1)
        } else {
            val splitPoint = random.nextInt(config.minLeafSize, leaf.width - config.minLeafSize + 1)
            Leaf(leaf.x, leaf.y, splitPoint, leaf.height, leaf.depth + 1) to
                Leaf(leaf.x + splitPoint, leaf.y, leaf.width - splitPoint, leaf.height, leaf.depth + 1)
        }
    }

    private fun createRoom(
        leaf: Leaf,
        random: Random,
    ): Room {
        val maxRoomWidth = min(config.maxRoomSize, leaf.width - 2)
        val maxRoomHeight = min(config.maxRoomSize, leaf.height - 2)
        val roomWidth = random.nextInt(config.minRoomSize, maxRoomWidth + 1)
        val roomHeight = random.nextInt(config.minRoomSize, maxRoomHeight + 1)
        val roomX = random.nextInt(leaf.x + 1, leaf.x + leaf.width - roomWidth)
        val roomY = random.nextInt(leaf.y + 1, leaf.y + leaf.height - roomHeight)
        return Room(roomX, roomY, roomWidth, roomHeight)
    }

    private fun carveCorridor(
        builder: GameMap.Builder,
        start: Point,
        end: Point,
        random: Random,
    ) {
        if (random.nextBoolean()) {
            carveHorizontal(builder, start.x, end.x, start.y)
            carveVertical(builder, start.y, end.y, end.x)
        } else {
            carveVertical(builder, start.y, end.y, start.x)
            carveHorizontal(builder, start.x, end.x, end.y)
        }
    }

    private fun carveHorizontal(
        builder: GameMap.Builder,
        startX: Int,
        endX: Int,
        y: Int,
    ) {
        val range = if (startX <= endX) startX..endX else endX..startX
        range.forEach { x ->
            builder.setTile(Point(x, y), TileType.FLOOR)
        }
    }

    private fun carveVertical(
        builder: GameMap.Builder,
        startY: Int,
        endY: Int,
        x: Int,
    ) {
        val range = if (startY <= endY) startY..endY else endY..startY
        range.forEach { y ->
            builder.setTile(Point(x, y), TileType.FLOOR)
        }
    }

    private data class Leaf(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val depth: Int,
    )
}
