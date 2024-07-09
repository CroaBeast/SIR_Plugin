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
import java.util.function.Supplier;

final class MessageCommand extends PrivateCommand {

    MessageCommand() {
        super("msg");
    }

    @NotNull
    public Executable getExecutable() {
        return Executable.from((sender, args) -> {
            if (!isPermitted(sender)) return true;

            Player player = !(sender instanceof Player) ?
                    null :
                    (Player) sender;
            if (player != null && MuteCommand.isMuted(player))
                return createSender(sender).send("is-muted");

            if (args.length == 0)
                return createSender(sender).send("need-player");

            Player target = PlayerUtils.getClosest(args[0]);

            if (target == null)
                return createSender(sender)
                        .addKeyValue("{target}", args[0])
                        .send("not-player");

            if (target == sender)
                return createSender(sender).send("not-yourself");

            if (isIgnoring(target, player)) return true;

            if (VanishHook.isVanished(target))
                return createSender(sender).send("vanish-messages.message");

            new Conversation(sender, target);

            String message = LangUtils.stringFromArray(args, 0);
            if (StringUtils.isBlank(message))
                return createSender(sender).send("empty-message");

            INITIATOR_VALUES.playSound(sender);
            RECEIVER_VALUES.playSound(target);

            MessageSender msg = createSender(sender)
                    .addKeyValue("{message}", message).setLogger(false);

            msg.copy()
                    .addKeyValue("{receiver}", isConsoleValue(target))
                    .send(INITIATOR_VALUES.getOutput());

            msg.copy().setTargets(target)
                    .addKeyValue("{sender}", isConsoleValue(sender))
                    .send(RECEIVER_VALUES.getOutput());

            return createSender(null).setLogger(true)
                    .addKeyValue("{receiver}", isConsoleValue(target))
                    .addKeyValue("{message}", message)
                    .addKeyValue("{sender}", isConsoleValue(sender))
                    .send("console-formatting.format");
        });
    }

    @NotNull
    public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
        return () -> PlayerUtils.createBuilder()
                .addArguments(0,
                        CollectionBuilder.of(Bukkit.getOnlinePlayers())
                                .filter(VanishHook::isVisible)
                                .map(HumanEntity::getName).toList()
                )
                .addArgument(1, "<message>").build(sender, arguments);
    }
}
