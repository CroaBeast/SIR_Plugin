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
    private final Initializer init;

    private final Recorder recorder;
    private final Reporter reporter;

    private final TextUtils text;
    private final PermUtils perms;

    private String[] args;
    private CommandSender sender;

    public Executor(Application main) {
        this.main = main;
        this.init = main.getInitializer();

        this.recorder = main.getRecorder();
        this.reporter = main.getReporter();

        this.text = main.getTextUtils();
        this.perms = main.getPermUtils();

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
        text.send(sender, path, key, value);
    }

    private boolean oneMessage(String path, String key, String value) {
        sendMessage(path, key, value);
        return true;
    }

    private boolean oneMessage(String path) {
        sendMessage(path, null, null);
        return true;
    }

    private boolean hasNoPerm(String perm) {
        if (perms.hasPerm(sender, "sir." + perm)) return false;
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
            main.everyPlayer().stream().filter(p -> perms.hasPerm(p, perm)).forEach(players::add);
            return players;
        }

        if (input.startsWith("GROUP:")) {
            String group = input.substring(6);
            main.everyPlayer().stream().filter(
                    p -> init.HAS_VAULT && Initializer.Perms.
                            getPrimaryGroup(null, p).
                            matches("(?i)" + group)
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

        recorder.doRecord(start);
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
                    if (reporter.isRunning()) return oneMessage("cant-start");

                    reporter.startTask();
                    return oneMessage("started");

                case "cancel":
                    if (args.length > 1) return notArgument(args[args.length - 1]);
                    if (!reporter.isRunning())  return oneMessage("cant-stop");

                    reporter.cancelTask();
                    return oneMessage("stopped");

                case "reboot":
                    if (args.length > 1) return notArgument(args[args.length - 1]);
                    if (!reporter.isRunning()) reporter.startTask();
                    return oneMessage("rebooted");

                case "preview":
                    if (sender instanceof ConsoleCommandSender) {
                        recorder.doRecord("&cYou can't preview an announce in console.");
                        return true;
                    }

                    if (args.length == 1 || reporter.getSection() == null ||
                            reporter.getSection().getConfigurationSection(args[1]) == null)
                        return oneMessage("select-announce");

                    reporter.runSection(reporter.getSection().getConfigurationSection(args[1]));
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
                    long start = System.currentTimeMillis();

                    main.getFiles().loadFiles(false);
                    if (!reporter.isRunning()) reporter.startTask();

                    init.unloadAdvances();
                    init.loadAdvances();

                    sendMessage("reload-files", "TIME",
                            (System.currentTimeMillis() - start) + "");
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
                    recorder.doRecord(sender, "<P> &7Use my secret command wisely...");
                    return true;
                }

                recorder.doRecord(rawMessage(1));
            }

            else if (args[0].matches("(?i)ACTION-BAR")) {
                if (hasNoPerm("print.action-bar")) return true;
                if (args.length == 1) return sendPrintHelp("action-bar");
                if (args.length < 3) return oneMessage("empty-message");

                String message = rawMessage(2);

                sendReminder(args[1]);
                if (!targets(args[1]).isEmpty()) {
                    targets(args[1]).forEach(p -> text.actionBar(p, text.colorize(p, message)));
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
                                p -> message.forEach(s -> p.sendMessage(text.colorize(p, s))));

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
                        String[] message = text.colorize(p, noFormat).split(split);
                        if (args[2].matches("(?i)DEFAULT"))
                            text.title(p, message, null);
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
