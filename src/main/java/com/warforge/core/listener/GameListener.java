package com.warforge.core.listener;

import com.warforge.core.WarforgeCore;
import com.warforge.core.game.GameMode;
import com.warforge.core.util.Messages;

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

    /** 死亡イベント */
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

    /** リスポーン時に銃の弾薬を初期化 */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().getPlayerGame(player.getUniqueId()).ifPresent(game -> {
            // ロードアウトを再配布
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getBulletHandler().resetAmmo(player.getUniqueId());
                player.setHealth(20.0);
                player.setFoodLevel(20);
            }, 5L);
        });
    }

    /** 試合中は空腹ゲージを減らさない（config制御）*/
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (plugin.getConfigManager().isHungerEnabled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        plugin.getGameManager().getPlayerGame(player.getUniqueId()).ifPresent(g -> {
            event.setCancelled(true);
        });
    }

    /** 試合中のブロック破壊（config制御）*/
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getConfigManager().isBlockBreakAllowed()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("warforge.admin")) return;
        plugin.getGameManager().getPlayerGame(player.getUniqueId()).ifPresent(g -> {
            event.setCancelled(true);
        });
    }

    /** 試合中のブロック設置（config制御）*/
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getConfigManager().isBlockPlaceAllowed()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("warforge.admin")) return;
        plugin.getGameManager().getPlayerGame(player.getUniqueId()).ifPresent(g -> {
            event.setCancelled(true);
        });
    }

    /** 試合中に切断した場合 */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().leaveCurrentGame(player);
        plugin.getPlayerManager().unloadPlayer(player.getUniqueId());
    }
}
