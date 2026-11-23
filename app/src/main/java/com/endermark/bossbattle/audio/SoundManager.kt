package com.endermark.bossbattle.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.endermark.bossbattle.model.GameSoundEvent
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager {

    private val sfxExecutor = Executors.newCachedThreadPool()
    private val sfxCache: Map<GameSoundEvent, ShortArray> = buildSfxBuffers()

    @Volatile
    private var musicPlaying = false
    private var musicThread: Thread? = null

    fun startMusic() {
        if (musicPlaying) return
        musicPlaying = true
        musicThread = Thread(::runMusicLoop, "BossBattleMusic").apply {
            isDaemon = true
            start()
        }
    }

    fun stopMusic() {
        musicPlaying = false
        val thread = musicThread
        musicThread = null
        if (thread != null && thread !== Thread.currentThread()) {
            try {
                thread.join(300)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    fun play(event: GameSoundEvent) {
        val samples = sfxCache[event] ?: return
        sfxExecutor.execute {
            playPcmBuffer(samples)
        }
    }

    fun release() {
        stopMusic()
        sfxExecutor.shutdownNow()
    }

    private fun runMusicLoop() {
        val bufferSamples = synthesizeBossMusicLoop()
        val byteSize = bufferSamples.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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

        try {
            track.play()
            val chunkSize = 2048
            var offset = 0
            while (musicPlaying) {
                val toWrite = minOf(chunkSize, bufferSamples.size - offset)
                track.write(bufferSamples, offset, toWrite)
                offset += toWrite
                if (offset >= bufferSamples.size) {
                    offset = 0
                }
            }
        } catch (_: Throwable) {
            // Ignore audio device interruption on pause/teardown
        } finally {
            try {
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore cleanup errors
            }
        }
    }

    private fun playPcmBuffer(samples: ShortArray) {
        val byteSize = samples.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(byteSize)

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
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(minBuf)
                .build()
        } catch (_: Throwable) {
            return
        }

        try {
            track.write(samples, 0, samples.size)
            track.play()
            val durationMs = (samples.size * 1000L / SAMPLE_RATE) + 30L
            Thread.sleep(durationMs)
        } catch (_: Throwable) {
            // Ignore interrupted playback
        } finally {
            try {
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore cleanup errors
            }
        }
    }

    private fun synthesizeBossMusicLoop(): ShortArray {
        // Energetic 16-step arcade boss battle loop (D minor pentatonic)
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
        val totalSamples = samplesPerStep * leadNotes.size
        val out = ShortArray(totalSamples)

        for (step in leadNotes.indices) {
            val leadFreq = leadNotes[step]
            val bassFreq = bassNotes[step]
            val start = step * samplesPerStep
            for (i in 0 until samplesPerStep) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = exp(-t * 5.5)
                val lead = if (leadFreq > 0.0) {
                    val s = sin(2.0 * PI * leadFreq * t)
                    (if (s >= 0.0) 1.0 else -1.0) * 0.10 * env
                } else {
                    0.0
                }
                val bass = sin(2.0 * PI * bassFreq * t) * 0.14 * exp(-t * 3.0)
                val sample = ((lead + bass).coerceIn(-0.9, 0.9) * Short.MAX_VALUE).toInt()
                out[start + i] = sample.toShort()
            }
        }
        return out
    }

    private fun buildSfxBuffers(): Map<GameSoundEvent, ShortArray> {
        return mapOf(
            GameSoundEvent.JUMP to sweepTone(260.0, 540.0, 0.11, 0.35),
            GameSoundEvent.DOUBLE_JUMP to sweepTone(420.0, 820.0, 0.12, 0.35),
            GameSoundEvent.ATTACK to sweepTone(620.0, 220.0, 0.09, 0.30),
            GameSoundEvent.BOSS_HIT to sweepTone(220.0, 95.0, 0.11, 0.42),
            GameSoundEvent.PLAYER_HIT to sweepTone(190.0, 75.0, 0.18, 0.45),
            GameSoundEvent.SHIELD_BLOCK to sweepTone(880.0, 1240.0, 0.10, 0.35),
            GameSoundEvent.CAMERA_SHUTTER to cameraShutterTone(),
            GameSoundEvent.VICTORY to arpeggioTone(doubleArrayOf(523.25, 659.25, 783.99, 1046.50), 0.11),
            GameSoundEvent.DEFEAT to arpeggioTone(doubleArrayOf(392.00, 349.23, 311.13, 261.63), 0.14)
        )
    }

    private fun sweepTone(
        startFreq: Double,
        endFreq: Double,
        durationSec: Double,
        volume: Double
    ): ShortArray {
        val count = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val env = (1.0 - progress) * volume
            out[i] = (sin(phase) * env * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun cameraShutterTone(): ShortArray {
        val count = (SAMPLE_RATE * 0.12).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val click1 = exp(-t * 90.0) * sin(2.0 * PI * 1400.0 * t)
            val click2 = if (t > 0.045) exp(-(t - 0.045) * 90.0) * sin(2.0 * PI * 1900.0 * t) else 0.0
            val v = ((click1 + click2) * 0.42).coerceIn(-0.95, 0.95)
            out[i] = (v * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun arpeggioTone(freqs: DoubleArray, noteDurationSec: Double): ShortArray {
        val perNote = (SAMPLE_RATE * noteDurationSec).toInt()
        val out = ShortArray(perNote * freqs.size)
        for (n in freqs.indices) {
            val freq = freqs[n]
            val offset = n * perNote
            for (i in 0 until perNote) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = (1.0 - i.toDouble() / perNote) * 0.35
                out[offset + i] = (sin(2.0 * PI * freq * t) * env * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    companion object {
        private const val SAMPLE_RATE = 22050
    }
}
