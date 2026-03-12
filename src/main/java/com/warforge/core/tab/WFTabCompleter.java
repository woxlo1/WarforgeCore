package com.warforge.core.tab;

import com.warforge.core.WarforgeCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class WFTabCompleter implements TabCompleter {

    private final WarforgeCore plugin;
    private final String type;

    public WFTabCompleter(WarforgeCore plugin, String type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        switch (type) {
            case "warforge" -> {
                if (args.length == 1) suggestions.addAll(List.of("reload", "stats", "admin"));
                if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
                    Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));
                }
            }
            case "gun" -> {
                if (args.length == 1) suggestions.addAll(List.of("give", "list", "info", "reload"));
                if (args.length == 2 && (args[0].equalsIgnoreCase("give"))) {
                    Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));
                }
                if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                    suggestions.addAll(plugin.getGunManager().getAllGuns().keySet());
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
                    suggestions.addAll(plugin.getGunManager().getAllGuns().keySet());
                }
            }
            case "arena" -> {
                if (args.length == 1) suggestions.addAll(List.of(
                    "wand", "create", "delete", "setspawn", "setlobby", "setredspawn",
                    "setbluespawn", "setrednexus", "setbluenexus", "setheistgold",
                    "addcapture", "addgoldspawn", "list", "info", "enable", "disable", "test"
                ));
                if (args.length == 2) {
                    switch (args[0].toLowerCase()) {
                        case "create" -> { /* arena name */ }
                        case "delete", "setspawn", "setlobby", "setredspawn", "setbluespawn",
                             "setrednexus", "setbluenexus", "setheistgold", "addcapture",
                             "addgoldspawn", "info", "enable", "disable", "test" ->
                            plugin.getArenaManager().getAllArenas().forEach(a ->
                                suggestions.add(String.valueOf(a.getId())));
                    }
                }
                if (args.length == 3 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("wand"))) {
                    suggestions.addAll(List.of("tdm", "br", "domination", "goldrush", "heist"));
                }
            }
            case "auction" -> {
                if (args.length == 1) suggestions.addAll(List.of("sell", "bid", "buy", "cancel", "list"));
                if (args.length == 2 && (args[0].equalsIgnoreCase("bid") ||
                    args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("cancel"))) {
                    plugin.getAuctionManager().getActiveAuctions().keySet()
                        .forEach(id -> suggestions.add(String.valueOf(id)));
                }
                if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
                    suggestions.addAll(List.of("500", "1000", "2000", "5000", "10000"));
                }
            }
            case "join" -> {
                plugin.getArenaManager().getAllArenas().stream()
                    .filter(a -> a.getEnabled())
                    .forEach(a -> {
                        suggestions.add(String.valueOf(a.getId()));
                        suggestions.add(a.getName());
                    });
            }
            case "stats" -> {
                if (args.length <= 1) {
                    Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));
                }
            }
            case "loadout" -> {
                if (args.length == 1) suggestions.addAll(List.of("save", "load", "list"));
                if (args.length == 2) suggestions.addAll(List.of("1", "2", "3"));
            }
            case "vote" -> {
                if (args.length == 1) {
                    plugin.getArenaManager().getAllArenas().forEach(a ->
                        suggestions.add(String.valueOf(a.getId())));
                }
            }
            case "mission" -> {
                if (args.length == 1) suggestions.addAll(List.of("list", "today"));
            }
        }

        return filter(suggestions, args.length > 0 ? args[args.length - 1] : "");
    }

    private List<String> filter(List<String> list, String input) {
        return StringUtil.copyPartialMatches(input, list, new ArrayList<>());
    }
}
