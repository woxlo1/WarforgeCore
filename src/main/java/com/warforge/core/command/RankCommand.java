package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankCommand implements CommandExecutor {

    private final WarforgeCore plugin;

    public RankCommand(WarforgeCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。"); return true;
        }
        
        if (args.length >= 1 && args[0].equalsIgnoreCase("top")) {
            String col = args.length >= 2 ? args[1] : "rank";
            plugin.getStatsManager().showTopRanking(player, col, 10);
        } else {
            var rank = plugin.getRankManager().getRank(player.getUniqueId());
            var wfp = plugin.getPlayerManager().getPlayer(player.getUniqueId());
            int pts = wfp != null ? wfp.getRankPoints() : 0;
            player.sendMessage(Messages.INSTANCE.prefixed("&7ランク: " + rank.formatted() + " &7(" + pts + "pt)"));
            player.sendMessage(Messages.INSTANCE.prefixed("&7/rank top [kills|wins|kdr] でランキングを表示"));
        }
        return true;
    }
}
