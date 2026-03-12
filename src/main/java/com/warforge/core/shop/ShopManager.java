package com.warforge.core.shop;

import com.warforge.core.WarforgeCore;
import com.warforge.core.gun.GunData;
import com.warforge.core.util.Messages;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class ShopManager {

    private final WarforgeCore plugin;

    public ShopManager(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    // ─── 購入処理 ───

    public ShopResult buyGun(Player player, String gunId) {
        if (!plugin.getVaultManager().isEnabled()) return ShopResult.NO_ECONOMY;
        GunData gun = plugin.getGunManager().getGun(gunId);
        if (gun == null) return ShopResult.NOT_FOUND;
        if (gun.getPrice() <= 0) return ShopResult.FREE_ITEM; // 無料は直接付与

        double balance = plugin.getVaultManager().getBalance(player);
        if (balance < gun.getPrice()) return ShopResult.INSUFFICIENT_FUNDS;

        plugin.getVaultManager().getEconomy().withdrawPlayer(player, gun.getPrice());
        ItemStack item = plugin.getGunItemManager().createGunItem(gunId);
        player.getInventory().addItem(item);
        plugin.getBulletHandler().initAmmo(player.getUniqueId(), gun);

        player.sendMessage(Messages.INSTANCE.prefixed(
            "&a" + gun.getDisplayName() + " &7を購入しました！ &c-¥" +
            String.format("%,.0f", gun.getPrice())
        ));
        return ShopResult.SUCCESS;
    }

    // ─── GUI ───

    public void openShop(Player player, ShopCategory category) {
        String title = "§6§lWarforgeショップ - " + category.display;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // カテゴリタブ
        int slot = 0;
        for (ShopCategory cat : ShopCategory.values()) {
            ItemStack tab = buildItem(cat.material,
                (cat == category ? "&e&l" : "&7") + cat.display,
                cat == category ? List.of("&a現在表示中") : List.of("&7クリックで切り替え"));
            inv.setItem(slot++, tab);
        }

        // 商品一覧（スロット9〜44）
        switch (category) {
            case GUNS -> fillGuns(inv, player);
            case EQUIPMENT -> fillEquipment(inv, player);
        }

        // 閉じるボタン
        inv.setItem(49, buildItem(Material.BARRIER, "&c閉じる", List.of()));
        // 残高表示
        double bal = plugin.getVaultManager().getBalance(player);
        inv.setItem(53, buildItem(Material.GOLD_NUGGET, "&e残高",
            List.of("&f¥" + String.format("%,.0f", bal))));

        player.openInventory(inv);
    }

    private void fillGuns(Inventory inv, Player player) {
        var guns = plugin.getGunManager().getAllGuns().values().stream()
            .sorted(Comparator.comparingDouble(GunData::getPrice))
            .collect(Collectors.toList());

        int slot = 9;
        for (GunData gun : guns) {
            if (slot >= 45) break;
            double balance = plugin.getVaultManager().getBalance(player);
            boolean canBuy = balance >= gun.getPrice();

            List<String> lore = new ArrayList<>(List.of(
                gun.getRarity().getColor() + gun.getGunType().name() + " | " + gun.getFireMode().name(),
                "&7ダメージ: &c" + gun.getDamage() + "  &7射程: &f" + gun.getRange() + "m",
                "&7弾数: &f" + gun.getMagazineSize() + "  &7HS倍率: &6x" + gun.getHeadshotMultiplier(),
                "",
                "&7価格: " + (gun.getPrice() <= 0 ? "&a無料" : "&e¥" + String.format("%,.0f", gun.getPrice())),
                canBuy ? "&aクリックで購入" : "&c残高不足"
            ));

            ItemStack display = plugin.getGunItemManager().createGunItem(gun.getId());
            if (display == null) { slot++; continue; }
            ItemMeta meta = display.getItemMeta();
            VersionAdapter.setLore(meta, lore);
            // shop_gun_idをPDCに保存
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "shop_gun_id"),
                org.bukkit.persistence.PersistentDataType.STRING, gun.getId()
            );
            display.setItemMeta(meta);
            inv.setItem(slot++, display);
        }
    }

    private void fillEquipment(Inventory inv, Player player) {
        // 将来拡張用：グレネード、アーマーなど
        ItemStack placeholder = buildItem(Material.GRAY_STAINED_GLASS_PANE,
            "&7準備中...", List.of("&8近日公開予定"));
        for (int i = 9; i < 45; i++) inv.setItem(i, placeholder);
    }

    public ItemStack buildItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        VersionAdapter.setDisplayName(meta, name);
        VersionAdapter.setLore(meta, lore);
        item.setItemMeta(meta);
        return item;
    }

    public enum ShopCategory {
        GUNS     (Material.IRON_HOE,   "銃器"),
        EQUIPMENT(Material.IRON_CHESTPLATE, "装備");

        final Material material;
        final String display;
        ShopCategory(Material m, String d) { material = m; display = d; }
    }

    public enum ShopResult {
        SUCCESS, NO_ECONOMY, NOT_FOUND, FREE_ITEM, INSUFFICIENT_FUNDS
    }
}
