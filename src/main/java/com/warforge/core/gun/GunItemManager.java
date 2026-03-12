package com.warforge.core.gun;

import com.warforge.core.WarforgeCore;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GunItemManager {

    private final WarforgeCore plugin;
    public final NamespacedKey GUN_ID_KEY;

    public GunItemManager(WarforgeCore plugin) {
        this.plugin = plugin;
        this.GUN_ID_KEY = new NamespacedKey(plugin, "gun_id");
    }

    public ItemStack createGunItem(String gunId) {
        GunData gun = plugin.getGunManager().getGun(gunId);
        if (gun == null) return null;

        Material material = switch (gun.getGunType()) {
            case PISTOL   -> Material.WOODEN_HOE;
            case SMG      -> Material.STONE_HOE;
            case RIFLE    -> Material.IRON_HOE;
            case SNIPER   -> Material.GOLDEN_HOE;
            case SHOTGUN  -> Material.DIAMOND_HOE;
            case LMG      -> Material.NETHERITE_HOE;
            case LAUNCHER -> Material.STICK;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        VersionAdapter.setDisplayName(meta, gun.getRarity().getColor() + gun.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("&8" + gun.getGunType().name() + " | " + gun.getFireMode().name());
        lore.add("&8レアリティ: " + gun.getRarity().getColor() + gun.getRarity().name());
        lore.add("&r");
        lore.add("&c⚔ ダメージ: &f" + gun.getDamage() +
            "  &6✦HS: &fx" + gun.getHeadshotMultiplier());
        lore.add("&b弾数: &f" + gun.getMagazineSize() +
            " / &7予備: " + gun.getReserveAmmo());
        lore.add("&e射程: &f" + gun.getRange() + "m" +
            "  &a速度: &f" + gun.getBulletSpeed());
        lore.add("&dリロード: &f" + (gun.getReloadTime() / 1000.0) + "s");
        if (gun.getArmorPenetration() > 0) {
            lore.add("&6貫通率: &f" + (int)(gun.getArmorPenetration() * 100) + "%");
        }
        if (gun.getSuppressorEquipped()) {
            lore.add("&7サプレッサー装備");
        }
        lore.add("&r");
        lore.add("&7価格: &e¥" + String.format("%,.0f", gun.getPrice()));

        VersionAdapter.setLore(meta, lore);
        meta.getPersistentDataContainer().set(GUN_ID_KEY, PersistentDataType.STRING, gunId);
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);
        return item;
    }

    public String getGunId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(GUN_ID_KEY, PersistentDataType.STRING);
    }

    public boolean isGun(ItemStack item) { return getGunId(item) != null; }
}
