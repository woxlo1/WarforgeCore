package com.warforge.core.killstreak;

import com.warforge.core.WarforgeCore;
import com.warforge.core.util.Messages;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillstreakManager {

    private final WarforgeCore plugin;
    private final Map<UUID, Integer> streaks = new HashMap<>();

    private static final KillstreakReward[] REWARDS = {
        new KillstreakReward(3,  "KILLING SPREE",  "&e",  KillstreakType.SPEED_BOOST),
        new KillstreakReward(5,  "RAMPAGE",         "&6",  KillstreakType.UAV),
        new KillstreakReward(7,  "UNSTOPPABLE",     "&c",  KillstreakType.HEAL),
        new KillstreakReward(10, "GODLIKE",          "&d",  KillstreakType.AIRSTRIKE),
        new KillstreakReward(15, "LEGENDARY",        "&6&l",KillstreakType.LEGENDARY),
    };

    public KillstreakManager(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    public void onKill(Player player) {
        UUID uuid = player.getUniqueId();
        int streak = streaks.merge(uuid, 1, Integer::sum);

        for (KillstreakReward reward : REWARDS) {
            if (streak == reward.killCount()) {
                announceStreak(player, streak, reward);
                applyReward(player, reward.type());
                return;
            }
        }

        if (streak >= 3) {
            VersionAdapter.sendActionBar(player, "&e" + streak + " キルストリーク継続中！");
        }
    }

    public void onDeath(Player player) {
        int streak = streaks.getOrDefault(player.getUniqueId(), 0);
        if (streak >= 5) {
            notifyGamePlayers(player,
                "&c" + player.getName() + " &7の " + streak + " キルストリークが終了！");
        }
        streaks.put(player.getUniqueId(), 0);
    }

    private void announceStreak(Player player, int streak, KillstreakReward reward) {
        VersionAdapter.sendTitle(player, reward.color() + streak + " KILL STREAK", "&f" + reward.name(), 5, 40, 10);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.5f);
        notifyGamePlayers(player,
            reward.color() + "&l" + reward.name() + " &r" + reward.color() +
            "! &f" + player.getName() + " &7が " + streak + " キル達成！");
    }

    private void applyReward(Player player, KillstreakType type) {
        switch (type) {
            case SPEED_BOOST -> {
                player.addPotionEffect(new PotionEffect(resolvePotion("SPEED"), 200, 1));
                player.sendMessage(Messages.INSTANCE.prefixed("&e速度アップ！ (10秒)"));
            }
            case UAV -> {
                player.sendMessage(Messages.INSTANCE.prefixed("&b敵の位置情報を取得！"));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGameManager().getPlayerGame(player.getUniqueId()).ifPresent(game ->
                        game.getPlayers().forEach(uuid -> {
                            Player target = Bukkit.getPlayer(uuid);
                            if (target != null && !target.equals(player)) {
                                player.sendMessage("&c▸ " + target.getName() + " &7(" +
                                    (int)target.getLocation().distance(player.getLocation()) + "m)");
                            }
                        })
                    );
                }, 5L);
            }
            case HEAL -> {
                double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                player.setHealth(Math.min(maxHealth, player.getHealth() + 10));
                player.addPotionEffect(new PotionEffect(resolvePotion("REGENERATION"), 100, 1));
                player.sendMessage(Messages.INSTANCE.prefixed("&aHP回復！"));
            }
            case AIRSTRIKE -> {
                player.sendMessage(Messages.INSTANCE.prefixed("&c💥 エアストライク発動！ &7(向いている方向に攻撃)"));
                org.bukkit.block.Block targetBlock = player.getTargetBlock(null, 50);
                Location target = (targetBlock != null && !targetBlock.getType().isAir())
                    ? targetBlock.getLocation()
                    : player.getLocation().add(player.getLocation().getDirection().multiply(30));

                final org.bukkit.World world = target.getWorld();
                if (world == null) return; // フォールバック: ワールドがない場合スキップ

                for (int i = 0; i < 5; i++) {
                    final int delay = i * 5;
                    final Location loc = target.clone().add(
                        (Math.random() - 0.5) * 8, 0, (Math.random() - 0.5) * 8
                    );
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (world != null) {
                            world.strikeLightningEffect(loc);
                            world.createExplosion(loc, 0f, false, false);
                        }
                        if (loc.getWorld() != null) {
                            loc.getWorld().getNearbyEntities(loc, 3, 3, 3).forEach(e -> {
                                if (e instanceof Player hit && !hit.equals(player)) {
                                    hit.damage(8.0, player);
                                }
                            });
                        }
                    }, delay);
                }
            }
            case LEGENDARY -> {
                player.addPotionEffect(new PotionEffect(resolvePotion("SPEED"), 300, 2));
                player.addPotionEffect(new PotionEffect(resolvePotion("STRENGTH", "INCREASE_DAMAGE"), 300, 1));
                player.addPotionEffect(new PotionEffect(resolvePotion("RESISTANCE", "DAMAGE_RESISTANCE"), 300, 1));
                player.sendMessage(Messages.INSTANCE.prefixed("&6&l✦ LEGENDARY モード発動！ (15秒)"));
            }
        }
    }

    private void notifyGamePlayers(Player player, String msg) {
        plugin.getGameManager().getPlayerGame(player.getUniqueId()).ifPresent(game ->
            game.getPlayers().forEach(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.sendMessage(Messages.INSTANCE.prefixed(msg));
            })
        );
    }

    private PotionEffectType resolvePotion(String... names) {
        for (String n : names) {
            try {
                // Try modern Bukkit API (1.20.5+)
                PotionEffectType t = PotionEffectType.getByName(n);
                if (t != null) return t;
            } catch (Exception ignored) {}
        }
        // Fallback to SPEED if all failed
        try {
            PotionEffectType t = PotionEffectType.getByName("SPEED");
            if (t != null) return t;
        } catch (Exception ignored) {}
        return PotionEffectType.SPEED;
    }

    private static final class KillstreakReward {
        private final int killCount;
        private final String name;
        private final String color;
        private final KillstreakType type;
        KillstreakReward(int killCount, String name, String color, KillstreakType type) {
            this.killCount = killCount; this.name = name; this.color = color; this.type = type;
        }
        public int killCount() { return killCount; }
        public String name() { return name; }
        public String color() { return color; }
        public KillstreakType type() { return type; }
    }

    private enum KillstreakType { SPEED_BOOST, UAV, HEAL, AIRSTRIKE, LEGENDARY }
}
