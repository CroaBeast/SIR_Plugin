package me.croabeast.sircore.listeners.login;

import com.elchologamer.userlogin.api.event.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class UserLogin implements Listener {

    public UserLogin(Application main) {
        Initializer init = main.getInitializer();
        if (!init.userLogin) return;
        main.getServer().getPluginManager().registerEvents(this, main);
        init.listeners++;
    }

    @EventHandler
    public void onLogin(AuthenticationEvent event){
        event.setAnnouncement(null);
        Bukkit.getPluginManager().callEvent(new LoginEvent(event.getPlayer()));
    }
}
