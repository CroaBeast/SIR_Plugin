package me.croabeast.sir.plugin.task.object.message;

import me.croabeast.sir.plugin.hook.VanishHook;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReplyTask extends DirectTask {

    ReplyTask() {
        super("reply");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
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
    }

    @Override
    protected @NotNull List<String> complete(CommandSender sender, String[] args) {
        boolean notPlayer = !getReceiverMap().containsKey(sender);

        if (args.length == 1)
            return notPlayer ?
                    generateList(args,
                            Bukkit.getOnlinePlayers().stream()
                                    .filter(VanishHook::isVisible)
                                    .map(HumanEntity::getName)
                                    .collect(Collectors.toList())
                    ) :
                    generateList(args, "<message>");

        return args.length == 2 && notPlayer ?
                generateList(args, "<message>") : new ArrayList<>();
    }
}
