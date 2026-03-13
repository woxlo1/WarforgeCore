package com.warforge.core.listener;

import com.warforge.core.WarforgeCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class GameListener implements Listener {

    private final WarforgeCore plugin;

    public GameListener(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        plugin.getGameManager().getPlayerGame(victim.getUniqueId()).ifPresent(game -> {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            game.onDeath(victim);
            plugin.getKillstreakManager().onDeath(victim);
        });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().getPlayerGame(player.getUniqueId()).ifPresent(game -> {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.setHealth(20.0);
                player.setFoodLevel(20);
            }, 5L);
        });
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (plugin.getConfigManager().isHungerEnabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        plugin.getGameManager().getPlayerGame(player.getUniqueId()).ifPresent(g ->
            event.setCancelled(true));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getConfigManager().isBlockBreakAllowed()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("warforge.admin")) return;
        plugin.getGameManager().getPlayerGame(player.getUniqueId()).ifPresent(g ->
            event.setCancelled(true));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getConfigManager().isBlockPlaceAllowed()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("warforge.admin")) return;
        plugin.getGameManager().getPlayerGame(player.getUniqueId()).ifPresent(g ->
            event.setCancelled(true));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().leaveCurrentGame(player);
        plugin.getPlayerManager().unloadPlayer(player.getUniqueId());
    }
}
