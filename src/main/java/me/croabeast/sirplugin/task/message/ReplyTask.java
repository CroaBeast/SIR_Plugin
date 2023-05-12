package me.croabeast.sirplugin.task.message;

import lombok.var;
import me.croabeast.sirplugin.hook.VanishHook;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReplyTask extends DirectTask {

    public ReplyTask() {
        super("reply");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender, "message.reply")) return true;

        if (args.length == 0)
            return fromSender(sender, MSG_PATH + "need-player");

        var target = RECEIVER_MAP.getOrDefault(
                sender,
                PlayerUtils.getClosestPlayer(args[0])
        );

        final var not = MSG_PATH + "not-";

        if (target == null)
            return RECEIVER_MAP.containsKey(sender) ?
                    fromSender(sender, "{target}", args[0], not + "player") :
                    fromSender(sender, not + "replied");

        if (target == sender)
            return fromSender(sender, not + "yourself");

        var player = sender instanceof Player ? (Player) sender : null;

        if (target instanceof Player) {
            final var vanish = MSG_PATH + "vanish-messages.";
            final var ignoring = IG_PATH + "ignoring.";

            var t = (Player) target;

            if (PlayerUtils.isIgnoring(t, player, false))
                return fromSender(sender, "{type}",
                        FileCache.LANG.getValue(IG_PATH + "channels.msg", ""),
                        ignoring + (player == null ? "all" : "player")
                );

            if (FileCache.LANG.getValue(vanish + "enabled", false) &&
                    VanishHook.isVanished(t))
                return fromSender(sender, vanish + "message");
        }

        return sendMessagingResult(sender, target, args, false);
    }

    @Override
    protected @NotNull List<String> complete(CommandSender sender, String[] args) {
        boolean notPlayer = !RECEIVER_MAP.containsKey(sender);

        if (args.length == 1)
            return notPlayer ?
                    generateList(args,
                            Bukkit.getOnlinePlayers().stream().
                                    filter(p -> !VanishHook.isVanished(p)).
                                    map(HumanEntity::getName).
                                    collect(Collectors.toList())
                    ) :
                    generateList(args, "<message>");

        return args.length == 2 && notPlayer ?
                generateList(args, "<message>") : new ArrayList<>();
    }
}
