package me.croabeast.sir.plugin.command.object;

import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.channel.ChatChannel;
import me.croabeast.sir.plugin.file.YAMLCache;
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

public class ChatViewCommand extends SIRCommand {

    ChatViewCommand() {
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
            YAMLFile file = YAMLCache.fromData("chat-view");

            boolean isToggled = file.get(path, false);

            file.set(path, !isToggled);
            file.save();

            return fromSender(p, "{channel}", key, "commands.chat-view." + !isToggled);
        };
    }

    @Override
    protected TabBuilder completer() {
        return TabBuilder.of().addArguments((s, a) -> getKeys((Player) s));
    }

    public static boolean isToggled(Player player, String key) {
        return YAMLCache.fromData("chat-view").get("data." + player.getUniqueId() + "." + key, true);
    }
}
