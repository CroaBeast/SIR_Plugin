package me.croabeast.sircore.objects;

import me.croabeast.iridiumapi.*;
import me.croabeast.sircore.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;

public class Records {

    private final Application main;

    public Records(Application main) {
        this.main = main;
    }

    public void rawRecord(String... lines) {
        for (String line : lines)
            main.getServer().getLogger().info(IridiumAPI.process(line));
    }

    public void playerRecord(Player player, String... lines) {
        for (String line : lines)
            player.sendMessage(IridiumAPI.process(line));
    }

    public void doRecord(CommandSender sender, String... lines) {
        for (String line : lines) {
            if (sender instanceof Player){
                String a = "<P> ", b = "&e SIR &8> &7";
                sender.sendMessage(IridiumAPI.process(line.replace(a, b)));
            }
            line = line.startsWith("<P> ") ? line.substring(4) : line;
            main.getLogger().info(IridiumAPI.process(line));
        }
    }

    public void doRecord(String... lines) { doRecord(null, lines); }
}
