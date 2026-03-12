package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.gun.GunData;
import com.warforge.core.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GunCommand implements CommandExecutor {

    private final WarforgeCore plugin;

    public GunCommand(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "give" -> {
                if (!sender.hasPermission("warforge.admin")) {
                    sender.sendMessage(Messages.INSTANCE.prefixed("&c権限がありません。")); return true;
                }
                // /gun give <player> <gunId> [amount]
                if (args.length < 3) {
                    sender.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /gun give <プレイヤー> <銃ID>")); return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Messages.INSTANCE.prefixed("&cプレイヤーが見つかりません。")); return true;
                }
                ItemStack gun = plugin.getGunItemManager().createGunItem(args[2]);
                if (gun == null) {
                    sender.sendMessage(Messages.INSTANCE.prefixed("&c銃ID &f" + args[2] + " &cが見つかりません。")); return true;
                }
                target.getInventory().addItem(gun);
                plugin.getBulletHandler().initAmmo(target.getUniqueId(), plugin.getGunManager().getGun(args[2]));
                target.sendMessage(Messages.INSTANCE.prefixed("&a銃を受け取りました: &f" + args[2]));
                sender.sendMessage(Messages.INSTANCE.prefixed("&a" + target.getName() + " に &f" + args[2] + " &aを付与しました。"));
            }
            case "list" -> {
                sender.sendMessage(Messages.INSTANCE.prefixed("&7--- 銃一覧 ---"));
                plugin.getGunManager().getAllGuns().forEach((id, gun) ->
                    sender.sendMessage(gun.getRarity().getColor() + "  " + id +
                        " &7(" + gun.getGunType().name() + "/" + gun.getFireMode().name() + ")" +
                        " &fダメージ:" + gun.getDamage() +
                        " &e¥" + String.format("%,.0f", gun.getPrice()))
                );
            }
            case "info" -> {
                if (args.length < 2) { sender.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /gun info <銃ID>")); return true; }
                GunData gun = plugin.getGunManager().getGun(args[1]);
                if (gun == null) { sender.sendMessage(Messages.INSTANCE.prefixed("&c見つかりません。")); return true; }
                sender.sendMessage(Messages.INSTANCE.prefixed("&7--- " + gun.getDisplayName() + " &7---"));
                sender.sendMessage("&7タイプ: &f" + gun.getGunType() + " | モード: &f" + gun.getFireMode());
                sender.sendMessage("&7ダメージ: &c" + gun.getDamage() + " &7HS: &6x" + gun.getHeadshotMultiplier());
                sender.sendMessage("&7足: &fx" + gun.getLegMultiplier() + " &7貫通: &f" + (int)(gun.getArmorPenetration()*100) + "%");
                sender.sendMessage("&7弾数: &f" + gun.getMagazineSize() + " &7予備: &f" + gun.getReserveAmmo());
                sender.sendMessage("&7射程: &f" + gun.getRange() + "m &7減衰: &f" + gun.getDropOff());
                sender.sendMessage("&7リコイル: &f" + gun.getRecoil() + " (V:" + gun.getRecoilVertical() + "/H:" + gun.getRecoilHorizontal() + ")");
                sender.sendMessage("&7リロード: &f" + gun.getReloadTime()/1000.0 + "s &7タクティカル: &f" + gun.getTacticalReloadTime()/1000.0 + "s");
                sender.sendMessage("&7価格: &e¥" + String.format("%,.0f", gun.getPrice()));
            }
            case "reload" -> {
                // /gun reload - 設定ファイルをホットリロード
                if (!sender.hasPermission("warforge.admin")) {
                    sender.sendMessage(Messages.INSTANCE.prefixed("&c権限がありません。")); return true;
                }
                plugin.getGunManager().loadGuns();
                sender.sendMessage(Messages.INSTANCE.prefixed("&a銃データをリロードしました！ &7(" + plugin.getGunManager().getAllGuns().size() + "種類)"));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Messages.INSTANCE.prefixed("&7--- /gun コマンド ---"));
        sender.sendMessage("&7/gun give <player> <id>  &8← 銃を付与");
        sender.sendMessage("&7/gun list               &8← 全銃一覧");
        sender.sendMessage("&7/gun info <id>          &8← 銃の詳細");
        sender.sendMessage("&7/gun reload             &8← 設定リロード");
    }
}
