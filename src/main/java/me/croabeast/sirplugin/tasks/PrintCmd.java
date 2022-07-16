package me.croabeast.sirplugin.tasks;

import com.google.common.collect.*;
import me.croabeast.beanslib.utilities.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.extensions.*;
import me.croabeast.sirplugin.utilities.LogUtils;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

import static me.croabeast.sirplugin.SIRPlugin.*;

public class PrintCmd extends SIRTask {

    @Override
    public String getName() {
        return "print";
    }

    @Override
    protected CommandExecutor getExecutor() {
        return (sender, command, label, args) -> {
            setSender(sender);
            if (hasNoPerm("print.*")) return true;
            if (args.length == 0) return oneMessage("commands.print.help.main");

            String center = getUtils().centerPrefix();
            String split = getUtils().lineSeparator();

            if (args[0].matches("(?i)targets")) {
                if (hasNoPerm("print.targets")) return true;

                if (args.length > 1) return notArgument(args[args.length - 1]);
                return oneMessage("commands.print.help.targets");
            }

            else if (args[0].matches("(?i)-CONSOLE")) {
                if (hasNoPerm("print.logger")) return true;

                if (args.length < 2) {
                    LogUtils.doLog(sender, "<P> &7Use my secret command wisely...");
                    return true;
                }
                LogUtils.doLog(rawMessage(args, 1));
            }

            else if (args[0].matches("(?i)ACTION-BAR")) {
                if (hasNoPerm("print.action-bar")) return true;

                if (args.length == 1) return oneMessage("commands.print.help.action-bar");
                if (args.length < 3) return oneMessage("commands.print.empty-message");

                String message = rawMessage(args, 2);

                sendReminder(args[1]);
                if (!catchTargets(args[1]).isEmpty()) {
                    catchTargets(args[1]).forEach(p ->
                            TextUtils.sendActionBar(p, getUtils().colorize(p, message)));
                    messageLogger("ACTION-BAR", message);
                }
            }

            else if (args[0].matches("(?i)CHAT")) {
                if (hasNoPerm("print.chat")) return true;

                if (args.length == 1) return oneMessage("commands.print.help.chat");
                if (args.length < 4) return oneMessage("commands.print.empty-message");

                if (!args[2].matches("(?i)DEFAULT|CENTERED|MIXED")) return notArgument(args[1]);

                String noFormat = rawMessage(args, 3);
                List<String> message = Lists.newArrayList(noFormat.split(split));

                sendReminder(args[1]);

                if (!catchTargets(args[1]).isEmpty()) {
                    catchTargets(args[1]).forEach(p -> message.forEach(s -> {
                        if (args[2].matches("(?i)CENTERED") && !s.startsWith(center))
                            s = center + s;
                        else if (args[2].matches("(?i)DEFAULT") && s.startsWith(center))
                            s = s.substring(center.length());
                        p.spigot().sendMessage(getUtils().stringToJson(p, s));
                    }));

                    messageLogger("CHAT", noFormat);
                }
            }

            else if (args[0].matches("(?i)TITLE")) {
                if (hasNoPerm("print.title")) return true;

                if (args.length == 1) return oneMessage("commands.print.help.title");
                if (args.length < 4) return oneMessage("commands.print.empty-message");

                String noFormat = rawMessage(args, 3);

                sendReminder(args[1]);
                if (!catchTargets(args[1]).isEmpty()) {
                    catchTargets(args[1]).forEach(p -> {
                        String[] array = args[2].matches("(?i)DEFAULT") ? null : args[2].split(","),
                                msg = getUtils().colorize(p, TextUtils.stripJson(noFormat)).split(split);
                        getUtils().sendTitle(p, msg, array);
                    });
                    messageLogger("TITLE", noFormat.replace(split, "&r" + split));
                }
            }

            else return notArgument(args[0]);
            return true;
        };
    }

    @Override
    protected TabCompleter getCompleter() {
        return (sender, command, alias, args) -> {
            setArgs(args);
            if (args.length == 1)
                return resultList("targets", "ACTION-BAR", "CHAT", "TITLE");

            if (args.length == 2) {
                if (args[1].matches("(?i)targets"))
                    return new ArrayList<>();

                List<String> l = Arrays.asList("@a", "PERM:", "WORLD:");
                if (Initializer.hasVault()) l.add("GROUP:");
                return resultList(l, onlinePlayers());
            }

            if (args.length == 3) {
                if (args[0].matches("(?i)ACTION-BAR")) return resultList("<message>");
                else if (args[0].matches("(?i)TITLE"))
                    return resultList("DEFAULT", "10,50,10");
                else if (args[0].matches("(?i)CHAT"))
                    return resultList("DEFAULT", "CENTERED", "MIXED");
            }

            if (args.length == 4) {
                if (!args[0].matches("(?i)CHAT|TITLE")) return new ArrayList<>();
                return resultList("<message>");
            }

            return new ArrayList<>();
        };
    }
}
