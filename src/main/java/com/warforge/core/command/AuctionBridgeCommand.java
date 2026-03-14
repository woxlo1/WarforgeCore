package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * WarforgeCore 側の /auction ブリッジコマンド。
 *
 * WarforgeAuction が導入されている場合は WarforgeAuctionProvider.getAPI() 経由で委譲する。
 * 導入されていない場合は「WarforgeAuction が必要です」と案内する。
 *
 * WarforgeCore の plugin.yml に以下を追加すること:
 *   auction:
 *     description: オークション
 *     usage: /auction
 */
public class AuctionBridgeCommand implements CommandExecutor {

    private final WarforgeCore plugin;

    public AuctionBridgeCommand(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。");
            return true;
        }

        // WarforgeAuction が導入されているか確認
        try {
            Class<?> providerClass = Class.forName("com.warforge.auction.api.WarforgeAuctionProvider");
            boolean available = (boolean) providerClass.getMethod("isAvailable").invoke(null);

            if (!available) {
                player.sendMessage(Messages.INSTANCE.prefixed(
                    "&cWarforgeAuction プラグインが見つかりません。サーバー管理者に連絡してください。"));
                return true;
            }

            // WarforgeAuction の AuctionGUI を直接取得して GUI を開く
            // （WarforgeAuction のコマンドハンドラにそのまま委譲するより
            //   GUI クラスを使う方がシンプルなので Reflection で呼ぶ）
            Class<?> mainClass = Class.forName("com.warforge.auction.WarforgeAuction");
            Object waInstance = mainClass.getMethod("getInstance").invoke(null);
            Object gui = mainClass.getMethod("getAuctionGUI").invoke(waInstance);

            if (args.length == 0 || args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("browse")) {
                gui.getClass().getMethod("openBrowse", Player.class, int.class).invoke(gui, player, 0);
            } else if (args[0].equalsIgnoreCase("my") || args[0].equalsIgnoreCase("mylist")) {
                gui.getClass().getMethod("openMyListings", Player.class).invoke(gui, player);
            } else if (args[0].equalsIgnoreCase("sell") && args.length >= 2) {
                try {
                    double price = Double.parseDouble(args[1]);
                    long hours = args.length >= 3 ? Long.parseLong(args[2])
                        : plugin.getConfigManager().getAuctionDurationMs() / 3600_000L;
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand.getType().isAir()) {
                        player.sendMessage(Messages.INSTANCE.prefixed("&c手に持っているアイテムがありません。"));
                        return true;
                    }
                    Object api = providerClass.getMethod("getAPI").invoke(null);
                    ItemStack selling = hand.clone();
                    selling.setAmount(1);
                    hand.setAmount(hand.getAmount() - 1);
                    api.getClass().getMethod("listItem", Player.class, ItemStack.class, double.class, long.class)
                        .invoke(api, player, selling, price, hours * 3600_000L);
                } catch (NumberFormatException e) {
                    player.sendMessage(Messages.INSTANCE.prefixed("&c数値を正しく入力してください。"));
                }
            } else {
                // それ以外はヘルプ表示
                player.sendMessage("§8--- §6/auction コマンド §8---");
                player.sendMessage("§7list              §8- 一覧 GUI");
                player.sendMessage("§7sell <価格> [時間h] §8- 出品");
                player.sendMessage("§7my                §8- 自分の出品");
            }

        } catch (ClassNotFoundException e) {
            player.sendMessage(Messages.INSTANCE.prefixed(
                "&cWarforgeAuction が導入されていません。"));
        } catch (Exception e) {
            plugin.getLogger().warning("[AuctionBridge] エラー: " + e.getMessage());
            player.sendMessage(Messages.INSTANCE.prefixed("&cオークション機能でエラーが発生しました。"));
        }
        return true;
    }
}
