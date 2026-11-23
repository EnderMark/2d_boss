package com.endermark.bossbattle.engine

import com.endermark.bossbattle.model.ArenaTheme
import com.endermark.bossbattle.model.BattleMode
import com.endermark.bossbattle.model.Boss
import com.endermark.bossbattle.model.BossAction
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.GameSoundEvent
import com.endermark.bossbattle.model.GameState
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.HitParticle
import com.endermark.bossbattle.model.LevelCatalog
import com.endermark.bossbattle.model.Player
import com.endermark.bossbattle.model.Projectile
import com.endermark.bossbattle.model.ProjectileKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin

class BossBattleEngine(
    val level: BossLevel = LevelCatalog.LEVEL_1,
    worldWidth: Float = DEFAULT_WORLD_WIDTH,
    worldHeight: Float = DEFAULT_WORLD_HEIGHT
) {
    val state: GameState = createInitialState(level, worldWidth, worldHeight)

    private var targetPlayerX: Float = worldWidth / 2f
    private var moveInput: Int = 0
    private var nextMeleeBossAttackIsCharge: Boolean = false
    private var complaintIndex: Int = 0
    private val pendingSoundEvents: MutableList<GameSoundEvent> = mutableListOf()

    fun drainSoundEvents(): List<GameSoundEvent> {
        if (pendingSoundEvents.isEmpty()) return emptyList()
        val copy = pendingSoundEvents.toList()
        pendingSoundEvents.clear()
        return copy
    }

    private fun emitSound(event: GameSoundEvent) {
        pendingSoundEvents.add(event)
    }

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

    fun setMoveInput(direction: Int) {
        moveInput = direction.coerceIn(-1, 1)
    }

    fun setBlocking(blocking: Boolean) {
        state.player.isBlocking = level.hasShield && blocking && state.status == GameStatus.PLAYING
    }

    fun triggerJump(): Boolean {
        if (state.status != GameStatus.PLAYING || level.mode != BattleMode.MELEE_PLATFORMER) {
            return false
        }
        val player = state.player
        if (player.jumpsRemaining <= 0) {
            return false
        }
        val isFirstJump = player.jumpsRemaining == Player.MAX_JUMPS
        player.vy = if (isFirstJump) JUMP_VELOCITY else DOUBLE_JUMP_VELOCITY
        player.onGround = false
        player.jumpsRemaining -= 1
        emitSound(if (isFirstJump) GameSoundEvent.JUMP else GameSoundEvent.DOUBLE_JUMP)
        spawnHitParticles(
            x = player.x,
            y = player.y + player.radius,
            color = if (isFirstJump) COLOR_JUMP_DUST else COLOR_DOUBLE_JUMP_SPARK,
            count = if (isFirstJump) 5 else 8
        )
        return true
    }

    fun triggerMeleeAttack(): Boolean {
        if (state.status != GameStatus.PLAYING || level.mode != BattleMode.MELEE_PLATFORMER) {
            return false
        }
        val player = state.player
        if (player.meleeCooldown > 0f || player.isBlocking) {
            return false
        }
        player.meleeTimer = MELEE_DURATION
        player.meleeCooldown = MELEE_COOLDOWN
        player.meleeHitApplied = false
        emitSound(GameSoundEvent.ATTACK)
        return true
    }

    fun resetBattle(status: GameStatus = GameStatus.READY) {
        state.status = status
        state.elapsedTime = 0f
        state.endStateCooldown = 0f
        state.projectiles.clear()
        state.particles.clear()
        pendingSoundEvents.clear()
        moveInput = 0
        nextMeleeBossAttackIsCharge = false
        complaintIndex = 0

        if (level.mode == BattleMode.MELEE_PLATFORMER) {
            state.player.apply {
                x = state.worldWidth * 0.24f
                y = state.groundY - radius
                hp = maxHp
                invulnerableTimer = 0f
                fireCooldown = 0f
                vx = 0f
                vy = 0f
                onGround = true
                jumpsRemaining = Player.MAX_JUMPS
                facingDir = 1
                meleeTimer = 0f
                meleeCooldown = 0f
                meleeHitApplied = false
                isBlocking = false
                shieldFlashTimer = 0f
            }
            state.boss.apply {
                x = state.worldWidth * 0.76f
                y = state.groundY - radius
                hp = maxHp
                displayedHp = maxHp.toFloat()
                velocityX = -level.bossBaseSpeed
                attackCooldown = 1.2f
                hitFlashTimer = 0f
                action = BossAction.PATROL
                actionTimer = 0f
                facingDir = -1
                vanishTimer = 0f
                hitsSinceVanish = 0
            }
        } else {
            state.player.apply {
                x = state.worldWidth / 2f
                y = state.worldHeight - PLAYER_BOTTOM_OFFSET
                hp = maxHp
                invulnerableTimer = 0f
                fireCooldown = 0.15f
                vx = 0f
                vy = 0f
                onGround = true
                jumpsRemaining = Player.MAX_JUMPS
                facingDir = 1
                meleeTimer = 0f
                meleeCooldown = 0f
                meleeHitApplied = false
                isBlocking = false
                shieldFlashTimer = 0f
            }
            state.boss.apply {
                x = state.worldWidth / 2f
                y = BOSS_TOP_OFFSET
                hp = maxHp
                displayedHp = maxHp.toFloat()
                velocityX = level.bossBaseSpeed
                attackCooldown = 0.9f
                hitFlashTimer = 0f
                action = BossAction.PATROL
                actionTimer = 0f
                facingDir = 1
                vanishTimer = 0f
                hitsSinceVanish = 0
            }
        }
        targetPlayerX = state.player.x
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

        if (level.mode == BattleMode.MELEE_PLATFORMER) {
            updateMeleePlayer(dt)
            updateMeleeBoss(dt)
        } else {
            updateShooterPlayer(dt)
            updateShooterBoss(dt)
        }
        updateProjectiles(dt)
        resolveCollisions()
    }

    private fun updateShooterPlayer(dt: Float) {
        val player = state.player
        val dx = targetPlayerX - player.x
        val maxStep = PLAYER_MOVE_SPEED * dt
        player.x = if (abs(dx) <= maxStep) {
            targetPlayerX
        } else {
            player.x + sign(dx) * maxStep
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

    private fun updateMeleePlayer(dt: Float) {
        val player = state.player
        val speedFactor = if (player.isBlocking) SHIELD_MOVE_MULTIPLIER else 1f
        player.vx = moveInput * PLATFORMER_MOVE_SPEED * speedFactor
        if (moveInput != 0) {
            player.facingDir = moveInput
        }

        player.vy += GRAVITY * dt
        player.x += player.vx * dt
        player.y += player.vy * dt

        val margin = player.radius + ARENA_PADDING
        player.x = player.x.coerceIn(margin, state.worldWidth - margin)

        val floorY = state.groundY - player.radius
        if (player.y >= floorY) {
            player.y = floorY
            player.vy = 0f
            player.onGround = true
            player.jumpsRemaining = Player.MAX_JUMPS
        } else {
            player.onGround = false
        }

        if (player.invulnerableTimer > 0f) {
            player.invulnerableTimer = max(0f, player.invulnerableTimer - dt)
        }
        if (player.meleeTimer > 0f) {
            player.meleeTimer = max(0f, player.meleeTimer - dt)
        }
        if (player.meleeCooldown > 0f) {
            player.meleeCooldown = max(0f, player.meleeCooldown - dt)
        }
        if (player.shieldFlashTimer > 0f) {
            player.shieldFlashTimer = max(0f, player.shieldFlashTimer - dt)
        }
    }

    private fun updateShooterBoss(dt: Float) {
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
            boss.velocityX = abs(boss.velocityX)
        } else if (boss.x >= maxX) {
            boss.x = maxX
            boss.velocityX = -abs(boss.velocityX)
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

    private fun updateMeleeBoss(dt: Float) {
        val boss = state.boss
        val player = state.player
        if (boss.hitFlashTimer > 0f) {
            boss.hitFlashTimer = max(0f, boss.hitFlashTimer - dt)
        }

        val minX = boss.radius + ARENA_PADDING
        val maxX = state.worldWidth - boss.radius - ARENA_PADDING
        boss.y = state.groundY - boss.radius

        // Ivanone vanish / reappear handling
        if (boss.vanishTimer > 0f) {
            boss.vanishTimer = max(0f, boss.vanishTimer - dt)
            if (boss.vanishTimer <= 0f) {
                boss.x = if (player.x < state.worldWidth / 2f) {
                    (state.worldWidth * 0.80f).coerceIn(minX, maxX)
                } else {
                    (state.worldWidth * 0.20f).coerceIn(minX, maxX)
                }
                boss.facingDir = if (player.x >= boss.x) 1 else -1
                boss.action = BossAction.PATROL
                boss.attackCooldown = if (boss.isEnraged) 0.55f else 0.75f
                spawnHitParticles(boss.x, boss.y, COLOR_VANISH_SMOKE, 12)
            }
            return
        }

        when (boss.action) {
            BossAction.PATROL -> {
                val dx = player.x - boss.x
                if (abs(dx) > 12f) {
                    boss.facingDir = if (dx >= 0f) 1 else -1
                }
                val speed = level.bossBaseSpeed * if (boss.isEnraged) 1.35f else 1f
                when (level.arenaTheme) {
                    // Louise: seduces from mid-range, then avoids/retreats when you get close!
                    ArenaTheme.PARIS -> {
                        val avoidDist = boss.radius + player.radius + 145f
                        if (abs(dx) < avoidDist) {
                            val awayDir = if (dx >= 0f) -1 else 1
                            boss.x += awayDir * speed * 1.15f * dt
                        } else if (abs(dx) > avoidDist + 120f) {
                            boss.x += boss.facingDir * speed * 0.7f * dt
                        }
                    }
                    // Andrea Dattero: approaches aggressively for grappling!
                    ArenaTheme.GYM_DEV -> {
                        if (abs(dx) > boss.radius + player.radius + 10f) {
                            boss.x += boss.facingDir * speed * 1.15f * dt
                        }
                    }
                    else -> {
                        if (abs(dx) > boss.radius + player.radius + 55f) {
                            boss.x += boss.facingDir * speed * dt
                        }
                    }
                }
                boss.x = boss.x.coerceIn(minX, maxX)

                boss.attackCooldown -= dt
                if (boss.attackCooldown <= 0f) {
                    if (nextMeleeBossAttackIsCharge) {
                        boss.action = BossAction.TELEGRAPH_CHARGE
                        boss.actionTimer = if (boss.isEnraged) 0.36f else 0.48f
                    } else {
                        boss.action = BossAction.TELEGRAPH_SHOCKWAVE
                        boss.actionTimer = if (boss.isEnraged) 0.34f else 0.44f
                    }
                    nextMeleeBossAttackIsCharge = !nextMeleeBossAttackIsCharge
                }
            }

            BossAction.TELEGRAPH_SHOCKWAVE -> {
                boss.actionTimer -= dt
                if (boss.actionTimer <= 0f) {
                    when (level.arenaTheme) {
                        ArenaTheme.HANOI -> spawnChanLanternAttack()
                        ArenaTheme.ZURICH -> spawnLolloComplaintAttack()
                        ArenaTheme.STUDIO -> spawnIvanoneDroneAttack()
                        ArenaTheme.ZURICH_TAX -> spawnAlessioTaxAttack()
                        ArenaTheme.PARIS -> spawnLouiseSeductionAttack()
                        ArenaTheme.UBS_ALPS -> spawnDaisyScreamAttack()
                        ArenaTheme.GYM_DEV -> spawnAndreaRiceAndDevAttack()
                        ArenaTheme.GOOGLE_ZURICH -> spawnMarcoPorscheAttack()
                        else -> spawnGroundShockwaves()
                    }
                    boss.action = BossAction.PATROL
                    boss.attackCooldown = if (boss.isEnraged) {
                        level.bossEnragedAttackInterval
                    } else {
                        level.bossAttackInterval
                    }
                }
            }

            BossAction.TELEGRAPH_CHARGE -> {
                boss.facingDir = if (player.x >= boss.x) 1 else -1
                boss.actionTimer -= dt
                if (boss.actionTimer <= 0f) {
                    when (level.arenaTheme) {
                        ArenaTheme.STUDIO -> {
                            spawnIvanoneCameraAttack()
                            triggerBossVanish()
                        }
                        ArenaTheme.ZURICH_TAX -> {
                            spawnAlessioWeirdToothAttack()
                            boss.action = BossAction.PATROL
                            boss.attackCooldown = if (boss.isEnraged) {
                                level.bossEnragedAttackInterval
                            } else {
                                level.bossAttackInterval
                            }
                        }
                        ArenaTheme.PARIS -> {
                            // Louise dodges away / darts across to avoid you, leaving a teasing heart
                            spawnLouiseTeaseHeart()
                            val awayDir = if (player.x >= boss.x) -1 else 1
                            val nearEdge = (awayDir < 0 && boss.x < state.worldWidth * 0.30f) ||
                                (awayDir > 0 && boss.x > state.worldWidth * 0.70f)
                            boss.facingDir = if (nearEdge) -awayDir else awayDir
                            boss.action = BossAction.CHARGING
                            boss.actionTimer = 0.52f
                        }
                        else -> {
                            boss.action = BossAction.CHARGING
                            boss.actionTimer = 0.65f
                        }
                    }
                }
            }

            BossAction.CHARGING -> {
                val baseCharge = when (level.arenaTheme) {
                    ArenaTheme.HANOI -> CHAN_CHARGE_SPEED
                    ArenaTheme.PARIS -> LOUISE_EVADE_SPEED
                    ArenaTheme.GYM_DEV -> ANDREA_GRAPPLE_SPEED
                    ArenaTheme.GOOGLE_ZURICH -> MARCO_PORSCHE_SPEED
                    else -> BOSS_CHARGE_SPEED
                }
                val enragedCharge = when (level.arenaTheme) {
                    ArenaTheme.HANOI -> CHAN_ENRAGED_CHARGE_SPEED
                    ArenaTheme.PARIS -> LOUISE_ENRAGED_EVADE_SPEED
                    ArenaTheme.GYM_DEV -> ANDREA_ENRAGED_GRAPPLE_SPEED
                    ArenaTheme.GOOGLE_ZURICH -> MARCO_ENRAGED_PORSCHE_SPEED
                    else -> BOSS_ENRAGED_CHARGE_SPEED
                }
                val chargeSpeed = if (boss.isEnraged) enragedCharge else baseCharge
                boss.x += boss.facingDir * chargeSpeed * dt
                if (boss.x <= minX || boss.x >= maxX) {
                    boss.x = boss.x.coerceIn(minX, maxX)
                    boss.actionTimer = 0f
                    spawnHitParticles(boss.x, state.groundY - 24f, COLOR_BOSS_HIT_SPARK, 8)
                }
                boss.actionTimer -= dt
                if (boss.actionTimer <= 0f) {
                    boss.action = BossAction.PATROL
                    boss.attackCooldown = if (boss.isEnraged) {
                        level.bossEnragedAttackInterval
                    } else {
                        level.bossAttackInterval
                    }
                }
            }
        }
    }

    private fun triggerBossVanish() {
        val boss = state.boss
        boss.vanishTimer = if (boss.isEnraged) IVANONE_ENRAGED_VANISH_DURATION else IVANONE_VANISH_DURATION
        boss.action = BossAction.PATROL
        boss.hitsSinceVanish = 0
        spawnHitParticles(boss.x, boss.y, COLOR_VANISH_SMOKE, 14)
    }

    private fun updateProjectiles(dt: Float) {
        val iterator = state.projectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            projectile.x += projectile.vx * dt
            projectile.y += projectile.vy * dt

            val outOfBounds = projectile.x < -projectile.radius * 3f ||
                projectile.x > state.worldWidth + projectile.radius * 3f ||
                projectile.y < -projectile.radius * 3f ||
                projectile.y > state.worldHeight + projectile.radius * 3f
            if (outOfBounds) {
                iterator.remove()
            }
        }
    }

    private fun resolveCollisions() {
        val boss = state.boss
        val player = state.player

        if (level.mode == BattleMode.MELEE_PLATFORMER && !boss.isVanished) {
            // Player melee swing vs Boss
            if (player.isAttacking && !player.meleeHitApplied) {
                val strikeX = player.x + player.facingDir * MELEE_REACH_OFFSET
                val strikeY = player.y
                if (circlesCollide(strikeX, strikeY, MELEE_HIT_RADIUS, boss.x, boss.y, boss.radius)) {
                    player.meleeHitApplied = true
                    applyDamageToBoss(MELEE_DAMAGE, strikeX, strikeY)
                    if (state.status != GameStatus.PLAYING) {
                        return
                    }
                }
            }

            // Boss body contact vs Player
            if (!boss.isVanished && !player.isInvulnerable && circlesCollide(
                    player.x, player.y, player.radius * 0.9f,
                    boss.x, boss.y, boss.radius * 0.85f
                )
            ) {
                val pushDir = if (player.x < boss.x) -1 else 1
                player.x = (player.x + pushDir * 64f).coerceIn(
                    player.radius + ARENA_PADDING,
                    state.worldWidth - player.radius - ARENA_PADDING
                )
                if (player.isBlocking) {
                    player.shieldFlashTimer = SHIELD_FLASH_DURATION
                    player.invulnerableTimer = 0.25f
                    emitSound(GameSoundEvent.SHIELD_BLOCK)
                    if (boss.action == BossAction.CHARGING) {
                        boss.action = BossAction.PATROL
                        boss.attackCooldown = level.bossAttackInterval
                    }
                    spawnHitParticles(
                        player.x - pushDir * player.radius,
                        player.y,
                        COLOR_SHIELD_BLOCK_SPARK,
                        10
                    )
                } else {
                    player.vy = -420f
                    player.onGround = false
                    applyDamageToPlayer(1)
                    if (state.status != GameStatus.PLAYING) {
                        return
                    }
                }
            }
        }

        val iterator = state.projectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            if (projectile.fromPlayer) {
                if (!boss.isVanished && circlesCollide(
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
                if (circlesCollide(
                        projectile.x, projectile.y, projectile.radius,
                        player.x, player.y, player.radius
                    )
                ) {
                    if (player.isBlocking) {
                        iterator.remove()
                        player.shieldFlashTimer = SHIELD_FLASH_DURATION
                        emitSound(GameSoundEvent.SHIELD_BLOCK)
                        spawnHitParticles(
                            projectile.x,
                            projectile.y,
                            COLOR_SHIELD_BLOCK_SPARK,
                            9
                        )
                    } else if (!player.isInvulnerable) {
                        iterator.remove()
                        applyDamageToPlayer(projectile.damage)
                        if (state.status != GameStatus.PLAYING) {
                            return
                        }
                    }
                }
            }
        }
    }

    private fun applyDamageToBoss(damage: Int, hitX: Float, hitY: Float) {
        val boss = state.boss
        boss.hp = max(0, boss.hp - damage)
        boss.hitFlashTimer = BOSS_HIT_FLASH_DURATION
        spawnHitParticles(hitX, hitY, color = COLOR_BOSS_HIT_SPARK, count = 8)

        if (boss.hp <= 0) {
            state.status = GameStatus.VICTORY
            state.endStateCooldown = END_STATE_COOLDOWN
            state.projectiles.clear()
            emitSound(GameSoundEvent.VICTORY)
            spawnHitParticles(boss.x, boss.y, color = COLOR_VICTORY_SPARK, count = 24)
            return
        }

        emitSound(GameSoundEvent.BOSS_HIT)

        // Ivanone vanishes easily after taking 2 hits!
        if (level.arenaTheme == ArenaTheme.STUDIO) {
            boss.hitsSinceVanish += 1
            if (boss.hitsSinceVanish >= IVANONE_HITS_BEFORE_VANISH) {
                triggerBossVanish()
            }
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
            emitSound(GameSoundEvent.DEFEAT)
        } else {
            emitSound(GameSoundEvent.PLAYER_HIT)
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
                damage = PLAYER_SHOT_DAMAGE,
                kind = ProjectileKind.PLAYER_BOLT
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
                    damage = 1,
                    kind = ProjectileKind.BOSS_ORB
                )
            )
        }
    }

    private fun spawnChanLanternAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.isEnraged) {
            level.bossEnragedProjectileSpeed
        } else {
            level.bossProjectileSpeed
        }
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val angles = if (boss.isEnraged) {
            listOf(baseAngle - 0.22, baseAngle, baseAngle + 0.22)
        } else {
            listOf(baseAngle - 0.12, baseAngle + 0.08)
        }

        spawnHitParticles(boss.x, boss.y - 12f, COLOR_SHIELD_BLOCK_SPARK, 8)
        for (angle in angles) {
            state.projectiles.add(
                Projectile(
                    x = boss.x + boss.facingDir * (boss.radius * 0.4f),
                    y = boss.y - 10f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 20f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.LANTERN
                )
            )
        }
    }

    private fun spawnLolloComplaintAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.isEnraged) {
            level.bossEnragedProjectileSpeed
        } else {
            level.bossProjectileSpeed
        }
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val angles = if (boss.isEnraged) {
            listOf(baseAngle - 0.20, baseAngle, baseAngle + 0.16)
        } else {
            listOf(baseAngle, baseAngle - 0.14)
        }

        spawnHitParticles(boss.x, boss.y - 30f, COLOR_VICTORY_SPARK, 8)
        for (angle in angles) {
            val text = LOLLO_COMPLAINTS[complaintIndex % LOLLO_COMPLAINTS.size]
            complaintIndex += 1
            state.projectiles.add(
                Projectile(
                    x = boss.x + boss.facingDir * (boss.radius * 0.45f),
                    y = boss.y - 24f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 24f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.COMPLAINT,
                    label = text
                )
            )
        }
    }

    private fun spawnIvanoneDroneAttack() {
        val boss = state.boss
        val speed = if (boss.isEnraged) {
            level.bossEnragedProjectileSpeed
        } else {
            level.bossProjectileSpeed
        }
        val dir = boss.facingDir.toFloat()
        val heights = if (boss.isEnraged) {
            listOf(state.groundY - 56f, state.groundY - 190f)
        } else {
            listOf(state.groundY - 90f)
        }
        spawnHitParticles(boss.x, boss.y - 30f, COLOR_PLAYER_HIT_SPARK, 8)
        for (yPos in heights) {
            state.projectiles.add(
                Projectile(
                    x = boss.x + dir * (boss.radius * 0.5f),
                    y = yPos,
                    vx = dir * speed,
                    vy = 0f,
                    radius = 24f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.DRONE
                )
            )
        }
    }

    private fun spawnIvanoneCameraAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.isEnraged) {
            level.bossEnragedProjectileSpeed * 1.15f
        } else {
            level.bossProjectileSpeed * 1.15f
        }
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val angles = if (boss.isEnraged) {
            listOf(baseAngle - 0.16, baseAngle, baseAngle + 0.16)
        } else {
            listOf(baseAngle)
        }
        spawnHitParticles(boss.x + boss.facingDir * 30f, boss.y - 16f, COLOR_SHIELD_BLOCK_SPARK, 12)
        for (angle in angles) {
            state.projectiles.add(
                Projectile(
                    x = boss.x + boss.facingDir * (boss.radius * 0.55f),
                    y = boss.y - 12f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 24f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.CAMERA_FLASH
                )
            )
        }
    }

    private fun spawnAlessioTaxAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.isEnraged) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val angles = if (boss.isEnraged) {
            listOf(baseAngle - 0.18, baseAngle, baseAngle + 0.16)
        } else {
            listOf(baseAngle, baseAngle - 0.14)
        }
        spawnHitParticles(boss.x, boss.y - 24f, COLOR_SHIELD_BLOCK_SPARK, 8)
        for (angle in angles) {
            val line = ALESSIO_TAX_LINES[complaintIndex % ALESSIO_TAX_LINES.size]
            complaintIndex += 1
            state.projectiles.add(
                Projectile(
                    x = boss.x + boss.facingDir * (boss.radius * 0.45f),
                    y = boss.y - 18f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 24f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.TAX_FORM,
                    label = line
                )
            )
        }
    }

    private fun spawnAlessioWeirdToothAttack() {
        val boss = state.boss
        val player = state.player
        val speed = (if (boss.isEnraged) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed) * 1.18f
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val angles = if (boss.isEnraged) listOf(baseAngle - 0.14, baseAngle + 0.14) else listOf(baseAngle)
        spawnHitParticles(boss.x + boss.facingDir * 24f, boss.y - 28f, COLOR_BOSS_HIT_SPARK, 12)
        for (angle in angles) {
            state.projectiles.add(
                Projectile(
                    x = boss.x + boss.facingDir * (boss.radius * 0.5f),
                    y = boss.y - 24f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 30f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.WEIRD_TOOTH,
                    label = "DENTE STRANO!"
                )
            )
        }
    }

    private fun spawnLouiseSeductionAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.isEnraged) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val angles = if (boss.isEnraged) {
            listOf(baseAngle - 0.22, baseAngle, baseAngle + 0.22)
        } else {
            listOf(baseAngle - 0.10, baseAngle + 0.10)
        }
        spawnHitParticles(boss.x, boss.y - 20f, COLOR_LOUISE_PINK, 10)
        for (angle in angles) {
            val line = LOUISE_LINES[complaintIndex % LOUISE_LINES.size]
            complaintIndex += 1
            state.projectiles.add(
                Projectile(
                    x = boss.x + boss.facingDir * (boss.radius * 0.4f),
                    y = boss.y - 18f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 22f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.HEART,
                    label = line
                )
            )
        }
    }

    private fun spawnLouiseTeaseHeart() {
        val boss = state.boss
        val player = state.player
        val angle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val speed = level.bossProjectileSpeed * 0.85f
        spawnHitParticles(boss.x, boss.y - 14f, COLOR_LOUISE_PINK, 8)
        state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y - 16f,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                radius = 22f,
                fromPlayer = false,
                damage = 1,
                kind = ProjectileKind.HEART,
                label = "Non, merci~"
            )
        )
    }

    private fun spawnDaisyScreamAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.isEnraged) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val angles = if (boss.isEnraged) {
            listOf(baseAngle - 0.20, baseAngle, baseAngle + 0.18)
        } else {
            listOf(baseAngle, baseAngle - 0.14)
        }
        spawnHitParticles(boss.x, boss.y - 26f, COLOR_VICTORY_SPARK, 10)
        for (angle in angles) {
            val line = DAISY_SCREAMS[complaintIndex % DAISY_SCREAMS.size]
            complaintIndex += 1
            state.projectiles.add(
                Projectile(
                    x = boss.x + boss.facingDir * (boss.radius * 0.45f),
                    y = boss.y - 22f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 26f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.SCREAM_WAVE,
                    label = line
                )
            )
        }
    }

    private fun spawnAndreaRiceAndDevAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.isEnraged) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val devLine = ANDREA_DEV_LINES[complaintIndex % ANDREA_DEV_LINES.size]
        complaintIndex += 1

        spawnHitParticles(boss.x, boss.y - 16f, COLOR_BOSS_HIT_SPARK, 8)
        // 200g rice & chicken meal-prep bowl
        state.projectiles.add(
            Projectile(
                x = boss.x + boss.facingDir * (boss.radius * 0.45f),
                y = boss.y - 12f,
                vx = (cos(baseAngle) * speed).toFloat(),
                vy = (sin(baseAngle) * speed).toFloat(),
                radius = 24f,
                fromPlayer = false,
                damage = 1,
                kind = ProjectileKind.RICE_CHICKEN,
                label = "200g RISO & POLLO"
            )
        )
        // Overconfident (bad) software engineer boast
        val boastAngle = baseAngle - 0.16
        state.projectiles.add(
            Projectile(
                x = boss.x + boss.facingDir * (boss.radius * 0.4f),
                y = boss.y - 34f,
                vx = (cos(boastAngle) * speed * 0.92f).toFloat(),
                vy = (sin(boastAngle) * speed * 0.92f).toFloat(),
                radius = 24f,
                fromPlayer = false,
                damage = 1,
                kind = ProjectileKind.COMPLAINT,
                label = devLine
            )
        )
    }

    private fun spawnMarcoPorscheAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.isEnraged) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val line = MARCO_DATING_LINES[complaintIndex % MARCO_DATING_LINES.size]
        complaintIndex += 1

        emitSound(GameSoundEvent.CAMERA_SHUTTER)
        spawnHitParticles(boss.x + boss.facingDir * 30f, boss.y - 26f, COLOR_SHIELD_BLOCK_SPARK, 12)

        // Camera flash shot from the white Porsche window
        val flashAngles = if (boss.isEnraged) listOf(baseAngle - 0.14, baseAngle + 0.14) else listOf(baseAngle)
        for (angle in flashAngles) {
            state.projectiles.add(
                Projectile(
                    x = boss.x + boss.facingDir * (boss.radius * 0.5f),
                    y = boss.y - 22f,
                    vx = (cos(angle) * speed * 1.1f).toFloat(),
                    vy = (sin(angle) * speed * 1.1f).toFloat(),
                    radius = 24f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.CAMERA_FLASH
                )
            )
        }

        // Dating / "sono pieno di milioni" flex bubble
        val flexAngle = baseAngle - 0.18
        state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y - 44f,
                vx = (cos(flexAngle) * speed * 0.88f).toFloat(),
                vy = (sin(flexAngle) * speed * 0.88f).toFloat(),
                radius = 24f,
                fromPlayer = false,
                damage = 1,
                kind = ProjectileKind.DATING_FLEX,
                label = line
            )
        )
    }

    private fun spawnGroundShockwaves() {
        val boss = state.boss
        val speed = if (boss.isEnraged) {
            level.bossEnragedProjectileSpeed
        } else {
            level.bossProjectileSpeed
        }
        val shockwaveY = state.groundY - 26f
        spawnHitParticles(boss.x, state.groundY - 10f, COLOR_VICTORY_SPARK, 10)
        for (dir in listOf(-1f, 1f)) {
            state.projectiles.add(
                Projectile(
                    x = boss.x + dir * (boss.radius * 0.5f),
                    y = shockwaveY,
                    vx = dir * speed,
                    vy = 0f,
                    radius = 24f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.SHOCKWAVE
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
        val isMelee = level.mode == BattleMode.MELEE_PLATFORMER
        val playerRadius = 36f
        val bossRadius = when (level.arenaTheme) {
            ArenaTheme.HANOI -> 64f
            ArenaTheme.ZURICH -> 66f
            ArenaTheme.STUDIO -> 74f
            ArenaTheme.ZURICH_TAX -> 68f
            ArenaTheme.PARIS -> 62f
            ArenaTheme.UBS_ALPS -> 64f
            ArenaTheme.GYM_DEV -> 58f
            ArenaTheme.GOOGLE_ZURICH -> 82f
            ArenaTheme.IRON_ARENA -> 84f
            ArenaTheme.SPACE -> 88f
        }
        return GameState(
            level = level,
            worldWidth = worldWidth,
            worldHeight = worldHeight,
            status = GameStatus.READY,
            player = Player(
                x = if (isMelee) worldWidth * 0.24f else worldWidth / 2f,
                y = if (isMelee) DEFAULT_GROUND_Y - playerRadius else worldHeight - PLAYER_BOTTOM_OFFSET,
                radius = playerRadius
            ),
            boss = Boss(
                name = level.bossName,
                x = if (isMelee) worldWidth * 0.76f else worldWidth / 2f,
                y = if (isMelee) DEFAULT_GROUND_Y - bossRadius else BOSS_TOP_OFFSET,
                radius = bossRadius,
                hp = level.bossMaxHp,
                maxHp = level.bossMaxHp,
                displayedHp = level.bossMaxHp.toFloat(),
                velocityX = if (isMelee) -level.bossBaseSpeed else level.bossBaseSpeed
            ),
            groundY = DEFAULT_GROUND_Y
        )
    }

    companion object {
        const val DEFAULT_WORLD_WIDTH = 1000f
        const val DEFAULT_WORLD_HEIGHT = 1600f
        const val DEFAULT_GROUND_Y = 1260f

        private const val MAX_DT = 0.05f
        private const val ARENA_PADDING = 36f
        private const val PLAYER_BOTTOM_OFFSET = 220f
        private const val BOSS_TOP_OFFSET = 360f

        private const val PLAYER_MOVE_SPEED = 1500f
        private const val PLAYER_FIRE_INTERVAL = 0.24f
        private const val PLAYER_PROJECTILE_SPEED = 1150f
        private const val PLAYER_SHOT_DAMAGE = 5
        private const val PLAYER_INVULNERABLE_DURATION = 1.0f

        private const val PLATFORMER_MOVE_SPEED = 460f
        private const val SHIELD_MOVE_MULTIPLIER = 0.55f
        private const val SHIELD_FLASH_DURATION = 0.18f
        private const val GRAVITY = 2600f
        private const val JUMP_VELOCITY = -1020f
        private const val DOUBLE_JUMP_VELOCITY = -920f
        private const val MELEE_DURATION = 0.20f
        private const val MELEE_COOLDOWN = 0.34f
        private const val MELEE_REACH_OFFSET = 64f
        private const val MELEE_HIT_RADIUS = 76f
        private const val MELEE_DAMAGE = 10

        private const val BOSS_ENRAGED_SPEED_MULTIPLIER = 1.45f
        private const val BOSS_CHARGE_SPEED = 740f
        private const val BOSS_ENRAGED_CHARGE_SPEED = 900f
        private const val CHAN_CHARGE_SPEED = 820f
        private const val CHAN_ENRAGED_CHARGE_SPEED = 980f
        private const val LOUISE_EVADE_SPEED = 860f
        private const val LOUISE_ENRAGED_EVADE_SPEED = 1020f
        private const val ANDREA_GRAPPLE_SPEED = 840f
        private const val ANDREA_ENRAGED_GRAPPLE_SPEED = 980f
        private const val MARCO_PORSCHE_SPEED = 900f
        private const val MARCO_ENRAGED_PORSCHE_SPEED = 1080f
        private const val BOSS_HIT_FLASH_DURATION = 0.08f
        private const val BOSS_HP_TRAIL_SPEED = 32f
        private const val SPREAD_ANGLE_RAD = 0.28

        private const val IVANONE_HITS_BEFORE_VANISH = 2
        private const val IVANONE_VANISH_DURATION = 0.72f
        private const val IVANONE_ENRAGED_VANISH_DURATION = 0.55f

        private const val END_STATE_COOLDOWN = 0.4f

        val LOLLO_COMPLAINTS = listOf(
            "CHE FREDDO!",
            "TROPPO CARO!",
            "SEMPRE PIOGGIA!",
            "ZERO SOLE!",
            "CAFFÈ PESSIMO!",
            "TUTTO CHIUSO!",
            "CHE ANSIA!",
            "QUANTO COSTA?!"
        )

        val ALESSIO_TAX_LINES = listOf(
            "PAGA LE TASSE A ZURIGO!",
            "SONO MIGLIORE DI TE!",
            "IO GUADAGNO DI PIÙ!",
            "MODULO FISCALE CHF!",
            "VITA SUPERIORE!",
            "TU NON CE LA FAI!"
        )

        val LOUISE_LINES = listOf(
            "Viens ici~",
            "Mon chéri~",
            "Regarde-moi...",
            "Trop mignon~"
        )

        val DAISY_SCREAMS = listOf(
            "NON MI INVITI ALLE HIKE!",
            "LAVORO A UBS IO!",
            "ANDATE SENZA DI ME?!",
            "OHHH! SENTIMI!",
            "DOMENICA HIKE SENZA DI ME?!",
            "PERCHÉ NON MI CHIAMI?!"
        )

        val ANDREA_DEV_LINES = listOf(
            "SONO UN 10X ENGINEER!",
            "CODICE PERFETTO! (crash)",
            "NULL POINTER EXCEPTION!",
            "DEPLOY ROTTO IN PROD!",
            "NON COMPILA MA FIDATI!"
        )

        val MARCO_DATING_LINES = listOf(
            "SONO PIENO DI MILIONI!",
            "VUOI UN GIRO IN PORSCHE?",
            "LAVORO A GOOGLE!",
            "SONO PIENO DI MILIONI!!",
            "MATCH SU HINGE?",
            "CENA STELLATA? PAGO IO!"
        )

        const val COLOR_BOSS_HIT_SPARK = 0xFFFFD54F.toInt()
        const val COLOR_VICTORY_SPARK = 0xFFFF7043.toInt()
        const val COLOR_PLAYER_HIT_SPARK = 0xFF4FC3F7.toInt()
        const val COLOR_JUMP_DUST = 0xFFB0BEC5.toInt()
        const val COLOR_DOUBLE_JUMP_SPARK = 0xFF80DEEA.toInt()
        const val COLOR_SHIELD_BLOCK_SPARK = 0xFFFFE082.toInt()
        const val COLOR_VANISH_SMOKE = 0xFFCFD8DC.toInt()
        const val COLOR_LOUISE_PINK = 0xFFF48FB1.toInt()
    }
}
