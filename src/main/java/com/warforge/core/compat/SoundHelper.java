package com.warforge.core.compat;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * バージョン間でSound名が変わった箇所を一元管理。
 *
 * 変更メモ (主要):
 *   1.9: UI_TOAST_CHALLENGE_COMPLETE 追加
 *   1.16: BLOCK_NOTE_BLOCK_* 形式に統一
 *   1.19: 一部音が削除・追加
 */
public final class SoundHelper {

    public static final Sound LEVEL_UP        = get("ENTITY_PLAYER_LEVELUP",       "LEVEL_UP");
    public static final Sound PLAYER_DEATH    = get("ENTITY_PLAYER_DEATH",          "PLAYER_DEATH");
    public static final Sound PLAYER_HURT     = get("ENTITY_PLAYER_HURT",           "PLAYER_HURT");
    public static final Sound ORB_PICKUP      = get("ENTITY_EXPERIENCE_ORB_PICKUP", "ORB_PICKUP");
    public static final Sound CHALLENGE_DONE  = get("UI_TOAST_CHALLENGE_COMPLETE",  "ENTITY_PLAYER_LEVELUP");
    public static final Sound ANVIL_LAND      = get("BLOCK_ANVIL_LAND",             "ANVIL_LAND");
    public static final Sound NOTE_PLING      = get("BLOCK_NOTE_BLOCK_PLING",       "NOTE_PLING");
    public static final Sound VILLAGER_NO     = get("ENTITY_VILLAGER_NO",           "VILLAGER_NO");
    public static final Sound DRAGON_GROWL    = get("ENTITY_ENDER_DRAGON_GROWL",    "ENDERDRAGON_GROWL");
    public static final Sound DRAGON_DEATH    = get("ENTITY_ENDER_DRAGON_DEATH",    "ENDERDRAGON_DEATH");

    private static Sound get(String... names) {
        for (String n : names) {
            try { return Sound.valueOf(n); }
            catch (IllegalArgumentException ignored) {}
        }
        return Sound.UI_BUTTON_CLICK;
    }

    // ── 便利メソッド ─────────────────────────────────────────────
    public static void play(Player player, Sound sound, float volume, float pitch) {
        try { player.playSound(player.getLocation(), sound, volume, pitch); }
        catch (Exception ignored) {}
    }

    public static void playAt(World world, Location loc, Sound sound, float volume, float pitch) {
        try { world.playSound(loc, sound, volume, pitch); }
        catch (Exception ignored) {}
    }
}
