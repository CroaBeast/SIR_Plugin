package me.croabeast.sir.plugin.task;

import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.module.instance.listener.ChatFormatter;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatViewTask extends SIRTask {

    public ChatViewTask() {
        super("chat-view");
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
        if (isProhibited(sender, "chat-view")) return true;

        if (!(sender instanceof Player)) {
            LogUtils.doLog("&cYou can't toggle a local chat in console.");
            return true;
        }

        Player p = (Player) sender;

        if (args.length == 0)
            return fromSender(p, "commands.chat-view.help");

        if (args.length != 1)
            return isWrongArgument(p, args[args.length - 1]);

        String key = null;

        for (String k : getKeys(p)) if (k.matches("(?i)" + args[0])) key = k;

        if (key == null) return isWrongArgument(p, args[0]);

        String path = "data." + p.getUniqueId() + "." + key;
        boolean isToggled = FileCache.CHAT_VIEW_DATA.getValue(path, false);

        FileCache.CHAT_VIEW_DATA.get().set(path, !isToggled);
        FileCache.CHAT_VIEW_DATA.getFile().save(false);

        return fromSender(p, "{channel}", key, "commands.chat-view." + !isToggled);
    }

    @Override
    protected @NotNull List<String> complete(CommandSender s, String[] args) {
        return args.length == 1 && s instanceof Player ? getKeys((Player) s) : new ArrayList<>();
    }

    public static boolean isToggled(Player player, String key) {
        return FileCache.CHAT_VIEW_DATA.getValue("data." + player.getUniqueId() + "." + key, false);
    }
}
