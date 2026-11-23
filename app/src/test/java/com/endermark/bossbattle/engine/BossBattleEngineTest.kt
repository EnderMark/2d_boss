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
    fun levelCatalogContainsAllNineteenLevels() {
        assertEquals(19, LevelCatalog.levels.size)

        assertEquals("GUARDIANO D'OMBRA", LevelCatalog.findById(1).bossName)
        assertEquals("COLOSSO DI FERRO", LevelCatalog.findById(2).bossName)
        assertEquals("CHAN", LevelCatalog.findById(3).bossName)
        assertEquals("LOLLO ZURICH", LevelCatalog.findById(4).bossName)
        assertEquals("IVANONE", LevelCatalog.findById(5).bossName)
        assertEquals("ALESSIO DELLE TASSE", LevelCatalog.findById(6).bossName)
        assertEquals("LOUISE", LevelCatalog.findById(7).bossName)
        assertEquals("DAISY", LevelCatalog.findById(8).bossName)
        assertEquals("ANDREA DATTERO", LevelCatalog.findById(9).bossName)
        assertEquals("MARCO DI GOOGLE", LevelCatalog.findById(10).bossName)
        assertEquals("CARAMEL", LevelCatalog.findById(11).bossName)

        // Arcade (Level 1 shooter-style) remixes from Lollo Zurich onward (Levels 12-19)
        for (id in 12..19) {
            assertEquals(BattleMode.SHOOTER, LevelCatalog.findById(id).mode)
        }
        assertEquals("LOLLO ZURICH", LevelCatalog.findById(12).bossName)
        assertEquals("CARAMEL", LevelCatalog.findById(19).bossName)
        assertEquals(70, LevelCatalog.findById(19).bossMaxArmor)
        assertTrue(LevelCatalog.findById(19).multiPhase)
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
}
