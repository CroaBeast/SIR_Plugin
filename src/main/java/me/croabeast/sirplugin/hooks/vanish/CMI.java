package me.croabeast.sirplugin.hooks.vanish;

import com.Zrips.CMI.events.*;
import me.croabeast.sirplugin.events.*;
import me.croabeast.sirplugin.objects.extensions.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class CMI implements RawViewer {

    public CMI() {
        if (Bukkit.getPluginManager().isPluginEnabled("CMI")) registerListener();
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
