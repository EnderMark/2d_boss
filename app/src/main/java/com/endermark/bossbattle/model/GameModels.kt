package com.endermark.bossbattle.model

enum class GameStatus {
    READY,
    PLAYING,
    VICTORY,
    DEFEAT
}

enum class BossAction {
    PATROL,
    TELEGRAPH_SHOCKWAVE,
    TELEGRAPH_CHARGE,
    CHARGING
}

enum class ProjectileKind {
    PLAYER_BOLT,
    BOSS_ORB,
    SHOCKWAVE,
    LANTERN,
    COMPLAINT,
    DRONE,
    CAMERA_FLASH,
    TAX_FORM,
    WEIRD_TOOTH,
    HEART,
    SCREAM_WAVE,
    RICE_CHICKEN,
    DATING_FLEX,
    HOT_STONE
}

enum class WeaponType(val displayName: String) {
    SWORD("SPADA"),
    HAMMER("MARTELLO"),
    KATANA("KATANA"),
    SPEAR("LANCIA")
}

enum class PickupKind(val label: String) {
    WEAPON_HAMMER("MARTELLO"),
    WEAPON_KATANA("KATANA"),
    WEAPON_SPEAR("LANCIA"),
    HEAL("+1 VITA"),
    SPEED_BOOST("VELOCITÀ"),
    POWER_SURGE("FURIA x2")
}

enum class GameSoundEvent {
    JUMP,
    DOUBLE_JUMP,
    ATTACK,
    PLAYER_SHOT,
    BOSS_HIT,
    PLAYER_HIT,
    SHIELD_BLOCK,
    BOSS_ATTACK,
    BOSS_CHARGE,
    BOSS_VANISH,
    CAMERA_SHUTTER,
    PICKUP,
    ARMOR_BREAK,
    VICTORY,
    DEFEAT
}

data class ArenaPickup(
    val x: Float,
    val y: Float,
    val radius: Float = 32f,
    val kind: PickupKind,
    var life: Float = 11f
)

data class Player(
    var x: Float,
    var y: Float,
    val radius: Float = 36f,
    var hp: Int = MAX_HP,
    val maxHp: Int = MAX_HP,
    var invulnerableTimer: Float = 0f,
    var fireCooldown: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var onGround: Boolean = true,
    var jumpsRemaining: Int = MAX_JUMPS,
    var facingDir: Int = 1,
    var meleeTimer: Float = 0f,
    var meleeCooldown: Float = 0f,
    var meleeHitApplied: Boolean = false,
    var isBlocking: Boolean = false,
    var shieldFlashTimer: Float = 0f,
    var weapon: WeaponType = WeaponType.SWORD,
    var weaponTimer: Float = 0f,
    var speedBoostTimer: Float = 0f,
    var damageBoostTimer: Float = 0f
) {
    val isInvulnerable: Boolean
        get() = invulnerableTimer > 0f

    val isAttacking: Boolean
        get() = meleeTimer > 0f

    val hasSpeedBoost: Boolean
        get() = speedBoostTimer > 0f

    val hasDamageBoost: Boolean
        get() = damageBoostTimer > 0f

    companion object {
        const val MAX_HP = 5
        const val MAX_JUMPS = 2
    }
}

data class Boss(
    val name: String = "GUARDIANO D'OMBRA",
    var x: Float,
    var y: Float,
    val radius: Float = 88f,
    var hp: Int = MAX_HP,
    val maxHp: Int = MAX_HP,
    var displayedHp: Float = MAX_HP.toFloat(),
    var armor: Int = 0,
    var maxArmor: Int = 0,
    var phase: Int = 1,
    var stunTimer: Float = 0f,
    var velocityX: Float = 240f,
    var attackCooldown: Float = 1.0f,
    var hitFlashTimer: Float = 0f,
    var action: BossAction = BossAction.PATROL,
    var actionTimer: Float = 0f,
    var facingDir: Int = -1,
    var vanishTimer: Float = 0f,
    var hitsSinceVanish: Int = 0
) {
    val hpRatio: Float
        get() = (hp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)

    val displayedHpRatio: Float
        get() = (displayedHp / maxHp.toFloat()).coerceIn(0f, 1f)

    val armorRatio: Float
        get() = if (maxArmor > 0) (armor.toFloat() / maxArmor.toFloat()).coerceIn(0f, 1f) else 0f

    val hasArmor: Boolean
        get() = armor > 0

    val isStunned: Boolean
        get() = stunTimer > 0f

    val isEnraged: Boolean
        get() = hp <= maxHp / 2 || phase >= 2

    val isVanished: Boolean
        get() = vanishTimer > 0f

    companion object {
        const val MAX_HP = 100
    }
}

data class Projectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val fromPlayer: Boolean,
    val damage: Int,
    val kind: ProjectileKind = if (fromPlayer) ProjectileKind.PLAYER_BOLT else ProjectileKind.BOSS_ORB,
    val label: String? = null
) {
    val isShockwave: Boolean
        get() = kind == ProjectileKind.SHOCKWAVE
}

data class HitParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Int
)

data class GameState(
    val level: BossLevel,
    val worldWidth: Float,
    val worldHeight: Float,
    var status: GameStatus,
    val player: Player,
    val boss: Boss,
    val projectiles: MutableList<Projectile> = mutableListOf(),
    val particles: MutableList<HitParticle> = mutableListOf(),
    val pickups: MutableList<ArenaPickup> = mutableListOf(),
    var elapsedTime: Float = 0f,
    var endStateCooldown: Float = 0f,
    val groundY: Float = 1260f
)
