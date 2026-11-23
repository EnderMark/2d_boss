package com.endermark.bossbattle.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.endermark.bossbattle.model.GameSoundEvent
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundManager(
    initialVolume: Float = AudioSettings.DEFAULT_VOLUME,
    initialMusicEnabled: Boolean = true,
    initialSfxEnabled: Boolean = true
) {

    private class ActiveVoice(
        val samples: FloatArray,
        var index: Int = 0
    )

    private val sfxCache: Map<GameSoundEvent, FloatArray> = buildSfxBuffers()
    private val musicLoop: FloatArray = synthesizeBossMusicLoop()
    private val activeVoices = CopyOnWriteArrayList<ActiveVoice>()

    @Volatile
    var masterVolume: Float = initialVolume.coerceIn(0f, 1f)

    @Volatile
    var musicEnabled: Boolean = initialMusicEnabled

    @Volatile
    var sfxEnabled: Boolean = initialSfxEnabled

    @Volatile
    private var running = false
    private var audioThread: Thread? = null

    fun applySettings(settings: AudioSettings) {
        masterVolume = settings.masterVolume
        musicEnabled = settings.musicEnabled
        sfxEnabled = settings.sfxEnabled
    }

    fun start() {
        if (running) return
        running = true
        audioThread = Thread(::runMixerLoop, "BossBattleAudioMixer").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        val thread = audioThread
        audioThread = null
        if (thread != null && thread !== Thread.currentThread()) {
            try {
                thread.join(350)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        activeVoices.clear()
    }

    fun play(event: GameSoundEvent) {
        if (!sfxEnabled || masterVolume <= 0.001f) return
        val samples = sfxCache[event] ?: return
        if (activeVoices.size >= MAX_ACTIVE_VOICES) {
            activeVoices.removeAt(0)
        }
        activeVoices.add(ActiveVoice(samples))
    }

    private fun runMixerLoop() {
        val chunkSamples = 512
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(chunkSamples * 4)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(minBuf)
                .build()
        } catch (_: Throwable) {
            return
        }

        val pcmBuffer = ShortArray(chunkSamples)
        var musicIndex = 0

        try {
            track.play()
            while (running) {
                val vol = masterVolume
                val playMusic = musicEnabled
                val playSfx = sfxEnabled

                for (i in 0 until chunkSamples) {
                    var mix = 0f
                    if (playMusic) {
                        mix += musicLoop[musicIndex]
                    }
                    musicIndex += 1
                    if (musicIndex >= musicLoop.size) {
                        musicIndex = 0
                    }

                    if (playSfx && activeVoices.isNotEmpty()) {
                        for (voice in activeVoices) {
                            val idx = voice.index
                            if (idx < voice.samples.size) {
                                mix += voice.samples[idx]
                                voice.index = idx + 1
                            }
                        }
                    }

                    val scaled = (mix * vol).coerceIn(-0.98f, 0.98f)
                    pcmBuffer[i] = (scaled * Short.MAX_VALUE).toInt().toShort()
                }

                if (activeVoices.isNotEmpty()) {
                    activeVoices.removeAll { it.index >= it.samples.size }
                }

                track.write(pcmBuffer, 0, chunkSamples)
            }
        } catch (_: Throwable) {
            // Ignore audio interruption during teardown
        } finally {
            try {
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore cleanup errors
            }
        }
    }

    private fun synthesizeBossMusicLoop(): FloatArray {
        val stepDurationSec = 0.16
        val leadNotes = doubleArrayOf(
            293.66, 0.0, 349.23, 392.00,
            440.00, 0.0, 392.00, 349.23,
            329.63, 0.0, 293.66, 261.63,
            293.66, 349.23, 440.00, 523.25
        )
        val bassNotes = doubleArrayOf(
            146.83, 146.83, 174.61, 174.61,
            196.00, 196.00, 174.61, 174.61,
            130.81, 130.81, 146.83, 146.83,
            110.00, 130.81, 146.83, 146.83
        )
        val samplesPerStep = (SAMPLE_RATE * stepDurationSec).toInt()
        val out = FloatArray(samplesPerStep * leadNotes.size)

        for (step in leadNotes.indices) {
            val leadFreq = leadNotes[step]
            val bassFreq = bassNotes[step]
            val start = step * samplesPerStep
            for (i in 0 until samplesPerStep) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 5.5)
                val lead = if (leadFreq > 0.0) {
                    val s = sin(2.0 * PI * leadFreq * t)
                    (if (s >= 0.0) 1.0 else -1.0) * 0.09 * env
                } else {
                    0.0
                }
                val bass = sin(2.0 * PI * bassFreq * t) * 0.12 * exp(-t * 3.0)
                out[start + i] = (lead + bass).toFloat()
            }
        }
        return out
    }

    private fun buildSfxBuffers(): Map<GameSoundEvent, FloatArray> {
        return mapOf(
            GameSoundEvent.JUMP to squareSweep(280.0, 680.0, 0.14, 0.58),
            GameSoundEvent.DOUBLE_JUMP to squareSweep(460.0, 980.0, 0.16, 0.62),
            GameSoundEvent.ATTACK to slashNoise(0.14, 0.65),
            GameSoundEvent.PLAYER_SHOT to squareSweep(920.0, 380.0, 0.08, 0.36),
            GameSoundEvent.BOSS_HIT to impactHit(220.0, 72.0, 0.16, 0.78),
            GameSoundEvent.PLAYER_HIT to impactHit(180.0, 58.0, 0.24, 0.82),
            GameSoundEvent.SHIELD_BLOCK to squareSweep(960.0, 1480.0, 0.14, 0.68),
            GameSoundEvent.BOSS_ATTACK to squareSweep(520.0, 260.0, 0.13, 0.48),
            GameSoundEvent.BOSS_CHARGE to squareSweep(190.0, 440.0, 0.22, 0.55),
            GameSoundEvent.BOSS_VANISH to slashNoise(0.18, 0.52),
            GameSoundEvent.CAMERA_SHUTTER to cameraShutterTone(),
            GameSoundEvent.VICTORY to arpeggioTone(doubleArrayOf(523.25, 659.25, 783.99, 1046.50), 0.13, 0.65),
            GameSoundEvent.DEFEAT to arpeggioTone(doubleArrayOf(392.00, 349.23, 311.13, 220.00), 0.17, 0.65)
        )
    }

    private fun squareSweep(
        startFreq: Double,
        endFreq: Double,
        durationSec: Double,
        volume: Double
    ): FloatArray {
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = FloatArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val wave = if (sin(phase) >= 0.0) 1.0 else -1.0
            val env = (1.0 - progress) * volume
            out[i] = (wave * env).toFloat()
        }
        return out
    }

    private fun slashNoise(durationSec: Double, volume: Double): FloatArray {
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = FloatArray(count)
        val rng = Random(42)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = 750.0 - 450.0 * progress
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val noise = rng.nextDouble(-1.0, 1.0) * 0.55
            val tone = (if (sin(phase) >= 0.0) 1.0 else -1.0) * 0.45
            val env = (1.0 - progress) * volume
            out[i] = ((noise + tone) * env).toFloat()
        }
        return out
    }

    private fun impactHit(
        startFreq: Double,
        endFreq: Double,
        durationSec: Double,
        volume: Double
    ): FloatArray {
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = FloatArray(count)
        val rng = Random(99)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val punch = (if (sin(phase) >= 0.0) 1.0 else -1.0) * 0.65
            val crunch = rng.nextDouble(-1.0, 1.0) * 0.35 * (1.0 - progress)
            val env = exp(-progress * 3.2) * volume
            out[i] = ((punch + crunch) * env).toFloat()
        }
        return out
    }

    private fun cameraShutterTone(): FloatArray {
        val count = (SAMPLE_RATE * 0.16).toInt()
        val out = FloatArray(count)
        val rng = Random(7)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val click1 = exp(-t * 70.0) * (sin(2.0 * PI * 1450.0 * t) + rng.nextDouble(-0.5, 0.5))
            val click2 = if (t > 0.055) {
                exp(-(t - 0.055) * 70.0) * (sin(2.0 * PI * 2000.0 * t) + rng.nextDouble(-0.5, 0.5))
            } else {
                0.0
            }
            out[i] = ((click1 + click2) * 0.62).coerceIn(-0.95, 0.95).toFloat()
        }
        return out
    }

    private fun arpeggioTone(
        freqs: DoubleArray,
        noteDurationSec: Double,
        volume: Double
    ): FloatArray {
        val perNote = (SAMPLE_RATE * noteDurationSec).toInt()
        val out = FloatArray(perNote * freqs.size)
        for (n in freqs.indices) {
            val freq = freqs[n]
            val offset = n * perNote
            for (i in 0 until perNote) {
                val t = i.toDouble() / SAMPLE_RATE
                val wave = if (sin(2.0 * PI * freq * t) >= 0.0) 1.0 else -1.0
                val env = (1.0 - i.toDouble() / perNote) * volume
                out[offset + i] = (wave * env).toFloat()
            }
        }
        return out
    }

    companion object {
        private const val SAMPLE_RATE = 22050
        private const val MAX_ACTIVE_VOICES = 8
    }
}
