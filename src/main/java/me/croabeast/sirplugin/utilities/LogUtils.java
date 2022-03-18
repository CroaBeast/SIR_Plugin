package me.croabeast.sirplugin.utilities;

import me.croabeast.sirplugin.objects.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import static me.croabeast.iridiumapi.IridiumAPI.*;
import static me.croabeast.sirplugin.SIRPlugin.*;
import static me.croabeast.sirplugin.utilities.TextUtils.*;

/**
 * The LogUtils class can send colorful log messages.
 */
public abstract class LogUtils {

    /**
     * Checks if the server has support for colored logs.
     */
    private static final boolean COLOR_SUPPORT =
            MAJOR_VERSION >= 12 && !MC_FORK.split(" ")[0].matches("(?i)Spigot");

    /**
     * Colorize the requested line if is supported.
     * @param line the requested line
     * @return a colored log line if supported.
     */
    private static String colorize(@NotNull String line) {
        if (JsonMsg.isValidJson(line)) line = JsonMsg.stripJson(line);
        return COLOR_SUPPORT ? process(line) : stripAll(line);
    }

    /**
     * Sends requested information for a player.
     * @param player a valid online player
     * @param lines the information to send
     */
    public static void playerLog(@NotNull Player player, String... lines) {
        for (String s : lines) if (s != null)
            player.sendMessage(process(s.replace("<P>", langPrefix())));
    }

    /**
     * Sends requested information using the bukkit's logger.
     * @param lines the information to send
     */
    public static void rawLog(String... lines) {
        for (String s : lines) if (s != null)
            Bukkit.getLogger().info(colorize(s));
    }

    /**
     * Sends requested information to a command sender.
     * @param sender a valid sender, can be the console or a player
     * @param lines the information to send
     */
    public static void doLog(@Nullable CommandSender sender, String... lines) {
        if (sender instanceof Player) playerLog((Player) sender, lines);

        for (String s : lines) if (s != null) {
            s = colorize(s.replace("<P> ", ""));
            getInstance().getLogger().info(s);
        }
    }

    /**
     * Sends requested information to the console.
     * @param lines the information to send
     */
    public static void doLog(String... lines) {
        doLog(null, lines);
    }
}
