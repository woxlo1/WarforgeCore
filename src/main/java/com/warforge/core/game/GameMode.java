package com.warforge.core.game;

import com.warforge.core.WarforgeCore;
import com.warforge.core.player.WFPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class GameMode {

    protected final WarforgeCore plugin;
    protected final int arenaId;
    protected GameState state = GameState.WAITING;
    protected final List<UUID> players = new ArrayList<>();
    protected int timeLeft = 0;

    public GameMode(WarforgeCore plugin, int arenaId) {
        this.plugin = plugin;
        this.arenaId = arenaId;
    }

    /** ゲーム開始 */
    public abstract void start();

    /** ゲーム終了 */
    public abstract void end();

    /** プレイヤーがキルしたとき */
    public abstract void onKill(Player killer, Player victim);

    /** プレイヤーが死亡したとき */
    public abstract void onDeath(Player victim);

    /** 毎秒呼ばれるtick処理 */
    public abstract void tick();

    /** 勝利条件チェック */
    public abstract boolean checkWinCondition();

    public void addPlayer(Player player) {
        players.add(player.getUniqueId());
        WFPlayer wfPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (wfPlayer != null) wfPlayer.setCurrentArenaId(arenaId);
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        WFPlayer wfPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (wfPlayer != null) wfPlayer.setCurrentArenaId(null);
    }

    public List<UUID> getPlayers() { return players; }
    public GameState getState() { return state; }
    public int getTimeLeft() { return timeLeft; }
    public int getArenaId() { return arenaId; }

    public enum GameState {
        WAITING, STARTING, IN_GAME, ENDING
    }
}
