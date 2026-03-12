package com.warforge.core.game.modes;

import com.warforge.core.WarforgeCore;
import com.warforge.core.game.GameMode;
import com.warforge.core.util.Messages;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BattleRoyale extends GameMode {

    public static final String NAME = "バトルロイヤル";
    private final int GAME_DURATION;
    private final Set<UUID> alivePlayers = new HashSet<>();
    private BukkitTask tickTask;

    public BattleRoyale(WarforgeCore plugin, int arenaId) {
        super(plugin, arenaId);
        this.GAME_DURATION = plugin.getConfigManager().getModeDuration("battle-royale");
        this.timeLeft = GAME_DURATION;
    }

    @Override
    public void start() {
        state = GameState.IN_GAME;
        alivePlayers.addAll(players);

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);

        broadcast("&6バトルロイヤル開始！ &7最後の1人になれ！");
        getOnlinePlayers().forEach(p -> {
            plugin.getUiManager().getScoreboardManager().showGameScoreboard(p, NAME, "");
        });
    }

    @Override
    public void end() {
        state = GameState.ENDING;
        if (tickTask != null) tickTask.cancel();

        String winnerName = alivePlayers.isEmpty() ? "なし" :
            Optional.ofNullable(Bukkit.getPlayer(alivePlayers.iterator().next()))
                .map(Player::getName).orElse("不明");

        getOnlinePlayers().forEach(p -> {
            VersionAdapter.sendTitle(p, "&6&l" + winnerName + " &f勝利！", "&7Winner of Battle Royale", 10, 80, 20);
            plugin.getUiManager().getScoreboardManager().clearScoreboard(p);
            plugin.getUiManager().getActionBarManager().stopActionBar(p);
        });

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            getOnlinePlayers().forEach(this::removePlayer);
            state = GameState.WAITING;
        }, 120L);
    }

    @Override
    public void onKill(Player killer, Player victim) {
        alivePlayers.remove(victim.getUniqueId());
        broadcast("&f" + victim.getName() + " &7が脱落！ 残り: &e" + alivePlayers.size() + "人");
        if (checkWinCondition()) end();
    }

    @Override
    public void onDeath(Player victim) {
        // BRはリスポーンなし → スペクテーターモードに
        victim.setGameMode(org.bukkit.GameMode.SPECTATOR);
        victim.sendMessage(Messages.INSTANCE.prefixed("&c脱落しました。観戦モードに切り替えます。"));
    }

    @Override
    public void tick() {
        if (timeLeft <= 0) { end(); return; }
        timeLeft--;
        List<Player> online = getOnlinePlayers();
        plugin.getUiManager().getBossBarManager().showTimerBar(online, arenaId, timeLeft, GAME_DURATION, NAME);
        plugin.getUiManager().getBossBarManager().showPlayersBar(online, arenaId, alivePlayers.size(), players.size());
        online.forEach(p -> plugin.getUiManager().getActionBarManager().startGameActionBar(p, () -> timeLeft));
    }

    @Override
    public boolean checkWinCondition() {
        return alivePlayers.size() <= 1 || timeLeft <= 0;
    }

    private void broadcast(String msg) {
        getOnlinePlayers().forEach(p -> p.sendMessage(Messages.INSTANCE.prefixed(msg)));
    }

    private List<Player> getOnlinePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) result.add(p);
        }
        return result;
    }
}
