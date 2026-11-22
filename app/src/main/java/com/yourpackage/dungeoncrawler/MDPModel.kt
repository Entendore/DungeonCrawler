package com.yourpackage.dungeoncrawler

import kotlin.math.max
import kotlin.random.Random

class MDPModel {
    private val gamma = 0.9; private val learningRate = 0.1
    private val qTable = mutableMapOf<GameState, MutableMap<Action, Double>>()
    
    fun getBestAction(state: GameState, enemy: EnemyState): Action {
        val actions = getAvailableActions(state, enemy)
        if (Random.nextDouble() < 0.1) return actions.random()
        return actions.maxByOrNull { getQValue(state, it) } ?: Action.WAIT
    }
    
    private fun getAvailableActions(state: GameState, enemy: EnemyState): List<Action> {
        val actions = mutableListOf<Action>(); val (x, y) = enemy.position
        if (y > 0 && state.map.isWalkable(x, y - 1)) actions.add(Action.MOVE_UP)
        if (y < state.map.height - 1 && state.map.isWalkable(x, y + 1)) actions.add(Action.MOVE_DOWN)
        if (x > 0 && state.map.isWalkable(x - 1, y)) actions.add(Action.MOVE_LEFT)
        if (x < state.map.width - 1 && state.map.isWalkable(x + 1, y)) actions.add(Action.MOVE_RIGHT)
        if (isAdjacent(enemy.position, state.playerPosition)) actions.add(Action.ATTACK)
        actions.add(Action.WAIT); return actions
    }
    
    private fun isAdjacent(a: Pair<Int, Int>, b: Pair<Int, Int>): Boolean = (kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second)) == 1
    fun getQValue(state: GameState, action: Action): Double = qTable[state]?.get(action) ?: 0.0
    
    fun updateQValue(state: GameState, action: Action, nextState: GameState) {
        val reward = calculateReward(state, action, nextState)
        val currentQ = getQValue(state, action)
        val maxNextQ = getAvailableActions(nextState, EnemyState(0, nextState.playerPosition, 100, 100, "", 0, 0, 0)).maxOfOrNull { getQValue(nextState, it) } ?: 0.0
        val newQ = currentQ + learningRate * (reward + gamma * maxNextQ - currentQ)
        qTable.getOrPut(state) { mutableMapOf() }[action] = newQ
    }
    
    fun calculateReward(state: GameState, action: Action, nextState: GameState): Double {
        var reward = 0.0; val enemy = state.enemies.firstOrNull() ?: return reward
        val distBefore = distance(enemy.position, state.playerPosition)
        val distAfter = distance(nextState.enemies.firstOrNull()?.position ?: enemy.position, nextState.playerPosition)
        reward += (distBefore - distAfter) * 1.0
        if (action == Action.ATTACK) reward += 5.0
        if (nextState.playerHealth <= 0) reward += 10.0
        val enemyHealthDiff = enemy.health - (nextState.enemies.firstOrNull()?.health ?: enemy.health)
        reward -= enemyHealthDiff * 2.0
        return reward
    }
    
    private fun distance(a: Pair<Int, Int>, b: Pair<Int, Int>): Int = kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second)
}
