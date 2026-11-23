package com.endermark.bossbattle.model

enum class BattleMode {
    SHOOTER,
    MELEE_PLATFORMER
}

enum class ArenaTheme {
    SPACE,
    IRON_ARENA,
    HANOI,
    ZURICH,
    STUDIO
}

data class BossLevel(
    val id: Int,
    val title: String,
    val bossName: String,
    val subtitle: String,
    val mode: BattleMode,
    val arenaTheme: ArenaTheme,
    val hasShield: Boolean = false,
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
        arenaTheme = ArenaTheme.SPACE,
        hasShield = false,
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
        arenaTheme = ArenaTheme.IRON_ARENA,
        hasShield = false,
        bossMaxHp = 120,
        bossBaseSpeed = 165f,
        bossAttackInterval = 2.0f,
        bossEnragedAttackInterval = 1.45f,
        bossProjectileSpeed = 460f,
        bossEnragedProjectileSpeed = 580f,
        isUnlocked = true
    )

    val LEVEL_3 = BossLevel(
        id = 3,
        title = "LIVELLO 3",
        bossName = "CHAN",
        subtitle = "Hanoi • Spada e scudo contro la guerriera delle lanterne",
        mode = BattleMode.MELEE_PLATFORMER,
        arenaTheme = ArenaTheme.HANOI,
        hasShield = true,
        bossMaxHp = 140,
        bossBaseSpeed = 230f,
        bossAttackInterval = 1.65f,
        bossEnragedAttackInterval = 1.2f,
        bossProjectileSpeed = 540f,
        bossEnragedProjectileSpeed = 660f,
        isUnlocked = true
    )

    val LEVEL_4 = BossLevel(
        id = 4,
        title = "LIVELLO 4",
        bossName = "LOLLO ZURICH",
        subtitle = "Zurigo • Senza scudo contro lamentele a raffica",
        mode = BattleMode.MELEE_PLATFORMER,
        arenaTheme = ArenaTheme.ZURICH,
        hasShield = false,
        bossMaxHp = 150,
        bossBaseSpeed = 210f,
        bossAttackInterval = 1.55f,
        bossEnragedAttackInterval = 1.1f,
        bossProjectileSpeed = 510f,
        bossEnragedProjectileSpeed = 630f,
        isUnlocked = true
    )

    val LEVEL_5 = BossLevel(
        id = 5,
        title = "LIVELLO 5",
        bossName = "IVANONE",
        subtitle = "Droni, flash fotografici e sparizioni improvvise",
        mode = BattleMode.MELEE_PLATFORMER,
        arenaTheme = ArenaTheme.STUDIO,
        hasShield = false,
        bossMaxHp = 160,
        bossBaseSpeed = 250f,
        bossAttackInterval = 1.45f,
        bossEnragedAttackInterval = 1.05f,
        bossProjectileSpeed = 560f,
        bossEnragedProjectileSpeed = 690f,
        isUnlocked = true
    )

    val levels: List<BossLevel> = listOf(LEVEL_1, LEVEL_2, LEVEL_3, LEVEL_4, LEVEL_5)

    fun findById(id: Int): BossLevel {
        return levels.firstOrNull { it.id == id } ?: LEVEL_1
    }
}
