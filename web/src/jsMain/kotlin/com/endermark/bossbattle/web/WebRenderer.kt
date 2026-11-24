package com.endermark.bossbattle.web

import com.endermark.bossbattle.model.ArenaTheme
import com.endermark.bossbattle.model.BattleMode
import com.endermark.bossbattle.model.Boss
import com.endermark.bossbattle.model.GameState
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.PickupKind
import com.endermark.bossbattle.model.Player
import com.endermark.bossbattle.model.ProjectileKind
import com.endermark.bossbattle.model.WeaponType
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CENTER
import org.w3c.dom.LEFT
import org.w3c.dom.RIGHT
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class Rect(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    val width: Double get() = right - left
    val height: Double get() = bottom - top
    val centerX: Double get() = (left + right) / 2.0
    val centerY: Double get() = (top + bottom) / 2.0
    fun contains(x: Double, y: Double): Boolean = x in left..right && y in top..bottom
}

class WebRenderer {

    // Shared control geometry in 1000x1600 world coordinates
    val joystickCenterX = 180.0
    val joystickCenterY = 1410.0
    val joystickRadius = 112.0
    val freeFireCenterX = 830.0
    val freeFireCenterY = 1410.0
    val freeFireRadius = 94.0

    fun leftBtn(hasShield: Boolean) = if (hasShield) Rect(28.0, 1345.0, 198.0, 1520.0) else Rect(38.0, 1345.0, 232.0, 1525.0)
    fun rightBtn(hasShield: Boolean) = if (hasShield) Rect(212.0, 1345.0, 382.0, 1520.0) else Rect(252.0, 1345.0, 446.0, 1525.0)
    fun shieldBtn() = Rect(405.0, 1345.0, 595.0, 1520.0)
    fun jumpBtn(hasShield: Boolean) = if (hasShield) Rect(618.0, 1345.0, 788.0, 1520.0) else Rect(554.0, 1345.0, 748.0, 1525.0)
    fun attackBtn(hasShield: Boolean) = if (hasShield) Rect(802.0, 1345.0, 972.0, 1520.0) else Rect(768.0, 1345.0, 962.0, 1525.0)

    fun render(ctx: CanvasRenderingContext2D, state: GameState, canvasW: Double, canvasH: Double) {
        ctx.fillStyle = "#070910"
        ctx.fillRect(0.0, 0.0, canvasW, canvasH)

        val scale = min(canvasW / state.worldWidth, canvasH / state.worldHeight)
        val offsetX = (canvasW - state.worldWidth * scale) / 2.0
        val offsetY = (canvasH - state.worldHeight * scale) / 2.0

        ctx.save()
        ctx.translate(offsetX, offsetY)
        ctx.scale(scale, scale)

        // Clip to world bounds
        ctx.beginPath()
        ctx.rect(0.0, 0.0, state.worldWidth.toDouble(), state.worldHeight.toDouble())
        ctx.clip()

        drawBackground(ctx, state)
        val isMelee = state.level.mode == BattleMode.MELEE_PLATFORMER
        val isFreeFlight = state.level.mode == BattleMode.FREE_FLIGHT_SHOOTER

        if (isMelee) {
            drawGround(ctx, state)
        }
        drawPlatforms(ctx, state)
        drawLasers(ctx, state)
        drawPickups(ctx, state)
        drawProjectiles(ctx, state)
        drawMinions(ctx, state)
        drawTurrets(ctx, state)
        drawParticles(ctx, state)

        // Bosses
        if (state.level.enableBoardOfDirectors) {
            drawBoardOfDirectors(ctx, state)
        } else if (state.boss.hp > 0 && !state.boss.isVanished) {
            drawSingleBoss(ctx, state)
        }

        // Player
        when {
            isMelee -> drawPlatformerPlayer(ctx, state.player, state.elapsedTime, state.level.hasShield, state.level.enablePistol)
            isFreeFlight -> drawRotatedPlayerShip(ctx, state.player, state.elapsedTime)
            else -> drawClassicPlayerShip(ctx, state.player, state.elapsedTime)
        }

        // HUD
        val sec = state.secondaryBoss
        if (state.level.enableBoardOfDirectors && sec != null) {
            drawDualBossBars(ctx, state.boss, sec, state.worldWidth.toDouble(), state.level.multiPhase)
        } else if (state.level.enableTurretFortress && state.hasActiveTurrets) {
            drawFortressSealedBar(ctx, state)
        } else {
            drawBossBar(ctx, state)
        }

        drawPlayerHearts(ctx, state.player, if (isMelee || isFreeFlight) 182.0 else state.worldHeight - 70.0)

        if (isMelee) {
            drawPlatformerControls(ctx, state.level.hasShield, state.level.enablePistol)
        } else if (isFreeFlight) {
            drawFreeFlightControls(ctx, state)
        }

        if (state.phaseFlashTimer > 0f) {
            val alpha = ((state.phaseFlashTimer / 0.6f).coerceIn(0f, 1f) * 0.55).toDouble()
            ctx.fillStyle = "rgba(255,255,255,$alpha)"
            ctx.fillRect(0.0, 0.0, state.worldWidth.toDouble(), state.worldHeight.toDouble())
        }

        if (state.status != GameStatus.PLAYING) {
            drawOverlay(ctx, state)
        }

        ctx.restore()
    }

    private fun drawBackground(ctx: CanvasRenderingContext2D, state: GameState) {
        val w = state.worldWidth.toDouble()
        val h = state.worldHeight.toDouble()
        val grad = ctx.createLinearGradient(0.0, 0.0, 0.0, h)
        when (state.level.arenaTheme) {
            ArenaTheme.DEEP_SPACE -> { grad.addColorStop(0.0, "#050714"); grad.addColorStop(1.0, "#101633") }
            ArenaTheme.ZURICH -> { grad.addColorStop(0.0, "#10203a"); grad.addColorStop(1.0, "#1d3557") }
            ArenaTheme.HANOI, ArenaTheme.HANOI_SPA -> { grad.addColorStop(0.0, "#2b1035"); grad.addColorStop(1.0, "#5c2544") }
            ArenaTheme.PARIS -> { grad.addColorStop(0.0, "#2a1230"); grad.addColorStop(1.0, "#4a1942") }
            ArenaTheme.GOOGLE_ZURICH -> { grad.addColorStop(0.0, "#0f2027"); grad.addColorStop(1.0, "#203a43") }
            ArenaTheme.OMEGA_FINALE -> when (state.boss.phase) {
                1 -> { grad.addColorStop(0.0, "#0a1026"); grad.addColorStop(1.0, "#1c1f4a") }
                2 -> { grad.addColorStop(0.0, "#2d0b0b"); grad.addColorStop(1.0, "#5e1c10") }
                else -> { grad.addColorStop(0.0, "#140529"); grad.addColorStop(1.0, "#310d57") }
            }
            else -> { grad.addColorStop(0.0, "#0d0f1a"); grad.addColorStop(1.0, "#171c33") }
        }
        ctx.fillStyle = grad
        ctx.fillRect(0.0, 0.0, w, h)

        // Subtle animated stars / grid
        ctx.fillStyle = "rgba(255,255,255,0.22)"
        for (i in 0 until 42) {
            val sx = ((i * 197 + 43) % 980 + 10).toDouble()
            val sy = (((i * 271 + (state.elapsedTime * 30).toInt()) % 1560) + 20).toDouble()
            val sr = if (i % 4 == 0) 2.6 else 1.5
            ctx.beginPath(); ctx.arc(sx, sy, sr, 0.0, PI * 2); ctx.fill()
        }
    }

    private fun drawGround(ctx: CanvasRenderingContext2D, state: GameState) {
        val gy = state.groundY.toDouble()
        ctx.fillStyle = "#161d33"
        ctx.fillRect(0.0, gy, state.worldWidth.toDouble(), state.worldHeight - gy)
        ctx.strokeStyle = "#38bdf8"
        ctx.lineWidth = 4.0
        ctx.beginPath(); ctx.moveTo(0.0, gy); ctx.lineTo(state.worldWidth.toDouble(), gy); ctx.stroke()
    }

    private fun drawPlatforms(ctx: CanvasRenderingContext2D, state: GameState) {
        for (p in state.platforms) {
            roundRect(ctx, (p.x - p.width / 2).toDouble(), (p.y - p.height / 2).toDouble(), p.width.toDouble(), p.height.toDouble(), 10.0, "#26324d", "#38bdf8", 3.0)
        }
    }

    private fun drawLasers(ctx: CanvasRenderingContext2D, state: GameState) {
        for (l in state.lasers) {
            if (!l.isActive) {
                ctx.save()
                ctx.strokeStyle = "rgba(255, 82, 82, 0.75)"
                ctx.lineWidth = 5.0
                ctx.setLineDash(arrayOf(16.0, 12.0))
                ctx.beginPath()
                if (l.isHorizontal) { ctx.moveTo(0.0, l.position.toDouble()); ctx.lineTo(state.worldWidth.toDouble(), l.position.toDouble()) }
                else { ctx.moveTo(l.position.toDouble(), 0.0); ctx.lineTo(l.position.toDouble(), state.worldHeight.toDouble()) }
                ctx.stroke()
                ctx.restore()
            } else {
                ctx.fillStyle = "rgba(255, 23, 68, 0.85)"
                if (l.isHorizontal) {
                    ctx.fillRect(0.0, (l.position - l.thickness / 2).toDouble(), state.worldWidth.toDouble(), l.thickness.toDouble())
                    ctx.fillStyle = "#ffffff"
                    ctx.fillRect(0.0, (l.position - l.thickness * 0.2).toDouble(), state.worldWidth.toDouble(), (l.thickness * 0.4).toDouble())
                } else {
                    ctx.fillRect((l.position - l.thickness / 2).toDouble(), 0.0, l.thickness.toDouble(), state.worldHeight.toDouble())
                    ctx.fillStyle = "#ffffff"
                    ctx.fillRect((l.position - l.thickness * 0.2).toDouble(), 0.0, (l.thickness * 0.4).toDouble(), state.worldHeight.toDouble())
                }
            }
        }
    }

    private fun drawPickups(ctx: CanvasRenderingContext2D, state: GameState) {
        for (pk in state.pickups) {
            val col = when (pk.kind) {
                PickupKind.HEAL -> "#4ade80"
                PickupKind.SPEED_BOOST -> "#38bdf8"
                PickupKind.POWER_SURGE -> "#f97316"
                else -> "#facc15"
            }
            circle(ctx, pk.x.toDouble(), pk.y.toDouble(), pk.radius.toDouble(), col, "#ffffff", 3.0)
            drawText(ctx, pk.kind.label, pk.x.toDouble(), pk.y.toDouble() + 5.0, "#0b0e18", "bold 14px system-ui", CanvasTextAlign.CENTER)
        }
    }

    private fun drawProjectiles(ctx: CanvasRenderingContext2D, state: GameState) {
        for (p in state.projectiles) {
            val x = p.x.toDouble(); val y = p.y.toDouble(); val r = p.radius.toDouble()
            when (p.kind) {
                ProjectileKind.PLAYER_BOLT -> {
                    circle(ctx, x, y, r, "#38bdf8", "#ffffff", 2.0)
                }
                ProjectileKind.COMPUTER -> {
                    roundRect(ctx, x - r, y - r * 0.8, r * 2, r * 1.1, 5.0, "#37474f", "#cbd5e1", 2.0)
                    ctx.fillStyle = "#38bdf8"; ctx.fillRect(x - r * 0.8, y - r * 0.6, r * 1.6, r * 0.7)
                    roundRect(ctx, x - r * 1.15, y + r * 0.3, r * 2.3, r * 0.35, 3.0, "#94a3b8", "#334155", 2.0)
                }
                ProjectileKind.TV -> {
                    circle(ctx, x, y, r * 1.18, "transparent", if (p.fromPlayer) "#00e5ff" else "#facc15", 4.0)
                    roundRect(ctx, x - r * 0.95, y - r * 0.72, r * 1.9, r * 1.44, 8.0, "#1e293b", "#f8fafc", 2.5)
                    roundRect(ctx, x - r * 0.72, y - r * 0.5, r * 1.2, r * 1.0, 5.0, if (p.fromPlayer) "#00e5ff" else "#93c5fd", null, 0.0)
                }
                ProjectileKind.SOFA -> {
                    circle(ctx, x, y, r * 1.15, "transparent", if (p.fromPlayer) "#00e5ff" else "#facc15", 4.0)
                    val col = if (p.fromPlayer) "#06b6d4" else "#dc2626"
                    roundRect(ctx, x - r * 0.95, y - r * 0.62, r * 1.9, r * 0.8, 10.0, col, "#ffffff", 2.0)
                    roundRect(ctx, x - r * 0.85, y - r * 0.05, r * 1.7, r * 0.58, 8.0, if (p.fromPlayer) "#22d3ee" else "#ef4444", "#ffffff", 2.0)
                }
                ProjectileKind.FART_CLOUD -> {
                    circle(ctx, x, y, r * 0.9, "rgba(118,255,3,0.82)", null, 0.0)
                    circle(ctx, x - r * 0.48, y + r * 0.15, r * 0.6, "rgba(118,255,3,0.78)", null, 0.0)
                    circle(ctx, x + r * 0.48, y + r * 0.15, r * 0.6, "rgba(118,255,3,0.78)", null, 0.0)
                    p.label?.let { drawText(ctx, it, x, y + 6.0, "#0b0e18", "bold 16px system-ui", CanvasTextAlign.CENTER) }
                }
                ProjectileKind.COMPLAINT, ProjectileKind.DATING_FLEX -> {
                    val txt = p.label ?: "!"
                    val bw = max(120.0, txt.length * 10.5 + 24.0)
                    val bg = if (p.kind == ProjectileKind.DATING_FLEX) "#facc15" else "#ffffff"
                    roundRect(ctx, x - bw / 2, y - 22.0, bw, 44.0, 14.0, bg, "#0f172a", 3.0)
                    drawText(ctx, txt, x, y + 6.0, "#0f172a", "bold 16px system-ui", CanvasTextAlign.CENTER)
                }
                ProjectileKind.HEART -> {
                    circle(ctx, x - r * 0.35, y - r * 0.15, r * 0.55, "#ec4899", null, 0.0)
                    circle(ctx, x + r * 0.35, y - r * 0.15, r * 0.55, "#ec4899", null, 0.0)
                    ctx.fillStyle = "#ec4899"
                    ctx.beginPath(); ctx.moveTo(x - r * 0.85, y); ctx.lineTo(x + r * 0.85, y); ctx.lineTo(x, y + r * 0.95); ctx.closePath(); ctx.fill()
                }
                ProjectileKind.DRONE -> {
                    roundRect(ctx, x - r, y - r * 0.45, r * 2, r * 0.9, 6.0, "#475569", "#38bdf8", 2.5)
                    circle(ctx, x, y, r * 0.3, "#ef4444", null, 0.0)
                }
                ProjectileKind.CAMERA_FLASH -> {
                    circle(ctx, x, y, r * 1.1, "rgba(250,204,21,0.45)", "#ffffff", 3.0)
                    circle(ctx, x, y, r * 0.55, "#ffffff", null, 0.0)
                }
                ProjectileKind.SHOCKWAVE -> {
                    roundRect(ctx, x - r * 1.4, y - r * 0.65, r * 2.8, r * 1.3, 12.0, "#f97316", "#fef08a", 3.0)
                }
                ProjectileKind.HOT_STONE -> {
                    circle(ctx, x, y, r, "#475569", "#f97316", 4.0)
                }
                else -> {
                    circle(ctx, x, y, r, "#ef4444", "#fde047", 3.0)
                }
            }
        }
    }

    private fun drawMinions(ctx: CanvasRenderingContext2D, state: GameState) {
        for (m in state.minions) {
            val x = m.x.toDouble(); val y = m.y.toDouble(); val r = m.radius.toDouble()
            if (state.level.enableTechLeadLollo) {
                circle(ctx, x, y, r * 1.12, "transparent", "#f97316", 3.0)
                circle(ctx, x, y + 6, r * 0.82, if (m.hitFlashTimer > 0f) "#fff" else "#1e293b", "#94a3b8", 2.0)
                circle(ctx, x, y - 6, r * 0.5, if (m.hitFlashTimer > 0f) "#fff" else "#ffcc99", null, 0.0)
                ctx.fillStyle = "#22c55e"; ctx.fillRect(x - 10, y + 8, 20.0, 10.0)
            } else {
                ctx.save()
                ctx.translate(x, y)
                ctx.rotate(m.angle.toDouble() + PI / 2)
                ctx.fillStyle = if (m.hitFlashTimer > 0f) "#ffffff" else "#fb923c"
                ctx.beginPath()
                ctx.moveTo(0.0, -r * 1.15); ctx.lineTo(r * 0.9, r * 0.85); ctx.lineTo(0.0, r * 0.45); ctx.lineTo(-r * 0.9, r * 0.85)
                ctx.closePath(); ctx.fill()
                ctx.restore()
            }
        }
    }

    private fun drawTurrets(ctx: CanvasRenderingContext2D, state: GameState) {
        for (t in state.turrets) {
            val x = t.x.toDouble(); val y = t.y.toDouble(); val r = t.radius.toDouble()
            if (t.isDestroyed) {
                circle(ctx, x, y, r * 0.7, "#1e293b", "#475569", 2.0)
                continue
            }
            circle(ctx, x, y, r, if (t.hitFlashTimer > 0f) "#fff" else "#475569", "#f8fafc", 3.0)
            ctx.save()
            ctx.translate(x, y); ctx.rotate(t.angle.toDouble())
            roundRect(ctx, 0.0, -9.0, r * 1.25, 18.0, 4.0, "#ef4444", "#ffffff", 2.0)
            ctx.restore()
            circle(ctx, x, y, r * 0.4, "#facc15", null, 0.0)
            // Mini HP bar
            roundRect(ctx, x - 28, y - r - 15, 56.0, 8.0, 4.0, "#1e293b", null, 0.0)
            roundRect(ctx, x - 28, y - r - 15, 56.0 * t.hpRatio, 8.0, 4.0, "#facc15", null, 0.0)
        }
    }

    private fun drawParticles(ctx: CanvasRenderingContext2D, state: GameState) {
        for (pt in state.particles) {
            val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f).toDouble()
            val r = (pt.color shr 16) and 0xFF
            val g = (pt.color shr 8) and 0xFF
            val b = pt.color and 0xFF
            ctx.fillStyle = "rgba($r,$g,$b,$alpha)"
            ctx.beginPath(); ctx.arc(pt.x.toDouble(), pt.y.toDouble(), 6.0 * alpha + 2.0, 0.0, PI * 2); ctx.fill()
        }
    }

    private fun drawBoardOfDirectors(ctx: CanvasRenderingContext2D, state: GameState) {
        val lollo = state.boss
        val ivanone = state.secondaryBoss
        if (lollo.hp > 0 && ivanone != null && ivanone.hp > 0 && !ivanone.isVanished) {
            ctx.strokeStyle = "rgba(250,204,21,0.55)"
            ctx.lineWidth = 8.0
            ctx.beginPath(); ctx.moveTo(lollo.x.toDouble(), lollo.y.toDouble()); ctx.lineTo(ivanone.x.toDouble(), ivanone.y.toDouble()); ctx.stroke()
        }
        if (lollo.hp > 0 && !lollo.isVanished) {
            if (ivanone == null || ivanone.hp <= 0) {
                circle(ctx, lollo.x.toDouble(), lollo.y.toDouble(), lollo.radius * 1.25, "transparent", "#facc15", 6.0)
            }
            drawLolloHead(ctx, lollo, withTie = true, furious = false, ingolfato = false, elapsed = state.elapsedTime)
        }
        if (ivanone != null && ivanone.hp > 0 && !ivanone.isVanished) {
            if (lollo.hp <= 0) {
                circle(ctx, ivanone.x.toDouble(), ivanone.y.toDouble(), ivanone.radius * 1.25, "transparent", "#facc15", 6.0)
            }
            drawIvanoneBody(ctx, ivanone)
        }
    }

    private fun drawSingleBoss(ctx: CanvasRenderingContext2D, state: GameState) {
        val b = state.boss
        val x = b.x.toDouble(); val y = b.y.toDouble(); val r = b.radius.toDouble()
        val isHit = b.hitFlashTimer > 0f
        when {
            state.level.enableTechLeadLollo -> {
                if (b.phase >= 3) circle(ctx, x, y, r * 1.22, "transparent", "#76ff03", 9.0)
                drawLolloHead(ctx, b, withTie = true, furious = false, ingolfato = false, elapsed = state.elapsedTime)
            }
            state.level.enableGiantLollo -> {
                if (!state.isLolloIngolfato) {
                    val col = if (state.level.enableFuriousLollo) "#ff3d00" else "#00e5ff"
                    circle(ctx, x, y, r * 1.15, "transparent", col, 9.0)
                }
                drawLolloHead(ctx, b, withTie = false, furious = state.level.enableFuriousLollo, ingolfato = state.isLolloIngolfato, elapsed = state.elapsedTime)
            }
            state.level.enableTurretFortress -> {
                roundRect(ctx, x - r * 1.5, y - r * 0.75, r * 3.0, r * 1.1, 16.0, "#1e293b", "#94a3b8", 3.0)
                circle(ctx, x, y, r, if (isHit) "#fff" else if (state.hasActiveTurrets) "#334155" else "#6b21a8", "#f8fafc", 4.0)
                if (state.hasActiveTurrets) {
                    circle(ctx, x, y, r * 1.2, "transparent", "#00e5ff", 8.0)
                } else {
                    val a = atan2((state.player.y - b.y).toDouble(), (state.player.x - b.x).toDouble())
                    ctx.save(); ctx.translate(x, y); ctx.rotate(a)
                    roundRect(ctx, 10.0, -30.0, r * 1.2, 18.0, 5.0, "#ef4444", "#fff", 2.0)
                    roundRect(ctx, 10.0, 12.0, r * 1.2, 18.0, 5.0, "#ef4444", "#fff", 2.0)
                    ctx.restore()
                }
                circle(ctx, x, y, r * 0.34, if (state.hasActiveTurrets) "#00e5ff" else "#ef4444", "#fff", 2.0)
            }
            state.level.arenaTheme == ArenaTheme.DEEP_SPACE -> {
                if (state.hasActiveWeakPoints) circle(ctx, x, y, r * 1.24, "transparent", "#00e5ff", 9.0)
                ctx.fillStyle = if (isHit) "#ffffff" else if (b.isEnraged) "#880e4f" else "#37474f"
                ctx.strokeStyle = "#f8fafc"; ctx.lineWidth = 4.0
                ctx.beginPath()
                ctx.moveTo(x, y + r); ctx.lineTo(x + r * 0.95, y + r * 0.25); ctx.lineTo(x + r * 0.78, y - r * 0.75)
                ctx.lineTo(x - r * 0.78, y - r * 0.75); ctx.lineTo(x - r * 0.95, y + r * 0.25)
                ctx.closePath(); ctx.fill(); ctx.stroke()
                circle(ctx, x, y + r * 0.08, r * 0.28, if (state.hasActiveWeakPoints) "#00e5ff" else "#ef4444", "#fff", 2.0)
                for (wp in state.weakPoints) {
                    val wx = x + wp.offsetX; val wy = y + wp.offsetY
                    if (wp.isDestroyed) {
                        circle(ctx, wx, wy, wp.radius * 0.6, "#1e293b", null, 0.0)
                    } else {
                        circle(ctx, wx, wy, wp.radius.toDouble(), if (wp.hitFlashTimer > 0f) "#fff" else "#ff1744", "#facc15", 4.0)
                        roundRect(ctx, wx - 26, wy - wp.radius - 14, 52.0, 7.0, 3.0, "#1e293b", null, 0.0)
                        roundRect(ctx, wx - 26, wy - wp.radius - 14, 52.0 * wp.hpRatio, 7.0, 3.0, "#facc15", null, 0.0)
                    }
                }
            }
            state.level.arenaTheme == ArenaTheme.ZURICH -> drawLolloHead(ctx, b, withTie = false, furious = false, ingolfato = false, elapsed = state.elapsedTime)
            state.level.arenaTheme == ArenaTheme.STUDIO -> drawIvanoneBody(ctx, b)
            state.level.arenaTheme == ArenaTheme.GOOGLE_ZURICH -> {
                // White Porsche!
                roundRect(ctx, x - r * 1.15, y - r * 0.3, r * 2.3, r * 0.85, 18.0, if (isHit) "#fef08a" else "#f8fafc", "#334155", 3.0)
                circle(ctx, x - r * 0.65, y + r * 0.55, 20.0, "#0f172a", "#94a3b8", 4.0)
                circle(ctx, x + r * 0.65, y + r * 0.55, 20.0, "#0f172a", "#94a3b8", 4.0)
                circle(ctx, x, y - r * 0.45, 24.0, "#ffcc99", "#0f172a", 2.5)
            }
            else -> {
                val fill = when {
                    isHit -> "#ffffff"
                    state.level.arenaTheme == ArenaTheme.PARIS -> "#ec4899"
                    state.level.arenaTheme == ArenaTheme.HANOI || state.level.arenaTheme == ArenaTheme.HANOI_SPA -> "#f59e0b"
                    state.level.arenaTheme == ArenaTheme.OMEGA_FINALE -> "#a855f7"
                    else -> "#ef4444"
                }
                circle(ctx, x, y, r, fill, "#ffffff", 4.0)
                circle(ctx, x - r * 0.3, y - r * 0.15, r * 0.14, "#ffffff", null, 0.0)
                circle(ctx, x + r * 0.3, y - r * 0.15, r * 0.14, "#ffffff", null, 0.0)
                circle(ctx, x - r * 0.3, y - r * 0.15, r * 0.06, "#0f172a", null, 0.0)
                circle(ctx, x + r * 0.3, y - r * 0.15, r * 0.06, "#0f172a", null, 0.0)
            }
        }
    }

    private fun drawLolloHead(ctx: CanvasRenderingContext2D, b: Boss, withTie: Boolean, furious: Boolean, ingolfato: Boolean, elapsed: Float) {
        val x = b.x.toDouble(); val y = b.y.toDouble(); val r = b.radius.toDouble()
        val isHit = b.hitFlashTimer > 0f
        if (furious && !ingolfato) {
            for (i in -2..2) {
                val fx = x + i * r * 0.28
                val fl = sin(elapsed * 16f + i * 1.3f) * 16.0
                ctx.fillStyle = if (i % 2 == 0) "#ff3d00" else "#ffab00"
                ctx.beginPath()
                ctx.moveTo(fx - 22, y - r * 0.8); ctx.lineTo(fx, y - r * 1.35 + fl); ctx.lineTo(fx + 22, y - r * 0.8)
                ctx.closePath(); ctx.fill()
            }
        }
        if (withTie) {
            roundRect(ctx, x - r * 0.72, y + r * 0.28, r * 1.44, r * 0.82, 16.0, if (isHit) "#fff" else "#eceff1", "#1e293b", 3.0)
            ctx.fillStyle = "#d50000"
            ctx.beginPath()
            ctx.moveTo(x - 12, y + r * 0.38); ctx.lineTo(x + 12, y + r * 0.38); ctx.lineTo(x + 18, y + r * 0.92)
            ctx.lineTo(x, y + r * 1.12); ctx.lineTo(x - 18, y + r * 0.92)
            ctx.closePath(); ctx.fill()
        }
        val skin = when {
            isHit -> "#ffffff"
            ingolfato -> "#ffab91"
            furious -> "#ff8a65"
            else -> "#ffcc99"
        }
        circle(ctx, x, y - if (withTie) r * 0.1 else 0.0, r * if (withTie) 0.66 else 0.92, skin, "#1e293b", 4.0)
        // Bald scalp shine
        ctx.fillStyle = "rgba(255,255,255,0.65)"
        ctx.beginPath(); ctx.ellipse(x - r * 0.2, y - r * 0.48, r * 0.22, r * 0.12, -0.3, 0.0, PI * 2); ctx.fill()
        val eyeY = y - r * 0.12
        if (ingolfato) {
            ctx.strokeStyle = "#0f172a"; ctx.lineWidth = 5.0
            for (ex in listOf(x - r * 0.32, x + r * 0.32)) {
                ctx.beginPath(); ctx.moveTo(ex - 14, eyeY - 14); ctx.lineTo(ex + 14, eyeY + 14); ctx.stroke()
                ctx.beginPath(); ctx.moveTo(ex + 14, eyeY - 14); ctx.lineTo(ex - 14, eyeY + 14); ctx.stroke()
            }
            for (i in 0 until 4) {
                val a = elapsed * 5.0 + i * 1.57
                circle(ctx, x + cos(a) * r * 0.65, y - r * 1.05 + sin(a) * 14, 9.0, "#facc15", null, 0.0)
            }
        } else {
            val eyeCol = if (furious) "#ff1744" else "#ffffff"
            circle(ctx, x - r * 0.3, eyeY, r * 0.15, eyeCol, "#0f172a", 2.0)
            circle(ctx, x + r * 0.3, eyeY, r * 0.15, eyeCol, "#0f172a", 2.0)
            circle(ctx, x - r * 0.3, eyeY, r * 0.06, if (furious) "#facc15" else "#0f172a", null, 0.0)
            circle(ctx, x + r * 0.3, eyeY, r * 0.06, if (furious) "#facc15" else "#0f172a", null, 0.0)
        }
        // Mouth
        ctx.fillStyle = "#b71c1c"
        ctx.beginPath(); ctx.ellipse(x, y + r * 0.32, r * 0.28, r * 0.14, 0.0, 0.0, PI * 2); ctx.fill()
    }

    private fun drawIvanoneBody(ctx: CanvasRenderingContext2D, b: Boss) {
        val x = b.x.toDouble(); val y = b.y.toDouble(); val r = b.radius.toDouble()
        val isHit = b.hitFlashTimer > 0f
        // Muscular shoulders + torso
        roundRect(ctx, x - r * 0.95, y - r * 0.25, r * 1.9, r * 1.05, 24.0, if (isHit) "#fff" else "#6d28d9", "#f8fafc", 3.0)
        circle(ctx, x - r * 0.85, y - r * 0.05, r * 0.32, if (isHit) "#fff" else "#ffcc99", "#1e293b", 2.5)
        circle(ctx, x + r * 0.85, y - r * 0.05, r * 0.32, if (isHit) "#fff" else "#ffcc99", "#1e293b", 2.5)
        // Head + hair
        circle(ctx, x, y - r * 0.55, r * 0.42, if (isHit) "#fff" else "#ffcc99", "#1e293b", 3.0)
        circle(ctx, x, y - r * 0.82, r * 0.32, "#1e293b", null, 0.0)
        // Camera on chest
        roundRect(ctx, x - 22, y + 6, 44.0, 30.0, 6.0, "#1e293b", "#facc15", 2.5)
        circle(ctx, x, y + 21, 9.0, "#38bdf8", null, 0.0)
    }

    private fun drawClassicPlayerShip(ctx: CanvasRenderingContext2D, p: Player, elapsed: Float) {
        if (p.isInvulnerable && ((elapsed * 16).toInt() % 2 == 0)) return
        val x = p.x.toDouble(); val y = p.y.toDouble(); val r = p.radius.toDouble()
        ctx.fillStyle = "#38bdf8"; ctx.strokeStyle = "#ffffff"; ctx.lineWidth = 3.0
        ctx.beginPath()
        ctx.moveTo(x, y - r); ctx.lineTo(x + r * 0.9, y + r * 0.85); ctx.lineTo(x, y + r * 0.45); ctx.lineTo(x - r * 0.9, y + r * 0.85)
        ctx.closePath(); ctx.fill(); ctx.stroke()
    }

    private fun drawRotatedPlayerShip(ctx: CanvasRenderingContext2D, p: Player, elapsed: Float) {
        if (p.isInvulnerable && ((elapsed * 16).toInt() % 2 == 0)) return
        val r = p.radius.toDouble()
        ctx.save()
        ctx.translate(p.x.toDouble(), p.y.toDouble())
        ctx.rotate(p.angle.toDouble() + PI / 2)
        if (abs(p.vx) > 20f || abs(p.vy) > 20f) {
            circle(ctx, 0.0, r * 0.85, r * 0.3, "#facc15", null, 0.0)
        }
        ctx.fillStyle = "#38bdf8"; ctx.strokeStyle = "#ffffff"; ctx.lineWidth = 3.0
        ctx.beginPath()
        ctx.moveTo(0.0, -r * 1.1); ctx.lineTo(r * 0.9, r * 0.85); ctx.lineTo(0.0, r * 0.45); ctx.lineTo(-r * 0.9, r * 0.85)
        ctx.closePath(); ctx.fill(); ctx.stroke()
        ctx.restore()
    }

    private fun drawPlatformerPlayer(ctx: CanvasRenderingContext2D, p: Player, elapsed: Float, hasShield: Boolean, enablePistol: Boolean) {
        if (p.isInvulnerable && ((elapsed * 16).toInt() % 2 == 0)) return
        val x = p.x.toDouble(); val y = p.y.toDouble(); val r = p.radius.toDouble()
        val dir = p.facingDir.toDouble()
        roundRect(ctx, x - 16, y - r * 0.32, 32.0, r * 0.7, 10.0, "#38bdf8", "#ffffff", 2.5)
        circle(ctx, x, y - r * 0.68, 17.0, "#fef08a", "#0f172a", 2.0)
        if (enablePistol) {
            val gx = x + dir * 20
            val tip = gx + dir * 34
            roundRect(ctx, min(gx, tip), y - 16, 34.0, 12.0, 4.0, "#334155", "#f8fafc", 2.0)
            if (p.isAttacking) circle(ctx, tip + dir * 8, y - 10, 11.0, "#facc15", "#ffffff", 2.0)
        } else {
            val tipX = x + dir * if (p.isAttacking) 82.0 else 48.0
            ctx.strokeStyle = if (p.weapon == WeaponType.HAMMER) "#facc15" else "#e2e8f0"
            ctx.lineWidth = 7.0
            ctx.beginPath(); ctx.moveTo(x + dir * 12, y - 4); ctx.lineTo(tipX, y - 10); ctx.stroke()
        }
        if (hasShield && p.isBlocking) {
            circle(ctx, x, y, r * 1.4, "rgba(56,189,248,0.22)", "#38bdf8", 5.0)
        }
    }

    private fun drawBossBar(ctx: CanvasRenderingContext2D, state: GameState) {
        val b = state.boss
        val mx = 52.0; val ty = 92.0; val bw = state.worldWidth - mx * 2; val bh = 34.0
        drawText(ctx, b.name, mx, ty - 14, "#f8fafc", "bold 24px system-ui", CanvasTextAlign.LEFT)
        drawText(ctx, "${b.hp} / ${b.maxHp} HP", state.worldWidth - mx, ty - 14, "#cbd5e1", "bold 20px system-ui", CanvasTextAlign.RIGHT)
        roundRect(ctx, mx, ty, bw, bh, 12.0, "#1e293b", "#94a3b8", 2.5)
        if (b.hpRatio > 0f) roundRect(ctx, mx, ty, bw * b.hpRatio, bh, 12.0, if (b.isEnraged) "#f97316" else "#ef4444", null, 0.0)
        if (b.maxArmor > 0 && b.armor > 0) {
            roundRect(ctx, mx, ty + bh + 8, bw, 14.0, 7.0, "#1e293b", "#38bdf8", 2.0)
            roundRect(ctx, mx, ty + bh + 8, bw * b.armorRatio, 14.0, 7.0, "#38bdf8", null, 0.0)
        }
        val status = when {
            state.level.enableGiantLollo && state.isLolloIngolfato -> "INGOLFATO! (${(state.lolloIngolfatoTimer * 10).toInt() / 10.0}s) • COLPISCILO!"
            state.level.enableGiantLollo -> "${if (state.level.enableFuriousLollo) "INFURIATO" else "INVINCIBILE"} • TV: ${if (state.lolloTvHit) "OK" else "MANCA"} • DIVANO: ${if (state.lolloSofaHit) "OK" else "MANCA"}"
            state.weakPoints.isNotEmpty() -> if (state.hasActiveWeakPoints) "INVINCIBILE • PUNTI DEBOLI: ${state.weakPoints.count { !it.isDestroyed }}/${state.weakPoints.size}" else "VULNERABILE! COLPISCI LA CORAZZATA!"
            state.level.enableTechLeadLollo -> "FASE ${b.phase}/3${if (b.phase >= 3) " • -50% DANNI • SCORREGGE!" else ""}"
            else -> null
        }
        if (status != null) drawText(ctx, status, state.worldWidth - mx, ty + bh + 34, "#facc15", "bold 19px system-ui", CanvasTextAlign.RIGHT)
    }

    private fun drawFortressSealedBar(ctx: CanvasRenderingContext2D, state: GameState) {
        val mx = 52.0; val ty = 92.0; val bw = state.worldWidth - mx * 2; val bh = 34.0
        val rem = state.turrets.count { !it.isDestroyed }; val tot = state.turrets.size.coerceAtLeast(1)
        drawText(ctx, "TORRETTE FORTEZZA", mx, ty - 14, "#f8fafc", "bold 24px system-ui", CanvasTextAlign.LEFT)
        drawText(ctx, "$rem / $tot ATTIVE", state.worldWidth - mx, ty - 14, "#facc15", "bold 20px system-ui", CanvasTextAlign.RIGHT)
        roundRect(ctx, mx, ty, bw, bh, 12.0, "#1e293b", "#94a3b8", 2.5)
        roundRect(ctx, mx, ty, bw * (rem.toDouble() / tot), bh, 12.0, "#facc15", null, 0.0)
        drawText(ctx, "SUPER-TORRETTA SIGILLATA • DISTRUGGI TUTTE LE TORRETTE", state.worldWidth - mx, ty + bh + 30, "#facc15", "bold 18px system-ui", CanvasTextAlign.RIGHT)
    }

    private fun drawDualBossBars(ctx: CanvasRenderingContext2D, left: Boss, right: Boss, worldW: Double, multiPhase: Boolean) {
        val mx = 44.0; val gap = 28.0; val bw = (worldW - mx * 2 - gap) / 2; val ty = 92.0; val bh = 30.0
        fun one(b: Boss, lx: Double, col: String) {
            drawText(ctx, if (b.hp <= 0) "${b.name} (KO)" else b.name, lx, ty - 12, "#f8fafc", "bold 20px system-ui", CanvasTextAlign.LEFT)
            drawText(ctx, "${b.hp}/${b.maxHp}", lx + bw, ty - 12, "#cbd5e1", "bold 18px system-ui", CanvasTextAlign.RIGHT)
            roundRect(ctx, lx, ty, bw, bh, 10.0, "#1e293b", "#94a3b8", 2.0)
            if (b.hpRatio > 0f) roundRect(ctx, lx, ty, bw * b.hpRatio, bh, 10.0, col, null, 0.0)
        }
        one(left, mx, "#ef4444")
        one(right, mx + bw + gap, "#a855f7")
        val phase = max(left.phase, right.phase)
        val txt = if (multiPhase) "FASE $phase/3 • BOARD OF DIRECTORS" else if (left.hp > 0 && right.hp > 0) "BOARD OF DIRECTORS • SINERGIA ATTIVA" else "SUPER-CEO ATTIVO!"
        drawText(ctx, txt, worldW - mx, ty + bh + 28, "#facc15", "bold 18px system-ui", CanvasTextAlign.RIGHT)
    }

    private fun drawPlayerHearts(ctx: CanvasRenderingContext2D, p: Player, y: Double) {
        for (i in 0 until p.maxHp) {
            val col = if (i < p.hp) "#ef4444" else "#334155"
            circle(ctx, 64.0 + i * 38.0, y, 13.0, col, "#ffffff", 2.0)
        }
    }

    private fun drawPlatformerControls(ctx: CanvasRenderingContext2D, hasShield: Boolean, enablePistol: Boolean) {
        fun btn(r: Rect, label: String, border: String) {
            roundRect(ctx, r.left, r.top, r.width, r.height, 22.0, "rgba(15,23,42,0.72)", border, 4.0)
            drawText(ctx, label, r.centerX, r.centerY + 12, "#f8fafc", "bold 30px system-ui", CanvasTextAlign.CENTER)
        }
        btn(leftBtn(hasShield), "◀", "#38bdf8")
        btn(rightBtn(hasShield), "▶", "#38bdf8")
        if (hasShield) btn(shieldBtn(), "SCUDO", "#60a5fa")
        btn(jumpBtn(hasShield), "SALTA", "#38bdf8")
        btn(attackBtn(hasShield), if (enablePistol) "SPARA" else "COLPO", "#f97316")
    }

    private fun drawFreeFlightControls(ctx: CanvasRenderingContext2D, state: GameState) {
        circle(ctx, joystickCenterX, joystickCenterY, joystickRadius, "rgba(15,23,42,0.65)", "#38bdf8", 4.0)
        val kx = joystickCenterX + state.joystickX * (joystickRadius * 0.62)
        val ky = joystickCenterY + state.joystickY * (joystickRadius * 0.62)
        circle(ctx, kx, ky, 44.0, "rgba(56,189,248,0.65)", "#ffffff", 3.0)

        circle(ctx, freeFireCenterX, freeFireCenterY, freeFireRadius, "rgba(239,68,68,0.45)", "#f97316", 5.0)
        drawText(ctx, "FUOCO", freeFireCenterX, freeFireCenterY + 10, "#ffffff", "bold 28px system-ui", CanvasTextAlign.CENTER)
    }

    private fun drawOverlay(ctx: CanvasRenderingContext2D, state: GameState) {
        ctx.fillStyle = "rgba(5, 7, 20, 0.72)"
        ctx.fillRect(0.0, 0.0, state.worldWidth.toDouble(), state.worldHeight.toDouble())
        val cx = state.worldWidth / 2.0
        val cy = state.worldHeight / 2.0
        val title = when (state.status) {
            GameStatus.READY -> state.level.title
            GameStatus.VICTORY -> "VITTORIA!"
            GameStatus.DEFEAT -> "SCONFITTA!"
            GameStatus.PLAYING -> ""
        }
        val sub = when (state.status) {
            GameStatus.READY -> "${state.level.bossName} • Tocca o premi SPAZIO per iniziare"
            GameStatus.VICTORY -> "Boss sconfitto! Tocca o premi SPAZIO per rigiocare"
            GameStatus.DEFEAT -> "Riprova! Tocca o premi SPAZIO"
            GameStatus.PLAYING -> ""
        }
        val col = if (state.status == GameStatus.VICTORY) "#4ade80" else if (state.status == GameStatus.DEFEAT) "#ef4444" else "#facc15"
        drawText(ctx, title, cx, cy - 28, col, "900 54px system-ui", CanvasTextAlign.CENTER)
        drawText(ctx, sub, cx, cy + 32, "#f8fafc", "bold 26px system-ui", CanvasTextAlign.CENTER)
    }

    private fun circle(ctx: CanvasRenderingContext2D, x: Double, y: Double, r: Double, fill: String, stroke: String?, lw: Double) {
        ctx.beginPath(); ctx.arc(x, y, r, 0.0, PI * 2)
        if (fill != "transparent") { ctx.fillStyle = fill; ctx.fill() }
        if (stroke != null) { ctx.strokeStyle = stroke; ctx.lineWidth = lw; ctx.stroke() }
    }

    private fun roundRect(ctx: CanvasRenderingContext2D, x: Double, y: Double, w: Double, h: Double, rad: Double, fill: String, stroke: String?, lw: Double) {
        val r = min(rad, min(w / 2, h / 2)).coerceAtLeast(0.0)
        ctx.beginPath()
        ctx.moveTo(x + r, y)
        ctx.lineTo(x + w - r, y); ctx.quadraticCurveTo(x + w, y, x + w, y + r)
        ctx.lineTo(x + w, y + h - r); ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
        ctx.lineTo(x + r, y + h); ctx.quadraticCurveTo(x, y + h, x, y + h - r)
        ctx.lineTo(x, y + r); ctx.quadraticCurveTo(x, y, x + r, y)
        ctx.closePath()
        ctx.fillStyle = fill; ctx.fill()
        if (stroke != null) { ctx.strokeStyle = stroke; ctx.lineWidth = lw; ctx.stroke() }
    }

    private fun drawText(ctx: CanvasRenderingContext2D, txt: String, x: Double, y: Double, color: String, font: String, align: CanvasTextAlign) {
        ctx.fillStyle = color
        ctx.font = font
        ctx.textAlign = align
        ctx.fillText(txt, x, y)
    }
}
