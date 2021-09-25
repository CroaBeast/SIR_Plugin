package me.croabeast.sircore.listeners.login;

import fr.xephi.authme.events.LoginEvent;
import me.croabeast.sircore.MainClass;
import me.croabeast.sircore.utils.EventUtils;
import org.bukkit.Bukkit;
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
        boolean doSpawn = main.getConfig().getBoolean("options.login.spawn-before");
        if (id == null) return;

        if (!main.hasLogin || !main.getConfig().getBoolean("options.login.send-after")) return;

        int ticks = main.getConfig().getInt("options.login.ticks-after", 0);
        Bukkit.getScheduler().runTaskLater(main, () ->
                eventUtils.doAllEvent(id, player, true, !doSpawn), ticks);
    }
}
