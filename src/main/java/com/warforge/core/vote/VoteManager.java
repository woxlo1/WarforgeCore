package com.warforge.core.vote;

import com.warforge.core.WarforgeCore;
import com.warforge.core.arena.Arena;
import com.warforge.core.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class VoteManager {

    private final WarforgeCore plugin;
    // arenaId → 投票候補マップ (候補arenaId → 票数)
    private final Map<Integer, Map<Integer, Integer>> votes = new HashMap<>();
    // arenaId → 投票済みプレイヤー
    private final Map<Integer, Set<UUID>> voted = new HashMap<>();
    private final Map<Integer, BukkitTask> voteTimers = new HashMap<>();

    public VoteManager(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    /** ロビーでの投票を開始（candidates: 投票できるアリーナIDリスト） */
    public void startVote(int lobbyArenaId, List<Integer> candidates, int durationSec) {
        Map<Integer, Integer> voteMap = new LinkedHashMap<>();
        candidates.forEach(id -> voteMap.put(id, 0));
        votes.put(lobbyArenaId, voteMap);
        voted.put(lobbyArenaId, new HashSet<>());

        // 全員に投票を告知
        broadcastToLobby(lobbyArenaId,
            "&6[マップ投票] &f次のマップを投票で決定！ &7(/vote <ID>)");
        candidates.forEach(id -> {
            Arena arena = plugin.getArenaManager().getArena(id);
            if (arena != null) {
                broadcastToLobby(lobbyArenaId,
                    "  &e/vote " + id + " &7→ &f" + arena.getName() + " (" + arena.getMode().toUpperCase() + ")");
            }
        });

        // タイマー
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endVote(lobbyArenaId);
        }, durationSec * 20L);
        voteTimers.put(lobbyArenaId, task);
    }

    public boolean vote(Player player, int lobbyArenaId, int candidateArenaId) {
        Map<Integer, Integer> voteMap = votes.get(lobbyArenaId);
        if (voteMap == null) {
            player.sendMessage(Messages.INSTANCE.prefixed("&c現在投票は受け付けていません。"));
            return false;
        }
        if (!voteMap.containsKey(candidateArenaId)) {
            player.sendMessage(Messages.INSTANCE.prefixed("&c無効な候補IDです。"));
            return false;
        }
        if (voted.get(lobbyArenaId).contains(player.getUniqueId())) {
            player.sendMessage(Messages.INSTANCE.prefixed("&cすでに投票済みです。"));
            return false;
        }

        voteMap.merge(candidateArenaId, 1, Integer::sum);
        voted.get(lobbyArenaId).add(player.getUniqueId());

        Arena arena = plugin.getArenaManager().getArena(candidateArenaId);
        broadcastToLobby(lobbyArenaId,
            "&f" + player.getName() + " &7が &e" +
            (arena != null ? arena.getName() : candidateArenaId) + " &7に投票！");

        showCurrentVotes(lobbyArenaId);
        return true;
    }

    private void endVote(int lobbyArenaId) {
        Map<Integer, Integer> voteMap = votes.get(lobbyArenaId);
        if (voteMap == null) return;

        // 最多票を獲得したアリーナを選ぶ
        int winner = voteMap.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(-1);

        Arena winArena = winner != -1 ? plugin.getArenaManager().getArena(winner) : null;
        String winName = winArena != null ? winArena.getName() : "???";

        broadcastToLobby(lobbyArenaId,
            "&6[投票結果] &f" + winName + " &7が選ばれました！");

        // 投票データクリア
        votes.remove(lobbyArenaId);
        voted.remove(lobbyArenaId);
        voteTimers.remove(lobbyArenaId);
    }

    private void showCurrentVotes(int lobbyArenaId) {
        Map<Integer, Integer> voteMap = votes.get(lobbyArenaId);
        if (voteMap == null) return;

        StringBuilder sb = new StringBuilder("&7現在の票数: ");
        voteMap.forEach((id, count) -> {
            Arena arena = plugin.getArenaManager().getArena(id);
            String name = arena != null ? arena.getName() : String.valueOf(id);
            sb.append("&f").append(name).append(" &e").append(count).append("票  ");
        });
        broadcastToLobby(lobbyArenaId, sb.toString());
    }

    private void broadcastToLobby(int arenaId, String msg) {
        var lobby = plugin.getGameManager().getLobbies().get(arenaId);
        if (lobby == null) return;
        lobby.getWaitingPlayers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(Messages.INSTANCE.prefixed(msg));
        });
    }

    public boolean hasActiveVote(int arenaId) { return votes.containsKey(arenaId); }
}
