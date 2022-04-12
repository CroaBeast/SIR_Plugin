package me.croabeast.sirplugin.tasks.extensions;

import com.google.common.collect.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.tasks.BaseCmd;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class IgnoreCmd extends BaseCmd {

    private final SIRPlugin main;

    public IgnoreCmd(SIRPlugin main) {
        this.main = main;
    }

    @Override
    public String getName() {
        return "ignore";
    }

    private boolean setBoolean(String uuid, String key, @Nullable Boolean obj, boolean isChat) {
        boolean value = obj == null || !obj;
        uuid += "." + (isChat ? "all-chat" : "all-msg");

        main.getIgnore().set("data." + uuid, value);
        main.getFiles().getObject("ignore").saveFile();

        String path = "commands.ignore." + (value ? "success" : "remove");
        return oneMessage(path + ".all", "type", key);
    }

    private boolean setList(List<String> list, String[] values, String target, String uuid, boolean isChat) {
        boolean add = list.isEmpty() || !list.contains(target);
        String[] keys = {"target", "type"};
        uuid += "." + (isChat ? "chat" : "msg");

        if (add) list.add(target);
        else list.remove(target);

        main.getIgnore().set("data." + uuid, list);
        main.getFiles().getObject("ignore").saveFile();

        String path = "commands.ignore." + (add ? "success" : "remove");
        return oneMessage(path + "player", keys, values);
    }

    @Override
    protected CommandExecutor getExecutor() {
        return (sender, command, label, args) -> {
            setSender(sender);

            if (sender instanceof ConsoleCommandSender) {
                LogUtils.doLog("&cYou can not ignore players in the console.");
                return true;
            }

            String path = "commands.ignore.";
            if (hasNoPerm("ignore")) return true;

            if (args.length == 0) return oneMessage(path + "help");
            if (args.length == 1) return oneMessage(path + "need-player");
            if (args.length > 2) return notArgument(args[args.length - 1]);

            String chatKey = main.getLang().getString(path + "channels.chat"),
                    msgKey = main.getLang().getString(path + "channels.msg");

            String uuid = ((Player) sender).getUniqueId().toString();

            Boolean allChat = main.getIgnore().contains("data." + uuid + ".all-chat") ?
                    main.getIgnore().getBoolean("data." + uuid + ".all-chat") : null;
            Boolean allMsg = main.getIgnore().contains("data." + uuid + ".all-msg") ?
                    main.getIgnore().getBoolean("data." + uuid + ".all-msg") : null;

            List<String> chatList = main.getIgnore().getStringList("data." + uuid + ".chat");
            List<String> msgList = main.getIgnore().getStringList("data." + uuid + ".msg");

            if (args[1].matches("(?i)@a")) {
                switch (args[0].toLowerCase()) {
                    case "msg": return setBoolean(uuid, msgKey, allMsg, false);
                    case "chat": return setBoolean(uuid, chatKey, allChat, true);
                    default: return notArgument(args[args.length - 1]);
                }
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null)
                return oneMessage(path + "not-player", "target", args[1]);
            if (target == sender) return oneMessage(path + "not-yourself");

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
            if (args.length == 1) return resultTab("chat", "msg");
            if (args.length == 2) return resultTab(onlinePlayers(), Lists.newArrayList("@a"));
            return new ArrayList<>();
        };
    }
}