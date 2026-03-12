package com.warforge.core.listener;

import com.warforge.core.WarforgeCore;
import com.warforge.core.game.GameMode;
import com.warforge.core.game.modes.Heist;
import com.warforge.core.game.modes.TeamDeathmatch;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class HeistListener implements Listener {

    private final WarforgeCore plugin;

    public HeistListener(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    /**
     * ネクサスを掘る（BlockDamageEvent = ダメージを与えるたび発火）
     * ネクサスは破壊されない＝BlockBreakをキャンセル
     */
    @EventHandler
    public void onNexusMine(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        Heist heist = getHeistGame(player);
        if (heist == null) return;

        // 赤ネクサスを掘ってる？
        if (isSameLocation(loc, heist.getRedNexus())) {
            event.setCancelled(false); // ダメージエフェクトは出す
            heist.onNexusMine(player, TeamDeathmatch.Team.RED);
            return;
        }
        // 青ネクサスを掘ってる？
        if (isSameLocation(loc, heist.getBlueNexus())) {
            event.setCancelled(false);
            heist.onNexusMine(player, TeamDeathmatch.Team.BLUE);
        }
    }

    /**
     * ネクサスブロックは絶対に壊させない
     */
    @EventHandler
    public void onNexusBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        Heist heist = getHeistGame(player);
        if (heist == null) return;

        if (isSameLocation(loc, heist.getRedNexus()) ||
            isSameLocation(loc, heist.getBlueNexus())) {
            event.setCancelled(true); // 破壊は絶対キャンセル
        }
    }

    /**
     * 自陣ネクサスに右クリック → デポジット
     */
    @EventHandler
    public void onNexusDeposit(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Location loc = event.getClickedBlock().getLocation();

        Heist heist = getHeistGame(player);
        if (heist == null) return;

        TeamDeathmatch.Team team = heist.getTeam(player.getUniqueId());
        if (team == null) return;

        // 自陣ネクサスかチェック
        Location myNexus = team == TeamDeathmatch.Team.RED
            ? heist.getRedNexus() : heist.getBlueNexus();

        if (isSameLocation(loc, myNexus)) {
            event.setCancelled(true);
            heist.onNexusDeposit(player);
        }
    }

    private Heist getHeistGame(Player player) {
        return plugin.getGameManager().getActiveGames().values().stream()
            .filter(g -> g instanceof Heist)
            .filter(g -> g.getPlayers().contains(player.getUniqueId()))
            .filter(g -> g.getState() == GameMode.GameState.IN_GAME)
            .map(g -> (Heist) g)
            .findFirst()
            .orElse(null);
    }

    private boolean isSameLocation(Location a, Location b) {
        if (a == null || b == null) return false;
        return a.getBlockX() == b.getBlockX()
            && a.getBlockY() == b.getBlockY()
            && a.getBlockZ() == b.getBlockZ()
            && a.getWorld().equals(b.getWorld());
    }
}
