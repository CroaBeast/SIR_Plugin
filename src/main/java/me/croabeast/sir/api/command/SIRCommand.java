package me.croabeast.sir.api.command;

import lombok.AccessLevel;
import lombok.Getter;
import me.croabeast.beans.message.MessageSender;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.command.*;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.api.SIRExtension;
import me.croabeast.sir.api.file.ConfigurableFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.YAMLData;
import me.croabeast.sir.plugin.util.PlayerUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public abstract class SIRCommand extends BukkitCommand {

    static final Set<Command> COMMAND_SET = new LinkedHashSet<>();

    private Options options;
    private final boolean modifiable;

    boolean registerCalled = false;
    boolean unregisterCalled = false;

    static ConfigurableFile getMain() {
        return YAMLData.Command.getMain();
    }

    static Player fromSender(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    @Getter(AccessLevel.PACKAGE)
    private static class Options {

        private final String permission;
        private final List<String> subCommands;

        private final boolean enabled;
        private final List<String> aliases;

        private final boolean override;

        Options(String name) throws IllegalStateException {
            ConfigurationSection s = getMain().getSection("commands." + name);
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

    protected SIRCommand(String name, boolean modifiable) {
        super(SIRPlugin.getInstance(), name);

        this.modifiable = modifiable;
        this.options = new Options(name);

        options.getSubCommands()
                .forEach(s -> registerSubCommand(new SubCommand(this, s) {
                    @Override
                    public boolean isPermitted(CommandSender sender, boolean log) {
                        return PlayerUtils.hasPerm(sender, getWildcardPermission()) ||
                                PlayerUtils.hasPerm(sender, this.getPermission());
                    }
                }));

        setExecutingError((sender, e) -> {
            e.printStackTrace();

            return MessageSender.loaded()
                    .setTargets(fromSender(sender))
                    .send("<P> &7Error executing the command " +
                            getName() + ": &c" +
                            e.getLocalizedMessage());
        });

        setCompletingError((sender, e) -> {
            e.printStackTrace();

            return MessageSender.loaded()
                    .setTargets(fromSender(sender))
                    .send("<P> &7Error completing the command " +
                            getName() + ": &c" +
                            e.getLocalizedMessage());
        });

        setWrongArgumentAction((sender, arg) -> {
            MessageSender.loaded()
                    .setTargets(fromSender(sender))
                    .addKeyValue("{arg}", arg)
                    .send(getMain().toStringList("lang.wrong-arg"));
            return true;
        });
        COMMAND_SET.add(this);
    }

    /**
     * Constructs a new SIRCommand with the specified name.
     * @param name the name of the command
     */
    protected SIRCommand(String name) {
        this(name, true);
    }

    @Nullable
    protected SIRExtension getParent() {
        return null;
    }

    private void loadOptionsFromFile() {
        if (modifiable) options = new Options(getName());
    }

    /**
     * Gets the configurable file for language settings associated with this command.
     * @return the configurable file for language settings
     */
    @NotNull
    protected abstract ConfigurableFile getLang();

    /**
     * Gets the configurable file for data associated with this command.
     * @return the configurable file for data, or null if not applicable
     */
    @Nullable
    protected ConfigurableFile getData() {
        return null;
    }

    public boolean isEnabled() {
        return !modifiable || (getParent() != null && getParent().isEnabled()) || options.isEnabled();
    }

    @Override
    public boolean isOverriding() {
        return options.isOverride();
    }

    @NotNull
    public String getPermission() {
        return options.getPermission();
    }

    public void setPermission(String permission) {
        loadOptionsFromFile();
    }

    @NotNull
    public List<String> getAliases() {
        return options.getAliases();
    }

    /**
     * Sets the aliases for this command from its file.
     *
     * @param aliases the list of aliases to set, file-dependent
     * @return this instance
     */
    @NotNull
    public SIRCommand setAliases(@NotNull List<String> aliases) {
        loadOptionsFromFile();
        return this;
    }

    @NotNull
    public abstract Executable getExecutable();

    public abstract TabBuilder getCompletionBuilder();

    @NotNull
    public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
        TabBuilder builder = getCompletionBuilder();
        return () -> builder == null ? new ArrayList<>() : builder.build(sender, arguments);
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
                PlayerUtils.hasPerm(target, getWildcardPermission());
    }

    public boolean isPermitted(CommandSender sender, boolean log) {
        if (testPermissionSilent(sender))
            return true;

        Player player = sender instanceof Player ? (Player) sender : null;
        if (log)
            MessageSender.loaded()
                    .setTargets(player).addKeyValue("{perm}", getPermission())
                    .send(getMain().toStringList("lang.no-permission"));

        return false;
    }

    /**
     * Creates a new MessageSender instance for sending messages from the command sender.
     *
     * @param sender the command sender
     * @return the MessageSender instance
     */
    protected MessageSender createSender(CommandSender sender) {
        return new CommandDisplayer().setTargets(fromSender(sender));
    }

    /**
     * Edits the behavior of a subcommand using a predicate.
     *
     * @param name      the name of the subcommand
     * @param predicate the predicate to apply
     *
     * @return true if the subcommand behavior was successfully edited, otherwise false
     */
    protected boolean editSubCommand(String name, BiPredicate<CommandSender, String[]> predicate) {
        if (StringUtils.isBlank(name) || predicate == null)
            return false;

        final BaseCommand subCommand = getSubCommand(name);
        if (subCommand == null) return false;

        ((SubCommand) subCommand).setExecutable(Executable.from(predicate));
        return true;
    }

    @Override
    public final boolean register() {
        loadOptionsFromFile();

        boolean result = super.register();
        if (result)
            registerCalled = true;

        return result;
    }

    @Override
    public final boolean unregister() {
        loadOptionsFromFile();

        boolean result = super.unregister();
        if (result)
            unregisterCalled = true;

        return result;
    }

    protected static List<String> getOnlineNames() {
        return CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(HumanEntity::getName).toList();
    }
}
