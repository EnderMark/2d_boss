package com.endermark.bossbattle.audio

import android.content.Context

class AudioSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var masterVolume: Float
        get() = prefs.getFloat(KEY_MASTER_VOLUME, DEFAULT_VOLUME).coerceIn(0f, 1f)
        set(value) {
            prefs.edit().putFloat(KEY_MASTER_VOLUME, value.coerceIn(0f, 1f)).apply()
        }

    var musicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MUSIC_ENABLED, value).apply()
        }

    var sfxEnabled: Boolean
        get() = prefs.getBoolean(KEY_SFX_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SFX_ENABLED, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "boss_battle_audio_prefs"
        private const val KEY_MASTER_VOLUME = "master_volume"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_SFX_ENABLED = "sfx_enabled"
        const val DEFAULT_VOLUME = 0.85f
    }
}
