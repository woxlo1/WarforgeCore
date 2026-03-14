package com.warforge.core.stats;

import com.warforge.core.WarforgeCore;
import com.warforge.core.compat.VersionAdapter;
import com.warforge.core.player.WFPlayer;
import com.warforge.core.rank.RankTier;
import com.warforge.core.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;

public class StatsManager {

    private final WarforgeCore plugin;

    public StatsManager(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    public void showStats(Player viewer, String targetName) {
        if (targetName == null) { showPlayerStats(viewer, viewer); return; }
        Player online = Bukkit.getPlayer(targetName);
        if (online != null) {
            showPlayerStats(viewer, online);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> showOfflineStats(viewer, targetName));
        }
    }

    private void showPlayerStats(Player viewer, Player target) {
        WFPlayer wfp = plugin.getPlayerManager().getPlayer(target.getUniqueId());
        if (wfp == null) { viewer.sendMessage(Messages.INSTANCE.prefixed("&cデータが見つかりません。")); return; }
        RankTier rank = plugin.getRankManager().getRank(target.getUniqueId());
        String sep = VersionAdapter.color("&8&m                              ");
        viewer.sendMessage(sep);
        viewer.sendMessage(VersionAdapter.color("  " + rank.formatted() + " &f") + target.getName());
        viewer.sendMessage(sep);
        viewer.sendMessage(VersionAdapter.color("  &7キル       &f") + wfp.getKills());
        viewer.sendMessage(VersionAdapter.color("  &7デス       &f") + wfp.getDeaths());
        viewer.sendMessage(VersionAdapter.color("  &6KDR        &f") + String.format("%.2f", wfp.getKdr()));
        viewer.sendMessage(VersionAdapter.color("  &aアシスト   &f") + wfp.getAssists());
        viewer.sendMessage(VersionAdapter.color("  &b勝利       &f") + wfp.getWins());
        viewer.sendMessage(VersionAdapter.color("  &c敗北       &f") + wfp.getLosses());
        viewer.sendMessage(VersionAdapter.color("  &e勝率       &f") + String.format("%.1f", wfp.getWinRate()) + "%");
        viewer.sendMessage(VersionAdapter.color("  &d試合数     &f") + wfp.getTotalGames());
        viewer.sendMessage(VersionAdapter.color("  &6ランクPT   &f") + wfp.getRankPoints() + " pt");
        viewer.sendMessage(sep);
    }

    private void showOfflineStats(Player viewer, String targetName) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM wf_players WHERE name=? LIMIT 1");
            stmt.setString(1, targetName);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        viewer.sendMessage(Messages.INSTANCE.prefixed("&c" + targetName + " のデータが見つかりません。")));
                return;
            }
            int kills = rs.getInt("kills"), deaths = rs.getInt("deaths");
            int wins = rs.getInt("wins"), losses = rs.getInt("losses");
            double kdr = deaths == 0 ? kills : (double) kills / deaths;
            int rankPts = rs.getInt("rank_points");
            RankTier rank = RankTier.Companion.fromPoints(rankPts);
            Bukkit.getScheduler().runTask(plugin, () -> {
                String sep = VersionAdapter.color("&8&m                              ");
                viewer.sendMessage(sep);
                viewer.sendMessage(VersionAdapter.color("  " + rank.formatted() + " &f") + targetName + VersionAdapter.color(" &8(オフライン)"));
                viewer.sendMessage(sep);
                viewer.sendMessage(VersionAdapter.color("  &7キル  &f") + kills + VersionAdapter.color("  &7デス &f") + deaths + VersionAdapter.color("  &6KDR &f") + String.format("%.2f", kdr));
                viewer.sendMessage(VersionAdapter.color("  &b勝利 &f") + wins + VersionAdapter.color("  &c敗北 &f") + losses);
                viewer.sendMessage(VersionAdapter.color("  &6ランクPT &f") + rankPts + " pt");
                viewer.sendMessage(sep);
            });
        } catch (SQLException e) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    viewer.sendMessage(Messages.INSTANCE.prefixed("&cデータ取得中にエラーが発生しました。")));
        }
    }

    public void showTopRanking(Player viewer, String column, int limit) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String col = switch (column) {
                    case "kills" -> "kills";
                    case "wins"  -> "wins";
                    case "kdr"   -> "kills / GREATEST(deaths, 1)";
                    default      -> "rank_points";
                };
                String label = switch (column) {
                    case "kills" -> "キル数";
                    case "wins"  -> "勝利数";
                    case "kdr"   -> "KDR";
                    default      -> "ランクポイント";
                };
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT name, kills, deaths, wins, rank_points FROM wf_players ORDER BY " + col + " DESC LIMIT ?");
                stmt.setInt(1, Math.min(limit, 10));
                ResultSet rs = stmt.executeQuery();

                List<String> lines = new ArrayList<>();
                String sep = VersionAdapter.color("&8&m                              ");
                lines.add(sep);
                lines.add(VersionAdapter.color("  &6&l✦ " + label + " ランキング TOP" + limit));
                lines.add(sep);
                int pos = 1;
                while (rs.next()) {
                    String medal = switch (pos) {
                        case 1 -> VersionAdapter.color("&6①");
                        case 2 -> VersionAdapter.color("&7②");
                        case 3 -> VersionAdapter.color("&c③");
                        default -> VersionAdapter.color("&8") + pos + ".";
                    };
                    String name = rs.getString("name");
                    int pts = rs.getInt("rank_points");
                    RankTier rank = RankTier.Companion.fromPoints(pts);
                    String value = switch (column) {
                        case "kills" -> rs.getInt("kills") + " キル";
                        case "wins"  -> rs.getInt("wins")  + " 勝";
                        case "kdr"   -> { int k = rs.getInt("kills"), d = rs.getInt("deaths"); yield String.format("%.2f", d == 0 ? k : (double) k / d); }
                        default      -> pts + " pt";
                    };
                    lines.add("  " + medal + " " + rank.formatted() + VersionAdapter.color(" &f") + name + VersionAdapter.color("  &e") + value);
                    pos++;
                }
                lines.add(sep);
                Bukkit.getScheduler().runTask(plugin, () -> lines.forEach(viewer::sendMessage));
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        viewer.sendMessage(Messages.INSTANCE.prefixed("&cランキング取得失敗。")));
            }
        });
    }
}
