package com.warforge.core.game.modes;

import com.warforge.core.WarforgeCore;
import com.warforge.core.arena.Arena;
import com.warforge.core.game.GameMode;
import com.warforge.core.i18n.LangManager;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GoldRush - 時間内に最も多くの金を集めたプレイヤーが勝利
 * - 指定数ではなく多く集めた人の勝ち
 * - 定期的に現在の1位をアナウンス
 * - キル時にドロップ量は全額ではなく60〜70%（config設定可能）
 */
public class GoldRush extends GameMode {

    public static final String NAME = "GoldRush";
    private final int GAME_DURATION;
    private final int GOLD_SPAWN_INTERVAL;
    private final double GOLD_DROP_RATE;         // キル時のドロップ率 0.6〜0.7
    private final int ANNOUNCE_INTERVAL;         // 順位発表間隔（秒）

    private final Map<UUID, Integer> goldCollected = new HashMap<>();
    private int goldSpawnTimer = 0;
    private int announceTimer = 0;
    private BukkitTask tickTask;

    public GoldRush(WarforgeCore plugin, int arenaId) {
        super(plugin, arenaId);
        this.GAME_DURATION     = plugin.getConfigManager().getModeDuration("gold-rush");
        this.GOLD_SPAWN_INTERVAL = plugin.getConfigManager().getGoldRushSpawnInterval();
        this.GOLD_DROP_RATE    = plugin.getConfig().getDouble("modes.gold-rush.kill-drop-rate", 0.65);
        this.ANNOUNCE_INTERVAL = plugin.getConfig().getInt("modes.gold-rush.announce-interval", 30);
        this.timeLeft          = GAME_DURATION;
    }

    @Override
    public void start() {
        state = GameState.IN_GAME;
        players.forEach(uuid -> goldCollected.put(uuid, 0));

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);

        broadcast("goldrush.start");
        getOnlinePlayers().forEach(p ->
            plugin.getUiManager().getScoreboardManager().showGameScoreboard(p, NAME, "")
        );
    }

    @Override
    public void end() {
        state = GameState.ENDING;
        if (tickTask != null) tickTask.cancel();

        // 最終ランキング（上位3名）
        List<Map.Entry<UUID, Integer>> ranking = getSortedRanking();

        String winnerName = "なし";
        int winnerGold = 0;
        if (!ranking.isEmpty()) {
            UUID winnerUuid = ranking.get(0).getKey();
            winnerGold = ranking.get(0).getValue();
            Player winner = Bukkit.getPlayer(winnerUuid);
            winnerName = winner != null ? winner.getName() :
                plugin.getPlayerManager().getPlayer(winnerUuid) != null
                    ? plugin.getPlayerManager().getPlayer(winnerUuid).getName() : "???";

            // 勝利報酬
            if (winner != null && plugin.getVaultManager().isEnabled()) {
                plugin.getVaultManager().reward(winner,
                    plugin.getEconomyConfig().getWinReward("goldrush"), "GoldRush優勝");
            }
            plugin.getRankManager().onGameEnd(winnerUuid, true, 0, true);

            // 勝利エフェクト
            if (winner != null) plugin.getKillEffectManager().playWinEffect(winner);
        }

        // 参加者にランク処理
        for (Map.Entry<UUID, Integer> entry : ranking) {
            boolean won = entry.getKey().equals(
                ranking.isEmpty() ? null : ranking.get(0).getKey());
            if (!won) plugin.getRankManager().onGameEnd(entry.getKey(), false, 0, false);
        }

        final String finalWinnerName = winnerName;
        final int finalGold = winnerGold;
        getOnlinePlayers().forEach(p -> {
            VersionAdapter.sendTitle(p, "&6&l" + finalWinnerName + " &f勝利！", "&e最終スコア: " + finalGold + "個", 10, 80, 20);
            plugin.getUiManager().getBossBarManager().clearBars(getOnlinePlayers(), arenaId);
            plugin.getUiManager().getScoreboardManager().clearScoreboard(p);
            plugin.getUiManager().getActionBarManager().stopActionBar(p);
        });

        broadcastFinalRanking(ranking);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            getOnlinePlayers().forEach(this::removePlayer);
            state = GameState.WAITING;
            plugin.getGameManager().onGameEnd(arenaId);
        }, 120L);
    }

    @Override
    public void onKill(Player killer, Player victim) {
        UUID victimUuid = victim.getUniqueId();
        int victimGold = goldCollected.getOrDefault(victimUuid, 0);

        if (victimGold > 0) {
            // ドロップ率：60〜70%をランダムに（残りは消滅）
            double rate = GOLD_DROP_RATE + (Math.random() * 0.1); // 0.6〜0.7
            int dropped = (int) Math.ceil(victimGold * rate);
            int remaining = victimGold - dropped;

            goldCollected.put(victimUuid, remaining);
            goldCollected.merge(killer.getUniqueId(), dropped, Integer::sum);

            // ドロップエフェクト
            plugin.getKillEffectManager().playGoldDropEffect(victim, dropped);

            // 視覚的にゴールドをドロップ（すぐに消えるように）
            ItemStack gold = new ItemStack(Material.GOLD_NUGGET, Math.min(dropped, 64));
            victim.getWorld().dropItem(victim.getLocation(), gold).remove();

            plugin.getLangManager().send(killer, "goldrush.drop",
                "victim", victim.getName(), "amount", String.valueOf(dropped));

            VersionAdapter.sendActionBar(killer, plugin.getLangManager().get("goldrush.pickup",
                "amount", String.valueOf(goldCollected.getOrDefault(killer.getUniqueId(), 0))));
        }

        plugin.getKillEffectManager().playKillEffect(killer, victim);

        broadcast("kill.message",
            "victim", victim.getName(), "killer", killer.getName(), "suffix", "");
        plugin.getMissionManager().onKill(killer);
        plugin.getKillstreakManager().onKill(killer);
    }

    @Override
    public void onDeath(Player victim) {
        int delay = plugin.getConfigManager().getModeRespawnDelay("gold-rush");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!players.contains(victim.getUniqueId())) return;
            victim.setHealth(20.0);
            victim.setFoodLevel(20);
            Location spawn = plugin.getArenaManager().getRandomSpawn(arenaId);
            if (spawn != null) victim.teleport(spawn);
            plugin.getLangManager().send(victim, "game.respawn");
        }, delay * 20L);
    }

    @Override
    public void tick() {
        if (timeLeft <= 0) { end(); return; }
        timeLeft--;
        goldSpawnTimer++;
        announceTimer++;

        // 定期的に金をスポーン
        if (goldSpawnTimer >= GOLD_SPAWN_INTERVAL) {
            goldSpawnTimer = 0;
            spawnGold();
        }

        // 定期的に順位発表
        if (announceTimer >= ANNOUNCE_INTERVAL) {
            announceTimer = 0;
            announceLeaderboard();
        }

        // 残り時間警告
        if (timeLeft == 60 || timeLeft == 30 || timeLeft == 10) {
            String leader = getLeaderName();
            int leaderGold = getLeaderGold();
            broadcastRaw("goldrush.time-warning",
                "seconds", String.valueOf(timeLeft),
                "leader", leader,
                "gold", String.valueOf(leaderGold));
        }

        updateUI();
    }

    @Override
    public boolean checkWinCondition() {
        return timeLeft <= 0;
    }

    /** アイテムピックアップイベントから呼ぶ */
    public void onGoldPickup(Player player, int amount) {
        goldCollected.merge(player.getUniqueId(), amount, Integer::sum);
        int total = goldCollected.getOrDefault(player.getUniqueId(), 0);

        plugin.getKillEffectManager().playGoldPickupEffect(player, amount);
        VersionAdapter.sendActionBar(player, plugin.getLangManager().get("goldrush.pickup",
            "amount", String.valueOf(total)));
    }

    /** 現在の順位を発表 */
    private void announceLeaderboard() {
        List<Map.Entry<UUID, Integer>> top3 = getSortedRanking().stream().limit(3).toList();
        if (top3.isEmpty()) return;

        String r1 = getNameFromUuid(top3.get(0).getKey()), g1 = String.valueOf(top3.get(0).getValue());
        String r2 = top3.size() > 1 ? getNameFromUuid(top3.get(1).getKey()) : "-";
        String g2 = top3.size() > 1 ? String.valueOf(top3.get(1).getValue()) : "0";
        String r3 = top3.size() > 2 ? getNameFromUuid(top3.get(2).getKey()) : "-";
        String g3 = top3.size() > 2 ? String.valueOf(top3.get(2).getValue()) : "0";

        broadcastRaw("goldrush.leaderboard-announce",
            "rank1", r1, "g1", g1, "rank2", r2, "g2", g2, "rank3", r3, "g3", g3);
    }

    private void broadcastFinalRanking(List<Map.Entry<UUID, Integer>> ranking) {
        getOnlinePlayers().forEach(p -> {
            p.sendMessage(plugin.getLangManager().getPrefix() + "§6§l─── 最終順位 ───");
            int pos = 1;
            for (Map.Entry<UUID, Integer> entry : ranking) {
                String medal = switch (pos) {
                    case 1 -> "§6①";
                    case 2 -> "§7②";
                    case 3 -> "§c③";
                    default -> "§8" + pos + ".";
                };
                p.sendMessage("  " + medal + " §f" + getNameFromUuid(entry.getKey()) +
                    "  §e" + entry.getValue() + "個");
                pos++;
            }
        });
    }

    private void spawnGold() {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null || arena.getGoldSpawnPoints().isEmpty()) return;
        Random rand = new Random();
        List<Location> pts = arena.getGoldSpawnPoints();
        Location loc = pts.get(rand.nextInt(pts.size()));
        int amt = rand.nextInt(3) + 1;
        loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.GOLD_NUGGET, amt));
    }

    private void updateUI() {
        List<Player> online = getOnlinePlayers();
        String leader = getLeaderName();
        int leaderGold = getLeaderGold();

        plugin.getUiManager().getBossBarManager()
            .showGoldBar(online, arenaId, leader, leaderGold, 9999);
        plugin.getUiManager().getBossBarManager()
            .showTimerBar(online, arenaId, timeLeft, GAME_DURATION, NAME);
        online.forEach(p ->
            plugin.getUiManager().getActionBarManager().startGameActionBar(p, () -> timeLeft));
    }

    private List<Map.Entry<UUID, Integer>> getSortedRanking() {
        return goldCollected.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .collect(Collectors.toList());
    }

    private String getLeaderName() {
        return getSortedRanking().stream().findFirst()
            .map(e -> getNameFromUuid(e.getKey())).orElse("なし");
    }

    private int getLeaderGold() {
        return getSortedRanking().stream().findFirst()
            .map(Map.Entry::getValue).orElse(0);
    }

    private String getNameFromUuid(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        var wfp = plugin.getPlayerManager().getPlayer(uuid);
        return wfp != null ? wfp.getName() : "???";
    }

    private void broadcast(String key, String... args) {
        getOnlinePlayers().forEach(p ->
            p.sendMessage(plugin.getLangManager().prefixed(key, args)));
    }

    private void broadcastRaw(String key, String... args) {
        getOnlinePlayers().forEach(p ->
            p.sendMessage(plugin.getLangManager().prefixed(key, args)));
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
