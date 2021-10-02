package me.croabeast.sircore.listeners.vanish;

import me.croabeast.sircore.Application;
import me.croabeast.sircore.utils.EventUtils;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Essentials implements Listener {

    private final Application main;
    private final EventUtils eventUtils;

    public Essentials(Application main){
        this.main = main;
        this.eventUtils = main.getEventUtils();
        if (!main.getInitializer().essentials) return;
        main.getInitializer().events++;
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onVanish(VanishStatusChangeEvent event) {
        IUser user = event.getAffected();
        Player player = user.getBase();

        String path = user.isVanished() ? "join" : "quit";
        ConfigurationSection id = eventUtils.lastSection(player, path);
        if (id == null) return;

        if (!main.getInitializer().hasVanish || !main.choice("trigger")) return;

        eventUtils.runEvent(id, player, user.isVanished(), main.choice("vSpawn"), false);
    }
}
