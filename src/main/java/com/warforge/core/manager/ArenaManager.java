package com.warforge.core.manager;

import com.warforge.core.WarforgeCore;
import com.warforge.core.arena.Arena;
import com.warforge.core.arena.CaptureOwner;
import com.warforge.core.arena.CapturePoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ArenaManager {

    private final WarforgeCore plugin;
    private final Map<Integer, Arena> arenas = new HashMap<>();
    private final File arenasFile;
    private YamlConfiguration arenasConfig;
    private int nextId = 1;

    public ArenaManager(WarforgeCore plugin) {
        this.plugin = plugin;
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadArenas();
    }

    public Arena createArena(String name, String mode, Player creator) {
        int id = nextId++;
        Arena arena = new Arena(id, name,
            creator.getWorld().getName(), mode,
            16, 2, true,
            new ArrayList<>(), null,
            new ArrayList<>(), new ArrayList<>()
        );
        arenas.put(id, arena);
        saveArenas();
        return arena;
    }

    public boolean deleteArena(int id) {
        if (!arenas.containsKey(id)) return false;
        arenas.remove(id);
        saveArenas();
        return true;
    }

    public void addSpawnPoint(int arenaId, Location loc) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.getSpawnPoints().add(loc);
        saveArenas();
    }

    public void setLobbySpawn(int arenaId, Location loc) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.setLobbySpawn(loc);
        saveArenas();
    }

    public Location getRandomSpawn(int arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null || arena.getSpawnPoints().isEmpty()) return null;
        List<Location> spawns = arena.getSpawnPoints();
        return spawns.get(new Random().nextInt(spawns.size()));
    }

    public void addCapturePoint(int arenaId, String pointName, Location loc, double radius) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        int pointId = arena.getCapturePoints().size() + 1;
        arena.getCapturePoints().add(new CapturePoint(pointId, pointName, loc, radius,
            CaptureOwner.NEUTRAL, 0.0));
        saveArenas();
    }

    public void addGoldSpawn(int arenaId, Location loc) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.getGoldSpawnPoints().add(loc);
        saveArenas();
    }

    
    /** ワンドセットアップから呼ばれるアリーナ作成 */
    public int createArena(String name, String mode, String worldName,
                           List<Location> redSpawns, List<Location> blueSpawns) {
        int id = generateId();
        Arena arena = new Arena(
            id, name, worldName, mode, 16, 2, false,
            new java.util.ArrayList<>(), null,
            new java.util.ArrayList<>(redSpawns),
            new java.util.ArrayList<>(blueSpawns),
            new java.util.ArrayList<>(),
            new java.util.ArrayList<>(),
            null, null, 100, 5
        );
        arenas.put(id, arena);
        saveArenas();
        plugin.getLogger().info("アリーナ作成: " + name + " (ID:" + id + ") mode:" + mode);
        return id;
    }

    private int generateId() {
        return arenas.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }


    public Arena getArena(int id) { return arenas.get(id); }

    public Optional<Arena> findByName(String name) {
        return arenas.values().stream()
            .filter(a -> a.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    public Collection<Arena> getAllArenas() { return arenas.values(); }

    private void loadArenas() {
        if (!arenasFile.exists()) { saveArenas(); return; }
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        if (!arenasConfig.contains("arenas")) return;

        for (String idStr : arenasConfig.getConfigurationSection("arenas").getKeys(false)) {
            int id = Integer.parseInt(idStr);
            String path = "arenas." + id + ".";
            String worldName = arenasConfig.getString(path + "world");
            World world = Bukkit.getWorld(worldName);

            Arena arena = new Arena(id,
                arenasConfig.getString(path + "name"), worldName,
                arenasConfig.getString(path + "mode"),
                arenasConfig.getInt(path + "max-players", 16),
                arenasConfig.getInt(path + "min-players", 2),
                arenasConfig.getBoolean(path + "enabled", true),
                new ArrayList<>(), null, new ArrayList<>(), new ArrayList<>()
            );

            if (arenasConfig.contains(path + "spawns") && world != null) {
                for (String key : arenasConfig.getConfigurationSection(path + "spawns").getKeys(false)) {
                    String sp = path + "spawns." + key + ".";
                    arena.getSpawnPoints().add(new Location(world,
                        arenasConfig.getDouble(sp + "x"), arenasConfig.getDouble(sp + "y"),
                        arenasConfig.getDouble(sp + "z"), (float) arenasConfig.getDouble(sp + "yaw"),
                        (float) arenasConfig.getDouble(sp + "pitch")));
                }
            }
            arenas.put(id, arena);
            if (id >= nextId) nextId = id + 1;
        }
        plugin.getLogger().info("アリーナ読み込み完了: " + arenas.size() + "件");
    }

    private void saveArenas() {
        arenasConfig = new YamlConfiguration();
        arenas.forEach((id, arena) -> {
            String path = "arenas." + id + ".";
            arenasConfig.set(path + "name", arena.getName());
            arenasConfig.set(path + "world", arena.getWorld());
            arenasConfig.set(path + "mode", arena.getMode());
            arenasConfig.set(path + "max-players", arena.getMaxPlayers());
            arenasConfig.set(path + "min-players", arena.getMinPlayers());
            arenasConfig.set(path + "enabled", arena.getEnabled());
            for (int i = 0; i < arena.getSpawnPoints().size(); i++) {
                Location loc = arena.getSpawnPoints().get(i);
                String sp = path + "spawns." + i + ".";
                arenasConfig.set(sp + "x", loc.getX()); arenasConfig.set(sp + "y", loc.getY());
                arenasConfig.set(sp + "z", loc.getZ()); arenasConfig.set(sp + "yaw", (double) loc.getYaw());
                arenasConfig.set(sp + "pitch", (double) loc.getPitch());
            }
        });
        try { arenasConfig.save(arenasFile); }
        catch (IOException e) { plugin.getLogger().severe("アリーナ保存失敗: " + e.getMessage()); }
    }

    // ─── 追記メソッド ───
    // (ArenaManagerクラスの末尾の
    public void addRedSpawnPoint(int arenaId, Location loc) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.getRedSpawnPoints().add(loc);
        saveArenas();
    }

    public void addBlueSpawnPoint(int arenaId, Location loc) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.getBlueSpawnPoints().add(loc);
        saveArenas();
    }

    public void setRedNexus(int arenaId, Location loc) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.setRedNexus(loc);
        saveArenas();
    }

    public void setBlueNexus(int arenaId, Location loc) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.setBlueNexus(loc);
        saveArenas();
    }

    public void setHeistGoldSettings(int arenaId, int winGold, int perMine) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) return;
        arena.setHeistWinGold(winGold);
        arena.setHeistGoldPerMine(perMine);
        saveArenas();
    }

    public Location getRandomRedSpawn(int arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null || arena.getRedSpawnPoints().isEmpty()) return getRandomSpawn(arenaId);
        List<Location> spawns = arena.getRedSpawnPoints();
        return spawns.get(new Random().nextInt(spawns.size()));
    }

    public Location getRandomBlueSpawn(int arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null || arena.getBlueSpawnPoints().isEmpty()) return getRandomSpawn(arenaId);
        List<Location> spawns = arena.getBlueSpawnPoints();
        return spawns.get(new Random().nextInt(spawns.size()));
    }

}

