package com.endermark.bossbattle.engine

import com.endermark.bossbattle.model.ArenaPickup
import com.endermark.bossbattle.model.ArenaTheme
import com.endermark.bossbattle.model.ArenaTurret
import com.endermark.bossbattle.model.BattleMode
import com.endermark.bossbattle.model.Boss
import com.endermark.bossbattle.model.BossAction
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.BossWeakPoint
import com.endermark.bossbattle.model.GameSoundEvent
import com.endermark.bossbattle.model.GameState
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.HitParticle
import com.endermark.bossbattle.model.LaserBeam
import com.endermark.bossbattle.model.LevelCatalog
import com.endermark.bossbattle.model.MinionShip
import com.endermark.bossbattle.model.MovingPlatform
import com.endermark.bossbattle.model.PickupKind
import com.endermark.bossbattle.model.Player
import com.endermark.bossbattle.model.Projectile
import com.endermark.bossbattle.model.ProjectileKind
import com.endermark.bossbattle.model.WeaponType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
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
    private var isFiring: Boolean = false
    private var minionSpawnTimer: Float = MINION_FIRST_SPAWN_DELAY
    private var nextMeleeBossAttackIsCharge: Boolean = false
    private var complaintIndex: Int = 0
    private var pickupSpawnTimer: Float = PICKUP_FIRST_SPAWN_DELAY
    private var pickupSequenceIndex: Int = 0
    private var giantLolloAttackIndex: Int = 0
    private var furiousLaserTimer: Float = FURIOUS_LASER_FIRST_DELAY
    private var furiousLaserToggle: Boolean = false
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

    fun setJoystickInput(dx: Float, dy: Float) {
        val mag = hypot(dx, dy)
        if (mag > 1f) {
            state.joystickX = dx / mag
            state.joystickY = dy / mag
        } else {
            state.joystickX = dx
            state.joystickY = dy
        }
    }

    fun setFiring(firing: Boolean) {
        isFiring = firing
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
        val boost = if (player.hasSpeedBoost) 1.12f else 1f
        player.vy = (if (isFirstJump) JUMP_VELOCITY else DOUBLE_JUMP_VELOCITY) * boost
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
        if (level.enablePistol) {
            player.meleeTimer = 0.12f
            player.meleeCooldown = 0.26f
            player.meleeHitApplied = true
            emitSound(GameSoundEvent.PLAYER_SHOT)
            val dmg = if (player.hasDamageBoost) 20 else 10
            state.projectiles.add(
                Projectile(
                    x = player.x + player.facingDir * 42f,
                    y = player.y - 8f,
                    vx = player.facingDir * 940f,
                    vy = 0f,
                    radius = 12f,
                    fromPlayer = true,
                    damage = dmg,
                    kind = ProjectileKind.PLAYER_BOLT
                )
            )
            return true
        }

        player.meleeTimer = MELEE_DURATION
        player.meleeCooldown = when (player.weapon) {
            WeaponType.KATANA -> 0.18f
            WeaponType.SPEAR -> 0.28f
            WeaponType.HAMMER -> 0.42f
            WeaponType.SWORD -> MELEE_COOLDOWN
        }
        player.meleeHitApplied = false
        emitSound(GameSoundEvent.ATTACK)

        // Spear also fires a horizontal piercing energy wave!
        if (player.weapon == WeaponType.SPEAR) {
            val dmg = if (player.hasDamageBoost) 24 else 12
            state.projectiles.add(
                Projectile(
                    x = player.x + player.facingDir * 44f,
                    y = player.y - 6f,
                    vx = player.facingDir * 860f,
                    vy = 0f,
                    radius = 16f,
                    fromPlayer = true,
                    damage = dmg,
                    kind = ProjectileKind.PLAYER_BOLT
                )
            )
        }
        return true
    }

    fun resetBattle(status: GameStatus = GameStatus.READY) {
        state.status = status
        state.elapsedTime = 0f
        state.endStateCooldown = 0f
        state.projectiles.clear()
        state.particles.clear()
        state.pickups.clear()
        state.platforms.clear()
        state.lasers.clear()
        state.minions.clear()
        state.weakPoints.clear()
        state.turrets.clear()
        state.joystickX = 0f
        state.joystickY = 0f
        state.phaseFlashTimer = 0f
        state.lolloTvHit = false
        state.lolloSofaHit = false
        state.lolloIngolfatoTimer = 0f
        pendingSoundEvents.clear()
        moveInput = 0
        isFiring = false
        minionSpawnTimer = MINION_FIRST_SPAWN_DELAY
        nextMeleeBossAttackIsCharge = false
        complaintIndex = 0
        pickupSpawnTimer = PICKUP_FIRST_SPAWN_DELAY
        pickupSequenceIndex = 0
        giantLolloAttackIndex = 0
        furiousLaserTimer = FURIOUS_LASER_FIRST_DELAY
        furiousLaserToggle = false

        if (level.enablePlatforms) {
            populateInitialPlatforms()
        }
        if (level.enableWeakPoints) {
            populateWeakPoints()
        }
        if (level.enableTurretFortress) {
            populateTurrets()
        }

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
                weapon = WeaponType.SWORD
                weaponTimer = 0f
                speedBoostTimer = 0f
                damageBoostTimer = 0f
            }
            state.boss.apply {
                x = state.worldWidth * 0.76f
                y = state.groundY - radius
                hp = maxHp
                displayedHp = maxHp.toFloat()
                armor = level.bossMaxArmor
                maxArmor = level.bossMaxArmor
                phase = 1
                stunTimer = 0f
                velocityX = -level.bossBaseSpeed
                attackCooldown = 1.2f
                hitFlashTimer = 0f
                action = BossAction.PATROL
                actionTimer = 0f
                facingDir = -1
                vanishTimer = 0f
                hitsSinceVanish = 0
            }
            if (level.enableBoardOfDirectors) {
                state.secondaryBoss = Boss(
                    name = "IVANONE",
                    x = state.worldWidth * 0.78f,
                    y = 785f,
                    radius = 68f,
                    hp = level.bossMaxHp,
                    maxHp = level.bossMaxHp,
                    displayedHp = level.bossMaxHp.toFloat(),
                    phase = 1,
                    velocityX = -level.bossBaseSpeed * 0.9f,
                    attackCooldown = 2.1f
                )
            } else {
                state.secondaryBoss = null
            }
        } else {
            state.player.apply {
                x = state.worldWidth / 2f
                y = if (level.enableTurretFortress) {
                    state.worldHeight * 0.54f
                } else {
                    state.worldHeight - PLAYER_BOTTOM_OFFSET
                }
                hp = maxHp
                invulnerableTimer = 0f
                fireCooldown = if (level.mode == BattleMode.FREE_FLIGHT_SHOOTER) 0f else 0.15f
                vx = 0f
                vy = 0f
                angle = -1.5708f
                onGround = true
                jumpsRemaining = Player.MAX_JUMPS
                facingDir = 1
                meleeTimer = 0f
                meleeCooldown = 0f
                meleeHitApplied = false
                isBlocking = false
                shieldFlashTimer = 0f
                weapon = WeaponType.SWORD
                weaponTimer = 0f
                speedBoostTimer = 0f
                damageBoostTimer = 0f
            }
            state.boss.apply {
                x = if (level.enableBoardOfDirectors) state.worldWidth * 0.28f else state.worldWidth / 2f
                y = when {
                    level.enableTurretFortress -> FORTRESS_BOSS_Y
                    level.enableBoardOfDirectors -> 390f
                    else -> BOSS_TOP_OFFSET
                }
                hp = maxHp
                displayedHp = maxHp.toFloat()
                armor = level.bossMaxArmor
                maxArmor = level.bossMaxArmor
                phase = 1
                stunTimer = 0f
                velocityX = level.bossBaseSpeed
                attackCooldown = 0.9f
                hitFlashTimer = 0f
                action = BossAction.PATROL
                actionTimer = 0f
                facingDir = 1
                vanishTimer = 0f
                hitsSinceVanish = 0
            }
            if (level.enableBoardOfDirectors) {
                state.secondaryBoss = Boss(
                    name = "IVANONE",
                    x = state.worldWidth * 0.72f,
                    y = 390f,
                    radius = 80f,
                    hp = level.bossMaxHp,
                    maxHp = level.bossMaxHp,
                    displayedHp = level.bossMaxHp.toFloat(),
                    phase = 1,
                    attackCooldown = 1.35f
                )
            } else {
                state.secondaryBoss = null
            }
        }
        targetPlayerX = state.player.x
    }

    fun update(dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0f, MAX_DT)
        state.elapsedTime += dt
        if (state.phaseFlashTimer > 0f) {
            state.phaseFlashTimer = max(0f, state.phaseFlashTimer - dt)
        }
        updateParticles(dt)
        updateDisplayedBossHp(dt)

        if (state.status != GameStatus.PLAYING) {
            if (state.endStateCooldown > 0f) {
                state.endStateCooldown = max(0f, state.endStateCooldown - dt)
            }
            return
        }

        if (level.enablePlatforms) {
            updatePlatforms(dt)
        }
        when (level.mode) {
            BattleMode.MELEE_PLATFORMER -> {
                updateMeleePlayer(dt)
                updateMeleeBoss(dt)
            }
            BattleMode.FREE_FLIGHT_SHOOTER -> {
                updateFreeFlightPlayer(dt)
                updateFreeFlightBoss(dt)
                updateMinions(dt)
                updateTurrets(dt)
            }
            BattleMode.SHOOTER -> {
                updateShooterPlayer(dt)
                updateShooterBoss(dt)
            }
        }
        if (level.enablePickups) {
            updatePickups(dt)
        }
        if (level.enableLasers) {
            updateLasers(dt)
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

    private fun updateFreeFlightPlayer(dt: Float) {
        val player = state.player
        val jx = state.joystickX
        val jy = state.joystickY
        val mag = hypot(jx, jy)
        if (mag > 0.12f) {
            player.angle = atan2(jy.toDouble(), jx.toDouble()).toFloat()
            player.vx = jx * FREE_FLIGHT_PLAYER_SPEED
            player.vy = jy * FREE_FLIGHT_PLAYER_SPEED
        } else {
            player.vx = 0f
            player.vy = 0f
        }

        player.x += player.vx * dt
        player.y += player.vy * dt

        val marginX = player.radius + ARENA_PADDING
        val minY = 200f + player.radius
        val maxY = state.worldHeight - player.radius - ARENA_PADDING
        player.x = player.x.coerceIn(marginX, state.worldWidth - marginX)
        player.y = player.y.coerceIn(minY, maxY)

        if (player.invulnerableTimer > 0f) {
            player.invulnerableTimer = max(0f, player.invulnerableTimer - dt)
        }

        if (player.fireCooldown > 0f) {
            player.fireCooldown = max(0f, player.fireCooldown - dt)
        }
        if (isFiring && player.fireCooldown <= 0f) {
            emitSound(GameSoundEvent.PLAYER_SHOT)
            val cosA = cos(player.angle.toDouble()).toFloat()
            val sinA = sin(player.angle.toDouble()).toFloat()
            state.projectiles.add(
                Projectile(
                    x = player.x + cosA * (player.radius + 10f),
                    y = player.y + sinA * (player.radius + 10f),
                    vx = cosA * FREE_FLIGHT_BOLT_SPEED,
                    vy = sinA * FREE_FLIGHT_BOLT_SPEED,
                    radius = 12f,
                    fromPlayer = true,
                    damage = 8,
                    kind = ProjectileKind.PLAYER_BOLT
                )
            )
            player.fireCooldown = FREE_FLIGHT_FIRE_INTERVAL
        }
    }

    private fun updateFreeFlightBoss(dt: Float) {
        val boss = state.boss
        if (boss.hitFlashTimer > 0f) {
            boss.hitFlashTimer = max(0f, boss.hitFlashTimer - dt)
        }
        for (wp in state.weakPoints) {
            if (wp.hitFlashTimer > 0f) {
                wp.hitFlashTimer = max(0f, wp.hitFlashTimer - dt)
            }
        }
        if (boss.stunTimer > 0f) {
            boss.stunTimer = max(0f, boss.stunTimer - dt)
            return
        }

        if (level.enableBoardOfDirectors) {
            updateBoardOfDirectors(dt)
            return
        }

        if (level.enableTurretFortress) {
            // Top fortress mega-turret stays anchored at top center
            boss.x = state.worldWidth * 0.5f
            boss.y = FORTRESS_BOSS_Y
            if (state.hasActiveTurrets) {
                return
            }
            boss.attackCooldown -= dt
            if (boss.attackCooldown <= 0f) {
                spawnMegaTurretSalvo()
                boss.attackCooldown = if (boss.isEnraged) {
                    level.bossEnragedAttackInterval
                } else {
                    level.bossAttackInterval
                }
            }
            return
        }

        if (level.enableGiantLollo) {
            if (state.lolloIngolfatoTimer > 0f) {
                state.lolloIngolfatoTimer = max(0f, state.lolloIngolfatoTimer - dt)
                boss.x = (boss.x + sin(state.elapsedTime * 18f) * 38f * dt)
                    .coerceIn(boss.radius + ARENA_PADDING, state.worldWidth - boss.radius - ARENA_PADDING)
                if (level.enableFuriousLollo && state.lolloIngolfatoTimer <= 0f) {
                    spawnFuriousLolloRageBurst()
                }
                return
            }
            val baseSpeedScale = if (level.enableFuriousLollo) 1.28f else 1.0f
            val speedScale = baseSpeedScale * if (boss.isEnraged) 1.25f else 1.0f
            val t = state.elapsedTime * speedScale
            val targetX = state.worldWidth * 0.5f + sin(t * 0.65f) * 265f
            val targetY = 435f + sin(t * 1.25f) * 95f
            boss.x += (targetX - boss.x) * (3.1f * dt).coerceAtMost(1f)
            boss.y += (targetY - boss.y) * (3.1f * dt).coerceAtMost(1f)

            boss.attackCooldown -= dt
            if (boss.attackCooldown <= 0f) {
                spawnGiantLolloSalvo()
                boss.attackCooldown = if (boss.isEnraged) {
                    level.bossEnragedAttackInterval
                } else {
                    level.bossAttackInterval
                }
            }

            if (level.enableFuriousLollo) {
                furiousLaserTimer -= dt
                if (furiousLaserTimer <= 0f) {
                    spawnFuriousLolloLasers()
                    furiousLaserTimer = if (boss.isEnraged) {
                        FURIOUS_LASER_ENRAGED_INTERVAL
                    } else {
                        FURIOUS_LASER_INTERVAL
                    }
                }
            }
            return
        }

        // Smooth orbital figure-8 flight across upper-central space
        val speedScale = when {
            level.enableTechLeadLollo && boss.phase >= 3 -> 1.48f
            boss.isEnraged -> 1.35f
            else -> 1.0f
        }
        val t = state.elapsedTime * speedScale
        val targetX = state.worldWidth * 0.5f + sin(t * 0.75f) * 290f
        val targetY = 440f + sin(t * 1.5f) * 115f
        boss.x += (targetX - boss.x) * (3.2f * dt).coerceAtMost(1f)
        boss.y += (targetY - boss.y) * (3.2f * dt).coerceAtMost(1f)

        boss.attackCooldown -= dt
        if (boss.attackCooldown <= 0f) {
            if (level.enableTechLeadLollo) {
                spawnTechLeadSalvo()
            } else {
                spawnDreadnoughtSalvo()
            }
            boss.attackCooldown = when {
                level.enableTechLeadLollo && boss.phase >= 3 -> level.bossEnragedAttackInterval * 0.85f
                boss.isEnraged -> level.bossEnragedAttackInterval
                else -> level.bossAttackInterval
            }
        }

        if (level.enableMinions) {
            minionSpawnTimer -= dt
            val maxMinions = if (level.enableTechLeadLollo && boss.phase >= 2) 4 else MAX_ACTIVE_MINIONS
            if (minionSpawnTimer <= 0f && state.minions.size < maxMinions) {
                spawnMinionShip()
                minionSpawnTimer = when {
                    level.enableTechLeadLollo && boss.phase >= 3 -> 3.6f
                    level.enableTechLeadLollo && boss.phase == 2 -> 4.6f
                    level.enableTechLeadLollo -> 5.6f
                    boss.isEnraged -> MINION_ENRAGED_SPAWN_INTERVAL
                    else -> MINION_SPAWN_INTERVAL
                }
            }
        }
    }

    private fun spawnTechLeadSalvo() {
        val boss = state.boss
        val player = state.player
        emitSound(GameSoundEvent.BOSS_ATTACK)
        val speed = if (boss.phase >= 2) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2((player.y - boss.y).toDouble(), (player.x - boss.x).toDouble())

        when (boss.phase) {
            1 -> {
                for (offset in listOf(-0.22, 0.22)) {
                    val angle = baseAngle + offset
                    state.projectiles.add(
                        Projectile(
                            x = boss.x + (cos(angle) * boss.radius * 0.7f).toFloat(),
                            y = boss.y + (sin(angle) * boss.radius * 0.7f).toFloat(),
                            vx = (cos(angle) * speed).toFloat(),
                            vy = (sin(angle) * speed).toFloat(),
                            radius = 24f,
                            fromPlayer = false,
                            damage = 1,
                            kind = ProjectileKind.COMPUTER,
                            label = "PC"
                        )
                    )
                }
            }
            2 -> {
                val label = TECH_LEAD_QUOTES[complaintIndex % TECH_LEAD_QUOTES.size]
                complaintIndex += 1
                state.projectiles.add(
                    Projectile(
                        x = boss.x + (cos(baseAngle) * boss.radius * 0.7f).toFloat(),
                        y = boss.y + (sin(baseAngle) * boss.radius * 0.7f).toFloat(),
                        vx = (cos(baseAngle) * speed * 0.92f).toFloat(),
                        vy = (sin(baseAngle) * speed * 0.92f).toFloat(),
                        radius = 28f,
                        fromPlayer = false,
                        damage = 1,
                        kind = ProjectileKind.COMPLAINT,
                        label = label
                    )
                )
                for (offset in listOf(-0.34, 0.34)) {
                    val angle = baseAngle + offset
                    state.projectiles.add(
                        Projectile(
                            x = boss.x + (cos(angle) * boss.radius * 0.7f).toFloat(),
                            y = boss.y + (sin(angle) * boss.radius * 0.7f).toFloat(),
                            vx = (cos(angle) * speed).toFloat(),
                            vy = (sin(angle) * speed).toFloat(),
                            radius = 24f,
                            fromPlayer = false,
                            damage = 1,
                            kind = ProjectileKind.COMPUTER,
                            label = "PC"
                        )
                    )
                }
            }
            else -> {
                // Phase 3: Fart clouds + flying computers!
                val fartLabels = listOf("PRRRRT!", "PFFFT!", "CHE PUZZA!")
                for ((idx, offset) in listOf(-0.30, 0.0, 0.30).withIndex()) {
                    val angle = baseAngle + offset
                    state.projectiles.add(
                        Projectile(
                            x = boss.x + (cos(angle) * boss.radius * 0.7f).toFloat(),
                            y = boss.y + (sin(angle) * boss.radius * 0.7f).toFloat(),
                            vx = (cos(angle) * speed * 0.85f).toFloat(),
                            vy = (sin(angle) * speed * 0.85f).toFloat(),
                            radius = 34f,
                            fromPlayer = false,
                            damage = 1,
                            kind = ProjectileKind.FART_CLOUD,
                            label = fartLabels[idx % fartLabels.size]
                        )
                    )
                }
            }
        }
    }

    private fun updateBoardOfDirectors(dt: Float) {
        val lollo = state.boss
        val ivanone = state.secondaryBoss
        val t = state.elapsedTime

        // Update Lollo Zurich (left/upper orbital arc; wider & faster if Ivanone is KO)
        if (lollo.hp > 0) {
            val isSuperCeo = ivanone == null || ivanone.hp <= 0
            val speedScale = if (isSuperCeo) 1.38f else 1.0f
            val centerX = if (isSuperCeo) state.worldWidth * 0.5f else state.worldWidth * 0.30f
            val ampX = if (isSuperCeo) 280f else 155f
            val targetX = centerX + sin(t * 0.9f * speedScale) * ampX
            val targetY = 395f + sin(t * 1.6f * speedScale) * 90f
            lollo.x += (targetX - lollo.x) * (3.2f * dt).coerceAtMost(1f)
            lollo.y += (targetY - lollo.y) * (3.2f * dt).coerceAtMost(1f)

            lollo.attackCooldown -= dt
            if (lollo.attackCooldown <= 0f) {
                spawnBoardLolloSalvo(isSuperCeo)
                lollo.attackCooldown = if (isSuperCeo || lollo.isEnraged) {
                    level.bossEnragedAttackInterval
                } else {
                    level.bossAttackInterval
                }
            }
        }

        // Update Ivanone (right/upper mirrored arc + periodic vanish teleport!)
        if (ivanone != null && ivanone.hp > 0) {
            if (ivanone.hitFlashTimer > 0f) {
                ivanone.hitFlashTimer = max(0f, ivanone.hitFlashTimer - dt)
            }
            if (ivanone.vanishTimer > 0f) {
                ivanone.vanishTimer = max(0f, ivanone.vanishTimer - dt)
                if (ivanone.vanishTimer <= 0f) {
                    ivanone.x = if (ivanone.x > state.worldWidth * 0.5f) {
                        state.worldWidth * 0.28f
                    } else {
                        state.worldWidth * 0.72f
                    }
                    ivanone.y = 410f
                    emitSound(GameSoundEvent.BOSS_VANISH)
                    spawnHitParticles(ivanone.x, ivanone.y, COLOR_VANISH_SMOKE, 16)
                }
                return
            }

            val isSuperCeo = lollo.hp <= 0
            val speedScale = if (isSuperCeo) 1.38f else 1.0f
            val centerX = if (isSuperCeo) state.worldWidth * 0.5f else state.worldWidth * 0.70f
            val ampX = if (isSuperCeo) 280f else 155f
            val targetX = centerX - sin(t * 0.9f * speedScale) * ampX
            val targetY = 395f - sin(t * 1.6f * speedScale) * 90f
            ivanone.x += (targetX - ivanone.x) * (3.2f * dt).coerceAtMost(1f)
            ivanone.y += (targetY - ivanone.y) * (3.2f * dt).coerceAtMost(1f)

            ivanone.attackCooldown -= dt
            if (ivanone.attackCooldown <= 0f) {
                spawnBoardIvanoneSalvo(isSuperCeo)
                ivanone.attackCooldown = if (isSuperCeo || ivanone.isEnraged) {
                    level.bossEnragedAttackInterval
                } else {
                    level.bossAttackInterval
                }
            }
        }
    }

    private fun spawnBoardLolloSalvo(isSuperCeo: Boolean) {
        val lollo = state.boss
        val player = state.player
        emitSound(GameSoundEvent.BOSS_ATTACK)
        val speed = if (isSuperCeo) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2((player.y - lollo.y).toDouble(), (player.x - lollo.x).toDouble())
        val label = BOARD_LOLLO_QUOTES[complaintIndex % BOARD_LOLLO_QUOTES.size]
        complaintIndex += 1

        state.projectiles.add(
            Projectile(
                x = lollo.x,
                y = lollo.y + 20f,
                vx = (cos(baseAngle) * speed * 0.92f).toFloat(),
                vy = (sin(baseAngle) * speed * 0.92f).toFloat(),
                radius = 28f,
                fromPlayer = false,
                damage = 1,
                kind = ProjectileKind.COMPLAINT,
                label = label
            )
        )
        val sideOffsets = if (isSuperCeo) listOf(-0.34, 0.34) else listOf(0.26)
        for (offset in sideOffsets) {
            val angle = baseAngle + offset
            state.projectiles.add(
                Projectile(
                    x = lollo.x,
                    y = lollo.y + 18f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 24f,
                    fromPlayer = false,
                    damage = 1,
                    kind = if (isSuperCeo && offset < 0) ProjectileKind.DRONE else ProjectileKind.COMPUTER,
                    label = if (isSuperCeo && offset < 0) null else "PC"
                )
            )
        }
    }

    private fun spawnBoardIvanoneSalvo(isSuperCeo: Boolean) {
        val ivanone = state.secondaryBoss ?: return
        val player = state.player
        emitSound(GameSoundEvent.CAMERA_SHUTTER)
        val speed = if (isSuperCeo) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2((player.y - ivanone.y).toDouble(), (player.x - ivanone.x).toDouble())

        state.projectiles.add(
            Projectile(
                x = ivanone.x,
                y = ivanone.y + 20f,
                vx = (cos(baseAngle) * speed).toFloat(),
                vy = (sin(baseAngle) * speed).toFloat(),
                radius = 26f,
                fromPlayer = false,
                damage = 1,
                kind = ProjectileKind.CAMERA_FLASH
            )
        )
        val sideOffsets = if (isSuperCeo) listOf(-0.32, 0.32) else listOf(-0.26)
        for (offset in sideOffsets) {
            val angle = baseAngle + offset
            state.projectiles.add(
                Projectile(
                    x = ivanone.x,
                    y = ivanone.y + 18f,
                    vx = (cos(angle) * speed * 0.95f).toFloat(),
                    vy = (sin(angle) * speed * 0.95f).toFloat(),
                    radius = 24f,
                    fromPlayer = false,
                    damage = 1,
                    kind = if (isSuperCeo && offset > 0) ProjectileKind.COMPUTER else ProjectileKind.DRONE
                )
            )
        }
    }

    private fun spawnGiantLolloSalvo() {
        val boss = state.boss
        val player = state.player
        emitSound(GameSoundEvent.BOSS_ATTACK)
        val speed = if (boss.isEnraged) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2((player.y - boss.y).toDouble(), (player.x - boss.x).toDouble())
        val step = giantLolloAttackIndex % 3
        giantLolloAttackIndex += 1

        when (step) {
            0 -> {
                // Aimed TV (reflectable!) + flanking Computer
                state.projectiles.add(
                    Projectile(
                        x = boss.x + (cos(baseAngle) * boss.radius * 0.72f).toFloat(),
                        y = boss.y + (sin(baseAngle) * boss.radius * 0.72f).toFloat(),
                        vx = (cos(baseAngle) * speed * 0.82f).toFloat(),
                        vy = (sin(baseAngle) * speed * 0.82f).toFloat(),
                        radius = 34f,
                        fromPlayer = false,
                        damage = 1,
                        kind = ProjectileKind.TV,
                        label = "TV"
                    )
                )
                val sideAngle = baseAngle + 0.34
                state.projectiles.add(
                    Projectile(
                        x = boss.x + (cos(sideAngle) * boss.radius * 0.72f).toFloat(),
                        y = boss.y + (sin(sideAngle) * boss.radius * 0.72f).toFloat(),
                        vx = (cos(sideAngle) * speed).toFloat(),
                        vy = (sin(sideAngle) * speed).toFloat(),
                        radius = 26f,
                        fromPlayer = false,
                        damage = 1,
                        kind = ProjectileKind.COMPUTER,
                        label = "PC"
                    )
                )
            }
            1 -> {
                // Aimed Sofa (reflectable!) + flanking Computer
                state.projectiles.add(
                    Projectile(
                        x = boss.x + (cos(baseAngle) * boss.radius * 0.72f).toFloat(),
                        y = boss.y + (sin(baseAngle) * boss.radius * 0.72f).toFloat(),
                        vx = (cos(baseAngle) * speed * 0.80f).toFloat(),
                        vy = (sin(baseAngle) * speed * 0.80f).toFloat(),
                        radius = 38f,
                        fromPlayer = false,
                        damage = 1,
                        kind = ProjectileKind.SOFA,
                        label = "DIVANO"
                    )
                )
                val sideAngle = baseAngle - 0.34
                state.projectiles.add(
                    Projectile(
                        x = boss.x + (cos(sideAngle) * boss.radius * 0.72f).toFloat(),
                        y = boss.y + (sin(sideAngle) * boss.radius * 0.72f).toFloat(),
                        vx = (cos(sideAngle) * speed).toFloat(),
                        vy = (sin(sideAngle) * speed).toFloat(),
                        radius = 26f,
                        fromPlayer = false,
                        damage = 1,
                        kind = ProjectileKind.COMPUTER,
                        label = "PC"
                    )
                )
            }
            else -> {
                // Spread of flying Computers (denser when Furious!)
                val offsets = when {
                    level.enableFuriousLollo && boss.isEnraged -> listOf(-0.48, -0.16, 0.16, 0.48)
                    level.enableFuriousLollo || boss.isEnraged -> listOf(-0.36, 0.0, 0.36)
                    else -> listOf(-0.24, 0.24)
                }
                for (offset in offsets) {
                    val angle = baseAngle + offset
                    state.projectiles.add(
                        Projectile(
                            x = boss.x + (cos(angle) * boss.radius * 0.72f).toFloat(),
                            y = boss.y + (sin(angle) * boss.radius * 0.72f).toFloat(),
                            vx = (cos(angle) * speed * 1.05f).toFloat(),
                            vy = (sin(angle) * speed * 1.05f).toFloat(),
                            radius = 26f,
                            fromPlayer = false,
                            damage = 1,
                            kind = ProjectileKind.COMPUTER,
                            label = "PC"
                        )
                    )
                }
            }
        }
    }

    private fun spawnFuriousLolloLasers() {
        val player = state.player
        val boss = state.boss
        furiousLaserToggle = !furiousLaserToggle
        val telegraph = if (boss.isEnraged) 0.80f else 0.95f
        val active = 0.52f
        val clampedY = player.y.coerceIn(260f, state.worldHeight - 240f)
        val clampedX = player.x.coerceIn(110f, state.worldWidth - 110f)

        if (boss.isEnraged) {
            // Cross laser (horizontal + vertical) when below 50% HP!
            state.lasers.add(
                LaserBeam(
                    isHorizontal = true,
                    position = clampedY,
                    thickness = 42f,
                    telegraphTimer = telegraph,
                    activeTimer = active
                )
            )
            state.lasers.add(
                LaserBeam(
                    isHorizontal = false,
                    position = clampedX,
                    thickness = 42f,
                    telegraphTimer = telegraph,
                    activeTimer = active
                )
            )
        } else {
            state.lasers.add(
                LaserBeam(
                    isHorizontal = furiousLaserToggle,
                    position = if (furiousLaserToggle) clampedY else clampedX,
                    thickness = 44f,
                    telegraphTimer = telegraph,
                    activeTimer = active
                )
            )
        }
    }

    private fun spawnFuriousLolloRageBurst() {
        val boss = state.boss
        state.phaseFlashTimer = 0.45f
        emitSound(GameSoundEvent.BOSS_CHARGE)
        spawnHitParticles(boss.x, boss.y, COLOR_BOSS_HIT_SPARK, 28)
        val count = 8
        val speed = level.bossProjectileSpeed * 0.92f
        for (i in 0 until count) {
            val angle = (2.0 * PI * i) / count
            state.projectiles.add(
                Projectile(
                    x = boss.x + (cos(angle) * boss.radius * 0.85f).toFloat(),
                    y = boss.y + (sin(angle) * boss.radius * 0.85f).toFloat(),
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 20f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.BOSS_ORB
                )
            )
        }
    }

    private fun spawnMegaTurretSalvo() {
        val boss = state.boss
        val player = state.player
        emitSound(GameSoundEvent.BOSS_ATTACK)
        val speed = if (boss.isEnraged) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2((player.y - boss.y).toDouble(), (player.x - boss.x).toDouble())
        val offsets = if (boss.isEnraged) {
            listOf(-0.48, -0.32, -0.16, 0.0, 0.16, 0.32, 0.48)
        } else {
            listOf(-0.32, -0.16, 0.0, 0.16, 0.32)
        }
        for (offset in offsets) {
            val angle = baseAngle + offset
            state.projectiles.add(
                Projectile(
                    x = boss.x + (cos(angle) * boss.radius * 0.8f).toFloat(),
                    y = boss.y + (sin(angle) * boss.radius * 0.8f).toFloat(),
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

    private fun updateTurrets(dt: Float) {
        if (state.turrets.isEmpty()) return
        val player = state.player
        for (turret in state.turrets) {
            if (turret.isDestroyed) continue
            if (turret.hitFlashTimer > 0f) {
                turret.hitFlashTimer = max(0f, turret.hitFlashTimer - dt)
            }
            turret.angle = atan2((player.y - turret.y).toDouble(), (player.x - turret.x).toDouble()).toFloat()
            turret.fireCooldown -= dt
            if (turret.fireCooldown <= 0f) {
                val cosA = cos(turret.angle.toDouble()).toFloat()
                val sinA = sin(turret.angle.toDouble()).toFloat()
                state.projectiles.add(
                    Projectile(
                        x = turret.x + cosA * (turret.radius + 8f),
                        y = turret.y + sinA * (turret.radius + 8f),
                        vx = cosA * TURRET_BOLT_SPEED,
                        vy = sinA * TURRET_BOLT_SPEED,
                        radius = 13f,
                        fromPlayer = false,
                        damage = 1,
                        kind = ProjectileKind.BOSS_ORB
                    )
                )
                turret.fireCooldown = TURRET_FIRE_INTERVAL
            }
        }
    }

    private fun spawnDreadnoughtSalvo() {
        val boss = state.boss
        val player = state.player
        emitSound(GameSoundEvent.BOSS_ATTACK)
        val speed = if (boss.isEnraged) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2((player.y - boss.y).toDouble(), (player.x - boss.x).toDouble())
        val angles = if (boss.isEnraged) {
            listOf(baseAngle - 0.36, baseAngle - 0.18, baseAngle, baseAngle + 0.18, baseAngle + 0.36)
        } else {
            listOf(baseAngle - 0.22, baseAngle, baseAngle + 0.22)
        }
        for (angle in angles) {
            state.projectiles.add(
                Projectile(
                    x = boss.x + (cos(angle) * boss.radius * 0.7f).toFloat(),
                    y = boss.y + (sin(angle) * boss.radius * 0.7f).toFloat(),
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

    private fun spawnMinionShip() {
        val boss = state.boss
        val side = if (state.minions.size % 2 == 0) -1f else 1f
        val mx = (boss.x + side * (boss.radius + 32f)).coerceIn(80f, state.worldWidth - 80f)
        val my = (boss.y + 40f).coerceIn(240f, state.worldHeight - 240f)
        emitSound(GameSoundEvent.BOSS_CHARGE)
        spawnHitParticles(mx, my, COLOR_DOUBLE_JUMP_SPARK, 10)
        state.minions.add(
            MinionShip(
                x = mx,
                y = my,
                fireCooldown = 1.2f
            )
        )
    }

    private fun updateMinions(dt: Float) {
        val player = state.player
        for (minion in state.minions) {
            if (minion.hitFlashTimer > 0f) {
                minion.hitFlashTimer = max(0f, minion.hitFlashTimer - dt)
            }
            val dx = player.x - minion.x
            val dy = player.y - minion.y
            val dist = hypot(dx, dy).coerceAtLeast(1f)
            minion.angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()

            if (level.enableTechLeadLollo) {
                // Kamikaze programmer minions rush straight at the player!
                val kamikazeSpeed = if (state.boss.phase >= 3) 295f else 255f
                minion.vx = (dx / dist) * kamikazeSpeed
                minion.vy = (dy / dist) * kamikazeSpeed
                minion.x = (minion.x + minion.vx * dt).coerceIn(minion.radius + 20f, state.worldWidth - minion.radius - 20f)
                minion.y = (minion.y + minion.vy * dt).coerceIn(210f, state.worldHeight - minion.radius - 20f)
                continue
            }

            // Approach player smoothly and orbit at mid-range
            val speed = MINION_SPEED
            if (dist > 170f) {
                minion.vx = (dx / dist) * speed
                minion.vy = (dy / dist) * speed
            } else {
                // Orbit tangentially
                minion.vx = (-dy / dist) * speed * 0.75f
                minion.vy = (dx / dist) * speed * 0.75f
            }
            minion.x = (minion.x + minion.vx * dt).coerceIn(minion.radius + 20f, state.worldWidth - minion.radius - 20f)
            minion.y = (minion.y + minion.vy * dt).coerceIn(210f, state.worldHeight - minion.radius - 20f)

            minion.fireCooldown -= dt
            if (minion.fireCooldown <= 0f) {
                state.projectiles.add(
                    Projectile(
                        x = minion.x + cos(minion.angle.toDouble()).toFloat() * (minion.radius + 6f),
                        y = minion.y + sin(minion.angle.toDouble()).toFloat() * (minion.radius + 6f),
                        vx = cos(minion.angle.toDouble()).toFloat() * MINION_BOLT_SPEED,
                        vy = sin(minion.angle.toDouble()).toFloat() * MINION_BOLT_SPEED,
                        radius = 12f,
                        fromPlayer = false,
                        damage = 1,
                        kind = ProjectileKind.BOSS_ORB
                    )
                )
                minion.fireCooldown = MINION_FIRE_INTERVAL
            }
        }
    }

    private fun updateMeleePlayer(dt: Float) {
        val player = state.player
        val shieldFactor = if (player.isBlocking) SHIELD_MOVE_MULTIPLIER else 1f
        val speedFactor = if (player.hasSpeedBoost) SPEED_BOOST_MULTIPLIER else 1f
        player.vx = moveInput * PLATFORMER_MOVE_SPEED * shieldFactor * speedFactor
        if (moveInput != 0) {
            player.facingDir = moveInput
        }

        val prevBottom = player.y + player.radius
        player.vy += GRAVITY * dt
        player.x += player.vx * dt
        player.y += player.vy * dt

        val margin = player.radius + ARENA_PADDING
        var landedOnPlatform = false

        // One-way landing on moving platforms when falling
        if (player.vy >= 0f && state.platforms.isNotEmpty()) {
            val newBottom = player.y + player.radius
            for (platform in state.platforms) {
                val halfW = platform.width / 2f
                val withinX = player.x + player.radius * 0.5f >= platform.x - halfW &&
                    player.x - player.radius * 0.5f <= platform.x + halfW
                if (withinX && prevBottom <= platform.y + 14f && newBottom >= platform.y) {
                    player.y = platform.y - player.radius
                    player.x += platform.vx * dt
                    player.vy = 0f
                    player.onGround = true
                    player.jumpsRemaining = Player.MAX_JUMPS
                    landedOnPlatform = true
                    break
                }
            }
        }

        player.x = player.x.coerceIn(margin, state.worldWidth - margin)

        if (!landedOnPlatform) {
            val floorY = state.groundY - player.radius
            if (player.y >= floorY) {
                player.y = floorY
                player.vy = 0f
                player.onGround = true
                player.jumpsRemaining = Player.MAX_JUMPS
            } else {
                player.onGround = false
            }
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
        if (player.weaponTimer > 0f) {
            player.weaponTimer = max(0f, player.weaponTimer - dt)
            if (player.weaponTimer <= 0f) {
                player.weapon = WeaponType.SWORD
            }
        }
        if (player.speedBoostTimer > 0f) {
            player.speedBoostTimer = max(0f, player.speedBoostTimer - dt)
        }
        if (player.damageBoostTimer > 0f) {
            player.damageBoostTimer = max(0f, player.damageBoostTimer - dt)
        }
    }

    private fun updateShooterBoss(dt: Float) {
        val boss = state.boss
        val player = state.player
        if (boss.hitFlashTimer > 0f) {
            boss.hitFlashTimer = max(0f, boss.hitFlashTimer - dt)
        }

        // Armor Break stun window (Level 19 Caramel)
        if (boss.stunTimer > 0f) {
            boss.stunTimer = max(0f, boss.stunTimer - dt)
            return
        }

        val minX = boss.radius + ARENA_PADDING
        val maxX = state.worldWidth - boss.radius - ARENA_PADDING

        // Vanish & reappear across the top (Level 13 Ivanone)
        if (boss.vanishTimer > 0f) {
            boss.vanishTimer = max(0f, boss.vanishTimer - dt)
            if (boss.vanishTimer <= 0f) {
                boss.x = if (player.x < state.worldWidth / 2f) {
                    (state.worldWidth * 0.78f).coerceIn(minX, maxX)
                } else {
                    (state.worldWidth * 0.22f).coerceIn(minX, maxX)
                }
                boss.attackCooldown = 0.55f
                spawnHitParticles(boss.x, boss.y, COLOR_VANISH_SMOKE, 12)
            }
            return
        }

        val speedMultiplier = if (boss.isEnraged) BOSS_ENRAGED_SPEED_MULTIPLIER else 1f
        if (level.arenaTheme == ArenaTheme.PARIS && abs(player.x - boss.x) < 120f) {
            // Louise dodges away from player's vertical line of fire!
            val awayDir = if (player.x >= boss.x) -1f else 1f
            boss.velocityX = awayDir * abs(boss.velocityX)
        }
        boss.x += boss.velocityX * speedMultiplier * dt
        boss.facingDir = if (boss.velocityX >= 0f) 1 else -1

        if (boss.x <= minX) {
            boss.x = minX
            boss.velocityX = abs(boss.velocityX)
        } else if (boss.x >= maxX) {
            boss.x = maxX
            boss.velocityX = -abs(boss.velocityX)
        }

        boss.attackCooldown -= dt
        if (boss.attackCooldown <= 0f) {
            spawnThemedShooterBossAttack()
            boss.attackCooldown = if (boss.isEnraged) {
                level.bossEnragedAttackInterval
            } else {
                level.bossAttackInterval
            }
        }
    }

    private fun spawnThemedShooterBossAttack() {
        when (level.arenaTheme) {
            ArenaTheme.ZURICH -> {
                emitSound(GameSoundEvent.BOSS_ATTACK)
                spawnLolloComplaintAttack()
            }
            ArenaTheme.STUDIO -> {
                if (nextMeleeBossAttackIsCharge) {
                    emitSound(GameSoundEvent.CAMERA_SHUTTER)
                    spawnIvanoneCameraAttack()
                    triggerBossVanish()
                } else {
                    emitSound(GameSoundEvent.BOSS_ATTACK)
                    spawnIvanoneShooterDroneAttack()
                }
                nextMeleeBossAttackIsCharge = !nextMeleeBossAttackIsCharge
            }
            ArenaTheme.ZURICH_TAX -> {
                emitSound(GameSoundEvent.BOSS_ATTACK)
                if (nextMeleeBossAttackIsCharge) {
                    spawnAlessioWeirdToothAttack()
                } else {
                    spawnAlessioTaxAttack()
                }
                nextMeleeBossAttackIsCharge = !nextMeleeBossAttackIsCharge
            }
            ArenaTheme.PARIS -> {
                emitSound(GameSoundEvent.BOSS_ATTACK)
                spawnLouiseSeductionAttack()
            }
            ArenaTheme.UBS_ALPS -> {
                emitSound(GameSoundEvent.BOSS_ATTACK)
                spawnDaisyScreamAttack()
            }
            ArenaTheme.GYM_DEV -> {
                emitSound(GameSoundEvent.BOSS_ATTACK)
                spawnAndreaRiceAndDevAttack()
            }
            ArenaTheme.GOOGLE_ZURICH -> {
                spawnMarcoPorscheAttack()
            }
            ArenaTheme.HANOI_SPA -> {
                emitSound(GameSoundEvent.BOSS_ATTACK)
                spawnCaramelSpaAttack()
            }
            else -> spawnBossAttack()
        }
    }

    private fun spawnIvanoneShooterDroneAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.isEnraged) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val angles = if (boss.isEnraged) {
            listOf(baseAngle - 0.20, baseAngle, baseAngle + 0.20)
        } else {
            listOf(baseAngle - 0.12, baseAngle + 0.12)
        }
        spawnHitParticles(boss.x, boss.y + 20f, COLOR_PLAYER_HIT_SPARK, 8)
        for (angle in angles) {
            state.projectiles.add(
                Projectile(
                    x = boss.x,
                    y = boss.y + boss.radius * 0.5f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 22f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.DRONE
                )
            )
        }
    }

    private fun updatePlatformBoardOfDirectors(dt: Float) {
        val lollo = state.boss
        val ivanone = state.secondaryBoss
        val player = state.player

        // Synchronize 3 phases based on combined Board HP (Phase 1 > 66%, Phase 2 33-66%, Phase 3 <= 33%)
        val totalMax = (lollo.maxHp + (ivanone?.maxHp ?: 0)).coerceAtLeast(1)
        val totalHp = lollo.hp + (ivanone?.hp ?: 0)
        val combinedRatio = totalHp.toFloat() / totalMax.toFloat()
        val currentPhase = max(lollo.phase, ivanone?.phase ?: 1)
        val targetPhase = when {
            combinedRatio <= 0.33f -> 3
            combinedRatio <= 0.66f -> 2
            else -> 1
        }
        if (targetPhase > currentPhase) {
            lollo.phase = targetPhase
            if (ivanone != null) ivanone.phase = targetPhase
            state.phaseFlashTimer = if (targetPhase >= 3) 0.60f else 0.45f
            emitSound(GameSoundEvent.BOSS_CHARGE)
            spawnHitParticles(lollo.x, lollo.y, COLOR_SHIELD_BLOCK_SPARK, 18)
            if (ivanone != null && ivanone.hp > 0) {
                spawnHitParticles(ivanone.x, ivanone.y, COLOR_SHIELD_BLOCK_SPARK, 18)
            }
        }
        val phase = max(lollo.phase, ivanone?.phase ?: 1)
        val phaseInterval = when (phase) {
            1 -> level.bossAttackInterval
            2 -> (level.bossAttackInterval + level.bossEnragedAttackInterval) * 0.5f
            else -> level.bossEnragedAttackInterval
        }
        val projSpeed = when (phase) {
            1 -> level.bossProjectileSpeed
            2 -> (level.bossProjectileSpeed + level.bossEnragedProjectileSpeed) * 0.5f
            else -> level.bossEnragedProjectileSpeed
        }

        // Lollo Zurich patrols smoothly on the ground
        if (lollo.hp > 0) {
            val minX = lollo.radius + ARENA_PADDING
            val maxX = state.worldWidth - lollo.radius - ARENA_PADDING
            lollo.y = state.groundY - lollo.radius
            val speedMult = if (phase >= 3 || (ivanone != null && ivanone.hp <= 0)) 1.30f else if (phase == 2) 1.12f else 1.0f
            lollo.x += lollo.velocityX * speedMult * dt
            if (lollo.x <= minX) {
                lollo.x = minX
                lollo.velocityX = abs(level.bossBaseSpeed)
            } else if (lollo.x >= maxX) {
                lollo.x = maxX
                lollo.velocityX = -abs(level.bossBaseSpeed)
            }
            lollo.facingDir = if (player.x >= lollo.x) 1 else -1

            lollo.attackCooldown -= dt
            if (lollo.attackCooldown <= 0f) {
                emitSound(GameSoundEvent.BOSS_ATTACK)
                val baseAngle = atan2((player.y - lollo.y).toDouble(), (player.x - lollo.x).toDouble())
                val label = BOARD_LOLLO_QUOTES[complaintIndex % BOARD_LOLLO_QUOTES.size]
                complaintIndex += 1
                state.projectiles.add(
                    Projectile(
                        x = lollo.x,
                        y = lollo.y - 16f,
                        vx = (cos(baseAngle) * projSpeed * 0.92f).toFloat(),
                        vy = (sin(baseAngle) * projSpeed * 0.92f).toFloat(),
                        radius = 26f,
                        fromPlayer = false,
                        damage = 1,
                        kind = ProjectileKind.COMPLAINT,
                        label = label
                    )
                )
                if (phase >= 2) {
                    // Ground shockwave that player jumps over onto platforms!
                    state.projectiles.add(
                        Projectile(
                            x = lollo.x,
                            y = state.groundY - 26f,
                            vx = lollo.facingDir * projSpeed * 0.85f,
                            vy = 0f,
                            radius = 26f,
                            fromPlayer = false,
                            damage = 1,
                            kind = ProjectileKind.SHOCKWAVE
                        )
                    )
                }
                if (phase >= 3) {
                    state.projectiles.add(
                        Projectile(
                            x = lollo.x,
                            y = lollo.y - 24f,
                            vx = (cos(baseAngle - 0.22) * projSpeed).toFloat(),
                            vy = (sin(baseAngle - 0.22) * projSpeed).toFloat(),
                            radius = 22f,
                            fromPlayer = false,
                            damage = 1,
                            kind = ProjectileKind.COMPUTER,
                            label = "PC"
                        )
                    )
                }
                lollo.attackCooldown = phaseInterval
            }
        }

        // Ivanone patrols upper platform lane and vanishes every 4 hits
        if (ivanone != null && ivanone.hp > 0) {
            if (ivanone.hitFlashTimer > 0f) {
                ivanone.hitFlashTimer = max(0f, ivanone.hitFlashTimer - dt)
            }
            val minX = ivanone.radius + ARENA_PADDING
            val maxX = state.worldWidth - ivanone.radius - ARENA_PADDING
            if (ivanone.vanishTimer > 0f) {
                ivanone.vanishTimer = max(0f, ivanone.vanishTimer - dt)
                if (ivanone.vanishTimer <= 0f) {
                    ivanone.x = if (player.x < state.worldWidth * 0.5f) {
                        (state.worldWidth * 0.80f).coerceIn(minX, maxX)
                    } else {
                        (state.worldWidth * 0.20f).coerceIn(minX, maxX)
                    }
                    ivanone.y = 785f
                    emitSound(GameSoundEvent.BOSS_VANISH)
                    spawnHitParticles(ivanone.x, ivanone.y, COLOR_VANISH_SMOKE, 16)
                }
                return
            }

            ivanone.y = 785f + sin(state.elapsedTime * 1.8f) * 26f
            val speedMult = if (phase >= 3 || lollo.hp <= 0) 1.30f else if (phase == 2) 1.12f else 1.0f
            ivanone.x += ivanone.velocityX * speedMult * dt
            if (ivanone.x <= minX) {
                ivanone.x = minX
                ivanone.velocityX = abs(level.bossBaseSpeed * 0.9f)
            } else if (ivanone.x >= maxX) {
                ivanone.x = maxX
                ivanone.velocityX = -abs(level.bossBaseSpeed * 0.9f)
            }
            ivanone.facingDir = if (player.x >= ivanone.x) 1 else -1

            ivanone.attackCooldown -= dt
            if (ivanone.attackCooldown <= 0f) {
                emitSound(GameSoundEvent.CAMERA_SHUTTER)
                val baseAngle = atan2((player.y - ivanone.y).toDouble(), (player.x - ivanone.x).toDouble())
                state.projectiles.add(
                    Projectile(
                        x = ivanone.x,
                        y = ivanone.y,
                        vx = (cos(baseAngle) * projSpeed).toFloat(),
                        vy = (sin(baseAngle) * projSpeed).toFloat(),
                        radius = 24f,
                        fromPlayer = false,
                        damage = 1,
                        kind = ProjectileKind.CAMERA_FLASH
                    )
                )
                if (phase >= 2) {
                    val offsets = if (phase >= 3) listOf(-0.28, 0.28) else listOf(-0.24)
                    for (offset in offsets) {
                        val a = baseAngle + offset
                        state.projectiles.add(
                            Projectile(
                                x = ivanone.x,
                                y = ivanone.y,
                                vx = (cos(a) * projSpeed * 0.92f).toFloat(),
                                vy = (sin(a) * projSpeed * 0.92f).toFloat(),
                                radius = 22f,
                                fromPlayer = false,
                                damage = 1,
                                kind = ProjectileKind.DRONE
                            )
                        )
                    }
                }
                ivanone.attackCooldown = phaseInterval * 1.08f
            }
        }
    }

    private fun updateMeleeBoss(dt: Float) {
        val boss = state.boss
        val player = state.player
        if (boss.hitFlashTimer > 0f) {
            boss.hitFlashTimer = max(0f, boss.hitFlashTimer - dt)
        }
        if (level.enableBoardOfDirectors) {
            updatePlatformBoardOfDirectors(dt)
            return
        }

        val minX = boss.radius + ARENA_PADDING
        val maxX = state.worldWidth - boss.radius - ARENA_PADDING
        boss.y = state.groundY - boss.radius

        // Armor Break stun window: boss cannot move or attack while stunned!
        if (boss.stunTimer > 0f) {
            boss.stunTimer = max(0f, boss.stunTimer - dt)
            return
        }

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
                        ArenaTheme.HANOI -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnChanLanternAttack()
                        }
                        ArenaTheme.ZURICH -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnLolloComplaintAttack()
                        }
                        ArenaTheme.STUDIO -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnIvanoneDroneAttack()
                        }
                        ArenaTheme.ZURICH_TAX -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnAlessioTaxAttack()
                        }
                        ArenaTheme.PARIS -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnLouiseSeductionAttack()
                        }
                        ArenaTheme.UBS_ALPS -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnDaisyScreamAttack()
                        }
                        ArenaTheme.GYM_DEV -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnAndreaRiceAndDevAttack()
                        }
                        ArenaTheme.GOOGLE_ZURICH -> spawnMarcoPorscheAttack()
                        ArenaTheme.HANOI_SPA -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnCaramelSpaAttack()
                        }
                        ArenaTheme.OMEGA_FINALE -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnOmegaOrbAndLaserAttack()
                        }
                        else -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnGroundShockwaves()
                        }
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
                            emitSound(GameSoundEvent.CAMERA_SHUTTER)
                            spawnIvanoneCameraAttack()
                            triggerBossVanish()
                        }
                        ArenaTheme.ZURICH_TAX -> {
                            emitSound(GameSoundEvent.BOSS_ATTACK)
                            spawnAlessioWeirdToothAttack()
                            boss.action = BossAction.PATROL
                            boss.attackCooldown = if (boss.isEnraged) {
                                level.bossEnragedAttackInterval
                            } else {
                                level.bossAttackInterval
                            }
                        }
                        ArenaTheme.PARIS -> {
                            emitSound(GameSoundEvent.BOSS_CHARGE)
                            spawnLouiseTeaseHeart()
                            val awayDir = if (player.x >= boss.x) -1 else 1
                            val nearEdge = (awayDir < 0 && boss.x < state.worldWidth * 0.30f) ||
                                (awayDir > 0 && boss.x > state.worldWidth * 0.70f)
                            boss.facingDir = if (nearEdge) -awayDir else awayDir
                            boss.action = BossAction.CHARGING
                            boss.actionTimer = 0.52f
                        }
                        else -> {
                            emitSound(GameSoundEvent.BOSS_CHARGE)
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
        emitSound(GameSoundEvent.BOSS_VANISH)
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
                val reach = when (player.weapon) {
                    WeaponType.SPEAR -> 96f
                    WeaponType.HAMMER -> 78f
                    WeaponType.KATANA -> 78f
                    WeaponType.SWORD -> MELEE_REACH_OFFSET
                }
                val hitRadius = when (player.weapon) {
                    WeaponType.HAMMER -> 104f
                    WeaponType.KATANA -> 88f
                    else -> MELEE_HIT_RADIUS
                }
                val strikeX = player.x + player.facingDir * reach
                val strikeY = player.y
                if (circlesCollide(strikeX, strikeY, hitRadius, boss.x, boss.y, boss.radius)) {
                    player.meleeHitApplied = true
                    val baseDmg = when (player.weapon) {
                        WeaponType.KATANA -> 18
                        WeaponType.HAMMER -> 14
                        WeaponType.SPEAR -> 14
                        WeaponType.SWORD -> MELEE_DAMAGE
                    }
                    val dmg = if (player.hasDamageBoost) baseDmg * 2 else baseDmg
                    val armorBonus = if (player.weapon == WeaponType.HAMMER) 2 else 1
                    applyDamageToBoss(dmg, strikeX, strikeY, armorMultiplier = armorBonus)
                    if (state.status != GameStatus.PLAYING) {
                        return
                    }
                }
            }

            // Boss body contact vs Player (skip if boss is stunned!)
            if (!boss.isVanished && !boss.isStunned && !player.isInvulnerable && circlesCollide(
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

        // Level 24 Giant Lollo: player bolts bounce TV and Sofa back at Lollo's giant bald head!
        if (level.enableGiantLollo) {
            val boltsToRemove = mutableListOf<Projectile>()
            for (bolt in state.projectiles) {
                if (!bolt.fromPlayer || bolt.kind != ProjectileKind.PLAYER_BOLT) continue
                for (furniture in state.projectiles) {
                    if (furniture.fromPlayer || !furniture.isReflectableFurniture) continue
                    if (circlesCollide(bolt.x, bolt.y, bolt.radius, furniture.x, furniture.y, furniture.radius)) {
                        boltsToRemove.add(bolt)
                        furniture.fromPlayer = true
                        val angleToBoss = atan2((boss.y - furniture.y).toDouble(), (boss.x - furniture.x).toDouble())
                        furniture.vx = (cos(angleToBoss) * REFLECTED_FURNITURE_SPEED).toFloat()
                        furniture.vy = (sin(angleToBoss) * REFLECTED_FURNITURE_SPEED).toFloat()
                        emitSound(GameSoundEvent.SHIELD_BLOCK)
                        spawnHitParticles(furniture.x, furniture.y, COLOR_SHIELD_BLOCK_SPARK, 12)
                        break
                    }
                }
            }
            if (boltsToRemove.isNotEmpty()) {
                state.projectiles.removeAll(boltsToRemove)
            }
        }

        val iterator = state.projectiles.iterator()
        while (iterator.hasNext()) {
            val projectile = iterator.next()
            if (projectile.fromPlayer) {
                var hitMinion = false
                val minionIter = state.minions.iterator()
                while (minionIter.hasNext()) {
                    val minion = minionIter.next()
                    if (circlesCollide(
                            projectile.x, projectile.y, projectile.radius,
                            minion.x, minion.y, minion.radius
                        )
                    ) {
                        hitMinion = true
                        iterator.remove()
                        minion.hp -= projectile.damage
                        minion.hitFlashTimer = 0.08f
                        if (minion.hp <= 0) {
                            minionIter.remove()
                            emitSound(GameSoundEvent.BOSS_HIT)
                            spawnHitParticles(minion.x, minion.y, COLOR_VICTORY_SPARK, 14)
                        } else {
                            emitSound(GameSoundEvent.BOSS_HIT)
                            spawnHitParticles(projectile.x, projectile.y, COLOR_BOSS_HIT_SPARK, 6)
                        }
                        break
                    }
                }
                if (hitMinion) continue

                // Check destructible weak points (Level 22 Aegis Dreadnought)
                var hitWeakPoint = false
                for (wp in state.weakPoints) {
                    if (wp.isDestroyed) continue
                    val wpX = boss.x + wp.offsetX
                    val wpY = boss.y + wp.offsetY
                    if (circlesCollide(projectile.x, projectile.y, projectile.radius, wpX, wpY, wp.radius)) {
                        hitWeakPoint = true
                        iterator.remove()
                        wp.hp = max(0, wp.hp - projectile.damage)
                        wp.hitFlashTimer = 0.09f
                        if (wp.isDestroyed) {
                            emitSound(GameSoundEvent.ARMOR_BREAK)
                            spawnHitParticles(wpX, wpY, COLOR_VICTORY_SPARK, 18)
                            if (!state.hasActiveWeakPoints) {
                                state.phaseFlashTimer = 0.50f
                                boss.stunTimer = 1.4f
                                spawnHitParticles(boss.x, boss.y, COLOR_SHIELD_BLOCK_SPARK, 24)
                            }
                        } else {
                            emitSound(GameSoundEvent.BOSS_HIT)
                            spawnHitParticles(projectile.x, projectile.y, COLOR_BOSS_HIT_SPARK, 8)
                        }
                        break
                    }
                }
                if (hitWeakPoint) continue

                // Check perimeter turrets (Level 23 Turret Fortress)
                var hitTurret = false
                for (turret in state.turrets) {
                    if (turret.isDestroyed) continue
                    if (circlesCollide(projectile.x, projectile.y, projectile.radius, turret.x, turret.y, turret.radius)) {
                        hitTurret = true
                        iterator.remove()
                        turret.hp = max(0, turret.hp - projectile.damage)
                        turret.hitFlashTimer = 0.09f
                        if (turret.isDestroyed) {
                            emitSound(GameSoundEvent.ARMOR_BREAK)
                            spawnHitParticles(turret.x, turret.y, COLOR_VICTORY_SPARK, 16)
                            if (!state.hasActiveTurrets) {
                                state.phaseFlashTimer = 0.60f
                                boss.attackCooldown = 0.9f
                                emitSound(GameSoundEvent.BOSS_CHARGE)
                                spawnHitParticles(boss.x, boss.y, COLOR_SHIELD_BLOCK_SPARK, 26)
                            }
                        } else {
                            emitSound(GameSoundEvent.BOSS_HIT)
                            spawnHitParticles(projectile.x, projectile.y, COLOR_BOSS_HIT_SPARK, 7)
                        }
                        break
                    }
                }
                if (hitTurret) continue

                // Check secondary boss (Level 27 Board of Directors: Ivanone)
                val secBoss = state.secondaryBoss
                if (secBoss != null && secBoss.hp > 0 && !secBoss.isVanished && circlesCollide(
                        projectile.x, projectile.y, projectile.radius,
                        secBoss.x, secBoss.y, secBoss.radius
                    )
                ) {
                    iterator.remove()
                    applyDamageToBoardMember(
                        target = secBoss,
                        partner = boss,
                        damage = projectile.damage,
                        hitX = projectile.x,
                        hitY = projectile.y,
                        canVanish = true
                    )
                    if (state.status != GameStatus.PLAYING) {
                        return
                    }
                    continue
                }

                if (boss.hp > 0 && !boss.isVanished && circlesCollide(
                        projectile.x, projectile.y, projectile.radius,
                        boss.x, boss.y, boss.radius
                    )
                ) {
                    iterator.remove()
                    if (level.enableBoardOfDirectors) {
                        applyDamageToBoardMember(
                            target = boss,
                            partner = state.secondaryBoss,
                            damage = projectile.damage,
                            hitX = projectile.x,
                            hitY = projectile.y,
                            canVanish = false
                        )
                        if (state.status != GameStatus.PLAYING) {
                            return
                        }
                    } else if (level.enableGiantLollo && projectile.isReflectableFurniture) {
                        if (projectile.kind == ProjectileKind.TV) {
                            state.lolloTvHit = true
                        } else if (projectile.kind == ProjectileKind.SOFA) {
                            state.lolloSofaHit = true
                        }
                        boss.hitFlashTimer = 0.14f
                        emitSound(GameSoundEvent.ARMOR_BREAK)
                        spawnHitParticles(projectile.x, projectile.y, COLOR_VICTORY_SPARK, 20)
                        if (state.lolloTvHit && state.lolloSofaHit) {
                            state.lolloIngolfatoTimer = if (level.enableFuriousLollo) {
                                FURIOUS_LOLLO_INGOLFATO_DURATION
                            } else {
                                LOLLO_INGOLFATO_DURATION
                            }
                            state.lolloTvHit = false
                            state.lolloSofaHit = false
                            state.lasers.clear()
                            state.phaseFlashTimer = 0.55f
                            emitSound(GameSoundEvent.BOSS_CHARGE)
                            spawnHitParticles(boss.x, boss.y, COLOR_SHIELD_BLOCK_SPARK, 28)
                        }
                    } else {
                        val isBossProtected = state.hasActiveWeakPoints ||
                            (level.enableTurretFortress && state.hasActiveTurrets) ||
                            (level.enableGiantLollo && !state.isLolloIngolfato)
                        if (isBossProtected) {
                            emitSound(GameSoundEvent.SHIELD_BLOCK)
                            spawnHitParticles(projectile.x, projectile.y, COLOR_ARMOR_SPARK, 8)
                        } else {
                            applyDamageToBoss(projectile.damage, projectile.x, projectile.y)
                            if (state.status != GameStatus.PLAYING) {
                                return
                            }
                        }
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

        // Free-flight body contact with bosses, minion ships, or live perimeter turrets
        if (level.mode == BattleMode.FREE_FLIGHT_SHOOTER && !player.isInvulnerable) {
            val secBoss = state.secondaryBoss
            if (boss.hp > 0 && !boss.isVanished &&
                circlesCollide(player.x, player.y, player.radius * 0.85f, boss.x, boss.y, boss.radius * 0.85f)
            ) {
                applyDamageToPlayer(1)
                if (state.status != GameStatus.PLAYING) return
            } else if (secBoss != null && secBoss.hp > 0 && !secBoss.isVanished &&
                circlesCollide(player.x, player.y, player.radius * 0.85f, secBoss.x, secBoss.y, secBoss.radius * 0.85f)
            ) {
                applyDamageToPlayer(1)
                if (state.status != GameStatus.PLAYING) return
            } else {
                var hitBody = false
                val bodyMinionIter = state.minions.iterator()
                while (bodyMinionIter.hasNext()) {
                    val minion = bodyMinionIter.next()
                    if (circlesCollide(player.x, player.y, player.radius * 0.85f, minion.x, minion.y, minion.radius * 0.85f)) {
                        if (level.enableTechLeadLollo) {
                            bodyMinionIter.remove()
                            emitSound(GameSoundEvent.ARMOR_BREAK)
                            spawnHitParticles(minion.x, minion.y, COLOR_BOSS_HIT_SPARK, 18)
                        }
                        applyDamageToPlayer(1)
                        if (state.status != GameStatus.PLAYING) return
                        hitBody = true
                        break
                    }
                }
                if (!hitBody) {
                    for (turret in state.turrets) {
                        if (turret.isDestroyed) continue
                        if (circlesCollide(player.x, player.y, player.radius * 0.85f, turret.x, turret.y, turret.radius * 0.85f)) {
                            applyDamageToPlayer(1)
                            if (state.status != GameStatus.PLAYING) return
                            break
                        }
                    }
                }
            }
        }
        // Active Laser Beam collision vs Player
        if (state.lasers.isNotEmpty() && !player.isInvulnerable) {
            for (laser in state.lasers) {
                if (!laser.isActive) continue
                val hit = if (laser.isHorizontal) {
                    abs(player.y - laser.position) <= player.radius * 0.75f + laser.thickness * 0.45f
                } else {
                    abs(player.x - laser.position) <= player.radius * 0.75f + laser.thickness * 0.45f
                }
                if (hit) {
                    applyDamageToPlayer(1)
                    if (state.status != GameStatus.PLAYING) {
                        return
                    }
                    break
                }
            }
        }
    }

    private fun applyDamageToBoardMember(
        target: Boss,
        partner: Boss?,
        damage: Int,
        hitX: Float,
        hitY: Float,
        canVanish: Boolean
    ) {
        target.hitFlashTimer = BOSS_HIT_FLASH_DURATION
        target.hp = max(0, target.hp - damage)
        spawnHitParticles(hitX, hitY, color = COLOR_BOSS_HIT_SPARK, count = 8)

        if (target.hp <= 0) {
            target.vanishTimer = 0f
            spawnHitParticles(target.x, target.y, color = COLOR_VICTORY_SPARK, count = 24)
            if (partner == null || partner.hp <= 0) {
                state.status = GameStatus.VICTORY
                state.endStateCooldown = END_STATE_COOLDOWN
                state.projectiles.clear()
                state.pickups.clear()
                state.lasers.clear()
                emitSound(GameSoundEvent.VICTORY)
            } else {
                // Surviving Board member absorbs the other seat -> SUPER-CEO MODE!
                partner.phase = max(partner.phase, 2)
                partner.hp = min(partner.maxHp, partner.hp + 30)
                partner.displayedHp = max(partner.displayedHp, partner.hp.toFloat())
                state.phaseFlashTimer = 0.65f
                emitSound(GameSoundEvent.BOSS_CHARGE)
                spawnHitParticles(partner.x, partner.y, color = COLOR_SHIELD_BLOCK_SPARK, count = 26)
            }
            return
        }

        emitSound(GameSoundEvent.BOSS_HIT)
        if (canVanish) {
            target.hitsSinceVanish += 1
            if (target.hitsSinceVanish >= 4) {
                target.hitsSinceVanish = 0
                target.vanishTimer = 0.65f
                emitSound(GameSoundEvent.BOSS_VANISH)
                spawnHitParticles(target.x, target.y, COLOR_VANISH_SMOKE, 14)
            }
        }
    }

    private fun applyDamageToBoss(
        damage: Int,
        hitX: Float,
        hitY: Float,
        armorMultiplier: Int = 1
    ) {
        val boss = state.boss
        boss.hitFlashTimer = BOSS_HIT_FLASH_DURATION

        // Armor absorbs incoming damage first; breaking it stuns the boss!
        if (boss.armor > 0) {
            val armorDamage = damage * armorMultiplier
            boss.armor = max(0, boss.armor - armorDamage)
            spawnHitParticles(hitX, hitY, color = COLOR_ARMOR_SPARK, count = 10)
            if (boss.armor == 0) {
                boss.stunTimer = ARMOR_BREAK_STUN_DURATION
                boss.action = BossAction.PATROL
                emitSound(GameSoundEvent.ARMOR_BREAK)
                spawnHitParticles(boss.x, boss.y, color = COLOR_SHIELD_BLOCK_SPARK, count = 22)
            } else {
                emitSound(GameSoundEvent.SHIELD_BLOCK)
            }
            return
        }

        // Level 26 Tech Lead Lollo takes half damage in Phase 3!
        val effectiveDamage = if (level.enableTechLeadLollo && boss.phase >= 3) {
            max(1, damage / 2)
        } else {
            damage
        }
        boss.hp = max(0, boss.hp - effectiveDamage)
        spawnHitParticles(hitX, hitY, color = COLOR_BOSS_HIT_SPARK, count = 8)

        if (boss.hp <= 0) {
            state.status = GameStatus.VICTORY
            state.endStateCooldown = END_STATE_COOLDOWN
            state.projectiles.clear()
            state.pickups.clear()
            state.lasers.clear()
            emitSound(GameSoundEvent.VICTORY)
            spawnHitParticles(boss.x, boss.y, color = COLOR_VICTORY_SPARK, count = 24)
            return
        }

        emitSound(GameSoundEvent.BOSS_HIT)

        // Multi-phase transitions: Phase 2 (66%) and Phase 3 (33%)
        if (level.multiPhase) {
            val isOmega = level.arenaTheme == ArenaTheme.OMEGA_FINALE
            if (boss.phase == 1 && boss.hpRatio <= 0.66f) {
                boss.phase = 2
                if (!level.enableTechLeadLollo) {
                    val newArmor = if (isOmega) 70 else 50
                    boss.armor = newArmor
                    boss.maxArmor = newArmor
                } else {
                    state.phaseFlashTimer = 0.45f
                }
                boss.stunTimer = 0f
                if (isOmega) {
                    state.phaseFlashTimer = 0.5f
                    state.lasers.clear()
                }
                emitSound(GameSoundEvent.BOSS_CHARGE)
                spawnHitParticles(boss.x, boss.y, color = COLOR_ARMOR_SPARK, count = 18)
            } else if (boss.phase == 2 && boss.hpRatio <= 0.33f) {
                boss.phase = 3
                if (!level.enableTechLeadLollo) {
                    val newArmor = if (isOmega) 60 else 40
                    boss.armor = newArmor
                    boss.maxArmor = newArmor
                } else {
                    state.phaseFlashTimer = 0.60f
                }
                boss.stunTimer = 0f
                if (isOmega) {
                    state.phaseFlashTimer = 0.6f
                    state.lasers.clear()
                    if (state.platforms.size < 3) {
                        state.platforms.add(
                            MovingPlatform(
                                x = state.worldWidth * 0.5f,
                                y = 700f,
                                width = 210f,
                                vx = 180f,
                                minX = 180f,
                                maxX = state.worldWidth - 180f
                            )
                        )
                    }
                }
                emitSound(GameSoundEvent.BOSS_CHARGE)
                spawnHitParticles(boss.x, boss.y, color = COLOR_VICTORY_SPARK, count = 20)
            }
        }

        // Ivanone vanishes easily (every 2 melee hits, or every 5 rapid shooter bolts)!
        if (level.arenaTheme == ArenaTheme.STUDIO) {
            val threshold = if (level.mode == BattleMode.SHOOTER) {
                IVANONE_SHOOTER_HITS_BEFORE_VANISH
            } else {
                IVANONE_HITS_BEFORE_VANISH
            }
            boss.hitsSinceVanish += 1
            if (boss.hitsSinceVanish >= threshold) {
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
        emitSound(GameSoundEvent.PLAYER_SHOT)
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
        emitSound(GameSoundEvent.BOSS_ATTACK)
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

    private fun updatePickups(dt: Float) {
        pickupSpawnTimer -= dt
        if (pickupSpawnTimer <= 0f && state.pickups.size < MAX_ACTIVE_PICKUPS) {
            spawnNextPickup()
            pickupSpawnTimer = PICKUP_SPAWN_INTERVAL
        }

        val player = state.player
        val iterator = state.pickups.iterator()
        while (iterator.hasNext()) {
            val pickup = iterator.next()
            pickup.life -= dt
            if (pickup.life <= 0f) {
                iterator.remove()
                continue
            }
            if (circlesCollide(player.x, player.y, player.radius + 10f, pickup.x, pickup.y, pickup.radius)) {
                applyPickup(pickup)
                iterator.remove()
            }
        }
    }

    private fun spawnNextPickup() {
        val sequence = listOf(
            PickupKind.WEAPON_HAMMER,
            PickupKind.POWER_SURGE,
            PickupKind.WEAPON_KATANA,
            PickupKind.HEAL,
            PickupKind.WEAPON_SPEAR,
            PickupKind.SPEED_BOOST
        )
        val kind = sequence[pickupSequenceIndex % sequence.size]
        val xs = listOf(220f, 500f, 780f, 340f, 660f)
        val ys = listOf(state.groundY - 52f, state.groundY - 190f, state.groundY - 52f)
        val x = xs[pickupSequenceIndex % xs.size]
        val y = ys[pickupSequenceIndex % ys.size]
        pickupSequenceIndex += 1

        state.pickups.add(ArenaPickup(x = x, y = y, kind = kind))
    }

    private fun applyPickup(pickup: ArenaPickup) {
        val player = state.player
        emitSound(GameSoundEvent.PICKUP)
        spawnHitParticles(pickup.x, pickup.y, COLOR_PICKUP_SPARK, 12)
        when (pickup.kind) {
            PickupKind.WEAPON_HAMMER -> {
                player.weapon = WeaponType.HAMMER
                player.weaponTimer = WEAPON_DURATION
            }
            PickupKind.WEAPON_KATANA -> {
                player.weapon = WeaponType.KATANA
                player.weaponTimer = WEAPON_DURATION
            }
            PickupKind.WEAPON_SPEAR -> {
                player.weapon = WeaponType.SPEAR
                player.weaponTimer = WEAPON_DURATION
            }
            PickupKind.HEAL -> {
                player.hp = min(player.maxHp, player.hp + 1)
            }
            PickupKind.SPEED_BOOST -> {
                player.speedBoostTimer = POWERUP_DURATION
            }
            PickupKind.POWER_SURGE -> {
                player.damageBoostTimer = POWERUP_DURATION
            }
        }
    }

    private fun spawnCaramelSpaAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.phase >= 2) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val angles = when (boss.phase) {
            1 -> listOf(baseAngle - 0.12, baseAngle + 0.12)
            2 -> listOf(baseAngle - 0.22, baseAngle, baseAngle + 0.22)
            else -> listOf(baseAngle - 0.26, baseAngle, baseAngle + 0.26)
        }
        spawnHitParticles(boss.x, boss.y - 20f, COLOR_VICTORY_SPARK, 10)
        for (angle in angles) {
            state.projectiles.add(
                Projectile(
                    x = boss.x + boss.facingDir * (boss.radius * 0.45f),
                    y = boss.y - 16f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 22f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.HOT_STONE
                )
            )
        }
        if (boss.phase >= 3 && level.mode == BattleMode.MELEE_PLATFORMER) {
            spawnGroundShockwaves()
        }
    }

    private fun populateWeakPoints() {
        val r = state.boss.radius
        state.weakPoints.clear()
        // 1 weak point behind (rear reactor) + 2 weak points on the sides
        state.weakPoints.add(
            BossWeakPoint(
                id = "REAR",
                offsetX = 0f,
                offsetY = -r * 0.92f
            )
        )
        state.weakPoints.add(
            BossWeakPoint(
                id = "LEFT",
                offsetX = -r * 1.08f,
                offsetY = 10f
            )
        )
        state.weakPoints.add(
            BossWeakPoint(
                id = "RIGHT",
                offsetX = r * 1.08f,
                offsetY = 10f
            )
        )
    }

    private fun populateTurrets() {
        val w = state.worldWidth
        state.turrets.clear()
        // 8 perimeter turrets surrounding the player across the entire arena
        val positions = listOf(
            Pair(115f, 340f),
            Pair(w - 115f, 340f),
            Pair(72f, 620f),
            Pair(w - 72f, 620f),
            Pair(72f, 960f),
            Pair(w - 72f, 960f),
            Pair(295f, 1290f),
            Pair(w - 295f, 1290f)
        )
        positions.forEachIndexed { idx, (tx, ty) ->
            state.turrets.add(
                ArenaTurret(
                    id = idx + 1,
                    x = tx,
                    y = ty,
                    fireCooldown = 1.1f + (idx % 4) * 0.55f
                )
            )
        }
    }

    private fun populateInitialPlatforms() {
        state.platforms.clear()
        state.platforms.add(
            MovingPlatform(
                x = state.worldWidth * 0.32f,
                y = 1045f,
                width = 230f,
                vx = 135f,
                minX = 165f,
                maxX = state.worldWidth - 165f
            )
        )
        state.platforms.add(
            MovingPlatform(
                x = state.worldWidth * 0.68f,
                y = 855f,
                width = 220f,
                vx = -155f,
                minX = 165f,
                maxX = state.worldWidth - 165f
            )
        )
    }

    private fun updatePlatforms(dt: Float) {
        val phaseSpeedBoost = when (state.boss.phase) {
            1 -> 1.0f
            2 -> 1.25f
            else -> 1.45f
        }
        for (platform in state.platforms) {
            platform.x += platform.vx * phaseSpeedBoost * dt
            if (platform.x <= platform.minX) {
                platform.x = platform.minX
                platform.vx = abs(platform.vx)
            } else if (platform.x >= platform.maxX) {
                platform.x = platform.maxX
                platform.vx = -abs(platform.vx)
            }
        }
    }

    private fun updateLasers(dt: Float) {
        val iterator = state.lasers.iterator()
        while (iterator.hasNext()) {
            val laser = iterator.next()
            if (laser.telegraphTimer > 0f) {
                laser.telegraphTimer = max(0f, laser.telegraphTimer - dt)
                if (laser.telegraphTimer <= 0f && !laser.firedSound) {
                    laser.firedSound = true
                    emitSound(GameSoundEvent.LASER_FIRE)
                }
            } else {
                laser.activeTimer -= dt
                if (laser.activeTimer <= 0f) {
                    iterator.remove()
                }
            }
        }
    }

    private fun spawnOmegaOrbAndLaserAttack() {
        val boss = state.boss
        val player = state.player
        val speed = if (boss.phase >= 2) level.bossEnragedProjectileSpeed else level.bossProjectileSpeed
        val baseAngle = atan2(player.y - boss.y, player.x - boss.x).toDouble()
        val orbAngles = when (boss.phase) {
            1 -> listOf(baseAngle - 0.16, baseAngle + 0.16)
            2 -> listOf(baseAngle - 0.24, baseAngle, baseAngle + 0.24)
            else -> listOf(baseAngle - 0.30, baseAngle - 0.10, baseAngle + 0.10, baseAngle + 0.30)
        }
        for (angle in orbAngles) {
            state.projectiles.add(
                Projectile(
                    x = boss.x,
                    y = boss.y - 12f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    radius = 20f,
                    fromPlayer = false,
                    damage = 1,
                    kind = ProjectileKind.BOSS_ORB
                )
            )
        }

        // Telegraphed Laser Beams escalating per phase!
        state.lasers.add(
            LaserBeam(
                isHorizontal = true,
                position = state.groundY - 46f,
                thickness = 46f,
                telegraphTimer = if (boss.phase >= 3) 0.65f else 0.80f,
                activeTimer = 0.52f
            )
        )
        if (boss.phase >= 2) {
            state.lasers.add(
                LaserBeam(
                    isHorizontal = false,
                    position = player.x.coerceIn(120f, state.worldWidth - 120f),
                    thickness = 44f,
                    telegraphTimer = 0.78f,
                    activeTimer = 0.50f
                )
            )
        }
        if (boss.phase >= 3) {
            val secondX = (state.worldWidth - player.x).coerceIn(140f, state.worldWidth - 140f)
            state.lasers.add(
                LaserBeam(
                    isHorizontal = false,
                    position = secondX,
                    thickness = 42f,
                    telegraphTimer = 0.72f,
                    activeTimer = 0.50f
                )
            )
        }
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
        val sec = state.secondaryBoss
        if (sec != null) {
            val secTarget = sec.hp.toFloat()
            if (sec.displayedHp > secTarget) {
                sec.displayedHp = max(secTarget, sec.displayedHp - BOSS_HP_TRAIL_SPEED * dt)
            } else {
                sec.displayedHp = secTarget
            }
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
        val bossRadius = when {
            level.enableGiantLollo -> 128f
            level.enableBoardOfDirectors && isMelee -> 66f
            level.enableBoardOfDirectors -> 78f
            level.arenaTheme == ArenaTheme.HANOI -> 64f
            level.arenaTheme == ArenaTheme.HANOI_SPA -> 68f
            level.arenaTheme == ArenaTheme.OMEGA_FINALE -> 82f
            level.arenaTheme == ArenaTheme.DEEP_SPACE -> 104f
            level.arenaTheme == ArenaTheme.ZURICH -> 66f
            level.arenaTheme == ArenaTheme.STUDIO -> 74f
            level.arenaTheme == ArenaTheme.ZURICH_TAX -> 68f
            level.arenaTheme == ArenaTheme.PARIS -> 62f
            level.arenaTheme == ArenaTheme.UBS_ALPS -> 64f
            level.arenaTheme == ArenaTheme.GYM_DEV -> 58f
            level.arenaTheme == ArenaTheme.GOOGLE_ZURICH -> 82f
            level.arenaTheme == ArenaTheme.IRON_ARENA -> 84f
            else -> 88f
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
                name = if (level.enableBoardOfDirectors) "LOLLO ZURICH" else level.bossName,
                x = when {
                    isMelee -> worldWidth * 0.76f
                    level.enableBoardOfDirectors -> worldWidth * 0.28f
                    else -> worldWidth / 2f
                },
                y = when {
                    isMelee -> DEFAULT_GROUND_Y - bossRadius
                    level.enableBoardOfDirectors -> 390f
                    else -> BOSS_TOP_OFFSET
                },
                radius = bossRadius,
                hp = level.bossMaxHp,
                maxHp = level.bossMaxHp,
                displayedHp = level.bossMaxHp.toFloat(),
                armor = level.bossMaxArmor,
                maxArmor = level.bossMaxArmor,
                phase = 1,
                velocityX = if (isMelee) -level.bossBaseSpeed else level.bossBaseSpeed
            ),
            secondaryBoss = if (level.enableBoardOfDirectors) {
                Boss(
                    name = "IVANONE",
                    x = if (isMelee) worldWidth * 0.78f else worldWidth * 0.72f,
                    y = if (isMelee) 785f else 390f,
                    radius = if (isMelee) 68f else 80f,
                    hp = level.bossMaxHp,
                    maxHp = level.bossMaxHp,
                    displayedHp = level.bossMaxHp.toFloat(),
                    phase = 1,
                    velocityX = if (isMelee) -level.bossBaseSpeed * 0.9f else 0f,
                    attackCooldown = if (isMelee) 2.1f else 1.35f
                )
            } else {
                null
            },
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

        private const val FREE_FLIGHT_PLAYER_SPEED = 520f
        private const val FREE_FLIGHT_BOLT_SPEED = 1050f
        private const val FREE_FLIGHT_FIRE_INTERVAL = 0.20f
        private const val MAX_ACTIVE_MINIONS = 3
        private const val MINION_FIRST_SPAWN_DELAY = 5.2f
        private const val MINION_SPAWN_INTERVAL = 8.4f
        private const val MINION_ENRAGED_SPAWN_INTERVAL = 5.8f
        private const val MINION_SPEED = 205f
        private const val MINION_BOLT_SPEED = 400f
        private const val MINION_FIRE_INTERVAL = 1.95f
        private const val FORTRESS_BOSS_Y = 285f
        private const val TURRET_FIRE_INTERVAL = 2.8f
        private const val TURRET_BOLT_SPEED = 360f
        private const val LOLLO_INGOLFATO_DURATION = 6.5f
        private const val FURIOUS_LOLLO_INGOLFATO_DURATION = 3.25f
        private const val FURIOUS_LASER_FIRST_DELAY = 2.6f
        private const val FURIOUS_LASER_INTERVAL = 3.4f
        private const val FURIOUS_LASER_ENRAGED_INTERVAL = 2.4f
        private const val REFLECTED_FURNITURE_SPEED = 780f

        private const val PLATFORMER_MOVE_SPEED = 460f
        private const val SHIELD_MOVE_MULTIPLIER = 0.55f
        private const val SPEED_BOOST_MULTIPLIER = 1.45f
        private const val SHIELD_FLASH_DURATION = 0.18f
        private const val GRAVITY = 2600f
        private const val JUMP_VELOCITY = -1020f
        private const val DOUBLE_JUMP_VELOCITY = -920f
        private const val MELEE_DURATION = 0.20f
        private const val MELEE_COOLDOWN = 0.34f
        private const val MELEE_REACH_OFFSET = 64f
        private const val MELEE_HIT_RADIUS = 76f
        private const val MELEE_DAMAGE = 10

        private const val MAX_ACTIVE_PICKUPS = 2
        private const val PICKUP_FIRST_SPAWN_DELAY = 2.5f
        private const val PICKUP_SPAWN_INTERVAL = 5.2f
        private const val WEAPON_DURATION = 11.0f
        private const val POWERUP_DURATION = 8.5f
        private const val ARMOR_BREAK_STUN_DURATION = 1.8f

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
        private const val IVANONE_SHOOTER_HITS_BEFORE_VANISH = 5
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

        val TECH_LEAD_QUOTES = listOf(
            "RIFAI LA PR!",
            "NON SCALA!",
            "STANDUP ALLE 8!",
            "DOV'È IL DESIGN DOC?",
            "LGTM... SCHERZAVO!"
        )

        val BOARD_LOLLO_QUOTES = listOf(
            "APPROVATO DAL BOARD!",
            "TAGLIAMO IL BUDGET!",
            "FA FREDDO A ZURIGO!",
            "SINERGIA MASSIMA!",
            "LICENZIATO!"
        )

        const val COLOR_BOSS_HIT_SPARK = 0xFFFFD54F.toInt()
        const val COLOR_VICTORY_SPARK = 0xFFFF7043.toInt()
        const val COLOR_PLAYER_HIT_SPARK = 0xFF4FC3F7.toInt()
        const val COLOR_JUMP_DUST = 0xFFB0BEC5.toInt()
        const val COLOR_DOUBLE_JUMP_SPARK = 0xFF80DEEA.toInt()
        const val COLOR_SHIELD_BLOCK_SPARK = 0xFFFFE082.toInt()
        const val COLOR_VANISH_SMOKE = 0xFFCFD8DC.toInt()
        const val COLOR_LOUISE_PINK = 0xFFF48FB1.toInt()
        const val COLOR_ARMOR_SPARK = 0xFF80DEEA.toInt()
        const val COLOR_PICKUP_SPARK = 0xFF69F0AE.toInt()
    }
}
