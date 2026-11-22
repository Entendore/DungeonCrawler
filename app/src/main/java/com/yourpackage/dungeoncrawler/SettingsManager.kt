package com.yourpackage.dungeoncrawler

import android.content.Context

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var soundEnabled: Boolean; get() = prefs.getBoolean("sound_enabled", true); set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()
    var hapticEnabled: Boolean; get() = prefs.getBoolean("haptic_enabled", true); set(value) = prefs.edit().putBoolean("haptic_enabled", value).apply()
    var fogOfWarEnabled: Boolean; get() = prefs.getBoolean("fog_of_war_enabled", true); set(value) = prefs.edit().putBoolean("fog_of_war_enabled", value).apply()
    var difficulty: String; get() = prefs.getString("difficulty", "Normal") ?: "Normal"; set(value) = prefs.edit().putString("difficulty", value).apply()
}
