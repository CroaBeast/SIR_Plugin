package me.croabeast.sircore.utilities;

import me.croabeast.iridiumapi.*;
import me.croabeast.sircore.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

public class Recorder {

    private final Application main;

    public Recorder(Application main) {
        this.main = main;
    }

    private String parseColor(String line) {
        String isBukkit = main.MC_FORK.split(" ")[0];
        return (main.MC_VERSION >= 12 && !isBukkit.matches("(?i)Spigot")) ?
                IridiumAPI.process(line) : IridiumAPI.stripAll(line);
    }

    public void playerRecord(Player player, String... lines) {
        for (String s : lines) {
            if (s == null) continue;
            player.sendMessage(IridiumAPI.process(s));
        }
    }

    public void rawRecord(String... lines) {
        for (String s : lines) {
            if (s == null) continue;
            main.getServer().getLogger().info(parseColor(s));
        }
    }

    public void doRecord(CommandSender sender, String... lines) {
        String key = "<P> ", prefix = "&e SIR &8> &7";
        for (String s : lines) {
            if (s == null) continue;
            if (sender instanceof Player)
                playerRecord((Player) sender, s.replace(key, prefix));

            main.getLogger().info(
                    parseColor(s.startsWith(key) ? s.substring(4) : s)
            );
        }
    }

    public void doRecord(String... lines) {
        doRecord(null, lines);
    }
}
