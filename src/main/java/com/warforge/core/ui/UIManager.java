package com.warforge.core.ui;

import com.warforge.core.WarforgeCore;

public class UIManager {

    private final ScoreboardManager scoreboardManager;
    private final ActionBarManager actionBarManager;
    private final BossBarManager bossBarManager;

    public UIManager(WarforgeCore plugin) {
        this.scoreboardManager = new ScoreboardManager(plugin);
        this.actionBarManager = new ActionBarManager(plugin);
        this.bossBarManager = new BossBarManager(plugin);
    }

    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }

    public void shutdown() {
        actionBarManager.stopAll();
    }
}
