package me.croabeast.sirplugin.hooks.vanish;

import com.Zrips.CMI.events.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.events.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class CMI implements Listener {

    public CMI() {
        if (Bukkit.getPluginManager().isPluginEnabled("CMI"))
            SIRPlugin.registerListener(this);
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
