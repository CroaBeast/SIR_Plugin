package me.croabeast.sircore.listeners.login;

import fr.xephi.authme.events.LoginEvent;
import me.croabeast.sircore.Application;
import me.croabeast.sircore.utils.EventUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AuthMe implements Listener {

    private final Application main;
    private final EventUtils eventUtils;

    public AuthMe(Application main) {
        this.main = main;
        this.eventUtils = main.getEventUtils();
        if (!main.getInitializer().authMe) return;
        main.getInitializer().events++;
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, true);

        if (!main.getInitializer().hasLogin || !main.choice("after")) return;
        if (eventUtils.isVanished(player, true) && main.choice("silent")) return;

        eventUtils.runEvent(id, player, true, !main.choice("lSpawn"), true);
    }
}
