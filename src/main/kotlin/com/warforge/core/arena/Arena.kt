package com.warforge.core.arena

import org.bukkit.Location
import kotlin.jvm.JvmOverloads

data class Arena @JvmOverloads constructor(
    val id: Int,
    val name: String,
    val world: String,
    val mode: String,
    val maxPlayers: Int = 16,
    val minPlayers: Int = 2,
    var enabled: Boolean = true,
    // 共通スポーン
    val spawnPoints: MutableList<Location> = mutableListOf(),
    var lobbySpawn: Location? = null,
    // チーム別スポーン
    val redSpawnPoints: MutableList<Location> = mutableListOf(),
    val blueSpawnPoints: MutableList<Location> = mutableListOf(),
    // 拠点制圧用
    val capturePoints: MutableList<CapturePoint> = mutableListOf(),
    // ゴールドラッシュ用
    val goldSpawnPoints: MutableList<Location> = mutableListOf(),
    // Heist用ネクサス
    var redNexus: Location? = null,
    var blueNexus: Location? = null,
    // Heist設定
    var heistWinGold: Int = 100,
    var heistGoldPerMine: Int = 5
)

data class CapturePoint(
    val id: Int,
    val name: String,
    var location: Location,
    var radius: Double = 5.0,
    var owner: CaptureOwner = CaptureOwner.NEUTRAL,
    var captureProgress: Double = 0.0
)

enum class CaptureOwner { NEUTRAL, RED, BLUE }
