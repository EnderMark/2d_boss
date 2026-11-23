package com.endermark.bossbattle.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.endermark.bossbattle.model.Boss
import com.endermark.bossbattle.model.GameState
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.Player
import kotlin.math.min
import kotlin.math.sin

class GameRenderer {

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#0D0F1A")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#181D33")
        strokeWidth = 2f
    }

    private val bossArmorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bossOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF8A80")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val bossCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#29B6F6")
        style = Paint.Style.FILL
    }

    private val playerAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E1F5FE")
        style = Paint.Style.FILL
    }

    private val playerShotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7")
        style = Paint.Style.FILL
    }

    private val bossShotOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252")
        style = Paint.Style.FILL
    }

    private val bossShotInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFEB3B")
        style = Paint.Style.FILL
    }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E2235")
        style = Paint.Style.FILL
    }

    private val barTrailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB74D")
        style = Paint.Style.FILL
    }

    private val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val barBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A5378")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val bossNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        isFakeBoldText = true
    }

    private val hpTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CFD8DC")
        textSize = 30f
        textAlign = Paint.Align.RIGHT
        isFakeBoldText = true
    }

    private val phaseBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFAB40")
        textSize = 26f
        isFakeBoldText = true
    }

    private val playerHpLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90A4AE")
        textSize = 28f
        isFakeBoldText = true
    }

    private val heartActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#29B6F6")
        style = Paint.Style.FILL
    }

    private val heartEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#263238")
        style = Paint.Style.FILL
    }

    private val overlayScrimPaint = Paint().apply {
        color = Color.argb(170, 10, 12, 22)
        style = Paint.Style.FILL
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 74f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0BEC5")
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private val promptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFCA28")
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val tempRect = RectF()
    private val tempPath = Path()

    fun render(canvas: Canvas, state: GameState) {
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), backgroundPaint)

        val scale = min(
            canvas.width.toFloat() / state.worldWidth,
            canvas.height.toFloat() / state.worldHeight
        )
        val offsetX = (canvas.width - state.worldWidth * scale) / 2f
        val offsetY = (canvas.height - state.worldHeight * scale) / 2f

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        drawArenaGrid(canvas, state.worldWidth, state.worldHeight)
        drawBossHealthBar(canvas, state.boss, state.worldWidth)
        drawProjectiles(canvas, state)
        drawParticles(canvas, state)
        if (state.boss.hp > 0) {
            drawBoss(canvas, state.boss, state.elapsedTime)
        }
        drawPlayer(canvas, state.player, state.elapsedTime)
        drawPlayerHud(canvas, state.player, state.worldHeight)

        if (state.status != GameStatus.PLAYING) {
            drawOverlay(canvas, state)
        }

        canvas.restore()
    }

    private fun drawArenaGrid(canvas: Canvas, width: Float, height: Float) {
        val step = 100f
        var x = step
        while (x < width) {
            canvas.drawLine(x, 0f, x, height, gridPaint)
            x += step
        }
        var y = step
        while (y < height) {
            canvas.drawLine(0f, y, width, y, gridPaint)
            y += step
        }
    }

    private fun drawBossHealthBar(canvas: Canvas, boss: Boss, worldWidth: Float) {
        val marginX = 52f
        val topY = 92f
        val barHeight = 36f
        val barWidth = worldWidth - marginX * 2f
        val cornerRadius = 14f

        canvas.drawText(boss.name, marginX, topY - 18f, bossNamePaint)
        if (boss.isEnraged && boss.hp > 0) {
            val nameWidth = bossNamePaint.measureText(boss.name)
            canvas.drawText("FASE 2", marginX + nameWidth + 20f, topY - 18f, phaseBadgePaint)
        }
        canvas.drawText(
            "${boss.hp} / ${boss.maxHp} HP",
            worldWidth - marginX,
            topY - 18f,
            hpTextPaint
        )

        // Bar background
        tempRect.set(marginX, topY, marginX + barWidth, topY + barHeight)
        canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, barBgPaint)

        // Trailing damage bar
        val trailWidth = barWidth * boss.displayedHpRatio
        if (trailWidth > 0f) {
            tempRect.set(marginX, topY, marginX + trailWidth, topY + barHeight)
            canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, barTrailPaint)
        }

        // Actual HP fill
        val fillWidth = barWidth * boss.hpRatio
        if (fillWidth > 0f) {
            val startColor = if (boss.isEnraged) Color.parseColor("#FF5722") else Color.parseColor("#E53935")
            val endColor = if (boss.isEnraged) Color.parseColor("#FFAB00") else Color.parseColor("#FF5252")
            barFillPaint.shader = LinearGradient(
                marginX,
                topY,
                marginX + barWidth,
                topY,
                startColor,
                endColor,
                Shader.TileMode.CLAMP
            )
            tempRect.set(marginX, topY, marginX + fillWidth, topY + barHeight)
            canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, barFillPaint)
            barFillPaint.shader = null
        }

        // Frame border
        tempRect.set(marginX, topY, marginX + barWidth, topY + barHeight)
        canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, barBorderPaint)
    }

    private fun drawBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val pulse = 1f + 0.04f * sin(elapsedTime * 5f)
        val r = boss.radius * pulse
        val isHit = boss.hitFlashTimer > 0f

        bossArmorPaint.color = when {
            isHit -> Color.WHITE
            boss.isEnraged -> Color.parseColor("#BF360C")
            else -> Color.parseColor("#C62828")
        }

        // Geometric armored boss silhouette
        tempPath.reset()
        tempPath.moveTo(boss.x, boss.y - r * 0.95f)
        tempPath.lineTo(boss.x + r * 0.85f, boss.y - r * 0.35f)
        tempPath.lineTo(boss.x + r * 0.65f, boss.y + r * 0.75f)
        tempPath.lineTo(boss.x, boss.y + r)
        tempPath.lineTo(boss.x - r * 0.65f, boss.y + r * 0.75f)
        tempPath.lineTo(boss.x - r * 0.85f, boss.y - r * 0.35f)
        tempPath.close()

        canvas.drawPath(tempPath, bossArmorPaint)
        canvas.drawPath(tempPath, bossOutlinePaint)

        // Glowing inner core
        bossCorePaint.color = when {
            isHit -> Color.parseColor("#FFF9C4")
            boss.isEnraged -> Color.parseColor("#FFD740")
            else -> Color.parseColor("#FF8A65")
        }
        canvas.drawCircle(boss.x, boss.y + r * 0.05f, r * 0.36f, bossCorePaint)
    }

    private fun drawPlayer(canvas: Canvas, player: Player, elapsedTime: Float) {
        if (player.isInvulnerable && ((elapsedTime * 16f).toInt() % 2 == 0)) {
            return
        }

        val r = player.radius
        tempPath.reset()
        tempPath.moveTo(player.x, player.y - r * 1.15f)
        tempPath.lineTo(player.x + r, player.y + r * 0.9f)
        tempPath.lineTo(player.x, player.y + r * 0.45f)
        tempPath.lineTo(player.x - r, player.y + r * 0.9f)
        tempPath.close()

        canvas.drawPath(tempPath, playerPaint)
        canvas.drawCircle(player.x, player.y + r * 0.05f, r * 0.28f, playerAccentPaint)
    }

    private fun drawProjectiles(canvas: Canvas, state: GameState) {
        for (projectile in state.projectiles) {
            if (projectile.fromPlayer) {
                tempRect.set(
                    projectile.x - projectile.radius * 0.55f,
                    projectile.y - projectile.radius * 1.4f,
                    projectile.x + projectile.radius * 0.55f,
                    projectile.y + projectile.radius * 1.4f
                )
                canvas.drawRoundRect(tempRect, 8f, 8f, playerShotPaint)
            } else {
                canvas.drawCircle(projectile.x, projectile.y, projectile.radius, bossShotOuterPaint)
                canvas.drawCircle(
                    projectile.x,
                    projectile.y,
                    projectile.radius * 0.5f,
                    bossShotInnerPaint
                )
            }
        }
    }

    private fun drawParticles(canvas: Canvas, state: GameState) {
        for (particle in state.particles) {
            val alpha = ((particle.life / particle.maxLife).coerceIn(0f, 1f) * 255).toInt()
            particlePaint.color = particle.color
            particlePaint.alpha = alpha
            canvas.drawCircle(particle.x, particle.y, 7f, particlePaint)
        }
    }

    private fun drawPlayerHud(canvas: Canvas, player: Player, worldHeight: Float) {
        val baseY = worldHeight - 76f
        val startX = 52f
        canvas.drawText("VITA", startX, baseY + 10f, playerHpLabelPaint)

        val pipStartX = startX + 92f
        val pipWidth = 42f
        val pipHeight = 20f
        val pipGap = 14f

        for (i in 0 until player.maxHp) {
            val left = pipStartX + i * (pipWidth + pipGap)
            tempRect.set(left, baseY - pipHeight / 2f, left + pipWidth, baseY + pipHeight / 2f)
            val paint = if (i < player.hp) heartActivePaint else heartEmptyPaint
            canvas.drawRoundRect(tempRect, 6f, 6f, paint)
        }
    }

    private fun drawOverlay(canvas: Canvas, state: GameState) {
        canvas.drawRect(0f, 0f, state.worldWidth, state.worldHeight, overlayScrimPaint)

        val centerX = state.worldWidth / 2f
        val centerY = state.worldHeight / 2f

        val (title, subtitle, prompt) = when (state.status) {
            GameStatus.READY -> Triple(
                "2D BOSS BATTLE",
                "Trascina per schivare i colpi",
                "Tocca per iniziare"
            )
            GameStatus.VICTORY -> Triple(
                "VITTORIA!",
                "Hai sconfitto ${state.boss.name}",
                "Tocca per rigiocare"
            )
            GameStatus.DEFEAT -> Triple(
                "SCONFITTA",
                "Il boss ti ha abbattuto",
                "Tocca per riprovare"
            )
            GameStatus.PLAYING -> return
        }

        canvas.drawText(title, centerX, centerY - 60f, titlePaint)
        canvas.drawText(subtitle, centerX, centerY + 12f, subtitlePaint)
        canvas.drawText(prompt, centerX, centerY + 110f, promptPaint)
    }
}
