package me.croabeast.sirplugin.tasks;

import com.google.common.collect.*;
import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.objects.files.FileCache;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class IgnCmd extends SIRTask {

    private final String PATH = "commands.ignore.";

    @Override
    public String getName() {
        return "ignore";
    }

    private boolean setBoolean(String uuid, String key, @Nullable Boolean obj, boolean isChat) {
        boolean value = obj == null || !obj;
        uuid += "." + (isChat ? "all-chat" : "all-msg");

        FileCache.IGNORE.get().set("data." + uuid, value);
        FileCache.IGNORE.source().saveFile(false);

        String path = PATH + (value ? "success" : "remove");
        return oneMessage(path + ".all", "type", key);
    }

    private boolean setList(List<String> list, String[] values, String target, String uuid, boolean isChat) {
        boolean add = list.isEmpty() || !list.contains(target);
        String[] keys = {"{target}", "{type}"};
        uuid += "." + (isChat ? "chat" : "msg");

        if (add) list.add(target);
        else list.remove(target);

        FileCache.IGNORE.get().set("data." + uuid, list);
        FileCache.IGNORE.source().saveFile(false);

        String path = PATH + (add ? "success" : "remove");
        oneMessage(path + ".player", keys, values);
        return true;
    }

    @Override
    protected CommandExecutor getExecutor() {
        return (sender, command, label, args) -> {
            setSender(sender);

            if (sender instanceof ConsoleCommandSender) {
                LogUtils.doLog("&cYou can not ignore players in the console.");
                return true;
            }

            if (hasNoPerm("ignore")) return true;

            if (args.length == 0) return oneMessage(PATH + "help");
            if (args.length == 1) return oneMessage(PATH + "need-player");
            if (args.length > 2) return notArgument(args[args.length - 1]);

            String chatKey = FileCache.LANG.get().getString(PATH + "channels.chat"),
                    msgKey = FileCache.LANG.get().getString(PATH + "channels.msg");

            String uuid = ((Player) sender).getUniqueId().toString();

            Boolean allChat = FileCache.IGNORE.get().contains("data." + uuid + ".all-chat") ?
                    FileCache.IGNORE.get().getBoolean("data." + uuid + ".all-chat") : null;
            Boolean allMsg = FileCache.IGNORE.get().contains("data." + uuid + ".all-msg") ?
                    FileCache.IGNORE.get().getBoolean("data." + uuid + ".all-msg") : null;

            List<String> chatList = FileCache.IGNORE.get().getStringList("data." + uuid + ".chat");
            List<String> msgList = FileCache.IGNORE.get().getStringList("data." + uuid + ".msg");

            if (args[1].matches("(?i)@a")) {
                switch (args[0].toLowerCase()) {
                    case "msg": return setBoolean(uuid, msgKey, allMsg, false);
                    case "chat": return setBoolean(uuid, chatKey, allChat, true);
                    default: return notArgument(args[args.length - 1]);
                }
            }

            Player target = PlayerUtils.getClosestPlayer(args[1]);

            if (target == null)
                return oneMessage(PATH + "not-player", "target", args[1]);
            if (target == sender) return oneMessage(PATH + "not-yourself");

            String targetUUID = target.getUniqueId().toString();

            String[] chatValues = {target.getName(), chatKey};
            String[] msgValues = {target.getName(), msgKey};

            switch (args[0].toLowerCase()) {
                case "msg": return setList(msgList, msgValues, targetUUID, uuid, false);
                case "chat": return setList(chatList, chatValues, targetUUID, uuid, true);
                default: return notArgument(args[args.length - 1]);
            }
        };
    }

    @Override
    protected TabCompleter getCompleter() {
        return (sender, command, alias, args) -> {
            setArgs(args);
            if (args.length == 1) return resultList("chat", "msg");
            if (args.length == 2) return resultList(onlinePlayers(), Lists.newArrayList("@a"));
            return new ArrayList<>();
        };
    }
}
