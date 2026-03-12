package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VoteCommand implements CommandExecutor {

    private final WarforgeCore plugin;

    public VoteCommand(WarforgeCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。"); return true;
        }
        
        if (args.length == 0) { player.sendMessage(Messages.INSTANCE.prefixed("&c/vote <アリーナID>")); return true; }
        try {
            int candidateId = Integer.parseInt(args[0]);
            plugin.getGameManager().getPlayerLobby(player.getUniqueId()).ifPresentOrElse(
                lobby -> plugin.getVoteManager().vote(player, lobby.getArena().getId(), candidateId),
                () -> player.sendMessage(Messages.INSTANCE.prefixed("&cロビーに参加していません。"))
            );
        } catch (NumberFormatException e) { player.sendMessage(Messages.INSTANCE.prefixed("&cIDは数字で入力してください。")); }
        return true;
    }
}
