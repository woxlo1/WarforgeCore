package com.warforge.core.economy;

import com.warforge.core.WarforgeCore;
import com.warforge.core.util.Messages;

public class EconomyConfig {

    private final WarforgeCore plugin;

    public EconomyConfig(WarforgeCore plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // ConfigManagerのreload()で一括処理
    }

    public String formatReward(double amount, String reason) {
        return Messages.INSTANCE.prefixed(
            "&a+" + VaultManager.formatYen(amount) + " &7(" + reason + ")"
        );
    }

    public double getKillReward()            { return plugin.getConfigManager().getKillReward(); }
    public double getHeadshotBonus()         { return plugin.getConfigManager().getHeadshotBonus(); }
    public double getAssistReward()          { return plugin.getConfigManager().getAssistReward(); }
    public double getParticipationReward()   { return plugin.getConfigManager().getParticipationReward(); }
    public double getHeistDepositRewardPerGold() { return plugin.getConfigManager().getHeistDepositReward(); }
    public double getWinReward(String mode)  { return plugin.getConfigManager().getWinReward(mode); }
}
