package com.warforge.core.data;

import com.warforge.core.WarforgeCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private final WarforgeCore plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        FileConfiguration config = plugin.getConfig();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://"
                + config.getString("database.host") + ":"
                + config.getInt("database.port") + "/"
                + config.getString("database.name")
                + "?useSSL=false&characterEncoding=UTF-8");
        hikariConfig.setUsername(config.getString("database.username"));
        hikariConfig.setPassword(config.getString("database.password"));
        hikariConfig.setMaximumPoolSize(config.getInt("database.pool-size", 10));
        hikariConfig.setPoolName("WarforgePool");

        try {
            dataSource = new HikariDataSource(hikariConfig);
            createTables();
            plugin.getLogger().info("MySQL接続成功！");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("MySQL接続失敗: " + e.getMessage());
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // プレイヤーデータテーブル
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS wf_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(16) NOT NULL,
                    kills INT DEFAULT 0,
                    deaths INT DEFAULT 0,
                    wins INT DEFAULT 0,
                    losses INT DEFAULT 0,
                    coins INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """).execute();

            // アリーナテーブル
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS wf_arenas (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(64) NOT NULL UNIQUE,
                    world VARCHAR(64) NOT NULL,
                    mode VARCHAR(32) NOT NULL,
                    max_players INT DEFAULT 16,
                    enabled BOOLEAN DEFAULT true,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """).execute();

            // 統計テーブル
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS wf_stats (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    game_mode VARCHAR(32) NOT NULL,
                    kills INT DEFAULT 0,
                    deaths INT DEFAULT 0,
                    result VARCHAR(16),
                    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (uuid) REFERENCES wf_players(uuid)
                )
            """).execute();

            
            // オークションテーブル
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS wf_auctions (
                    id INT PRIMARY KEY,
                    seller_uuid VARCHAR(36) NOT NULL,
                    seller_name VARCHAR(16) NOT NULL,
                    start_price DOUBLE NOT NULL,
                    current_bid DOUBLE NOT NULL,
                    highest_bidder_uuid VARCHAR(36),
                    highest_bidder_name VARCHAR(16),
                    listed_at BIGINT NOT NULL,
                    expires_at BIGINT NOT NULL,
                    status VARCHAR(16) DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """).execute();

            
            // ランクポイント列（既存テーブルに追加）
            try { conn.prepareStatement("ALTER TABLE wf_players ADD COLUMN rank_points INT DEFAULT 0").execute(); } catch (Exception ignored) {}
            try { conn.prepareStatement("ALTER TABLE wf_players ADD COLUMN assists INT DEFAULT 0").execute(); } catch (Exception ignored) {}
            try { conn.prepareStatement("ALTER TABLE wf_players ADD COLUMN total_games INT DEFAULT 0").execute(); } catch (Exception ignored) {}

            // ロードアウト
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS wf_loadouts (
                    uuid VARCHAR(36) NOT NULL,
                    slot INT NOT NULL,
                    name VARCHAR(32) DEFAULT 'loadout',
                    gun_ids TEXT NOT NULL,
                    PRIMARY KEY (uuid, slot)
                )
            """).execute();

            // デイリーミッション
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS wf_missions (
                    uuid VARCHAR(36) NOT NULL,
                    mission_id VARCHAR(32) NOT NULL,
                    mission_date DATE NOT NULL,
                    current INT DEFAULT 0,
                    completed BOOLEAN DEFAULT FALSE,
                    PRIMARY KEY (uuid, mission_id, mission_date)
                )
            """).execute();

            // キルログ
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS wf_log_kills (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    killer_uuid VARCHAR(36),
                    victim_uuid VARCHAR(36),
                    weapon VARCHAR(32),
                    headshot BOOLEAN DEFAULT FALSE,
                    arena_id INT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """).execute();

            // 経済ログ
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS wf_log_economy (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36),
                    amount DOUBLE,
                    reason VARCHAR(64),
                    type VARCHAR(8),
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """).execute();

            // 試合ログ
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS wf_log_games (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    arena_id INT,
                    mode VARCHAR(32),
                    winner_uuid VARCHAR(36),
                    duration_sec INT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """).execute();

            // オークションログ
            conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS wf_log_auctions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    seller_uuid VARCHAR(36),
                    buyer_uuid VARCHAR(36),
                    item_name VARCHAR(64),
                    price DOUBLE,
                    action VARCHAR(16),
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """).execute();

            plugin.getLogger().info("テーブル作成/確認完了！");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
