package com.yourpackage.dungeoncrawler.save

import android.content.Context
import org.json.JSONObject
import java.io.File

class SaveManager(context: Context) {
    private val appContext = context.applicationContext
    private val saveDir: File = File(appContext.filesDir, "saves").apply { mkdirs() }
    private val backupDir: File = File(appContext.filesDir, "saves_backup").apply { mkdirs() }
    private val maxSlots = 5
    
    data class SaveSlot(val id: Int, val name: String, val timestamp: Long, val playerLevel: Int, val dungeonLevel: Int, val playTime: Long, val isValid: Boolean)
    
    fun getSaveSlots(): List<SaveSlot> {
        val slots = mutableListOf<SaveSlot>()
        for (i in 1..maxSlots) {
            val file = File(saveDir, "slot_$i.json")
            if (file.exists()) {
                try {
                    val json = JSONObject(file.readText())
                    slots.add(SaveSlot(i, json.optString("name", "Save $i"), json.optLong("timestamp", 0), json.optInt("playerLevel", 1), json.optInt("dungeonLevel", 1), json.optLong("playTime", 0), true))
                } catch (e: Exception) { slots.add(SaveSlot(i, "Corrupted", 0, 0, 0, 0, false)) }
            } else slots.add(SaveSlot(i, "Empty Slot", 0, 0, 0, 0, false))
        }
        return slots
    }
    
    fun saveGame(slotId: Int, data: JSONObject, name: String): Boolean = try {
        val file = File(saveDir, "slot_$slotId.json")
        if (file.exists()) file.copyTo(File(backupDir, "slot_${slotId}_backup.json"), overwrite = true)
        data.put("name", name); data.put("timestamp", System.currentTimeMillis()); data.put("save_version", SaveMigrator.CURRENT_VERSION)
        file.writeText(data.toString(2)); true
    } catch (e: Exception) { false }
    
    fun loadGame(slotId: Int): JSONObject? = try {
        val file = File(saveDir, "slot_$slotId.json"); if (!file.exists()) return null
        SaveMigrator.migrate(JSONObject(file.readText()))
    } catch (e: Exception) { null }
    
    fun deleteSave(slotId: Int) { File(saveDir, "slot_$slotId.json").delete(); File(backupDir, "slot_${slotId}_backup.json").delete() }
    fun getAutoSaveFile(): File = File(saveDir, "autosave.json")
    fun autoSave(data: JSONObject): Boolean = try { val file = getAutoSaveFile(); data.put("timestamp", System.currentTimeMillis()); data.put("save_version", SaveMigrator.CURRENT_VERSION); file.writeText(data.toString(2)); true } catch (e: Exception) { false }
    fun loadAutoSave(): JSONObject? = try { val file = getAutoSaveFile(); if (!file.exists()) return null; SaveMigrator.migrate(JSONObject(file.readText())) } catch (e: Exception) { null }
}
