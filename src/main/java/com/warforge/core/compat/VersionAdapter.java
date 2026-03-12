package com.warforge.core.compat;

import com.warforge.core.compat.VersionAdapter;
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
import java.util.logging.Logger;

/**
 * 1.16.5 〜 1.21 のAPI差異を吸収するアダプター。
 *
 * 基本方針:
 *   - テキストは常に §コードの文字列で渡す（Adventure Component は使わない）
 *   - Adventure が使えるバージョンでは Reflection 経由で呼ぶ
 *   - それ以外は Spigot の旧 API にフォールバック
 *
 * 対応バージョン:
 *   1.16.5, 1.17.x, 1.18.x, 1.19.x, 1.20.x, 1.21.x
 */
public final class VersionAdapter {

    // ── バージョン検出 ────────────────────────────────────────────
    private static final int MAJOR;   // 1
    private static final int MINOR;   // 16 / 17 / 18 …
    private static final int PATCH;   // 0 / 1 / 2 …

    /** Adventure の sendActionBar(Component) が使えるか（1.17+） */
    public static final boolean HAS_ADVENTURE;
    /** meta.displayName(Component) が使えるか（Paper 1.16.5+ ならあるが安全のため 1.17 基準） */
    public static final boolean HAS_ADVENTURE_ITEM;

    static {
        String raw = Bukkit.getServer().getBukkitVersion(); // e.g. "1.20.4-R0.1-SNAPSHOT"
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

    // ── § カラーコード変換 ──────────────────────────────────────
    /** &コードを§コードに変換 */
    public static String color(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    /** §コードを除去してプレーンテキストにする */
    public static String strip(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    // ── sendMessage ─────────────────────────────────────────────
    /** §コード付き文字列でプレイヤーにメッセージ送信 */
    public static void sendMessage(Player player, String message) {
        player.sendMessage(color(message));
    }

    // ── sendTitle ────────────────────────────────────────────────
    /**
     * タイトル表示。1.16〜1.21 対応。
     * @param title    §コード付きタイトル
     * @param subtitle §コード付きサブタイトル
     * @param in       フェードイン (ticks)
     * @param stay     表示時間 (ticks)
     * @param out      フェードアウト (ticks)
     */
    @SuppressWarnings("deprecation")
    public static void sendTitle(Player player, String title, String subtitle, int in, int stay, int out) {
        if (HAS_ADVENTURE) {
            // 1.17+ : Player#showTitle(Title) — Reflection 経由で Adventure を使う
            try {
                // net.kyori.adventure.title.Title
                Class<?> titleClass   = Class.forName("net.kyori.adventure.title.Title");
                Class<?> compClass    = Class.forName("net.kyori.adventure.text.Component");
                Class<?> timesClass   = Class.forName("net.kyori.adventure.title.Title$Times");
                Class<?> durClass     = Class.forName("java.time.Duration");
                Class<?> lcsClass     = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");

                // LegacyComponentSerializer.legacySection()
                Method lcsInstance = lcsClass.getMethod("legacySection");
                Object lcs = lcsInstance.invoke(null);
                Method deserialize = lcsClass.getMethod("deserialize", String.class);

                Object titleComp    = deserialize.invoke(lcs, color(title));
                Object subtitleComp = deserialize.invoke(lcs, color(subtitle));

                // Duration.ofMillis
                Method ofMillis = durClass.getMethod("ofMillis", long.class);
                Object dIn   = ofMillis.invoke(null, (long)(in   * 50));
                Object dStay = ofMillis.invoke(null, (long)(stay * 50));
                Object dOut  = ofMillis.invoke(null, (long)(out  * 50));

                Method timesOf = timesClass.getMethod("times", durClass, durClass, durClass);
                Object times = timesOf.invoke(null, dIn, dStay, dOut);

                Method titleOf = titleClass.getMethod("title", compClass, compClass, timesClass);
                Object titleObj = titleOf.invoke(null, titleComp, subtitleComp, times);

                Method showTitle = player.getClass().getMethod("showTitle", titleClass);
                showTitle.invoke(player, titleObj);
                return;
            } catch (Exception ignored) {}
        }
        // 1.16 fallback: deprecated sendTitle(String, String, int, int, int)
        player.sendTitle(color(title), color(subtitle), in, stay, out);
    }

    // ── sendActionBar ────────────────────────────────────────────
    @SuppressWarnings("deprecation")
    public static void sendActionBar(Player player, String message) {
        if (HAS_ADVENTURE) {
            try {
                Class<?> compClass = Class.forName("net.kyori.adventure.text.Component");
                Class<?> lcsClass  = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
                Method lcsInstance  = lcsClass.getMethod("legacySection");
                Object lcs = lcsInstance.invoke(null);
                Method deserialize  = lcsClass.getMethod("deserialize", String.class);
                Object comp = deserialize.invoke(lcs, color(message));
                Method sab = player.getClass().getMethod("sendActionBar", compClass);
                sab.invoke(player, comp);
                return;
            } catch (Exception ignored) {}
        }
        // 1.16 fallback: spigot().sendMessage with ChatMessageType
        try {
            Class<?> cmtClass   = Class.forName("net.md_5.bungee.api.ChatMessageType");
            Class<?> compClass  = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Object type = cmtClass.getField("ACTION_BAR").get(null);
            Object comp = compClass.getConstructor(String.class).newInstance(color(message));
            Method sendMsg = player.spigot().getClass().getMethod("sendMessage", cmtClass, compClass.getSuperclass());
            sendMsg.invoke(player.spigot(), type, comp);
        } catch (Exception e2) {
            // 最終フォールバック
            player.sendMessage(color(message));
        }
    }

    // ── ItemMeta displayName / lore ──────────────────────────────
    /**
     * §コード付き文字列でアイテム名を設定。
     * 1.17+ なら Adventure Component 経由、それ以外は deprecated setDisplayName。
     */
    @SuppressWarnings("deprecation")
    public static void setDisplayName(ItemMeta meta, String name) {
        if (HAS_ADVENTURE_ITEM) {
            try {
                Class<?> lcsClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
                Method inst = lcsClass.getMethod("legacySection");
                Object lcs  = inst.invoke(null);
                Method des  = lcsClass.getMethod("deserialize", String.class);
                Object comp = des.invoke(lcs, color(name));
                Method dn   = meta.getClass().getMethod("displayName",
                    Class.forName("net.kyori.adventure.text.Component"));
                dn.invoke(meta, comp);
                return;
            } catch (Exception ignored) {}
        }
        meta.setDisplayName(color(name));
    }

    /** §コード付き文字列リストでloreを設定 */
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
                for (String line : lines) {
                    comps.add(des.invoke(lcs, color(line)));
                }
                Method loreMeth = meta.getClass().getMethod("lore", List.class);
                loreMeth.invoke(meta, comps);
                return;
            } catch (Exception ignored) {}
        }
        // 旧API
        List<String> colored = new ArrayList<>();
        for (String l : lines) colored.add(color(l));
        meta.setLore(colored);
    }

    /** アイテムのdisplayNameをプレーン文字列で取得 */
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
                Method ser = plainClass.getMethod("serialize",
                    Class.forName("net.kyori.adventure.text.Component"));
                return (String) ser.invoke(serializer, comp);
            } catch (Exception ignored) {}
        }
        return meta.getDisplayName();
    }

    // ── BossBar ──────────────────────────────────────────────────
    /**
     * Bukkit の BossBar を作成（1.16〜1.21 は Bukkit.createBossBar で統一）。
     * Adventure BossBar ではなく Bukkit BossBar を使うことで全バージョン対応。
     */
    public static BossBar createBossBar(String title, BarColor color, BarStyle style) {
        return Bukkit.createBossBar(color(title), color, style);
    }

    public static void setBossBarTitle(BossBar bar, String title) {
        bar.setTitle(color(title));
    }

    // ── Scoreboard ───────────────────────────────────────────────
    /**
     * スコアボードのObjective表示名を§コード付き文字列で設定。
     * 1.16〜1.20.4: setDisplayName(String)
     * 1.21+: displayName(Component) ※Reflectionで対応
     */
    @SuppressWarnings({"deprecation", "removal"})
    public static Objective registerObjective(Scoreboard board, String name, String displayName) {
        Objective obj;
        try {
            // 1.17以降は Criteria クラス
            Class<?> criteriaClass = Class.forName("org.bukkit.scoreboard.Criteria");
            Method dummy = criteriaClass.getMethod("statistic",
                Class.forName("org.bukkit.Statistic"));
            // "dummy" criteriaがあれば使う
            obj = board.registerNewObjective(name, "dummy", color(displayName));
        } catch (Exception e) {
            try {
                obj = board.registerNewObjective(name, "dummy", color(displayName));
            } catch (Exception e2) {
                obj = board.registerNewObjective(name, "dummy");
                obj.setDisplayName(color(displayName));
            }
        }
        return obj;
    }

    // ── Particle ─────────────────────────────────────────────────
    /**
     * バージョン間でParticle名が変わっているものを吸収するユーティリティ。
     * 存在しない場合はフォールバックParticleを使う。
     */
    public static org.bukkit.Particle getParticle(String modern, String legacy) {
        try {
            return org.bukkit.Particle.valueOf(modern);
        } catch (IllegalArgumentException e) {
            try {
                return org.bukkit.Particle.valueOf(legacy);
            } catch (IllegalArgumentException e2) {
                return org.bukkit.Particle.FLAME; // 最終フォールバック
            }
        }
    }

    // ── Sound ────────────────────────────────────────────────────
    /**
     * バージョン間でSound名が変わっているものを吸収。
     */
    public static org.bukkit.Sound getSound(String... candidates) {
        for (String name : candidates) {
            try {
                return org.bukkit.Sound.valueOf(name);
            } catch (IllegalArgumentException ignored) {}
        }
        return org.bukkit.Sound.UI_BUTTON_CLICK;
    }

    // ── ArmorStand (ホログラム用) ────────────────────────────────
    @SuppressWarnings("deprecation")
    public static org.bukkit.entity.ArmorStand spawnArmorStand(org.bukkit.Location loc, String text) {
        org.bukkit.entity.ArmorStand stand = loc.getWorld()
            .spawn(loc, org.bukkit.entity.ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setCustomNameVisible(true);
        // 1.16〜1.17: setCustomName(String)  1.18+: customName(Component) も可だが旧APIで統一
        stand.setCustomName(color(text));
        return stand;
    }

    // ── Inventory title ─────────────────────────────────────────
    /** GUI作成用: §コード付きタイトル文字列をそのまま使う（全バージョン対応） */
    public static org.bukkit.inventory.Inventory createInventory(int size, String title) {
        return Bukkit.createInventory(null, size, color(title));
    }
}
