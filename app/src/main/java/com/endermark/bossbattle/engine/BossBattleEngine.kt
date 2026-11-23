package com.endermark.bossbattle.engine

import com.endermark.bossbattle.model.Boss
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.GameState
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.HitParticle
import com.endermark.bossbattle.model.LevelCatalog
import com.endermark.bossbattle.model.Player
import com.endermark.bossbattle.model.Projectile
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

class BossBattleEngine(
    val level: BossLevel = LevelCatalog.LEVEL_1,
    worldWidth: Float = DEFAULT_WORLD_WIDTH,
    worldHeight: Float = DEFAULT_WORLD_HEIGHT
) {
    val state: GameState = createInitialState(level, worldWidth, worldHeight)

    private var targetPlayerX: Float = worldWidth / 2f

    fun startGame() {
        resetBattle(GameStatus.PLAYING)
    }

    fun handleTapOrRestart() {
        when (state.status) {
            GameStatus.READY -> startGame()
            GameStatus.VICTORY, GameStatus.DEFEAT -> {
                if (state.endStateCooldown <= 0f) {
                    startGame()
                }
            }
            GameStatus.PLAYING -> Unit
        }
    }

    fun setPlayerTargetX(worldX: Float) {
        val margin = state.player.radius + ARENA_PADDING
        targetPlayerX = worldX.coerceIn(margin, state.worldWidth - margin)
    }

    fun resetBattle(status: GameStatus = GameStatus.READY) {
        state.status = status
        state.elapsedTime = 0f
        state.endStateCooldown = 0f
        state.projectiles.clear()
        state.particles.clear()

        state.player.apply {
            x = state.worldWidth / 2f
            y = state.worldHeight - PLAYER_BOTTOM_OFFSET
            hp = maxHp
            invulnerableTimer = 0f
            fireCooldown = 0.15f
        }
        targetPlayerX = state.player.x

        state.boss.apply {
            x = state.worldWidth / 2f
            y = BOSS_TOP_OFFSET
            hp = maxHp
            displayedHp = maxHp.toFloat()
            velocityX = level.bossBaseSpeed
            attackCooldown = 0.9f
            hitFlashTimer = 0f
        }
    }

    fun update(dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0f, MAX_DT)
        state.elapsedTime += dt
        updateParticles(dt)
        updateDisplayedBossHp(dt)

        if (state.status != GameStatus.PLAYING) {
            if (state.endStateCooldown > 0f) {
                state.endStateCooldown = max(0f, state.endStateCooldown - dt)
            }
            return
        }

        updatePlayer(dt)
        updateBoss(dt)
        updateProjectiles(dt)
        resolveCollisions()
    }

    private fun updatePlayer(dt: Float) {
        val player = state.player
        val dx = targetPlayerX - player.x
        val maxStep = PLAYER_MOVE_SPEED * dt
        player.x = if (kotlin.math.abs(dx) <= maxStep) {
            targetPlayerX
        } else {
            player.x + kotlin.math.sign(dx) * maxStep
        }

        val margin = player.radius + ARENA_PADDING
        player.x = player.x.coerceIn(margin, state.worldWidth - margin)

        if (player.invulnerableTimer > 0f) {
            player.invulnerableTimer = max(0f, player.invulnerableTimer - dt)
        }

        player.fireCooldown -= dt
        if (player.fireCooldown <= 0f) {
            spawnPlayerShot()
            player.fireCooldown = PLAYER_FIRE_INTERVAL
        }
    }

    private fun updateBoss(dt: Float) {
        val boss = state.boss
        if (boss.hitFlashTimer > 0f) {
            boss.hitFlashTimer = max(0f, boss.hitFlashTimer - dt)
        }

        val speedMultiplier = if (boss.isEnraged) BOSS_ENRAGED_SPEED_MULTIPLIER else 1f
        boss.x += boss.velocityX * speedMultiplier * dt

        val minX = boss.radius + ARENA_PADDING
        val maxX = state.worldWidth - boss.radius - ARENA_PADDING
        if (boss.x <= minX) {
            boss.x = minX
            boss.velocityX = kotlin.math.abs(boss.velocityX)
        } else if (boss.x >= maxX) {
            boss.x = maxX
            boss.velocityX = -kotlin.math.abs(boss.velocityX)
        }

        boss.attackCooldown -= dt
        if (boss.attackCooldown <= 0f) {
            spawnBossAttack()
            boss.attackCooldown = if (boss.isEnraged) {
                level.bossEnragedAttackInterval
            } else {
                level.bossAttackInterval
            }
        }
    }

    private fun updateProjectiles(dt: Float) {
        val iterator = state.projectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            projectile.x += projectile.vx * dt
            projectile.y += projectile.vy * dt

            val outOfBounds = projectile.x < -projectile.radius * 2f ||
                projectile.x > state.worldWidth + projectile.radius * 2f ||
                projectile.y < -projectile.radius * 2f ||
                projectile.y > state.worldHeight + projectile.radius * 2f
            if (outOfBounds) {
                iterator.remove()
            }
        }
    }

    private fun resolveCollisions() {
        val boss = state.boss
        val player = state.player
        val iterator = state.projectiles.iterator()

        while (iterator.hasNext()) {
            val projectile = iterator.next()
            if (projectile.fromPlayer) {
                if (circlesCollide(
                        projectile.x, projectile.y, projectile.radius,
                        boss.x, boss.y, boss.radius
                    )
                ) {
                    iterator.remove()
                    applyDamageToBoss(projectile.damage, projectile.x, projectile.y)
                    if (state.status != GameStatus.PLAYING) {
                        return
                    }
                }
            } else {
                if (!player.isInvulnerable && circlesCollide(
                        projectile.x, projectile.y, projectile.radius,
                        player.x, player.y, player.radius
                    )
                ) {
                    iterator.remove()
                    applyDamageToPlayer(projectile.damage)
                    if (state.status != GameStatus.PLAYING) {
                        return
                    }
                }
            }
        }
    }

    private fun applyDamageToBoss(damage: Int, hitX: Float, hitY: Float) {
        val boss = state.boss
        boss.hp = max(0, boss.hp - damage)
        boss.hitFlashTimer = BOSS_HIT_FLASH_DURATION
        spawnHitParticles(hitX, hitY, color = COLOR_BOSS_HIT_SPARK, count = 7)

        if (boss.hp <= 0) {
            state.status = GameStatus.VICTORY
            state.endStateCooldown = END_STATE_COOLDOWN
            state.projectiles.clear()
            spawnHitParticles(boss.x, boss.y, color = COLOR_VICTORY_SPARK, count = 24)
        }
    }

    private fun applyDamageToPlayer(damage: Int) {
        val player = state.player
        player.hp = max(0, player.hp - damage)
        player.invulnerableTimer = PLAYER_INVULNERABLE_DURATION
        spawnHitParticles(player.x, player.y, color = COLOR_PLAYER_HIT_SPARK, count = 12)

        if (player.hp <= 0) {
            state.status = GameStatus.DEFEAT
            state.endStateCooldown = END_STATE_COOLDOWN
            state.projectiles.clear()
        }
    }

    private fun spawnPlayerShot() {
        val player = state.player
        state.projectiles.add(
            Projectile(
                x = player.x,
                y = player.y - player.radius - 8f,
                vx = 0f,
                vy = -PLAYER_PROJECTILE_SPEED,
                radius = 12f,
                fromPlayer = true,
                damage = PLAYER_SHOT_DAMAGE
            )
        )
    }

    private fun spawnBossAttack() {
        val boss = state.boss
        val player = state.player
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()

        val angles = if (boss.isEnraged) {
            listOf(baseAngle - SPREAD_ANGLE_RAD, baseAngle, baseAngle + SPREAD_ANGLE_RAD)
        } else {
            listOf(baseAngle)
        }

        val speed = if (boss.isEnraged) {
            level.bossEnragedProjectileSpeed
        } else {
            level.bossProjectileSpeed
        }
        for (angle in angles) {
            state.projectiles.add(
                Projectile(
                    x = boss.x,
                    y = boss.y + boss.radius * 0.65f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 18f,
                    fromPlayer = false,
                    damage = 1
                )
            )
        }
    }

    private fun spawnHitParticles(x: Float, y: Float, color: Int, count: Int) {
        for (i in 0 until count) {
            val angle = (2.0 * PI * i) / count
            val speed = 140f + (i % 3) * 75f
            state.particles.add(
                HitParticle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    life = 0.35f,
                    maxLife = 0.35f,
                    color = color
                )
            )
        }
    }

    private fun updateParticles(dt: Float) {
        val iterator = state.particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.x += particle.vx * dt
            particle.y += particle.vy * dt
            particle.life -= dt
            if (particle.life <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun updateDisplayedBossHp(dt: Float) {
        val boss = state.boss
        val target = boss.hp.toFloat()
        if (boss.displayedHp > target) {
            boss.displayedHp = max(target, boss.displayedHp - BOSS_HP_TRAIL_SPEED * dt)
        } else {
            boss.displayedHp = target
        }
    }

    private fun circlesCollide(
        x1: Float,
        y1: Float,
        r1: Float,
        x2: Float,
        y2: Float,
        r2: Float
    ): Boolean {
        return hypot(x2 - x1, y2 - y1) <= (r1 + r2)
    }

    private fun createInitialState(
        level: BossLevel,
        worldWidth: Float,
        worldHeight: Float
    ): GameState {
        return GameState(
            level = level,
            worldWidth = worldWidth,
            worldHeight = worldHeight,
            status = GameStatus.READY,
            player = Player(
                x = worldWidth / 2f,
                y = worldHeight - PLAYER_BOTTOM_OFFSET
            ),
            boss = Boss(
                name = level.bossName,
                x = worldWidth / 2f,
                y = BOSS_TOP_OFFSET,
                hp = level.bossMaxHp,
                maxHp = level.bossMaxHp,
                displayedHp = level.bossMaxHp.toFloat(),
                velocityX = level.bossBaseSpeed
            )
        )
    }

    companion object {
        const val DEFAULT_WORLD_WIDTH = 1000f
        const val DEFAULT_WORLD_HEIGHT = 1600f

        private const val MAX_DT = 0.05f
        private const val ARENA_PADDING = 36f
        private const val PLAYER_BOTTOM_OFFSET = 220f
        private const val BOSS_TOP_OFFSET = 360f

        private const val PLAYER_MOVE_SPEED = 1500f
        private const val PLAYER_FIRE_INTERVAL = 0.24f
        private const val PLAYER_PROJECTILE_SPEED = 1150f
        private const val PLAYER_SHOT_DAMAGE = 5
        private const val PLAYER_INVULNERABLE_DURATION = 1.0f

        private const val BOSS_BASE_SPEED = 240f
        private const val BOSS_ENRAGED_SPEED_MULTIPLIER = 1.45f
        private const val BOSS_ATTACK_INTERVAL = 1.1f
        private const val BOSS_ENRAGED_ATTACK_INTERVAL = 0.9f
        private const val BOSS_PROJECTILE_SPEED = 520f
        private const val BOSS_ENRAGED_PROJECTILE_SPEED = 600f
        private const val BOSS_HIT_FLASH_DURATION = 0.08f
        private const val BOSS_HP_TRAIL_SPEED = 32f
        private const val SPREAD_ANGLE_RAD = 0.28

        private const val END_STATE_COOLDOWN = 0.4f

        const val COLOR_BOSS_HIT_SPARK = 0xFFFFD54F.toInt()
        const val COLOR_VICTORY_SPARK = 0xFFFF7043.toInt()
        const val COLOR_PLAYER_HIT_SPARK = 0xFF4FC3F7.toInt()
    }
}
