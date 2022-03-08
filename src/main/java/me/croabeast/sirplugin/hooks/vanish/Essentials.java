package me.croabeast.sirplugin.hooks.vanish;

import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.events.*;
import net.ess3.api.*;
import net.ess3.api.events.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class Essentials implements Listener {

    public Essentials() {
        if (Bukkit.getPluginManager().isPluginEnabled("Essentials"))
            SIRPlugin.registerListener(this);
    }

    @EventHandler
    private void onVanish(VanishStatusChangeEvent event) {
        IUser user = event.getAffected();
        Bukkit.getPluginManager().callEvent(new VanishEvent(user.getBase(), user.isVanished()));
    }
}
