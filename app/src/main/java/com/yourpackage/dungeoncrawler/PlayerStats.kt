package com.yourpackage.dungeoncrawler

data class PlayerStats(
    var level: Int = 1, var xp: Int = 0, var xpToNextLevel: Int = 100,
    var strength: Int = 10, var agility: Int = 10, var vitality: Int = 10,
    var skillPoints: Int = 0, var gold: Int = 0, var totalKills: Int = 0,
    var totalDamageDealt: Int = 0, var totalDamageTaken: Int = 0, var deepestLevel: Int = 1,
    var critChance: Float = 0.05f, var dodgeChance: Float = 0.05f
) {
    fun getMaxHealth(): Int = 100 + vitality * 10
    fun getAttackDamage(): Int = 10 + strength * 2
    fun addXP(amount: Int): Boolean {
        xp += amount
        var leveledUp = false
        while (xp >= xpToNextLevel) { xp -= xpToNextLevel; level++; skillPoints += 3; xpToNextLevel = (xpToNextLevel * 1.5).toInt(); leveledUp = true }
        return leveledUp
    }
}
