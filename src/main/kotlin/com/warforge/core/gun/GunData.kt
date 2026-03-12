package com.warforge.core.gun

data class GunData(
    val id: String,
    val displayName: String,

    // ─── ダメージ設定 ───
    val damage: Double,
    val headshotMultiplier: Double = 2.0,
    val bodyMultiplier: Double = 1.0,
    val legMultiplier: Double = 0.75,
    val armorPenetration: Double = 0.0,

    // ─── 射撃設定 ───
    val fireRate: Long = 200L,
    val fireMode: FireMode = FireMode.AUTO,
    val burstCount: Int = 3,
    val magazineSize: Int = 30,
    val reserveAmmo: Int = 90,

    // ─── 弾道設定 ───
    val bulletSpeed: Double = 5.0,
    val range: Double = 50.0,
    val dropOff: Double = 0.1,
    val bulletCount: Int = 1,
    val spreadAngle: Double = 0.0,

    // ─── リコイル設定 ───
    val recoil: Double = 0.5,
    val recoilVertical: Double = 0.3,
    val recoilHorizontal: Double = 0.15,
    val recoilRecovery: Double = 0.8,

    // ─── リロード設定 ───
    val reloadTime: Long = 2000L,
    val tacticalReloadTime: Long = 1500L,

    // ─── ADS設定 ───
    val adsSpeedMultiplier: Double = 0.7,
    val adsAccuracy: Double = 0.9,

    // ─── 音・エフェクト ───
    val shootSound: String = "ENTITY_GENERIC_EXPLODE",
    val shootSoundVolume: Float = 0.3f,
    val shootSoundPitch: Float = 2.0f,
    val suppressorEquipped: Boolean = false,

    // ─── 価格・レアリティ ───
    val price: Double = 0.0,
    val rarity: GunRarity = GunRarity.COMMON,

    val gunType: GunType = GunType.RIFLE
)

enum class GunType { PISTOL, RIFLE, SHOTGUN, SNIPER, SMG, LMG, LAUNCHER }
enum class FireMode { AUTO, BURST, SEMI }
enum class GunRarity(val color: String) {
    COMMON("&7"), UNCOMMON("&a"), RARE("&9"), EPIC("&5"), LEGENDARY("&6")
}
