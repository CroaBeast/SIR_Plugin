package me.croabeast.sir.plugin.command.object.message;

import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.module.object.listener.ChatFormatter;
import me.croabeast.sir.plugin.utility.LangUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DirectCommand extends SIRCommand {

    private static final Map<CommandSender, CommandSender> RECEIVER_MAP = new HashMap<>();

    static final String MSG_PATH = "commands.msg-reply.";
    static final String IG_PATH = "commands.ignore.";

    private static final String CONSOLE_PATH = MSG_PATH + "console-formatting.";

    DirectCommand(String name) {
        super(name);
    }

    private static YAMLFile lang() {
        return YAMLCache.getLang();
    }

    static String isConsole(CommandSender sender) {
        return !(sender instanceof Player) ?
                lang().get(CONSOLE_PATH + "name-value", String.class) :
                sender.getName();
    }

    static String getPath(boolean isSender) {
        return isSender ? "sender" : "receiver";
    }

    static String getSound(boolean isSender) {
        return lang().get(MSG_PATH + "for-" + getPath(isSender) + ".sound", "");
    }

    static List<String> getMessagingOutput(boolean isSender) {
        return lang().toList(MSG_PATH + "for-" + getPath(isSender) + ".message");
    }

    boolean sendMessagingResult(CommandSender sender, CommandSender target, String[] args, boolean isMsg) {
        if (sender instanceof Player && ChatFormatter.isMuted((Player) sender))
            return fromSender(sender, MSG_PATH + "is-muted");

        String message = LangUtils.messageFromArray(
                args, isMsg || !RECEIVER_MAP.containsKey(sender) ? 1 : 0);

        if (StringUtils.isBlank(message))
            return fromSender(sender, MSG_PATH + "empty-message");

        PlayerUtils.playSound(sender, getSound(true));
        PlayerUtils.playSound(target, getSound(false));

        MessageSender msg = MessageSender.fromLoaded()
                .addKeyValue("{message}", message).setLogger(false);

        new MessageSender(msg).setTargets(sender)
                .addKeyValue("{receiver}",
                        !isMsg ?
                                isConsole(target) :
                                args[0]
                )
                .send(getMessagingOutput(true));

        new MessageSender(msg).setTargets(target)
                .addKeyValue("{sender}", isConsole(sender))
                .send(getMessagingOutput(false));

        return MessageSender.fromLoaded()
                .addKeyValue("{receiver}",
                        !isMsg ?
                                isConsole(target) :
                                args[0]
                )
                .addKeyValue("{sender}", isConsole(sender))
                .addKeyValue("{message}", message)
                .send(lang().toList(CONSOLE_PATH + "format"));
    }

    public static Map<CommandSender, CommandSender> getReceiverMap() {
        return RECEIVER_MAP;
    }
}
