package me.croabeast.sir.api.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.reflect.Craft;
import me.croabeast.lib.util.ArrayUtils;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.SIRExtension;
import me.croabeast.sir.api.command.tab.TabBuilder;
import me.croabeast.sir.api.command.tab.TabPredicate;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The SIRCommand abstract class provides a base implementation for custom commands
 * in the SIR plugin framework.
 *
 * <p> It extends the BukkitCommand class and adds functionality for managing command
 * options, permissions, and subcommands.
 */
public abstract class SIRCommand extends BukkitCommand {

    static final Set<Command> COMMAND_SET = new LinkedHashSet<>();

    /**
     * Gets the unique identifier for this command instance.
     */
    @Getter
    private final UUID uuid = UUID.randomUUID();

    private final SIRPlugin plugin;
    private Options options;

    private final String wildcard;

    /**
     * Indicates whether this command is registered.
     */
    @Getter
    private boolean registered = false;
    private boolean permsLoaded = false;

    private final boolean modifiable;

    private CommandExecutor executor;
    protected final Map<List<String>, SubCommand> subCommands;

    private final List<String> mainAlias;
    private final Map<String, List<String>> aliasesMap;



    @UtilityClass
    static class CommandFile {

        ConfigurableFile getMainFile() {
            return YAMLData.Command.getMain();
        }

        ConfigurationSection getCommandSection(String name) {
            return getMainFile().getSection("commands." + name);
        }

        List<String> getWrongArgumentMessages() {
            return getMainFile().toStringList("lang.wrong-arg");
        }

        List<String> getNoPermissionMessages() {
            return getMainFile().toStringList("lang.no-permission");
        }
    }

    @Getter(AccessLevel.PACKAGE)
    private static class Options {

        private final String permission;
        private final List<String> subCommands;

        private final boolean enabled;
        private final List<String> aliases;

        private final boolean override;

        Options(String name) throws IllegalStateException {
            ConfigurationSection s = CommandFile.getCommandSection(name);
            Objects.requireNonNull(s);

            final String path = "permissions.";

            this.permission = s.getString(path + "main", "");
            Exceptions.validate(StringUtils::isNotBlank, permission);

            enabled = s.getBoolean("enabled", true);
            aliases = TextUtils.toList(s, "aliases");

            subCommands = TextUtils.toList(s, path + "subcommands");
            override = s.getBoolean("override-existing");
        }
    }

    /**
     * Constructs a new SIRCommand with the specified name and modifiability.
     *
     * @param name       the name of the command
     * @param modifiable indicates whether the command is modifiable
     */
    @SneakyThrows
    protected SIRCommand(String name, boolean modifiable) {
        super(name);

        this.plugin = SIRPlugin.getInstance();

        this.options = new Options(name);
        this.modifiable = modifiable;

        this.subCommands = new HashMap<>();

        options.getSubCommands().forEach(s -> {
            SubCommand sub = new AbstractSub(s);
            subCommands.put(sub.getNames(), sub);
        });

        wildcard = subCommands.isEmpty() ?
                null :
                getPermission() + ".*";

        mainAlias = Collections.singletonList("sir:" + name + " $1-");
        aliasesMap = new HashMap<>();

        getAliases().forEach(s ->
                aliasesMap.put(
                        s,
                        Collections.singletonList("sir:" + s + " $1-")
                ));

        COMMAND_SET.add(this);
    }

    /**
     * Constructs a new SIRCommand with the specified name.
     *
     * @param name the name of the command
     */
    protected SIRCommand(String name) {
        this(name, true);
    }

    @Nullable
    protected SIRExtension getParent() {
        return null;
    }

    private void loadCommandOptionsFromFile() {
        if (modifiable) options = new Options(getName());
    }

    /**
     * Gets the configurable file for language settings associated with this command.
     *
     * @return the configurable file for language settings
     */
    @NotNull
    protected abstract ConfigurableFile getLang();

    /**
     * Gets the configurable file for data associated with this command.
     *
     * @return the configurable file for data, or null if not applicable
     */
    @Nullable
    protected ConfigurableFile getData() {
        return null;
    }

    /**
     * Executes the command for the specified command sender with the given arguments.
     *
     * @param sender the command sender
     * @param args   the command arguments
     * @return true if the command execution was successful, otherwise false
     */
    protected abstract boolean execute(CommandSender sender, String[] args);

    /**
     * Gets the tab completion builder associated with this command.
     *
     * @return the tab completion builder, or null if not applicable
     */
    @Nullable
    protected abstract TabBuilder completer();

    /**
     * Indicates whether this command is enabled.
     *
     * @return true if the command is enabled, otherwise false
     */
    public boolean isEnabled() {
        return !modifiable || (getParent() != null && getParent().isEnabled()) || options.isEnabled();
    }

    public boolean isOverriding() {
        return options.isOverride();
    }

    /**
     * Gets the permission required to execute this command.
     *
     * @return the permission required to execute this command
     */
    @NotNull
    public String getPermission() {
        return options.getPermission();
    }

    /**
     * Sets the permission required to execute this command from its file.
     *
     * @param permission the permission to set, file-dependent
     */
    public void setPermission(String permission) {
        loadCommandOptionsFromFile();
    }

    class CommandDisplayer extends MessageSender {

        private CommandDisplayer() {
            super(MessageSender.loaded());
        }

        @Override
        public boolean send(String... strings) {
            if (strings.length != 1)
                throw new NullPointerException("Needs only a single path");

            return super.send(getLang().toStringList("lang." + strings[0]));
        }
    }

    /**
     * Tests whether the specified command sender has the permission to execute this command.
     *
     * @param target the command sender to test
     * @return true if the command sender has the permission, otherwise false
     */
    public boolean testPermissionSilent(@NotNull CommandSender target) {
        return PlayerUtils.hasPerm(target, getPermission()) ||
                PlayerUtils.hasPerm(target, wildcard);
    }

    /**
     * Displays a message indicating that the command sender does not have permission to execute the command.
     *
     * @param sender the command sender
     * @return true if the sender does not have the permission and the message was sent successfully, otherwise false
     */
    protected boolean isProhibited(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;

        return !testPermissionSilent(sender) && MessageSender.loaded()
                .setTargets(player).addKeyValue("{perm}", getPermission())
                .send(CommandFile.getNoPermissionMessages());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean testPermission(@NotNull CommandSender target) {
        return !isProhibited(target);
    }

    /**
     * Displays a message indicating that the command sender provided an incorrect argument.
     *
     * @param sender the command sender
     * @param arg    the incorrect argument provided
     * @return true if the message was sent successfully, otherwise false
     */
    protected boolean isWrongArgument(CommandSender sender, String arg) {
        Player player = sender instanceof Player ? (Player) sender : null;

        return MessageSender.loaded()
                .setTargets(player).addKeyValue("{arg}", arg)
                .send(CommandFile.getWrongArgumentMessages());
    }

    /**
     * Creates a new MessageSender instance for sending messages from the command sender.
     *
     * @param sender the command sender
     * @return the MessageSender instance
     */
    protected MessageSender fromSender(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        return new CommandDisplayer().setTargets(player);
    }

    /**
     * Gets the aliases associated with this command.
     *
     * @return the list of aliases
     */
    @NotNull
    public List<String> getAliases() {
        return options.getAliases();
    }

    /**
     * Sets the aliases for this command from its file.
     *
     * @param aliases the list of aliases to set, file-dependent
     * @return the SIRCommand instance
     */
    @NotNull
    public SIRCommand setAliases(@NotNull List<String> aliases) {
        loadCommandOptionsFromFile();
        return this;
    }

    private static void addPerm(String perm) {
        try {
            Permission permission = new Permission(perm);
            Bukkit.getPluginManager().addPermission(permission);
        }
        catch (Exception ignored) {}
    }

    private static void removePerm(String perm) {
        Bukkit.getPluginManager().removePermission(perm);
    }

    private static void loadCommandPermissions(SIRCommand cmd) {
        Map<List<String>, SubCommand> subCommands = cmd.subCommands;

        if (cmd.permsLoaded) {
            removePerm(cmd.getPermission());

            if (!subCommands.isEmpty()) {
                subCommands.values().forEach(s ->
                        removePerm(s.getPermission()));

                removePerm(cmd.wildcard);
            }

            cmd.permsLoaded = false;
        }

        addPerm(cmd.getPermission());

        if (!subCommands.isEmpty()) {
            subCommands.values().forEach(s ->
                    addPerm(s.getPermission()));

            addPerm(cmd.wildcard);
        }

        cmd.permsLoaded = true;
    }

    /**
     * Finds a subcommand by name or alias.
     *
     * @param name the name or alias of the subcommand
     * @return the subcommand if found, otherwise null
     */
    protected SubCommand findSubCommandByNameOrAlias(String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        SubCommand sub = null;

        for (Map.Entry<List<String>, SubCommand> e : subCommands.entrySet())
            if (e.getKey().contains(name)) {
                sub = e.getValue();
                break;
            }

        return sub;
    }

    /**
     * Executes the command for the specified command sender with the given label and arguments.
     *
     * @param sender the command sender
     * @param label  the alias of the command
     * @param args   the command arguments
     *
     * @return true if the command execution was successful, otherwise false
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        boolean success;

        if (!subCommands.isEmpty() && args.length > 0) {
            SubCommand sub = findSubCommandByNameOrAlias(args[0]);
            if (sub == null)
                return isWrongArgument(sender, args[0]);

            if (sub.isPermitted(sender)) {
                String[] newArgs = new String[args.length - 1];

                if (args.length > 1)
                    System.arraycopy(args, 1, newArgs, 0, args.length - 1);

                try {
                    success = sub.execute(sender, newArgs);
                } catch (Throwable e) {
                    Player p = sender instanceof Player ? (Player) sender : null;

                    success = MessageSender.loaded().setTargets(p).send(
                            "<P> &7Error executing the command " +
                                    getName() + ": &c" +
                                    e.getLocalizedMessage()
                    );
                    e.printStackTrace();
                }

                return success;
            }
        }

        try {
            success = executor.onCommand(sender, this, label, args);
        } catch (Throwable e) {
            Player p = sender instanceof Player ? (Player) sender : null;

            success = MessageSender.loaded().setTargets(p).send(
                    "<P> &7Error executing the command " +
                            getName() + ": &c" +
                            e.getLocalizedMessage()
            );
            e.printStackTrace();
        }

        return success;
    }

    /**
     * Tab-completes the command for the specified command sender with the given alias and arguments.
     *
     * @param sender the command sender
     * @param alias  the alias of the command
     * @param args   the command arguments
     *
     * @return a list of tab-completions
     */
    @NotNull
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        List<String> comps;

        try {
            final TabBuilder builder = completer();

            comps = builder != null && !builder.isEmpty() ?
                    builder.build(sender, args) :
                    plugin.onTabComplete(sender, this, alias, args);

            final CommandExecutor ex = executor;

            if (comps == null && ex instanceof TabCompleter) {
                TabCompleter t = (TabCompleter) ex;
                comps = t.onTabComplete(sender, this, alias, args);
            }

            return Objects.requireNonNull(comps);
        }
        catch (Throwable e) {
            Player p = sender instanceof Player ? (Player) sender : null;

            MessageSender.loaded().setTargets(p).send(
                    "<P> &7Error completing the command " +
                            getName() + ": &c" +
                            e.getLocalizedMessage()
            );
            e.printStackTrace();

            return super.tabComplete(sender, alias, args);
        }
    }

    /**
     * Edits the behavior of a subcommand using a predicate.
     *
     * @param name      the name of the subcommand
     * @param predicate the predicate to apply
     *
     * @return true if the subcommand behavior was successfully edited, otherwise false
     */
    protected boolean editSubCommand(String name, TabPredicate predicate) {
        if (StringUtils.isBlank(name) || predicate == null)
            return false;

        SubCommand subCommand = findSubCommandByNameOrAlias(name);
        if (subCommand == null) return false;

        ((AbstractSub) subCommand).setPredicate(predicate);
        return true;
    }

    static FileConfiguration commandsConfig() {
        return Craft.Server.createCommandsConfiguration();
    }

    /**
     * Registers this command with the Bukkit command map.
     *
     * @return true if registration was successful, otherwise false
     */
    public final boolean register() {
        loadCommandOptionsFromFile();

        if (registered) return true;
        if (!isEnabled()) return false;

        String name = plugin.getName().toLowerCase();

        try {
            Map<String, Command> map = Craft.Server.getKnownCommands();
            boolean changed = false;

            if (!map.containsKey(getName())) map.put(getName(), this);
            map.put(name + ":" + getName(), this);

            FileConfiguration config = commandsConfig();

            if (isOverriding() &&
                    !(map.get(getName()) instanceof SIRCommand)) {
                config.set("aliases." + getName(), mainAlias);
                changed = true;
            }

            for (String alias : getAliases()) {
                if (!map.containsKey(alias)) map.put(alias, this);
                map.put(name + ":" + alias, this);

                if (isOverriding() &&
                        !(map.get(getName()) instanceof SIRCommand)) {
                    List<String> list = aliasesMap.get(alias);
                    if (list == null) continue;

                    config.set("aliases." + alias, list);
                    if (!changed) changed = true;
                }
            }

            try {
                this.executor = (s, c, l, a) -> execute(s, a);
            } catch (Exception e) {
                this.executor = plugin;
            }

            register(Craft.Server.getCommandMap());
            loadCommandPermissions(this);

            if (changed)
                try {
                    config.save(Craft.Server.getCommands());
                    Craft.Server.reloadCommandsFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            Craft.Server.INSTANCE.call("syncCommands");
            return registered = true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Unregisters this command from the Bukkit command map.
     *
     * @return true if un-registration was successful, otherwise false
     */
    @SuppressWarnings("all")
    public final boolean unregister() {
        loadCommandOptionsFromFile();
        if (!modifiable) return !registered;

        if (!registered) return true;
        if (isEnabled()) return false;

        try {
            unregister(Craft.Server.getCommandMap());

            Map<String, Command> map = Craft.Server.getKnownCommands();
            map.values().removeIf(c -> {
                SIRCommand cmd = c instanceof SIRCommand ? (SIRCommand) c : null;
                if (cmd != null) {
                    loadCommandPermissions(cmd);
                    return cmd.getUuid().equals(getUuid());
                }

                return false;
            });

            FileConfiguration config = commandsConfig();
            boolean changed = false;

            if (config.contains("aliases." + getName())) {
                config.set("aliases." + getName(), null);
                changed = true;

                for (String alias : getAliases()) {
                    if (!config.contains("aliases." + alias))
                        continue;

                    config.set("aliases." + alias, null);
                }
            }

            if (changed)
                try {
                    config.save(Craft.Server.getCommands());
                    Craft.Server.reloadCommandsFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            Craft.Server.INSTANCE.call("syncCommands");
            return !(registered = false);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * The interface for subcommands.
     */
    protected interface SubCommand {

        /**
         * Gets the permission required to execute this subcommand.
         * @return the permission required
         */
        @NotNull String getPermission();

        /**
         * Checks if the command sender can execute this subcommand.
         *
         * @param sender the command sender
         * @return true if the command sender is allowed, otherwise false
         */
        boolean isPermitted(CommandSender sender);

        /**
         * Gets the name of this subcommand.
         * @return the name of this subcommand
         */
        @NotNull String getName();

        /**
         * Gets the aliases of this subcommand.
         * @return the aliases of this subcommand
         */
        @NotNull List<String> getAliases();

        /**
         * Gets the names, including aliases, of this subcommand.
         * @return the names of this subcommand
         */
        @NotNull List<String> getNames();

        /**
         * Executes this subcommand for the specified command sender with the given arguments.
         *
         * @param sender the command sender
         * @param args   the arguments for this subcommand
         *
         * @return true if the execution was successful, otherwise false
         */
        boolean execute(CommandSender sender, String[] args);
    }

    @Getter
    private class AbstractSub implements SubCommand {

        private final String name;

        private final List<String> names = new ArrayList<>();
        private final List<String> aliases = new ArrayList<>();

        @Setter @Getter(AccessLevel.NONE)
        private TabPredicate predicate;
        private final String permission;

        AbstractSub(String name) {
            permission = SIRCommand.this.getPermission() + '.' + name;

            List<String> list = ArrayUtils.toList(name.split(";"));
            list.replaceAll(s -> s.toLowerCase(Locale.ENGLISH));

            this.name = list.get(0);
            this.names.addAll(list);

            if (list.size() <= 1) return;

            List<String> result = new ArrayList<>(list);
            result.remove(this.name);

            this.aliases.addAll(result);
        }

        public boolean isPermitted(CommandSender sender) {
            return PlayerUtils.hasPerm(sender, SIRCommand.this.wildcard) ||
                    PlayerUtils.hasPerm(sender, permission);
        }

        @Override
        public boolean execute(CommandSender sender, String[] args) {
            return Objects.requireNonNull(predicate, "Sub command is not properly set up").test(sender, args);
        }

        @Override
        public String toString() {
            return "SubCommand{name='" + name + '\'' +
                    ", aliases=" + aliases +
                    ", permission='" + permission + '\'' + '}';
        }
    }
}
