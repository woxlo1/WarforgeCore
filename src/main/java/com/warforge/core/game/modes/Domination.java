package com.warforge.core.game.modes;

import com.warforge.core.WarforgeCore;
import com.warforge.core.arena.Arena;
import com.warforge.core.arena.CaptureOwner;
import com.warforge.core.arena.CapturePoint;
import com.warforge.core.game.GameMode;
import com.warforge.core.util.Messages;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Domination extends GameMode {

    public static final String NAME = "拠点制圧";
    private final int GAME_DURATION;
    private final int MAX_SCORE;
    private final double CAPTURE_SPEED;

    private int redScore = 0;
    private int blueScore = 0;
    private final Map<UUID, TeamDeathmatch.Team> playerTeams = new HashMap<>();
    private BukkitTask tickTask;
    private BukkitTask captureTask;

    public Domination(WarforgeCore plugin, int arenaId) {
        super(plugin, arenaId);
        this.GAME_DURATION = plugin.getConfigManager().getModeDuration("domination");
        this.MAX_SCORE = plugin.getConfigManager().getDominationMaxScore();
        this.CAPTURE_SPEED = plugin.getConfigManager().getDominationCaptureSpeed();
        this.timeLeft = GAME_DURATION;
    }

    @Override
    public void start() {
        state = GameState.IN_GAME;
        assignTeams();

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        // 拠点制圧チェックは0.5秒ごと
        captureTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processCapturePoints, 0L, 10L);

        broadcast("&a拠点制圧開始！ &7" + MAX_SCORE + "ポイント先取で勝利！");
        updateUI();
    }

    @Override
    public void end() {
        state = GameState.ENDING;
        if (tickTask != null) tickTask.cancel();
        if (captureTask != null) captureTask.cancel();

        String winner = redScore >= MAX_SCORE ? "&cレッドチーム" :
                        blueScore >= MAX_SCORE ? "&9ブルーチーム" :
                        redScore > blueScore ? "&cレッドチーム" :
                        blueScore > redScore ? "&9ブルーチーム" : "&e引き分け";

        getOnlinePlayers().forEach(p -> {
            VersionAdapter.sendTitle(p, winner + " &f勝利！", "&cRed: " + redScore + " &7| &9Blue: " + blueScore, 10, 80, 20);
            plugin.getUiManager().getBossBarManager().clearBars(getOnlinePlayers(), arenaId);
            plugin.getUiManager().getScoreboardManager().clearScoreboard(p);
            plugin.getUiManager().getActionBarManager().stopActionBar(p);
        });

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            getOnlinePlayers().forEach(this::removePlayer);
            state = GameState.WAITING;
        }, 120L);
    }

    @Override
    public void onKill(Player killer, Player victim) {
        broadcast("&f" + killer.getName() + " &7が &f" + victim.getName() + " &7を倒した！");
    }

    @Override
    public void onDeath(Player victim) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (players.contains(victim.getUniqueId())) {
                victim.setHealth(20.0);
                Location spawn = plugin.getArenaManager().getRandomSpawn(arenaId);
                if (spawn != null) victim.teleport(spawn);
            }
        }, 60L);
    }

    @Override
    public void tick() {
        if (timeLeft <= 0) { end(); return; }
        timeLeft--;

        // 制圧中の拠点ごとにスコア加算
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena != null) {
            for (CapturePoint cp : arena.getCapturePoints()) {
                if (cp.getOwner() == CaptureOwner.RED) redScore++;
                else if (cp.getOwner() == CaptureOwner.BLUE) blueScore++;
            }
        }

        updateUI();
        if (checkWinCondition()) end();
    }

    /** 拠点制圧処理（0.5秒ごと） */
    private void processCapturePoints() {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) return;

        for (CapturePoint cp : arena.getCapturePoints()) {
            int redNearby = 0, blueNearby = 0;

            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                if (p.getLocation().distance(cp.getLocation()) <= cp.getRadius()) {
                    TeamDeathmatch.Team team = playerTeams.get(uuid);
                    if (team == TeamDeathmatch.Team.RED) redNearby++;
                    else if (team == TeamDeathmatch.Team.BLUE) blueNearby++;
                }
            }

            // 制圧ロジック
            if (redNearby > blueNearby) {
                if (cp.getOwner() != CaptureOwner.RED) {
                    double newProgress = Math.min(1.0, cp.getCaptureProgress() + CAPTURE_SPEED);
                    cp.setCaptureProgress(newProgress);
                    if (newProgress >= 1.0) {
                        cp.setOwner(CaptureOwner.RED);
                        broadcast("&c" + cp.getName() + " &7がレッドチームに制圧された！");
                    }
                }
            } else if (blueNearby > redNearby) {
                if (cp.getOwner() != CaptureOwner.BLUE) {
                    double newProgress = Math.min(1.0, cp.getCaptureProgress() + CAPTURE_SPEED);
                    cp.setCaptureProgress(newProgress);
                    if (newProgress >= 1.0) {
                        cp.setOwner(CaptureOwner.BLUE);
                        broadcast("&9" + cp.getName() + " &7がブルーチームに制圧された！");
                    }
                }
            }
        }
    }

    @Override
    public boolean checkWinCondition() {
        return redScore >= MAX_SCORE || blueScore >= MAX_SCORE || timeLeft <= 0;
    }

    private void assignTeams() {
        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        for (int i = 0; i < shuffled.size(); i++) {
            playerTeams.put(shuffled.get(i), i % 2 == 0 ? TeamDeathmatch.Team.RED : TeamDeathmatch.Team.BLUE);
        }
    }

    private void updateUI() {
        List<Player> online = getOnlinePlayers();
        plugin.getUiManager().getBossBarManager().showTimerBar(online, arenaId, timeLeft, GAME_DURATION, NAME);
        plugin.getUiManager().getBossBarManager().showDominationBar(online, arenaId, redScore, blueScore, MAX_SCORE);
        online.forEach(p -> {
            String teamInfo = playerTeams.getOrDefault(p.getUniqueId(), TeamDeathmatch.Team.RED) == TeamDeathmatch.Team.RED
                ? "&cRed " + redScore : "&9Blue " + blueScore;
            plugin.getUiManager().getScoreboardManager().showGameScoreboard(p, NAME, teamInfo);
            plugin.getUiManager().getActionBarManager().startGameActionBar(p, () -> timeLeft);
        });
    }

    private void broadcast(String msg) {
        getOnlinePlayers().forEach(p -> p.sendMessage(Messages.INSTANCE.prefixed(msg)));
    }

    private List<Player> getOnlinePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) result.add(p);
        }
        return result;
    }

    private org.bukkit.Location getRandomSpawn() {
        return plugin.getArenaManager().getRandomSpawn(arenaId);
    }
}
