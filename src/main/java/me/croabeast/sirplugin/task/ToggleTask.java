package me.croabeast.sirplugin.task;

import lombok.var;
import me.croabeast.sirplugin.channel.ChatChannel;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.instance.SIRTask;
import me.croabeast.sirplugin.module.listener.ChatFormatter;
import me.croabeast.sirplugin.utility.LogUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ToggleTask extends SIRTask {

    public ToggleTask() {
        super("chat-toggle");
    }

    private static List<String> getKeys(Player player) {
        return ChatFormatter.CHANNEL_LIST.stream().
                filter(c -> !c.isGlobal()).
                filter(c -> PlayerUtils.hasPerm(player, c.getPermission())).
                map(ChatChannel::getName).
                collect(Collectors.toList());
    }

    @SuppressWarnings("all")
    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender, "chat-toggle")) return true;

        if (args.length == 0)
            return fromSender(sender, "commands.chat-toggle.help");

        if (args.length != 1)
            return isWrongArgument(sender, args[args.length - 1]);

        if (!(sender instanceof Player)) {
            LogUtils.doLog("&cYou can't toggle a local chat in console.");
            return true;
        }

        var p = (Player) sender;
        String key = null;

        for (var k : getKeys(p)) if (k.matches("(?i)" + args[0])) key = k;
        if (key == null) return isWrongArgument(p, args[0]);

        String path = "data." + p.getUniqueId() + "." + key;
        var isToggled = FileCache.TOGGLE_DATA.getValue(path, false);

        FileCache.TOGGLE_DATA.get().set(path, !isToggled);
        FileCache.TOGGLE_DATA.getFile().saveFile();

        return fromSender(p,
                "commands.chat-toggle." + (isToggled ? "off" : "on"));
    }

    @Override
    protected @NotNull List<String> complete(CommandSender s, String[] args) {
        return args.length == 1 && s instanceof Player ? getKeys((Player) s) : new ArrayList<>();
    }

    public static boolean isToggled(Player player, String key) {
        return FileCache.TOGGLE_DATA.getValue("data." + player.getUniqueId() + "." + key, false);
    }
}
