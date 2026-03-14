package com.warforge.core;

import com.warforge.core.admin.AdminGUI;
import com.warforge.core.arena.ArenaWand;
import com.warforge.core.effect.KillEffectManager;
import com.warforge.core.i18n.LangManager;
import com.warforge.core.command.*;
import com.warforge.core.config.ConfigManager;
import com.warforge.core.data.DatabaseManager;
import com.warforge.core.economy.EconomyConfig;
import com.warforge.core.economy.VaultManager;
import com.warforge.core.killstreak.KillstreakManager;
import com.warforge.core.listener.*;
import com.warforge.core.loadout.LoadoutManager;
import com.warforge.core.log.TransactionLogger;
import com.warforge.core.manager.ArenaManager;
import com.warforge.core.manager.GameManager;
import com.warforge.core.manager.PlayerManager;
import com.warforge.core.mission.MissionManager;
import com.warforge.core.rank.RankManager;
import com.warforge.core.spectator.SpectatorManager;
import com.warforge.core.stats.StatsManager;
import com.warforge.core.tab.WFTabCompleter;
import com.warforge.core.ui.UIManager;
import com.warforge.core.vote.VoteManager;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

public class WarforgeCore extends JavaPlugin {

    private static WarforgeCore instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PlayerManager playerManager;
    private GameManager gameManager;
    private ArenaManager arenaManager;
    private UIManager uiManager;
    private VaultManager vaultManager;
    private EconomyConfig economyConfig;
    private RankManager rankManager;
    private KillstreakManager killstreakManager;
    private LoadoutManager loadoutManager;
    private StatsManager statsManager;
    private MissionManager missionManager;
    private SpectatorManager spectatorManager;
    private VoteManager voteManager;
    private AdminGUI adminGUI;
    private TransactionLogger transactionLogger;
    private LangManager langManager;
    private ArenaWand arenaWand;
    private KillEffectManager killEffectManager;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager    = new ConfigManager(this);
        this.langManager      = new LangManager(this);
        this.economyConfig    = new EconomyConfig(this);
        this.vaultManager     = new VaultManager(this);

        this.databaseManager  = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("データベース接続失敗。プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.playerManager      = new PlayerManager(this);
        this.arenaManager       = new ArenaManager(this);
        this.gameManager        = new GameManager(this);
        this.uiManager          = new UIManager(this);
        this.rankManager        = new RankManager(this);
        this.killstreakManager  = new KillstreakManager(this);
        this.loadoutManager     = new LoadoutManager(this);
        this.statsManager       = new StatsManager(this);
        this.missionManager     = new MissionManager(this);
        this.spectatorManager   = new SpectatorManager(this);
        this.voteManager        = new VoteManager(this);
        this.adminGUI           = new AdminGUI(this);
        this.transactionLogger  = new TransactionLogger(this);
        this.arenaWand          = new ArenaWand(this);
        this.killEffectManager  = new KillEffectManager(this);

        // ─── コマンド ───
        var wfCmd = getCommand("warforge");
        wfCmd.setExecutor(new WarforgeCommand(this));
        wfCmd.setTabCompleter(new WFTabCompleter(this, "warforge"));

        var arenaCmd = getCommand("arena");
        arenaCmd.setExecutor(new ArenaCommand(this));
        arenaCmd.setTabCompleter(new WFTabCompleter(this, "arena"));

        var joinCmd = getCommand("join");
        joinCmd.setExecutor(new JoinCommand(this, false));
        joinCmd.setTabCompleter(new WFTabCompleter(this, "join"));

        getCommand("leave").setExecutor(new JoinCommand(this, true));

        var statsCmd = getCommand("stats");
        statsCmd.setExecutor(new StatsCommand(this));
        statsCmd.setTabCompleter(new WFTabCompleter(this, "stats"));

        var rankCmd = getCommand("rank");
        rankCmd.setExecutor(new RankCommand(this));

        var loadoutCmd = getCommand("loadout");
        loadoutCmd.setExecutor(new LoadoutCommand(this));
        loadoutCmd.setTabCompleter(new WFTabCompleter(this, "loadout"));

        var missionCmd = getCommand("mission");
        missionCmd.setExecutor(new MissionCommand(this));
        missionCmd.setTabCompleter(new WFTabCompleter(this, "mission"));

        var voteCmd = getCommand("vote");
        voteCmd.setExecutor(new VoteCommand(this));
        voteCmd.setTabCompleter(new WFTabCompleter(this, "vote"));

        var spectateCmd = getCommand("spectate");
        spectateCmd.setExecutor(new SpectateCommand(this));


        // ─── リスナー ───
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new HeistListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(adminGUI, this);
        getServer().getPluginManager().registerEvents(arenaWand, this);

        PacketEvents.getAPI().init();

        getLogger().info("WarforgeCore v" + getDescription().getVersion() + " 起動完了！");
        if (vaultManager.isEnabled())
            getLogger().info("Vault: " + vaultManager.getEconomy().getName());
    }

    @Override
    public void onDisable() {
        if (spectatorManager != null) spectatorManager.shutdown();
        if (uiManager != null) uiManager.shutdown();
        if (gameManager != null) gameManager.shutdown();
        if (databaseManager != null) databaseManager.disconnect();
        PacketEvents.getAPI().terminate();
        getLogger().info("WarforgeCore シャットダウン完了。");
    }

    public static WarforgeCore getInstance()              { return instance; }
    public ConfigManager getConfigManager()               { return configManager; }
    public DatabaseManager getDatabaseManager()           { return databaseManager; }
    public PlayerManager getPlayerManager()               { return playerManager; }
    public GameManager getGameManager()                   { return gameManager; }
    public ArenaManager getArenaManager()                 { return arenaManager; }
    public UIManager getUiManager()                       { return uiManager; }
    public VaultManager getVaultManager()                 { return vaultManager; }
    public EconomyConfig getEconomyConfig()               { return economyConfig; }
    public RankManager getRankManager()                   { return rankManager; }
    public KillstreakManager getKillstreakManager()       { return killstreakManager; }
    public LoadoutManager getLoadoutManager()             { return loadoutManager; }
    public StatsManager getStatsManager()                 { return statsManager; }
    public MissionManager getMissionManager()             { return missionManager; }
    public SpectatorManager getSpectatorManager()         { return spectatorManager; }
    public VoteManager getVoteManager()                   { return voteManager; }
    public AdminGUI getAdminGUI()                         { return adminGUI; }
    public TransactionLogger getTransactionLogger()       { return transactionLogger; }
    public LangManager getLangManager()                   { return langManager; }
    public ArenaWand getArenaWand()                       { return arenaWand; }
    public KillEffectManager getKillEffectManager()       { return killEffectManager; }
}
