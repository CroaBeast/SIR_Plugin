package me.croabeast.sircore.listeners.vanish;

import com.Zrips.CMI.events.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class CMI implements Listener {

    public CMI(Application main) {
        if (!main.getInitializer().hasCMI) return;
        main.getInitializer().listeners++;
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onUnVanish(CMIPlayerUnVanishEvent event) {
        Bukkit.getPluginManager().callEvent(new VanishEvent(event.getPlayer(), true));
    }

    @EventHandler
    private void onVanish(CMIPlayerVanishEvent event) {
        Bukkit.getPluginManager().callEvent(new VanishEvent(event.getPlayer(), false));
    }
}
