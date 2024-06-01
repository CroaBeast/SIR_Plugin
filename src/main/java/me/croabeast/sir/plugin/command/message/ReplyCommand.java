package me.croabeast.sir.plugin.command.message;

import me.croabeast.sir.api.command.tab.TabBuilder;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.command.ignore.IgnoreCommand;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.hook.VanishHook;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

final class ReplyCommand extends PrivateMessageCommand {

    ReplyCommand() {
        super("reply");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender)) return true;

        if (args.length == 0)
            return fromSender(sender).send("need-player");

        CommandSender target = SENDER_MAP.getOrDefault(
                sender,
                PlayerUtils.getClosest(args[0])
        );

        if (target == null)
            return SENDER_MAP.containsKey(sender) ?
                    fromSender(sender)
                            .addKeyValue("{target}", args[0])
                            .send("not-player") :
                    fromSender(sender).send("not-replied");

        if (target == sender)
            return fromSender(sender).send("not-yourself");

        Player player = sender instanceof Player ? (Player) sender : null;

        if (target instanceof Player) {
            final Player t = (Player) target;

            if (IgnoreCommand.isIgnoring(t, player, false)) {
                ConfigurableFile file = YAMLData.Command.Multi.IGNORE.from(true);
                boolean b = IgnoreCommand.getSettings(t).isForAll(false);

                return fromSender(sender)
                        .addKeyValue("{type}", file.get("lang.channels.msg", ""))
                        .send("ignoring." + (b ? "all" : "player"));
            }

            if (VanishHook.isVanished(t))
                return fromSender(sender).send("vanish-messages.message");
        }

        return sendMessagingResult(sender, target, args, false);
    }

    @Override
    protected @Nullable TabBuilder completer() {
        return null;
    }
}
