package com.yourpackage.dungeoncrawler

data class GameState(
    val map: DungeonMap, val playerStats: PlayerStats, val playerHealth: Int,
    val playerPosition: Pair<Int, Int>, val enemies: List<EnemyState>,
    val inventory: Inventory, val turn: Int, val dungeonLevel: Int, val algorithm: MapAlgorithm
)

data class EnemyState(
    val id: Int, val position: Pair<Int, Int>, val health: Int, val maxHealth: Int,
    val type: String, val damage: Int, val xpReward: Int, val goldReward: Int,
    val behavior: EnemyBehavior = EnemyBehavior.AGGRESSIVE
)

enum class EnemyBehavior { AGGRESSIVE, CAUTIOUS, RANGED, BOSS }

enum class Action { MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, ATTACK, WAIT, USE_POTION }

enum class MapAlgorithm { BSP, CELLULAR_AUTOMATA, RANDOM_WALK }

object EnemyFactory {
    fun generateEnemy(id: Int, position: Pair<Int, Int>, level: Int): EnemyState {
        val types = listOf("Goblin" to EnemyBehavior.AGGRESSIVE, "Skeleton" to EnemyBehavior.CAUTIOUS, "Orc" to EnemyBehavior.AGGRESSIVE, "Dark Mage" to EnemyBehavior.RANGED)
        val (type, behavior) = types.random()
        return EnemyState(id, position, 30 + level * 8, 30 + level * 8, type, 5 + level * 2, 20 + level * 5, 10 + level * 3, behavior)
    }
}
