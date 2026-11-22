package com.yourpackage.dungeoncrawler

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.yourpackage.dungeoncrawler.ads.AdManager
import com.yourpackage.dungeoncrawler.config.LocalConfig
import com.yourpackage.dungeoncrawler.config.LocalFeatureFlags
import com.yourpackage.dungeoncrawler.save.SaveManager
import com.yourpackage.dungeoncrawler.telemetry.LocalAnalytics
import com.yourpackage.dungeoncrawler.telemetry.LocalCrashLog
import com.yourpackage.dungeoncrawler.ui.GameView

class MainActivity : AppCompatActivity() {
    private lateinit var config: LocalConfig
    private lateinit var features: LocalFeatureFlags
    private lateinit var analytics: LocalAnalytics
    private lateinit var crashLog: LocalCrashLog
    private lateinit var adManager: AdManager
    private lateinit var saveManager: SaveManager
    private lateinit var gameEngine: GameEngine
    private lateinit var gameView: GameView
    private lateinit var gameLoop: GameLoop
    private lateinit var statusText: TextView
    private lateinit var statsText: TextView
    private lateinit var narrativeLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        config = LocalConfig(this)
        features = LocalFeatureFlags(this, config)
        analytics = LocalAnalytics(this)
        crashLog = LocalCrashLog(this).also { it.install() }
        adManager = AdManager(this, config, analytics).also { it.initialize() }
        saveManager = SaveManager(this)
        
        gameEngine = GameEngine(MapAlgorithm.BSP).also {
            it.initialize(this)
            it.setConfig(config)
            it.setFeatures(features)
            it.setAnalytics(analytics)
        }
        
        val autoSave = saveManager.loadAutoSave()
        if (autoSave != null) gameEngine.loadFromJson(autoSave)
        
        gameView = findViewById(R.id.gameView)
        gameView.setGameEngine(gameEngine)
        statusText = findViewById(R.id.statusText)
        statsText = findViewById(R.id.statsText)
        narrativeLog = findViewById(R.id.narrativeLog)
        
        setupGameLoop()
        setupControls()
        setupButtons()
        setupBannerAd()
        startNarrativeUpdater()
        
        analytics.logSessionStart()
    }
    
    private fun setupBannerAd() {
        if (!config.bannerEnabled) {
            findViewById<AdView>(R.id.adView).visibility = android.view.View.GONE
            return
        }
        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())
    }
    
    private fun setupGameLoop() {
        gameLoop = GameLoop(gameView, gameEngine)
        gameLoop.start()
    }
    
    private fun setupControls() {
        gameView.onMoveListener = { dx, dy ->
            val threshold = 50f
            val moveX = if (kotlin.math.abs(dx) > threshold) dx / kotlin.math.abs(dx) else 0f
            val moveY = if (kotlin.math.abs(dy) > threshold) dy / kotlin.math.abs(dy) else 0f
            gameLoop.setMoveDirection(moveX, moveY)
        }
        gameView.onTapListener = { gameEngine.processRealTimeAction(Action.ATTACK) }
    }
    
    private fun setupButtons() {
        findViewById<Button>(R.id.btnUp).setOnClickListener { gameLoop.setMoveDirection(0f, -1f) }
        findViewById<Button>(R.id.btnDown).setOnClickListener { gameLoop.setMoveDirection(0f, 1f) }
        findViewById<Button>(R.id.btnLeft).setOnClickListener { gameLoop.setMoveDirection(-1f, 0f) }
        findViewById<Button>(R.id.btnRight).setOnClickListener { gameLoop.setMoveDirection(1f, 0f) }
        findViewById<Button>(R.id.btnAttack).setOnClickListener { gameEngine.processRealTimeAction(Action.ATTACK) }
        findViewById<Button>(R.id.btnStats).setOnClickListener { showStatsDialog() }
        findViewById<Button>(R.id.btnInventory).setOnClickListener { showInventoryDialog() }
        findViewById<Button>(R.id.btnQuests).setOnClickListener { showQuestsDialog() }
        findViewById<Button>(R.id.btnWatchAd).setOnClickListener { showRewardedAdMenu() }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { showSettingsDialog() }
        findViewById<Button>(R.id.btnSaveLoad).setOnClickListener { showSaveLoadDialog() }
        findViewById<Button>(R.id.btnMode).setOnClickListener { gameEngine.togglePlayMode() }
    }
    
    private fun startNarrativeUpdater() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateUI()
                handler.postDelayed(this, 200)
            }
        }, 200)
    }
    
    private fun updateUI() {
        val state = gameEngine.getCurrentState()
        statusText.text = "❤️${state.playerHealth}/${state.playerStats.getMaxHealth()}  💰${state.playerStats.gold}  Lv${state.dungeonLevel}  Turn ${state.turn}"
        statsText.text = "⚔${state.playerStats.getAttackDamage()} 🛡${state.inventory.getDefenseBonus()} | Lvl${state.playerStats.level} XP:${state.playerStats.xp}/${state.playerStats.xpToNextLevel} | 👹${state.enemies.size}"
        
        val spannable = android.text.SpannableStringBuilder()
        for (line in gameEngine.recentNarrative) {
            val color = when (line.style) {
                NarrativeLine.Style.DANGER -> 0xFFFF5252.toInt()
                NarrativeLine.Style.TRIUMPH -> 0xFF69F0AE.toInt()
                NarrativeLine.Style.MYSTIC -> 0xFFB388FF.toInt()
                NarrativeLine.Style.COMBAT -> 0xFFFFD54F.toInt()
                NarrativeLine.Style.NORMAL -> 0xFFCCCCCC.toInt()
            }
            val start = spannable.length
            spannable.append(line.text).append("\n")
            spannable.setSpan(android.text.style.ForegroundColorSpan(color), start, start + line.text.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        narrativeLog.text = spannable
    }
    
    private fun showRewardedAdMenu() {
        if (!adManager.canWatchRewardedAd()) {
            Toast.makeText(this, "Daily ad limit reached", Toast.LENGTH_SHORT).show()
            return
        }
        val offers = adManager.getAvailableOffers()
        val items = offers.map { "${it.title}\n   ${it.description}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("📺 Watch Ad (${adManager.getRewardedAdsRemaining()} left)")
            .setItems(items) { _, which -> adManager.showRewardedAd(this) { grantReward(offers[which]) } }
            .setNegativeButton("Close", null).show()
    }
    
    private fun grantReward(offer: AdManager.RewardedOffer) {
        val state = gameEngine.getCurrentState()
        when (offer) {
            AdManager.RewardedOffer.REVIVE -> if (gameEngine.isGameOver()) gameEngine.revivePlayer()
            AdManager.RewardedOffer.DOUBLE_GOLD -> gameEngine.doubleGoldTurnsRemaining = 5
            AdManager.RewardedOffer.BONUS_POTION -> state.inventory.addItem(Item.generatePotion())
            AdManager.RewardedOffer.RARE_LOOT -> state.inventory.addItem(Item.generateWeapon(state.dungeonLevel + 2))
            AdManager.RewardedOffer.XP_BOOST -> state.playerStats.addXP(500)
            AdManager.RewardedOffer.GOLD_RUSH -> state.playerStats.gold += 200
            AdManager.RewardedOffer.FULL_HEAL -> state.playerHealth = state.playerStats.getMaxHealth()
            AdManager.RewardedOffer.SKIP_LEVEL -> gameEngine.skipToNextLevel()
        }
        Toast.makeText(this, "Reward granted!", Toast.LENGTH_SHORT).show()
    }
    
    private fun showStatsDialog() {
        val stats = gameEngine.getCurrentState().playerStats
        val msg = "Level: ${stats.level}\nXP: ${stats.xp}/${stats.xpToNextLevel}\nStrength: ${stats.strength}\nAgility: ${stats.agility}\nVitality: ${stats.vitality}\nKills: ${stats.totalKills}\nGold: ${stats.gold}"
        AlertDialog.Builder(this).setTitle("📊 Stats").setMessage(msg).setPositiveButton("Close", null).show()
    }
    
    private fun showInventoryDialog() {
        val inv = gameEngine.getCurrentState().inventory
        val items = mutableListOf<String>()
        items.add("=== EQUIPPED ===")
        items.add("Weapon: ${inv.equippedWeapon?.name ?: "None"}")
        items.add("Armor: ${inv.equippedArmor?.name ?: "None"}")
        items.add("\n=== WEAPONS (${inv.weapons.size}) ===")
        inv.weapons.forEach { items.add(it.name) }
        items.add("\n=== ARMOR (${inv.armors.size}) ===")
        inv.armors.forEach { items.add(it.name) }
        items.add("\n=== POTIONS (${inv.consumables.size}) ===")
        inv.consumables.forEach { items.add(it.name) }
        AlertDialog.Builder(this).setTitle("🎒 Inventory").setItems(items.toTypedArray(), null).setNegativeButton("Close", null).show()
    }
    
    private fun showQuestsDialog() {
        Toast.makeText(this, "Quests coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    private fun showSettingsDialog() {
        val options = arrayOf("Sound: ${if (gameEngine.settings.soundEnabled) "ON" else "OFF"}", "Haptic: ${if (gameEngine.settings.hapticEnabled) "ON" else "OFF"}", "Fog of War: ${if (gameEngine.settings.fogOfWarEnabled) "ON" else "OFF"}", "Privacy", "About")
        AlertDialog.Builder(this).setTitle("⚙️ Settings").setItems(options) { _, which ->
            when (which) {
                0 -> { gameEngine.settings.soundEnabled = !gameEngine.settings.soundEnabled; showSettingsDialog() }
                1 -> { gameEngine.settings.hapticEnabled = !gameEngine.settings.hapticEnabled; showSettingsDialog() }
                2 -> { gameEngine.settings.fogOfWarEnabled = !gameEngine.settings.fogOfWarEnabled; showSettingsDialog() }
                3 -> showPrivacyDialog()
                4 -> showAboutDialog()
            }
        }.setNegativeButton("Close", null).show()
    }
    
    private fun showPrivacyDialog() {
        val summary = analytics.getSummary()
        val msg = "🔒 YOUR PRIVACY\n\nThis game is 100% local. Only ad requests leave your device.\n\n📊 Local Analytics:\n• Events: ${summary.totalEvents}\n• Deaths: ${summary.deaths}\n• Levels: ${summary.levelsCleared}\n• Ads watched: ${summary.adsWatched}\n\nYou can export or delete this data anytime."
        AlertDialog.Builder(this).setTitle("🔒 Privacy").setMessage(msg).setNeutralButton("Export") { _, _ ->
            val file = analytics.exportToFile()
            Toast.makeText(this, "Exported to: ${file?.absolutePath}", Toast.LENGTH_LONG).show()
        }.setNegativeButton("Clear All") { _, _ -> analytics.clearAll(); Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show() }
        .setPositiveButton("Close", null).show()
    }
    
    private fun showAboutDialog() {
        val msg = "Dungeon Crawler v${BuildConfig.VERSION_NAME}\n\n100% local. No accounts. No tracking.\n\nFeatures:\n• MDP-based AI that learns you\n• Commander Mode\n• Rival system\n• 6 themed biomes\n• Procedural narrative\n• And much more"
        AlertDialog.Builder(this).setTitle("About").setMessage(msg).setPositiveButton("Close", null).show()
    }
    
    private fun showSaveLoadDialog() {
        val slots = saveManager.getSaveSlots()
        val items = slots.map { slot ->
            if (slot.isValid) {
                val date = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(slot.timestamp))
                "Slot ${slot.id}: ${slot.name}\n   Lv${slot.playerLevel} · Depth ${slot.dungeonLevel} · $date"
            } else "Slot ${slot.id}: Empty"
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle("💾 Save/Load").setItems(items) { _, which ->
            val slot = slots[which]
            if (slot.isValid) showSlotOptions(slot) else showSaveDialog(which + 1)
        }.setNegativeButton("Close", null).show()
    }
    
    private fun showSlotOptions(slot: SaveManager.SaveSlot) {
        AlertDialog.Builder(this).setTitle("Slot ${slot.id}: ${slot.name}")
            .setItems(arrayOf("Load", "Overwrite", "Delete")) { _, which ->
                when (which) {
                    0 -> { val data = saveManager.loadGame(slot.id); if (data != null && gameEngine.loadFromJson(data)) Toast.makeText(this, "Loaded!", Toast.LENGTH_SHORT).show() }
                    1 -> showSaveDialog(slot.id)
                    2 -> { saveManager.deleteSave(slot.id); Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show() }
                }
            }.setNegativeButton("Cancel", null).show()
    }
    
    private fun showSaveDialog(slotId: Int) {
        val input = EditText(this).apply { hint = "Save name"; setText("Save $slotId") }
        AlertDialog.Builder(this).setTitle("Save Game").setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().ifBlank { "Save $slotId" }
                if (saveManager.saveGame(slotId, gameEngine.toJson(), name)) Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }
    
    override fun onPause() {
        super.onPause()
        gameLoop.stop()
        saveManager.autoSave(gameEngine.toJson())
        analytics.flush()
    }
    
    override fun onResume() {
        super.onResume()
        gameLoop.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        analytics.flush()
    }
}
