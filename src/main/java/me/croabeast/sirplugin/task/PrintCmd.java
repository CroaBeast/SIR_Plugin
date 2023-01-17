package me.croabeast.sirplugin.task;

import com.google.common.collect.*;
import me.croabeast.beanslib.object.display.JsonMessage;
import me.croabeast.beanslib.utility.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.module.EmParser;
import me.croabeast.sirplugin.object.instance.*;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.utility.LogUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

import static me.croabeast.sirplugin.SIRPlugin.*;

public class PrintCmd extends SIRTask {

    @Override
    public String getName() {
        return "print";
    }

    Set<Player> catchTargets(CommandSender sender, String input) {
        Player player = Bukkit.getPlayer(input);
        Set<Player> players = new HashSet<>();

        if (player == sender || player != null) return Collections.singleton(player);
        if (input.matches("(?i)@a")) return new HashSet<>(Bukkit.getOnlinePlayers());

        input = input.toUpperCase(Locale.ENGLISH);

        if (input.startsWith("WORLD:")) {
            World w = Bukkit.getWorld(input.substring(6));
            if (w == null) return new HashSet<>();
            return new HashSet<>(w.getPlayers());
        }

        if (input.startsWith("PERM:")) {
            String perm = input.substring(5);
            Bukkit.getOnlinePlayers().stream().filter(p ->
                    PlayerUtils.hasPerm(p, perm)).forEach(players::add);
            return players;
        }

        if (input.startsWith("GROUP:")) {
            String group = input.substring(6);
            Bukkit.getOnlinePlayers().stream().filter(
                    p -> Initializer.hasVault() && Initializer.getPerms().
                            getPrimaryGroup(null, p).
                            matches("(?i)" + group)
            ).forEach(players::add);
            return players;
        }

        return new HashSet<>();
    }

    void sendReminder(CommandSender sender, String input) {
        Set<Player> set = catchTargets(sender, input);
        if (sender instanceof Player && set.size() == 1 &&
                set.contains((Player) sender)) return;

        if (set.isEmpty()) sendMessage(sender, "reminder.empty", "target", input);
        else if (set.size() == 1) {
            String playerName = set.toArray(new Player[1])[0].getName();
            sendMessage(sender, "reminder.success", "target", playerName);
        }
        else sendMessage(sender, "reminder.success", "target", input);
    }

    void messageLogger(String type, String line) {
        String start = FileCache.LANG.get().getString("logger.header");
        if (start == null || start.equals("")) return;

        LogUtils.doLog(start, "&7[" + type + "] " +
                getUtils().colorize(null, null, line));
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (hasNoPerm(sender, "print.*")) return true;
        if (args.length == 0) return oneMessage(sender, "commands.print.help.main");

        final String center = getUtils().centerPrefix(),
                split = getUtils().lineSeparator();

        if (args[0].matches("(?i)targets")) {
            if (hasNoPerm(sender, "print.targets")) return true;

            if (args.length > 1) return notArgument(sender, args[args.length - 1]);
            return oneMessage(sender, "commands.print.help.targets");
        }

        else if (args[0].matches("(?i)-CONSOLE")) {
            if (hasNoPerm(sender, "print.logger")) return true;

            if (args.length < 2) {
                LogUtils.doLog(sender, "<P> &7Use my secret command wisely...");
                return true;
            }
            LogUtils.doLog(rawMessage(args, 1));
        }

        else if (args[0].matches("(?i)ACTION-BAR")) {
            if (hasNoPerm(sender, "print.action-bar")) return true;

            if (args.length == 1) return oneMessage(sender, "commands.print.help.action-bar");
            if (args.length < 3) return oneMessage(sender, "commands.print.empty-message");

            String message = rawMessage(args, 2);
            sendReminder(sender, args[1]);

            if (!catchTargets(sender, args[1]).isEmpty()) {
                catchTargets(sender, args[1]).forEach(p ->
                        TextUtils.sendActionBar(p, getUtils().colorize(null, p, message)));
                messageLogger("ACTION-BAR", message);
            }
        }

        else if (args[0].matches("(?i)CHAT")) {
            if (hasNoPerm(sender, "print.chat")) return true;

            if (args.length == 1) return oneMessage(sender, "commands.print.help.chat");
            if (args.length < 4) return oneMessage(sender, "commands.print.empty-message");

            if (!args[2].matches("(?i)DEFAULT|CENTERED|MIXED")) return notArgument(sender, args[2]);

            String noFormat = rawMessage(args, 3);
            List<String> message = Lists.newArrayList(noFormat.split(split));

            sendReminder(sender, args[1]);

            if (!catchTargets(sender, args[1]).isEmpty()) {
                catchTargets(sender, args[1]).forEach(p -> message.forEach(s -> {
                    if (args[2].matches("(?i)CENTERED") && !s.startsWith(center))
                        s = center + s;
                    else if (args[2].matches("(?i)DEFAULT") && s.startsWith(center))
                        s = s.substring(center.length());
                    new JsonMessage(getUtils(), p, p, EmParser.parseEmojis(p, s)).send();
                }));

                messageLogger("CHAT", noFormat);
            }
        }

        else if (args[0].matches("(?i)TITLE")) {
            if (hasNoPerm(sender, "print.title")) return true;

            if (args.length == 1) return oneMessage(sender, "commands.print.help.title");
            if (args.length < 4) return oneMessage(sender, "commands.print.empty-message");

            String noFormat = rawMessage(args, 3);
            sendReminder(sender, args[1]);

            if (!catchTargets(sender, args[1]).isEmpty()) {
                catchTargets(sender, args[1]).forEach(p -> {
                    int[] i = new int[] {10, 50, 10};
                    String[] array = args[2].split(",");

                    if (!args[2].matches("(?i)DEFAULT") && array.length == 3) {
                        for (int x = 0; x < array.length; x++) {
                            try {
                                i[x] = Integer.parseInt(array[x]);
                            } catch (Exception ignored) {}
                        }
                    }

                    String[] msg = getUtils().colorize(null, p,
                            TextUtils.stripJson(noFormat)).split(split);
                    TextUtils.sendTitle(p, msg[0], msg[1], i[0], i[1], i[2]);
                });

                messageLogger("TITLE", noFormat.replace(split, "&r" + split));
            }
        }

        notArgument(sender, args[0]);
        return true;
    }

    @Override
    protected List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return resultList(args, "targets", "ACTION-BAR", "CHAT", "TITLE");

        if (args.length == 2) {
            if (args[1].matches("(?i)targets"))
                return new ArrayList<>();

            List<String> l = Lists.newArrayList("@a", "PERM:", "WORLD:");
            if (Initializer.hasVault()) l.add("GROUP:");
            return resultList(args, l, onlinePlayers());
        }

        if (args.length == 3) {
            if (args[0].matches("(?i)ACTION-BAR")) return resultList(args, "<message>");
            else if (args[0].matches("(?i)TITLE"))
                return resultList(args, "DEFAULT", "10,50,10");
            else if (args[0].matches("(?i)CHAT"))
                return resultList(args, "DEFAULT", "CENTERED", "MIXED");
        }

        if (args.length == 4) {
            if (!args[0].matches("(?i)CHAT|TITLE")) return new ArrayList<>();
            return resultList(args, "<message>");
        }

        return new ArrayList<>();
    }
}
