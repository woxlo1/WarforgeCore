package com.warforge.core.config;
import com.warforge.core.WarforgeCore;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
public class ConfigManager {
    private final WarforgeCore plugin;
    private FileConfiguration cfg;
    public ConfigManager(WarforgeCore plugin) { this.plugin=plugin; reload(); }
    public void reload() { plugin.reloadConfig(); this.cfg=plugin.getConfig(); }
    public int getLobbyCountdown()            { return cfg.getInt("lobby.countdown",30); }
    public int getLobbyCountdownFull()        { return cfg.getInt("lobby.countdown-full",10); }
    public List<Integer> getCountdownNotify() { return cfg.getIntegerList("lobby.countdown-notify"); }
    public int getRespawnDelay()              { return cfg.getInt("game.respawn-delay",3); }
    public int getMaxPlayersPerArena()        { return cfg.getInt("game.max-players-per-arena",16); }
    public int getMinPlayersToStart()         { return cfg.getInt("game.min-players-to-start",2); }
    public boolean isBlockBreakAllowed()      { return cfg.getBoolean("game.block-break",false); }
    public boolean isBlockPlaceAllowed()      { return cfg.getBoolean("game.block-place",false); }
    public boolean isHungerEnabled()          { return cfg.getBoolean("game.hunger",false); }
    public boolean isDropItemsOnDeath()       { return cfg.getBoolean("game.drop-items-on-death",false); }
    public boolean isKeepInventory()          { return cfg.getBoolean("game.keep-inventory",true); }
    public int getModeDuration(String mode)   { return cfg.getInt("modes."+mode+".duration", switch(mode){case "battle-royale"->600;case "gold-rush"->240;default->300;}); }
    public int getModeRespawnDelay(String mode){ return cfg.getInt("modes."+mode+".respawn-delay",3); }
    public int getTdmKillLimit()              { return cfg.getInt("modes.tdm.kill-limit",30); }
    public int getDominationMaxScore()        { return cfg.getInt("modes.domination.max-score",200); }
    public double getDominationCaptureSpeed() { return cfg.getDouble("modes.domination.capture-speed",0.05); }
    public int getDominationScorePerTick()    { return cfg.getInt("modes.domination.score-per-tick",1); }
    public int getGoldRushWinGold()           { return cfg.getInt("modes.gold-rush.win-gold",10); }
    public int getGoldRushSpawnInterval()     { return cfg.getInt("modes.gold-rush.gold-spawn-interval",20); }
    public boolean isGoldDropOnDeath()        { return cfg.getBoolean("modes.gold-rush.gold-drop-on-death",true); }
    public int getHeistWinGold()              { return cfg.getInt("modes.heist.win-gold",100); }
    public int getHeistGoldPerMine()          { return cfg.getInt("modes.heist.gold-per-mine",5); }
    public boolean isDamageEnabled()          { return cfg.getBoolean("guns.damage-enabled",true); }
    public boolean isFriendlyFire()           { return cfg.getBoolean("guns.friendly-fire",false); }
    public boolean isEconomyEnabled()         { return cfg.getBoolean("economy.enabled",true); }
    public String getCurrencySymbol()         { return cfg.getString("economy.currency-symbol","¥"); }
    public double getKillReward()             { return cfg.getDouble("economy.rewards.kill",500); }
    public double getHeadshotBonus()          { return cfg.getDouble("economy.rewards.headshot-bonus",200); }
    public double getAssistReward()           { return cfg.getDouble("economy.rewards.assist",100); }
    public double getParticipationReward()    { return cfg.getDouble("economy.rewards.participation",200); }
    public double getHeistDepositReward()     { return cfg.getDouble("economy.rewards.heist-deposit-per-gold",50); }
    public double getWinReward(String mode)   { return cfg.getDouble("economy.rewards.win."+mode.toLowerCase().replace(" ","-"),1000); }
    public boolean isScoreboardEnabled()      { return cfg.getBoolean("scoreboard.enabled",true); }
    public String getScoreboardTitle()        { return cfg.getString("scoreboard.title","&6&l✦ WarForgeCore ✦"); }
    public boolean isActionBarEnabled()       { return cfg.getBoolean("actionbar.enabled",true); }
    public String getActionBarGameFormat()    { return cfg.getString("actionbar.game-format","&f&l⏱ &e{time}  &7| &f&lKill &a{kills}  &f&lDeath &c{deaths}  &f&lKDR &6{kdr}"); }
    public String getActionBarLobbyFormat()   { return cfg.getString("actionbar.lobby-format","&f&lKill &a{kills}  &f&lDeath &c{deaths}  &f&lKDR &6{kdr}"); }
    public boolean isBossBarEnabled()         { return cfg.getBoolean("bossbar.enabled",true); }
    public String getMessage(String key)      { return cfg.getString("messages."+key,"&c[メッセージ未設定: "+key+"]"); }
    public String getMessage(String key, String... placeholders) { String msg=getMessage(key); for(int i=0;i+1<placeholders.length;i+=2) msg=msg.replace("{"+placeholders[i]+"}",placeholders[i+1]); return msg; }
    public boolean isDebug()                  { return cfg.getBoolean("settings.debug",false); }
    public String getPrefix()                 { return cfg.getString("settings.prefix","&f&l[&6&lWarForgeCore&f&l] &r"); }
}
