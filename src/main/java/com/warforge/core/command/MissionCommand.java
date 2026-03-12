package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MissionCommand implements CommandExecutor {

    private final WarforgeCore plugin;

    public MissionCommand(WarforgeCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。"); return true;
        }
        
        plugin.getMissionManager().showMissions(player);
        return true;
    }
}
