package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpectateCommand implements CommandExecutor {

    private final WarforgeCore plugin;

    public SpectateCommand(WarforgeCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。"); return true;
        }
        
        if (plugin.getSpectatorManager().isSpectating(player.getUniqueId())) {
            plugin.getSpectatorManager().exitSpectator(player); return true;
        }
        if (args.length == 0) {
            player.sendMessage(Messages.INSTANCE.prefixed("&c/spectate <アリーナID>")); return true;
        }
        try {
            int arenaId = Integer.parseInt(args[0]);
            plugin.getSpectatorManager().enterSpectator(player, arenaId);
        } catch (NumberFormatException e) { player.sendMessage(Messages.INSTANCE.prefixed("&cIDは数字で入力してください。")); }
        return true;
    }
}
