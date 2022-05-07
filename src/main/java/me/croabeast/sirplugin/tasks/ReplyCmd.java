package me.croabeast.sirplugin.tasks;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.*;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

import static me.croabeast.sirplugin.objects.FileCatcher.*;

public class ReplyCmd extends BaseCmd {

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
                        oneMessage(path + "not-player", "TARGET", args[0]);
            if (target == sender) return oneMessage(path + "not-yourself");

            if (target instanceof Player) {
                String path1 = "commands.ignore.", s = "data." + ((Player) target).getUniqueId() + ".",
                        key = LANG.toFile().getString(path1 + "channels.msg");

                if (IGNORE.toFile().getBoolean(s + "all-msg"))
                    return oneMessage(path1 + "ignoring.all", "type", key);

                if (sender instanceof Player) {
                    String s1 = ((Player) sender).getUniqueId().toString();
                    List<String> list = IGNORE.toFile().getStringList(s + "msg");

                    if (!list.isEmpty() && list.contains(s1))
                        return oneMessage(path1 + "ignoring.player", "type", key);
                }
            }

            String message = rawMessage(args, receivers.containsKey(sender) ? 0 : 1);

            String[] values1 = {isConsole(target), message};
            String[] values2 = {isConsole(sender), message};

            SIRPlugin.textUtils().sendMessageList(sender, LANG.toFile(),
                    path + "sender", new String[] {"receiver", "message"}, values1);
            SIRPlugin.textUtils().sendMessageList(target, LANG.toFile(),
                    path + "receiver", new String[] {"sender", "message"}, values2);
            return true;
        };
    }

    @Override
    protected TabCompleter getCompleter() {
        return (sender, command, alias, args) -> {
            setArgs(args);
            boolean notPlayer = !CmdUtils.getReceivers().containsKey(sender);

            if (args.length == 1)
                return notPlayer ? resultTab(onlinePlayers()) : resultTab("<message>");
            if (args.length == 2 && notPlayer) return resultTab("<message>");

            return new ArrayList<>();
        };
    }
}
