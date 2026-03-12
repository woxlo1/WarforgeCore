package com.warforge.core.listener;

import com.warforge.core.WarforgeCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final WarforgeCore plugin;

    public PlayerListener(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerManager().loadPlayer(
            event.getPlayer().getUniqueId(),
            event.getPlayer().getName()
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerManager().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
