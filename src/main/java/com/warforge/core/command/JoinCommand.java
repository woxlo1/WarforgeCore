package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.arena.Arena;
import com.warforge.core.game.lobby.GameLobby;
import com.warforge.core.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class JoinCommand implements CommandExecutor {

    private final WarforgeCore plugin;
    private final boolean isLeave;

    public JoinCommand(WarforgeCore plugin, boolean isLeave) {
        this.plugin = plugin;
        this.isLeave = isLeave;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。"); return true;
        }

        if (isLeave) {
            plugin.getGameManager().leaveCurrentGame(player);
            player.sendMessage(Messages.INSTANCE.prefixed("&a試合を退出しました。"));
            return true;
        }

        // /join → アリーナ一覧
        if (args.length == 0) {
            showArenaList(player);
            return true;
        }

        // /join <名前 or ID>
        try {
            int id = Integer.parseInt(args[0]);
            plugin.getGameManager().joinArena(player, id);
        } catch (NumberFormatException e) {
            plugin.getGameManager().joinArenaByName(player, args[0]);
        }
        return true;
    }

    private void showArenaList(Player player) {
        List<Arena> arenas = plugin.getArenaManager().getAllArenas().stream()
            .filter(Arena::getEnabled).toList();

        if (arenas.isEmpty()) {
            player.sendMessage(Messages.INSTANCE.prefixed("&c利用可能なアリーナがありません。"));
            return;
        }

        player.sendMessage(Messages.INSTANCE.prefixed("&7--- アリーナ一覧 ---"));
        arenas.forEach(arena -> {
            GameLobby lobby = plugin.getGameManager().getLobbies().get(arena.getId());
            boolean inGame = plugin.getGameManager().getActiveGames().containsKey(arena.getId());
            int playerCount = lobby != null ? lobby.getWaitingPlayers().size() : 0;
            String status = inGame ? "&c試合中" :
                lobby != null && lobby.getState() == GameLobby.LobbyState.STARTING ? "&e開始待ち" : "&a待機中";

            player.sendMessage(
                "&8[&f" + arena.getId() + "&8] &f" + arena.getName() +
                " &7(" + arena.getMode().toUpperCase() + ")" +
                " &7人数: &f" + playerCount + "/" + arena.getMaxPlayers() +
                " " + status +
                " &7→ &e/join " + arena.getId()
            );
        });
    }
}
