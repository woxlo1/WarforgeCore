package com.warforge.core.command;

import com.warforge.core.WarforgeCore;
import com.warforge.core.arena.Arena;
import com.warforge.core.util.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaCommand implements CommandExecutor {

    private final WarforgeCore plugin;

    public ArenaCommand(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("プレイヤーのみ使用可能です。");
            return true;
        }
        if (!player.hasPermission("warforge.admin")) {
            player.sendMessage(Messages.INSTANCE.prefixed("&c権限がありません。"));
            return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "wand" -> {
                // /arena wand <名前> <ゲームタイプ>
                if (args.length < 3) {
                    sender.sendMessage(plugin.getLangManager().prefixed("error.invalid-args",
                        "usage", "/arena wand <名前> <tdm|br|domination|goldrush|heist>"));
                    return true;
                }
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("プレイヤーのみ使用可能です。"); return true;
                }
                String arenaName = args[1];
                String gameType = args[2].toLowerCase();
                plugin.getArenaWand().startSession(p, arenaName, gameType);
                p.sendMessage(plugin.getLangManager().prefixed("arena.wand-given"));
            }

            case "create" -> {
                if (args.length < 3) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena create <名前> <mode>")); return true; }
                Arena arena = plugin.getArenaManager().createArena(args[1], args[2].toLowerCase(), player);
                player.sendMessage(Messages.INSTANCE.prefixed("&aアリーナ &f" + args[1] + " &a(ID:" + arena.getId() + ") 作成！"));
            }
            case "delete" -> {
                if (args.length < 2) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena delete <id>")); return true; }
                boolean ok = plugin.getArenaManager().deleteArena(Integer.parseInt(args[1]));
                player.sendMessage(ok ? Messages.INSTANCE.prefixed("&a削除しました。") : Messages.INSTANCE.prefixed("&c見つかりません。"));
            }

            // ─── 共通スポーン ───
            case "setspawn" -> {
                if (args.length < 2) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena setspawn <id>")); return true; }
                plugin.getArenaManager().addSpawnPoint(Integer.parseInt(args[1]), player.getLocation());
                player.sendMessage(Messages.INSTANCE.prefixed("&a共通スポーン追加！"));
            }
            case "setlobby" -> {
                if (args.length < 2) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena setlobby <id>")); return true; }
                plugin.getArenaManager().setLobbySpawn(Integer.parseInt(args[1]), player.getLocation());
                player.sendMessage(Messages.INSTANCE.prefixed("&aロビースポーン設定！"));
            }

            // ─── チーム別スポーン ───
            case "setredspawn" -> {
                if (args.length < 2) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena setredspawn <id>")); return true; }
                plugin.getArenaManager().addRedSpawnPoint(Integer.parseInt(args[1]), player.getLocation());
                player.sendMessage(Messages.INSTANCE.prefixed("&c赤チームスポーン追加！"));
            }
            case "setbluespawn" -> {
                if (args.length < 2) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena setbluespawn <id>")); return true; }
                plugin.getArenaManager().addBlueSpawnPoint(Integer.parseInt(args[1]), player.getLocation());
                player.sendMessage(Messages.INSTANCE.prefixed("&9青チームスポーン追加！"));
            }

            // ─── Heist ネクサス ───
            case "setrednexus" -> {
                if (args.length < 2) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena setrednexus <id>")); return true; }
                plugin.getArenaManager().setRedNexus(Integer.parseInt(args[1]), player.getTargetBlock(null, 5) != null
                    ? player.getTargetBlock(null, 5).getLocation() : player.getLocation());
                player.sendMessage(Messages.INSTANCE.prefixed("&c赤ネクサス設定！ &7(見ているブロック)"));
            }
            case "setbluenexus" -> {
                if (args.length < 2) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena setbluenexus <id>")); return true; }
                plugin.getArenaManager().setBlueNexus(Integer.parseInt(args[1]), player.getTargetBlock(null, 5) != null
                    ? player.getTargetBlock(null, 5).getLocation() : player.getLocation());
                player.sendMessage(Messages.INSTANCE.prefixed("&9青ネクサス設定！ &7(見ているブロック)"));
            }

            // ─── Heist パラメータ ───
            case "setheistgold" -> {
                // /arena setheistgold <id> <目標金額> <1回掘るごとの金額>
                if (args.length < 4) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena setheistgold <id> <目標> <掘り金額>")); return true; }
                int id = Integer.parseInt(args[1]);
                int winGold = Integer.parseInt(args[2]);
                int perMine = Integer.parseInt(args[3]);
                plugin.getArenaManager().setHeistGoldSettings(id, winGold, perMine);
                player.sendMessage(Messages.INSTANCE.prefixed("&aHeist設定: 目標 &e" + winGold + " &a/ 掘り &e" + perMine + "G"));
            }

            // ─── 拠点制圧 ───
            case "addcapture" -> {
                if (args.length < 3) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena addcapture <id> <名前> [radius]")); return true; }
                double radius = args.length >= 4 ? Double.parseDouble(args[3]) : 5.0;
                plugin.getArenaManager().addCapturePoint(Integer.parseInt(args[1]), args[2], player.getLocation(), radius);
                player.sendMessage(Messages.INSTANCE.prefixed("&a拠点 &f" + args[2] + " &a追加！"));
            }

            // ─── ゴールドラッシュ ───
            case "addgoldspawn" -> {
                if (args.length < 2) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena addgoldspawn <id>")); return true; }
                plugin.getArenaManager().addGoldSpawn(Integer.parseInt(args[1]), player.getLocation());
                player.sendMessage(Messages.INSTANCE.prefixed("&aゴールドスポーン追加！"));
            }

            case "list" -> {
                player.sendMessage(Messages.INSTANCE.prefixed("&7--- アリーナ一覧 ---"));
                plugin.getArenaManager().getAllArenas().forEach(a ->
                    player.sendMessage("&8[&f" + a.getId() + "&8] &f" + a.getName() +
                        " &7mode:&f" + a.getMode() +
                        " spawn:&f" + a.getSpawnPoints().size() +
                        " red:&c" + a.getRedSpawnPoints().size() +
                        " blue:&9" + a.getBlueSpawnPoints().size() +
                        (a.getEnabled() ? " &a有効" : " &c無効"))
                );
            }
            case "info" -> {
                if (args.length < 2) { player.sendMessage(Messages.INSTANCE.prefixed("&c使用法: /arena info <id>")); return true; }
                Arena a = plugin.getArenaManager().getArena(Integer.parseInt(args[1]));
                if (a == null) { player.sendMessage(Messages.INSTANCE.prefixed("&c見つかりません。")); return true; }
                player.sendMessage(Messages.INSTANCE.prefixed("&7--- " + a.getName() + " 情報 ---"));
                player.sendMessage("&7モード: &f" + a.getMode());
                player.sendMessage("&7共通スポーン: &f" + a.getSpawnPoints().size() + "箇所");
                player.sendMessage("&cRedスポーン: &f" + a.getRedSpawnPoints().size() + "箇所");
                player.sendMessage("&9Blueスポーン: &f" + a.getBlueSpawnPoints().size() + "箇所");
                player.sendMessage("&c赤ネクサス: &f" + (a.getRedNexus() != null ? "設定済み" : "未設定"));
                player.sendMessage("&9青ネクサス: &f" + (a.getBlueNexus() != null ? "設定済み" : "未設定"));
                player.sendMessage("&7Heist目標: &e" + a.getHeistWinGold() + "G / 掘り: &e" + a.getHeistGoldPerMine() + "G");
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Messages.INSTANCE.prefixed("&7--- /arena コマンド ---"));
        player.sendMessage("&7create <名前> <mode>  &8| mode: tdm/br/domination/goldrush/heist");
        player.sendMessage("&7setspawn / setlobby <id>");
        player.sendMessage("&csetredspawn &7/ &9setbluespawn &7<id>  &8← チーム別スポーン");
        player.sendMessage("&csetrednexus &7/ &9setbluenexus &7<id>  &8← 見てるブロックがネクサスに");
        player.sendMessage("&7setheistgold <id> <目標> <掘り金額>");
        player.sendMessage("&7addcapture <id> <名前> [radius]");
        player.sendMessage("&7addgoldspawn <id>");
        player.sendMessage("&7list / info <id>");
    }
}
