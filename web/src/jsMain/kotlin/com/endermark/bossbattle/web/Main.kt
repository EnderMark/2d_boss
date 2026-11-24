package com.endermark.bossbattle.web

import com.endermark.bossbattle.engine.BossBattleEngine
import com.endermark.bossbattle.model.BattleMode
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.LevelCatalog
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.math.hypot
import kotlin.math.min

fun main() {
    window.onload = { initApp() }
}

private fun initApp() {
    val menuScreen = document.getElementById("menuScreen") as HTMLDivElement
    val gameScreen = document.getElementById("gameScreen") as HTMLDivElement
    val levelGrid = document.getElementById("levelGrid") as HTMLDivElement
    val canvas = document.getElementById("gameCanvas") as HTMLCanvasElement
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    val backBtn = document.getElementById("backBtn") as HTMLButtonElement
    val muteBtn = document.getElementById("muteBtn") as HTMLButtonElement
    val volSlider = document.getElementById("volSlider") as HTMLInputElement
    val musicToggle = document.getElementById("musicToggle") as HTMLInputElement
    val sfxToggle = document.getElementById("sfxToggle") as HTMLInputElement

    val renderer = WebRenderer()
    val sound = WebSoundManager()
    var engine: BossBattleEngine? = null
    var lastTimeMs = 0.0
    var muted = false

    // Active pointer tracking for multi-touch mobile controls
    val activePointers = mutableMapOf<Int, Pair<Double, Double>>()
    var joyPointerId: Int? = null
    var firePointerId: Int? = null

    // Active keyboard state for desktop controls
    val keysDown = mutableSetOf<String>()

    fun resizeCanvas() {
        val dpr = window.devicePixelRatio.coerceIn(1.0, 2.0)
        canvas.width = (window.innerWidth * dpr).toInt()
        canvas.height = (window.innerHeight * dpr).toInt()
    }
    resizeCanvas()
    window.addEventListener("resize", { resizeCanvas() })

    fun toWorld(clientX: Double, clientY: Double): Pair<Double, Double> {
        val dpr = window.devicePixelRatio.coerceIn(1.0, 2.0)
        val cx = clientX * dpr
        val cy = clientY * dpr
        val w = BossBattleEngine.DEFAULT_WORLD_WIDTH.toDouble()
        val h = BossBattleEngine.DEFAULT_WORLD_HEIGHT.toDouble()
        val scale = min(canvas.width / w, canvas.height / h)
        val offsetX = (canvas.width - w * scale) / 2.0
        val offsetY = (canvas.height - h * scale) / 2.0
        return ((cx - offsetX) / scale) to ((cy - offsetY) / scale)
    }

    fun applyInputs() {
        val eng = engine ?: return
        val lvl = eng.level
        when (lvl.mode) {
            BattleMode.MELEE_PLATFORMER -> {
                var dir = 0
                if (keysDown.contains("ArrowLeft") || keysDown.contains("KeyA")) dir -= 1
                if (keysDown.contains("ArrowRight") || keysDown.contains("KeyD")) dir += 1
                var blocking = keysDown.contains("ShiftLeft") || keysDown.contains("ShiftRight")
                for ((_, pos) in activePointers) {
                    val (wx, wy) = pos
                    if (renderer.leftBtn(lvl.hasShield).contains(wx, wy)) dir -= 1
                    if (renderer.rightBtn(lvl.hasShield).contains(wx, wy)) dir += 1
                    if (lvl.hasShield && renderer.shieldBtn().contains(wx, wy)) blocking = true
                }
                eng.setMoveInput(dir.coerceIn(-1, 1))
                eng.setBlocking(blocking)
            }
            BattleMode.FREE_FLIGHT_SHOOTER -> {
                var jx = 0f
                var jy = 0f
                if (keysDown.contains("ArrowLeft") || keysDown.contains("KeyA")) jx -= 1f
                if (keysDown.contains("ArrowRight") || keysDown.contains("KeyD")) jx += 1f
                if (keysDown.contains("ArrowUp") || keysDown.contains("KeyW")) jy -= 1f
                if (keysDown.contains("ArrowDown") || keysDown.contains("KeyS")) jy += 1f
                val jp = joyPointerId?.let { activePointers[it] }
                if (jp != null) {
                    jx = ((jp.first - renderer.joystickCenterX) / renderer.joystickRadius).toFloat()
                    jy = ((jp.second - renderer.joystickCenterY) / renderer.joystickRadius).toFloat()
                }
                eng.setJoystickInput(jx, jy)
                val firing = firePointerId != null ||
                    keysDown.contains("Space") || keysDown.contains("KeyZ") ||
                    keysDown.contains("KeyJ") || keysDown.contains("Enter")
                eng.setFiring(firing)
            }
            BattleMode.SHOOTER -> {
                if (keysDown.contains("ArrowLeft") || keysDown.contains("KeyA")) {
                    eng.setPlayerTargetX(eng.state.player.x - 45f)
                }
                if (keysDown.contains("ArrowRight") || keysDown.contains("KeyD")) {
                    eng.setPlayerTargetX(eng.state.player.x + 45f)
                }
            }
        }
    }

    fun startLevel(level: BossLevel) {
        sound.unlock()
        activePointers.clear()
        joyPointerId = null
        firePointerId = null
        keysDown.clear()
        engine = BossBattleEngine(level = level)
        menuScreen.style.display = "none"
        gameScreen.style.display = "block"
        resizeCanvas()
        sound.startMusic()
    }

    fun returnToMenu() {
        engine = null
        sound.stopMusic()
        activePointers.clear()
        joyPointerId = null
        firePointerId = null
        keysDown.clear()
        gameScreen.style.display = "none"
        menuScreen.style.display = "flex"
    }

    // Populate Level Select Grid dynamically from shared LevelCatalog
    for (level in LevelCatalog.levels) {
        val card = document.createElement("button") as HTMLButtonElement
        card.className = "level-card"
        val badge = when {
            level.mode == BattleMode.FREE_FLIGHT_SHOOTER -> "SPAZIO 360°"
            level.enablePistol -> "PLATFORM PISTOLA"
            level.mode == BattleMode.MELEE_PLATFORMER -> "PLATFORM"
            else -> "ARCADE"
        }
        card.innerHTML = """
            <div class="level-top">
              <span class="level-title">${level.title}</span>
              <span class="mode-badge">$badge</span>
            </div>
            <div class="boss-name">${level.bossName}</div>
            <div class="level-sub">${level.subtitle}</div>
        """.trimIndent()
        card.onclick = { startLevel(level) }
        levelGrid.appendChild(card)
    }

    // Audio controls
    volSlider.oninput = {
        sound.volume = (volSlider.value.toDoubleOrNull() ?: 80.0) / 100.0
    }
    musicToggle.onchange = { sound.musicEnabled = musicToggle.checked }
    sfxToggle.onchange = { sound.sfxEnabled = sfxToggle.checked }
    backBtn.onclick = { returnToMenu() }
    muteBtn.onclick = {
        muted = !muted
        sound.musicEnabled = !muted && musicToggle.checked
        sound.sfxEnabled = !muted && sfxToggle.checked
        muteBtn.textContent = if (muted) "🔇 MUTO" else "🔊 AUDIO"
    }

    // Pointer / Touch events on Canvas
    canvas.addEventListener("pointerdown", { ev ->
        val pe = ev as PointerEvent
        pe.preventDefault()
        sound.unlock()
        val eng = engine ?: return@addEventListener
        if (eng.state.status != GameStatus.PLAYING) {
            eng.handleTapOrRestart()
            return@addEventListener
        }
        val (wx, wy) = toWorld(pe.clientX.toDouble(), pe.clientY.toDouble())
        activePointers[pe.pointerId] = wx to wy
        when (eng.level.mode) {
            BattleMode.SHOOTER -> eng.setPlayerTargetX(wx.toFloat())
            BattleMode.MELEE_PLATFORMER -> {
                if (renderer.jumpBtn(eng.level.hasShield).contains(wx, wy)) eng.triggerJump()
                if (renderer.attackBtn(eng.level.hasShield).contains(wx, wy)) eng.triggerMeleeAttack()
            }
            BattleMode.FREE_FLIGHT_SHOOTER -> {
                val dFire = hypot(wx - renderer.freeFireCenterX, wy - renderer.freeFireCenterY)
                if (dFire <= renderer.freeFireRadius * 1.45) {
                    firePointerId = pe.pointerId
                } else if (wx < 560.0 || joyPointerId == null) {
                    joyPointerId = pe.pointerId
                }
            }
        }
        applyInputs()
    })

    canvas.addEventListener("pointermove", { ev ->
        val pe = ev as PointerEvent
        if (!activePointers.containsKey(pe.pointerId)) return@addEventListener
        pe.preventDefault()
        val eng = engine ?: return@addEventListener
        val (wx, wy) = toWorld(pe.clientX.toDouble(), pe.clientY.toDouble())
        activePointers[pe.pointerId] = wx to wy
        if (eng.level.mode == BattleMode.SHOOTER) {
            eng.setPlayerTargetX(wx.toFloat())
        }
        applyInputs()
    })

    val releasePointer: (org.w3c.dom.events.Event) -> Unit = { ev ->
        val pe = ev as PointerEvent
        activePointers.remove(pe.pointerId)
        if (joyPointerId == pe.pointerId) joyPointerId = null
        if (firePointerId == pe.pointerId) firePointerId = null
        applyInputs()
    }
    canvas.addEventListener("pointerup", releasePointer)
    canvas.addEventListener("pointercancel", releasePointer)

    // Keyboard controls for Desktop
    window.addEventListener("keydown", { ev ->
        val ke = ev as KeyboardEvent
        val eng = engine ?: return@addEventListener
        if (ke.code == "Escape") {
            returnToMenu()
            return@addEventListener
        }
        if (ke.code in setOf("ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "Space")) {
            ke.preventDefault()
        }
        sound.unlock()
        if (eng.state.status != GameStatus.PLAYING) {
            if (ke.code == "Space" || ke.code == "Enter") eng.handleTapOrRestart()
            return@addEventListener
        }
        val firstPress = keysDown.add(ke.code)
        if (firstPress && eng.level.mode == BattleMode.MELEE_PLATFORMER) {
            if (ke.code == "Space" || ke.code == "ArrowUp" || ke.code == "KeyW") eng.triggerJump()
            if (ke.code == "KeyZ" || ke.code == "KeyJ" || ke.code == "Enter") eng.triggerMeleeAttack()
        }
        applyInputs()
    })

    window.addEventListener("keyup", { ev ->
        val ke = ev as KeyboardEvent
        keysDown.remove(ke.code)
        applyInputs()
    })

    // 60fps Game Loop
    fun frame(nowMs: Double) {
        val dt = if (lastTimeMs == 0.0) 0.016f else ((nowMs - lastTimeMs) / 1000.0).toFloat().coerceIn(0f, 0.05f)
        lastTimeMs = nowMs
        val eng = engine
        if (eng != null) {
            applyInputs()
            eng.update(dt)
            for (evt in eng.drainSoundEvents()) {
                sound.play(evt)
            }
            renderer.render(ctx, eng.state, canvas.width.toDouble(), canvas.height.toDouble())
        }
        window.requestAnimationFrame(::frame)
    }
    window.requestAnimationFrame(::frame)
}
