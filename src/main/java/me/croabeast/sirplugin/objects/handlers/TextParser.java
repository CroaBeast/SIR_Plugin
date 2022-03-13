package me.croabeast.sirplugin.objects.handlers;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.*;
import net.md_5.bungee.api.chat.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.util.regex.*;

import static me.croabeast.sirplugin.utilities.TextUtils.*;

public abstract class TextParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\[(.[^\\[][^]]+)])(.+)");
    private static final String TITLE = "title(:\\d+)?",
            BOSSBAR = "bossbar(:(([a-z]+)(,([0-9]+)(,(true|false))?)?)?)?";

    private static FileConfiguration configFile() {
        return SIRPlugin.getInstance().getConfig();
    }

    public static String stripPrefix(String line) {
        Matcher matcher = PATTERN.matcher(line);
        boolean notFound = !matcher.find() || !configFile().getBoolean("options.show-prefix");
        return removeSpace(notFound ? line : line.replace(matcher.group(1), ""));
    }

    public static void send(@Nullable Player target, @NotNull Player sender, String line) {
        if (target == null) target = sender;
        Matcher match = PATTERN.matcher(line);

        if (match.find()) {
            String message = colorize(sender, removeSpace(match.group(3)));
            String prefix = match.group(2);
            BaseComponent[] comp = new JsonMsg(sender, message).build();

            if (prefix.matches("(?i)" + BOSSBAR)) {
                String[] v = !prefix.contains(":") ? new String[3] :
                        prefix.substring(8).split(",");
                new Bossbar(target, message, v[0], v[1], v[2]).display();
            }
            else if (prefix.matches("(?i)" + TITLE)) {
                String[] times = {"10", !prefix.contains(":") ? "60" :
                        (Integer.parseInt(prefix.substring(6)) * 20) + "", "10"};
                sendTitle(target, message.split(lineSplitter()), times);
            }
            else if (prefix.matches("(?i)action-bar")) sendActionBar(target, message);
            else target.spigot().sendMessage(comp);
        }
        else target.spigot().sendMessage(new JsonMsg(sender, line).build());
    }
}
