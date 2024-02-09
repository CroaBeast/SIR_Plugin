package me.croabeast.sir.plugin.command;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.beanslib.misc.CollectionBuilder;
import me.croabeast.beanslib.utility.ArrayUtils;
import me.croabeast.sir.api.file.YAMLFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.command.tab.TabBuilder;
import me.croabeast.sir.plugin.command.tab.TabPredicate;
import me.croabeast.sir.plugin.file.YAMLCache;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public abstract class SIRCommand extends Command {

    static final Set<SIRCommand> COMMAND_SET = new LinkedHashSet<>();

    private CommandExecutor executor = SIRPlugin.getInstance();
    private TabCompleter completer = SIRPlugin.getInstance();

    private final boolean alwaysEnabled;
    @Getter
    private boolean registered = false;

    static YAMLFile commandsFile() {
        return YAMLCache.fromData("commands");
    }

    static List<String> toAliases(String name) {
        return commandsFile().toList("commands." + name + ".aliases");
    }

    @SneakyThrows
    protected SIRCommand(String name, boolean isModifiable) {
        super(name);

        if (isModifiable)
            setAliases(toAliases(name));

        alwaysEnabled = !isModifiable;
        COMMAND_SET.add(this);
    }

    protected SIRCommand(String name) {
        this(name, true);
    }

    @Nullable
    protected abstract TabPredicate executor();

    @Nullable
    protected abstract TabBuilder completer();

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
            ex.printStackTrace();
        }

        return success;
    }

    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        Validate.notNull(sender, "Sender cannot be null");
        Validate.notNull(args, "Arguments cannot be null");
        Validate.notNull(alias, "Alias cannot be null");

        List<String> completions = null;
        final TabBuilder b = completer();

        if (b != null && !b.isEmpty())
            completer = (s, c, l, a) -> b.build(s, a);

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
        return alwaysEnabled || commandsFile().get("commands." + getName() + ".enabled", true);
    }

    private static CommandMap getCommandMap() throws Exception {
        Server server = Bukkit.getServer();

        Field field = server.getClass().getDeclaredField("commandMap");
        field.setAccessible(true);

        return (CommandMap) field.get(server);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> getKnownCommands() throws Exception {
        CommandMap map = getCommandMap();

        try {
            Method m = map.getClass().getDeclaredMethod("getKnownCommands");
            m.setAccessible(true);

            return (Map<String, Command>) m.invoke(map);
        }
        catch (Exception e) {
            Field field = map.getClass().getDeclaredField("knownCommands");
            field.setAccessible(true);

            return (Map<String, Command>) field.get(map);
        }
    }

    public boolean register(boolean debug) {
        if (!isEnabled()) return false;
        if (registered) return true;

        CommandMap map;
        try {
            map = getCommandMap();
        } catch (Exception e) {
            if (debug) e.printStackTrace();
            return false;
        }

        TabPredicate tab = executor();
        if (tab != null)
            executor = (s, c, l, a) -> tab.test(s, a);

        map.register("sir", this);
        return registered = true;
    }

    public boolean register() {
        return register(false);
    }

    public boolean unregister(boolean debug) {
        if (isEnabled()) return false;
        if (!registered) return true;

        Map<String, Command> known;
        CommandMap map;

        try {
            known = getKnownCommands();
            map = getCommandMap();
        } catch (Exception e) {
            if (debug) e.printStackTrace();
            return false;
        }

        List<String> names = ArrayUtils.toList(getName());

        names.add(getLabel());
        names.addAll(getAliases());

        names.replaceAll(s -> s.toUpperCase(Locale.ENGLISH));

        SIRCommand loaded = null;

        for (String s : names) {
            Command cmd = known.getOrDefault(s, null);
            if (!Objects.equals(cmd, this) &&
                    !Objects.equals(cmd = known.get("sir:" + s), this))
                continue;

            known.remove(s, cmd);
            if (!Objects.equals(loaded, this))
                loaded = (SIRCommand) cmd;
        }
        if (loaded == null) return false;

        loaded.unregister(map);

        executor = null;
        completer = null;

        try {
            Server server = Bukkit.getServer();
            Method m = server.getClass().getDeclaredMethod("syncCommands");

            m.setAccessible(true);
            m.invoke(server);
        } catch (Exception e) {
            if (debug) e.printStackTrace();
            return false;
        }

        registered = false;
        return true;
    }

    public boolean unregister() {
        return unregister(false);
    }

    protected <T> boolean fromSender(CommandSender sender, String key, T value, String path) {
        return MessageSender.fromLoaded().setTargets(sender)
                .addKeyValue(key, value)
                .send(YAMLCache.getLang().toList(path));
    }

    protected boolean fromSender(CommandSender sender, String path) {
        return MessageSender.fromLoaded()
                .setTargets(sender)
                .send(YAMLCache.getLang().toList(path));
    }

    protected boolean isProhibited(CommandSender sender, String perm) {
        return !PlayerUtils.hasPerm(sender, "sir." + perm) &&
                fromSender(sender, "{perm}", "sir." + perm, "no-permission");
    }

    protected boolean isWrongArgument(CommandSender sender, String value) {
        return fromSender(sender, "{arg}", value, "wrong-arg");
    }

    protected List<String> getPlayersNames() {
        return CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(Player::getName).toList();
    }
}
