package me.croabeast.sir.utils;

import me.croabeast.sir.SIR;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.*;

import java.util.List;

public class CmdUtils implements TabExecutor {

    private final SIR main;
    private final LangUtils langUtils;

    public CmdUtils(SIR main) {
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

        if (args.length > 1) {
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
                main.reloadFiles();
                sendMessage("reload");
                return true;
        }
    }

    @Nullable @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return null;
    }
}
