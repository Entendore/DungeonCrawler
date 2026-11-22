package com.yourpackage.dungeoncrawler

enum class TileType(val symbol: String, val walkable: Boolean) {
    WALL("#", false), FLOOR(".", true), EXIT(">", true), PLAYER("@", true), ENEMY("E", true)
}

data class DungeonMap(val width: Int, val height: Int, val tiles: Array<TileType>, val exitPosition: Pair<Int, Int>) {
    fun getTile(x: Int, y: Int): TileType = if (x < 0 || y < 0 || x >= width || y >= height) TileType.WALL else tiles[y * width + x]
    fun setTile(x: Int, y: Int, type: TileType) { if (x in 0 until width && y in 0 until height) tiles[y * width + x] = type }
    fun isWalkable(x: Int, y: Int): Boolean = getTile(x, y).walkable
}
