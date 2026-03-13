package com.warforge.core.admin;

import com.warforge.core.WarforgeCore;
import com.warforge.core.arena.Arena;
import com.warforge.core.game.GameMode;
import com.warforge.core.game.lobby.GameLobby;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AdminGUI implements Listener {

    private final WarforgeCore plugin;
    private static final String TITLE = "§4§l⚙ Warforge 管理パネル";

    public AdminGUI(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin) {
        Collection<Arena> arenas = plugin.getArenaManager().getAllArenas();
        int rows = Math.max(3, (int) Math.ceil(((double)arenas.size() + 9) / 9.0) + 1);
        Inventory inv = Bukkit.createInventory(null, Math.min(rows * 9, 54), TITLE);

        // ヘッダー情報
        inv.setItem(0, buildItem(Material.PAPER, "&f&lサーバー状況", List.of(
            "&7オンライン: &f" + Bukkit.getOnlinePlayers().size() + "人",
            "&7アクティブ試合: &f" + plugin.getGameManager().getActiveGames().size()
        )));

        // リロードボタン
        inv.setItem(1, buildItem(Material.REPEATER, "&a設定リロード", List.of("&7クリックで全設定を再読込")));
        // テストモードボタン
        inv.setItem(2, buildItem(Material.COMMAND_BLOCK, "&eテストモード", List.of("&7アリーナを単独でテスト")));

        // アリーナ一覧（スロット9〜）
        int slot = 9;
        for (Arena arena : arenas) {
            if (slot >= 54) break;
            boolean inGame = plugin.getGameManager().getActiveGames().containsKey(arena.getId());
            GameLobby lobby = plugin.getGameManager().getLobbies().get(arena.getId());
            int waiting = lobby != null ? lobby.getWaitingPlayers().size() : 0;

            Material mat = inGame ? Material.RED_WOOL :
                (arena.getEnabled() ? Material.GREEN_WOOL : Material.GRAY_WOOL);
            String status = inGame ? "&c試合中" : (arena.getEnabled() ? "&a有効" : "&7無効");

            List<String> lore = new ArrayList<>(List.of(
                "&7ID: &f" + arena.getId(),
                "&7モード: &f" + arena.getMode().toUpperCase(),
                "&7状態: " + status,
                "&7ロビー人数: &f" + waiting + "/" + arena.getMaxPlayers(),
                "",
                inGame ? "&c[左クリック] 試合を強制終了" : "&a[左クリック] 試合開始",
                "&e[右クリック] 詳細設定"
            ));
            inv.setItem(slot++, buildItemWithId(mat, arena.getName(), lore, arena.getId()));
        }

        admin.openInventory(inv);
    }

    @EventHandler
    public void onAdminClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        if (!event.getView().getTitle().equals(TITLE)) return;
        if (!admin.hasPermission("warforge.admin")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
            ? VersionAdapter.getDisplayName(clicked.getItemMeta()) : "";

        if (name.contains("設定リロード")) {
            plugin.getConfigManager().reload();
            admin.sendMessage("§a全設定をリロードしました！");
            open(admin);
            return;
        }

        // アリーナアクション
        if (clicked.hasItemMeta()) {
            Integer arenaId = clicked.getItemMeta().getPersistentDataContainer()
                .get(new org.bukkit.NamespacedKey(plugin, "admin_arena_id"),
                    org.bukkit.persistence.PersistentDataType.INTEGER);
            if (arenaId == null) return;

            Arena arena = plugin.getArenaManager().getArena(arenaId);
            if (arena == null) return;

            if (event.isLeftClick()) {
                GameMode game = plugin.getGameManager().getActiveGames().get(arenaId);
                if (game != null) {
                    game.end();
                    plugin.getGameManager().onGameEnd(arenaId);
                    admin.sendMessage("§c試合を強制終了しました: " + arena.getName());
                } else {
                    admin.sendMessage("§c試合は進行していません。/join " + arenaId + " で参加してください。");
                }
            } else if (event.isRightClick()) {
                admin.closeInventory();
                admin.sendMessage("§7アリーナ設定: /arena info " + arenaId);
            }
            open(admin);
        }
    }

    private ItemStack buildItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        VersionAdapter.setDisplayName(meta, name);
        VersionAdapter.setLore(meta, lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildItemWithId(Material mat, String name, List<String> lore, int arenaId) {
        ItemStack item = buildItem(mat, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "admin_arena_id"),
            org.bukkit.persistence.PersistentDataType.INTEGER, arenaId
        );
        item.setItemMeta(meta);
        return item;
    }
}
