package me.croabeast.sirplugin.tasks.message;

import me.croabeast.sirplugin.objects.files.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

public class MsgCmd extends DirectTask {

    @Override
    public String getName() {
        return "msg";
    }

    @Override
    protected CommandExecutor getExecutor() {
        return (sender, command, label, args) -> {
            setSender(sender);
            if (hasNoPerm("message.default")) return true;

            String path = "commands.msg-reply.", path1 = "commands.ignore.";
            if (args.length == 0) return oneMessage(path + "need-player");

            Player target = PlayerUtils.getClosestPlayer(args[0]);
            if (target == null) return oneMessage(path + "not-player", "target", args[0]);
            if (target == sender) return oneMessage(path + "not-yourself");

            String key = FileCache.LANG.get().getString(path1 + "channels.msg");

            if (FileCache.IGNORE.get().getBoolean("data." + target.getUniqueId() + ".all-msg"))
                return oneMessage(path1 + "ignoring.all", "type", key);

            if (sender instanceof Player && isPlayer(sender, target, key)) return true;

            getReceivers().put(sender, target);
            getReceivers().put(target, sender);

            return sendResult(sender, target, args, true);
        };
    }

    @Override
    protected TabCompleter getCompleter() {
        return (sender, command, alias, args) -> {
            setArgs(args);
            if (args.length == 1) return resultList(onlinePlayers());
            if (args.length == 2) return resultList("<message>");
            return new ArrayList<>();
        };
    }
}
