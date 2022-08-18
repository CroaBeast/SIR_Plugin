package me.croabeast.sirplugin.task.message;

import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.utility.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

public class ReplyCmd extends DirectTask {

    @Override
    public String getName() {
        return "reply";
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (hasNoPerm(sender, "message.reply")) return true;

        String path = "commands.msg-reply.", path1 = "commands.ignore.";
        if (args.length == 0) return oneMessage(sender, path + "need-player");

        CommandSender target = !getReceivers().containsKey(sender) ?
                PlayerUtils.getClosestPlayer(args[0]) : getReceivers().get(sender);

        if (target == null)
            return getReceivers().containsKey(sender) ?
                    oneMessage(sender, path + "not-player", "target", args[0]) :
                    oneMessage(sender, path + "not-replied");

        if (target == sender) return oneMessage(sender, path + "not-yourself");

        if (target instanceof Player) {
            String key = FileCache.LANG.get().getString(path1 + "channels.msg");

            if (FileCache.IGNORE.get().getBoolean("data." +
                    ((Player) target).getUniqueId() + ".all-msg"))
                return oneMessage(sender, path1 + "ignoring.all", "type", key);

            if (sender instanceof Player && isPlayer(sender, target, key)) return true;
        }

        return sendResult(sender, target, args, false);
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        boolean notPlayer = !getReceivers().containsKey(sender);

        if (args.length == 1) return notPlayer ?
                    resultList(args, onlinePlayers()) :
                    resultList(args, "<message>");

        if (args.length == 2 && notPlayer)
            return resultList(args, "<message>");

        return new ArrayList<>();
    }
}
