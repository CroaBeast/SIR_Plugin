package me.croabeast.sirplugin.tasks.message;

import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.objects.files.FileCache;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

import static me.croabeast.sirplugin.utilities.EventUtils.*;

public abstract class DirectTask extends SIRTask {

    static HashMap<CommandSender, CommandSender> receivers = new HashMap<>();

    protected boolean isPlayer(CommandSender sender, CommandSender target, String key) {
        String path = "data." + ((Player) target).getUniqueId() + ".",
                uuid = ((Player) sender).getUniqueId().toString();

        List<String> list = FileCache.IGNORE.get().getStringList(path + "msg");

        if (!list.isEmpty() && list.contains(uuid))
            return oneMessage("commands.ignore.ignoring.player", "type", key);
        return false;
    }

    private String isConsole(CommandSender sender) {
        return !(sender instanceof ConsoleCommandSender) ? sender.getName() :
                FileCache.LANG.get().getString("commands.msg-reply.console-name");
    }

    private String toSound(String path) {
        return FileCache.LANG.get().getString("commands.msg-reply.for-" + path + ".sound");
    }

    boolean sendResult(CommandSender sender, CommandSender target, String[] args, boolean isMsg) {
        String path = "commands.msg-reply.", key = isMsg ? args[0] : isConsole(target),
                message = rawMessage(args, isMsg || !receivers.containsKey(sender) ? 1 : 0);

        message = message.replace("$", "\\$").replace("%", "%%");

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
