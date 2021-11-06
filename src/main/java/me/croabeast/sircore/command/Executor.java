package me.croabeast.sircore.command;

import me.croabeast.iridiumapi.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.objects.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class Executor implements CommandExecutor {

    private final Application main;
    private final Records records;
    private final TextUtils text;
    private final EventUtils utils;
    private final Announcer announcer;

    private String[] args;
    private CommandSender sender;
    private Command command;

    public Executor(Application main) {
        this.main = main;
        this.records = main.getRecords();
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
        this.announcer = main.getAnnouncer();
        Arrays.asList("announcer", "sir", "print").forEach(s -> {
            PluginCommand executor = main.getCommand(s);
            if (executor != null) executor.setExecutor(this);
        });
        new Completer(main);
    }

    private void sendMessage(String path, String key, String value) {
        text.send(sender, path, new String[] {"{" + key + "}"}, value);
    }

    private boolean oneMessage(String path, String key, String value) {
        sendMessage(path, key, value);
        return true;
    }

    private boolean isCommand(String cmdName) {
        return command.getName().toLowerCase().equals(cmdName);
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

    private void loadedSections() {
        String[] keys = {"{TOTAL}", "{SECTION}"};
        main.getMessages().getKeys(false).forEach(s -> {
            String sect = text.getSections(s) + "";
            text.send(sender, "get-sections", keys, sect, s);
        });
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
            Bukkit.getOnlinePlayers().stream().filter(p -> utils.hasPerm(p, perm))
                    .forEach(players::add);
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

        if (start == null || start.equals("")) return;
        if (text.getOption(1, "format-logger"))
            line = IridiumAPI.process(line);

        records.doRecord(start);
        main.getLogger().info("[" + type + "] " + line);
    }

    private String rawMessage(int size) {
        StringBuilder builder = new StringBuilder();
        String[] args = this.args;

        for (int i = size; i < args.length; i++)
            builder.append(args[i]).append(" ");

        return builder.substring(0, builder.toString().length() - 1);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        this.sender = sender;
        this.command = command;
        this.args = args;

        if (isCommand("sir")) {
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
                    loadedSections();
                    if (!announcer.isRunning()) announcer.startTask();
                    sendMessage("reload-files", null, null);
                    if (!text.getOption(1, "enabled") && main.getAnnouncer().getDelay() == 0)
                        records.doRecord(sender,
                                "", "<P> &7Both main features of &eS.I.R. &7are disabled.",
                                "<P> &cIt's better to delete the plugin instead doing that...", ""
                        );
                    return true;

                case "support":
                    if (hasNoPerm("admin.support")) return true;
                    return oneMessage("for-support", "LINK", "https://discord.gg/s9YFGMrjyF");

                default: return notArgument(args[0]);
            }
        }

        if (isCommand("print")) {
            String split = text.getValue("split");

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

                if (args.length < 3) return oneMessage("empty-message", null, null);

                String message = rawMessage(2);

                sendReminder(args[1]);
                if (!targets(args[1]).isEmpty()) {
                    targets(args[1]).forEach(
                            player -> text.actionBar(player, text.parsePAPI(player, message))
                    );
                    messageLogger("ACTION-BAR", message);
                }
            }

            else if (args[0].matches("(?i)CHAT")) {
                if (hasNoPerm("print.chat")) return true;

                if (args.length == 1) return sendPrintHelp("chat");

                if (args.length < 4) return oneMessage("empty-message", null, null);

                if (!args[2].matches("(?i)DEFAULT|CENTERED|MIXED")) return notArgument(args[1]);

                String unformatted = rawMessage(3);
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

                if (args.length == 1) return sendPrintHelp("title");

                if (args.length < 4) return oneMessage("empty-message", null, null);

                String unformatted = rawMessage(3);

                sendReminder(args[1]);
                if (!targets(args[1]).isEmpty()) {
                    targets(args[1]).forEach(p -> {
                        String[] message = text.parsePAPI(p, unformatted).split(split);
                        if (args[2].matches("(?i)DEFAULT"))
                            text.title(p, message, new String[] {"20", "60", "20"});
                        else text.title(p, message, args[2].split(","));
                    });
                    String colorSplit = IridiumAPI.process("&r" + split);
                    messageLogger("TITLE", unformatted.replace(split, colorSplit));
                }
            }

            else sendMessage("wrong-arg", "ARG", args[0]);
        }

        if (isCommand("announcer")) {
            if (hasNoPerm("announcer.*")) return true;

            if (args.length == 0) return oneMessage("announcer-help", null, null);

            if (args.length > 2) return notArgument(args[args.length - 1]);

            ConfigurationSection id = main.getAnnounces().getConfigurationSection("messages");

            switch (args[0].toLowerCase()) {
                case "start":
                    if (args.length > 1) return notArgument(args[args.length - 1]);

                    if (announcer.isRunning()) return oneMessage("cant-start", null, null);

                    announcer.startTask();
                    return oneMessage("started", null, null);

                case "cancel":
                    if (args.length > 1) return notArgument(args[args.length - 1]);

                    if (!announcer.isRunning())  return oneMessage("cant-stop", null, null);

                    announcer.cancelTask();
                    return oneMessage("stopped", null, null);

                case "reboot":
                    if (args.length > 1) return notArgument(args[args.length - 1]);

                    if (announcer.isRunning()) announcer.cancelTask();
                    announcer.startTask();
                    return oneMessage("rebooted", null, null);

                case "preview":
                    if (sender instanceof ConsoleCommandSender) {
                        records.doRecord(
                                "&cYou can't preview an announce if you are the console."
                        );
                        return true;
                    }

                    if (args.length == 1 || id == null || id.getConfigurationSection(args[1]) == null)
                        return oneMessage("select-announce", null, null);

                    announcer.runSection(Objects.requireNonNull(id.getConfigurationSection(args[1])));
                    return true;

                default: return notArgument(args[args.length - 1]);
            }
        }

        return true;
    }
}
