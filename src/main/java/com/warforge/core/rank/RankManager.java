package com.warforge.core.rank;

import com.warforge.core.WarforgeCore;
import com.warforge.core.compat.VersionAdapter;
import com.warforge.core.player.WFPlayer;
import com.warforge.core.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.UUID;

public class RankManager {

    private final WarforgeCore plugin;
    private int winPoints, lossPoints, killPoints, mvpBonus;

    public RankManager(WarforgeCore plugin) { this.plugin = plugin; reload(); }

    public void reload() {
        var cfg = plugin.getConfig();
        winPoints  = cfg.getInt("rank.points.win",       25);
        lossPoints = cfg.getInt("rank.points.loss",      -15);
        killPoints = cfg.getInt("rank.points.kill",        3);
        mvpBonus   = cfg.getInt("rank.points.mvp-bonus",  10);
    }

    public void onGameEnd(UUID uuid, boolean won, int kills, boolean isMvp) {
        WFPlayer wfp = plugin.getPlayerManager().getPlayer(uuid);
        if (wfp == null) return;
        int before = wfp.getRankPoints();
        RankTier tierBefore = RankTier.Companion.fromPoints(before);
        int delta = (won ? winPoints : lossPoints) + (kills * killPoints) + (isMvp ? mvpBonus : 0);
        int after = Math.max(0, before + delta);
        wfp.setRankPoints(after);
        RankTier tierAfter = RankTier.Companion.fromPoints(after);
        plugin.getPlayerManager().savePlayer(wfp);
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        String sign = delta >= 0 ? VersionAdapter.color("&a+") : VersionAdapter.color("&c");
        player.sendMessage(Messages.INSTANCE.prefixed(
                "&7ランクポイント: " + sign + delta + VersionAdapter.color(" &8(") + after + VersionAdapter.color("pt) ") + tierAfter.formatted()));
        if (!tierAfter.name().equals(tierBefore.name())) {
            boolean promoted = after > before && tierAfter.getMinPoints() > tierBefore.getMinPoints();
            if (promoted) showPromotion(player, tierAfter);
            else          showDemotion(player, tierAfter);
        }
    }

    private void showPromotion(Player player, RankTier tier) {
        VersionAdapter.sendTitle(player, "&a&l🎉 RANK UP!", tier.formatted(), 10, 60, 20);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        Bukkit.getOnlinePlayers().forEach(p ->
                p.sendMessage(Messages.INSTANCE.prefixed("&6" + player.getName() + " &7が " + tier.formatted() + " &7に昇格！🎊")));
    }

    private void showDemotion(Player player, RankTier tier) {
        VersionAdapter.sendTitle(player, "&c&l▼ RANK DOWN", tier.formatted(), 10, 60, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
        player.sendMessage(Messages.INSTANCE.prefixed("&c" + tier.formatted() + " &cに降格しました。"));
    }

    public RankTier getRank(UUID uuid) {
        WFPlayer wfp = plugin.getPlayerManager().getPlayer(uuid);
        return wfp != null ? RankTier.Companion.fromPoints(wfp.getRankPoints()) : RankTier.BRONZE;
    }

    public String getRankPrefix(UUID uuid) { return getRank(uuid).formatted(); }
}
