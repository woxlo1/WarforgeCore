package com.warforge.core.ui

import com.warforge.core.WarforgeCore
import com.warforge.core.compat.VersionAdapter
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard

/**
 * ScoreboardManager — 1.16.5〜1.21 対応。
 * Adventure Component を使わず §カラーコードで統一。
 */
class ScoreboardManager(private val plugin: WarforgeCore) {

    private val version: String = plugin.description.version
    private val title: String = plugin.configManager.scoreboardTitle

    /**
     * 汎用的に Scoreboard と Objective を作成する
     */
    private fun create(title: String, id: String): Pair<Scoreboard?, Objective> {
        val sb = Bukkit.getScoreboardManager()?.newScoreboard
        val obj = VersionAdapter.registerObjective(sb, id, title)
        obj.displaySlot = DisplaySlot.SIDEBAR
        return sb to obj
    }

    /**
     * 行を追加する共通関数
     */
    private fun addLines(obj: Objective, vararg lines: String) {
        var score = lines.size
        for (line in lines) {
            obj.getScore(VersionAdapter.color(line)).score = score--
        }
    }

    /**
     * ロビー用スコアボード
     */
    fun showLobbyScoreboard(player: Player) {
        val (sb, obj) = create(title, "wf_lobby")

        addLines(
            obj,
            "&8▸ &7Version &f$version",
            "&8▸ &7Mode &aLobby",
            "&r ",
            "&8▸ &7Players &f${Bukkit.getOnlinePlayers().size}",
            "&r  ",
            "&8github.com/woxlo1"
        )

        if (sb != null) {
            player.scoreboard = sb
        }
    }

    /**
     * ゲーム中スコアボード
     */
    fun showGameScoreboard(player: Player, modeName: String, teamInfo: String? = null) {
        val (sb, obj) = create(title, "wf_game")

        val lines = mutableListOf(
            "&8▸ &7Mode &b$modeName",
            "&r "
        )

        if (teamInfo != null) {
            lines += "&8▸ &7Team &f$teamInfo"
            lines += "&r  "
        }

        lines += "&8▸ &7Version &f$version"
        lines += "&r   "
        lines += "&8github.com/woxlo1"

        addLines(obj, *lines.toTypedArray())
        if (sb != null) {
            player.scoreboard = sb
        }
    }

    /**
     * スコアボードをクリア
     */
    fun clearScoreboard(player: Player) {
        player.scoreboard = Bukkit.getScoreboardManager()?.newScoreboard ?: player.scoreboard
    }
}
