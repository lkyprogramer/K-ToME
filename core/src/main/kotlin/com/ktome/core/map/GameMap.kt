package com.ktome.core.map

class GameMap private constructor(
    val width: Int,
    val height: Int,
    private val tiles: Array<TileType>,
    val rooms: List<Room>,
    val playerStart: Point,
) {
    init {
        require(width > 0) { "Map width must be positive." }
        require(height > 0) { "Map height must be positive." }
        require(tiles.size == width * height) { "Tile buffer size does not match map dimensions." }
        require(isInBounds(playerStart.x, playerStart.y)) { "Player start must be inside the map." }
        require(!blocksMovement(playerStart.x, playerStart.y)) { "Player start must be a walkable tile." }
    }

    operator fun get(point: Point): TileType = get(point.x, point.y)

    operator fun get(x: Int, y: Int): TileType {
        require(isInBounds(x, y)) { "Point ($x, $y) is outside the map." }
        return tiles[indexOf(x, y)]
    }

    fun isInBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    fun blocksMovement(x: Int, y: Int): Boolean = !isInBounds(x, y) || tiles[indexOf(x, y)].blocksMovement

    fun blocksVision(x: Int, y: Int): Boolean = !isInBounds(x, y) || tiles[indexOf(x, y)].blocksVision

    fun floorPoints(): Set<Point> =
        buildSet {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (!blocksMovement(x, y)) {
                        add(Point(x, y))
                    }
                }
            }
        }

    fun asGlyphRows(): List<String> =
        (0 until height).map { y ->
            buildString(width) {
                for (x in 0 until width) {
                    append(tiles[indexOf(x, y)].glyph)
                }
            }
        }

    private fun indexOf(x: Int, y: Int): Int = y * width + x

    class Builder(
        private val width: Int,
        private val height: Int,
    ) {
        private val tiles = Array(width * height) { TileType.WALL }

        fun setTile(point: Point, tile: TileType): Builder {
            require(point.x in 0 until width && point.y in 0 until height) {
                "Point $point is outside the builder bounds."
            }
            tiles[point.y * width + point.x] = tile
            return this
        }

        fun carveRoom(room: Room): Builder {
            for (y in room.top..room.bottom) {
                for (x in room.left..room.right) {
                    setTile(Point(x, y), TileType.FLOOR)
                }
            }
            return this
        }

        fun build(
            rooms: List<Room>,
            playerStart: Point,
        ): GameMap = GameMap(width, height, tiles.copyOf(), rooms.toList(), playerStart)
    }

    companion object {
        fun fromAscii(
            rows: List<String>,
            playerStart: Point? = null,
            rooms: List<Room> = emptyList(),
        ): GameMap {
            require(rows.isNotEmpty()) { "ASCII rows cannot be empty." }
            val width = rows.first().length
            require(rows.all { it.length == width }) { "All rows must have the same width." }

            val tiles = Array(width * rows.size) { TileType.WALL }
            var discoveredStart: Point? = null

            rows.forEachIndexed { y, row ->
                row.forEachIndexed { x, glyph ->
                    tiles[y * width + x] = when (glyph) {
                        '#', ' ' -> TileType.WALL
                        '.', '@' -> TileType.FLOOR
                        else -> error("Unsupported map glyph: $glyph")
                    }

                    if (glyph == '@') {
                        discoveredStart = Point(x, y)
                    }
                }
            }

            val resolvedStart = playerStart ?: discoveredStart ?: rows.indices
                .asSequence()
                .flatMap { y -> rows[y].indices.asSequence().map { x -> Point(x, y) } }
                .firstOrNull { point -> tiles[point.y * width + point.x] == TileType.FLOOR }
                ?: error("ASCII map must contain at least one walkable tile.")

            return GameMap(width, rows.size, tiles, rooms, resolvedStart)
        }
    }
}
