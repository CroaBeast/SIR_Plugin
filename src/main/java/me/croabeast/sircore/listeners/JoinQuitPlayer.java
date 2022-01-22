package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class JoinQuitPlayer implements Listener {

    private final Application main;

    private final Initializer init;
    private final TextUtils text;
    private final PermUtils perms;

    private final EventUtils utils;

    public JoinQuitPlayer(Application main) {
        this.main = main;

        this.init = main.getInitializer();
        this.text = main.getTextUtils();
        this.perms = main.getPermUtils();

        this.utils = main.getEventUtils();
        main.registerListener(this);
    }

    private boolean isSilent() { return text.getOption(3, "silent"); }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        main.getAmender().initUpdater(player);
        if (!main.getMessages().getBoolean("enabled", true)) return;

        ConfigurationSection id = utils.resultSection(player, true);
        if (id == null) {
            main.getRecorder().doRecord(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e messages.yml &7file."
            );
            return;
        }

        event.setJoinMessage(null);

        if (init.HAS_VANISH && isSilent() && perms.isVanished(player, true)) return;
        if (init.HAS_LOGIN && text.getOption(2, "enabled")) {
            if (text.getOption(2, "spawn-before")) utils.goSpawn(id, player);
            return;
        }

        utils.runEvent(id, player, true, true, false);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        main.getExecutor().getReceivers().remove(event.getPlayer());

        if (!main.getMessages().getBoolean("enabled", true)) return;
        Player player = event.getPlayer();

        ConfigurationSection id = utils.resultSection(player, false);
        if (id == null) {
            main.getRecorder().doRecord(player,
                    "<P> &cA valid message group isn't found...",
                    "<P> &7Please check your&e messages.yml &7file."
            );
            return;
        }

        event.setQuitMessage(null); //Message initializer

        if (init.HAS_VANISH && perms.isVanished(player, false) && isSilent()) return;
        if (init.HAS_LOGIN) {
            if (!utils.getLoggedPlayers().contains(player)) return;
            utils.getLoggedPlayers().remove(player);
        }

        utils.runEvent(id, player, false, false, false);
    }
}
