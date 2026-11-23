package com.endermark.bossbattle.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.endermark.bossbattle.model.LevelCatalog

class MainActivity : AppCompatActivity() {

    private lateinit var gameSurfaceView: GameSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val levelId = intent.getIntExtra(EXTRA_LEVEL_ID, LevelCatalog.LEVEL_1.id)
        val selectedLevel = LevelCatalog.findById(levelId)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        gameSurfaceView = GameSurfaceView(
            context = this,
            level = selectedLevel,
            onExitToMenu = { finish() }
        )
        setContentView(gameSurfaceView)

        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
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
