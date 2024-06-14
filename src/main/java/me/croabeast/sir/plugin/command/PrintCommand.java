package me.croabeast.sir.plugin.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.croabeast.beans.BeansLib;
import me.croabeast.beans.message.MessageChannel;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.sir.api.command.SIRCommand;
import me.croabeast.sir.api.command.tab.TabBuilder;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.SIRInitializer;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.module.chat.EmojiParser;
import me.croabeast.sir.plugin.module.chat.TagsParser;
import me.croabeast.sir.plugin.util.LangUtils;
import me.croabeast.sir.plugin.util.PlayerUtils;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

final class PrintCommand extends SIRCommand {

    PrintCommand() {
        super("print", false);

        editSubCommand("targets", (sender, args) -> args.length <= 0 ?
                fromSender(sender).send("help.targets") :
                isWrongArgument(sender, args[args.length - 1]));

        editSubCommand("chat", (sender, args) -> {
            TargetCatcher catcher = new TargetCatcher(sender, args.length > 0 ? args[0] : null);

            if (args.length == 0)
                return fromSender(sender).send("help.chat");
            if (args.length < 3)
                return fromSender(sender).send("empty-message");

            catcher.sendConfirmation();

            boolean hasArg = args[1].matches("(?i)DEFAULT|CENTERED|MIXED");
            new Printer(catcher, args, hasArg ? 2 : 1).print("");

            return true;
        });

        editSubCommand("action-bar", (sender, args) -> {
            TargetCatcher catcher = new TargetCatcher(sender, args.length > 0 ? args[0] : null);

            if (args.length == 0)
                return fromSender(sender).send("help.action-bar");
            if (args.length < 2)
                return fromSender(sender).send("empty-message");

            catcher.sendConfirmation();
            new Printer(catcher, args, 1).print("ACTION-BAR");

            return true;
        });

        editSubCommand("title", (sender, args) -> {
            TargetCatcher catcher = new TargetCatcher(sender, args.length > 0 ? args[0] : null);

            if (args.length == 0)
                return fromSender(sender).send("help.title");
            if (args.length < 2)
                return fromSender(sender).send("empty-message");

            catcher.sendConfirmation();
            new Printer(catcher, args, 2).print("TITLE");

            return true;
        });
    }

    @NotNull
    protected ConfigurableFile getLang() {
        return YAMLData.Command.Single.PRINT.from();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        return false;
    }

    @Override
    protected @Nullable TabBuilder completer() {
        return null;
    }

    private class TargetCatcher {

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
                CollectionBuilder<Player> stream =
                        CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(p -> p);

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
                                .toSet();
                        break;

                    case "GROUP":
                        targets = stream.filter(
                                p -> {
                                    Permission perms = SIRInitializer.getPermsMeta();
                                    String temp = "(?i)" + array[1];

                                    return perms != null &&
                                            perms.getPrimaryGroup(null, p).matches(temp);
                                })
                                .toSet();
                        break;

                    default:
                }}

            return this.targets =
                    CollectionBuilder.of(targets).filter(Objects::nonNull).toSet();
        }

        private boolean sendConfirmation() {
            if (targets.isEmpty()) {
                fromSender(sender).addKeyValue("{target}", input).send("reminder.empty");
                return false;
            }

            if (targets.size() == 1) {
                String target = Lists.newArrayList(targets).get(0).getName();

                return (!(sender instanceof Player) || !targets.contains(sender)) &&
                        fromSender(sender)
                                .addKeyValue("{target}", target)
                                .send("reminder.success");
            }

            return fromSender(sender).addKeyValue("{target}", input).send("reminder.success");
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private class Printer {

        private final TargetCatcher catcher;
        private final String[] args;
        private final int index;

        private void print(String key) {
            String message = LangUtils.stringFromArray(args, index);
            String center = BeansLib.getLib().getCenterPrefix();

            for (Player player : catcher.targets) {
                MessageChannel c = MessageChannel.fromName(key);

                if (c == MessageChannel.CHAT) {
                    String[] a = BeansLib.getLib().splitString(message);

                    for (int i = 0; i < a.length; i++) {
                        final String s = a[i];

                        if (args[2].matches("(?i)CENTERED") &&
                                !s.startsWith(center)) a[i] = center + s;

                        else if (args[2].matches("(?i)DEFAULT") &&
                                s.startsWith(center))
                            a[i] = s.substring(center.length());
                    }

                    for (String s : a) {
                        s = TagsParser.parse(player, s);
                        c.send(player, EmojiParser.parse(player, s));
                    }
                    continue;
                }

                else if (c == MessageChannel.TITLE) {
                    String[] d = MessageChannel.getDelimiters();

                    String time = null;
                    try {
                        time = Integer.parseInt(args[2]) + "";
                    } catch (Exception ignored) {}

                    time = time != null ? (":" + time) : "";
                    message = TagsParser.parse(player, message);

                    c.send(player,
                            d[0] + c.getName() + time + d[1] + " " +
                                    EmojiParser.parse(player, message)
                    );
                    continue;
                }

                message = TagsParser.parse(player, message);
                c.send(player, EmojiParser.parse(player, message));
            }
        }
    }
}
