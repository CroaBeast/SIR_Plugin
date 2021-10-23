package me.croabeast.sircore.listeners.vanish;

import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import net.ess3.api.*;
import net.ess3.api.events.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class Essentials implements Listener {

    public Essentials(Application main){
        if (!main.getInitializer().essentials) return;
        main.getInitializer().listeners++;
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onVanish(VanishStatusChangeEvent event) {
        IUser user = event.getAffected();
        Bukkit.getPluginManager().callEvent(new VanishEvent(user.getBase(), user.isVanished()));
    }
}
