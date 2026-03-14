package com.warforge.core.mission;

import com.warforge.core.WarforgeCore;
import com.warforge.core.economy.VaultManager;
import com.warforge.core.i18n.LangManager;
import com.warforge.core.util.Messages;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class MissionManager {

    private final WarforgeCore plugin;

    private static final List<MissionDef> ALL_MISSIONS = List.of(
            new MissionDef("killer",      "KILL",    10,  1500, "敵を10回倒す"),
            new MissionDef("sharpshooter","HEADSHOT", 5,  2000, "ヘッドショットを5回決める"),
            new MissionDef("survivor",    "WIN",      3,  3000, "試合に3回勝利する"),
            new MissionDef("veteran",     "GAME",     5,  1000, "試合に5回参加する"),
            new MissionDef("gunslinger",  "KILL",    20,  2500, "敵を20回倒す"),
            new MissionDef("ace",         "HEADSHOT",10,  4000, "ヘッドショットを10回決める"),
            new MissionDef("champion",    "WIN",      5,  5000, "試合に5回勝利する"),
            new MissionDef("warrior",     "KILL",     5,   500, "敵を5回倒す")
    );

    private final Map<UUID, Map<String, MissionProgress>> dailyCache = new HashMap<>();

    public MissionManager(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    // ─── プログレス更新 ───

    public void onKill(Player player)           { progress(player, "KILL",    1); }
    public void onHeadshot(Player player)       { progress(player, "HEADSHOT",1); }
    public void onWin(Player player)            { progress(player, "WIN", 1); progress(player, "GAME", 1); }
    public void onGameParticipate(Player player){ progress(player, "GAME",    1); }

    private void progress(Player player, String type, int amount) {
        UUID uuid = player.getUniqueId();
        Map<String, MissionProgress> missions = getOrLoadMissions(uuid);

        missions.values().stream()
                .filter(m -> m.def.type().equals(type) && !m.completed)
                .forEach(m -> {
                    m.current = Math.min(m.current + amount, m.def.goal());
                    saveProgress(uuid, m);

                    int pct = (int)((double) m.current / m.def.goal() * 100);
                    if (pct % 25 == 0 || m.current == m.def.goal()) {
                        VersionAdapter.sendActionBar(player,
                                "&6[ミッション] &f" + m.def.description()
                                        + " &7(" + m.current + "/" + m.def.goal() + ")");
                    }

                    if (m.current >= m.def.goal() && !m.completed) {
                        m.completed = true;
                        onComplete(player, m);
                    }
                });
    }

    private void onComplete(Player player, MissionProgress m) {
        player.sendMessage(Messages.INSTANCE.prefixed("&6&l[ミッション完了！] &f" + m.def.description()));
        player.sendMessage(Messages.INSTANCE.prefixed("&a報酬: &e+" + VaultManager.formatYen(m.def.reward())));
        if (plugin.getVaultManager().isEnabled()) {
            plugin.getVaultManager().getEconomy().depositPlayer(player, m.def.reward());
        }
        saveProgress(player.getUniqueId(), m);
    }

    // ─── ミッション表示 ───

    public void showMissions(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, MissionProgress> missions = getOrLoadMissions(uuid);

        // 全行を LangManager.colorize() または Messages.INSTANCE.prefixed() 経由で送信
        player.sendMessage(c("&8&m                              "));
        player.sendMessage(c("  &6&l✦ 本日のデイリーミッション"));
        player.sendMessage(c("&8&m                              "));
        for (MissionProgress m : missions.values()) {
            String status = m.completed
                    ? c("&a✔ 完了")
                    : c("&e") + m.current + "/" + m.def.goal();
            String bar = buildProgressBar(m.current, m.def.goal());
            player.sendMessage(c("  &f") + m.def.description());
            player.sendMessage("  " + bar + " " + status + "  " + c("&e¥") + String.format("%,.0f", m.def.reward()));
        }
        player.sendMessage(c("&8&m                              "));
    }

    /** LangManager.colorize() の短縮エイリアス */
    private static String c(String text) {
        return LangManager.colorize(text);
    }

    private String buildProgressBar(int current, int goal) {
        int bars = 20;
        int filled = (int)((double) current / goal * bars);
        return c("&a") + "█".repeat(filled) + c("&8") + "█".repeat(bars - filled);
    }

    // ─── ミッション取得/生成 ───

    private Map<String, MissionProgress> getOrLoadMissions(UUID uuid) {
        Map<String, MissionProgress> cached = dailyCache.get(uuid);
        if (cached != null) return cached;

        Map<String, MissionProgress> loaded = loadFromDb(uuid);
        if (loaded.isEmpty()) loaded = generateDailyMissions(uuid);
        dailyCache.put(uuid, loaded);
        return loaded;
    }

    private Map<String, MissionProgress> generateDailyMissions(UUID uuid) {
        List<MissionDef> shuffled = new ArrayList<>(ALL_MISSIONS);
        Collections.shuffle(shuffled);
        Map<String, MissionProgress> result = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(3, shuffled.size()); i++) {
            MissionDef def = shuffled.get(i);
            MissionProgress prog = new MissionProgress(def, 0, false);
            result.put(def.id(), prog);
            saveProgress(uuid, prog);
        }
        return result;
    }

    // ─── DB ───

    private Map<String, MissionProgress> loadFromDb(UUID uuid) {
        Map<String, MissionProgress> result = new LinkedHashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM wf_missions WHERE uuid=? AND mission_date=?");
            stmt.setString(1, uuid.toString());
            stmt.setString(2, LocalDate.now(ZoneId.of("Asia/Tokyo")).toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("mission_id");
                ALL_MISSIONS.stream().filter(m -> m.id().equals(id)).findFirst().ifPresent(def -> {
                    try {
                        result.put(id, new MissionProgress(def, rs.getInt("current"), rs.getBoolean("completed")));
                    } catch (SQLException e) { throw new RuntimeException(e); }
                });
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("ミッション読み込み失敗: " + e.getMessage());
        }
        return result;
    }

    private void saveProgress(UUID uuid, MissionProgress m) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                        "REPLACE INTO wf_missions (uuid, mission_id, mission_date, current, completed) VALUES (?,?,?,?,?)");
                stmt.setString(1, uuid.toString());
                stmt.setString(2, m.def.id());
                stmt.setString(3, LocalDate.now(ZoneId.of("Asia/Tokyo")).toString());
                stmt.setInt(4, m.current);
                stmt.setBoolean(5, m.completed);
                stmt.execute();
            } catch (SQLException e) {
                plugin.getLogger().warning("ミッション保存失敗: " + e.getMessage());
            }
        });
    }

    public void clearCache(UUID uuid) { dailyCache.remove(uuid); }

    // ─── データクラス ───

    private static final class MissionDef {
        private final String id, type, description;
        private final int goal;
        private final double reward;
        MissionDef(String id, String type, int goal, double reward, String description) {
            this.id = id; this.type = type; this.goal = goal;
            this.reward = reward; this.description = description;
        }
        public String id()          { return id; }
        public String type()        { return type; }
        public int goal()           { return goal; }
        public double reward()      { return reward; }
        public String description() { return description; }
    }

    private static class MissionProgress {
        final MissionDef def;
        int current;
        boolean completed;
        MissionProgress(MissionDef def, int current, boolean completed) {
            this.def = def; this.current = current; this.completed = completed;
        }
    }
}
