package com.warforge.core.spectator;

import com.warforge.core.WarforgeCore;
import com.warforge.core.game.GameMode;
import com.warforge.core.util.Messages;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class SpectatorManager {

    private final WarforgeCore plugin;
    private final Set<UUID> spectators = new HashSet<>();
    // spectator → 現在観戦中のターゲット
    private final Map<UUID, UUID> spectatingTarget = new HashMap<>();
    private BukkitTask updateTask;

    public SpectatorManager(WarforgeCore plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    /** 観戦モードに入る */
    public void enterSpectator(Player player, int arenaId) {
        GameMode game = plugin.getGameManager().getActiveGames().get(arenaId);
        if (game == null) {
            player.sendMessage(Messages.INSTANCE.prefixed("&cその試合は現在進行していません。"));
            return;
        }

        spectators.add(player.getUniqueId());
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.sendMessage(Messages.INSTANCE.prefixed("&b観戦モードに入りました。 &7Qキーで離脱"));

        // ランダムなプレイヤーに視点を合わせる
        snapToNextPlayer(player, game);
    }

    /** 観戦モードを離脱 */
    public void exitSpectator(Player player) {
        if (!spectators.contains(player.getUniqueId())) return;
        spectators.remove(player.getUniqueId());
        spectatingTarget.remove(player.getUniqueId());
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.sendMessage(Messages.INSTANCE.prefixed("&a観戦モードを終了しました。"));
    }

    /** 次のプレイヤーに視点を切り替え */
    public void nextTarget(Player spectator) {
        plugin.getGameManager().getActiveGames().values().stream()
            .filter(g -> spectators.contains(spectator.getUniqueId()))
            .findFirst()
            .ifPresent(g -> snapToNextPlayer(spectator, g));
    }

    private void snapToNextPlayer(Player spectator, GameMode game) {
        List<UUID> players = new ArrayList<>(game.getPlayers());
        if (players.isEmpty()) return;

        UUID current = spectatingTarget.get(spectator.getUniqueId());
        int idx = current != null ? players.indexOf(current) : -1;
        UUID nextUuid = players.get((idx + 1) % players.size());
        Player target = Bukkit.getPlayer(nextUuid);

        if (target != null) {
            spectator.setSpectatorTarget(target);
            spectatingTarget.put(spectator.getUniqueId(), nextUuid);
            VersionAdapter.sendActionBar(spectator, "&7観戦中: &f" + target.getName() + "  &8[Fキー: 次のプレイヤー]");
        }
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            spectators.forEach(uuid -> {
                Player spectator = Bukkit.getPlayer(uuid);
                if (spectator == null) return;

                // 観戦中ターゲットが死んだり離脱した場合は自動で切り替え
                UUID targetUuid = spectatingTarget.get(uuid);
                if (targetUuid != null) {
                    Player target = Bukkit.getPlayer(targetUuid);
                    if (target == null || target.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                        nextTarget(spectator);
                    }
                }
            });
        }, 40L, 40L);
    }

    public boolean isSpectating(UUID uuid) { return spectators.contains(uuid); }

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
        spectators.forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) exitSpectator(p);
        });
    }
}
