package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import me.croabeast.sircore.listeners.vanish.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;

public class VanishListener implements Listener {

    private final Initializer init;
    private final TextUtils text;
    private final EventUtils utils;

    public VanishListener(Application main) {
        this.init = main.getInitializer();
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
        if (!init.HAS_VANISH) return;
        new CMI(main);
        new Essentials(main);
        new Vanish(main);
        main.registerListener(this);
    }

    @EventHandler
    private void onVanish(VanishEvent event) {
        Player player = event.getPlayer();
        boolean vanish = event.isVanished();
        ConfigurationSection id = utils.lastSection(player, vanish ? "join" : "quit");

        if (!init.HAS_VANISH || !text.getOption(3, "enabled")) return;
        if(init.HAS_LOGIN) utils.getLoggedPlayers().add(player);

        utils.runEvent(id, player, vanish, text.getOption(3, "use-spawn"), false);
    }
}
