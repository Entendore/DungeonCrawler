package com.yourpackage.dungeoncrawler

import kotlin.random.Random

interface MapGenerator { fun generate(width: Int, height: Int, seed: Long = System.currentTimeMillis()): DungeonMap }

class BSPGenerator : MapGenerator {
    private data class Room(val x: Int, val y: Int, val w: Int, val h: Int) { fun centerX() = x + w / 2; fun centerY() = y + h / 2 }
    override fun generate(width: Int, height: Int, seed: Long): DungeonMap {
        val rand = Random(seed); val map = DungeonMap(width, height, Array(width * height) { TileType.WALL }, Pair(0, 0))
        val rooms = mutableListOf<Room>()
        split(map, Room(1, 1, width - 2, height - 2), rooms, rand, 0, 4)
        rooms.forEach { room -> for (y in room.y until room.y + room.h) for (x in room.x until room.x + room.w) map.setTile(x, y, TileType.FLOOR) }
        rooms.sortedBy { it.centerX() + it.centerY() }.zipWithNext { a, b -> carveCorridor(map, a.centerX(), a.centerY(), b.centerX(), b.centerY()) }
        val lastRoom = rooms.last(); return DungeonMap(width, height, map.tiles, Pair(lastRoom.centerX(), lastRoom.centerY()))
    }
    private fun split(map: DungeonMap, region: Room, rooms: MutableList<Room>, rand: Random, depth: Int, maxDepth: Int) {
        if (depth >= maxDepth || region.w < 7 || region.h < 7) { rooms.add(region); return }
        val splitH = if (region.w > region.h) false else if (region.h > region.w) true else rand.nextBoolean()
        if (splitH) { val s = rand.nextInt(region.y + 3, region.y + region.h - 3); split(map, Room(region.x, region.y, region.w, s - region.y), rooms, rand, depth + 1, maxDepth); split(map, Room(region.x, s, region.w, region.y + region.h - s), rooms, rand, depth + 1, maxDepth) }
        else { val s = rand.nextInt(region.x + 3, region.x + region.w - 3); split(map, Room(region.x, region.y, s - region.x, region.h), rooms, rand, depth + 1, maxDepth); split(map, Room(s, region.y, region.x + region.w - s, region.h), rooms, rand, depth + 1, maxDepth) }
    }
    private fun carveCorridor(map: DungeonMap, x1: Int, y1: Int, x2: Int, y2: Int) {
        var x = x1; var y = y1; while (x != x2) { map.setTile(x, y, TileType.FLOOR); x += if (x2 > x) 1 else -1 }; while (y != y2) { map.setTile(x, y, TileType.FLOOR); y += if (y2 > y) 1 else -1 }; map.setTile(x2, y2, TileType.FLOOR)
    }
}

class CellularAutomataGenerator : MapGenerator {
    override fun generate(width: Int, height: Int, seed: Long): DungeonMap {
        val rand = Random(seed); val tiles = Array(width * height) { TileType.WALL }
        for (y in 0 until height) for (x in 0 until width) { if (x == 0 || y == 0 || x == width - 1 || y == height - 1) continue; tiles[y * width + x] = if (rand.nextDouble() < 0.45) TileType.WALL else TileType.FLOOR }
        repeat(5) { val next = tiles.copyOf(); for (y in 1 until height - 1) for (x in 1 until width - 1) { var c = 0; for (dy in -1..1) for (dx in -1..1) { if (dx == 0 && dy == 0) continue; val nx = x + dx; val ny = y + dy; if (nx < 0 || ny < 0 || nx >= width || ny >= height || tiles[ny * width + nx] == TileType.WALL) c++ }; next[y * width + x] = if (c >= 5) TileType.WALL else TileType.FLOOR }; for (i in tiles.indices) tiles[i] = next[i] }
        return DungeonMap(width, height, tiles, Pair(width / 2, height / 2))
    }
}

class RandomWalkGenerator : MapGenerator {
    override fun generate(width: Int, height: Int, seed: Long): DungeonMap {
        val rand = Random(seed); val tiles = Array(width * height) { TileType.WALL }
        var x = width / 2; var y = height / 2; var carved = 0; val target = (width * height * 0.4).toInt()
        while (carved < target) { tiles[y * width + x] = TileType.FLOOR; carved++; when (rand.nextInt(4)) { 0 -> x = (x - 1).coerceAtLeast(1); 1 -> x = (x + 1).coerceAtMost(width - 2); 2 -> y = (y - 1).coerceAtLeast(1); 3 -> y = (y + 1).coerceAtMost(height - 2) } }
        return DungeonMap(width, height, tiles, Pair(width - 2, height - 2))
    }
}

object MapGeneratorFactory {
    fun create(algorithm: MapAlgorithm): MapGenerator = when (algorithm) {
        MapAlgorithm.BSP -> BSPGenerator(); MapAlgorithm.CELLULAR_AUTOMATA -> CellularAutomataGenerator(); MapAlgorithm.RANDOM_WALK -> RandomWalkGenerator()
    }
}
