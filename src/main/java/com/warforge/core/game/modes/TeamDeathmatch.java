package com.warforge.core.game.modes;

import com.warforge.core.WarforgeCore;
import com.warforge.core.game.GameMode;
import com.warforge.core.util.Messages;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class TeamDeathmatch extends GameMode {

    public static final String NAME = "チームデスマッチ";
    private final int GAME_DURATION;
    private final int KILL_LIMIT;

    private final Map<UUID, Team> playerTeams = new HashMap<>();
    private int redScore = 0;
    private int blueScore = 0;
    private BukkitTask tickTask;

    public TeamDeathmatch(WarforgeCore plugin, int arenaId) {
        super(plugin, arenaId);
        this.GAME_DURATION = plugin.getConfigManager().getModeDuration("tdm");
        this.KILL_LIMIT = plugin.getConfigManager().getTdmKillLimit();
        this.timeLeft = GAME_DURATION;
    }

    @Override
    public void start() {
        state = GameState.IN_GAME;
        redScore = 0;
        blueScore = 0;

        // プレイヤーをチームに振り分け
        assignTeams();

        // スコアボード更新
        updateScoreboards();

        // タイマー開始
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);

        broadcast("&aゲームスタート！ &7目標: &f" + KILL_LIMIT + "キル");
    }

    @Override
    public void end() {
        state = GameState.ENDING;
        if (tickTask != null) tickTask.cancel();

        String winner = redScore > blueScore ? "&cレッドチーム" :
                        blueScore > redScore ? "&9ブルーチーム" : "&e引き分け";

        getOnlinePlayers().forEach(p -> {
            VersionAdapter.sendTitle(p, winner + " &f勝利！", "&7Red: " + redScore + " | Blue: " + blueScore, 10, 60, 20);
            plugin.getUiManager().getScoreboardManager().clearScoreboard(p);
            plugin.getUiManager().getActionBarManager().stopActionBar(p);
        });

        // 5秒後にリセット
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            getOnlinePlayers().forEach(p -> removePlayer(p));
            state = GameState.WAITING;
        }, 100L);
    }

    @Override
    public void onKill(Player killer, Player victim) {
        Team killerTeam = playerTeams.get(killer.getUniqueId());
        if (killerTeam == Team.RED) redScore++;
        else if (killerTeam == Team.BLUE) blueScore++;

        broadcast("&f" + killer.getName() + " &7が &f" + victim.getName() + " &7を倒した！ " +
                  "&c" + redScore + " &7- &9" + blueScore);

        updateScoreboards();

        if (checkWinCondition()) end();
    }

    @Override
    public void onDeath(Player victim) {
        // リスポーン処理
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (players.contains(victim.getUniqueId())) {
                VersionAdapter.sendTitle(victim, "&cリスポーン", "", 5, 20, 5);
                // TODO: スポーン地点にテレポート
                victim.setHealth(20.0);
            }
        }, 60L); // 3秒後リスポーン
    }

    @Override
    public void tick() {
        if (timeLeft <= 0) {
            end();
            return;
        }
        timeLeft--;

        // アクションバー更新
        List<Player> online = getOnlinePlayers();
        plugin.getUiManager().getBossBarManager().showTimerBar(online, arenaId, timeLeft, GAME_DURATION, NAME);
        online.forEach(p -> plugin.getUiManager().getActionBarManager().startGameActionBar(p, () -> timeLeft));
    }

    @Override
    public boolean checkWinCondition() {
        return redScore >= KILL_LIMIT || blueScore >= KILL_LIMIT || timeLeft <= 0;
    }

    private void assignTeams() {
        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        for (int i = 0; i < shuffled.size(); i++) {
            playerTeams.put(shuffled.get(i), i % 2 == 0 ? Team.RED : Team.BLUE);
        }
    }

    private void updateScoreboards() {
        getOnlinePlayers().forEach(p -> {
            Team team = playerTeams.getOrDefault(p.getUniqueId(), Team.RED);
            String teamInfo = (team == Team.RED ? "&cRed " + redScore : "&9Blue " + blueScore);
            plugin.getUiManager().getScoreboardManager().showGameScoreboard(p, NAME, teamInfo);
        });
    }

    private void broadcast(String message) {
        getOnlinePlayers().forEach(p -> p.sendMessage(Messages.INSTANCE.prefixed(message)));
    }

    private List<Player> getOnlinePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) result.add(p);
        }
        return result;
    }

    public Team getTeam(UUID uuid) { return playerTeams.get(uuid); }

    public enum Team { RED, BLUE }
}
