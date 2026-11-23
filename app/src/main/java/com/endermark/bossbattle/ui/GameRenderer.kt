package com.endermark.bossbattle.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.endermark.bossbattle.model.ArenaTheme
import com.endermark.bossbattle.model.BattleMode
import com.endermark.bossbattle.model.Boss
import com.endermark.bossbattle.model.BossAction
import com.endermark.bossbattle.model.GameState
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.PickupKind
import com.endermark.bossbattle.model.Player
import com.endermark.bossbattle.model.ProjectileKind
import com.endermark.bossbattle.model.WeaponType
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

class GameRenderer {

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#0D0F1A")
        style = Paint.Style.FILL
    }

    private val skyGradientPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val hanoiSilhouettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D1328")
        style = Paint.Style.FILL
    }

    private val hanoiLakePaint = Paint().apply {
        color = Color.parseColor("#241933")
        style = Paint.Style.FILL
    }

    private val hanoiLanternCordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6D4C41")
        strokeWidth = 3f
    }

    private val hanoiLanternRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E53935")
        style = Paint.Style.FILL
    }

    private val hanoiLanternGoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        style = Paint.Style.FILL
    }

    private val zurichMountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C2C42")
        style = Paint.Style.FILL
    }

    private val zurichSnowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CFD8DC")
        style = Paint.Style.FILL
    }

    private val zurichCityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#121D2E")
        style = Paint.Style.FILL
    }

    private val swissCrossWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val studioBuildingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B1726")
        style = Paint.Style.FILL
    }

    private val studioBeamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(32, 255, 224, 130)
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#181D33")
        strokeWidth = 2f
    }

    private val groundFillPaint = Paint().apply {
        color = Color.parseColor("#15192B")
        style = Paint.Style.FILL
    }

    private val hanoiGroundFillPaint = Paint().apply {
        color = Color.parseColor("#2B2130")
        style = Paint.Style.FILL
    }

    private val zurichGroundFillPaint = Paint().apply {
        color = Color.parseColor("#1A2433")
        style = Paint.Style.FILL
    }

    private val studioGroundFillPaint = Paint().apply {
        color = Color.parseColor("#26202C")
        style = Paint.Style.FILL
    }

    private val groundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A5378")
        strokeWidth = 6f
    }

    private val hanoiGroundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D4A373")
        strokeWidth = 6f
    }

    private val zurichGroundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90CAF9")
        strokeWidth = 6f
    }

    private val studioGroundLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFAB91")
        strokeWidth = 6f
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

    private val bossTelegraphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 171, 64)
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    private val chanHairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141118")
        style = Paint.Style.FILL
    }

    private val skinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5CBA7")
        style = Paint.Style.FILL
    }

    private val baldShinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val darkDetailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E1E24")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B71C1C")
        style = Paint.Style.FILL
    }

    private val chanHatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6C27A")
        style = Paint.Style.FILL
    }

    private val chanHatOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A67C52")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val chanGoldTrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val speechBubbleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val speechBubbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E53935")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val speechBubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A22")
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val droneBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#263238")
        style = Paint.Style.FILL
    }

    private val droneRotorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80DEEA")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val droneLedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF1744")
        style = Paint.Style.FILL
    }

    private val cameraFlashOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 255, 245, 157)
        style = Paint.Style.FILL
    }

    private val cameraFlashInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#29B6F6")
        style = Paint.Style.FILL
    }

    private val playerLimbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#81D4FA")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val playerAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E1F5FE")
        style = Paint.Style.FILL
    }

    private val swordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ECEFF1")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val shieldFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        style = Paint.Style.FILL
    }

    private val shieldBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val shieldAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(145, 255, 224, 130)
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    private val slashArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80DEEA")
        style = Paint.Style.STROKE
        strokeWidth = 16f
        strokeCap = Paint.Cap.ROUND
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

    private val shockwavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7043")
        style = Paint.Style.FILL
    }

    private val shockwaveCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF176")
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

    private val controlBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E2540")
        style = Paint.Style.FILL
    }

    private val controlAccentBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A2038")
        style = Paint.Style.FILL
    }

    private val controlShieldBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B2D48")
        style = Paint.Style.FILL
    }

    private val controlBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5C6BC0")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val controlAttackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7043")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val controlShieldBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val controlLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
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
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val promptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFCA28")
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val menuButtonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#232946")
        style = Paint.Style.FILL
    }

    private val menuButtonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5C6BC0")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val menuButtonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val tempRect = RectF()
    private val tempPath = Path()

    fun leftButtonRect(hasShield: Boolean): RectF =
        if (hasShield) RectF(32f, 1365f, 178f, 1520f) else RectF(48f, 1360f, 218f, 1520f)

    fun rightButtonRect(hasShield: Boolean): RectF =
        if (hasShield) RectF(196f, 1365f, 342f, 1520f) else RectF(244f, 1360f, 414f, 1520f)

    fun shieldButtonRect(): RectF = RectF(460f, 1365f, 616f, 1520f)

    fun jumpButtonRect(hasShield: Boolean): RectF =
        if (hasShield) RectF(634f, 1365f, 790f, 1520f) else RectF(576f, 1360f, 756f, 1520f)

    fun attackButtonRect(hasShield: Boolean): RectF =
        if (hasShield) RectF(808f, 1365f, 968f, 1520f) else RectF(782f, 1360f, 952f, 1520f)

    fun padCenterX(): Float = 200f
    fun padCenterY(): Float = 1405f
    fun padRadius(): Float = 115f
    fun fireButtonRect(): RectF = RectF(730f, 1320f, 940f, 1495f)

    fun isMenuButtonHit(worldX: Float, worldY: Float, state: GameState): Boolean {
        if (state.status == GameStatus.PLAYING) return false
        val rect = menuButtonRect(state.worldWidth, state.worldHeight)
        return rect.contains(worldX, worldY)
    }

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

        val isMelee = state.level.mode == BattleMode.MELEE_PLATFORMER
        val isFreeFlight = state.level.mode == BattleMode.FREE_FLIGHT_SHOOTER
        when (state.level.arenaTheme) {
            ArenaTheme.HANOI -> drawHanoiBackground(canvas, state)
            ArenaTheme.HANOI_SPA -> drawHanoiSpaBackground(canvas, state)
            ArenaTheme.OMEGA_FINALE -> drawOmegaFinaleBackground(canvas, state)
            ArenaTheme.DEEP_SPACE -> drawDeepSpaceBackground(canvas, state)
            ArenaTheme.ZURICH -> drawZurichBackground(canvas, state)
            ArenaTheme.ZURICH_TAX -> drawZurichTaxBackground(canvas, state)
            ArenaTheme.STUDIO -> drawStudioBackground(canvas, state)
            ArenaTheme.PARIS -> drawParisBackground(canvas, state)
            ArenaTheme.UBS_ALPS -> drawUbsAlpsBackground(canvas, state)
            ArenaTheme.GYM_DEV -> drawGymDevBackground(canvas, state)
            ArenaTheme.GOOGLE_ZURICH -> drawGoogleCampusBackground(canvas, state)
            else -> drawArenaGrid(canvas, state.worldWidth, if (isMelee) state.groundY else state.worldHeight)
        }
        if (isMelee || (state.level.arenaTheme != ArenaTheme.SPACE && state.level.arenaTheme != ArenaTheme.DEEP_SPACE)) {
            drawGroundArena(canvas, state)
        }

        drawMovingPlatforms(canvas, state)
        drawBossHealthBar(canvas, state.boss, state.worldWidth, state.level.multiPhase)
        drawPickups(canvas, state)
        drawLasers(canvas, state)
        drawProjectiles(canvas, state)
        drawMinions(canvas, state)
        drawParticles(canvas, state)

        if (state.boss.hp > 0 && !state.boss.isVanished) {
            when (state.level.arenaTheme) {
                ArenaTheme.HANOI -> drawChanBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.HANOI_SPA -> drawCaramelBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.OMEGA_FINALE -> drawOmegaBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.DEEP_SPACE -> drawDreadnoughtBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.ZURICH -> drawLolloZurichBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.STUDIO -> drawIvanoneBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.ZURICH_TAX -> drawAlessioTaxBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.PARIS -> drawLouiseBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.UBS_ALPS -> drawDaisyBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.GYM_DEV -> drawAndreaDatteroBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.GOOGLE_ZURICH -> drawMarcoGoogleBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.IRON_ARENA -> drawMeleeBoss(canvas, state.boss, state.elapsedTime)
                ArenaTheme.SPACE -> drawShooterBoss(canvas, state.boss, state.elapsedTime)
            }
        }

        when {
            isMelee -> {
                drawMeleeCharacter(canvas, state.player, state.elapsedTime, state.level.hasShield)
                val hudY = if (state.boss.maxArmor > 0) 204f else 178f
                drawPlayerHud(canvas, state.player, baseY = hudY, showWeaponStatus = state.level.enablePickups)
                drawMeleeControls(canvas, state.level.hasShield)
            }
            isFreeFlight -> {
                drawRotatedPlayerShip(canvas, state.player, state.elapsedTime)
                drawPlayerHud(canvas, state.player, baseY = 178f, showWeaponStatus = false)
                drawFreeFlightControls(canvas, state)
            }
            else -> {
                drawPlayerShip(canvas, state.player, state.elapsedTime)
                drawPlayerHud(canvas, state.player, baseY = state.worldHeight - 76f, showWeaponStatus = false)
            }
        }

        if (state.phaseFlashTimer > 0f) {
            val flashAlpha = ((state.phaseFlashTimer / 0.6f).coerceIn(0f, 1f) * 190).toInt()
            val flashPaint = Paint().apply {
                color = Color.argb(flashAlpha, 255, 245, 200)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, state.worldWidth, state.worldHeight, flashPaint)
        }

        if (state.status != GameStatus.PLAYING) {
            drawOverlay(canvas, state)
        }

        canvas.restore()
    }

    private fun drawHanoiBackground(canvas: Canvas, state: GameState) {
        skyGradientPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            state.groundY,
            intArrayOf(
                Color.parseColor("#161029"),
                Color.parseColor("#351C3A"),
                Color.parseColor("#6B2D3E")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, state.worldWidth, state.groundY, skyGradientPaint)
        skyGradientPaint.shader = null

        val lakeTop = state.groundY - 170f
        canvas.drawRect(0f, lakeTop, state.worldWidth, state.groundY, hanoiLakePaint)

        val cx = state.worldWidth * 0.5f
        val baseY = lakeTop + 28f
        canvas.drawRect(cx - 125f, baseY - 20f, cx + 125f, baseY + 18f, hanoiSilhouettePaint)
        canvas.drawRect(cx - 76f, baseY - 110f, cx + 76f, baseY - 20f, hanoiSilhouettePaint)
        canvas.drawRect(cx - 56f, baseY - 178f, cx + 56f, baseY - 110f, hanoiSilhouettePaint)
        tempPath.reset()
        tempPath.moveTo(cx - 88f, baseY - 178f)
        tempPath.lineTo(cx, baseY - 235f)
        tempPath.lineTo(cx + 88f, baseY - 178f)
        tempPath.close()
        canvas.drawPath(tempPath, hanoiSilhouettePaint)

        val cordY = 235f
        canvas.drawLine(0f, cordY, state.worldWidth, cordY, hanoiLanternCordPaint)
        val lanternXs = listOf(120f, 290f, 500f, 710f, 880f)
        for ((index, lx) in lanternXs.withIndex()) {
            val sway = sin(state.elapsedTime * 2.2f + index) * 8f
            val ly = cordY + 46f
            canvas.drawLine(lx, cordY, lx + sway, ly - 24f, hanoiLanternCordPaint)
            tempRect.set(lx + sway - 20f, ly - 24f, lx + sway + 20f, ly + 26f)
            val lanternPaint = if (index % 2 == 0) hanoiLanternRedPaint else hanoiLanternGoldPaint
            canvas.drawOval(tempRect, lanternPaint)
            canvas.drawCircle(lx + sway, ly, 8f, hanoiLanternGoldPaint)
        }
    }

    private fun drawZurichBackground(canvas: Canvas, state: GameState) {
        skyGradientPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            state.groundY,
            intArrayOf(
                Color.parseColor("#0E1726"),
                Color.parseColor("#1B2D45"),
                Color.parseColor("#2A4463")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, state.worldWidth, state.groundY, skyGradientPaint)
        skyGradientPaint.shader = null

        // Swiss Alps silhouette
        tempPath.reset()
        tempPath.moveTo(0f, state.groundY)
        tempPath.lineTo(0f, state.groundY - 260f)
        tempPath.lineTo(220f, state.groundY - 480f)
        tempPath.lineTo(440f, state.groundY - 300f)
        tempPath.lineTo(680f, state.groundY - 520f)
        tempPath.lineTo(890f, state.groundY - 340f)
        tempPath.lineTo(state.worldWidth, state.groundY - 410f)
        tempPath.lineTo(state.worldWidth, state.groundY)
        tempPath.close()
        canvas.drawPath(tempPath, zurichMountainPaint)

        // Snow caps
        tempPath.reset()
        tempPath.moveTo(220f, state.groundY - 480f)
        tempPath.lineTo(175f, state.groundY - 425f)
        tempPath.lineTo(265f, state.groundY - 425f)
        tempPath.close()
        canvas.drawPath(tempPath, zurichSnowPaint)

        tempPath.reset()
        tempPath.moveTo(680f, state.groundY - 520f)
        tempPath.lineTo(632f, state.groundY - 462f)
        tempPath.lineTo(728f, state.groundY - 462f)
        tempPath.close()
        canvas.drawPath(tempPath, zurichSnowPaint)

        // Grossmunster twin towers silhouette
        canvas.drawRect(380f, state.groundY - 280f, 450f, state.groundY, zurichCityPaint)
        canvas.drawRect(500f, state.groundY - 280f, 570f, state.groundY, zurichCityPaint)
        canvas.drawRect(450f, state.groundY - 190f, 500f, state.groundY, zurichCityPaint)
        canvas.drawCircle(415f, state.groundY - 280f, 35f, zurichCityPaint)
        canvas.drawCircle(535f, state.groundY - 280f, 35f, zurichCityPaint)

        // Swiss flag badge in upper right
        val flagX = 875f
        val flagY = 230f
        tempRect.set(flagX - 36f, flagY - 36f, flagX + 36f, flagY + 36f)
        canvas.drawRoundRect(tempRect, 10f, 10f, hanoiLanternRedPaint)
        canvas.drawRect(flagX - 20f, flagY - 7f, flagX + 20f, flagY + 7f, swissCrossWhitePaint)
        canvas.drawRect(flagX - 7f, flagY - 20f, flagX + 7f, flagY + 20f, swissCrossWhitePaint)
    }

    private fun drawStudioBackground(canvas: Canvas, state: GameState) {
        skyGradientPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            state.groundY,
            intArrayOf(
                Color.parseColor("#181326"),
                Color.parseColor("#3B234A"),
                Color.parseColor("#8A463B")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, state.worldWidth, state.groundY, skyGradientPaint)
        skyGradientPaint.shader = null

        // Subtle photography spotlight beams
        tempPath.reset()
        tempPath.moveTo(150f, 0f)
        tempPath.lineTo(420f, state.groundY)
        tempPath.lineTo(240f, state.groundY)
        tempPath.close()
        canvas.drawPath(tempPath, studioBeamPaint)

        tempPath.reset()
        tempPath.moveTo(850f, 0f)
        tempPath.lineTo(760f, state.groundY)
        tempPath.lineTo(580f, state.groundY)
        tempPath.close()
        canvas.drawPath(tempPath, studioBeamPaint)

        // Rooftop skyline
        canvas.drawRect(40f, state.groundY - 210f, 210f, state.groundY, studioBuildingPaint)
        canvas.drawRect(250f, state.groundY - 310f, 430f, state.groundY, studioBuildingPaint)
        canvas.drawRect(560f, state.groundY - 260f, 750f, state.groundY, studioBuildingPaint)
        canvas.drawRect(790f, state.groundY - 340f, 960f, state.groundY, studioBuildingPaint)
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

    private fun drawGroundArena(canvas: Canvas, state: GameState) {
        val fill = when (state.level.arenaTheme) {
            ArenaTheme.HANOI -> hanoiGroundFillPaint
            ArenaTheme.ZURICH -> zurichGroundFillPaint
            ArenaTheme.STUDIO -> studioGroundFillPaint
            else -> groundFillPaint
        }
        val line = when (state.level.arenaTheme) {
            ArenaTheme.HANOI -> hanoiGroundLinePaint
            ArenaTheme.ZURICH -> zurichGroundLinePaint
            ArenaTheme.STUDIO -> studioGroundLinePaint
            else -> groundLinePaint
        }
        canvas.drawRect(0f, state.groundY, state.worldWidth, state.worldHeight, fill)
        canvas.drawLine(0f, state.groundY, state.worldWidth, state.groundY, line)
    }

    private fun drawBossHealthBar(
        canvas: Canvas,
        boss: Boss,
        worldWidth: Float,
        multiPhase: Boolean
    ) {
        val marginX = 52f
        val topY = 92f
        val barHeight = 36f
        val barWidth = worldWidth - marginX * 2f
        val cornerRadius = 14f

        canvas.drawText(boss.name, marginX, topY - 18f, bossNamePaint)
        if (boss.hp > 0) {
            val phaseLabel = when {
                multiPhase -> "FASE ${boss.phase}"
                boss.isEnraged -> "FASE 2"
                else -> null
            }
            if (phaseLabel != null) {
                val nameWidth = bossNamePaint.measureText(boss.name)
                canvas.drawText(phaseLabel, marginX + nameWidth + 20f, topY - 18f, phaseBadgePaint)
            }
        }
        canvas.drawText(
            "${boss.hp} / ${boss.maxHp} HP",
            worldWidth - marginX,
            topY - 18f,
            hpTextPaint
        )

        tempRect.set(marginX, topY, marginX + barWidth, topY + barHeight)
        canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, barBgPaint)

        val trailWidth = barWidth * boss.displayedHpRatio
        if (trailWidth > 0f) {
            tempRect.set(marginX, topY, marginX + trailWidth, topY + barHeight)
            canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, barTrailPaint)
        }

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

        tempRect.set(marginX, topY, marginX + barWidth, topY + barHeight)
        canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, barBorderPaint)

        // Armor bar directly below HP bar (Level 11 Caramel)
        if (boss.maxArmor > 0) {
            val armorTopY = topY + barHeight + 10f
            val armorHeight = 18f
            tempRect.set(marginX, armorTopY, marginX + barWidth, armorTopY + armorHeight)
            canvas.drawRoundRect(tempRect, 9f, 9f, barBgPaint)

            val armorWidth = barWidth * boss.armorRatio
            if (armorWidth > 0f) {
                barFillPaint.shader = LinearGradient(
                    marginX,
                    armorTopY,
                    marginX + barWidth,
                    armorTopY,
                    Color.parseColor("#26C6DA"),
                    Color.parseColor("#FFD54F"),
                    Shader.TileMode.CLAMP
                )
                tempRect.set(marginX, armorTopY, marginX + armorWidth, armorTopY + armorHeight)
                canvas.drawRoundRect(tempRect, 9f, 9f, barFillPaint)
                barFillPaint.shader = null
            }

            tempRect.set(marginX, armorTopY, marginX + barWidth, armorTopY + armorHeight)
            canvas.drawRoundRect(tempRect, 9f, 9f, barBorderPaint)

            val armorStatus = if (boss.isStunned) {
                "ARMATURA ROTTA! ATTACCA!"
            } else {
                "ARMATURA ${boss.armor}/${boss.maxArmor}"
            }
            canvas.drawText(armorStatus, worldWidth - marginX, armorTopY + 42f, phaseBadgePaint.apply {
                textAlign = Paint.Align.RIGHT
            })
            phaseBadgePaint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawShooterBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val pulse = 1f + 0.04f * sin(elapsedTime * 5f)
        val r = boss.radius * pulse
        val isHit = boss.hitFlashTimer > 0f

        bossArmorPaint.color = when {
            isHit -> Color.WHITE
            boss.isEnraged -> Color.parseColor("#BF360C")
            else -> Color.parseColor("#C62828")
        }

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

        bossCorePaint.color = when {
            isHit -> Color.parseColor("#FFF9C4")
            boss.isEnraged -> Color.parseColor("#FFD740")
            else -> Color.parseColor("#FF8A65")
        }
        canvas.drawCircle(boss.x, boss.y + r * 0.05f, r * 0.36f, bossCorePaint)
    }

    private fun drawMeleeBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            val auraRadius = r * (1.15f + 0.12f * sin(elapsedTime * 20f))
            canvas.drawCircle(boss.x, boss.y, auraRadius, bossTelegraphPaint)
        }

        bossArmorPaint.color = when {
            isHit -> Color.WHITE
            boss.action == BossAction.CHARGING -> Color.parseColor("#FF5722")
            boss.isEnraged -> Color.parseColor("#8D2828")
            else -> Color.parseColor("#546E7A")
        }

        tempRect.set(boss.x - r * 0.78f, boss.y - r * 0.72f, boss.x + r * 0.78f, boss.y + r * 0.96f)
        canvas.drawRoundRect(tempRect, 24f, 24f, bossArmorPaint)
        canvas.drawRoundRect(tempRect, 24f, 24f, bossOutlinePaint)

        tempRect.set(boss.x - r * 0.45f, boss.y - r * 1.05f, boss.x + r * 0.45f, boss.y - r * 0.58f)
        canvas.drawRoundRect(tempRect, 14f, 14f, bossArmorPaint)
        canvas.drawRoundRect(tempRect, 14f, 14f, bossOutlinePaint)

        bossCorePaint.color = when {
            isHit -> Color.parseColor("#FFF9C4")
            isTelegraphing || boss.isEnraged -> Color.parseColor("#FFD740")
            else -> Color.parseColor("#FF5252")
        }
        val visorOffsetX = boss.facingDir * r * 0.16f
        tempRect.set(
            boss.x + visorOffsetX - r * 0.24f,
            boss.y - r * 0.88f,
            boss.x + visorOffsetX + r * 0.24f,
            boss.y - r * 0.72f
        )
        canvas.drawRoundRect(tempRect, 8f, 8f, bossCorePaint)

        canvas.drawCircle(boss.x, boss.y + r * 0.12f, r * 0.24f, bossCorePaint)
    }

    private fun drawChanBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            val auraRadius = r * (1.2f + 0.12f * sin(elapsedTime * 20f))
            canvas.drawCircle(x, y, auraRadius, bossTelegraphPaint)
        }

        tempRect.set(x - r * 0.44f, y - r * 0.82f, x + r * 0.44f, y + r * 0.12f)
        canvas.drawRoundRect(tempRect, 20f, 20f, chanHairPaint)

        bossArmorPaint.color = when {
            isHit -> Color.WHITE
            boss.action == BossAction.CHARGING -> Color.parseColor("#FF7043")
            boss.isEnraged -> Color.parseColor("#AD1457")
            else -> Color.parseColor("#C62828")
        }
        tempPath.reset()
        tempPath.moveTo(x - r * 0.34f, y - r * 0.32f)
        tempPath.lineTo(x + r * 0.34f, y - r * 0.32f)
        tempPath.lineTo(x + r * 0.56f, y + r * 0.98f)
        tempPath.lineTo(x - r * 0.56f, y + r * 0.98f)
        tempPath.close()
        canvas.drawPath(tempPath, bossArmorPaint)
        canvas.drawLine(x, y - r * 0.30f, x, y + r * 0.94f, chanGoldTrimPaint)

        canvas.drawCircle(x, y - r * 0.58f, r * 0.30f, if (isHit) bossArmorPaint else skinPaint)

        tempPath.reset()
        tempPath.moveTo(x - r * 0.86f, y - r * 0.72f)
        tempPath.lineTo(x, y - r * 1.32f)
        tempPath.lineTo(x + r * 0.86f, y - r * 0.72f)
        tempPath.close()
        canvas.drawPath(tempPath, if (isHit) bossArmorPaint else chanHatPaint)
        canvas.drawPath(tempPath, chanHatOutlinePaint)
    }

    private fun drawLolloZurichBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            val auraRadius = r * (1.18f + 0.12f * sin(elapsedTime * 20f))
            canvas.drawCircle(x, y, auraRadius, bossTelegraphPaint)
        }

        // Smart Zurich blazer / jacket
        bossArmorPaint.color = when {
            isHit -> Color.WHITE
            boss.isEnraged -> Color.parseColor("#37474F")
            else -> Color.parseColor("#263238")
        }
        tempRect.set(x - r * 0.55f, y - r * 0.28f, x + r * 0.55f, y + r * 0.96f)
        canvas.drawRoundRect(tempRect, 20f, 20f, bossArmorPaint)

        // Red Swiss tie
        tempPath.reset()
        tempPath.moveTo(x - 10f, y - r * 0.25f)
        tempPath.lineTo(x + 10f, y - r * 0.25f)
        tempPath.lineTo(x, y + r * 0.45f)
        tempPath.close()
        canvas.drawPath(tempPath, hanoiLanternRedPaint)

        // Bald round head with shiny specular reflection!
        val headY = y - r * 0.68f
        val headR = r * 0.44f
        canvas.drawCircle(x, headY, headR, if (isHit) bossArmorPaint else skinPaint)
        if (!isHit) {
            canvas.drawCircle(x - headR * 0.28f, headY - headR * 0.35f, headR * 0.22f, baldShinePaint)
            // Angry eyebrows
            val dir = boss.facingDir.toFloat()
            canvas.drawLine(x - 16f, headY - 10f, x - 2f, headY - 2f, darkDetailPaint)
            canvas.drawLine(x + 16f, headY - 10f, x + 2f, headY - 2f, darkDetailPaint)
            // Shouting mouth when complaining/telegraphing
            val mouthR = if (isTelegraphing || boss.action == BossAction.CHARGING) 11f else 6f
            canvas.drawCircle(x + dir * 6f, headY + 14f, mouthR, mouthPaint)
        }
    }

    private fun drawIvanoneBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            val auraRadius = r * (1.2f + 0.12f * sin(elapsedTime * 20f))
            canvas.drawCircle(x, y, auraRadius, bossTelegraphPaint)
        }

        // Legs (athletic stance)
        bossArmorPaint.color = if (isHit) Color.WHITE else Color.parseColor("#212121")
        canvas.drawRoundRect(
            RectF(x - r * 0.38f, y + r * 0.35f, x - r * 0.08f, y + r * 0.98f),
            12f,
            12f,
            bossArmorPaint
        )
        canvas.drawRoundRect(
            RectF(x + r * 0.08f, y + r * 0.35f, x + r * 0.38f, y + r * 0.98f),
            12f,
            12f,
            bossArmorPaint
        )

        // Heroic V-taper muscular torso
        val torsoPaint = if (isHit) bossArmorPaint else skinPaint
        tempPath.reset()
        tempPath.moveTo(x - r * 0.72f, y - r * 0.42f)
        tempPath.lineTo(x + r * 0.72f, y - r * 0.42f)
        tempPath.lineTo(x + r * 0.38f, y + r * 0.40f)
        tempPath.lineTo(x - r * 0.38f, y + r * 0.40f)
        tempPath.close()
        canvas.drawPath(tempPath, torsoPaint)

        // Big muscular shoulders / biceps
        canvas.drawCircle(x - r * 0.68f, y - r * 0.28f, r * 0.26f, torsoPaint)
        canvas.drawCircle(x + r * 0.68f, y - r * 0.28f, r * 0.26f, torsoPaint)

        // Defined chest/abs lines
        if (!isHit) {
            canvas.drawLine(x - 22f, y - r * 0.12f, x + 22f, y - r * 0.12f, darkDetailPaint)
            canvas.drawLine(x, y - r * 0.12f, x, y + r * 0.26f, darkDetailPaint)
        }

        // Head + stylish hair
        val headY = y - r * 0.76f
        canvas.drawCircle(x, headY, r * 0.30f, torsoPaint)
        tempRect.set(x - r * 0.32f, headY - r * 0.36f, x + r * 0.32f, headY - r * 0.06f)
        canvas.drawRoundRect(tempRect, 14f, 14f, chanHairPaint)

        // DSLR Camera held in front when telegraphing camera flash
        if (boss.action == BossAction.TELEGRAPH_CHARGE) {
            val camX = x + boss.facingDir * r * 0.58f
            val camY = y - r * 0.28f
            tempRect.set(camX - 24f, camY - 16f, camX + 24f, camY + 16f)
            canvas.drawRoundRect(tempRect, 8f, 8f, droneBodyPaint)
            canvas.drawCircle(camX + boss.facingDir * 8f, camY, 10f, cameraFlashOuterPaint)
        }
    }

    private fun drawPlayerShip(canvas: Canvas, player: Player, elapsedTime: Float) {
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

    private fun drawMeleeCharacter(
        canvas: Canvas,
        player: Player,
        elapsedTime: Float,
        hasShield: Boolean
    ) {
        if (player.isInvulnerable && ((elapsedTime * 16f).toInt() % 2 == 0)) {
            return
        }

        val x = player.x
        val y = player.y
        val r = player.radius
        val dir = player.facingDir.toFloat()
        val walkSwing = if (player.onGround && abs(player.vx) > 10f) {
            sin(elapsedTime * 18f) * 14f
        } else {
            0f
        }

        canvas.drawLine(x - 6f, y + r * 0.3f, x - 12f - walkSwing, y + r, playerLimbPaint)
        canvas.drawLine(x + 6f, y + r * 0.3f, x + 12f + walkSwing, y + r, playerLimbPaint)

        tempRect.set(x - 16f, y - r * 0.32f, x + 16f, y + r * 0.38f)
        canvas.drawRoundRect(tempRect, 10f, 10f, playerPaint)

        canvas.drawCircle(x, y - r * 0.68f, 17f, playerAccentPaint)

        if (player.hasDamageBoost) {
            canvas.drawCircle(x, y, r * 1.48f, bossTelegraphPaint)
        } else if (player.hasSpeedBoost) {
            canvas.drawCircle(x, y, r * 1.38f, slashArcPaint.apply { strokeWidth = 6f })
            slashArcPaint.strokeWidth = 16f
        }

        val armStartX = x + dir * 12f
        val armStartY = y - r * 0.1f
        when (player.weapon) {
            WeaponType.HAMMER -> {
                val tipX = if (player.isAttacking) x + dir * 90f else x + dir * 56f
                val tipY = if (player.isAttacking) y + 4f else y - 28f
                canvas.drawLine(armStartX, armStartY, tipX, tipY, swordPaint)
                tempRect.set(tipX - 20f, tipY - 14f, tipX + 20f, tipY + 14f)
                canvas.drawRoundRect(tempRect, 6f, 6f, hanoiLanternGoldPaint)
                canvas.drawRoundRect(tempRect, 6f, 6f, controlAttackBorderPaint)
            }
            WeaponType.KATANA -> {
                val tipX = if (player.isAttacking) x + dir * 98f else x + dir * 64f
                val tipY = if (player.isAttacking) y - 8f else y - 34f
                canvas.drawLine(armStartX, armStartY, tipX, tipY, speechBubbleBorderPaint)
            }
            WeaponType.SPEAR -> {
                val tipX = if (player.isAttacking) x + dir * 112f else x + dir * 74f
                val tipY = y - 10f
                canvas.drawLine(armStartX, armStartY, tipX, tipY, chanGoldTrimPaint)
                canvas.drawCircle(tipX, tipY, 8f, hanoiLanternGoldPaint)
            }
            WeaponType.SWORD -> {
                val swordTipX = if (player.isAttacking) x + dir * 82f else x + dir * 48f
                val swordTipY = if (player.isAttacking) y - 6f else y - 24f
                canvas.drawLine(armStartX, armStartY, swordTipX, swordTipY, swordPaint)
            }
        }

        if (hasShield) {
            val shieldX = if (player.isBlocking) x + dir * 34f else x - dir * 20f
            val shieldHalfW = if (player.isBlocking) 14f else 10f
            val shieldHalfH = if (player.isBlocking) 34f else 22f
            tempRect.set(
                shieldX - shieldHalfW,
                y - shieldHalfH,
                shieldX + shieldHalfW,
                y + shieldHalfH
            )
            shieldFillPaint.color = if (player.shieldFlashTimer > 0f) {
                Color.parseColor("#FFF59D")
            } else {
                Color.parseColor("#1565C0")
            }
            canvas.drawRoundRect(tempRect, 10f, 10f, shieldFillPaint)
            canvas.drawRoundRect(tempRect, 10f, 10f, shieldBorderPaint)

            if (player.isBlocking) {
                canvas.drawCircle(x, y - 4f, r * 1.42f, shieldAuraPaint)
            }
        }

        if (player.isAttacking) {
            val arcRadius = if (player.weapon == WeaponType.HAMMER) 76f else 58f
            val arcCenterX = x + dir * 48f
            tempRect.set(arcCenterX - arcRadius, y - arcRadius, arcCenterX + arcRadius, y + arcRadius)
            val startAngle = if (player.facingDir >= 0) -65f else 115f
            canvas.drawArc(tempRect, startAngle, 130f, false, slashArcPaint)
        }
    }

    private fun drawMeleeControls(canvas: Canvas, hasShield: Boolean) {
        drawControlButton(canvas, leftButtonRect(hasShield), "◀", style = ControlStyle.NORMAL)
        drawControlButton(canvas, rightButtonRect(hasShield), "▶", style = ControlStyle.NORMAL)
        if (hasShield) {
            drawControlButton(canvas, shieldButtonRect(), "SCUDO", style = ControlStyle.SHIELD)
        }
        drawControlButton(canvas, jumpButtonRect(hasShield), "SALTA", style = ControlStyle.NORMAL)
        drawControlButton(canvas, attackButtonRect(hasShield), "COLPO", style = ControlStyle.ATTACK)
    }

    private enum class ControlStyle {
        NORMAL,
        SHIELD,
        ATTACK
    }

    private fun drawControlButton(
        canvas: Canvas,
        rect: RectF,
        label: String,
        style: ControlStyle
    ) {
        val bg = when (style) {
            ControlStyle.NORMAL -> controlBgPaint
            ControlStyle.SHIELD -> controlShieldBgPaint
            ControlStyle.ATTACK -> controlAccentBgPaint
        }
        val border = when (style) {
            ControlStyle.NORMAL -> controlBorderPaint
            ControlStyle.SHIELD -> controlShieldBorderPaint
            ControlStyle.ATTACK -> controlAttackBorderPaint
        }
        canvas.drawRoundRect(rect, 24f, 24f, bg)
        canvas.drawRoundRect(rect, 24f, 24f, border)
        canvas.drawText(label, rect.centerX(), rect.centerY() + 11f, controlLabelPaint)
    }

    private fun drawProjectiles(canvas: Canvas, state: GameState) {
        for (projectile in state.projectiles) {
            when (projectile.kind) {
                ProjectileKind.PLAYER_BOLT -> {
                    tempRect.set(
                        projectile.x - projectile.radius * 0.55f,
                        projectile.y - projectile.radius * 1.4f,
                        projectile.x + projectile.radius * 0.55f,
                        projectile.y + projectile.radius * 1.4f
                    )
                    canvas.drawRoundRect(tempRect, 8f, 8f, playerShotPaint)
                }
                ProjectileKind.SHOCKWAVE -> {
                    tempRect.set(
                        projectile.x - projectile.radius * 1.3f,
                        projectile.y - projectile.radius * 0.85f,
                        projectile.x + projectile.radius * 1.3f,
                        projectile.y + projectile.radius
                    )
                    canvas.drawRoundRect(tempRect, 14f, 14f, shockwavePaint)
                    canvas.drawCircle(
                        projectile.x,
                        projectile.y,
                        projectile.radius * 0.45f,
                        shockwaveCorePaint
                    )
                }
                ProjectileKind.LANTERN -> {
                    tempRect.set(
                        projectile.x - projectile.radius * 0.85f,
                        projectile.y - projectile.radius * 1.15f,
                        projectile.x + projectile.radius * 0.85f,
                        projectile.y + projectile.radius * 1.15f
                    )
                    canvas.drawOval(tempRect, hanoiLanternRedPaint)
                    canvas.drawCircle(
                        projectile.x,
                        projectile.y,
                        projectile.radius * 0.45f,
                        hanoiLanternGoldPaint
                    )
                }
                ProjectileKind.COMPLAINT -> {
                    val text = projectile.label ?: "CHE ANSIA!"
                    val halfW = (speechBubbleTextPaint.measureText(text) / 2f + 22f).coerceAtLeast(72f)
                    val halfH = 26f
                    tempRect.set(
                        projectile.x - halfW,
                        projectile.y - halfH,
                        projectile.x + halfW,
                        projectile.y + halfH
                    )
                    canvas.drawRoundRect(tempRect, 16f, 16f, speechBubbleBgPaint)
                    canvas.drawRoundRect(tempRect, 16f, 16f, speechBubbleBorderPaint)
                    canvas.drawText(text, projectile.x, projectile.y + 8f, speechBubbleTextPaint)
                }
                ProjectileKind.DRONE -> {
                    val x = projectile.x
                    val y = projectile.y
                    val rotorJitter = sin(state.elapsedTime * 40f) * 6f
                    // Drone arms & spinning rotors
                    canvas.drawLine(x - 30f, y - 16f, x - 8f, y - 16f + rotorJitter * 0.2f, droneRotorPaint)
                    canvas.drawLine(x + 8f, y - 16f, x + 30f, y - 16f - rotorJitter * 0.2f, droneRotorPaint)
                    tempRect.set(x - 24f, y - 10f, x + 24f, y + 12f)
                    canvas.drawRoundRect(tempRect, 8f, 8f, droneBodyPaint)
                    canvas.drawCircle(x, y + 1f, 6f, droneLedPaint)
                }
                ProjectileKind.CAMERA_FLASH -> {
                    val x = projectile.x
                    val y = projectile.y
                    val r = projectile.radius * 1.3f
                    canvas.drawCircle(x, y, r, cameraFlashOuterPaint)
                    canvas.drawLine(x - r * 1.2f, y, x + r * 1.2f, y, cameraFlashInnerPaint)
                    canvas.drawLine(x, y - r * 1.2f, x, y + r * 1.2f, cameraFlashInnerPaint)
                    canvas.drawCircle(x, y, r * 0.55f, cameraFlashInnerPaint)
                }
                ProjectileKind.TAX_FORM -> {
                    val text = projectile.label ?: "TASSE CHF!"
                    val halfW = (speechBubbleTextPaint.measureText(text) / 2f + 22f).coerceAtLeast(82f)
                    val halfH = 28f
                    tempRect.set(
                        projectile.x - halfW,
                        projectile.y - halfH,
                        projectile.x + halfW,
                        projectile.y + halfH
                    )
                    canvas.drawRoundRect(tempRect, 10f, 10f, speechBubbleBgPaint)
                    canvas.drawRoundRect(tempRect, 10f, 10f, controlShieldBorderPaint)
                    canvas.drawText(text, projectile.x, projectile.y + 8f, speechBubbleTextPaint)
                }
                ProjectileKind.WEIRD_TOOTH -> {
                    val x = projectile.x
                    val y = projectile.y
                    val r = projectile.radius
                    // Giant weird crooked tooth!
                    tempPath.reset()
                    tempPath.moveTo(x - r * 0.7f, y - r * 0.8f)
                    tempPath.lineTo(x + r * 0.75f, y - r * 0.65f)
                    tempPath.lineTo(x + r * 0.5f, y + r * 0.9f)
                    tempPath.lineTo(x + r * 0.05f, y + r * 0.35f)
                    tempPath.lineTo(x - r * 0.45f, y + r * 0.95f)
                    tempPath.close()
                    canvas.drawPath(tempPath, swissCrossWhitePaint)
                    canvas.drawPath(tempPath, controlShieldBorderPaint)
                }
                ProjectileKind.HEART -> {
                    val x = projectile.x
                    val y = projectile.y
                    val r = projectile.radius
                    val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#EC407A")
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(x - r * 0.38f, y - r * 0.2f, r * 0.52f, heartPaint)
                    canvas.drawCircle(x + r * 0.38f, y - r * 0.2f, r * 0.52f, heartPaint)
                    tempPath.reset()
                    tempPath.moveTo(x - r * 0.85f, y - r * 0.05f)
                    tempPath.lineTo(x, y + r * 0.95f)
                    tempPath.lineTo(x + r * 0.85f, y - r * 0.05f)
                    tempPath.close()
                    canvas.drawPath(tempPath, heartPaint)
                    projectile.label?.let { text ->
                        canvas.drawText(text, x, y - r - 10f, phaseBadgePaint)
                    }
                }
                ProjectileKind.SCREAM_WAVE -> {
                    val text = projectile.label ?: "OHHH!"
                    val halfW = (speechBubbleTextPaint.measureText(text) / 2f + 26f).coerceAtLeast(90f)
                    val halfH = 30f
                    tempRect.set(
                        projectile.x - halfW,
                        projectile.y - halfH,
                        projectile.x + halfW,
                        projectile.y + halfH
                    )
                    canvas.drawRoundRect(tempRect, 20f, 20f, shockwaveCorePaint)
                    canvas.drawRoundRect(tempRect, 20f, 20f, speechBubbleBorderPaint)
                    canvas.drawText(text, projectile.x, projectile.y + 8f, speechBubbleTextPaint)
                }
                ProjectileKind.RICE_CHICKEN -> {
                    val x = projectile.x
                    val y = projectile.y
                    val r = projectile.radius
                    // Meal-prep bowl with white rice + golden chicken chunks
                    tempRect.set(x - r * 1.15f, y - r * 0.2f, x + r * 1.15f, y + r * 0.95f)
                    canvas.drawRoundRect(tempRect, 14f, 14f, droneBodyPaint)
                    canvas.drawCircle(x - 10f, y - 6f, 13f, swissCrossWhitePaint)
                    canvas.drawCircle(x + 10f, y - 6f, 13f, swissCrossWhitePaint)
                    canvas.drawCircle(x, y - 10f, 10f, hanoiLanternGoldPaint)
                    canvas.drawText("200g", x, y + r * 0.55f, speechBubbleTextPaint)
                }
                ProjectileKind.DATING_FLEX -> {
                    val text = projectile.label ?: "SONO PIENO DI MILIONI!"
                    val halfW = (speechBubbleTextPaint.measureText(text) / 2f + 24f).coerceAtLeast(92f)
                    val halfH = 28f
                    tempRect.set(
                        projectile.x - halfW,
                        projectile.y - halfH,
                        projectile.x + halfW,
                        projectile.y + halfH
                    )
                    canvas.drawRoundRect(tempRect, 18f, 18f, hanoiLanternGoldPaint)
                    canvas.drawRoundRect(tempRect, 18f, 18f, speechBubbleBorderPaint)
                    canvas.drawText(text, projectile.x, projectile.y + 8f, speechBubbleTextPaint)
                }
                ProjectileKind.HOT_STONE -> {
                    val x = projectile.x
                    val y = projectile.y
                    val r = projectile.radius
                    tempRect.set(x - r * 1.15f, y - r * 0.85f, x + r * 1.15f, y + r * 0.85f)
                    canvas.drawOval(tempRect, droneBodyPaint)
                    canvas.drawOval(tempRect, shockwavePaint)
                    canvas.drawCircle(x, y, r * 0.42f, shockwaveCorePaint)
                }
                ProjectileKind.BOSS_ORB -> {
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
    }

    private fun drawGoogleCampusBackground(canvas: Canvas, state: GameState) {
        skyGradientPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            state.groundY,
            intArrayOf(
                Color.parseColor("#111827"),
                Color.parseColor("#1E293B"),
                Color.parseColor("#334155")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, state.worldWidth, state.groundY, skyGradientPaint)
        skyGradientPaint.shader = null

        // Modern Google glass office building
        canvas.drawRect(140f, state.groundY - 420f, 860f, state.groundY, zurichCityPaint)
        // Four iconic Google color dots
        val dotColors = intArrayOf(
            Color.parseColor("#4285F4"),
            Color.parseColor("#EA4335"),
            Color.parseColor("#FBBC05"),
            Color.parseColor("#34A853")
        )
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (i in dotColors.indices) {
            dotPaint.color = dotColors[i]
            canvas.drawCircle(365f + i * 90f, state.groundY - 335f, 24f, dotPaint)
        }
        canvas.drawText("GOOGLE CAMPUS", 500f, state.groundY - 260f, controlLabelPaint)
    }

    private fun drawMarcoGoogleBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val dir = boss.facingDir.toFloat()
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            canvas.drawCircle(x, y, r * (1.28f + 0.1f * sin(elapsedTime * 22f)), bossTelegraphPaint)
        }

        // Marco sitting in the open Porsche cockpit
        val driverY = y - r * 0.42f
        canvas.drawCircle(x - dir * 8f, driverY - 26f, 22f, if (isHit) bossArmorPaint else skinPaint)
        tempRect.set(x - dir * 8f - 22f, driverY - 50f, x - dir * 8f + 22f, driverY - 30f)
        canvas.drawRoundRect(tempRect, 10f, 10f, chanHairPaint)
        // Camera held out the Porsche window
        val camX = x + dir * 42f
        val camY = driverY - 12f
        tempRect.set(camX - 18f, camY - 12f, camX + 18f, camY + 12f)
        canvas.drawRoundRect(tempRect, 6f, 6f, droneBodyPaint)
        canvas.drawCircle(camX + dir * 8f, camY, 8f, cameraFlashOuterPaint)

        // White Porsche 911 coupe body
        bossArmorPaint.color = if (isHit) Color.parseColor("#FFF59D") else Color.WHITE
        tempPath.reset()
        tempPath.moveTo(x - r * 1.25f, y + r * 0.58f)
        tempPath.lineTo(x - r * 1.15f, y + r * 0.02f)
        tempPath.lineTo(x - r * 0.55f, y - r * 0.36f)
        tempPath.lineTo(x + r * 0.45f, y - r * 0.36f)
        tempPath.lineTo(x + r * 1.18f, y + r * 0.08f)
        tempPath.lineTo(x + r * 1.25f, y + r * 0.58f)
        tempPath.close()
        canvas.drawPath(tempPath, bossArmorPaint)
        canvas.drawPath(tempPath, controlBorderPaint)

        // Glowing headlight
        canvas.drawCircle(x + dir * r * 1.05f, y + r * 0.24f, 11f, hanoiLanternGoldPaint)

        // Sport wheels with red Porsche brake calipers
        val wheelY = y + r * 0.64f
        for (wx in listOf(x - r * 0.72f, x + r * 0.72f)) {
            canvas.drawCircle(wx, wheelY, 28f, chanHairPaint)
            canvas.drawCircle(wx, wheelY, 15f, barBorderPaint)
            canvas.drawCircle(wx, wheelY, 7f, hanoiLanternRedPaint)
        }
    }

    private fun drawZurichTaxBackground(canvas: Canvas, state: GameState) {
        drawZurichBackground(canvas, state)
        // Golden "TAX CHF" badge on the skyline
        tempRect.set(60f, 205f, 280f, 270f)
        canvas.drawRoundRect(tempRect, 12f, 12f, zurichCityPaint)
        canvas.drawRoundRect(tempRect, 12f, 12f, controlShieldBorderPaint)
        canvas.drawText("TAX CHF", 170f, 248f, controlLabelPaint)
    }

    private fun drawParisBackground(canvas: Canvas, state: GameState) {
        skyGradientPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            state.groundY,
            intArrayOf(
                Color.parseColor("#1D1433"),
                Color.parseColor("#4A235A"),
                Color.parseColor("#9B4B6B")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, state.worldWidth, state.groundY, skyGradientPaint)
        skyGradientPaint.shader = null

        // Eiffel Tower silhouette
        val cx = state.worldWidth * 0.5f
        val baseY = state.groundY
        tempPath.reset()
        tempPath.moveTo(cx - 150f, baseY)
        tempPath.lineTo(cx - 26f, baseY - 520f)
        tempPath.lineTo(cx + 26f, baseY - 520f)
        tempPath.lineTo(cx + 150f, baseY)
        tempPath.lineTo(cx + 80f, baseY)
        tempPath.lineTo(cx, baseY - 140f)
        tempPath.lineTo(cx - 80f, baseY)
        tempPath.close()
        canvas.drawPath(tempPath, hanoiSilhouettePaint)
        canvas.drawLine(cx, baseY - 520f, cx, baseY - 600f, hanoiGroundLinePaint)
    }

    private fun drawUbsAlpsBackground(canvas: Canvas, state: GameState) {
        drawZurichBackground(canvas, state)
        // UBS red badge & Hike signpost
        tempRect.set(64f, 205f, 224f, 270f)
        canvas.drawRoundRect(tempRect, 12f, 12f, hanoiLanternRedPaint)
        canvas.drawText("UBS", 144f, 248f, controlLabelPaint)

        tempRect.set(660f, 940f, 890f, 1000f)
        canvas.drawRoundRect(tempRect, 10f, 10f, hanoiLanternGoldPaint)
        canvas.drawText("HIKE ->", 775f, 980f, speechBubbleTextPaint)
    }

    private fun drawGymDevBackground(canvas: Canvas, state: GameState) {
        skyGradientPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            state.groundY,
            intArrayOf(
                Color.parseColor("#121520"),
                Color.parseColor("#1D2336"),
                Color.parseColor("#2A2E45")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, state.worldWidth, state.groundY, skyGradientPaint)
        skyGradientPaint.shader = null

        // Dev monitor showing red syntax errors in background
        tempRect.set(160f, 680f, 540f, 940f)
        canvas.drawRoundRect(tempRect, 16f, 16f, zurichCityPaint)
        canvas.drawRoundRect(tempRect, 16f, 16f, controlBorderPaint)
        canvas.drawText("NullPointerException", 350f, 770f, phaseBadgePaint)
        canvas.drawText("BUILD FAILED", 350f, 845f, bossNamePaint)

        // 200g meal-prep kitchen scale
        tempRect.set(640f, 860f, 860f, 940f)
        canvas.drawRoundRect(tempRect, 14f, 14f, barBgPaint)
        canvas.drawRoundRect(tempRect, 14f, 14f, controlShieldBorderPaint)
        canvas.drawText("200g", 750f, 912f, controlLabelPaint)
    }

    private fun drawAlessioTaxBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            canvas.drawCircle(x, y, r * (1.18f + 0.12f * sin(elapsedTime * 20f)), bossTelegraphPaint)
        }

        // Luxury navy suit & gold tie
        bossArmorPaint.color = if (isHit) Color.WHITE else Color.parseColor("#1A237E")
        tempRect.set(x - r * 0.56f, y - r * 0.28f, x + r * 0.56f, y + r * 0.96f)
        canvas.drawRoundRect(tempRect, 18f, 18f, bossArmorPaint)
        tempPath.reset()
        tempPath.moveTo(x - 10f, y - r * 0.25f)
        tempPath.lineTo(x + 10f, y - r * 0.25f)
        tempPath.lineTo(x, y + r * 0.45f)
        tempPath.close()
        canvas.drawPath(tempPath, hanoiLanternGoldPaint)

        // Head & hair
        val headY = y - r * 0.68f
        val headR = r * 0.40f
        canvas.drawCircle(x, headY, headR, if (isHit) bossArmorPaint else skinPaint)
        tempRect.set(x - headR, headY - headR * 1.05f, x + headR, headY - headR * 0.35f)
        canvas.drawRoundRect(tempRect, 14f, 14f, chanHairPaint)

        // Smug grin + THE WEIRD GIANT TOOTH!
        if (!isHit) {
            val dir = boss.facingDir.toFloat()
            canvas.drawArc(
                RectF(x - 16f, headY + 2f, x + 16f, headY + 22f),
                10f,
                160f,
                false,
                darkDetailPaint
            )
            // Oversized weird protruding tooth
            tempPath.reset()
            tempPath.moveTo(x + dir * 2f, headY + 12f)
            tempPath.lineTo(x + dir * 18f, headY + 12f)
            tempPath.lineTo(x + dir * 13f, headY + 34f)
            tempPath.close()
            canvas.drawPath(tempPath, swissCrossWhitePaint)
            canvas.drawPath(tempPath, controlShieldBorderPaint)
        }
    }

    private fun drawLouiseBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            canvas.drawCircle(x, y, r * (1.2f + 0.12f * sin(elapsedTime * 20f)), bossTelegraphPaint)
        }

        // Flowing chestnut hair
        bossArmorPaint.color = if (isHit) Color.WHITE else Color.parseColor("#5D4037")
        tempRect.set(x - r * 0.46f, y - r * 0.85f, x + r * 0.46f, y + r * 0.16f)
        canvas.drawRoundRect(tempRect, 22f, 22f, bossArmorPaint)

        // Chic Parisian dress
        bossCorePaint.color = if (isHit) Color.WHITE else Color.parseColor("#AD1457")
        tempPath.reset()
        tempPath.moveTo(x - r * 0.34f, y - r * 0.28f)
        tempPath.lineTo(x + r * 0.34f, y - r * 0.28f)
        tempPath.lineTo(x + r * 0.56f, y + r * 0.96f)
        tempPath.lineTo(x - r * 0.56f, y + r * 0.96f)
        tempPath.close()
        canvas.drawPath(tempPath, bossCorePaint)

        // Face + bright blue eyes!
        val headY = y - r * 0.60f
        canvas.drawCircle(x, headY, r * 0.32f, if (isHit) bossArmorPaint else skinPaint)
        if (!isHit) {
            canvas.drawCircle(x - 10f, headY - 4f, 5.5f, playerPaint)
            canvas.drawCircle(x + 10f, headY - 4f, 5.5f, playerPaint)
        }

        // Chic red French beret
        tempRect.set(x - r * 0.46f, headY - r * 0.44f, x + r * 0.36f, headY - r * 0.14f)
        canvas.drawOval(tempRect, hanoiLanternRedPaint)
    }

    private fun drawDaisyBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            canvas.drawCircle(x, y, r * (1.22f + 0.14f * sin(elapsedTime * 22f)), bossTelegraphPaint)
        }

        // Dark wavy hair
        tempRect.set(x - r * 0.46f, y - r * 0.86f, x + r * 0.46f, y + r * 0.14f)
        canvas.drawRoundRect(tempRect, 20f, 20f, chanHairPaint)

        // Corporate UBS blazer + red lanyard
        bossArmorPaint.color = if (isHit) Color.WHITE else Color.parseColor("#ECEFF1")
        tempRect.set(x - r * 0.48f, y - r * 0.28f, x + r * 0.48f, y + r * 0.96f)
        canvas.drawRoundRect(tempRect, 18f, 18f, bossArmorPaint)
        canvas.drawLine(x - 12f, y - r * 0.28f, x, y + r * 0.22f, speechBubbleBorderPaint)
        canvas.drawLine(x + 12f, y - r * 0.28f, x, y + r * 0.22f, speechBubbleBorderPaint)
        tempRect.set(x - 14f, y + r * 0.18f, x + 14f, y + r * 0.42f)
        canvas.drawRoundRect(tempRect, 4f, 4f, hanoiLanternRedPaint)

        // Face + wide open screaming mouth!
        val headY = y - r * 0.62f
        canvas.drawCircle(x, headY, r * 0.34f, if (isHit) bossArmorPaint else skinPaint)
        if (!isHit) {
            canvas.drawLine(x - 14f, headY - 10f, x - 3f, headY - 4f, darkDetailPaint)
            canvas.drawLine(x + 14f, headY - 10f, x + 3f, headY - 4f, darkDetailPaint)
            val screamR = if (isTelegraphing || boss.action == BossAction.CHARGING) 14f else 9f
            canvas.drawCircle(x, headY + 12f, screamR, mouthPaint)
        }
    }

    private fun drawAndreaDatteroBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            canvas.drawCircle(x, y, r * (1.22f + 0.12f * sin(elapsedTime * 20f)), bossTelegraphPaint)
        }

        // Very skinny legs!
        canvas.drawLine(x - 10f, y + r * 0.35f, x - 14f, y + r * 0.98f, darkDetailPaint)
        canvas.drawLine(x + 10f, y + r * 0.35f, x + 14f, y + r * 0.98f, darkDetailPaint)

        // Oversized black MOSCHINO hoodie on a very skinny frame
        bossArmorPaint.color = if (isHit) Color.WHITE else Color.parseColor("#111116")
        tempRect.set(x - r * 0.52f, y - r * 0.36f, x + r * 0.52f, y + r * 0.46f)
        canvas.drawRoundRect(tempRect, 18f, 18f, bossArmorPaint)
        canvas.drawRoundRect(tempRect, 18f, 18f, controlShieldBorderPaint)

        if (!isHit) {
            val moschinoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFD54F")
                textSize = 18f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.drawText("MOSCHINO", x, y + 6f, moschinoPaint)
        }

        // Grappling arms reaching forward
        val dir = boss.facingDir.toFloat()
        canvas.drawLine(x + dir * 24f, y - 6f, x + dir * 56f, y - 10f, darkDetailPaint)

        // Skinny head + hair
        val headY = y - r * 0.72f
        canvas.drawCircle(x, headY, r * 0.28f, if (isHit) bossArmorPaint else skinPaint)
        tempRect.set(x - r * 0.30f, headY - r * 0.32f, x + r * 0.30f, headY - r * 0.06f)
        canvas.drawRoundRect(tempRect, 10f, 10f, chanHairPaint)
    }

    private fun drawPickups(canvas: Canvas, state: GameState) {
        for (pickup in state.pickups) {
            val bobY = pickup.y + sin(state.elapsedTime * 5f + pickup.x * 0.02f) * 8f
            val bgColor = when (pickup.kind) {
                PickupKind.WEAPON_HAMMER -> Color.parseColor("#FF8F00")
                PickupKind.WEAPON_KATANA -> Color.parseColor("#D81B60")
                PickupKind.WEAPON_SPEAR -> Color.parseColor("#00ACC1")
                PickupKind.HEAL -> Color.parseColor("#43A047")
                PickupKind.SPEED_BOOST -> Color.parseColor("#039BE5")
                PickupKind.POWER_SURGE -> Color.parseColor("#F4511E")
            }
            bossCorePaint.color = bgColor
            canvas.drawCircle(pickup.x, bobY, pickup.radius, bossCorePaint)
            canvas.drawCircle(pickup.x, bobY, pickup.radius, controlShieldBorderPaint)
            canvas.drawText(pickup.kind.label, pickup.x, bobY - pickup.radius - 10f, controlLabelPaint.apply {
                textSize = 22f
            })
            controlLabelPaint.textSize = 32f
        }
    }

    private fun drawHanoiSpaBackground(canvas: Canvas, state: GameState) {
        skyGradientPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            state.groundY,
            intArrayOf(
                Color.parseColor("#211219"),
                Color.parseColor("#4E2428"),
                Color.parseColor("#8A4733")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, state.worldWidth, state.groundY, skyGradientPaint)
        skyGradientPaint.shader = null

        // Bamboo silhouettes on left and right
        val bambooPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E1D21")
            strokeWidth = 18f
        }
        for (bx in listOf(90f, 155f, 845f, 910f)) {
            canvas.drawLine(bx, 220f, bx, state.groundY, bambooPaint)
        }

        // Warm spa lotus emblem & candles
        val cx = state.worldWidth * 0.5f
        canvas.drawCircle(cx, 540f, 120f, studioBeamPaint)
        canvas.drawText("HANOI SPA", cx, 555f, controlLabelPaint)

        for (candleX in listOf(260f, 500f, 740f)) {
            val base = state.groundY
            canvas.drawRect(candleX - 14f, base - 48f, candleX + 14f, base, swissCrossWhitePaint)
            val flicker = sin(state.elapsedTime * 12f + candleX) * 4f
            canvas.drawCircle(candleX + flicker * 0.3f, base - 58f, 10f, hanoiLanternGoldPaint)
        }
    }

    private fun drawCaramelBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            canvas.drawCircle(x, y, r * (1.25f + 0.12f * sin(elapsedTime * 20f)), bossTelegraphPaint)
        }

        // Golden-cyan hot-stone Armor Shield around Caramel when armor > 0!
        if (boss.hasArmor) {
            val armorRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(180, 38, 198, 218)
                style = Paint.Style.STROKE
                strokeWidth = 12f
            }
            canvas.drawCircle(x, y, r * 1.32f, armorRingPaint)
            canvas.drawCircle(x, y, r * 1.18f, shieldAuraPaint)
        }

        // Dark bun hair + lotus pin
        canvas.drawCircle(x, y - r * 0.96f, r * 0.22f, chanHairPaint)
        canvas.drawCircle(x + 14f, y - r * 1.02f, 10f, hanoiLanternRedPaint)

        // Caramel-gold silk spa tunic
        bossArmorPaint.color = when {
            isHit -> Color.WHITE
            boss.phase == 3 -> Color.parseColor("#D84315")
            boss.phase == 2 -> Color.parseColor("#E65100")
            else -> Color.parseColor("#C67C3B")
        }
        tempPath.reset()
        tempPath.moveTo(x - r * 0.38f, y - r * 0.32f)
        tempPath.lineTo(x + r * 0.38f, y - r * 0.32f)
        tempPath.lineTo(x + r * 0.58f, y + r * 0.98f)
        tempPath.lineTo(x - r * 0.58f, y + r * 0.98f)
        tempPath.close()
        canvas.drawPath(tempPath, bossArmorPaint)
        canvas.drawLine(x, y - r * 0.30f, x, y + r * 0.94f, chanGoldTrimPaint)

        // Face
        val headY = y - r * 0.62f
        canvas.drawCircle(x, headY, r * 0.32f, if (isHit) bossArmorPaint else skinPaint)

        // Stunned dizzy stars above head when Armor Break triggers!
        if (boss.isStunned) {
            for (i in 0..2) {
                val angle = elapsedTime * 6f + i * 2.1f
                val sx = x + (kotlin.math.cos(angle.toDouble()) * 42f).toFloat()
                val sy = headY - 42f + (sin(angle.toDouble()) * 10f).toFloat()
                canvas.drawCircle(sx, sy, 8f, hanoiLanternGoldPaint)
            }
        }
    }

    private fun drawOmegaFinaleBackground(canvas: Canvas, state: GameState) {
        val phase = state.boss.phase
        val colors = when (phase) {
            1 -> intArrayOf(
                Color.parseColor("#0B0C22"),
                Color.parseColor("#1D1B4B"),
                Color.parseColor("#0E4A6B")
            )
            2 -> intArrayOf(
                Color.parseColor("#1A080C"),
                Color.parseColor("#5C1414"),
                Color.parseColor("#B83B18")
            )
            else -> intArrayOf(
                Color.parseColor("#140B24"),
                Color.parseColor("#4A2B0E"),
                Color.parseColor("#C78D1E")
            )
        }
        skyGradientPaint.shader = LinearGradient(0f, 0f, 0f, state.groundY, colors, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, state.worldWidth, state.groundY, skyGradientPaint)
        skyGradientPaint.shader = null

        val cx = state.worldWidth * 0.5f
        val cy = 540f
        when (phase) {
            1 -> {
                // Scenario 1: Cyber Citadel
                drawArenaGrid(canvas, state.worldWidth, state.groundY)
                canvas.drawRect(60f, state.groundY - 290f, 220f, state.groundY, studioBuildingPaint)
                canvas.drawRect(780f, state.groundY - 340f, 940f, state.groundY, studioBuildingPaint)
                canvas.drawCircle(cx, cy, 130f + sin(state.elapsedTime * 3f) * 10f, controlBorderPaint)
                canvas.drawText("CYBER CITADEL", cx, 360f, controlLabelPaint)
            }
            2 -> {
                // Scenario 2: Vulcano Cosmico
                canvas.drawCircle(cx, cy, 145f, hanoiLanternRedPaint)
                canvas.drawCircle(cx, cy, 118f, studioBuildingPaint)
                tempPath.reset()
                tempPath.moveTo(0f, state.groundY)
                tempPath.lineTo(200f, state.groundY - 340f)
                tempPath.lineTo(420f, state.groundY - 120f)
                tempPath.lineTo(660f, state.groundY - 390f)
                tempPath.lineTo(state.worldWidth, state.groundY - 180f)
                tempPath.lineTo(state.worldWidth, state.groundY)
                tempPath.close()
                canvas.drawPath(tempPath, studioBuildingPaint)
                canvas.drawText("VULCANO COSMICO", cx, 360f, controlLabelPaint)
            }
            else -> {
                // Scenario 3: Singolarita Omega
                for (i in 1..3) {
                    val r = 90f * i + sin(state.elapsedTime * 5f + i) * 14f
                    canvas.drawCircle(cx, cy, r, shieldAuraPaint)
                }
                canvas.drawCircle(cx, cy, 55f, hanoiLanternGoldPaint)
                canvas.drawText("SINGOLARITÀ OMEGA", cx, 360f, controlLabelPaint)
            }
        }
    }

    private fun drawMovingPlatforms(canvas: Canvas, state: GameState) {
        for (platform in state.platforms) {
            val halfW = platform.width / 2f
            val left = platform.x - halfW
            val right = platform.x + halfW
            val top = platform.y
            val bottom = platform.y + platform.height

            tempRect.set(left, top, right, bottom)
            canvas.drawRoundRect(tempRect, 10f, 10f, controlBgPaint)
            val borderPaint = when (state.boss.phase) {
                2 -> controlAttackBorderPaint
                3 -> controlShieldBorderPaint
                else -> controlBorderPaint
            }
            canvas.drawRoundRect(tempRect, 10f, 10f, borderPaint)

            // Glowing underside hover thrusters
            canvas.drawCircle(left + 26f, bottom + 6f, 6f, hanoiLanternGoldPaint)
            canvas.drawCircle(right - 26f, bottom + 6f, 6f, hanoiLanternGoldPaint)
        }
    }

    private fun drawLasers(canvas: Canvas, state: GameState) {
        val warnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 82, 82)
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val outerBeamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 23, 68)
            style = Paint.Style.FILL
        }
        val midBeamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 213, 79)
            style = Paint.Style.FILL
        }

        for (laser in state.lasers) {
            if (laser.telegraphTimer > 0f) {
                val pulse = 4f + 4f * sin(state.elapsedTime * 30f)
                warnPaint.strokeWidth = pulse.coerceAtLeast(3f)
                if (laser.isHorizontal) {
                    canvas.drawLine(0f, laser.position, state.worldWidth, laser.position, warnPaint)
                } else {
                    canvas.drawLine(laser.position, 0f, laser.position, state.groundY, warnPaint)
                }
            } else if (laser.isActive) {
                val halfT = laser.thickness / 2f
                if (laser.isHorizontal) {
                    canvas.drawRect(0f, laser.position - halfT, state.worldWidth, laser.position + halfT, outerBeamPaint)
                    canvas.drawRect(0f, laser.position - halfT * 0.6f, state.worldWidth, laser.position + halfT * 0.6f, midBeamPaint)
                    canvas.drawRect(0f, laser.position - halfT * 0.25f, state.worldWidth, laser.position + halfT * 0.25f, swissCrossWhitePaint)
                } else {
                    canvas.drawRect(laser.position - halfT, 0f, laser.position + halfT, state.groundY, outerBeamPaint)
                    canvas.drawRect(laser.position - halfT * 0.6f, 0f, laser.position + halfT * 0.6f, state.groundY, midBeamPaint)
                    canvas.drawRect(laser.position - halfT * 0.25f, 0f, laser.position + halfT * 0.25f, state.groundY, swissCrossWhitePaint)
                }
            }
        }
    }

    private fun drawOmegaBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f
        val isTelegraphing = boss.action == BossAction.TELEGRAPH_SHOCKWAVE ||
            boss.action == BossAction.TELEGRAPH_CHARGE

        if (isTelegraphing) {
            canvas.drawCircle(x, y, r * (1.30f + 0.14f * sin(elapsedTime * 22f)), bossTelegraphPaint)
        }

        // Armor barrier when armor > 0
        if (boss.hasArmor) {
            val armorRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 38, 198, 218)
                style = Paint.Style.STROKE
                strokeWidth = 12f
            }
            canvas.drawCircle(x, y, r * 1.35f, armorRingPaint)
            canvas.drawCircle(x, y, r * 1.20f, shieldAuraPaint)
        }

        // Orbiting cosmic energy nodes
        for (i in 0..3) {
            val angle = elapsedTime * (2.5f + boss.phase) + i * (Math.PI / 2.0)
            val ox = x + (kotlin.math.cos(angle) * r * 1.12f).toFloat()
            val oy = y + (sin(angle) * r * 0.65f).toFloat()
            canvas.drawCircle(ox, oy, 11f, hanoiLanternGoldPaint)
        }

        // Armored cosmic body
        bossArmorPaint.color = when {
            isHit -> Color.WHITE
            boss.phase == 3 -> Color.parseColor("#FFB300")
            boss.phase == 2 -> Color.parseColor("#C62828")
            else -> Color.parseColor("#283593")
        }
        tempPath.reset()
        tempPath.moveTo(x, y - r * 1.10f)
        tempPath.lineTo(x + r * 0.85f, y - r * 0.35f)
        tempPath.lineTo(x + r * 0.68f, y + r * 0.92f)
        tempPath.lineTo(x - r * 0.68f, y + r * 0.92f)
        tempPath.lineTo(x - r * 0.85f, y - r * 0.35f)
        tempPath.close()
        canvas.drawPath(tempPath, bossArmorPaint)
        canvas.drawPath(tempPath, controlShieldBorderPaint)

        // Radiant inner core
        bossCorePaint.color = when {
            isHit -> Color.WHITE
            boss.phase == 3 -> Color.WHITE
            boss.phase == 2 -> Color.parseColor("#FFAB00")
            else -> Color.parseColor("#00E5FF")
        }
        canvas.drawCircle(x, y, r * 0.36f, bossCorePaint)

        if (boss.isStunned) {
            for (i in 0..2) {
                val angle = elapsedTime * 6f + i * 2.1f
                val sx = x + (kotlin.math.cos(angle.toDouble()) * 46f).toFloat()
                val sy = y - r - 20f + (sin(angle.toDouble()) * 10f).toFloat()
                canvas.drawCircle(sx, sy, 9f, hanoiLanternGoldPaint)
            }
        }
    }

    private fun drawDeepSpaceBackground(canvas: Canvas, state: GameState) {
        skyGradientPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            state.worldHeight,
            intArrayOf(
                Color.parseColor("#050712"),
                Color.parseColor("#0D1229"),
                Color.parseColor("#140E26")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, state.worldWidth, state.worldHeight, skyGradientPaint)
        skyGradientPaint.shader = null

        // Twinkling starfield
        for (i in 0 until 36) {
            val sx = ((i * 197 + 73) % 960 + 20).toFloat()
            val sy = ((i * 313 + 111) % 1520 + 40).toFloat()
            val twinkle = 0.5f + 0.5f * sin(state.elapsedTime * 3.5f + i)
            val starR = if (i % 4 == 0) 3.5f else 2.0f
            particlePaint.color = Color.WHITE
            particlePaint.alpha = (110 + twinkle * 140).toInt()
            canvas.drawCircle(sx, sy, starR, particlePaint)
        }
        particlePaint.alpha = 255
    }

    private fun drawDreadnoughtBoss(canvas: Canvas, boss: Boss, elapsedTime: Float) {
        val x = boss.x
        val y = boss.y
        val r = boss.radius
        val isHit = boss.hitFlashTimer > 0f

        // Twin rear engine thrusters
        val glow = 16f + sin(elapsedTime * 24f) * 5f
        canvas.drawCircle(x - r * 0.48f, y - r * 0.72f, glow, hanoiLanternGoldPaint)
        canvas.drawCircle(x + r * 0.48f, y - r * 0.72f, glow, hanoiLanternGoldPaint)

        // Massive Dreadnought hull
        bossArmorPaint.color = when {
            isHit -> Color.WHITE
            boss.isEnraged -> Color.parseColor("#880E4F")
            else -> Color.parseColor("#37474F")
        }
        tempPath.reset()
        tempPath.moveTo(x, y + r * 1.02f)
        tempPath.lineTo(x + r * 0.96f, y + r * 0.25f)
        tempPath.lineTo(x + r * 0.78f, y - r * 0.76f)
        tempPath.lineTo(x - r * 0.78f, y - r * 0.76f)
        tempPath.lineTo(x - r * 0.96f, y + r * 0.25f)
        tempPath.close()
        canvas.drawPath(tempPath, bossArmorPaint)
        canvas.drawPath(tempPath, bossOutlinePaint)

        // Side hangar bays & turrets
        tempRect.set(x - r * 1.12f, y - 16f, x - r * 0.72f, y + 32f)
        canvas.drawRoundRect(tempRect, 8f, 8f, controlBgPaint)
        canvas.drawRoundRect(tempRect, 8f, 8f, controlAttackBorderPaint)
        tempRect.set(x + r * 0.72f, y - 16f, x + r * 1.12f, y + 32f)
        canvas.drawRoundRect(tempRect, 8f, 8f, controlBgPaint)
        canvas.drawRoundRect(tempRect, 8f, 8f, controlAttackBorderPaint)

        // Glowing central bridge / reactor core
        bossCorePaint.color = when {
            isHit -> Color.WHITE
            boss.isEnraged -> Color.parseColor("#FFAB00")
            else -> Color.parseColor("#FF5252")
        }
        canvas.drawCircle(x, y + r * 0.08f, r * 0.30f, bossCorePaint)
        canvas.drawCircle(x, y + r * 0.08f, r * 0.14f, swissCrossWhitePaint)
    }

    private fun drawMinions(canvas: Canvas, state: GameState) {
        for (minion in state.minions) {
            val r = minion.radius
            canvas.save()
            canvas.translate(minion.x, minion.y)
            canvas.rotate(Math.toDegrees(minion.angle.toDouble()).toFloat() + 90f)

            bossArmorPaint.color = if (minion.hitFlashTimer > 0f) Color.WHITE else Color.parseColor("#FF7043")
            tempPath.reset()
            tempPath.moveTo(0f, -r * 1.15f)
            tempPath.lineTo(r * 0.9f, r * 0.85f)
            tempPath.lineTo(0f, r * 0.45f)
            tempPath.lineTo(-r * 0.9f, r * 0.85f)
            tempPath.close()
            canvas.drawPath(tempPath, bossArmorPaint)
            canvas.drawCircle(0f, 0f, r * 0.25f, hanoiLanternGoldPaint)
            canvas.restore()
        }
    }

    private fun drawRotatedPlayerShip(canvas: Canvas, player: Player, elapsedTime: Float) {
        if (player.isInvulnerable && ((elapsedTime * 16f).toInt() % 2 == 0)) {
            return
        }
        val r = player.radius
        canvas.save()
        canvas.translate(player.x, player.y)
        canvas.rotate(Math.toDegrees(player.angle.toDouble()).toFloat() + 90f)

        // Thruster flame when moving
        if (abs(player.vx) > 20f || abs(player.vy) > 20f) {
            val flameLen = r * (0.9f + 0.25f * sin(elapsedTime * 30f))
            canvas.drawCircle(0f, r * 0.85f, flameLen * 0.32f, hanoiLanternGoldPaint)
        }

        tempPath.reset()
        tempPath.moveTo(0f, -r * 1.2f)
        tempPath.lineTo(r, r * 0.9f)
        tempPath.lineTo(0f, r * 0.45f)
        tempPath.lineTo(-r, r * 0.9f)
        tempPath.close()
        canvas.drawPath(tempPath, playerPaint)
        canvas.drawCircle(0f, r * 0.05f, r * 0.28f, playerAccentPaint)
        canvas.restore()
    }

    private fun drawFreeFlightControls(canvas: Canvas, state: GameState) {
        val cx = padCenterX()
        val cy = padCenterY()
        val pr = padRadius()

        // Outer translucent virtual analog pad
        val padBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 30, 37, 64)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, pr, padBg)
        canvas.drawCircle(cx, cy, pr, controlBorderPaint)

        // Inner thumb knob showing current stick offset
        val knobX = cx + state.joystickX * (pr * 0.65f)
        val knobY = cy + state.joystickY * (pr * 0.65f)
        canvas.drawCircle(knobX, knobY, 44f, playerPaint)
        canvas.drawCircle(knobX, knobY, 44f, controlShieldBorderPaint)

        // Fire button
        drawControlButton(canvas, fireButtonRect(), "FUOCO", style = ControlStyle.ATTACK)
    }

    private fun drawParticles(canvas: Canvas, state: GameState) {
        for (particle in state.particles) {
            val alpha = ((particle.life / particle.maxLife).coerceIn(0f, 1f) * 255).toInt()
            particlePaint.color = particle.color
            particlePaint.alpha = alpha
            canvas.drawCircle(particle.x, particle.y, 7f, particlePaint)
        }
    }

    private fun drawPlayerHud(
        canvas: Canvas,
        player: Player,
        baseY: Float,
        showWeaponStatus: Boolean
    ) {
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

        if (showWeaponStatus) {
            val weaponInfo = if (player.weaponTimer > 0f) {
                "ARMA: ${player.weapon.displayName} (${player.weaponTimer.toInt() + 1}s)"
            } else {
                "ARMA: ${player.weapon.displayName}"
            }
            val boostInfo = buildString {
                if (player.hasDamageBoost) append(" • FURIA x2")
                if (player.hasSpeedBoost) append(" • VELOCITÀ")
            }
            canvas.drawText(weaponInfo + boostInfo, startX, baseY + 46f, phaseBadgePaint)
        }
    }

    private fun menuButtonRect(worldWidth: Float, worldHeight: Float): RectF {
        val centerX = worldWidth / 2f
        val centerY = worldHeight / 2f
        val width = 380f
        val height = 92f
        return RectF(
            centerX - width / 2f,
            centerY + 180f,
            centerX + width / 2f,
            centerY + 180f + height
        )
    }

    private fun drawOverlay(canvas: Canvas, state: GameState) {
        canvas.drawRect(0f, 0f, state.worldWidth, state.worldHeight, overlayScrimPaint)

        val centerX = state.worldWidth / 2f
        val centerY = state.worldHeight / 2f

        val readyHint = when {
            state.level.mode == BattleMode.FREE_FLIGHT_SHOOTER -> "${state.boss.name} • Pad 360° per volare/ruotare + FUOCO"
            state.level.hasShield -> "${state.boss.name} • Spada, scudo e doppio salto"
            state.level.mode == BattleMode.MELEE_PLATFORMER -> "${state.boss.name} • Muovi, salta (x2) e colpisci"
            else -> "${state.boss.name} • Trascina per schivare"
        }

        val (title, subtitle, prompt) = when (state.status) {
            GameStatus.READY -> Triple(
                state.level.title,
                readyHint,
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

        val buttonRect = menuButtonRect(state.worldWidth, state.worldHeight)
        canvas.drawRoundRect(buttonRect, 20f, 20f, menuButtonBgPaint)
        canvas.drawRoundRect(buttonRect, 20f, 20f, menuButtonBorderPaint)
        canvas.drawText(
            "MENU LIVELLI",
            buttonRect.centerX(),
            buttonRect.centerY() + 12f,
            menuButtonTextPaint
        )
    }
}
