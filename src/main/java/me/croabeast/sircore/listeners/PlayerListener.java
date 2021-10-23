package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.objects.Updater;
import me.croabeast.sircore.utils.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private final Application main;
    private final EventUtils eventUtils;

    public PlayerListener(Application main) {
        this.main = main;
        main.getInitializer().listeners++;
        this.eventUtils = main.getEventUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, true);

        new Updater(main, 96378).getVersion(latest -> {
            if (!main.choice("toOp") || !eventUtils.hasPerm(player, "sir.admin.updater")) return;
            if (!main.version.equals(latest)) {
                main.playerLogger(player,
                        "", "  &7&lSIR \u00BB &4BIG WARNING!",
                        "  &cYou don't have the latest version of S.I.R. installed.",
                        "  &cRemember, older versions won't receive any support.",
                        "  &7New Version: &e" + latest + "&7 - Your Version: &e" + main.version,
                        "  &7Link:&b https://www.spigotmc.org/resources/96378/", ""
                );
            }
        });

        if (main.getInitializer().hasLogin && main.choice("after")) {
            if (main.choice("login")) eventUtils.spawn(id, player);
            return;
        }

        if (eventUtils.isVanished(player, true) && main.choice("silent")) return;

        eventUtils.runEvent(id, player, true, true, false);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = eventUtils.lastSection(player, false);

        if (eventUtils.isVanished(player, false) && main.choice("silent")) return;

        if (main.getInitializer().hasLogin) {
            if (!eventUtils.loggedPlayers.contains(player)) return;
            eventUtils.loggedPlayers.remove(player);
        }

        eventUtils.runEvent(id, player, false, false, false);
    }
}
