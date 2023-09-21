package me.croabeast.sir.plugin.task;

import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.FileCache;
import me.croabeast.sir.plugin.utility.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents the base of every command used in this plugin.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SIRTask {

    private final String name;

    /**
     * Defines the executor for this command.
     * @return the command's executor
     */
    protected abstract boolean execute(CommandSender sender, String[] args);

    /**
     * Defines the completer for this command.
     * @return the command's completer
     */
    @NotNull
    protected abstract List<String> complete(CommandSender sender, String[] args);

    /**
     * Registers the command in the server.
     */
    public final void register() {
        PluginCommand command = SIRPlugin.getInstance().getCommand(name);
        if (command == null) return;

        command.setExecutor((s, c, l, a) -> execute(s, a));
        command.setTabCompleter((s, c, l, a) -> complete(s, a));
    }

    /**
     * Creates a cloned instance of the {@link MessageSender#fromLoaded()} object
     * using a {@link CommandSender} as the main target.
     *
     * @param sender a sender
     * @return a cloned instance
     */
    protected MessageSender getClonedSender(CommandSender sender) {
        return MessageSender.fromLoaded().setTargets(sender).clone();
    }

    /**
     * Sends a list / unique string from the lang.yml file using a single key
     * and value, and a unique path.
     *
     * @param sender a sender
     * @param key a key to replace for a value
     * @param value a value to parse
     * @param path the lang.yml file path
     *
     * @return the {@link MessageSender#send(List)} result
     * @param <T> the class of the value
     */
    protected <T> boolean fromSender(CommandSender sender, String key, T value, String path) {
        return getClonedSender(sender).setKeys(key).
                setValues(value).
                send(FileCache.getLang().toList(path));
    }

    /**
     * Sends a list / unique string from the lang.yml file using a unique path.
     *
     * @param sender a sender
     * @param path the lang.yml file path
     *
     * @return the {@link MessageSender#send(List)} result
     */
    protected boolean fromSender(CommandSender sender, String path) {
        return getClonedSender(sender).send(FileCache.getLang().toList(path));
    }

    protected boolean isProhibited(CommandSender sender, String perm) {
        if (PlayerUtils.hasPerm(sender, "sir." + perm)) return false;

        fromSender(sender, "{perm}", "sir." + perm, "no-permission");
        return true;
    }

    protected boolean isWrongArgument(CommandSender sender, String value) {
        return fromSender(sender, "{arg}", value, "wrong-arg");
    }

    @SafeVarargs
    protected final List<String> generateList(String[] args, Collection<String>... lists) {
        List<String> originalList = new ArrayList<>();
        for (Collection<String> list : lists) originalList.addAll(list);

        final String token = args[args.length - 1];
        List<String> result = new ArrayList<>();

        for (String s : originalList)
            if (s.regionMatches(true, 0, token, 0, token.length())) result.add(s);

        return result;
    }

    protected List<String> generateList(String[] args, String... array) {
        return generateList(args, Lists.newArrayList(array));
    }

    protected List<String> getPlayersNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    public static String getFromArray(String[] args, int argumentIndex) {
        if (argumentIndex >= args.length) return null;
        StringBuilder b = new StringBuilder();

        for (int i = argumentIndex; i < args.length; i++)  {
            b.append(args[i]);
            if (i != args.length - 1) b.append(" ");
        }

        return b.toString();
    }
}
