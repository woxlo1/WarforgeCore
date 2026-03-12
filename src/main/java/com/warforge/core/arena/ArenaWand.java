package com.warforge.core.arena;

import com.warforge.core.WarforgeCore;
import com.warforge.core.i18n.LangManager;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ArenaWand implements Listener {

    private final WarforgeCore plugin;
    // プレイヤーUUID → [pos1, pos2]
    private final Map<UUID, Location[]> selections = new HashMap<>();
    // アリーナ作成待ち状態
    private final Map<UUID, WandSession> sessions = new HashMap<>();

    private static final String WAND_TAG = "warforge_wand";

    public ArenaWand(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    /** ワンドアイテムを生成して渡す */
    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        VersionAdapter.setDisplayName(meta, "&6&l✦ Arena Wand");
        VersionAdapter.setLore(meta, List.of(
            "&7左クリック: &e地点1を選択",
            "&7右クリック: &e地点2を選択",
            "&8WarforgeCore アリーナワンド"
        ));
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, WAND_TAG),
            org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1
        );
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        wand.setItemMeta(meta);
        return wand;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(new NamespacedKey(plugin, WAND_TAG), org.bukkit.persistence.PersistentDataType.BYTE);
    }

    /** アリーナ作成セッションを開始 */
    public void startSession(Player player, String arenaName, String gameType) {
        sessions.put(player.getUniqueId(), new WandSession(arenaName, gameType));
        player.getInventory().addItem(createWand());
        plugin.getLangManager().send(player, "arena.wand-given");
        player.sendMessage(plugin.getLangManager().prefixed("arena.wand-given"));
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isWand(item)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        Location[] sel = selections.computeIfAbsent(uuid, k -> new Location[2]);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            sel[0] = block.getLocation().clone();
            player.sendMessage(plugin.getLangManager().getPrefix() +
                "§e地点1 §7を設定: §f" + formatLoc(sel[0]));
            block.getWorld().spawnParticle(Particle.FLAME, sel[0].add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            sel[1] = block.getLocation().clone();
            player.sendMessage(plugin.getLangManager().getPrefix() +
                "§e地点2 §7を設定: §f" + formatLoc(sel[1]));
            block.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, sel[1].add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0);
        }

        // 両方選択済みの場合
        if (sel[0] != null && sel[1] != null) {
            WandSession session = sessions.get(uuid);
            if (session != null) {
                tryFinalizeArena(player, sel[0], sel[1], session);
            }
        }
    }

    /**
     * アリーナ範囲を確定
     * - チームモードの場合：範囲内に赤・青ウールがあるか確認
     */
    private void tryFinalizeArena(Player player, Location pos1, Location pos2, WandSession session) {
        boolean needsTeams = isTeamMode(session.gameType);

        if (needsTeams) {
            // 範囲内の羊毛チェック
            WoolCheck check = scanForWool(pos1, pos2);
            if (!check.hasRed) {
                player.sendMessage(plugin.getLangManager().prefixed("arena.need-wool",
                    "team", "赤", "color", "赤"));
                return;
            }
            if (!check.hasBlue) {
                player.sendMessage(plugin.getLangManager().prefixed("arena.need-wool",
                    "team", "青", "color", "青"));
                return;
            }

            // 羊毛の位置をスポーン候補として登録
            player.sendMessage(plugin.getLangManager().getPrefix() +
                "§a赤・青ウールを検出！ スポーン地点として登録します。");
            finalizeWithWool(player, session, check);
        } else {
            // ソロモード：手動でスポーン設定
            createArena(player, session, pos1, null, null);
        }

        // セッション終了
        sessions.remove(player.getUniqueId());
        selections.remove(player.getUniqueId());
    }

    private void finalizeWithWool(Player player, WandSession session, WoolCheck check) {
        // ArenaManagerでアリーナ作成
        int id = plugin.getArenaManager().createArena(
            session.arenaName, session.gameType,
            player.getWorld().getName(),
            check.redLocations, check.blueLocations
        );
        player.sendMessage(plugin.getLangManager().prefixed("arena.created",
            "name", session.arenaName, "id", String.valueOf(id)));
        player.sendMessage(plugin.getLangManager().getPrefix() +
            "§7赤スポーン: §c" + check.redLocations.size() + "箇所  §7青スポーン: §9" + check.blueLocations.size() + "箇所");
    }

    private void createArena(Player player, WandSession session, Location center,
                              List<Location> redSpawns, List<Location> blueSpawns) {
        int id = plugin.getArenaManager().createArena(
            session.arenaName, session.gameType,
            player.getWorld().getName(),
            redSpawns != null ? redSpawns : List.of(),
            blueSpawns != null ? blueSpawns : List.of()
        );
        player.sendMessage(plugin.getLangManager().prefixed("arena.created",
            "name", session.arenaName, "id", String.valueOf(id)));
    }

    /** 範囲内の赤・青ウールをスキャン */
    private WoolCheck scanForWool(Location pos1, Location pos2) {
        WoolCheck check = new WoolCheck();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        World world = pos1.getWorld();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.RED_WOOL) {
                        check.hasRed = true;
                        check.redLocations.add(block.getLocation().add(0.5, 1, 0.5));
                    } else if (block.getType() == Material.BLUE_WOOL) {
                        check.hasBlue = true;
                        check.blueLocations.add(block.getLocation().add(0.5, 1, 0.5));
                    }
                }
            }
        }
        return check;
    }

    private boolean isTeamMode(String gameType) {
        return switch (gameType.toLowerCase()) {
            case "tdm", "heist", "domination" -> true;
            default -> false;
        };
    }

    private String formatLoc(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    public Location[] getSelection(UUID uuid) { return selections.get(uuid); }

        private static final class WandSession {
    private final String arenaName;
    private final String gameType;
    WandSession(String arenaName, String gameType) {
        this.arenaName = arenaName;
        this.gameType = gameType;
    }
    public String arenaName() { return arenaName; }
    public String gameType() { return gameType; }
    }

    private static class WoolCheck {
        boolean hasRed = false, hasBlue = false;
        List<Location> redLocations = new ArrayList<>();
        List<Location> blueLocations = new ArrayList<>();
    }
}
