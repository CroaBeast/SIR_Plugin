package me.croabeast.sircore.listeners.login;

import com.elchologamer.userlogin.api.event.AuthenticationEvent;
import me.croabeast.sircore.Application;
import me.croabeast.sircore.utils.EventUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class UserLogin implements Listener {

    private final Application main;
    private final EventUtils eventUtils;

    public UserLogin(Application main) {
        this.main = main;
        this.eventUtils = main.getEventUtils();
        if (!main.getInitializer().userLogin) return;
        main.getInitializer().events++;
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onLogin(AuthenticationEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, true);

        if (!main.getInitializer().hasLogin || !main.choice("after")) return;
        if (eventUtils.isVanished(player, true) && main.choice("silent")) return;

        eventUtils.runEvent(id, player, true, !main.choice("lSpawn"), true);
    }
}
