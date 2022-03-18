package me.croabeast.sirplugin.objects.handlers;

import me.croabeast.sirplugin.*;
import org.bukkit.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.util.regex.*;

import static me.croabeast.sirplugin.utilities.TextUtils.*;

public abstract class TextParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\[(.[^\\[][^]]+)])(.+)");

    private static FileConfiguration configFile() {
        return SIRPlugin.getInstance().getConfig();
    }

    public static String stripPrefix(String line) {
        Matcher matcher = PATTERN.matcher(line);
        return removeSpace(
                !matcher.find() || !configFile().getBoolean("options.show-prefix")
                ? line : line.replace(matcher.group(1), ""));
    }

    public static void send(@Nullable Player target, @NotNull Player sender, String line) {
        if (target == null) target = sender;
        Matcher match = PATTERN.matcher(line);

        if (match.find()) {
            String message = colorize(sender, removeSpace(match.group(3))),
                    prefix = match.group(2);

            if (prefix.matches("(?i)title(:\\d+)?")) {
                int i = prefix.contains(":") ? Integer.parseInt(prefix.substring(6)) * 20 : 60;
                sendTitle(target, message.split(lineSplitter()), i + "");
            }
            else if (prefix.matches("(?i)json")) {
                String cmd = "minecraft:tellraw " + target.getName() + " " + line;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            else if (prefix.matches("(?i)action-bar")) sendActionBar(target, message);
            else if (prefix.matches("(?i)^bossbar")) sendBossbar(target, sender, line);
            else sendChat(target, sender, removeSpace(match.group(3)));
        }
        else sendChat(target, sender, line);
    }
}
