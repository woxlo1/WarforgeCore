package com.warforge.core.game.lobby;

import com.warforge.core.WarforgeCore;
import com.warforge.core.arena.Arena;
import com.warforge.core.game.GameMode;
import com.warforge.core.game.modes.*;
import com.warforge.core.util.Messages;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameLobby {

    private final WarforgeCore plugin;
    private final Arena arena;
    private final List<UUID> waitingPlayers = new ArrayList<>();
    private LobbyState state = LobbyState.WAITING;
    private int countdown;
    private BukkitTask countdownTask;

    public GameLobby(WarforgeCore plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public void joinPlayer(Player player) {
        if (waitingPlayers.contains(player.getUniqueId())) return;
        if (waitingPlayers.size() >= arena.getMaxPlayers()) {
            player.sendMessage(Messages.INSTANCE.prefixed("&c" + arena.getName() + " は満員です！"));
            return;
        }
        if (state == LobbyState.IN_GAME) {
            player.sendMessage(Messages.INSTANCE.prefixed("&c試合がすでに進行中です！"));
            return;
        }

        waitingPlayers.add(player.getUniqueId());

        // ロビースポーンにTP
        if (arena.getLobbySpawn() != null) {
            player.teleport(arena.getLobbySpawn());
        }

        // UI初期化
        plugin.getUiManager().getScoreboardManager().showLobbyScoreboard(player);
        plugin.getUiManager().getActionBarManager().startLobbyActionBar(player);

        broadcastLobby("&f" + player.getName() + " &7が参加！ &e" +
            waitingPlayers.size() + "/" + arena.getMaxPlayers());

        checkStartCondition();
    }

    public void leavePlayer(Player player) {
        waitingPlayers.remove(player.getUniqueId());
        plugin.getUiManager().getScoreboardManager().clearScoreboard(player);
        plugin.getUiManager().getActionBarManager().stopActionBar(player);
        broadcastLobby("&f" + player.getName() + " &7が退出。 &e" +
            waitingPlayers.size() + "/" + arena.getMaxPlayers());

        if (waitingPlayers.size() < arena.getMinPlayers() && state == LobbyState.STARTING) {
            cancelCountdown();
        }
    }

    private void checkStartCondition() {
        if (waitingPlayers.size() >= arena.getMinPlayers() && state == LobbyState.WAITING) {
            startCountdown();
        }
        // 満員になったらカウントダウン短縮
        if (waitingPlayers.size() >= arena.getMaxPlayers() && state == LobbyState.STARTING) {
            countdown = Math.min(countdown, plugin.getConfigManager().getLobbyCountdownFull());
        }
    }

    private void startCountdown() {
        state = LobbyState.STARTING;
        countdown = plugin.getConfigManager().getLobbyCountdown();
        broadcastLobby(plugin.getConfigManager().getMessage("countdown-start", "seconds", String.valueOf(countdown)));

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            countdown--;

            getOnlinePlayers().forEach(p -> {
                plugin.getUiManager().getBossBarManager()
                    .showTimerBar(getOnlinePlayers(), arena.getId(), countdown, 30,
                        "試合開始まで");

                // カウントダウン通知
                if (plugin.getConfigManager().getCountdownNotify().contains(countdown)) {
                    VersionAdapter.sendTitle(p,
                        "&e" + countdown,
                        "&7" + arena.getName() + " | " + arena.getMode().toUpperCase(),
                        0, 25, 5);
                    com.warforge.core.compat.SoundHelper.play(p,
                        com.warforge.core.compat.SoundHelper.NOTE_PLING, 1f,
                        countdown == 1 ? 2f : 1f);
                }
            });

            if (countdown <= 0) {
                countdownTask.cancel();
                launchGame();
            }
        }, 20L, 20L);
    }

    private void cancelCountdown() {
        state = LobbyState.WAITING;
        if (countdownTask != null) countdownTask.cancel();
        plugin.getUiManager().getBossBarManager().clearBars(getOnlinePlayers(), arena.getId());
        broadcastLobby("&c人数が足りないためカウントダウンをキャンセルしました。");
    }

    private void launchGame() {
        state = LobbyState.IN_GAME;
        plugin.getUiManager().getBossBarManager().clearBars(getOnlinePlayers(), arena.getId());

        // ゲームモードをインスタンス化
        GameMode game = switch (arena.getMode().toLowerCase()) {
            case "tdm" -> new TeamDeathmatch(plugin, arena.getId());
            case "br", "battleroyale" -> new BattleRoyale(plugin, arena.getId());
            case "domination" -> new Domination(plugin, arena.getId());
            case "goldrush" -> new GoldRush(plugin, arena.getId());
            case "heist" -> new Heist(plugin, arena.getId());
            default -> new TeamDeathmatch(plugin, arena.getId());
        };

        // プレイヤーを追加してスポーンにTP
        for (UUID uuid : waitingPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            game.addPlayer(p);
            Location spawn = plugin.getArenaManager().getRandomSpawn(arena.getId());
            if (spawn != null) p.teleport(spawn);
        }

        // 参加報酬
        getOnlinePlayers().forEach(p -> {
            if (plugin.getVaultManager().isEnabled()) {
                plugin.getVaultManager().reward(p,
                    plugin.getEconomyConfig().getParticipationReward(), "試合参加");
            }
        });

        plugin.getGameManager().getActiveGames().put(arena.getId(), game);
        game.start();

        broadcastLobby("&a&l試合開始！ &7モード: &f" + arena.getMode().toUpperCase());
        waitingPlayers.clear();
    }

    private void broadcastLobby(String msg) {
        getOnlinePlayers().forEach(p -> p.sendMessage(Messages.INSTANCE.prefixed(msg)));
    }

    private List<Player> getOnlinePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : waitingPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) result.add(p);
        }
        return result;
    }

    public List<UUID> getWaitingPlayers() { return waitingPlayers; }
    public LobbyState getState() { return state; }
    public Arena getArena() { return arena; }

    public enum LobbyState { WAITING, STARTING, IN_GAME }
}
