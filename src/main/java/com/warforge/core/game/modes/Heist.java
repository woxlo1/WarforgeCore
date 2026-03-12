package com.warforge.core.game.modes;

import com.warforge.core.WarforgeCore;
import com.warforge.core.arena.Arena;
import com.warforge.core.game.GameMode;
import com.warforge.core.compat.VersionAdapter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Heist - 敵ネクサスを掘って自陣に持ち帰るチーム戦
 * 修正ポイント:
 * - アリーナ重複変数バグ修正
 * - 銃インベントリを保存→リストア対応
 * - キル時ドロップ量を設定可能（全額ではなく割合）
 * - リスポーン時ロードアウト自動復元
 */
public class Heist extends GameMode {

    public static final String NAME = "Heist";

    private final int GAME_DURATION;
    private final int WIN_GOLD;
    private final int GOLD_PER_MINE;
    private final double KILL_DROP_RATE; // キル時のドロップ割合

    private int redScore = 0;
    private int blueScore = 0;

    private final Map<UUID, Integer> carrying = new HashMap<>();
    private final Map<UUID, TeamDeathmatch.Team> playerTeams = new HashMap<>();

    // 銃インベントリ保存（gold持ち中に使えなくするため）
    private final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();

    private Location redNexus;
    private Location blueNexus;

    private BukkitTask tickTask;

    public Heist(WarforgeCore plugin, int arenaId) {
        super(plugin, arenaId);
        Arena arena = plugin.getArenaManager().getArena(arenaId);

        this.GAME_DURATION  = plugin.getConfigManager().getModeDuration("heist");
        this.WIN_GOLD       = arena != null && arena.getHeistWinGold() > 0
            ? arena.getHeistWinGold()    : plugin.getConfigManager().getHeistWinGold();
        this.GOLD_PER_MINE  = arena != null && arena.getHeistGoldPerMine() > 0
            ? arena.getHeistGoldPerMine() : plugin.getConfigManager().getHeistGoldPerMine();
        this.KILL_DROP_RATE = plugin.getConfig().getDouble("modes.heist.kill-drop-rate", 0.65);
        this.timeLeft       = GAME_DURATION;

        if (arena != null) {
            redNexus  = arena.getRedNexus();
            blueNexus = arena.getBlueNexus();
        }
    }

    @Override
    public void start() {
        state = GameState.IN_GAME;
        redScore = 0; blueScore = 0;
        carrying.clear();
        assignTeams();

        // バリデーション
        if (redNexus == null || blueNexus == null) {
            broadcast("&c[エラー] ネクサスが設定されていません。/arena setrednexus / setbluenexus で設定してください。");
            end(); return;
        }

        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        plugin.getLangManager();
        broadcast("&6&lHeist 開始！ &7相手のネクサスを掘って &e" + WIN_GOLD + "ゴールド &7を持ち帰れ！");
        broadcast("&7ルール: ネクサスを右クリック→採掘 / 自陣ネクサスに右クリック→持ち帰り");
        updateUI();
    }

    @Override
    public void end() {
        state = GameState.ENDING;
        if (tickTask != null) tickTask.cancel();

        // 全員グロー解除・インベントリ復元
        getOnlinePlayers().forEach(p -> {
            p.setGlowing(false);
            restoreInventory(p);
        });

        String winnerTeamMsg;
        TeamDeathmatch.Team winnerTeam;
        if (redScore >= WIN_GOLD)        { winnerTeamMsg = "&cレッドチーム"; winnerTeam = TeamDeathmatch.Team.RED; }
        else if (blueScore >= WIN_GOLD)  { winnerTeamMsg = "&9ブルーチーム"; winnerTeam = TeamDeathmatch.Team.BLUE; }
        else if (redScore > blueScore)   { winnerTeamMsg = "&cレッドチーム"; winnerTeam = TeamDeathmatch.Team.RED; }
        else if (blueScore > redScore)   { winnerTeamMsg = "&9ブルーチーム"; winnerTeam = TeamDeathmatch.Team.BLUE; }
        else                              { winnerTeamMsg = "&e引き分け";     winnerTeam = null; }

        // 勝利チームに報酬
        if (winnerTeam != null) {
            final TeamDeathmatch.Team wt = winnerTeam;
            playerTeams.entrySet().stream()
                .filter(e -> e.getValue() == wt)
                .forEach(e -> {
                    plugin.getRankManager().onGameEnd(e.getKey(), true, 0, false);
                    if (plugin.getVaultManager().isEnabled()) {
                        Player p = Bukkit.getPlayer(e.getKey());
                        if (p != null) plugin.getVaultManager().reward(p,
                            plugin.getEconomyConfig().getWinReward("heist"), "Heist勝利");
                    }
                });
        }

        getOnlinePlayers().forEach(p -> {
            VersionAdapter.sendTitle(p, winnerTeamMsg + " &f勝利！", "&cRed: " + redScore + " &7| &9Blue: " + blueScore + " &7| 目標: " + WIN_GOLD, 10, 80, 20);
            plugin.getUiManager().getBossBarManager().clearBars(getOnlinePlayers(), arenaId);
            plugin.getUiManager().getScoreboardManager().clearScoreboard(p);
        });

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            getOnlinePlayers().forEach(this::removePlayer);
            state = GameState.WAITING;
            plugin.getGameManager().onGameEnd(arenaId);
        }, 120L);
    }

    @Override
    public void onKill(Player killer, Player victim) {
        UUID victimUuid = victim.getUniqueId();
        int droppedGold = carrying.getOrDefault(victimUuid, 0);

        if (droppedGold > 0) {
            // ドロップ率：設定値（デフォルト65%）をキラーが取得、残りは消失
            double rate = KILL_DROP_RATE + (Math.random() * 0.1);
            int killerGets = (int) Math.ceil(droppedGold * rate);
            int lost       = droppedGold - killerGets;

            carrying.put(victimUuid, 0);

            // キラーのキャリーに追加（または直接チームスコアへ）
            TeamDeathmatch.Team killerTeam = playerTeams.get(killer.getUniqueId());
            if (killerTeam == TeamDeathmatch.Team.RED) redScore += killerGets;
            else blueScore += killerGets;

            // 被キルプレイヤーのインベントリ復元
            restoreInventory(victim);
            victim.setGlowing(false);

            // エフェクト
            plugin.getKillEffectManager().playGoldDropEffect(victim, droppedGold);

            broadcast("&f" + killer.getName() + " &7が &f" + victim.getName() + " &7を倒し &e" +
                killerGets + "ゴールド &7を奪った！ &8(" + lost + "G消失)");
        } else {
            broadcast("&f" + killer.getName() + " &7が &f" + victim.getName() + " &7を倒した！");
        }

        plugin.getKillEffectManager().playKillEffect(killer, victim);
        plugin.getMissionManager().onKill(killer);
        plugin.getKillstreakManager().onKill(killer);
        updateUI();
        if (checkWinCondition()) end();
    }

    @Override
    public void onDeath(Player victim) {
        int delay = plugin.getConfigManager().getModeRespawnDelay("heist");
        carrying.put(victim.getUniqueId(), 0);
        restoreInventory(victim);
        victim.setGlowing(false);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!players.contains(victim.getUniqueId())) return;
            victim.setHealth(20.0);
            victim.setFoodLevel(20);
            Location spawn = getTeamSpawn(playerTeams.get(victim.getUniqueId()));
            if (spawn != null) victim.teleport(spawn);
            plugin.getLangManager().send(victim, "game.respawn");
        }, delay * 20L);
    }

    @Override
    public void tick() {
        if (timeLeft <= 0) { end(); return; }
        timeLeft--;
        updateUI();
        // グロー中プレイヤーのコンパス更新
        updateCarrierGlow();
    }

    @Override
    public boolean checkWinCondition() {
        return redScore >= WIN_GOLD || blueScore >= WIN_GOLD || timeLeft <= 0;
    }

    /** ネクサス採掘（HeistListenerから呼ぶ） */
    public void onNexusMine(Player miner, TeamDeathmatch.Team nexusTeam) {
        TeamDeathmatch.Team minerTeam = playerTeams.get(miner.getUniqueId());
        if (minerTeam == null) return;

        if (minerTeam == nexusTeam) {
            plugin.getLangManager().send(miner, "heist.own-nexus");
            return;
        }

        // インベントリを保存してから銃を取り上げ
        if (carrying.getOrDefault(miner.getUniqueId(), 0) == 0) {
            saveInventory(miner);
        }

        int current = carrying.merge(miner.getUniqueId(), GOLD_PER_MINE, Integer::sum);

        // エフェクト
        miner.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,
            miner.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
        miner.playSound(miner.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);

        VersionAdapter.sendActionBar(miner, plugin.getLangManager().get("heist.mine", "amount", String.valueOf(current)));
        miner.setGlowing(true);
        updateUI();
    }

    /** 自陣ネクサスに持ち帰り（HeistListenerから呼ぶ） */
    public void onNexusDeposit(Player player) {
        TeamDeathmatch.Team team = playerTeams.get(player.getUniqueId());
        if (team == null) return;

        int gold = carrying.getOrDefault(player.getUniqueId(), 0);
        if (gold <= 0) {
            plugin.getLangManager().send(player, "heist.no-gold");
            return;
        }

        if (team == TeamDeathmatch.Team.RED) redScore += gold;
        else blueScore += gold;

        carrying.put(player.getUniqueId(), 0);
        restoreInventory(player);
        player.setGlowing(false);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        plugin.getLangManager().send(player, "heist.deposit", "amount", String.valueOf(gold));

        // Vault報酬
        if (plugin.getVaultManager().isEnabled()) {
            double rewardPerGold = plugin.getEconomyConfig().getHeistDepositRewardPerGold();
            plugin.getVaultManager().reward(player, gold * rewardPerGold, "Heistデポジット");
        }

        broadcast(plugin.getLangManager().get("heist.score",
            "red", String.valueOf(redScore),
            "blue", String.valueOf(blueScore),
            "goal", String.valueOf(WIN_GOLD)));

        updateUI();
        if (checkWinCondition()) end();
    }

    /** 銃インベントリ保存 */
    private void saveInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents().clone();
        savedInventory.put(player.getUniqueId(), contents);
        // 銃だけ取り上げ
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (plugin.getGunItemManager().isGun(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    /** 銃インベントリ復元 */
    private void restoreInventory(Player player) {
        ItemStack[] saved = savedInventory.remove(player.getUniqueId());
        if (saved == null) return;
        player.getInventory().setContents(saved);
    }

    private void updateCarrierGlow() {
        getOnlinePlayers().forEach(p -> {
            boolean carrying = this.carrying.getOrDefault(p.getUniqueId(), 0) > 0;
            if (carrying != p.isGlowing()) p.setGlowing(carrying);
        });
    }

    private void updateUI() {
        List<Player> online = getOnlinePlayers();
        plugin.getUiManager().getBossBarManager().showTimerBar(online, arenaId, timeLeft, GAME_DURATION, NAME);

        // Heistスコアバー
        String scoreText = "§cRed: " + redScore + " §7| §9Blue: " + blueScore + " §7| 目標: §f" + WIN_GOLD;
        online.forEach(p -> {
            TeamDeathmatch.Team team = playerTeams.getOrDefault(p.getUniqueId(), TeamDeathmatch.Team.RED);
            String teamInfo = team == TeamDeathmatch.Team.RED
                ? "&cRed " + redScore : "&9Blue " + blueScore;
            plugin.getUiManager().getScoreboardManager().showGameScoreboard(p, NAME, teamInfo);
            plugin.getUiManager().getActionBarManager().startGameActionBar(p, () -> timeLeft);
        });
    }

    private void assignTeams() {
        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        for (int i = 0; i < shuffled.size(); i++) {
            playerTeams.put(shuffled.get(i),
                i % 2 == 0 ? TeamDeathmatch.Team.RED : TeamDeathmatch.Team.BLUE);
        }
    }

    private Location getTeamSpawn(TeamDeathmatch.Team team) {
        Arena arena = plugin.getArenaManager().getArena(arenaId);
        if (arena == null) return null;
        List<Location> spawns = team == TeamDeathmatch.Team.RED
            ? arena.getRedSpawnPoints() : arena.getBlueSpawnPoints();
        if (spawns.isEmpty()) return plugin.getArenaManager().getRandomSpawn(arenaId);
        return spawns.get(new Random().nextInt(spawns.size()));
    }

    private void broadcast(String msg) {
        getOnlinePlayers().forEach(p ->
            p.sendMessage(plugin.getLangManager().getPrefix() + msg));
    }

    private List<Player> getOnlinePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) result.add(p);
        }
        return result;
    }

    public TeamDeathmatch.Team getTeam(UUID uuid) { return playerTeams.get(uuid); }
    public Location getRedNexus()  { return redNexus; }
    public Location getBlueNexus() { return blueNexus; }
    public int getCarrying(UUID uuid) { return carrying.getOrDefault(uuid, 0); }
}
