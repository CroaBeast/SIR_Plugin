package me.croabeast.sir.plugin.command.object;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.croabeast.beanslib.Beans;
import me.croabeast.beanslib.message.MessageExecutor;
import me.croabeast.sir.plugin.SIRInitializer;
import me.croabeast.sir.plugin.module.object.EmojiParser;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.utility.LangUtils;
import me.croabeast.sir.plugin.utility.LogUtils;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrinterTask extends SIRCommand {

    PrinterTask() {
        super("print");
    }

    @Override
    protected TabPredicate executor() {
        return (sender, args) -> {
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

                LogUtils.doLog(LangUtils.messageFromArray(args, 1));
                return true;
            }

            TargetCatcher catcher = new TargetCatcher(sender, args.length > 1 ? args[1] : null);

            if (args[0].matches("(?i)ACTION[-_]BAR")) {
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
        };
    }

    @Override
    @NotNull
    protected TabBuilder completer() {
        TabBuilder builder = TabBuilder.of()
                .addArgument("sir.print.targets", "targets")
                .addArgument("sir.print.chat", "chat")
                .addArgument("sir.print.action-bar", "action-bar")
                .addArgument("sir.print.TITLE", "TITLE")
                .addArguments(1, "@a", "perm:", "world:")
                .addArguments(1, getPlayersNames());

        if (SIRInitializer.hasVault())
            builder.addArgument(1, "group:");

        builder.setIndex(2)
                .addArgument(
                        (s, a) -> a[0].matches("(?i)action-bar"),
                        "<message>"
                )
                .addArguments(
                        (s, a) -> a[0].matches("(?i)chat"),
                        "default", "centered", "mixed"
                )
                .addArguments(
                        (s, a) -> a[0].matches("(?i)title"),
                        "default", "10,50,10"
                );

        return builder.addArgument(3,
                (s, a) -> a[0].matches("(?i)chat|title"),
                "<message>"
        );
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
                        targets = stream
                                .filter(p -> PlayerUtils.hasPerm(p, array[1]))
                                .collect(Collectors.toSet());
                        break;

                    case "GROUP":
                        targets = stream.filter(
                                        p -> {
                                            Permission perms = SIRInitializer.getPerms();
                                            if (perms == null) return false;

                                            return perms.getPrimaryGroup(null, p).
                                                    matches("(?i)" + array[1]);
                                        }).
                                collect(Collectors.toSet());
                        break;

                    default:
                }}

            return this.targets = targets.stream()
                    .filter(Objects::nonNull).collect(Collectors.toSet());
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

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Printer {

        private final TargetCatcher catcher;
        private final String[] args;
        private final int argumentIndex;

        private void print(String key) {
            final String center = Beans.getCenterPrefix();
            final String message = LangUtils.messageFromArray(args, argumentIndex);

            for (Player player : catcher.targets) {
                MessageExecutor k = MessageExecutor.matchKey(key);

                if (k == MessageExecutor.CHAT_EXECUTOR) {
                    String[] a = Beans.splitLine(message);

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

                else if (k == MessageExecutor.TITLE_EXECUTOR) {
                    String[] d = Beans.getKeysDelimiters();

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
