package com.warforge.core.loadout;

import com.warforge.core.WarforgeCore;
import com.warforge.core.gun.GunData;
import com.warforge.core.util.Messages;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;

public class LoadoutManager {

    private final WarforgeCore plugin;
    private static final int MAX_SLOTS = 3; // 最大ロードアウト数

    // cache: UUID → (slot → gunIds)
    private final Map<UUID, Map<Integer, List<String>>> cache = new HashMap<>();

    public LoadoutManager(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    /** 現在のインベントリからロードアウトを保存 */
    public boolean save(Player player, int slot, String name) {
        if (slot < 1 || slot > MAX_SLOTS) {
            player.sendMessage(Messages.INSTANCE.prefixed("&cスロットは1〜" + MAX_SLOTS + "を指定してください。"));
            return false;
        }

        // インベントリ内の全銃IDを収集
        List<String> gunIds = new ArrayList<>();
        for (var item : player.getInventory().getContents()) {
            String gunId = plugin.getGunItemManager().getGunId(item);
            if (gunId != null) gunIds.add(gunId);
        }

        if (gunIds.isEmpty()) {
            player.sendMessage(Messages.INSTANCE.prefixed("&cインベントリに銃がありません。"));
            return false;
        }

        UUID uuid = player.getUniqueId();
        cache.computeIfAbsent(uuid, k -> new HashMap<>()).put(slot, gunIds);
        saveToDb(uuid, slot, name, gunIds);

        player.sendMessage(Messages.INSTANCE.prefixed(
            "&aロードアウト &f" + slot + " &7[" + name + "] &aを保存しました！ &7(" +
            String.join(", ", gunIds) + ")"
        ));
        return true;
    }

    /** ロードアウトをインベントリに展開 */
    public boolean load(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        Map<Integer, List<String>> slots = cache.computeIfAbsent(uuid, k -> loadFromDb(uuid));
        List<String> gunIds = slots.get(slot);

        if (gunIds == null || gunIds.isEmpty()) {
            player.sendMessage(Messages.INSTANCE.prefixed("&cスロット " + slot + " は未保存です。"));
            return false;
        }

        // 既存の銃を全部外す
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            String gid = plugin.getGunItemManager().getGunId(player.getInventory().getItem(i));
            if (gid != null) player.getInventory().setItem(i, null);
        }

        // 銃を配布
        for (String gunId : gunIds) {
            var item = plugin.getGunItemManager().createGunItem(gunId);
            if (item != null) {
                player.getInventory().addItem(item);
                GunData gun = plugin.getGunManager().getGun(gunId);
                if (gun != null) plugin.getBulletHandler().initAmmo(uuid, gun);
            }
        }

        player.sendMessage(Messages.INSTANCE.prefixed(
            "&aロードアウト &f" + slot + " &aを装備しました！"
        ));
        return true;
    }

    /** ロードアウト一覧を表示 */
    public void list(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Integer, List<String>> slots = cache.computeIfAbsent(uuid, k -> loadFromDb(uuid));

        player.sendMessage(Messages.INSTANCE.prefixed("&7--- ロードアウト一覧 ---"));
        for (int i = 1; i <= MAX_SLOTS; i++) {
            List<String> guns = slots.get(i);
            String content = (guns == null || guns.isEmpty())
                ? "&8(未設定)" : "&f" + String.join(", ", guns);
            player.sendMessage("&7スロット " + i + ": " + content);
        }
        player.sendMessage("&7/loadout save <1-" + MAX_SLOTS + "> <名前>  /loadout load <1-" + MAX_SLOTS + ">");
    }

    private void saveToDb(UUID uuid, int slot, String name, List<String> gunIds) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.prepareStatement(
                "DELETE FROM wf_loadouts WHERE uuid=? AND slot=?"
            ).executeUpdate();
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO wf_loadouts (uuid, slot, name, gun_ids) VALUES (?,?,?,?)"
            );
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, slot);
            stmt.setString(3, name);
            stmt.setString(4, String.join(",", gunIds));
            stmt.execute();
        } catch (SQLException e) {
            plugin.getLogger().warning("ロードアウト保存失敗: " + e.getMessage());
        }
    }

    private Map<Integer, List<String>> loadFromDb(UUID uuid) {
        Map<Integer, List<String>> result = new HashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT slot, gun_ids FROM wf_loadouts WHERE uuid=?"
            );
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int slot = rs.getInt("slot");
                String[] ids = rs.getString("gun_ids").split(",");
                result.put(slot, new ArrayList<>(Arrays.asList(ids)));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("ロードアウト読み込み失敗: " + e.getMessage());
        }
        return result;
    }

    public void clearCache(UUID uuid) { cache.remove(uuid); }
}
