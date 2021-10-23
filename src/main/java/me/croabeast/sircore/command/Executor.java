package me.croabeast.sircore.command;

import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sircore.Application;
import me.croabeast.sircore.Initializer;
import me.croabeast.sircore.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.*;

import java.util.*;

public class Executor implements CommandExecutor {

    private final Application main;
    private final TextUtils text;

    private CommandSender sender;
    private Command command;

    public Executor(Application main) {
        this.main = main;
        this.text = main.getTextUtils();
        Arrays.asList("sir", "print").forEach(s -> {
            PluginCommand cmd = main.getCommand(s);
            if (cmd == null) return;
            cmd.setExecutor(this);
        });
        new Completer(main);
    }

    private void sendMessage(String path, String key, String value) {
        text.send(sender, path, new String[] {"{" + key + "}"}, value);
    }

    private boolean isCommand(String cmdName) {
        return command.getName().toLowerCase().equals(cmdName);
    }

    private boolean hasNoPerm(String perm) {
        if (main.getEventUtils().hasPerm(sender, "sir." + perm)) return false;
        sendMessage("no-permission", "PERM", "sir." + perm);
        return true;
    }

    private void loadedSections() {
        String[] keys = {"{TOTAL}", "{SECTION}"};
        for (String id : main.getMessages().getKeys(false))
            text.send(sender, "get-sections", keys, main.sections(id) + "", id);
    }

    private Set<Player> targets(String input) {
        Player player = Bukkit.getPlayer(input);
        Set<Player> players = new HashSet<>();

        if (player == sender || player != null) return Collections.singleton(player);
        if (input.matches("(?i)@a")) return new HashSet<>(Bukkit.getOnlinePlayers());

        input = input.toUpperCase();

        if (input.startsWith("WORLD:")) {
            World w = Bukkit.getWorld(input.substring(6));
            if (w == null) return new HashSet<>();
            return new HashSet<>(w.getPlayers());
        }

        if (input.startsWith("PERM:")) {
            String perm = input.substring(5);
            Bukkit.getOnlinePlayers().stream().filter(
                    p -> main.getEventUtils().hasPerm(p, perm)
            ).forEach(players::add);
            return players;
        }

        if (input.startsWith("GROUP:")) {
            String group = input.substring(6);
            Bukkit.getOnlinePlayers().stream().filter(
                    p -> {
                        boolean isGroup = false;
                        if (main.getInitializer().hasVault) {
                            String first = Initializer.Perms.getPrimaryGroup(null, p);
                            isGroup = first.matches("(?i)" + group);
                        }
                        return isGroup;
                    }
            ).forEach(players::add);
            return players;
        }

        return new HashSet<>();
    }

    private void sendPrintHelp(String name) {
        sendMessage("print-help." + name, null, null);
    }

    private void sendReminder(String input) {
        Set<Player> set = targets(input);
        if (sender instanceof Player && (set.size() == 1 && set.contains((Player) sender))) return;

        if (set.isEmpty()) sendMessage("reminder.empty", "TARGET", input);
        else if (set.size() == 1) {
            String playerName = set.toArray(new Player[1])[0].getName();
            sendMessage("reminder.success", "TARGET", playerName);
        }
        else sendMessage("reminder.success", "TARGET", input);
    }

    private void messageLogger(String type, String message) {
        String start = main.getLang().getString("logger.header");
        if (start == null || start.equals("")) return;
        if (main.choice("format")) message = IridiumAPI.process(message);

        main.doLogger(IridiumAPI.process(start));
        main.doLogger("&7[" + type + "] " + message);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        this.sender = sender;
        this.command = command;

        if (isCommand("sir")) {
            if (hasNoPerm("admin.*")) return true;

            if (args.length == 0) {
                sendMessage("main-help", "VERSION", main.getDescription().getVersion());
                return true;
            }

            if (args.length > 1) {
                sendMessage("wrong-arg", "ARG", args[args.length - 1]);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "help":
                    if (hasNoPerm("admin.help")) return true;

                    sendMessage("main-help", "VERSION", main.getDescription().getVersion());
                    return true;

                case "reload":
                    if (hasNoPerm("admin.reload")) return true;

                    main.reloadFiles();
                    loadedSections();
                    sendMessage("reload-files", null, null);
                    return true;

                case "support":
                    if (hasNoPerm("admin.support")) return true;

                    sendMessage("for-support", "LINK", "https://discord.gg/s9YFGMrjyF");
                    return true;

                default:
                    sendMessage("wrong-arg", "ARG", args[0]);
                    return true;
            }
        }

        if (isCommand("print")) {
            StringBuilder builder = new StringBuilder();
            String split = main.getConfig().getString("options.line-separator", "<n>");

            if (hasNoPerm("print.*")) return true;

            if (args.length == 0) {
                sendPrintHelp("main");
                return true;
            }

            else if (args[0].matches("(?i)targets")) {
                if (hasNoPerm("print.targets")) return true;

                if (args.length > 1) {
                    sendMessage("wrong-arg", "ARG", args[args.length - 1]);
                    return true;
                }

                sendPrintHelp("targets");
                return true;
            }

            else if (args[0].matches("(?i)ACTION-BAR")) {
                if (hasNoPerm("print.action-bar")) return true;

                if (args.length == 1) {
                    sendPrintHelp("action-bar");
                    return true;
                }

                if (args.length < 3) {
                    sendMessage("empty-message", null, null);
                    return true;
                }

                for (int i = 2; i < args.length; i++) builder.append(args[i]).append(" ");
                String message = builder.substring(0, builder.toString().length() - 1);

                sendReminder(args[1]);
                if (!targets(args[1]).isEmpty()) {
                    targets(args[1]).forEach(
                            player -> text.actionBar(player, text.parsePAPI(player, message))
                    );
                    messageLogger("ACTION-BAR", message);
                }
                return true;
            }

            else if (args[0].matches("(?i)CHAT")) {
                if (hasNoPerm("print.chat")) return true;

                if (args.length == 1) {
                    sendPrintHelp("chat");
                    return true;
                }

                if (args.length < 4) {
                    sendMessage("empty-message", null, null);
                    return true;
                }

                if (!args[2].matches("(?i)DEFAULT|CENTERED|MIXED")) {
                    sendMessage("wrong-arg", "ARG", args[1]);
                    return true;
                }

                for (int i = 3; i < args.length; i++) builder.append(args[i]).append(" ");
                String unformatted = builder.substring(0, builder.toString().length() - 1);
                List<String> message = Arrays.asList(unformatted.split(split));

                sendReminder(args[1]);
                if (!targets(args[1]).isEmpty()) {
                    if (args[2].matches("(?i)CENTERED")) {
                        targets(args[1]).forEach(
                                p -> message.forEach(s -> text.sendCentered(p, s)));
                    }
                    else if (args[2].matches("(?i)MIXED")) {
                        targets(args[1]).forEach(
                                p -> message.forEach(s -> text.sendMixed(p, s)));
                    }
                    else if (args[2].matches("(?i)DEFAULT")) {
                        targets(args[1]).forEach(
                                p -> message.forEach(s -> p.sendMessage(text.parsePAPI(p, s))));
                    }
                    messageLogger("CHAT", unformatted.replace(split, "&r" + split));
                }
            }

            else if (args[0].matches("(?i)TITLE")) {
                if (hasNoPerm("print.chat")) return true;

                if (args.length == 1) {
                    sendPrintHelp("title");
                    return true;
                }

                if (args.length < 4) {
                    sendMessage("empty-message", null, null);
                    return true;
                }

                for (int i = 3; i < args.length; i++) builder.append(args[i]).append(" ");
                String unformatted = builder.substring(0, builder.toString().length() - 1);

                sendReminder(args[1]);
                if (!targets(args[1]).isEmpty()) {
                    targets(args[1]).forEach(p -> {
                        String[] message = text.parsePAPI(p, unformatted).split(split);
                        if (args[2].matches("(?i)DEFAULT"))
                            text.title(p, message, new String[] {"20", "60", "20"});
                        else text.title(p, message, args[2].split(","));
                    });
                    messageLogger("TITLE", unformatted.replace(split, "&r" + split));
                }
            }

            else {
                sendMessage("wrong-arg", "ARG", args[0]);
                return true;
            }
        }

        return true;
    }
}
