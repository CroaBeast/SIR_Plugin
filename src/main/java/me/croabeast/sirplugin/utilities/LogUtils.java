package me.croabeast.sirplugin.utilities;

import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import static me.croabeast.sirplugin.SIRPlugin.*;

/**
 * The LogUtils class can send colorful log messages.
 */
public class LogUtils {

    /**
     * Sends requested information for a player.
     * @param player a valid online player
     * @param lines the information to send
     */
    public static void playerLog(@NotNull Player player, String... lines) {
        getTextUtils().playerLog(player, lines);
    }

    /**
     * Sends requested information using the bukkit's logger.
     * @param lines the information to send
     */
    public static void rawLog(String... lines) {
        getTextUtils().rawLog(lines);
    }

    /**
     * Sends requested information to a command sender.
     * @param sender a valid sender, can be the console or a player
     * @param lines the information to send
     */
    public static void doLog(@Nullable CommandSender sender, String... lines) {
        getTextUtils().doLog(sender, lines);
    }

    /**
     * Sends requested information to the console.
     * @param lines the information to send
     */
    public static void doLog(String... lines) {
        doLog(null, lines);
    }
}
