package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.auction.AuctionManager;
import com.warforge.core.economy.VaultManager;
import com.warforge.core.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AuctionCommand implements CommandExecutor {

    private final WarforgeCore plugin;

    public AuctionCommand(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。");
            return true;
        }

        if (args.length == 0) {
            plugin.getAuctionGUI().openList(player, 0);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell", "出品" -> {
                // /auction sell <開始価格>
                if (args.length < 2) {
                    player.sendMessage(Messages.INSTANCE.prefixed(
                        "&c使用法: /auction sell <開始価格>  &7(手に持ったアイテムを出品)")); return true;
                }
                double price;
                try { price = Double.parseDouble(args[1].replace(",", "")); }
                catch (NumberFormatException e) {
                    player.sendMessage(Messages.INSTANCE.prefixed("&c価格は数字で入力してください。")); return true;
                }
                ItemStack hand = player.getInventory().getItemInMainHand();
                AuctionManager.AuctionResult result = plugin.getAuctionManager().listItem(player, hand, price);
                handleResult(player, result, "出品");
            }
            case "bid", "入札" -> {
                // /auction bid <id> <金額>
                if (args.length < 3) {
                    player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /auction bid <ID> <金額>")); return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    double amount = Double.parseDouble(args[2].replace(",", ""));
                    handleResult(player, plugin.getAuctionManager().bid(player, id, amount), "入札");
                } catch (NumberFormatException e) {
                    player.sendMessage(Messages.INSTANCE.prefixed("&c正しい形式で入力してください。"));
                }
            }
            case "buy", "購入" -> {
                // /auction buy <id>
                if (args.length < 2) {
                    player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /auction buy <ID>")); return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    handleResult(player, plugin.getAuctionManager().buyNow(player, id), "購入");
                } catch (NumberFormatException e) {
                    player.sendMessage(Messages.INSTANCE.prefixed("&c正しいIDを入力してください。"));
                }
            }
            case "cancel", "キャンセル" -> {
                if (args.length < 2) {
                    player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /auction cancel <ID>")); return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    handleResult(player, plugin.getAuctionManager().cancel(player, id), "キャンセル");
                } catch (NumberFormatException e) {
                    player.sendMessage(Messages.INSTANCE.prefixed("&c正しいIDを入力してください。"));
                }
            }
            case "list", "一覧" -> {
                // テキストで一覧表示
                var auctions = plugin.getAuctionManager().getActiveAuctions();
                if (auctions.isEmpty()) {
                    player.sendMessage(Messages.INSTANCE.prefixed("&7現在出品中のアイテムはありません。"));
                    return true;
                }
                player.sendMessage(Messages.INSTANCE.prefixed("&7--- オークション一覧 ---"));
                auctions.values().forEach(a -> {
                    long remaining = (a.getExpiresAt() - System.currentTimeMillis()) / 60000;
                    player.sendMessage(
                        "&8#" + a.getId() + " &f" + a.getItem().getType().name() +
                        " &7出品者:&f" + a.getSellerName() +
                        " &7現在値:&e" + VaultManager.formatYen(a.getCurrentBid()) +
                        " &7残り:&c" + remaining + "分"
                    );
                });
                player.sendMessage("&7/auction または &7/auction buy <ID> &7で購入");
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleResult(Player player, AuctionManager.AuctionResult result, String action) {
        String msg = switch (result) {
            case SUCCESS -> "&a" + action + "しました！";
            case NO_ECONOMY -> "&cVaultが有効ではありません。";
            case INVALID_ITEM -> "&c有効なアイテムを手に持ってください。";
            case INVALID_PRICE -> "&c価格は¥1以上にしてください。";
            case MAX_LISTINGS -> "&c出品上限（5件）に達しています。";
            case NOT_FOUND -> "&cID が見つかりません。";
            case SELF_BID -> "&c自分の出品には" + action + "できません。";
            case BID_TOO_LOW -> "&c現在価格より高い金額を指定してください。";
            case EXPIRED -> "&cこのオークションは終了しています。";
            case INSUFFICIENT_FUNDS -> "&c残高が足りません。";
            case NO_PERMISSION -> "&c権限がありません。";
            case HAS_BIDS -> "&c入札があるためキャンセルできません。";
        };
        player.sendMessage(Messages.INSTANCE.prefixed(msg));
    }

    private void sendHelp(Player player) {
        player.sendMessage(Messages.INSTANCE.prefixed("&7--- /auction コマンド ---"));
        player.sendMessage("&7/auction &8- GUIを開く");
        player.sendMessage("&7/auction sell <価格> &8- 手持ちアイテムを出品");
        player.sendMessage("&7/auction bid <ID> <金額> &8- 入札");
        player.sendMessage("&7/auction buy <ID> &8- 即決購入");
        player.sendMessage("&7/auction cancel <ID> &8- 出品キャンセル");
        player.sendMessage("&7/auction list &8- テキスト一覧");
    }
}
