package com.yourpackage.dungeoncrawler.config

import android.content.Context

enum class LocalFeature(val key: String, val defaultEnabled: Boolean, val description: String) {
    COMMANDER_MODE("commander_mode", true, "AI commander gameplay"),
    SPELLS("spells", true, "Spell casting system"),
    PETS("pets", true, "Pet companion system"),
    CRAFTING("crafting", true, "Item crafting"),
    RIVAL("rival", true, "Rival AI competitor"),
    FOG_OF_WAR("fog_of_war", true, "Visibility system")
}

class LocalFeatureFlags(private val context: Context, private val config: LocalConfig) {
    private val prefs = context.getSharedPreferences("feature_flags", Context.MODE_PRIVATE)
    fun isEnabled(feature: LocalFeature): Boolean {
        if (prefs.contains("override_${feature.key}")) return prefs.getBoolean("override_${feature.key}", feature.defaultEnabled)
        return config.getBoolean("features.${feature.key}", feature.defaultEnabled)
    }
    fun setOverride(feature: LocalFeature, enabled: Boolean) { prefs.edit().putBoolean("override_${feature.key}", enabled).apply() }
    fun getEnabledFeatures(): List<LocalFeature> = LocalFeature.values().filter { isEnabled(it) }
}
