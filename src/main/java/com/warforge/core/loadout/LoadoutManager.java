package com.warforge.core.loadout;
import com.warforge.core.WarforgeCore;
import com.warforge.core.compat.VersionAdapter;
import com.warforge.core.util.Messages;
import org.bukkit.entity.Player;
import java.sql.*; import java.util.*;
public class LoadoutManager {
    private final WarforgeCore plugin;
    private static final int MAX_SLOTS=3;
    private final Map<UUID,Map<Integer,String>> cache=new HashMap<>();
    public LoadoutManager(WarforgeCore plugin) { this.plugin=plugin; }
    public boolean save(Player player, int slot, String name) {
        if(slot<1||slot>MAX_SLOTS){player.sendMessage(Messages.INSTANCE.prefixed("&cスロットは1〜"+MAX_SLOTS+"を指定してください。"));return false;}
        UUID uuid=player.getUniqueId();
        cache.computeIfAbsent(uuid,k->new HashMap<>()).put(slot,name);
        saveToDb(uuid,slot,name);
        player.sendMessage(Messages.INSTANCE.prefixed("&aロードアウト &f"+slot+" &7["+name+"] &aを保存しました！"));
        return true;
    }
    public String getLoadoutName(UUID uuid, int slot) { Map<Integer,String> slots=cache.computeIfAbsent(uuid,k->loadFromDb(uuid)); return slots.get(slot); }
    public void list(Player player) {
        UUID uuid=player.getUniqueId();
        Map<Integer,String> slots=cache.computeIfAbsent(uuid,k->loadFromDb(uuid));
        player.sendMessage(Messages.INSTANCE.prefixed("&7--- ロードアウト一覧 ---"));
        for (int i = 1; i <= MAX_SLOTS; i++) {String n = slots.get(i);player.sendMessage(VersionAdapter.color("&7スロット " + i + ": " + ((n == null || n.isBlank()) ? "&8(未設定)" : "&f" + n)));}
    }
    private void saveToDb(UUID uuid, int slot, String name) {
        try(Connection conn=plugin.getDatabaseManager().getConnection()){
            PreparedStatement del=conn.prepareStatement("DELETE FROM wf_loadouts WHERE uuid=? AND slot=?");
            del.setString(1,uuid.toString()); del.setInt(2,slot); del.executeUpdate();
            PreparedStatement stmt=conn.prepareStatement("INSERT INTO wf_loadouts (uuid,slot,name,gun_ids) VALUES (?,?,?,?)");
            stmt.setString(1,uuid.toString()); stmt.setInt(2,slot); stmt.setString(3,name); stmt.setString(4,""); stmt.execute();
        } catch(SQLException e){plugin.getLogger().warning("ロードアウト保存失敗: "+e.getMessage());}
    }
    private Map<Integer,String> loadFromDb(UUID uuid) {
        Map<Integer,String> result=new HashMap<>();
        try(Connection conn=plugin.getDatabaseManager().getConnection()){
            PreparedStatement stmt=conn.prepareStatement("SELECT slot,name FROM wf_loadouts WHERE uuid=?");
            stmt.setString(1,uuid.toString()); ResultSet rs=stmt.executeQuery();
            while(rs.next()) result.put(rs.getInt("slot"),rs.getString("name"));
        } catch(SQLException e){plugin.getLogger().warning("ロードアウト読み込み失敗: "+e.getMessage());}
        return result;
    }
    public void clearCache(UUID uuid) { cache.remove(uuid); }
}
