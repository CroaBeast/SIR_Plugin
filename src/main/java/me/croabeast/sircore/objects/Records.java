package me.croabeast.sircore.objects;

import me.croabeast.iridiumapi.IridiumAPI;
import me.croabeast.sircore.Application;
import org.bukkit.entity.Player;

public class Records {

    private final Application main;

    public Records(Application main) {
        this.main = main;
    }

    public void rawRecord(String... lines) {
        for (String s : lines)
            main.getServer().getLogger().info(IridiumAPI.process(s));
    }

    public void playerRecord(Player player, String... lines) {
        for (String line : lines) player.sendMessage(IridiumAPI.process(line));
    }

    public void doRecord(Player player, String... lines) {
        for (String line : lines) {
            main.getLogger().info(IridiumAPI.process(line));
            if (player != null)
                playerRecord(player, "[SIR] " + line);
        }
    }

    public void doRecord(String... lines) { doRecord(null, lines); }
}
