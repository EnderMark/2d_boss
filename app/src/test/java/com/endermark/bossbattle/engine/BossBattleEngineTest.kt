package com.endermark.bossbattle.engine

import com.endermark.bossbattle.model.Boss
import com.endermark.bossbattle.model.BossLevel
import com.endermark.bossbattle.model.GameStatus
import com.endermark.bossbattle.model.LevelCatalog
import com.endermark.bossbattle.model.Player
import com.endermark.bossbattle.model.Projectile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BossBattleEngineTest {

    @Test
    fun levelCatalogStartsWithLevelOneGuardianoDombra() {
        assertEquals(1, LevelCatalog.levels.size)
        val level1 = LevelCatalog.levels.first()
        assertEquals(1, level1.id)
        assertEquals("LIVELLO 1", level1.title)
        assertEquals("GUARDIANO D'OMBRA", level1.bossName)
        assertEquals(100, level1.bossMaxHp)
        assertEquals(level1, LevelCatalog.findById(1))
        assertEquals(level1, LevelCatalog.findById(999))
    }

    @Test
    fun engineInitializesBossFromSelectedLevel() {
        val customLevel = BossLevel(
            id = 2,
            title = "LIVELLO 2",
            bossName = "TITANO DI FUOCO",
            subtitle = "Test",
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

        // Place a player projectile directly on the boss
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
}
