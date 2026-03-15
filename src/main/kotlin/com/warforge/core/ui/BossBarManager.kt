package com.warforge.core.ui

import com.warforge.core.compat.BossBarCompat
import org.bukkit.entity.Player

/**
 * BossBarCompat に委譲するだけのラッパー。
 * Adventure BossBar を廃止 → Bukkit BossBar で 1.16.5〜1.21 対応。
 */
class BossBarManager {

    private val compat = BossBarCompat()

    fun showTimerBar(players: List<Player>, arenaId: Int, timeLeft: Int, maxTime: Int, modeName: String) =
        compat.showTimerBar(players, arenaId, timeLeft, maxTime, modeName)

    fun showPlayersBar(players: List<Player>, arenaId: Int, alive: Int, total: Int) =
        compat.showPlayersBar(players, arenaId, alive, total)

    fun showGoldBar(players: List<Player>, arenaId: Int, leaderName: String, collected: Int, goal: Int) =
        compat.showGoldBar(players, arenaId, leaderName, collected, goal)

    fun showDominationBar(players: List<Player>, arenaId: Int, redScore: Int, blueScore: Int, maxScore: Int) =
        compat.showDominationBar(players, arenaId, redScore, blueScore, maxScore)

    fun clearBars(players: List<Player>, arenaId: Int) =
        compat.clearBars(players, arenaId)

    fun shutdownAll() =
        compat.shutdownAll()
}
