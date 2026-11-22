package com.yourpackage.dungeoncrawler

import android.os.Handler
import android.os.Looper
import com.yourpackage.dungeoncrawler.ui.GameView
import kotlin.math.max

class GameLoop(private val gameView: GameView, private val gameEngine: GameEngine) {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var lastUpdateTime = 0L
    private var moveDirectionX = 0f; private var moveDirectionY = 0f
    
    fun start() { isRunning = true; lastUpdateTime = System.currentTimeMillis(); handler.post(gameLoopRunnable) }
    fun stop() { isRunning = false; handler.removeCallbacks(gameLoopRunnable) }
    fun setMoveDirection(dx: Float, dy: Float) { moveDirectionX = dx; moveDirectionY = dy }
    
    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastUpdateTime) / 1000f
            lastUpdateTime = currentTime
            gameView.invalidate()
            handler.postDelayed(this, max(0, 16 - (System.currentTimeMillis() - currentTime)))
        }
    }
}
