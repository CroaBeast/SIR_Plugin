package me.croabeast.sir.plugin.command.mute;

import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.sir.api.command.tab.TabBuilder;
import me.croabeast.sir.plugin.util.LangUtils;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

class UnMuteCommand extends MuteCommand {

    UnMuteCommand() {
        super("unmute");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender)) return true;

        if (args.length < 1)
            return fromSender(sender).send("help.un-mute");

        Player target = PlayerUtils.getClosest(args[0]);
        if (target == null)
            return fromSender(sender).send("not-player");

        String reason = getLang().get("lang.default-reason.mute", "Not following server rules.");

        if (args.length > 1) {
            String temp = LangUtils.stringFromArray(args, 1);
            if (temp != null) reason = temp;
        }

        MessageSender message = fromSender(sender)
                .addKeyValue("{reason}", reason)
                .addKeyValue("{target}", target.getName());

        if (isMuted(target)) {
            MUTED_MAP.remove(target.getUniqueId()).cancel();
            return message.send("action.un-mute");
        }

        return message.send("is-muted.un-mute");
    }

    @Override
    protected @Nullable TabBuilder completer() {
        return TabBuilder.of()
                .addArguments(CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(Player::getName).toList())
                .addArgument(1, "<reason>");
    }
}
