package me.croabeast.sirplugin.hooks.vanish;

import me.croabeast.sirplugin.events.*;
import me.croabeast.sirplugin.objects.extensions.*;
import net.ess3.api.*;
import net.ess3.api.events.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class Essentials implements RawViewer {

    public Essentials() {
        if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) registerListener();
    }

    @EventHandler
    private void onVanish(VanishStatusChangeEvent event) {
        IUser user = event.getAffected();
        Bukkit.getPluginManager().callEvent(new VanishEvent(user.getBase(), user.isVanished()));
    }
}
