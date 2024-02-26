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
    protected @Nullable TabPredicate executor() {
        return (sender, args) -> {
            if (isProhibited(sender, "mute.temp")) return true;

            if (args.length < 2)
                return fromSender(sender, PATH + "help.temp");

            Player target = PlayerUtils.getClosest(args[0]);
            if (target == null)
                return fromSender(sender, PATH + "not-player");

            String reason = YAMLCache.getLang().get(
                    PATH + "default-reason.mute",
                    "Not following server rules"
            );

            if (args.length > 2) {
                String temp = LangUtils.messageFromArray(args, 2);
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

            int time = convertToSeconds(args[1]);
            invoke(target.getUniqueId(), time);

            return message
                    .addKeyValue("{time}", muteParser(time))
                    .setTargets(Bukkit.getOnlinePlayers())
                    .send(YAMLCache.getLang().toList(PATH + "action.temp"));
        };
    }

    @Override
    protected @Nullable TabBuilder completer() {
        return TabBuilder.of()
                .addArguments(getPlayersNames())
                .addArgument(1, "<time>")
                .addArgument(2, "<reason>");
    }
}
