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
            file = new File(plugin.getDataFolder(), "lang/ja.yml");
            if (!file.exists()) plugin.saveResource("lang/ja.yml", false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * キーからメッセージを取得し、プレースホルダーを置換して返す。
     * &カラーコードは colorize() で §に変換される。
     */
    public String get(String key, String... placeholders) {
        String msg = lang.getString(key, "&c[Missing: " + key + "]");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return colorize(msg);
    }

    /** prefix付きで取得 */
    public String prefixed(String key, String... placeholders) {
        return colorize(getPrefix()) + get(key, placeholders);
    }

    /** Componentとして取得（後方互換） */
    public String component(String key, String... placeholders) {
        return get(key, placeholders);
    }

    /** プレイヤーに直接送信（prefix付き） */
    public void send(Player player, String key, String... placeholders) {
        player.sendMessage(prefixed(key, placeholders));
    }

    /** アクションバーで送信 */
    public void sendActionBar(Player player, String key, String... placeholders) {
        VersionAdapter.sendActionBar(player, get(key, placeholders));
    }

    public String getPrefix() {
        return colorize(lang.getString("prefix", "&f&l[&6&lWarForgeCore&f&l] &r"));
    }

    /**
     * &カラーコードを §に変換する。
     *
     * 修正点: 旧実装は text.replace("&", "§") で全置換していたため、
     * チャットメッセージ内の & 記号なども誤変換されていた。
     * 文字単位でチェックし、有効なカラーコード文字の直前の & のみ変換する。
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";
        char[] chars = text.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length && isColorCode(chars[i + 1])) {
                sb.append('§');
                sb.append(chars[++i]);
            } else {
                sb.append(chars[i]);
            }
        }
        return sb.toString();
    }

    private static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'k' && c <= 'o')
                || c == 'r'
                || (c >= 'A' && c <= 'F')
                || (c >= 'K' && c <= 'O')
                || c == 'R';
    }

    public String getCurrentLocale() { return currentLocale; }
}
