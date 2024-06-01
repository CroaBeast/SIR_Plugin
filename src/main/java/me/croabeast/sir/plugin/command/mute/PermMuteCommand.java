package me.croabeast.sir.plugin.command.mute;

import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.sir.api.command.tab.TabBuilder;
import me.croabeast.sir.plugin.util.LangUtils;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class PermMuteCommand extends MuteCommand {

    PermMuteCommand() {
        super("mute");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender)) return true;

        if (args.length == 0)
            return fromSender(sender).send("help.perm");

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
            TempMuteTask task = MUTED_MAP.get(target.getUniqueId());
            String path = "perm";

            if (task.isTemporary()) {
                message.addKeyValue("{time}", task.getParsedTime());
                path = "temp";
            }

            return message.send("is-muted." + path);
        }

        invokeEmpty(target.getUniqueId());
        return message.setTargets(Bukkit.getOnlinePlayers()).send("action.perm");
    }

    @Override
    protected TabBuilder completer() {
        return TabBuilder.of()
                .addArguments(CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(Player::getName).toList())
                .addArgument(1, "<reason>");
    }
}
