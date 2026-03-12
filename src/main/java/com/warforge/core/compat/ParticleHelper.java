package com.warforge.core.compat;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * バージョン間でParticle/Sound名が変わった箇所を一元管理。
 *
 * 変更点メモ:
 *   BLOCK_CRACK       → 1.20.5以降は BLOCK_CRUMBLE
 *   CRIT_MAGIC        → 1.20.5以降は ENCHANTED_HIT
 *   ENCHANTMENT_TABLE → 1.20.5以降は ENCHANT
 *   EXPLOSION_LARGE   → 1.20.5以降は EXPLOSION_EMITTER
 *   VILLAGER_HAPPY    → 1.20.5以降は HAPPY_VILLAGER
 *   TOTEM             → 1.20.5以降は TOTEM_OF_UNDYING
 *   SOUL_FIRE_FLAME   → 1.16+で追加（1.15以前はFLAMEフォールバック）
 *   FLASH             → 1.14+
 *   DAMAGE_INDICATOR  → 1.11+
 */
public final class ParticleHelper {

    // ── 定数 ──────────────────────────────────────────────────────
    public static final Particle BLOCK_CRACK     = get("BLOCK_CRUMBLE",    "BLOCK_CRACK",      "SMOKE_NORMAL");
    public static final Particle CRIT_MAGIC      = get("ENCHANTED_HIT",    "CRIT_MAGIC",       "CRIT");
    public static final Particle ENCHANTMENT     = get("ENCHANT",          "ENCHANTMENT_TABLE","CRIT_MAGIC");
    public static final Particle EXPLOSION_LARGE = get("EXPLOSION_EMITTER","EXPLOSION_LARGE",  "EXPLOSION_NORMAL");
    public static final Particle VILLAGER_HAPPY  = get("HAPPY_VILLAGER",   "VILLAGER_HAPPY",   "HEART");
    public static final Particle TOTEM           = get("TOTEM_OF_UNDYING", "TOTEM",            "END_ROD");
    public static final Particle SOUL_FIRE_FLAME = get("SOUL_FIRE_FLAME",  "FLAME");
    public static final Particle FLASH           = get("FLASH",            "EXPLOSION_NORMAL");
    public static final Particle DAMAGE_INDICATOR= get("DAMAGE_INDICATOR", "CRIT");
    public static final Particle FIREWORKS_SPARK = get("FIREWORK",         "FIREWORKS_SPARK",  "END_ROD");
    public static final Particle END_ROD         = get("END_ROD",          "PORTAL");
    public static final Particle PORTAL          = get("PORTAL",           "SPELL");
    public static final Particle ITEM_CRACK      = get("ITEM",             "ITEM_CRACK");
    public static final Particle FLAME           = Particle.FLAME;
    public static final Particle CRIT            = Particle.CRIT;

    private static Particle get(String... names) {
        for (String name : names) {
            try { return Particle.valueOf(name); }
            catch (IllegalArgumentException ignored) {}
        }
        return Particle.SMOKE_NORMAL;
    }

    // ── 便利メソッド ─────────────────────────────────────────────
    public static void spawn(World world, Location loc, Particle particle,
                             int count, double offX, double offY, double offZ, double extra) {
        try {
            world.spawnParticle(particle, loc, count, offX, offY, offZ, extra);
        } catch (Exception ignored) {}
    }

    public static void spawnItem(World world, Location loc, int count,
                                  double offX, double offY, double offZ, double speed, ItemStack data) {
        try {
            if (ITEM_CRACK == Particle.valueOf("ITEM")) {
                // 1.20.5+: data type は org.bukkit.Particle.DustOptions ではなくItemStack
                world.spawnParticle(ITEM_CRACK, loc, count, offX, offY, offZ, speed, data);
            } else {
                world.spawnParticle(ITEM_CRACK, loc, count, offX, offY, offZ, speed, data);
            }
        } catch (Exception e) {
            world.spawnParticle(Particle.FLAME, loc, count, offX, offY, offZ, speed);
        }
    }

    public static void spawnBlock(World world, Location loc, int count,
                                   double offX, double offY, double offZ, double speed,
                                   org.bukkit.block.data.BlockData data) {
        try {
            world.spawnParticle(BLOCK_CRACK, loc, count, offX, offY, offZ, speed, data);
        } catch (Exception e) {
            world.spawnParticle(Particle.SMOKE_NORMAL, loc, count, offX, offY, offZ, speed);
        }
    }
}
