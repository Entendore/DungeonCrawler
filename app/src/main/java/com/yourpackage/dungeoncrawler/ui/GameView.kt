package com.yourpackage.dungeoncrawler.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.yourpackage.dungeoncrawler.GameEngine
import com.yourpackage.dungeoncrawler.TileType
import kotlin.math.abs
import kotlin.math.min

class GameView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private var gameEngine: GameEngine? = null
    private val backgroundPaint = Paint().apply { color = Color.rgb(30, 30, 40) }
    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 40f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    private var tileSize = 0f; private var viewportWidth = 15; private var viewportHeight = 15
    private var cameraX = 0f; private var cameraY = 0f
    private var touchStartX = 0f; private var touchStartY = 0f; private var isTouching = false
    var onMoveListener: ((Float, Float) -> Unit)? = null
    var onTapListener: (() -> Unit)? = null
    
    fun setGameEngine(engine: GameEngine) { gameEngine = engine }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { super.onSizeChanged(w, h, oldw, oldh); tileSize = min(w.toFloat() / viewportWidth, h.toFloat() / viewportHeight) }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); val engine = gameEngine ?: return; val state = engine.getCurrentState()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        val targetCameraX = state.playerPosition.first * tileSize - width / 2f + tileSize / 2
        val targetCameraY = state.playerPosition.second * tileSize - height / 2f + tileSize / 2
        cameraX += (targetCameraX - cameraX) * 0.1f; cameraY += (targetCameraY - cameraY) * 0.1f
        val startX = (cameraX / tileSize).toInt() - 1; val startY = (cameraY / tileSize).toInt() - 1
        val endX = startX + viewportWidth + 2; val endY = startY + viewportHeight + 2
        val tilePaint = Paint()
        for (y in startY..endY) for (x in startX..endX) {
            val screenX = x * tileSize - cameraX; val screenY = y * tileSize - cameraY
            val tile = state.map.getTile(x, y)
            tilePaint.color = when (tile) { TileType.WALL -> Color.rgb(80, 80, 100); TileType.FLOOR -> Color.rgb(50, 50, 60); TileType.EXIT -> Color.rgb(100, 255, 100); else -> Color.TRANSPARENT }
            canvas.drawRect(screenX, screenY, screenX + tileSize, screenY + tileSize, tilePaint)
        }
        val enemyPaint = Paint().apply { color = Color.rgb(255, 80, 80) }
        for (enemy in state.enemies) { val sx = enemy.position.first * tileSize - cameraX + tileSize / 2; val sy = enemy.position.second * tileSize - cameraY + tileSize / 2; canvas.drawCircle(sx, sy, tileSize * 0.35f, enemyPaint) }
        val playerPaint = Paint().apply { color = Color.rgb(100, 200, 255) }
        val px = state.playerPosition.first * tileSize - cameraX + tileSize / 2
        val py = state.playerPosition.second * tileSize - cameraY + tileSize / 2
        canvas.drawCircle(px, py, tileSize * 0.4f, playerPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { touchStartX = event.x; touchStartY = event.y; isTouching = true; return true }
            MotionEvent.ACTION_MOVE -> { if (isTouching) { val dx = event.x - touchStartX; val dy = event.y - touchStartY; if (abs(dx) > 50f || abs(dy) > 50f) { onMoveListener?.invoke(dx, dy); touchStartX = event.x; touchStartY = event.y } }; return true }
            MotionEvent.ACTION_UP -> { if (abs(event.x - touchStartX) < 20 && abs(event.y - touchStartY) < 20) onTapListener?.invoke(); isTouching = false; return true }
        }
        return super.onTouchEvent(event)
    }
}
