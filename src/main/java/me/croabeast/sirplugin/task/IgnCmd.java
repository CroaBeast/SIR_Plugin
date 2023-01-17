package me.croabeast.sirplugin.task;

import com.google.common.collect.*;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.utility.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class IgnCmd extends SIRTask {

    private static final String PATH = "commands.ignore.";

    @Override
    public String getName() {
        return "ignore";
    }

    private boolean setBoolean(CommandSender sender, String uuid, String key,
                               @Nullable Boolean obj, boolean isChat) {

        boolean value = obj == null || !obj;
        uuid += "." + (isChat ? "all-chat" : "all-msg");

        FileCache.IGNORE.get().set("data." + uuid, value);
        FileCache.IGNORE.source().saveFile(false);

        String path = PATH + (value ? "success" : "remove");
        return oneMessage(sender, path + ".all", "type", key);
    }

    private boolean setList(CommandSender sender, List<String> list, String[] values,
                            String target, String uuid, boolean isChat) {

        boolean add = list.isEmpty() || !list.contains(target);
        String[] keys = {"{target}", "{type}"};
        uuid += "." + (isChat ? "chat" : "msg");

        if (add) list.add(target);
        else list.remove(target);

        FileCache.IGNORE.get().set("data." + uuid, list);
        FileCache.IGNORE.source().saveFile(false);

        String path = PATH + (add ? "success" : "remove");
        oneMessage(sender, path + ".player", keys, values);
        return true;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        FileConfiguration ig = FileCache.IGNORE.get();

        if (sender instanceof ConsoleCommandSender) {
            LogUtils.doLog("&cYou can not ignore players in the console.");
            return true;
        }

        if (hasNoPerm(sender, "ignore")) return true;

        if (args.length == 0) return oneMessage(sender, PATH + "help");
        if (args.length == 1) return oneMessage(sender, PATH + "need-player");
        if (args.length > 2) return notArgument(sender, args[args.length - 1]);

        String chatKey = FileCache.LANG.get().getString(PATH + "channels.chat"),
                msgKey = FileCache.LANG.get().getString(PATH + "channels.msg");

        String uuid = ((Player) sender).getUniqueId().toString();

        Boolean allChat = ig.contains("data." + uuid + ".all-chat") ?
                ig.getBoolean("data." + uuid + ".all-chat") : null;
        Boolean allMsg = ig.contains("data." + uuid + ".all-msg") ?
                ig.getBoolean("data." + uuid + ".all-msg") : null;

        List<String> chatList = ig.getStringList("data." + uuid + ".chat");
        List<String> msgList = ig.getStringList("data." + uuid + ".msg");

        if (args[1].matches("(?i)@a")) {
            switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "msg": return setBoolean(sender, uuid, msgKey, allMsg, false);
                case "chat": return setBoolean(sender, uuid, chatKey, allChat, true);
                default: return notArgument(sender, args[args.length - 1]);
            }
        }

        Player target = PlayerUtils.getClosestPlayer(args[1]);

        if (target == null)
            return oneMessage(sender, PATH + "not-player", "target", args[1]);
        if (target == sender)
            return oneMessage(sender, PATH + "not-yourself");

        String targetUUID = target.getUniqueId().toString();

        String[] chatValues = {target.getName(), chatKey};
        String[] msgValues = {target.getName(), msgKey};

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "msg": return setList(sender, msgList, msgValues, targetUUID, uuid, false);
            case "chat": return setList(sender, chatList, chatValues, targetUUID, uuid, true);
            default: return notArgument(sender, args[args.length - 1]);
        }
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) return resultList(args, "chat", "msg");
        if (args.length == 2) return resultList(args, onlinePlayers(), Lists.newArrayList("@a"));
        return new ArrayList<>();
    }
}
