package me.croabeast.sirplugin.objects.analytics;

import com.google.common.collect.*;
import me.croabeast.sirplugin.*;
import me.croabeast.sirplugin.objects.files.FileCache;
import me.croabeast.sirplugin.utilities.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.util.*;


public class Amender {

    private final SIRPlugin main;

    public Amender(SIRPlugin main) {
        this.main = main;
    }

    private void updateLogger(@Nullable Player player, String... lines) {
        List<String> list = Lists.newArrayList(lines);

        if (player != null) {
            if (list.get(0).equals("RR")) list.remove(0);
            list.forEach(s -> LogUtils.playerLog(player, " " + s));
        }
        else {
            if (list.get(0).equals("RR")) {
                list.remove(0);
                list.forEach(LogUtils::rawLog);
            }
            else list.forEach(LogUtils::doLog);
        }
    }

    private void runUpdater(@Nullable Player player) {
        Updater.init(main, 96378).updateCheck().whenComplete((result, e) -> {
            String latest = result.getNewestVersion();

            updateLogger(player, "RR", "");
            switch (result.getReason()) {
                case NEW_UPDATE:
                    updateLogger(player,
                            "&4NEW UPDATE!",
                            "&cYou don't have the latest version of S.I.R. installed.",
                            "&cRemember, older versions won't receive any support.",
                            "&7New Version: &a" + latest +
                                    "&7 - Your Version: &e" + SIRPlugin.pluginVersion(),
                            "&7Link:&b https://www.spigotmc.org/resources/96378/"
                    );
                    break;

                case UP_TO_DATE:
                    updateLogger(player,
                            "&eYou have the latest version of S.I.R. &7(" + latest + ")",
                            "&7I would appreciate if you keep updating &c<3"
                    );
                    break;

                case UNRELEASED_VERSION:
                    updateLogger(player,
                            "&4DEVELOPMENT BUILD:",
                            "&cYou have a newer version of S.I.R. installed.",
                            "&cErrors might occur in this build.",
                            "Spigot Version: &a" + result.getSpigotVersion() +
                            "&7 - Your Version: &e" + SIRPlugin.pluginVersion()
                    );
                    break;

                default:
                    updateLogger(player,
                            "&4WARNING!",
                            "&cCould not check for a new version of S.I.R.",
                            "&7Please check your connection and restart the server.",
                            "&7Possible reason: &e" + result.getReason()
                    );
                    break;
            }
            updateLogger(player, "RR", "");
        });
    }

    public void initUpdater(@Nullable Player player) {
        if (player == null) {
            if (!FileCache.CONFIG.get().getBoolean("updater.plugin.on-start")) return;
            runUpdater(null);
        }
        else {
            if (!FileCache.CONFIG.get().getBoolean("updater.plugin.send-op") ||
                    !PlayerUtils.hasPerm(player, "sir.admin.updater")) return;
            runUpdater(player);
        }
    }
}
