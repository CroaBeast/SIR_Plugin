package me.croabeast.sirplugin.object.analytic;

import lombok.experimental.UtilityClass;
import me.croabeast.sirplugin.SIRPlugin;
import me.croabeast.sirplugin.object.file.FileCache;
import me.croabeast.sirplugin.utility.LogUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class Amender {

    private void runUpdater(@Nullable Player player) {
        Updater.init(SIRPlugin.getInstance(), 96378).updateCheck().whenComplete((result, e) -> {
            String latest = result.getNewestVersion();

            switch (result.getReason()) {
                case NEW_UPDATE:
                    LogUtils.mixLog(player,
                            "true::", "&4NEW UPDATE!",
                            "&cYou don't have the latest version of S.I.R. installed.",
                            "&cRemember, older versions won't receive any support.",
                            "&7New Version: &a" + latest +
                                    "&7 - Your Version: &e" + SIRPlugin.getVersion(),
                            "&7Link:&b https://www.spigotmc.org/resources/96378/",
                            "true::"
                    );
                    break;

                case UP_TO_DATE:
                    LogUtils.mixLog(player,
                            "true::", "&eYou have the latest version of S.I.R. &7(" + latest + ")",
                            "&7I would appreciate if you keep updating &c<3", "true::"
                    );
                    break;

                case UNRELEASED_VERSION:
                    LogUtils.mixLog(player,
                            "true::", "&4DEVELOPMENT BUILD:",
                            "&cYou have a newer version of S.I.R. installed.",
                            "&cErrors might occur in this build.",
                            "Spigot Version: &a" + result.getSpigotVersion() +
                            "&7 - Your Version: &e" + SIRPlugin.getVersion(),
                            "true::"
                    );
                    break;

                default:
                    LogUtils.mixLog(player,
                            "true::", "&4WARNING!",
                            "&cCould not check for a new version of S.I.R.",
                            "&7Please check your connection and restart the server.",
                            "&7Possible reason: &e" + result.getReason(), "true::"
                    );
                    break;
            }
        });
    }

    public void initUpdater(@Nullable Player player) {
        if (player == null) {
            if (!FileCache.MAIN_CONFIG.getValue("updater.plugin.on-start", false)) return;
            runUpdater(null);
        }
        else {
            if (!FileCache.MAIN_CONFIG.getValue("updater.plugin.send-op", false) ||
                    !player.hasPermission("sir.admin.updater")) return;
            runUpdater(player);
        }
    }
}
