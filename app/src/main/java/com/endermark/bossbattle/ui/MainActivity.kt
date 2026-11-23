package com.endermark.bossbattle.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.endermark.bossbattle.R
import com.endermark.bossbattle.audio.AudioSettings
import com.endermark.bossbattle.model.LevelCatalog
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var gameSurfaceView: GameSurfaceView
    private lateinit var audioSettings: AudioSettings
    private lateinit var audioPanel: LinearLayout
    private lateinit var volumeLabel: TextView
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var musicSwitch: SwitchCompat
    private lateinit var sfxSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val levelId = intent.getIntExtra(EXTRA_LEVEL_ID, LevelCatalog.LEVEL_1.id)
        val selectedLevel = LevelCatalog.findById(levelId)
        audioSettings = AudioSettings(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_battle)

        gameSurfaceView = GameSurfaceView(
            context = this,
            level = selectedLevel,
            audioSettings = audioSettings,
            onExitToMenu = { finish() }
        )
        findViewById<FrameLayout>(R.id.gameContainer).addView(gameSurfaceView)

        setupInBattleAudioControls()
        hideSystemBars()
    }

    private fun setupInBattleAudioControls() {
        audioPanel = findViewById(R.id.audioOverlayPanel)
        volumeLabel = findViewById(R.id.battleVolumeLabel)
        volumeSeekBar = findViewById(R.id.battleVolumeSeekBar)
        musicSwitch = findViewById(R.id.battleMusicSwitch)
        sfxSwitch = findViewById(R.id.battleSfxSwitch)

        val audioButton = findViewById<Button>(R.id.audioOverlayButton)
        val closeButton = findViewById<Button>(R.id.audioCloseButton)

        syncAudioUi()

        audioButton.setOnClickListener {
            audioPanel.visibility = if (audioPanel.visibility == View.VISIBLE) {
                View.GONE
            } else {
                syncAudioUi()
                View.VISIBLE
            }
        }
        closeButton.setOnClickListener {
            audioPanel.visibility = View.GONE
        }

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volumeLabel.text = getString(R.string.audio_volume_format, progress)
                if (fromUser) {
                    audioSettings.masterVolume = progress / 100f
                    gameSurfaceView.refreshAudioSettings()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                gameSurfaceView.playTestSfx()
            }
        })

        musicSwitch.setOnCheckedChangeListener { _, isChecked ->
            audioSettings.musicEnabled = isChecked
            gameSurfaceView.refreshAudioSettings()
        }

        sfxSwitch.setOnCheckedChangeListener { _, isChecked ->
            audioSettings.sfxEnabled = isChecked
            gameSurfaceView.refreshAudioSettings()
            if (isChecked) {
                gameSurfaceView.playTestSfx()
            }
        }
    }

    private fun syncAudioUi() {
        val percent = (audioSettings.masterVolume * 100f).roundToInt().coerceIn(0, 100)
        volumeLabel.text = getString(R.string.audio_volume_format, percent)
        if (volumeSeekBar.progress != percent) {
            volumeSeekBar.progress = percent
        }
        if (musicSwitch.isChecked != audioSettings.musicEnabled) {
            musicSwitch.isChecked = audioSettings.musicEnabled
        }
        if (sfxSwitch.isChecked != audioSettings.sfxEnabled) {
            sfxSwitch.isChecked = audioSettings.sfxEnabled
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        syncAudioUi()
        gameSurfaceView.resumeGame()
    }

    override fun onPause() {
        gameSurfaceView.pauseGame()
        super.onPause()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    companion object {
        const val EXTRA_LEVEL_ID = "extra_level_id"
    }
}
