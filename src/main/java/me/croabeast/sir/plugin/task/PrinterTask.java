package me.croabeast.sir.plugin.task;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.croabeast.beanslib.message.MessageKey;
import me.croabeast.sir.plugin.Initializer;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.module.instance.EmojiParser;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrinterTask extends SIRTask {

    public PrinterTask() {
        super("print");
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (isProhibited(sender, "print.*")) return true;
        if (args.length == 0) return fromSender(sender, "commands.print.help.main");

        if (args[0].matches("(?i)targets")) {
            if (isProhibited(sender, "print.targets")) return true;

            if (args.length > 1) return isWrongArgument(sender, args[args.length - 1]);
            return fromSender(sender, "commands.print.help.targets");
        }

        else if (args[0].matches("(?i)-CONSOLE")) {
            if (isProhibited(sender, "print.logger")) return true;

            if (args.length < 2) {
                LogUtils.doLog(sender, "<P> &7Use my secret command wisely...");
                return true;
            }

            LogUtils.doLog(getFromArray(args, 1));
            return true;
        }

        TargetCatcher catcher = new TargetCatcher(sender, args.length > 1 ? args[1] : null);

        if (args[0].matches("(?i)ACTION-BAR")) {
            if (fromSender(sender, "print.action-bar")) return true;

            if (args.length == 1) return fromSender(sender, "commands.print.help.action-bar");
            if (args.length < 3) return fromSender(sender, "commands.print.empty-message");

            catcher.sendConfirmation();
            new Printer(catcher, args, 2).print("ACTION-BAR");
            return true;
        }

        else if (args[0].matches("(?i)CHAT")) {
            if (isProhibited(sender, "print.chat")) return true;

            if (args.length == 1)
                return fromSender(sender, "commands.print.help.chat");
            if (args.length < 4)
                return fromSender(sender, "commands.print.empty-message");

            catcher.sendConfirmation();

            boolean hasArg = args[2].matches("(?i)DEFAULT|CENTERED|MIXED");
            new Printer(catcher, args, hasArg ? 3 : 2).print("");

            return true;
        }

        else if (args[0].matches("(?i)TITLE")) {
            if (isProhibited(sender, "print.title")) return true;

            if (args.length == 1)
                return fromSender(sender, "commands.print.help.title");
            if (args.length < 4)
                return fromSender(sender, "commands.print.empty-message");

            catcher.sendConfirmation();
            new Printer(catcher, args, 3).print("TITLE");
            return true;
        }

        isWrongArgument(sender, args[0]);
        return true;
    }

    @Override
    protected @NotNull List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return generateList(args, "targets", "ACTION-BAR", "CHAT", "TITLE");

        if (args.length == 2) {
            if (args[1].matches("(?i)targets"))
                return new ArrayList<>();

            ArrayList<String> l = Lists.newArrayList("@a", "PERM:", "WORLD:");
            if (Initializer.hasVault()) l.add("GROUP:");

            return generateList(args, l, getPlayersNames());
        }

        if (args.length == 3) {
            if (args[0].matches("(?i)ACTION-BAR")) return generateList(args, "<message>");
            else if (args[0].matches("(?i)TITLE"))
                return generateList(args, "DEFAULT", "10,50,10");
            else if (args[0].matches("(?i)CHAT"))
                return generateList(args, "DEFAULT", "CENTERED", "MIXED");
        }

        if (args.length == 4) {
            if (!args[0].matches("(?i)CHAT|TITLE")) return new ArrayList<>();
            return generateList(args, "<message>");
        }

        return new ArrayList<>();
    }

    class TargetCatcher {

        private final CommandSender sender;
        private final String input;

        private Set<Player> targets = null;

        TargetCatcher(CommandSender sender, String input) {
            this.sender = sender;
            this.input = input;

            loadAllTargets();
        }

        private Set<Player> loadAllTargets() {
            if (targets != null) return targets;

            if (StringUtils.isBlank(input)) return new HashSet<>();

            Set<Player> targets = new HashSet<>();
            boolean notLoaded = true;

            Player player = Bukkit.getPlayer(input);

            if (player == sender || player != null) {
                targets = Sets.newHashSet(player);
                notLoaded = false;
            }

            if (input.matches("@[Aa]")) {
                targets = new HashSet<>(Bukkit.getOnlinePlayers());
                notLoaded = false;
            }

            String[] array = input.split(":", 2);
            final String id = array[0].toUpperCase(Locale.ENGLISH);

            if (notLoaded) {
                Stream<? extends Player> stream = Bukkit.getOnlinePlayers().stream();

                switch (id) {
                    case "WORLD":
                        World w = Bukkit.getWorld(array[1]);

                        targets = w != null ?
                                new HashSet<>(w.getPlayers()) :
                                new HashSet<>();
                        break;

                    case "PERM":
                        targets = stream.filter(p -> PlayerUtils.hasPerm(p, array[1])).
                                collect(Collectors.toSet());
                        break;

                    case "GROUP":
                        targets = stream.filter(
                                        p -> Initializer.hasVault() && Initializer.getPerms().
                                                getPrimaryGroup(null, p).
                                                matches("(?i)" + array[1])).
                                collect(Collectors.toSet());
                        break;

                    default:
                }}

            return this.targets = targets.stream().
                    filter(Objects::nonNull).collect(Collectors.toSet());
        }

        private boolean sendConfirmation() {
            if (targets.isEmpty()) {
                fromSender(sender, "{target}", input, "reminder.empty");
                return false;
            }

            if (targets.size() == 1) {
                if (sender instanceof Player && targets.contains(sender)) return false;
                fromSender(
                        sender, "{target}",
                        Lists.newArrayList(targets).get(0).getName(),
                        "reminder.success"
                );
                return true;
            }

            return fromSender(sender, "{target}", input, "reminder.success");
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    class Printer {

        private final TargetCatcher catcher;
        private final String[] args;
        private final int argumentIndex;

        private void print(String key) {
            final String center = SIRPlugin.getUtils().getCenterPrefix();
            final String message = getFromArray(args, argumentIndex);

            for (Player player : catcher.targets) {
                MessageKey k = MessageKey.matchKey(key);

                if (k == MessageKey.CHAT_KEY) {
                    String[] a = SIRPlugin.getUtils().splitLine(message);

                    for (int i = 0; i < a.length; i++) {
                        final String s = a[i];

                        if (args[2].matches("(?i)CENTERED") &&
                                !s.startsWith(center)) a[i] = center + s;

                        else if (args[2].matches("(?i)DEFAULT") &&
                                s.startsWith(center))
                            a[i] = s.substring(center.length());
                    }

                    for (String s : a)
                        k.execute(player, EmojiParser.parse(player, s));
                    continue;
                }

                else if (k == MessageKey.TITLE_KEY) {
                    String[] d = SIRPlugin.getUtils().getKeysDelimiters();

                    String time = null;
                    try {
                        time = Integer.parseInt(args[2]) + "";
                    } catch (Exception ignored) {}

                    time = time != null ? (":" + time) : "";

                    k.execute(player,
                            d[0] + k.getFlag() + time + d[1] + " " +
                            EmojiParser.parse(player, message)
                    );
                    continue;
                }

                k.execute(player, EmojiParser.parse(player, message));
            }
        }
    }
}
