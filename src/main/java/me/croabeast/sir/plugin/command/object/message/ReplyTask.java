package me.croabeast.sir.plugin.command.object.message;

import me.croabeast.beanslib.misc.CollectionBuilder;
import me.croabeast.sir.plugin.hook.VanishHook;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

public class ReplyTask extends DirectTask {

    ReplyTask() {
        super("reply");
    }

    @Override
    protected TabPredicate executor() {
        return (sender, args) -> {
            if (isProhibited(sender, "message.reply")) return true;

            if (args.length == 0)
                return fromSender(sender, MSG_PATH + "need-player");

            CommandSender target = getReceiverMap().getOrDefault(
                    sender,
                    PlayerUtils.getClosestPlayer(args[0])
            );

            final String not = MSG_PATH + "not-";

            if (target == null)
                return getReceiverMap().containsKey(sender) ?
                        fromSender(sender, "{target}", args[0], not + "player") :
                        fromSender(sender, not + "replied");

            if (target == sender)
                return fromSender(sender, not + "yourself");

            Player player = sender instanceof Player ? (Player) sender : null;

            if (target instanceof Player) {
                final String vanish = MSG_PATH + "vanish-messages.";
                final String ignoring = IG_PATH + "ignoring.";

                Player t = (Player) target;

                if (PlayerUtils.isIgnoring(t, player, false))
                    return fromSender(sender, "{type}",
                            FileCache.getLang().getValue(IG_PATH + "channels.msg", ""),
                            ignoring + (player == null ? "all" : "player")
                    );

                if (FileCache.getLang().getValue(vanish + "enabled", false) &&
                        VanishHook.isVanished(t))
                    return fromSender(sender, vanish + "message");
            }

            return sendMessagingResult(sender, target, args, false);
        };
    }

    @Override
    protected TabBuilder completer() {
        return TabBuilder.of()
                .addArguments(
                        (s, a) -> !getReceiverMap().containsKey(s),
                        CollectionBuilder.of(Bukkit.getOnlinePlayers())
                                .filter(VanishHook::isVisible)
                                .map(HumanEntity::getName).toList()
                )
                .addArgument(
                        (s, a) -> getReceiverMap().containsKey(s),
                        "<message>"
                )
                .addArgument(1,
                        (s, a) -> !getReceiverMap().containsKey(s),
                        "<message>"
                );
    }
}
