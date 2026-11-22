package com.yourpackage.dungeoncrawler

import android.content.Context
import android.content.SharedPreferences
import com.yourpackage.dungeoncrawler.config.LocalConfig
import com.yourpackage.dungeoncrawler.config.LocalFeatureFlags
import com.yourpackage.dungeoncrawler.telemetry.LocalAnalytics
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(private var algorithm: MapAlgorithm = MapAlgorithm.BSP) {
    private val mdp = MDPModel()
    private val narrative = NarrativeEngine()
    val recentNarrative = mutableListOf<NarrativeLine>()
    private val mapWidth = 40
    private val mapHeight = 25
    private var currentState: GameState
    private lateinit var prefs: SharedPreferences
    lateinit var settings: SettingsManager
    private var config: LocalConfig? = null
    private var features: LocalFeatureFlags? = null
    private var analytics: LocalAnalytics? = null
    var doubleGoldTurnsRemaining = 0
    
    init { currentState = generateNewLevel(algorithm, 1) }
    
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("dungeon_crawler_prefs", Context.MODE_PRIVATE)
        settings = SettingsManager(context)
        loadProgress()
    }
    
    fun setConfig(c: LocalConfig) { config = c }
    fun setFeatures(f: LocalFeatureFlags) { features = f }
    fun setAnalytics(a: LocalAnalytics) { analytics = a }
    
    fun generateNewLevel(algorithm: MapAlgorithm, dungeonLevel: Int): GameState {
        this.algorithm = algorithm
        val generator = MapGeneratorFactory.create(algorithm)
        val map = generator.generate(mapWidth, mapHeight)
        val floorTiles = map.tiles.indices.filter { map.tiles[it] == TileType.FLOOR }
        val playerIdx = floorTiles.minByOrNull {
            val px = it % mapWidth; val py = it / mapWidth
            val ex = map.exitPosition.first; val ey = map.exitPosition.second
            (px - ex) * (px - ex) + (py - ey) * (py - ey)
        } ?: 0
        val playerPos = Pair(playerIdx % mapWidth, playerIdx / mapWidth)
        val enemyCount = 3 + dungeonLevel
        val enemies = floorTiles.filter { it != playerIdx }.shuffled(Random(System.currentTimeMillis())).take(enemyCount)
            .mapIndexed { i, idx -> EnemyFactory.generateEnemy(i + 1, Pair(idx % mapWidth, idx / mapWidth), dungeonLevel) }
        val stats = currentState?.playerStats ?: PlayerStats()
        stats.deepestLevel = max(stats.deepestLevel, dungeonLevel)
        return GameState(map, stats, stats.getMaxHealth(), playerPos, enemies, currentState?.inventory ?: Inventory(), 0, dungeonLevel, algorithm)
    }
    
    fun regenerateMap(algorithm: MapAlgorithm) { currentState = generateNewLevel(algorithm, currentState.dungeonLevel) }
    
    fun processPlayerAction(action: Action): GameState {
        if (currentState.playerPosition == currentState.map.exitPosition && currentState.enemies.isEmpty()) {
            val newLevel = currentState.dungeonLevel + 1
            emit(narrative.generate(NarrativeEvent.LEVEL_TRANSITION, mapOf("level" to newLevel.toString())))
            currentState = generateNewLevel(algorithm, newLevel)
            saveProgress()
            return currentState
        }
        val prevState = currentState
        val newState = applyAction(currentState, action, isPlayer = true)
        currentState = newState
        if (action == Action.ATTACK) {
            val killed = prevState.enemies.size - currentState.enemies.size
            if (killed > 0) {
                val victim = prevState.enemies.first { e -> currentState.enemies.none { it.id == e.id } }
                val goldEarned = if (doubleGoldTurnsRemaining > 0) { doubleGoldTurnsRemaining--; victim.goldReward * 2 } else victim.goldReward
                currentState.playerStats.totalKills++
                currentState.playerStats.totalDamageDealt += currentState.playerStats.getAttackDamage()
                currentState.playerStats.addXP(victim.xpReward)
                currentState.playerStats.gold += goldEarned
                emit(narrative.generate(NarrativeEvent.ENEMY_DIED, mapOf("enemy" to victim.type)))
                if (Random.nextInt(100) < 40) {
                    val loot = when (Random.nextInt(3)) { 0 -> Item.generateWeapon(currentState.dungeonLevel); 1 -> Item.generateArmor(currentState.dungeonLevel); else -> Item.generatePotion() }
                    currentState.inventory.addItem(loot)
                    emit(NarrativeLine("📦 Found: ${loot.name}", NarrativeLine.Style.TRIUMPH))
                }
            }
        }
        for (enemy in currentState.enemies.toList()) {
            val beforeHp = currentState.playerHealth
            val enemyAction = mdp.getBestAction(currentState, enemy)
            val afterEnemy = applyAction(currentState, enemyAction, isPlayer = false, enemyId = enemy.id)
            mdp.updateQValue(currentState, enemyAction, afterEnemy)
            if (enemyAction == Action.ATTACK && afterEnemy.playerHealth < beforeHp) {
                val damage = beforeHp - afterEnemy.playerHealth
                emit(narrative.generate(NarrativeEvent.ENEMY_ATTACK_HIT, mapOf("enemy" to enemy.type, "damage" to damage.toString())))
            }
            currentState = afterEnemy
        }
        currentState = currentState.copy(turn = currentState.turn + 1)
        saveProgress()
        return currentState
    }
    
    fun processRealTimeAction(action: Action) { processPlayerAction(action) }
    
    private fun applyAction(state: GameState, action: Action, isPlayer: Boolean, enemyId: Int? = null): GameState {
        var newState = state
        if (isPlayer) {
            val (px, py) = state.playerPosition
            val newPos = when (action) {
                Action.MOVE_UP -> Pair(px, py - 1); Action.MOVE_DOWN -> Pair(px, py + 1)
                Action.MOVE_LEFT -> Pair(px - 1, py); Action.MOVE_RIGHT -> Pair(px + 1, py)
                else -> state.playerPosition
            }
            if (state.map.isWalkable(newPos.first, newPos.second) && state.enemies.none { it.position == newPos }) newState = newState.copy(playerPosition = newPos)
        } else {
            val enemy = state.enemies.find { it.id == enemyId } ?: return state
            val (ex, ey) = enemy.position
            val newPos = when (action) {
                Action.MOVE_UP -> Pair(ex, ey - 1); Action.MOVE_DOWN -> Pair(ex, ey + 1)
                Action.MOVE_LEFT -> Pair(ex - 1, ey); Action.MOVE_RIGHT -> Pair(ex + 1, ey)
                else -> enemy.position
            }
            val updatedEnemy = if (state.map.isWalkable(newPos.first, newPos.second) && newPos != state.playerPosition && state.enemies.none { it.id != enemyId && it.position == newPos }) enemy.copy(position = newPos) else enemy
            var hp = state.playerHealth
            if (action == Action.ATTACK && isAdjacent(updatedEnemy.position, state.playerPosition)) hp = max(0, hp - enemy.damage)
            newState = newState.copy(playerHealth = hp, enemies = state.enemies.map { if (it.id == enemyId) updatedEnemy else it })
        }
        return newState
    }
    
    private fun isAdjacent(a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean = (kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second)) == 1
    
    fun getCurrentState(): GameState = currentState
    fun isGameOver(): Boolean = currentState.playerHealth <= 0
    fun isVictory(): Boolean = currentState.enemies.isEmpty() && currentState.playerPosition == currentState.map.exitPosition
    
    fun getViewport(viewWidth: Int, viewHeight: Int): Array<Array<String>> {
        val (px, py) = currentState.playerPosition
        val halfW = viewWidth / 2; val halfH = viewHeight / 2
        val startX = (px - halfW).coerceIn(0, max(0, mapWidth - viewWidth))
        val startY = (py - halfH).coerceIn(0, max(0, mapHeight - viewHeight))
        val viewport = Array(viewHeight) { Array(viewWidth) { " " } }
        for (vy in 0 until viewHeight) for (vx in 0 until viewWidth) {
            val mx = startX + vx; val my = startY + vy
            val tile = currentState.map.getTile(mx, my)
            viewport[vy][vx] = when {
                Pair(mx, my) == currentState.playerPosition -> TileType.PLAYER.symbol
                currentState.enemies.any { it.position == Pair(mx, my) } -> TileType.ENEMY.symbol
                Pair(mx, my) == currentState.map.exitPosition -> TileType.EXIT.symbol
                else -> tile.symbol
            }
        }
        return viewport
    }
    
    fun revivePlayer() { currentState = currentState.copy(playerHealth = currentState.playerStats.getMaxHealth() / 2) }
    fun skipToNextLevel() { currentState = generateNewLevel(algorithm, currentState.dungeonLevel + 1) }
    fun togglePlayMode() { emit(NarrativeLine("🧠 Mode toggled", NarrativeLine.Style.MYSTIC)) }
    
    private fun emit(line: NarrativeLine) { recentNarrative.add(line); if (recentNarrative.size > 15) recentNarrative.removeAt(0) }
    
    private fun saveProgress() {
        if (!::prefs.isInitialized) return
        with(prefs.edit()) {
            putString("player_stats", serializeStats(currentState.playerStats))
            apply()
        }
    }
    
    private fun loadProgress() {
        if (!::prefs.isInitialized) return
        val statsJson = prefs.getString("player_stats", null)
        if (statsJson != null) currentState = currentState.copy(playerStats = deserializeStats(statsJson))
    }
    
    private fun serializeStats(stats: PlayerStats): String = "${stats.level}|${stats.xp}|${stats.xpToNextLevel}|${stats.strength}|${stats.agility}|${stats.vitality}|${stats.skillPoints}|${stats.gold}|${stats.totalKills}|${stats.totalDamageDealt}|${stats.totalDamageTaken}|${stats.deepestLevel}"
    
    private fun deserializeStats(data: String): PlayerStats {
        val parts = data.split("|")
        return PlayerStats().apply {
            level = parts.getOrNull(0)?.toIntOrNull() ?: 1
            xp = parts.getOrNull(1)?.toIntOrNull() ?: 0
            xpToNextLevel = parts.getOrNull(2)?.toIntOrNull() ?: 100
            strength = parts.getOrNull(3)?.toIntOrNull() ?: 10
            agility = parts.getOrNull(4)?.toIntOrNull() ?: 10
            vitality = parts.getOrNull(5)?.toIntOrNull() ?: 10
            skillPoints = parts.getOrNull(6)?.toIntOrNull() ?: 0
            gold = parts.getOrNull(7)?.toIntOrNull() ?: 0
            totalKills = parts.getOrNull(8)?.toIntOrNull() ?: 0
            totalDamageDealt = parts.getOrNull(9)?.toIntOrNull() ?: 0
            totalDamageTaken = parts.getOrNull(10)?.toIntOrNull() ?: 0
            deepestLevel = parts.getOrNull(11)?.toIntOrNull() ?: 1
        }
    }
    
    fun toJson(): JSONObject = JSONObject().apply {
        put("playerHealth", currentState.playerHealth)
        put("playerPosition", "${currentState.playerPosition.first},${currentState.playerPosition.second}")
        put("dungeonLevel", currentState.dungeonLevel)
        put("turn", currentState.turn)
        put("algorithm", algorithm.name)
        put("player_stats", serializeStats(currentState.playerStats))
    }
    
    fun loadFromJson(json: JSONObject): Boolean {
        return try {
            val pos = json.getString("playerPosition").split(",")
            currentState = currentState.copy(
                playerHealth = json.optInt("playerHealth", 100),
                playerPosition = Pair(pos[0].toInt(), pos[1].toInt()),
                dungeonLevel = json.optInt("dungeonLevel", 1),
                turn = json.optInt("turn", 0),
                playerStats = deserializeStats(json.optString("player_stats", serializeStats(PlayerStats())))
            )
            true
        } catch (e: Exception) { false }
    }
}
