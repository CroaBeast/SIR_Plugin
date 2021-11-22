package me.croabeast.sircore.command;

import me.croabeast.iridiumapi.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.objects.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

public class Executor {

    private final Application main;
    private final Records records;
    private final TextUtils text;
    private final EventUtils utils;
    private final Announcer announcer;

    private String[] args;
    private CommandSender sender;

    public Executor(Application main) {
        this.main = main;
        this.records = main.getRecords();
        this.announcer = main.getAnnouncer();
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();

        registerCmd("announcer", announcerCmd());
        registerCmd("sir", mainCmd());
        registerCmd("print", printCmd());
        new Completer(main);
    }

    private void registerCmd(String name, CommandExecutor executor) {
        PluginCommand cmd = main.getCommand(name);
        if (cmd != null) cmd.setExecutor(executor);
    }

    private void sendMessage(String path, String key, String value) {
        if (key == null && value == null) text.send(sender, path);
        else text.send(sender, path, new String[] {"{" + key + "}"}, value);
    }

    private boolean oneMessage(String path, String key, String value) {
        sendMessage(path, key, value);
        return true;
    }

    private boolean oneMessage(String path) {
        text.send(sender, path);
        return true;
    }

    private boolean hasNoPerm(String perm) {
        if (utils.hasPerm(sender, "sir." + perm)) return false;

        sendMessage("no-permission", "PERM", "sir." + perm);
        return true;
    }

    private boolean notArgument(String arg) {
        sendMessage("wrong-arg", "ARG", arg);
        return true;
    }

    private boolean sendPrintHelp(String name) {
        sendMessage("print-help." + name, null, null);
        return true;
    }

    private Set<Player> targets(String input) {
        Player player = Bukkit.getPlayer(input);
        Set<Player> players = new HashSet<>();

        if (player == sender || player != null) return Collections.singleton(player);
        if (input.matches("(?i)@a")) return new HashSet<>(main.everyPlayer());

        input = input.toUpperCase();

        if (input.startsWith("WORLD:")) {
            World w = Bukkit.getWorld(input.substring(6));
            if (w == null) return new HashSet<>();
            return new HashSet<>(w.getPlayers());
        }

        if (input.startsWith("PERM:")) {
            String perm = input.substring(5);
            main.everyPlayer().stream().filter(p -> utils.hasPerm(p, perm)).forEach(players::add);
            return players;
        }

        if (input.startsWith("GROUP:")) {
            String group = input.substring(6);
            main.everyPlayer().stream().filter(
                    p -> {
                        boolean isGroup = false;
                        if (main.getInitializer().HAS_VAULT) {
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

    private String rawMessage(int size) {
        StringBuilder builder = new StringBuilder();
        String[] args = this.args;

        for (int i = size; i < args.length; i++)
            builder.append(args[i]).append(" ");

        return builder.substring(0, builder.toString().length() - 1);
    }

    private void sendReminder(String input) {
        Set<Player> set = targets(input);
        if (sender instanceof Player && set.size() == 1 &&
                set.contains((Player) sender)) return;

        if (set.isEmpty()) sendMessage("reminder.empty", "TARGET", input);
        else if (set.size() == 1) {
            String playerName = set.toArray(new Player[1])[0].getName();
            sendMessage("reminder.success", "TARGET", playerName);
        }
        else sendMessage("reminder.success", "TARGET", input);
    }

    private void messageLogger(String type, String line) {
        String start = main.getLang().getString("logger.header");
        boolean format = text.getOption(1, "format-logger");

        if (start == null || start.equals("")) return;
        if (format) line = IridiumAPI.process(line);

        records.doRecord(start);
        main.getLogger().info("[" + type + "] " + line);
    }

    private CommandExecutor announcerCmd() {
        return (sender, command, label, args) -> {
            this.sender = sender;
            this.args = args;

            if (hasNoPerm("announcer.*")) return true;
            if (args.length == 0) return oneMessage("announcer-help");
            if (args.length > 2) return notArgument(args[args.length - 1]);

            switch (args[0].toLowerCase()) {
                case "start":
                    if (args.length > 1) return notArgument(args[args.length - 1]);
                    if (announcer.isRunning()) return oneMessage("cant-start");

                    announcer.startTask();
                    return oneMessage("started", null, null);

                case "cancel":
                    if (args.length > 1) return notArgument(args[args.length - 1]);
                    if (!announcer.isRunning())  return oneMessage("cant-stop");

                    announcer.cancelTask();
                    return oneMessage("stopped", null, null);

                case "reboot":
                    if (args.length > 1) return notArgument(args[args.length - 1]);

                    if (announcer.isRunning()) announcer.cancelTask();
                    announcer.startTask();
                    return oneMessage("rebooted");

                case "preview":
                    if (sender instanceof ConsoleCommandSender) {
                        records.doRecord(
                                "&cYou can't preview an announce if you are the console."
                        );
                        return true;
                    }

                    if (args.length == 1 || announcer.getID() == null ||
                            announcer.getID().getConfigurationSection(args[1]) == null)
                        return oneMessage("select-announce");

                    announcer.runSection(announcer.getID().getConfigurationSection(args[1]));
                    return true;

                default: return notArgument(args[args.length - 1]);
            }
        };
    }

    private CommandExecutor mainCmd() {
        return (sender, command, label, args) -> {
            this.sender = sender;
            this.args = args;
            if (hasNoPerm("admin.*")) return true;
            if (args.length == 0)
                return oneMessage("main-help", "VERSION", main.getDescription().getVersion());
            if (args.length > 1) return notArgument(args[args.length - 1]);

            switch (args[0].toLowerCase()) {
                case "help":
                    if (hasNoPerm("admin.help")) return true;
                    return oneMessage("main-help", "VERSION", main.getDescription().getVersion());

                case "reload":
                    if (hasNoPerm("admin.reload")) return true;

                    main.getInitializer().reloadFiles();
                    if (!announcer.isRunning()) announcer.startTask();
                    sendMessage("reload-files", null, null);
                    main.getInitializer().checkFeatures(sender);
                    return true;

                case "support":
                    if (hasNoPerm("admin.support")) return true;
                    return oneMessage("for-support", "LINK", "https://discord.gg/s9YFGMrjyF");

                default: return notArgument(args[0]);
            }
        };
    }

    private CommandExecutor printCmd() {
        return (sender, command, label, args) -> {
            this.sender = sender;
            this.args = args;

            String split = text.getSplit();

            if (hasNoPerm("print.*")) return true;
            if (args.length == 0) return sendPrintHelp("main");

            else if (args[0].matches("(?i)targets")) {
                if (hasNoPerm("print.targets")) return true;
                if (args.length > 1) return notArgument(args[args.length - 1]);
                return sendPrintHelp("targets");
            }

            else if (args[0].matches("(?i)-CONSOLE")) {
                if (hasNoPerm("print.logger")) return true;

                if (args.length < 2) {
                    records.doRecord(sender, "<P> &7Use my secret command wisely...");
                    return true;
                }

                records.doRecord(rawMessage(1));
            }

            else if (args[0].matches("(?i)ACTION-BAR")) {
                if (hasNoPerm("print.action-bar")) return true;
                if (args.length == 1) return sendPrintHelp("action-bar");
                if (args.length < 3) return oneMessage("empty-message");

                String message = rawMessage(2);

                sendReminder(args[1]);
                if (!targets(args[1]).isEmpty()) {
                    targets(args[1]).forEach(p -> text.actionBar(p, text.parsePAPI(p, message)));
                    messageLogger("ACTION-BAR", message);
                }
            }

            else if (args[0].matches("(?i)CHAT")) {
                if (hasNoPerm("print.chat")) return true;
                if (args.length == 1) return sendPrintHelp("chat");
                if (args.length < 4) return oneMessage("empty-message");
                if (!args[2].matches("(?i)DEFAULT|CENTERED|MIXED")) return notArgument(args[1]);

                String noFormat = rawMessage(3);
                List<String> message = Arrays.asList(noFormat.split(split));

                sendReminder(args[1]);
                if (!targets(args[1]).isEmpty()) {
                    if (args[2].matches("(?i)CENTERED"))
                        targets(args[1]).forEach(p -> message.forEach(s -> text.sendCentered(p, s)));

                    else if (args[2].matches("(?i)DEFAULT"))
                        targets(args[1]).forEach(
                                p -> message.forEach(s -> p.sendMessage(text.parsePAPI(p, s))));

                    else if (args[2].matches("(?i)MIXED"))
                        targets(args[1]).forEach(p -> message.forEach(s -> text.sendMixed(p, s)));

                    messageLogger("CHAT", noFormat.replace(split, "&r" + split));
                }
            }

            else if (args[0].matches("(?i)TITLE")) {
                if (hasNoPerm("print.chat")) return true;
                if (args.length == 1) return sendPrintHelp("title");
                if (args.length < 4) return oneMessage("empty-message");

                String noFormat = rawMessage(3);

                sendReminder(args[1]);
                if (!targets(args[1]).isEmpty()) {
                    targets(args[1]).forEach(p -> {
                        String[] message = text.parsePAPI(p, noFormat).split(split);
                        if (args[2].matches("(?i)DEFAULT"))
                            text.title(p, message, new String[] {"10", "50", "10"});
                        else text.title(p, message, args[2].split(","));
                    });
                    messageLogger("TITLE", noFormat.replace(split, "&r" + split));
                }
            }

            else sendMessage("wrong-arg", "ARG", args[0]);
            return true;
        };
    }
}
