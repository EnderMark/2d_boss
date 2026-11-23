package com.endermark.bossbattle.ui

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.endermark.bossbattle.audio.SoundManager
import com.endermark.bossbattle.engine.BossBattleEngine
import com.endermark.bossbattle.model.BattleMode
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.LevelCatalog
import kotlin.math.min

class GameSurfaceView(
    context: Context,
    private val level: BossLevel = LevelCatalog.LEVEL_1,
    private val onExitToMenu: () -> Unit = {}
) : SurfaceView(context), SurfaceHolder.Callback {

    private val engine = BossBattleEngine(level = level)
    private val renderer = GameRenderer()
    private val soundManager = SoundManager()
    private val stateLock = Any()

    @Volatile
    private var running = false
    private var gameThread: Thread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        soundManager.startMusic()
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // World coordinates are scaled dynamically in GameRenderer.
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        soundManager.stopMusic()
        stopLoop()
    }

    fun resumeGame() {
        soundManager.startMusic()
        if (holder.surface.isValid) {
            startLoop()
        }
    }

    fun pauseGame() {
        soundManager.stopMusic()
        stopLoop()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var exitRequested = false
        synchronized(stateLock) {
            val action = event.actionMasked
            val actionIndex = event.actionIndex
            val (actionX, actionY) = screenToWorld(
                event.getX(actionIndex),
                event.getY(actionIndex)
            )

            if (engine.state.status != GameStatus.PLAYING) {
                if (action == MotionEvent.ACTION_DOWN) {
                    if (renderer.isMenuButtonHit(actionX, actionY, engine.state)) {
                        exitRequested = true
                    } else {
                        engine.handleTapOrRestart()
                        if (level.mode == BattleMode.SHOOTER) {
                            engine.setPlayerTargetX(actionX)
                        }
                    }
                }
            } else if (level.mode == BattleMode.SHOOTER) {
                when (action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        val (x, _) = screenToWorld(event.x, event.y)
                        engine.setPlayerTargetX(x)
                    }
                }
            } else {
                handleMeleeTouch(event, action, actionIndex, actionX, actionY)
            }
        }
        if (exitRequested) {
            soundManager.stopMusic()
            post { onExitToMenu() }
        }
        return true
    }

    private fun handleMeleeTouch(
        event: MotionEvent,
        action: Int,
        actionIndex: Int,
        actionX: Float,
        actionY: Float
    ) {
        val hasShield = level.hasShield
        val leftRect = renderer.leftButtonRect(hasShield)
        val rightRect = renderer.rightButtonRect(hasShield)
        val shieldRect = renderer.shieldButtonRect()
        val jumpRect = renderer.jumpButtonRect(hasShield)
        val attackRect = renderer.attackButtonRect(hasShield)

        // Discrete button presses for Jump (including double jump) and Melee Attack
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (jumpRect.contains(actionX, actionY)) {
                engine.triggerJump()
            } else if (attackRect.contains(actionX, actionY)) {
                engine.triggerMeleeAttack()
            }
        }

        // Continuous multi-touch check for Left / Right movement and Shield hold
        var moveDir = 0
        var blocking = false
        val ignoreIndex = if (
            action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_POINTER_UP ||
            action == MotionEvent.ACTION_CANCEL
        ) {
            actionIndex
        } else {
            -1
        }

        if (action != MotionEvent.ACTION_CANCEL) {
            for (i in 0 until event.pointerCount) {
                if (i == ignoreIndex) continue
                val (px, py) = screenToWorld(event.getX(i), event.getY(i))
                when {
                    leftRect.contains(px, py) -> moveDir = -1
                    rightRect.contains(px, py) -> moveDir = 1
                    hasShield && shieldRect.contains(px, py) -> blocking = true
                }
            }
        }
        engine.setMoveInput(moveDir)
        engine.setBlocking(blocking)
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

            val soundEvents: List<com.endermark.bossbattle.model.GameSoundEvent>
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    synchronized(stateLock) {
                        engine.update(dt)
                        soundEvents = engine.drainSoundEvents()
                        renderer.render(canvas, engine.state)
                    }
                    for (event in soundEvents) {
                        soundManager.play(event)
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
