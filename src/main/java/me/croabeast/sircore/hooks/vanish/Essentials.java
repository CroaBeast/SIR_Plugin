package me.croabeast.sircore.hooks.vanish;

import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import net.ess3.api.*;
import net.ess3.api.events.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class Essentials implements Listener {

    public Essentials(Application main){
        Initializer init = main.getInitializer();
        if (!init.essentials) return;
        main.registerListener(this);
        init.LISTENERS++;
    }

    @EventHandler
    private void onVanish(VanishStatusChangeEvent event) {
        IUser user = event.getAffected();
        Bukkit.getPluginManager().callEvent(new VanishEvent(user.getBase(), user.isVanished()));
    }
}
