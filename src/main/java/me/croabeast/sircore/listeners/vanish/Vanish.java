package me.croabeast.sircore.listeners.vanish;

import de.myzelyam.api.vanish.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;

public class Vanish implements Listener {

    public Vanish(Application main) {
        if (!main.getInitializer().superVanish &&
                !main.getInitializer().prVanish) return;
        main.getInitializer().events++;
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onVanish(PlayerVanishStateChangeEvent event) {
        Player player = Bukkit.getPlayer(event.getUUID());
        Bukkit.getPluginManager().callEvent(new VanishEvent(player, !event.isVanishing()));
    }
}
