package me.croabeast.sir.plugin.command.object.mute;

import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.utility.LangUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

class PermMuteCommand extends MuteCommand {

    protected PermMuteCommand() {
        super("mute");
    }

    @Override
    protected @Nullable TabPredicate executor() {
        return (sender, args) -> {
            if (isProhibited(sender, "mute")) return true;

            if (args.length == 0)
                return fromSender(sender, PATH + "help.perm");

            Player target = PlayerUtils.getClosestPlayer(args[0]);
            if (target == null)
                return fromSender(sender, PATH + "not-player");

            String reason = YAMLCache.getLang().get(
                    PATH + "default-reason.mute",
                    "Not following server rules"
            );

            if (args.length > 1) {
                String temp = LangUtils.messageFromArray(args, 1);
                if (temp != null) reason = temp;
            }

            MessageSender message = MessageSender.fromLoaded()
                    .addKeyValue("{reason}", reason)
                    .addKeyValue("{target}", target.getName());

            if (isMuted(target)) {
                TempMuteTask task = MUTED_MAP.get(target.getUniqueId());
                message = message.setTargets(sender);

                return task.isTemporary() ?
                        message
                                .addKeyValue("{time}", muteParser(task.restTicksFromNow() / 20))
                                .send(YAMLCache.getLang().toList(PATH + "is-muted.temp")) :
                        message
                                .send(YAMLCache.getLang().toList(PATH + "is-muted.perm"));
            }

            invokeEmpty(target.getUniqueId());

            return message
                    .setTargets(Bukkit.getOnlinePlayers())
                    .send(YAMLCache.getLang().toList(PATH + "action.perm"));
        };
    }

    @Override
    protected @Nullable TabBuilder completer() {
        return TabBuilder.of()
                .addArguments(getPlayersNames())
                .addArgument(1, "<reason>");
    }
}
