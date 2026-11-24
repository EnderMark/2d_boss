package com.endermark.bossbattle.engine

import com.endermark.bossbattle.model.ArenaTheme
import com.endermark.bossbattle.model.BattleMode
import com.endermark.bossbattle.model.Boss
import com.endermark.bossbattle.model.BossAction
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.LevelCatalog
import com.endermark.bossbattle.model.Player
import com.endermark.bossbattle.model.Projectile
import com.endermark.bossbattle.model.ProjectileKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BossBattleEngineTest {

    @Test
    fun levelCatalogContainsAllTwentyEightLevels() {
        assertEquals(28, LevelCatalog.levels.size)

        val level21 = LevelCatalog.findById(21)
        assertEquals("DREADNOUGHT STELLARE", level21.bossName)
        assertEquals(BattleMode.FREE_FLIGHT_SHOOTER, level21.mode)
        assertTrue(level21.enableMinions)
        assertFalse(level21.enableWeakPoints)

        val level22 = LevelCatalog.findById(22)
        assertEquals("CORAZZATA AEGIS", level22.bossName)
        assertEquals(BattleMode.FREE_FLIGHT_SHOOTER, level22.mode)
        assertFalse(level22.enableMinions)
        assertTrue(level22.enableWeakPoints)

        val level23 = LevelCatalog.findById(23)
        assertEquals("BASTIONE ORBITALE", level23.bossName)
        assertEquals(BattleMode.FREE_FLIGHT_SHOOTER, level23.mode)
        assertFalse(level23.enableMinions)
        assertFalse(level23.enableWeakPoints)
        assertTrue(level23.enableTurretFortress)

        val level24 = LevelCatalog.findById(24)
        assertEquals("LOLLO ZURICH GIGANTE", level24.bossName)
        assertEquals(BattleMode.FREE_FLIGHT_SHOOTER, level24.mode)
        assertTrue(level24.enableGiantLollo)
        assertFalse(level24.enableFuriousLollo)

        val level25 = LevelCatalog.findById(25)
        assertEquals("LOLLO GIGANTE INFURIATO", level25.bossName)
        assertEquals(BattleMode.FREE_FLIGHT_SHOOTER, level25.mode)
        assertTrue(level25.enableGiantLollo)
        assertTrue(level25.enableFuriousLollo)
        assertTrue(level25.enableLasers)

        val level26 = LevelCatalog.findById(26)
        assertEquals("LOLLO TECH LEAD", level26.bossName)
        assertEquals(BattleMode.FREE_FLIGHT_SHOOTER, level26.mode)
        assertTrue(level26.multiPhase)
        assertTrue(level26.enableMinions)
        assertTrue(level26.enableTechLeadLollo)

        val level27 = LevelCatalog.findById(27)
        assertEquals("BOARD OF DIRECTORS", level27.bossName)
        assertEquals(BattleMode.FREE_FLIGHT_SHOOTER, level27.mode)
        assertTrue(level27.enableBoardOfDirectors)

        val level28 = LevelCatalog.findById(28)
        assertEquals("BOARD OF DIRECTORS", level28.bossName)
        assertEquals(BattleMode.MELEE_PLATFORMER, level28.mode)
        assertTrue(level28.enableBoardOfDirectors)
        assertTrue(level28.enablePistol)
        assertTrue(level28.enablePlatforms)
        assertTrue(level28.multiPhase)
        assertEquals(LevelCatalog.LEVEL_1, LevelCatalog.findById(999))
    }

    @Test
    fun engineInitializesBossFromSelectedLevel() {
        val customLevel = BossLevel(
            id = 9,
            title = "LIVELLO 9",
            bossName = "TITANO DI FUOCO",
            subtitle = "Test",
            mode = BattleMode.SHOOTER,
            arenaTheme = ArenaTheme.SPACE,
            bossMaxHp = 150,
            bossBaseSpeed = 300f,
            bossAttackInterval = 0.8f,
            bossEnragedAttackInterval = 0.6f,
            bossProjectileSpeed = 580f,
            bossEnragedProjectileSpeed = 680f
        )

        val engine = BossBattleEngine(level = customLevel)

        assertEquals(customLevel, engine.state.level)
        assertEquals("TITANO DI FUOCO", engine.state.boss.name)
        assertEquals(150, engine.state.boss.hp)
        assertEquals(150, engine.state.boss.maxHp)
    }

    @Test
    fun bossTakesDamageAndHealthBarRatioDecreases() {
        val engine = BossBattleEngine()
        engine.startGame()

        val boss = engine.state.boss
        val initialHp = boss.hp

        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = 0f,
                radius = 12f,
                fromPlayer = true,
                damage = 10
            )
        )

        engine.update(0.016f)

        assertEquals(initialHp - 10, boss.hp)
        assertEquals(0.9f, boss.hpRatio, 0.001f)
        assertEquals(GameStatus.PLAYING, engine.state.status)
    }

    @Test
    fun bossDefeatTriggersVictoryState() {
        val engine = BossBattleEngine()
        engine.startGame()

        val boss = engine.state.boss
        boss.hp = 5

        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = 0f,
                radius = 12f,
                fromPlayer = true,
                damage = 5
            )
        )

        engine.update(0.016f)

        assertEquals(0, boss.hp)
        assertEquals(0f, boss.hpRatio, 0.001f)
        assertEquals(GameStatus.VICTORY, engine.state.status)
    }

    @Test
    fun playerTakesDamageRespectingInvulnerabilityAndLosesAtZeroHp() {
        val engine = BossBattleEngine()
        engine.startGame()

        val player = engine.state.player
        player.hp = 1

        engine.state.projectiles.add(
            Projectile(
                x = player.x,
                y = player.y,
                vx = 0f,
                vy = 0f,
                radius = 18f,
                fromPlayer = false,
                damage = 1
            )
        )

        engine.update(0.016f)

        assertEquals(0, player.hp)
        assertEquals(GameStatus.DEFEAT, engine.state.status)
    }

    @Test
    fun resetRestoresFullBossAndPlayerHealth() {
        val engine = BossBattleEngine()
        engine.startGame()
        engine.state.boss.hp = 20
        engine.state.player.hp = 1

        engine.resetBattle(GameStatus.PLAYING)

        assertEquals(Boss.MAX_HP, engine.state.boss.hp)
        assertEquals(1f, engine.state.boss.hpRatio, 0.001f)
        assertEquals(Player.MAX_HP, engine.state.player.hp)
        assertTrue(engine.state.projectiles.isEmpty())
    }

    @Test
    fun levelTwoSupportsLeftRightJumpAndDoubleJumpOnly() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_2)
        engine.startGame()

        val startX = engine.state.player.x
        engine.setMoveInput(1)
        engine.update(0.05f)
        assertTrue(engine.state.player.x > startX)
        assertEquals(1, engine.state.player.facingDir)

        val afterRightX = engine.state.player.x
        engine.setMoveInput(-1)
        engine.update(0.05f)
        assertTrue(engine.state.player.x < afterRightX)
        assertEquals(-1, engine.state.player.facingDir)

        assertTrue(engine.triggerJump())
        engine.update(0.05f)
        assertFalse(engine.state.player.onGround)
        assertTrue(engine.triggerJump())
        assertFalse(engine.triggerJump())
    }

    @Test
    fun levelThreeShieldBlocksChanProjectilesWithZeroDamage() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_3)
        engine.startGame()

        val player = engine.state.player
        val initialHp = player.hp

        engine.setBlocking(true)
        assertTrue(player.isBlocking)
        assertFalse(engine.triggerMeleeAttack())

        engine.state.projectiles.add(
            Projectile(
                x = player.x,
                y = player.y,
                vx = 0f,
                vy = 0f,
                radius = 20f,
                fromPlayer = false,
                damage = 1,
                kind = ProjectileKind.LANTERN
            )
        )

        engine.update(0.016f)

        assertEquals(initialHp, player.hp)
        assertTrue(player.shieldFlashTimer > 0f)
        assertTrue(engine.state.projectiles.isEmpty())

        engine.setBlocking(false)
        assertFalse(player.isBlocking)
        assertTrue(engine.triggerMeleeAttack())
    }

    @Test
    fun levelFourLolloZurichFiresItalianComplaintsAndPlayerHasNoShield() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_4)
        engine.startGame()

        // Shield is disabled on Level 4
        engine.setBlocking(true)
        assertFalse(engine.state.player.isBlocking)

        // Trigger verbal complaint attack
        engine.state.boss.action = BossAction.TELEGRAPH_SHOCKWAVE
        engine.state.boss.actionTimer = 0.01f
        engine.update(0.02f)

        val complaints = engine.state.projectiles.filter { it.kind == ProjectileKind.COMPLAINT }
        assertTrue(complaints.isNotEmpty())
        assertNotNull(complaints.first().label)
        assertTrue(BossBattleEngine.LOLLO_COMPLAINTS.contains(complaints.first().label))
    }

    @Test
    fun levelFiveIvanoneUsesDroneCameraAndVanishesEasily() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_5)
        engine.startGame()

        // Trigger drone attack
        engine.state.boss.action = BossAction.TELEGRAPH_SHOCKWAVE
        engine.state.boss.actionTimer = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.DRONE })

        // Trigger camera flash attack -> spawns CAMERA_FLASH and vanishes!
        engine.state.projectiles.clear()
        engine.state.boss.action = BossAction.TELEGRAPH_CHARGE
        engine.state.boss.actionTimer = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.CAMERA_FLASH })
        assertTrue(engine.state.boss.isVanished)

        // Reappears after vanishTimer expires
        repeat(20) { engine.update(0.05f) }
        assertFalse(engine.state.boss.isVanished)
    }

    @Test
    fun levelSixAlessioUsesZurichTaxesAndWeirdToothSpecial() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_6)
        engine.startGame()

        engine.state.boss.action = BossAction.TELEGRAPH_SHOCKWAVE
        engine.state.boss.actionTimer = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.TAX_FORM })

        engine.state.projectiles.clear()
        engine.state.boss.action = BossAction.TELEGRAPH_CHARGE
        engine.state.boss.actionTimer = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.WEIRD_TOOTH })
    }

    @Test
    fun levelSevenLouiseSeducesWithHeartsAndAvoidsClosePlayer() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_7)
        engine.startGame()

        // Seduction hearts attack
        engine.state.boss.action = BossAction.TELEGRAPH_SHOCKWAVE
        engine.state.boss.actionTimer = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.HEART })

        // Avoids player when player gets close
        engine.state.player.x = 400f
        engine.state.boss.x = 480f
        engine.state.boss.action = BossAction.PATROL
        engine.state.boss.attackCooldown = 5f
        val beforeX = engine.state.boss.x
        engine.update(0.05f)
        assertTrue(engine.state.boss.x > beforeX)
    }

    @Test
    fun levelEightDaisyScreamsAboutUbsAndHikes() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_8)
        engine.startGame()

        engine.state.boss.action = BossAction.TELEGRAPH_SHOCKWAVE
        engine.state.boss.actionTimer = 0.01f
        engine.update(0.02f)

        val screams = engine.state.projectiles.filter { it.kind == ProjectileKind.SCREAM_WAVE }
        assertTrue(screams.isNotEmpty())
        assertTrue(BossBattleEngine.DAISY_SCREAMS.contains(screams.first().label))
    }

    @Test
    fun levelNineAndreaDatteroThrowsRiceChickenAndBadDevBoasts() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_9)
        engine.startGame()

        engine.state.boss.action = BossAction.TELEGRAPH_SHOCKWAVE
        engine.state.boss.actionTimer = 0.01f
        engine.update(0.02f)

        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.RICE_CHICKEN })
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.COMPLAINT })
    }

    @Test
    fun levelTenMarcoDiGoogleCameraDatingAndSoundEvents() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_10)
        engine.startGame()

        assertTrue(engine.triggerJump())
        assertTrue(engine.triggerMeleeAttack())

        engine.state.boss.action = BossAction.TELEGRAPH_SHOCKWAVE
        engine.state.boss.actionTimer = 0.01f
        engine.update(0.02f)

        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.CAMERA_FLASH })
        val flex = engine.state.projectiles.firstOrNull { it.kind == ProjectileKind.DATING_FLEX }
        assertNotNull(flex)
        assertEquals("SONO PIENO DI MILIONI!", flex?.label)

        val sounds = engine.drainSoundEvents()
        assertTrue(sounds.contains(com.endermark.bossbattle.model.GameSoundEvent.JUMP))
        assertTrue(sounds.contains(com.endermark.bossbattle.model.GameSoundEvent.ATTACK))
        assertTrue(sounds.contains(com.endermark.bossbattle.model.GameSoundEvent.CAMERA_SHUTTER))
        assertTrue(engine.drainSoundEvents().isEmpty())
    }

    @Test
    fun levelElevenCaramelArmorPhasesAndPickups() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_11)
        engine.startGame()

        val boss = engine.state.boss
        val player = engine.state.player
        assertEquals(80, boss.armor)
        assertEquals(1, boss.phase)

        // Collect a Hammer pickup placed on the player
        engine.state.pickups.add(
            com.endermark.bossbattle.model.ArenaPickup(
                x = player.x,
                y = player.y,
                kind = com.endermark.bossbattle.model.PickupKind.WEAPON_HAMMER
            )
        )
        engine.update(0.016f)
        assertEquals(com.endermark.bossbattle.model.WeaponType.HAMMER, player.weapon)
        assertTrue(engine.drainSoundEvents().contains(com.endermark.bossbattle.model.GameSoundEvent.PICKUP))

        // Attack Caramel's armor with Hammer until Armor Break triggers stun
        boss.x = player.x + boss.radius + 30f
        player.facingDir = 1
        repeat(4) {
            player.meleeCooldown = 0f
            engine.triggerMeleeAttack()
            engine.update(0.016f)
        }
        assertEquals(0, boss.armor)
        assertTrue(boss.isStunned)
        assertTrue(engine.drainSoundEvents().contains(com.endermark.bossbattle.model.GameSoundEvent.ARMOR_BREAK))

        // Drop HP below 66% to verify Phase 2 transition and armor re-forge
        boss.hp = 175
        player.meleeCooldown = 0f
        engine.triggerMeleeAttack()
        engine.update(0.016f)
        assertEquals(2, boss.phase)
        assertEquals(50, boss.armor)
    }

    @Test
    fun arcadeRemixLevelsFireThemedAttacks() {
        // Level 12: Lollo Zurich in Shooter mode fires Italian complaints
        val lollo = BossBattleEngine(level = LevelCatalog.LEVEL_12)
        lollo.startGame()
        lollo.state.boss.attackCooldown = 0.01f
        lollo.update(0.02f)
        assertTrue(lollo.state.projectiles.any { it.kind == ProjectileKind.COMPLAINT })

        // Level 18: Marco di Google in Shooter mode fires camera flashes & dating flex
        val marco = BossBattleEngine(level = LevelCatalog.LEVEL_18)
        marco.startGame()
        marco.state.boss.attackCooldown = 0.01f
        marco.update(0.02f)
        assertTrue(marco.state.projectiles.any { it.kind == ProjectileKind.CAMERA_FLASH })
        assertTrue(marco.state.projectiles.any { it.kind == ProjectileKind.DATING_FLEX })

        // Level 19: Caramel in Shooter mode fires hot stones and has armor
        val caramel = BossBattleEngine(level = LevelCatalog.LEVEL_19)
        caramel.startGame()
        assertEquals(70, caramel.state.boss.armor)
        caramel.state.boss.attackCooldown = 0.01f
        caramel.update(0.02f)
        assertTrue(caramel.state.projectiles.any { it.kind == ProjectileKind.HOT_STONE })
    }

    @Test
    fun levelTwentyOmegaFinalePlatformsLasersAndPhases() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_20)
        engine.startGame()

        // Moving platforms initialized and moving
        assertEquals(2, engine.state.platforms.size)
        val firstPlat = engine.state.platforms.first()
        val initialPlatX = firstPlat.x
        engine.update(0.05f)
        assertTrue(firstPlat.x != initialPlatX)

        // Player lands on moving platform and resets jumps
        val player = engine.state.player
        player.x = firstPlat.x
        player.y = firstPlat.y - player.radius - 2f
        player.vy = 180f
        player.jumpsRemaining = 0
        engine.update(0.05f)
        assertTrue(player.onGround)
        assertEquals(Player.MAX_JUMPS, player.jumpsRemaining)

        // Omega spawns telegraphed laser beam -> activates and emits LASER_FIRE
        engine.state.boss.action = BossAction.TELEGRAPH_SHOCKWAVE
        engine.state.boss.actionTimer = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.lasers.isNotEmpty())
        val laser = engine.state.lasers.first()
        assertFalse(laser.isActive)

        repeat(20) { engine.update(0.05f) }
        assertTrue(engine.drainSoundEvents().contains(com.endermark.bossbattle.model.GameSoundEvent.LASER_FIRE))

        // Phase 3 transition adds 3rd platform and reforges armor
        engine.state.boss.armor = 0
        engine.state.boss.phase = 2
        engine.state.boss.hp = 120
        engine.state.boss.x = player.x + engine.state.boss.radius + 25f
        player.y = engine.state.boss.y
        player.facingDir = 1
        player.meleeCooldown = 0f
        engine.triggerMeleeAttack()
        engine.update(0.016f)
        assertEquals(3, engine.state.boss.phase)
        assertEquals(60, engine.state.boss.armor)
        assertEquals(3, engine.state.platforms.size)
        assertTrue(engine.state.phaseFlashTimer > 0f)
    }

    @Test
    fun levelTwentyOneFreeFlightPadAndMinions() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_21)
        engine.startGame()

        val player = engine.state.player
        val startX = player.x
        val startY = player.y

        // Move diagonally up-right and rotate with pad + fire
        engine.setJoystickInput(1f, -1f)
        engine.setFiring(true)
        engine.update(0.05f)
        assertTrue(player.x > startX)
        assertTrue(player.y < startY)
        assertTrue(engine.state.projectiles.any { it.fromPlayer && it.vx > 0f && it.vy < 0f })

        // Advance time until Dreadnought spawns a minion ship (50% slower spawn rate -> 5.2s)
        engine.setFiring(false)
        repeat(120) { engine.update(0.05f) }
        assertTrue(engine.state.minions.isNotEmpty())

        // Player bolt destroys minion ship
        val minion = engine.state.minions.first()
        minion.hp = 5
        engine.state.projectiles.add(
            Projectile(
                x = minion.x,
                y = minion.y,
                vx = 0f,
                vy = 0f,
                radius = 12f,
                fromPlayer = true,
                damage = 8
            )
        )
        val countBefore = engine.state.minions.size
        engine.update(0.016f)
        assertEquals(countBefore - 1, engine.state.minions.size)
    }

    @Test
    fun levelTwentyTwoInvincibleUntilThreeWeakPointsDestroyed() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_22)
        engine.startGame()

        val boss = engine.state.boss
        assertEquals(3, engine.state.weakPoints.size)
        assertTrue(engine.state.hasActiveWeakPoints)

        // Advance time: NO minions should ever spawn on Level 22
        repeat(150) { engine.update(0.05f) }
        assertTrue(engine.state.minions.isEmpty())

        // Direct hull hit deals 0 damage while weak points are alive
        val initialHp = boss.hp
        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = 0f,
                radius = 12f,
                fromPlayer = true,
                damage = 20
            )
        )
        engine.update(0.016f)
        assertEquals(initialHp, boss.hp)

        // Destroy all 3 weak points (REAR, LEFT, RIGHT)
        for (wp in engine.state.weakPoints) {
            engine.state.projectiles.add(
                Projectile(
                    x = boss.x + wp.offsetX,
                    y = boss.y + wp.offsetY,
                    vx = 0f,
                    vy = 0f,
                    radius = 12f,
                    fromPlayer = true,
                    damage = wp.maxHp
                )
            )
            engine.update(0.016f)
            assertTrue(wp.isDestroyed)
        }
        assertFalse(engine.state.hasActiveWeakPoints)

        // Now direct hull hit damages the boss!
        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = 0f,
                radius = 12f,
                fromPlayer = true,
                damage = 20
            )
        )
        engine.update(0.016f)
        assertEquals(initialHp - 20, boss.hp)
    }

    @Test
    fun levelTwentyThreePerimeterTurretsUnlockTopMegaTurret() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_23)
        engine.startGame()

        val boss = engine.state.boss
        assertEquals(8, engine.state.turrets.size)
        assertTrue(engine.state.hasActiveTurrets)

        // Advance time: perimeter turrets fire at player, no minions spawn
        repeat(40) { engine.update(0.05f) }
        assertTrue(engine.state.minions.isEmpty())
        assertTrue(engine.state.projectiles.any { !it.fromPlayer })

        // Top mega-turret is locked/invincible while perimeter turrets remain
        val initialHp = boss.hp
        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = 0f,
                radius = 12f,
                fromPlayer = true,
                damage = 25
            )
        )
        engine.update(0.016f)
        assertEquals(initialHp, boss.hp)

        // Destroy all 8 perimeter turrets
        for (turret in engine.state.turrets) {
            engine.state.projectiles.add(
                Projectile(
                    x = turret.x,
                    y = turret.y,
                    vx = 0f,
                    vy = 0f,
                    radius = 12f,
                    fromPlayer = true,
                    damage = turret.maxHp
                )
            )
            engine.update(0.016f)
            assertTrue(turret.isDestroyed)
        }
        assertFalse(engine.state.hasActiveTurrets)

        // Top mega-turret is now unlocked, takes damage, and fires back
        engine.state.projectiles.clear()
        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = 0f,
                radius = 12f,
                fromPlayer = true,
                damage = 25
            )
        )
        engine.update(0.016f)
        assertEquals(initialHp - 25, boss.hp)

        boss.attackCooldown = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.count { !it.fromPlayer } >= 5)
    }

    @Test
    fun levelTwentyFourGiantLolloIngolfatoByTvAndSofa() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_24)
        engine.startGame()

        val boss = engine.state.boss
        assertFalse(engine.state.isLolloIngolfato)

        // Attack cycle produces TV, SOFA, and COMPUTER
        boss.attackCooldown = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.TV })
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.COMPUTER })

        boss.attackCooldown = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.SOFA })

        // Normal player bolt deals 0 damage while NOT ingolfato
        val initialHp = boss.hp
        engine.state.projectiles.clear()
        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = 0f,
                radius = 12f,
                fromPlayer = true,
                damage = 20
            )
        )
        engine.update(0.016f)
        assertEquals(initialHp, boss.hp)

        // Shooting a hostile TV reflects it back toward Lollo
        val tv = Projectile(
            x = boss.x,
            y = boss.y + 320f,
            vx = 0f,
            vy = 300f,
            radius = 34f,
            fromPlayer = false,
            damage = 1,
            kind = ProjectileKind.TV
        )
        val bolt = Projectile(
            x = tv.x,
            y = tv.y,
            vx = 0f,
            vy = -500f,
            radius = 12f,
            fromPlayer = true,
            damage = 8,
            kind = ProjectileKind.PLAYER_BOLT
        )
        engine.state.projectiles.add(tv)
        engine.state.projectiles.add(bolt)
        engine.update(0.016f)
        assertTrue(tv.fromPlayer)
        assertTrue(tv.vy < 0f)

        // Bounce TV into Lollo -> lolloTvHit = true, not ingolfato yet
        tv.x = boss.x
        tv.y = boss.y
        engine.update(0.016f)
        assertTrue(engine.state.lolloTvHit)
        assertFalse(engine.state.isLolloIngolfato)

        // Bounce SOFA into Lollo -> triggers INGOLFATO!
        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = -500f,
                radius = 38f,
                fromPlayer = true,
                damage = 1,
                kind = ProjectileKind.SOFA
            )
        )
        engine.update(0.016f)
        assertTrue(engine.state.isLolloIngolfato)

        // Now player bolt damages Giant Lollo!
        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = 0f,
                radius = 12f,
                fromPlayer = true,
                damage = 20
            )
        )
        engine.update(0.016f)
        assertEquals(initialHp - 20, boss.hp)
    }

    @Test
    fun levelTwentyFiveFuriousLolloHalfIngolfatoLasersAndRageBurst() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_25)
        engine.startGame()

        val boss = engine.state.boss
        // Advance time until Furious Lollo spawns a laser beam (> 2.6s)
        repeat(60) { engine.update(0.05f) }
        assertTrue(engine.state.lasers.isNotEmpty())

        // Bounce TV + SOFA into Furious Lollo -> triggers half-duration INGOLFATO (3.25s) & clears lasers
        engine.state.projectiles.clear()
        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = -500f,
                radius = 34f,
                fromPlayer = true,
                damage = 1,
                kind = ProjectileKind.TV
            )
        )
        engine.state.projectiles.add(
            Projectile(
                x = boss.x,
                y = boss.y,
                vx = 0f,
                vy = -500f,
                radius = 38f,
                fromPlayer = true,
                damage = 1,
                kind = ProjectileKind.SOFA
            )
        )
        engine.update(0.016f)
        assertTrue(engine.state.isLolloIngolfato)
        assertTrue(engine.state.lolloIngolfatoTimer in 3.0f..3.3f)
        assertTrue(engine.state.lasers.isEmpty())

        // Advance past 3.25s: ingolfato ends and Furious Lollo unleashes 8-way rage burst!
        engine.state.projectiles.clear()
        repeat(70) { engine.update(0.05f) }
        assertFalse(engine.state.isLolloIngolfato)
        assertTrue(engine.state.projectiles.count { !it.fromPlayer } >= 8)
    }

    @Test
    fun levelTwentySixTechLeadLolloThreePhasesKamikazeAndFarts() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_26)
        engine.startGame()

        val boss = engine.state.boss
        val player = engine.state.player
        assertEquals(1, boss.phase)

        // Advance time until kamikaze programmer spawns and chases player
        repeat(120) { engine.update(0.05f) }
        assertTrue(engine.state.minions.isNotEmpty())
        val kamikaze = engine.state.minions.first()
        val distBefore = kotlin.math.hypot(player.x - kamikaze.x, player.y - kamikaze.y)
        engine.update(0.05f)
        val distAfter = kotlin.math.hypot(player.x - kamikaze.x, player.y - kamikaze.y)
        assertTrue(distAfter < distBefore)

        // Kamikaze explodes on contact with player
        player.invulnerableTimer = 0f
        kamikaze.x = player.x
        kamikaze.y = player.y
        val minionsBefore = engine.state.minions.size
        val hpBefore = player.hp
        engine.update(0.016f)
        assertEquals(minionsBefore - 1, engine.state.minions.size)
        assertEquals(hpBefore - 1, player.hp)

        // Drop HP to trigger Phase 2 (<= 66%) and Phase 3 (<= 33%), no armor reforged
        boss.hp = (boss.maxHp * 0.65f).toInt()
        engine.state.projectiles.add(
            Projectile(x = boss.x, y = boss.y, vx = 0f, vy = 0f, radius = 12f, fromPlayer = true, damage = 10)
        )
        engine.update(0.016f)
        assertEquals(2, boss.phase)
        assertEquals(0, boss.armor)

        boss.hp = (boss.maxHp * 0.32f).toInt()
        engine.state.projectiles.add(
            Projectile(x = boss.x, y = boss.y, vx = 0f, vy = 0f, radius = 12f, fromPlayer = true, damage = 10)
        )
        engine.update(0.016f)
        assertEquals(3, boss.phase)
        assertEquals(0, boss.armor)

        // Phase 3: takes ONLY HALF damage (20 -> 10) and shoots FART_CLOUDs!
        val phase3Hp = boss.hp
        engine.state.projectiles.clear()
        engine.state.projectiles.add(
            Projectile(x = boss.x, y = boss.y, vx = 0f, vy = 0f, radius = 12f, fromPlayer = true, damage = 20)
        )
        engine.update(0.016f)
        assertEquals(phase3Hp - 10, boss.hp)

        boss.attackCooldown = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.FART_CLOUD })
    }

    @Test
    fun levelTwentySevenBoardOfDirectorsDualHealthBarsAndSuperCeo() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_27)
        engine.startGame()

        val lollo = engine.state.boss
        val ivanone = engine.state.secondaryBoss
        assertNotNull(ivanone)
        assertEquals("LOLLO ZURICH", lollo.name)
        assertEquals("IVANONE", ivanone!!.name)
        assertEquals(220, lollo.hp)
        assertEquals(220, ivanone.hp)

        // Both directors fire their signature attacks
        lollo.attackCooldown = 0.01f
        ivanone.attackCooldown = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.COMPLAINT })
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.CAMERA_FLASH })

        // Defeating Lollo alone does NOT trigger victory; Ivanone enters Super-CEO mode (phase 2 + heal)
        lollo.hp = 10
        ivanone.hp = 150
        engine.state.projectiles.clear()
        engine.state.projectiles.add(
            Projectile(x = lollo.x, y = lollo.y, vx = 0f, vy = 0f, radius = 12f, fromPlayer = true, damage = 20)
        )
        engine.update(0.016f)
        assertEquals(0, lollo.hp)
        assertEquals(GameStatus.PLAYING, engine.state.status)
        assertEquals(2, ivanone.phase)
        assertEquals(180, ivanone.hp)

        // Defeating Ivanone too triggers VICTORY!
        ivanone.hp = 10
        engine.state.projectiles.add(
            Projectile(x = ivanone.x, y = ivanone.y, vx = 0f, vy = 0f, radius = 12f, fromPlayer = true, damage = 20)
        )
        engine.update(0.016f)
        assertEquals(0, ivanone.hp)
        assertEquals(GameStatus.VICTORY, engine.state.status)
    }

    @Test
    fun levelTwentyEightBoardPlatformerPistolAndThreePhases() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_28)
        engine.startGame()

        val lollo = engine.state.boss
        val ivanone = engine.state.secondaryBoss
        assertNotNull(ivanone)
        assertTrue(engine.state.platforms.isNotEmpty())
        assertEquals(1, lollo.phase)
        assertEquals(1, ivanone!!.phase)

        // Platformer character fires horizontal pistol bullet with triggerMeleeAttack()
        engine.state.player.facingDir = 1
        assertTrue(engine.triggerMeleeAttack())
        val bullet = engine.state.projectiles.firstOrNull { it.fromPlayer && it.kind == ProjectileKind.PLAYER_BOLT }
        assertNotNull(bullet)
        assertTrue(bullet!!.vx > 0f)
        assertEquals(0f, bullet.vy, 0.01f)

        // Drop combined Board HP below 66% -> Phase 2 (shockwaves unlocked!)
        lollo.hp = 150
        ivanone.hp = 150
        engine.update(0.016f)
        assertEquals(2, lollo.phase)
        assertEquals(2, ivanone.phase)

        lollo.attackCooldown = 0.01f
        engine.update(0.02f)
        assertTrue(engine.state.projectiles.any { it.kind == ProjectileKind.SHOCKWAVE })

        // Drop combined Board HP below 33% -> Phase 3!
        lollo.hp = 70
        ivanone.hp = 70
        engine.update(0.016f)
        assertEquals(3, lollo.phase)
        assertEquals(3, ivanone.phase)
    }
}
