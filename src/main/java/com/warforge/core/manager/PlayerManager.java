package com.warforge.core.manager;

import com.warforge.core.WarforgeCore;
import com.warforge.core.player.WFPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private final WarforgeCore plugin;
    private final Map<UUID, WFPlayer> onlinePlayers = new HashMap<>();

    public PlayerManager(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    public WFPlayer loadPlayer(UUID uuid, String name) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM wf_players WHERE uuid = ?"
            );
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                WFPlayer player = new WFPlayer(
                    uuid, name,
                    rs.getInt("kills"),
                    rs.getInt("deaths"),
                    rs.getInt("wins"),
                    rs.getInt("losses"),
                    rs.getDouble("coins"),
                    rs.getInt("assists"),
                    rs.getInt("total_games"),
                    rs.getInt("rank_points")
                );
                onlinePlayers.put(uuid, player);
                return player;
            } else {
                // 新規プレイヤー作成
                PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO wf_players (uuid, name) VALUES (?, ?)"
                );
                insert.setString(1, uuid.toString());
                insert.setString(2, name);
                insert.execute();

                WFPlayer player = new WFPlayer(uuid, name, 0, 0, 0, 0, 0.0, 0, 0, 0);
                onlinePlayers.put(uuid, player);
                return player;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("プレイヤーデータ読み込み失敗: " + e.getMessage());
            return null;
        }
    }

    public void savePlayer(WFPlayer wfPlayer) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE wf_players SET name=?, kills=?, deaths=?, wins=?, losses=?, coins=? WHERE uuid=?"
            );
            stmt.setString(1, wfPlayer.getName());
            stmt.setInt(2, wfPlayer.getKills());
            stmt.setInt(3, wfPlayer.getDeaths());
            stmt.setInt(4, wfPlayer.getWins());
            stmt.setInt(5, wfPlayer.getLosses());
            stmt.setDouble(6, wfPlayer.getCoins());
            stmt.setString(7, wfPlayer.getUuid().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("プレイヤーデータ保存失敗: " + e.getMessage());
        }
    }

    public void unloadPlayer(UUID uuid) {
        WFPlayer player = onlinePlayers.get(uuid);
        if (player != null) {
            savePlayer(player);
            onlinePlayers.remove(uuid);
        }
    }

    public WFPlayer getPlayer(UUID uuid) {
        return onlinePlayers.get(uuid);
    }

    public Map<UUID, WFPlayer> getOnlinePlayers() {
        return onlinePlayers;
    }
}
