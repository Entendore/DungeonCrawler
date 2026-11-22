package com.yourpackage.dungeoncrawler.config

import android.content.Context
import org.json.JSONObject

class LocalConfig(context: Context) {
    private val appContext = context.applicationContext
    private val baseConfig: JSONObject = try {
        JSONObject(appContext.assets.open("config/balance.json").bufferedReader().use { it.readText() })
    } catch (e: Exception) { JSONObject() }
    
    fun getFloat(path: String, default: Float = 0f): Float {
        val parts = path.split(".")
        var node: JSONObject? = baseConfig
        for (part in parts) { node = node?.optJSONObject(part); if (node == null) return default }
        return default
    }
    fun getInt(path: String, default: Int = 0): Int {
        val parts = path.split("."); var node: JSONObject? = baseConfig
        for (part in parts) { node = node?.optJSONObject(part); if (node == null) return default }
        return default
    }
    fun getBoolean(path: String, default: Boolean = false): Boolean {
        val parts = path.split("."); var node: JSONObject? = baseConfig
        for (i in 0 until parts.size - 1) { node = node?.optJSONObject(parts[i]); if (node == null) return default }
        return node?.optBoolean(parts.last(), default) ?: default
    }
    
    val enemyHpMultiplier: Float get() = getFloat("balance.enemy_hp_multiplier", 1f)
    val enemyDamageMultiplier: Float get() = getFloat("balance.enemy_damage_multiplier", 1f)
    val critChanceBase: Float get() = getFloat("balance.crit_chance_base", 0.05f)
    val interstitialInterval: Int get() = getInt("ads.interstitial_interval_turns", 15)
    val rewardedDailyCap: Int get() = getInt("ads.rewarded_daily_cap", 20)
    val bannerEnabled: Boolean get() = getBoolean("ads.banner_enabled", true)
    val commanderModeEnabled: Boolean get() = getBoolean("features.commander_mode", true)
    val maxDungeonLevel: Int get() = getInt("content.max_dungeon_level", 50)
}
