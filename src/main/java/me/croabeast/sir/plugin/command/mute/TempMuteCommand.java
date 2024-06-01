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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TempMuteCommand extends MuteCommand {

    TempMuteCommand() {
        super("tempmute");
    }

    static int convertToSeconds(String string) {
        Pattern p = Pattern.compile("^(?i)(\\d+)([smhdwy])$");

        Matcher matcher = p.matcher(string);
        if (!matcher.find()) return 1;

        char identifier = matcher.group(2).toCharArray()[0];
        int number = Integer.parseInt(matcher.group(1));

        switch (identifier) {
            case 'm':
                number = number * 60;
                break;
            case 'h': case 'H':
                number = number * 3600;
                break;
            case 'd': case 'D':
                number = number * 3600 * 24;
                break;
            case 'w': case 'W':
                number = number * 3600 * 24 * 7;
                break;
            case 'M':
                number = number * 3600 * 24 * 30;
                break;
            case 'y': case 'Y':
                number = number * 3600 * 24 * 365;
                break;
            case 's': default:
                break;
        }

        return number;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender)) return true;

        if (args.length < 2)
            return fromSender(sender).send("help.temp");

        Player target = PlayerUtils.getClosest(args[0]);
        if (target == null)
            return fromSender(sender).send("not-player");

        String reason = getLang().get("lang.default-reason.mute", "Not following server rules.");

        if (args.length > 2) {
            String temp = LangUtils.stringFromArray(args, 2);
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

        int time = convertToSeconds(args[1]);
        invoke(target.getUniqueId(), time);

        return message.addKeyValue("{time}", getParsedTime(time))
                .setTargets(Bukkit.getOnlinePlayers()).send("action.temp");
    }

    @Override
    protected @Nullable TabBuilder completer() {
        return TabBuilder.of()
                .addArguments(CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(Player::getName).toList())
                .addArgument(1, "<time>")
                .addArgument(2, "<reason>");
    }
}
