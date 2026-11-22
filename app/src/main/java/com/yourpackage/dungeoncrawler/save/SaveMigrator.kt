package com.yourpackage.dungeoncrawler.save

import org.json.JSONObject

object SaveMigrator {
    const val CURRENT_VERSION = 7
    fun migrate(data: JSONObject): JSONObject {
        val version = data.optInt("save_version", 1); if (version >= CURRENT_VERSION) return data
        var current = data
        for (v in version until CURRENT_VERSION) {
            current = when (v) {
                1 -> { current.put("gems", 0); current }
                2 -> { current.put("commander_state", ""); current }
                3 -> { current.put("rival_data", ""); current }
                4 -> { current.put("spells", "[]"); current }
                5 -> { current.put("pet_data", ""); current }
                6 -> current
                else -> current
            }
            current.put("save_version", v + 1)
        }
        return current
    }
}
