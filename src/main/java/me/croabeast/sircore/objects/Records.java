package me.croabeast.sircore.objects;

import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sircore.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

import java.util.*;

public class Records {

    private final Application main;

    public Records(Application main) {
        this.main = main;
    }

    private String parseColor(String line) {
        String isBukkit = main.MC_FORK.split(" ")[0];
        return (main.MC_VERSION >= 12 && !isBukkit.matches("(?i)Spigot")) ?
                IridiumAPI.process(line) : IridiumAPI.stripColor(line);
    }

    public void playerRecord(Player player, String... lines) {
        Arrays.asList(lines).forEach(s -> player.sendMessage(IridiumAPI.process(s)));
    }

    public void rawRecord(String... lines) {
        Arrays.asList(lines).forEach(s -> main.getServer().getLogger().info(parseColor(s)));
    }

    public void doRecord(CommandSender sender, String... lines) {
        String key = "<P> ", prefix = "&e SIR &8> &7";
        Arrays.asList(lines).forEach(s -> {
            if (sender instanceof Player) playerRecord((Player) sender, s.replace(key, prefix));
            main.getLogger().info(parseColor(s.startsWith(key) ? s.substring(4) : s));
        });
    }

    public void doRecord(String... lines) { doRecord(null, lines); }
}
