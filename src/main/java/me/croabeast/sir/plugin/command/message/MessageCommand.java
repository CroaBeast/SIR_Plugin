package me.croabeast.sir.plugin.command.message;

import me.croabeast.lib.CollectionBuilder;
import me.croabeast.sir.api.command.tab.TabBuilder;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.command.ignore.IgnoreCommand;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.hook.VanishHook;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

final class MessageCommand extends PrivateMessageCommand {

    MessageCommand() {
        super("msg");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender)) return true;

        Player player = sender instanceof Player ? (Player) sender : null;

        if (args.length == 0)
            return fromSender(sender).send("need-player");

        Player target = PlayerUtils.getClosest(args[0]);

        if (target == null)
            return fromSender(sender)
                    .addKeyValue("{target}", args[0])
                    .send("not-player");

        if (target == sender)
            return fromSender(sender).send("not-yourself");

        if (IgnoreCommand.isIgnoring(target, player, false)) {
            ConfigurableFile file = YAMLData.Command.Multi.IGNORE.from(true);

            return fromSender(sender)
                    .addKeyValue("{type}", file.get("lang.channels.msg", ""))
                    .send("ignoring." + (player == null ? "all" : "player"));
        }

        if (VanishHook.isVanished(target))
            return fromSender(sender).send("vanish-messages.message");

        addFromData(sender, target);
        return sendMessagingResult(sender, target, args, true);
    }

    @NotNull
    protected TabBuilder completer() {
        return TabBuilder
                .of().addArguments(
                        CollectionBuilder.of(Bukkit.getOnlinePlayers())
                                .filter(VanishHook::isVisible)
                                .map(HumanEntity::getName).toList()
                )
                .addArgument(1, "<message>");
    }
}
