package com.endermark.bossbattle.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.endermark.bossbattle.R
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.LevelCatalog

class LevelSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

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

    private fun launchLevel(level: BossLevel) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_LEVEL_ID, level.id)
        }
        startActivity(intent)
    }
}
