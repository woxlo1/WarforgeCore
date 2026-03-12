package com.warforge.core.auction;

import com.warforge.core.WarforgeCore;
import com.warforge.core.economy.VaultManager;
import com.warforge.core.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {

    private final WarforgeCore plugin;
    private final Map<Integer, AuctionItem> activeAuctions = new ConcurrentHashMap<>();
    private int nextId = 1;
    private BukkitTask expiryTask;

    // 設定値はConfigManagerから動的取得

    public AuctionManager(WarforgeCore plugin) {
        this.plugin = plugin;
        loadFromDatabase();
        startExpiryChecker();
    }

    // ─────────────────────────────────────────
    // 出品
    // ─────────────────────────────────────────

    public AuctionResult listItem(Player seller, ItemStack item, double startPrice) {
        if (!plugin.getVaultManager().isEnabled()) return AuctionResult.NO_ECONOMY;
        if (item == null || item.getType().isAir()) return AuctionResult.INVALID_ITEM;
        if (startPrice < plugin.getConfigManager().getAuctionMinPrice()) return AuctionResult.INVALID_PRICE;

        // 出品数制限
        long sellerListings = activeAuctions.values().stream()
            .filter(a -> a.getSellerUuid().equals(seller.getUniqueId()))
            .filter(a -> a.getStatus() == AuctionStatus.ACTIVE)
            .count();
        if (sellerListings >= plugin.getConfigManager().getAuctionMaxListings()) return AuctionResult.MAX_LISTINGS;

        int id = nextId++;
        ItemStack listedItem = item.clone();
        // インベントリからアイテムを削除
        item.setAmount(0);

        AuctionItem auctionItem = new AuctionItem(
            id, seller.getUniqueId(), seller.getName(),
            listedItem, startPrice, startPrice,
            null, null,
            System.currentTimeMillis(),
            System.currentTimeMillis() + plugin.getConfigManager().getAuctionDurationMs(),
            AuctionStatus.ACTIVE
        );

        activeAuctions.put(id, auctionItem);
        saveToDatabase(auctionItem);

        // 全員に通知
        broadcastAll("&6[オークション] &f" + seller.getName() + " &7が &f" +
            getItemName(listedItem) + " &7を &e" + VaultManager.formatYen(startPrice) +
            " &7から出品！ &8/auction list");

        return AuctionResult.SUCCESS;
    }

    // ─────────────────────────────────────────
    // 入札
    // ─────────────────────────────────────────

    public AuctionResult bid(Player bidder, int auctionId, double amount) {
        if (!plugin.getVaultManager().isEnabled()) return AuctionResult.NO_ECONOMY;

        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE) return AuctionResult.NOT_FOUND;
        if (auction.getSellerUuid().equals(bidder.getUniqueId())) return AuctionResult.SELF_BID;
        if (amount < auction.getCurrentBid() + plugin.getConfigManager().getAuctionBidIncrement()) return AuctionResult.BID_TOO_LOW;
        if (System.currentTimeMillis() > auction.getExpiresAt()) return AuctionResult.EXPIRED;

        // 残高確認
        double balance = plugin.getVaultManager().getBalance(bidder);
        if (balance < amount) return AuctionResult.INSUFFICIENT_FUNDS;

        // 前の最高入札者に返金
        if (auction.getHighestBidderUuid() != null) {
            Player prevBidder = Bukkit.getPlayer(auction.getHighestBidderUuid());
            if (prevBidder != null) {
                plugin.getVaultManager().getEconomy().depositPlayer(prevBidder, auction.getCurrentBid());
                prevBidder.sendMessage(Messages.INSTANCE.prefixed(
                    "&e" + getItemName(auction.getItem()) + " &7の入札が更新されました。" +
                    VaultManager.formatYen(auction.getCurrentBid()) + " &7を返金しました。"));
            } else {
                // オフラインなら直接残高へ
                Bukkit.getOfflinePlayer(auction.getHighestBidderUuid());
                plugin.getVaultManager().getEconomy().depositPlayer(
                    Bukkit.getOfflinePlayer(auction.getHighestBidderUuid()),
                    auction.getCurrentBid());
            }
        }

        // 入札者から引き落とし
        plugin.getVaultManager().getEconomy().withdrawPlayer(bidder, amount);

        auction.setCurrentBid(amount);
        auction.setHighestBidderUuid(bidder.getUniqueId());
        auction.setHighestBidderName(bidder.getName());
        updateDatabase(auction);

        broadcastAll("&6[オークション] &f" + bidder.getName() + " &7が &f" +
            getItemName(auction.getItem()) + " &7に &e" + VaultManager.formatYen(amount) +
            " &7で入札！");

        return AuctionResult.SUCCESS;
    }

    // ─────────────────────────────────────────
    // 即決購入
    // ─────────────────────────────────────────

    public AuctionResult buyNow(Player buyer, int auctionId) {
        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE) return AuctionResult.NOT_FOUND;
        if (auction.getSellerUuid().equals(buyer.getUniqueId())) return AuctionResult.SELF_BID;

        double price = auction.getCurrentBid();
        double balance = plugin.getVaultManager().getBalance(buyer);
        if (balance < price) return AuctionResult.INSUFFICIENT_FUNDS;

        // 前の入札者に返金
        if (auction.getHighestBidderUuid() != null &&
            !auction.getHighestBidderUuid().equals(buyer.getUniqueId())) {
            plugin.getVaultManager().getEconomy().depositPlayer(
                Bukkit.getOfflinePlayer(auction.getHighestBidderUuid()),
                auction.getCurrentBid());
        }

        // 購入者から引き落とし（手数料5%）
        double tax = price * plugin.getConfigManager().getAuctionTaxRate();
        double sellerReceives = price - tax;
        plugin.getVaultManager().getEconomy().withdrawPlayer(buyer, price);

        // 出品者に送金
        Player seller = Bukkit.getPlayer(auction.getSellerUuid());
        if (seller != null) {
            plugin.getVaultManager().getEconomy().depositPlayer(seller, sellerReceives);
            seller.sendMessage(Messages.INSTANCE.prefixed(
                "&f" + buyer.getName() + " &7が &f" + getItemName(auction.getItem()) +
                " &7を購入！ &a+" + VaultManager.formatYen(sellerReceives) +
                " &8(手数料 " + VaultManager.formatYen(tax) + ")"));
        } else {
            plugin.getVaultManager().getEconomy().depositPlayer(
                Bukkit.getOfflinePlayer(auction.getSellerUuid()), sellerReceives);
        }

        // アイテムを購入者に渡す
        buyer.getInventory().addItem(auction.getItem().clone());
        buyer.sendMessage(Messages.INSTANCE.prefixed(
            "&f" + getItemName(auction.getItem()) + " &7を購入しました！ &c-" + VaultManager.formatYen(price)));

        auction.setStatus(AuctionStatus.SOLD);
        auction.setHighestBidderUuid(buyer.getUniqueId());
        auction.setHighestBidderName(buyer.getName());
        updateDatabase(auction);
        activeAuctions.remove(auctionId);

        broadcastAll("&6[オークション] &f" + buyer.getName() + " &7が &f" +
            getItemName(auction.getItem()) + " &7を &e" + VaultManager.formatYen(price) + " &7で落札！");

        return AuctionResult.SUCCESS;
    }

    // ─────────────────────────────────────────
    // キャンセル
    // ─────────────────────────────────────────

    public AuctionResult cancel(Player player, int auctionId) {
        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction == null) return AuctionResult.NOT_FOUND;
        if (!auction.getSellerUuid().equals(player.getUniqueId()) &&
            !player.hasPermission("warforge.admin")) return AuctionResult.NO_PERMISSION;
        if (auction.getHighestBidderUuid() != null) return AuctionResult.HAS_BIDS;

        // アイテムを返却
        player.getInventory().addItem(auction.getItem().clone());
        auction.setStatus(AuctionStatus.CANCELLED);
        updateDatabase(auction);
        activeAuctions.remove(auctionId);

        player.sendMessage(Messages.INSTANCE.prefixed("&aオークションをキャンセルしました。アイテムを返却しました。"));
        return AuctionResult.SUCCESS;
    }

    // ─────────────────────────────────────────
    // 期限切れチェック
    // ─────────────────────────────────────────

    private void startExpiryChecker() {
        expiryTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            new ArrayList<>(activeAuctions.values()).forEach(auction -> {
                if (auction.getStatus() != AuctionStatus.ACTIVE) return;
                if (now < auction.getExpiresAt()) return;

                if (auction.getHighestBidderUuid() != null) {
                    // 落札者にアイテム渡す
                    Player winner = Bukkit.getPlayer(auction.getHighestBidderUuid());
                    if (winner != null) {
                        winner.getInventory().addItem(auction.getItem().clone());
                        winner.sendMessage(Messages.INSTANCE.prefixed(
                            "&6[オークション] &f" + getItemName(auction.getItem()) +
                            " &7の落札おめでとうございます！"));
                    }
                    // 出品者に送金
                    double sellerReceives = auction.getCurrentBid() * (1 - plugin.getConfigManager().getAuctionTaxRate());
                    plugin.getVaultManager().getEconomy().depositPlayer(
                        Bukkit.getOfflinePlayer(auction.getSellerUuid()), sellerReceives);

                    broadcastAll("&6[オークション] &f" +
                        (auction.getHighestBidderName() != null ? auction.getHighestBidderName() : "???") +
                        " &7が &f" + getItemName(auction.getItem()) + " &7を &e" +
                        VaultManager.formatYen(auction.getCurrentBid()) + " &7で落札！");
                } else {
                    // 入札なし → 出品者に返却
                    Player seller = Bukkit.getPlayer(auction.getSellerUuid());
                    if (seller != null) {
                        seller.getInventory().addItem(auction.getItem().clone());
                        seller.sendMessage(Messages.INSTANCE.prefixed(
                            "&e" + getItemName(auction.getItem()) + " &7は入札なしで期限切れになりました。"));
                    }
                }

                auction.setStatus(AuctionStatus.EXPIRED);
                updateDatabase(auction);
                activeAuctions.remove(auction.getId());
            });
        }, 200L, 200L); // 10秒ごとにチェック
    }

    // ─────────────────────────────────────────
    // データベース
    // ─────────────────────────────────────────

    private void loadFromDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            ResultSet rs = conn.prepareStatement(
                "SELECT * FROM wf_auctions WHERE status = 'ACTIVE'"
            ).executeQuery();
            while (rs.next()) {
                // シリアライズ済みアイテムの復元は簡略化
                // 実装時はBukkitSerializationを使用
                nextId = Math.max(nextId, rs.getInt("id") + 1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("オークションデータ読み込み失敗: " + e.getMessage());
        }
    }

    private void saveToDatabase(AuctionItem item) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO wf_auctions (id, seller_uuid, seller_name, start_price, current_bid, " +
                "highest_bidder_uuid, highest_bidder_name, listed_at, expires_at, status) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)"
            );
            stmt.setInt(1, item.getId());
            stmt.setString(2, item.getSellerUuid().toString());
            stmt.setString(3, item.getSellerName());
            stmt.setDouble(4, item.getStartPrice());
            stmt.setDouble(5, item.getCurrentBid());
            stmt.setString(6, item.getHighestBidderUuid() != null ? item.getHighestBidderUuid().toString() : null);
            stmt.setString(7, item.getHighestBidderName());
            stmt.setLong(8, item.getListedAt());
            stmt.setLong(9, item.getExpiresAt());
            stmt.setString(10, item.getStatus().name());
            stmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().warning("オークション保存失敗: " + e.getMessage());
        }
    }

    private void updateDatabase(AuctionItem item) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE wf_auctions SET current_bid=?, highest_bidder_uuid=?, highest_bidder_name=?, status=? WHERE id=?"
            );
            stmt.setDouble(1, item.getCurrentBid());
            stmt.setString(2, item.getHighestBidderUuid() != null ? item.getHighestBidderUuid().toString() : null);
            stmt.setString(3, item.getHighestBidderName());
            stmt.setString(4, item.getStatus().name());
            stmt.setInt(5, item.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("オークション更新失敗: " + e.getMessage());
        }
    }

    private String getItemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            try {
                return meta.getDisplayName();
            } catch (NoSuchMethodError | AbstractMethodError ignored) {
                try {
                    java.lang.reflect.Method m = meta.getClass().getMethod("displayName");
                    Object comp = m.invoke(meta);
                    if (comp != null) return comp.toString();
                } catch (Exception e) {
                    // ignore and fallback
                }
            }
        }
        return item.getType().name();
    }

    private void broadcastAll(String msg) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(Messages.INSTANCE.prefixed(msg)));
    }

    public Map<Integer, AuctionItem> getActiveAuctions() { return activeAuctions; }

    public void shutdown() {
        if (expiryTask != null) expiryTask.cancel();
    }

    public enum AuctionResult {
        SUCCESS, NO_ECONOMY, INVALID_ITEM, INVALID_PRICE, MAX_LISTINGS,
        NOT_FOUND, SELF_BID, BID_TOO_LOW, EXPIRED, INSUFFICIENT_FUNDS,
        NO_PERMISSION, HAS_BIDS
    }
}
