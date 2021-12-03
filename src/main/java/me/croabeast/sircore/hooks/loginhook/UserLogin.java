package me.croabeast.sircore.hooks.loginhook;

import com.elchologamer.userlogin.api.event.*;
import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import org.bukkit.*;
import org.bukkit.event.*;

public class UserLogin implements Listener {

    public UserLogin(Application main) {
        Initializer init = main.getInitializer();
        if (!init.userLogin) return;
        main.registerListener(this);
    }

    @EventHandler
    private void onLogin(AuthenticationEvent event){
        event.setAnnouncement(null);
        Bukkit.getPluginManager().callEvent(new LoginEvent(event.getPlayer()));
    }
}
