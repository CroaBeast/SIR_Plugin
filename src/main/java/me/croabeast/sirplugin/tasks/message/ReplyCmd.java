package me.croabeast.sirplugin.tasks.message;

import me.croabeast.sirplugin.objects.files.FileCache;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

public class ReplyCmd extends DirectTask {

    @Override
    public String getName() {
        return "reply";
    }

    @Override
    protected CommandExecutor getExecutor() {
        return (sender, command, label, args) -> {
            setSender(sender);
            if (hasNoPerm("message.reply")) return true;

            String path = "commands.msg-reply.", path1 = "commands.ignore.";
            if (args.length == 0) return oneMessage(path + "need-player");

            CommandSender target = !getReceivers().containsKey(sender) ?
                    PlayerUtils.getClosestPlayer(args[0]) : getReceivers().get(sender);

            if (target == null)
                return !getReceivers().containsKey(sender) ? oneMessage(path + "not-replied") :
                        oneMessage(path + "not-player", "target", args[0]);
            if (target == sender) return oneMessage(path + "not-yourself");

            if (target instanceof Player) {
                String key = FileCache.LANG.get().getString(path1 + "channels.msg");

                if (FileCache.IGNORE.get().getBoolean("data." + ((Player) target).getUniqueId() + ".all-msg"))
                    return oneMessage(path1 + "ignoring.all", "type", key);

                if (sender instanceof Player && isPlayer(sender, target, key)) return true;
            }

            return sendResult(sender, target, args, false);
        };
    }

    @Override
    protected TabCompleter getCompleter() {
        return (sender, command, alias, args) -> {
            setArgs(args);
            boolean notPlayer = !getReceivers().containsKey(sender);

            if (args.length == 1)
                return notPlayer ? resultList(onlinePlayers()) : resultList("<message>");
            if (args.length == 2 && notPlayer) return resultList("<message>");

            return new ArrayList<>();
        };
    }
}
