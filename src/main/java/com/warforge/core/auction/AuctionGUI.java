package com.warforge.core.auction;

import com.warforge.core.WarforgeCore;
import com.warforge.core.economy.VaultManager;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AuctionGUI {

    private static final String GUI_TITLE = "§6§lWarforge オークション";
    private static final int PAGE_SIZE = 45;
    private final WarforgeCore plugin;

    public AuctionGUI(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    /** オークション一覧GUIを開く */
    public void openList(Player player, int page) {
        List<AuctionItem> items = new ArrayList<>(plugin.getAuctionManager().getActiveAuctions().values());
        items.sort(Comparator.comparingLong(AuctionItem::getListedAt).reversed());

        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // アイテム表示
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && (start + i) < items.size(); i++) {
            AuctionItem auction = items.get(start + i);
            inv.setItem(i, buildAuctionDisplayItem(auction));
        }

        // ナビゲーションバー（最下段）
        // 前のページ
        if (page > 0) inv.setItem(45, buildNavItem(Material.ARROW, "&7← 前のページ"));
        // 情報
        inv.setItem(49, buildInfoItem(page, totalPages, items.size()));
        // 次のページ
        if (page < totalPages - 1) inv.setItem(53, buildNavItem(Material.ARROW, "&7次のページ →"));
        // 自分の出品
        inv.setItem(47, buildNavItem(Material.CHEST, "&e自分の出品一覧"));
        // 閉じる
        inv.setItem(51, buildNavItem(Material.BARRIER, "&c閉じる"));

        player.openInventory(inv);
    }

    /** アイテムの詳細GUIを開く（入札・即決ボタン付き） */
    public void openDetail(Player player, int auctionId) {
        AuctionItem auction = plugin.getAuctionManager().getActiveAuctions().get(auctionId);
        if (auction == null) { player.closeInventory(); return; }

        Inventory inv = Bukkit.createInventory(null, 27, "§6オークション詳細");

        // メインアイテム表示
        inv.setItem(13, auction.getItem().clone());

        // 入札ボタン
        ItemStack bidBtn = buildNavItem(Material.GOLD_INGOT,
            "&e入札する &7(現在: " + VaultManager.formatYen(auction.getCurrentBid()) + ")");
        addLore(bidBtn, List.of(
            "&7クリックして入札金額を入力",
            "&7最低入札額: &e" + VaultManager.formatYen(auction.getCurrentBid() + 1)
        ));
        inv.setItem(11, bidBtn);

        // 即決ボタン
        ItemStack buyBtn = buildNavItem(Material.EMERALD,
            "&a即決購入 &e" + VaultManager.formatYen(auction.getCurrentBid()));
        addLore(buyBtn, List.of(
            "&7今すぐ購入！",
            "&7残高: &e" + VaultManager.formatYen(plugin.getVaultManager().getBalance(player)),
            "&8手数料5%が差し引かれます"
        ));
        inv.setItem(15, buyBtn);

        // 戻るボタン
        inv.setItem(22, buildNavItem(Material.ARROW, "&7← 一覧に戻る"));

        // IDをガラスパネルで保持（スロット0に）
        ItemStack idHolder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = idHolder.getItemMeta();
        VersionAdapter.setDisplayName(m, String.valueOf(String.valueOf(auctionId)));
        idHolder.setItemMeta(m);
        inv.setItem(0, idHolder);

        player.openInventory(inv);
    }

    /** 自分の出品一覧 */
    public void openMyListings(Player player) {
        List<AuctionItem> myItems = new ArrayList<>();
        plugin.getAuctionManager().getActiveAuctions().values().stream()
            .filter(a -> a.getSellerUuid().equals(player.getUniqueId()))
            .forEach(myItems::add);

        Inventory inv = Bukkit.createInventory(null, 27, "§e自分の出品一覧");
        for (int i = 0; i < Math.min(myItems.size(), 21); i++) {
            inv.setItem(i, buildAuctionDisplayItem(myItems.get(i)));
        }
        inv.setItem(22, buildNavItem(Material.ARROW, "&7← 一覧に戻る"));
        player.openInventory(inv);
    }

    private ItemStack buildAuctionDisplayItem(AuctionItem auction) {
        ItemStack display = auction.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        long remaining = (auction.getExpiresAt() - System.currentTimeMillis()) / 1000;
        long mins = remaining / 60;
        long secs = remaining % 60;

        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) lore.addAll(meta.getLore());
        lore.add("");
        lore.add("&7出品者: &f" + auction.getSellerName());
        lore.add("&7開始価格: &e" + VaultManager.formatYen(auction.getStartPrice()));
        lore.add("&7現在価格: &6" + VaultManager.formatYen(auction.getCurrentBid()));
        if (auction.getHighestBidderName() != null) {
            lore.add("&7最高入札: &f" + auction.getHighestBidderName());
        }
        lore.add("&7残り時間: &c" + mins + "分" + secs + "秒");
        lore.add("&7ID: &8#" + auction.getId());
        lore.add("");
        lore.add("&aクリックで詳細を見る");
        VersionAdapter.setLore(meta, lore);

        // IDをPDCで保存
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "auction_id"),
            org.bukkit.persistence.PersistentDataType.INTEGER,
            auction.getId()
        );
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack buildNavItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        VersionAdapter.setDisplayName(meta, name);
        item.setItemMeta(meta);
        return item;
    }

    private void addLore(ItemStack item, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        List<String> loreComps = new ArrayList<>();
        loreComps.addAll(lore);
        VersionAdapter.setLore(meta, loreComps);
        item.setItemMeta(meta);
    }

    private ItemStack buildInfoItem(int page, int totalPages, int total) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        VersionAdapter.setDisplayName(meta, String.valueOf("&7ページ " + (page + 1 + "/" + totalPages)));
        VersionAdapter.setLore(meta, List.of("&7全 &f" + total + " &7件出品中"));
        item.setItemMeta(meta);
        return item;
    }

    public static String GUI_TITLE_STR() { return GUI_TITLE; }
}
