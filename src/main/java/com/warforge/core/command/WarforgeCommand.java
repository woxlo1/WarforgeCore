package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarforgeCommand implements CommandExecutor {

    private final WarforgeCore plugin;

    public WarforgeCommand(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§8[§cWarforge§8] §fv" + plugin.getDescription().getVersion());
            sender.sendMessage("§7/wf reload - 設定リロード");
            sender.sendMessage("§7/wf stats - 自分の統計");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "admin" -> {
                if (!sender.hasPermission("warforge.admin")) {
                    sender.sendMessage(Messages.INSTANCE.prefixed("&c権限がありません。")); return true;
                }
                if (sender instanceof org.bukkit.entity.Player p) {
                    plugin.getAdminGUI().open(p);
                } else {
                    sender.sendMessage("プレイヤーのみ使用可能です。");
                }
            }
            case "reload" -> {
                if (!sender.hasPermission("warforge.admin")) {
                    sender.sendMessage("§c権限がありません。");
                    return true;
                }
                plugin.reloadConfig();
                plugin.getConfigManager().reload();
                plugin.getEconomyConfig().reload();
                plugin.getGunManager().loadGuns();
                sender.sendMessage(plugin.getConfigManager().getMessage("game-start")
                    .replace("{mode}", "リロード完了"));
                sender.sendMessage("§a設定・銃データをリロードしました！");
            }
            case "stats" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cプレイヤーのみ使用可能です。");
                    return true;
                }
                var wfPlayer = plugin.getPlayerManager().getPlayer(player.getUniqueId());
                if (wfPlayer == null) return true;
                sender.sendMessage("§8--- §cWarforge Stats §8---");
                sender.sendMessage("§7Kill: §f" + wfPlayer.getKills());
                sender.sendMessage("§7Death: §f" + wfPlayer.getDeaths());
                sender.sendMessage("§7KDR: §f" + String.format("%.2f", wfPlayer.getKdr()));
                sender.sendMessage("§7Win: §f" + wfPlayer.getWins());
            }
            default -> sender.sendMessage("§c不明なコマンドです。");
        }

        return true;
    }
}
