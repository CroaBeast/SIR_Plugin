package me.croabeast.sircore.utils;

import me.croabeast.sircore.MainClass;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdUtils implements TabExecutor {

    private final MainClass main;
    private final LangUtils langUtils;

    public CmdUtils(MainClass main) {
        this.main = main;
        this.langUtils = main.getLangUtils();
        PluginCommand cmd = main.getCommand("sir");
        if (cmd == null) return;
        cmd.setExecutor(this); cmd.setTabCompleter(this);
    }

    private CommandSender sender;

    private void sendMessage(String path, String... values) { langUtils.send(sender, path, values); }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        this.sender = sender;
        if (!sender.hasPermission("sir.main")) {
            sendMessage("no-permission", null, "sir.main", null, null);
            return true;
        }

        if (args.length == 0) {
            sendMessage("main-help", null, null, null, main.getDescription().getVersion());
            return true;
        }

        if (args.length > 3) {
            sendMessage("wrong-arg", args[1], null, null, null);
            return true;
        }

        switch (args[0].toLowerCase()) {
            default:
                sendMessage("wrong-arg", args[0], null, null, null);
                return true;

            case "reload": case "r":
                if (!sender.hasPermission("sir.reload")) {
                    sendMessage("no-permission", null, "sir.reload", null, null);
                    return true;
                }

                if (args.length > 1) {
                    sendMessage("wrong-arg", args[1], null, null, null);
                    return true;
                }

                main.reloadAllFiles();
                sendMessage("reload");
                return true;

            case "print": case "p":
                if (!sender.hasPermission("sir.print")) {
                    sendMessage("no-permission", null, "sir.print", null, null);
                    return true;
                }

                if (args.length < 3) {
                    debugMessage("You need to type a path to print a file string.");
                    return true;
                }

                printPaths(args[1], args[2]);
                return true;
        }
    }

    private void debugMessage(String message) {
        sender.sendMessage(langUtils.parseColor("&7 &4&lDEBUG &8>&7 " + message));
    }

    private void printPaths(String file, String path) {
        if (file.matches("(?i)config")) debugMessage(main.getConfig().getString(path));
        else if (file.matches("(?i)lang")) debugMessage(main.getLang().getString(path));
        else if (file.matches("(?i)messages")) debugMessage(main.getMessages().getString(path));
        else debugMessage("&cThis file doesn't exist, try another one.");
    }

    @Nullable @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> tab = new ArrayList<>();

        if (args.length == 1) tab.addAll(Arrays.asList("reload", "r", "print", "p"));

        if (args.length == 2 && args[0].matches("(?i)print|p")) {
            tab.addAll(Arrays.asList("config", "lang", "messages"));
        }

        if (args.length == 3) {
            if (args[1].matches("config")) {
                tab.addAll(new ArrayList<>(main.getConfig().getKeys(false)));
            }
            if (args[1].matches("lang")) {
                tab.addAll(new ArrayList<>(main.getLang().getKeys(false)));
            }
            if (args[1].matches("messages")) {
                tab.addAll(new ArrayList<>(main.getMessages().getKeys(false)));
            }
        }

        List<String> finalTab = new ArrayList<>();
        StringUtil.copyPartialMatches(args[args.length-1], tab, finalTab);
        return finalTab;
    }
}
