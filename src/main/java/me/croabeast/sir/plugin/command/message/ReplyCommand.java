package me.croabeast.sir.plugin.command.message;

import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.command.Executable;
import me.croabeast.sir.plugin.command.mute.MuteCommand;
import me.croabeast.sir.plugin.module.hook.VanishHook;
import me.croabeast.sir.plugin.util.LangUtils;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

final class ReplyCommand extends PrivateCommand {

    ReplyCommand() {
        super("reply");
    }

    @NotNull
    public Executable getExecutable() {
        return Executable.from((receiver, args) -> {
            if (!isPermitted(receiver)) return true;

            Player receiverPlayer = !(receiver instanceof Player) ?
                    null :
                    (Player) receiver;
            if (receiverPlayer != null && MuteCommand.isMuted(receiverPlayer))
                return createSender(receiver).send("is-muted");

            if (args.length == 0)
                return createSender(receiver).send("empty-message");

            CommandSender initiator = null;
            int messageCount = 0;

            Player previous = PlayerUtils.getClosest(args[0]);
            if (previous == receiver)
                return createSender(receiver).send("not-yourself");

            if (previous != null) {
                Conversation conversation = Conversation.asInitiator(previous);
                if (conversation == null)
                    return createSender(receiver).send("not-replied");

                if (conversation.isReceiver(receiver)) {
                    initiator = conversation.getInitiator();
                    messageCount = 1;
                }
            }

            if (initiator == null) {
                Conversation conversation = Conversation.asReceiver(receiver);
                if (conversation == null)
                    return createSender(receiver).send("not-replied");

                initiator = conversation.getInitiator();
            }

            if (initiator instanceof Player) {
                final Player initPlayer = (Player) initiator;

                if (receiverPlayer != null && isIgnoring(initPlayer, receiverPlayer))
                    return true;

                if (VanishHook.isVanished(initPlayer))
                    return createSender(receiver).send("vanish-messages.message");
            }

            String message = LangUtils.stringFromArray(args, messageCount);
            if (StringUtils.isBlank(message))
                return createSender(receiver).send("empty-message");

            INITIATOR_VALUES.playSound(initiator);
            RECEIVER_VALUES.playSound(receiver);

            MessageSender msg = createSender(receiver)
                    .addKeyValue("{message}", message).setLogger(false);

            msg.copy()
                    .addKeyValue("{receiver}", isConsoleValue(initiator))
                    .send(INITIATOR_VALUES.getOutput());

            Player initPlayer = !(initiator instanceof Player) ?
                    null :
                    (Player) initiator;

            msg.copy().setTargets(initPlayer)
                    .addKeyValue("{sender}", isConsoleValue(receiver))
                    .send(RECEIVER_VALUES.getOutput());

            return createSender(null)
                    .addKeyValue("{receiver}", isConsoleValue(initiator))
                    .addKeyValue("{message}", message)
                    .addKeyValue("{sender}", isConsoleValue(receiver))
                    .send("console-formatting.format");
        });
    }

    @NotNull
    public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
        return () -> PlayerUtils.createBuilder()
                .addArguments(
                        0, (s, a) -> Conversation.asReceiver(s) != null,
                        CollectionBuilder.of(Bukkit.getOnlinePlayers())
                                .filter(VanishHook::isVisible)
                                .map(p -> {
                                    Conversation c = Conversation.asInitiator(p);
                                    return c != null && c.isReceiver(sender) ? p : null;
                                })
                                .filter(Objects::nonNull)
                                .map(HumanEntity::getName).toList()
                )
                .addArgument(0, (s, a) -> Conversation.asReceiver(s) == null, "<message>")
                .addArgument(1, (s, a) -> Conversation.asReceiver(s) != null, "<message>")
                .build(sender, arguments);
    }
}
