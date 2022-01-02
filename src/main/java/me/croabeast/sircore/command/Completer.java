package me.croabeast.sircore.command;

import me.croabeast.sircore.*;
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

    private List<String> resultTab(Collection<?>... lists) {
        List<String> tab = new ArrayList<>();
        for (Collection<?> list : lists)
            list.forEach(e -> tab.add((String) e));
        return StringUtil.copyPartialMatches(
                args[args.length - 1], tab, new ArrayList<>()
        );
    }

    private List<String> resultTab(String... args) {
        return resultTab(Arrays.asList(args));
    }

    private TabCompleter announcerCompleter() {
        return (sender, command, alias, args) -> {
            this.args = args;

            if (args.length == 1) return resultTab("start", "preview", "cancel", "reboot");

            if(args.length == 2 && args[0].matches("(?i)preview")) {
                ConfigurationSection id = main.getAnnounces().getConfigurationSection("messages");
                if (id == null) return resultTab("NOT_FOUND");
                else return resultTab(id.getKeys(false));
            }

            return new ArrayList<>();
        };
    }

    private TabCompleter sirCompleter() {
        return (sender, command, alias, args) -> {
            this.args = args;
            if (args.length == 1) return resultTab("reload", "help", "support");
            return new ArrayList<>();
        };
    }

    private TabCompleter printCompleter() {
        return (sender, command, alias, args) -> {
            this.args = args;

            if (args.length == 1) return resultTab("targets", "ACTION-BAR", "CHAT", "TITLE");

            if (args.length == 2 && args[0].matches("(?i)ACTION-BAR|CHAT|TITLE")) {
                return resultTab(
                        Arrays.asList("@a", "PERM:", "WORLD:", (main.getInitializer().HAS_VAULT ? "GROUP:" : null)),
                        main.everyPlayer().stream().map(Player::getName).collect(Collectors.toList())
                );
            }

            if (args.length == 3) {
                if (args[0].matches("(?i)ACTION-BAR")) return resultTab("<message>");
                else if (args[0].matches("(?i)CHAT")) return resultTab("DEFAULT", "CENTERED", "MIXED");
                else if (args[0].matches("(?i)TITLE")) return resultTab("DEFAULT", "10,50,10");
            }

            if (args.length == 4 && args[0].matches("(?i)CHAT|TITLE")) return resultTab("<message>");

            return new ArrayList<>();
        };
    }
}
