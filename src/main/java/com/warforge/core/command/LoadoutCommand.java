package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoadoutCommand implements CommandExecutor {

    private final WarforgeCore plugin;

    public LoadoutCommand(WarforgeCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。"); return true;
        }
        
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            plugin.getLoadoutManager().list(player); return true;
        }
        if (args[0].equalsIgnoreCase("save") && args.length >= 2) {
            try { int slot = Integer.parseInt(args[1]);
                String name = args.length >= 3 ? args[2] : "loadout" + slot;
                plugin.getLoadoutManager().save(player, slot, name);
            } catch (NumberFormatException e) { player.sendMessage(Messages.INSTANCE.prefixed("&c数字を入力してください。")); }
            return true;
        }
        if (args[0].equalsIgnoreCase("load") && args.length >= 2) {
            try { plugin.getLoadoutManager().load(player, Integer.parseInt(args[1])); }
            catch (NumberFormatException e) { player.sendMessage(Messages.INSTANCE.prefixed("&c数字を入力してください。")); }
            return true;
        }
        player.sendMessage(Messages.INSTANCE.prefixed("&7/loadout [save <1-3> [名前] | load <1-3> | list]"));
        return true;
    }
}
