package com.warforge.core.effect;

import com.warforge.core.WarforgeCore;
import com.warforge.core.compat.ParticleHelper;
import com.warforge.core.compat.SoundHelper;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class KillEffectManager {

    private final WarforgeCore plugin;

    public KillEffectManager(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    public void playKillEffect(Player killer, Player victim) {
        Location loc = victim.getLocation().add(0, 1, 0);
        World world = loc.getWorld();

        ParticleHelper.spawn(world, loc, ParticleHelper.DAMAGE_INDICATOR, 15, 0.3, 0.5, 0.3, 0.1);
        ParticleHelper.spawnBlock(world, loc, 20, 0.4, 0.5, 0.4, 0.1,
            Material.REDSTONE_BLOCK.createBlockData());

        SoundHelper.playAt(world, loc, SoundHelper.PLAYER_DEATH, 0.8f, 0.9f);
        SoundHelper.play(killer, SoundHelper.ORB_PICKUP, 0.6f, 1.5f);

        spawnHologram(loc, "&cKILL!");
    }

    public void playHeadshotEffect(Player killer, Player victim) {
        Location loc = victim.getLocation().add(0, 1.8, 0);
        World world = loc.getWorld();

        ParticleHelper.spawn(world, loc, ParticleHelper.FLASH, 1, 0, 0, 0, 0);
        ParticleHelper.spawn(world, loc, ParticleHelper.CRIT_MAGIC, 30, 0.2, 0.2, 0.2, 0.3);
        ParticleHelper.spawn(world, loc, ParticleHelper.DAMAGE_INDICATOR, 20, 0.2, 0.2, 0.2, 0.2);

        SoundHelper.playAt(world, loc, SoundHelper.PLAYER_DEATH, 1.0f, 1.5f);
        SoundHelper.playAt(world, loc, SoundHelper.ANVIL_LAND, 0.3f, 2.0f);
        SoundHelper.play(killer, SoundHelper.CHALLENGE_DONE, 0.5f, 2.0f);
        SoundHelper.play(killer, SoundHelper.ORB_PICKUP, 1.0f, 2.0f);

        spawnHologram(loc, "&6&lHEADSHOT!");
    }

    public void playStreakEffect(Player player, int streak) {
        Location loc = player.getLocation().add(0, 2.5, 0);
        World world = loc.getWorld();

        switch (streak) {
            case 3 -> {
                ParticleHelper.spawn(world, loc, ParticleHelper.FLAME, 20, 0.5, 0.5, 0.5, 0.05);
                SoundHelper.play(player, SoundHelper.NOTE_PLING, 1f, 1.5f);
            }
            case 5 -> {
                ParticleHelper.spawn(world, loc, ParticleHelper.SOUL_FIRE_FLAME, 30, 0.5, 0.5, 0.5, 0.1);
                spiralEffect(player, ParticleHelper.FLAME);
                SoundHelper.play(player, SoundHelper.CHALLENGE_DONE, 0.8f, 1.2f);
            }
            case 7 -> {
                ParticleHelper.spawn(world, loc, ParticleHelper.ENCHANTMENT, 40, 0.7, 0.7, 0.7, 0.2);
                spiralEffect(player, ParticleHelper.CRIT_MAGIC);
                SoundHelper.play(player, SoundHelper.CHALLENGE_DONE, 1f, 1f);
            }
            case 10 -> {
                spiralEffect(player, ParticleHelper.PORTAL);
                ParticleHelper.spawn(world, loc, ParticleHelper.EXPLOSION_LARGE, 3, 0.5, 0.5, 0.5, 0);
                SoundHelper.play(player, SoundHelper.DRAGON_GROWL, 0.5f, 1.5f);
            }
            case 15 -> {
                spiralEffect(player, ParticleHelper.END_ROD);
                ParticleHelper.spawn(world, loc, ParticleHelper.TOTEM, 50, 1, 1, 1, 0.3);
                SoundHelper.play(player, SoundHelper.CHALLENGE_DONE, 1f, 0.8f);
                SoundHelper.play(player, SoundHelper.DRAGON_DEATH, 0.3f, 1.5f);
            }
        }
    }

    public void playWinEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        ParticleHelper.spawn(world, loc, ParticleHelper.TOTEM, 100, 1, 2, 1, 0.3);
        ParticleHelper.spawn(world, loc, ParticleHelper.END_ROD, 50, 0.5, 2, 0.5, 0.1);
        SoundHelper.play(player, SoundHelper.CHALLENGE_DONE, 1f, 1.2f);
        SoundHelper.play(player, SoundHelper.LEVEL_UP, 1f, 1f);

        new BukkitRunnable() {
            int tick = 0;
            public void run() {
                if (tick >= 5 || !player.isOnline()) { cancel(); return; }
                ParticleHelper.spawn(player.getWorld(),
                    player.getLocation().add(0, 2, 0),
                    ParticleHelper.FIREWORKS_SPARK, 20, 1, 1, 1, 0.2);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void playGoldPickupEffect(Player player, int amount) {
        Location loc = player.getLocation().add(0, 1, 0);
        ParticleHelper.spawnItem(player.getWorld(), loc, 5 + amount,
            0.3, 0.3, 0.3, 0.1,
            new org.bukkit.inventory.ItemStack(Material.GOLD_NUGGET));
        SoundHelper.play(player, SoundHelper.ORB_PICKUP, 0.5f, 1.8f);
    }

    public void playGoldDropEffect(Player victim, int amount) {
        Location loc = victim.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        ParticleHelper.spawnItem(world, loc, Math.min(amount * 2, 30),
            0.5, 0.5, 0.5, 0.2,
            new org.bukkit.inventory.ItemStack(Material.GOLD_NUGGET));
        ParticleHelper.spawn(world, loc, ParticleHelper.CRIT, 15, 0.4, 0.4, 0.4, 0.1);
        SoundHelper.playAt(world, loc, SoundHelper.ORB_PICKUP, 0.8f, 0.8f);
    }

    private void spiralEffect(Player player, Particle particle) {
        new BukkitRunnable() {
            int step = 0;
            public void run() {
                if (step >= 30 || !player.isOnline()) { cancel(); return; }
                double angle = step * Math.PI / 8;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                ParticleHelper.spawn(player.getWorld(),
                    player.getLocation().add(x, step * 0.1, z),
                    particle, 1, 0, 0, 0, 0);
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @SuppressWarnings("deprecation")
    private void spawnHologram(Location loc, String text) {
        ArmorStand stand = VersionAdapter.spawnArmorStand(loc, text);
        new BukkitRunnable() {
            int tick = 0;
            public void run() {
                if (tick >= 20 || stand.isDead()) { stand.remove(); cancel(); return; }
                stand.teleport(stand.getLocation().add(0, 0.05, 0));
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
