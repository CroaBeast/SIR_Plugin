package me.croabeast.sir.plugin.command.object;

import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.module.object.listener.ChatFormatter;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChatViewTask extends SIRCommand {

    ChatViewTask() {
        super("chat-view");
    }

    static List<String> getKeys(Player player) {
        List<String> keys = new ArrayList<>();

        for (Set<ChatChannel> channels : ChatFormatter.LOCAL_MAP.values()) {
            channels.forEach(c -> {
                if (PlayerUtils.hasPerm(player, c.getPermission())) keys.add(c.getName());
            });
        }

        return keys;
    }

    @SuppressWarnings("all")
    @Override
    protected TabPredicate executor() {
        return (sender, args) -> {
            if (isProhibited(sender, "chat-view")) return true;

            if (!(sender instanceof Player)) {
                LogUtils.doLog("&cYou can't toggle a local chat in console.");
                return true;
            }
            if (args.length == 0)
                return fromSender(sender, "commands.chat-view.help");
            if (args.length != 1)
                return isWrongArgument(sender, args[args.length - 1]);

            Player p = (Player) sender;
            String key = null;

            for (String k : getKeys(p)) if (k.matches("(?i)" + args[0])) key = k;

            if (key == null) return isWrongArgument(p, args[0]);

            String path = "data." + p.getUniqueId() + "." + key;
            boolean isToggled = FileCache.CHAT_VIEW_DATA.getValue(path, false);

            FileCache.CHAT_VIEW_DATA.get().set(path, !isToggled);
            FileCache.CHAT_VIEW_DATA.getFile().save(false);

            return fromSender(p,
                    "{channel}", key, "commands.chat-view." + !isToggled);
        };
    }

    @Override
    protected TabBuilder completer() {
        return TabBuilder.of().addArguments((s, a) -> getKeys((Player) s));
    }

    public static boolean isToggled(Player player, String key) {
        return FileCache.CHAT_VIEW_DATA.getValue("data." + player.getUniqueId() + "." + key, true);
    }
}
