package me.croabeast.sir.plugin.command;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.sir.plugin.SIRCollector;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.command.object.message.DirectTask;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public abstract class SIRCommand extends BukkitCommand {

    private static final Set<SIRCommand> COMMAND_SET = new LinkedHashSet<>();
    private static boolean areCommandsLoaded = false;

    private final CommandExecutor executor;
    private final TabCompleter completer;

    private final boolean alwaysEnabled;
    @Getter private boolean registered = false;

    static List<String> toAliases(String name) {
        return FileCache.COMMANDS_DATA.toList("commands." + name + ".aliases");
    }

    @SneakyThrows
    protected SIRCommand(String name, boolean isModifiable) {
        super(name);

        SIRPlugin.checkAccess(SIRCommand.class);
        SIRPlugin plugin = SIRPlugin.getInstance();

        if (isModifiable)
            setAliases(toAliases(name));

        TabPredicate tab = executor();
        executor = tab != null ?
                ((s, c, l, a) -> tab.test(s, a)) :
                plugin;

        TabBuilder b = completer();

        completer = b != null && !b.isEmpty() ?
                ((s, c, l, a) -> b.build(s, a)) :
                plugin;

        alwaysEnabled = !isModifiable;
        COMMAND_SET.add(this);
    }

    protected SIRCommand(String name) {
        this(name, true);
    }

    @Nullable
    protected abstract TabPredicate executor();

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!testPermission(sender)) return true;
        boolean success;

        try {
            success = executor.onCommand(sender, this, label, args);
        } catch (Throwable ex) {
            success = MessageSender.fromLoaded().setTargets(sender).send(
                    "<P> &7Error handling the command " +
                            getName() +
                            ": &c" + ex.getLocalizedMessage()
            );
        }

        return success;
    }

    @Nullable
    protected abstract TabBuilder completer();

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");

        List<String> completions = null;

        try {
            if (completer != null)
                completions = completer.onTabComplete(sender, this, alias, args);

            if (completions == null && executor instanceof TabCompleter)
                completions = ((TabCompleter) executor).onTabComplete(sender, this, alias, args);
        }
        catch (Throwable ignored) {}

        return completions == null ? super.tabComplete(sender, alias, args) : completions;
    }

    public boolean isEnabled() {
        return alwaysEnabled || FileCache.COMMANDS_DATA.getValue("commands." + getName() + ".enabled", true);
    }

    private static CommandMap getCommandMap() throws Exception {
        Server server = Bukkit.getServer();

        Field field = server.getClass().getDeclaredField("commandMap");
        field.setAccessible(true);

        return (CommandMap) field.get(server);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> getKnownCommands() throws Exception {
        final CommandMap map = getCommandMap();
        Map<String, Command> commands;

        try {
            commands = (Map<String, Command>) map
                    .getClass()
                    .getDeclaredMethod("getKnownCommands")
                    .invoke(map);
        }
        catch (Exception e) {
            final Class<?> c = SimpleCommandMap.class;

            Field field = c.getDeclaredField("knownCommands");
            field.setAccessible(true);

            commands = (Map<String, Command>) field.get(map);
        }

        return commands;
    }

    @SneakyThrows
    public boolean register() {
        if (!isEnabled() || registered) return false;

        try {
            getCommandMap().register("sir", this);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        final String pluginName = SIRPlugin.getInstance()
                .getDescription()
                .getName().toLowerCase(Locale.ENGLISH);

        if (!getKnownCommands().containsKey(getName())) {
            getKnownCommands().put(getName(), this);
            getKnownCommands().put(pluginName + ":" + getName(), this);
        }

        if (getAliases().isEmpty()) return true;

        for (String alias : getAliases()) {
            if (getKnownCommands().containsKey(alias)) continue;

            getKnownCommands().put(alias, this);
            getKnownCommands().put(pluginName + ":" + alias, this);
        }

        return registered = true;
    }

    protected <T> boolean fromSender(CommandSender sender, String key, T value, String path) {
        return MessageSender.fromLoaded().setTargets(sender)
                .addKeyValue(key, value)
                .send(FileCache.getLang().toList(path));
    }

    protected boolean fromSender(CommandSender sender, String path) {
        return MessageSender.fromLoaded()
                .setTargets(sender)
                .send(FileCache.getLang().toList(path));
    }

    protected boolean isProhibited(CommandSender sender, String perm) {
        return !PlayerUtils.hasPerm(sender, "sir." + perm) &&
                fromSender(sender, "{perm}", "sir." + perm, "no-permission");
    }

    protected boolean isWrongArgument(CommandSender sender, String value) {
        return fromSender(sender, "{arg}", value, "wrong-arg");
    }

    protected List<String> getPlayersNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    @SneakyThrows
    public static void registerCommands() {
        SIRPlugin.checkAccess(SIRCommand.class);

        if (!areCommandsLoaded) {
            SIRCollector.from("me.croabeast.sir.plugin.task.object")
                    .filter(c -> !c.getName().contains("$"))
                    .filter(SIRCommand.class::isAssignableFrom)
                    .filter(c -> c != SIRCommand.class && c != DirectTask.class)
                    .collect().forEach(c -> {
                        try {
                            Constructor<?> cons = c.getDeclaredConstructor();

                            cons.setAccessible(true);
                            cons.newInstance();
                            cons.setAccessible(false);
                        }
                        catch (Exception ignored) {}
                    });

            areCommandsLoaded = true;
        }

        COMMAND_SET.forEach(SIRCommand::register);
    }
}
