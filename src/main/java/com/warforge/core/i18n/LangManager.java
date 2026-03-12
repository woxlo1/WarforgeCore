package com.warforge.core.i18n;

import com.warforge.core.WarforgeCore;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LangManager {

    private final WarforgeCore plugin;
    private YamlConfiguration lang;
    private String currentLocale;

    // プレイヤーごとの言語設定（将来的な個人設定用）
    private final Map<String, String> playerLocales = new HashMap<>();

    public LangManager(WarforgeCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        currentLocale = plugin.getConfig().getString("settings.language", "ja");
        lang = loadLang(currentLocale);
    }

    private YamlConfiguration loadLang(String locale) {
        File file = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");
        if (!file.exists()) {
            plugin.saveResource("lang/" + locale + ".yml", false);
        }
        if (!file.exists()) {
            // フォールバック: ja
            file = new File(plugin.getDataFolder(), "lang/ja.yml");
            if (!file.exists()) plugin.saveResource("lang/ja.yml", false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * キーからメッセージ取得（プレースホルダー対応）
     * 例: get("kill.message", "victim", "Steve", "killer", "Alex", "suffix", "")
     */
    public String get(String key, String... placeholders) {
        String msg = lang.getString(key, "&c[Missing: " + key + "]");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return msg;
    }

    /** prefixを付けて取得 */
    public String prefixed(String key, String... placeholders) {
        return colorize(getPrefix() + get(key, placeholders));
    }

    /** Componentとして取得 */
    public String component(String key, String... placeholders) {
        return VersionAdapter.color(get(key, placeholders));
    }

    /** prefixed Component */
    public String prefixedComponent(String key, String... placeholders) {
        return VersionAdapter.color(prefixed(key, placeholders));
    }

    /** プレイヤーに直接送信（prefix付き） */
    public void send(Player player, String key, String... placeholders) {
        player.sendMessage(VersionAdapter.color(prefixed(key, placeholders)));
    }

    /** アクションバーで送信 */
    public void sendActionBar(Player player, String key, String... placeholders) {
        VersionAdapter.sendActionBar(player, get(key, placeholders));
    }

    public String getPrefix() {
        return lang.getString("prefix", "&f&l[&6&lWarForgeCore&f&l] &r");
    }

    public String colorize(String text) {
        return text.replace("&", "§");
    }

    public String getCurrentLocale() { return currentLocale; }
}
