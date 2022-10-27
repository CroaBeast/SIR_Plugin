package me.croabeast.sirplugin.hook.vanish;

import me.croabeast.beanslib.utility.Exceptions;
import me.croabeast.sirplugin.event.*;
import me.croabeast.sirplugin.object.instance.*;
import net.ess3.api.*;
import net.ess3.api.events.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class Essentials implements RawViewer {

    public Essentials() {
        if (Exceptions.isPluginEnabled("Essentials")) registerListener();
    }

    @EventHandler
    private void onVanish(VanishStatusChangeEvent event) {
        IUser user = event.getAffected();
        Bukkit.getPluginManager().callEvent(new VanishEvent(user.getBase(), user.isVanished()));
    }
}
