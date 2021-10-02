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

        boolean doSpawn = main.getConfig().getBoolean("login.spawn-before");
        boolean sendAfter = main.getConfig().getBoolean("login.send-after");
        boolean vanish = eventUtils.isVanished(player, true);
        boolean silent = main.getConfig().getBoolean("vanish.silent");

        if (!main.getInitializer().hasLogin || !sendAfter) return;
        if (vanish && silent) return;

        eventUtils.runEvent(id, player, true, !doSpawn, true);
    }
}
