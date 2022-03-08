package me.croabeast.sirplugin.hooks.vanish;

import de.myzelyam.api.vanish.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.events.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;

public class Vanish implements Listener {

    public Vanish() {
        if (Bukkit.getPluginManager().isPluginEnabled("SuperVanish") ||
                Bukkit.getPluginManager().isPluginEnabled("PremiumVanish"))
            SIRPlugin.registerListener(this);
    }

    @EventHandler
    private void onVanish(PlayerVanishStateChangeEvent event) {
        Player player = Bukkit.getPlayer(event.getUUID());
        Bukkit.getPluginManager().callEvent(new VanishEvent(player, !event.isVanishing()));
    }
}
