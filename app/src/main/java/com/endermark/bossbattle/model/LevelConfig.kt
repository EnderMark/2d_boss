package com.endermark.bossbattle.model

enum class BattleMode {
    SHOOTER,
    MELEE_PLATFORMER
}

data class BossLevel(
    val id: Int,
    val title: String,
    val bossName: String,
    val subtitle: String,
    val mode: BattleMode,
    val bossMaxHp: Int,
    val bossBaseSpeed: Float,
    val bossAttackInterval: Float,
    val bossEnragedAttackInterval: Float,
    val bossProjectileSpeed: Float,
    val bossEnragedProjectileSpeed: Float,
    val isUnlocked: Boolean = true
)

object LevelCatalog {
    val LEVEL_1 = BossLevel(
        id = 1,
        title = "LIVELLO 1",
        bossName = "GUARDIANO D'OMBRA",
        subtitle = "Navicella • Schiva i colpi e distruggi il custode",
        mode = BattleMode.SHOOTER,
        bossMaxHp = 100,
        bossBaseSpeed = 240f,
        bossAttackInterval = 1.1f,
        bossEnragedAttackInterval = 0.9f,
        bossProjectileSpeed = 520f,
        bossEnragedProjectileSpeed = 600f,
        isUnlocked = true
    )

    val LEVEL_2 = BossLevel(
        id = 2,
        title = "LIVELLO 2",
        bossName = "COLOSSO DI FERRO",
        subtitle = "Mischia • Salta, fai doppio salto e colpisci da vicino",
        mode = BattleMode.MELEE_PLATFORMER,
        bossMaxHp = 120,
        bossBaseSpeed = 165f,
        bossAttackInterval = 2.0f,
        bossEnragedAttackInterval = 1.45f,
        bossProjectileSpeed = 460f,
        bossEnragedProjectileSpeed = 580f,
        isUnlocked = true
    )

    val levels: List<BossLevel> = listOf(LEVEL_1, LEVEL_2)

    fun findById(id: Int): BossLevel {
        return levels.firstOrNull { it.id == id } ?: LEVEL_1
    }
}
