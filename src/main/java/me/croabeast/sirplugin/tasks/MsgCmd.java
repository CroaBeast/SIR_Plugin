package me.croabeast.sirplugin.tasks;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

import static me.croabeast.sirplugin.objects.FileCatcher.*;

public class MsgCmd extends BaseCmd {

    @Override
    public String getName() {
        return "msg";
    }

    @Override
    protected CommandExecutor getExecutor() {
        return (sender, command, label, args) -> {
            setSender(sender);
            if (hasNoPerm("message.default")) return true;

            String path = "commands.msg-reply.";
            if (args.length == 0) return oneMessage(path + "need-player");

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) return oneMessage(path + "not-player", "target", args[0]);
            if (target == sender) return oneMessage(path + "not-yourself");

            String path1 = "commands.ignore.", s = "data." + target.getUniqueId() + ".",
                    key = LANG.toFile().getString(path1 + "channels.msg");

            if (IGNORE.toFile().getBoolean(s + "all-msg"))
                return oneMessage(path1 + "ignoring.all", "type", key);

            if (sender instanceof Player) {
                String s1 = ((Player) sender).getUniqueId().toString();
                List<String> list = IGNORE.toFile().getStringList(s + "msg");

                if (!list.isEmpty() && list.contains(s1))
                    return oneMessage(path1 + "ignoring.player", "type", key);
            }

            receivers.put(sender, target);
            receivers.put(target, sender);

            String message = rawMessage(args, 1);

            String[] values1 = {args[0], message};
            String[] values2 = {isConsole(sender), message};

            SIRPlugin.textUtils().sendMessageList(sender, LANG.toFile(),
                    path + "sender", new String[] {"receiver", "message"}, values1);
            SIRPlugin.textUtils().sendMessageList(target, LANG.toFile(),
                    path + "receiver", new String[] {"sender", "message"}, values2);
            return false;
        };
    }

    @Override
    protected TabCompleter getCompleter() {
        return (sender, command, alias, args) -> {
            setArgs(args);
            if (args.length == 1) return resultTab(onlinePlayers());
            if (args.length == 2) return resultTab("<message>");
            return new ArrayList<>();
        };
    }
}