package me.croabeast.sircore.utils;

import me.croabeast.sircore.*;
import org.bukkit.command.*;
import org.bukkit.util.*;
import org.jetbrains.annotations.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdUtils implements TabExecutor {

    private final Application main;
    private final TextUtils textUtils;

    public CmdUtils(Application main) {
        this.main = main;
        this.textUtils = main.getTextUtils();
        PluginCommand cmd = main.getCommand("sir");
        if (cmd == null) return;
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    private CommandSender sender;

    private void sendMessage(String path, String key, String value) {
        textUtils.send(sender, path, new String[]{key}, value);
    }

    private void sendLoadedSections() {
        String[] keys = {"{TOTAL}", "{SECTION}"};
        for (String id : main.getMessages().getKeys(false)) {
            textUtils.send(sender, "get-sections", keys, main.sections(id) + "", id);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        this.sender = sender;
        if (!sender.hasPermission("sir.main")) {
            sendMessage("no-permission", "{PERM}", "sir.main");
            return true;
        }

        if (args.length == 0) {
            sendMessage("main-help", "{VERSION}", main.getDescription().getVersion());
            return true;
        }

        if (args.length > 3) {
            sendMessage("wrong-arg", "{ARG}", args[1]);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload": case "r":
                if (!sender.hasPermission("sir.reload")) {
                    sendMessage("no-permission", "{PERM}", "sir.reload");
                    return true;
                }

                if (args.length > 1) {
                    sendMessage("wrong-arg", "{ARG}", args[1]);
                    return true;
                }

                main.reloadFiles();
                sendLoadedSections();
                sendMessage("reload", null, null);
                return true;

            default:
                sendMessage("wrong-arg", "{ARG}", args.length == 2 ? args[1] : args[0]);
                return true;
        }
    }

    @Nullable @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> tab = new ArrayList<>();
        List<String> finalTab = new ArrayList<>();

        if (args.length == 1) tab.addAll(Arrays.asList("reload", "r"));

        if (args.length == 2 && args[0].matches("(?i)preview")) {
            tab.addAll(new ArrayList<>(main.getMessages().getKeys(false)));
        }

        StringUtil.copyPartialMatches(args[args.length - 1], tab, finalTab);
        return finalTab;
    }
}
