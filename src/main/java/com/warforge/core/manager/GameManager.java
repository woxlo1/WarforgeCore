package com.warforge.core.manager;

import com.warforge.core.WarforgeCore;
import com.warforge.core.arena.Arena;
import com.warforge.core.game.GameMode;
import com.warforge.core.game.lobby.GameLobby;
import com.warforge.core.util.Messages;
import org.bukkit.entity.Player;

import java.util.*;

public class GameManager {

    private final WarforgeCore plugin;
    private final Map<Integer, GameMode> activeGames = new HashMap<>();
    private final Map<Integer, GameLobby> lobbies = new HashMap<>();

    public GameManager(WarforgeCore plugin) {
        this.plugin = plugin;
        initLobbies();
    }

    private void initLobbies() {
        plugin.getArenaManager().getAllArenas().forEach(arena -> {
            if (arena.getEnabled()) {
                lobbies.put(arena.getId(), new GameLobby(plugin, arena));
            }
        });
    }

    public void refreshLobby(Arena arena) {
        lobbies.put(arena.getId(), new GameLobby(plugin, arena));
    }

    /** プレイヤーをアリーナのロビーに参加させる */
    public boolean joinArena(Player player, int arenaId) {
        GameLobby lobby = lobbies.get(arenaId);
        if (lobby == null) {
            player.sendMessage(Messages.INSTANCE.prefixed("&cアリーナが見つかりません。"));
            return false;
        }
        // 既に試合中なら退出させる
        leaveCurrentGame(player);
        lobby.joinPlayer(player);
        return true;
    }

    /** 名前でアリーナを探して参加 */
    public boolean joinArenaByName(Player player, String name) {
        return plugin.getArenaManager().findByName(name)
            .map(a -> joinArena(player, a.getId()))
            .orElseGet(() -> {
                player.sendMessage(Messages.INSTANCE.prefixed("&cアリーナ &f" + name + " &cが見つかりません。"));
                return false;
            });
    }

    /** プレイヤーが現在参加している試合/ロビーから退出 */
    public void leaveCurrentGame(Player player) {
        UUID uuid = player.getUniqueId();

        // ロビーから退出
        lobbies.values().stream()
            .filter(l -> l.getWaitingPlayers().contains(uuid))
            .findFirst()
            .ifPresent(l -> l.leavePlayer(player));

        // 試合から退出
        activeGames.values().stream()
            .filter(g -> g.getPlayers().contains(uuid))
            .findFirst()
            .ifPresent(g -> g.removePlayer(player));
    }

    /** 試合終了後にロビーをリセット */
    public void onGameEnd(int arenaId) {
        activeGames.remove(arenaId);
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena != null) refreshLobby(arena);
    }

    public void shutdown() {
        activeGames.values().forEach(GameMode::end);
        activeGames.clear();
        lobbies.clear();
        plugin.getLogger().info("全ゲーム終了。");
    }

    public Map<Integer, GameMode> getActiveGames() { return activeGames; }
    public Map<Integer, GameLobby> getLobbies() { return lobbies; }

    public Optional<GameMode> getPlayerGame(UUID uuid) {
        return activeGames.values().stream()
            .filter(g -> g.getPlayers().contains(uuid))
            .findFirst();
    }

    public Optional<GameLobby> getPlayerLobby(UUID uuid) {
        return lobbies.values().stream()
            .filter(l -> l.getWaitingPlayers().contains(uuid))
            .findFirst();
    }
}
