package com.endermark.bossbattle.model

data class BossLevel(
    val id: Int,
    val title: String,
    val bossName: String,
    val subtitle: String,
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
        subtitle = "Il custode dell'arena",
        bossMaxHp = 100,
        bossBaseSpeed = 240f,
        bossAttackInterval = 1.1f,
        bossEnragedAttackInterval = 0.9f,
        bossProjectileSpeed = 520f,
        bossEnragedProjectileSpeed = 600f,
        isUnlocked = true
    )

    val levels: List<BossLevel> = listOf(LEVEL_1)

    fun findById(id: Int): BossLevel {
        return levels.firstOrNull { it.id == id } ?: LEVEL_1
    }
}
