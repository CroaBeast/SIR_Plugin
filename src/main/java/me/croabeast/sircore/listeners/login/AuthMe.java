package me.croabeast.sircore.listeners.login;

import fr.xephi.authme.events.*;
import me.croabeast.sircore.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class AuthMe implements Listener {

    public AuthMe(Application main) {
        if (!main.getInitializer().authMe) return;
        main.getServer().getPluginManager().registerEvents(this, main);
        main.getInitializer().events++;
    }

    @EventHandler
    public void onLogin(LoginEvent event){
        Bukkit.getPluginManager().callEvent(new me.croabeast.sircore.events.LoginEvent(event.getPlayer()));
    }
}
