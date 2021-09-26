package me.croabeast.sircore.listeners.vanish;

import me.croabeast.sircore.MainClass;
import me.croabeast.sircore.utils.EventUtils;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class Essentials implements Listener {

    private final MainClass main;
    private final EventUtils eventUtils;

    public Essentials(MainClass main){
        this.main = main;
        this.eventUtils = main.getEventUtils();
        if (!main.essentials) return;
        main.events++;
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onVanish(VanishStatusChangeEvent event) {
        IUser affected = event.getAffected();
        Player player = affected.getBase();

        String path = affected.isVanished() ? "join" : "quit";
        ConfigurationSection id = eventUtils.lastSection(player, path);
        if (id == null) return;

        boolean trigger = main.getConfig().getBoolean("vanish.trigger");
        boolean spawn = main.getConfig().getBoolean("vanish.do-spawn");

        if (!main.hasVanish || !trigger) return;

        eventUtils.runEvent(id, player, affected.isVanished(), spawn, false);
    }
}
