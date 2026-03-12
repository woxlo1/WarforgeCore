package com.warforge.core.log;

import com.warforge.core.WarforgeCore;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.UUID;

public class TransactionLogger {

    private final WarforgeCore plugin;

    public TransactionLogger(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    /** キルログ */
    public void logKill(UUID killer, UUID victim, String weapon, boolean headshot, int arenaId) {
        logAsync(
            "INSERT INTO wf_log_kills (killer_uuid, victim_uuid, weapon, headshot, arena_id, timestamp) VALUES (?,?,?,?,?,NOW())",
            stmt -> {
                stmt.setString(1, killer.toString());
                stmt.setString(2, victim.toString());
                stmt.setString(3, weapon);
                stmt.setBoolean(4, headshot);
                stmt.setInt(5, arenaId);
            }
        );
    }

    /** 経済ログ（入金/出金） */
    public void logEconomy(UUID uuid, double amount, String reason, String type) {
        logAsync(
            "INSERT INTO wf_log_economy (uuid, amount, reason, type, timestamp) VALUES (?,?,?,?,NOW())",
            stmt -> {
                stmt.setString(1, uuid.toString());
                stmt.setDouble(2, amount);
                stmt.setString(3, reason);
                stmt.setString(4, type); // CREDIT / DEBIT
            }
        );
    }

    /** 試合ログ */
    public void logGame(int arenaId, String mode, UUID winner, int durationSec) {
        logAsync(
            "INSERT INTO wf_log_games (arena_id, mode, winner_uuid, duration_sec, timestamp) VALUES (?,?,?,?,NOW())",
            stmt -> {
                stmt.setInt(1, arenaId);
                stmt.setString(2, mode);
                stmt.setString(3, winner != null ? winner.toString() : null);
                stmt.setInt(4, durationSec);
            }
        );
    }

    /** オークションログ */
    public void logAuction(UUID seller, UUID buyer, String itemName, double price, String action) {
        logAsync(
            "INSERT INTO wf_log_auctions (seller_uuid, buyer_uuid, item_name, price, action, timestamp) VALUES (?,?,?,?,?,NOW())",
            stmt -> {
                stmt.setString(1, seller.toString());
                stmt.setString(2, buyer != null ? buyer.toString() : null);
                stmt.setString(3, itemName);
                stmt.setDouble(4, price);
                stmt.setString(5, action); // LIST / SOLD / EXPIRED / CANCELLED
            }
        );
    }

    @FunctionalInterface
    private interface StmtPopulator {
        void populate(PreparedStatement stmt) throws SQLException;
    }

    private void logAsync(String sql, StmtPopulator populator) {
        if (!plugin.getConfigManager().isDebug() && !isLoggingEnabled()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                populator.populate(stmt);
                stmt.execute();
            } catch (SQLException e) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("[LOG] DB書き込み失敗: " + e.getMessage());
                }
            }
        });
    }

    private boolean isLoggingEnabled() {
        return plugin.getConfig().getBoolean("logging.enabled", true);
    }
}
