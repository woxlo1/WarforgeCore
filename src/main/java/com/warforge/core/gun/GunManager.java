package com.warforge.core.gun;

import com.warforge.core.WarforgeCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GunManager {

    private final WarforgeCore plugin;
    private final Map<String, GunData> guns = new HashMap<>();

    public GunManager(WarforgeCore plugin) {
        this.plugin = plugin;
        loadGuns();
    }

    public void loadGuns() {
        guns.clear();
        File gunsDir = new File(plugin.getDataFolder(), "guns");
        if (!gunsDir.exists()) gunsDir.mkdirs();

        File defaultFile = new File(gunsDir, "default_guns.yml");
        if (!defaultFile.exists()) plugin.saveResource("guns/default_guns.yml", false);

        // gunsディレクトリ内の全YMLを読み込む
        File[] files = gunsDir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String id : config.getKeys(false)) {
                String p = id + ".";
                GunData gun = new GunData(
                    id,
                    config.getString(p + "display-name", id),
                    config.getDouble(p + "damage", 10.0),
                    config.getDouble(p + "headshot-multiplier", 2.0),
                    config.getDouble(p + "body-multiplier", 1.0),
                    config.getDouble(p + "leg-multiplier", 0.75),
                    config.getDouble(p + "armor-penetration", 0.0),
                    config.getLong(p + "fire-rate", 200L),
                    FireMode.valueOf(config.getString(p + "fire-mode", "AUTO")),
                    config.getInt(p + "burst-count", 3),
                    config.getInt(p + "magazine-size", 30),
                    config.getInt(p + "reserve-ammo", 90),
                    config.getDouble(p + "bullet-speed", 5.0),
                    config.getDouble(p + "range", 50.0),
                    config.getDouble(p + "drop-off", 0.1),
                    config.getInt(p + "bullet-count", 1),
                    config.getDouble(p + "spread-angle", 0.0),
                    config.getDouble(p + "recoil", 0.5),
                    config.getDouble(p + "recoil-vertical", 0.3),
                    config.getDouble(p + "recoil-horizontal", 0.15),
                    config.getDouble(p + "recoil-recovery", 0.8),
                    config.getLong(p + "reload-time", 2000L),
                    config.getLong(p + "tactical-reload-time", 1500L),
                    config.getDouble(p + "ads-speed-multiplier", 0.7),
                    config.getDouble(p + "ads-accuracy", 0.9),
                    config.getString(p + "shoot-sound", "ENTITY_GENERIC_EXPLODE"),
                    (float) config.getDouble(p + "shoot-sound-volume", 0.3),
                    (float) config.getDouble(p + "shoot-sound-pitch", 2.0),
                    config.getBoolean(p + "suppressor", false),
                    config.getDouble(p + "price", 0.0),
                    GunRarity.valueOf(config.getString(p + "rarity", "COMMON")),
                    GunType.valueOf(config.getString(p + "type", "RIFLE"))
                );
                guns.put(id, gun);
            }
        }
        plugin.getLogger().info("銃データ読み込み完了: " + guns.size() + "種類");
    }

    public GunData getGun(String id) { return guns.get(id); }
    public Map<String, GunData> getAllGuns() { return guns; }
}
