package me.croabeast.sircore.command;

import me.croabeast.sircore.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.util.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.stream.Collectors;

public class Completer implements TabCompleter {

    private final Application main;
    private String[] args;

    public Completer(Application main) {
        this.main = main;
        Arrays.asList("sir", "print").forEach(s -> {
            PluginCommand cmd = main.getCommand(s);
            if (cmd == null) return;
            cmd.setTabCompleter(this);
        });
    }

    /**
     * Creates a new list with partial copies
     * of all the lists' values you input.
     * @param lists the lists you input
     * @return the final list of elements
     */
    private List<String> finalTab(List<?>... lists) {
        List<String> tab = new ArrayList<>();
        String arg = args[args.length - 1];
        for (List<?> list : lists) list.forEach(e -> tab.add((String) e));
        return StringUtil.copyPartialMatches(arg, tab, new ArrayList<>());
    }

    @Nullable @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        this.args = args;

        if (cmd.matches("(?i)sir")) {
            if (args.length == 1) return finalTab(Arrays.asList("reload", "help", "support"));
            if (args.length > 1) return new ArrayList<>();
        }

        if (cmd.matches("(?i)print")) {
            if (args.length == 1) return finalTab(Arrays.asList("targets", "ACTION-BAR", "CHAT", "TITLE"));

            if (args.length == 2) {
                switch (args[0].toUpperCase()) {
                    case "ACTION-BAR": case "CHAT": case "TITLE":
                        return finalTab(
                                Arrays.asList("@a", "PERM:", "WORLD:", (main.getInitializer().hasVault ? "GROUP:" : null)),
                                Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList())
                        );
                    case "targets": default: return new ArrayList<>();
                }
            }

            if (args.length == 3) {
                switch (args[0].toUpperCase()) {
                    case "targets": default: return new ArrayList<>();
                    case "ACTION-BAR": return finalTab(Collections.singletonList("<message>"));
                    case "CHAT": return finalTab(Arrays.asList("DEFAULT", "CENTERED", "MIXED"));
                    case "TITLE": return finalTab(Arrays.asList("DEFAULT", "10,50,10"));
                }
            }

            if (args.length == 4) {
                switch (args[0].toUpperCase()) {
                    case "CHAT": case "TITLE": return finalTab(Collections.singletonList("<message>"));
                    case "targets": case "ACTION-BAR": default: return new ArrayList<>();
                }
            }

            if (args.length >= 5) return new ArrayList<>();
        }

        return new ArrayList<>();
    }
}
