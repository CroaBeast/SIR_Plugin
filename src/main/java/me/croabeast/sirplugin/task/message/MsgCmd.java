package me.croabeast.sirplugin.task.message;

import me.croabeast.sirplugin.object.file.*;
import me.croabeast.sirplugin.utility.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

public class MsgCmd extends DirectTask {

    @Override
    public String getName() {
        return "msg";
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (hasNoPerm(sender, "message.default")) return true;

        String path = "commands.msg-reply.", path1 = "commands.ignore.";
        if (args.length == 0) return oneMessage(sender, path + "need-player");

        Player target = PlayerUtils.getClosestPlayer(args[0]);
        if (target == null) return oneMessage(sender, path + "not-player", "target", args[0]);
        if (target == sender) return oneMessage(sender, path + "not-yourself");

        String key = FileCache.LANG.get().getString(path1 + "channels.msg");

        if (FileCache.IGNORE.get().getBoolean("data." + target.getUniqueId() + ".all-msg"))
            return oneMessage(sender, path1 + "ignoring.all", "type", key);

        if (sender instanceof Player && isPlayer(sender, target, key)) return true;

        getReceivers().put(sender, target);
        getReceivers().put(target, sender);

        return sendResult(sender, target, args, true);
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) return resultList(args, onlinePlayers());
        if (args.length == 2) return resultList(args, "<message>");
        return new ArrayList<>();
    }
}
