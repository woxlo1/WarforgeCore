package com.warforge.core.economy;

import com.warforge.core.WarforgeCore;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.NumberFormat;
import java.util.Locale;

public class VaultManager {

    private final WarforgeCore plugin;
    private Economy economy;
    private boolean enabled = false;

    public VaultManager(WarforgeCore plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vaultが見つかりません。経済機能は無効です。");
            return;
        }
        RegisteredServiceProvider<Economy> rsp =
            plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("経済プラグインが見つかりません。Vault連携無効。");
            return;
        }
        economy = rsp.getProvider();
        enabled = true;
        plugin.getLogger().info("Vault連携成功！ (" + economy.getName() + ")");
    }

    /** 報酬を付与 */
    public boolean reward(Player player, double amount, String reason) {
        if (!enabled || economy == null) return false;
        economy.depositPlayer(player, amount);
        player.sendMessage(
            plugin.getEconomyConfig().formatReward(amount, reason)
        );
        return true;
    }

    /** 残高確認 */
    public double getBalance(OfflinePlayer player) {
        if (!enabled || economy == null) return 0;
        return economy.getBalance(player);
    }

    /** 円表示フォーマット */
    public static String formatYen(double amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.JAPAN);
        return "¥" + nf.format((long) amount);
    }

    public boolean isEnabled() { return enabled; }
    public Economy getEconomy() { return economy; }
}
