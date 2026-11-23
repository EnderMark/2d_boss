package com.endermark.bossbattle.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.endermark.bossbattle.R
import com.endermark.bossbattle.audio.AudioSettings
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.LevelCatalog
import kotlin.math.roundToInt

class LevelSelectActivity : AppCompatActivity() {

    private lateinit var audioSettings: AudioSettings
    private lateinit var volumeLabel: TextView
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var musicSwitch: SwitchCompat
    private lateinit var sfxSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

        audioSettings = AudioSettings(this)
        setupAudioControls()

        val container = findViewById<LinearLayout>(R.id.levelsContainer)
        val inflater = LayoutInflater.from(this)

        for (level in LevelCatalog.levels) {
            val card = inflater.inflate(R.layout.item_level_card, container, false)
            card.findViewById<TextView>(R.id.levelNumberText).text = level.title
            card.findViewById<TextView>(R.id.levelHpText).text =
                getString(R.string.boss_hp_format, level.bossMaxHp)
            card.findViewById<TextView>(R.id.bossNameText).text = level.bossName
            card.findViewById<TextView>(R.id.bossSubtitleText).text = level.subtitle

            val playButton = card.findViewById<Button>(R.id.playLevelButton)
            playButton.isEnabled = level.isUnlocked
            playButton.setOnClickListener {
                launchLevel(level)
            }
            card.setOnClickListener {
                if (level.isUnlocked) {
                    launchLevel(level)
                }
            }

            container.addView(card)
        }
    }

    override fun onResume() {
        super.onResume()
        syncAudioUiFromSettings()
    }

    private fun setupAudioControls() {
        volumeLabel = findViewById(R.id.volumeLabel)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)
        musicSwitch = findViewById(R.id.musicSwitch)
        sfxSwitch = findViewById(R.id.sfxSwitch)

        syncAudioUiFromSettings()

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                volumeLabel.text = getString(R.string.audio_volume_format, progress)
                if (fromUser) {
                    audioSettings.masterVolume = progress / 100f
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        musicSwitch.setOnCheckedChangeListener { _, isChecked ->
            audioSettings.musicEnabled = isChecked
        }

        sfxSwitch.setOnCheckedChangeListener { _, isChecked ->
            audioSettings.sfxEnabled = isChecked
        }
    }

    private fun syncAudioUiFromSettings() {
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

    private fun launchLevel(level: BossLevel) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_LEVEL_ID, level.id)
        }
        startActivity(intent)
    }
}
