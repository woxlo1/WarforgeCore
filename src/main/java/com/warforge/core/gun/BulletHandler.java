package com.warforge.core.gun;

import com.warforge.core.WarforgeCore;
import com.warforge.core.player.WFPlayer;
import com.warforge.core.util.Messages;
import com.warforge.core.compat.VersionAdapter;
import com.warforge.core.compat.SoundHelper;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class BulletHandler {

    private final WarforgeCore plugin;
    private final Map<UUID, Boolean> reloading = new HashMap<>();
    private final Map<UUID, Integer> ammo = new HashMap<>();
    private final Map<UUID, Integer> reserveAmmo = new HashMap<>();
    private final Map<UUID, Long> lastShot = new HashMap<>();
    private final Map<UUID, Integer> burstCount = new HashMap<>(); // バーストカウント

    public BulletHandler(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    public void shoot(Player shooter, GunData gun) {
        if (!plugin.getConfigManager().isDamageEnabled()) return;
        UUID uuid = shooter.getUniqueId();
        if (isReloading(uuid)) {
            VersionAdapter.sendActionBar(shooter, "&cリロード中...");
            return;
        }

        long now = System.currentTimeMillis();
        long last = lastShot.getOrDefault(uuid, 0L);
        if (now - last < gun.getFireRate()) return;
        lastShot.put(uuid, now);

        int currentAmmo = ammo.getOrDefault(uuid, gun.getMagazineSize());
        if (currentAmmo <= 0) {
            int reserve = reserveAmmo.getOrDefault(uuid, gun.getReserveAmmo());
            if (reserve > 0) reload(shooter, gun);
            else VersionAdapter.sendActionBar(shooter, "&c弾切れ！");
            return;
        }

        // FireModeごとの処理
        switch (gun.getFireMode()) {
            case AUTO, SEMI -> {
                ammo.put(uuid, currentAmmo - 1);
                fireBullets(shooter, gun);
            }
            case BURST -> {
                // バースト：burstCount発を連続発射
                int shots = Math.min(gun.getBurstCount(), currentAmmo);
                ammo.put(uuid, currentAmmo - shots);
                for (int i = 0; i < shots; i++) {
                    final int delay = i * 60; // 3tick間隔
                    Bukkit.getScheduler().runTaskLater(plugin, () -> fireBullets(shooter, gun), delay);
                }
            }
        }

        // 残弾アクションバー更新
        updateAmmoBar(shooter, gun);
    }

    private void fireBullets(Player shooter, GunData gun) {
        // ショットガンなど複数弾
        for (int i = 0; i < gun.getBulletCount(); i++) {
            fireSingleBullet(shooter, gun);
        }
        // 射撃サウンド（サプレッサーで音量半減）
        float vol = gun.getSuppressorEquipped() ? gun.getShootSoundVolume() * 0.4f : gun.getShootSoundVolume();
        try {
            Sound sound = Sound.valueOf(gun.getShootSound());
            shooter.getWorld().playSound(shooter.getLocation(), sound, vol, gun.getShootSoundPitch());
        } catch (IllegalArgumentException ignored) {}
        spawnMuzzleFlash(shooter.getEyeLocation());
    }

    private void fireSingleBullet(Player shooter, GunData gun) {
        Location start = shooter.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        direction = applySpread(direction, gun.getSpreadAngle());

        final Vector[] dir = {direction};
        final Location[] pos = {start.clone()};
        final double maxRange = gun.getRange();
        final double[] traveled = {0};

        new BukkitRunnable() {
            @Override
            public void run() {
                if (traveled[0] >= maxRange * 1.5) { cancel(); return; }

                double step = gun.getBulletSpeed();
                pos[0].add(dir[0].clone().multiply(step));
                traveled[0] += step;

                shooter.getWorld().spawnParticle(Particle.CRIT, pos[0], 1, 0, 0, 0, 0);

                if (pos[0].getBlock().getType().isSolid()) {
                    spawnHitBlockEffect(pos[0]);
                    cancel();
                    return;
                }

                for (Entity entity : pos[0].getWorld().getNearbyEntities(pos[0], 0.5, 0.5, 0.5)) {
                    if (!(entity instanceof LivingEntity target)) continue;
                    if (entity.equals(shooter)) continue;
                    // フレンドリーファイア無効チェック
                    if (!plugin.getConfigManager().isFriendlyFire() && entity instanceof Player other) {
                        // 同チームなら無視（TODO: チーム判定）
                    }

                    // 距離減衰
                    double distanceFactor = 1.0;
                    if (traveled[0] > gun.getRange()) {
                        double excess = traveled[0] - gun.getRange();
                        distanceFactor = Math.max(0.2, 1.0 - (excess * gun.getDropOff()));
                    }

                    // ヒット部位判定
                    double hitY = pos[0].getY() - target.getLocation().getY();
                    boolean headshotEnabled = plugin.getConfigManager().isHeadshotEnabled();
                    boolean legEnabled = plugin.getConfigManager().isLegDamageEnabled();
                    double entityHeight = target.getHeight();
                    double multiplier;
                    String hitZone;
                    if (headshotEnabled && hitY >= entityHeight * 0.85) {
                        multiplier = gun.getHeadshotMultiplier();
                        hitZone = "HEADSHOT";
                    } else if (legEnabled && hitY <= entityHeight * 0.3) {
                        multiplier = gun.getLegMultiplier();
                        hitZone = "LEG";
                    } else {
                        multiplier = gun.getBodyMultiplier();
                        hitZone = "BODY";
                    }

                    double finalDamage = gun.getDamage() * multiplier * distanceFactor
                        * plugin.getConfigManager().getGlobalDamageMultiplier();

                    onHit(shooter, target, finalDamage, hitZone, gun);
                    cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void onHit(Player shooter, LivingEntity target, double damage, String hitZone, GunData gun) {
        target.damage(damage, shooter);
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 5);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);

        String damageStr = String.format("%.0f", damage);
        switch (hitZone) {
            case "HEADSHOT" -> {
                VersionAdapter.sendActionBar(shooter, "&6&l✦ HEADSHOT &e" + damageStr);
                shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
                // ヘッドショットボーナス報酬
                if (plugin.getVaultManager().isEnabled()) {
                    plugin.getVaultManager().reward(shooter,
                        plugin.getEconomyConfig().getHeadshotBonus(), "ヘッドショット");
                }
                // ヘッドショットミッション
                plugin.getMissionManager().onHeadshot(shooter);
            }
            case "LEG" -> {
                VersionAdapter.sendActionBar(shooter, "&7LEG &f" + damageStr);
                shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 0.8f);
            }
            default -> {
                VersionAdapter.sendActionBar(shooter, "&f" + damageStr);
                shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            }
        }

        if (target instanceof Player victim && target.getHealth() - damage <= 0) {
            onKill(shooter, victim, hitZone.equals("HEADSHOT"));
        }
    }

    private void onKill(Player killer, Player victim, boolean headshot) {
        WFPlayer wfKiller = plugin.getPlayerManager().getPlayer(killer.getUniqueId());
        WFPlayer wfVictim = plugin.getPlayerManager().getPlayer(victim.getUniqueId());

        if (wfKiller != null) {
            wfKiller.setKills(wfKiller.getKills() + 1);
        }
        if (wfVictim != null) {
            wfVictim.setDeaths(wfVictim.getDeaths() + 1);
        }

        // キル報酬（Vault）
        if (plugin.getVaultManager().isEnabled()) {
            plugin.getVaultManager().reward(killer,
                plugin.getEconomyConfig().getKillReward(), "キル");
        }

        // キルストリーク
        plugin.getKillstreakManager().onKill(killer);

        // ミッション進捗
        plugin.getMissionManager().onKill(killer);

        // ゲームモードにキルを通知
        plugin.getGameManager().getActiveGames().values().stream()
            .filter(g -> g.getPlayers().contains(killer.getUniqueId()))
            .findFirst()
            .ifPresent(g -> g.onKill(killer, victim));

        String killMsg = Messages.INSTANCE.prefixed(
            "&c" + victim.getName() + " &7が &a" + killer.getName() +
            " &7に倒された" + (headshot ? " &6[HEADSHOT]" : "")
        );
        plugin.getGameManager().getActiveGames().values().stream()
            .filter(g -> g.getPlayers().contains(killer.getUniqueId()))
            .findFirst()
            .ifPresent(g -> g.getPlayers().forEach(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.sendMessage(killMsg);
            }));
    }

    public void reload(Player player, GunData gun) {
        UUID uuid = player.getUniqueId();
        if (isReloading(uuid)) return;

        int currentAmmo = ammo.getOrDefault(uuid, gun.getMagazineSize());
        int reserve = reserveAmmo.getOrDefault(uuid, gun.getReserveAmmo());
        if (currentAmmo >= gun.getMagazineSize() || reserve <= 0) return;

        reloading.put(uuid, true);
        // 残弾ありならタクティカルリロード（短縮）
        long reloadMs = currentAmmo > 0 ? gun.getTacticalReloadTime() : gun.getReloadTime();
        double reloadSec = reloadMs / 1000.0;

        VersionAdapter.sendActionBar(player, String.format("&eリロード中... &7(%.1f秒)", reloadSec));
        SoundHelper.play(player, SoundHelper.NOTE_PLING, 1.0f, 0.8f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int needed = gun.getMagazineSize() - currentAmmo;
            int toReload = Math.min(needed, reserve);
            ammo.put(uuid, currentAmmo + toReload);
            reserveAmmo.put(uuid, reserve - toReload);
            reloading.put(uuid, false);
            SoundHelper.play(player, SoundHelper.NOTE_PLING, 1.0f, 1.2f);
            updateAmmoBar(player, gun);
        }, reloadMs / 50L);
    }

    private void updateAmmoBar(Player player, GunData gun) {
        UUID uuid = player.getUniqueId();
        int current = ammo.getOrDefault(uuid, gun.getMagazineSize());
        int reserve = reserveAmmo.getOrDefault(uuid, gun.getReserveAmmo());
        VersionAdapter.sendActionBar(player,
            gun.getRarity().getColor() + gun.getDisplayName() +
            " &f| &e" + current + "&7/" + gun.getMagazineSize() +
            "  &7予備: &f" + reserve);
    }

    public void initAmmo(UUID uuid, GunData gun) {
        ammo.put(uuid, gun.getMagazineSize());
        reserveAmmo.put(uuid, gun.getReserveAmmo());
        reloading.put(uuid, false);
    }

    public boolean isReloading(UUID uuid) { return reloading.getOrDefault(uuid, false); }

    public void resetAmmo(UUID uuid) {
        reloading.remove(uuid);
        ammo.remove(uuid);
        reserveAmmo.remove(uuid);
        lastShot.remove(uuid);
    }

    private Vector applySpread(Vector direction, double spreadAngle) {
        if (spreadAngle <= 0) return direction;
        Random rand = new Random();
        double spread = Math.toRadians(spreadAngle) * (rand.nextDouble() - 0.5);
        return direction.add(new Vector(
            (rand.nextDouble() - 0.5) * spread,
            (rand.nextDouble() - 0.5) * spread,
            (rand.nextDouble() - 0.5) * spread
        )).normalize();
    }

    private void spawnMuzzleFlash(Location loc) {
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
    }

    private void spawnHitBlockEffect(Location loc) {
        loc.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 5, loc.getBlock().getBlockData());
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_HIT, 0.5f, 1.0f);
    }
}
