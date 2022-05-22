package me.croabeast.sirplugin.tasks.message;

import me.croabeast.sirplugin.objects.extensions.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

import static me.croabeast.sirplugin.objects.FileCache.*;
import static me.croabeast.sirplugin.utilities.EventUtils.*;

public abstract class DirectCmd extends BaseCmd {

    protected static HashMap<CommandSender, CommandSender> receivers = new HashMap<>();

    protected boolean isPlayer(CommandSender sender, CommandSender target, String key) {
        String path = "data." + ((Player) target).getUniqueId() + ".",
                uuid = ((Player) sender).getUniqueId().toString();

        List<String> list = IGNORE.toFile().getStringList(path + "msg");

        if (!list.isEmpty() && list.contains(uuid))
            return oneMessage("commands.ignore.ignoring.player", "type", key);
        return false;
    }

    private String isConsole(CommandSender sender) {
        return !(sender instanceof ConsoleCommandSender) ? sender.getName() :
                LANG.toFile().getString("commands.msg-reply.console-name");
    }

    private String toSound(String path) {
        return LANG.toFile().getString("commands.msg-reply.for-" + path + ".sound");
    }

    boolean sendResult(CommandSender sender, CommandSender target, String[] args, boolean isMsg) {
        String path = "commands.msg-reply.", key = isMsg ? args[0] : isConsole(target),
                message = rawMessage(args, isMsg || !receivers.containsKey(sender) ? 1 : 0);

        String[] sendValues = {key, message}, recValues = {isConsole(sender), message},
                toSender = {"{receiver}", "{message}"}, toReceiver = {"{sender}", "{message}"};

        oneMessage(sender, path + "for-sender.message", toSender, sendValues);
        oneMessage(target, path + "for-receiver.message", toReceiver, recValues);

        if (sender instanceof Player) playSound((Player) sender, toSound("sender"));
        if (target instanceof Player) playSound((Player) target, toSound("receiver"));
        return true;
    }

    public static HashMap<CommandSender, CommandSender> getReceivers() {
        return receivers;
    }
}
