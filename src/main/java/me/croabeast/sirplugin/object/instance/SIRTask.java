package me.croabeast.sirplugin.object.instance;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.var;
import me.croabeast.beanslib.message.MessageSender;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.task.message.DirectTask;
import me.croabeast.sirplugin.utility.LangUtils;
import me.croabeast.sirplugin.utility.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * This class represents the base of every command used in this plugin.
 */
@RequiredArgsConstructor
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
    protected abstract List<String> complete(CommandSender sender, String[] args);

    /**
     * Registers the command in the server.
     */
    public final void registerCommand() {
        var command = SIRPlugin.getInstance().getCommand(name);
        if (command == null) return;

        command.setExecutor((s, c, l, a) -> execute(s, a));
        command.setTabCompleter((s, c, l, a) -> complete(s, a));
    }

    /**
     * Creates a cloned instance of the {@link LangUtils#getSender()} object
     * using a {@link CommandSender} as the main target.
     *
     * @param sender a sender
     * @return a cloned instance
     */
    protected <C extends CommandSender> MessageSender getClonedSender(C sender) {
        return LangUtils.getSender().setTargets(sender).clone();
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
    protected <C extends CommandSender, T> boolean fromSender(C sender, String key, T value, String path) {
        return getClonedSender(sender).setKeys(key).
                setValues(value).
                send(LangUtils.toList(FileCache.LANG, path));
    }

    /**
     * Sends a list / unique string from the lang.yml file using a unique path.
     *
     * @param sender a sender
     * @param path the lang.yml file path
     *
     * @return the {@link MessageSender#send(List)} result
     */
    protected <C extends CommandSender> boolean fromSender(C sender, String path) {
        return getClonedSender(sender).send(LangUtils.toList(FileCache.LANG, path));
    }

    protected <C extends CommandSender> boolean isProhibited(C sender, String perm) {
        if (PlayerUtils.hasPerm(sender, "sir." + perm)) return false;

        fromSender(sender, "{perm}", "sir." + perm, "no-permission");
        return true;
    }

    protected <C extends CommandSender> boolean isWrongArgument(C sender, String value) {
        return fromSender(sender, "{arg}", value, "wrong-arg");
    }

    protected String getFromArray(String[] args, int argumentIndex) {
        if (argumentIndex >= args.length) return null;
        var b = new StringBuilder();

        for (int i = argumentIndex; i < args.length; i++)  {
            b.append(args[i]);
            if (i != args.length - 1) b.append(" ");
        }

        return b.toString();
    }

    @SafeVarargs
    protected final List<String> generateList(String[] args, Collection<String>... lists) {
        var firstList = new ArrayList<String>();
        for (var list : lists) firstList.addAll(list);

        final var token = args[args.length - 1];
        return firstList.stream().
                filter(s -> s.matches("(?i)^" + token)).
                collect(Collectors.toList());
    }

    protected List<String> generateList(String[] args, String... array) {
        return generateList(args, Lists.newArrayList(array));
    }

    protected List<String> getPlayersNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    static void createInstances(String pack) {
        @SuppressWarnings("deprecation")
        var file = new File(URLDecoder.decode(SIRPlugin.class.getProtectionDomain().
                getCodeSource().getLocation().getPath()));

        final var packPath = pack.replace(".", "/");

        try (JarFile jarFile = new JarFile(file)) {
            var entries = Collections.list(jarFile.entries());

            for (var entry : entries) {
                var name = entry.getName();
                if (!name.startsWith(packPath)) continue;

                if (name.endsWith(".class")) {
                    String className = name.replace("/", ".").replace(".class", "");
                    var clazz = Class.forName(className);

                    var sup = clazz.getSuperclass();
                    if (sup != SIRTask.class && sup != DirectTask.class) continue;

                    if (clazz.getSimpleName().equals("DirectTask")) continue;

                    var constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);

                    var instance = constructor.newInstance();
                    ((SIRTask) instance).registerCommand();
                    continue;
                }

                if (name.endsWith("/")) {
                    var subName = name.replace("/", ".").replaceFirst(packPath + "\\.", "");
                    createInstances(pack + "." + subName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerCommands() {
        try {
            createInstances("me.croabeast.sirplugin.task");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
