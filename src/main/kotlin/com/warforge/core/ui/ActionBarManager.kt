package com.warforge.core.ui

import com.warforge.core.WarforgeCore
import com.warforge.core.compat.VersionAdapter
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

/**
 * ActionBarManager — VersionAdapter 経由で 1.16.5〜1.21 対応。
 */
class ActionBarManager(private val plugin: WarforgeCore) {

    private val tasks = mutableMapOf<Player, BukkitTask>()

    fun startGameActionBar(player: Player, getTimeLeft: () -> Int) {
        stopActionBar(player)
        val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val wfp = plugin.playerManager.getPlayer(player.uniqueId) ?: return@Runnable
            val t = getTimeLeft()
            val timeStr = String.format("%02d:%02d", t / 60, t % 60)
            val bar = plugin.configManager.actionBarGameFormat
                .replace("{time}", timeStr)
                .replace("{kills}", wfp.kills.toString())
                .replace("{deaths}", wfp.deaths.toString())
                .replace("{kdr}", String.format("%.2f", wfp.kdr))
            VersionAdapter.sendActionBar(player, bar)
        }, 0L, 20L)
        tasks[player] = task
    }

    fun startLobbyActionBar(player: Player) {
        stopActionBar(player)
        val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val wfp = plugin.playerManager.getPlayer(player.uniqueId) ?: return@Runnable
            val bar = plugin.configManager.actionBarLobbyFormat
                .replace("{kills}", wfp.kills.toString())
                .replace("{deaths}", wfp.deaths.toString())
                .replace("{kdr}", String.format("%.2f", wfp.kdr))
            VersionAdapter.sendActionBar(player, bar)
        }, 0L, 40L)
        tasks[player] = task
    }

    fun stopActionBar(player: Player) {
        tasks[player]?.cancel()
        tasks.remove(player)
        VersionAdapter.sendActionBar(player, "")
    }

    fun stopAll() {
        tasks.values.forEach { it.cancel() }
        tasks.clear()
    }
}
