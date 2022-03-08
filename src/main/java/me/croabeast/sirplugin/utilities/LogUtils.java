package me.croabeast.sirplugin.utilities;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import static me.croabeast.iridiumapi.IridiumAPI.*;
import static me.croabeast.sirplugin.SIRPlugin.*;

public final class LogUtils {

    private static final boolean COLOR_SUPPORT =
            MAJOR_VERSION >= 12 && !MC_FORK.split(" ")[0].matches("(?i)Spigot");

    private static String colorize(String line) {
        return JsonMsg.stripJson(COLOR_SUPPORT ? process(line) : stripAll(line));
    }

    public static void playerLog(Player player, String... lines) {
        for (String s : lines) if (s != null)
            player.sendMessage(process(s.replace("<P>", TextUtils.langPrefix())));
    }

    public static void rawLog(String... lines) {
        for (String s : lines) if (s != null) Bukkit.getServer().getLogger().info(colorize(s));
    }

    public static void doLog(CommandSender sender, String... lines) {
        if (sender instanceof Player) playerLog((Player) sender, lines);
        for (String s : lines) if (s != null)
            SIRPlugin.getInstance().getLogger().info(colorize(s.replace("<P> ", "")));
    }

    public static void doLog(String... lines) {
        doLog(null, lines);
    }
}
