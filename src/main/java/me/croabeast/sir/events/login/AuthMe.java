package me.croabeast.sir.events.login;

import fr.xephi.authme.events.LoginEvent;
import me.croabeast.sir.SIR;
import me.croabeast.sir.utils.EventUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AuthMe implements Listener {

    private final SIR main;
    private final EventUtils eventUtils;

    public AuthMe(SIR main) {
        this.main = main;
        this.eventUtils = main.getEventUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onLogin(LoginEvent event) {
        if (!main.hasLogin || !main.afterLogin) return;

        Player player = event.getPlayer();
        ConfigurationSection section = eventUtils.joinSection(player);
        if (section == null) return;

        eventUtils.addPerms(section);
        eventUtils.checkSections(section, player, true);
    }
}
