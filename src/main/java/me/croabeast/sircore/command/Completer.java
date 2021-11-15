package me.croabeast.sircore.command;

import me.croabeast.sircore.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.util.*;

import java.util.*;
import java.util.stream.*;

public class Completer {

    private final Application main;
    private String[] args;

    public Completer(Application main) {
        this.main = main;

        registerCmptr("announcer", announcerCompleter());
        registerCmptr("sir", sirCompleter());
        registerCmptr("print", printCompleter());
    }

    private void registerCmptr(String name, TabCompleter completer) {
        PluginCommand cmd = main.getCommand(name);
        if (cmd != null) cmd.setTabCompleter(completer);
    }

    private List<String> finalTab(List<?>... lists) {
        List<String> tab = new ArrayList<>();
        String arg = args[args.length - 1];
        for (List<?> list : lists) list.forEach(e -> tab.add((String) e));
        return StringUtil.copyPartialMatches(arg, tab, new ArrayList<>());
    }

    private List<String> finalTab(String... args) {
        return finalTab(Arrays.asList(args));
    }

    private TabCompleter announcerCompleter() {
        return (sender, command, alias, args) -> {
            this.args = args;

            if (args.length == 1) return finalTab("start", "preview", "cancel", "reboot");

            if(args.length == 2 && args[1].matches("(?i)preview")){
                ConfigurationSection id = main.getAnnounces().getConfigurationSection("messages");
                if (id != null) return finalTab(new ArrayList<>(id.getKeys(false)));
            }

            return new ArrayList<>();
        };
    }

    private TabCompleter sirCompleter() {
        return (sender, command, alias, args) -> {
            this.args = args;
            if (args.length == 1) return finalTab("reload", "help", "support");
            return new ArrayList<>();
        };
    }

    private TabCompleter printCompleter() {
        return (sender, command, alias, args) -> {
            this.args = args;

            if (args.length == 1) return finalTab("targets", "ACTION-BAR", "CHAT", "TITLE");

            if (args.length == 2 && args[0].matches("(?i)ACTION-BAR|CHAT|TITLE")) {
                return finalTab(
                        Arrays.asList("@a", "PERM:", "WORLD:", (main.getInitializer().HAS_VAULT ? "GROUP:" : null)),
                        Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList())
                );
            }

            if (args.length == 3) {
                if (args[0].matches("(?i)ACTION-BAR")) return finalTab("<message>");
                else if (args[0].matches("(?i)CHAT")) return finalTab("DEFAULT", "CENTERED", "MIXED");
                else if (args[0].matches("(?i)TITLE")) return finalTab("DEFAULT", "10,50,10");
            }

            if (args.length == 4 && args[0].matches("(?i)CHAT|TITLE")) return finalTab("<message>");

            return new ArrayList<>();
        };
    }
}
