package com.warforge.core.listener;

import com.warforge.core.WarforgeCore;
import com.warforge.core.auction.AuctionGUI;
import com.warforge.core.auction.AuctionManager;
import com.warforge.core.util.Messages;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionListener implements Listener {

    private final WarforgeCore plugin;
    // 入札入力待ちプレイヤー: UUID → auctionId
    private final Map<UUID, Integer> awaitingBidInput = new HashMap<>();

    public AuctionListener(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = event.getView().getTitle().replace("§", "&");

        // ─── 一覧GUI ───
        if (title.contains(AuctionGUI.GUI_TITLE_STR())) {

            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            // ページナビ
            String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? VersionAdapter.getDisplayName(clicked.getItemMeta()) : "";

            if (name.contains("次のページ")) {
                // TODO: ページ番号管理
                return;
            }
            if (name.contains("自分の出品")) {
                plugin.getAuctionGUI().openMyListings(player);
                return;
            }
            if (name.contains("閉じる")) {
                player.closeInventory();
                return;
            }

            // アイテムクリック → 詳細へ
            if (clicked.hasItemMeta()) {
                Integer auctionId = clicked.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, "auction_id"), PersistentDataType.INTEGER);
                if (auctionId != null) {
                    plugin.getAuctionGUI().openDetail(player, auctionId);
                }
            }
        }

        // ─── 詳細GUI ───
        if (event.getInventory().getSize() == 27 &&
            event.getView().getTitle().contains("オークション詳細")) {

            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            // IDホルダーからauctionId取得
            ItemStack idHolder = event.getInventory().getItem(0);
            if (idHolder == null || !idHolder.hasItemMeta()) return;
            String idStr = VersionAdapter.getDisplayName(idHolder.getItemMeta());
            int auctionId;
            try { auctionId = Integer.parseInt(idStr); } catch (NumberFormatException e) { return; }

            String btnName = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? VersionAdapter.getDisplayName(clicked.getItemMeta()) : "";

            if (btnName.contains("入札")) {
                player.closeInventory();
                awaitingBidInput.put(player.getUniqueId(), auctionId);
                player.sendMessage(Messages.INSTANCE.prefixed("&e入札金額を入力してください。 &8(cancelで中止)"));
            } else if (btnName.contains("即決")) {
                handleBuyNow(player, auctionId);
            } else if (btnName.contains("戻る")) {
                plugin.getAuctionGUI().openList(player, 0);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!awaitingBidInput.containsKey(uuid)) return;
        event.setCancelled(true);

        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            awaitingBidInput.remove(uuid);
            player.sendMessage(Messages.INSTANCE.prefixed("&c入札をキャンセルしました。"));
            return;
        }

        try {
            double amount = Double.parseDouble(msg.replace(",", "").replace("¥", ""));
            int auctionId = awaitingBidInput.remove(uuid);

            // メインスレッドで処理
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                AuctionManager.AuctionResult result = plugin.getAuctionManager().bid(player, auctionId, amount);
                handleResult(player, result, "入札");
            });
        } catch (NumberFormatException e) {
            player.sendMessage(Messages.INSTANCE.prefixed("&c数字を入力してください。"));
        }
    }

    private void handleBuyNow(Player player, int auctionId) {
        AuctionManager.AuctionResult result = plugin.getAuctionManager().buyNow(player, auctionId);
        handleResult(player, result, "購入");
        if (result == AuctionManager.AuctionResult.SUCCESS) {
            player.closeInventory();
        }
    }

    private void handleResult(Player player, AuctionManager.AuctionResult result, String action) {
        String msg = switch (result) {
            case SUCCESS -> "&a" + action + "しました！";
            case NO_ECONOMY -> "&cVaultが無効です。";
            case INVALID_ITEM -> "&c無効なアイテムです。";
            case INVALID_PRICE -> "&c価格は¥1以上にしてください。";
            case MAX_LISTINGS -> "&c出品数の上限に達しています（最大5件）。";
            case NOT_FOUND -> "&cオークションが見つかりません。";
            case SELF_BID -> "&c自分の出品には入札/購入できません。";
            case BID_TOO_LOW -> "&c現在の入札額より高い金額を入力してください。";
            case EXPIRED -> "&cこのオークションは終了しています。";
            case INSUFFICIENT_FUNDS -> "&c残高が不足しています。";
            case NO_PERMISSION -> "&c権限がありません。";
            case HAS_BIDS -> "&c入札があるためキャンセルできません。";
        };
        player.sendMessage(Messages.INSTANCE.prefixed(msg));
    }
}
