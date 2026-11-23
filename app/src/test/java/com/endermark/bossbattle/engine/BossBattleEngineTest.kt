package com.endermark.bossbattle.engine

import com.endermark.bossbattle.model.ArenaTheme
import com.endermark.bossbattle.model.BattleMode
import com.endermark.bossbattle.model.Boss
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.LevelCatalog
import com.endermark.bossbattle.model.Player
import com.endermark.bossbattle.model.Projectile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BossBattleEngineTest {

    @Test
    fun levelCatalogContainsAllThreeLevels() {
        assertEquals(3, LevelCatalog.levels.size)

        val level1 = LevelCatalog.levels[0]
        assertEquals(1, level1.id)
        assertEquals("LIVELLO 1", level1.title)
        assertEquals("GUARDIANO D'OMBRA", level1.bossName)
        assertEquals(BattleMode.SHOOTER, level1.mode)

        val level2 = LevelCatalog.levels[1]
        assertEquals(2, level2.id)
        assertEquals("LIVELLO 2", level2.title)
        assertEquals("COLOSSO DI FERRO", level2.bossName)
        assertEquals(BattleMode.MELEE_PLATFORMER, level2.mode)
        assertFalse(level2.hasShield)

        val level3 = LevelCatalog.levels[2]
        assertEquals(3, level3.id)
        assertEquals("LIVELLO 3", level3.title)
        assertEquals("CHAN", level3.bossName)
        assertEquals(BattleMode.MELEE_PLATFORMER, level3.mode)
        assertEquals(ArenaTheme.HANOI, level3.arenaTheme)
        assertTrue(level3.hasShield)

        assertEquals(level1, LevelCatalog.findById(1))
        assertEquals(level2, LevelCatalog.findById(2))
        assertEquals(level3, LevelCatalog.findById(3))
        assertEquals(level1, LevelCatalog.findById(999))
    }

    @Test
    fun engineInitializesBossFromSelectedLevel() {
        val customLevel = BossLevel(
            id = 4,
            title = "LIVELLO 4",
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
    fun levelTwoUsesMeleeOnlyWithoutPlayerProjectiles() {
        val engine = BossBattleEngine(level = LevelCatalog.LEVEL_2)
        engine.startGame()

        val player = engine.state.player
        val boss = engine.state.boss
        val initialBossHp = boss.hp

        player.x = 120f
        boss.x = 850f
        assertTrue(engine.triggerMeleeAttack())
        engine.update(0.016f)
        assertEquals(initialBossHp, boss.hp)
        assertTrue(engine.state.projectiles.none { it.fromPlayer })

        repeat(10) { engine.update(0.05f) }
        boss.x = 500f
        player.x = boss.x - boss.radius - 40f
        player.facingDir = 1
        assertTrue(engine.triggerMeleeAttack())
        engine.update(0.016f)
        assertTrue(boss.hp < initialBossHp)
        assertTrue(engine.state.projectiles.none { it.fromPlayer })
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
                damage = 1
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
}
