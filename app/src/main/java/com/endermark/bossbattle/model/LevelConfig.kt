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
    STUDIO,
    ZURICH_TAX,
    PARIS,
    UBS_ALPS,
    GYM_DEV,
    GOOGLE_ZURICH,
    HANOI_SPA,
    OMEGA_FINALE
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
    val bossMaxArmor: Int = 0,
    val enablePickups: Boolean = false,
    val multiPhase: Boolean = false,
    val enablePlatforms: Boolean = false,
    val enableLasers: Boolean = false,
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

    val LEVEL_6 = BossLevel(
        id = 6,
        title = "LIVELLO 6",
        bossName = "ALESSIO DELLE TASSE",
        subtitle = "Zurigo • Tasse, superiorità morale e lo strano dente",
        mode = BattleMode.MELEE_PLATFORMER,
        arenaTheme = ArenaTheme.ZURICH_TAX,
        hasShield = false,
        bossMaxHp = 170,
        bossBaseSpeed = 205f,
        bossAttackInterval = 1.5f,
        bossEnragedAttackInterval = 1.1f,
        bossProjectileSpeed = 530f,
        bossEnragedProjectileSpeed = 650f,
        isUnlocked = true
    )

    val LEVEL_7 = BossLevel(
        id = 7,
        title = "LIVELLO 7",
        bossName = "LOUISE",
        subtitle = "Parigi • Occhi azzurri: ti seduce e poi ti evita",
        mode = BattleMode.MELEE_PLATFORMER,
        arenaTheme = ArenaTheme.PARIS,
        hasShield = false,
        bossMaxHp = 175,
        bossBaseSpeed = 265f,
        bossAttackInterval = 1.45f,
        bossEnragedAttackInterval = 1.05f,
        bossProjectileSpeed = 520f,
        bossEnragedProjectileSpeed = 640f,
        isUnlocked = true
    )

    val LEVEL_8 = BossLevel(
        id = 8,
        title = "LIVELLO 8",
        bossName = "DAISY",
        subtitle = "UBS • Urla calabresi e lamentele per le hike mancate",
        mode = BattleMode.MELEE_PLATFORMER,
        arenaTheme = ArenaTheme.UBS_ALPS,
        hasShield = false,
        bossMaxHp = 180,
        bossBaseSpeed = 240f,
        bossAttackInterval = 1.4f,
        bossEnragedAttackInterval = 1.0f,
        bossProjectileSpeed = 550f,
        bossEnragedProjectileSpeed = 680f,
        isUnlocked = true
    )

    val LEVEL_9 = BossLevel(
        id = 9,
        title = "LIVELLO 9",
        bossName = "ANDREA DATTERO",
        subtitle = "Felpa Moschino • Grappling, 200g riso e pollo e bug ovunque",
        mode = BattleMode.MELEE_PLATFORMER,
        arenaTheme = ArenaTheme.GYM_DEV,
        hasShield = false,
        bossMaxHp = 190,
        bossBaseSpeed = 275f,
        bossAttackInterval = 1.35f,
        bossEnragedAttackInterval = 0.98f,
        bossProjectileSpeed = 540f,
        bossEnragedProjectileSpeed = 660f,
        isUnlocked = true
    )

    val LEVEL_10 = BossLevel(
        id = 10,
        title = "LIVELLO 10",
        bossName = "MARCO DI GOOGLE",
        subtitle = "Porsche bianca • Dating, milioni e flash fotografici",
        mode = BattleMode.MELEE_PLATFORMER,
        arenaTheme = ArenaTheme.GOOGLE_ZURICH,
        hasShield = false,
        bossMaxHp = 220,
        bossBaseSpeed = 290f,
        bossAttackInterval = 1.35f,
        bossEnragedAttackInterval = 0.95f,
        bossProjectileSpeed = 590f,
        bossEnragedProjectileSpeed = 720f,
        isUnlocked = true
    )

    val LEVEL_11 = BossLevel(
        id = 11,
        title = "LIVELLO 11",
        bossName = "CARAMEL",
        subtitle = "Hanoi Spa • 3 Fasi, armatura da spezzare, armi e power-up",
        mode = BattleMode.MELEE_PLATFORMER,
        arenaTheme = ArenaTheme.HANOI_SPA,
        hasShield = false,
        bossMaxHp = 260,
        bossMaxArmor = 80,
        enablePickups = true,
        multiPhase = true,
        bossBaseSpeed = 235f,
        bossAttackInterval = 1.45f,
        bossEnragedAttackInterval = 1.0f,
        bossProjectileSpeed = 540f,
        bossEnragedProjectileSpeed = 680f,
        isUnlocked = true
    )

    val LEVEL_12 = BossLevel(
        id = 12,
        title = "LIVELLO 12 • ARCADE",
        bossName = "LOLLO ZURICH",
        subtitle = "Navicella • Pioggia di lamentele da Zurigo",
        mode = BattleMode.SHOOTER,
        arenaTheme = ArenaTheme.ZURICH,
        hasShield = false,
        bossMaxHp = 130,
        bossBaseSpeed = 260f,
        bossAttackInterval = 1.05f,
        bossEnragedAttackInterval = 0.80f,
        bossProjectileSpeed = 520f,
        bossEnragedProjectileSpeed = 620f,
        isUnlocked = true
    )

    val LEVEL_13 = BossLevel(
        id = 13,
        title = "LIVELLO 13 • ARCADE",
        bossName = "IVANONE",
        subtitle = "Navicella • Droni, flash fotografici e sparizioni",
        mode = BattleMode.SHOOTER,
        arenaTheme = ArenaTheme.STUDIO,
        hasShield = false,
        bossMaxHp = 140,
        bossBaseSpeed = 280f,
        bossAttackInterval = 1.0f,
        bossEnragedAttackInterval = 0.78f,
        bossProjectileSpeed = 540f,
        bossEnragedProjectileSpeed = 650f,
        isUnlocked = true
    )

    val LEVEL_14 = BossLevel(
        id = 14,
        title = "LIVELLO 14 • ARCADE",
        bossName = "ALESSIO DELLE TASSE",
        subtitle = "Navicella • Moduli CHF e lancio dello strano dente",
        mode = BattleMode.SHOOTER,
        arenaTheme = ArenaTheme.ZURICH_TAX,
        hasShield = false,
        bossMaxHp = 150,
        bossBaseSpeed = 250f,
        bossAttackInterval = 1.0f,
        bossEnragedAttackInterval = 0.75f,
        bossProjectileSpeed = 540f,
        bossEnragedProjectileSpeed = 660f,
        isUnlocked = true
    )

    val LEVEL_15 = BossLevel(
        id = 15,
        title = "LIVELLO 15 • ARCADE",
        bossName = "LOUISE",
        subtitle = "Navicella • Cuori seducenti e schivate parigine",
        mode = BattleMode.SHOOTER,
        arenaTheme = ArenaTheme.PARIS,
        hasShield = false,
        bossMaxHp = 155,
        bossBaseSpeed = 310f,
        bossAttackInterval = 0.98f,
        bossEnragedAttackInterval = 0.74f,
        bossProjectileSpeed = 530f,
        bossEnragedProjectileSpeed = 640f,
        isUnlocked = true
    )

    val LEVEL_16 = BossLevel(
        id = 16,
        title = "LIVELLO 16 • ARCADE",
        bossName = "DAISY",
        subtitle = "Navicella • Urla calabresi e lamentele UBS sulle hike",
        mode = BattleMode.SHOOTER,
        arenaTheme = ArenaTheme.UBS_ALPS,
        hasShield = false,
        bossMaxHp = 160,
        bossBaseSpeed = 285f,
        bossAttackInterval = 0.95f,
        bossEnragedAttackInterval = 0.72f,
        bossProjectileSpeed = 560f,
        bossEnragedProjectileSpeed = 680f,
        isUnlocked = true
    )

    val LEVEL_17 = BossLevel(
        id = 17,
        title = "LIVELLO 17 • ARCADE",
        bossName = "ANDREA DATTERO",
        subtitle = "Navicella • 200g riso e pollo e bug da 10x engineer",
        mode = BattleMode.SHOOTER,
        arenaTheme = ArenaTheme.GYM_DEV,
        hasShield = false,
        bossMaxHp = 165,
        bossBaseSpeed = 295f,
        bossAttackInterval = 0.95f,
        bossEnragedAttackInterval = 0.72f,
        bossProjectileSpeed = 550f,
        bossEnragedProjectileSpeed = 660f,
        isUnlocked = true
    )

    val LEVEL_18 = BossLevel(
        id = 18,
        title = "LIVELLO 18 • ARCADE",
        bossName = "MARCO DI GOOGLE",
        subtitle = "Navicella • Porsche bianca, milioni e flash fotografici",
        mode = BattleMode.SHOOTER,
        arenaTheme = ArenaTheme.GOOGLE_ZURICH,
        hasShield = false,
        bossMaxHp = 180,
        bossBaseSpeed = 340f,
        bossAttackInterval = 0.92f,
        bossEnragedAttackInterval = 0.68f,
        bossProjectileSpeed = 580f,
        bossEnragedProjectileSpeed = 700f,
        isUnlocked = true
    )

    val LEVEL_19 = BossLevel(
        id = 19,
        title = "LIVELLO 19 • ARCADE",
        bossName = "CARAMEL",
        subtitle = "Navicella • Hanoi Spa a 3 fasi con armatura e pietre calde",
        mode = BattleMode.SHOOTER,
        arenaTheme = ArenaTheme.HANOI_SPA,
        hasShield = false,
        bossMaxHp = 210,
        bossMaxArmor = 70,
        enablePickups = false,
        multiPhase = true,
        bossBaseSpeed = 280f,
        bossAttackInterval = 0.95f,
        bossEnragedAttackInterval = 0.70f,
        bossProjectileSpeed = 560f,
        bossEnragedProjectileSpeed = 680f,
        isUnlocked = true
    )

    val LEVEL_20 = BossLevel(
        id = 20,
        title = "LIVELLO 20 • FINALE",
        bossName = "OMEGA ARCHITECT",
        subtitle = "3 Scenari & Fasi • Piattaforme mobili, raggi laser, armatura e power-up",
        mode = BattleMode.MELEE_PLATFORMER,
        arenaTheme = ArenaTheme.OMEGA_FINALE,
        hasShield = false,
        bossMaxHp = 360,
        bossMaxArmor = 100,
        enablePickups = true,
        multiPhase = true,
        enablePlatforms = true,
        enableLasers = true,
        bossBaseSpeed = 260f,
        bossAttackInterval = 1.40f,
        bossEnragedAttackInterval = 0.95f,
        bossProjectileSpeed = 570f,
        bossEnragedProjectileSpeed = 700f,
        isUnlocked = true
    )

    val levels: List<BossLevel> = listOf(
        LEVEL_1,
        LEVEL_2,
        LEVEL_3,
        LEVEL_4,
        LEVEL_5,
        LEVEL_6,
        LEVEL_7,
        LEVEL_8,
        LEVEL_9,
        LEVEL_10,
        LEVEL_11,
        LEVEL_12,
        LEVEL_13,
        LEVEL_14,
        LEVEL_15,
        LEVEL_16,
        LEVEL_17,
        LEVEL_18,
        LEVEL_19,
        LEVEL_20
    )

    fun findById(id: Int): BossLevel {
        return levels.firstOrNull { it.id == id } ?: LEVEL_1
    }
}
