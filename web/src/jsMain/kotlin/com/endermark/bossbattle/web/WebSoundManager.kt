package com.endermark.bossbattle.web

import com.endermark.bossbattle.model.GameSoundEvent
import kotlinx.browser.window

class WebSoundManager {
    var volume: Double = 0.8
    var musicEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) stopMusic() else if (isPlayingLevel) startMusic()
        }
    var sfxEnabled: Boolean = true

    private var ctx: dynamic = null
    private var musicTimerId: Int? = null
    private var musicStep: Int = 0
    private var isPlayingLevel: Boolean = false

    private val melodyHz = doubleArrayOf(
        261.63, 329.63, 392.00, 523.25,
        392.00, 329.63, 293.66, 349.23,
        440.00, 587.33, 440.00, 349.23,
        329.63, 392.00, 493.88, 659.25
    )

    fun unlock() {
        if (ctx == null) {
            ctx = js("new (window.AudioContext || window.webkitAudioContext)()")
        }
        if (ctx != null && ctx.state == "suspended") {
            ctx.resume()
        }
    }

    fun startMusic() {
        isPlayingLevel = true
        if (!musicEnabled || musicTimerId != null) return
        unlock()
        musicTimerId = window.setInterval({
            if (!musicEnabled || volume <= 0.01) return@setInterval
            val freq = melodyHz[musicStep % melodyHz.size]
            musicStep += 1
            playTone(freq, 0.14, "square", 0.08 * volume)
            if (musicStep % 2 == 0) {
                playTone(freq * 0.5, 0.16, "triangle", 0.09 * volume)
            }
        }, 185)
    }

    fun stopMusic() {
        isPlayingLevel = false
        val id = musicTimerId
        if (id != null) {
            window.clearInterval(id)
            musicTimerId = null
        }
    }

    fun play(event: GameSoundEvent) {
        if (!sfxEnabled || volume <= 0.01) return
        unlock()
        val gain = 0.22 * volume
        when (event) {
            GameSoundEvent.PLAYER_SHOT -> playSweep(780.0, 320.0, 0.08, "square", gain)
            GameSoundEvent.ATTACK -> playSweep(420.0, 180.0, 0.10, "sawtooth", gain)
            GameSoundEvent.JUMP -> playSweep(260.0, 520.0, 0.11, "square", gain)
            GameSoundEvent.DOUBLE_JUMP -> playSweep(420.0, 820.0, 0.12, "square", gain)
            GameSoundEvent.BOSS_HIT -> playSweep(220.0, 110.0, 0.09, "sawtooth", gain * 1.1)
            GameSoundEvent.PLAYER_HIT -> playSweep(180.0, 65.0, 0.20, "sawtooth", gain * 1.2)
            GameSoundEvent.SHIELD_BLOCK -> playSweep(620.0, 920.0, 0.09, "triangle", gain)
            GameSoundEvent.BOSS_ATTACK -> playSweep(340.0, 170.0, 0.12, "square", gain * 0.85)
            GameSoundEvent.BOSS_CHARGE -> playSweep(180.0, 620.0, 0.25, "sawtooth", gain)
            GameSoundEvent.BOSS_VANISH -> playSweep(700.0, 220.0, 0.18, "sine", gain)
            GameSoundEvent.CAMERA_SHUTTER -> playSweep(1200.0, 300.0, 0.06, "square", gain)
            GameSoundEvent.PICKUP -> playSweep(523.0, 1046.0, 0.16, "triangle", gain)
            GameSoundEvent.ARMOR_BREAK -> playSweep(480.0, 90.0, 0.28, "sawtooth", gain * 1.25)
            GameSoundEvent.LASER_FIRE -> playSweep(960.0, 240.0, 0.26, "sawtooth", gain * 1.1)
            GameSoundEvent.VICTORY -> {
                playTone(523.25, 0.14, "square", gain)
                window.setTimeout({ playTone(659.25, 0.14, "square", gain) }, 140)
                window.setTimeout({ playTone(783.99, 0.28, "square", gain) }, 280)
            }
            GameSoundEvent.DEFEAT -> {
                playTone(329.63, 0.16, "sawtooth", gain)
                window.setTimeout({ playTone(261.63, 0.16, "sawtooth", gain) }, 160)
                window.setTimeout({ playTone(196.00, 0.32, "sawtooth", gain) }, 320)
            }
        }
    }

    private fun playTone(freq: Double, duration: Double, type: String, gainVal: Double) {
        val c = ctx ?: return
        try {
            val now = c.currentTime as Double
            val osc = c.createOscillator()
            val g = c.createGain()
            osc.type = type
            osc.frequency.setValueAtTime(freq, now)
            g.gain.setValueAtTime(gainVal, now)
            g.gain.exponentialRampToValueAtTime(0.001, now + duration)
            osc.connect(g)
            g.connect(c.destination)
            osc.start(now)
            osc.stop(now + duration)
        } catch (_: Throwable) {
        }
    }

    private fun playSweep(startHz: Double, endHz: Double, duration: Double, type: String, gainVal: Double) {
        val c = ctx ?: return
        try {
            val now = c.currentTime as Double
            val osc = c.createOscillator()
            val g = c.createGain()
            osc.type = type
            osc.frequency.setValueAtTime(startHz, now)
            osc.frequency.exponentialRampToValueAtTime(endHz.coerceAtLeast(20.0), now + duration)
            g.gain.setValueAtTime(gainVal, now)
            g.gain.exponentialRampToValueAtTime(0.001, now + duration)
            osc.connect(g)
            g.connect(c.destination)
            osc.start(now)
            osc.stop(now + duration)
        } catch (_: Throwable) {
        }
    }
}
