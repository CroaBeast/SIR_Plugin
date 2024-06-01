package me.croabeast.sir.plugin.command.message;

import lombok.RequiredArgsConstructor;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.chat.ChannelHandler;
import me.croabeast.sir.plugin.util.LangUtils;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PrivateMessageCommand extends SIRCommand {

    static final Map<CommandSender, CommandSender> SENDER_MAP = new HashMap<>();
    static final Map<CommandSender, CommandSender> RECEIVER_MAP = new HashMap<>();

    @RequiredArgsConstructor
    private class CommandValues {

        private final boolean sender;

        String getPath() {
            return "lang.for-" + (sender ? "sender" : "receiver") + '.';
        }

        void playSound(CommandSender sender) {
            PlayerUtils.playSound(sender, getLang().get(getPath() + "sound", ""));
        }

        List<String> getOutput() {
            return getLang().toStringList(getPath() + "message");
        }
    }

    protected PrivateMessageCommand(String name) {
        super(name);
    }

    private String isConsoleValue(CommandSender sender) {
        return !(sender instanceof Player) ?
                getLang().get("lang.console-formatting.name", String.class) :
                sender.getName();
    }

    @NotNull
    protected ConfigurableFile getLang() {
        return YAMLData.Command.Single.MSG_REPLY.from();
    }

    boolean sendMessagingResult(CommandSender sender, CommandSender target, String[] args, boolean isMessage) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            if (ChannelHandler.isMuted(player))
                return fromSender(sender).send("is-muted");
        }

        String message = LangUtils.stringFromArray(args,
                isMessage || !SENDER_MAP.containsKey(sender) ? 1 : 0);

        if (StringUtils.isBlank(message))
            return fromSender(sender).send("empty-message");

        CommandValues senderValues = new CommandValues(true);
        CommandValues receiverValues = new CommandValues(true);

        senderValues.playSound(sender);
        receiverValues.playSound(target);

        MessageSender msg = fromSender(sender)
                .addKeyValue("{message}", message).setLogger(false);

        msg.copy()
                .addKeyValue("{receiver}",
                        !isMessage ?
                                isConsoleValue(target) :
                                args[0]
                )
                .send(senderValues.getOutput());

        msg.copy()
                .addKeyValue("{sender}", isConsoleValue(sender))
                .send(receiverValues.getOutput());

        return fromSender(null)
                .addKeyValue("{receiver}",
                        !isMessage ?
                                isConsoleValue(target) :
                                args[0]
                )
                .addKeyValue("{message}", message)
                .addKeyValue("{sender}", isConsoleValue(sender))
                .send("console-formatting.format");
    }

    public static void addFromData(CommandSender sender, CommandSender target) {
        SENDER_MAP.put(sender, target);
        RECEIVER_MAP.put(target, sender);
    }

    public static void removeFromData(CommandSender sender, CommandSender target) {
        SENDER_MAP.remove(sender);
        RECEIVER_MAP.remove(target);
    }
}
