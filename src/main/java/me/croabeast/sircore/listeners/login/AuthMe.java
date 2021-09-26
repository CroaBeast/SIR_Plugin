package me.croabeast.sircore.listeners.login;

import fr.xephi.authme.events.LoginEvent;
import me.croabeast.sircore.MainClass;
import me.croabeast.sircore.utils.EventUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AuthMe implements Listener {

    private final MainClass main;
    private final EventUtils eventUtils;

    public AuthMe(MainClass main) {
        this.main = main;
        this.eventUtils = main.getEventUtils();
        if (!main.authMe) return;
        main.events++;
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, true);
        boolean doSpawn = main.getConfig().getBoolean("login.spawn-before");

        if (!main.hasLogin || !main.getConfig().getBoolean("login.send-after")) return;

        boolean vanish = eventUtils.isVanished(player, true);
        if (vanish && main.getConfig().getBoolean("vanish.silent")) return;

        eventUtils.runEvent(id, player, true, !doSpawn, true);
    }
}
