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
        return main.GET_VERSION < 12 ? IridiumAPI.stripColor(line) : IridiumAPI.process(line);
    }

    public void playerRecord(Player player, String... lines) {
        Arrays.asList(lines).forEach(s -> player.sendMessage(IridiumAPI.process(s)));
    }

    public void rawRecord(String... lines) {
        Arrays.asList(lines).forEach(s -> main.getServer().getLogger().info(parseColor(s)));
    }

    public void doRecord(CommandSender sender, String... lines) {
        Arrays.asList(lines).forEach(s -> {
            if (sender instanceof Player)
                playerRecord((Player) sender, s.replace("<P> ", "&e SIR &8> &7"));
            main.getLogger().info(
                    parseColor(s.startsWith("<P> ") ? s.substring(4) : s)
            );
        });
    }

    public void doRecord(String... lines) {
        doRecord(null, lines);
    }
}
