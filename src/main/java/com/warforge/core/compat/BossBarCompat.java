package com.warforge.core.compat;

import com.warforge.core.compat.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adventure BossBar の代わりに Bukkit BossBar を使う。
 * Bukkit BossBar は 1.9+ で使用可能 → 1.16.5〜1.21 全対応。
 */
public final class BossBarCompat {

    private final Map<String, BossBar> bars = new HashMap<>();

    public void showTimerBar(List<Player> players, int arenaId,
                              int timeLeft, int maxTime, String modeName) {
        String key = arenaId + "_timer";
        float progress = maxTime > 0 ? Math.max(0f, Math.min(1f, (float) timeLeft / maxTime)) : 0f;
        BarColor color = progress > 0.5f ? BarColor.GREEN : (progress > 0.25f ? BarColor.YELLOW : BarColor.RED);

        int min = timeLeft / 60, sec = timeLeft % 60;
        String title = VersionAdapter.color(
            "&f" + modeName + " &7| &e" + String.format("%02d:%02d", min, sec));

        BossBar bar = bars.computeIfAbsent(key,
            k -> Bukkit.createBossBar(title, color, BarStyle.SOLID));
        bar.setTitle(title);
        bar.setProgress(progress);
        bar.setColor(color);

        for (Player p : players) bar.addPlayer(p);
    }

    public void showPlayersBar(List<Player> players, int arenaId, int alive, int total) {
        String key = arenaId + "_players";
        float progress = total > 0 ? Math.max(0f, Math.min(1f, (float) alive / total)) : 0f;
        String title = VersionAdapter.color("&c残りプレイヤー &f" + alive + " / " + total);

        BossBar bar = bars.computeIfAbsent(key,
            k -> Bukkit.createBossBar(title, BarColor.RED, BarStyle.SEGMENTED_10));
        bar.setTitle(title);
        bar.setProgress(progress);

        for (Player p : players) bar.addPlayer(p);
    }

    public void showGoldBar(List<Player> players, int arenaId,
                             String leaderName, int collected, int goal) {
        String key = arenaId + "_gold";
        float progress = goal > 0 ? Math.max(0f, Math.min(1f, (float) collected / goal)) : 0f;
        String title = VersionAdapter.color("&6Gold Rush &f| &e" + leaderName + " &7: " + collected);

        BossBar bar = bars.computeIfAbsent(key,
            k -> Bukkit.createBossBar(title, BarColor.YELLOW, BarStyle.SEGMENTED_20));
        bar.setTitle(title);
        bar.setProgress(progress);

        for (Player p : players) bar.addPlayer(p);
    }

    public void showDominationBar(List<Player> players, int arenaId,
                                   int redScore, int blueScore, int maxScore) {
        String key = arenaId + "_domination";
        int total = redScore + blueScore;
        float progress = maxScore > 0 ? Math.max(0f, Math.min(1f, (float) total / (maxScore * 2))) : 0f;
        String title = VersionAdapter.color("&cRed " + redScore + " &7vs &9Blue " + blueScore
            + " &7| 目標: &f" + maxScore);

        BossBar bar = bars.computeIfAbsent(key,
            k -> Bukkit.createBossBar(title, BarColor.PURPLE, BarStyle.SEGMENTED_20));
        bar.setTitle(title);
        bar.setProgress(progress);

        for (Player p : players) bar.addPlayer(p);
    }

    public void clearBars(List<Player> players, int arenaId) {
        for (String type : new String[]{"timer", "players", "gold", "domination"}) {
            BossBar bar = bars.remove(arenaId + "_" + type);
            if (bar != null) {
                for (Player p : players) bar.removePlayer(p);
                bar.removeAll();
            }
        }
    }

    public void shutdownAll() {
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
    }
}
