package com.yourpackage.dungeoncrawler.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.yourpackage.dungeoncrawler.config.LocalConfig
import com.yourpackage.dungeoncrawler.telemetry.LocalAnalytics

class AdManager(private val context: Context, private val config: LocalConfig, private val analytics: LocalAnalytics) {
    private val bannerAdUnitId = "ca-app-pub-3940256099942544/6300978111"
    private val interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712"
    private val rewardedAdUnitId = "ca-app-pub-3940256099942544/5224354917"
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var turnsSinceLastInterstitial = 0
    private var rewardedAdsWatchedToday = 0
    private var lastAdDay = 0
    private var initialized = false
    
    fun initialize() { if (initialized) return; MobileAds.initialize(context) {}; preloadAll(); initialized = true }
    fun preloadAll() { loadRewardedAd(); loadInterstitialAd() }
    
    private fun loadRewardedAd() {
        RewardedAd.load(context, rewardedAdUnitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
        })
    }
    
    fun isRewardedAdAvailable(): Boolean = rewardedAd != null
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        if (!canWatchRewardedAd()) return
        rewardedAd?.show(activity) { onRewarded(); rewardedAdsWatchedToday++ }
        rewardedAd = null; loadRewardedAd()
    }
    fun canWatchRewardedAd(): Boolean {
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        if (today != lastAdDay) { rewardedAdsWatchedToday = 0; lastAdDay = today }
        return rewardedAd != null && rewardedAdsWatchedToday < config.rewardedDailyCap
    }
    fun getRewardedAdsRemaining(): Int = (config.rewardedDailyCap - rewardedAdsWatchedToday).coerceAtLeast(0)
    
    private fun loadInterstitialAd() {
        InterstitialAd.load(context, interstitialAdUnitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
        })
    }
    
    fun isInterstitialReady(): Boolean = interstitialAd != null
    fun showInterstitialIfReady(activity: Activity): Boolean {
        if (interstitialAd == null) return false
        interstitialAd?.show(activity); interstitialAd = null; loadInterstitialAd(); return true
    }
    fun onTurnCompleted() { turnsSinceLastInterstitial++ }
    fun shouldShowInterstitialOnTurn(): Boolean = turnsSinceLastInterstitial >= config.interstitialInterval
    fun onLevelCleared() { turnsSinceLastInterstitial = 0 }
    fun resetTurnCounter() { turnsSinceLastInterstitial = 0 }
    fun getBannerAdUnitId(): String = bannerAdUnitId
    
    enum class RewardedOffer(val title: String, val description: String) {
        REVIVE("💖 Revive", "Return to life with 50% HP"),
        DOUBLE_GOLD("💰 Double Gold", "2x gold from next 5 kills"),
        BONUS_POTION("🧪 Free Potion", "Get a random potion"),
        RARE_LOOT("🎁 Rare Loot", "Guaranteed rare weapon or armor"),
        XP_BOOST("⚡ XP Boost", "+500 XP instantly"),
        GOLD_RUSH("💵 Gold Rush", "+200 gold instantly"),
        FULL_HEAL("❤️ Full Heal", "Restore all HP"),
        SKIP_LEVEL("⏭️ Skip Level", "Skip to next dungeon level")
    }
    fun getAvailableOffers(): List<RewardedOffer> = RewardedOffer.values().toList()
}
