package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.events.*;
import me.croabeast.sircore.hooks.vanishhook.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;

public class VanishListener implements Listener {

    private final Application main;

    private final Initializer init;
    private final TextUtils text;
    private final EventUtils utils;

    public VanishListener(Application main) {
        this.main = main;

        this.init = main.getInitializer();
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();

        new CMI(main);
        new Essentials(main);
        new Vanish(main);

        main.registerListener(this, false);
    }

    @EventHandler
    private void onVanish(VanishEvent event) {
        Player player = event.getPlayer();
        boolean vanish = event.isVanished();

        if (!init.HAS_VANISH || !text.getOption(3, "enabled")) return;
        if(init.HAS_LOGIN) utils.getLoggedPlayers().add(player);

        ConfigurationSection id = utils.resultSection(player, vanish ? "join" : "quit");
        if (id == null) {
            main.getRecorder().doRecord(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e messages.yml &7file."
            );
            return;
        }

        utils.runEvent(id, player, vanish, text.getOption(3, "use-spawn"), false);
    }
}
