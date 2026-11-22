package com.yourpackage.dungeoncrawler.telemetry

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

enum class EventCategory { GAMEPLAY, ECONOMY, AI, MONETIZATION, RETENTION, ERROR }
data class LocalEvent(val name: String, val category: EventCategory, val params: Map<String, Any>, val timestamp: Long = System.currentTimeMillis())

class LocalAnalytics(context: Context) {
    private val appContext = context.applicationContext
    private val queue = ConcurrentLinkedQueue<LocalEvent>()
    private val sessionId = UUID.randomUUID().toString()
    private val logDir: File = File(appContext.filesDir, "analytics").apply { mkdirs() }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    
    fun log(name: String, category: EventCategory, vararg params: Pair<String, Any>) { queue.add(LocalEvent(name, category, params.toMap())); if (queue.size >= 50) flush() }
    fun logDeath(cause: String, level: Int, gold: Int, playTime: Long) = log("player_death", EventCategory.GAMEPLAY, "cause" to cause, "level" to level, "gold" to gold, "play_time" to playTime)
    fun logLevelClear(dungeonLevel: Int, time: Long, kills: Int) = log("level_clear", EventCategory.GAMEPLAY, "dungeon_level" to dungeonLevel, "time" to time, "kills" to kills)
    fun logAdWatched(adType: String, reward: String) = log("ad_watched", EventCategory.MONETIZATION, "ad_type" to adType, "reward" to reward)
    fun logAdFailed(adType: String, reason: String) = log("ad_failed", EventCategory.MONETIZATION, "ad_type" to adType, "reason" to reason)
    fun logSessionStart() = log("session_start", EventCategory.RETENTION)
    fun logSessionEnd(duration: Long) = log("session_end", EventCategory.RETENTION, "duration" to duration)
    
    fun flush() {
        if (queue.isEmpty()) return
        val events = mutableListOf<LocalEvent>()
        while (events.size < 500 && queue.isNotEmpty()) queue.poll()?.let { events.add(it) }
        try {
            val logFile = File(logDir, "events_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.log")
            logFile.appendText(events.joinToString("\n") { eventToJson(it) } + "\n")
        } catch (e: Exception) { android.util.Log.e("LocalAnalytics", "Failed to write log", e) }
    }
    
    private fun eventToJson(event: LocalEvent): String {
        val json = JSONObject().apply { put("ts", dateFormat.format(Date(event.timestamp))); put("name", event.name); put("cat", event.category.name); put("sid", sessionId); val p = JSONObject(); event.params.forEach { (k, v) -> p.put(k, v) }; put("p", p) }
        return json.toString()
    }
    
    fun getSummary(): AnalyticsSummary {
        val totalEvents = queue.size + (logDir.listFiles()?.sumOf { it.readLines().size } ?: 0)
        val deaths = queue.count { it.name == "player_death" }
        val levelsCleared = queue.count { it.name == "level_clear" }
        val adsWatched = queue.count { it.name == "ad_watched" }
        return AnalyticsSummary(totalEvents, deaths, levelsCleared, adsWatched, 0, sessionId)
    }
    
    fun exportToFile(): File? = try {
        val exportFile = File(appContext.filesDir, "analytics_export.txt")
        val content = buildString {
            appendLine("# Dungeon Crawler Analytics Export")
            appendLine("# Session: $sessionId")
            appendLine("# Generated: ${dateFormat.format(Date())}")
            appendLine("# This file contains ONLY your local gameplay data.\n")
            logDir.listFiles()?.sortedBy { it.name }?.forEach { file -> appendLine("# --- ${file.name} ---"); appendLine(file.readText()) }
        }
        exportFile.writeText(content); exportFile
    } catch (e: Exception) { null }
    
    fun clearAll() { queue.clear(); logDir.listFiles()?.forEach { it.delete() } }
}

data class AnalyticsSummary(val totalEvents: Int, val deaths: Int, val levelsCleared: Int, val adsWatched: Int, val sessionDurationMs: Long, val sessionId: String)
