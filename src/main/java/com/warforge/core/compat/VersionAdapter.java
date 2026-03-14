package com.warforge.core.compat;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class VersionAdapter {

    private static final int MAJOR, MINOR, PATCH;
    public static final boolean HAS_ADVENTURE;
    public static final boolean HAS_ADVENTURE_ITEM;

    static {
        String raw = Bukkit.getServer().getBukkitVersion();
        String[] parts = raw.split("-")[0].split("\\.");
        MAJOR = parseInt(parts, 0, 1);
        MINOR = parseInt(parts, 1, 16);
        PATCH = parseInt(parts, 2, 5);
        HAS_ADVENTURE      = MINOR >= 17;
        HAS_ADVENTURE_ITEM = MINOR >= 17;
    }

    private static int parseInt(String[] arr, int idx, int def) {
        try { return idx < arr.length ? Integer.parseInt(arr[idx]) : def; }
        catch (NumberFormatException e) { return def; }
    }

    public static int getMinor() { return MINOR; }

    /**
     * &カラーコードを §に変換する。
     * 有効なコード文字 (0-9, a-f, k-o, r) の直前の & のみ変換する。
     * text.replace("&","§") は & を全て置換するバグがあったため修正。
     */
    public static String color(String text) {
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

    public static String strip(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(color(message));
    }

    @SuppressWarnings("deprecation")
    public static void sendTitle(Player player, String title, String subtitle, int in, int stay, int out) {
        if (HAS_ADVENTURE) {
            try {
                Class<?> titleClass = Class.forName("net.kyori.adventure.title.Title");
                Class<?> compClass  = Class.forName("net.kyori.adventure.text.Component");
                Class<?> timesClass = Class.forName("net.kyori.adventure.title.Title$Times");
                Class<?> durClass   = Class.forName("java.time.Duration");
                Class<?> lcsClass   = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
                Method lcsInstance  = lcsClass.getMethod("legacySection");
                Object lcs          = lcsInstance.invoke(null);
                Method deserialize  = lcsClass.getMethod("deserialize", String.class);
                Object titleComp    = deserialize.invoke(lcs, color(title));
                Object subtitleComp = deserialize.invoke(lcs, color(subtitle));
                Method ofMillis     = durClass.getMethod("ofMillis", long.class);
                Object dIn   = ofMillis.invoke(null, (long)(in   * 50));
                Object dStay = ofMillis.invoke(null, (long)(stay * 50));
                Object dOut  = ofMillis.invoke(null, (long)(out  * 50));
                Method timesOf  = timesClass.getMethod("times", durClass, durClass, durClass);
                Object times    = timesOf.invoke(null, dIn, dStay, dOut);
                Method titleOf  = titleClass.getMethod("title", compClass, compClass, timesClass);
                Object titleObj = titleOf.invoke(null, titleComp, subtitleComp, times);
                Method showTitle = player.getClass().getMethod("showTitle", titleClass);
                showTitle.invoke(player, titleObj);
                return;
            } catch (Exception ignored) {}
        }
        player.sendTitle(color(title), color(subtitle), in, stay, out);
    }

    @SuppressWarnings("deprecation")
    public static void sendActionBar(Player player, String message) {
        if (HAS_ADVENTURE) {
            try {
                Class<?> compClass = Class.forName("net.kyori.adventure.text.Component");
                Class<?> lcsClass  = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
                Method inst = lcsClass.getMethod("legacySection");
                Object lcs  = inst.invoke(null);
                Method des  = lcsClass.getMethod("deserialize", String.class);
                Object comp = des.invoke(lcs, color(message));
                Method sab  = player.getClass().getMethod("sendActionBar", compClass);
                sab.invoke(player, comp);
                return;
            } catch (Exception ignored) {}
        }
        try {
            Class<?> cmtClass  = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Class<?> compClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object type = cmtClass.getField("ACTION_BAR").get(null);
            Object comp = compClass.getConstructor(String.class).newInstance(color(message));
            Method sendMsg = player.spigot().getClass().getMethod("sendMessage", cmtClass, compClass.getSuperclass());
            sendMsg.invoke(player.spigot(), type, comp);
        } catch (Exception e2) {
            player.sendMessage(color(message));
        }
    }

    @SuppressWarnings("deprecation")
    public static void setDisplayName(ItemMeta meta, String name) {
        if (HAS_ADVENTURE_ITEM) {
            try {
                Class<?> lcsClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
                Method inst = lcsClass.getMethod("legacySection");
                Object lcs  = inst.invoke(null);
                Method des  = lcsClass.getMethod("deserialize", String.class);
                Object comp = des.invoke(lcs, color(name));
                Method dn   = meta.getClass().getMethod("displayName", Class.forName("net.kyori.adventure.text.Component"));
                dn.invoke(meta, comp);
                return;
            } catch (Exception ignored) {}
        }
        meta.setDisplayName(color(name));
    }

    @SuppressWarnings("deprecation")
    public static void setLore(ItemMeta meta, List<String> lines) {
        if (HAS_ADVENTURE_ITEM) {
            try {
                Class<?> lcsClass  = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
                Class<?> compClass = Class.forName("net.kyori.adventure.text.Component");
                Method inst = lcsClass.getMethod("legacySection");
                Object lcs  = inst.invoke(null);
                Method des  = lcsClass.getMethod("deserialize", String.class);
                List<Object> comps = new ArrayList<>();
                for (String line : lines) comps.add(des.invoke(lcs, color(line)));
                meta.getClass().getMethod("lore", List.class).invoke(meta, comps);
                return;
            } catch (Exception ignored) {}
        }
        List<String> colored = new ArrayList<>();
        for (String l : lines) colored.add(color(l));
        meta.setLore(colored);
    }

    @SuppressWarnings("deprecation")
    public static String getDisplayName(ItemMeta meta) {
        if (!meta.hasDisplayName()) return "";
        if (HAS_ADVENTURE_ITEM) {
            try {
                Method dn = meta.getClass().getMethod("displayName");
                Object comp = dn.invoke(meta);
                Class<?> plainClass = Class.forName("net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer");
                Method plainInst = plainClass.getMethod("plainText");
                Object serializer = plainInst.invoke(null);
                Method ser = plainClass.getMethod("serialize", Class.forName("net.kyori.adventure.text.Component"));
                return (String) ser.invoke(serializer, comp);
            } catch (Exception ignored) {}
        }
        return meta.getDisplayName();
    }

    public static BossBar createBossBar(String title, BarColor color, BarStyle style) {
        return Bukkit.createBossBar(color(title), color, style);
    }

    public static void setBossBarTitle(BossBar bar, String title) {
        bar.setTitle(color(title));
    }

    @SuppressWarnings({"deprecation", "removal"})
    public static Objective registerObjective(Scoreboard board, String name, String displayName) {
        Objective obj;
        try {
            obj = board.registerNewObjective(name, "dummy", color(displayName));
        } catch (Exception e) {
            obj = board.registerNewObjective(name, "dummy");
            obj.setDisplayName(color(displayName));
        }
        return obj;
    }

    public static org.bukkit.Particle getParticle(String modern, String legacy) {
        try { return org.bukkit.Particle.valueOf(modern); }
        catch (IllegalArgumentException e) {
            try { return org.bukkit.Particle.valueOf(legacy); }
            catch (IllegalArgumentException e2) { return org.bukkit.Particle.FLAME; }
        }
    }

    public static org.bukkit.Sound getSound(String... candidates) {
        for (String name : candidates) {
            try { return org.bukkit.Sound.valueOf(name); }
            catch (IllegalArgumentException ignored) {}
        }
        return org.bukkit.Sound.UI_BUTTON_CLICK;
    }

    @SuppressWarnings("deprecation")
    public static org.bukkit.entity.ArmorStand spawnArmorStand(org.bukkit.Location loc, String text) {
        org.bukkit.entity.ArmorStand stand = loc.getWorld().spawn(loc, org.bukkit.entity.ArmorStand.class);
        stand.setVisible(false); stand.setGravity(false); stand.setSmall(true);
        stand.setCustomNameVisible(true); stand.setCustomName(color(text));
        return stand;
    }

    public static org.bukkit.inventory.Inventory createInventory(int size, String title) {
        return Bukkit.createInventory(null, size, color(title));
    }
}
