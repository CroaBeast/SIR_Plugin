package me.croabeast.sir.plugin.task.message;

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

public class MessageTask extends DirectTask {

    public MessageTask() {
        super("msg");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender, "message.default")) return true;

        Player player = sender instanceof Player ? (Player) sender : null;

        if (args.length == 0)
            return fromSender(sender, MSG_PATH + "need-player");

        Player target = PlayerUtils.getClosestPlayer(args[0]);
        String not = MSG_PATH + "not-";

        if (target == null)
            return fromSender(sender, "{target}", args[0], not + "player");
        if (target == sender)
            return fromSender(sender, not + "yourself");

        final String ignoring = IG_PATH + "ignoring.";

        if (PlayerUtils.isIgnoring(target, player, false))
            return fromSender(sender, "{type}",
                    FileCache.getLang().getValue(IG_PATH + "channels.msg", ""),
                    ignoring + (player == null ? "all" : "player"));

        final String vanish = MSG_PATH + "vanish-messages.";

        if (FileCache.getLang().getValue(vanish + "enabled", false) &&
                VanishHook.isVanished(target))
            return fromSender(sender, vanish + "message");

        getReceiverMap().put(sender, target);
        getReceiverMap().put(target, sender);

        return sendMessagingResult(sender, target, args, true);
    }

    @Override
    protected @NotNull List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return generateList(args,
                    Bukkit.getOnlinePlayers().stream().
                            filter(VanishHook::isVisible).
                            map(HumanEntity::getName).
                            collect(Collectors.toList())
            );

        return args.length == 2 ? generateList(args, "<message>") : new ArrayList<>();
    }
}
