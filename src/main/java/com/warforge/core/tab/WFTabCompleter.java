package com.warforge.core.tab;

import com.warforge.core.WarforgeCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.*;

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
            case "arena" -> {
                if (args.length == 1) suggestions.addAll(List.of(
                    "wand", "create", "delete", "setspawn", "setlobby", "setredspawn",
                    "setbluespawn", "setrednexus", "setbluenexus", "setheistgold",
                    "addcapture", "addgoldspawn", "list", "info", "enable", "disable"
                ));
                if (args.length == 2) {
                    switch (args[0].toLowerCase()) {
                        case "delete", "setspawn", "setlobby", "setredspawn", "setbluespawn",
                             "setrednexus", "setbluenexus", "setheistgold", "addcapture",
                             "addgoldspawn", "info", "enable", "disable" ->
                            plugin.getArenaManager().getAllArenas().forEach(a ->
                                suggestions.add(String.valueOf(a.getId())));
                    }
                }
                if (args.length == 3 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("wand"))) {
                    suggestions.addAll(List.of("tdm", "br", "domination", "goldrush", "heist"));
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
                if (args.length == 1) suggestions.addAll(List.of("save", "list"));
                if (args.length == 2 && args[0].equalsIgnoreCase("save")) {
                    suggestions.addAll(List.of("1", "2", "3"));
                }
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
