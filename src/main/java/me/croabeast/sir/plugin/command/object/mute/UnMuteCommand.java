package me.croabeast.sir.plugin.command.object.mute;

import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.utility.LangUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

class UnMuteCommand extends MuteCommand {

    protected UnMuteCommand() {
        super("unmute");
    }

    @Override
    protected @Nullable TabPredicate executor() {
        return (sender, args) -> {
            if (isProhibited(sender, "unmute")) return true;

            if (args.length < 1)
                return fromSender(sender, PATH + "help.un-mute");

            Player target = PlayerUtils.getClosestPlayer(args[0]);
            if (target == null)
                return fromSender(sender, PATH + "not-player");

            String reason = YAMLCache.getLang().get(
                    PATH + "default-reason.un-mute",
                    "Not following server rules"
            );

            if (args.length > 1) {
                String temp = LangUtils.messageFromArray(args, 1);
                if (temp != null) reason = temp;
            }

            MessageSender message = MessageSender.fromLoaded()
                    .addKeyValue("{reason}", reason)
                    .addKeyValue("{target}", target.getName());

            if (!isMuted(target))
                return message.setTargets(sender)
                        .send(YAMLCache.getLang().toList(PATH + "is-muted.un-mute"));

            MUTED_MAP.remove(target.getUniqueId());

            return message.send(YAMLCache.getLang().toList(PATH + "action.un-mute"));
        };
    }

    @Override
    protected @Nullable TabBuilder completer() {
        return TabBuilder.of()
                .addArguments(getPlayersNames())
                .addArgument(1, "<reason>");
    }
}
