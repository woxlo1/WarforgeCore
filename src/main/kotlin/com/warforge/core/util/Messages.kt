package com.warforge.core.util

import com.warforge.core.WarforgeCore
import com.warforge.core.compat.VersionAdapter
import com.warforge.core.i18n.LangManager
import org.bukkit.entity.Player

/**
 * Messages — Adventure Component を廃止し §コード文字列で統一。
 * 1.16.5〜1.21 全対応。
 *
 * 修正: prefix() が replace("&","§") で全置換していたバグを修正。
 *      LangManager.colorize() 経由で文字単位チェックを行う。
 */
object Messages {

    private val plugin get() = WarforgeCore.getInstance()

    fun prefix(): String {
        val raw = plugin?.langManager?.getPrefix()
            ?: "&f&l[&6&lWarForgeCore&f&l] &r"
        // LangManager.colorize() で正しく変換（文字単位チェック）
        return LangManager.colorize(raw)
    }

    /** prefix付き §コード文字列 */
    fun prefixed(message: String): String = prefix() + VersionAdapter.color(message)

    /** §コード文字列（prefix なし）*/
    fun colored(message: String): String = VersionAdapter.color(message)

    /** プレイヤーに送信 */
    fun send(player: Player, message: String) {
        player.sendMessage(prefixed(message))
    }

    /** ActionBar に送信 */
    fun sendActionBar(player: Player, message: String) {
        VersionAdapter.sendActionBar(player, VersionAdapter.color(message))
    }
}
