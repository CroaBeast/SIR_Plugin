package me.croabeast.sirplugin.tasks.message;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

import static me.croabeast.sirplugin.objects.FileCache.*;

public class ReplyCmd extends DirectCmd {

    @Override
    public String getName() {
        return "reply";
    }

    @Override
    protected CommandExecutor getExecutor() {
        return (sender, command, label, args) -> {
            setSender(sender);
            if (hasNoPerm("message.reply")) return true;

            String path = "commands.msg-reply.";
            if (args.length == 0) return oneMessage(path + "need-player");

            CommandSender target = (receivers.isEmpty() || !receivers.containsKey(sender)) ?
                    Bukkit.getPlayer(args[0]) : receivers.getOrDefault(sender, null);

            if (target == null)
                return receivers.isEmpty() ? oneMessage(path + "not-replied") :
                        oneMessage(path + "not-player", "target", args[0]);
            if (target == sender) return oneMessage(path + "not-yourself");

            if (target instanceof Player) {
                String path1 = "commands.ignore.", key = LANG.toFile().getString(path1 + "channels.msg");

                if (IGNORE.toFile().getBoolean("data." + ((Player) target).getUniqueId() + ".all-msg"))
                    return oneMessage(path1 + "ignoring.all", "type", key);

                if (sender instanceof Player) return isPlayer(sender, target, key);
            }

            return sendResult(sender, target, args, false);
        };
    }

    @Override
    protected TabCompleter getCompleter() {
        return (sender, command, alias, args) -> {
            setArgs(args);
            boolean notPlayer = !receivers.containsKey(sender);

            if (args.length == 1)
                return notPlayer ? resultList(onlinePlayers()) : resultList("<message>");
            if (args.length == 2 && notPlayer) return resultList("<message>");

            return new ArrayList<>();
        };
    }
}
