package me.croabeast.sirplugin.hook.vanish;

import de.myzelyam.api.vanish.*;
import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.sirplugin.event.*;
import me.croabeast.sirplugin.object.instance.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;

public class Vanish implements RawViewer {

    public Vanish() {
        if (Exceptions.arePluginsEnabled(false, "SuperVanish", "PremiumVanish"))
            registerListener();
    }

    @EventHandler
    private void onVanish(PlayerVanishStateChangeEvent event) {
        Player player = Bukkit.getPlayer(event.getUUID());
        Bukkit.getPluginManager().callEvent(new VanishEvent(player, !event.isVanishing()));
    }
}
