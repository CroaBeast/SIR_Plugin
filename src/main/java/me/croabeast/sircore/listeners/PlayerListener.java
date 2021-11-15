package me.croabeast.sircore.listeners;

import me.croabeast.sircore.*;
import me.croabeast.sircore.objects.*;
import me.croabeast.sircore.utilities.*;
import org.bukkit.configuration.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private final Application main;
    private final Records records;
    private final Initializer init;
    private final TextUtils text;
    private final EventUtils utils;

    public PlayerListener(Application main) {
        this.main = main;
        this.records = main.getRecords();
        this.init = main.getInitializer();
        this.text = main.getTextUtils();
        this.utils = main.getEventUtils();
        main.getServer().getPluginManager().registerEvents(this, main);
        init.LISTENERS++;
    }

    private void adminUpdateChecker(Player player) {
        Updater.init(main, 96378).updateCheck().whenComplete(((updateResult, throwable) -> {
            if (!text.getOption(4, "send-op") ||
                    !utils.hasPerm(player, "sir.admin.updater")) return;

            String latest = updateResult.getNewestVersion();

            switch (updateResult.getReason()) {
                case NEW_UPDATE:
                    records.playerRecord(player,
                            "", " &4NEW UPDATE!",
                            " &cYou don't have the latest version of S.I.R. installed.",
                            " &cRemember, older versions won't receive any support.",
                            " &7New Version: &a" + latest + "&7 - Your Version: &e" + main.PLUGIN_VERSION,
                            " &7Link:&b https://www.spigotmc.org/resources/96378/", ""
                    );
                    break;
                case UP_TO_DATE:
                    records.playerRecord(player,
                            "", " &eYou have the latest version of S.I.R. &7(" + latest + ")",
                            " &7I would appreciate if you keep updating &c<3", ""
                    );
                    break;
                case UNRELEASED_VERSION:
                    records.playerRecord(player,
                            "", " &4DEVELOPMENT BUILD:",
                            " &cYou have a newer version of S.I.R. installed.",
                            " &cErrors might occur in this build.",
                            " Spigot Version: &a" + updateResult.getSpigotVersion()
                                    + "&7 - Your Version: &e" + main.PLUGIN_VERSION, ""
                    );
                    break;
                default:
                    records.playerRecord(player,
                            "", " &4WARNING!",
                            " &cCould not check for a new version of S.I.R.",
                            " &7Please check your connection and restart the server.",
                            " &7Possible reason: &e" + updateResult.getReason(), ""
                    );
                    break;
            }
        }));
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        if (!text.getOption(1, "enabled")) return;
        
        event.setJoinMessage(null); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = utils.lastSection(player, true);
        adminUpdateChecker(player);

        if (init.HAS_LOGIN && text.getOption(2, "enabled")) {
            if (text.getOption(2, "spawn-before")) utils.goSpawn(id, player);
            return;
        }
        if (utils.isVanished(player, true) &&
                text.getOption(3, "silent")) return;

        utils.runEvent(id, player, true, true, false);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        if (!text.getOption(1, "enabled")) return;
        
        event.setQuitMessage(null); //Message initializer

        Player player = event.getPlayer();
        ConfigurationSection id = utils.lastSection(player, false);

        if (utils.isVanished(player, false) &&
                text.getOption(3, "silent")) return;
        if (init.HAS_LOGIN) utils.loggedPlayers.remove(player);

        utils.runEvent(id, player, false, false, false);
    }
}
