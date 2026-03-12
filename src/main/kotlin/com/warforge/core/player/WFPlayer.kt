package com.warforge.core.player

import java.util.UUID

class WFPlayer(
    val uuid: UUID,
    var name: String,
    var kills: Int = 0,
    var deaths: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var coins: Double = 0.0,
    var assists: Int = 0,
    var totalGames: Int = 0,
    var rankPoints: Int = 0
) {
    var currentArenaId: Int? = null

    val kdr: Double get() = if (deaths == 0) kills.toDouble() else kills.toDouble() / deaths.toDouble()
    val winRate: Double get() = if (totalGames == 0) 0.0 else wins.toDouble() / totalGames.toDouble() * 100.0
}
