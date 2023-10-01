package me.croabeast.sir.plugin.task.object.message;

import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.task.SIRTask;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DirectTask extends SIRTask {

    private static final Map<CommandSender, CommandSender> RECEIVER_MAP = new HashMap<>();

    static final String MSG_PATH = "commands.msg-reply.";
    static final String IG_PATH = "commands.ignore.";

    private static final String CONSOLE_PATH = MSG_PATH + "console-formatting.";

    DirectTask(String name) {
        super(name);
    }

    private static FileCache lang() {
        return FileCache.getLang();
    }

    static String isConsole(CommandSender sender) {
        return !(sender instanceof Player) ?
                lang().getValue(CONSOLE_PATH + "name-value", String.class) :
                sender.getName();
    }

    static String getPath(boolean isSender) {
        return isSender ? "sender" : "receiver";
    }

    static String getSound(boolean isSender) {
        return lang().getValue(MSG_PATH + "for-" + getPath(isSender) + ".sound", "");
    }

    static List<String> getMessagingOutput(boolean isSender) {
        return lang().toList(MSG_PATH + "for-" + getPath(isSender) + ".message");
    }

    <C extends CommandSender> boolean sendMessagingResult(C sender, C target, String[] args, boolean isMsg) {
        String message = createMessageFromArray(args,
                (isMsg || !RECEIVER_MAP.containsKey(sender)) ? 1 : 0);

        if (StringUtils.isBlank(message))
            return fromSender(sender, MSG_PATH + "empty-message");

        PlayerUtils.playSound(sender, getSound(true));
        PlayerUtils.playSound(target, getSound(false));

        getClonedSender(sender).setKeys("{receiver}", "{message}")
                .setValues(
                        isMsg ? args[0] : isConsole(target),
                        message
                )
                .setLogger(false)
                .send(getMessagingOutput(true));

        getClonedSender(target).setKeys("{sender}", "{message}")
                .setValues(
                        isConsole(sender),
                        message
                )
                .setLogger(false)
                .send(getMessagingOutput(false));

        return MessageSender.fromLoaded()
                .setKeys("{receiver}", "{sender}", "{message}")
                .setValues(
                        isMsg ? args[0] : isConsole(target),
                        isConsole(sender), message
                )
                .send(lang().toList(CONSOLE_PATH + "format"));
    }

    public static Map<CommandSender, CommandSender> getReceiverMap() {
        return RECEIVER_MAP;
    }
}
