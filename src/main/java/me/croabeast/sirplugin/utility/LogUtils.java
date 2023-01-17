package me.croabeast.sirplugin.utility;

import me.croabeast.sirplugin.module.EmParser;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import static me.croabeast.sirplugin.SIRPlugin.*;

/**
 * The LogUtils class can send colorful log messages.
 */
public class LogUtils {

    private static String[] parseEmojis(String... lines) {
        String[] results = new String[lines.length];
        for (int i = 0; i < lines.length; i++) {
            try {
                results[i] = EmParser.parseEmojis(null, lines[i]);
            }
            catch (Exception e) {
                results[i] = lines[i];
            }
        }
        return results;
    }

    /**
     * Sends requested information for a player.
     * @param player a valid online player
     * @param lines the information to send
     */
    public static void playerLog(Player player, String... lines) {
        getUtils().playerLog(player, parseEmojis(lines));
    }

    /**
     * Sends requested information using the bukkit's logger.
     * @param lines the information to send
     */
    public static void rawLog(String... lines) {
        getUtils().rawLog(parseEmojis(lines));
    }

    /**
     * Sends requested information to a command sender.
     * @param sender a valid sender, can be the console or a player
     * @param lines the information to send
     */
    public static void doLog(CommandSender sender, String... lines) {
        getUtils().doLog(sender, parseEmojis(lines));
    }

    /**
     * Sends requested information to the console.
     * @param lines the information to send
     */
    public static void doLog(String... lines) {
        doLog(null, lines);
    }
}
