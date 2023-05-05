package me.croabeast.sirplugin.task.message;

import lombok.var;
import me.croabeast.sirplugin.file.FileCache;
import me.croabeast.sirplugin.instance.SIRTask;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public abstract class DirectTask extends SIRTask {

    public static final HashMap<CommandSender, CommandSender> RECEIVER_MAP = new HashMap<>();

    static final String MSG_PATH = "commands.msg-reply.";
    static final String IG_PATH = "commands.ignore.";

    private static final String CONSOLE_PATH = MSG_PATH + "console-formatting.";

    DirectTask(String name) {
        super(name);
    }

    String isConsole(CommandSender sender) {
        return !(sender instanceof Player) ?
                FileCache.LANG.getValue(CONSOLE_PATH + "name-value", String.class) :
                sender.getName();
    }

    String getPath(boolean isSender) {
        return isSender ? "sender" : "receiver";
    }

    String getSound(boolean isSender) {
        return FileCache.LANG.getValue(MSG_PATH + "for-" + getPath(isSender) + ".sound", "");
    }

    List<String> getMessagingOutput(boolean isSender) {
        return FileCache.LANG.toList(MSG_PATH + "for-" + getPath(isSender) + ".message");
    }

    <C extends CommandSender> boolean sendMessagingResult(C sender, C target, String[] args, boolean isMsg) {
        var message = getFromArray(args,
                (isMsg || !RECEIVER_MAP.containsKey(sender)) ? 1 : 0);

        if (StringUtils.isBlank(message))
            return fromSender(sender, MSG_PATH + "empty-message");

        message = message.replace("$", "\\$").replace("%", "%%");

        PlayerUtils.playSound(sender, getSound(true));
        PlayerUtils.playSound(target, getSound(false));

        getClonedSender(sender).setKeys("{receiver}", "{message}").
                setValues(
                        isMsg ? args[0] : isConsole(target),
                        message
                ).
                send(getMessagingOutput(true));

        getClonedSender(target).setKeys("{sender}", "{message}").
                setValues(
                        isConsole(sender),
                        message
                ).
                send(getMessagingOutput(false));

        LangUtils.getSender().setKeys("{receiver}", "{sender}", "{message}").
                setValues(
                        isMsg ? args[0] : isConsole(target),
                        isConsole(sender), message
                ).
                send(FileCache.LANG.toList(CONSOLE_PATH + "format"));

        return true;
    }
}
