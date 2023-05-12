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

public class MessageTask extends DirectTask {

    public MessageTask() {
        super("msg");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender, "message.default")) return true;

        var player = sender instanceof Player ? (Player) sender : null;

        if (args.length == 0)
            return fromSender(sender, MSG_PATH + "need-player");

        var target = PlayerUtils.getClosestPlayer(args[0]);
        var not = MSG_PATH + "not-";

        if (target == null)
            return fromSender(sender, "{target}", args[0], not + "player");
        if (target == sender)
            return fromSender(sender, not + "yourself");

        final var ignoring = IG_PATH + "ignoring.";

        if (PlayerUtils.isIgnoring(target, player, false))
            return fromSender(sender, "{type}",
                    FileCache.LANG.getValue(IG_PATH + "channels.msg", ""),
                    ignoring + (player == null ? "all" : "player"));

        final var vanish = MSG_PATH + "vanish-messages.";

        if (FileCache.LANG.getValue(vanish + "enabled", false) &&
                VanishHook.isVanished(target))
            return fromSender(sender, vanish + "message");

        RECEIVER_MAP.put(sender, target);
        RECEIVER_MAP.put(target, sender);

        return sendMessagingResult(sender, target, args, true);
    }

    @Override
    protected @NotNull List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return generateList(args,
                    Bukkit.getOnlinePlayers().stream().
                            filter(p -> !VanishHook.isVanished(p)).
                            map(HumanEntity::getName).
                            collect(Collectors.toList())
            );

        return args.length == 2 ? generateList(args, "<message>") : new ArrayList<>();
    }
}
