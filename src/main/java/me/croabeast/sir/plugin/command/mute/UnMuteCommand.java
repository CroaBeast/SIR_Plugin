package me.croabeast.sir.plugin.command.mute;

import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.command.Executable;
import me.croabeast.lib.command.TabBuilder;
import me.croabeast.sir.plugin.util.LangUtils;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

final class UnMuteCommand extends MuteCommand {

    UnMuteCommand() {
        super("unmute");
    }

    @NotNull
    public Executable getExecutable() {
        return Executable.from((sender, args) -> {
            if (!isPermitted(sender)) return true;

            if (args.length < 1)
                return createSender(sender).send("help.un-mute");

            Player target = PlayerUtils.getClosest(args[0]);
            if (target == null)
                return createSender(sender).send("not-player");

            String reason = getLang().get("lang.default-reason.mute", "Not following server rules.");

            if (args.length > 1) {
                String temp = LangUtils.stringFromArray(args, 1);
                if (temp != null) reason = temp;
            }

            MessageSender message = createSender(sender)
                    .addKeyValue("{reason}", reason)
                    .addKeyValue("{target}", target.getName());

            if (isMuted(target)) {
                MUTED_MAP.remove(target.getUniqueId()).cancel();
                return message.send("action.un-mute");
            }

            return message.send("is-muted.un-mute");
        });
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return PlayerUtils.createBuilder()
                .addArguments(0, getOnlineNames())
                .addArgument(1, "<reason>");
    }
}
