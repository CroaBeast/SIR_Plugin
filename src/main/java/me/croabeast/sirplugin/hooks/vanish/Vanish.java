package me.croabeast.sirplugin.hooks.vanish;

import de.myzelyam.api.vanish.*;
import me.croabeast.sirplugin.events.*;
import me.croabeast.sirplugin.objects.extensions.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;

public class Vanish implements RawViewer {

    public Vanish() {
        if (Bukkit.getPluginManager().isPluginEnabled("SuperVanish") ||
                Bukkit.getPluginManager().isPluginEnabled("PremiumVanish")) registerListener();
    }

    @EventHandler
    private void onVanish(PlayerVanishStateChangeEvent event) {
        Player player = Bukkit.getPlayer(event.getUUID());
        Bukkit.getPluginManager().callEvent(new VanishEvent(player, !event.isVanishing()));
    }
}
