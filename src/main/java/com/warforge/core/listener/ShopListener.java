package com.warforge.core.listener;

import com.warforge.core.WarforgeCore;
import com.warforge.core.shop.ShopManager;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ShopListener implements Listener {

    private final WarforgeCore plugin;

    public ShopListener(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.contains("Warforgeショップ")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        // カテゴリタブ
        String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
            ? VersionAdapter.getDisplayName(clicked.getItemMeta()) : "";

        if (name.contains("銃器")) {
            plugin.getShopManager().openShop(player, ShopManager.ShopCategory.GUNS);
            return;
        }
        if (name.contains("装備")) {
            plugin.getShopManager().openShop(player, ShopManager.ShopCategory.EQUIPMENT);
            return;
        }
        if (name.contains("閉じる")) {
            player.closeInventory();
            return;
        }

        // 銃購入
        if (clicked.hasItemMeta()) {
            String gunId = clicked.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "shop_gun_id"), PersistentDataType.STRING);
            if (gunId != null) {
                ShopManager.ShopResult result = plugin.getShopManager().buyGun(player, gunId);
                if (result == ShopManager.ShopResult.SUCCESS ||
                    result == ShopManager.ShopResult.FREE_ITEM) {
                    // 残高更新のためGUIを再描画
                    plugin.getShopManager().openShop(player, ShopManager.ShopCategory.GUNS);
                }
            }
        }
    }
}
