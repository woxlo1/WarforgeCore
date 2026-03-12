package com.warforge.core.listener;

import com.warforge.core.WarforgeCore;
import com.warforge.core.gun.GunData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GunListener implements Listener {

    private final WarforgeCore plugin;

    public GunListener(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    // 左クリック → 射撃
    @EventHandler
    public void onShoot(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR &&
            event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        String gunId = plugin.getGunItemManager().getGunId(item);
        if (gunId == null) return;

        GunData gun = plugin.getGunManager().getGun(gunId);
        if (gun == null) return;

        event.setCancelled(true);
        plugin.getBulletHandler().shoot(player, gun);
    }

    // 右クリック → リロード
    @EventHandler
    public void onReload(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        String gunId = plugin.getGunItemManager().getGunId(item);
        if (gunId == null) return;

        GunData gun = plugin.getGunManager().getGun(gunId);
        if (gun == null) return;

        event.setCancelled(true);
        plugin.getBulletHandler().reload(player, gun);
    }

    // 銃アイテムをドロップさせない
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (plugin.getGunItemManager().isGun(item)) {
            event.setCancelled(true);
        }
    }

    // デフォルトダメージを無効化（銃ダメージで上書き）
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player damager = (Player) event.getDamager();
        ItemStack item = damager.getInventory().getItemInMainHand();
        if (plugin.getGunItemManager().isGun(item)) {
            event.setCancelled(true);
        }
    }
}
