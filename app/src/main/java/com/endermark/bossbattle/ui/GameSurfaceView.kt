package com.endermark.bossbattle.ui

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.endermark.bossbattle.engine.BossBattleEngine
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.LevelCatalog
import kotlin.math.min

class GameSurfaceView(
    context: Context,
    level: BossLevel = LevelCatalog.LEVEL_1,
    private val onExitToMenu: () -> Unit = {}
) : SurfaceView(context), SurfaceHolder.Callback {

    private val engine = BossBattleEngine(level = level)
    private val renderer = GameRenderer()
    private val stateLock = Any()

    @Volatile
    private var running = false
    private var gameThread: Thread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // World coordinates are scaled dynamically in GameRenderer.
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    fun resumeGame() {
        if (holder.surface.isValid) {
            startLoop()
        }
    }

    fun pauseGame() {
        stopLoop()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val (worldX, worldY) = screenToWorld(event.x, event.y)
        var exitRequested = false
        synchronized(stateLock) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (renderer.isMenuButtonHit(worldX, worldY, engine.state)) {
                        exitRequested = true
                    } else {
                        engine.handleTapOrRestart()
                        engine.setPlayerTargetX(worldX)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    engine.setPlayerTargetX(worldX)
                }
            }
        }
        if (exitRequested) {
            post { onExitToMenu() }
        }
        return true
    }

    private fun screenToWorld(screenX: Float, screenY: Float): Pair<Float, Float> {
        if (width <= 0 || height <= 0) {
            return BossBattleEngine.DEFAULT_WORLD_WIDTH / 2f to
                BossBattleEngine.DEFAULT_WORLD_HEIGHT / 2f
        }
        val worldWidth = engine.state.worldWidth
        val worldHeight = engine.state.worldHeight
        val scale = min(width.toFloat() / worldWidth, height.toFloat() / worldHeight)
        val offsetX = (width - worldWidth * scale) / 2f
        val offsetY = (height - worldHeight * scale) / 2f
        return ((screenX - offsetX) / scale) to ((screenY - offsetY) / scale)
    }

    private fun startLoop() {
        if (running) return
        running = true
        gameThread = Thread(::runGameLoop, "BossBattleLoop").apply { start() }
    }

    private fun stopLoop() {
        running = false
        val thread = gameThread
        gameThread = null
        if (thread != null && thread !== Thread.currentThread()) {
            try {
                thread.join(500)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun runGameLoop() {
        var lastTimeNs = System.nanoTime()
        while (running) {
            val nowNs = System.nanoTime()
            val dt = (nowNs - lastTimeNs) / 1_000_000_000f
            lastTimeNs = nowNs

            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    synchronized(stateLock) {
                        engine.update(dt)
                        renderer.render(canvas, engine.state)
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            val frameDurationMs = (System.nanoTime() - nowNs) / 1_000_000L
            val sleepMs = TARGET_FRAME_MS - frameDurationMs
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    companion object {
        private const val TARGET_FRAME_MS = 16L
    }
}
