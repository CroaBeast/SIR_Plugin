package me.croabeast.sircore.command;

import me.croabeast.sircore.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.util.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.stream.*;

public class Completer implements TabCompleter {

    private final Application main;
    private String[] args;

    public Completer(Application main) {
        this.main = main;
        Arrays.asList("announcer", "sir", "print").forEach(s -> {
            PluginCommand completer = main.getCommand(s);
            if (completer != null) completer.setTabCompleter(this);
        });
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

    @Nullable @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        this.args = args;

        if (cmd.matches("(?i)sir") && args.length == 1) return finalTab("reload", "help", "support");

        if (cmd.matches("(?i)print")) {
            if (args.length == 1) return finalTab("targets", "ACTION-BAR", "CHAT", "TITLE");

            if (args.length == 2 && args[0].matches("(?i)ACTION-BAR|CHAT|TITLE")) {
                return finalTab(
                        Arrays.asList("@a", "PERM:", "WORLD:", (main.getInitializer().hasVault ? "GROUP:" : null)),
                        Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList())
                );
            }

            if (args.length == 3) {
                switch (args[0].toUpperCase()) {
                    case "ACTION-BAR": return finalTab("<message>");
                    case "CHAT": return finalTab("DEFAULT", "CENTERED", "MIXED");
                    case "TITLE": return finalTab("DEFAULT", "10,50,10");
                }
            }

            if (args.length == 4 && args[0].matches("(?i)CHAT|TITLE")) return finalTab("<message>");
        }

        if (cmd.matches("(?i)announcer")) {
            if (args.length == 1) return finalTab("start", "preview", "cancel", "reboot");

            if(args.length == 2 && args[1].matches("(?i)preview")){
                ConfigurationSection id = main.getAnnounces().getConfigurationSection("messages");
                if (id != null) return finalTab(new ArrayList<>(id.getKeys(false)));
            }
        }
        
        return new ArrayList<>();
    }
}
